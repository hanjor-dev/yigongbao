# 生产流转卡强制释放打印设备配置 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让待打印流转卡能够原子地撤销整次打印设备配置，使原设备可重新分配并让当前流转卡从头配置。

**Architecture:** 在现有 ProductionRecord Controller/Service 中增加释放动作，以一个事务对流转卡、PRINT 工序和产品执行对称清理；通过行锁与监听器条件更新封闭分配、释放、设备状态事件之间的竞争窗口；通过联合索引优化设备占用查询。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、MySQL 8、JUnit 5、Mockito、MockMvc、Maven。

---

### Task 0: 建立可验证基线

**Files:**
- Local cache only: `D:/08_Maven_Repo/com/yigongbao/**/maven-metadata-local.xml`

- [x] **Step 1:** 枚举并备份 `com/yigongbao` 命名空间内首字节为 NUL 的损坏 metadata。
- [x] **Step 2:** 运行 Reactor install，让 production 使用当前 design/order 等依赖。
- [x] **Step 3:** 运行生产模块全量测试，确认修改前基线通过（216 tests，0 failures，0 errors）。

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -DskipTests install
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production test
```

### Task 1: 用失败测试定义接口与释放行为

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/controller/ProductionRecordControllerTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java`

- [x] **Step 1:** 增加 `POST /{id}/release-device` 委托 Service 的 Controller 测试，并反射校验操作日志类型为 `CANCEL`、名称为“强制释放打印设备配置”。
- [x] **Step 2:** 增加待打印记录释放时清空 record 的 `printDeviceId/Code/Name/material`、PRINT process 的 `deviceId/No/Name/processParams/operatorId/Name`、product 的 `productNo/weight`，并断言 `contentUpdateTime` 更新。
- [x] **Step 3:** 增加非待打印拒绝、重复释放幂等、已分配禁止重复 assign 的测试。
- [x] **Step 4:** 增加监听器条件更新返回 0 时不产生后续副作用的测试。
- [x] **Step 5:** 断言释放不调用设备日计数器和产品编号生成服务。
- [x] **Step 6:** 运行针对性测试，确认因接口或行为尚不存在而失败。

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production '-Dtest=ProductionRecordControllerTest,ProductionRecordAssignDeviceTest,DeviceStatusListenerTest' test
```

Expected: FAIL，且失败原因是释放接口/方法或新增约束尚未实现。

### Task 2: 实现释放接口与事务清理

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/controller/ProductionRecordController.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/IProductionRecordService.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`

- [x] **Step 1:** 新增释放状态和重复分配错误码。
- [x] **Step 2:** 增加 Controller 路由与取消操作日志。
- [x] **Step 3:** Service 使用 `FOR UPDATE` 读取记录并校验仅待打印可操作。
- [x] **Step 4:** 使用 `LambdaUpdateWrapper.set(..., null)` 清空 record、PRINT process 和 product 的全部配置字段，并将 `production_record.content_update_time` 更新为当前时间以使 Excel 缓存失效。
- [x] **Step 5:** 让 `assignDevice` 锁定记录并拒绝未释放的重复分配。
- [x] **Step 6:** 运行 Task 1 测试直至通过，再重构重复的锁定读取逻辑。

### Task 3: 封闭设备状态事件竞争窗口

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DeviceStatusListener.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java`

- [x] **Step 1:** 将待打印到打印中的更新改为带原状态和设备ID条件的原子更新。
- [x] **Step 2:** 仅在更新影响一行时继续更新 PRINT 工序、产品状态和 Flow。
- [x] **Step 3:** 运行监听器及释放相关测试。

### Task 4: 增加设备占用联合索引

**Files:**
- Create: `sql/migration-production-record-release-device-2026-08-10.sql`
- Modify: `sql/ddl.sql`
- Modify: `sql/ddl-prod.sql`

- [x] **Step 1:** 编写幂等迁移，为 `production_record(print_device_id, status, is_deleted)` 增加 `idx_print_device_status`。
- [x] **Step 2:** 同步开发和生产目标 DDL。
- [x] **Step 3:** 静态核对迁移与目标 DDL 的索引定义一致。

### Task 5: 审查、修复和验证

**Files:**
- Review: 本分支相对 `dev` 的全部变更。

- [x] **Step 1:** 运行针对性 Controller、Service、Listener 测试。
- [x] **Step 2:** 运行生产模块全量测试。
- [x] **Step 3:** 运行 Reactor 编译并执行 `git diff --check`。
- [x] **Step 4:** 按 SOLID、安全、竞态、事务、空值更新和数据库索引进行代码审查。
- [x] **Step 5:** 修复所有 P0/P1/P2 发现并重新执行完整验证。
- [ ] **Step 6:** 提交功能分支，合并到 `dev`，在合并结果上重新运行生产模块全量测试。

## Baseline Issues

| 问题 | 处理 |
|---|---|
| 直接运行生产模块测试时引用了本机 Maven 缓存中的旧版 design 依赖，缺少 `DesignDrawingEntity.getProductCategory()` | 先安装当前 Reactor 依赖后再运行生产模块测试 |
| Reactor install 读取 `D:/08_Maven_Repo/com/yigongbao/yigongbao-module-order/maven-metadata-local.xml` 时发现 NUL 损坏 | 备份损坏元数据，让 Maven 重新生成后重试 |
| 重试后 design 模块 metadata 也发现 NUL，证明损坏并非单文件 | 枚举 `com/yigongbao` 命名空间并备份全部首字节为 NUL 的 metadata（order/design/imaging）后统一重建 |
