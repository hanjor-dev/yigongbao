# Task 1 实施报告：冻结契约和生成可执行 Schema

## 变更文件

仓库内新增：

- `yigongbao-parent/yigongbao-module-backup/src/main/resources/openapi/backup-internal-v1.yaml`
- `yigongbao-parent/sql/migration/2026-10-27-01-backup-candidate-receipt.sql`
- `yigongbao-parent/sql/migration/2026-10-27-02-backup-restore.sql`
- `yigongbao-parent/sql/migration/2026-10-27-03-backup-cleanup.sql`
- `yigongbao-parent/sql/migration/2026-10-27-04-backup-security.sql`
- `yigongbao-parent/sql/migration/2026-10-27-05-backup-reference-backfill.sql`
- `yigongbao-parent/sql/migration/2026-10-27-06-backup-candidate-backfill.sql`
- `yigongbao-parent/sql/migration/2026-10-27-07-backup-cleanup-resource.sql`

仓库外本地备份项目新增/确认：

- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/src/main/resources/openapi/local-backup-v1.yaml`
- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/src/main/resources/schema/manifest-v1.schema.json`
- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/00-create-database.sql`
- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/01-schema.sql`
- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/02-indexes.sql`

## 验证命令与结果

在 `D:/01_Project/02_Personal/医工宝/.worktrees/backup-implementation` 执行：

1. PowerShell 路径检查：在线 OpenAPI、本地 OpenAPI、manifest、7 个迁移文件和 3 个本地 init 文件均存在。
2. Python `yaml.safe_load`：两个 OpenAPI 文件解析成功（`YAML_OK`）。
3. Python `json.loads`：manifest 解析成功；required 字段为 `schemaVersion, snapshotId, orderSnapshot, source, assets, summary`（`JSON_OK`）。
4. 迁移结构检查：7 个文件均包含幂等/SQL guard 或验证说明（`MIGRATION_OK`）。
5. `2026-10-27-07-backup-cleanup-resource.sql` 检查通过：以 `sys_resource.resource_code='Backup'` 为父资源，包含 `BackupCleanup` 和六个按钮资源；角色使用 `role_code IN ('admin','company-admin')`；仅插入 `role_id,resource_id`；无硬编码角色/资源主键（`RESOURCE_PERMISSION_OK`）。

## 假设

- 按实施计划，Task 1 只在在线模块目录放置 OpenAPI 资源，Java/Maven 模块由 Task 2 创建。
- 本地备份项目位于仓库外，因此其契约和初始化脚本不纳入本次 Git 提交，但已按要求存在并完成快速校验。

## 关注事项

- 本次未启动数据库实例，也未执行真实 SQL migration；验证覆盖文件结构、解析、关键资源/角色约束和幂等性标记。
- 未实现任何运行时服务、Mapper XML 或 Java 模块代码。

## 追加验证记录（2026-10-26）

以下命令均在 `D:/01_Project/02_Personal/医工宝/.worktrees/backup-implementation` 执行，输出为本次实际结果：

```powershell
$required = @(
  'yigongbao-parent/yigongbao-module-backup/src/main/resources/openapi/backup-internal-v1.yaml',
  'yigongbao-parent/sql/migration/2026-10-27-01-backup-candidate-receipt.sql',
  'yigongbao-parent/sql/migration/2026-10-27-02-backup-restore.sql',
  'yigongbao-parent/sql/migration/2026-10-27-03-backup-cleanup.sql',
  'yigongbao-parent/sql/migration/2026-10-27-04-backup-security.sql',
  'yigongbao-parent/sql/migration/2026-10-27-05-backup-reference-backfill.sql',
  'yigongbao-parent/sql/migration/2026-10-27-06-backup-candidate-backfill.sql',
  'yigongbao-parent/sql/migration/2026-10-27-07-backup-cleanup-resource.sql',
  'D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/src/main/resources/openapi/local-backup-v1.yaml',
  'D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/src/main/resources/schema/manifest-v1.schema.json',
  'D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/00-create-database.sql',
  'D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/01-schema.sql',
  'D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/02-indexes.sql'
)
$missing = $required | Where-Object { -not (Test-Path -LiteralPath $_) }
"REQUIRED_PATH_CHECK missing=$($missing.Count) total=$($required.Count)"
```

```text
REQUIRED_PATH_CHECK missing=0 total=13
```

```powershell
python -c "import json; p=r'D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/src/main/resources/schema/manifest-v1.schema.json'; d=json.load(open(p, encoding='utf-8')); print('JSON_OK required=' + ','.join(d.get('required', [])))"
```

```text
JSON_OK required=schemaVersion,snapshotId,orderSnapshot,source,assets,summary
```

本地备份项目产物路径：

- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/src/main/resources/openapi/local-backup-v1.yaml`
- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/src/main/resources/schema/manifest-v1.schema.json`
- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/00-create-database.sql`
- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/01-schema.sql`
- `D:/01_Project/02_Personal/医工宝备份子系统/yigongbao-backup-server/sql/init/02-indexes.sql`
