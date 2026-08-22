-- ============================================================
-- 区域管理员账户级多机构权限线上迁移
-- 日期：2026-08-22
-- 适用：MySQL 8.0+
--
-- 已确认迁移口径：
-- 1. sys_user.org_id 是必填主所属机构；
-- 2. sys_user_managed_org 只存额外管理机构，可为零条；
-- 3. 不从部门、历史订单或业务员反推和回填额外机构；
-- 4. 迁移后现有区域管理员默认仅管理主所属机构，由管理员手动编辑补充。
-- ============================================================

-- ---------- 0. 执行前检查（结果必须人工留档） ----------
SELECT r.id AS role_id, r.role_code, r.data_scope_type, COUNT(u.id) AS user_count
FROM sys_role r
LEFT JOIN sys_user u ON u.role_id = r.id AND u.is_deleted = 0
WHERE r.role_code = 'regional-manager' AND r.is_deleted = 0
GROUP BY r.id, r.role_code, r.data_scope_type;

-- 主机构为空的区域管理员必须先修复；期望结果为 0 行。
SELECT u.id, u.username, u.real_name, u.org_id
FROM sys_user u
INNER JOIN sys_role r ON r.id = u.role_id AND r.is_deleted = 0
WHERE u.is_deleted = 0
  AND r.role_code = 'regional-manager'
  AND u.org_id IS NULL;

-- 主机构不是正常经销商/服务商的账户需人工确认。
SELECT u.id, u.username, u.real_name, u.org_id, o.org_name, o.org_type, o.status
FROM sys_user u
INNER JOIN sys_role r ON r.id = u.role_id AND r.is_deleted = 0
LEFT JOIN sys_org o ON o.id = u.org_id AND o.is_deleted = 0
WHERE u.is_deleted = 0
  AND r.role_code = 'regional-manager'
  AND (o.id IS NULL OR o.status <> 1 OR o.org_type NOT IN ('1.2', '1.4'));

-- 自动阻断：角色缺失/重复或主机构数据不合规时，脚本立即失败，不允许继续切换权限。
DROP PROCEDURE IF EXISTS assert_regional_scope_migration_ready_20260822;
DELIMITER $$
CREATE PROCEDURE assert_regional_scope_migration_ready_20260822()
BEGIN
    DECLARE regional_role_count INT DEFAULT 0;
    DECLARE invalid_user_count INT DEFAULT 0;

    SELECT COUNT(*) INTO regional_role_count
    FROM sys_role
    WHERE role_code = 'regional-manager' AND is_deleted = 0;

    IF regional_role_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '迁移中止：有效 regional-manager 角色数量必须为 1';
    END IF;

    SELECT COUNT(*) INTO invalid_user_count
    FROM sys_user u
    INNER JOIN sys_role r ON r.id = u.role_id AND r.is_deleted = 0
    LEFT JOIN sys_org o ON o.id = u.org_id AND o.is_deleted = 0
    WHERE u.is_deleted = 0
      AND r.role_code = 'regional-manager'
      AND (u.org_id IS NULL OR o.id IS NULL OR o.status <> 1 OR o.org_type NOT IN ('1.2', '1.4'));

    IF invalid_user_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '迁移中止：存在主机构为空、无效或非经销商/服务商的区域管理员';
    END IF;
END$$
DELIMITER ;

CALL assert_regional_scope_migration_ready_20260822();
DROP PROCEDURE assert_regional_scope_migration_ready_20260822;

