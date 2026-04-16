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
-- 系统配置数据初始化 (默认只有 system / security / others 三种分类)
-- ============================================================
-- 初始化系统配置数据
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status) VALUES
-- 安全配置
('default.password', '默认密码', '123456', 'string', 'security', '新用户初始密码', 1, 0, 1, 1),
('login.max.failures', '最大连续登录失败次数', '5', 'number', 'security', '连续失败后锁定账号', 1, 0, 2, 1),
('login.lock.duration', '登录锁定时长', '15', 'number', 'security', '自动解锁时间（分钟）', 1, 0, 3, 1),
('sms.send.interval', '短信发送间隔', '60', 'number', 'security', '同一手机号发送间隔（秒）', 1, 0, 4, 1),
-- 系统配置
('system.name', '系统名称', '医工宝', 'string', 'system', '系统显示名称', 1, 1, 5, 1),
('max.upload.size', '文件上传最大大小', '524288000', 'number', 'system', '文件上传最大大小（字节），默认 500MB', 1, 0, 6, 1),
-- 订单配置
('order.image.required', '提交订单是否必须上传影像文件', 'true', 'boolean', 'system', 'true-必须上传，false-非必填', 1, 0, 7, 1),
('order.draft.expire.days', '草稿自动过期天数', '30', 'number', 'system', '草稿超过此天数自动过期（天）', 1, 0, 8, 1),
('order.modify.window.minutes', '订单提交后修改窗口期', '10', 'number', 'system', '订单提交后允许修改的时间窗口（分钟）', 1, 0, 9, 1),
('order.column.config', '订单列表默认列配置', '{"module":"order","columns":[{"field":"orderCode","label":"订单编号","visible":true,"sort":1,"width":160,"fixed":null},{"field":"phaseName","label":"当前阶段","visible":true,"sort":2,"width":100,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"isUrgent","label":"加急","visible":true,"sort":4,"width":70,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":5,"width":90,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":6,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":7,"width":90,"fixed":null},{"field":"orgName","label":"提单机构","visible":true,"sort":8,"width":150,"fixed":null},{"field":"operatorName","label":"操作员","visible":true,"sort":9,"width":100,"fixed":null},{"field":"operatorPhone","label":"操作员电话","visible":true,"sort":10,"width":120,"fixed":null},{"field":"operatorDeptName","label":"所属部门","visible":true,"sort":11,"width":120,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":12,"width":180,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":13,"width":100,"fixed":null},{"field":"fullAreaName","label":"完整地区","visible":true,"sort":14,"width":160,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":15,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":16,"width":100,"fixed":null},{"field":"doctorPhone","label":"医生电话","visible":true,"sort":17,"width":120,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":18,"width":100,"fixed":null},{"field":"patientAge","label":"患者年龄","visible":true,"sort":19,"width":80,"fixed":null},{"field":"patientGenderName","label":"患者性别","visible":true,"sort":20,"width":80,"fixed":null},{"field":"isPostal","label":"是否邮寄","visible":true,"sort":21,"width":80,"fixed":null},{"field":"postalAddress","label":"邮寄地址","visible":true,"sort":22,"width":160,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":23,"width":100,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付时间","visible":true,"sort":24,"width":160,"fixed":null},{"field":"estimatedCost","label":"预估费用","visible":true,"sort":25,"width":100,"fixed":null},{"field":"dataEvaluationOpinion","label":"影像评估意见","visible":true,"sort":26,"width":160,"fixed":null},{"field":"rebuildProjectList","label":"重建项目","visible":true,"sort":27,"width":200,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":28,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":29,"width":150,"fixed":null}]}', 'json', 'system', '订单列表默认显示的列（JSON格式，结构为 OrderColumnConfigVO）', 1, 0, 10, 1),
-- 流程状态机配置
('flow.max.audit.reject', '最大允许的审核驳回次数', '3', 'number', 'system', '审核驳回超过此次数后不再允许提交', 1, 0, 11, 1),
('flow.max.rework', '最大允许的返工次数', '2', 'number', 'system', '返工超过此次数后不再允许继续', 1, 0, 12, 1),
('flow.max.design.reject', '最大允许的设计审核驳回次数', '3', 'number', 'system', '设计审核驳回超过此次数后不再允许提交', 1, 0, 13, 1),
-- 订单修改申请配置
-- config_value 为 JSON，结构：{ "14.1": { "name": "基础信息", "fields": [...] }, "14.2": {...}, "14.3": {...} }
-- 完整格式化版本见 .docs/技术实现/order/03_订单修改审核实现方案.md
('order.modify.field.config', '订单修改申请字段配置', '{"14.1":{"name":"基础信息","fields":[{"field":"hospitalId","label":"医院","type":"autocomplete","required":false,"group":"hospital_doctor"},{"field":"hospitalDeptId","label":"科室","type":"autocomplete","required":false,"group":"hospital_doctor"},{"field":"doctorId","label":"关联医生","type":"autocomplete","required":false,"group":"hospital_doctor"},{"field":"doctorName","label":"医生姓名","type":"text","required":false,"group":"hospital_doctor"},{"field":"doctorPhone","label":"医生电话","type":"text","required":false,"group":"hospital_doctor"},{"field":"patientName","label":"患者姓名","type":"text","required":false},{"field":"patientAge","label":"患者年龄","type":"number","required":false},{"field":"patientGender","label":"患者性别","type":"select","required":false,"options":[{"value":"12.1","label":"男"},{"value":"12.2","label":"女"}]},{"field":"isUrgent","label":"是否加急","type":"switch","required":false},{"field":"isPostal","label":"是否邮寄","type":"switch","required":false},{"field":"postalAddress","label":"邮寄地址","type":"textarea","required":false},{"field":"expectedDeliveryDate","label":"期望交付时间","type":"datetime","required":false}]},"14.2":{"name":"影像文件","fields":[{"field":"imageDataFileIds","label":"影像数据文件","type":"file","required":false},{"field":"imageReportFileIds","label":"影像报告文件","type":"file","required":false}]},"14.3":{"name":"重建项目","fields":[{"field":"items","label":"重建项目明细","type":"array","required":false,"subFields":[{"field":"bodyPartId","label":"部位","type":"select"},{"field":"projectId","label":"重建项目","type":"select"},{"field":"projectDesc","label":"项目说明","type":"textarea"},{"field":"formingRequirement","label":"成形需求","type":"textarea"},{"field":"otherRequirement","label":"其他要求","type":"textarea"}]}]}}', 'json', 'order', '订单修改申请各类型允许修改的字段配置', 1, 0, 14, 1),
-- 设计师分配配置
('design.assign.mode', '设计师分配模式', 'auto', 'string', 'system', 'auto-自动分配，manual-手动分配', 1, 0, 15, 1);


