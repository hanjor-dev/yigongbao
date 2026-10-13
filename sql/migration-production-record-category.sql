-- ============================================================
-- 历史流转卡数据迁移：补填产品类别信息
-- ============================================================
-- 执行时机：数据库表结构变更完成后
-- 执行方式：业务低峰期执行，建议分批更新

-- 步骤1：检查需要迁移的数据量
SELECT
    COUNT(*) as total_records,
    SUM(CASE WHEN product_category IS NULL THEN 1 ELSE 0 END) as null_category_count
FROM production_record
WHERE is_deleted = 0;

-- 步骤2：批量更新产品类别信息（关联产品主数据表）
UPDATE production_record pr
INNER JOIN product p ON pr.product_id = p.id
SET
    pr.product_category = p.category,
    pr.product_category_name = p.category_name,
    pr.update_time = CURRENT_TIMESTAMP
WHERE pr.is_deleted = 0
  AND pr.product_category IS NULL
  AND p.is_deleted = 0;

-- 步骤3：验证迁移结果
SELECT
    COUNT(*) as total_records,
    SUM(CASE WHEN product_category IS NULL THEN 1 ELSE 0 END) as null_category_count,
    SUM(CASE WHEN product_category IS NOT NULL THEN 1 ELSE 0 END) as filled_category_count
FROM production_record
WHERE is_deleted = 0;

-- 步骤4：检查无法匹配的记录（产品主数据已被删除）
SELECT
    pr.id,
    pr.record_no,
    pr.product_id,
    pr.product_name,
    '产品主数据不存在' as reason
FROM production_record pr
LEFT JOIN product p ON pr.product_id = p.id AND p.is_deleted = 0
WHERE pr.is_deleted = 0
  AND pr.product_category IS NULL
  AND p.id IS NULL;

-- 注意事项：
-- 1. 如果存在产品主数据已删除的流转卡，需要人工确认产品类别
-- 2. 建议在测试环境先执行验证
-- 3. 执行前建议备份 production_record 表
