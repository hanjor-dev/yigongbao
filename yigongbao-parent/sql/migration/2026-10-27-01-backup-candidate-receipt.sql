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
