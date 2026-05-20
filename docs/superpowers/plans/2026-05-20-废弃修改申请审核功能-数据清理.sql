-- ============================================================
-- 废弃修改申请审核功能 - 数据清理脚本
-- 执行前务必备份数据库！
-- ============================================================

-- 1. 查看当前存量申请数据（执行前先确认）
SELECT
    status,
    COUNT(*) as count,
    GROUP_CONCAT(DISTINCT order_code) as sample_orders
FROM erp_order_modify_apply
WHERE is_deleted = 0
GROUP BY status;

-- 2. 将所有待审核/已批准的申请改为已拒绝状态（避免阻断订单流转）
UPDATE erp_order_modify_apply
SET
    status = '30',  -- REJECTED
    reject_reason = '系统升级，修改申请审核流程已废弃，请使用直接修改功能',
    auditor_id = 1,  -- 系统管理员ID，根据实际情况调整
    auditor_name = '系统',
    audit_time = NOW(),
    update_time = NOW()
WHERE status IN ('10', '20')  -- PENDING, APPROVED
  AND is_deleted = 0;

-- 3. 验证清理结果（应该没有 PENDING/APPROVED 状态的记录）
SELECT
    status,
    COUNT(*) as count
FROM erp_order_modify_apply
WHERE is_deleted = 0
  AND status IN ('10', '20');

-- 预期结果：0 rows

-- 4. （可选）如果确认不再需要历史申请记录，可以逻辑删除所有申请
-- UPDATE erp_order_modify_apply SET is_deleted = 1, update_time = NOW();

-- 5. 验证是否有订单被阻断（执行清理后应该没有）
SELECT
    om.id,
    om.order_code,
    om.phase,
    om.status,
    oma.id as apply_id,
    oma.status as apply_status
FROM erp_order_main om
INNER JOIN erp_order_modify_apply oma
    ON om.id = oma.order_id
    AND oma.is_deleted = 0
    AND oma.status IN ('10', '20')
WHERE om.is_deleted = 0;

-- 预期结果：0 rows
