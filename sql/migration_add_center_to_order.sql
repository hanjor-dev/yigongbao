-- ============================================================
-- 数据迁移脚本：为订单表添加加工中心字段
-- 执行时间：2026-06-16
-- 说明：支持 CENTER 数据权限类型
-- ============================================================

-- 1. 为订单主表添加加工中心字段
ALTER TABLE order_main
ADD COLUMN center_id BIGINT COMMENT '加工中心ID',
ADD COLUMN center_name VARCHAR(100) COMMENT '加工中心名称（冗余）';

-- 2. 添加索引
CREATE INDEX idx_order_center_id ON order_main(center_id);

-- 3. 更新角色表数据权限配置
UPDATE sys_role
SET data_scope_type = 'center',
    role_desc = '执行生产任务，可见本加工中心数据'
WHERE role_code = 'production-worker';

UPDATE sys_role
SET data_scope_type = 'center',
    role_desc = '生产任务分配、进度管理，可见本加工中心数据'
WHERE role_code = 'production-manager';