-- ---------- 1. 建表（幂等，不写入任何授权数据） ----------
CREATE TABLE IF NOT EXISTS sys_user_managed_org (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id             BIGINT          NOT NULL COMMENT '区域管理员用户ID',
    org_id              BIGINT          NOT NULL COMMENT '额外管理机构ID（仅经销商/服务商）',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_managed_org (user_id, org_id),
    KEY idx_managed_org_user (user_id),
    KEY idx_managed_org_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='区域管理员-额外管理机构关联表';

-- ---------- 2. 保存原角色范围（首次执行保存，重复执行不覆盖） ----------
CREATE TABLE IF NOT EXISTS migration_backup_regional_scope_20260822 (
    role_id             BIGINT          NOT NULL,
    old_data_scope_type VARCHAR(16)     NOT NULL,
    old_role_desc       VARCHAR(256)    DEFAULT NULL,
    backup_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='20260822区域管理员权限迁移回退备份';

INSERT IGNORE INTO migration_backup_regional_scope_20260822 (role_id, old_data_scope_type, old_role_desc)
SELECT id, data_scope_type, role_desc
FROM sys_role
WHERE role_code = 'regional-manager' AND is_deleted = 0;

-- ---------- 3. 切换角色权限类型 ----------
START TRANSACTION;

UPDATE sys_role
SET data_scope_type = 'user_orgs',
    role_desc = '管理主所属机构及账户额外分配机构的全部订单',
    update_time = CURRENT_TIMESTAMP
WHERE role_code = 'regional-manager'
  AND is_deleted = 0
  AND data_scope_type <> 'user_orgs';

COMMIT;

-- 重要：这里故意没有 INSERT INTO sys_user_managed_org ... SELECT ...；禁止自动回填。

-- ---------- 4. 执行后验收 ----------
-- 4.1 区域管理员角色应全部为 user_orgs。
SELECT id, role_code, data_scope_type, role_desc
FROM sys_role
WHERE role_code = 'regional-manager' AND is_deleted = 0;

-- 4.2 首次上线、管理员尚未手动配置时，关系表应为 0 条。
SELECT COUNT(*) AS managed_relation_count FROM sys_user_managed_org;

-- 4.3 待管理员逐个维护的区域管理员清单及当前有效范围说明。
SELECT u.id,
       u.username,
       u.real_name,
       u.org_id AS primary_org_id,
       o.org_name AS primary_org_name,
       COUNT(umo.id) AS additional_org_count,
       CASE WHEN COUNT(umo.id) = 0 THEN '仅主机构，待按需配置' ELSE '已配置额外机构' END AS migration_status
FROM sys_user u
INNER JOIN sys_role r ON r.id = u.role_id AND r.is_deleted = 0
LEFT JOIN sys_org o ON o.id = u.org_id AND o.is_deleted = 0
LEFT JOIN sys_user_managed_org umo ON umo.user_id = u.id
WHERE u.is_deleted = 0 AND r.role_code = 'regional-manager'
GROUP BY u.id, u.username, u.real_name, u.org_id, o.org_name
ORDER BY u.id;

-- 4.4 非区域管理员不应存在额外管理关系；期望结果为 0 行。
SELECT umo.user_id, umo.org_id, u.role_code
FROM sys_user_managed_org umo
INNER JOIN sys_user u ON u.id = umo.user_id AND u.is_deleted = 0
LEFT JOIN sys_role r ON r.id = u.role_id AND r.is_deleted = 0
WHERE r.id IS NULL OR r.role_code <> 'regional-manager';

-- 4.5 额外机构必须是正常经销商或服务商，且不能等于主机构；期望结果为 0 行。
SELECT umo.id, umo.user_id, umo.org_id, u.org_id AS primary_org_id,
       o.org_type, o.status, o.is_deleted
FROM sys_user_managed_org umo
INNER JOIN sys_user u ON u.id = umo.user_id
LEFT JOIN sys_org o ON o.id = umo.org_id
WHERE umo.org_id = u.org_id
   OR o.id IS NULL
   OR o.is_deleted <> 0
   OR o.status <> 1
   OR o.org_type NOT IN ('1.2', '1.4');

-- ---------- 5. 回退命令（仅发生严重问题时人工执行） ----------
-- 注意：回退角色范围会恢复旧 dept 行为；关系表保留便于再次上线，不影响 dept 范围。
-- START TRANSACTION;
-- UPDATE sys_role r
-- INNER JOIN migration_backup_regional_scope_20260822 b ON b.role_id = r.id
-- SET r.data_scope_type = b.old_data_scope_type,
--     r.role_desc = b.old_role_desc,
--     r.update_time = CURRENT_TIMESTAMP
-- WHERE r.role_code = 'regional-manager' AND r.is_deleted = 0;
-- COMMIT;
--
-- 确认不再重上线且已备份后才可执行：
-- DROP TABLE sys_user_managed_org;
-- DROP TABLE migration_backup_regional_scope_20260822;
