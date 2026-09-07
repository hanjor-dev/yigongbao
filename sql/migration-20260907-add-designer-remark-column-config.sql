-- 为订单和设计工单默认列配置增加设计师备注字段
-- 适用：MySQL 8.0+
-- 说明：仅补充缺失字段，重复执行不会重复追加。
SET NAMES utf8mb4;

START TRANSACTION;

UPDATE sys_config
SET config_value = JSON_SET(
        config_value,
        '$.columns',
        JSON_ARRAY_APPEND(
            JSON_EXTRACT(config_value, '$.columns'),
            '$',
            JSON_OBJECT(
                'field', 'designerRemark',
                'label', '设计师备注',
                'visible', TRUE,
                'sort', 21,
                'width', 200,
                'fixed', NULL
            )
        )
    ),
    update_time = CURRENT_TIMESTAMP
WHERE config_key = 'design.column.config'
  AND is_deleted = 0
  AND JSON_SEARCH(config_value, 'one', 'designerRemark', NULL, '$.columns[*].field') IS NULL;

UPDATE sys_config
SET config_value = JSON_SET(
        config_value,
        '$.columns',
        JSON_ARRAY_APPEND(
            JSON_EXTRACT(config_value, '$.columns'),
            '$',
            JSON_OBJECT(
                'field', 'designerRemark',
                'label', '设计师备注',
                'visible', TRUE,
                'sort', 34,
                'width', 200,
                'fixed', NULL
            )
        )
    ),
    update_time = CURRENT_TIMESTAMP
WHERE config_key = 'order.column.config'
  AND is_deleted = 0
  AND JSON_SEARCH(config_value, 'one', 'designerRemark', NULL, '$.columns[*].field') IS NULL;

COMMIT;

-- 核验结果
SELECT config_key, JSON_SEARCH(config_value, 'one', 'designerRemark', NULL, '$.columns[*].field') AS designer_remark_path
  FROM sys_config
 WHERE config_key IN ('design.column.config', 'order.column.config')
   AND is_deleted = 0;
