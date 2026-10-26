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
