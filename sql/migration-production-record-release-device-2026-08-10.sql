-- ============================================================
-- Production record print-device release support
-- Generated: 2026-08-10
-- Scope: allow assignment data to be cleared and add the occupancy lookup index
-- ============================================================

SET NAMES utf8mb4;

-- 释放后产品编号需要回到“尚未生成”，兼容旧生产建表脚本的 NOT NULL 定义。
ALTER TABLE production_product
    MODIFY COLUMN product_no VARCHAR(50) NULL COMMENT '产品编号（分配设备时生成）';

DELIMITER $$

DROP PROCEDURE IF EXISTS ygb_ensure_print_device_status_index $$
CREATE PROCEDURE ygb_ensure_print_device_status_index()
BEGIN
    DECLARE v_index_count INT DEFAULT 0;
    DECLARE v_columns VARCHAR(255);
    DECLARE v_non_unique INT DEFAULT 1;

    SELECT COUNT(*), GROUP_CONCAT(column_name ORDER BY seq_in_index), MIN(non_unique)
      INTO v_index_count, v_columns, v_non_unique
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'production_record'
       AND index_name = 'idx_print_device_status';

    IF v_index_count = 0 THEN
        CREATE INDEX idx_print_device_status
            ON production_record(print_device_id, status, is_deleted);
    ELSEIF v_columns <> 'print_device_id,status,is_deleted'
            OR v_index_count <> 3
            OR v_non_unique <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'idx_print_device_status exists with an unexpected definition';
    END IF;
END $$

CALL ygb_ensure_print_device_status_index() $$
DROP PROCEDURE IF EXISTS ygb_ensure_print_device_status_index $$

DELIMITER ;

SELECT index_name, seq_in_index, column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'production_record'
  AND index_name = 'idx_print_device_status'
ORDER BY seq_in_index;
