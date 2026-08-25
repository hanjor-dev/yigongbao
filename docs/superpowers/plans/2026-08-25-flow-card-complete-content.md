# 流转卡 Excel 完整内容 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让流转卡 Excel 在打印开始及后续状态都具备完整设备和打印时间内容。

**Architecture:** WebSocket 在 Basic 模块解析设备端打印元数据，并通过 Common 状态事件传递给 Production；Production 在打印状态监听器中写入统一的打印起止时间。Excel 生成时优先读取已管理设备，缺失时按加工中心和设备类型 `LIMIT 1` 查询设备表，不回写工序数据。

**Tech Stack:** Java、Spring WebSocket、Spring ApplicationEvent、MyBatis-Plus、Apache POI、JUnit 5、Mockito。

---

### Task 1: 扩展 WebSocket 打印元数据模型与解析

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/dto/DeviceStatusPushDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/impl/DeviceServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/event/DeviceStateChangeEvent.java`
- Test: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/websocket/DeviceWebSocketHandlerTest.java`
- Test: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/service/DeviceServiceImplTest.java`

- [ ] 增加 `print_start_time`、`estimated_duration` 字段，保持旧消息兼容。
- [ ] 增加时间格式 `yyyy-MM-dd HH:mm:ss` 和中文天/小时/分钟解析，解析失败只记录日志并返回空值。
- [ ] 将解析后的开始时间、预计分钟数和推算结束时间放入状态事件。
- [ ] 增加新旧消息、完整耗时、缺失单位和非法耗时测试。
- [ ] 运行 Basic 模块相关测试，确认既有设备状态测试通过。

### Task 2: 调整打印状态监听器的时间写入

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DeviceStatusListener.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java`

- [ ] 打印开始优先使用事件中的显式开始时间，否则回退 `LocalDateTime.now()`。
- [ ] 使用开始时间加预计分钟数计算结束时间，写入流转卡和打印工序。
- [ ] 保持打印完成只由原有状态转换触发；已有推算结束时间时不被当前时间覆盖，没有推算值时才使用当前时间。
- [ ] 确保打印记录、打印工序和后处理排程使用同一结束时间。
- [ ] 保证打印开始和完成均更新 `content_update_time`。
- [ ] 增加显式时间、预计耗时、旧消息回退和重复事件测试。

### Task 3: 实现 Excel 工序设备兜底查询

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`

- [ ] 建立工序到设备类型的集中映射。
- [ ] 主设备编号优先使用工序已有编号，缺失时按加工中心和设备类型查询一条设备。
- [ ] 清洗干燥辅助设备缺失时按 `AIR_COMPRESSOR` 查询一条设备。
- [ ] 打印设备优先使用已分配设备，兜底结果只用于 Excel，不写回业务表。
- [ ] 查询不到设备时传递空值，由 Builder 输出 `-`。
- [ ] 增加跨加工中心隔离、多设备 `LIMIT 1`、辅助设备类型和已有值优先测试。

### Task 4: 验证完整 Excel 内容与缓存刷新

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/helper/FlowCardExcelBuilder.java`（仅在必要处保持现有展示规则）
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/FlowCardExcelBuilderTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`

- [ ] 验证表头打印起止时间来自记录字段。
- [ ] 验证洗、固化、清洗干燥仍追加开始/结束时间。
- [ ] 验证打印和包装不新增工序时间文本。
- [ ] 验证打印开始阶段后续工序设备编号已填充，时间为空时仍生成完整 Excel。
- [ ] 验证状态更新后 `content_update_time` 使旧缓存失效。
- [ ] 运行 Production 模块全部相关测试。

### Task 5: 全量验证与交付检查

**Files:**
- No new files.

- [ ] 运行 Basic、Common、Production 模块测试。
- [ ] 检查旧 WebSocket 消息、旧数据库记录和设备查询失败场景。
- [ ] 检查工作区差异，只保留本需求相关文件。
- [ ] 按仓库提交规范完成一次中文 Conventional Commit。

