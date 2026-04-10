-- ============================================================
-- 医工宝系统测试 DDL
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

-- 插入测试数据
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES
-- 机构类型
(1, 0, '1', '机构类型', NULL, 1, 1, 1, '字典类型', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(2, 1, '1.1', '生产企业', 'production', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(3, 1, '1.2', '经销商', 'distributor', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(4, 1, '1.3', '医疗机构', 'medical', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(5, 1, '1.4', '其他', 'other', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
-- 机构编码前缀
(6, 0, '2', '机构编码前缀', NULL, 1, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(7, 6, '2.1', '生产企业编码前缀', 'ORG-P-', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(8, 6, '2.2', '经销商编码前缀', 'ORG-D-', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(9, 6, '2.3', '医疗机构编码前缀', 'ORG-H-', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(10, 6, '2.4', '其他编码前缀', 'ORG-O-', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
-- 医院等级
(11, 0, '3', '医院等级', NULL, 1, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(12, 11, '3.1', '三甲', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(13, 11, '3.2', '三乙', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(14, 11, '3.3', '二甲', '3', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(15, 11, '3.4', '二乙', '4', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(16, 11, '3.5', '其他', '5', 2, 5, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
-- 医院类型
(17, 0, '4', '医院类型', NULL, 1, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(18, 17, '4.1', '综合', '1', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(19, 17, '4.2', '专科', '2', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
-- 代理产品线
(20, 0, '5', '代理产品线', NULL, 1, 5, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(21, 20, '5.1', '医疗器械', 'medical_device', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(22, 20, '5.2', '药品', 'drug', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(23, 20, '5.3', '耗材', 'consumable', 2, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(24, 20, '5.4', '设备', 'equipment', 2, 4, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

-- ------------------------------------------------------------
-- 机构表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_org;
CREATE TABLE sys_org (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    org_name            VARCHAR(128)    NOT NULL COMMENT '机构名称',
    org_code            VARCHAR(32)     NOT NULL COMMENT '机构编码',
    org_type            VARCHAR(8)          NOT NULL COMMENT '机构类型（字典编码，如：1.1/1.2/1.3/1.4）',
    area_id            BIGINT          COMMENT '所属地区ID',
    area_name          VARCHAR(64)     COMMENT '所属地区名称',
    address            VARCHAR(256)    COMMENT '详细地址',
    contact            VARCHAR(32)     NOT NULL COMMENT '联系人',
    phone              VARCHAR(32)     NOT NULL COMMENT '联系电话',
    email              VARCHAR(64)     COMMENT '联系邮箱',
    credit_code        VARCHAR(32)     COMMENT '统一社会信用代码',
    business_license   VARCHAR(512)    COMMENT '营业执照',
    agent_area         VARCHAR(64)     COMMENT '代理区域',
    agent_product_line VARCHAR(256)    COMMENT '代理产品线',
    hospital_level      VARCHAR(16)      COMMENT '医院等级（字典：dict_code=3，值如 3.1/3.2）',
    hospital_type       VARCHAR(16)      COMMENT '医院类型（字典：dict_code=4，值如 4.1/4.2）',
    status             TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark            VARCHAR(512)    COMMENT '备注说明',
    create_time        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by          BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted         TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id)
);

-- ============================================================
-- 地区表（省市区）
-- 说明：存储中国行政区划数据，层级关系：省→市→区/县
-- ============================================================
DROP TABLE IF EXISTS sys_area;
CREATE TABLE sys_area (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    level               TINYINT         NOT NULL COMMENT '层级（1=省/直辖市，2=市，3=区/县）',
    parent_code         BIGINT          NOT NULL DEFAULT 0 COMMENT '父级行政代码',
    area_code           BIGINT          NOT NULL DEFAULT 0 COMMENT '行政代码',
    zip_code            INT             NOT NULL DEFAULT 0 COMMENT '邮政编码',
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

-- 插入测试数据（地区，与 cnarea_2023 结构一致）
INSERT INTO sys_area (id, level, parent_code, area_code, zip_code, city_code, name, short_name, merger_name, pinyin, lng, lat) VALUES
-- 省份/直辖市
(1, 1, 0, 110000, 100000, '010', '北京市', '北京', '中国,北京', 'beijing', 116.407526, 39.904030),
(2, 1, 0, 310000, 200000, '021', '上海市', '上海', '中国,上海', 'shanghai', 121.473701, 31.230416),
(3, 1, 0, 330000, 310000, '', '浙江省', '浙江', '中国,浙江', 'zhejiang', 120.153576, 30.287459),
(4, 1, 0, 440000, 510000, '', '广东省', '广东', '中国,广东', 'guangdong', 113.280637, 23.125178),
-- 城市
(11, 2, 110000, 110100, 100000, '010', '北京市', '北京', '中国,北京,北京市', 'beijing', 116.407526, 39.904030),
(21, 2, 310000, 310100, 200000, '021', '上海市', '上海', '中国,上海,上海市', 'shanghai', 121.473701, 31.230416),
(31, 2, 330000, 330100, 310000, '0571', '杭州市', '杭州', '中国,浙江,杭州市', 'hangzhou', 120.155070, 30.274084),
(32, 2, 330000, 330200, 315000, '0574', '宁波市', '宁波', '中国,浙江,宁波市', 'ningbo', 121.544007, 29.868336),
(33, 2, 330000, 330300, 325000, '0577', '温州市', '温州', '中国,浙江,温州市', 'wenzhou', 120.699366, 28.000575),
(41, 2, 440000, 440100, 510000, '020', '广州市', '广州', '中国,广东,广州市', 'guangzhou', 113.264385, 23.129112),
(42, 2, 440000, 440300, 518000, '0755', '深圳市', '深圳', '中国,广东,深圳市', 'shenzhen', 114.057868, 22.543099),
-- 区县
(111, 3, 110100, 110101, 100010, '010', '东城区', '东城', '中国,北京,北京市,东城区', 'dongcheng', 116.416357, 39.928353),
(112, 3, 110100, 110102, 100032, '010', '西城区', '西城', '中国,北京,北京市,西城区', 'xicheng', 116.365868, 39.912289),
(113, 3, 110100, 110105, 100020, '010', '朝阳区', '朝阳', '中国,北京,北京市,朝阳区', 'chaoyang', 116.443108, 39.921470),
(114, 3, 110100, 110108, 100089, '010', '海淀区', '海淀', '中国,北京,北京市,海淀区', 'haidian', 116.298056, 39.959893),
(311, 3, 330100, 330102, 310002, '0571', '上城区', '上城', '中国,浙江,杭州市,上城区', 'shangcheng', 120.169219, 30.242312),
(312, 3, 330100, 330105, 310011, '0571', '拱墅区', '拱墅', '中国,浙江,杭州市,拱墅区', 'gongshu', 120.142059, 30.319037),
(313, 3, 330100, 330106, 310013, '0571', '西湖区', '西湖', '中国,浙江,杭州市,西湖区', 'xihu', 120.130203, 30.259324),
(314, 3, 330100, 330108, 310051, '0571', '滨江区', '滨江', '中国,浙江,杭州市,滨江区', 'binjiang', 120.211816, 30.208560),
(321, 3, 330200, 330203, 315000, '0574', '海曙区', '海曙', '中国,浙江,宁波市,海曙区', 'haishu', 121.550485, 29.874724),
(322, 3, 330200, 330205, 315020, '0574', '江北区', '江北', '中国,浙江,宁波市,江北区', 'jiangbei', 121.555227, 29.886757),
(411, 3, 440100, 440103, 510145, '020', '荔湾区', '荔湾', '中国,广东,广州市,荔湾区', 'liwan', 113.244261, 23.125981),
(412, 3, 440100, 440104, 510030, '020', '越秀区', '越秀', '中国,广东,广州市,越秀区', 'yuexiu', 113.266841, 23.129162),
(413, 3, 440100, 440105, 510220, '020', '海珠区', '海珠', '中国,广东,广州市,海珠区', 'haizhu', 113.317388, 23.083801);

-- ============================================================
-- 重建部位表（rebuild_body_part）
-- ============================================================
DROP TABLE IF EXISTS rebuild_body_part;
CREATE TABLE rebuild_body_part (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    parent_id       BIGINT          NOT NULL DEFAULT 0 COMMENT '父级ID（0=顶级身体区域）',
    name            VARCHAR(100)    NOT NULL COMMENT '部位名称',
    code            VARCHAR(50)     NOT NULL COMMENT '部位编码',
    level           INT             NOT NULL DEFAULT 1 COMMENT '层级（1=身体区域，2=具体部位）',
    sort            INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status          TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark          VARCHAR(512)    DEFAULT NULL COMMENT '备注说明',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    KEY idx_body_part_parent_id (parent_id),
    KEY idx_body_part_level (level),
    KEY idx_body_part_status (status)
);

INSERT INTO rebuild_body_part (id, parent_id, name, code, level, sort, status, remark, create_time, update_time, is_deleted) VALUES
(1, 0, '头部', 'BP_001', 1, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, '前额', 'BP_001_001', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, '后脑', 'BP_001_002', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 0, '躯干', 'BP_002', 1, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 重建项目表（rebuild_project）
-- ============================================================
DROP TABLE IF EXISTS rebuild_project;
CREATE TABLE rebuild_project (
    id                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    body_part_id          BIGINT          NOT NULL COMMENT '关联部位ID',
    parent_id             BIGINT          NOT NULL DEFAULT 0 COMMENT '父项目ID（0=顶级重建项目）',
    name                  VARCHAR(100)    NOT NULL COMMENT '项目名称',
    code                  VARCHAR(50)     NOT NULL COMMENT '项目编码',
    level                 INT             NOT NULL DEFAULT 1 COMMENT '层级（1=重建项目，2=子重建项目）',
    standard_price        DECIMAL(10,2)   DEFAULT NULL COMMENT '标准价格（元）',
    urgent_price          DECIMAL(10,2)   DEFAULT NULL COMMENT '加急价格（元）',
    category              VARCHAR(50)     DEFAULT NULL COMMENT '项目分类（如：模型、导板）',
    estimated_hours       DECIMAL(8,2)    DEFAULT NULL COMMENT '预计耗时（小时，支持小数）',
    description           TEXT            DEFAULT NULL COMMENT '项目说明模板',
    forming_requirements  TEXT            DEFAULT NULL COMMENT '成形需求模板',
    sort                  INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status                TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    specialty             VARCHAR(64)     DEFAULT NULL COMMENT '专业方向字典编码（单值，如 7.1）',
    remark                VARCHAR(500)    DEFAULT NULL COMMENT '备注说明',
    create_time           DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by             BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by             BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted            TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    KEY idx_project_body_part_id (body_part_id),
    KEY idx_project_parent_id (parent_id),
    KEY idx_project_level (level),
    KEY idx_project_status (status)
);

INSERT INTO rebuild_project (id, body_part_id, parent_id, name, code, level, standard_price, urgent_price, category, estimated_hours, specialty, sort, status, create_time, update_time, is_deleted) VALUES
(1, 1, 0, '颅骨重建', 'RP_1_001', 1, 5000.00, 7500.00, '模型', 8.5, '7.1', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 1, '颞骨重建', 'RP_1_001_001', 2, 3000.00, 4500.00, '导板', 5.0, '7.1', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 0, '面部轮廓', 'RP_1_002', 1, 8000.00, 12000.00, '模型', 12.0, '7.2', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 医院科室表（hospital_dept）
-- ============================================================
DROP TABLE IF EXISTS hospital_dept;
CREATE TABLE hospital_dept (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    hospital_dept_code  VARCHAR(50)     NOT NULL COMMENT '科室编码',
    hospital_dept_name  VARCHAR(100)    NOT NULL COMMENT '科室名称',
    sort                INT             DEFAULT 0 COMMENT '排序',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注说明',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    KEY idx_hdept_code (hospital_dept_code)
);

INSERT INTO hospital_dept (id, hospital_dept_code, hospital_dept_name, sort, status, remark, create_time, update_time, is_deleted) VALUES
(1, 'HDEPT-0001', '骨科', 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'HDEPT-0002', '口腔科', 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 医生表（doctor）
-- ============================================================
DROP TABLE IF EXISTS doctor;
CREATE TABLE doctor (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    doctor_name         VARCHAR(50)     NOT NULL COMMENT '医生姓名',
    doctor_phone       VARCHAR(20)     DEFAULT NULL COMMENT '医生电话',
    hospital_id        BIGINT          NOT NULL COMMENT '所属医院ID',
    hospital_dept_id    BIGINT          DEFAULT NULL COMMENT '所属科室ID',
    creator_id          BIGINT          DEFAULT NULL COMMENT '创建该医生记录的业务员ID',
    order_count         INT             DEFAULT 0 COMMENT '关联订单数量',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注说明',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    KEY idx_doctor_hospital (hospital_id),
    KEY idx_doctor_dept (hospital_dept_id),
    KEY idx_doctor_creator (creator_id)
);

INSERT INTO doctor (id, doctor_name, doctor_phone, hospital_id, hospital_dept_id, creator_id, order_count, status, remark, create_time, update_time, is_deleted) VALUES
(1, '张三', '13800138001', 1, 1, 1, 5, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '李四', '13800138002', 1, 2, 1, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 产品型号表（product）
-- ============================================================
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_code        VARCHAR(50)      NOT NULL COMMENT '产品型号编码',
    product_name        VARCHAR(100)     NOT NULL COMMENT '产品名称',
    category            VARCHAR(50)      DEFAULT NULL COMMENT '产品分类',
    spec                VARCHAR(100)    DEFAULT NULL COMMENT '规格',
    cert_id             BIGINT          DEFAULT NULL COMMENT '关联注册证ID',
    material            VARCHAR(100)     DEFAULT NULL COMMENT '材质',
    color_options       VARCHAR(500)    DEFAULT NULL COMMENT '可选颜色（JSON数组）',
    price               DECIMAL(12,2)   DEFAULT NULL COMMENT '标准价格',
    image_url           VARCHAR(500)     DEFAULT NULL COMMENT '产品图片URL',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注说明',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    KEY idx_product_code (product_code),
    KEY idx_product_cert (cert_id),
    KEY idx_product_category (category)
);

INSERT INTO product (id, product_code, product_name, category, spec, cert_id, material, price, status, create_time, update_time, is_deleted) VALUES
(1, 'P-0001', '膝关节假体', '关节', '标准型', 1, '钛合金', 50000.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'P-0002', '髋关节假体', '关节', '加厚型', 1, '钴铬合金', 65000.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 注册证表（registration_cert）
-- ============================================================
DROP TABLE IF EXISTS registration_cert;
CREATE TABLE registration_cert (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    cert_code          VARCHAR(50)      NOT NULL COMMENT '注册证号',
    cert_name          VARCHAR(100)    NOT NULL COMMENT '注册证名称',
    valid_from          DATE            DEFAULT NULL COMMENT '有效期开始',
    valid_to            DATE            DEFAULT NULL COMMENT '有效期截止',
    cert_file_url      VARCHAR(500)    DEFAULT NULL COMMENT '注册证扫描件URL',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=过期，1=有效）',
    remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注说明',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    KEY idx_cert_code (cert_code),
    KEY idx_cert_status (status)
);

INSERT INTO registration_cert (id, cert_code, cert_name, valid_from, valid_to, status, remark, create_time, update_time, is_deleted) VALUES
(1, 'REG-20260001', '医疗器械注册证', '2026-01-01', '2028-12-31', 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'REG-20230002', '过期的注册证', '2023-01-01', '2023-12-31', 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 编码规则表（sys_code_rule）
-- ============================================================
DROP TABLE IF EXISTS sys_code_rule;
CREATE TABLE sys_code_rule (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    rule_code      VARCHAR(50)      NOT NULL COMMENT '规则编码',
    rule_name      VARCHAR(100)    NOT NULL COMMENT '规则名称',
    prefix         VARCHAR(50)      DEFAULT NULL COMMENT '前缀',
    date_format    VARCHAR(50)      DEFAULT NULL COMMENT '日期格式',
    seq_length     INT             DEFAULT 6 COMMENT '序号长度',
    reset_type     VARCHAR(20)     DEFAULT 'NEVER' COMMENT '重置类型（DAY/MONTH/YEAR/NEVER）',
    current_value  BIGINT          DEFAULT 0 COMMENT '当前序号值',
    step           INT             DEFAULT 1 COMMENT '递增步长',
    status         TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark         VARCHAR(500)    DEFAULT NULL COMMENT '备注说明',
    create_time    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by      BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted     TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code),
    KEY idx_rule_status (status)
);

INSERT INTO sys_code_rule (id, rule_code, rule_name, prefix, date_format, seq_length, reset_type, step, status, create_time, update_time, is_deleted) VALUES
(1, 'BODY_PART_CODE', '部位编码', 'BP-', '{yyyy}{MM}', 4, 'NEVER', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'REBUILD_PROJECT_CODE', '重建项目编码', 'RP-', '{yyyy}{MM}', 4, 'NEVER', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'PRODUCT_CODE', '产品型号编码', 'P-', NULL, 4, 'NEVER', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ============================================================
-- 编码序号表（sys_code_sequence）
-- ============================================================
DROP TABLE IF EXISTS sys_code_sequence;
CREATE TABLE sys_code_sequence (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    rule_code      VARCHAR(50)      NOT NULL COMMENT '规则编码',
    biz_key        VARCHAR(64)     DEFAULT NULL COMMENT '业务标识（用于按业务维度隔离序号，如订单编号，为空表示全局序号）',
    current_seq    BIGINT          DEFAULT 0 COMMENT '当前序号',
    last_date      DATE            DEFAULT NULL COMMENT '上次重置日期',
    version        INT             DEFAULT 0 COMMENT '乐观锁版本号',
    create_time    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seq_rule_biz_key (rule_code, biz_key),
    KEY idx_seq_rule_code (rule_code),
    KEY idx_seq_last_date (last_date)
);

INSERT INTO sys_code_sequence (id, rule_code, biz_key, current_seq, last_date, version, create_time, update_time) VALUES
(1, 'BODY_PART_CODE', NULL, 0, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'REBUILD_PROJECT_CODE', NULL, 0, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'PRODUCT_CODE', NULL, 0, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