-- ============================================================
-- 资源数据初始化（sys_resource）
-- resource_type: 1=一级菜单, 2=二级菜单, 3=按钮
-- 基于 .docs/temp.md 整理的菜单权限数据
-- ============================================================

-- ------------------------------------------------------------
-- 一级菜单（按 sort 排序）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status) VALUES
(1, 0, '数据概览', 'DataBoard', 1, '&#xe62e;', '/home', NULL, '/home/index.vue', 1, 1, 1),
(2, 0, '业务运营', 'Business', 1, NULL, '/business', NULL, '/business/order', 2, 1, 1),
(3, 0, '客户管理', 'Customer', 1, NULL, '/customer', NULL, '/customer/doctor', 3, 1, 1),
(4, 0, '模块管理', 'Module', 1, NULL, '/module', NULL, '/module/org', 4, 1, 1),
(5, 0, '用户和权限', 'Auth', 1, NULL, '/auth', NULL, '/auth/account', 5, 1, 1),
(6, 0, '资料管理', 'Datum', 1, NULL, '/datum', NULL, '/datum/index.vue', 6, 1, 1),
(7, 0, '系统配置', 'System', 1, NULL, '/system', NULL, '/system/dict', 7, 1, 1),
(8, 0, '统计报表', 'Statistical', 1, '&#xe6d6;', '/statistical', NULL, '/statistical/index.vue', 8, 1, 1),
(9, 0, '备份管理', 'Backup', 1, '&#xe7a0;', '/backup', NULL, '/backup/index.vue', 9, 1, 1),
(10, 0, '个人中心', 'Personal', 1, '&#xe651;', '/personal', NULL, '/personal/index.vue', 10, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：业务运营（parent_id=2）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(101, 2, '订单管理', 'Order', 2, '&#xeb49;', '/order', 'business/order/index.vue', 1, 1, 1),
(102, 2, '我的工单', 'Design', 2, '&#xe608;', '/design', 'business/design/index.vue', 2, 1, 1),
(103, 2, '生产管理', 'Manufacture', 2, '&#xe662;', '/manufacture', 'business/manufacture/index.vue', 3, 1, 1),
(104, 2, '质检管理', 'Quality', 2, NULL, '/quality', 'business/quality/index.vue', 4, 1, 1),
(105, 2, '仓储管理', 'Storage', 2, NULL, '/storage', 'business/storage/index.vue', 5, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：客户管理（parent_id=3）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(201, 3, '医生管理', 'Doctor', 2, '&#xe813;', '/doctor', 'customer/doctor/index.vue', 1, 1, 1),
(202, 3, '科室管理', 'Department', 2, '&#xe69f;', '/department', 'customer/department/index.vue', 2, 1, 1),
(203, 3, '医院管理', 'Hospital', 2, '&#xe811;', '/hospital', 'customer/hospital/index.vue', 3, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：模块管理（parent_id=4）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(301, 4, '机构管理', 'Org', 2, '&#xe61a;', '/org', 'module/org/index.vue', 1, 1, 1),
(302, 4, '部门管理', 'Branch', 2, '&#xe62b;', '/branch', 'module/branch/index.vue', 2, 1, 1),
(303, 4, '项目管理', 'Project', 2, '&#xe620;', '/project', 'module/project/index.vue', 3, 1, 1),
(304, 4, '产品管理', 'Product', 2, '&#xe601;', '/product', 'module/product/index.vue', 4, 1, 1),
(305, 4, '物料管理', 'Material', 2, '&#xe65c;', '/material', 'module/material/index.vue', 5, 1, 1),
(306, 4, '医院范围模板管理', 'Template', 2, '&#xe605;', '/template', 'module/template/index.vue', 6, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：用户和权限（parent_id=5）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(401, 5, '账户管理', 'Account', 2, '&#xe602;', '/account', 'auth/account/index.vue', 1, 1, 1),
(402, 5, '角色管理', 'Role', 2, '&#xe6a0;', '/role', 'auth/role/index.vue', 2, 1, 1),
(403, 5, '资源管理', 'Resource', 2, '&#xe607;', '/resource', 'auth/resource/index.vue', 3, 1, 1),
(404, 5, '注册审核', 'RegReview', 2, '&#xe76a;', '/regReview', 'auth/regReview/index.vue', 4, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：系统配置（parent_id=7）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status) VALUES
(501, 7, '字典管理', 'Dict', 2, '&#xe636;', '/dict', 'system/dict/index.vue', 1, 1, 1),
(502, 7, '参数配置', 'Param', 2, '&#xe60e;', '/param', 'system/param/index.vue', 2, 1, 1),
(503, 7, '操作日志', 'Log', 2, '&#xe668;', '/log', 'system/log/index.vue', 3, 1, 1);

--- ------------------------------------------------------------
--- 按钮级别资源（resource_type=3）
--- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, status) VALUES
-- 资源管理（parent_id=403）
(1001, 403, '添加', 'resource:Add', 3, 1, 1),
(1002, 403, '编辑', 'resource:Edit', 3, 2, 1),
(1003, 403, '状态', 'resource:Status', 3, 3, 1),
(1004, 403, '删除', 'resource:Delete', 3, 4, 1),
-- 角色权限（parent_id=402）
(1005, 402, '新建角色', 'role:Add', 3, 1, 1),
(1006, 402, '编辑', 'role:Edit', 3, 2, 1),
(1007, 402, '状态', 'role:Status', 3, 3, 1),
(1008, 402, '删除', 'role:Delete', 3, 4, 1),
(1009, 402, '配置权限', 'role:config', 3, 5, 1),
-- 医院管理（parent_id=203）
(1010, 203, '添加', 'hospital:Add', 3, 1, 1),
(1011, 203, '编辑', 'hospital:Edit', 3, 2, 1),
(1012, 203, '状态', 'hospital:Status', 3, 3, 1),
(1013, 203, '删除', 'hospital:Delete', 3, 4, 1),
-- 科室管理（parent_id=202）
(1014, 202, '添加', 'hospital-dept:Add', 3, 1, 1),
(1015, 202, '编辑', 'hospital-dept:Edit', 3, 2, 1),
(1016, 202, '状态', 'hospital-dept:Status', 3, 3, 1),
(1017, 202, '删除', 'hospital-dept:Delete', 3, 4, 1),
-- 医生管理（parent_id=201）
(1018, 201, '添加', 'doctor:Add', 3, 1, 1),
(1019, 201, '编辑', 'doctor:Edit', 3, 2, 1),
(1020, 201, '状态', 'doctor:Status', 3, 3, 1),
(1021, 201, '删除', 'doctor:Delete', 3, 4, 1),
-- 医院范围模板管理（parent_id=306）
(1022, 306, '添加', 'hospital-Temp:Add', 3, 1, 1),
(1023, 306, '编辑', 'hospital-Temp:Edit', 3, 2, 1),
(1024, 306, '状态', 'hospital-Temp:Status', 3, 3, 1),
(1025, 306, '删除', 'hospital-Temp:Delete', 3, 4, 1),
-- 机构管理（parent_id=301）
(1026, 301, '添加', 'org:Add', 3, 1, 1),
(1027, 301, '编辑', 'org:Edit', 3, 2, 1),
(1028, 301, '状态', 'org:Status', 3, 3, 1),
(1029, 301, '删除', 'org:Delete', 3, 4, 1),
-- 部门管理（parent_id=302）
(1030, 302, '添加', 'department:Add', 3, 1, 1),
(1031, 302, '编辑', 'department:Edit', 3, 2, 1),
(1032, 302, '状态', 'department:Status', 3, 3, 1),
(1033, 302, '删除', 'department:Delete', 3, 4, 1),
-- 重建项目管理（parent_id=303）
(1034, 303, '添加', 'project:Add', 3, 1, 1),
(1035, 303, '编辑', 'project:Edit', 3, 2, 1),
(1036, 303, '状态', 'project:Status', 3, 3, 1),
(1037, 303, '删除', 'project:Delete', 3, 4, 1),
-- 产品管理（parent_id=304）
(1038, 304, '添加', 'product:Add', 3, 1, 1),
(1039, 304, '编辑', 'product:Edit', 3, 2, 1),
(1040, 304, '状态', 'product:Status', 3, 3, 1),
(1041, 304, '删除', 'product:Delete', 3, 4, 1),
-- 注册证管理（parent_id=305）
(1042, 305, '添加', 'registration-cert:Add', 3, 1, 1),
(1043, 305, '编辑', 'registration-cert:Edit', 3, 2, 1),
(1044, 305, '状态', 'registration-cert:Status', 3, 3, 1),
(1045, 305, '删除', 'registration-cert:Delete', 3, 4, 1),
-- 字典管理（parent_id=501）
(1046, 501, '添加', 'dict:Add', 3, 1, 1),
(1047, 501, '编辑', 'dict:Edit', 3, 2, 1),
(1048, 501, '状态', 'dict:Status', 3, 3, 1),
(1049, 501, '删除', 'dict:Delete', 3, 4, 1),
-- 参数配置（parent_id=502）
(1050, 502, '添加', 'param:Add', 3, 1, 1),
(1051, 502, '编辑', 'param:Edit', 3, 2, 1),
(1052, 502, '删除', 'param:Delete', 3, 3, 1),
-- 操作日志（parent_id=503）
(1053, 503, '导出Excel', 'log:Export', 3, 1, 1);


-- ============================================================
-- 机构基础数据初始化（sys_org）
-- ------------------------------------------------------------
INSERT INTO sys_org (id, org_name, org_code, org_type, contact, phone, status) VALUES
(1, '系统默认机构', 'ORG-DEFAULT', '1.1', '管理员', '13800000000', 1);

-- ============================================================
-- 角色基础数据初始化（sys_role）
-- ------------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_code, role_desc, account_type, data_scope_type, status) VALUES
-- 生产企业角色
(1, '超级管理员', 'admin', '系统管理员，拥有全部权限', 1, 'all', 1),
(2, '设计师', 'designer', '设计人员，负责订单设计工作', 1, 'self', 1),
(3, '设计师管理员', 'designer-manager', '设计团队管理员，负责设计工作分配、审核、统计等', 1, 'org', 1),
-- 业务员角色
(4, '业务员', 'salesman', '负责订单开拓、客户维护', 1, 'hospitals', 1),
(5, '业务管理员', 'sale-manager', '管理下属业务员', 1, 'dept', 1);



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
(1, '北京协和医院', 'HOS-001', 111, '东城区', '中国,北京,北京市,东城区', '3.1', '4.1', '张主任', '13800138001', 'info@pekingunion.com', '北京市东城区帅府园1号', 1),
(2, '上海市第一人民医院', 'HOS-002', 21, '上海市', '中国,上海,上海市', '3.2', '4.1', '李医生', '13800138002', 'info@shfirsthospital.com', '上海市虹口区武进路85号', 1),
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


-- ============================================================
-- 编码规则数据初始化（sys_code_rule）
-- ============================================================
INSERT INTO sys_code_rule (rule_code, rule_name, prefix, date_format, seq_length, reset_type, status) VALUES
-- 订单相关编码
('ORDER_NO', '订单编号', 'ORD-', '{yyyy}{MM}{dd}', 6, 'DAY', 1),
('WORK_NO', '工单编号', 'WO-', '{yyyy}{MM}', 4, 'MONTH', 1),
('INSTRUCTION_NO', '指令单编号', 'ZL-', NULL, 4, 'NEVER', 1),
('DRAWING_NO', '图纸编号', 'TZ-', NULL, 4, 'NEVER', 1),
('IMAGE_PACKAGE_NO', '影像数据包编号', 'SJB-', '{yyyy}{MM}{dd}', 4, 'DAY', 1),
('UDI', 'UDI码', 'UDI-', NULL, 8, 'NEVER', 1),
('ATTACHMENT_NO', '附件编号', 'ATT-', '{yyyy}{MM}{dd}', 6, 'DAY', 1),

-- 基础数据编码
('HOSPITAL_NO', '医院编码', 'HOS-', NULL, 4, 'NEVER', 1),
('TEMPLATE_NO', '医院组合模板编码', 'TPL-', NULL, 4, 'NEVER', 1),
('DEPT_NO', '部门编码', 'DEPT-', NULL, 4, 'NEVER', 1),
('ORG_NO', '机构编码', NULL, NULL, 4, 'NEVER', 1),
('BODYPART_NO', '重建部位编码', 'BP-', NULL, 4, 'NEVER', 1),
('PROJECT_NO', '重建项目编码', 'RP-', NULL, 4, 'NEVER', 1),
('PRODUCT_CODE', '产品型号编码', 'PROD-', NULL, 4, 'NEVER', 1),
('HDEPT_NO', '医院科室编码', 'HDEPT-', NULL, 4, 'NEVER', 1),
('DOCTOR_NO', '医生编码', 'DOC-', NULL, 4, 'NEVER', 1),
('FILE_NO', '文件编码', 'FILE-', NULL, 4, 'NEVER', 1),
('ORDER_ITEM_NO', '订单明细编码', NULL, NULL, 4, 'NEVER', 1);


-- ============================================================
-- 医院科室数据初始化（hospital_dept）
-- ============================================================
INSERT INTO hospital_dept (hospital_dept_code, hospital_dept_name, sort, status) VALUES
('HDEPT-0001', '骨科', 1, 1),
('HDEPT-0002', '口腔科', 2, 1),
('HDEPT-0003', '神经外科', 3, 1),
('HDEPT-0004', '心内科', 4, 1),
('HDEPT-0005', '普外科', 5, 1),
('HDEPT-0006', '整形科', 6, 1),
('HDEPT-0007', '康复科', 7, 1),
('HDEPT-0008', '影像科', 8, 1);


-- ============================================================
-- 编码序号同步（sys_code_rule.current_value）
-- 注意：必须在对应业务表数据插入之后执行，确保 current_value 与实际序号一致
-- ============================================================
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM hospital) WHERE rule_code = 'HOSPITAL_NO';
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM hospital_group_template) WHERE rule_code = 'TEMPLATE_NO';
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM hospital_dept) WHERE rule_code = 'HDEPT_NO';


