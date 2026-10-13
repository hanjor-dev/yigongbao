# 历史订单列表接口

## 权限

- 查询与任务状态：`LegacyOrder`
- 增量迁移：`legacyOrder:Migrate`
- 授权角色：`admin`、`company-admin`、`designer-manager`

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/legacy-order/page` | 分页查询归档列表，`keyword` 同时匹配订单号、医院和患者 |
| GET | `/legacy-order/filter-options` | 获取归档数据中的状态、业务类型下拉选项 |
| POST | `/legacy-order/migration/incremental` | 创建增量迁移任务；已有运行中任务时返回其 ID |
| GET | `/legacy-order/migration/tasks/{taskId}` | 查询任务进度 |
| GET | `/legacy-order/migration/tasks/latest` | 查询最近任务 |
| POST | `/legacy-order/migration/tasks/page` | 分页查询迁移任务，可按 `status` 筛选 |

列表接口保留旧系统原始状态、业务类型、打印要求和时间原文；不转换为新版订单流程状态。接口响应仅来自 `legacy_order_list`，不会在查询时访问旧库。
