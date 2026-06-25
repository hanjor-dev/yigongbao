-- ============================================================
-- 新增时间字段 ALTER 语句
-- 执行日期：2026-06-25
-- ============================================================

-- 1. order_main 表：新增生产开始时间和生产结束时间
ALTER TABLE order_main
ADD COLUMN production_start_time DATETIME COMMENT '生产开始时间（任意流转卡开始打印时）' AFTER user_confirm_time;

ALTER TABLE order_main
ADD COLUMN production_end_time DATETIME COMMENT '生产结束时间（所有流转卡后处理结束时）' AFTER production_start_time;

-- 2. production_record 表：新增后处理结束时间
ALTER TABLE production_record
ADD COLUMN post_processing_end_time DATETIME COMMENT '后处理结束时间' AFTER print_finish_time;
