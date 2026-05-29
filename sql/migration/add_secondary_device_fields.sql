-- 为 production_process 表添加辅助设备字段（用于 clean_dry 工序）
-- 执行时间：2026-05-29

ALTER TABLE production_process
ADD COLUMN secondary_device_id BIGINT COMMENT '辅助设备ID（用于 clean_dry 工序的干燥设备）' AFTER device_name,
ADD COLUMN secondary_device_no VARCHAR(50) COMMENT '辅助设备编号' AFTER secondary_device_id,
ADD COLUMN secondary_device_name VARCHAR(100) COMMENT '辅助设备名称' AFTER secondary_device_no;