-- ============================================================
-- 重建部位数据初始化（rebuild_body_part）
-- 平级结构，直接列出所有具体部位
-- ============================================================
INSERT INTO rebuild_body_part (id, name, code, sort, status) VALUES
(1, '颅骨', 'BP-0001', 1, 1),
(2, '颌面', 'BP-0002', 2, 1),
(3, '眼眶', 'BP-0003', 3, 1),
(4, '颈椎', 'BP-0004', 4, 1);


-- ============================================================
-- 重建项目数据初始化（rebuild_project）
-- 层级结构：部位（body_part_id） → 重建项目（level=1） → 子重建项目（level=2）
-- ============================================================
INSERT INTO rebuild_project (id, body_part_id, parent_id, name, code, level, standard_price, urgent_price, category_code, category_name, estimated_hours, sort, status, specialty) VALUES
-- 颅骨重建（body_part_id=1）
(1, 1, 0, '颅骨缺损修补', 'RP-HEAD-SKULL-001', 1, 15000.00, 22000.00, '13.1', '模型', 48.0, 1, 1, '7.1'),
(2, 1, 1, '3D钛网修补', 'RP-HEAD-SKULL-001-01', 2, 8000.00, 12000.00, '13.2', '导板', 8.0, 1, 1, '7.1'),
(3, 1, 1, 'PEEK材料修补', 'RP-HEAD-SKULL-001-02', 2, 12000.00, 18000.00, '13.2', '导板', 12.0, 2, 1, '7.1'),
-- 颌面重建（body_part_id=2）
(4, 2, 0, '颌面部骨折复位', 'RP-HEAD-FACE-001', 1, 20000.00, 30000.00, '13.1', '模型', 72.0, 2, 1, '7.2'),
(5, 2, 4, '下颌骨骨折复位导板', 'RP-HEAD-FACE-001-01', 2, 6000.00, 9000.00, '13.2', '导板', 6.0, 1, 1, '7.2');


