-- ============================================================
-- 生产数据库升级：图纸按“数据包 + 产品分类”拆分
-- 适用基线：2026-08-04 从生产环境恢复的 yigongbao 数据库
-- 目标版本：当前代码与 sql/ddl.sql
-- MySQL：8.0+
--
-- 说明：
-- 1. 本脚本不指定 USE 数据库，执行前必须先选中目标 schema。
-- 2. MySQL DDL 会隐式提交，不能依赖事务整体回滚。
-- 3. 所有结构操作均通过 information_schema 判断，可重复执行。
-- 4. 历史 NULL 分类代表旧版“整包混合图纸”，不得强制回填。
-- ============================================================

SET NAMES utf8mb4;

SELECT DATABASE() AS migration_schema,
       VERSION() AS mysql_version,
       NOW() AS migration_start_time;

-- 目标库指纹校验。兼容同结构的隔离验证库，但拒绝空 schema 或明显错误的数据库。
SET @baseline_table_count = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN ('design_drawing', 'design_product', 'sys_dict')
      AND table_type = 'BASE TABLE'
);
SET @baseline_column_count = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND (
          (table_name = 'design_drawing' AND column_name = 'qr_file_id')
          OR (table_name = 'design_product' AND column_name = 'product_id')
          OR (table_name = 'sys_dict' AND column_name = 'dict_code')
      )
);
SET @assertion_sql = IF(
    DATABASE() IS NOT NULL
        AND @baseline_table_count = 3
        AND @baseline_column_count = 3,
    'SELECT ''target schema fingerprint verified'' AS migration_info',
    'SELECT * FROM __MIGRATION_ABORT_INVALID_TARGET_SCHEMA__'
);
PREPARE migration_assertion FROM @assertion_sql;
EXECUTE migration_assertion;
DEALLOCATE PREPARE migration_assertion;

-- 动态记录迁移前总行数，不把备份时点的 12/16 硬编码为长期生产约束。
SET @drawing_rows_before = (SELECT COUNT(*) FROM design_drawing);
SET @product_rows_before = (SELECT COUNT(*) FROM design_product);

-- ============================================================
-- 一、DDL
-- ============================================================

-- 1. 图纸增加产品分类。同一数据包可按产品分类分别生成图纸。
SET @ddl = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN
            'ALTER TABLE design_drawing ADD COLUMN product_category VARCHAR(32) DEFAULT NULL COMMENT ''产品分类字典码；同包按分类分别出图'' AFTER package_id'
        WHEN SUM(data_type = 'varchar'
                 AND character_maximum_length = 32
                 AND is_nullable = 'YES'
                 AND column_default IS NULL) = 1 THEN
            'SELECT ''design_drawing.product_category already exists and is compatible'' AS migration_info'
        ELSE
            'SELECT * FROM __MIGRATION_ABORT_BAD_DRAWING_CATEGORY_COLUMN__'
        END
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'design_drawing'
      AND column_name = 'product_category'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- 2. 打印产品增加分类快照。只写入迁移后的新数据或重新保存的数据。
SET @ddl = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN
            'ALTER TABLE design_product ADD COLUMN product_category VARCHAR(32) DEFAULT NULL COMMENT ''打印时产品分类快照'' AFTER product_name'
        WHEN SUM(data_type = 'varchar'
                 AND character_maximum_length = 32
                 AND is_nullable = 'YES'
                 AND column_default IS NULL) = 1 THEN
            'SELECT ''design_product.product_category already exists and is compatible'' AS migration_info'
        ELSE
            'SELECT * FROM __MIGRATION_ABORT_BAD_PRODUCT_CATEGORY_COLUMN__'
        END
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'design_product'
      AND column_name = 'product_category'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN
            'ALTER TABLE design_product ADD COLUMN product_category_name VARCHAR(64) DEFAULT NULL COMMENT ''打印时产品分类名称快照'' AFTER product_category'
        WHEN SUM(data_type = 'varchar'
                 AND character_maximum_length = 64
                 AND is_nullable = 'YES'
                 AND column_default IS NULL) = 1 THEN
            'SELECT ''design_product.product_category_name already exists and is compatible'' AS migration_info'
        ELSE
            'SELECT * FROM __MIGRATION_ABORT_BAD_CATEGORY_NAME_COLUMN__'
        END
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'design_product'
      AND column_name = 'product_category_name'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- NULL 使用保留哨兵参与唯一性，保证旧版图纸本身仍保持“数据包 + 版本”唯一。
