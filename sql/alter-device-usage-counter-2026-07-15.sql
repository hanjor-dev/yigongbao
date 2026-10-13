-- 为 device_daily_usage_counter 表添加 BaseEntity 缺失字段
-- 日期：2026-07-15
-- 说明：修复 DeviceUsageCounterEntity 继承 BaseEntity 导致的字段不匹配问题

-- 添加 create_by 字段
ALTER TABLE device_daily_usage_counter
    ADD COLUMN create_by BIGINT COMMENT '创建人ID' AFTER update_time;

-- 添加 update_by 字段
ALTER TABLE device_daily_usage_counter
    ADD COLUMN update_by BIGINT COMMENT '更新人ID' AFTER create_by;

-- 添加 is_deleted 字段
ALTER TABLE device_daily_usage_counter
    ADD COLUMN is_deleted TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）' AFTER update_by;

-- 验证字段添加
SELECT
    CASE
        WHEN COUNT(*) = 3 THEN '✓ device_daily_usage_counter 表字段添加成功'
        ELSE '✗ device_daily_usage_counter 表字段添加失败'
    END AS '执行结果'
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'device_daily_usage_counter'
  AND column_name IN ('create_by', 'update_by', 'is_deleted');
