-- 添加订单修改申请列表权限
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, status)
VALUES (1122, 101, '修改申请列表', 'order:ModifyApply', 3, 22, 1);

-- 添加设计师工作量导出权限
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, status)
VALUES (1214, 102, '工作量导出', 'design:WorkExport', 3, 14, 1);
