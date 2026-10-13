-- 订单重建项目分类快照修复
-- 目的：回填历史订单/草稿中缺失的分类字段。
-- 执行前请先确认 rebuild_project 中的 category_code/category_name 已完成主数据治理。

-- 1. 检查仍存在不完整分类的重建项目
SELECT id, name, category_code, category_name
FROM rebuild_project
WHERE is_deleted = 0
  AND (category_code IS NULL OR category_name IS NULL OR category_code = '' OR category_name = '');

-- 记录执行前影响范围，便于发布后核对。
SET @order_item_missing_before = (
    SELECT COUNT(*) FROM order_item
    WHERE project_id IS NOT NULL
      AND (category_code IS NULL OR category_code = '' OR category_name IS NULL OR category_name = '')
);
SET @order_item_draft_missing_before = (
    SELECT COUNT(*) FROM order_item_draft
    WHERE project_id IS NOT NULL
      AND (category_code IS NULL OR category_code = '' OR category_name IS NULL OR category_name = '')
);
SELECT @order_item_missing_before AS order_item_missing_before,
       @order_item_draft_missing_before AS order_item_draft_missing_before;

START TRANSACTION;

-- 2. 回填订单明细，仅更新缺失字段，保留已有快照值
UPDATE order_item oi
JOIN rebuild_project rp ON rp.id = oi.project_id AND rp.is_deleted = 0
SET oi.category_code = COALESCE(NULLIF(oi.category_code, ''), rp.category_code),
    oi.category_name = COALESCE(NULLIF(oi.category_name, ''), rp.category_name)
WHERE oi.project_id IS NOT NULL
  AND (oi.category_code IS NULL OR oi.category_code = ''
       OR oi.category_name IS NULL OR oi.category_name = '');
SET @order_item_updated = ROW_COUNT();

-- 3. 回填草稿明细，避免后续提交再次复制空快照
UPDATE order_item_draft oid
JOIN rebuild_project rp ON rp.id = oid.project_id AND rp.is_deleted = 0
SET oid.category_code = COALESCE(NULLIF(oid.category_code, ''), rp.category_code),
    oid.category_name = COALESCE(NULLIF(oid.category_name, ''), rp.category_name)
WHERE oid.project_id IS NOT NULL
  AND (oid.category_code IS NULL OR oid.category_code = ''
       OR oid.category_name IS NULL OR oid.category_name = '');
SET @order_item_draft_updated = ROW_COUNT();

SELECT @order_item_updated AS order_item_updated,
       @order_item_draft_updated AS order_item_draft_updated;

-- 4. 验证仍无法回填的数据（通常表示主数据本身缺失或项目已删除）
SELECT 'order_item' AS source, oi.id, oi.order_id, oi.project_id,
       oi.category_code, oi.category_name
FROM order_item oi
WHERE oi.project_id IS NOT NULL
  AND (oi.category_code IS NULL OR oi.category_code = ''
       OR oi.category_name IS NULL OR oi.category_name = '')
UNION ALL
SELECT 'order_item_draft' AS source, oid.id, oid.draft_id, oid.project_id,
       oid.category_code, oid.category_name
FROM order_item_draft oid
WHERE oid.project_id IS NOT NULL
  AND (oid.category_code IS NULL OR oid.category_code = ''
       OR oid.category_name IS NULL OR oid.category_name = '');

COMMIT;

-- 提交后复核；若仍有记录，需治理对应重建项目或人工补录。
SELECT 'order_item' AS source, COUNT(*) AS unresolved_count
FROM order_item oi
WHERE oi.project_id IS NOT NULL
  AND (oi.category_code IS NULL OR oi.category_code = ''
       OR oi.category_name IS NULL OR oi.category_name = '')
UNION ALL
SELECT 'order_item_draft' AS source, COUNT(*) AS unresolved_count
FROM order_item_draft oid
WHERE oid.project_id IS NOT NULL
  AND (oid.category_code IS NULL OR oid.category_code = ''
       OR oid.category_name IS NULL OR oid.category_name = '');
