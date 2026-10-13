# 流转卡 Excel 完整内容与打印时间设计

## 目标

在打印开始及后续状态查询时，流转卡 Excel 均生成完整内容：尚未进入的工序也能显示对应设备编号；打印开始消息中的显式开始时间和预计耗时能够计算并填充打印结束时间；保持现有打印完成状态推进逻辑及工序时间展示范围。

## 设计决策

1. 工序设备编号优先使用 `production_process.device_no`；为空时按流转卡加工中心和工序设备类型查询 `device` 表，限制 `LIMIT 1`。清洗干燥辅助设备类型固定为 `AIR_COMPRESSOR`。打印设备仍优先使用已分配设备，查询结果只作为打印前 Excel 的展示兜底。
2. WebSocket DTO 接收 `print_start_time` 和 `estimated_duration` 原始字符串，在 Basic 层解析成开始时间、预计分钟数和推算结束时间，并通过设备状态事件传给 Production 模块。
3. 打印开始状态事件仍负责推进流转卡进入打印中，但时间优先使用设备显式开始时间；打印结束时间按开始时间加预计耗时计算并写入现有 `print_finish_time` 字段。若解析失败，则回退现有服务器时间逻辑。
4. 打印完成仍由现有 `WORKING/PRINT_FINISHED/OFFLINE -> IDLE` 状态转换触发。已推算的结束时间不被当前时间覆盖；没有有效推算值时使用当前时间。
5. Excel 仍仅对洗、固化、清洗干燥追加工序开始/结束文本，不为打印和包装工序增加额外时间文本。Excel 表头继续展示流转卡打印起止时间。
6. 保留现有 Excel 缓存机制，打印开始、打印完成、后处理排程等影响内容的状态变化必须更新 `content_update_time`。

## 影响范围

- Basic：`DeviceStatusPushDTO`、设备状态推送解析及事件构造。
- Common：`DeviceStateChangeEvent` 增加可选打印时间元数据。
- Production：`DeviceStatusListener` 写入打印时间；`ProductionRecordServiceImpl` 完成设备兜底查询；`FlowCardExcelBuilder` 保持既有时间展示范围并验证空值行为。
- Tests：WebSocket DTO/解析、打印状态监听、设备兜底查询、Excel Builder 和缓存刷新回归测试。

## 兼容与异常

- 旧版 WebSocket 消息没有新增字段时继续按现有服务器时间逻辑处理。
- 预计耗时支持天、小时、分钟任意缺省组合；无法解析时记录警告，不阻断设备状态更新。
- 设备查询不到时 Excel 写入 `-`，不回写工序设备字段。
- 同一状态消息重复到达时保持现有幂等条件，不重复推进流转卡。

