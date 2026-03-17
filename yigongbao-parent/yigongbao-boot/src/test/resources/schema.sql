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
    UNIQUE KEY uk_dict_code (dict_code),
    KEY idx_parent_id (parent_id),
    KEY idx_level (level),
    KEY idx_is_deleted (is_deleted)
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
    org_type            TINYINT         NOT NULL COMMENT '机构类型',
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
    hospital_level     TINYINT         COMMENT '医院等级',
    hospital_type      TINYINT         COMMENT '医院类型',
    status             TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark            VARCHAR(512)    COMMENT '备注说明',
    create_time        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by          BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted         TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    UNIQUE KEY uk_org_name (org_name)
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
