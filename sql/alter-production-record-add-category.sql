-- ============================================================
-- 数据库变更脚本：production_record表新增产品类别字段
-- ============================================================
-- 创建时间: 2026-07-16
-- 目标: 在流转卡表中新增产品类别字段，支持"模型"/"导板"等分类显示
--
-- 执行前检查清单:
--   [ ] 已在测试环境完整验证
--   [ ] 已完整备份生产数据库
--   [ ] 已通知相关开发人员
--   [ ] 已确认业务代码已准备好适配新字段
--   [ ] 已选择业务低峰期执行时间窗口
--
-- 预计影响:
--   - 预计执行时间: < 1分钟
--   - 影响表: production_record
--   - 停机要求: 不需要停机，DDL操作瞬时完成
--
-- 执行方式:
--   方式1: mysql -u用户名 -p数据库名 < alter-production-record-add-category.sql
--   方式2: 在MySQL客户端中逐步执行，每步验证
--
-- 回滚脚本: 见本文件末尾
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 执行前验证：检查表是否存在
-- ============================================================
SELECT '========================================' AS '';
SELECT '开始执行数据库变更脚本' AS '状态';
SELECT '执行时间：', NOW() AS '';
SELECT '========================================' AS '';

-- 检查 production_record 表是否存在
SELECT
    CASE
        WHEN COUNT(*) = 1 THEN '✓ production_record表存在，可以继续'
        ELSE '✗ production_record表不存在，请检查数据库'
    END AS '预检查结果'
FROM information_schema.tables
WHERE table_schema = DATABASE()
AND table_name = 'production_record';

-- ============================================================
-- 步骤1: 新增 product_category 字段（产品大类代码）
-- 变更说明: 冗余存储产品大类代码，用于区分"模型"、"导板"等分类
-- 影响范围: 流转卡详情查询、流转卡列表筛选
-- 风险等级: 低（新增字段，允许NULL，不影响现有数据）
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤1: 新增 product_category 字段' AS '执行状态';
SELECT '========================================' AS '';

ALTER TABLE production_record
ADD COLUMN product_category VARCHAR(50)
COMMENT '产品大类代码（如17.1，冗余自product.category）'
AFTER product_name;

-- 验证字段新增
SELECT
    COLUMN_NAME as '字段名',
    COLUMN_TYPE as '类型',
    IS_NULLABLE as '允许NULL',
    COLUMN_DEFAULT as '默认值',
    COLUMN_COMMENT as '注释'
FROM information_schema.columns
WHERE table_schema = DATABASE()
AND table_name = 'production_record'
AND column_name = 'product_category';

SELECT '✓ product_category 字段新增完成' AS '步骤1结果';

-- ============================================================
-- 步骤2: 新增 product_category_name 字段（产品大类名称）
-- 变更说明: 冗余存储产品大类名称，前端直接显示，无需查字典
-- 影响范围: 流转卡详情显示、流转卡列表显示
-- 风险等级: 低（新增字段，允许NULL，不影响现有数据）
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤2: 新增 product_category_name 字段' AS '执行状态';
SELECT '========================================' AS '';

ALTER TABLE production_record
ADD COLUMN product_category_name VARCHAR(100)
COMMENT '产品大类名称（如"模型"、"导板"，冗余自product.category_name）'
AFTER product_category;

-- 验证字段新增
SELECT
    COLUMN_NAME as '字段名',
    COLUMN_TYPE as '类型',
    IS_NULLABLE as '允许NULL',
    COLUMN_DEFAULT as '默认值',
    COLUMN_COMMENT as '注释'
FROM information_schema.columns
WHERE table_schema = DATABASE()
AND table_name = 'production_record'
AND column_name = 'product_category_name';

SELECT '✓ product_category_name 字段新增完成' AS '步骤2结果';

-- ============================================================
-- 步骤3: 创建索引（可选，如需按产品类别筛选流转卡）
-- 变更说明: 为 product_category 字段创建普通索引，优化按类别筛选查询
-- 影响范围: 流转卡列表筛选性能
-- 风险等级: 低（索引创建瞬时完成，表数据量不大）
-- 是否必需: 可选 - 如果流转卡列表需要按产品类别筛选，建议创建
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤3: 创建索引（可选）' AS '执行状态';
SELECT '========================================' AS '';

