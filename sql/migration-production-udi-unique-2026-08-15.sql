-- 活跃生产产品的 UDI 必须全局唯一；先拒绝脏数据，避免静默丢失追溯信息。
DROP PROCEDURE IF EXISTS ygb_add_active_udi_unique;
DELIMITER $$
CREATE PROCEDURE ygb_add_active_udi_unique()
BEGIN
    IF EXISTS (
        SELECT 1 FROM production_product
        WHERE is_deleted = 0 AND udi_code IS NOT NULL AND udi_code <> ''
        GROUP BY udi_code HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate active UDI codes exist; clean data before migration';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'production_product'
          AND index_name = 'uk_active_udi_code'
    ) THEN
        CREATE UNIQUE INDEX uk_active_udi_code
        ON production_product ((CASE WHEN is_deleted = 0 AND udi_code IS NOT NULL AND udi_code <> '' THEN udi_code ELSE NULL END));
    END IF;
END$$
DELIMITER ;
CALL ygb_add_active_udi_unique();
DROP PROCEDURE IF EXISTS ygb_add_active_udi_unique;
