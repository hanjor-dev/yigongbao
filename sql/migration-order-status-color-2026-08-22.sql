-- 订单及流转卡状态标签颜色配置
-- 适用于已存在 sys_config 的环境，重复执行不会插入重复配置。

INSERT INTO sys_config (
    config_key, config_name, config_value, config_type, config_group,
    config_desc, is_system, is_public, sort, status
)
SELECT
    'order.status.color',
    '订单及流转卡状态标签颜色配置',
    '{"1010":"#909399","1020":"#E6A23C","1030":"#67C23A","1040":"#F56C6C","2010":"#E6A23C","2020":"#409EFF","2030":"#67C23A","3010":"#E6A23C","3020":"#409EFF","3030":"#67C23A","3040":"#F56C6C","4010":"#409EFF","5010":"#409EFF","5020":"#67C23A","5030":"#F56C6C","5040":"#E6A23C","5050":"#409EFF","6010":"#E6A23C","6020":"#67C23A","6030":"#67C23A","8010":"#67C23A","9010":"#909399"}',
    'json', 'system', '订单及流转卡状态标签颜色（按 FlowStatusEnum 状态值映射）', 1, 0, 17, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config
    WHERE config_key = 'order.status.color'
);
