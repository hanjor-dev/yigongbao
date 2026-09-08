-- 订单虚拟单号一体化迁移脚本
-- 执行顺序：配置迁移 -> 表结构迁移 -> 历史数据回填 -> 数据校验 -> 设置非空约束
-- 规则：YG + 10 位字符，总长度 12 位；排除易混淆字符 0/O/1/I/L。
-- 说明：本脚本可重复执行；不删除、不修改原 order_code/orderNo 配置。

-- 1. 默认列表配置：仅追加 publicOrderCode，保留原有订单流水号字段。
UPDATE sys_config
SET config_value = JSON_SET(
    config_value,
    '$.columns',
    JSON_ARRAY_APPEND(
        JSON_EXTRACT(config_value, '$.columns'),
        '$',
        JSON_OBJECT('field', 'publicOrderCode', 'label', '虚拟单号', 'visible', TRUE,
                    'sort', JSON_LENGTH(JSON_EXTRACT(config_value, '$.columns')) + 1,
                    'width', 160, 'fixed', NULL)
    )
)
WHERE config_key IN (
    'order.column.config',
    'design.column.config',
    'production.column.config',
    'quality.column.config',
    'warehouse.column.config'
)
AND JSON_SEARCH(config_value, 'one', 'publicOrderCode', NULL, '$.columns[*].field') IS NULL;

-- 个人列配置由应用层兼容处理，不删除用户已有 orderCode/orderNo 配置。

-- 2. 增加订单虚拟单号字段。先允许 NULL，历史数据回填并校验通过后再设置 NOT NULL。
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

-- 3. 创建活跃订单虚拟单号唯一索引。
-- 先检查已有非 NULL 重复值，避免 CREATE UNIQUE INDEX 过程中出现不明确错误。
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

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_main'
          AND index_name = 'uk_order_main_public_code'
          AND non_unique = 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '迁移中止：uk_order_main_public_code 已存在但不是唯一索引';
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

-- 4. 为历史订单补齐虚拟单号。
DELIMITER $$
DROP PROCEDURE IF EXISTS backfill_order_public_code $$
CREATE PROCEDURE backfill_order_public_code()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_order_id BIGINT;
    DECLARE v_code VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_attempts INT DEFAULT 0;
    DECLARE cur CURSOR FOR
        SELECT id
        FROM order_main
        WHERE is_deleted = 0
          AND (public_order_code IS NULL OR public_order_code = '')
        ORDER BY id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_order_id;
        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        SET v_attempts = 0;
        code_loop: LOOP
            SET v_attempts = v_attempts + 1;
            IF v_attempts > 1000 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = '补齐中止：单个订单虚拟单号生成连续冲突超过 1000 次';
            END IF;

            SET v_code = 'YG';
            SET v_i = 0;
            WHILE v_i < 10 DO
                SET v_code = CONCAT(
                    v_code,
                    SUBSTRING(
                        '23456789ABCDEFGHJKMNPQRSTUVWXYZ',
                        FLOOR(RAND() * 32) + 1,
                        1
                    )
                );
                SET v_i = v_i + 1;
            END WHILE;

            SET v_exists = (
                SELECT COUNT(*)
                FROM order_main
                WHERE is_deleted = 0
                  AND public_order_code = v_code
            );
            IF v_exists = 0 THEN
                LEAVE code_loop;
            END IF;
        END LOOP;

        UPDATE order_main
        SET public_order_code = v_code
        WHERE id = v_order_id
          AND is_deleted = 0
          AND (public_order_code IS NULL OR public_order_code = '');
    END LOOP;
    CLOSE cur;
END $$

CALL backfill_order_public_code() $$
DROP PROCEDURE backfill_order_public_code $$
DELIMITER ;

-- 5. 严格校验回填结果；任一校验失败都会中止后续 NOT NULL 变更。
SET @order_public_code_validation_failed = 0;
DELIMITER $$
DROP PROCEDURE IF EXISTS validate_order_public_code $$
CREATE PROCEDURE validate_order_public_code()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM order_main
        WHERE is_deleted = 0
          AND (public_order_code IS NULL OR public_order_code = '')
    ) THEN
        SET @order_public_code_validation_failed = 1;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '校验失败：存在未补齐虚拟单号的活跃订单';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM order_main
        WHERE is_deleted = 0
        GROUP BY public_order_code
        HAVING COUNT(*) > 1
    ) THEN
        SET @order_public_code_validation_failed = 1;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '校验失败：活跃订单存在重复虚拟单号';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM order_main
        WHERE is_deleted = 0
          AND public_order_code NOT REGEXP '^YG[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{10}$'
    ) THEN
        SET @order_public_code_validation_failed = 1;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '校验失败：存在格式不正确的虚拟单号';
    END IF;
END $$

CALL validate_order_public_code() $$
DROP PROCEDURE validate_order_public_code $$
DELIMITER ;

-- 6. 历史数据校验通过后，将字段设为必填；已为 NOT NULL 时跳过。
SET @make_public_order_code_not_null_sql = (
    SELECT IF(
        @order_public_code_validation_failed = 1
            OR COUNT(*) = 0
            OR MAX(is_nullable) = 'NO',
        'SELECT 1',
        'ALTER TABLE order_main MODIFY COLUMN public_order_code VARCHAR(12) NOT NULL COMMENT ''订单虚拟单号'''
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'order_main'
      AND column_name = 'public_order_code'
);
PREPARE make_public_order_code_not_null_stmt FROM @make_public_order_code_not_null_sql;
EXECUTE make_public_order_code_not_null_stmt;
DEALLOCATE PREPARE make_public_order_code_not_null_stmt;

-- 7. 输出最终校验结果，正常情况下三项均为 0。
SELECT COUNT(*) AS missing_public_order_code
FROM order_main
WHERE is_deleted = 0
  AND (public_order_code IS NULL OR public_order_code = '');

SELECT COUNT(*) - COUNT(DISTINCT public_order_code) AS duplicate_public_order_code
FROM order_main
WHERE is_deleted = 0;

SELECT COUNT(*) AS invalid_public_order_code
FROM order_main
WHERE is_deleted = 0
  AND public_order_code NOT REGEXP '^YG[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{10}$';
