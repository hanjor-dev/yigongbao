-- ============================================================
-- 线上数据更新脚本：修改"其他医院"机构编码
-- 执行日期：2026-06-25
-- 目的：将 ORG-H-0008 改为 ORG-H-9999，避免与业务编号冲突
-- ============================================================

-- 【步骤1】查看当前数据（执行前确认）
SELECT id, org_name, org_code, org_type, status, is_deleted
FROM sys_org
WHERE org_code = 'ORG-H-0008';

-- 【步骤2】更新机构编码
UPDATE sys_org
SET org_code = 'ORG-H-9999',
    update_time = NOW()
WHERE org_code = 'ORG-H-0008'
  AND org_name = '其他医院'
  AND is_deleted = 0;

-- 【步骤3】验证更新结果
SELECT id, org_name, org_code, org_type, status, is_deleted
FROM sys_org
WHERE org_code = 'ORG-H-9999';

-- 【步骤4】检查是否还有旧编码残留
SELECT COUNT(*) as remaining_count
FROM sys_org
WHERE org_code = 'ORG-H-0008';
