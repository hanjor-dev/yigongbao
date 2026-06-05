-- 测试查询：验证sys_resource表中有多少资源
SELECT COUNT(*) as total_resources FROM sys_resource;

-- 测试查询：验证超级管理员关联了多少资源
SELECT COUNT(*) as admin_permissions FROM sys_role_resource WHERE role_id = 1;

-- 对比查询：应该相等
SELECT 
    (SELECT COUNT(*) FROM sys_resource) as total_resources,
    (SELECT COUNT(*) FROM sys_role_resource WHERE role_id = 1) as admin_permissions,
    CASE 
        WHEN (SELECT COUNT(*) FROM sys_resource) = (SELECT COUNT(*) FROM sys_role_resource WHERE role_id = 1) 
        THEN '权限关联正确' 
        ELSE '权限关联不完整' 
    END as status;