-- 如果不需要按产品类别筛选，可以跳过此步骤
CREATE INDEX idx_production_record_category
ON production_record(product_category);

-- 验证索引创建
SELECT
    INDEX_NAME as '索引名',
    COLUMN_NAME as '列名',
    NON_UNIQUE as '是否非唯一(1=是)',
    INDEX_TYPE as '索引类型'
FROM information_schema.statistics
WHERE table_schema = DATABASE()
AND table_name = 'production_record'
AND index_name = 'idx_production_record_category';

SELECT '✓ 索引创建完成' AS '步骤3结果';

-- ============================================================
-- 执行后验证：检查所有变更是否成功
-- ============================================================

SELECT '========================================' AS '';
SELECT '开始执行后验证' AS '验证状态';
SELECT '========================================' AS '';

-- 验证1：检查新增字段
SELECT '验证1：检查新增字段' AS '';
SELECT
    COLUMN_NAME as '字段名',
    COLUMN_TYPE as '类型',
    IS_NULLABLE as '允许NULL',
    COLUMN_COMMENT as '注释'
FROM information_schema.columns
WHERE table_schema = DATABASE()
AND table_name = 'production_record'
AND column_name IN ('product_category', 'product_category_name')
ORDER BY ORDINAL_POSITION;

-- 验证2：统计字段填充情况
SELECT '验证2：统计字段填充情况' AS '';
SELECT
    COUNT(*) as '总记录数',
    SUM(CASE WHEN product_category IS NOT NULL THEN 1 ELSE 0 END) as 'category非空数',
    SUM(CASE WHEN product_category_name IS NOT NULL THEN 1 ELSE 0 END) as 'category_name非空数',
    SUM(CASE WHEN product_category IS NULL THEN 1 ELSE 0 END) as 'category为空数'
FROM production_record
WHERE is_deleted = 0;

-- ============================================================
-- 执行完成总结
-- ============================================================

SELECT '========================================' AS '';
SELECT '数据库变更执行完成' AS '状态';
SELECT NOW() AS '完成时间';
SELECT '========================================' AS '';

SELECT '变更汇总' AS '';
SELECT '1. 新增 product_category 字段（产品大类代码）' AS '变更内容';
SELECT '2. 新增 product_category_name 字段（产品大类名称）' AS '变更内容';
SELECT '3. 创建 idx_production_record_category 索引（可选）' AS '变更内容';

SELECT '后续操作' AS '';
SELECT '1. 执行历史数据迁移脚本（sql/migration-production-record-category.sql）' AS '建议';
SELECT '2. 部署代码变更（实体类+VO+监听器）' AS '建议';
SELECT '3. 验证新创建的流转卡包含产品类别信息' AS '建议';
SELECT '4. 验证流转卡详情API返回包含产品类别字段' AS '建议';

-- ============================================================
-- 回滚脚本（如果需要回滚，请执行以下语句）
-- ============================================================
-- 注意：回滚将删除新增的字段和索引，已填充的数据将丢失
-- 建议：回滚前先备份 production_record 表

/*
-- 回滚步骤1：删除索引
DROP INDEX idx_production_record_category ON production_record;

-- 回滚步骤2：删除 product_category_name 字段
ALTER TABLE production_record DROP COLUMN product_category_name;

-- 回滚步骤3：删除 product_category 字段
ALTER TABLE production_record DROP COLUMN product_category;

-- 回滚验证
SELECT
    COLUMN_NAME as '字段名'
FROM information_schema.columns
WHERE table_schema = DATABASE()
AND table_name = 'production_record'
AND column_name IN ('product_category', 'product_category_name');
-- 预期结果：无记录（字段已删除）
*/

-- ============================================================
-- 脚本结束
-- ============================================================
SELECT '========================================' AS '';
SELECT '✓ DDL变更脚本执行完成' AS '最终状态';
SELECT '========================================' AS '';
