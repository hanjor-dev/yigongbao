-- ============================================================
-- 医工宝系统初始化数据
-- 包含字典数据初始化语句
-- ============================================================

-- ------------------------------------------------------------
-- 机构类型（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (1, 0, '1', '机构类型', NULL, 1, 1, 1);

-- 机构类型（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(2, 1, '1.1', '生产企业', 'production', 2, 1, 1),
(3, 1, '1.2', '经销商', 'distributor', 2, 2, 1),
(4, 1, '1.3', '医疗机构', 'medical', 2, 3, 1),
(5, 1, '1.4', '其他', 'other', 2, 4, 1);

-- ------------------------------------------------------------
-- 机构编码前缀（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (6, 0, '2', '机构编码前缀', NULL, 1, 2, 1);

-- 机构编码前缀（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(7, 6, '2.1', '生产企业编码前缀', 'ORG-P-', 2, 1, 1),
(8, 6, '2.2', '经销商编码前缀', 'ORG-D-', 2, 2, 1),
(9, 6, '2.3', '医疗机构编码前缀', 'ORG-H-', 2, 3, 1),
(10, 6, '2.4', '其他编码前缀', 'ORG-O-', 2, 4, 1);

-- ------------------------------------------------------------
-- 医院等级（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (11, 0, '3', '医院等级', NULL, 1, 3, 1);

-- 医院等级（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(12, 11, '3.1', '三甲', '1', 2, 1, 1),
(13, 11, '3.2', '三乙', '2', 2, 2, 1),
(14, 11, '3.3', '二甲', '3', 2, 3, 1),
(15, 11, '3.4', '二乙', '4', 2, 4, 1),
(16, 11, '3.5', '其他', '5', 2, 5, 1);

-- ------------------------------------------------------------
-- 医院类型（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (17, 0, '4', '医院类型', NULL, 1, 4, 1);

-- 医院类型（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(18, 17, '4.1', '综合', '1', 2, 1, 1),
(19, 17, '4.2', '专科', '2', 2, 2, 1);

-- ------------------------------------------------------------
-- 代理产品线（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES (20, 0, '5', '代理产品线', NULL, 1, 5, 1);

-- 代理产品线（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) 
VALUES 
(21, 20, '5.1', '医疗器械', 'medical_device', 2, 1, 1),
(22, 20, '5.2', '药品', 'drug', 2, 2, 1),
(23, 20, '5.3', '耗材', 'consumable', 2, 3, 1),
(24, 20, '5.4', '设备', 'equipment', 2, 4, 1);

-- ------------------------------------------------------------
-- 账户分类（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (25, 0, '6', '账户分类', NULL, 1, 6, 1);

-- 账户分类（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(26, 25, '6.1', '内部用户', '1', 2, 1, 1),
(27, 25, '6.2', '外部用户', '2', 2, 2, 1);

-- ------------------------------------------------------------
-- 专业方向（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (28, 0, '7', '专业方向', NULL, 1, 7, 1);

-- 专业方向（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(29, 28, '7.1', '头部方向', '1', 2, 1, 1),
(30, 28, '7.2', '颈部方向', '2', 2, 2, 1),
(31, 28, '7.3', '胸部方向', '3', 2, 3, 1),
(32, 28, '7.4', '腹部方向', '4', 2, 4, 1),
(33, 28, '7.5', '四肢方向', '5', 2, 5, 1),
(34, 28, '7.6', '背部方向', '6', 2, 6, 1),
(35, 28, '7.7', '其他', '7', 2, 7, 1);

-- ------------------------------------------------------------
-- 结算类型（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (36, 0, '8', '结算类型', NULL, 1, 8, 1);

-- 结算类型（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(37, 36, '8.1', '月薪制', '1', 2, 1, 1),
(38, 36, '8.2', '提成制', '2', 2, 2, 1),
(39, 36, '8.3', '混合制', '3', 2, 3, 1);

-- ------------------------------------------------------------
-- 性别（父节点）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (40, 0, '9', '性别', NULL, 1, 9, 1);

-- 性别（子节点）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(41, 40, '9.1', '男', '1', 2, 1, 1),
(42, 40, '9.2', '女', '0', 2, 2, 1);

