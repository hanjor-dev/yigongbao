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
    UNIQUE KEY uk_dict_code (dict_code),
    KEY idx_parent_id (parent_id),
    KEY idx_level (level)
);

-- 插入字典测试数据
-- 机构类型（dict_code=1）
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 0, '1', '机构类型', NULL, 1, 0, 1, '机构类型字典', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(2, 1, '1.1', '生产企业', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(3, 1, '1.2', '经销商', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(4, 1, '1.3', '医疗机构', '3', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(5, 1, '1.4', '其他', '4', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),

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
(41, 40, '5.1', '医疗器械', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(42, 40, '5.2', '医用耗材', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(43, 40, '5.3', '药品', '3', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(44, 40, '5.4', '设备维修', '4', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

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
    org_type            TINYINT         NOT NULL COMMENT '机构类型',
    area_id             BIGINT          COMMENT '所属地区ID',
    area_name           VARCHAR(64)     COMMENT '所属地区名称',
    address             VARCHAR(256)    COMMENT '详细地址',
    contact             VARCHAR(32)     NOT NULL COMMENT '联系人',
    phone               VARCHAR(32)     NOT NULL COMMENT '联系电话',
    email               VARCHAR(64)     COMMENT '联系邮箱',
    credit_code         VARCHAR(32)     COMMENT '统一社会信用代码',
    business_license    VARCHAR(512)    COMMENT '营业执照',
    agent_area          VARCHAR(64)     COMMENT '代理区域',
    agent_product_line  VARCHAR(256)    COMMENT '代理产品线',
    hospital_level      TINYINT         COMMENT '医院等级',
    hospital_type       TINYINT         COMMENT '医院类型',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    -- 通用字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    UNIQUE KEY uk_org_name (org_name),
    KEY idx_org_type (org_type),
    KEY idx_area_id (area_id),
    KEY idx_status (status)
);

-- 插入机构测试数据
INSERT INTO sys_org (id, org_name, org_code, org_type, area_id, area_name, address, contact, phone, email, status, remark, create_time, update_time, is_deleted) VALUES
(1, '测试医疗机构', 'ORG-H-001', 3, 1, '北京市', '朝阳区测试路123号', '张医生', '13800138001', 'test@hospital.com', 1, '测试医院', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '测试生产企业', 'ORG-P-001', 1, 2, '上海市', '浦东新区工业园1号', '李经理', '13800138002', 'test@factory.com', 1, '测试工厂', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '测试经销商', 'ORG-D-001', 2, 3, '广州市', '天河区商业街88号', '王总', '13800138003', 'test@distributor.com', 0, '已禁用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 地区表（省市区，与 cnarea_2023 结构一致）
-- ============================================================
DROP TABLE IF EXISTS sys_area;
CREATE TABLE sys_area (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    level               TINYINT         NOT NULL COMMENT '层级（1=省/直辖市，2=市，3=区/县）',
    parent_code         BIGINT          NOT NULL DEFAULT 0 COMMENT '父级行政代码',
    area_code           BIGINT          NOT NULL DEFAULT 0 COMMENT '行政代码',
    zip_code            INT             UNSIGNED NOT NULL DEFAULT 0 COMMENT '邮政编码',
    city_code           CHAR(6)         NOT NULL DEFAULT '' COMMENT '区号',
    name                VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '名称',
    short_name          VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '简称',
    merger_name         VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '组合名',
    pinyin              VARCHAR(30)     NOT NULL DEFAULT '' COMMENT '拼音',
    lng                 DECIMAL(10,6)   NOT NULL DEFAULT 0 COMMENT '经度',
    lat                 DECIMAL(10,6)   NOT NULL DEFAULT 0 COMMENT '纬度',
    PRIMARY KEY (id),
    UNIQUE KEY uk_area_code (area_code),
    KEY idx_parent_code (parent_code),
    KEY idx_area_level (level)
);

-- 插入地区测试数据（parent_code/area_code 与 kakuilan/china_area_mysql 一致）
INSERT INTO sys_area (id, level, parent_code, area_code, zip_code, city_code, name, short_name, merger_name, pinyin, lng, lat) VALUES
-- 省份/直辖市（parent_code=0）
(1, 1, 0, 110000, 100000, '010', '北京市', '北京', '中国,北京', 'beijing', 116.407526, 39.904030),
(2, 1, 0, 310000, 200000, '021', '上海市', '上海', '中国,上海', 'shanghai', 121.473701, 31.230416),
(3, 1, 0, 330000, 310000, '', '浙江省', '浙江', '中国,浙江', 'zhejiang', 120.153576, 30.287459),
(4, 1, 0, 440000, 510000, '', '广东省', '广东', '中国,广东', 'guangdong', 113.280637, 23.125178),
-- 城市（parent_code=省 area_code）
(11, 2, 110000, 110100, 100000, '010', '北京市', '北京', '中国,北京,北京市', 'beijing', 116.407526, 39.904030),
(21, 2, 310000, 310100, 200000, '021', '上海市', '上海', '中国,上海,上海市', 'shanghai', 121.473701, 31.230416),
(31, 2, 330000, 330100, 310000, '0571', '杭州市', '杭州', '中国,浙江,杭州市', 'hangzhou', 120.155070, 30.274084),
(32, 2, 330000, 330200, 315000, '0574', '宁波市', '宁波', '中国,浙江,宁波市', 'ningbo', 121.544007, 29.868336),
(41, 2, 440000, 440100, 510000, '020', '广州市', '广州', '中国,广东,广州市', 'guangzhou', 113.264385, 23.129112),
-- 区县（parent_code=市 area_code）
(111, 3, 110100, 110101, 100010, '010', '东城区', '东城', '中国,北京,北京市,东城区', 'dongcheng', 116.416357, 39.928353),
(112, 3, 110100, 110105, 100020, '010', '朝阳区', '朝阳', '中国,北京,北京市,朝阳区', 'chaoyang', 116.443108, 39.921470),
(311, 3, 330100, 330102, 310002, '0571', '上城区', '上城', '中国,浙江,杭州市,上城区', 'shangcheng', 120.169219, 30.242312),
(312, 3, 330100, 330105, 310011, '0571', '拱墅区', '拱墅', '中国,浙江,杭州市,拱墅区', 'gongshu', 120.142059, 30.319037),
(411, 3, 440100, 440103, 510145, '020', '荔湾区', '荔湾', '中国,广东,广州市,荔湾区', 'liwan', 113.244261, 23.125981);

-- ------------------------------------------------------------
-- 部门表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dept_name           VARCHAR(128)    NOT NULL COMMENT '部门名称',
    dept_code           VARCHAR(32)     NOT NULL COMMENT '部门编码',
    org_id              BIGINT          NOT NULL COMMENT '所属机构ID',
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
    UNIQUE KEY uk_dept_code (dept_code),
    UNIQUE KEY uk_dept_name_org (dept_name, org_id, is_deleted),
    KEY idx_dept_org_id (org_id),
    KEY idx_dept_status (status)
);

-- 插入部门测试数据
INSERT INTO sys_dept (id, dept_name, dept_code, org_id, leader_user_id, status, remark, create_time, update_time, is_deleted) VALUES
(1, '研发部', 'DEPT-001', 1, NULL, 1, '研发部门', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '市场部', 'DEPT-002', 1, NULL, 1, '市场部门', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '销售部', 'DEPT-003', 2, NULL, 1, '销售部门', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, '已禁用部门', 'DEPT-004', 1, NULL, 0, '已禁用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
