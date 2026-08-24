# 订单修改与审核权限矩阵 v2 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使订单直接修改、超时申请、设计管理员审核和过期清理严格符合新的角色×阶段×修改范围矩阵，并保证订单主表、项目、文件、申请状态和修改日志的一致性。

**Architecture:** 以 `modifyOrderFullV2 → modifyOrderFull` 与 `submitApply → auditApply → modifyOrderFull` 为内部服务分层，废弃旧的 `/full` Controller 入口，避免业务员绕过时间窗口；权限决策集中为明确的角色/阶段策略。申请保留完整内容和差异历史，审核使用原子状态抢占，过期申请统一返回过期异常且禁止审核，所有订单数据写入成功后才落修改日志和最终审核信息。暂不引入订单版本快照或版本冲突校验。超时申请仍由前端确认后调用 `/apply`，`full-v2` 返回值保持 `1/-1` 兼容契约。

**Tech Stack:** Spring Boot 3, MyBatis-Plus 3.5.8, MySQL 8, Sa-Token, JUnit 5, Mockito。

---

## 一、最终业务规则

| 角色 | 订单阶段 | 设计阶段 | 生产及后续阶段 | 修改范围 |
|---|---|---|---|---|
| 系统管理员/公司管理员 | 直接修改 | 直接修改，无需审批 | 直接修改 | 全部字段 |
| 业务角色 | 创建后配置窗口内直接修改；超时可申请 | 可提交申请，不可直接修改 | 不允许修改/提交申请 | 申请全部字段 |
| 设计师 | 不允许直接修改/申请 | 可提交申请，不可直接修改 | 不允许修改/提交申请 | 申请全部字段 |
| 设计管理员 | 作为审批人处理申请 | 作为审批人处理申请 | 作为审批人处理申请 | 不建议主动修改，禁止自提自审 |
| 其他角色 | 不允许 | 不允许 | 不允许 | 无 |

说明：系统管理员/公司管理员是唯一可绕过阶段和时间窗口直接修改的角色；设计管理员可以审批，但不能审批自己提交的申请。申请过期后保留修改内容和差异，用于查询申请历史；过期申请不可审核。

## 二、文件职责

- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java`
  - 统一直接修改入口、申请入口、审核入口的角色/阶段判定。
  - 保持审核原子抢占，统一处理定时任务已标记和实时判断的过期申请。
  - 过期申请保留内容，只检查状态并返回明确过期异常；检查申请和状态更新结果。
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyFullServiceImpl.java`
  - 保持 `dev` 当前按全部对象计算差异的行为；申请通过后按权限矩阵允许全部字段执行。
  - 校验主表、项目、文件写入影响行数。
  - 在数据写入成功后记录修改日志，并安全递增版本号。
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/OrderModifyFullService.java`
  - 增加审核申请上下文（`applyId`）的传递方式，使审核产生的修改日志可关联到申请。
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java`
  - 移除旧的 `/full` 入口，只保留带时间窗口决策的 `full-v2`。
  - 明确 `full-v2` 返回契约并补充 OpenAPI 说明。
  - 申请提交接口返回申请 ID 和过期时间，保证前端可正确提示。
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/task/OrderModifyApplyCleanTask.java`
  - 仅将过期的待审核记录更新为 `EXPIRED`，保留 `modification_content`、`modification_diff`。
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/modify/OrderModifyFullDTO.java`
  - 明确全量列表字段语义：传空列表表示清空，未传字段表示不修改；必要时用包装请求对象区分两种状态。
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImplBoundaryTest.java`
  - 增加角色/阶段、过期、审核并发和自提自审测试。
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyFullServiceImplBoundaryTest.java`
  - 增加管理员直改、业务员窗口、设计师拒绝直改、完整字段应用和写入失败测试。
- Test: 新增 `OrderModifyApplyCleanTaskTest.java`（如现有测试结构允许）
  - 验证过期记录状态更新且内容和差异保持不变。

## 三、接口和状态流转设计

### Task 1: 固化权限决策并写失败测试

**Files:**
- Test: `OrderModifyApplyServiceImplBoundaryTest.java`
- Test: `OrderModifyFullServiceImplBoundaryTest.java`

