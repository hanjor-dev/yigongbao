-- ============================================================
-- 医工宝基础模块测试 DDL（module-basic）
-- H2 内存数据库建表语句
-- ============================================================

-- ------------------------------------------------------------
-- 地区表（省市区，与 cnarea_2023 结构一致）
-- ------------------------------------------------------------
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
    UNIQUE KEY uk_sys_area_code (area_code),
    KEY idx_sys_area_parent_code (parent_code),
    KEY idx_sys_area_level (level)
);

-- 插入地区测试数据
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
(41, 2, 440000, 440100, 510000, '020', '广州市', '广州', '中国,广东,广州市', 'guangzhou', 113.264385, 23.129112),
-- 区县
(111, 3, 110100, 110101, 100010, '010', '东城区', '东城', '中国,北京,北京市,东城区', 'dongcheng', 116.416357, 39.928353),
(112, 3, 110100, 110105, 100020, '010', '朝阳区', '朝阳', '中国,北京,北京市,朝阳区', 'chaoyang', 116.443108, 39.921470),
(311, 3, 330100, 330102, 310002, '0571', '上城区', '上城', '中国,浙江,杭州市,上城区', 'shangcheng', 120.169219, 30.242312),
(312, 3, 330100, 330105, 310011, '0571', '拱墅区', '拱墅', '中国,浙江,杭州市,拱墅区', 'gongshu', 120.142059, 30.319037),
(411, 3, 440100, 440103, 510145, '020', '荔湾区', '荔湾', '中国,广东,广州市,荔湾区', 'liwan', 113.244261, 23.125981);

-- ------------------------------------------------------------
-- 医院表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS hospital;
CREATE TABLE hospital (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    hospital_name       VARCHAR(128)    NOT NULL COMMENT '医院名称',
    hospital_code       VARCHAR(32)     NOT NULL COMMENT '医院编码',
    area_id             BIGINT          NOT NULL COMMENT '所属地区ID',
    area_name           VARCHAR(64)     COMMENT '地区名称',
    full_area_name      VARCHAR(256)    COMMENT '完整地区路径',
    hospital_level      VARCHAR(16)      COMMENT '医院等级（字典：dict_code=3，值如3.1/3.2）',
    hospital_type       VARCHAR(16)      COMMENT '医院类型（字典：dict_code=4，值如4.1/4.2）',
    contact             VARCHAR(32)     NOT NULL COMMENT '联系人',
    phone               VARCHAR(32)     NOT NULL COMMENT '联系电话',
    email               VARCHAR(64)     COMMENT '电子邮箱',
    address             VARCHAR(256)    COMMENT '详细地址',
    credit_code         VARCHAR(32)     COMMENT '统一社会信用代码',
    business_license    VARCHAR(512)   COMMENT '营业执照路径',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)   COMMENT '备注说明',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_hospital_area_id (area_id),
    KEY idx_hospital_level (hospital_level),
    KEY idx_hospital_type (hospital_type),
    KEY idx_hospital_status (status)
);

