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
