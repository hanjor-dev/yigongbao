# 生产流转卡强制完成打印 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为生产管理员增加一个可审计、幂等且与 WebSocket 共用完成逻辑的强制完成打印接口。

**Architecture:** 抽取统一打印完成领域服务，WebSocket 监听器和人工接口共同调用；接口通过 `@RequirePermission` 保护，服务层再次校验生产管理员角色和加工中心数据范围。数据库迁移按资源编码解析 `Manufacture` 父资源和 `production-manager` 角色，不依赖固定 ID。

**Tech Stack:** Spring Boot、MyBatis-Plus、Sa-Token Session 权限、MySQL 8、JUnit 5、Mockito、Vue 前端。

---

### Task 1: 建立权限资源和迁移脚本

**Files:**
- Create: `sql/migration-production-force-complete-print-permission-20260903.sql`
- Modify: `sql/init.sql`（补充新环境初始化资源与生产管理员关联）

- [ ] 增加幂等 SQL：校验 `production-manager` 和 `Manufacture` 唯一；新增或恢复按钮资源 `manufacture:ForceCompletePrint`。
- [ ] 只插入 `sys_role_resource` 到 `production-manager`，发现其他角色已绑定时阻断。
- [ ] 增加执行后验收 SQL，并验证 `git diff --check`。

### Task 2: 先写打印完成领域服务测试

**Files:**
- Create/Modify: `yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionPrintLifecycleServiceTest.java`
- Test: `DeviceStatusListenerTest.java`

- [ ] 覆盖 `PRINTING` 成功完成、非打印中拒绝、已完成幂等、缺少打印工序回滚。
- [ ] 覆盖预计完成时间保留、为空时使用当前时间、后处理排程和订单聚合触发。
- [ ] 覆盖 WebSocket 与人工完成竞争时只有一次状态副作用。

### Task 3: 实现统一打印完成逻辑

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/ProductionPrintLifecycleService.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionPrintLifecycleServiceImpl.java`
- Modify: `DeviceStatusListener.java`

- [ ] 将现有完成逻辑迁移到服务，使用流转卡行锁和 `status=PRINTING` 条件更新。
- [ ] 保留 `print_finish_time` 作为现有排程基准；为空时写当前时间。
- [ ] 更新打印工序、后处理排程、订单 Flow 和生产状态补偿。
- [ ] 监听器只负责识别设备状态变化并调用统一服务，不伪造人工设备事件。

### Task 4: 增加强制完成接口和权限校验

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/dto/ForceCompletePrintDTO.java`
- Modify: `IProductionRecordService.java`
- Modify: `ProductionRecordServiceImpl.java`
- Modify: `ProductionRecordController.java`

- [ ] 新增 `POST /production/record/{recordId}/force-complete-print`。
- [ ] 增加 `@RequirePermission("manufacture:ForceCompletePrint")` 和操作日志。
- [ ] DTO 强制要求原因字段；服务层校验当前用户角色必须为 `production-manager`，并校验加工中心归属。
- [ ] 保留打印设备绑定，不修改设备实时状态。

### Task 5: 前端增加生产管理员操作入口

**Files:**
- Modify: `frontend` 中生产列表/详情源文件（根据实际源码定位）

- [ ] 仅在 `PRINTING` 且拥有 `manufacture:ForceCompletePrint` 时展示按钮。
- [ ] 增加二次确认和必填原因弹窗。
- [ ] 成功后刷新流转卡、订单和设备占用展示。

### Task 6: 回归测试和验收

- [ ] 运行生产模块定向测试、Flow 测试和权限相关测试。
- [ ] 运行 Maven 编译/测试及 `git diff --check`。
- [ ] 检查数据库脚本只修改目标资源和目标角色权限。
- [ ] 检查现有未跟踪文件未被纳入本次变更。

