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
    KEY idx_level (level),
    KEY idx_is_deleted (is_deleted)
);

-- 插入测试数据
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status, remark, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 0, '1', '机构类型', NULL, 1, 0, 1, '测试用的字典类型', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(2, 1, '1.1', '生产企业', 'production', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(3, 1, '1.2', '经销商', 'distributor', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(4, 0, '2', '性别', NULL, 1, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(5, 4, '2.1', '男', 'male', 2, 1, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0),
(6, 4, '2.2', '女', 'female', 2, 2, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 0);

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