-- ============================================================
-- 地区数据（sys_area）导入说明
-- 表结构与 https://github.com/kakuilan/china_area_mysql 的 cnarea_2023 兼容
-- 推荐步骤：
-- 1. 在库中先执行 sql/ddl.sql 创建 sys_area 表
-- 2. 下载并解压 cnarea_2023.sql.zip，将 cnarea_2023 表导入同一库（或另一库后同库再导）
-- 3. 仅需省/市/区三级时，可从 cnarea_2023 导入到 sys_area（补全项目公共字段）：
-- ============================================================
-- INSERT INTO sys_area (level, parent_code, area_code, zip_code, city_code, name, short_name, merger_name, pinyin, lng, lat, status, create_time, update_time, create_by, update_by, is_deleted)
-- SELECT level, parent_code, area_code, zip_code, city_code, name, short_name, merger_name, pinyin, lng, lat, 1, NOW(), NOW(), NULL, NULL, 0
-- FROM cnarea_2023
-- WHERE level IN (1, 2, 3);

-- ============================================================
-- 系统配置数据初始化
-- ============================================================
-- 初始化系统配置数据
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status) VALUES
('default.password', '默认密码', '123456', 'string', 'security', '新用户初始密码', 1, 0, 1, 1),
('login.max.failures', '最大连续登录失败次数', '5', 'number', 'security', '连续失败后锁定账号', 1, 0, 2, 1),
('login.lock.duration', '登录锁定时长', '15', 'number', 'security', '自动解锁时间（分钟）', 1, 0, 3, 1),
('sms.send.interval', '短信发送间隔', '60', 'number', 'security', '同一手机号发送间隔（秒）', 1, 0, 4, 1);


-- ============================================================
-- 资源数据初始化（sys_resource）
-- resource_type: 1=一级菜单, 2=二级菜单, 3=按钮
-- ============================================================

