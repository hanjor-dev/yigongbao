-- 医工宝备份功能生产数据库一体化迁移脚本
-- 生成日期：2026-10-27
--
-- 用途：将备份候选、凭证、恢复、COS 清理、安全认证、历史数据回填、事件号分配器及
-- COS 清理菜单权限一次性部署到线上医工宝数据库。
--
-- 执行顺序（已按依赖调整）：
-- 01 候选/凭证 -> 02 恢复 -> 03 清理 -> 04 机器安全 -> 05 引用回填 ->
-- 06 候选回填 -> 08 事件号初始化 -> 07 菜单与角色权限。
--
-- 执行前：
-- 1. 确认连接的是线上医工宝业务数据库，并确认 MySQL 8.x。
-- 2. 完成数据库备份，并在低峰期执行。
-- 3. 先审阅每个来源脚本中的执行前检查和业务状态假设（尤其是订单状态 1010/9010）。
-- 4. 本文件不自动开启一个覆盖全部语句的总事务：MySQL DDL 会隐式提交，菜单权限脚本
--    自带独立事务；出现 SIGNAL 时应按错误信息人工处理，不要跳过失败步骤继续开放功能。
-- 5. 05/06 为历史数据回填，会扫描 file_detail/order_main，执行前请评估数据量和锁影响。
--
-- 幂等性：来源脚本中的 CREATE TABLE IF NOT EXISTS、NOT EXISTS 和 UPSERT 逻辑均予以保留。
-- 再次执行前仍应检查来源脚本的验证结果，禁止将 SELECT 验证结果当作错误忽略。
-- ============================================================================

-- ============================================================================
-- 来源脚本：2026-10-27-01-backup-candidate-receipt.sql
-- ============================================================================
-- 备份候选、凭证和云端对象状态。
-- 执行前检查：SELECT VERSION(); 确认 MySQL 8；确认四个目标表不存在或结构与本文件一致。
-- 可重入策略：DDL 使用 CREATE TABLE IF NOT EXISTS；本文件不覆盖任何既有业务数据。

