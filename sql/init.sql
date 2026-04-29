-- ============================================================
-- 医工宝系统初始化数据
-- 按表分组组织，每个表的数据集中放在一起
-- ============================================================


-- ============================================================
-- 字典数据初始化（sys_dict）
-- ============================================================

-- ------------------------------------------------------------
-- 机构类型（父节点 id=1）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (1, 0, '1', '机构类型', NULL, 1, 1, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(2, 1, '1.1', '生产企业', 'production', 2, 1, 0),
(3, 1, '1.2', '经销商', 'distributor', 2, 2, 1),
(4, 1, '1.3', '医疗机构', 'medical', 2, 3, 1);

-- ------------------------------------------------------------
-- 机构编码前缀（父节点 id=6）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (6, 0, '2', '机构编码前缀', NULL, 1, 2, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(7, 6, '2.1', '生产企业编码前缀', 'ORG-P-', 2, 1, 1),
(8, 6, '2.2', '经销商编码前缀', 'ORG-D-', 2, 2, 1),
(9, 6, '2.3', '医疗机构编码前缀', 'ORG-H-', 2, 3, 1),
(10, 6, '2.4', '其他编码前缀', 'ORG-O-', 2, 4, 1);

-- ------------------------------------------------------------
-- 医院等级（父节点 id=11）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (11, 0, '3', '医院等级', NULL, 1, 3, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(12, 11, '3.1', '三甲', '1', 2, 1, 1),
(13, 11, '3.2', '三乙', '2', 2, 2, 1),
(14, 11, '3.3', '二甲', '3', 2, 3, 1),
(15, 11, '3.4', '二乙', '4', 2, 4, 1),
(16, 11, '3.5', '其他', '5', 2, 5, 1);

-- ------------------------------------------------------------
-- 医院类型（父节点 id=17）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (17, 0, '4', '医院类型', NULL, 1, 4, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(18, 17, '4.1', '综合', '1', 2, 1, 1),
(19, 17, '4.2', '专科', '2', 2, 2, 1);

-- ------------------------------------------------------------
-- 代理产品线（父节点 id=20）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (20, 0, '5', '代理产品线', NULL, 1, 5, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(21, 20, '5.1', '医疗器械', 'medical_device', 2, 1, 1),
(22, 20, '5.2', '药品', 'drug', 2, 2, 1),
(23, 20, '5.3', '耗材', 'consumable', 2, 3, 1),
(24, 20, '5.4', '设备', 'equipment', 2, 4, 1);

-- ------------------------------------------------------------
-- 账户分类（父节点 id=25）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (25, 0, '6', '账户分类', NULL, 1, 6, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(26, 25, '6.1', '内部用户', '1', 2, 1, 1),
(27, 25, '6.2', '外部用户', '2', 2, 2, 1);

-- ------------------------------------------------------------
-- 专业方向（父节点 id=28）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (28, 0, '7', '专业方向', NULL, 1, 7, 1);

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
-- 结算类型（父节点 id=36）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (36, 0, '8', '结算类型', NULL, 1, 8, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(37, 36, '8.1', '月薪制', '1', 2, 1, 1),
(38, 36, '8.2', '提成制', '2', 2, 2, 1),
(39, 36, '8.3', '混合制', '3', 2, 3, 1);

-- ------------------------------------------------------------
-- 性别（父节点 id=40，用于 sys_user.sex）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (40, 0, '9', '性别', NULL, 1, 9, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(41, 40, '9.1', '男', '1', 2, 1, 1),
(42, 40, '9.2', '女', '0', 2, 2, 1);

-- ------------------------------------------------------------
-- 文件业务类型（父节点 id=50，dict_code=10）
-- dict_code：前端上传时 bizType 的值
-- dict_value：对应 sys_config 的配置前缀（NULL 表示无格式/大小限制）
--             约定 configPrefix.allowed_extensions / configPrefix.max_size_mb
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (50, 0, '10', '文件业务类型', NULL, 1, 10, 1);

-- 影像资料（10.1-10.3）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(51, 50, '10.1', '影像数据',      'order.image.data',   2, 1, 1),
(52, 50, '10.2', '影像报告',      'order.image.report', 2, 2, 1),
(53, 50, '10.3', '订单其他附件',  NULL,                  2, 3, 1);

-- 设计文件（10.4-10.8）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(54, 50, '10.4', '打印文件包',    'design.package', 2, 4, 1),
(55, 50, '10.5', '设计报告',      'design.report',  2, 5, 1),
(56, 50, '10.6', '可视化模型',    'design.model',   2, 6, 1),
(57, 50, '10.7', '图纸文件',      NULL,              2, 7, 1),
(58, 50, '10.8', '指令单文件',    NULL,              2, 8, 1);

-- 电子签名（10.9）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(59, 50, '10.9', '签名图片', NULL, 2, 9, 1);

-- 医生相关（10.10-10.11）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(60, 50, '10.10', '医生资质证明', NULL, 2, 10, 1),
(61, 50, '10.11', '头像',         NULL, 2, 11, 1);

-- 医院/机构相关（10.12-10.13）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(62, 50, '10.12', '机构资质文件', 'org.cert', 2, 12, 1),
(63, 50, '10.13', '医院图片',     NULL, 2, 13, 1);

-- 产品相关（10.14）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(64, 50, '10.14', '产品图片', NULL, 2, 14, 1);

-- 注册证相关（10.15）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(65, 50, '10.15', '注册证扫描件', NULL, 2, 15, 1);

-- 模板相关（10.16）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(66, 50, '10.16', '模板附件', NULL, 2, 16, 1);

-- 通用（10.17）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(67, 50, '10.17', '通用文件', NULL, 2, 17, 1);

-- 截图文件（10.18）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(99, 50, '10.18', '图纸截图', NULL, 2, 18, 1);

-- 审批文件（10.20，业务类型为测试/试用时必须上传）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(100, 50, '10.20', '免费业务审批文件', NULL, 2, 20, 1);

-- ------------------------------------------------------------
-- 订单业务类型（父节点 id=68，dict_code=11）
-- 用于区分订单的业务类型（业务/测试/试用/代理）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (68, 0, '11', '订单业务类型', NULL, 1, 11, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(69, 68, '11.1', '业务', 'business', 2, 1, 1),
(70, 68, '11.2', '测试', 'test', 2, 2, 1),
(71, 68, '11.3', '试用', 'trial', 2, 3, 1),
(72, 68, '11.4', '代理', 'agent', 2, 4, 1);

-- ------------------------------------------------------------
-- 患者性别（父节点 id=73，dict_code=12）
-- 用于订单中患者性别字段，区别于 sys_user.sex 使用的 dict_code=9
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (73, 0, '12', '患者性别', NULL, 1, 12, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(74, 73, '12.1', '男', 'male', 2, 1, 1),
(75, 73, '12.2', '女', 'female', 2, 2, 1);

-- ------------------------------------------------------------
-- 重建项目分类（父节点 id=76，dict_code=13）
-- 用于重建项目的分类标识
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (76, 0, '13', '重建项目分类', NULL, 1, 13, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(77, 76, '13.1', '模型', 'model', 2, 1, 1),
(78, 76, '13.2', '导板', 'guide', 2, 2, 1),
(79, 76, '13.3', '假体', 'implant', 2, 3, 1),
(80, 76, '13.4', '其他', 'other', 2, 4, 1);

-- ------------------------------------------------------------
-- 订单修改申请类型（父节点 id=81，dict_code=14）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (81, 0, '14', '订单修改申请类型', NULL, 1, 14, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(82, 81, '14.1', '基础信息', 'INFO', 2, 1, 1),
(83, 81, '14.2', '影像文件', 'IMAGE', 2, 2, 1),
(84, 81, '14.3', '重建项目', 'ITEM', 2, 3, 1);

-- ------------------------------------------------------------
-- 打印材质（父节点 id=85，dict_code=15）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (85, 0, '15', '打印材质', NULL, 1, 15, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(86, 85, '15.1', '树脂', 'resin', 2, 1, 1),
(87, 85, '15.2', '尼龙', 'nylon', 2, 2, 1),
(88, 85, '15.3', '金属', 'metal', 2, 3, 1),
(89, 85, '15.4', 'PEEK', 'peek', 2, 4, 1);

-- ------------------------------------------------------------
-- 打印颜色（父节点 id=90，dict_code=16）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (90, 0, '16', '打印颜色', NULL, 1, 16, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(91, 90, '16.1', '白色', '17.1', 2, 1, 1),
(92, 90, '16.2', '透明', '17.2', 2, 2, 1),
(93, 90, '16.3', '肤色', '17.1', 2, 3, 1),
(94, 90, '16.4', '蓝色', NULL,   2, 4, 1);

-- ------------------------------------------------------------
-- 产品大类（父节点 id=95，dict_code=17）
-- 用于颜色过滤
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (95, 0, '17', '产品大类', NULL, 1, 17, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(96, 95, '17.1', '模型类', 'MODEL', 2, 1, 1),
(97, 95, '17.2', '导板类', 'GUIDE', 2, 2, 1),
(98, 95, '17.3', '假体类', 'IMPLANT', 2, 3, 1);


-- ============================================================
-- 地区数据初始化（sys_area）导入说明
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
-- 系统配置数据初始化（sys_config）
-- ============================================================

-- ------------------------------------------------------------
-- 安全配置（group=security）
-- ------------------------------------------------------------
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('default.password', '默认密码', '123456', 'string', 'security', '新用户初始密码', 1, 0, 1, 1),
('login.max.failures', '最大连续登录失败次数', '5', 'number', 'security', '连续失败后锁定账号', 1, 0, 2, 1),
('login.lock.duration', '登录锁定时长', '15', 'number', 'security', '自动解锁时间（分钟）', 1, 0, 3, 1);

INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('captcha.expire.seconds',   '验证码有效期',           '300',  'number',  'security', '验证码有效期（秒）',                           1, 0, 5, 1),
('captcha.cooldown.seconds', '验证码发送冷却',          '60',   'number',  'security', '同一目标发送冷却（秒）',                       1, 0, 6, 1),
('captcha.daily.limit',      '验证码每日发送上限',      '10',   'number',  'security', '同一目标每日最大发送次数',                     1, 0, 7, 1),
('mail.from',                '发件人邮箱地址',          'hanjor666@qq.com',     'string',  'security', '发件人邮箱地址（必填，否则邮件发送失败）',     1, 0, 8, 1);

-- ------------------------------------------------------------
-- 系统配置（group=system）
-- ------------------------------------------------------------
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('system.name', '系统名称', '医工宝', 'string', 'system', '系统显示名称', 1, 1, 1, 1),
('order.image.required', '提交订单是否必须上传影像文件', 'true', 'boolean', 'system', 'true-必须上传，false-非必填', 1, 0, 2, 1),
('order.draft.expire.days', '草稿自动过期天数', '30', 'number', 'system', '草稿超过此天数自动过期（天）', 1, 0, 3, 1),
('order.modify.window.minutes', '订单提交后修改窗口期', '10', 'number', 'system', '订单提交后允许修改的时间窗口（分钟）', 1, 0, 4, 1),
('flow.max.audit.reject', '最大允许的审核驳回次数', '3', 'number', 'system', '审核驳回超过此次数后不再允许提交', 1, 0, 5, 1),
('flow.max.rework', '最大允许的返工次数', '2', 'number', 'system', '返工超过此次数后不再允许继续', 1, 0, 6, 1),
('flow.max.design.reject', '最大允许的设计审核驳回次数', '3', 'number', 'system', '设计审核驳回超过此次数后不再允许提交', 1, 0, 7, 1),
('design.assign.mode', '设计师分配模式', 'auto', 'string', 'system', 'auto-自动分配，manual-手动分配', 1, 0, 8, 1),
('design.mode', '设计模式', '2', 'number', 'system', '设计模式：1=线下修改（需上传修订版），2=在线编辑', 1, 1, 9, 1);

-- 订单列表默认列配置（独立 INSERT，避免 JSON 跨行问题）
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES ('order.column.config', '订单列表默认列配置', '{"module":"order","columns":[{"field":"orderCode","label":"订单编号","visible":true,"sort":1,"width":160,"fixed":null},{"field":"phaseName","label":"当前阶段","visible":true,"sort":2,"width":100,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"isUrgent","label":"加急","visible":true,"sort":4,"width":70,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":5,"width":90,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":6,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":7,"width":90,"fixed":null},{"field":"orgName","label":"提单机构","visible":true,"sort":8,"width":150,"fixed":null},{"field":"operatorName","label":"操作员","visible":true,"sort":9,"width":100,"fixed":null},{"field":"operatorPhone","label":"操作员电话","visible":true,"sort":10,"width":120,"fixed":null},{"field":"operatorDeptName","label":"所属部门","visible":true,"sort":11,"width":120,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":12,"width":180,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":13,"width":100,"fixed":null},{"field":"fullAreaName","label":"完整地区","visible":true,"sort":14,"width":160,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":15,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":16,"width":100,"fixed":null},{"field":"doctorPhone","label":"医生电话","visible":true,"sort":17,"width":120,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":18,"width":100,"fixed":null},{"field":"patientAge","label":"患者年龄","visible":true,"sort":19,"width":80,"fixed":null},{"field":"patientGenderName","label":"患者性别","visible":true,"sort":20,"width":80,"fixed":null},{"field":"isPostal","label":"是否邮寄","visible":true,"sort":21,"width":80,"fixed":null},{"field":"postalAddress","label":"邮寄地址","visible":true,"sort":22,"width":160,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":23,"width":100,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付时间","visible":true,"sort":24,"width":160,"fixed":null},{"field":"estimatedCost","label":"预估费用","visible":true,"sort":25,"width":100,"fixed":null},{"field":"dataEvaluationOpinion","label":"影像评估意见","visible":true,"sort":26,"width":160,"fixed":null},{"field":"rebuildProjectList","label":"重建项目","visible":true,"sort":27,"width":200,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":28,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":29,"width":150,"fixed":null}]}', 'json', 'system', '订单列表默认显示的列（JSON格式）', 1, 0, 10, 1);

-- 订单修改申请字段配置（独立 INSERT）
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES ('order.modify.field.config', '订单修改申请字段配置', '{"14.1":{"name":"基础信息","fields":[{"field":"hospitalId","label":"医院","type":"autocomplete","required":false,"group":"hospital_doctor"},{"field":"hospitalDeptId","label":"科室","type":"autocomplete","required":false,"group":"hospital_doctor"},{"field":"doctorId","label":"关联医生","type":"autocomplete","required":false,"group":"hospital_doctor"},{"field":"doctorName","label":"医生姓名","type":"text","required":false,"group":"hospital_doctor"},{"field":"doctorPhone","label":"医生电话","type":"text","required":false,"group":"hospital_doctor"},{"field":"patientName","label":"患者姓名","type":"text","required":false},{"field":"patientAge","label":"患者年龄","type":"number","required":false},{"field":"patientGender","label":"患者性别","type":"select","required":false,"options":[{"value":"12.1","label":"男"},{"value":"12.2","label":"女"}]},{"field":"isUrgent","label":"是否加急","type":"switch","required":false},{"field":"isPostal","label":"是否邮寄","type":"switch","required":false},{"field":"postalAddress","label":"邮寄地址","type":"textarea","required":false},{"field":"expectedDeliveryDate","label":"期望交付时间","type":"datetime","required":false}]},"14.2":{"name":"影像文件","fields":[{"field":"imageDataFileIds","label":"影像数据文件","type":"file","required":false},{"field":"imageReportFileIds","label":"影像报告文件","type":"file","required":false}]},"14.3":{"name":"重建项目","fields":[{"field":"items","label":"重建项目明细","type":"array","required":false,"subFields":[{"field":"bodyPartId","label":"部位","type":"select"},{"field":"projectId","label":"重建项目","type":"select"},{"field":"projectDesc","label":"项目说明","type":"textarea"},{"field":"formingRequirement","label":"成形需求","type":"textarea"},{"field":"otherRequirement","label":"其他要求","type":"textarea"}]}]}}', 'json', 'system', '订单修改申请各类型允许修改的字段配置', 1, 0, 11, 1);

-- 设计工单列表默认列配置（独立 INSERT）
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES ('design.column.config', '设计工单列表默认列配置', '{"module":"design","columns":[{"field":"isUrgent","label":"加急","visible":true,"sort":1,"width":70,"fixed":null},{"field":"orderCode","label":"订单编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":4,"width":100,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":5,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":6,"width":90,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":7,"width":100,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":8,"width":180,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":9,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":10,"width":100,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":11,"width":100,"fixed":null},{"field":"rebuildProjectSummary","label":"重建项目","visible":true,"sort":12,"width":200,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":13,"width":100,"fixed":null},{"field":"packageCount","label":"数据包数","visible":true,"sort":14,"width":90,"fixed":null},{"field":"designStartTime","label":"开始设计时间","visible":true,"sort":15,"width":160,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":16,"width":120,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":17,"width":160,"fixed":null},{"field":"rejectReason","label":"驳回原因","visible":false,"sort":18,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":19,"width":150,"fixed":"right"}]}', 'json', 'system', '设计工单列表默认显示的列（JSON格式）', 1, 0, 12, 1);

-- ------------------------------------------------------------
-- 文件配置（group=file）
-- ------------------------------------------------------------
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('max.upload.size', '文件上传最大大小', '2147483648', 'number', 'file', '文件上传最大大小（字节），默认 2GB', 1, 0, 1, 1),
('order.image.data.allowed_extensions', '影像数据包允许的文件扩展名', '.zip,.rar,.7z', 'string', 'file', '影像数据包允许的文件扩展名，逗号分隔', 0, 0, 2, 1),
('order.image.data.max_size_mb', '影像数据包最大文件大小', '500', 'number', 'file', '影像数据包上传时的最大文件大小（MB）', 0, 0, 3, 1),
('order.image.report.allowed_extensions', '影像报告允许的文件扩展名', '.pdf,.doc,.docx,.xls,.xlsx', 'string', 'file', '影像报告允许的文件扩展名，逗号分隔（pdf/word/excel）', 0, 0, 4, 1),
('order.image.report.max_size_mb', '影像报告最大文件大小', '50', 'number', 'file', '影像报告上传时的最大文件大小（MB）', 0, 0, 5, 1),
('design.package.archive_extensions', '设计数据包容器格式', '.zip,.rar,.7z,.tar', 'string', 'file', '上传数据包时允许的压缩包格式，逗号分隔', 0, 0, 6, 1),
('design.package.max_size_mb', '设计数据包最大文件大小', '500', 'number', 'file', '设计数据包压缩包上传时的最大文件大小（MB）', 0, 0, 7, 1),
('design.package.allowed_extensions', '数据包内部允许的文件类型', '.stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp', 'string', 'file', '数据包压缩包内允许的文件扩展名，逗号分隔（3D打印格式）', 0, 0, 8, 1),
('design.report.allowed_extensions', '设计报告允许的文件扩展名', '.pdf,.doc,.docx,.xls,.xlsx', 'string', 'file', '设计报告允许的文件扩展名，逗号分隔（word/pdf/excel）', 0, 0, 9, 1),
('design.report.max_size_mb', '设计报告最大文件大小', '50', 'number', 'file', '设计报告上传时的最大文件大小（MB）', 0, 0, 10, 1),
('design.model.allowed_extensions', '可视化模型允许的文件扩展名', '.stl,.obj,.ply,.3mf', 'string', 'file', '可视化模型允许的文件扩展名，逗号分隔', 0, 0, 11, 1),
('design.model.max_size_mb', '可视化模型最大文件大小', '200', 'number', 'file', '可视化模型上传时的最大文件大小（MB）', 0, 0, 12, 1),
('org.cert.allowed_extensions', '资质文件允许格式', 'zip,rar,tar,7z', 'string', 'file', '机构资质文件允许的压缩包格式，逗号分隔', 0, 0, 13, 1),
('org.cert.max_size_mb', '资质文件最大大小', '500', 'number', 'file', '机构资质文件上传时的最大文件大小（MB）', 0, 0, 14, 1);

INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('manufacturer.org.id', '生产企业机构ID', '1', 'number', 'system', '系统预设唯一生产企业机构ID，不可动态创建', 1, 0, 10, 1),
('unknown.hospital.org.id', '未知医院机构ID', '8', 'number', 'system', '提单时用于隐藏具体客户信息的占位医院ID，权限校验豁免', 1, 0, 11, 1);



-- ============================================================
-- 机构种子数据（sys_org）


-- ============================================================
-- 资源数据初始化（sys_resource）
-- ------------------------------------------------------------
-- resource_type: 1=一级菜单, 2=二级菜单, 3=按钮
-- ------------------------------------------------------------
-- 一级菜单
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status)
VALUES
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
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status)
VALUES
(101, 2, '订单管理', 'Order', 2, '&#xeb49;', '/order', 'business/order/index.vue', 1, 1, 1),
(102, 2, '我的工单', 'Design', 2, '&#xe608;', '/design', 'business/design/index.vue', 2, 1, 1),
(103, 2, '生产管理', 'Manufacture', 2, '&#xe662;', '/manufacture', 'business/manufacture/index.vue', 3, 1, 1),
(104, 2, '质检管理', 'Quality', 2, NULL, '/quality', 'business/quality/index.vue', 4, 1, 1),
(105, 2, '仓储管理', 'Storage', 2, NULL, '/storage', 'business/storage/index.vue', 5, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：客户管理（parent_id=3）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status)
VALUES
(201, 3, '医生管理', 'Doctor', 2, '&#xe813;', '/doctor', 'customer/doctor/index.vue', 1, 1, 1),
(202, 3, '科室管理', 'Department', 2, '&#xe69f;', '/department', 'customer/department/index.vue', 2, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：模块管理（parent_id=4）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status)
VALUES
(301, 4, '机构管理', 'Org', 2, '&#xe61a;', '/org', 'module/org/index.vue', 1, 1, 1),
(302, 4, '部门管理', 'Branch', 2, '&#xe62b;', '/branch', 'module/branch/index.vue', 2, 1, 1),
(303, 4, '项目管理', 'Project', 2, '&#xe620;', '/project', 'module/project/index.vue', 3, 1, 1),
(304, 4, '产品管理', 'Product', 2, '&#xe601;', '/product', 'module/product/index.vue', 4, 1, 1),
(305, 4, '物料管理', 'Material', 2, '&#xe65c;', '/material', 'module/material/index.vue', 5, 1, 1),
(306, 4, '医院范围模板管理', 'Template', 2, '&#xe605;', '/template', 'module/template/index.vue', 6, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：用户和权限（parent_id=5）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status)
VALUES
(401, 5, '账户管理', 'Account', 2, '&#xe602;', '/account', 'auth/account/index.vue', 1, 1, 1),
(402, 5, '角色管理', 'Role', 2, '&#xe6a0;', '/role', 'auth/role/index.vue', 2, 1, 1),
(403, 5, '资源管理', 'Resource', 2, '&#xe607;', '/resource', 'auth/resource/index.vue', 3, 1, 1),
(404, 5, '注册审核', 'RegReview', 2, '&#xe76a;', '/regReview', 'auth/regReview/index.vue', 4, 1, 1);

-- ------------------------------------------------------------
-- 二级菜单：系统配置（parent_id=7）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status)
VALUES
(501, 7, '字典管理', 'Dict', 2, '&#xe636;', '/dict', 'system/dict/index.vue', 1, 1, 1),
(502, 7, '参数配置', 'Param', 2, '&#xe60e;', '/param', 'system/param/index.vue', 2, 1, 1),
(503, 7, '操作日志', 'Log', 2, '&#xe668;', '/log', 'system/log/index.vue', 3, 1, 1);

-- ------------------------------------------------------------
-- 按钮级别资源（resource_type=3）
-- ------------------------------------------------------------
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, status)
VALUES
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
-- ============================================================
INSERT INTO sys_org (id, org_name, org_code, org_type, contact, phone, status)
VALUES
(1, '嘉一高科（内部）', 'ORG-0001', '1.1', '管理员', '13800000000', 1);


-- ============================================================
-- 机构种子数据（sys_org）
-- area_id 通过子查询按省级 area_code 获取，避免硬编码自增 id
-- 省级行政区划代码（area_code）：北京=110000，上海=310000，浙江=330000，广东=440000
-- ============================================================

-- 经销商（orgType=1.2）
INSERT INTO sys_org (id, org_name, org_code, org_type, area_id, area_name, contact, phone, status)
VALUES
(2, '北京医疗器械经销有限公司', 'ORG-D-0001', '1.2',
    (SELECT id FROM sys_area WHERE area_code = 110000 LIMIT 1), '北京市',
    '张经理', '13800000002', 1),
(3, '广东医疗科技经销有限公司', 'ORG-D-0002', '1.2',
    (SELECT id FROM sys_area WHERE area_code = 440000 LIMIT 1), '广东省',
    '李经理', '13800000003', 1);

-- 医疗机构（orgType=1.3）
INSERT INTO sys_org (id, org_name, org_code, org_type, area_id, area_name, contact, phone, status)
VALUES
(4, '北京协和医院',               'ORG-H-0001', '1.3',
    (SELECT id FROM sys_area WHERE area_code = 110000 LIMIT 1), '北京市',
    '张主任', '13800000004', 1),
(5, '上海市第一人民医院',         'ORG-H-0002', '1.3',
    (SELECT id FROM sys_area WHERE area_code = 310000 LIMIT 1), '上海市',
    '李医生', '13800000005', 1),
(6, '浙江大学医学院附属第一医院', 'ORG-H-0003', '1.3',
    (SELECT id FROM sys_area WHERE area_code = 330000 LIMIT 1), '浙江省',
    '王医生', '13800000006', 1),
(7, '广东省人民医院',             'ORG-H-0004', '1.3',
    (SELECT id FROM sys_area WHERE area_code = 440000 LIMIT 1), '广东省',
    '陈医生', '13800000007', 1),
(8, '未知医院',                   'ORG-H-0000', '1.3',
    NULL, NULL, NULL, NULL, 1);

-- 经销商-医疗机构关联（sys_org_hospital）
INSERT INTO sys_org_hospital (org_id, hospital_org_id)
VALUES
(2, 4), (2, 5),
(3, 6), (3, 7);


-- ============================================================
-- 角色基础数据初始化（sys_role）
-- ============================================================
INSERT INTO sys_role (id, role_name, role_code, role_desc, account_type, data_scope_type, status)
VALUES
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
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, phone, account_type, org_id, role_id, role_name, role_code, status)
VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800000000', 1, 1, 1, '超级管理员', 'admin', 1);


-- ============================================================
-- 角色资源关联数据初始化（sys_role_resource）
-- ============================================================

-- 超级管理员角色关联所有资源
INSERT INTO sys_role_resource (role_id, resource_id)
SELECT 1, id FROM sys_resource;


-- ============================================================
-- 医院组合模板数据初始化（hospital_group_template）
-- ============================================================
INSERT INTO hospital_group_template (id, template_name, template_code, template_desc, status, remark)
VALUES
(1, '北京市医院联盟', 'TPL-0001', '覆盖北京市主要三甲医院', 1, '用于北京地区业务拓展'),
(2, '华东地区医院群', 'TPL-0002', '覆盖华东地区重点医院',   1, '用于华东区域业务'),
(3, '广东省医院联盟', 'TPL-0003', '覆盖广东省主要医院',     1, '用于广东地区业务');


-- ============================================================
-- 医院组合模板明细数据初始化（hospital_group_template_detail）
-- ============================================================
INSERT INTO hospital_group_template_detail (id, template_id, hospital_id)
VALUES
(1, 1, 4),
(2, 2, 5),
(3, 2, 6),
(4, 3, 7);


-- ============================================================
-- 编码规则数据初始化（sys_code_rule）
-- 仅保留代码中实际调用的规则（通过 CodeRuleConstants 或硬编码字符串调用）
-- ============================================================
INSERT INTO sys_code_rule (rule_code, rule_name, prefix, date_format, seq_length, reset_type, status)
VALUES
-- 订单相关编码
('ORDER_NO',       '订单编号',       NULL,   '{yyyy}{MM}{dd}', 6, 'DAY',   1),  -- OrderMainServiceImpl
('INSTRUCTION_NO', '指令单编号',     'ZL-',    NULL,             4, 'NEVER', 1),  -- DesignDocServiceImpl
('DATA_PACKAGE_NO','数据包编号',     NULL,     NULL,             4, 'NEVER', 1),  -- DesignFileServiceImpl（generateWithSeqSuffix，格式：{orderCode}-1/-2/-N，prefix/dateFormat 不参与生成）
-- 基础数据编码
('ORG_NO',         '机构编码',       NULL,     NULL,             4, 'NEVER', 1),  -- OrgServiceImpl（generateWithCustomPrefix，运行时拼接前缀）
('HDEPT_NO',       '医院科室编码',   'HDEPT-', NULL,             4, 'NEVER', 1),  -- HospitalDeptServiceImpl
('TEMPLATE_NO',    '医院组合模板编码','TPL-',  NULL,             4, 'NEVER', 1),  -- HospitalGroupTemplateServiceImpl
('BODYPART_NO',    '重建部位编码',   'BP-',    NULL,             4, 'NEVER', 1),  -- BodyPartServiceImpl
('PROJECT_NO',     '重建项目编码',   'RP-',    NULL,             4, 'NEVER', 1),  -- RebuildProjectServiceImpl
-- 系统模块编码
('DEPT_NO',        '部门编码',       'DEPT-',  NULL,             4, 'NEVER', 1);  -- DeptServiceImpl


-- ============================================================
-- 医院科室数据初始化（hospital_dept）
-- ============================================================
INSERT INTO hospital_dept (hospital_dept_code, hospital_dept_name, sort, status)
VALUES
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
-- 编码生成器首次调用时以此值为初始序号，确保新生成的编码不与已有种子数据冲突
-- 注意：必须在对应业务表数据插入之后执行
-- ============================================================
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM hospital_group_template) WHERE rule_code = 'TEMPLATE_NO';
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM hospital_dept)          WHERE rule_code = 'HDEPT_NO';
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM rebuild_body_part)      WHERE rule_code = 'BODYPART_NO';
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM rebuild_project)        WHERE rule_code = 'PROJECT_NO';
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM sys_org)                WHERE rule_code = 'ORG_NO';


-- -- ============================================================
-- -- 重建部位数据初始化（rebuild_body_part）
-- -- 平级结构，直接列出所有具体部位
-- -- ============================================================
INSERT INTO rebuild_body_part (id, name, code, sort, status, remark, scope, create_time, update_time, create_by, update_by, is_deleted)
VALUES
(1, '头颈', 'BP-0001', 0, 1, NULL, '{"x":0.3807583867458058,"y":0.03811484718472379,"w":0.28001296039804896,"h":0.16117593298799132}', NOW(), NOW(), 1, 1, 0),
(2, '腹部', 'BP-0002', 1, 1, NULL, '{"x":0.2840872456560032,"y":0.3577329175973382,"w":0.49668965594415837,"h":0.165454940058469}', NOW(), NOW(), 1, 1, 0);


-- -- ============================================================
-- -- 重建项目数据初始化（rebuild_project）
-- -- 层级结构：部位（body_part_id） → 重建项目（level=1） → 子重建项目（level=2）
-- -- ============================================================
INSERT INTO rebuild_project (id, body_part_id, parent_id, name, code, level, standard_price, urgent_price, category_code, category_name, estimated_hours, description, forming_requirements, sort, status, specialty, remark, create_time, update_time, create_by, update_by, is_deleted)
VALUES
-- 头颈重建（body_part_id=1）
(1, 1, 0, '头部测试项目1', 'RP-0001', 1, NULL, NULL, '13.1', '模型', NULL, '头部测试项目1项目说明1111111', '头部测试项目1成型需求2222222', 0, 1, '7.1', NULL, NOW(), NOW(), 1, 1, 0),
(2, 1, 0, '颈部测试项目1', 'RP-0002', 1, NULL, NULL, '13.2', '导板', NULL, '颈部测试项目1项目说明1111111', '颈部测试项目1成型续期2222', 1, 1, '7.2', NULL, NOW(), NOW(), 1, 1, 0),
-- 腹部重建（body_part_id=2）
(3, 2, 0, '腹部项目A', 'RP-0003', 1, NULL, NULL, '13.1', '模型', NULL, NULL, NULL, 0, 1, '7.4', NULL, NOW(), NOW(), 1, 1, 0);


-- ============================================================
-- 注册证数据初始化（registration_cert）
-- ============================================================
INSERT INTO registration_cert (id, cert_code, cert_name, valid_from, valid_to, status)
VALUES
(1, '国械注准20243130001', '颅骨修补模型注册证', '2024-01-01', '2029-12-31', 1),
(2, '国械注准20243130002', '颌面导板注册证',     '2024-01-01', '2029-12-31', 1),
(3, '国械注准20243130003', '颅骨修补假体注册证', '2024-01-01', '2029-12-31', 1);


-- ============================================================
-- 产品数据初始化（product）
-- 覆盖三大类：17.1 模型类 / 17.2 导板类 / 17.3 假体类
-- ============================================================
INSERT INTO product (id, product_name, category, category_name, status)
VALUES
(1, '颅骨修补模型', '17.1', '模型类', 1),
(2, '颌面导板', '17.2', '导板类', 1),
(3, '颅骨修补假体', '17.3', '假体类', 1);


-- ============================================================
-- 产品规格数据初始化（product_spec）
-- cert_id 引用 registration_cert.id；cert_no 为冗余字段
-- ============================================================
INSERT INTO product_spec (id, product_id, spec_name, cert_id, cert_no, sort, status)
VALUES
-- 颅骨修补模型（product_id=1）规格
(1, 1, 'S号', 1, '国械注准20243130001', 1, 1),
(2, 1, 'M号', 1, '国械注准20243130001', 2, 1),
(3, 1, 'L号', 1, '国械注准20243130001', 3, 1),
-- 颌面导板（product_id=2）规格
(4, 2, '标准型', 2, '国械注准20243130002', 1, 1),
(5, 2, '定制型', 2, '国械注准20243130002', 2, 1),
-- 颅骨修补假体（product_id=3）规格
(6, 3, '小号', 3, '国械注准20243130003', 1, 1),
(7, 3, '大号', 3, '国械注准20243130003', 2, 1);

-- part_colors 初始数据（从 image-3d-ai 迁移，opacity 统一默认 1.00）
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (1, '右肺上叶', '170,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (2, '右肺上叶尖段', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (3, '右肺上叶前段', '255,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (4, '右肺上叶后段', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (5, '右肺中叶', '85,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (6, '右肺中叶外侧段', '85,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (7, '右肺中叶内侧段', '85,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (8, '右肺下叶', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (9, '右肺下叶背段', '170,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (10, '右肺下叶内基底段', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (11, '右肺下叶前基底段', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (12, '右肺下叶外基底段', '255,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (13, '右肺下叶后基底段', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (14, '左肺上叶', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (15, '左肺上叶尖后段', '85,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (16, '左肺上叶前段', '0,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (17, '左肺上叶上舌段', '0,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (18, '左肺上叶下舌段', '0,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (19, '左肺下叶', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (20, '左肺下叶背段', '170,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (21, '左肺下叶内前基底段', '170,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (22, '左肺下叶外基底段', '170,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (23, '左肺下叶后基底段', '170,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (24, '肺', '85,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (25, '左肺', '85,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (26, '右肺', '170,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (27, '右肺上叶后段a', '170,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (29, '右肺上叶后段b', '255,170,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (31, '右肺上叶前段a', '170,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (33, '右肺上叶前段b', '210,120,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (35, '右肺上叶尖段a', '255,120,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (37, '右肺上叶尖段b', '255,185,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (39, '右肺下叶中叶外侧段a', '85,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (41, '右肺下叶中叶外侧段b', '0,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (43, '右肺下叶中叶内侧段a', '170,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (45, '右肺下叶中叶内侧段b', '40,75,155', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (47, '右肺下叶背段a', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (49, '右肺下叶背段b', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (51, '右肺下叶背段c', '85,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (53, '右肺下叶后基底段a', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (55, '右肺下叶后基底段b', '85,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (57, '右肺下叶后基底段c', '225,125,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (59, '右肺下叶内基底段a', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (61, '右肺下叶内基底段b', '225,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (63, '右肺下叶前基底段a', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (65, '右肺下叶前基底段b', '255,127,85', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (67, '右肺下叶外基底段a', '170,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (69, '右肺下叶外基底段b', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (71, '左肺上叶尖后段a', '85,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (73, '左肺上叶尖后段b', '0,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (75, '左肺上叶尖后段c', '0,225,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (77, '左肺上叶前段a', '0,200,120', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (79, '左肺上叶前段b', '85,255,185', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (81, '左肺上叶前段c', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (83, '左肺上叶上舌段a', '0,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (85, '左肺上叶上舌段b', '85,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (87, '左肺上叶下舌段a', '127,160,225', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (89, '左肺上叶下舌段b', '105,85,225', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (91, '左肺下叶背段a', '125,125,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (93, '左肺下叶背段b', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (95, '左肺下叶背段c', '170,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (97, '左肺下叶后基底段a', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (99, '左肺下叶后基底段b', '255,100,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (101, '左肺下叶后基底段c', '200,0,165', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (103, '左肺下叶内前基底段a', '170,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (105, '左肺下叶内前基底段b', '120,65,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (107, '左肺下叶外基底段a', '170,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (109, '左肺下叶外基底段b', '120,0,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (111, '左肺下叶异常段a', '170,80,160', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (112, '左肺下叶异常段b', '125,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (113, '左肺上叶异常段', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (114, '右肺奇叶', '255,223,126', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (115, '气管', '255,255,230', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (116, '右肺上叶异常段', '170,80,160', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (117, '左肺上叶外侧段', '0,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (118, '右肺上叶尖后段', '255,150,240', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (119, '肝', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (120, '左半肝', '170,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (121, '肝左外叶', '170,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (122, '右半肝', '85,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (123, '肝Ⅰ段', '170,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (124, '肝Ⅱ段', '255,220,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (125, '肝Ⅲ段', '200,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (126, '肝Ⅳ段', '255,255,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (127, '肝Ⅴ段', '200,200,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (128, '右肝上叶', '200,200,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (129, '肝Ⅵ段', '255,200,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (130, '肝Ⅶ段', '255,200,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (131, '肝Ⅷ段', '200,255,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (132, '右肝下叶', '200,255,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (133, '脾脏', '85,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (134, '副脾', '85,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (135, '胰腺', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (136, '胆囊', '0,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (137, '胆总管', '0,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (138, '胰管', '85,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (139, '胃', '170,160,60', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (140, '胃内壁', '127,127,55', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (141, '肝内胆管扩张', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (142, '胆囊壁', '0,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (143, '食道', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (144, '食管', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (145, '右肝后叶', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (146, '右肝前叶', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (147, '肝Ⅳa段', '170,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (148, '肝Ⅳb段', '170,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (149, '病变所在半肝', '255,120,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (150, '残余半肝', '255,185,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (151, '膀胱', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (152, '肾乳头', '170,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (153, '子宫', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (154, '附件', '255,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (155, '前列腺', '85,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (156, '精囊', '255,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (157, '肾', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (158, '输尿管', '0,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (159, '肾盂', '0,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (160, '肾上腺', '255,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (161, '精囊腺', '255,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (162, '前列腺', '85,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (163, '膀胱壁', '255,85,85', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (164, '阴茎', '255,240,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (165, '尿道', '0,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (166, '海绵体', '255,180,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (167, '睾丸', '240,240,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (168, '脑干', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (169, '大脑', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (170, '脑', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (171, '闭孔神经', '170,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (172, '三叉神经', '255,230,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (173, '甲状腺', '180,255,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (174, '侧脑室', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (175, '面听神经', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (176, '听神经', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (177, '泪腺', '185,250,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (178, '视神经', '255,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (179, '眼球', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (180, '小脑', '170,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (181, '小脑幕', '170,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (182, '腮腺', '180,255,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (183, '臂丛神经', '255,230,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (184, '垂体组织', '85,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (185, '视交叉', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (186, 'Ⅰa区淋巴结', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (187, 'Ⅰb区淋巴结', '0,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (188, 'Ⅱb区淋巴结', '220,240,100', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (189, 'Ⅱa区淋巴结', '230,240,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (190, 'Ⅲ区淋巴结', '165,195,195', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (191, 'Ⅳ区淋巴结', '140,190,140', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (192, 'Ⅴa区淋巴结', '140,220,40', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (193, 'Ⅴb区淋巴结', '240,220,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (194, 'Ⅵ区淋巴结', '210,190,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (195, 'Ⅶ区淋巴结', '30,160,130', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (196, '迷走神经', '250,220,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (197, '鼻中隔', '222,198,194', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (198, '鼻甲', '230,220,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (199, '上颌窦', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (200, '筛窦', '255,230,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (201, '脑脊液', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (202, '漏口', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (203, '额窦', '180,255,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (204, '蝶窦', '0,200,150', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (205, '附件', '255,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (206, '子宫', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (207, '子宫及附件', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (208, '宫颈残端', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (209, '子宫主韧带', '255,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (210, '阴道', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (211, '宫腔', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (212, '脐带', '170,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (213, '胎盘', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (214, '胎儿皮肤', '255,240,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (215, '直肠', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (216, '肠道', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (217, '肠子', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (218, '回肠', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (219, '升结肠', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (220, '乙状结肠', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (221, '肠道', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (222, '十二指肠等肠道系统', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (223, '脑容积', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (224, '咽部', '255,255,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (225, '乳腺', '255,200,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (226, '颈动脉闭塞', '100,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (227, '颈动脉狭窄', '150,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (228, '血管', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (229, '门静脉', '0,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (230, '动脉', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (231, '真腔', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (232, '下腔静脉', '0,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (233, '静脉', '0,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (234, '肝静脉', '0,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (235, '奇静脉', '0,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (236, '动脉管腔', '255,170,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (237, '上腔静脉', '0,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (238, '肺动脉', '0,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (239, '肺静脉', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (240, '肺血管', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (241, '胸主动脉', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (242, '胸主动脉外层', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (243, '血管曲张（相关曲张）', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (244, '未知血管', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (245, '占位血管', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (246, '迂曲血管团', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (247, '迂曲血管', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (248, '胸主动脉内层', '52,52,52', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (249, '脑干', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (250, '大脑', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (251, '脑', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (252, '闭孔神经', '170,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (253, '三叉神经', '255,230,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (254, '甲状腺', '180,255,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (255, '侧脑室', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (256, '面听神经', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (257, '听神经', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (258, '泪腺', '185,250,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (259, '视神经', '255,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (260, '眼球', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (261, '小脑', '170,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (262, '小脑幕', '170,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (263, '腮腺', '180,255,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (264, '臂丛神经', '255,230,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (265, '垂体组织', '85,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (266, '视交叉', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (267, 'Ⅰa区淋巴结', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (268, 'Ⅰb区淋巴结', '0,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (269, 'Ⅱb区淋巴结', '220,240,100', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (270, 'Ⅱa区淋巴结', '230,240,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (271, 'Ⅲ区淋巴结', '165,195,195', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (272, 'Ⅳ区淋巴结', '140,190,140', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (273, 'Ⅴa区淋巴结', '140,220,40', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (274, 'Ⅴb区淋巴结', '240,220,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (275, 'Ⅵ区淋巴结', '210,190,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (276, 'Ⅶ区淋巴结', '30,160,130', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (277, '迷走神经', '250,220,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (278, '鼻中隔', '222,198,194', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (279, '鼻甲', '230,220,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (280, '上颌窦', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (281, '筛窦', '255,230,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (282, '脑脊液', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (283, '漏口', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (284, '额窦', '180,255,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (285, '蝶窦', '0,200,150', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (286, '附件', '255,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (287, '子宫', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (288, '子宫及附件', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (289, '宫颈残端', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (290, '子宫主韧带', '255,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (291, '阴道', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (292, '宫腔', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (293, '脐带', '170,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (294, '胎盘', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (295, '胎儿皮肤', '255,240,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (296, '直肠', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (297, '肠道', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (298, '肠子', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (299, '回肠', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (300, '升结肠', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (301, '乙状结肠', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (302, '肠道', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (303, '十二指肠等肠道系统', '85,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (304, '脑容积', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (305, '咽部', '255,255,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (306, '乳腺', '255,200,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (307, '颈动脉闭塞', '100,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (308, '颈动脉狭窄', '150,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (309, '血管', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (310, '门静脉', '0,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (311, '动脉', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (312, '真腔', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (313, '下腔静脉', '0,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (314, '静脉', '0,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (315, '肝静脉', '0,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (316, '奇静脉', '0,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (317, '动脉管腔', '255,170,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (318, '上腔静脉', '0,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (319, '肺动脉', '0,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (320, '肺静脉', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (321, '肺血管', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (322, '胸主动脉', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (323, '胸主动脉外层', '255,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (324, '血管曲张（相关曲张）', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (325, '未知血管', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (326, '占位血管', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (327, '迂曲血管团', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (328, '迂曲血管', '210,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (329, '胸主动脉内层', '52,52,52', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (330, '渗液', '255,255,165', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (331, '积液', '255,255,165', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (332, '肺大泡', '255,160,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (333, '胆囊息肉（相关息肉）', '225,165,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (334, '环形强化影', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (335, '积水', '55,127,155', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (336, '腹水', '55,127,155', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (337, '水肿', '55,127,155', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (338, '异常密度影', '55,127,155', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (339, '软组织密度影', '55,127,155', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (340, '软化灶', '155,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (341, '导管', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (342, '引流管', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (343, '内固定', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (344, '节育器', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (345, '造瘘管', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (346, '瘘管', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (347, '低信号', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (348, '低密度', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (349, '低密度灶', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (350, '低密度影', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (351, '无强化灶', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (352, '可疑充盈缺损', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (353, '脂肪间隙模糊影', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (354, '瘤栓', '225,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (355, '瘘道', '255,245,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (356, '高密度', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (357, '高密度灶', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (358, '高密度影', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (359, '强化灶', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (360, '子灶', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (361, '致密影', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (362, '条索影', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (363, '增殖灶', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (364, '混杂密度影', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (365, '骨质破坏', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (366, '游离体', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (367, '片状影', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (368, '室间隔缺损', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (369, '碘油', '255,155,75', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (370, '碘油沉积', '255,155,75', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (371, '异常灌注', '255,155,75', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (372, '破口', '85,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (373, '疑似破口', '85,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (374, '梗死灶', '127,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (375, '梗阻(相关梗阻）', '127,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (376, '囊肿', '85,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (377, '结节', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (378, '动脉期强化', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (379, '钙化', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (380, '斑块（相关斑块）', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (381, '结石', '255,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (382, '异物', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (383, '肿块', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (384, '肿物', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (385, '胆总管上端受累', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (386, '胆囊管受累', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (387, '淋巴结', '170,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (388, '占位', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (389, '肠壁增厚', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (390, '肌瘤', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (391, '动脉瘤', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (392, '血管瘤', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (393, '壁增厚', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (394, '子宫腺肌瘤', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (395, '转移灶', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (396, '假腔', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (397, '胸膜增厚', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (398, '混杂密度影', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (399, '子宫腺肌症', '255,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (400, '膀胱憩室', '255,210,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (401, '憩室（相关憩室）', '255,210,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (402, '栓子', '0,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (403, '癌栓', '0,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (404, '栓塞', '0,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (405, '脑梗塞', '85,127,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (406, '胆囊息肉（相关息肉）', '225,165,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (407, '出血', '127,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (408, '血块', '127,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (409, '血块', '127,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (410, '血栓（相关血栓）', '127,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (411, '积气', '255,255,230', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (412, '气胸', '255,255,230', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (413, '硬化灶', '255,85,85', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (414, '肝内钙化', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (415, '脾内钙化', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (416, '胰内钙化', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (417, '动脉钙化', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (418, '疑似斑块', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (419, '肝囊肿', '85,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (420, '脾囊肿', '85,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (421, '肾囊肿（相关囊肿）', '85,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (422, '囊性灶', '85,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (423, '分房囊腔', '85,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (424, '炎症', '105,158,158', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (425, '肺隔离症', '105,158,158', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (426, '肝内胆管结石', '255,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (427, '胆总管结石', '255,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (428, '胆结石（相关结石）', '255,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (429, '良性病变', '255,255,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (430, '肝脏凸起', '230,170,100', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (431, '术后改变', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (432, '肺气肿', '255,160,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (433, '磨玻璃结节', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (434, '射频范围', '170,170,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (435, '脂肪结节', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (436, '磨玻璃影', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (437, '肺不张', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (438, '空洞', '0,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (439, '磨玻璃', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (440, '肺气囊', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (441, '支气管堵塞', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (442, '肺结核', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (443, '感染', '170,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (444, '粘液栓', '0,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (445, '疑似黏液栓（相关栓子）', '0,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (446, '软化斑', '105,158,158', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (447, '支架', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (448, '造影剂', '230,230,230', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (449, '腹股沟疝', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (450, '肝脏边缘凸起密度影', '85,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (451, '渗出（疑似渗出等相关渗出）', '255,255,165', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (452, '闭塞段', '127,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (453, '骨质缺损', '255,255,20', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (454, '海绵窦', '0,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (455, '团块', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (456, '团块影', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (457, '游离气体影', '255,255,230', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (458, '黏连带', '170,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (459, '良性病变', '255,255,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (460, '脂肪瘤', '255,255,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (461, '透亮影', '255,255,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (462, '脂肪密度灶', '255,255,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (463, '动静脉瘘', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (464, '疑似动静脉瘘', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (465, '植入物（相关植入物）', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (466, '膨胀不全', '170,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (467, '缝合线', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (468, '人工颅板', '255,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (469, '吻合口', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (470, '吻合口区', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (471, '扩张支气管', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (472, '栓塞剂', '230,230,230', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (473, '网膜侵犯', '255,85,160', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (474, '脓肿', '85,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (475, '蛛网膜颗粒', '180,180,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (476, '疝囊', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (477, '前列腺外周带', '170,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (478, '前列腺中央带', '0,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (479, '甲状旁腺', '127,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (480, '占位实质成分', '170,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (481, '憩室入口', '255,127,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (482, '片状影', '255,255,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (483, '畸形', '0,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (484, '出血灶', '85,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (485, '环状软骨', '255,255,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (486, '甲状软骨角', '255,255,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (487, '甲状腺软骨板', '255,255,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (488, '舌骨', '255,230,210', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (489, '胸膜锁乳突肌', '210,150,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (490, '腹膜', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (491, '喉返神经', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (492, '肛瘘', '200,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (493, '肛瘘', '200,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (494, '瘘道', '200,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (495, '窦道', '200,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (496, '肛管', '200,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (497, '内异灶', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (498, '脓肿', '240,240,20', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (499, '肾上腺增粗', '0,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (500, '肾上腺增大', '0,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (501, '宫颈残端', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (502, '子宫肌瘤', '85,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (503, '积血', '160,0,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (504, '挫裂伤', '170,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (505, '喉上神经', '255,200,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (506, 'RS1', '255,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (507, 'RS3', '255,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (508, 'RS2', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (509, 'RS4', '85,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (510, 'RS5', '85,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (511, 'RS6', '170,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (512, 'RS7', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (513, 'RS8', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (514, 'RS9', '255,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (515, 'RS10', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (516, 'LS1+2', '85,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (517, 'LS3', '0,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (518, 'LS4', '0,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (519, 'LS5', '0,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (520, 'LS6', '170,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (521, 'LS7+8', '170,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (522, 'LS9', '170,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (523, 'LS10', '170,0,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (524, 'RS6a', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (525, 'RS6b', '255,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (526, 'RS6c', '85,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (527, 'RS1a', '170,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (528, 'RS1b', '255,170,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (529, 'RS2a', '170,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (530, 'RS2b', '210,120,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (531, 'RS3a', '255,120,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (532, 'RS3b', '255,185,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (533, 'RS4a', '85,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (534, 'RS4b', '0,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (535, 'RS5a', '170,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (536, 'RS5b', '40,75,155', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (537, 'LS1+2b', '0,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (538, 'LS1+2c', '0,225,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (539, 'LS3a', '0,200,120', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (540, 'RS7a', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (541, 'RS7b', '85,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (542, 'RS7c', '225,125,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (543, 'RS8a', '255,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (544, 'RS8b', '225,200,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (545, 'RS9b', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (546, 'RS9a', '255,127,85', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (547, 'RS10a', '170,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (548, 'RS10b', '255,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (549, 'LS1+2a', '85,255,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (550, 'LS8b', '255,100,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (551, 'LS8c', '200,0,165', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (552, 'LS9a', '170,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (553, 'LS3b', '85,255,185', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (554, 'LS3c', '170,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (555, 'LS4a', '0,170,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (556, 'LS4b', '85,85,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (557, 'LS5b', '127,160,225', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (558, 'LS5a', '105,85,225', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (559, 'LS6a', '125,125,180', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (560, 'LS7b', '85,85,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (561, 'LS7c', '170,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (562, 'LS8a', '255,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (563, 'LS9b', '120,65,190', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (564, 'LS10a', '170,0,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (565, 'LS10b', '120,0,170', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (566, '肾脏', '255,170,127', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (567, '肝脏', '170,85,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (568, 'S1', '170,170,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (569, 'S2', '255,220,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (570, 'S3', '200,255,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (571, 'S4', '255,255,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (572, 'S5', '200,200,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (573, 'S6', '255,200,200', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (574, 'S7', '255,200,255', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (575, 'S8', '200,255,180', 1.00);
