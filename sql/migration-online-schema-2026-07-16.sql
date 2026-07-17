-- ============================================================
-- Online schema migration: ddl-prod.sql -> ddl.sql + alter scripts
-- Generated: 2026-07-16
-- Scope: schema changes only. Run data migration script afterwards.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELIMITER $$

DROP PROCEDURE IF EXISTS ygb_add_column_if_missing $$
CREATE PROCEDURE ygb_add_column_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_column_name VARCHAR(128),
    IN p_alter_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @ygb_sql = p_alter_sql;
        PREPARE ygb_stmt FROM @ygb_sql;
        EXECUTE ygb_stmt;
        DEALLOCATE PREPARE ygb_stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS ygb_add_index_if_missing $$
CREATE PROCEDURE ygb_add_index_if_missing(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_alter_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ygb_sql = p_alter_sql;
        PREPARE ygb_stmt FROM @ygb_sql;
        EXECUTE ygb_stmt;
        DEALLOCATE PREPARE ygb_stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS ygb_drop_index_if_exists $$
CREATE PROCEDURE ygb_drop_index_if_exists(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ygb_sql = CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_index_name, '`');
        PREPARE ygb_stmt FROM @ygb_sql;
        EXECUTE ygb_stmt;
        DEALLOCATE PREPARE ygb_stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS ygb_assert_no_duplicate_package_product $$
CREATE PROCEDURE ygb_assert_no_duplicate_package_product()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT design_package_id, product_id
            FROM production_record
            WHERE product_id IS NOT NULL
            GROUP BY design_package_id, product_id
            HAVING COUNT(*) > 1
        ) duplicated_package_product
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate production_record(design_package_id, product_id) exists; clean data before creating uk_package_product';
    END IF;
END $$

DELIMITER ;

SELECT '1. sys_user: replace unique phone index with normal index' AS step;
CALL ygb_drop_index_if_exists('sys_user', 'uk_phone');
CALL ygb_add_index_if_missing(
    'sys_user',
    'idx_user_phone',
    'CREATE INDEX idx_user_phone ON sys_user(phone)'
);

SELECT '2. order_main: add pending cancel apply marker' AS step;
CALL ygb_add_column_if_missing(
    'order_main',
    'has_pending_cancel_apply',
    'ALTER TABLE order_main ADD COLUMN has_pending_cancel_apply TINYINT DEFAULT 0 COMMENT ''是否有待审核的取消申请（0=否，1=是）'' AFTER version'
);
CALL ygb_add_index_if_missing(
    'order_main',
    'idx_order_main_has_pending_cancel_apply',
    'CREATE INDEX idx_order_main_has_pending_cancel_apply ON order_main(has_pending_cancel_apply)'
);

