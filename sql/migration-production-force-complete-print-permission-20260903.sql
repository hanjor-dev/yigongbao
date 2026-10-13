-- 生产流转卡强制完成打印权限，仅授予 production-manager 角色。
-- 适用：MySQL 8.0+
SET NAMES utf8mb4;

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

SELECT r.role_code, res.resource_code
  FROM sys_role_resource rr
  INNER JOIN sys_role r ON r.id = rr.role_id AND r.is_deleted = 0
  INNER JOIN sys_resource res ON res.id = rr.resource_id AND res.is_deleted = 0
 WHERE res.resource_code = 'manufacture:ForceCompletePrint';
