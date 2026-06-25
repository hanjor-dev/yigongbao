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
VALUES (25, 0, '6', '账户/部门分类', NULL, 1, 6, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(26, 25, '6.1', '企业', '1', 2, 1, 1),
(27, 25, '6.2', '业务', '2', 2, 2, 1);

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
(101, 28, '7.99', '通用', '99', 2, 99, 1);

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
(78, 76, '13.2', '导板', 'guide', 2, 2, 1);

-- ------------------------------------------------------------
-- 订单修改申请类型（父节点 id=81，dict_code=14）
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (81, 0, '14', '订单修改申请类型', NULL, 1, 14, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (82, 81, '14.1', '全量修改', 'FULL', 2, 1, 1);

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
(92, 90, '16.2', '透明', '17.2', 2, 2, 1);

-- ------------------------------------------------------------
-- 产品大类（父节点 id=95，dict_code=17）
-- 用于颜色过滤
-- ------------------------------------------------------------
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES (95, 0, '17', '产品大类', NULL, 1, 17, 1);

INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status)
VALUES
(96, 95, '17.1', '模型类', 'MODEL', 2, 1, 1),
(97, 95, '17.2', '导板类', 'GUIDE', 2, 2, 1);


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
('default.password', '默认密码', '1234.com', 'string', 'security', '新用户初始密码', 1, 0, 1, 1),
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
('order.image.required', '提交订单是否必须上传影像文件', 'false', 'boolean', 'system', 'true-必须上传，false-非必填', 1, 0, 2, 1),
('order.draft.expire.days', '草稿自动过期天数', '30', 'number', 'system', '草稿超过此天数自动过期（天）', 1, 0, 3, 1),
('order.modify.window.minutes', '订单提交后修改窗口期', '10', 'number', 'system', '订单提交后允许修改的时间窗口（分钟）', 1, 0, 4, 1),
('order.modify.apply.expire.minutes', '订单修改申请过期时间', '10', 'number', 'system', '订单修改申请提交后的有效期（分钟）', 1, 0, 5, 1),
('flow.max.audit.reject', '最大允许的审核驳回次数', '3', 'number', 'system', '审核驳回超过此次数后不再允许提交', 1, 0, 6, 1),
('flow.max.rework', '最大允许的返工次数', '2', 'number', 'system', '返工超过此次数后不再允许继续', 1, 0, 6, 1),
('flow.max.design.reject', '最大允许的设计审核驳回次数', '3', 'number', 'system', '设计审核驳回超过此次数后不再允许提交', 1, 0, 7, 1),
('design.assign.mode', '设计师分配模式', 'auto', 'string', 'system', 'auto-自动分配，manual-手动分配', 1, 0, 8, 1),
('design.mode', '设计模式', '2', 'number', 'system', '设计模式：1=线下修改（需上传修订版），2=在线编辑', 1, 1, 9, 1);


-- 订单列表默认列配置（独立 INSERT，避免 JSON 跨行问题）
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES ('order.column.config', '订单列表默认列配置', '{"module":"order","columns":[{"field":"orderCode","label":"订单编号","visible":true,"sort":1,"width":160,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"isUrgent","label":"加急","visible":true,"sort":4,"width":70,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":5,"width":100,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":6,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":7,"width":90,"fixed":null},{"field":"orgName","label":"提单机构","visible":true,"sort":8,"width":150,"fixed":null},{"field":"operatorName","label":"业务员","visible":true,"sort":9,"width":100,"fixed":null},{"field":"operatorPhone","label":"业务员电话","visible":true,"sort":10,"width":120,"fixed":null},{"field":"operatorDeptName","label":"部门","visible":true,"sort":11,"width":120,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":12,"width":180,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":13,"width":100,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":15,"width":100,"fixed":null},{"field":"doctorName","label":"医生","visible":true,"sort":16,"width":100,"fixed":null},{"field":"doctorPhone","label":"医生电话","visible":true,"sort":17,"width":120,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":18,"width":100,"fixed":null},{"field":"patientAge","label":"患者年龄","visible":true,"sort":19,"width":80,"fixed":null},{"field":"patientGenderName","label":"患者性别","visible":true,"sort":20,"width":80,"fixed":null},{"field":"isPostal","label":"是否邮寄","visible":true,"sort":21,"width":80,"fixed":null},{"field":"postalAddress","label":"邮寄地址","visible":true,"sort":22,"width":160,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":23,"width":100,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付时间","visible":true,"sort":24,"width":160,"fixed":null},{"field":"estimatedCost","label":"预估费用","visible":true,"sort":25,"width":100,"fixed":null},{"field":"dataEvaluationOpinion","label":"影像评估意见","visible":true,"sort":26,"width":160,"fixed":null},{"field":"rebuildProjectList","label":"重建项目","visible":true,"sort":27,"width":200,"fixed":null},{"field":"designStartTime","label":"设计开始时间","visible":true,"sort":28,"width":160,"fixed":null},{"field":"designSubmitTime","label":"设计结束时间","visible":true,"sort":29,"width":160,"fixed":null},{"field":"productionStartTime","label":"生产开始时间","visible":true,"sort":30,"width":160,"fixed":null},{"field":"productionEndTime","label":"生产结束时间","visible":true,"sort":31,"width":160,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":32,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":33,"width":150,"fixed":null}]}', 'json', 'system', '订单列表默认显示的列（JSON格式）', 1, 0, 10, 1);

-- -- 订单可修改内容配置
-- INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
-- VALUES ('order.modify.full.config', '订单可修改字段配置', '{"ORDER":{"allowedObjects":["patient","doctor","hospital","delivery","items","images"],"objects":{"patient":{"label":"患者信息","fields":["patientName","patientGender","patientAge"]},"doctor":{"label":"医生信息","fields":["doctorId","doctorName","doctorPhone"]},"hospital":{"label":"医院科室","fields":["hospitalId","hospitalDeptId"]},"delivery":{"label":"交付信息","fields":["isMailDelivery","deliveryAddress","expectedDeliveryTime","isUrgent"]},"items":{"label":"重建项目","coreFields":["bodyPartId","projectId"],"descFields":["projectDesc","moldingRequirement","otherRequirement"]},"images":{"label":"影像文件"}}},"DESIGN":{"allowedObjects":["items"]}}', 'json', 'system', '订单修改允许修改的内容配置', 1, 0, 12, 1);

-- 影像查看器配置（group=system）
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('imaging.viewer.base_url', '影像阅片器查看器URL前缀', 'http://81.70.104.108:8082/#/aiView', 'string', 'system', '影像阅片器查看器完整URL前缀（含协议、端口和路由路径），如 http://127.0.0.1:81/#/viewer', 1, 0, 11, 1);


-- 设计工单列表默认列配置（独立 INSERT）
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES ('design.column.config', '设计工单列表默认列配置', '{"module":"design","columns":[{"field":"isUrgent","label":"加急","visible":true,"sort":1,"width":70,"fixed":null},{"field":"orderCode","label":"订单编号","visible":true,"sort":2,"width":160,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":4,"width":100,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":5,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":6,"width":90,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":7,"width":100,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":8,"width":180,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":9,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":10,"width":100,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":11,"width":100,"fixed":null},{"field":"rebuildProjectSummary","label":"重建项目","visible":true,"sort":12,"width":200,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":13,"width":100,"fixed":null},{"field":"packageCount","label":"数据包数","visible":true,"sort":14,"width":90,"fixed":null},{"field":"designStartTime","label":"设计开始时间","visible":true,"sort":15,"width":160,"fixed":null},{"field":"designSubmitTime","label":"设计结束时间","visible":true,"sort":16,"width":160,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付","visible":true,"sort":17,"width":120,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":18,"width":160,"fixed":null},{"field":"rejectReason","label":"驳回原因","visible":false,"sort":19,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":20,"width":150,"fixed":"right"}]}', 'json', 'system', '设计工单列表默认显示的列（JSON格式）', 1, 0, 13, 1);

-- 生产流转卡列表默认列配置
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES ('production.column.config', '生产流转卡列表默认列配置', '{"module":"production","columns":[{"field":"isUrgent","label":"加急","visible":true,"sort":1,"width":70,"fixed":null},{"field":"recordNo","label":"流转卡编号","visible":true,"sort":2,"width":150,"fixed":null},{"field":"designPackageCode","label":"数据包编号","visible":true,"sort":3,"width":150,"fixed":null},{"field":"productionBatchNo","label":"生产批号","visible":true,"sort":4,"width":150,"fixed":null},{"field":"orderCode","label":"订单流水号","visible":true,"sort":5,"width":160,"fixed":null},{"field":"currentProcessName","label":"当前工序","visible":true,"sort":6,"width":120,"fixed":null},{"field":"statusName","label":"状态","visible":true,"sort":7,"width":120,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":8,"width":110,"fixed":null},{"field":"totalProductCount","label":"产品数量","visible":true,"sort":9,"width":90,"fixed":null},{"field":"hospitalName","label":"医院名称","visible":true,"sort":10,"width":180,"fixed":null},{"field":"hospitalDeptName","label":"科室名称","visible":true,"sort":11,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":12,"width":100,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":13,"width":100,"fixed":null},{"field":"isPostal","label":"是否邮寄","visible":true,"sort":14,"width":80,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付时间","visible":true,"sort":15,"width":160,"fixed":null},{"field":"printStartTime","label":"生产开始时间","visible":true,"sort":16,"width":160,"fixed":null},{"field":"postProcessingEndTime","label":"生产结束时间","visible":true,"sort":17,"width":160,"fixed":null},{"field":"orgName","label":"机构名称","visible":true,"sort":18,"width":150,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":19,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":20,"width":150,"fixed":"right"}]}', 'json', 'system', '生产流转卡列表默认显示的列（JSON格式）', 1, 0, 14, 1);

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
('org.cert.allowed_extensions', '资质文件允许格式', '.zip,.rar,.tar,.7z', 'string', 'file', '机构资质文件允许的压缩包格式，逗号分隔', 0, 0, 13, 1),
('org.cert.max_size_mb', '资质文件最大大小', '500', 'number', 'file', '机构资质文件上传时的最大文件大小（MB）', 0, 0, 14, 1);

INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('manufacturer.org.id', '生产企业机构ID', '1', 'number', 'system', '系统预设唯一生产企业机构ID，不可动态创建', 1, 0, 10, 1),
('unknown.hospital.org.id', '其他医院机构ID', '2', 'number', 'system', '提单时用于隐藏具体客户信息的占位医院ID，权限校验豁免', 1, 0, 11, 1),
('user.username.auto.generate', '用户名自动生成开关', 'true', 'boolean', 'system', 'true=后端按机构前缀+序号自动生成，false=前端手动输入', 1, 0, 21, 1);

-- ------------------------------------------------------------
-- 生产管理配置（group=production）
-- ------------------------------------------------------------
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('production.pending.print.timeout.minutes', '待打印超时阈值（分钟）', '10', 'number', 'system', '分配设备后超过此时间未收到打印开始推送，触发超时提醒', 1, 0, 1, 1),
('production.printing.timeout.minutes', '打印中超时阈值（分钟）', '240', 'number', 'system', '打印开始后超过此时间未收到打印完成推送，触发超时提醒', 1, 0, 2, 1);

INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES ('production.process.params.config', '工序参数配置字典', '{"print":{"layerThickness":{"label":"层厚","type":"number","unit":"mm","required":true},"laserPower":{"label":"激光器功率","type":"number","unit":"mW","required":true}},"wash":{"alcoholBatchNo":{"label":"酒精批号","type":"text","required":true},"soakLevel":{"label":"浸泡程度","type":"select","options":["完全浸泡","部分浸泡"],"default":"完全浸泡","required":true}},"cure":{"cureMode":{"label":"固化模式","type":"select","options":["HIGH","LOW"],"default":"HIGH","required":true}},"clean_dry":{"alcoholBatchNo":{"label":"酒精批号","type":"text","required":true},"cleanMode":{"label":"清洗模式","type":"select","options":["变波","脱气"],"default":"变波","required":true},"heating":{"label":"加热","type":"select","options":["开","关"],"default":"关","required":true}},"pack":{"h2o2Sterilization":{"label":"过氧化氢等离子灭菌","type":"switch","required":false},"sealTemperature":{"label":"纸塑袋热封温度","type":"number","unit":"℃","default":122,"required":true},"sealTime":{"label":"热封时间","type":"number","unit":"秒","default":3,"required":true},"zipBagSeal":{"label":"PE复合食品包装袋热封","type":"switch","required":false,"dependents":{"zipBagSealTemperature":{"label":"PE复合食品包装袋热封温度","type":"number","unit":"℃","default":130,"required":true},"zipBagSealTime":{"label":"热封时间","type":"number","unit":"秒","default":3,"required":true}}}}}', 'json', 'system', '各工序参数字段定义（JSON），前端据此动态渲染参数表单', 1, 0, 3, 1);



-- ============================================================
-- 资源数据初始化（sys_resource）
-- ------------------------------------------------------------
-- resource_type: 1=一级菜单, 2=二级菜单, 3=按钮
-- ------------------------------------------------------------

INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1, 0, '数据概览', 'DataBoard', 1, '&#xe62e;', '/home', null, '/home/index', 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (2, 0, '业务运营', 'Business', 1, null, '/business', null, '/business/order', 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (4, 0, '模块管理', 'Module', 1, null, '/module', null, '/module/org', 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (5, 0, '用户和权限', 'Auth', 1, null, '/auth', null, '/auth/account', 5, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (6, 0, '资料管理', 'Datum', 1, null, '/datum', null, '/datum/index', 6, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (7, 0, '系统配置', 'System', 1, null, '/system', null, '/system/dict', 7, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (8, 0, '统计报表', 'Statistical', 1, '&#xe6d6;', '/statistical', null, '/statistical/index', 8, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (9, 0, '备份管理', 'Backup', 1, '&#xe7a0;', '/backup', null, '/backup/index', 9, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (101, 2, '订单管理', 'Order', 2, '&#xeb49;', '/order', 'business/order.vue', null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (102, 2, '我的工单', 'Design', 2, '&#xe608;', '/design', 'business/design.vue', null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (103, 2, '生产管理', 'Manufacture', 2, '&#xe662;', '/manufacture', 'business/manufacture.vue', null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (104, 2, '质检管理', 'Quality', 2, null, '/quality', 'business/quality.vue', null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (105, 2, '仓储管理', 'Storage', 2, null, '/storage', 'business/storage.vue', null, 5, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (301, 4, '机构管理', 'Org', 2, '&#xe61a;', '/org', 'module/org.vue', null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (302, 4, '部门管理', 'Branch', 2, '&#xe62b;', '/branch', 'module/branch.vue', null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (303, 4, '科室管理', 'Department', 2, '&#xe69f;', '/department', 'module/department.vue', null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (304, 4, '项目管理', 'Project', 2, '&#xe620;', '/project', 'module/project.vue', null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (305, 4, '产品管理', 'Product', 2, '&#xe601;', '/product', 'module/product.vue', null, 5, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (306, 4, '医院范围模板管理', 'Template', 2, '&#xe605;', '/template', 'module/template.vue', null, 7, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (307, 4, '注册证管理', 'RegCertificate', 2, '&#xe76a;', '/regCertificate', 'module/regCertificate.vue', null, 6, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (308, 4, '加工中心管理', 'MacCenter', 2, null, '/center', 'module/center.vue', null, 8, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (309, 4, '设备管理', 'Device', 2, null, '/device', 'module/device.vue', null, 9, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (401, 5, '账户管理', 'Account', 2, '&#xe602;', '/account', 'auth/account.vue', null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (402, 5, '角色管理', 'Role', 2, '&#xe6a0;', '/role', 'auth/role.vue', null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (403, 5, '资源管理', 'Resource', 2, '&#xe607;', '/resource', 'auth/resource.vue', null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (501, 7, '字典管理', 'Dict', 2, '&#xe636;', '/dict', 'system/dict.vue', null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (502, 7, '参数配置', 'Param', 2, '&#xe60e;', '/param', 'system/param.vue', null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (503, 7, '操作日志', 'Log', 2, '&#xe668;', '/log', 'system/log.vue', null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1001, 403, '添加', 'resource:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1002, 403, '编辑', 'resource:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1003, 403, '状态', 'resource:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1004, 403, '删除', 'resource:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1005, 402, '新建角色', 'role:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1006, 402, '编辑', 'role:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1007, 402, '状态', 'role:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1008, 402, '删除', 'role:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1009, 402, '配置权限', 'role:config', 3, null, null, null, null, 5, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1014, 303, '添加', 'hospital-dept:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1015, 303, '编辑', 'hospital-dept:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1016, 303, '状态', 'hospital-dept:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1017, 303, '删除', 'hospital-dept:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1022, 306, '添加', 'hospital-Temp:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1023, 306, '编辑', 'hospital-Temp:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1024, 306, '状态', 'hospital-Temp:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1025, 306, '删除', 'hospital-Temp:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1026, 301, '添加', 'org:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1027, 301, '编辑', 'org:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1028, 301, '状态', 'org:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1029, 301, '删除', 'org:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1030, 302, '添加', 'department:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1031, 302, '编辑', 'department:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1032, 302, '状态', 'department:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1033, 302, '删除', 'department:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1034, 304, '添加', 'project:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1035, 304, '编辑', 'project:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1036, 304, '状态', 'project:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1037, 304, '删除', 'project:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1038, 305, '添加', 'product:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1039, 305, '编辑', 'product:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1040, 305, '状态', 'product:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1041, 305, '删除', 'product:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1042, 307, '添加', 'registration-cert:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1043, 307, '编辑', 'registration-cert:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1044, 307, '状态', 'registration-cert:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1045, 307, '删除', 'registration-cert:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1046, 501, '添加', 'dict:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1047, 501, '编辑', 'dict:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1048, 501, '状态', 'dict:Status', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1049, 501, '删除', 'dict:Delete', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1050, 502, '添加', 'param:Add', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1051, 502, '编辑', 'param:Edit', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1052, 502, '删除', 'param:Delete', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1053, 503, '导出Excel', 'log:Export', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1101, 101, '我的草稿Tab', 'order:TabDraft', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1102, 101, '订单列表Tab', 'order:TabOrderList', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1103, 101, '经典案例Tab', 'order:TabCase', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1104, 101, '新建订单', 'order:Add', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1105, 101, '批量导出', 'order:BatchExport', 3, null, null, null, null, 5, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1106, 101, '查看详情', 'order:View', 3, null, null, null, null, 6, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1107, 101, '影像在线预览', 'order:DicomView', 3, null, null, null, null, 7, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1108, 101, '生成二维码', 'order:EncodeView', 3, null, null, null, null, 8, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1109, 101, '修改订单', 'order:Modify', 3, null, null, null, null, 9, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1110, 101, '修改历史', 'order:ModifyHistory', 3, null, null, null, null, 10, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1111, 101, '审核通过', 'order:Approve', 3, null, null, null, null, 11, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1112, 101, '审核驳回', 'order:Reject', 3, null, null, null, null, 12, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1113, 101, '订单取消', 'order:Cancel', 3, null, null, null, null, 13, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1114, 101, '完成订单', 'order:Complete', 3, null, null, null, null, 14, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1115, 101, '重新提交订单', 'order:review', 3, null, null, null, null, 15, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1116, 101, '编辑草稿', 'draft:Edit', 3, null, null, null, null, 16, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1117, 101, '查看草稿', 'draft:View', 3, null, null, null, null, 17, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1118, 101, '删除草稿', 'draft:Delete', 3, null, null, null, null, 18, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1119, 101, '查看案例详情', 'case:View', 3, null, null, null, null, 19, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1120, 101, '标记为经典案例', 'order:MarkCase', 3, null, null, null, null, 20, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1121, 101, '分配设计师', 'design:AssignDesigner', 3, null, null, null, null, 21, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1122, 101, '修改申请列表', 'order:ModifyApply', 3, null, null, null, null, 22, 1, 1, null, '2026-06-15 17:18:28', '2026-06-15 17:18:28', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1201, 102, '查看详情', 'design:View', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1202, 102, '开始设计', 'design:Start', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1203, 102, '完成设计', 'design:Complete', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1204, 102, '上传设计文件', 'design:Upload', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1205, 102, '填写打印信息', 'design:PrintInfo', 3, null, null, null, null, 5, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1206, 102, '指令单和图纸', 'design:DocumentPreview', 3, null, null, null, null, 6, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1207, 102, '影像在线预览', 'design:DicomView', 3, null, null, null, null, 7, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1208, 102, '生成二维码', 'design:EncodeView', 3, null, null, null, null, 8, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1209, 102, '在线标注', 'design:StlMark', 3, null, null, null, null, 9, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1210, 102, '取消工单', 'design:Cancel', 3, null, null, null, null, 10, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1211, 102, '审核通过', 'design:Approve', 3, null, null, null, null, 11, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1212, 102, '审核驳回', 'design:Reject', 3, null, null, null, null, 12, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1213, 102, '批量导出', 'design:BatchExport', 3, null, null, null, null, 13, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1214, 102, '工作量导出', 'design:WorkExport', 3, null, null, null, null, 14, 1, 1, null, '2026-06-15 17:18:28', '2026-06-15 17:18:28', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1301, 103, '查看详情', 'manufacture:View', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1302, 103, '下载数据包', 'manufacture:Download', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1303, 103, '分配设备', 'manufacture:Device', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1304, 103, '查看流转卡', 'manufacture:Flow', 3, null, null, null, null, 4, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1305, 103, '生成流转二维码', 'manufacture:QR', 3, null, null, null, null, 5, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1306, 103, '生成流转卡', 'manufacture:ProCard', 3, null, null, null, null, 6, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1307, 103, '开始后处理', 'manufacture:Start', 3, null, null, null, null, 7, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1308, 103, '完成后处理', 'manufacture:Complete', 3, null, null, null, null, 8, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1309, 103, '提交质检', 'manufacture:QC', 3, null, null, null, null, 9, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1310, 103, '包装', 'manufacture:packaging', 3, null, null, null, null, 10, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1311, 103, '保存包装信息', 'manufacture:SaveInfo', 3, null, null, null, null, 11, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1312, 103, '流转入库', 'manufacture:Warehousing', 3, null, null, null, null, 12, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1313, 103, '导出生产产品台账', 'manufacture:Export', 3, null, null, null, null, 13, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1401, 104, '开始质检', 'quality:Start', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1402, 104, '重新质检', 'quality:Again', 3, null, null, null, null, 2, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1403, 104, '流转包装', 'quality:ToPackaging', 3, null, null, null, null, 3, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1501, 105, '查看详情', 'storage:View', 3, null, null, null, null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1502, 4, '收费模板管理', 'Charge', 2, null, '/charge', 'module/charge.vue', null, 1, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);
INSERT INTO yigongbao.sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES (1504, 0, '个人中心', 'Personal', 1, null, '/personal', null, '/personal/index', 8, 1, 1, null, '2026-06-15 10:39:52', '2026-06-15 10:39:52', null, null, 0);

-- ============================================================
-- 机构基础数据初始化（sys_org）
-- ============================================================
INSERT INTO sys_org (id, org_name, org_code, org_type, username_prefix, contact, phone, status)
VALUES
(1, '嘉一高科（企业）', 'ORG-0001', '1.1', 'jygk', '管理员', '13800000000', 1),
(2, '其他医院', 'ORG-H-9999', '1.3', NULL, NULL, NULL, 1);


-- ============================================================
-- 角色基础数据初始化（sys_role）
-- ============================================================
INSERT INTO sys_role (id, role_name, role_code, role_desc, account_type, data_scope_type, status)
VALUES
-- 系统级角色（仅用于系统维护）
(1,  '超级管理员',   'admin',               '系统维护专用，拥有全部权限',                                   '6.1', 'all',       1),
-- 业务线角色（经销商侧）
(2,  '业务员',       'salesman',            '负责订单开拓、客户维护，只能看自己关联医院的数据',             '6.2', 'hospitals', 1),
(3,  '区域管理员',   'regional-manager',    '管理本部门下所有机构及订单，拥有订单模块全部操作权限',         '6.2', 'dept',      1),
-- 设计线角色（企业内部）
(4,  '设计师',       'designer',            '处理分配给自己的设计工单',                                     '6.1', 'self',      1),
(5,  '设计管理员',   'designer-manager',    '工单分配、审核、统计，拥有设计模块全部操作权限',               '6.1', 'all',       1),
-- 生产线角色（企业内部）
(6,  '生产员',       'production-worker',   '执行生产任务，可见本加工中心数据',                             '6.1', 'center',    1),
(7,  '生产管理员',   'production-manager',  '生产任务分配、进度管理，可见本加工中心数据',                   '6.1', 'center',    1),
-- 质检线角色（企业内部）
(8,  'QC',           'qc',                  '质检执行，可见全部数据',                                       '6.1', 'all',       1),
(12, '质检管理员',   'qc-manager',          '质检任务分配、进度管理、工单和生产查看',                       '6.1', 'all',       1),
-- 仓储线角色（企业内部）
(9,  '库管',         'warehouse-manager',   '仓储管理，成品入库出库，可见全部数据',                         '6.1', 'all',       1),
-- 财务线角色（企业内部）
(10, '财务',         'finance',             '财务核算、对账、报表，可见全部数据',                           '6.1', 'all',       1),
-- 公司管理角色
(11, '公司管理员',   'company-admin',       '公司级业务管理者，拥有全部业务权限（不含系统底层配置）',       '6.1', 'all',       1);


-- ============================================================
-- 用户基础数据初始化（sys_user）
-- 初始密码为 BCrypt 加密后的 "123456"
-- $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, phone, account_type, org_id, org_name, role_id, role_name, role_code, status)
VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800000000', '6.1', 1, '嘉一高科（企业）', 1, '超级管理员', 'admin', 1);


-- ============================================================
-- 角色资源关联数据初始化（sys_role_resource）
-- ============================================================

INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (1, 1, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (2, 1, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (3, 1, 4);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (4, 1, 5);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (5, 1, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (6, 1, 7);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (7, 1, 8);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (8, 1, 9);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (10, 1, 101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (11, 1, 102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (12, 1, 103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (13, 1, 104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (14, 1, 105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (15, 1, 301);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (16, 1, 302);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (17, 1, 303);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (18, 1, 304);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (19, 1, 305);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (20, 1, 306);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (21, 1, 307);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (22, 1, 308);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (23, 1, 309);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (25, 1, 401);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (26, 1, 402);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (27, 1, 403);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (28, 1, 501);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (29, 1, 502);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (30, 1, 503);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (31, 1, 1001);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (32, 1, 1002);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (33, 1, 1003);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (34, 1, 1004);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (35, 1, 1005);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (36, 1, 1006);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (37, 1, 1007);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (38, 1, 1008);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (39, 1, 1009);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (40, 1, 1014);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (41, 1, 1015);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (42, 1, 1016);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (43, 1, 1017);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (44, 1, 1022);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (45, 1, 1023);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (46, 1, 1024);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (47, 1, 1025);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (48, 1, 1026);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (49, 1, 1027);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (50, 1, 1028);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (51, 1, 1029);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (52, 1, 1030);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (53, 1, 1031);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (54, 1, 1032);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (55, 1, 1033);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (56, 1, 1034);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (57, 1, 1035);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (58, 1, 1036);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (59, 1, 1037);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (60, 1, 1038);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (61, 1, 1039);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (62, 1, 1040);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (63, 1, 1041);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (64, 1, 1042);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (65, 1, 1043);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (66, 1, 1044);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (67, 1, 1045);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (68, 1, 1046);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (69, 1, 1047);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (70, 1, 1048);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (71, 1, 1049);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (72, 1, 1050);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (73, 1, 1051);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (74, 1, 1052);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (75, 1, 1053);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (76, 1, 1101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (77, 1, 1102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (78, 1, 1103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (79, 1, 1104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (80, 1, 1105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (81, 1, 1106);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (82, 1, 1107);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (83, 1, 1108);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (84, 1, 1109);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (85, 1, 1110);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (86, 1, 1111);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (87, 1, 1112);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (88, 1, 1113);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (89, 1, 1114);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (90, 1, 1115);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (91, 1, 1116);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (92, 1, 1117);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (93, 1, 1118);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (94, 1, 1119);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (95, 1, 1120);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (96, 1, 1121);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (433, 1, 1122);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (97, 1, 1201);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (98, 1, 1202);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (99, 1, 1203);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (100, 1, 1204);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (101, 1, 1205);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (102, 1, 1206);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (103, 1, 1207);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (104, 1, 1208);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (105, 1, 1209);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (106, 1, 1210);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (107, 1, 1211);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (108, 1, 1212);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (109, 1, 1213);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (434, 1, 1214);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (110, 1, 1301);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (111, 1, 1302);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (112, 1, 1303);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (113, 1, 1304);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (114, 1, 1305);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (115, 1, 1306);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (116, 1, 1307);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (117, 1, 1308);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (118, 1, 1309);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (119, 1, 1310);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (120, 1, 1311);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (121, 1, 1312);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (1213, 1, 1313);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (122, 1, 1401);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (123, 1, 1402);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (124, 1, 1403);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (125, 1, 1501);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (24, 1, 1502);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (9, 1, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (852, 2, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (854, 2, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (873, 2, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (855, 2, 101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (856, 2, 1101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (857, 2, 1102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (858, 2, 1103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (859, 2, 1104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (860, 2, 1105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (861, 2, 1106);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (862, 2, 1107);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (863, 2, 1108);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (864, 2, 1109);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (865, 2, 1110);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (866, 2, 1113);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (867, 2, 1114);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (868, 2, 1115);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (869, 2, 1116);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (870, 2, 1117);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (871, 2, 1118);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (872, 2, 1119);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (853, 2, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (482, 3, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (484, 3, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (506, 3, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (485, 3, 101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (486, 3, 1101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (487, 3, 1102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (488, 3, 1103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (489, 3, 1104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (490, 3, 1105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (491, 3, 1106);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (492, 3, 1107);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (493, 3, 1108);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (494, 3, 1109);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (495, 3, 1110);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (496, 3, 1111);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (497, 3, 1112);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (498, 3, 1113);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (499, 3, 1114);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (500, 3, 1115);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (501, 3, 1116);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (502, 3, 1117);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (503, 3, 1118);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (504, 3, 1119);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (505, 3, 1120);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (483, 3, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (507, 4, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (520, 4, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (519, 4, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (509, 4, 102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (510, 4, 1201);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (511, 4, 1202);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (512, 4, 1203);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (513, 4, 1204);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (514, 4, 1205);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (515, 4, 1206);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (516, 4, 1207);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (517, 4, 1208);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (518, 4, 1209);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (508, 4, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (623, 5, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (670, 5, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (671, 5, 4);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (669, 5, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (625, 5, 101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (648, 5, 102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (664, 5, 304);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (665, 5, 1034);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (666, 5, 1035);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (667, 5, 1036);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (668, 5, 1037);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (627, 5, 1102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (628, 5, 1103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (629, 5, 1104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (630, 5, 1105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (631, 5, 1106);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (632, 5, 1107);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (633, 5, 1108);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (634, 5, 1109);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (635, 5, 1110);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (636, 5, 1111);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (637, 5, 1112);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (638, 5, 1113);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (639, 5, 1114);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (640, 5, 1115);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (641, 5, 1116);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (642, 5, 1117);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (643, 5, 1118);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (644, 5, 1119);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (645, 5, 1120);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (646, 5, 1121);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (647, 5, 1122);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (649, 5, 1201);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (650, 5, 1202);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (651, 5, 1203);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (652, 5, 1204);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (653, 5, 1205);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (654, 5, 1206);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (655, 5, 1207);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (656, 5, 1208);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (657, 5, 1209);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (658, 5, 1210);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (659, 5, 1211);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (660, 5, 1212);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (661, 5, 1213);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (662, 5, 1214);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (663, 5, 1502);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (624, 5, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (672, 6, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (688, 6, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (687, 6, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (674, 6, 103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (675, 6, 1301);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (676, 6, 1302);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (677, 6, 1303);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (678, 6, 1304);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (679, 6, 1305);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (680, 6, 1306);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (681, 6, 1307);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (682, 6, 1308);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (683, 6, 1309);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (684, 6, 1310);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (685, 6, 1311);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (686, 6, 1312);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (673, 6, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (689, 7, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (711, 7, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (710, 7, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (691, 7, 101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (697, 7, 103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (692, 7, 1102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (693, 7, 1103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (694, 7, 1105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (695, 7, 1106);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (696, 7, 1119);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (698, 7, 1301);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (699, 7, 1302);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (700, 7, 1303);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (701, 7, 1304);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (702, 7, 1305);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (703, 7, 1306);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (704, 7, 1307);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (705, 7, 1308);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (706, 7, 1309);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (707, 7, 1310);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (708, 7, 1311);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (709, 7, 1312);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (1214, 7, 1313);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (690, 7, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (712, 8, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (719, 8, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (718, 8, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (714, 8, 104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (715, 8, 1401);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (716, 8, 1402);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (717, 8, 1403);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (713, 8, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (846, 9, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (851, 9, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (850, 9, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (848, 9, 105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (849, 9, 1501);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (847, 9, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (874, 10, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (884, 10, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (882, 10, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (883, 10, 8);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (876, 10, 101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (877, 10, 1102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (878, 10, 1103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (879, 10, 1105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (880, 10, 1106);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (881, 10, 1119);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (1215, 10, 1313);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (875, 10, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (732, 11, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (734, 11, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (792, 11, 4);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (831, 11, 5);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (844, 11, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (845, 11, 8);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (735, 11, 101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (758, 11, 102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (773, 11, 103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (786, 11, 104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (790, 11, 105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (793, 11, 301);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (799, 11, 302);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (804, 11, 303);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (809, 11, 304);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (814, 11, 305);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (824, 11, 306);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (819, 11, 307);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (829, 11, 308);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (830, 11, 309);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (832, 11, 401);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (833, 11, 402);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (839, 11, 403);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (840, 11, 1001);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (841, 11, 1002);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (842, 11, 1003);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (843, 11, 1004);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (834, 11, 1005);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (835, 11, 1006);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (836, 11, 1007);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (837, 11, 1008);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (838, 11, 1009);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (805, 11, 1014);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (806, 11, 1015);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (807, 11, 1016);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (808, 11, 1017);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (825, 11, 1022);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (826, 11, 1023);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (827, 11, 1024);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (828, 11, 1025);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (794, 11, 1026);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (795, 11, 1027);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (796, 11, 1028);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (797, 11, 1029);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (800, 11, 1030);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (801, 11, 1031);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (802, 11, 1032);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (803, 11, 1033);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (810, 11, 1034);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (811, 11, 1035);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (812, 11, 1036);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (813, 11, 1037);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (815, 11, 1038);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (816, 11, 1039);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (817, 11, 1040);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (818, 11, 1041);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (820, 11, 1042);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (821, 11, 1043);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (822, 11, 1044);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (823, 11, 1045);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (736, 11, 1101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (737, 11, 1102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (738, 11, 1103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (739, 11, 1104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (740, 11, 1105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (741, 11, 1106);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (742, 11, 1107);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (743, 11, 1108);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (744, 11, 1109);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (745, 11, 1110);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (746, 11, 1111);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (747, 11, 1112);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (748, 11, 1113);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (749, 11, 1114);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (750, 11, 1115);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (751, 11, 1116);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (752, 11, 1117);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (753, 11, 1118);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (754, 11, 1119);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (755, 11, 1120);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (756, 11, 1121);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (757, 11, 1122);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (759, 11, 1201);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (760, 11, 1202);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (761, 11, 1203);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (762, 11, 1204);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (763, 11, 1205);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (764, 11, 1206);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (765, 11, 1207);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (766, 11, 1208);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (767, 11, 1209);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (768, 11, 1210);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (769, 11, 1211);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (770, 11, 1212);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (771, 11, 1213);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (772, 11, 1214);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (774, 11, 1301);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (775, 11, 1302);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (776, 11, 1303);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (777, 11, 1304);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (778, 11, 1305);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (779, 11, 1306);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (780, 11, 1307);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (781, 11, 1308);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (782, 11, 1309);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (783, 11, 1310);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (784, 11, 1311);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (785, 11, 1312);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (1216, 11, 1313);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (787, 11, 1401);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (788, 11, 1402);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (789, 11, 1403);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (791, 11, 1501);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (798, 11, 1502);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (733, 11, 1504);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (1217, 12, 1);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (731, 12, 2);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (730, 12, 6);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (720, 12, 101);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (726, 12, 104);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (721, 12, 1102);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (722, 12, 1103);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (723, 12, 1105);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (724, 12, 1106);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (725, 12, 1119);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (727, 12, 1401);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (728, 12, 1402);
INSERT INTO yigongbao.sys_role_resource (id, role_id, resource_id) VALUES (729, 12, 1403);


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
('DEPT_NO',        '部门编码',       'DEPT-',  NULL,             4, 'NEVER', 1),  -- DeptServiceImpl
('USER_NO',        '用户名序号',     NULL,     NULL,             3, 'NEVER', 1),  -- UserServiceImpl（generateWithSeqSuffix，按机构前缀隔离序号池）
-- 生产模块编码
('PRODUCTION_RECORD_NO', '生产流转卡编号', 'PR-', NULL,         6, 'NEVER', 1),  -- ProductionRecordServiceImpl
('PRODUCTION_BATCH_NO',  '生产批号',       'PB-', '{yyyy}{MM}{dd}', 4, 'DAY',   1),  -- ProductionRecordServiceImpl
('PRODUCT_NO',           '产品编号',       'PD-', NULL,         6, 'NEVER', 1),  -- ProductionRecordServiceImpl
('UDI_CODE',             'UDI编码',        'UDI-',NULL,         8, 'NEVER', 1);  -- 产品唯一标识码


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
('HDEPT-0008', '影像科', 8, 1),
('HDEPT-0009', '其他科室', 9, 1);


-- ============================================================
-- 编码序号同步（sys_code_rule.current_value）
-- 编码生成器首次调用时以此值为初始序号，确保新生成的编码不与已有种子数据冲突
-- 注意：必须在对应业务表数据插入之后执行
-- ============================================================
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM hospital_dept)          WHERE rule_code = 'HDEPT_NO';
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM sys_org)                WHERE rule_code = 'ORG_NO';


-- ============================================================
-- 重建部位数据初始化（rebuild_body_part）
-- ============================================================
INSERT INTO rebuild_body_part (code, name, sort, status, remark)
VALUES
('BP-0001', '头部', 1, 1, '包括颅骨、面骨等头部结构'),
('BP-0002', '颈部', 2, 1, '包括颈椎、颈部软组织等结构'),
('BP-0003', '胸部', 3, 1, '包括胸椎、肋骨、胸骨等胸部结构'),
('BP-0004', '腹部', 4, 1, '包括腰椎、腹部软组织等结构'),
('BP-0005', '盆腔', 5, 1, '包括骨盆、髋关节等盆腔结构'),
('BP-0006', '脊柱', 6, 1, '包括颈椎、胸椎、腰椎、骶椎等脊柱结构'),
('BP-0007', '左上肢', 7, 1, '包括左侧肩关节、肱骨、肘关节、前臂、腕关节等'),
('BP-0008', '右上肢', 8, 1, '包括右侧肩关节、肱骨、肘关节、前臂、腕关节等'),
('BP-0009', '左下肢', 9, 1, '包括左侧髋关节、股骨、膝关节、小腿、踝关节等'),
('BP-0010', '右下肢', 10, 1, '包括右侧髋关节、股骨、膝关节、小腿、踝关节等'),
('BP-0011', '左手', 11, 1, '包括左侧腕骨、掌骨、指骨等手部结构'),
('BP-0012', '右手', 12, 1, '包括右侧腕骨、掌骨、指骨等手部结构'),
('BP-0013', '左足', 13, 1, '包括左侧跗骨、跖骨、趾骨等足部结构'),
('BP-0014', '右足', 14, 1, '包括右侧跗骨、跖骨、趾骨等足部结构');


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
