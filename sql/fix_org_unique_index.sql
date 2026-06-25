-- ============================================================
-- 修复 sys_org 表唯一索引（支持逻辑删除）
-- 执行日期：2026-06-25
-- ============================================================

-- 1. 删除旧的普通唯一索引（如果存在）
DROP INDEX IF EXISTS uk_org_code ON sys_org;
DROP INDEX IF EXISTS uk_org_name ON sys_org;

-- 2. 创建函数唯一索引（仅对未删除记录生效）
CREATE UNIQUE INDEX uk_org_code ON sys_org ((CASE WHEN is_deleted = 0 THEN org_code ELSE NULL END));
CREATE UNIQUE INDEX uk_org_name ON sys_org ((CASE WHEN is_deleted = 0 THEN org_name ELSE NULL END));

-- 说明：
-- - 函数索引使得 is_deleted=0 时唯一约束生效，is_deleted=1 时返回 NULL（多个 NULL 不冲突）
-- - 这样已删除记录不会占用唯一索引槽位，支持多次删除/重建相同数据
