-- 订单业务页面统一使用虚拟单号展示。
-- 订单流水号 order_code 仍保留为内部关联键；本迁移只调整 sys_config 的列表列配置。
-- 执行前提：订单虚拟单号字段已完成历史数据回填。

UPDATE sys_config
SET config_value = JSON_REMOVE(
    config_value,
    REPLACE(JSON_UNQUOTE(JSON_SEARCH(config_value, 'one', 'publicOrderCode', NULL, '$.columns[*].field')), '.field', '')
), update_time = NOW()
WHERE config_key IN ('order.column.config', 'design.column.config', 'production.column.config',
                     'quality.column.config', 'warehouse.column.config')
  AND JSON_SEARCH(config_value, 'one', 'publicOrderCode', NULL, '$.columns[*].field') IS NOT NULL;

UPDATE sys_config
SET config_value = JSON_SET(
    config_value,
    JSON_UNQUOTE(JSON_SEARCH(config_value, 'one', 'orderCode', NULL, '$.columns[*].field')),
    'publicOrderCode'
), update_time = NOW()
WHERE config_key IN ('order.column.config', 'design.column.config', 'production.column.config',
                     'quality.column.config', 'warehouse.column.config')
  AND JSON_SEARCH(config_value, 'one', 'orderCode', NULL, '$.columns[*].field') IS NOT NULL;

-- 上面的通用更新会在 JSON_SEARCH 找不到旧列时保持原值；统一修正标题。
UPDATE sys_config
SET config_value = JSON_SET(config_value,
    REPLACE(JSON_UNQUOTE(JSON_SEARCH(config_value, 'one', 'publicOrderCode', NULL, '$.columns[*].field')), '.field', '.label'),
    '订单号'
), update_time = NOW()
WHERE config_key IN ('order.column.config', 'design.column.config', 'production.column.config',
                     'quality.column.config', 'warehouse.column.config')
  AND JSON_SEARCH(config_value, 'one', 'publicOrderCode', NULL, '$.columns[*].field') IS NOT NULL;