-- ============================================================
-- 文件业务类型字典数据初始化（sys_dict）
-- dict_code 层级编码用于存储到 file_detail.object_type 字段
-- dict_value 即枚举 BizTypeEnum.dictCode 的值
-- ============================================================

-- 文件业务类型（父节点，dict_code=10）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (50, 0, '10', '文件业务类型', NULL, 1, 10, 1);

-- 文件业务类型（子节点：影像资料）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(51, 50, '10.1', '影像数据',       '10.1', 2, 1, 1),
(52, 50, '10.2', '影像报告',       '10.2', 2, 2, 1),
(53, 50, '10.3', '订单其他附件',   '10.3', 2, 3, 1);

-- 文件业务类型（子节点：设计文件）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(54, 50, '10.4', '打印文件包',     '10.4', 2, 4, 1),
(55, 50, '10.5', '设计报告',       '10.5', 2, 5, 1),
(56, 50, '10.6', '可视化模型',     '10.6', 2, 6, 1),
(57, 50, '10.7', '图纸文件',       '10.7', 2, 7, 1),
(58, 50, '10.8', '指令单文件',     '10.8', 2, 8, 1);

-- 文件业务类型（子节点：电子签名）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(59, 50, '10.9', '签名图片',       '10.9', 2, 9, 1);