-- '__LEGACY_NULL__' 是索引内部保留值，业务产品分类不得使用该值。
SELECT package_id,
       COALESCE(product_category, '__LEGACY_NULL__') AS indexed_product_category,
       version_seq,
       COUNT(*) AS duplicate_count
FROM design_drawing
WHERE is_deleted = 0
GROUP BY package_id, COALESCE(product_category, '__LEGACY_NULL__'), version_seq
HAVING COUNT(*) > 1;

SET @drawing_version_duplicate_count = (
    SELECT COUNT(*)
    FROM (
        SELECT 1
        FROM design_drawing
        WHERE is_deleted = 0
        GROUP BY package_id, COALESCE(product_category, '__LEGACY_NULL__'), version_seq
        HAVING COUNT(*) > 1
    ) duplicate_versions
);
SET @reserved_category_count = (
    SELECT COUNT(*)
    FROM design_drawing
    WHERE is_deleted = 0
      AND product_category = '__LEGACY_NULL__'
);
SET @assertion_sql = IF(
    @drawing_version_duplicate_count = 0 AND @reserved_category_count = 0,
    'SELECT ''drawing version uniqueness precheck passed'' AS migration_info',
    'SELECT * FROM __MIGRATION_ABORT_DUPLICATE_DRAWING_VERSION__'
);
PREPARE migration_assertion FROM @assertion_sql;
EXECUTE migration_assertion;
DEALLOCATE PREPARE migration_assertion;

-- 3. 先创建新唯一索引，再删除旧索引，避免迁移过程中失去版本唯一性保护。
-- NULL 历史记录使用内部哨兵，仅与其他历史 NULL 记录保持版本唯一，不与真实分类冲突。
SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE UNIQUE INDEX uk_design_drawing_pkg_cat_ver ON design_drawing ((CASE WHEN is_deleted = 0 THEN package_id ELSE NULL END), (CASE WHEN is_deleted = 0 THEN COALESCE(product_category, ''__LEGACY_NULL__'') ELSE NULL END), (CASE WHEN is_deleted = 0 THEN version_seq ELSE NULL END))',
        'SELECT ''uk_design_drawing_pkg_cat_ver exists; validating definition'' AS migration_info'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'design_drawing'
      AND index_name = 'uk_design_drawing_pkg_cat_ver'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- 只有完整、唯一且三段表达式均正确的新索引才能替换旧索引。
SET @replacement_index_valid = (
    SELECT COUNT(*) = 3
       AND SUM(non_unique = 0) = 3
       AND SUM(seq_in_index = 1
               AND LOCATE('is_deleted', expression) > 0
               AND LOCATE('package_id', expression) > 0) = 1
       AND SUM(seq_in_index = 2
               AND LOCATE('is_deleted', expression) > 0
               AND LOCATE('coalesce', LOWER(expression)) > 0
               AND LOCATE('product_category', expression) > 0
               AND LOCATE('__LEGACY_NULL__', expression) > 0) = 1
       AND SUM(seq_in_index = 3
               AND LOCATE('is_deleted', expression) > 0
               AND LOCATE('version_seq', expression) > 0) = 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'design_drawing'
      AND index_name = 'uk_design_drawing_pkg_cat_ver'
);
SET @assertion_sql = IF(
    @replacement_index_valid = 1,
    'SELECT ''replacement index definition verified'' AS migration_info',
    'SELECT * FROM __MIGRATION_ABORT_INVALID_REPLACEMENT_INDEX__'
);
PREPARE migration_assertion FROM @assertion_sql;
EXECUTE migration_assertion;
DEALLOCATE PREPARE migration_assertion;

