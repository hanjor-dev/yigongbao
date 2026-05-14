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
    KEY idx_dict_parent_id (parent_id),
    KEY idx_dict_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典表';
CREATE UNIQUE INDEX uk_dict_code ON sys_dict ((CASE WHEN is_deleted = 0 THEN dict_code ELSE NULL END));

-- ------------------------------------------------------------
-- 机构表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_org;
CREATE TABLE sys_org (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    org_name            VARCHAR(128)    NOT NULL COMMENT '机构名称',
    org_code            VARCHAR(32)     NOT NULL COMMENT '机构编码',
    org_type            VARCHAR(16)     NOT NULL COMMENT '机构类型（关联字典编码=1，子节点dict_code=1.1/1.2/1.3/1.4）',
    area_id            BIGINT          COMMENT '所属地区ID（关联sys_area表）',
    area_name          VARCHAR(64)     COMMENT '所属地区名称（冗余存储）',
    address            VARCHAR(256)    COMMENT '详细地址',
    contact            VARCHAR(32)     COMMENT '联系人',
    phone              VARCHAR(32)     COMMENT '联系电话',
    email              VARCHAR(64)     COMMENT '联系邮箱',
    credit_code        VARCHAR(32)     COMMENT '统一社会信用代码',
    qualification_file VARCHAR(512)    COMMENT '资质文件（存储压缩包路径/URL）',

    -- 经销商额外字段
    qualification_type TINYINT         COMMENT '资质类型（1=医疗器械，2=非医疗器械）',

    -- 医疗机构额外字段
    hospital_level      VARCHAR(16)      COMMENT '医院等级（关联字典编码=3，子节点dict_code=3.1/3.2/3.3/3.4/3.5）',
    hospital_type       VARCHAR(16)      COMMENT '医院类型（关联字典编码=4，子节点dict_code=4.1/4.2）',

    -- 账号前缀（生产企业/经销商专用，用于自动生成用户名）
    username_prefix     VARCHAR(16)      COMMENT '账号前缀（英文字母和数字，2-16位）',

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
    KEY idx_org_type (org_type),
    KEY idx_org_area_id (area_id),
    KEY idx_org_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构表';
CREATE UNIQUE INDEX uk_org_code ON sys_org ((CASE WHEN is_deleted = 0 THEN org_code ELSE NULL END));
CREATE UNIQUE INDEX uk_org_name ON sys_org ((CASE WHEN is_deleted = 0 THEN org_name ELSE NULL END));


-- ------------------------------------------------------------
-- 部门表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dept_name           VARCHAR(128)    NOT NULL COMMENT '部门名称',
    dept_code           VARCHAR(32)     NOT NULL COMMENT '部门编码',
    dept_type           VARCHAR(10)     NOT NULL COMMENT '部门类型（字典编码：6.1=企业部门，6.2=业务部门）',
    leader_user         VARCHAR(64)     COMMENT '部门负责人姓名（自由输入）',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    -- 通用字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_dept_type (dept_type),
    KEY idx_dept_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';
CREATE UNIQUE INDEX uk_dept_code ON sys_dept ((CASE WHEN is_deleted = 0 THEN dept_code ELSE NULL END));
CREATE UNIQUE INDEX uk_dept_name ON sys_dept ((CASE WHEN is_deleted = 0 THEN dept_name ELSE NULL END));


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
    data_scope_type     VARCHAR(16)     NOT NULL DEFAULT 'org' COMMENT '数据权限范围（self=仅自己，hospitals=医院范围，dept=本部门，org=本机构，all=全部）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
CREATE UNIQUE INDEX uk_role_code ON sys_role ((CASE WHEN is_deleted = 0 THEN role_code ELSE NULL END));

-- 生产环境迁移参考（已有数据时使用，勿直接执行 DDL DROP/CREATE）：
-- ALTER TABLE sys_role ADD COLUMN data_scope_type VARCHAR(16) NULL COMMENT '数据权限范围';
-- UPDATE sys_role SET data_scope_type = CASE WHEN hospital_scope_enabled = 1 THEN 'hospitals' WHEN account_type = '6.1' THEN 'all' ELSE 'self' END;
-- UPDATE sys_role SET data_scope_type = 'org'  WHERE role_code IN ('ROLE_DESIGNER', 'ROLE_PRODUCTION', 'ROLE_ORG_ADMIN');
-- UPDATE sys_role SET data_scope_type = 'self' WHERE role_code IN ('ROLE_DOCTOR', 'ROLE_ORG_USER');
-- ALTER TABLE sys_role MODIFY COLUMN data_scope_type VARCHAR(16) NOT NULL DEFAULT 'org';
-- ALTER TABLE sys_role DROP COLUMN hospital_scope_enabled;


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
    sex                 VARCHAR(20)     COMMENT '性别（关联字典编码）',
    avatar              VARCHAR(512)    COMMENT '头像路径',
    account_type        VARCHAR(10)     NOT NULL COMMENT '账户分类（字典编码：6.1=企业账户，6.2=业务账户）',
    org_id              BIGINT          NOT NULL COMMENT '所属机构ID（关联sys_org表）',
    org_name            VARCHAR(128)    COMMENT '所属机构名称（冗余字段）',
    dept_id             BIGINT          COMMENT '所属部门ID（关联sys_dept表）',
    dept_name           VARCHAR(128)    COMMENT '所属部门名称（冗余字段）',
    role_id             BIGINT          COMMENT '关联角色ID（关联sys_role表）',
    role_name           VARCHAR(64)     COMMENT '关联角色名称（冗余字段）',
    role_code           VARCHAR(32)     COMMENT '关联角色编码（冗余字段）',

    -- 扩展字段
    employee_no         VARCHAR(32)     COMMENT '工号',
    specialty           VARCHAR(255)    COMMENT '专业方向（多选逗号拼接，如 7.1,7.2）',
    qualification       VARCHAR(512)    COMMENT '资质证书信息',
    settlement_type     TINYINT         COMMENT '结算类型（关联字典编码=8）',

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
CREATE UNIQUE INDEX uk_username ON sys_user ((CASE WHEN is_deleted = 0 THEN username ELSE NULL END));
CREATE UNIQUE INDEX uk_phone ON sys_user ((CASE WHEN is_deleted = 0 THEN phone ELSE NULL END));

-- email 唯一（逻辑删除兼容）
CREATE UNIQUE INDEX uk_email
    ON sys_user ((CASE WHEN is_deleted = 0 THEN email ELSE NULL END));

-- employee_no 唯一（本期不作登录凭据，同步补上约束）
CREATE UNIQUE INDEX uk_employee_no
    ON sys_user ((CASE WHEN is_deleted = 0 THEN employee_no ELSE NULL END));

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
    config_group    VARCHAR(32)     DEFAULT 'system' COMMENT '配置分组（system/security/file/other）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
CREATE UNIQUE INDEX uk_config_key ON sys_config ((CASE WHEN is_deleted = 0 THEN config_key ELSE NULL END));


-- ------------------------------------------------------------
-- 资源表（整合菜单和按钮权限）
-- resource_type: 1=一级菜单, 2=二级菜单, 3=按钮
-- ------------------------------------------------------------
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
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_resource_parent_id (parent_id),
    KEY idx_resource_type (resource_type),
    KEY idx_resource_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源表';
CREATE UNIQUE INDEX uk_resource_code ON sys_resource ((CASE WHEN is_deleted = 0 THEN resource_code ELSE NULL END));


-- ------------------------------------------------------------
-- 角色资源关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role_resource;
CREATE TABLE sys_role_resource (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id          BIGINT          NOT NULL COMMENT '角色ID',
    resource_id      BIGINT          NOT NULL COMMENT '资源ID',

    PRIMARY KEY (id),
    UNIQUE KEY uk_role_resource (role_id, resource_id),
    KEY idx_role_id (role_id),
    KEY idx_resource_id (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色资源关联表';


-- ------------------------------------------------------------
-- 登录日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT          COMMENT '用户ID',
    username        VARCHAR(64)     COMMENT '用户名',
    login_type      VARCHAR(16)     COMMENT '登录方式（PASSWORD/PHONE/EMAIL）',
    ip              VARCHAR(64)     COMMENT '登录IP',
    user_agent      VARCHAR(512)    COMMENT 'User-Agent（浏览器/设备信息）',
    login_time      DATETIME        NOT NULL COMMENT '登录时间',
    login_status    TINYINT         NOT NULL COMMENT '登录结果（1=成功，0=失败）',
    fail_reason     VARCHAR(256)    COMMENT '失败原因',

    PRIMARY KEY (id),
    KEY idx_login_user_id (user_id),
    KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';


-- ============================================================
-- 地区表（sys_area）
-- 用于存储省/市/区三级行政区划数据
-- 与 https://github.com/kakuilan/china_area_mysql 的 cnarea_2023 结构兼容
-- ============================================================
DROP TABLE IF EXISTS sys_area;
CREATE TABLE sys_area (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    level           TINYINT         NOT NULL COMMENT '层级（1=省/直辖市，2=市，3=区/县）',
    parent_code     BIGINT          NOT NULL DEFAULT 0 COMMENT '父级行政代码',
    area_code       BIGINT          NOT NULL DEFAULT 0 COMMENT '行政代码',
    zip_code        INT             UNSIGNED NOT NULL DEFAULT 0 COMMENT '邮政编码',
    city_code       CHAR(6)         NOT NULL DEFAULT '' COMMENT '区号',
    name            VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '名称',
    short_name      VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '简称',
    merger_name     VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '组合名（如：中国,北京,北京市,朝阳区）',
    pinyin          VARCHAR(30)     NOT NULL DEFAULT '' COMMENT '拼音',
    lng             DECIMAL(10,6)   NOT NULL DEFAULT 0 COMMENT '经度',
    lat             DECIMAL(10,6)   NOT NULL DEFAULT 0 COMMENT '纬度',

    PRIMARY KEY (id),
    UNIQUE KEY uk_area_code (area_code),
    KEY idx_parent_code (parent_code),
    KEY idx_area_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表（省/市/区三级行政区划）';


-- ============================================================
-- 经销商-医疗机构关联表（sys_org_hospital）
-- ============================================================
DROP TABLE IF EXISTS sys_org_hospital;
CREATE TABLE sys_org_hospital (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    distributor_org_id BIGINT NOT NULL COMMENT '经销商机构ID（关联sys_org表，orgType=1.2）',
    hospital_org_id    BIGINT NOT NULL COMMENT '医疗机构org_id（关联sys_org表，orgType=1.3）',
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_distributor_hospital (distributor_org_id, hospital_org_id),
    KEY idx_hospital (hospital_org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='经销商-医疗机构关联表';


-- ============================================================
-- 部门-机构关联表（sys_dept_org）
-- ============================================================
DROP TABLE IF EXISTS sys_dept_org;
CREATE TABLE sys_dept_org (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_id     BIGINT NOT NULL COMMENT '部门ID（关联sys_dept表）',
    org_id      BIGINT NOT NULL COMMENT '机构ID（关联sys_org表）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dept_org (dept_id, org_id),
    KEY idx_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门-机构关联表';


-- ============================================================
-- 医院组合模板表（hospital_group_template）
-- 用于预设医院分组方案，方便管理员批量分配用户的数据范围权限
-- ============================================================
DROP TABLE IF EXISTS hospital_group_template;
CREATE TABLE hospital_group_template (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_name       VARCHAR(64)     NOT NULL COMMENT '模板名称',
    template_code       VARCHAR(32)     NOT NULL COMMENT '模板编码（TPL-HOS-XXX 格式）',
    template_desc       VARCHAR(256)    COMMENT '模板描述',
    status              TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark              VARCHAR(512)    COMMENT '备注说明',

    -- 通用字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院组合模板表';
CREATE UNIQUE INDEX uk_template_code ON hospital_group_template ((CASE WHEN is_deleted = 0 THEN template_code ELSE NULL END));
CREATE UNIQUE INDEX uk_template_name ON hospital_group_template ((CASE WHEN is_deleted = 0 THEN template_name ELSE NULL END));


-- ============================================================
-- 医院组合模板明细表（hospital_group_template_detail）
-- 存储医院组合模板与医院的多对多关联关系
-- ============================================================
DROP TABLE IF EXISTS hospital_group_template_detail;
CREATE TABLE hospital_group_template_detail (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_id         BIGINT          NOT NULL COMMENT '模板ID（关联hospital_group_template表）',
    hospital_id         BIGINT          NOT NULL COMMENT '医疗机构org_id（关联sys_org表，orgType=1.3）',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_template_hospital (template_id, hospital_id),
    KEY idx_template_id (template_id),
    KEY idx_hospital_id (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院组合模板明细表';


-- ============================================================
-- 用户-医院关联表（sys_user_hospital）
-- 存储用户与医院的多对多关联关系，用于数据范围权限控制
-- 当用户角色数据范围为医院范围（data_scope=4）时，关联此表限制可操作医院
-- ============================================================
DROP TABLE IF EXISTS sys_user_hospital;
CREATE TABLE sys_user_hospital (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID（关联sys_user表）',
    hospital_id         BIGINT          NOT NULL COMMENT '医疗机构org_id（关联sys_org表，orgType=1.3）',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_hospital (user_id, hospital_id),
    KEY idx_user_id (user_id),
    KEY idx_hospital_id (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-医院关联表';


-- ============================================================
-- 重建部位表（rebuild_body_part）
-- 平级结构，部位直接关联重建项目
-- ============================================================
DROP TABLE IF EXISTS rebuild_body_part;
CREATE TABLE rebuild_body_part (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(100)    NOT NULL COMMENT '部位名称',
    code            VARCHAR(50)     NOT NULL COMMENT '部位编码',
    sort            INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status          TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark          VARCHAR(512)    DEFAULT NULL COMMENT '备注说明',
    scope           TEXT            DEFAULT NULL COMMENT '范围值（JSON 字符串，存储部位的扩展范围数据）',

    -- 通用字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_body_part_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='重建部位表';
CREATE UNIQUE INDEX uk_body_part_code ON rebuild_body_part ((CASE WHEN is_deleted = 0 THEN code ELSE NULL END));


-- ============================================================
-- 重建项目表（rebuild_project）
-- 用于管理重建项目树形结构（部位 → 重建项目 → 子重建项目）
-- 支持价格、耗时、成形需求等模板信息
-- ============================================================
DROP TABLE IF EXISTS rebuild_project;
CREATE TABLE rebuild_project (
    id                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    body_part_id          BIGINT          NOT NULL COMMENT '关联部位ID（关联rebuild_body_part表）',
    parent_id             BIGINT          NOT NULL DEFAULT 0 COMMENT '父项目ID（0=顶级重建项目）',
    name                  VARCHAR(100)    NOT NULL COMMENT '项目名称',
    code                  VARCHAR(50)     NOT NULL COMMENT '项目编码',
    level                 INT             NOT NULL DEFAULT 1 COMMENT '层级（1=重建项目，2=子重建项目）',
    standard_price        DECIMAL(10,2)   DEFAULT NULL COMMENT '标准价格（元）',
    urgent_price          DECIMAL(10,2)   DEFAULT NULL COMMENT '加急价格（元）',
    category_code          VARCHAR(20)     DEFAULT NULL COMMENT '项目分类编码（字典 dict_code=13，如 13.1=模型）',
    category_name          VARCHAR(50)     DEFAULT NULL COMMENT '项目分类名称（冗余字段，与字典 dict_name 一致）',
    estimated_hours       DECIMAL(8,2)    DEFAULT NULL COMMENT '预计耗时（小时，支持小数）',
    description           TEXT            DEFAULT NULL COMMENT '项目说明模板',
    forming_requirements  TEXT            DEFAULT NULL COMMENT '成形需求模板',
    sort                  INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status                TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    specialty             VARCHAR(64)     DEFAULT NULL COMMENT '专业方向字典编码（单值，如 7.1）',
    remark                VARCHAR(500)    DEFAULT NULL COMMENT '备注说明',

    -- 通用字段
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='重建项目表';
CREATE UNIQUE INDEX uk_project_code ON rebuild_project ((CASE WHEN is_deleted = 0 THEN code ELSE NULL END));


-- ============================================================
-- 操作日志表（sys_operation_log）
-- 用于记录用户的操作行为，支持审计追溯
-- ============================================================
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    module         VARCHAR(64)     COMMENT '请求模块',
    business_type  INT             COMMENT '业务类型（关联 OperationTypeEnum 枚举）',
    business_type_name VARCHAR(32) COMMENT '业务类型名称',
    operation      VARCHAR(64)     COMMENT '操作描述（操作动词，如：新增、编辑、删除）',
    description     VARCHAR(256)   COMMENT '业务描述（操作的业务内容）',
    request_method VARCHAR(10)     COMMENT '请求方法（GET/POST/PUT/DELETE）',
    request_url    VARCHAR(512)   COMMENT '请求URL',
    request_params TEXT            COMMENT '请求参数（JSON格式，已脱敏）',
    ip             VARCHAR(64)    COMMENT '请求IP地址',
    location       VARCHAR(128)   COMMENT '操作地点（根据IP解析的地理位置）',
    user_agent     VARCHAR(512)   COMMENT 'User-Agent（浏览器/设备信息）',
    user_id        BIGINT          COMMENT '操作用户ID',
    username       VARCHAR(64)    COMMENT '操作用户名',
    real_name      VARCHAR(64)    COMMENT '操作用户真实姓名',
    status         INT             COMMENT '响应状态（0=失败，1=成功）',
    error_message  TEXT             COMMENT '错误信息',
    duration       BIGINT          COMMENT '执行时长（毫秒）',
    operation_time DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    PRIMARY KEY (id),
    KEY idx_log_module (module),
    KEY idx_log_business_type (business_type),
    KEY idx_log_user_id (user_id),
    KEY idx_log_status (status),
    KEY idx_log_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';


-- ============================================================
-- 编码规则表（sys_code_rule）
-- 用于配置和管理系统中各类编码的生成规则
-- ============================================================
DROP TABLE IF EXISTS sys_code_rule;
CREATE TABLE sys_code_rule (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    rule_code      VARCHAR(64)     NOT NULL COMMENT '规则编码（如：ORDER_NO）',
    rule_name      VARCHAR(128)   NOT NULL COMMENT '规则名称',
    prefix         VARCHAR(32)    COMMENT '前缀（如：ORD-）',
    date_format    VARCHAR(64)    COMMENT '日期格式（支持 {yyyy}{MM}{dd} 等）',
    seq_length     INT             DEFAULT 6 COMMENT '序号长度（不够补0）',
    reset_type     VARCHAR(32)    DEFAULT 'NEVER' COMMENT '重置类型（DAY/MONTH/YEAR/NEVER）',
    current_value  BIGINT          DEFAULT 0 COMMENT '当前序号值',
    step           INT             DEFAULT 1 COMMENT '递增步长',
    status         TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=启用）',
    remark         VARCHAR(512)   COMMENT '备注',

    -- 通用字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_code_rule_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编码规则表';
CREATE UNIQUE INDEX uk_code_rule_code ON sys_code_rule ((CASE WHEN is_deleted = 0 THEN rule_code ELSE NULL END));

-- ============================================================
-- 编码序号表（sys_code_sequence）
-- 用于记录各编码规则的当前序号，支持乐观锁更新
-- 支持按业务标识（biz_key）隔离序号，用于同一规则下的不同业务分组
-- ============================================================
DROP TABLE IF EXISTS sys_code_sequence;
CREATE TABLE sys_code_sequence (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    rule_code      VARCHAR(64)     NOT NULL COMMENT '规则编码',
    biz_key        VARCHAR(64)     DEFAULT NULL COMMENT '业务标识（用于按业务维度隔离序号，如订单编号，为空表示全局序号）',
    current_seq    BIGINT          DEFAULT 0 COMMENT '当前序号',
    last_date      DATE            COMMENT '上次重置日期（用于判断是否需要重置）',
    version        INT             DEFAULT 0 COMMENT '乐观锁版本号',

    -- 通用字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_seq_rule_biz_key (rule_code, biz_key),
    KEY idx_seq_rule_code (rule_code),
    KEY idx_seq_last_date (last_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编码序号表';


-- ============================================================
-- 文件记录表（file_detail）
-- 基于 x-file-storage 框架 FileRecorder 接口标准实现
-- 支持多存储平台、业务关联、缩略图、元数据、分片上传
-- 注意：禁止在此表上定义 is_deleted 字段的索引，MyBatis-Plus @TableLogic 会自动创建
-- ============================================================
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
    th_size             BIGINT(20)   DEFAULT NULL COMMENT '缩略图大小，单位字节',
    th_content_type     VARCHAR(128) DEFAULT NULL COMMENT '缩略图MIME类型',
    object_id           VARCHAR(32)  DEFAULT NULL COMMENT '关联业务ID',
    object_type         VARCHAR(32)  DEFAULT NULL COMMENT '关联业务类型（如 registration_cert、doctor_cert）',
    metadata            TEXT COMMENT '文件元数据',
    user_metadata       TEXT COMMENT '用户元数据',
    th_metadata         TEXT COMMENT '缩略图元数据',
    th_user_metadata    TEXT COMMENT '缩略图用户元数据',
    attr                TEXT COMMENT '附加属性',
    file_acl            VARCHAR(32)  DEFAULT NULL COMMENT '文件ACL',
    th_file_acl         VARCHAR(32)  DEFAULT NULL COMMENT '缩略图ACL',
    hash_info           TEXT COMMENT '哈希信息（MD5/SHA256）',
    upload_id           VARCHAR(128) DEFAULT NULL COMMENT '上传ID（手动分片上传时使用）',
    upload_status       INT          DEFAULT NULL COMMENT '上传状态：1-初始化完成，2-上传完成',
    create_time         DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME     DEFAULT NULL COMMENT '更新时间',
    create_by           BIGINT       DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT       DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_file_detail_object (object_type, object_id),
    KEY idx_file_detail_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件记录表（x-file-storage FileRecorder）';


-- ============================================================
-- 文件分片信息表（file_part_detail）
-- 仅在手动分片上传（大文件断点续传）时使用
-- ============================================================
DROP TABLE IF EXISTS file_part_detail;
CREATE TABLE file_part_detail (
    id          VARCHAR(32)  NOT NULL COMMENT '分片ID',
    platform    VARCHAR(32)  DEFAULT NULL COMMENT '存储平台',
    upload_id   VARCHAR(128) DEFAULT NULL COMMENT '上传ID',
    e_tag       VARCHAR(255) DEFAULT NULL COMMENT '分片ETag',
    part_number INT          DEFAULT NULL COMMENT '分片号',
    part_size   BIGINT(20)  DEFAULT NULL COMMENT '分片大小',
    hash_info   TEXT COMMENT '哈希信息',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分片信息表';


-- ============================================================
-- 医院科室表（hospital_dept）
-- 全系统通用科室字典，如内科/外科/骨科等，与 sys_dept（机构组织部门）是不同概念
-- ============================================================
DROP TABLE IF EXISTS hospital_dept;
CREATE TABLE hospital_dept (
    id                   BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    hospital_dept_code   VARCHAR(32)     NOT NULL COMMENT '科室编码（系统唯一，如 HDEPT-0001）',
    hospital_dept_name   VARCHAR(100)    NOT NULL COMMENT '科室名称',
    sort                 INT             DEFAULT 0 COMMENT '排序',
    status               TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark               VARCHAR(512)    COMMENT '备注',
    create_time          DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by            BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by            BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted           TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    KEY idx_hospital_dept_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院科室表（全系统通用科室字典）';
CREATE UNIQUE INDEX uk_hospital_dept_code ON hospital_dept ((CASE WHEN is_deleted = 0 THEN hospital_dept_code ELSE NULL END));


-- ============================================================
-- 医生表（doctor）
-- 用于管理医生基础信息，关联业务员（创建人）和医院
-- ============================================================
DROP TABLE IF EXISTS doctor;
CREATE TABLE doctor (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    doctor_name       VARCHAR(64)     NOT NULL COMMENT '医生姓名',
    doctor_phone      VARCHAR(32)    COMMENT '医生电话',
    hospital_id       BIGINT          COMMENT '所属医疗机构ID（关联sys_org表，orgType=1.3）',
    creator_id        BIGINT          COMMENT '创建该医生记录的业务员ID',
    order_count       INT             DEFAULT 0 COMMENT '关联订单数量',
    status            TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark            VARCHAR(512)   COMMENT '备注',

    -- 通用字段
    create_time       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by         BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted        TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_doctor_hospital (hospital_id),
    KEY idx_doctor_creator (creator_id),
    KEY idx_doctor_name (doctor_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生表';
-- 唯一索引改为（医院+姓名）组合索引，仅对未删除记录生效（is_deleted=0）
-- 使用函数索引：is_deleted=0 时表达式非 NULL 参与唯一约束，is_deleted=1 时返回 NULL 跳过唯一约束
-- 效果：同一医院内未删除的医生姓名唯一，不同医院可重名，已删除医生不受此约束
CREATE UNIQUE INDEX uk_doctor_hospital_name ON doctor ((CASE WHEN is_deleted = 0 THEN CONCAT_WS('|', hospital_id, doctor_name) ELSE NULL END));


-- ============================================================
-- 产品表（product）
-- 产品按大类管理，规格单独维护于 product_spec 表
-- ============================================================
DROP TABLE IF EXISTS product_spec;
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_name    VARCHAR(128)    NOT NULL COMMENT '产品名称',
    category        VARCHAR(20)     COMMENT '产品大类 dict_code（如 17.1）',
    category_name   VARCHAR(64)     COMMENT '大类名称（冗余）',
    status          TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark          VARCHAR(512)    COMMENT '备注',

    -- 通用字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_product_category (category),
    KEY idx_product_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品表';
CREATE UNIQUE INDEX uk_product_name ON product ((CASE WHEN is_deleted = 0 THEN product_name ELSE NULL END));


-- ============================================================
-- 产品规格表（product_spec）
-- 一个产品大类下可关联多条规格，每条规格独立关联注册证
-- ============================================================
CREATE TABLE product_spec (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id      BIGINT          NOT NULL COMMENT '关联产品ID',
    spec_name       VARCHAR(128)    NOT NULL COMMENT '规格名称',
    cert_id         BIGINT          DEFAULT NULL COMMENT '关联注册证ID（可空）',
    cert_no         VARCHAR(64)     DEFAULT NULL COMMENT '注册证号（冗余）',
    sort            INT             DEFAULT 0 COMMENT '排序',
    status          TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark          VARCHAR(512)    COMMENT '备注',

    -- 通用字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_product_spec_product_id (product_id),
    KEY idx_product_spec_cert_id (cert_id),
    KEY idx_product_spec_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品规格表';
CREATE UNIQUE INDEX uk_product_spec_name ON product_spec ((CASE WHEN is_deleted = 0 THEN CONCAT_WS('|', product_id, spec_name) ELSE NULL END));


-- ============================================================
-- 注册证表（registration_cert）
-- 用于管理医疗器械注册证号，支持有效期管理
-- ============================================================
DROP TABLE IF EXISTS registration_cert;
CREATE TABLE registration_cert (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    cert_code       VARCHAR(64)     NOT NULL COMMENT '注册证号',
    cert_name       VARCHAR(256)  NOT NULL COMMENT '注册证名称',
    valid_from      DATE            COMMENT '有效期开始',
    valid_to         DATE            COMMENT '有效期截止',
    cert_file_url   VARCHAR(512)   COMMENT '注册证扫描件URL',
    status          TINYINT        DEFAULT 1 COMMENT '状态（0=过期，1=有效）',
    remark          VARCHAR(512)  COMMENT '备注',

    -- 通用字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_cert_valid_to (valid_to),
    KEY idx_cert_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='注册证表';
CREATE UNIQUE INDEX uk_cert_code ON registration_cert ((CASE WHEN is_deleted = 0 THEN cert_code ELSE NULL END));


-- ============================================================
-- 订单模块 DDL（第一期核心表）
-- 说明：
-- - order_draft/order_item_draft：草稿阶段独立存储，提交后转入正式订单
-- - order_main/order_item/order_file：正式订单存储
-- - order_status_history：状态变更历史，用于追溯
-- 索引规范：所有 order 相关表索引统一使用 idx_order_ 前缀
-- ============================================================


-- ============================================================
-- 订单草稿表（order_draft）
-- 设计说明：
-- 1. 无订单编号字段：提交时由 CodeGeneratorService 生成
-- 2. 无 phase/status 字段：草稿阶段不需要状态机，提交后转入正式订单
-- 3. expires_at 字段：用于30天过期管理
-- 4. operator_id 字段：索引字段，用于查询"我的草稿"
-- 5. status 字段：区分有效/已提交/已过期，便于清理
-- ============================================================
DROP TABLE IF EXISTS order_draft;
CREATE TABLE order_draft (
    -- ==================== 主键与冗余字段 ====================
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    operator_id     BIGINT          NOT NULL COMMENT '操作员ID（创建人）',

    -- ==================== 订单类型（固定不变） ====================
    order_type      TINYINT         NOT NULL COMMENT '订单类型：1-医疗器械，2-非医疗器械',

    -- ==================== 是否需要实体交付 ====================
    -- needsPhysicalDelivery = 1：走完整生产流程（打印→后处理→质检→仓储）
    -- needsPhysicalDelivery = 0：跳过生产阶段，直接到确认阶段
    needs_physical_delivery TINYINT DEFAULT 1 COMMENT '是否需要实体交付：0-不需要，1-需要',

    business_type   VARCHAR(20)     NOT NULL COMMENT '业务类型（字典 dict_code：11.1-业务，11.2-测试，11.3-试用，11.4-代理）',

    -- ==================== 机构信息 ====================
    org_id          BIGINT          NOT NULL COMMENT '提单机构ID',
    org_name        VARCHAR(200)     COMMENT '提单机构名称',
    operator_name   VARCHAR(100)    COMMENT '操作员姓名',
    operator_phone  VARCHAR(20)     COMMENT '操作员电话',

    -- ==================== 医院与科室 ====================
    hospital_id     BIGINT          COMMENT '医院ID',
    hospital_name   VARCHAR(200)     COMMENT '医院名称',
    area_id         BIGINT          COMMENT '地区ID（冗余自医院）',
    area_name       VARCHAR(64)     COMMENT '地区名称（冗余自医院）',
    full_area_name  VARCHAR(256)    COMMENT '完整地区路径名称（冗余自医院）',
    hospital_dept_id   BIGINT          COMMENT '医院科室ID',
    hospital_dept_name VARCHAR(100)    COMMENT '医院科室名称（冗余）',

    -- ==================== 医生/患者信息 ====================
    doctor_id       BIGINT          COMMENT '医生ID',
    doctor_name     VARCHAR(100)    COMMENT '医生姓名',
    doctor_phone    VARCHAR(20)     COMMENT '医生电话',
    patient_name    VARCHAR(100)    COMMENT '患者姓名',
    patient_age     INT             COMMENT '患者年龄',
    patient_gender  VARCHAR(20)     COMMENT '患者性别（字典 dict_code：12.1-男，12.2-女）',

    -- ==================== 业务信息 ====================
    is_urgent       TINYINT         DEFAULT 0 COMMENT '是否加急：0-否，1-是',
    is_postal       TINYINT         DEFAULT 0 COMMENT '是否邮寄：0-否，1-是',
    postal_address  TEXT            COMMENT '邮寄地址',

    -- ==================== 时效信息 ====================
    expected_delivery_date DATETIME    COMMENT '期望交付时间',

    -- ==================== 有效期管理 ====================
    expires_at      DATETIME        NOT NULL COMMENT '过期时间（创建时间+30天）',
    status          TINYINT         DEFAULT 1 COMMENT '状态：1-有效，2-已提交，3-已过期',

    -- ==================== 公共字段（BaseEntity） ====================
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          COMMENT '创建人ID',
    update_by       BIGINT          COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_order_draft_operator_id (operator_id),
    KEY idx_order_draft_status (status),
    KEY idx_order_draft_expires_at (expires_at),
    KEY idx_order_draft_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单草稿表';


-- ============================================================
-- 订单草稿明细表（order_item_draft）
-- 设计说明：存储草稿中的重建项目明细，提交时复制到 order_item 表
-- ============================================================
DROP TABLE IF EXISTS order_item_draft;
CREATE TABLE order_item_draft (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    draft_id        BIGINT          NOT NULL COMMENT '草稿ID',

    -- ==================== 重建项目信息 ====================
    body_part_id    BIGINT          COMMENT '部位ID',
    body_part_name  VARCHAR(100)    COMMENT '部位名称',
    project_id      BIGINT          COMMENT '重建项目ID',
    project_name    VARCHAR(200)    COMMENT '重建项目名称',
    category_code   VARCHAR(20)     COMMENT '项目分类编码（字典 dict_code=13）',
    category_name   VARCHAR(50)     COMMENT '项目分类名称',
    project_estimated_hours DECIMAL(8,2) COMMENT '预计耗时（小时）',

    -- ==================== 用户填写内容 ====================
    project_desc    TEXT            COMMENT '项目说明',
    forming_requirement TEXT        COMMENT '成形需求',
    other_requirement TEXT          COMMENT '其他要求',

    -- ==================== 序号 ====================
    sort_order      INT             DEFAULT 1 COMMENT '排序序号',

    -- ==================== 公共字段 ====================
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          COMMENT '创建人ID',
    update_by       BIGINT          COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_order_item_draft_draft_id (draft_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单草稿明细表';


-- ============================================================
-- 订单主表（order_main）
-- 设计说明：
-- 1. 表名改为 order_main：与 order_draft 对应，更清晰
-- 2. business_type 使用字典值：business/test/trial/agent
-- 3. patient_gender 使用字典值：male/female（替代 TINYINT 0/1）
-- 4. version 字段：乐观锁，处理并发更新
-- ============================================================
DROP TABLE IF EXISTS order_main;
CREATE TABLE order_main (
    -- ==================== 主键与编码 ====================
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_code     VARCHAR(50)     NOT NULL COMMENT '订单编号',

    -- ==================== 订单类型 ====================
    order_type      TINYINT         NOT NULL COMMENT '订单类型：1-医疗器械，2-非医疗器械',

    -- ==================== 是否需要实体交付 ====================
    -- needsPhysicalDelivery = 1：走完整生产流程（打印→后处理→质检→仓储）
    -- needsPhysicalDelivery = 0：跳过生产阶段，直接到确认阶段
    needs_physical_delivery TINYINT DEFAULT 1 COMMENT '是否需要实体交付：0-不需要，1-需要',

    business_type   VARCHAR(20)     NOT NULL COMMENT '业务类型（字典 dict_code：11.1-业务，11.2-测试，11.3-试用，11.4-代理）',

    -- ==================== 机构信息 ====================
    org_id          BIGINT          NOT NULL COMMENT '提单机构ID',
    org_name        VARCHAR(200)    COMMENT '提单机构名称（冗余）',
    operator_id     BIGINT          COMMENT '操作员ID（创建人）',
    operator_name   VARCHAR(100)    COMMENT '操作员姓名（冗余）',
    operator_phone  VARCHAR(20)     COMMENT '操作员电话',

    -- ==================== 医院与科室 ====================
    hospital_id     BIGINT          COMMENT '医院ID',
    hospital_name   VARCHAR(200)    COMMENT '医院名称（冗余）',
    area_id         BIGINT          COMMENT '地区ID（冗余自医院）',
    area_name       VARCHAR(64)     COMMENT '地区名称（冗余自医院）',
    full_area_name  VARCHAR(256)    COMMENT '完整地区路径名称（冗余自医院，如"广东省/广州市/天河区"）',
    hospital_dept_id    BIGINT          COMMENT '医院科室ID',
    hospital_dept_name  VARCHAR(100)    COMMENT '医院科室名称（冗余）',
    operator_dept_id    BIGINT          COMMENT '提单人所属部门ID（冗余自 sys_user.dept_id）',
    operator_dept_name  VARCHAR(128)    COMMENT '提单人所属部门名称（冗余自 sys_user.dept_name）',

    -- ==================== 医生/患者信息 ====================
    doctor_id       BIGINT          COMMENT '医生ID',
    doctor_name     VARCHAR(100)    COMMENT '医生姓名',
    doctor_phone    VARCHAR(20)     COMMENT '医生电话',
    patient_name    VARCHAR(100)    COMMENT '患者姓名',
    patient_age     INT             COMMENT '患者年龄',
    patient_gender  VARCHAR(20)     COMMENT '患者性别（字典 dict_code：12.1-男，12.2-女）',

    -- ==================== 业务信息 ====================
    is_urgent       TINYINT         DEFAULT 0 COMMENT '是否加急：0-否，1-是',
    is_postal       TINYINT         DEFAULT 0 COMMENT '是否邮寄：0-否，1-是',
    postal_address  TEXT            COMMENT '邮寄地址',

    -- ==================== 时效信息 ====================
    expected_delivery_date DATETIME   COMMENT '期望交付时间',
    design_start_time     DATETIME    COMMENT '设计开始时间',
    design_submit_time     DATETIME    COMMENT '设计提交时间',
    design_mode           TINYINT     DEFAULT 1 COMMENT '设计模式：1=线下修改，2=在线编辑',
    user_confirm_time     DATETIME    COMMENT '用户确认时间（服务订单）',
    actual_complete_time  DATETIME    COMMENT '实际完成时间',

    -- ==================== 【核心】阶段 + 状态 ====================
    phase           INT             NOT NULL DEFAULT 10 COMMENT '当前阶段：10-订单，20-设计，30-打印，40-后处理，50-质检，60-仓储，70-确认，80-完成（间隔10，支持扩展插入）',
    status          INT             NOT NULL DEFAULT 1010 COMMENT '当前状态（格式：phase×100+序号×10，如1010=订单草稿，2010=待设计）',

    -- ==================== 当前处理人 ====================
    current_handler_id BIGINT        COMMENT '当前处理人ID',
    current_handler_name VARCHAR(100) COMMENT '当前处理人姓名',
    designer_id     BIGINT          COMMENT '设计师ID',
    designer_name   VARCHAR(100)    COMMENT '设计师姓名（冗余）',
    producer_id     BIGINT          COMMENT '生产员ID',

    -- ==================== 审核信息 ====================
    audit_remark    TEXT            COMMENT '审核备注（驳回原因等）',
    design_review_remark TEXT        COMMENT '设计审核备注',
    estimated_cost  DECIMAL(10,2)   COMMENT '预估费用',
    data_evaluation_opinion TEXT     COMMENT '影像数据评估意见',

    -- ==================== 乐观锁 ====================
    version         INT             DEFAULT 0 COMMENT '版本号（乐观锁）',

    -- ==================== 公共字段（BaseEntity） ====================
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          COMMENT '创建人ID',
    update_by       BIGINT          COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_order_main_org_id (org_id),
    KEY idx_order_main_hospital_id (hospital_id),
    KEY idx_order_main_operator_id (operator_id),
    KEY idx_order_main_phase_status (phase, status),
    KEY idx_order_main_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';
CREATE UNIQUE INDEX uk_order_main_code ON order_main ((CASE WHEN is_deleted = 0 THEN order_code ELSE NULL END));


-- ============================================================
-- 订单明细表（order_item）
-- 设计说明：存储订单中的重建项目明细，与 order_main 一对多关系
-- ============================================================
DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_code     VARCHAR(50)     NOT NULL COMMENT '订单编号',

    -- ==================== 重建项目信息 ====================
    body_part_id    BIGINT          COMMENT '部位ID',
    body_part_name  VARCHAR(100)    COMMENT '部位名称',
    project_id      BIGINT          COMMENT '重建项目ID',
    project_name    VARCHAR(200)    COMMENT '重建项目名称',
    category_code   VARCHAR(20)     COMMENT '项目分类编码（字典 dict_code=13）',
    category_name   VARCHAR(50)     COMMENT '项目分类名称',
    project_estimated_hours DECIMAL(8,2) COMMENT '预计耗时（小时）',

    -- ==================== 用户填写内容 ====================
    project_desc    TEXT            COMMENT '项目说明',
    forming_requirement TEXT         COMMENT '成形需求',
    other_requirement TEXT         COMMENT '其他要求',

    -- ==================== 序号 ====================
    sort_order      INT             DEFAULT 1 COMMENT '排序序号',

    -- ==================== 公共字段 ====================
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          COMMENT '创建人ID',
    update_by       BIGINT          COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_order_item_order_id (order_id),
    KEY idx_order_item_order_code (order_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';


-- ============================================================
-- 订单文件关联表（order_file）
-- 设计说明：
-- 1. file_detail：存储文件元数据（x-file-storage 框架标准），object_type 存储 dict_code
-- 2. order_file：作为业务层索引表，关联 file_detail.id + 订单ID
-- 3. 提供文件类别细分、数据包编号、订单明细关联能力
-- ============================================================
DROP TABLE IF EXISTS order_file;
CREATE TABLE order_file (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID（order_main.id）',
    order_code     VARCHAR(50)     NOT NULL COMMENT '订单编号',

    -- ==================== 文件关联 ====================
    file_id         VARCHAR(32)     NOT NULL COMMENT '文件ID（file_detail.id）',
    file_category   VARCHAR(20)     NOT NULL COMMENT '文件类别（字典 dict_code：10.1-影像数据，10.2-影像报告，10.3-订单其他附件，10.20-免费业务审批文件...）',
    package_no      VARCHAR(50)     COMMENT '数据包编号（用于关联同一订单下的多个数据包）',

    -- ==================== 关联明细 ====================
    order_item_id   BIGINT          COMMENT '关联的订单明细ID',

    -- ==================== 公共字段 ====================
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          COMMENT '创建人ID',
    update_by       BIGINT          COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_order_file_order_id (order_id),
    KEY idx_order_file_file_id (file_id),
    KEY idx_order_file_category (file_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单文件关联表';


-- ============================================================
-- 订单流程状态历史表（order_flow_status_history）
-- 设计说明：记录订单在各阶段的状态变更历史，用于追溯和审计
-- ============================================================
DROP TABLE IF EXISTS order_flow_status_history;
CREATE TABLE order_flow_status_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_code     VARCHAR(50)     NOT NULL COMMENT '订单编号',

    -- ==================== 状态变更信息 ====================
    phase           INT             COMMENT '变更时阶段（FlowPhaseEnum.value）',
    phase_name      VARCHAR(50)     COMMENT '变更时阶段名称（快照，防止枚举改名后历史展示错误）',
    from_status     INT             COMMENT '变更前状态（FlowStatusEnum.value）',
    from_status_name VARCHAR(50)    COMMENT '变更前状态名称（快照）',
    to_status       INT             COMMENT '变更后状态（FlowStatusEnum.value）',
    to_status_name  VARCHAR(50)     COMMENT '变更后状态名称（快照）',
    action          VARCHAR(50)     COMMENT '触发动作（如 SUBMIT_ORDER、DATA_AUDIT_PASS）',
    action_name     VARCHAR(100)    COMMENT '动作名称',

    -- ==================== 操作人信息 ====================
    operator_id     BIGINT          COMMENT '操作人ID',
    operator_name   VARCHAR(100)    COMMENT '操作人姓名',
    remark          TEXT            COMMENT '备注（如驳回原因）',

    -- ==================== 公共字段 ====================
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          COMMENT '创建人ID',
    update_by       BIGINT          COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_order_flow_order_id (order_id),
    KEY idx_order_flow_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单流程状态历史表';


-- ============================================================
-- 订单信息修改申请表（order_modify_apply）
-- 设计说明：业务员发起、管理员审核的订单字段修改申请
-- ============================================================
DROP TABLE IF EXISTS order_modify_apply;
CREATE TABLE order_modify_apply (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id            BIGINT          NOT NULL COMMENT '订单ID',
    order_code          VARCHAR(50)     NOT NULL COMMENT '订单编号（冗余）',
    hospital_name       VARCHAR(100)    COMMENT '医院名称（冗余，用于列表展示）',
    patient_name        VARCHAR(100)    COMMENT '患者姓名（冗余，用于列表展示）',

    -- ==================== 申请信息 ====================
    -- 多选时逗号分隔字典编码，如 "14.1,14.3"，最长 "14.1,14.2,14.3" = 11字符
    apply_type_codes    VARCHAR(50)     NOT NULL COMMENT '申请类型字典编码（逗号分隔，如 14.1,14.3）',
    apply_type_names    VARCHAR(200)    COMMENT '申请类型中文名冗余（如 基础信息、重建项目）',
    apply_reason        TEXT            COMMENT '申请原因',

    -- ==================== 审核信息 ====================
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待审核，APPROVED-已同意，REJECTED-已拒绝，COMPLETED-已执行',
    reject_reason       TEXT            COMMENT '驳回原因（审核不通过时必填）',
    auditor_id          BIGINT          COMMENT '审核人ID',
    auditor_name        VARCHAR(100)    COMMENT '审核人姓名',
    audit_time          DATETIME        COMMENT '审核时间',

    -- ==================== 操作人 ====================
    applicant_id        BIGINT          NOT NULL COMMENT '申请人ID',
    applicant_name      VARCHAR(100)    COMMENT '申请人姓名',

    -- ==================== 公共字段 ====================
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          COMMENT '创建人ID',
    update_by           BIGINT          COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_order_modify_apply_order_id (order_id),
    KEY idx_order_modify_apply_applicant_id (applicant_id),
    KEY idx_order_modify_apply_status (status),
    KEY idx_order_modify_apply_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单信息修改申请表';

-- 并发控制：同一订单只能有1个 PENDING 申请（is_deleted=1 时返回 NULL，不触发唯一约束）
CREATE UNIQUE INDEX uk_order_modify_apply_pending
    ON order_modify_apply ((CASE WHEN is_deleted = 0 AND status = 'PENDING' THEN order_id ELSE NULL END));


-- ============================================================
-- 订单信息修改留痕表（order_modification_log）
-- 设计说明：每次字段修改的不可变审计记录，永久留存
-- ============================================================
DROP TABLE IF EXISTS order_modification_log;
CREATE TABLE order_modification_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_code      VARCHAR(50)     NOT NULL COMMENT '订单编号（冗余）',
    apply_id        BIGINT          COMMENT '关联的修改申请ID',

    -- ==================== 修改字段信息 ====================
    field_name      VARCHAR(50)     NOT NULL COMMENT '修改字段名称（如 patient_name）',
    field_label     VARCHAR(100)    COMMENT '修改字段中文名（如 患者姓名）',
    old_value       TEXT            COMMENT '修改前的值',
    new_value       TEXT            COMMENT '修改后的值',

    -- ==================== 操作人 ====================
    modifier_id     BIGINT          NOT NULL COMMENT '修改人ID',
    modifier_name   VARCHAR(100)    COMMENT '修改人姓名',

    -- ==================== 公共字段（仅 create_time，不继承 BaseEntity）====================
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_order_modification_log_order_id (order_id),
    KEY idx_order_modification_log_apply_id (apply_id),
    KEY idx_order_modification_log_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单信息修改留痕表';


-- ============================================================
-- 打印文件数据包表（design_package）
-- 存储设计师上传的打印文件压缩包
-- ============================================================
DROP TABLE IF EXISTS design_package;
CREATE TABLE design_package (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_code      VARCHAR(50)     NOT NULL COMMENT '订单编号（冗余）',
    package_code    VARCHAR(50)     NOT NULL COMMENT '数据包编号（规则：订单编号-序号）',
    package_seq     INT             NOT NULL COMMENT '序号（订单内递增）',
    file_id         VARCHAR(32)     NOT NULL COMMENT '文件ID（关联 file_detail）',
    file_name       VARCHAR(256)    DEFAULT NULL COMMENT '原始文件名',
    file_url        VARCHAR(512)    DEFAULT NULL COMMENT '文件访问地址',
    file_size       BIGINT          DEFAULT NULL COMMENT '文件大小（字节）',
    file_count      INT             DEFAULT 0 COMMENT '包内文件数量',
    upload_time     DATETIME        DEFAULT NULL COMMENT '上传时间',
    product_mark    VARCHAR(100)    DEFAULT NULL COMMENT '产品标识（必填，数据包级别）',
    pack_quantity   INT             DEFAULT NULL COMMENT '包装数量（数据包级别统计值）',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',

    -- 公共字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_package_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打印文件数据包表';
CREATE UNIQUE INDEX uk_design_package_code ON design_package ((CASE WHEN is_deleted = 0 THEN package_code ELSE NULL END));
-- 防止并发上传时 package_seq 重复（order_id + package_seq 在未删除记录中唯一）
CREATE UNIQUE INDEX uk_design_package_order_seq ON design_package ((CASE WHEN is_deleted = 0 THEN order_id ELSE NULL END), (CASE WHEN is_deleted = 0 THEN package_seq ELSE NULL END));


-- ============================================================
-- 数据包内文件表（design_package_file）
-- 存储从压缩包解析出的文件列表
-- ============================================================
DROP TABLE IF EXISTS design_package_file;
CREATE TABLE design_package_file (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    package_id      BIGINT          NOT NULL COMMENT '数据包ID',
    file_name       VARCHAR(256)    NOT NULL COMMENT '文件名（如 左髋骨.stl）',
    file_ext        VARCHAR(32)     DEFAULT NULL COMMENT '文件扩展名（stl/3mf/obj）',
    file_path       VARCHAR(512)    DEFAULT NULL COMMENT '包内相对路径',
    file_size       BIGINT          DEFAULT NULL COMMENT '文件大小（字节）',
    sort_order      INT             DEFAULT 0 COMMENT '排序序号',
    file_id         VARCHAR(64)     DEFAULT NULL COMMENT '包内文件在 OSS 的文件ID（file_detail.id）',
    file_url        VARCHAR(1024)   DEFAULT NULL COMMENT '包内文件独立 OSS 访问地址',

    -- 公共字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_package_file_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据包内文件表';


-- ============================================================
-- 数据包文件截图关联表（design_package_file_screenshot）
-- 每个 design_package_file 最多一条有效截图，upsert 语义
-- ============================================================
DROP TABLE IF EXISTS design_package_file_screenshot;
CREATE TABLE design_package_file_screenshot (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    package_file_id     BIGINT          NOT NULL COMMENT '关联 design_package_file.id',
    file_id             VARCHAR(64)     NOT NULL COMMENT '截图文件ID（关联 file_detail.id）',
    create_time         DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_time         DATETIME        DEFAULT NULL COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_pkg_file_screenshot_file_id (package_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据包文件截图关联表';
-- 每个 package_file 只允许一条有效截图（逻辑删除兼容函数索引）
CREATE UNIQUE INDEX uk_design_pkg_file_screenshot_pkg_file_id
    ON design_package_file_screenshot ((CASE WHEN is_deleted = 0 THEN package_file_id ELSE NULL END));


-- ============================================================
-- 打印产品信息表（design_product）
-- 存储打印产品信息，一行对应一个打印文件（指令单中的一行）
-- ============================================================
DROP TABLE IF EXISTS design_product;
CREATE TABLE design_product (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    package_id      BIGINT          NOT NULL COMMENT '数据包ID',
    product_id      BIGINT          NOT NULL COMMENT '产品ID',
    product_name    VARCHAR(128)    DEFAULT NULL COMMENT '产品名称（冗余）',
    spec_id         BIGINT          NOT NULL COMMENT '型号规格ID',
    spec_name       VARCHAR(128)    DEFAULT NULL COMMENT '型号规格名称（冗余）',
    cert_no         VARCHAR(64)     DEFAULT NULL COMMENT '注册证号（冗余）',
    material_id     VARCHAR(20)     DEFAULT NULL COMMENT '材质 dict_code（如 15.1）',
    material_name   VARCHAR(64)     DEFAULT NULL COMMENT '材质名称（冗余）',
    color_id        VARCHAR(20)     DEFAULT NULL COMMENT '颜色 dict_code（如 16.1.1）',
    color_name      VARCHAR(64)     DEFAULT NULL COMMENT '颜色名称（冗余）',
    quantity        INT             NOT NULL DEFAULT 1 COMMENT '数量',
    is_urgent       TINYINT         DEFAULT 0 COMMENT '是否加急（0=普通，1=加急），默认从订单带出，可修改',
    sort_order      INT             DEFAULT 0 COMMENT '排序序号',

    -- 公共字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_product_order_id (order_id),
    KEY idx_design_product_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打印产品信息表';


-- ============================================================
-- 打印产品关联文件表（design_product_file）
-- 存储产品行与数据包内文件的一对多关联
-- ============================================================
DROP TABLE IF EXISTS design_product_file;
CREATE TABLE design_product_file (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    design_product_id   BIGINT          NOT NULL COMMENT '关联 design_product.id',
    package_file_id     BIGINT          NOT NULL COMMENT '关联 design_package_file.id',
    package_file_name   VARCHAR(256)    DEFAULT NULL COMMENT '文件名（冗余）',
    sort_order          INT             DEFAULT 0 COMMENT '排序',

    -- 公共字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_dpf_design_product_id (design_product_id),
    KEY idx_dpf_package_file_id (package_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打印产品关联文件表';


-- ============================================================
-- 指令单表（design_instruction）
-- 存储指令单信息，每个数据包对应一份
-- ============================================================
DROP TABLE IF EXISTS design_instruction;
CREATE TABLE design_instruction (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id            BIGINT          NOT NULL COMMENT '订单ID',
    package_id          BIGINT          NOT NULL COMMENT '数据包ID',
    instruction_code    VARCHAR(50)     NOT NULL COMMENT '指令单编号（ZL-XXXX）',
    version             VARCHAR(10)     DEFAULT 'A/1' COMMENT '版本号（A/1, A/2...）',
    version_seq         INT             DEFAULT 1 COMMENT '版本序号（1, 2, 3...）',
    template_file_id    VARCHAR(32)     DEFAULT NULL COMMENT '模板文件ID（系统生成）',
    template_file_url   VARCHAR(512)    DEFAULT NULL COMMENT '模板文件URL',
    revised_file_id     VARCHAR(32)     DEFAULT NULL COMMENT '修订版文件ID（设计师上传）',
    revised_file_url    VARCHAR(512)    DEFAULT NULL COMMENT '修订版文件URL',
    generate_time       DATETIME        DEFAULT NULL COMMENT '生成时间',
    revised_upload_time DATETIME        DEFAULT NULL COMMENT '修订版上传时间',
    is_confirmed        TINYINT         DEFAULT 0 COMMENT '指令单是否已确认（0=未确认，1=已确认；在线模式下生成/重新生成时重置为0，手动确认后置1；离线模式下上传修订版时自动置1）',
    confirm_time        DATETIME        DEFAULT NULL COMMENT '确认时间',
    issuer_id           BIGINT          DEFAULT NULL COMMENT '指令人ID',
    issuer_name         VARCHAR(64)     DEFAULT NULL COMMENT '指令人姓名（冗余）',
    issue_date          DATE            DEFAULT NULL COMMENT '指令日期',
    checker_id          BIGINT          DEFAULT NULL COMMENT '复核人ID',
    checker_name        VARCHAR(64)     DEFAULT NULL COMMENT '复核人姓名（冗余）',
    check_date          DATE            DEFAULT NULL COMMENT '复核日期',

    -- 公共字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_instruction_order_id (order_id),
    KEY idx_design_instruction_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指令单表';
CREATE UNIQUE INDEX uk_design_instruction_code ON design_instruction ((CASE WHEN is_deleted = 0 THEN instruction_code ELSE NULL END));
-- 防止并发生成时同一数据包写入重复版本号
CREATE UNIQUE INDEX uk_design_instruction_pkg_ver ON design_instruction ((CASE WHEN is_deleted = 0 THEN package_id ELSE NULL END), (CASE WHEN is_deleted = 0 THEN version_seq ELSE NULL END));


-- ============================================================
-- 图纸表（design_drawing）
-- 存储图纸信息，每个数据包对应一份
-- ============================================================
DROP TABLE IF EXISTS design_drawing;
CREATE TABLE design_drawing (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id            BIGINT          NOT NULL COMMENT '订单ID',
    package_id          BIGINT          NOT NULL COMMENT '数据包ID',
    page_count          INT             DEFAULT 1 COMMENT '总页数',
    version             VARCHAR(10)     DEFAULT 'A/1' COMMENT '版本号（与指令单同步）',
    version_seq         INT             DEFAULT 1 COMMENT '版本序号',
    template_file_id    VARCHAR(32)     DEFAULT NULL COMMENT '模板文件ID（系统生成）',
    template_file_url   VARCHAR(512)    DEFAULT NULL COMMENT '模板文件URL',
    revised_file_id     VARCHAR(32)     DEFAULT NULL COMMENT '修订版文件ID（设计师上传）',
    revised_file_url    VARCHAR(512)    DEFAULT NULL COMMENT '修订版文件URL',
    generate_time       DATETIME        DEFAULT NULL COMMENT '生成时间',
    revised_upload_time DATETIME        DEFAULT NULL COMMENT '修订版上传时间',
    designer_id         BIGINT          DEFAULT NULL COMMENT '设计人ID',
    designer_name       VARCHAR(64)     DEFAULT NULL COMMENT '设计人姓名（冗余）',
    design_date         DATE            DEFAULT NULL COMMENT '设计日期',
    auditor_id          BIGINT          DEFAULT NULL COMMENT '审核人ID',
    auditor_name        VARCHAR(64)     DEFAULT NULL COMMENT '审核人姓名（冗余）',
    audit_date          DATE            DEFAULT NULL COMMENT '审核日期',
    is_confirmed        TINYINT         DEFAULT 0 COMMENT '图纸是否已确认（0=未确认，1=已确认；在线模式下生成/重新生成时重置为0，手动确认后置1；离线模式下上传修订版时自动置1）',
    confirm_time        DATETIME        DEFAULT NULL COMMENT '确认时间',

    -- 公共字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_drawing_order_id (order_id),
    KEY idx_design_drawing_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图纸表';
-- 防止并发生成时同一数据包写入重复版本号
CREATE UNIQUE INDEX uk_design_drawing_pkg_ver ON design_drawing ((CASE WHEN is_deleted = 0 THEN package_id ELSE NULL END), (CASE WHEN is_deleted = 0 THEN version_seq ELSE NULL END));


-- ============================================================
-- 可视化模型文件表（design_model）
-- 精简版，文件详情通过 file_detail 查询
-- ============================================================
DROP TABLE IF EXISTS design_model;
CREATE TABLE design_model (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    file_id         VARCHAR(32)     NOT NULL COMMENT '文件ID（关联 file_detail.id）',

    -- 公共字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_model_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可视化模型文件表';


-- ============================================================
-- 设计审核记录表（design_review）
-- 存储设计审核历史记录
-- ============================================================
DROP TABLE IF EXISTS design_review;
CREATE TABLE design_review (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    reviewer_id     BIGINT          NOT NULL COMMENT '审核人ID',
    reviewer_name   VARCHAR(64)     DEFAULT NULL COMMENT '审核人姓名（冗余）',
    review_result   TINYINT         NOT NULL COMMENT '审核结果：0=驳回，1=通过',
    comment         TEXT            DEFAULT NULL COMMENT '审批意见（通过时）',
    reject_reason   TEXT            DEFAULT NULL COMMENT '驳回原因（驳回时必填）',
    review_time     DATETIME        NOT NULL COMMENT '审核时间',

    -- 公共字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_review_order_id (order_id),
    KEY idx_design_review_reviewer_id (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计审核记录表';


-- ============================================================
-- 订单主表扩展字段（ALTER）
-- 仅对已存在的生产库执行，新建环境直接使用完整建表语句
-- ============================================================
-- ALTER TABLE order_main ADD COLUMN design_mode TINYINT DEFAULT 1 COMMENT '设计模式：1=线下修改，2=在线编辑' AFTER design_submit_time;



-- ============================================================
-- 部位颜色透明度配置表（从 image-3d-ai 迁移，新增 opacity 字段）
-- ============================================================
DROP TABLE IF EXISTS part_colors;
CREATE TABLE part_colors
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    part_detail VARCHAR(255) NULL COMMENT '部位名称（与模型文件名精确匹配，去扩展名）',
    color_code  VARCHAR(255) NULL COMMENT '颜色RGB值（如：170,255,0）',
    opacity     DECIMAL(3, 2) DEFAULT 1.00 COMMENT '透明度（0.00~1.00，1=不透明）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '部位颜色透明度配置表';

CREATE INDEX idx_part_colors_part_detail ON part_colors (part_detail);
