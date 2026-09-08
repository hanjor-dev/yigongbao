-- 订单虚拟单号结构迁移
-- 不删除、不修改原 order_code；脚本可重复执行。

SET @add_public_order_code_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE order_main ADD COLUMN public_order_code VARCHAR(12) NULL COMMENT ''订单虚拟单号'' AFTER order_code',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'order_main'
      AND column_name = 'public_order_code'
);
PREPARE add_public_order_code_stmt FROM @add_public_order_code_sql;
EXECUTE add_public_order_code_stmt;
DEALLOCATE PREPARE add_public_order_code_stmt;

DROP PROCEDURE IF EXISTS ensure_order_public_code_index;
DELIMITER $$
CREATE PROCEDURE ensure_order_public_code_index()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM order_main
        WHERE is_deleted = 0
          AND public_order_code IS NOT NULL
        GROUP BY public_order_code
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '迁移中止：活跃订单存在重复虚拟单号，请先清理数据';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_main'
          AND index_name = 'uk_order_main_public_code'
    ) THEN
        CREATE UNIQUE INDEX uk_order_main_public_code
            ON order_main ((CASE WHEN is_deleted = 0 THEN public_order_code ELSE NULL END));
    END IF;
END$$
DELIMITER ;

CALL ensure_order_public_code_index();
DROP PROCEDURE IF EXISTS ensure_order_public_code_index;

-- 历史数据补齐并校验通过后执行：
-- ALTER TABLE order_main MODIFY COLUMN public_order_code VARCHAR(12) NOT NULL COMMENT '订单虚拟单号';
