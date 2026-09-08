-- 订单虚拟单号列表配置迁移
-- 仅追加 publicOrderCode，保留原 orderCode/orderNo。

UPDATE sys_config
SET config_value = JSON_SET(
    config_value,
    '$.columns',
    JSON_ARRAY_APPEND(
        JSON_EXTRACT(config_value, '$.columns'),
        '$',
        JSON_OBJECT('field', 'publicOrderCode', 'label', '虚拟单号', 'visible', TRUE,
                    'sort', JSON_LENGTH(JSON_EXTRACT(config_value, '$.columns')) + 1,
                    'width', 160, 'fixed', NULL)
    )
)
WHERE config_key IN (
    'order.column.config',
    'design.column.config',
    'production.column.config',
    'quality.column.config',
    'warehouse.column.config'
)
AND JSON_SEARCH(config_value, 'one', 'publicOrderCode', NULL, '$.columns[*].field') IS NULL;

-- 个人配置兼容策略由应用层处理：不删除用户已有 orderCode/orderNo 配置。
