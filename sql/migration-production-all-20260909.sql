-- 线上数据库合并迁移脚本
-- 合并内容：
--   1. 生产流转卡强制完成打印权限
--   2. 设计工单设计师备注字段及默认列配置
--   3. 订单明细/草稿分类快照回填
--   4. 订单虚拟单号配置、字段、历史数据及唯一约束
-- 适用：MySQL 8.0+；请在备份并确认低峰期后执行。
-- 注意：ALTER TABLE、CREATE INDEX 等 DDL 会隐式提交，不能依赖一个总事务回滚全部变更。

SET NAMES utf8mb4;

-- ============================================================
-- 1. 设计工单设计师备注字段（幂等）
-- ============================================================
SET @add_designer_remark_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE order_main ADD COLUMN designer_remark TEXT NULL COMMENT ''设计师备注'' AFTER data_evaluation_opinion',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'order_main'
      AND column_name = 'designer_remark'
);
PREPARE add_designer_remark_stmt FROM @add_designer_remark_sql;
EXECUTE add_designer_remark_stmt;
DEALLOCATE PREPARE add_designer_remark_stmt;

-- ============================================================
-- 2. 设计工单、订单默认列配置
-- ============================================================
UPDATE sys_config
SET config_value = JSON_SET(
        config_value,
        '$.columns',
        JSON_ARRAY_APPEND(
            JSON_EXTRACT(config_value, '$.columns'),
            '$',
            JSON_OBJECT(
                'field', 'designerRemark', 'label', '设计师备注', 'visible', TRUE,
                'sort', 21, 'width', 200, 'fixed', NULL
            )
        )
    ),
    update_time = CURRENT_TIMESTAMP
WHERE config_key = 'design.column.config'
  AND is_deleted = 0
  AND JSON_SEARCH(config_value, 'one', 'designerRemark', NULL, '$.columns[*].field') IS NULL;

UPDATE sys_config
SET config_value = JSON_SET(
        config_value,
        '$.columns',
        JSON_ARRAY_APPEND(
            JSON_EXTRACT(config_value, '$.columns'),
            '$',
            JSON_OBJECT(
                'field', 'designerRemark', 'label', '设计师备注', 'visible', TRUE,
                'sort', 34, 'width', 200, 'fixed', NULL
            )
        )
    ),
    update_time = CURRENT_TIMESTAMP
WHERE config_key = 'order.column.config'
  AND is_deleted = 0
  AND JSON_SEARCH(config_value, 'one', 'designerRemark', NULL, '$.columns[*].field') IS NULL;

-- ============================================================
-- 3. 生产流转卡强制完成打印权限
-- ============================================================
DROP PROCEDURE IF EXISTS ygb_migrate_force_complete_print_permission_20260903;
DELIMITER $$
CREATE PROCEDURE ygb_migrate_force_complete_print_permission_20260903()
BEGIN
    DECLARE v_role_id BIGINT DEFAULT NULL;
    DECLARE v_parent_id BIGINT DEFAULT NULL;
    DECLARE v_resource_id BIGINT DEFAULT NULL;
    DECLARE v_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COUNT(*) INTO v_count FROM sys_role
     WHERE role_code = 'production-manager' AND is_deleted = 0;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移中止：production-manager 角色必须且只能存在 1 个';
    END IF;
    SELECT id INTO v_role_id FROM sys_role
     WHERE role_code = 'production-manager' AND is_deleted = 0;

    SELECT COUNT(*) INTO v_count FROM sys_resource
     WHERE resource_code = 'Manufacture' AND resource_type = 2 AND is_deleted = 0;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移中止：Manufacture 生产管理资源必须且只能存在 1 个';
    END IF;
    SELECT id INTO v_parent_id FROM sys_resource
     WHERE resource_code = 'Manufacture' AND resource_type = 2 AND is_deleted = 0;

    SELECT COUNT(*) INTO v_count FROM sys_resource
     WHERE resource_code = 'manufacture:ForceCompletePrint';
    IF v_count > 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移中止：强制完成打印资源编码重复';
    END IF;
    SELECT id INTO v_resource_id FROM sys_resource
     WHERE resource_code = 'manufacture:ForceCompletePrint' LIMIT 1;

    IF v_resource_id IS NULL THEN
        INSERT INTO sys_resource (
            parent_id, resource_name, resource_code, resource_type, sort,
            visible, status, remark, create_time, update_time, is_deleted
        ) VALUES (
            v_parent_id, '强制完成打印', 'manufacture:ForceCompletePrint', 3, 14,
            1, 1, '设备未推送打印完成消息时由生产管理员进行异常补偿',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
        );
        SET v_resource_id = LAST_INSERT_ID();
    ELSE
        UPDATE sys_resource
           SET parent_id = v_parent_id, resource_name = '强制完成打印',
               resource_type = 3, sort = 14, visible = 1, status = 1,
               remark = '设备未推送打印完成消息时由生产管理员进行异常补偿',
               update_time = CURRENT_TIMESTAMP, is_deleted = 0
         WHERE id = v_resource_id;
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM sys_role_resource rr
      INNER JOIN sys_role r ON r.id = rr.role_id AND r.is_deleted = 0
     WHERE rr.resource_id = v_resource_id
       AND r.role_code <> 'production-manager';
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '迁移中止：强制完成打印权限已被其他角色绑定';
    END IF;

    INSERT INTO sys_role_resource (role_id, resource_id)
    SELECT v_role_id, v_resource_id FROM DUAL
     WHERE NOT EXISTS (
         SELECT 1 FROM sys_role_resource
          WHERE role_id = v_role_id AND resource_id = v_resource_id
     );