-- 文件业务类型（子节点：医生相关）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(60, 50, '10.10', '医生资质证明',  '10.10', 2, 10, 1),
(61, 50, '10.11', '头像',       '10.11', 2, 11, 1);

-- 文件业务类型（子节点：医院/机构相关）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(62, 50, '10.12', '医院资质文件',  '10.12', 2, 12, 1),
(63, 50, '10.13', '医院图片',       '10.13', 2, 13, 1);

-- 文件业务类型（子节点：产品相关）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(64, 50, '10.14', '产品图片',       '10.14', 2, 14, 1);

-- 文件业务类型（子节点：注册证相关）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(65, 50, '10.15', '注册证扫描件',  '10.15', 2, 15, 1);

-- 文件业务类型（子节点：模板相关）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(66, 50, '10.16', '模板附件',       '10.16', 2, 16, 1);

-- 文件业务类型（子节点：通用）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
(67, 50, '10.17', '通用文件',       '10.17', 2, 17, 1);


-- ============================================================
-- 订单业务类型字典数据初始化（dict_code=11）
-- 设计说明：用于区分订单的业务类型（业务/测试/试用/代理）
-- 存储时使用 dict_code 值（如 11.1）而非 dict_value
-- ============================================================
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(68, 0, '11', '订单业务类型', NULL, 1, 11, 1),
-- 子节点
(69, 68, '11.1', '业务', 'business', 2, 1, 1),
(70, 68, '11.2', '测试', 'test', 2, 2, 1),
(71, 68, '11.3', '试用', 'trial', 2, 3, 1),
(72, 68, '11.4', '代理', 'agent', 2, 4, 1);


