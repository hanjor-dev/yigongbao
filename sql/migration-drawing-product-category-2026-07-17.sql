-- 图纸按数据包+产品分类拆分
-- 旧数据 product_category 保持 NULL，作为历史混合图纸兼容记录；新逻辑只生成带分类图纸。
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE design_drawing ADD COLUMN product_category VARCHAR(32) DEFAULT NULL COMMENT ''产品分类字典码；同包按分类分别出图'' AFTER package_id',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'design_drawing' AND column_name = 'product_category');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE design_product ADD COLUMN product_category VARCHAR(32) DEFAULT NULL COMMENT ''打印时产品分类快照'' AFTER product_name',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'design_product' AND column_name = 'product_category');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE design_product ADD COLUMN product_category_name VARCHAR(64) DEFAULT NULL COMMENT ''打印时产品分类名称快照'' AFTER product_category',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'design_product' AND column_name = 'product_category_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) > 0,
    'DROP INDEX uk_design_drawing_pkg_ver ON design_drawing',
    'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'design_drawing' AND index_name = 'uk_design_drawing_pkg_ver');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'CREATE UNIQUE INDEX uk_design_drawing_pkg_cat_ver ON design_drawing ((CASE WHEN is_deleted = 0 THEN package_id ELSE NULL END), (CASE WHEN is_deleted = 0 THEN product_category ELSE NULL END), (CASE WHEN is_deleted = 0 THEN version_seq ELSE NULL END))',
    'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'design_drawing' AND index_name = 'uk_design_drawing_pkg_cat_ver');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
