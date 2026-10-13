# 生产台账结束日期包含全天修复设计

## 背景

生产产品台账导出接口接收 `ProductLedgerExportDTO.startTime/endTime`，当前 SQL 直接使用：

```text
pr.print_start_time >= startTime
pr.print_start_time <= endTime
```

前端日期选择器把结束日期序列化为当天零点。例如选择 2026-08-13 时，实际请求为
`endTime=2026-08-13T00:00:00`，导致该日零点之后的打印记录全部被排除。

## 目标

将 `endTime` 解释为用户选择的结束日期，并使该日期全天都包含在导出范围中，同时保持现有请求格式和 DTO 类型兼容。

## 查询语义

- `startTime` 非空：继续使用 `pr.print_start_time >= startTime`。
- `endTime` 非空：忽略其时分秒，取其日历日期的次日零点作为排他上界。
- 两者均为空：不增加时间条件。
- 时间范围继续按流转卡打印开始时间 `production_record.print_start_time` 筛选。
- 明细查询与总数查询必须使用完全相同的时间条件。

例如请求：

```json
{
  "startTime": "2026-08-12T00:00:00",
  "endTime": "2026-08-13T00:00:00"
}
```

对应查询范围为：

```text
print_start_time >= 2026-08-12 00:00:00
print_start_time <  2026-08-14 00:00:00
```

采用次日零点半开区间，避免 `23:59:59` 无法覆盖毫秒或微秒数据的问题。

## 实现设计

保留 `ProductLedgerExportDTO.endTime` 的 `LocalDateTime` 类型和 Controller 接口不变。Service 在查询前
创建查询 DTO 副本，忽略原始结束时间的时分秒，将副本中的 `endTime` 规范化为该日历日期的次日零点：

```text
exclusiveEnd = endTime.toLocalDate().plusDays(1).atStartOfDay()
dto.endTime = exclusiveEnd
```

原始请求 DTO 不被修改，因此同一请求对象被重试或重复调用时不会再次增加一天。

`ProductionProductMapper` 的明细与计数 SQL 只使用规范化后的排他上界：

```sql
AND pr.print_start_time < #{dto.endTime}
```

因此即使调用方传入 `2026-08-13T15:00:00`，结束日期仍只解释为 2026-08-13，
有效上界为 `2026-08-14T00:00:00`，不会多包含次日下午。

Service 层将范围校验同步调整为比较规范化后的实际查询边界：

```text
exclusiveEnd = endTime.toLocalDate().plusDays(1).atStartOfDay()
startTime 必须早于 exclusiveEnd
```

这样 `startTime=2026-08-13T15:00:00`、`endTime=2026-08-13T00:00:00` 是有效的同日范围；
若 `startTime >= 2026-08-14T00:00:00`，则继续抛出“开始时间不能晚于结束时间”。

数据权限、产品状态范围、排序、1 万条限制和 Excel 展示均不修改。

## 测试

先更新 `ProductionProductMapperSqlTest`，使测试在旧 SQL 下失败，再修改生产 SQL：

- 明细查询使用 `print_start_time < endTime`；
- 计数查询使用相同条件；
- 两条查询不再包含 `print_start_time <= endTime`；
- 开始时间条件仍为 `print_start_time >= startTime`；
- 其他过滤条件保持原有契约。

为 Service 规范化和范围校验增加单元测试，捕获传给 Mapper 的 DTO，并覆盖：

- 原始结束时间无论是否为零点，传入 Mapper 时均为该日期的次日零点；
- 同一原始 DTO 重复导出时，两次 Mapper 上界一致，原始 DTO 保持不变；
- 同一天内“开始时刻晚于结束日期零点”仍合法；
- 开始时刻达到次日零点时被拒绝。

增加使用真实测试数据库执行的 Mapper 边界测试，至少插入并验证：

- 结束日期当天下午的打印记录被命中；
- 次日零点的打印记录不被命中；
- 非零点原始 `endTime` 经 Service 规范化后仍以该日次日零点为上界。

完成后运行 Mapper SQL 契约测试和生产模块完整测试。

## 非目标

- 不修改前端编译产物。
- 不把 DTO 改成 `LocalDate`。
- 不改变 Excel“时间”列仍展示订单创建日期的现有行为。
- 不修改打印开始事件、产品状态流转或数据权限逻辑。
