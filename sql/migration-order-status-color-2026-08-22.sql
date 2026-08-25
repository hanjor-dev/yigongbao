-- 订单及流转卡状态标签颜色配置
-- 适用于已存在 sys_config 的环境，重复执行不会插入重复配置。

UPDATE sys_config
SET config_value = '{"1010":{"bgColor":"#f3f4f6","bdColor":"#e5e7eb","color":"#6b7280"},"1020":{"bgColor":"#fef2f2","bdColor":"#fecaca","color":"#dc2626"},"1030":{"bgColor":"#fefce8","bdColor":"#fef08a","color":"#ca8a04"},"1040":{"bgColor":"#fff7ed","bdColor":"#fed7aa","color":"#ea580c"},"2010":{"bgColor":"#eff6ff","bdColor":"#bfdbfe","color":"#2563eb"},"2020":{"bgColor":"#ecfeff","bdColor":"#a5f3fc","color":"#0891b2"},"2030":{"bgColor":"#f0fdf4","bdColor":"#bbf7d0","color":"#16a34a"},"3010":{"bgColor":"#f5f3ff","bdColor":"#ddd6fe","color":"#7c3aed"},"3020":{"bgColor":"#faf5ff","bdColor":"#e9d5ff","color":"#9333ea"},"3030":{"bgColor":"#f0fdf4","bdColor":"#bbf7d0","color":"#15803d"},"3040":{"bgColor":"#fef2f2","bdColor":"#fecaca","color":"#b91c1c"},"4010":{"bgColor":"#faf5ff","bdColor":"#e9d5ff","color":"#7c3aed"},"5010":{"bgColor":"#fffbeb","bdColor":"#fde68a","color":"#d97706"},"5020":{"bgColor":"#f0fdf4","bdColor":"#86efac","color":"#15803d"},"5030":{"bgColor":"#fef2f2","bdColor":"#fca5a5","color":"#b91c1c"},"5040":{"bgColor":"#fef3c7","bdColor":"#fcd34d","color":"#b45309"},"5050":{"bgColor":"#f1f5f9","bdColor":"#cbd5e1","color":"#475569"},"6010":{"bgColor":"#e0e7ff","bdColor":"#a5b4fc","color":"#4f46e5"},"6020":{"bgColor":"#ccfbf1","bdColor":"#99f6e4","color":"#0d9488"},"6030":{"bgColor":"#e0f2fe","bdColor":"#7dd3fc","color":"#0284c7"},"8010":{"bgColor":"#dcfce7","bdColor":"#86efac","color":"#16a34a"},"9010":{"bgColor":"#f9fafb","bdColor":"#e5e7eb","color":"#9ca3af"}}'
WHERE config_key = 'order.status.color';

INSERT INTO sys_config (
    config_key, config_name, config_value, config_type, config_group,
    config_desc, is_system, is_public, sort, status
)
SELECT
    'order.status.color',
    '订单及流转卡状态标签颜色配置',
    '{"1010":{"bgColor":"#f3f4f6","bdColor":"#e5e7eb","color":"#6b7280"},"1020":{"bgColor":"#fef2f2","bdColor":"#fecaca","color":"#dc2626"},"1030":{"bgColor":"#fefce8","bdColor":"#fef08a","color":"#ca8a04"},"1040":{"bgColor":"#fff7ed","bdColor":"#fed7aa","color":"#ea580c"},"2010":{"bgColor":"#eff6ff","bdColor":"#bfdbfe","color":"#2563eb"},"2020":{"bgColor":"#ecfeff","bdColor":"#a5f3fc","color":"#0891b2"},"2030":{"bgColor":"#f0fdf4","bdColor":"#bbf7d0","color":"#16a34a"},"3010":{"bgColor":"#f5f3ff","bdColor":"#ddd6fe","color":"#7c3aed"},"3020":{"bgColor":"#faf5ff","bdColor":"#e9d5ff","color":"#9333ea"},"3030":{"bgColor":"#f0fdf4","bdColor":"#bbf7d0","color":"#15803d"},"3040":{"bgColor":"#fef2f2","bdColor":"#fecaca","color":"#b91c1c"},"4010":{"bgColor":"#faf5ff","bdColor":"#e9d5ff","color":"#7c3aed"},"5010":{"bgColor":"#fffbeb","bdColor":"#fde68a","color":"#d97706"},"5020":{"bgColor":"#f0fdf4","bdColor":"#86efac","color":"#15803d"},"5030":{"bgColor":"#fef2f2","bdColor":"#fca5a5","color":"#b91c1c"},"5040":{"bgColor":"#fef3c7","bdColor":"#fcd34d","color":"#b45309"},"5050":{"bgColor":"#f1f5f9","bdColor":"#cbd5e1","color":"#475569"},"6010":{"bgColor":"#e0e7ff","bdColor":"#a5b4fc","color":"#4f46e5"},"6020":{"bgColor":"#ccfbf1","bdColor":"#99f6e4","color":"#0d9488"},"6030":{"bgColor":"#e0f2fe","bdColor":"#7dd3fc","color":"#0284c7"},"8010":{"bgColor":"#dcfce7","bdColor":"#86efac","color":"#16a34a"},"9010":{"bgColor":"#f9fafb","bdColor":"#e5e7eb","color":"#9ca3af"}}',
    'json',
    'system',
    '订单及流转卡状态标签颜色（按 FlowStatusEnum 状态值映射）',
    1,
    0,
    17,
    1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config
    WHERE config_key = 'order.status.color'
);