-- ============================================================
-- 患者性别字典数据初始化（dict_code=12）
-- 设计说明：用于订单中患者性别字段，区别于 sys_user.sex 使用的 dict_code=9
-- 存储时使用 dict_code 值（如 12.1）而非 dict_value
-- ============================================================
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(73, 0, '12', '患者性别', NULL, 1, 12, 1),
-- 子节点
(74, 73, '12.1', '男', 'male', 2, 1, 1),
(75, 73, '12.2', '女', 'female', 2, 2, 1);


-- ============================================================
-- 重建项目分类字典数据初始化（dict_code=13）
-- 设计说明：用于重建项目的分类标识，存储 dict_code 值（如 13.1）
-- ============================================================
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(76, 0, '13', '重建项目分类', NULL, 1, 13, 1),
-- 子节点
(77, 76, '13.1', '模型', 'model', 2, 1, 1),
(78, 76, '13.2', '导板', 'guide', 2, 2, 1),
(79, 76, '13.3', '假体', 'implant', 2, 3, 1),
(80, 76, '13.4', '其他', 'other', 2, 4, 1);

-- 订单修改申请类型字典数据初始化（dict_code=14）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(81, 0, '14', '订单修改申请类型', NULL, 1, 14, 1),
-- 子节点
(82, 81, '14.1', '基础信息', 'INFO',  2, 1, 1),
(83, 81, '14.2', '影像文件', 'IMAGE', 2, 2, 1),
(84, 81, '14.3', '重建项目', 'ITEM',  2, 3, 1);


