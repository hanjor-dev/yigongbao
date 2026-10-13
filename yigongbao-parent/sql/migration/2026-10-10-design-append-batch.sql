-- 追加设计批次功能统一数据库变更脚本
--
-- 本脚本包含：
-- 1. 新增 design_package_batch 追加设计批次表；
-- 2. design_package、production_record 增加批次关联字段和索引；
-- 3. 补充 design_package_batch 所需的通用审计字段。
--
-- 说明：
-- - 本脚本只负责结构和权限初始化，不回填历史数据的 batch_id；历史数据保持 NULL，原有按 order_id/package_id 的查询不受影响。
-- - DDL 在 MySQL 中会产生隐式提交，因此不使用一个覆盖全部语句的事务；权限数据插入使用幂等条件。
-- - 执行前应在目标环境完成备份，并在变更窗口验证表结构和权限结果。

CREATE TABLE IF NOT EXISTS design_package_batch (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '追加设计批次编号',
    batch_type VARCHAR(32) NOT NULL DEFAULT 'ADDITIONAL' COMMENT '批次类型：NORMAL=正常设计，ADDITIONAL=追加设计',
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADING' COMMENT '批次状态：UPLOADING/PRINT_INFO_EDITING/COMPLETED/CANCELLED',
    source_order_status INT NULL COMMENT '创建批次时订单状态',
    created_by BIGINT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    completed_time DATETIME NULL COMMENT '完成时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT NULL COMMENT '创建人ID（通用审计字段）',
    update_by BIGINT NULL COMMENT '更新人ID（通用审计字段）',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_design_package_batch_no (batch_no),
    KEY idx_design_package_batch_order_status (order_id, status, is_deleted),
    KEY idx_design_package_batch_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计数据包追加批次';

SET @sql_add_design_package_batch_version := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package_batch'
          AND COLUMN_NAME = 'version'
    ),
    'SELECT 1',
    'ALTER TABLE design_package_batch ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'''
);
PREPARE stmt_add_design_package_batch_version FROM @sql_add_design_package_batch_version;
EXECUTE stmt_add_design_package_batch_version;
DEALLOCATE PREPARE stmt_add_design_package_batch_version;

SET @sql_add_design_package_batch_create_by := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package_batch'
          AND COLUMN_NAME = 'create_by'
    ),
    'SELECT 1',
    'ALTER TABLE design_package_batch ADD COLUMN create_by BIGINT NULL COMMENT ''创建人ID（通用审计字段）'''
);
PREPARE stmt_add_design_package_batch_create_by FROM @sql_add_design_package_batch_create_by;
EXECUTE stmt_add_design_package_batch_create_by;
DEALLOCATE PREPARE stmt_add_design_package_batch_create_by;

SET @sql_add_design_package_batch_update_by := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package_batch'
          AND COLUMN_NAME = 'update_by'
    ),
    'SELECT 1',
    'ALTER TABLE design_package_batch ADD COLUMN update_by BIGINT NULL COMMENT ''更新人ID（通用审计字段）'''
);
PREPARE stmt_add_design_package_batch_update_by FROM @sql_add_design_package_batch_update_by;
EXECUTE stmt_add_design_package_batch_update_by;
DEALLOCATE PREPARE stmt_add_design_package_batch_update_by;

-- 为原有设计数据包增加批次关联。历史数据保留 NULL，新增追加数据包写入对应 batch_id。
SET @sql_add_design_package_batch_id := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package'
          AND COLUMN_NAME = 'batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE design_package ADD COLUMN batch_id BIGINT NULL COMMENT ''追加设计批次ID'''
);
PREPARE stmt_add_design_package_batch_id FROM @sql_add_design_package_batch_id;
EXECUTE stmt_add_design_package_batch_id;
DEALLOCATE PREPARE stmt_add_design_package_batch_id;

SET @sql_add_design_package_batch_index := IF(
    EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package'
          AND INDEX_NAME = 'idx_design_package_batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE design_package ADD KEY idx_design_package_batch_id (batch_id)'
);
PREPARE stmt_add_design_package_batch_index FROM @sql_add_design_package_batch_index;
EXECUTE stmt_add_design_package_batch_index;
DEALLOCATE PREPARE stmt_add_design_package_batch_index;

-- 为生产流转卡增加批次关联。历史生产记录保留 NULL，追加批次新记录写入对应 batch_id。
SET @sql_add_production_record_batch_id := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'production_record'
          AND COLUMN_NAME = 'batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE production_record ADD COLUMN batch_id BIGINT NULL COMMENT ''追加设计批次ID'''
);
PREPARE stmt_add_production_record_batch_id FROM @sql_add_production_record_batch_id;
EXECUTE stmt_add_production_record_batch_id;
DEALLOCATE PREPARE stmt_add_production_record_batch_id;

SET @sql_add_production_record_batch_index := IF(
    EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'production_record'
          AND INDEX_NAME = 'idx_production_record_batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE production_record ADD KEY idx_production_record_batch_id (batch_id)'
);
PREPARE stmt_add_production_record_batch_index FROM @sql_add_production_record_batch_index;
EXECUTE stmt_add_production_record_batch_index;
DEALLOCATE PREPARE stmt_add_production_record_batch_index;

-- 发布后建议执行以下只读校验：
-- SELECT COUNT(*) FROM design_package_batch;
-- SHOW COLUMNS FROM design_package_batch;