CREATE TABLE IF NOT EXISTS order_backup_candidate (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    event_id BIGINT NOT NULL COMMENT '全局单调候选事件ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    generation INT NOT NULL COMMENT '订单资格代次',
    eligible_at DATETIME(3) NOT NULL COMMENT '本代首次满足备份条件时间',
    order_created_at DATETIME(3) NULL COMMENT '订单创建时间（仅筛选展示）',
    event_status VARCHAR(32) NOT NULL COMMENT 'ACTIVE/CANCELLED/DELETED',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID',
    update_by BIGINT NULL COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_backup_candidate_event (event_id),
    UNIQUE KEY uk_backup_candidate_order_generation (order_id, generation),
    KEY idx_backup_candidate_cursor (eligible_at, event_id),
    KEY idx_backup_candidate_order_status (order_id, event_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单备份候选事件';

CREATE TABLE IF NOT EXISTS order_backup_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    manifest_revision VARCHAR(128) NOT NULL COMMENT '不可变清单修订号',
    manifest_sha256 CHAR(64) NOT NULL COMMENT 'JCS 清单 SHA-256',
    package_sha256 CHAR(64) NOT NULL COMMENT '订单包 SHA-256',
    file_count INT NOT NULL COMMENT '文件数量',
    source_total_size BIGINT NOT NULL COMMENT '源文件总字节数',
    local_idempotency_key VARCHAR(128) NOT NULL COMMENT '本地幂等键',
    receipt_version INT NOT NULL COMMENT '订单凭证版本',
    previous_receipt_id BIGINT NULL COMMENT '上一凭证ID',
    is_current TINYINT NOT NULL DEFAULT 1 COMMENT '是否当前凭证',
    current_order_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_current = 1 AND is_deleted = 0 THEN order_id ELSE NULL END) STORED COMMENT '当前凭证唯一键',
    active_order_id BIGINT GENERATED ALWAYS AS (CASE WHEN receipt_status = 'ACTIVE' AND is_current = 1 AND is_deleted = 0 THEN order_id ELSE NULL END) STORED COMMENT '当前活动凭证唯一键',
    state_version INT NOT NULL DEFAULT 0 COMMENT '状态 CAS 版本',
    backup_completed_at DATETIME(3) NOT NULL COMMENT '本地备份完成时间',
    receipt_status VARCHAR(32) NOT NULL COMMENT 'ACTIVE/SUSPENDED/INVALIDATED',
    last_verified_at DATETIME(3) NULL COMMENT '最近完整校验时间',
    verification_source VARCHAR(64) NULL COMMENT '校验来源',
    invalidated_at DATETIME(3) NULL COMMENT '永久失效时间',
    invalidated_reason VARCHAR(512) NULL COMMENT '永久失效原因',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID',
    update_by BIGINT NULL COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_backup_receipt_order_version (order_id, receipt_version),
    UNIQUE KEY uk_backup_receipt_idempotency (local_idempotency_key),
    UNIQUE KEY uk_backup_receipt_current_order (current_order_id),
    UNIQUE KEY uk_backup_receipt_active_order (active_order_id),
    KEY idx_backup_receipt_status_verified (receipt_status, last_verified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单备份凭证';

CREATE TABLE IF NOT EXISTS order_backup_receipt_object (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    receipt_id BIGINT NOT NULL COMMENT '备份凭证ID',
    asset_identity VARCHAR(255) NOT NULL COMMENT '清单资产稳定身份',
    file_id BIGINT NULL COMMENT '线上文件ID',
    asset_type VARCHAR(64) NOT NULL COMMENT '资产类型',
    platform VARCHAR(64) NOT NULL COMMENT '存储平台',
    bucket VARCHAR(255) NOT NULL COMMENT 'Bucket',
    object_key VARCHAR(1024) NOT NULL COMMENT '对象键',
    source_url TEXT NULL COMMENT '备份时访问地址',
    size_bytes BIGINT NOT NULL COMMENT '对象字节数',
    etag VARCHAR(255) NULL COMMENT '对象 ETag',
    version_identity VARCHAR(255) NOT NULL COMMENT '精确版本、字面值 null 或 *',
    sha256 CHAR(64) NULL COMMENT '对象 SHA-256',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID',
    update_by BIGINT NULL COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_backup_receipt_object (receipt_id, asset_identity),
    KEY idx_backup_receipt_object_file (file_id),
    KEY idx_backup_receipt_object_identity (platform, bucket, object_key(191), version_identity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备份凭证对象明细';

CREATE TABLE IF NOT EXISTS file_cloud_state (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    asset_identity VARCHAR(255) NOT NULL COMMENT '资产稳定身份',
    file_id BIGINT NULL COMMENT '线上文件ID，可空',
    platform VARCHAR(64) NOT NULL COMMENT '存储平台',
    bucket VARCHAR(255) NOT NULL COMMENT 'Bucket',
    object_key VARCHAR(1024) NOT NULL COMMENT '对象键',
    version_identity VARCHAR(255) NOT NULL COMMENT '精确版本、字面值 null 或 *',
    cloud_state VARCHAR(32) NOT NULL COMMENT 'PRESENT/CLEARED/RESTORING/ERROR/UNKNOWN',
    last_known_url TEXT NULL COMMENT '最近已知访问地址',
    cleared_at DATETIME(3) NULL COMMENT '清理时间',
    restored_at DATETIME(3) NULL COMMENT '恢复时间',
    cleanup_task_id BIGINT NULL COMMENT '最近清理任务ID',
    last_error VARCHAR(1024) NULL COMMENT '最近错误',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID',
    update_by BIGINT NULL COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_cloud_asset (asset_identity),
    KEY idx_file_cloud_file (file_id),
    KEY idx_file_cloud_state (cloud_state, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件云端权威状态';

-- 执行后验证：以下查询应返回 4，且重复执行本文件仍返回 4。
SELECT COUNT(*) AS created_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('order_backup_candidate','order_backup_receipt','order_backup_receipt_object','file_cloud_state');
-- 验证唯一键：SHOW INDEX FROM order_backup_receipt; SHOW INDEX FROM order_backup_candidate;
-- 人工回滚：功能未启用且表为空时方可 DROP；生产已有凭证/候选证据时禁止删除，改为关闭调度与入口。

-- ============================================================================
-- 来源脚本：2026-10-27-02-backup-restore.sql
-- ============================================================================
-- 线上恢复会话、不可变计划、幂等提交与对象证据。
-- 执行前检查：确认 MySQL 8；确认未存在同名异构表。DDL 全部可重入。

CREATE TABLE IF NOT EXISTS order_restore_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', order_id BIGINT NOT NULL COMMENT '订单ID',
    restore_session_id VARCHAR(128) NOT NULL COMMENT '外部恢复会话号', idempotency_key VARCHAR(128) NOT NULL COMMENT 'prepare 幂等键',
    request_digest CHAR(64) NOT NULL COMMENT 'prepare 请求摘要', manifest_revision VARCHAR(128) NOT NULL COMMENT '清单修订号',
    manifest_sha256 CHAR(64) NOT NULL COMMENT '清单哈希', conflict_policy VARCHAR(32) NOT NULL COMMENT '冲突策略',
    relation_set_hash CHAR(64) NOT NULL COMMENT '关系集合哈希', session_status VARCHAR(32) NOT NULL COMMENT '会话状态',
    protection_until DATETIME(3) NULL COMMENT '恢复后保护期', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id), UNIQUE KEY uk_restore_session_no (restore_session_id), UNIQUE KEY uk_restore_session_idempotency (idempotency_key),
    KEY idx_restore_session_order_status (order_id, session_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线上订单恢复会话';

CREATE TABLE IF NOT EXISTS order_restore_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', session_id BIGINT NOT NULL COMMENT '恢复会话ID',
    signed_plan_digest CHAR(64) NOT NULL COMMENT '不可变签名计划摘要', relation_set_hash CHAR(64) NOT NULL COMMENT '关系集合哈希',
    order_version_token VARCHAR(255) NOT NULL COMMENT '订单版本令牌', version_tokens_json JSON NOT NULL COMMENT 'typed 关系版本令牌',
    upload_targets_json JSON NOT NULL COMMENT '限会话前缀上传目标', plan_status VARCHAR(32) NOT NULL COMMENT 'ACTIVE/CONSUMED/ABORTED',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id), UNIQUE KEY uk_restore_plan_session (session_id), UNIQUE KEY uk_restore_plan_digest (signed_plan_digest)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变恢复计划';

CREATE TABLE IF NOT EXISTS order_restore_commit (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', idempotency_key VARCHAR(128) NOT NULL COMMENT 'commit 幂等键',
    restore_session_id VARCHAR(128) NOT NULL COMMENT '恢复会话号', order_id BIGINT NOT NULL COMMENT '订单ID',
    request_digest CHAR(64) NOT NULL COMMENT '提交请求摘要', signed_plan_digest CHAR(64) NOT NULL COMMENT '签名计划摘要',
    commit_status VARCHAR(32) NOT NULL COMMENT 'COMMITTED/FAILED/RESULT_UNKNOWN', result_json JSON NULL COMMENT '原始业务结果',
    id_mapping_digest CHAR(64) NULL COMMENT '原新ID映射摘要', committed_at DATETIME(3) NULL COMMENT '提交时间',
    protection_until DATETIME(3) NULL COMMENT '恢复后保护期',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id), UNIQUE KEY uk_restore_commit_idempotency (idempotency_key), UNIQUE KEY uk_restore_commit_session (restore_session_id),
    KEY idx_restore_commit_order (order_id, committed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='恢复幂等提交证据';

CREATE TABLE IF NOT EXISTS order_restore_object (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', session_id BIGINT NOT NULL COMMENT '恢复会话ID',
    asset_identity VARCHAR(255) NOT NULL COMMENT '资产身份', original_file_id BIGINT NULL COMMENT '原文件ID',
    target_object_key VARCHAR(1024) NOT NULL COMMENT '新对象键', version_id VARCHAR(255) NULL COMMENT 'COS VersionId',
    etag VARCHAR(255) NULL COMMENT 'ETag', size_bytes BIGINT NOT NULL COMMENT '实际字节数', sha256 CHAR(64) NOT NULL COMMENT 'SHA-256',
    evidence_digest CHAR(64) NOT NULL COMMENT '上传证据摘要', object_status VARCHAR(32) NOT NULL COMMENT 'PLANNED/UPLOADED/VERIFIED/COMMITTED',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id), UNIQUE KEY uk_restore_object_session_asset (session_id, asset_identity), KEY idx_restore_object_status (session_id, object_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='恢复对象证据';

-- 执行后验证：应返回 4；并检查所有 UNIQUE 索引存在。
SELECT COUNT(*) AS created_table_count FROM information_schema.tables WHERE table_schema=DATABASE()
AND table_name IN ('order_restore_session','order_restore_plan','order_restore_commit','order_restore_object');
-- 人工回滚：生产已有恢复会话、提交或对象证据时禁止 DROP；关闭恢复入口并保留审计证据。

-- ============================================================================
-- 来源脚本：2026-10-27-03-backup-cleanup.sql
-- ============================================================================
-- COS 清理引用、策略、快照、任务、对象结果、隔离、保护和删除授权。
-- 执行前检查：Bucket Versioning 和引用索引尚未验收前保持 ACTUAL 功能开关关闭。
-- 可重入策略：仅 CREATE TABLE IF NOT EXISTS，不覆盖既有任务或 permit。

CREATE TABLE IF NOT EXISTS cos_object_reference (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', platform VARCHAR(64) NOT NULL COMMENT '平台', bucket VARCHAR(255) NOT NULL COMMENT 'Bucket',
 object_key VARCHAR(1024) NOT NULL COMMENT '对象键', version_identity VARCHAR(255) NOT NULL COMMENT '精确版本、null 或 *', file_id BIGINT NULL COMMENT '文件ID',
 relation_kind VARCHAR(64) NOT NULL COMMENT '白名单关系类型', relation_id BIGINT NOT NULL COMMENT '关系记录ID', active TINYINT NOT NULL DEFAULT 1 COMMENT '活动引用',
 source_updated_at DATETIME(3) NULL COMMENT '源关系更新时间', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
 identity_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '完整对象身份 SHA-256：platform、bucket、object_key、version_identity、relation_kind、relation_id',
 PRIMARY KEY(id), UNIQUE KEY uk_cos_reference_identity(identity_hash),
 KEY idx_cos_reference_lookup (platform,bucket,object_key(191),version_identity,active,is_deleted), KEY idx_cos_reference_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='COS 规范化业务引用索引';

CREATE TABLE IF NOT EXISTS cos_cleanup_policy (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', policy_code VARCHAR(64) NOT NULL COMMENT '策略编码', policy_version INT NOT NULL COMMENT '策略版本',
 policy_json JSON NOT NULL COMMENT '不可变策略参数', enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
 PRIMARY KEY(id), UNIQUE KEY uk_cleanup_policy_version (policy_code,policy_version), KEY idx_cleanup_policy_enabled (enabled,is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='COS 清理策略版本';

CREATE TABLE IF NOT EXISTS cos_cleanup_snapshot (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', order_id BIGINT NOT NULL COMMENT '订单ID', receipt_id BIGINT NOT NULL COMMENT '当前 ACTIVE 凭证ID',
 snapshot_hash CHAR(64) NOT NULL COMMENT '不可变快照哈希', object_set_hash CHAR(64) NOT NULL COMMENT '对象集合哈希',
 order_status INT NOT NULL COMMENT '固化订单状态', classic_case TINYINT NOT NULL COMMENT '固化经典案例标记', snapshot_json JSON NOT NULL COMMENT '不可变评估输入',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
 PRIMARY KEY(id), UNIQUE KEY uk_cleanup_snapshot_hash (snapshot_hash), KEY idx_cleanup_snapshot_order (order_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='COS 清理不可变候选快照';

CREATE TABLE IF NOT EXISTS cos_cleanup_task (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', task_no VARCHAR(64) NOT NULL COMMENT '任务号', idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
 policy_id BIGINT NOT NULL COMMENT '策略ID', snapshot_id BIGINT NOT NULL COMMENT '快照ID', mode VARCHAR(32) NOT NULL COMMENT 'DRY_RUN/ACTUAL',
 task_status VARCHAR(32) NOT NULL COMMENT '任务状态', estimated_version_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '预计释放字节', actual_purged_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '实际物理清理字节',
 failure_code VARCHAR(64) NULL COMMENT '失败码', failure_message VARCHAR(1024) NULL COMMENT '失败原因', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
 create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
 PRIMARY KEY(id), UNIQUE KEY uk_cleanup_task_no(task_no), UNIQUE KEY uk_cleanup_task_idempotency(idempotency_key), KEY idx_cleanup_task_status(task_status,update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='COS 清理任务';

CREATE TABLE IF NOT EXISTS cos_cleanup_item (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', cleanup_task_id BIGINT NOT NULL COMMENT '清理任务ID', file_id BIGINT NULL COMMENT '文件ID',
 platform VARCHAR(64) NOT NULL COMMENT '平台', bucket VARCHAR(255) NOT NULL COMMENT 'Bucket', object_key VARCHAR(1024) NOT NULL COMMENT '对象键', version_identity VARCHAR(255) NOT NULL COMMENT '物理版本身份',
 shared_reference_count INT NOT NULL DEFAULT 0 COMMENT '活动共享引用数', delete_status VARCHAR(32) NOT NULL COMMENT 'PENDING/BLOCKED/URL_INACCESSIBLE/PHYSICALLY_PURGED/FAILED',
 version_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '版本字节数', retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数', last_error VARCHAR(1024) NULL COMMENT '错误',
 version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本', create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
 update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间', create_by BIGINT NULL COMMENT '创建人ID',
 update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', PRIMARY KEY(id),
 UNIQUE KEY uk_cleanup_item_identity(cleanup_task_id,platform,bucket,object_key(191),version_identity), KEY idx_cleanup_item_status(cleanup_task_id,delete_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='COS 清理物理版本结果';

CREATE TABLE IF NOT EXISTS cos_cleanup_reservation (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', order_id BIGINT NOT NULL COMMENT '隔离订单ID', cleanup_task_id BIGINT NOT NULL COMMENT '清理任务ID',
 object_set_hash CHAR(64) NOT NULL COMMENT '对象集合哈希', reservation_status VARCHAR(32) NOT NULL COMMENT 'DELETING/RELEASED/EXPIRED', fencing_token BIGINT NOT NULL COMMENT '单调令牌',
 lease_until DATETIME(3) NOT NULL COMMENT '租约截止', active_order_id BIGINT GENERATED ALWAYS AS (CASE WHEN reservation_status='DELETING' AND is_deleted=0 THEN order_id ELSE NULL END) STORED COMMENT '有效订单唯一键',
 version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本', create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
 PRIMARY KEY(id), UNIQUE KEY uk_cleanup_reservation_active(active_order_id), KEY idx_cleanup_reservation_lease(reservation_status,lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='清理订单写隔离';

CREATE TABLE IF NOT EXISTS cos_cleanup_protection (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', order_id BIGINT NOT NULL COMMENT '订单ID', protection_type VARCHAR(32) NOT NULL COMMENT 'MANUAL/RESTORE',
 reason VARCHAR(512) NOT NULL COMMENT '保护原因', protected_until DATETIME(3) NULL COMMENT '空表示永久', active TINYINT NOT NULL DEFAULT 1 COMMENT '是否生效',
 version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本', create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
 PRIMARY KEY(id), KEY idx_cleanup_protection_order(order_id,active,protected_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='COS 清理保护名单';

CREATE TABLE IF NOT EXISTS cos_delete_permit (
 id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', permit_id VARCHAR(128) NOT NULL COMMENT '外部 permit 标识', cleanup_task_id BIGINT NOT NULL COMMENT '清理任务ID',
 attempt_no INT NOT NULL COMMENT '签发尝试号', permit_status VARCHAR(32) NOT NULL COMMENT 'REQUESTED/ISSUED/CLAIMED/CONSUMED/EXPIRED/REVOKED',
 cleanup_snapshot_hash CHAR(64) NOT NULL COMMENT '快照哈希', object_set_hash CHAR(64) NOT NULL COMMENT '对象集合哈希', fencing_token BIGINT NOT NULL COMMENT '隔离令牌',
 receipt_id BIGINT NOT NULL COMMENT '本地核验凭证ID', receipt_version INT NOT NULL COMMENT '凭证版本', package_hash CHAR(64) NOT NULL COMMENT '本地包哈希',
 nonce VARCHAR(128) NOT NULL COMMENT '一次性随机值', issued_at DATETIME(3) NULL COMMENT '签发时间', expires_at DATETIME(3) NULL COMMENT '过期时间',
 active_task_id BIGINT GENERATED ALWAYS AS (CASE WHEN permit_status IN ('REQUESTED','ISSUED','CLAIMED') AND is_deleted=0 THEN cleanup_task_id ELSE NULL END) STORED COMMENT '每任务有效permit唯一键',
 version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本', create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除', PRIMARY KEY(id),
 UNIQUE KEY uk_delete_permit_id(permit_id), UNIQUE KEY uk_delete_permit_nonce(nonce), UNIQUE KEY uk_delete_permit_attempt(cleanup_task_id,attempt_no), UNIQUE KEY uk_delete_permit_active(active_task_id),
 KEY idx_delete_permit_expiry(permit_status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短期 COS 删除授权';

-- 执行后验证：第一条应返回 8；第二条应返回 1，且仅包含 identity_hash；ACTUAL 功能开关仍须保持关闭。
SELECT COUNT(*) AS created_table_count FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN
('cos_object_reference','cos_cleanup_policy','cos_cleanup_snapshot','cos_cleanup_task','cos_cleanup_item','cos_cleanup_reservation','cos_cleanup_protection','cos_delete_permit');
SELECT COUNT(*) AS bounded_identity_index_count
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='cos_object_reference'
  AND index_name='uk_cos_reference_identity' AND column_name='identity_hash' AND sub_part IS NULL;
-- 人工回滚：已有快照、任务、隔离或 permit 时禁止 DROP；仅关闭 ACTUAL 模式并保留全部安全证据。

-- ============================================================================
-- 来源脚本：2026-10-27-04-backup-security.sql
-- ============================================================================
-- 机器身份、密钥元数据和 HMAC nonce 防重放。
-- 安全说明：密钥正文只存在外部密钥存储，绝不写入本数据库；scope 包含 CLEANUP_PERMIT。
-- 执行前检查：确认密钥引用格式和 nonce 保留窗口配置；DDL 使用 IF NOT EXISTS 可重入。

CREATE TABLE IF NOT EXISTS backup_machine_identity (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    machine_id VARCHAR(128) NOT NULL COMMENT '机器稳定标识',
    display_name VARCHAR(128) NOT NULL COMMENT '机器显示名称',
    machine_status VARCHAR(32) NOT NULL COMMENT 'ACTIVE/SUSPENDED/REVOKED',
    allowed_scopes VARCHAR(512) NOT NULL COMMENT 'MANIFEST,ACCOUNT,RESTORE,CLEANUP,CLEANUP_PERMIT',
    last_seen_at DATETIME(3) NULL COMMENT '最近认证成功时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id), UNIQUE KEY uk_backup_machine_identity (machine_id), KEY idx_backup_machine_status (machine_status,is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备份机器身份';

CREATE TABLE IF NOT EXISTS backup_machine_key (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', machine_id VARCHAR(128) NOT NULL COMMENT '机器标识',
    key_id VARCHAR(128) NOT NULL COMMENT '密钥版本标识', secret_ref VARCHAR(512) NOT NULL COMMENT '外部密钥引用（非正文）',
    allowed_scopes VARCHAR(512) NOT NULL COMMENT 'MANIFEST,ACCOUNT,RESTORE,CLEANUP,CLEANUP_PERMIT',
    key_status VARCHAR(32) NOT NULL COMMENT 'ACTIVE/RETIRING/REVOKED', valid_from DATETIME(3) NOT NULL COMMENT '生效时间',
    valid_until DATETIME(3) NULL COMMENT '失效时间', revoked_at DATETIME(3) NULL COMMENT '吊销时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本', create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id), UNIQUE KEY uk_backup_machine_key (machine_id,key_id), KEY idx_backup_machine_key_validity (key_status,valid_from,valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备份机器密钥元数据';

CREATE TABLE IF NOT EXISTS backup_hmac_nonce (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', machine_id VARCHAR(128) NOT NULL COMMENT '机器标识',
    key_id VARCHAR(128) NOT NULL COMMENT '密钥版本标识', nonce_hash CHAR(64) NOT NULL COMMENT 'nonce SHA-256',
    request_timestamp DATETIME(3) NOT NULL COMMENT '签名请求时间', expires_at DATETIME(3) NOT NULL COMMENT '安全窗口后过期时间',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id), UNIQUE KEY uk_backup_hmac_nonce (machine_id,key_id,nonce_hash), KEY idx_backup_hmac_nonce_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='HMAC nonce 防重放记录';

-- 执行后验证：应返回 3；secret_ref 外不得出现任何密钥正文列。
SELECT COUNT(*) AS created_table_count FROM information_schema.tables WHERE table_schema=DATABASE()
AND table_name IN ('backup_machine_identity','backup_machine_key','backup_hmac_nonce');
SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='backup_machine_key'
AND column_name IN ('secret','secret_value','key_plaintext');
-- 人工回滚：有认证记录时不删除；先吊销 key、停用机器身份，待审计保留期结束后另行评审。

-- ============================================================================
-- 来源脚本：2026-10-27-05-backup-reference-backfill.sql
-- ============================================================================
-- 将现有 file_detail 业务关系分批回填到 cos_object_reference，并记录水位和统计。
-- 执行前检查：先执行 03-backup-cleanup.sql；确认 file_detail.id 为十进制字符串且 path/base_path 能还原对象键。
-- bucket 优先读取 metadata.bucket；无法解析时写 UNKNOWN 并由验收查询阻止开放实际删除。

CREATE TABLE IF NOT EXISTS backup_migration_watermark (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', migration_code VARCHAR(128) NOT NULL COMMENT '迁移编码',
    shard_key VARCHAR(128) NOT NULL COMMENT '分片键', high_watermark VARCHAR(255) NOT NULL COMMENT '已处理高水位',
    scanned_count BIGINT NOT NULL DEFAULT 0 COMMENT '扫描数量', inserted_count BIGINT NOT NULL DEFAULT 0 COMMENT '插入数量',
    unresolved_count BIGINT NOT NULL DEFAULT 0 COMMENT '未解析数量', status VARCHAR(32) NOT NULL COMMENT 'RUNNING/COMPLETED/NEEDS_REVIEW',
    stats_json JSON NULL COMMENT '校验统计', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY(id), UNIQUE KEY uk_backup_migration_watermark(migration_code,shard_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备份迁移水位与校验统计';

SET @reference_before := (SELECT COUNT(*) FROM cos_object_reference WHERE relation_kind='FILE_DETAIL' AND is_deleted=0);

INSERT INTO cos_object_reference
    (platform,bucket,object_key,version_identity,file_id,relation_kind,relation_id,identity_hash,active,source_updated_at,
     create_time,update_time,create_by,update_by,is_deleted)
SELECT COALESCE(NULLIF(fd.platform,''),'UNKNOWN'),
       COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(fd.metadata) THEN fd.metadata ELSE '{}' END,'$.bucket')),''),'UNKNOWN'),
       COALESCE(NULLIF(CONCAT(COALESCE(fd.base_path,''),COALESCE(fd.path,''),COALESCE(fd.filename,'')),''),fd.url),
       COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(fd.metadata) THEN fd.metadata ELSE '{}' END,'$.versionId')),''),'null'),
       CAST(fd.id AS UNSIGNED),'FILE_DETAIL',CAST(fd.id AS UNSIGNED),
       SHA2(CONCAT_WS(CHAR(0),
           COALESCE(NULLIF(fd.platform,''),'UNKNOWN'),
           COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(fd.metadata) THEN fd.metadata ELSE '{}' END,'$.bucket')),''),'UNKNOWN'),
           COALESCE(NULLIF(CONCAT(COALESCE(fd.base_path,''),COALESCE(fd.path,''),COALESCE(fd.filename,'')),''),fd.url),
           COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(fd.metadata) THEN fd.metadata ELSE '{}' END,'$.versionId')),''),'null'),
           'FILE_DETAIL',CAST(fd.id AS UNSIGNED)),256),
       1,fd.update_time,
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),NULL,NULL,0
FROM file_detail fd
WHERE fd.is_deleted=0 AND fd.id REGEXP '^[0-9]+$'
  AND NOT EXISTS (
      SELECT 1 FROM cos_object_reference r
      WHERE r.relation_kind='FILE_DETAIL' AND r.relation_id=CAST(fd.id AS UNSIGNED)
        AND r.platform=COALESCE(NULLIF(fd.platform,''),'UNKNOWN') AND r.is_deleted=0
  );

SET @reference_after := (SELECT COUNT(*) FROM cos_object_reference WHERE relation_kind='FILE_DETAIL' AND is_deleted=0);
SET @file_high_watermark := COALESCE((SELECT MAX(CAST(id AS UNSIGNED)) FROM file_detail WHERE id REGEXP '^[0-9]+$'),0);
INSERT INTO backup_migration_watermark
    (migration_code,shard_key,high_watermark,scanned_count,inserted_count,unresolved_count,status,stats_json,create_time,update_time,is_deleted)
VALUES
    ('2026-10-27-reference-backfill','file_detail',CAST(@file_high_watermark AS CHAR),
     (SELECT COUNT(*) FROM file_detail WHERE is_deleted=0),@reference_after-@reference_before,
     (SELECT COUNT(*) FROM cos_object_reference WHERE relation_kind='FILE_DETAIL' AND (platform='UNKNOWN' OR bucket='UNKNOWN') AND is_deleted=0),
     IF((SELECT COUNT(*) FROM cos_object_reference WHERE relation_kind='FILE_DETAIL' AND (platform='UNKNOWN' OR bucket='UNKNOWN') AND is_deleted=0)=0,'COMPLETED','NEEDS_REVIEW'),
     JSON_OBJECT('before',@reference_before,'after',@reference_after),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),0)
ON DUPLICATE KEY UPDATE high_watermark=VALUES(high_watermark), scanned_count=VALUES(scanned_count),
 inserted_count=inserted_count+VALUES(inserted_count), unresolved_count=VALUES(unresolved_count), status=VALUES(status),
 stats_json=VALUES(stats_json), update_time=CURRENT_TIMESTAMP(3);

-- 执行后验证：第二次执行 inserted_count 增量应为 0；UNKNOWN 必须治理为 0 才可启用 ACTUAL 清理。
SELECT migration_code,high_watermark,scanned_count,inserted_count,unresolved_count,status FROM backup_migration_watermark
WHERE migration_code='2026-10-27-reference-backfill' AND shard_key='file_detail';
SELECT platform,bucket,COUNT(*) unresolved FROM cos_object_reference WHERE relation_kind='FILE_DETAIL'
AND (platform='UNKNOWN' OR bucket='UNKNOWN') AND is_deleted=0 GROUP BY platform,bucket;
-- 人工回滚：只允许按 migration_code 审核后删除本脚本生成且未被业务维护的引用；不得删除引用表或业务文件。

-- ============================================================================
-- 来源脚本：2026-10-27-06-backup-candidate-backfill.sql
-- ============================================================================
-- 为历史正式、非取消、非删除订单补建 generation=1 候选事件。
-- 执行前检查：先执行 01 和 05 迁移；复核 status=1010 为草稿、status=9010 为取消。
-- 正式订单判定：非草稿 status<>1010、非取消 status<>9010、is_deleted=0；上线前由业务负责人复核状态集。
-- 重新获得资格的订单由运行时代码在同一事务追加 generation+1，本脚本只处理首次历史基线。

SET @candidate_before := (SELECT COUNT(*) FROM order_backup_candidate WHERE is_deleted=0);
INSERT INTO order_backup_candidate
    (event_id,order_id,generation,eligible_at,order_created_at,event_status,version,create_time,update_time,create_by,update_by,is_deleted)
SELECT o.id,o.id,1,COALESCE(o.update_time,o.create_time,CURRENT_TIMESTAMP(3)),o.create_time,'ACTIVE',0,
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),NULL,NULL,0
FROM order_main o
WHERE o.is_deleted=0 AND o.status<>1010 AND o.status<>9010
  AND NOT EXISTS (SELECT 1 FROM order_backup_candidate c WHERE c.order_id=o.id AND c.generation=1);
SET @candidate_after := (SELECT COUNT(*) FROM order_backup_candidate WHERE is_deleted=0);

INSERT INTO backup_migration_watermark
    (migration_code,shard_key,high_watermark,scanned_count,inserted_count,unresolved_count,status,stats_json,create_time,update_time,is_deleted)
VALUES ('2026-10-27-candidate-backfill','order_main',
    CAST(COALESCE((SELECT MAX(id) FROM order_main),0) AS CHAR),
    (SELECT COUNT(*) FROM order_main WHERE is_deleted=0 AND status<>1010 AND status<>9010),
    @candidate_after-@candidate_before,
    (SELECT COUNT(*) FROM order_main o WHERE o.is_deleted=0 AND o.status<>1010 AND o.status<>9010
      AND NOT EXISTS (SELECT 1 FROM order_backup_candidate c WHERE c.order_id=o.id AND c.generation=1)),
    'COMPLETED',JSON_OBJECT('before',@candidate_before,'after',@candidate_after),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),0)
ON DUPLICATE KEY UPDATE high_watermark=VALUES(high_watermark), scanned_count=VALUES(scanned_count),
 inserted_count=inserted_count+VALUES(inserted_count), unresolved_count=VALUES(unresolved_count), status=VALUES(status),
 stats_json=VALUES(stats_json), update_time=CURRENT_TIMESTAMP(3);

-- 执行后验证：missing_count 必须为 0；重复执行 new_rows 必须为 0。
SELECT COUNT(*) missing_count FROM order_main o WHERE o.is_deleted=0 AND o.status<>1010 AND o.status<>9010
AND NOT EXISTS (SELECT 1 FROM order_backup_candidate c WHERE c.order_id=o.id AND c.generation=1);
SELECT @candidate_after-@candidate_before AS new_rows;
-- 人工回滚：候选事件是审计事实，生产环境禁止删除；状态判定有误时追加纠正事件并暂停调度。

-- ============================================================================
-- 来源脚本：2026-10-27-08-backup-candidate-event-sequence.sql
-- ============================================================================
-- 备份候选事件号分配器；通过单行 UPDATE 的 InnoDB 行锁保证多实例并发唯一。
CREATE TABLE IF NOT EXISTS backup_candidate_event_sequence (
    id TINYINT NOT NULL COMMENT '固定单行ID',
    next_event_id BIGINT NOT NULL COMMENT '最近已分配事件号',
    PRIMARY KEY (id),
    CONSTRAINT chk_backup_candidate_event_sequence_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='备份候选事件号分配器';

INSERT INTO backup_candidate_event_sequence (id, next_event_id)
SELECT 1, COALESCE(MAX(event_id), 0) FROM order_backup_candidate
ON DUPLICATE KEY UPDATE next_event_id = GREATEST(next_event_id, VALUES(next_event_id));

-- 验证：SELECT next_event_id FROM backup_candidate_event_sequence WHERE id = 1;

-- ============================================================================
-- 来源脚本：2026-10-27-07-backup-cleanup-resource.sql
-- ============================================================================
-- COS 清理菜单、按钮及角色授权。
-- 执行前检查：Backup 父资源必须启用且未删除并且唯一；admin、company-admin 两角色必须各唯一、启用、未删除。
-- 可重入策略：按 resource_code 和 (role_id,resource_id) 判重；不使用任何固定角色/资源主键。

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_backup_cleanup_resources$$
CREATE PROCEDURE migrate_backup_cleanup_resources()
BEGIN
    DECLARE v_backup_parent_count INT DEFAULT 0;
    DECLARE v_target_role_count INT DEFAULT 0;
    DECLARE v_cleanup_menu_id BIGINT DEFAULT NULL;
    DECLARE v_conflict_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_backup_parent_count
      FROM sys_resource
     WHERE resource_code='Backup' AND resource_type=1 AND status=1 AND is_deleted=0;
    IF v_backup_parent_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Backup 父资源不存在、重复、类型错误或已停用；请人工处理';
    END IF;

    SELECT COUNT(*) INTO v_target_role_count
      FROM sys_role
     WHERE role_code IN ('admin','company-admin') AND status=1 AND is_deleted=0;
    IF v_target_role_count <> 2
       OR (SELECT COUNT(*) FROM sys_role WHERE role_code='admin' AND status=1 AND is_deleted=0) <> 1
       OR (SELECT COUNT(*) FROM sys_role WHERE role_code='company-admin' AND status=1 AND is_deleted=0) <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='admin、company-admin 必须各有且仅有一个启用且未删除角色';
    END IF;

    SELECT COUNT(*) INTO v_conflict_count
      FROM sys_resource r
     WHERE r.resource_code='BackupCleanup'
       AND NOT (r.parent_id=(SELECT id FROM sys_resource WHERE resource_code='Backup' AND resource_type=1 AND status=1 AND is_deleted=0)
                AND r.resource_type=2 AND r.status=1 AND r.is_deleted=0);
    IF v_conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BackupCleanup 已存在冲突定义；拒绝自动覆盖';
    END IF;

    INSERT INTO sys_resource
        (parent_id,resource_name,resource_code,resource_type,icon,path,component,redirect,sort,visible,status,remark,
         create_time,update_time,create_by,update_by,is_deleted)
    SELECT p.id,'COS 清理','BackupCleanup',2,'Delete','/cleanup','system/backup/CleanupTaskList.vue',NULL,10,1,1,
           '备份管理下的 COS 清理任务与保护名单',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),NULL,NULL,0
      FROM sys_resource p
     WHERE p.resource_code='Backup' AND p.resource_type=1 AND p.status=1 AND p.is_deleted=0
       AND NOT EXISTS (SELECT 1 FROM sys_resource x WHERE x.resource_code='BackupCleanup');

    SELECT id INTO v_cleanup_menu_id
      FROM sys_resource
     WHERE resource_code='BackupCleanup' AND resource_type=2 AND status=1 AND is_deleted=0;

    SELECT COUNT(*) INTO v_conflict_count
      FROM sys_resource r
     WHERE r.resource_code IN ('backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun',
                               'backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       AND NOT (r.parent_id=v_cleanup_menu_id AND r.resource_type=3 AND r.status=1 AND r.is_deleted=0);
    IF v_conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='备份清理按钮已存在冲突定义；拒绝自动覆盖';
    END IF;

    INSERT INTO sys_resource
        (parent_id,resource_name,resource_code,resource_type,icon,path,component,redirect,sort,visible,status,remark,
         create_time,update_time,create_by,update_by,is_deleted)
    SELECT v_cleanup_menu_id,v.resource_name,v.resource_code,3,NULL,NULL,NULL,NULL,v.sort_no,1,1,v.remark,
           CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),NULL,NULL,0
      FROM (
        SELECT '查看清理任务列表' resource_name,'backup-cleanup:List' resource_code,1 sort_no,'清理任务分页查询' remark
        UNION ALL SELECT '查看清理任务详情','backup-cleanup:View',2,'任务、快照、明细和阻断原因'
        UNION ALL SELECT '创建清理试运行','backup-cleanup:DryRun',3,'仅试运行，不签发实际删除授权'
        UNION ALL SELECT '暂停清理任务','backup-cleanup:Pause',4,'暂停任务'
        UNION ALL SELECT '恢复清理任务','backup-cleanup:Resume',5,'恢复任务'
        UNION ALL SELECT '管理清理保护名单','backup-cleanup:Protection',6,'保护名单查询、新增和移除'
      ) v
     WHERE NOT EXISTS (SELECT 1 FROM sys_resource x WHERE x.resource_code=v.resource_code);

    INSERT INTO sys_role_resource (role_id,resource_id)
    SELECT role_row.id,resource_row.id
      FROM sys_role role_row
      JOIN sys_resource resource_row
        ON resource_row.resource_code IN ('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun',
                                          'backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       AND resource_row.status=1 AND resource_row.is_deleted=0
     WHERE role_row.role_code IN ('admin','company-admin')
       AND role_row.status=1 AND role_row.is_deleted=0
       AND NOT EXISTS (
         SELECT 1 FROM sys_role_resource existing_relation
          WHERE existing_relation.role_id=role_row.id AND existing_relation.resource_id=resource_row.id
       );

    IF (SELECT COUNT(*) FROM sys_resource WHERE resource_code IN
       ('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       AND status=1 AND is_deleted=0) <> 7 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='执行后资源数量不是 7；事务回滚';
    END IF;
    IF EXISTS (
       SELECT 1 FROM sys_role_resource rr
       JOIN sys_resource res ON res.id=rr.resource_id
       JOIN sys_role role_row ON role_row.id=rr.role_id
       WHERE res.resource_code IN
       ('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       AND role_row.role_code NOT IN ('admin','company-admin')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='发现非目标角色关联；拒绝继续并要求人工评估';
    END IF;
    IF EXISTS (
       SELECT role_row.role_code
       FROM sys_role role_row
       LEFT JOIN sys_role_resource rr ON rr.role_id=role_row.id
       LEFT JOIN sys_resource res ON res.id=rr.resource_id AND res.resource_code IN
       ('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       WHERE role_row.role_code IN ('admin','company-admin') AND role_row.status=1 AND role_row.is_deleted=0
       GROUP BY role_row.id,role_row.role_code HAVING COUNT(res.id)<>7
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='目标角色未各获得 7 条资源关联；事务回滚';
    END IF;
END$$
DELIMITER ;

START TRANSACTION;
CALL migrate_backup_cleanup_resources();
COMMIT;
DROP PROCEDURE IF EXISTS migrate_backup_cleanup_resources;

-- 执行后验证：第一行应为 7；随后 admin、company-admin 各 7；第三个查询应为 0 行。
SELECT COUNT(*) AS enabled_resource_count FROM sys_resource WHERE resource_code IN
('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
AND status=1 AND is_deleted=0;
SELECT role_row.role_code,COUNT(*) AS granted_resource_count FROM sys_role role_row
JOIN sys_role_resource rr ON rr.role_id=role_row.id JOIN sys_resource res ON res.id=rr.resource_id
WHERE role_row.role_code IN ('admin','company-admin') AND res.resource_code IN
('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
GROUP BY role_row.role_code;
SELECT role_row.role_code,res.resource_code FROM sys_role_resource rr JOIN sys_role role_row ON role_row.id=rr.role_id
JOIN sys_resource res ON res.id=rr.resource_id WHERE res.resource_code IN
('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
AND role_row.role_code NOT IN ('admin','company-admin');
-- 人工回滚：先确认七个资源未被其他功能复用；仅删除对应 sys_role_resource 关系并逻辑删除七个新资源，绝不修改 Backup 父资源。

