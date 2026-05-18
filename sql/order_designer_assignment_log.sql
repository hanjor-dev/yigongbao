-- ------------------------------------------------------------
-- 设计师分配记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS order_designer_assignment_log;
CREATE TABLE order_designer_assignment_log (
    id                      BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id                BIGINT          NOT NULL COMMENT '订单ID',
    order_code              VARCHAR(64)     COMMENT '订单编号（冗余字段）',

    -- 分配前的设计师信息
    old_designer_id         BIGINT          COMMENT '原设计师ID（首次分配时为NULL）',
    old_designer_name       VARCHAR(64)     COMMENT '原设计师姓名（冗余字段）',

    -- 分配后的设计师信息
    new_designer_id         BIGINT          NOT NULL COMMENT '新设计师ID',
    new_designer_name       VARCHAR(64)     NOT NULL COMMENT '新设计师姓名（冗余字段）',

    -- 分配操作信息
    assign_type             VARCHAR(16)     NOT NULL COMMENT '分配类型（AUTO=自动分配，MANUAL=手动分配）',
    operator_id             BIGINT          COMMENT '操作人ID（自动分配时为NULL）',
    operator_name           VARCHAR(64)     COMMENT '操作人姓名（冗余字段）',
    assign_time             DATETIME        NOT NULL COMMENT '分配时间',
    remark                  VARCHAR(512)    COMMENT '备注说明',

    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_old_designer_id (old_designer_id),
    KEY idx_new_designer_id (new_designer_id),
    KEY idx_assign_time (assign_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计师分配记录表';
