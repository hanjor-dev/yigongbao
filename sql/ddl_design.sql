-- ============================================================
-- 医工宝系统 - 设计阶段 DDL
-- 用于存储设计阶段相关表的建表语句
-- 创建日期：2026-04-15
-- 修订日期：2026-04-15（表名前缀统一为 design_，优化命名）
-- ============================================================


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
    material_id     BIGINT          DEFAULT NULL COMMENT '材质ID（字典）',
    material_name   VARCHAR(64)     DEFAULT NULL COMMENT '材质名称（冗余）',
    color_id        BIGINT          DEFAULT NULL COMMENT '颜色ID（字典）',
    color_name      VARCHAR(64)     DEFAULT NULL COMMENT '颜色名称（冗余）',
    quantity        INT             NOT NULL DEFAULT 1 COMMENT '数量',
    pack_quantity   INT             DEFAULT NULL COMMENT '包装数量',
    timeliness      VARCHAR(64)     DEFAULT NULL COMMENT '时效',
    product_mark    VARCHAR(128)    DEFAULT NULL COMMENT '产品标识',
    package_file_id BIGINT          NOT NULL COMMENT '数据包内文件ID（design_package_file.id）',
    package_file_name VARCHAR(256)  DEFAULT NULL COMMENT '文件名（冗余）',
    sort_order      INT             DEFAULT 0 COMMENT '排序序号',

    -- 公共字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    PRIMARY KEY (id),
    KEY idx_design_product_order_id (order_id),
    KEY idx_design_product_package_id (package_id),
    KEY idx_design_product_package_file_id (package_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打印产品信息表';




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