END$$
DELIMITER ;

START TRANSACTION;
CALL ygb_migrate_force_complete_print_permission_20260903();
COMMIT;
DROP PROCEDURE IF EXISTS ygb_migrate_force_complete_print_permission_20260903;

-- ============================================================
-- 4. 订单明细和草稿分类快照回填
-- ============================================================
SELECT id, name, category_code, category_name
FROM rebuild_project
WHERE is_deleted = 0
  AND (category_code IS NULL OR category_name IS NULL OR category_code = '' OR category_name = '');

SET @order_item_missing_before = (
    SELECT COUNT(*) FROM order_item
    WHERE project_id IS NOT NULL
      AND (category_code IS NULL OR category_code = '' OR category_name IS NULL OR category_name = '')
);
SET @order_item_draft_missing_before = (
    SELECT COUNT(*) FROM order_item_draft
    WHERE project_id IS NOT NULL
      AND (category_code IS NULL OR category_code = '' OR category_name IS NULL OR category_name = '')
);
SELECT @order_item_missing_before AS order_item_missing_before,
       @order_item_draft_missing_before AS order_item_draft_missing_before;

START TRANSACTION;
UPDATE order_item oi
JOIN rebuild_project rp ON rp.id = oi.project_id AND rp.is_deleted = 0
SET oi.category_code = COALESCE(NULLIF(oi.category_code, ''), rp.category_code),
    oi.category_name = COALESCE(NULLIF(oi.category_name, ''), rp.category_name)
WHERE oi.project_id IS NOT NULL
  AND (oi.category_code IS NULL OR oi.category_code = ''
       OR oi.category_name IS NULL OR oi.category_name = '');
SET @order_item_updated = ROW_COUNT();

UPDATE order_item_draft oid
JOIN rebuild_project rp ON rp.id = oid.project_id AND rp.is_deleted = 0
SET oid.category_code = COALESCE(NULLIF(oid.category_code, ''), rp.category_code),
    oid.category_name = COALESCE(NULLIF(oid.category_name, ''), rp.category_name)
WHERE oid.project_id IS NOT NULL
  AND (oid.category_code IS NULL OR oid.category_code = ''
       OR oid.category_name IS NULL OR oid.category_name = '');
SET @order_item_draft_updated = ROW_COUNT();

SELECT @order_item_updated AS order_item_updated,
       @order_item_draft_updated AS order_item_draft_updated;

SELECT 'order_item' AS source, oi.id, oi.order_id, oi.project_id,
       oi.category_code, oi.category_name
FROM order_item oi
WHERE oi.project_id IS NOT NULL
  AND (oi.category_code IS NULL OR oi.category_code = ''
       OR oi.category_name IS NULL OR oi.category_name = '')
UNION ALL
SELECT 'order_item_draft' AS source, oid.id, oid.draft_id, oid.project_id,
       oid.category_code, oid.category_name
FROM order_item_draft oid
WHERE oid.project_id IS NOT NULL
  AND (oid.category_code IS NULL OR oid.category_code = ''
       OR oid.category_name IS NULL OR oid.category_name = '');
COMMIT;

SELECT 'order_item' AS source, COUNT(*) AS unresolved_count
FROM order_item oi
WHERE oi.project_id IS NOT NULL
  AND (oi.category_code IS NULL OR oi.category_code = ''
       OR oi.category_name IS NULL OR oi.category_name = '')
UNION ALL
SELECT 'order_item_draft' AS source, COUNT(*) AS unresolved_count
FROM order_item_draft oid
WHERE oid.project_id IS NOT NULL
  AND (oid.category_code IS NULL OR oid.category_code = ''
       OR oid.category_name IS NULL OR oid.category_name = '');

-- ============================================================
-- 5. 订单虚拟单号配置、字段、历史数据及唯一约束
-- ============================================================
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
),
    update_time = CURRENT_TIMESTAMP
WHERE config_key IN (
    'order.column.config', 'design.column.config', 'production.column.config',
    'quality.column.config', 'warehouse.column.config'
)
  AND is_deleted = 0
  AND JSON_SEARCH(config_value, 'one', 'publicOrderCode', NULL, '$.columns[*].field') IS NULL;

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