SET @ddl = (
    SELECT IF(
        COUNT(*) > 0,
        'DROP INDEX uk_design_drawing_pkg_ver ON design_drawing',
        'SELECT ''uk_design_drawing_pkg_ver already absent'' AS migration_info'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'design_drawing'
      AND index_name = 'uk_design_drawing_pkg_ver'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- ============================================================
-- 二、初始化数据
-- ============================================================
-- 无新增初始化数据。
-- 生产备份中已存在 sys_dict.dict_code = '10.21'（图纸二维码图片），无需重复插入。
SELECT COUNT(*) AS active_drawing_qr_dict_count
FROM sys_dict
WHERE dict_code = '10.21'
  AND is_deleted = 0
  AND status = 1;

SET @drawing_qr_dict_count = (
    SELECT COUNT(*)
    FROM sys_dict
    WHERE dict_code = '10.21'
      AND is_deleted = 0
      AND status = 1
);
SET @assertion_sql = IF(
    @drawing_qr_dict_count = 1,
    'SELECT ''drawing QR dictionary verified'' AS migration_info',
    'SELECT * FROM __MIGRATION_ABORT_DRAWING_QR_DICT_INVALID__'
);
PREPARE migration_assertion FROM @assertion_sql;
EXECUTE migration_assertion;
DEALLOCATE PREPARE migration_assertion;

-- ============================================================
-- 三、历史数据迁移
-- ============================================================
-- 不执行 UPDATE/INSERT。
--
-- 原因：生产备份中存在同时包含 17.1（模型类）和 17.2（导板类）的数据包，
-- 但每个数据包只有一份旧版混合图纸。强制回填会造成历史图纸归属错误，
-- 或使旧工单被误判为缺少分类图纸。新增字段保持 NULL，作为历史兼容标识。
-- 后续打印信息重新保存时，新代码会写入分类快照并按分类生成图纸。

-- ============================================================
-- 四、执行后校验
-- ============================================================

SET @drawing_rows_after = (SELECT COUNT(*) FROM design_drawing);
SET @product_rows_after = (SELECT COUNT(*) FROM design_product);
SET @assertion_sql = IF(
    @drawing_rows_before = @drawing_rows_after
        AND @product_rows_before = @product_rows_after,
    'SELECT ''row preservation verified'' AS migration_info',
    'SELECT * FROM __MIGRATION_ABORT_ROW_COUNT_CHANGED__'
);
PREPARE migration_assertion FROM @assertion_sql;
EXECUTE migration_assertion;
DEALLOCATE PREPARE migration_assertion;

-- 应返回 3 行，字段类型/长度与 sql/ddl.sql 一致。
SELECT table_name,
       column_name,
       column_type,
       is_nullable,
       column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
      (table_name = 'design_drawing' AND column_name = 'product_category')
      OR
      (table_name = 'design_product' AND column_name IN ('product_category', 'product_category_name'))
  )
ORDER BY table_name, ordinal_position;

-- 新索引应返回 3 行，旧索引应返回 0 行。
SELECT index_name,
       non_unique,
       seq_in_index,
       column_name,
       expression
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'design_drawing'
  AND index_name IN ('uk_design_drawing_pkg_cat_ver', 'uk_design_drawing_pkg_ver')
ORDER BY index_name, seq_in_index;

-- 仅用于审计历史兼容数据规模；不应在本脚本中将这些 NULL 强制回填。
SELECT 'design_drawing.active_total' AS metric,
       COUNT(*) AS metric_value
FROM design_drawing
WHERE is_deleted = 0
UNION ALL
SELECT 'design_drawing.legacy_category_null',
       COUNT(*)
FROM design_drawing
WHERE is_deleted = 0
  AND product_category IS NULL
UNION ALL
SELECT 'design_product.active_total',
       COUNT(*)
FROM design_product
WHERE is_deleted = 0
UNION ALL
SELECT 'design_product.legacy_category_null',
       COUNT(*)
FROM design_product
WHERE is_deleted = 0
  AND product_category IS NULL;

SELECT NOW() AS migration_finish_time;

-- ============================================================
-- 五、质检/仓储列表列配置
-- ============================================================
-- 目标：沿用订单、设计、生产列表的“系统默认 + 用户个人覆盖”模式。
-- 数据策略：新增用户字段保持 NULL，不复制历史用户配置；用户首次读取时使用 sys_config 默认值。
-- 本段可重复执行；不直接更新既有用户业务数据。

-- 5.1 用户个人配置字段：质检
SET @ddl_quality_user_column = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN quality_column_settings TEXT NULL COMMENT ''质检列表列配置（JSON，用户个人自定义列显示设置）'' AFTER production_column_settings',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND column_name = 'quality_column_settings'
);
PREPARE stmt_quality_user_column FROM @ddl_quality_user_column;
EXECUTE stmt_quality_user_column;
DEALLOCATE PREPARE stmt_quality_user_column;

-- 5.2 用户个人配置字段：仓储
SET @ddl_warehouse_user_column = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN warehouse_column_settings TEXT NULL COMMENT ''仓储列表列配置（JSON，用户个人自定义列显示设置）'' AFTER quality_column_settings',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND column_name = 'warehouse_column_settings'
);
PREPARE stmt_warehouse_user_column FROM @ddl_warehouse_user_column;
EXECUTE stmt_warehouse_user_column;
DEALLOCATE PREPARE stmt_warehouse_user_column;

