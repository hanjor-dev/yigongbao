-- ============================================================
-- 服务商机构类型功能实施脚本
-- 执行时机：代码部署前
-- 功能：新增服务商机构类型、业务员（自营）角色及权限配置
-- ============================================================

-- 1. 新增服务商机构类型字典项
INSERT INTO sys_dict (dict_code, dict_name, parent_code, sort_order, status, remark, create_time, update_time, is_deleted)
VALUES ('1.4', '服务商', '1', 4, 1, '企业自营业务机构', NOW(), NOW(), 0);

-- 2. 新增业务员（自营）角色
INSERT INTO sys_role (role_name, role_code, account_type, data_scope_type, status, remark, create_time, update_time, is_deleted)
VALUES ('业务员（自营）', 'salesman-self', '6.1', 'HOSPITALS', 1, '企业自营业务人员，负责自营订单创建和跟进', NOW(), NOW(), 0);

-- 3. 复制业务员角色的权限资源到业务员（自营）
SET @salesman_role_id = (SELECT id FROM sys_role WHERE role_code = 'salesman' AND is_deleted = 0);
SET @salesman_self_role_id = (SELECT id FROM sys_role WHERE role_code = 'salesman-self' AND is_deleted = 0);

INSERT INTO sys_role_resource (role_id, resource_id, create_time, update_time, is_deleted)
SELECT @salesman_self_role_id, resource_id, NOW(), NOW(), 0
FROM sys_role_resource
WHERE role_id = @salesman_role_id AND is_deleted = 0;

-- 4. 数据迁移：为现有企业部门补充与生产企业的关联记录
SET @manufacturer_org_id = (
    SELECT config_value
    FROM sys_config
    WHERE config_key = 'manufacturer.org.id'
      AND is_deleted = 0
);
SET @manufacturer_org_id = IFNULL(@manufacturer_org_id, 1);

INSERT INTO sys_dept_org (dept_id, org_id, create_time, update_time, is_deleted)
SELECT
    d.id,
    @manufacturer_org_id,
    NOW(),
    NOW(),
    0
FROM sys_dept d
WHERE d.dept_type = '6.1'
  AND d.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_dept_org do
      WHERE do.dept_id = d.id AND do.is_deleted = 0
  );

-- 5. 验证：查询企业部门关联情况
SELECT
    d.id AS dept_id,
    d.dept_name,
    do.org_id,
    o.org_name,
    o.org_type
FROM sys_dept d
LEFT JOIN sys_dept_org do ON d.id = do.dept_id AND do.is_deleted = 0
LEFT JOIN sys_org o ON do.org_id = o.id AND o.is_deleted = 0
WHERE d.dept_type = '6.1'
  AND d.is_deleted = 0
ORDER BY d.id;