-- ============================================================
-- 设计模式系统配置初始化
-- ============================================================
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status) VALUES
('DESIGN_MODE', '设计模式', '1', 'number', 'business', '设计模式：1=线下修改（需上传修订版），2=在线编辑', 1, 1, 100, 1);

-- 设计文件上传相关配置
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status) VALUES
('design.package.allowed_extensions', '数据包允许的文件扩展名', '.stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp', 'string', 'design', '数据包压缩包内允许的文件扩展名，逗号分隔', 0, 0, 101, 1),
('design.package.max_size_mb', '数据包最大大小', '500', 'number', 'design', '数据包文件最大大小（MB）', 0, 0, 102, 1),
('design.model.max_size_mb', '可视化模型最大大小', '200', 'number', 'design', '可视化模型文件最大大小（MB）', 0, 0, 103, 1),
('design.report.max_size_mb', '设计报告最大大小', '50', 'number', 'design', '设计报告文件最大大小（MB）', 0, 0, 104, 1),
('design.column.config', '设计工单列表默认列配置', '{"module":"design","columns":[{"field":"isUrgent","label":"加急","visible":true,"sort":1,"width":70,"fixed":null},{"field":"orderCode","label":"订单编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":4,"width":100,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":5,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":6,"width":90,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":7,"width":100,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":8,"width":180,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":9,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":10,"width":100,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":11,"width":100,"fixed":null},{"field":"rebuildProjectSummary","label":"重建项目","visible":true,"sort":12,"width":200,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":13,"width":100,"fixed":null},{"field":"packageCount","label":"数据包数","visible":true,"sort":14,"width":90,"fixed":null},{"field":"designStartTime","label":"开始设计时间","visible":true,"sort":15,"width":160,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":16,"width":120,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":17,"width":160,"fixed":null},{"field":"rejectReason","label":"驳回原因","visible":false,"sort":18,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":19,"width":150,"fixed":"right"}]}', 'json', 'system', '设计工单列表默认显示的列（JSON格式）', 1, 0, 21, 1);