- [x] **Step 1: 写角色/阶段矩阵测试**
  - 管理员在订单、设计、生产阶段均可直接修改。
  - 业务员仅订单阶段窗口内直接修改；超时返回需申请；设计阶段不可直改但可申请；生产及后续拒绝。
  - 设计师订单阶段拒绝；设计阶段不可直改但可申请；生产及后续拒绝。
  - 设计管理员不可直接修改且不可审核自己的申请。
  - 其他角色所有修改入口拒绝。
- [x] **Step 2: 运行测试确认按当前 dev 代码产生预期失败**
  - Run: `mvn -pl yigongbao-module-order -am -Dtest=OrderModifyApplyServiceImplBoundaryTest,OrderModifyFullServiceImplBoundaryTest test`
- [x] **Step 3: 提取统一角色/阶段判定方法**
  - 明确区分 `canDirectModify`、`canSubmitApply`、`canAudit`。
  - 不让 `skipPermissionCheck` 绕过审核时的订单状态和过期安全校验。
- [x] **Step 4: 运行上述测试确认通过**
- [ ] **Step 5: 仅提交相关测试与权限逻辑文件**

### Task 2: 修正 `modifyOrderFullV2` 和申请提交契约

**Files:**
- Modify: `OrderModifyApplyServiceImpl.java`
- Modify: `OrderModifyApplyController.java`
- Test: `OrderModifyApplyServiceImplBoundaryTest.java`, `OrderModifyApplyControllerTest.java`

- [x] **Step 1: 写窗口内直改、超时返回、申请提交测试**
- [x] **Step 2: 运行测试确认失败点**
- [x] **Step 3: 实现规则**
  - 管理员直接调用全量修改服务，不受时间窗口限制。
  - 业务员订单阶段窗口内直接修改，窗口外返回明确的“需要申请”结果。
  - 设计师在设计阶段的 `full-v2` 不执行修改，只返回需要申请。
  - 生产及后续阶段业务员/设计师直接拒绝，不返回可申请状态。
  - `/apply` 负责最终创建申请，校验角色、阶段、数据权限、取消申请冲突和重复申请。
- [x] **Step 4: 运行测试确认通过**
- [x] **Step 5: 更新 OpenAPI 描述，明确 `1/-1` 与错误码含义**

### Task 3: 修正申请内容语义和过期清理

**Files:**
- Modify: `OrderModifyApplyServiceImpl.java`
- Modify: `OrderModifyApplyCleanTask.java`
- Modify: `OrderModifyFullDTO.java`（仅在必要时）
- Test: clean task and service tests

- [x] **Step 1: 写申请内容和过期清理失败测试**
  - 空影像列表能够表达“清空影像”，未提供字段表达“不修改影像”。
  - 过期后状态为 `EXPIRED`，内容字段和差异字段保持不变。
- [x] **Step 2: 运行测试确认失败**
- [x] **Step 3: 实现申请字段语义和保留内容的清理逻辑**
- [x] **Step 4: 验证 JSON 反序列化前后字段语义一致**
- [x] **Step 5: 运行测试确认通过**

### Task 4: 修正审核执行的原子性和过期处理

**Files:**
- Modify: `OrderModifyApplyServiceImpl.java`
- Modify: `OrderModifyFullServiceImpl.java`
- Test: `OrderModifyApplyServiceImplBoundaryTest.java`, `OrderModifyFullServiceImplBoundaryTest.java`

- [x] **Step 1: 写审核通过成功、过期、并发和执行失败测试**
- [x] **Step 2: 运行测试确认失败**
- [x] **Step 3: 实现审核流程**
  - 重新读取申请和订单。
  - 如果申请状态为 `EXPIRED`，或仍为待审核但已超过 `expireTime`，返回统一的申请过期异常，不允许审核。
  - 校验申请仍为待审核、未过期、申请人与审核人不同。
  - 原子抢占审核操作，防止重复审核。
  - 执行主表、项目、文件更新并校验影响行数。
  - 所有数据更新成功后写修改日志、更新订单版本和申请审核信息。
  - 任一异常整体回滚，不允许出现“申请已通过但订单未更新”。
