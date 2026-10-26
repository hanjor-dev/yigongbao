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
 identity_hash CHAR(64) NOT NULL COMMENT '完整对象身份哈希：platform、bucket、object_key、version_identity、relation_kind、relation_id',
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

-- 执行后验证：应返回 8；ACTUAL 功能开关仍须保持关闭。
SELECT COUNT(*) AS created_table_count FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN
('cos_object_reference','cos_cleanup_policy','cos_cleanup_snapshot','cos_cleanup_task','cos_cleanup_item','cos_cleanup_reservation','cos_cleanup_protection','cos_delete_permit');
-- 人工回滚：已有快照、任务、隔离或 permit 时禁止 DROP；仅关闭 ACTUAL 模式并保留全部安全证据。
