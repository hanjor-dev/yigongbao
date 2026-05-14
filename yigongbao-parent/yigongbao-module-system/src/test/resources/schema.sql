-- ============================================================
-- 医工宝系统测试 DDL（module-system）
-- H2 内存数据库建表语句
-- ============================================================

-- ------------------------------------------------------------
-- 字典表（单表树形结构）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父级ID（0表示根节点/字典类型）',
    dict_code       VARCHAR(32)      NOT NULL COMMENT '字典编码（层级数字，如：1、1.1、1.1.1）',
    dict_name       VARCHAR(128)    NOT NULL COMMENT '字典名称',
    dict_value      VARCHAR(256)    DEFAULT NULL COMMENT '字典值（叶子节点使用）',
    level           INT             DEFAULT 1 COMMENT '层级（1=字典类型，2=字典数据，3+=扩展层级）',
    sort            INT             DEFAULT 0 COMMENT '排序（同级内排序）',
    status          TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark          VARCHAR(512)    DEFAULT NULL COMMENT '备注说明',

    -- 通用字段
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_level (level)
);

-- 插入字典测试数据
-- 机构类型（dict_code=1）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 0, '1', '机构类型', NULL, 1, 0, 1, '机构类型字典', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(2, 1, '1.1', '生产企业', 'production', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(3, 1, '1.2', '经销商', 'distributor', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(4, 1, '1.3', '医疗机构', 'medical', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(5, 1, '1.4', '其他', 'other', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),

-- 性别（dict_code=2）
(10, 0, '2', '性别', NULL, 1, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(11, 10, '2.1', '男', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(12, 10, '2.2', '女', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),

-- 医院等级（dict_code=3）
(20, 0, '3', '医院等级', NULL, 1, 0, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(21, 20, '3.1', '三级甲等', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(22, 20, '3.2', '三级乙等', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(23, 20, '3.3', '二级甲等', '3', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(24, 20, '3.4', '二级乙等', '4', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(25, 20, '3.5', '一级医院', '5', 2, 5, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),

-- 医院类型（dict_code=4）
(30, 0, '4', '医院类型', NULL, 1, 0, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(31, 30, '4.1', '综合医院', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(32, 30, '4.2', '专科医院', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(33, 30, '4.3', '社区医院', '3', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),

-- 代理产品线（dict_code=5）
(40, 0, '5', '代理产品线', NULL, 1, 0, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(41, 40, '5.1', '医疗器械', 'medical_device', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(42, 40, '5.2', '药品', 'drug', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(43, 40, '5.3', '耗材', 'consumable', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(44, 40, '5.4', '设备', 'equipment', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),

-- 账户分类（dict_code=6）
(60, 0, '6', '账户分类', NULL, 1, 5, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(61, 60, '6.1', '内部用户', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(62, 60, '6.2', '外部用户', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),

-- 专业方向（dict_code=7，按人体部位划分）
(70, 0, '7', '专业方向', NULL, 1, 6, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
-- 头部方向
(71, 70, '7.1', '头部方向', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(72, 71, '7.1.1', '口腔修复', '1_1', 3, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(73, 71, '7.1.2', '口腔种植', '1_2', 3, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(74, 71, '7.1.3', '正畸', '1_3', 3, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(75, 71, '7.1.4', '口腔综合', '1_4', 3, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(76, 71, '7.1.5', '口腔颌面外科', '1_5', 3, 5, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
-- 四肢方向
(77, 70, '7.2', '四肢方向', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(78, 77, '7.2.1', '骨科', '2_1', 3, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(79, 77, '7.2.2', '康复医学', '2_2', 3, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(80, 77, '7.2.3', '运动医学', '2_3', 3, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
-- 脊椎方向
(81, 70, '7.3', '脊椎方向', '3', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(82, 81, '7.3.1', '脊柱外科', '3_1', 3, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(83, 81, '7.3.2', '神经外科', '3_2', 3, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
-- 通用（兜底专业方向）
(89, 70, '7.99', '通用', '99', 2, 99, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),

-- 结算类型（dict_code=8）
(90, 0, '8', '结算类型', NULL, 1, 7, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(91, 90, '8.1', '预付费', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(92, 90, '8.2', '后付费', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(93, 90, '8.3', '月结', '3', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- ------------------------------------------------------------
-- 测试表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS test;
CREATE TABLE test (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    key1            VARCHAR(64)     DEFAULT NULL COMMENT '键',
    value1          VARCHAR(256)    DEFAULT NULL COMMENT '值',

    -- 通用字段
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id)
);

-- ------------------------------------------------------------
-- 机构表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_org;
CREATE TABLE sys_org (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    org_name            VARCHAR(128)    NOT NULL COMMENT '机构名称',
    org_code            VARCHAR(32)     NOT NULL COMMENT '机构编码',
    org_type            VARCHAR(8)          NOT NULL COMMENT '机构类型（字典编码，如：1.1=生产企业，1.2=经销商，1.3=医疗机构，1.4=其他）',
    area_id             BIGINT          COMMENT '所属地区ID',
    area_name           VARCHAR(64)     COMMENT '所属地区名称',
    address             VARCHAR(256)    COMMENT '详细地址',
    contact             VARCHAR(32)     NOT NULL COMMENT '联系人',
    phone               VARCHAR(32)     NOT NULL COMMENT '联系电话',
    email               VARCHAR(64)     COMMENT '联系邮箱',
    credit_code         VARCHAR(32)     COMMENT '统一社会信用代码',
    qualification_file  VARCHAR(512)    COMMENT '资质文件',
    qualification_type  TINYINT         COMMENT '1=医疗器械,2=非医疗器械',
    hospital_level      VARCHAR(16)      COMMENT '医院等级（字典：dict_code=3，值如 3.1/3.2）',
    hospital_type       VARCHAR(16)      COMMENT '医院类型（字典：dict_code=4，值如 4.1/4.2）',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    -- 通用字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_org_type (org_type),
    KEY idx_area_id (area_id),
    KEY idx_status (status)
);

-- 插入机构测试数据
INSERT INTO sys_org (id, org_name, org_code, org_type, area_id, area_name, address, contact, phone, email, status, remark, create_time, update_time, is_deleted) VALUES
(1, '测试医疗机构', 'ORG-H-001', '1.3', 1, '北京市', '朝阳区测试路123号', '张医生', '13800138001', 'test@hospital.com', 1, '测试医院', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '测试生产企业', 'ORG-P-001', '1.1', 2, '上海市', '浦东新区工业园1号', '李经理', '13800138002', 'test@factory.com', 1, '测试工厂', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '测试经销商', 'ORG-D-001', '1.2', 3, '广州市', '天河区商业街88号', '王总', '13800138003', 'test@distributor.com', 0, '已禁用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 机构-医院关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_org_hospital;
CREATE TABLE IF NOT EXISTS sys_org_hospital (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    distributor_org_id BIGINT NOT NULL,
    hospital_org_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_distributor_hospital (distributor_org_id, hospital_org_id)
);

-- ------------------------------------------------------------
-- 部门-机构关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dept_org;
CREATE TABLE IF NOT EXISTS sys_dept_org (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dept_org (dept_id, org_id)
);

-- ============================================================
-- 部门表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dept_name           VARCHAR(128)    NOT NULL COMMENT '部门名称',
    dept_code           VARCHAR(32)     NOT NULL COMMENT '部门编码',
    dept_type           VARCHAR(10)     NOT NULL DEFAULT '6.1' COMMENT '字典编码：6.1=企业部门,6.2=业务部门',
    leader_user_id      BIGINT          COMMENT '部门负责人用户ID',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    -- 通用字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_name (dept_name),
    KEY idx_dept_status (status)
);

-- 插入部门测试数据
INSERT INTO sys_dept (id, dept_name, dept_code, dept_type, leader_user_id, status, remark, create_time, update_time, is_deleted) VALUES
(1, '研发部', 'DEPT-001', '6.1', NULL, 1, '研发部门', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '市场部', 'DEPT-002', '6.1', NULL, 1, '市场部门', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '销售部', 'DEPT-003', '6.1', NULL, 1, '销售部门', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, '已禁用部门', 'DEPT-004', '6.1', NULL, 0, '已禁用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 角色表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_name           VARCHAR(64)     NOT NULL COMMENT '角色名称',
    role_code           VARCHAR(32)     NOT NULL COMMENT '角色编码',
    role_desc           VARCHAR(256)    COMMENT '角色描述',
    account_type        VARCHAR(10)     NOT NULL COMMENT '账户分类（字典编码：6.1=企业账户，6.2=业务账户）',
    data_scope_type     VARCHAR(16)     NOT NULL DEFAULT 'org' COMMENT '数据权限范围（self/hospitals/org/all）',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    -- 通用字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_role_account_type (account_type),
    KEY idx_role_status (status)
);

-- 插入角色测试数据
INSERT INTO sys_role (id, role_name, role_code, role_desc, account_type, data_scope_type, status, remark, create_time, update_time, is_deleted) VALUES
(1, '公司管理员', 'ROLE_ADMIN', '系统管理员，拥有全部系统功能', 1, 'all', 1, '适用于生产企业', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '设计师', 'ROLE_DESIGNER', '负责设计工作', 1, 'org', 1, '适用于生产企业', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '生产员', 'ROLE_PRODUCTION', '负责生产加工', 1, 'org', 1, '适用于生产企业', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, '业务员', 'ROLE_SALES', '负责订单开拓、客户维护', 1, 'hospitals', 1, '适用于经销商', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, '医生', 'ROLE_DOCTOR', '医生、查看数据', 2, 'self', 1, '适用于医疗机构', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, '机构管理员', 'ROLE_ORG_ADMIN', '外部机构的管理员', 2, 'org', 1, '适用于外部用户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(7, '机构用户', 'ROLE_ORG_USER', '外部机构普通用户', 2, 'self', 1, '适用于外部用户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(8, '已禁用角色', 'ROLE_DISABLED', '已禁用角色', 1, 'org', 0, '测试用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username            VARCHAR(32)     NOT NULL COMMENT '用户名（登录账号）',
    password            VARCHAR(128)    NOT NULL COMMENT '登录密码（BCrypt加密）',
    real_name           VARCHAR(32)     NOT NULL COMMENT '真实姓名',
    phone               VARCHAR(20)     NOT NULL COMMENT '手机号',
    email               VARCHAR(64)     COMMENT '邮箱',
    sex                 TINYINT         COMMENT '性别',
    avatar              VARCHAR(512)    COMMENT '头像路径',
    account_type        VARCHAR(10)     NOT NULL COMMENT '账户分类（字典编码：6.1=企业账户，6.2=业务账户）',
    org_id              BIGINT          NOT NULL COMMENT '所属机构ID',
    org_name            VARCHAR(128)    COMMENT '所属机构名称（冗余字段）',
    dept_id             BIGINT          COMMENT '所属部门ID',
    dept_name           VARCHAR(128)    COMMENT '所属部门名称（冗余字段）',
    role_id             BIGINT          COMMENT '关联角色ID',
    role_name           VARCHAR(64)     COMMENT '关联角色名称（冗余字段）',
    role_code           VARCHAR(32)     COMMENT '关联角色编码（冗余字段）',

    -- 扩展字段
    employee_no         VARCHAR(32)     COMMENT '工号',
    specialty           VARCHAR(255)    COMMENT '专业方向（多选逗号拼接，如 7.1,7.2）',
    qualification       VARCHAR(256)    COMMENT '资质证书信息',
    settlement_type     TINYINT         COMMENT '结算类型',

    -- 状态
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',

    -- 账户安全
    login_fail_count    TINYINT         DEFAULT 0 COMMENT '连续登录失败次数',
    lock_time           DATETIME        COMMENT '账户锁定时间',

    remark              VARCHAR(512)    COMMENT '备注说明',

    -- 列配置
    order_column_settings  TEXT         COMMENT '订单列配置（JSON，用户个人自定义列显示设置）',
    design_column_settings TEXT         COMMENT '设计工单列配置（JSON，用户个人自定义列显示设置）',

    -- 通用字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_user_org_id (org_id),
    KEY idx_user_dept_id (dept_id),
    KEY idx_user_role_id (role_id),
    KEY idx_user_account_type (account_type),
    KEY idx_user_status (status)
);

-- 插入用户测试数据（密码均为 bcrypt 加密后的 "123456"）
-- bcrypt 加密：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO sys_user (id, username, password, real_name, phone, email, sex, avatar, account_type, org_id, org_name, dept_id, dept_name, role_id, role_name, role_code, employee_no, specialty, qualification, settlement_type, status, remark, create_time, update_time, is_deleted) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800000001', 'admin@test.com', 1, NULL, 1, 1, '测试医疗机构', NULL, NULL, 1, '超级管理员', 'admin', 'A001', NULL, '高级工程师', 3, 1, '测试管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'designer1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '设计师张三', '13800000002', 'designer@test.com', 1, NULL, 1, 1, '测试医疗机构', 1, '设计部', 2, '设计师', 'designer', 'D001', '7.1', '口腔修复专家', 1, 1, '设计师用户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'sales1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '业务员李四', '13800000003', 'sales@test.com', 1, NULL, 1, 2, '测试生产企业', NULL, NULL, 4, '销售员', 'sales', 'S001', NULL, NULL, 2, 1, '经销商业务员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 'doctor1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '医生王五', '13800000004', 'doctor@test.com', 1, NULL, 1, 1, '测试医疗机构', NULL, NULL, 5, '医生', 'doctor', 'DOC001', '7.1.3', '正畸主治医师', 3, 1, '医院医生', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 'org_admin1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '机构管理员', '13800000005', 'orgadmin@test.com', 2, NULL, 2, 1, '测试医疗机构', NULL, NULL, 6, '外部管理员', 'org_admin', NULL, NULL, NULL, NULL, 1, '外部机构管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, 'disabled_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '已禁用用户', '13800000006', 'disabled@test.com', 1, NULL, 1, 1, '测试医疗机构', NULL, NULL, 1, '超级管理员', 'admin', NULL, NULL, NULL, NULL, 0, '已禁用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 系统配置表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    config_key      VARCHAR(64)     NOT NULL COMMENT '配置键',
    config_name     VARCHAR(128)    NOT NULL COMMENT '配置名称',
    config_value    TEXT            COMMENT '配置值',
    config_type     VARCHAR(32)     DEFAULT 'string' COMMENT '配置类型（string/number/boolean/json）',
    config_group    VARCHAR(32)     DEFAULT 'system' COMMENT '配置分组（system/security/other）',
    config_desc     VARCHAR(256)    COMMENT '配置说明',
    is_system       TINYINT         DEFAULT 0 COMMENT '是否系统内置（0=否，1=是）',
    is_public       TINYINT         DEFAULT 1 COMMENT '是否公开（0=私密，1=公开）',
    sort            INT             DEFAULT 0 COMMENT '排序',
    status          TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',

    -- 通用字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_config_group (config_group),
    KEY idx_config_type (config_type),
    KEY idx_config_status (status)
);

-- 插入系统配置测试数据
INSERT INTO sys_config (id, config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status, create_time, update_time, is_deleted) VALUES
(1, 'default.password', '默认密码', '123456', 'string', 'security', '新用户初始密码', 1, 0, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'login.max.failures', '最大连续登录失败次数', '5', 'number', 'security', '连续失败后锁定账号', 1, 0, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'login.lock.duration', '登录锁定时长', '15', 'number', 'security', '自动解锁时间（分钟）', 1, 0, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 'sms.send.interval', '短信发送间隔', '60', 'number', 'security', '同一手机号发送间隔（秒）', 1, 0, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 'test.config', '测试配置', 'testValue', 'string', 'system', '测试用配置', 0, 1, 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);


-- ============================================================
-- 资源表（整合菜单和按钮权限）
-- resource_type: 1=一级菜单, 2=二级菜单, 3=按钮
-- ============================================================
DROP TABLE IF EXISTS sys_resource;
CREATE TABLE sys_resource (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父级ID（0=根节点/一级菜单）',
    resource_name   VARCHAR(64)     NOT NULL COMMENT '资源名称',
    resource_code   VARCHAR(64)     NOT NULL COMMENT '资源编码（唯一标识，如：system:org）',
    resource_type   TINYINT         NOT NULL COMMENT '资源类型（1=一级菜单，2=二级菜单，3=按钮）',

    -- 菜单相关字段（按钮类型可为空）
    icon            VARCHAR(128)    COMMENT '菜单图标',
    path            VARCHAR(256)    COMMENT '路由路径（按钮类型可为空）',
    component       VARCHAR(256)    COMMENT '组件路径（按钮类型可为空）',
    redirect        VARCHAR(256)    COMMENT '重定向路径（可选）',

    -- 排序与状态
    sort            INT             DEFAULT 0 COMMENT '排序（同级内升序）',
    visible         TINYINT         DEFAULT 1 COMMENT '显示状态（0=隐藏，1=显示）',
    status          TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark          VARCHAR(512)    COMMENT '备注说明',

    -- 通用字段
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_resource_parent_id (parent_id),
    KEY idx_resource_type (resource_type),
    KEY idx_resource_status (status)
);

-- 插入资源测试数据
-- 一级菜单
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, redirect, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 0, '系统管理', 'system', 1, 'Setting', '/system', NULL, '/system/index', 100, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(2, 0, '权限管理', 'permission', 1, 'Lock', '/permission', NULL, '/permission/index', 90, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(3, 0, '基础数据', 'basedata', 1, 'Database', '/basedata', NULL, '/basedata/index', 80, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 二级菜单（系统管理）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(101, 1, '机构管理', 'system:org', 2, 'Office', '/system/org', 'system/org/index', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(102, 1, '部门管理', 'system:dept', 2, 'Dept', '/system/dept', 'system/dept/index', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(103, 1, '用户管理', 'system:user', 2, 'User', '/system/user', 'system/user/index', 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(104, 1, '角色管理', 'system:role', 2, 'Role', '/system/role', 'system/role/index', 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 二级菜单（权限管理）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(201, 2, '资源管理', 'permission:resource', 2, 'Menu', '/permission/resource', 'permission/resource/index', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(202, 2, '登录日志', 'permission:loginlog', 2, 'Log', '/permission/loginlog', 'permission/loginlog/index', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 二级菜单（基础数据）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(301, 3, '字典管理', 'basedata:dict', 2, 'Dict', '/basedata/dict', 'basedata/dict/index', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(302, 3, '系统配置', 'basedata:config', 2, 'Config', '/basedata/config', 'basedata/config/index', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 按钮权限（机构管理）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1001, 101, '查看机构列表', 'system:org:list', 3, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1002, 101, '查看机构详情', 'system:org:detail', 3, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1003, 101, '新增机构', 'system:org:add', 3, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1004, 101, '编辑机构', 'system:org:edit', 3, 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1005, 101, '删除机构', 'system:org:delete', 3, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1006, 101, '启用/停用机构', 'system:org:status', 3, 6, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 按钮权限（部门管理）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1101, 102, '查看部门列表', 'system:dept:list', 3, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1102, 102, '查看部门详情', 'system:dept:detail', 3, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1103, 102, '新增部门', 'system:dept:add', 3, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1104, 102, '编辑部门', 'system:dept:edit', 3, 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1105, 102, '删除部门', 'system:dept:delete', 3, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1106, 102, '启用/停用部门', 'system:dept:status', 3, 6, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 按钮权限（用户管理）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1201, 103, '查看用户列表', 'system:user:list', 3, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1202, 103, '查看用户详情', 'system:user:detail', 3, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1203, 103, '新增用户', 'system:user:add', 3, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1204, 103, '编辑用户', 'system:user:edit', 3, 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1205, 103, '删除用户', 'system:user:delete', 3, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1206, 103, '重置密码', 'system:user:reset-password', 3, 6, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1207, 103, '修改状态', 'system:user:status', 3, 7, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1208, 103, '修改密码', 'system:user:change-password', 3, 8, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 按钮权限（角色管理）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1301, 104, '查看角色列表', 'system:role:list', 3, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1302, 104, '查看角色详情', 'system:role:detail', 3, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1303, 104, '新增角色', 'system:role:add', 3, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1304, 104, '编辑角色', 'system:role:edit', 3, 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1305, 104, '删除角色', 'system:role:delete', 3, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1306, 104, '修改状态', 'system:role:status', 3, 6, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1307, 104, '分配资源', 'system:role:assign-resource', 3, 7, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 按钮权限（资源管理）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1401, 201, '查看资源列表', 'permission:resource:list', 3, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1402, 201, '查看资源详情', 'permission:resource:detail', 3, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1403, 201, '新增资源', 'permission:resource:add', 3, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1404, 201, '编辑资源', 'permission:resource:edit', 3, 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1405, 201, '删除资源', 'permission:resource:delete', 3, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 按钮权限（登录日志）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1501, 202, '查看登录日志', 'permission:loginlog:list', 3, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1502, 202, '查看登录详情', 'permission:loginlog:detail', 3, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1503, 202, '导出登录日志', 'permission:loginlog:export', 3, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 按钮权限（字典管理）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1601, 301, '查看字典列表', 'basedata:dict:list', 3, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1602, 301, '查看字典详情', 'basedata:dict:detail', 3, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1603, 301, '新增字典', 'basedata:dict:add', 3, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1604, 301, '编辑字典', 'basedata:dict:edit', 3, 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1605, 301, '删除字典', 'basedata:dict:delete', 3, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- 按钮权限（系统配置）
INSERT INTO sys_resource (id, parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1701, 302, '查看配置列表', 'basedata:config:list', 3, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1702, 302, '查看配置详情', 'basedata:config:detail', 3, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1703, 302, '新增配置', 'basedata:config:add', 3, 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1704, 302, '编辑配置', 'basedata:config:edit', 3, 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1705, 302, '删除配置', 'basedata:config:delete', 3, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(1706, 302, '刷新配置', 'basedata:config:refresh', 3, 6, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);


-- ============================================================
-- 角色资源关联表
-- ============================================================
DROP TABLE IF EXISTS sys_role_resource;
CREATE TABLE sys_role_resource (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id          BIGINT          NOT NULL COMMENT '角色ID',
    resource_id      BIGINT          NOT NULL COMMENT '资源ID',

    PRIMARY KEY (id),
    UNIQUE KEY uk_role_resource (role_id, resource_id),
    KEY idx_role_id (role_id),
    KEY idx_resource_id (resource_id)
);

-- 插入角色资源关联测试数据（超级管理员角色关联所有资源）
INSERT INTO sys_role_resource (role_id, resource_id)
SELECT 1, id FROM sys_resource;


-- ============================================================
-- 登录日志表
-- ============================================================
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT          COMMENT '用户ID',
    username        VARCHAR(64)     COMMENT '用户名',
    login_type      VARCHAR(16)     COMMENT '登录方式（PASSWORD/PHONE/EMAIL）',
    ip              VARCHAR(64)     COMMENT '登录IP',
    user_agent      VARCHAR(512)    COMMENT 'User-Agent（浏览器/设备信息）',
    login_time      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    login_status    TINYINT         NOT NULL COMMENT '登录结果（1=成功，0=失败）',
    fail_reason     VARCHAR(256)    COMMENT '失败原因',

    PRIMARY KEY (id),
    KEY idx_login_user_id (user_id),
    KEY idx_login_time (login_time)
);

-- 插入登录日志测试数据
INSERT INTO sys_login_log (user_id, username, ip, user_agent, login_time, login_status, fail_reason) VALUES
(1, 'admin', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', CURRENT_TIMESTAMP, 1, NULL),
(2, 'designer1', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 1, NULL),
(3, 'sales1', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 0, '密码错误');

-- ------------------------------------------------------------
-- 用户-医院关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user_hospital;
CREATE TABLE sys_user_hospital (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    hospital_id         BIGINT          NOT NULL COMMENT '医院ID',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_hospital (user_id, hospital_id),
    KEY idx_user_id (user_id),
    KEY idx_hospital_id (hospital_id)
);
