-- ============================================================
-- 测试环境DDL变更脚本：production_record表新增产品类别字段
-- ============================================================
-- 适用于：H2数据库（测试环境）
-- 创建时间: 2026-07-16
-- 说明：简化版本，去除MySQL特有的验证查询，仅保留核心DDL

-- ============================================================
-- 步骤1: 新增 product_category 字段
-- ============================================================
ALTER TABLE production_record
ADD COLUMN product_category VARCHAR(50)
COMMENT '产品大类代码（如17.1，冗余自product.category）';

-- ============================================================
-- 步骤2: 新增 product_category_name 字段
-- ============================================================
ALTER TABLE production_record
ADD COLUMN product_category_name VARCHAR(100)
COMMENT '产品大类名称（如"模型"、"导板"，冗余自product.category_name）';

-- ============================================================
-- 步骤3: 创建索引（可选）
-- ============================================================
-- H2数据库索引语法与MySQL相同
CREATE INDEX idx_production_record_category
ON production_record(product_category);

-- ============================================================
-- 回滚脚本
-- ============================================================
/*
DROP INDEX idx_production_record_category;
ALTER TABLE production_record DROP COLUMN product_category_name;
ALTER TABLE production_record DROP COLUMN product_category;
*/
