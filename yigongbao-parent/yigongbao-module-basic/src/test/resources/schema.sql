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
    hospital_level      TINYINT         COMMENT '医院等级（字典：dict_code=3）',
    hospital_type       TINYINT         COMMENT '医院类型（字典：dict_code=4）',
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
    UNIQUE KEY uk_hospital_code (hospital_code),
    UNIQUE KEY uk_hospital_name (hospital_name),
    KEY idx_hospital_area_id (area_id),
    KEY idx_hospital_level (hospital_level),
    KEY idx_hospital_type (hospital_type),
    KEY idx_hospital_status (status)
);

-- 插入医院测试数据
INSERT INTO hospital (id, hospital_name, hospital_code, area_id, area_name, full_area_name, hospital_level, hospital_type, contact, phone, email, address, status, create_time, update_time, is_deleted) VALUES
(1, '北京协和医院', 'HOS-001', 111, '东城区', '中国,北京,北京市,东城区', 1, 1, '张主任', '13800138001', 'info@pekingunion.com', '北京市东城区帅府园1号', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '上海市第一人民医院', 'HOS-002', 21, '上海市', '中国,上海,上海市', 2, 1, '李医生', '13800138002', 'info@shfirsthospital.com', '上海市虹口区武进路85号', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
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
    UNIQUE KEY uk_template_code (template_code),
    UNIQUE KEY uk_template_name (template_name),
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
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_hospital (user_id, hospital_id),
    KEY idx_user_hospital_user_id (user_id),
    KEY idx_user_hospital_hospital_id (hospital_id)
);