DELIMITER $$
DROP PROCEDURE IF EXISTS backfill_order_public_code $$
CREATE PROCEDURE backfill_order_public_code()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_order_id BIGINT;
    DECLARE v_code VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_j INT DEFAULT 0;
    DECLARE v_char CHAR(1);
    DECLARE v_attempts INT DEFAULT 0;
    DECLARE cur CURSOR FOR
        SELECT id FROM order_main
        WHERE is_deleted = 0
          AND (public_order_code IS NULL OR public_order_code = ''
               OR public_order_code NOT REGEXP '^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{12}$')
        ORDER BY id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_order_id;
        IF v_done = 1 THEN LEAVE read_loop; END IF;
        SET v_attempts = 0;
        code_loop: LOOP
            SET v_attempts = v_attempts + 1;
            IF v_attempts > 1000 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '补齐中止：单个订单虚拟单号生成连续冲突超过 1000 次';
            END IF;
            SET v_code = '';
            SET v_i = 0;
            WHILE v_i < 8 DO
                SET v_code = CONCAT(v_code, SUBSTRING('23456789', FLOOR(RAND() * 8) + 1, 1));
                SET v_i = v_i + 1;
            END WHILE;
            SET v_i = 0;
            WHILE v_i < 4 DO
                SET v_code = CONCAT(v_code, SUBSTRING('ABCDEFGHJKMNPQRSTUVWXYZ', FLOOR(RAND() * 23) + 1, 1));
                SET v_i = v_i + 1;
            END WHILE;
            SET v_i = 12;
            WHILE v_i > 1 DO
                SET v_j = FLOOR(RAND() * v_i) + 1;
                SET v_char = SUBSTRING(v_code, v_i, 1);
                SET v_code = INSERT(v_code, v_i, 1, SUBSTRING(v_code, v_j, 1));
                SET v_code = INSERT(v_code, v_j, 1, v_char);
                SET v_i = v_i - 1;
            END WHILE;
            SET v_exists = (SELECT COUNT(*) FROM order_main
                            WHERE is_deleted = 0 AND public_order_code = v_code);
            IF v_exists = 0 THEN LEAVE code_loop; END IF;
        END LOOP;
        UPDATE order_main
        SET public_order_code = v_code
        WHERE id = v_order_id AND is_deleted = 0
          AND (public_order_code IS NULL OR public_order_code = ''
               OR public_order_code NOT REGEXP '^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{12}$');
    END LOOP;
    CLOSE cur;
END $$
CALL backfill_order_public_code() $$
DROP PROCEDURE backfill_order_public_code $$
DELIMITER ;

SET @order_public_code_validation_failed = 0;
DELIMITER $$
DROP PROCEDURE IF EXISTS validate_order_public_code $$
CREATE PROCEDURE validate_order_public_code()
BEGIN
    IF EXISTS (SELECT 1 FROM order_main WHERE is_deleted = 0
               AND (public_order_code IS NULL OR public_order_code = '')) THEN
        SET @order_public_code_validation_failed = 1;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '校验失败：存在未补齐虚拟单号的活跃订单';
    END IF;
    IF EXISTS (SELECT 1 FROM order_main WHERE is_deleted = 0
               GROUP BY public_order_code HAVING COUNT(*) > 1) THEN
        SET @order_public_code_validation_failed = 1;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '校验失败：活跃订单存在重复虚拟单号';
    END IF;
    IF EXISTS (SELECT 1 FROM order_main WHERE is_deleted = 0
               AND public_order_code NOT REGEXP '^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{12}$') THEN
        SET @order_public_code_validation_failed = 1;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '校验失败：存在格式不正确的虚拟单号';
    END IF;
END $$
CALL validate_order_public_code() $$
DROP PROCEDURE validate_order_public_code $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE ensure_order_public_code_index()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_main'
          AND index_name = 'uk_order_main_public_code'
          AND non_unique = 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '迁移中止：uk_order_main_public_code 已存在但不是唯一索引';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'order_main'
          AND index_name = 'uk_order_main_public_code'
    ) THEN
        CREATE UNIQUE INDEX uk_order_main_public_code
            ON order_main ((CASE WHEN is_deleted = 0 THEN public_order_code ELSE NULL END));
    END IF;
END $$
CALL ensure_order_public_code_index() $$
DROP PROCEDURE ensure_order_public_code_index $$
DELIMITER ;

SET @make_public_order_code_not_null_sql = (
    SELECT IF(
        COUNT(*) = 0 OR MAX(is_nullable) = 'NO',
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

-- ============================================================
-- 最终校验：正常情况下三项均为 0
-- ============================================================
SELECT COUNT(*) AS missing_public_order_code
FROM order_main WHERE is_deleted = 0
  AND (public_order_code IS NULL OR public_order_code = '');
SELECT COUNT(*) - COUNT(DISTINCT public_order_code) AS duplicate_public_order_code
FROM order_main WHERE is_deleted = 0;
SELECT COUNT(*) AS invalid_public_order_code
FROM order_main WHERE is_deleted = 0
  AND public_order_code NOT REGEXP '^[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{12}$';
