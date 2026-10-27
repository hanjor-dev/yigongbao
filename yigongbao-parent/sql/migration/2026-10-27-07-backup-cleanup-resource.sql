-- COS 清理菜单、按钮及角色授权。
-- 执行前检查：Backup 父资源必须启用且未删除并且唯一；admin、company-admin 两角色必须各唯一、启用、未删除。
-- 可重入策略：按 resource_code 和 (role_id,resource_id) 判重；不使用任何固定角色/资源主键。

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_backup_cleanup_resources$$
CREATE PROCEDURE migrate_backup_cleanup_resources()
BEGIN
    DECLARE v_backup_parent_count INT DEFAULT 0;
    DECLARE v_target_role_count INT DEFAULT 0;
    DECLARE v_cleanup_menu_id BIGINT DEFAULT NULL;
    DECLARE v_conflict_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_backup_parent_count
      FROM sys_resource
     WHERE resource_code='Backup' AND resource_type=1 AND status=1 AND is_deleted=0;
    IF v_backup_parent_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Backup 父资源不存在、重复、类型错误或已停用；请人工处理';
    END IF;

    SELECT COUNT(*) INTO v_target_role_count
      FROM sys_role
     WHERE role_code IN ('admin','company-admin') AND status=1 AND is_deleted=0;
    IF v_target_role_count <> 2
       OR (SELECT COUNT(*) FROM sys_role WHERE role_code='admin' AND status=1 AND is_deleted=0) <> 1
       OR (SELECT COUNT(*) FROM sys_role WHERE role_code='company-admin' AND status=1 AND is_deleted=0) <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='admin、company-admin 必须各有且仅有一个启用且未删除角色';
    END IF;

    SELECT COUNT(*) INTO v_conflict_count
      FROM sys_resource r
     WHERE r.resource_code='BackupCleanup'
       AND NOT (r.parent_id=(SELECT id FROM sys_resource WHERE resource_code='Backup' AND resource_type=1 AND status=1 AND is_deleted=0)
                AND r.resource_type=2 AND r.status=1 AND r.is_deleted=0);
    IF v_conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='BackupCleanup 已存在冲突定义；拒绝自动覆盖';
    END IF;

    INSERT INTO sys_resource
        (parent_id,resource_name,resource_code,resource_type,icon,path,component,redirect,sort,visible,status,remark,
         create_time,update_time,create_by,update_by,is_deleted)
    SELECT p.id,'COS 清理','BackupCleanup',2,'Delete','/cleanup','system/backup/CleanupTaskList.vue',NULL,10,1,1,
           '备份管理下的 COS 清理任务与保护名单',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),NULL,NULL,0
      FROM sys_resource p
     WHERE p.resource_code='Backup' AND p.resource_type=1 AND p.status=1 AND p.is_deleted=0
       AND NOT EXISTS (SELECT 1 FROM sys_resource x WHERE x.resource_code='BackupCleanup');

    SELECT id INTO v_cleanup_menu_id
      FROM sys_resource
     WHERE resource_code='BackupCleanup' AND resource_type=2 AND status=1 AND is_deleted=0;

    SELECT COUNT(*) INTO v_conflict_count
      FROM sys_resource r
     WHERE r.resource_code IN ('backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun',
                               'backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       AND NOT (r.parent_id=v_cleanup_menu_id AND r.resource_type=3 AND r.status=1 AND r.is_deleted=0);
    IF v_conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='备份清理按钮已存在冲突定义；拒绝自动覆盖';
    END IF;

    INSERT INTO sys_resource
        (parent_id,resource_name,resource_code,resource_type,icon,path,component,redirect,sort,visible,status,remark,
         create_time,update_time,create_by,update_by,is_deleted)
    SELECT v_cleanup_menu_id,v.resource_name,v.resource_code,3,NULL,NULL,NULL,NULL,v.sort_no,1,1,v.remark,
           CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),NULL,NULL,0
      FROM (
        SELECT '查看清理任务列表' resource_name,'backup-cleanup:List' resource_code,1 sort_no,'清理任务分页查询' remark
        UNION ALL SELECT '查看清理任务详情','backup-cleanup:View',2,'任务、快照、明细和阻断原因'
        UNION ALL SELECT '创建清理试运行','backup-cleanup:DryRun',3,'仅试运行，不签发实际删除授权'
        UNION ALL SELECT '暂停清理任务','backup-cleanup:Pause',4,'暂停任务'
        UNION ALL SELECT '恢复清理任务','backup-cleanup:Resume',5,'恢复任务'
        UNION ALL SELECT '管理清理保护名单','backup-cleanup:Protection',6,'保护名单查询、新增和移除'
      ) v
     WHERE NOT EXISTS (SELECT 1 FROM sys_resource x WHERE x.resource_code=v.resource_code);

    INSERT INTO sys_role_resource (role_id,resource_id)
    SELECT role_row.id,resource_row.id
      FROM sys_role role_row
      JOIN sys_resource resource_row
        ON resource_row.resource_code IN ('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun',
                                          'backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       AND resource_row.status=1 AND resource_row.is_deleted=0
     WHERE role_row.role_code IN ('admin','company-admin')
       AND role_row.status=1 AND role_row.is_deleted=0
       AND NOT EXISTS (
         SELECT 1 FROM sys_role_resource existing_relation
          WHERE existing_relation.role_id=role_row.id AND existing_relation.resource_id=resource_row.id
       );

    IF (SELECT COUNT(*) FROM sys_resource WHERE resource_code IN
       ('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       AND status=1 AND is_deleted=0) <> 7 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='执行后资源数量不是 7；事务回滚';
    END IF;
    IF EXISTS (
       SELECT 1 FROM sys_role_resource rr
       JOIN sys_resource res ON res.id=rr.resource_id
       JOIN sys_role role_row ON role_row.id=rr.role_id
       WHERE res.resource_code IN
       ('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       AND role_row.role_code NOT IN ('admin','company-admin')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='发现非目标角色关联；拒绝继续并要求人工评估';
    END IF;
    IF EXISTS (
       SELECT role_row.role_code
       FROM sys_role role_row
       LEFT JOIN sys_role_resource rr ON rr.role_id=role_row.id
       LEFT JOIN sys_resource res ON res.id=rr.resource_id AND res.resource_code IN
       ('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
       WHERE role_row.role_code IN ('admin','company-admin') AND role_row.status=1 AND role_row.is_deleted=0
       GROUP BY role_row.id,role_row.role_code HAVING COUNT(res.id)<>7
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='目标角色未各获得 7 条资源关联；事务回滚';
    END IF;
END$$
DELIMITER ;

START TRANSACTION;
CALL migrate_backup_cleanup_resources();
COMMIT;
DROP PROCEDURE IF EXISTS migrate_backup_cleanup_resources;

-- 执行后验证：第一行应为 7；随后 admin、company-admin 各 7；第三个查询应为 0 行。
SELECT COUNT(*) AS enabled_resource_count FROM sys_resource WHERE resource_code IN
('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
AND status=1 AND is_deleted=0;
SELECT role_row.role_code,COUNT(*) AS granted_resource_count FROM sys_role role_row
JOIN sys_role_resource rr ON rr.role_id=role_row.id JOIN sys_resource res ON res.id=rr.resource_id
WHERE role_row.role_code IN ('admin','company-admin') AND res.resource_code IN
('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
GROUP BY role_row.role_code;
SELECT role_row.role_code,res.resource_code FROM sys_role_resource rr JOIN sys_role role_row ON role_row.id=rr.role_id
JOIN sys_resource res ON res.id=rr.resource_id WHERE res.resource_code IN
('BackupCleanup','backup-cleanup:List','backup-cleanup:View','backup-cleanup:DryRun','backup-cleanup:Pause','backup-cleanup:Resume','backup-cleanup:Protection')
AND role_row.role_code NOT IN ('admin','company-admin');
-- 人工回滚：先确认七个资源未被其他功能复用；仅删除对应 sys_role_resource 关系并逻辑删除七个新资源，绝不修改 Backup 父资源。
