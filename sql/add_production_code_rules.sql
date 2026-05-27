-- ============================================================
-- 生产模块编码规则补充脚本
-- 执行前请确认这些规则尚未存在，避免重复插入
-- ============================================================

INSERT INTO sys_code_rule (rule_code, rule_name, prefix, date_format, seq_length, reset_type, status, current_value)
VALUES
('PRODUCTION_RECORD_NO', '生产流转卡编号', 'PR-', NULL,             6, 'NEVER', 1, 0),
('PRODUCTION_BATCH_NO',  '生产批号',       'PB-', '{yyyy}{MM}{dd}', 4, 'DAY',   1, 0),
('PRODUCT_NO',           '产品编号',       'PD-', NULL,             6, 'NEVER', 1, 0),
('UDI_CODE',             'UDI编码',        'UDI-',NULL,             8, 'NEVER', 1, 0);

-- 验证插入结果
SELECT rule_code, rule_name, prefix, date_format, seq_length, reset_type, status
FROM sys_code_rule
WHERE rule_code IN ('PRODUCTION_RECORD_NO', 'PRODUCTION_BATCH_NO', 'PRODUCT_NO', 'UDI_CODE');