-- 5.3 系统默认配置：质检
-- 若历史上已存在同 key 的禁用/软删除记录，恢复为启用；正常记录保留既有配置值。
UPDATE sys_config
SET config_name = '质检列表默认列配置',
    config_value = IF(is_deleted = 1 OR status = 0 OR config_value IS NULL OR TRIM(config_value) = '',
                      '{"module":"quality","columns":[{"field":"recordNo","label":"流转卡编号","visible":true,"sort":1,"width":160,"fixed":null},{"field":"designPackageCode","label":"数据包编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"productionBatchNo","label":"生产批号","visible":true,"sort":3,"width":140,"fixed":null},{"field":"orderCode","label":"订单号","visible":true,"sort":4,"width":130,"fixed":null},{"field":"hospitalName","label":"医院名称","visible":true,"sort":5,"width":130,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":6,"width":110,"fixed":null},{"field":"doctorName","label":"医生","visible":true,"sort":7,"width":90,"fixed":null},{"field":"patientName","label":"患者","visible":true,"sort":8,"width":90,"fixed":null},{"field":"isUrgent","label":"加急","visible":true,"sort":9,"width":65,"fixed":null},{"field":"isPostal","label":"邮寄","visible":true,"sort":10,"width":65,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":11,"width":110,"fixed":null},{"field":"orgName","label":"机构","visible":true,"sort":12,"width":110,"fixed":null},{"field":"totalProductCount","label":"产品数","visible":true,"sort":13,"width":70,"fixed":null},{"field":"qualifiedCount","label":"合格","visible":true,"sort":14,"width":60,"fixed":null},{"field":"unqualifiedCount","label":"不合格","visible":true,"sort":15,"width":70,"fixed":null},{"field":"pendingCount","label":"待检","visible":true,"sort":16,"width":60,"fixed":null},{"field":"status","label":"状态","visible":true,"sort":17,"width":100,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":18,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":19,"width":170,"fixed":"right"}]}',
                      config_value),
    config_type = 'json',
    config_group = 'system',
    config_desc = '质检列表默认显示的列（JSON格式）',
    is_system = 1,
    is_public = 0,
    sort = 15,
    status = 1,
    is_deleted = 0
WHERE config_key = 'quality.column.config';

INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
SELECT 'quality.column.config',
       '质检列表默认列配置',
       '{"module":"quality","columns":[{"field":"recordNo","label":"流转卡编号","visible":true,"sort":1,"width":160,"fixed":null},{"field":"designPackageCode","label":"数据包编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"productionBatchNo","label":"生产批号","visible":true,"sort":3,"width":140,"fixed":null},{"field":"orderCode","label":"订单号","visible":true,"sort":4,"width":130,"fixed":null},{"field":"hospitalName","label":"医院名称","visible":true,"sort":5,"width":130,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":6,"width":110,"fixed":null},{"field":"doctorName","label":"医生","visible":true,"sort":7,"width":90,"fixed":null},{"field":"patientName","label":"患者","visible":true,"sort":8,"width":90,"fixed":null},{"field":"isUrgent","label":"加急","visible":true,"sort":9,"width":65,"fixed":null},{"field":"isPostal","label":"邮寄","visible":true,"sort":10,"width":65,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":11,"width":110,"fixed":null},{"field":"orgName","label":"机构","visible":true,"sort":12,"width":110,"fixed":null},{"field":"totalProductCount","label":"产品数","visible":true,"sort":13,"width":70,"fixed":null},{"field":"qualifiedCount","label":"合格","visible":true,"sort":14,"width":60,"fixed":null},{"field":"unqualifiedCount","label":"不合格","visible":true,"sort":15,"width":70,"fixed":null},{"field":"pendingCount","label":"待检","visible":true,"sort":16,"width":60,"fixed":null},{"field":"status","label":"状态","visible":true,"sort":17,"width":100,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":18,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":19,"width":170,"fixed":"right"}]}',
       'json', 'system', '质检列表默认显示的列（JSON格式）', 1, 0, 15, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config
    WHERE config_key = 'quality.column.config'
      AND is_deleted = 0
);

