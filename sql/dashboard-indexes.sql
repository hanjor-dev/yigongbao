-- 数据概览模块性能优化索引
-- 创建日期：2026-06-22

-- 业务员查询优化索引
CREATE INDEX IF NOT EXISTS idx_order_operator_time
ON order_main(operator_id, create_time, status);

-- 设计师查询优化索引
CREATE INDEX IF NOT EXISTS idx_order_designer_phase
ON order_main(designer_id, phase, create_time, status);

-- 时间范围查询优化索引
CREATE INDEX IF NOT EXISTS idx_order_create_time
ON order_main(create_time, status);

-- 验证索引创建
SHOW INDEX FROM order_main WHERE Key_name IN ('idx_order_operator_time', 'idx_order_designer_phase', 'idx_order_create_time');