SELECT '3. device_daily_usage_counter: create table and BaseEntity columns' AS step;
CREATE TABLE IF NOT EXISTS device_daily_usage_counter (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    device_id       BIGINT NOT NULL COMMENT '设备ID（关联device表）',
    usage_date      DATE NOT NULL COMMENT '使用日期',
    usage_count     INT NOT NULL DEFAULT 0 COMMENT '当日上机次数',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT COMMENT '创建人ID',
    update_by       BIGINT COMMENT '更新人ID',
    is_deleted      TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    UNIQUE KEY uk_device_date (device_id, usage_date),
    KEY idx_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备每日上机次数统计表';

CALL ygb_add_column_if_missing(
    'device_daily_usage_counter',
    'create_by',
    'ALTER TABLE device_daily_usage_counter ADD COLUMN create_by BIGINT COMMENT ''创建人ID'' AFTER update_time'
);
CALL ygb_add_column_if_missing(
    'device_daily_usage_counter',
    'update_by',
    'ALTER TABLE device_daily_usage_counter ADD COLUMN update_by BIGINT COMMENT ''更新人ID'' AFTER create_by'
);
CALL ygb_add_column_if_missing(
    'device_daily_usage_counter',
    'is_deleted',
    'ALTER TABLE device_daily_usage_counter ADD COLUMN is_deleted TINYINT DEFAULT 0 COMMENT ''是否删除（0=否，1=是）'' AFTER update_by'
);
CALL ygb_add_index_if_missing(
    'device_daily_usage_counter',
    'uk_device_date',
    'CREATE UNIQUE INDEX uk_device_date ON device_daily_usage_counter(device_id, usage_date)'
);
CALL ygb_add_index_if_missing(
    'device_daily_usage_counter',
    'idx_usage_date',
    'CREATE INDEX idx_usage_date ON device_daily_usage_counter(usage_date)'
);

SELECT '4. order_cancel_apply: create table' AS step;
CREATE TABLE IF NOT EXISTS order_cancel_apply (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id            BIGINT NOT NULL COMMENT '订单ID',
    apply_by            BIGINT NOT NULL COMMENT '申请人ID',
    apply_reason        VARCHAR(500) COMMENT '取消原因（选填）',
    audit_status        TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：1=待审核，2=已通过，3=已驳回',
    audit_by            BIGINT COMMENT '审核人ID',
    audit_reason        VARCHAR(500) COMMENT '审核驳回原因（选填）',
    audit_time          DATETIME COMMENT '审核时间',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT COMMENT '创建人ID',
    update_by           BIGINT COMMENT '更新人ID',
    is_deleted          TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_order_cancel_apply_order_id (order_id),
    KEY idx_order_cancel_apply_audit_status (audit_status),
    KEY idx_order_cancel_apply_apply_by (apply_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单取消申请表';

CALL ygb_add_index_if_missing(
    'order_cancel_apply',
    'idx_order_cancel_apply_order_id',
    'CREATE INDEX idx_order_cancel_apply_order_id ON order_cancel_apply(order_id)'
);
CALL ygb_add_index_if_missing(
    'order_cancel_apply',
    'idx_order_cancel_apply_audit_status',
    'CREATE INDEX idx_order_cancel_apply_audit_status ON order_cancel_apply(audit_status)'
);
CALL ygb_add_index_if_missing(
    'order_cancel_apply',
    'idx_order_cancel_apply_apply_by',
    'CREATE INDEX idx_order_cancel_apply_apply_by ON order_cancel_apply(apply_by)'
);

SELECT '5. production_record: add product split and packaging/category fields' AS step;
CALL ygb_add_column_if_missing(
    'production_record',
    'product_id',
    'ALTER TABLE production_record ADD COLUMN product_id BIGINT COMMENT ''产品ID'' AFTER design_package_code'
);
CALL ygb_add_column_if_missing(
    'production_record',
    'product_name',
    'ALTER TABLE production_record ADD COLUMN product_name VARCHAR(100) COMMENT ''产品名称（冗余）'' AFTER product_id'
);
CALL ygb_add_column_if_missing(
    'production_record',
    'product_category',
    'ALTER TABLE production_record ADD COLUMN product_category VARCHAR(50) COMMENT ''产品大类代码（如17.1，冗余自product.category）'' AFTER product_name'
);
CALL ygb_add_column_if_missing(
    'production_record',
    'product_category_name',
    'ALTER TABLE production_record ADD COLUMN product_category_name VARCHAR(100) COMMENT ''产品大类名称（如"模型"、"导板"，冗余自product.category_name）'' AFTER product_category'
);
CALL ygb_add_column_if_missing(
    'production_record',
    'pack_material',
    'ALTER TABLE production_record ADD COLUMN pack_material VARCHAR(100) COMMENT ''包装材质（如：纸封袋、PE符合食品包装袋）'' AFTER pack_seal_time'
);

SELECT '6. production_record: add indexes' AS step;
SELECT
    design_package_id,
    product_id,
    COUNT(*) AS duplicate_count
FROM production_record
WHERE product_id IS NOT NULL
GROUP BY design_package_id, product_id
HAVING COUNT(*) > 1;

CALL ygb_assert_no_duplicate_package_product();
CALL ygb_drop_index_if_exists('production_record', 'uk_package_product');
CALL ygb_add_index_if_missing(
    'production_record',
    'uk_package_product',
    'ALTER TABLE production_record ADD UNIQUE KEY uk_package_product (design_package_id, product_id)'
);
CALL ygb_add_index_if_missing(
    'production_record',
    'idx_production_record_category',
    'CREATE INDEX idx_production_record_category ON production_record(product_category)'
);

SELECT '7. production_product: allow delayed product_no generation' AS step;
ALTER TABLE production_product
    MODIFY COLUMN product_no VARCHAR(50) NULL COMMENT '产品编号（分配设备时生成）';

DROP PROCEDURE IF EXISTS ygb_add_column_if_missing;
DROP PROCEDURE IF EXISTS ygb_add_index_if_missing;
DROP PROCEDURE IF EXISTS ygb_drop_index_if_exists;
DROP PROCEDURE IF EXISTS ygb_assert_no_duplicate_package_product;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'schema migration complete' AS status, NOW() AS finished_at;
