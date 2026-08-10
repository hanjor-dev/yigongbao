# 生产台账打印开始时间筛选设计

## 目标

生产产品台账导出的 `startTime`、`endTime` 不再按订单创建时间筛选，而是共同限定流转卡的打印开始时间 `production_record.print_start_time`。

## 查询语义

- `startTime` 非空：`pr.print_start_time >= startTime`
- `endTime` 非空：`pr.print_start_time <= endTime`
- 两者均为空：不增加时间条件
- 任一时间条件存在时，打印开始时间为空的流转卡不会命中

时间范围继续使用闭区间，现有“开始时间不能晚于结束时间”校验保持不变。

## 变更边界

- 同步修改台账明细查询和总数查询，确保空结果判断、万条上限提示和实际导出一致。
- 更新 DTO 注释与 SQL 契约测试。
- 不修改 Controller、Excel 展示的订单创建日期、排序规则或数据库索引。

## 验证

SQL 契约测试必须断言两段导出查询都使用 `pr.print_start_time`，并且不再使用 `om.create_time` 作为时间过滤条件；随后运行生产模块完整测试。
