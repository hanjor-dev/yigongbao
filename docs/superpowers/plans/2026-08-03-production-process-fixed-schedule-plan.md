# 生产后处理工序固定时间排程 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将医疗器械订单的酒精初洗、UV 固化、超声清洗时间改为基于打印完成时间的固定排程，同时保持打印和包装流程行为不变。

**Architecture:** 由 `ProductionProcessServiceImpl` 提供幂等的后处理排程能力，统一按打印完成时间计算三道工序的时间；`DeviceStatusListener` 在打印状态条件更新成功后调用该能力。后处理开始/完成接口只补算缺失排程并更新操作状态，不再使用用户点击时间覆盖固定时间。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、JUnit 5、Mockito、Maven。

---

## 文件结构与职责

- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/IProductionProcessService.java` — 暴露打印完成后的后处理排程接口。
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImpl.java` — 定义固定时长/间隔、幂等排程、缺失时间补算，以及后处理开始/完成逻辑。
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DeviceStatusListener.java` — 用秒级打印完成时间做条件状态更新，并触发后处理排程。
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/entity/ProductionProcessEntity.java` — 更新时间字段注释，说明后处理时间是固定排程时间。
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImplTest.java` — 覆盖固定时间计算、补算、操作时间不覆盖及异常场景。
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java` — 覆盖打印完成事件的秒级基准、条件更新和排程调用。
- No change: `sql/ddl.sql` — `production_process.start_time/end_time` 已具备所需存储能力，无需新增字段或迁移。

### Task 1: 为固定排程写失败测试

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImplTest.java`

- [ ] **Step 1: 增加三道工序秒级排程测试**

构造 `printFinishTime = 2026-07-19T19:33:42` 和 `wash/cure/clean_dry` 三条工序，调用计划中的排程接口，断言时间分别为 `19:35:42-19:45:42`、`19:46:42-20:26:42`、`20:27:42-20:37:42`。

- [ ] **Step 2: 增加纳秒截断、缺少工序和重复工序测试**

验证排程使用 `withNano(0)`；缺少某一道后处理工序时只跳过该工序；同一工序出现重复记录时抛出业务异常。

- [ ] **Step 3: 运行新增测试确认按预期失败**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -Dtest=ProductionProcessServiceImplTest test`

Expected: FAIL，因为排程接口和固定时间行为尚未实现。

### Task 2: 实现统一的固定排程能力

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/IProductionProcessService.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImpl.java`

- [ ] **Step 1: 增加排程接口和固定常量**

增加 `schedulePostProcessing(Long recordId, LocalDateTime printFinishTime)` 接口；在实现类中集中定义间隔 2/1/1 分钟及时长 10/40/10 分钟，避免散落魔法数字。

- [ ] **Step 2: 实现幂等排程**

按 `production_record_id` 查询工序，按 `process_type` 定位目标记录；基准时间先 `withNano(0)`，依次计算并写入 `wash/cure/clean_dry`。不存在的目标工序跳过；重复目标工序抛出 `PARAM_ERROR` 业务异常，确保事务回滚。

- [ ] **Step 3: 运行排程测试确认通过**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -Dtest=ProductionProcessServiceImplTest test`

Expected: 排程计算、纳秒截断、部分工序和重复工序测试 PASS。

### Task 3: 修改后处理开始/完成操作，禁止覆盖固定时间

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImplTest.java`

- [ ] **Step 1: 增加失败测试**

新增测试：后处理工序已有固定 `start_time/end_time` 时，调用 `startProcess` 和 `finishProcess` 后时间保持不变；设备 `processing_minutes` 与用户点击时间不影响固定结束时间。另测 `print_finish_time` 缺失且需要补算时抛出业务异常，状态不变。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -Dtest=ProductionProcessServiceImplTest test`

Expected: FAIL，当前 `startProcess` 会写入当前时间，`finishProcess` 会按设备耗时/当前时间计算结束时间。

- [ ] **Step 3: 实现补算和操作逻辑**

在后处理开始/完成前检查时间是否缺失；若缺失则使用流转卡的 `print_finish_time` 调用统一排程接口。后处理开始不再设置 `start_time = now`，后处理完成不再重新计算 `end_time`；包装类型保留现有逻辑。补算失败时事务回滚，不推进状态。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl yigongbao-module-production -am -Dtest=ProductionProcessServiceImplTest test`

Expected: 固定时间保持、补算和缺失基准异常测试 PASS。

### Task 4: 接入打印完成事件并增加并发保护测试

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DeviceStatusListener.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java`

- [ ] **Step 1: 增加失败测试**

为监听器注入 `IProductionProcessService` mock；新增测试验证打印完成时传入秒级完成时间调用排程，并验证状态条件更新影响行数为 0 时不重复排程。保留包装工序不被排程更新的断言。

- [ ] **Step 2: 运行监听器测试确认失败**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -Dtest=DeviceStatusListenerTest test`

Expected: FAIL，因为当前监听器没有调用后处理排程，也没有用条件更新结果做幂等保护。

- [ ] **Step 3: 实现打印完成接入**

打印完成时间使用 `LocalDateTime.now().withNano(0)`；使用 `id + status=PRINTING` 条件更新生产记录，只有更新成功才调用排程接口、触发 Flow 和状态补偿。重复状态事件直接忽略。

- [ ] **Step 4: 运行监听器测试确认通过**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -Dtest=DeviceStatusListenerTest test`

Expected: 打印完成、秒级时间和重复事件保护测试 PASS。

### Task 5: 更新注释并执行完整验证

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/entity/ProductionProcessEntity.java`

- [ ] **Step 1: 更新实体时间字段注释**

明确打印工序时间来自设备事件，`wash/cure/clean_dry` 时间来自固定排程，`pack` 保持包装操作时间。

- [ ] **Step 2: 运行生产模块全部测试**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am test`

Expected: Exit code 0，所有测试通过。

- [ ] **Step 3: 运行编译和差异检查**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -DskipTests compile` and `git diff --check`

Expected: 编译成功，差异检查无输出。

- [ ] **Step 4: 检查变更范围并提交实现**

Run: `git status --short` and `git diff --stat`

Expected: 仅包含规格计划及本需求涉及的生产服务、监听器、实体和测试文件；确认后提交 `feat: schedule production post-processing times`。