-- 插入医院测试数据
INSERT INTO hospital (id, hospital_name, hospital_code, area_id, area_name, full_area_name, hospital_level, hospital_type, contact, phone, email, address, status, create_time, update_time, is_deleted) VALUES
(1, '北京协和医院', 'HOS-001', 111, '东城区', '中国,北京,北京市,东城区', '3.1', '4.1', '张主任', '13800138001', 'info@pekingunion.com', '北京市东城区帅府园1号', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '上海市第一人民医院', 'HOS-002', 21, '上海市', '中国,上海,上海市', '3.2', '4.1', '李医生', '13800138002', 'info@shfirsthospital.com', '上海市虹口区武进路85号', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '浙江大学医学院附属第一医院', 'HOS-003', 311, '上城区', '中国,浙江,杭州市,上城区', 1, 1, '王医生', '13800138003', 'info@hzdu1hospital.com', '杭州市上城区庆春路79号', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 医院组合模板表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS hospital_group_template;
CREATE TABLE hospital_group_template (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_name       VARCHAR(64)     NOT NULL COMMENT '模板名称',
    template_code       VARCHAR(32)     NOT NULL COMMENT '模板编码',
    template_desc       VARCHAR(256)    COMMENT '模板描述',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_template_status (status)
);

-- 插入模板测试数据
INSERT INTO hospital_group_template (id, template_name, template_code, template_desc, status, remark, create_time, update_time, is_deleted) VALUES
(1, '北京市医院联盟', 'TPL-HOS-001', '覆盖北京市主要三甲医院', 1, '用于北京地区业务', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '华东地区医院群', 'TPL-HOS-002', '覆盖华东地区重点医院', 1, '用于华东区域拓展', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 医院组合模板明细表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS hospital_group_template_detail;
CREATE TABLE hospital_group_template_detail (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_id         BIGINT          NOT NULL COMMENT '模板ID',
    hospital_id         BIGINT          NOT NULL COMMENT '医院ID',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_hospital (template_id, hospital_id),
    KEY idx_template_detail_template_id (template_id),
    KEY idx_template_detail_hospital_id (hospital_id)
);

-- 插入模板明细测试数据
INSERT INTO hospital_group_template_detail (id, template_id, hospital_id, create_time) VALUES
(1, 1, 1, CURRENT_TIMESTAMP),
(2, 2, 2, CURRENT_TIMESTAMP),
(3, 2, 3, CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 用户-医院关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user_hospital;
CREATE TABLE sys_user_hospital (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    hospital_id         BIGINT          NOT NULL COMMENT '医院ID',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_hospital (user_id, hospital_id),
    KEY idx_user_hospital_user_id (user_id),
    KEY idx_user_hospital_hospital_id (hospital_id)
);

-- ------------------------------------------------------------
-- 医生表（对应 DoctorEntity）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS doctor;
CREATE TABLE doctor (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    doctor_name         VARCHAR(100)    NOT NULL COMMENT '医生姓名',
    doctor_phone        VARCHAR(32)     COMMENT '医生电话',
    hospital_id         BIGINT          NOT NULL COMMENT '所属医院ID',
    hospital_dept_id    BIGINT          COMMENT '所属医院科室ID',
    creator_id          BIGINT          COMMENT '创建该医生记录的业务员ID',
    order_count         INT             DEFAULT 0 COMMENT '关联订单数量',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_doctor_hospital (hospital_id),
    KEY idx_doctor_dept (hospital_dept_id),
    KEY idx_doctor_status (status)
);

-- 插入医生测试数据
INSERT INTO doctor (id, doctor_name, doctor_phone, hospital_id, hospital_dept_id, creator_id, order_count, status, remark, create_time, update_time, is_deleted) VALUES
(1, '张主任', '13800001111', 1, NULL, NULL, 0, 1, '骨科主任医师', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '李医生', '13800001112', 1, NULL, NULL, 0, 1, '神经外科医生', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '王医生', '13800001113', 2, NULL, NULL, 0, 1, '口腔科医生', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 医院科室表（对应 HospitalDeptEntity）
-- 注意：表名保持与 Entity @TableName 一致（hospital_dept），以兼容现有代码
-- ------------------------------------------------------------
DROP TABLE IF EXISTS hospital_dept;
CREATE TABLE hospital_dept (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    hospital_dept_code  VARCHAR(32)     NOT NULL COMMENT '科室编码（如：HDEPT-0001）',
    hospital_dept_name  VARCHAR(100)    NOT NULL COMMENT '科室名称',
    sort                INT             DEFAULT 0 COMMENT '排序',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_hdept_status (status)
);

-- 插入科室测试数据
INSERT INTO hospital_dept (id, hospital_dept_code, hospital_dept_name, sort, status, remark, create_time, update_time, is_deleted) VALUES
(1, 'HDEPT-001', '骨科', 1, 1, '骨科', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'HDEPT-002', '神经外科', 2, 1, '神经外科', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'HDEPT-003', '口腔科', 3, 1, '口腔科', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 产品型号表（对应 ProductEntity）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_code        VARCHAR(64)     NOT NULL COMMENT '产品型号编码',
    product_name        VARCHAR(128)   NOT NULL COMMENT '产品名称',
    category            VARCHAR(50)     COMMENT '产品分类（如：髋关节、膝关节、脊柱）',
    spec                VARCHAR(128)    COMMENT '规格',
    cert_id             BIGINT          NOT NULL COMMENT '关联注册证ID',
    material            VARCHAR(128)    COMMENT '材质',
    color_options       VARCHAR(512)    COMMENT '可选颜色（JSON数组）',
    price               DECIMAL(12,2)   DEFAULT 0 COMMENT '标准价格',
    image_url           VARCHAR(512)    COMMENT '产品图片URL',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_product_cert (cert_id),
    KEY idx_product_category (category),
    KEY idx_product_status (status)
);

-- 插入产品测试数据
INSERT INTO product (id, product_code, product_name, category, spec, cert_id, material, color_options, price, status, remark, create_time, update_time, is_deleted) VALUES
(1, 'PROD-001', '膝关节假体', '膝关节类', '标准型', NULL, '钴铬钼合金', '["银色", "钛金色"]', 25000.00, 1, '膝关节假体标准型', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'PROD-002', '髋关节假体', '髋关节类', '标准型', NULL, '钛合金', '["银色"]', 28000.00, 1, '髋关节假体标准型', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'PROD-003', '椎间融合器', '脊柱类', 'PEEK材质', NULL, 'PEEK', '["本色"]', 15000.00, 1, '椎间融合器PEEK材质', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 重建部位表（对应 BodyPartEntity）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS rebuild_body_part;
CREATE TABLE rebuild_body_part (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name                VARCHAR(100)    NOT NULL COMMENT '部位名称',
    code                VARCHAR(50)     COMMENT '部位编码',
    sort                INT             DEFAULT 0 COMMENT '排序',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',
    scope               TEXT            COMMENT '范围值（JSON 字符串）',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_bodypart_status (status)
);

-- 插入部位测试数据（平级结构）
INSERT INTO rebuild_body_part (id, name, code, sort, status, remark, create_time, update_time, is_deleted) VALUES
(1, '颅骨', 'BP-0001', 1, 1, '颅骨修补区域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '颌面', 'BP-0002', 2, 1, '颌面重建区域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '脊柱', 'BP-0003', 3, 1, '脊柱区域', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 重建项目表（对应 RebuildProjectEntity）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS rebuild_project;
CREATE TABLE rebuild_project (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    body_part_id        BIGINT          NOT NULL COMMENT '关联部位ID',
    parent_id           BIGINT          NOT NULL DEFAULT 0 COMMENT '父项目ID（0=顶级重建项目）',
    name                VARCHAR(200)     NOT NULL COMMENT '项目名称',
    code                VARCHAR(50)      COMMENT '项目编码',
    level               INT             NOT NULL DEFAULT 1 COMMENT '层级（1=重建项目，2=子重建项目）',
    standard_price      DECIMAL(12,2)   DEFAULT 0 COMMENT '标准价格（元）',
    urgent_price        DECIMAL(12,2)   DEFAULT 0 COMMENT '加急价格（元）',
    category            VARCHAR(50)      COMMENT '项目分类（模型/导板等）',
    estimated_hours     DECIMAL(6,1)    DEFAULT 0 COMMENT '预计耗时（小时，支持小数）',
    description         TEXT COMMENT '项目说明模板',
    forming_requirements TEXT COMMENT '成形需求模板',
    sort                INT             DEFAULT 0 COMMENT '排序',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    specialty           VARCHAR(64)     DEFAULT NULL COMMENT '专业方向字典编码（单值，如 7.1）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_project_bodypart (body_part_id),
    KEY idx_project_status (status)
);

-- 插入重建项目测试数据
INSERT INTO rebuild_project (id, body_part_id, parent_id, name, code, level, standard_price, urgent_price, category, estimated_hours, specialty, status, remark, create_time, update_time, is_deleted) VALUES
(1, 1, 0, '颅骨修补术', 'HEAD-SKULL-001', 1, 8000.00, 10000.00, '修补类', 4.0, '7.1', 1, '颅骨修补术', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 2, 0, '颌面重建术', 'HEAD-MANDIBLE-001', 1, 12000.00, 15000.00, '重建类', 6.0, '7.1', 1, '颌面重建术', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 3, 0, '脊柱矫形术', 'BODY-SPINE-001', 1, 15000.00, 20000.00, '矫形类', 8.0, '7.2', 1, '脊柱矫形术', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 注册证表（对应 RegistrationCertEntity）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS registration_cert;
CREATE TABLE registration_cert (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    cert_code           VARCHAR(64)     NOT NULL COMMENT '注册证号',
    cert_name           VARCHAR(256)   NOT NULL COMMENT '注册证名称',
    valid_from          DATE            COMMENT '有效期开始',
    valid_to            DATE            COMMENT '有效期截止',
    cert_file_url       VARCHAR(512)    COMMENT '注册证扫描件URL',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=过期，1=有效）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_cert_status (status),
    KEY idx_cert_valid_to (valid_to)
);

-- 插入注册证测试数据
INSERT INTO registration_cert (id, cert_code, cert_name, valid_from, valid_to, cert_file_url, status, remark, create_time, update_time, is_deleted) VALUES
(1, '国械注进20263120001', '金属3D打印髋关节假体系统', '2026-01-01', '2031-01-01', NULL, 1, '髋关节假体注册证', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '国械注进20263120002', '椎间融合器', '2026-01-01', '2031-01-01', NULL, 1, '脊柱融合器注册证', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '国械注进20253120003', '膝关节假体系统（已过期）', '2025-01-01', '2025-12-31', NULL, 0, '膝关节假体注册证（已过期）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 编码规则表（对应 CodeRuleEntity）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_code_rule;
CREATE TABLE sys_code_rule (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    rule_code           VARCHAR(50)     NOT NULL COMMENT '规则编码（如：ORDER_NO）',
    rule_name           VARCHAR(100)   NOT NULL COMMENT '规则名称',
    prefix              VARCHAR(50)     COMMENT '前缀（如：ORD-）',
    date_format         VARCHAR(50)     COMMENT '日期格式（支持 {yyyy}{MM}{dd} 等）',
    seq_length          INT             DEFAULT 6 COMMENT '序号长度（不够补0）',
    reset_type          VARCHAR(20)     DEFAULT 'NEVER' COMMENT '重置类型（DAY/MONTH/YEAR/NEVER）',
    current_value       BIGINT          DEFAULT 0 COMMENT '当前序号值',
    step                INT             DEFAULT 1 COMMENT '递增步长',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=启用）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code),
    KEY idx_rule_status (status)
);

-- 插入编码规则测试数据
INSERT INTO sys_code_rule (id, rule_code, rule_name, prefix, date_format, seq_length, reset_type, current_value, step, status, remark, create_time, update_time, is_deleted) VALUES
(1, 'HOSPITAL_CODE', '医院编码', 'H', NULL, 4, NULL, 3, 1, 1, '医院编码规则', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'DEPT_CODE', '科室编码', 'D', NULL, 4, NULL, 3, 1, 1, '科室编码规则', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'PRODUCT_CODE', '产品编码', 'PROD', NULL, 4, NULL, 3, 1, 1, '产品编码规则', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ------------------------------------------------------------
-- 编码序号表（对应 CodeSequenceEntity）
-- 注意：此 Entity 不继承 BaseEntity（独立设计）
-- 支持按业务标识（biz_key）隔离序号
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_code_sequence;
CREATE TABLE sys_code_sequence (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    rule_code           VARCHAR(50)     NOT NULL COMMENT '规则编码',
    biz_key            VARCHAR(64)     DEFAULT NULL COMMENT '业务标识（用于按业务维度隔离序号，如订单编号，为空表示全局序号）',
    current_seq         BIGINT          DEFAULT 0 COMMENT '当前序号',
    last_date           DATE            COMMENT '上次重置日期（用于判断是否需要重置）',
    version             INT             DEFAULT 0 COMMENT '乐观锁版本号',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_seq_rule_biz_key (rule_code, biz_key),
    KEY idx_seq_rule_code (rule_code),
    KEY idx_seq_last_date (last_date)
);

-- ------------------------------------------------------------
-- 文件记录表（file_detail）
-- H2 建表语句，对应 x-file-storage 框架标准
-- ------------------------------------------------------------
DROP TABLE IF EXISTS file_detail;
CREATE TABLE file_detail (
    id                  VARCHAR(32)  NOT NULL COMMENT '文件ID（雪花算法生成）',
    url                 VARCHAR(512) NOT NULL COMMENT '文件访问地址',
    size                BIGINT(20)   DEFAULT NULL COMMENT '文件大小，单位字节',
    filename            VARCHAR(256) DEFAULT NULL COMMENT '保存的文件名',
    original_filename   VARCHAR(256) DEFAULT NULL COMMENT '原始文件名',
    base_path           VARCHAR(256) DEFAULT NULL COMMENT '基础存储路径',
    path                VARCHAR(256) DEFAULT NULL COMMENT '存储路径',
    ext                 VARCHAR(32)  DEFAULT NULL COMMENT '文件扩展名',
    content_type        VARCHAR(128) DEFAULT NULL COMMENT 'MIME类型',
    platform            VARCHAR(32)  DEFAULT NULL COMMENT '存储平台',
    th_url              VARCHAR(512) DEFAULT NULL COMMENT '缩略图访问地址',
    th_filename         VARCHAR(256) DEFAULT NULL COMMENT '缩略图文件名',
    th_size             BIGINT(20)   DEFAULT NULL COMMENT '缩略图大小',
    th_content_type     VARCHAR(128) DEFAULT NULL COMMENT '缩略图MIME类型',
    object_id           VARCHAR(32)  DEFAULT NULL COMMENT '关联业务ID',
    object_type         VARCHAR(32)  DEFAULT NULL COMMENT '关联业务类型',
    metadata            TEXT COMMENT '文件元数据',
    user_metadata       TEXT COMMENT '用户元数据',
    th_metadata         TEXT COMMENT '缩略图元数据',
    th_user_metadata    TEXT COMMENT '缩略图用户元数据',
    attr                TEXT COMMENT '附加属性',
    file_acl            VARCHAR(32)  DEFAULT NULL COMMENT '文件ACL',
    th_file_acl         VARCHAR(32)  DEFAULT NULL COMMENT '缩略图ACL',
    hash_info           TEXT COMMENT '哈希信息',
    upload_id           VARCHAR(128) DEFAULT NULL COMMENT '上传ID',
    upload_status       INT          DEFAULT NULL COMMENT '上传状态：1-初始化，2-完成',
    create_time         DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME     DEFAULT NULL COMMENT '更新时间',
    create_by           BIGINT       DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT       DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_file_detail_object (object_type, object_id),
    KEY idx_file_detail_create_time (create_time)
);

-- ------------------------------------------------------------
-- 文件分片信息表（file_part_detail）
-- 仅在手动分片上传（大文件断点续传）时使用
-- ------------------------------------------------------------
DROP TABLE IF EXISTS file_part_detail;
CREATE TABLE file_part_detail (
    id          VARCHAR(32)  NOT NULL COMMENT '分片ID',
    platform    VARCHAR(32)  DEFAULT NULL COMMENT '存储平台',
    upload_id   VARCHAR(128) DEFAULT NULL COMMENT '上传ID',
    e_tag       VARCHAR(255) DEFAULT NULL COMMENT '分片ETag',
    part_number INT          DEFAULT NULL COMMENT '分片号',
    part_size   BIGINT(20)   DEFAULT NULL COMMENT '分片大小',
    hash_info   TEXT COMMENT '哈希信息',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id)
);

-- ------------------------------------------------------------
-- 操作日志表（sys_operation_log）
-- H2 建表语句，对应 OperationLogEntity
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    module             VARCHAR(64)     COMMENT '请求模块',
    business_type      INT             COMMENT '业务类型（关联 OperationTypeEnum 枚举）',
    business_type_name VARCHAR(32)     COMMENT '业务类型名称',
    operation          VARCHAR(64)     COMMENT '操作描述',
    description        VARCHAR(256)    COMMENT '业务描述',
    request_method     VARCHAR(10)     COMMENT '请求方法（GET/POST/PUT/DELETE）',
    request_url        VARCHAR(512)   COMMENT '请求URL',
    request_params     TEXT            COMMENT '请求参数（JSON格式，已脱敏）',
    ip                 VARCHAR(64)    COMMENT '请求IP地址',
    location           VARCHAR(128)   COMMENT '操作地点',
    user_agent         VARCHAR(512)   COMMENT 'User-Agent',
    user_id            BIGINT          COMMENT '操作用户ID',
    username           VARCHAR(64)    COMMENT '操作用户名',
    real_name          VARCHAR(64)    COMMENT '操作用户真实姓名',
    status             INT             COMMENT '响应状态（0=失败，1=成功）',
    error_message      VARCHAR(512)   COMMENT '错误信息',
    duration           BIGINT          COMMENT '执行时长（毫秒）',
    operation_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    PRIMARY KEY (id),
    KEY idx_log_module (module),
    KEY idx_log_business_type (business_type),
    KEY idx_log_user_id (user_id),
    KEY idx_log_status (status),
    KEY idx_log_operation_time (operation_time)
);

-- 插入操作日志测试数据
INSERT INTO sys_operation_log (id, module, business_type, business_type_name, operation, description, request_method, request_url, ip, user_id, username, real_name, status, duration, operation_time) VALUES
(1, '医院管理', 1, '新增', '创建医院', '创建医院：测试医院', 'POST', '/api/basic/hospital', '127.0.0.1', 1, 'admin', '系统管理员', 1, 150, CURRENT_TIMESTAMP),
(2, '医院管理', 2, '修改', '更新医院', '更新医院：北京协和医院', 'PUT', '/api/basic/hospital/1', '127.0.0.1', 1, 'admin', '系统管理员', 1, 120, DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
(3, '产品管理', 1, '新增', '创建产品', '创建产品：膝关节假体', 'POST', '/api/basic/product', '127.0.0.1', 1, 'admin', '系统管理员', 1, 80, DATEADD('DAY', -2, CURRENT_TIMESTAMP));
