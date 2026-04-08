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
('order.column.config', '订单列表默认列配置', '{"id":true,"orderNo":true,"hospitalName":true,"patientName":true,"statusName":true,"createTime":true}', 'string', 'system', '订单列表默认显示的列（JSON格式）', 1, 0, 10, 1),
-- 流程状态机配置
('flow.max.audit.reject', '最大允许的审核驳回次数', '3', 'number', 'system', '审核驳回超过此次数后不再允许提交', 1, 0, 11, 1),
('flow.max.rework', '最大允许的返工次数', '2', 'number', 'system', '返工超过此次数后不再允许继续', 1, 0, 12, 1),
('flow.max.design.reject', '最大允许的设计审核驳回次数', '3', 'number', 'system', '设计审核驳回超过此次数后不再允许提交', 1, 0, 13, 1);


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


-- ============================================================
-- 机构基础数据初始化（sys_org）
-- ------------------------------------------------------------
INSERT INTO sys_org (id, org_name, org_code, org_type, contact, phone, status) VALUES
(1, '系统默认机构', 'ORG-DEFAULT', '1.1', '管理员', '13800000000', 1);

-- ============================================================
-- 角色基础数据初始化（sys_role）
-- ------------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_code, role_desc, account_type, data_scope_type, status) VALUES
(1, '超级管理员', 'admin', '系统管理员，拥有全部权限', 1, 'all', 1);

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
-- 重建部位数据初始化（rebuild_body_part）
-- 层级结构：身体区域（level=1） → 具体部位（level=2）
-- ============================================================
INSERT INTO rebuild_body_part (id, parent_id, name, code, level, designer_code, sort, status) VALUES
-- 头部区域
(1, 0, '头部', 'HEAD', 1, NULL, 1, 1),
(2, 1, '颅骨', 'HEAD-SKULL', 2, 'A', 1, 1),
(3, 1, '颌面', 'HEAD-FACE', 2, 'B', 2, 1),
(4, 1, '眼眶', 'HEAD-ORBIT', 2, 'C', 3, 1),
-- 颈部区域
(5, 0, '颈部', 'NECK', 1, NULL, 2, 1),
(6, 5, '颈椎', 'NECK-CSPINE', 2, 'D', 1, 1),
-- 胸部区域
(7, 0, '胸部', 'CHEST', 1, NULL, 3, 1),
(8, 7, '肋骨', 'CHEST-RIB', 2, 'E', 1, 1),
(9, 7, '胸骨', 'CHEST-STERNUM', 2, 'F', 2, 1),
-- 脊柱区域
(10, 0, '脊柱', 'SPINE', 1, NULL, 4, 1),
(11, 10, '颈椎', 'SPINE-CERVICAL', 2, 'D', 1, 1),
(12, 10, '胸椎', 'SPINE-THORACIC', 2, 'G', 2, 1),
(13, 10, '腰椎', 'SPINE-LUMBAR', 2, 'H', 3, 1),
-- 骨盆区域
(14, 0, '骨盆', 'PELVIS', 1, NULL, 5, 1),
(15, 14, '髂骨', 'PELVIS-ILIUM', 2, 'I', 1, 1),
(16, 14, '坐骨', 'PELVIS-ISCHIUM', 2, 'I', 2, 1),
-- 上肢区域
(17, 0, '上肢', 'UPPER-LIMB', 1, NULL, 6, 1),
(18, 17, '肩胛骨', 'UPPER-SCAPULA', 2, 'J', 1, 1),
(19, 17, '肱骨', 'UPPER-HUMERUS', 2, 'K', 2, 1),
(20, 17, '尺桡骨', 'UPPER-ULNA-RADIUS', 2, 'L', 3, 1),
(21, 17, '手骨', 'UPPER-HAND', 2, 'M', 4, 1),
-- 下肢区域
(22, 0, '下肢', 'LOWER-LIMB', 1, NULL, 7, 1),
(23, 22, '股骨', 'LOWER-FEMUR', 2, 'N', 1, 1),
(24, 22, '胫腓骨', 'LOWER-TIBIA-FIBULA', 2, 'O', 2, 1),
(25, 22, '髌骨', 'LOWER-PATELLA', 2, 'P', 3, 1),
(26, 22, '足骨', 'LOWER-FOOT', 2, 'Q', 4, 1);