- [x] **Step 4: 运行测试确认通过**
- [x] **Step 5: 增加日志，记录申请 ID、订单版本、实际影响行数和失败原因**

### Task 5: 全量验证与文档同步

**Files:**
- Modify: `docs/superpowers/plans/2026-06-08-order-modification-apply.md` 或新增需求说明
- Test: controller/service tests

- [ ] **Step 1: 运行模块完整测试**
  - Run: `mvn -pl yigongbao-module-order -am test`
- [x] **Step 2: 运行编译和静态检查**
  - Run: `mvn -pl yigongbao-module-order -am -DskipTests compile`
- [x] **Step 3: 对照权限矩阵逐项检查接口**
- [x] **Step 4: 更新方案文档和接口说明**
- [x] **Step 5: 检查 `git diff`，只保留本功能相关文件**

## 四、错误处理和数据一致性要求

  - 申请不存在、订单不存在、申请已处理、申请已过期、角色无权限、阶段不允许都返回明确业务错误码。
- 申请过期清理只更新 `PENDING` 记录，避免覆盖已通过或已驳回结果；不得清理申请内容和差异。
- 审核状态抢占与后续修改必须位于同一事务，失败时回滚。
- 主表和子表的增删改必须检查影响行数；对于全量替换，删除/新增数量应与预期一致。
- 修改日志必须在实际数据更新成功后写入，并携带 `applyId`，便于从日志反查申请。
- 不在日志或异常中输出完整患者信息、手机号和大段申请 JSON。

## 五、自审清单

### 需求覆盖

- [x] 角色矩阵已覆盖系统管理员、公司管理员、业务角色、设计师、设计管理员和其他角色。
- [x] 阶段矩阵已覆盖订单、设计、生产及后续阶段。
- [x] 直接修改、提交申请、审核和拒绝的路径均有任务。
- [x] 10 分钟直改窗口与 10 分钟申请暂存期限分开处理。
- [x] 过期申请保留内容，支持历史查看；审核接口统一返回过期异常。

### 当前 dev 差异已纳入

- [x] 已考虑 `6c0ae018` 的并发审核抢占逻辑，不会重复设计一套状态抢占机制。
- [x] 已保留当前前端 `full-v2` 返回 `1/-1` 的兼容路线，并在计划中要求补充契约说明。
- [x] 已确认 `dev` 当前已取消设计师 `onlyItems` 限制，计划不重复改造该部分。
- [x] 已按最新确认保留过期内容，不再将其作为问题；仍纳入更新影响行数校验。
- [x] 已废弃旧 `/full` 接口，避免业务员绕过 10 分钟时间窗口。

### 风险与未决事项

- [ ] 需要确认“设计管理员是否完全禁止直接修改”，本计划按图片矩阵中的“不建议修改”落实为后端禁止，以避免自提自审。
- [ ] 需要确认“未传字段”和“空列表”的全量 DTO 语义；本计划采用未传=不修改、空列表=清空。
- [ ] 需要确认管理员在生产及后续阶段直接修改是否需要额外审计或二次确认；本计划按图片矩阵允许直接修改，但保留完整日志。

### 实施验证结果

- [x] 订单修改相关定向测试通过：22 个测试全部通过。
- [x] 订单模块编译通过，`git diff --check` 无差异格式错误。
- [x] 已修复 `full-v2` 数据权限校验、项目删除影响行数校验、申请状态更新失败检查和敏感申请内容日志输出。
- [ ] 全仓/订单模块完整测试未全部通过：存在与本功能无关的既有环境/测试问题，包括 `RegistrationCertControllerTest` 错误码断言不一致、订单模块独立运行时缺少 `FlowStatusColorResolver` 类，以及部分 Mockito inline mock 初始化失败。

## 六、自审结论

本计划覆盖了当前已发现的功能偏差和数据一致性风险，并明确了每项行为的测试入口。已根据最新确认移除“过期清空内容”和“订单版本快照”两项范围；实施前仍需确认设计管理员是否禁止直接修改、空列表语义和管理员跨阶段修改审计要求。计划没有包含无关模块重构，优先复用现有 Service、Mapper 和事务边界。