-- ------------------------------------------------------------
-- 一级菜单
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status) VALUES
(1, 0, '系统管理', 'system', 1, 'Setting', '/system', NULL, '/system/index', 100, 1, 1),
(2, 0, '权限管理', 'permission', 1, 'Lock', '/permission', NULL, '/permission/index', 90, 1, 1),
(3, 0, '基础数据', 'basedata', 1, 'Database', '/basedata', NULL, '/basedata/index', 80, 1, 1),
(4, 0, '客户管理', 'customer', 1, 'Hospital', '/customer', NULL, '/customer/index', 70, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单（系统管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(101, 1, '机构管理', 'system:org', 2, 'Office', '/system/org', 'system/org/index', 1, 1, 1),
(102, 1, '部门管理', 'system:dept', 2, 'Dept', '/system/dept', 'system/dept/index', 2, 1, 1),
(103, 1, '用户管理', 'system:user', 2, 'User', '/system/user', 'system/user/index', 3, 1, 1),
(104, 1, '角色管理', 'system:role', 2, 'Role', '/system/role', 'system/role/index', 4, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单（权限管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(201, 2, '资源管理', 'permission:resource', 2, 'Menu', '/permission/resource', 'permission/resource/index', 1, 1, 1),
(202, 2, '登录日志', 'permission:loginlog', 2, 'Log', '/permission/loginlog', 'permission/loginlog/index', 2, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单（基础数据）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(301, 3, '字典管理', 'basedata:dict', 2, 'Dict', '/basedata/dict', 'basedata/dict/index', 1, 1, 1),
(302, 3, '系统配置', 'basedata:config', 2, 'Config', '/basedata/config', 'basedata/config/index', 2, 1, 1),
(303, 3, '地区管理', 'basedata:area', 2, 'Map', '/basedata/area', 'basedata/area/index', 3, 1, 1),
(304, 3, '医院管理', 'basedata:hospital', 2, 'Hospital', '/basedata/hospital', 'basedata/hospital/index', 4, 1, 1),
(305, 3, '医院组合模板', 'basedata:hospital-template', 2, 'Collection', '/basedata/hospital-template', 'basedata/hospital-template/index', 5, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单（客户管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(401, 4, '用户医院分配', 'customer:user-hospital', 2, 'Link', '/customer/user-hospital', 'customer/user-hospital/index', 1, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（机构管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1001, 101, '查看机构列表', 'system:org:list', 3, 1, 1, 1),
(1002, 101, '查看机构详情', 'system:org:detail', 3, 2, 1, 1),
(1003, 101, '新增机构', 'system:org:add', 3, 3, 1, 1),
(1004, 101, '编辑机构', 'system:org:edit', 3, 4, 1, 1),
(1005, 101, '删除机构', 'system:org:delete', 3, 5, 1, 1),
(1006, 101, '启用/停用机构', 'system:org:status', 3, 6, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（部门管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1101, 102, '查看部门列表', 'system:dept:list', 3, 1, 1, 1),
(1102, 102, '查看部门详情', 'system:dept:detail', 3, 2, 1, 1),
(1103, 102, '新增部门', 'system:dept:add', 3, 3, 1, 1),
(1104, 102, '编辑部门', 'system:dept:edit', 3, 4, 1, 1),
(1105, 102, '删除部门', 'system:dept:delete', 3, 5, 1, 1),
(1106, 102, '启用/停用部门', 'system:dept:status', 3, 6, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（用户管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1201, 103, '查看用户列表', 'system:user:list', 3, 1, 1, 1),
(1202, 103, '查看用户详情', 'system:user:detail', 3, 2, 1, 1),
(1203, 103, '新增用户', 'system:user:add', 3, 3, 1, 1),
(1204, 103, '编辑用户', 'system:user:edit', 3, 4, 1, 1),
(1205, 103, '删除用户', 'system:user:delete', 3, 5, 1, 1),
(1206, 103, '重置密码', 'system:user:reset-password', 3, 6, 1, 1),
(1207, 103, '修改状态', 'system:user:status', 3, 7, 1, 1),
(1208, 103, '修改密码', 'system:user:change-password', 3, 8, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（角色管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1301, 104, '查看角色列表', 'system:role:list', 3, 1, 1, 1),
(1302, 104, '查看角色详情', 'system:role:detail', 3, 2, 1, 1),
(1303, 104, '新增角色', 'system:role:add', 3, 3, 1, 1),
(1304, 104, '编辑角色', 'system:role:edit', 3, 4, 1, 1),
(1305, 104, '删除角色', 'system:role:delete', 3, 5, 1, 1),
(1306, 104, '修改状态', 'system:role:status', 3, 6, 1, 1),
(1307, 104, '分配资源', 'system:role:assign-resource', 3, 7, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（资源管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1401, 201, '查看资源列表', 'permission:resource:list', 3, 1, 1, 1),
(1402, 201, '查看资源详情', 'permission:resource:detail', 3, 2, 1, 1),
(1403, 201, '新增资源', 'permission:resource:add', 3, 3, 1, 1),
(1404, 201, '编辑资源', 'permission:resource:edit', 3, 4, 1, 1),
(1405, 201, '删除资源', 'permission:resource:delete', 3, 5, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（登录日志）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1501, 202, '查看登录日志', 'permission:loginlog:list', 3, 1, 1, 1),
(1502, 202, '查看登录详情', 'permission:loginlog:detail', 3, 2, 1, 1),
(1503, 202, '导出登录日志', 'permission:loginlog:export', 3, 3, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（字典管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1601, 301, '查看字典列表', 'basedata:dict:list', 3, 1, 1, 1),
(1602, 301, '查看字典详情', 'basedata:dict:detail', 3, 2, 1, 1),
(1603, 301, '新增字典', 'basedata:dict:add', 3, 3, 1, 1),
(1604, 301, '编辑字典', 'basedata:dict:edit', 3, 4, 1, 1),
(1605, 301, '删除字典', 'basedata:dict:delete', 3, 5, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（系统配置）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1701, 302, '查看配置列表', 'basedata:config:list', 3, 1, 1, 1),
(1702, 302, '查看配置详情', 'basedata:config:detail', 3, 2, 1, 1),
(1703, 302, '新增配置', 'basedata:config:add', 3, 3, 1, 1),
(1704, 302, '编辑配置', 'basedata:config:edit', 3, 4, 1, 1),
(1705, 302, '删除配置', 'basedata:config:delete', 3, 5, 1, 1),
(1706, 302, '刷新配置', 'basedata:config:refresh', 3, 6, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（地区管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(2101, 303, '查看地区列表', 'basedata:area:list', 3, 1, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（医院管理）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1801, 304, '查看医院列表', 'basedata:hospital:list', 3, 1, 1, 1),
(1802, 304, '查看医院详情', 'basedata:hospital:detail', 3, 2, 1, 1),
(1803, 304, '新增医院', 'basedata:hospital:add', 3, 3, 1, 1),
(1804, 304, '编辑医院', 'basedata:hospital:edit', 3, 4, 1, 1),
(1805, 304, '修改状态', 'basedata:hospital:status', 3, 5, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（医院组合模板）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(1901, 305, '查看模板列表', 'basedata:hospital-template:list', 3, 1, 1, 1),
(1902, 305, '查看模板详情', 'basedata:hospital-template:detail', 3, 2, 1, 1),
(1903, 305, '新增模板', 'basedata:hospital-template:add', 3, 3, 1, 1),
(1904, 305, '编辑模板', 'basedata:hospital-template:edit', 3, 4, 1, 1),
(1905, 305, '修改状态', 'basedata:hospital-template:status', 3, 5, 1, 1);

-- ------------------------------------------------------------
-- 按钮权限（用户医院分配）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status) VALUES
(2001, 401, '查看用户医院列表', 'customer:user-hospital:list', 3, 1, 1, 1),
(2002, 401, '分配用户医院', 'customer:user-hospital:assign', 3, 2, 1, 1);


-- ============================================================
-- 机构基础数据初始化（sys_org）
-- ------------------------------------------------------------
INSERT INTO sys_org (id, org_name, org_code, org_type, contact, phone, status) VALUES
(1, '系统默认机构', 'ORG-DEFAULT', 1, '管理员', '13800000000', 1);

-- ============================================================
-- 角色基础数据初始化（sys_role）
-- ------------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_code, role_desc, account_type, hospital_scope_enabled, status) VALUES
(1, '超级管理员', 'admin', '系统管理员，拥有全部权限', 1, 0, 1);

-- ============================================================
-- 用户基础数据初始化（sys_user）
-- 初始密码为 BCrypt 加密后的 "123456"
-- $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- ------------------------------------------------------------
INSERT INTO sys_user (id, username, password, real_name, phone, account_type, org_id, role_id, role_name, role_code, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800000000', 1, 1, 1, '超级管理员', 'admin', 1);

-- ============================================================
-- 角色资源关联数据初始化（sys_role_resource）
-- ------------------------------------------------------------

-- 超级管理员角色（ROLE_ADMIN）关联所有资源
INSERT INTO sys_role_resource (role_id, resource_id)
SELECT 1, id FROM sys_resource;


-- ============================================================
-- 医院数据初始化（hospital）
-- 初始化一些示例医院数据
-- ============================================================
INSERT INTO hospital (id, hospital_name, hospital_code, area_id, area_name, full_area_name, hospital_level, hospital_type, contact, phone, email, address, status) VALUES
(1, '北京协和医院', 'HOS-001', 111, '东城区', '中国,北京,北京市,东城区', 1, 1, '张主任', '13800138001', 'info@pekingunion.com', '北京市东城区帅府园1号', 1),
(2, '上海市第一人民医院', 'HOS-002', 21, '上海市', '中国,上海,上海市', 2, 1, '李医生', '13800138002', 'info@shfirsthospital.com', '上海市虹口区武进路85号', 1),
(3, '浙江大学医学院附属第一医院', 'HOS-003', 311, '上城区', '中国,浙江,杭州市,上城区', 1, 1, '王医生', '13800138003', 'info@hzdu1hospital.com', '杭州市上城区庆春路79号', 1),
(4, '广东省人民医院', 'HOS-004', 411, '荔湾区', '中国,广东,广州市,荔湾区', 1, 1, '陈医生', '13800138004', 'info@gdhospital.com', '广州市荔湾区岭南大道123号', 1);


-- ============================================================
-- 医院组合模板数据初始化（hospital_group_template）
-- ============================================================
INSERT INTO hospital_group_template (id, template_name, template_code, template_desc, status, remark) VALUES
(1, '北京市医院联盟', 'TPL-HOS-001', '覆盖北京市主要三甲医院', 1, '用于北京地区业务拓展'),
(2, '华东地区医院群', 'TPL-HOS-002', '覆盖华东地区重点医院', 1, '用于华东区域业务'),
(3, '广东省医院联盟', 'TPL-HOS-003', '覆盖广东省主要医院', 1, '用于广东地区业务');


-- ============================================================
-- 医院组合模板明细数据初始化（hospital_group_template_detail）
-- ============================================================
INSERT INTO hospital_group_template_detail (id, template_id, hospital_id) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 2, 3),
(4, 3, 4);
