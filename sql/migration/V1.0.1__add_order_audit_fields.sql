-- ============================================================
-- 订单两级审核系统 - 数据库迁移
-- 版本: V1.0.1
-- 日期: 2026-06-04
-- 说明: 为order_main表新增区域审核和设计审核相关字段
-- ============================================================

-- 第一部分：字段添加
-- 新增8个审核字段（区域审核4个 + 设计审核4个）
ALTER TABLE order_main
ADD COLUMN regional_audit_status TINYINT DEFAULT NULL
    COMMENT '区域管理员审核状态：0-未审核，1-已通过，2-已驳回（仅试用订单）',
ADD COLUMN regional_audit_remark VARCHAR(500) DEFAULT NULL
    COMMENT '区域管理员审核备注',
ADD COLUMN regional_audit_time DATETIME DEFAULT NULL
    COMMENT '区域管理员审核时间',
ADD COLUMN regional_audit_by BIGINT DEFAULT NULL
    COMMENT '区域管理员审核人ID',
ADD COLUMN design_audit_status TINYINT DEFAULT 0
    COMMENT '设计管理员审核状态：0-未审核，1-已通过，2-已驳回',
ADD COLUMN design_audit_remark VARCHAR(500) DEFAULT NULL
    COMMENT '设计管理员审核备注',
ADD COLUMN design_audit_time DATETIME DEFAULT NULL
    COMMENT '设计管理员审核时间',
ADD COLUMN design_audit_by BIGINT DEFAULT NULL
    COMMENT '设计管理员审核人ID';

-- 第二部分：索引创建
-- 索引1：区域审核列表查询优化
CREATE INDEX idx_order_regional_audit
    ON order_main(business_type, regional_audit_status, status, operator_dept_id);

-- 索引2：设计审核列表查询优化
CREATE INDEX idx_order_design_audit
    ON order_main(design_audit_status, status);

-- 第三部分：历史数据迁移
-- 1. 待审核状态订单：设计审核状态设为"未审核"
UPDATE order_main
SET design_audit_status = 0
WHERE status = 1020;

-- 2. 已审核通过订单：设计审核状态设为"已通过"
UPDATE order_main
SET design_audit_status = 1
WHERE status = 1030;

-- 3. 已驳回订单：设计审核状态设为"已驳回"
UPDATE order_main
SET design_audit_status = 2
WHERE status = 1040;

-- 4. 试用订单且已审核通过：区域审核状态设为"已通过"
UPDATE order_main
SET regional_audit_status = 1
WHERE business_type = '11.3'
  AND status = 1030;

-- 5. 设计阶段和生产阶段订单：设计审核状态设为"已通过"
UPDATE order_main
SET design_audit_status = 1
WHERE status >= 2000;
