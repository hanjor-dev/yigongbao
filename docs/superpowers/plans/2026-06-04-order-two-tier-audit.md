-- ============================================================
-- 订单两级审核系统 - 数据库迁移
-- 版本: V1.0.1
-- 日期: 2026-06-04
-- ============================================================

-- 新增审核字段
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

-- 为现有订单初始化审核状态字段
UPDATE order_main 
SET design_audit_status = CASE 
    WHEN status = 1030 THEN 1  -- 已审核通过
    WHEN status = 1040 THEN 2  -- 已驳回
    ELSE 0  -- 待审核
END
WHERE status IN (1020, 1030, 1040);

-- 试用订单：如果已审核通过，默认认为区域审核和设计审核都已通过
UPDATE order_main 
SET regional_audit_status = 1
WHERE business_type = '11.3' 
  AND status = 1030;

-- 创建索引（用于审核列表查询性能优化）
CREATE INDEX idx_order_regional_audit 
    ON order_main(business_type, regional_audit_status, status);
CREATE INDEX idx_order_design_audit 
    ON order_main(design_audit_status, status);