-- ============================================================
-- 重建项目数据初始化（rebuild_project）
-- 层级结构：部位（body_part_id） → 重建项目（level=1） → 子重建项目（level=2）
-- ============================================================
INSERT INTO rebuild_project (id, body_part_id, parent_id, name, code, level, standard_price, urgent_price, category_code, category_name, estimated_hours, sort, status) VALUES
-- 颅骨重建
(1, 2, 0, '颅骨缺损修补', 'RP-HEAD-SKULL-001', 1, 15000.00, 22000.00, '13.1', '模型', 48.0, 1, 1),
(2, 2, 1, '3D钛网修补', 'RP-HEAD-SKULL-001-01', 2, 8000.00, 12000.00, '13.2', '导板', 8.0, 1, 1),
(3, 2, 1, 'PEEK材料修补', 'RP-HEAD-SKULL-001-02', 2, 12000.00, 18000.00, '13.2', '导板', 12.0, 2, 1),
-- 颌面重建
(4, 3, 0, '颌面部骨折复位', 'RP-HEAD-FACE-001', 1, 20000.00, 30000.00, '13.1', '模型', 72.0, 2, 1),
(5, 3, 4, '下颌骨骨折复位导板', 'RP-HEAD-FACE-001-01', 2, 6000.00, 9000.00, '13.2', '导板', 6.0, 1, 1),
-- 眼眶重建
(6, 4, 0, '眼眶骨折修复', 'RP-HEAD-ORBIT-001', 1, 18000.00, 27000.00, '13.1', '模型', 60.0, 3, 1),
-- 肋骨重建
(7, 8, 0, '肋骨骨折固定导板', 'RP-CHEST-RIB-001', 1, 12000.00, 18000.00, '13.2', '导板', 24.0, 4, 1),
-- 脊柱重建
(8, 11, 0, '颈椎融合导板', 'RP-SPINE-CERVICAL-001', 1, 25000.00, 38000.00, '13.2', '导板', 36.0, 5, 1),
(9, 12, 0, '胸椎后路固定导板', 'RP-SPINE-THORACIC-001', 1, 22000.00, 33000.00, '13.2', '导板', 32.0, 6, 1),
(10, 13, 0, '腰椎TLIF导板', 'RP-SPINE-LUMBAR-001', 1, 28000.00, 42000.00, '13.2', '导板', 40.0, 7, 1),
-- 骨盆重建
(11, 15, 0, '髂骨缺损重建', 'RP-PELVIS-ILIUM-001', 1, 30000.00, 45000.00, '13.1', '模型', 80.0, 8, 1),
(12, 16, 0, '髋臼骨折复位导板', 'RP-PELVIS-ISCHIUM-001', 1, 25000.00, 37500.00, '13.2', '导板', 48.0, 9, 1),
-- 肩胛骨重建
(13, 18, 0, '肩胛骨骨折复位导板', 'RP-UPPER-SCAPULA-001', 1, 10000.00, 15000.00, '13.2', '导板', 16.0, 10, 1),
-- 肱骨重建
(14, 19, 0, '肱骨骨折复位导板', 'RP-UPPER-HUMERUS-001', 1, 8000.00, 12000.00, '13.2', '导板', 12.0, 11, 1),
-- 尺桡骨重建
(15, 20, 0, '尺桡骨骨折复位导板', 'RP-UPPER-ULNA-RADIUS-001', 1, 9000.00, 13500.00, '13.2', '导板', 14.0, 12, 1),
-- 股骨重建
(16, 23, 0, '股骨骨折复位导板', 'RP-LOWER-FEMUR-001', 1, 14000.00, 21000.00, '13.2', '导板', 20.0, 13, 1),
(17, 23, 0, '股骨远端肿瘤假体', 'RP-LOWER-FEMUR-002', 1, 50000.00, 75000.00, '13.3', '假体', 120.0, 14, 1),
-- 胫腓骨重建
(18, 24, 0, '胫骨骨折复位导板', 'RP-LOWER-TIBIA-001', 1, 12000.00, 18000.00, '13.2', '导板', 18.0, 15, 1),
(19, 24, 0, '胫骨平台骨折复位导板', 'RP-LOWER-TIBIA-002', 1, 15000.00, 22500.00, '13.2', '导板', 24.0, 16, 1);


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
(61, 50, '10.11', '医生头像',       '10.11', 2, 11, 1);

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