-- ============================================================
-- 打印信息相关字典数据初始化
-- ============================================================

-- 材质字典（dict_code=15）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(85, 0, '15', '打印材质', NULL, 1, 15, 1),
-- 子节点
(86, 85, '15.1', '树脂', 'resin', 2, 1, 1),
(87, 85, '15.2', '尼龙', 'nylon', 2, 2, 1),
(88, 85, '15.3', '金属', 'metal', 2, 3, 1),
(89, 85, '15.4', 'PEEK', 'peek', 2, 4, 1);

-- 打印颜色字典（dict_code=16）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(90, 0, '16', '打印颜色', NULL, 1, 16, 1),
-- 子节点（categoryCode 用于按产品大类过滤）
(91, 90, '16.1', '白色', 'white', 2, 1, 1),
(92, 90, '16.2', '透明', 'transparent', 2, 2, 1),
(93, 90, '16.3', '肤色', 'skin', 2, 3, 1),
(94, 90, '16.4', '蓝色', 'blue', 2, 4, 1);

-- 产品大类字典（dict_code=17）- 用于颜色过滤
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(95, 0, '17', '产品大类', NULL, 1, 17, 1),
-- 子节点
(96, 95, '17.1', '模型类', 'MODEL', 2, 1, 1),
(97, 95, '17.2', '导板类', 'GUIDE', 2, 2, 1),
(98, 95, '17.3', '假体类', 'IMPLANT', 2, 3, 1);
