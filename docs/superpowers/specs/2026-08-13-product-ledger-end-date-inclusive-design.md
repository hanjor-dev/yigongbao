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
- `endTime` 非空：使用 `pr.print_start_time < endTime + 1 天`。
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

保留 `ProductLedgerExportDTO.endTime` 的 `LocalDateTime` 类型和 Controller 接口不变。在
`ProductionProductMapper` 的明细与计数 SQL 中，通过 MySQL `DATE_ADD(#{dto.endTime}, INTERVAL 1 DAY)`
计算排他上界：

```sql
AND pr.print_start_time < DATE_ADD(#{dto.endTime}, INTERVAL 1 DAY)
```

Service 层现有的“开始时间不能晚于结束时间”校验保持不变。数据权限、产品状态范围、排序、1 万条限制和 Excel 展示均不修改。

## 测试

先更新 `ProductionProductMapperSqlTest`，使测试在旧 SQL 下失败，再修改生产 SQL：

- 明细查询使用 `print_start_time < DATE_ADD(endTime, INTERVAL 1 DAY)`；
- 计数查询使用相同条件；
- 两条查询不再包含 `print_start_time <= endTime`；
- 开始时间条件仍为 `print_start_time >= startTime`；
- 其他过滤条件保持原有契约。

完成后运行 Mapper SQL 契约测试和生产模块完整测试。

## 非目标

- 不修改前端编译产物。
- 不把 DTO 改成 `LocalDate`。
- 不改变 Excel“时间”列仍展示订单创建日期的现有行为。
- 不修改打印开始事件、产品状态流转或数据权限逻辑。
