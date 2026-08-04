-- ============================================================
-- production_product 新增产品重量字段
-- 单位：克；允许 NULL；最多 2 位小数
-- ============================================================

SET NAMES utf8mb4;

SET @weight_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'production_product'
      AND column_name = 'weight'
);

SET @weight_ddl = IF(
    @weight_column_exists = 0,
    'ALTER TABLE production_product ADD COLUMN weight DECIMAL(10,2) NULL COMMENT ''产品重量，单位：克'' AFTER file_name',
    'SELECT ''production_product.weight already exists'' AS migration_info'
);

PREPARE weight_stmt FROM @weight_ddl;
EXECUTE weight_stmt;
DEALLOCATE PREPARE weight_stmt;

SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'production_product'
  AND column_name = 'weight';

SELECT
    CASE
        WHEN COUNT(*) = 1
         AND MAX(column_type) = 'decimal(10,2)'
         AND MAX(is_nullable) = 'YES'
         AND MAX(column_comment) = '产品重量，单位：克'
        THEN 'production_product.weight migration verified'
        ELSE 'production_product.weight migration verification failed'
    END AS migration_result
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'production_product'
  AND column_name = 'weight';
