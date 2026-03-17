-- ============================================================
-- 医工宝系统 DDL
-- 用于存储所有业务表的建表语句
-- ============================================================

-- ------------------------------------------------------------
-- 字典表（单表树形结构）
-- 采用父级/子级关系实现层级结构
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
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code (dict_code),
    KEY idx_dict_parent_id (parent_id),
    KEY idx_dict_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典表';

-- ------------------------------------------------------------
-- 机构表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_org;
CREATE TABLE sys_org (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    org_name            VARCHAR(128)    NOT NULL COMMENT '机构名称',
    org_code            VARCHAR(32)     NOT NULL COMMENT '机构编码',
    org_type            TINYINT         NOT NULL COMMENT '机构类型（关联字典编码=1，子节点dict_code=1.1/1.2/1.3/1.4）',
    area_id            BIGINT          COMMENT '所属地区ID（关联sys_area表）',
    area_name          VARCHAR(64)     COMMENT '所属地区名称（冗余存储）',
    address            VARCHAR(256)    COMMENT '详细地址',
    contact            VARCHAR(32)     NOT NULL COMMENT '联系人',
    phone              VARCHAR(32)     NOT NULL COMMENT '联系电话',
    email              VARCHAR(64)     COMMENT '联系邮箱',
    credit_code        VARCHAR(32)     COMMENT '统一社会信用代码',
    business_license   VARCHAR(512)    COMMENT '营业执照（存储路径/URL）',

    -- 经销商额外字段
    agent_area         VARCHAR(64)     COMMENT '代理区域',
    agent_product_line VARCHAR(256)    COMMENT '代理产品线（多个用逗号分隔，关联字典编码=5，子节点dict_code=5.1/5.2/5.3/5.4）',

    -- 医疗机构额外字段
    hospital_level     TINYINT         COMMENT '医院等级（关联字典编码=3，子节点dict_code=3.1/3.2/3.3/3.4/3.5）',
    hospital_type      TINYINT         COMMENT '医院类型（关联字典编码=4，子节点dict_code=4.1/4.2）',

    -- 状态
    status             TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark            VARCHAR(512)    COMMENT '备注说明',

    -- 通用字段
    create_time        DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by          BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted         TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    UNIQUE KEY uk_org_name (org_name),
    KEY idx_org_type (org_type),
    KEY idx_org_area_id (area_id),
    KEY idx_org_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构表';


-- ==================== 地区表（省市区） ====================
-- 说明：与 https://github.com/kakuilan/china_area_mysql 表结构完全一致
-- 可直接导入 cnarea_2023 数据，无需额外处理
DROP TABLE IF EXISTS sys_area;
CREATE TABLE sys_area (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    level               TINYINT         NOT NULL COMMENT '层级（1=省/直辖市，2=市，3=区/县）',
    parent_code         BIGINT          NOT NULL DEFAULT 0 COMMENT '父级行政代码',
    area_code           BIGINT          NOT NULL DEFAULT 0 COMMENT '行政代码（国家标准）',
    zip_code            INT             UNSIGNED ZEROFILL NOT NULL DEFAULT 0 COMMENT '邮政编码',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表（省市区，与 cnarea_2023 结构一致）';


-- ------------------------------------------------------------
-- 部门表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dept_name           VARCHAR(128)    NOT NULL COMMENT '部门名称',
    dept_code           VARCHAR(32)     NOT NULL COMMENT '部门编码',
    org_id              BIGINT          NOT NULL COMMENT '所属机构ID（关联sys_org表）',
    leader_user_id      BIGINT          COMMENT '部门负责人用户ID（关联sys_user表）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';