-- 5.4 系统默认配置：仓储
UPDATE sys_config
SET config_name = '仓储列表默认列配置',
    config_value = IF(is_deleted = 1 OR status = 0 OR config_value IS NULL OR TRIM(config_value) = '',
                      '{"module":"warehouse","columns":[{"field":"recordNo","label":"流转卡编号","visible":true,"sort":1,"width":160,"fixed":null},{"field":"designPackageCode","label":"数据包编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"status","label":"状态","visible":true,"sort":3,"width":90,"fixed":null},{"field":"productionBatchNo","label":"生产批号","visible":true,"sort":4,"width":140,"fixed":null},{"field":"orderNo","label":"订单号","visible":true,"sort":5,"width":130,"fixed":null},{"field":"hospitalName","label":"医院名称","visible":true,"sort":6,"width":130,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":7,"width":110,"fixed":null},{"field":"doctorName","label":"医生","visible":true,"sort":8,"width":90,"fixed":null},{"field":"patientName","label":"患者","visible":true,"sort":9,"width":90,"fixed":null},{"field":"isUrgent","label":"加急","visible":true,"sort":10,"width":65,"fixed":null},{"field":"isPostal","label":"邮寄","visible":true,"sort":11,"width":65,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":12,"width":110,"fixed":null},{"field":"totalCount","label":"产品总数","visible":true,"sort":13,"width":80,"fixed":null},{"field":"warehouseCountSummary","label":"待入/已入/已出","visible":true,"sort":14,"width":130,"fixed":null},{"field":"earliestInTime","label":"最早入库","visible":true,"sort":15,"width":160,"fixed":null},{"field":"latestOutTime","label":"最晚出库","visible":true,"sort":16,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":17,"width":140,"fixed":"right"}]}',
                      config_value),
    config_type = 'json',
    config_group = 'system',
    config_desc = '仓储列表默认显示的列（JSON格式）',
    is_system = 1,
    is_public = 0,
    sort = 16,
    status = 1,
    is_deleted = 0
WHERE config_key = 'warehouse.column.config';

INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
SELECT 'warehouse.column.config',
       '仓储列表默认列配置',
       '{"module":"warehouse","columns":[{"field":"recordNo","label":"流转卡编号","visible":true,"sort":1,"width":160,"fixed":null},{"field":"designPackageCode","label":"数据包编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"status","label":"状态","visible":true,"sort":3,"width":90,"fixed":null},{"field":"productionBatchNo","label":"生产批号","visible":true,"sort":4,"width":140,"fixed":null},{"field":"orderNo","label":"订单号","visible":true,"sort":5,"width":130,"fixed":null},{"field":"hospitalName","label":"医院名称","visible":true,"sort":6,"width":130,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":7,"width":110,"fixed":null},{"field":"doctorName","label":"医生","visible":true,"sort":8,"width":90,"fixed":null},{"field":"patientName","label":"患者","visible":true,"sort":9,"width":90,"fixed":null},{"field":"isUrgent","label":"加急","visible":true,"sort":10,"width":65,"fixed":null},{"field":"isPostal","label":"邮寄","visible":true,"sort":11,"width":65,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":12,"width":110,"fixed":null},{"field":"totalCount","label":"产品总数","visible":true,"sort":13,"width":80,"fixed":null},{"field":"warehouseCountSummary","label":"待入/已入/已出","visible":true,"sort":14,"width":130,"fixed":null},{"field":"earliestInTime","label":"最早入库","visible":true,"sort":15,"width":160,"fixed":null},{"field":"latestOutTime","label":"最晚出库","visible":true,"sort":16,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":17,"width":140,"fixed":"right"}]}',
       'json', 'system', '仓储列表默认显示的列（JSON格式）', 1, 0, 16, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config
    WHERE config_key = 'warehouse.column.config'
      AND is_deleted = 0
);

-- 5.5 迁移结果核对：应返回 2 个用户字段和 2 个启用配置。
SELECT table_name,
       column_name,
       column_type,
       is_nullable,
       column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_user'
  AND column_name IN ('quality_column_settings', 'warehouse_column_settings')
ORDER BY ordinal_position;

SELECT config_key,
       config_name,
       config_type,
       config_group,
       status,
       is_deleted
FROM sys_config
WHERE config_key IN ('quality.column.config', 'warehouse.column.config')
ORDER BY config_key;

-- 新增用户字段应保持 NULL；仅审计当前已保存的个人配置数量，不做历史回填。
SELECT 'sys_user.quality_column_settings_saved' AS metric,
       COUNT(*) AS metric_value
FROM sys_user
WHERE is_deleted = 0
  AND quality_column_settings IS NOT NULL
UNION ALL
SELECT 'sys_user.warehouse_column_settings_saved',
       COUNT(*)
FROM sys_user
WHERE is_deleted = 0
  AND warehouse_column_settings IS NOT NULL;
