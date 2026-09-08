-- 历史订单虚拟单号补齐
-- 规则：YG + 10 位字符，总长度 12 位；排除易混淆字符 0/O/1/I/L。
-- 可重复执行，只更新未删除且虚拟单号为空的订单。

DELIMITER $$

DROP PROCEDURE IF EXISTS backfill_order_public_code $$
CREATE PROCEDURE backfill_order_public_code()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_order_id BIGINT;
    DECLARE v_code VARCHAR(12);
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
                    SET MESSAGE_TEXT = '补齐中止：虚拟单号生成连续冲突超过 1000 次';
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

-- 以下校验结果均应为 0。
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

-- 确认上述校验均为 0 后执行：
-- ALTER TABLE order_main MODIFY COLUMN public_order_code VARCHAR(12) NOT NULL COMMENT '订单虚拟单号';
