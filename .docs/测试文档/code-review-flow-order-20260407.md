# 代码审查报告：yigongbao-module-flow & yigongbao-module-order

**审查日期**：2026-04-07  
**审查人**：Claude Code (静态代码审查)  
**审查范围**：`yigongbao-module-flow` 和 `yigongbao-module-order` 两个模块的全量静态代码  
**审查方式**：本地代码静态审查（不涉及 git 历史）

---

## 总体评估

**结论**：REQUEST_CHANGES

**已审查文件**：共 ~40 个 Java 文件

| 优先级 | 数量 |
|--------|------|
| P0 - 严重 | 2 |
| P1 - 高   | 5 |
| P2 - 中   | 7 |
| P3 - 低   | 6 |

---

## P0 - 严重（必须修复）

### P0-1 `FlowDebugController` 无访问控制，可在生产环境随意执行状态机动作

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/FlowDebugController.java`  
**位置**：全类，特别是 `/api/order/debug/execute`（L74）和 `/api/order/debug/reset`（L113）

**问题描述**：  
`FlowDebugController` 对外暴露了三个危险接口：
- `POST /api/order/debug/execute`：允许对任意订单执行任意流转动作（真实落库）
- `POST /api/order/debug/reset`：允许将任意订单重置到任意 `phase + status` 组合（无合法性校验）
- `GET /api/order/debug/preview`：预览时实际调用了 `executeFlow`，**会真实修改历史记录**（L59）

当前代码仅有注释说明"禁止暴露到外网"，但无任何代码层面的访问控制（无 `@RequirePermission`、无环境隔离、无 SaToken 角色校验）。若该 Controller 部署到生产环境，任何登录用户均可绕过正常业务流程随意操纵订单状态。

**另外**：`preview` 接口名义上是"不落库"，但实际调用了 `flowFacade.executeFlow()`，该方法会写入 `order_flow_status_history`，与文档描述不符（属于 P1-1 的延伸问题）。

**建议修复**：
```java
// 方案一：通过 Spring Profile 隔离，仅在 dev/test 环境激活
@Profile({"dev", "test"})
@RestController
// ...

// 方案二：添加权限注解，仅超级管理员可访问
@RequirePermission("system:debug:execute")
```

---

### P0-2 `FlowStatusHistoryServiceImpl.recordTransition` 中 `operator` 未做 null 判断，存在 NPE 风险

**文件**：`yigongbao-module-flow/src/main/java/com/yigongbao/flow/service/impl/FlowStatusHistoryServiceImpl.java:56`

**问题描述**：  
```java
log.info("记录订单状态变更，orderId={}, ..., operatorId={}",
        orderId, orderCode, phase, fromStatus, toStatus, action, operator.getOperatorId());
```
方法参数 `operator` 虽在 `FlowFacadeImpl.executeFlow`（L55）有 null 防御逻辑，但 `FlowStateMachineServiceImpl` 也会直接调用 `flowStatusHistoryService.recordTransition`（L122, L167, L181），传入的 `operator` 来自外部调用方，并非一定经过 Facade 层。若 `operator` 为 null，第 56 行的 `operator.getOperatorId()` 将抛出 NPE，且此异常在 catch 块中会被重新 throw，触发事务回滚，但日志仅打印 Exception 类型而非完整错误信息（可能模糊排查）。

接口文档（`FlowStatusHistoryService`）未声明 `operator` 为 `@NonNull`，存在调用方歧义。

**建议修复**：
```java
// 在 recordTransition 方法入口加防御
if (operator == null) {
    operator = new FlowOperator();
}
```
或在接口方法上添加 `@NonNull` 注解并更新 Javadoc。

---

## P1 - 高（应在合并前修复）

### P1-1 `FlowDebugController.preview` 接口实际写库，与文档描述严重不符

**文件**：`FlowDebugController.java:59`

**问题描述**：  
接口注释为"预览状态转换结果（不落库）"，但实现中直接调用 `flowFacade.executeFlow()`，该方法在内部会调用 `flowStatusHistoryService.recordTransition()` 写入历史记录表。Preview 接口每次被调用都会产生一条无效历史记录，污染状态轨迹。

**建议修复**：  
Preview 接口应调用 `FlowFacade.getAvailableActions` 或新增一个只计算不落库的 `previewTransition` 方法，不能复用现有的 `executeFlow`。

---

### P1-2 `OrderMainServiceImpl.removeOrder` 删除订单时未校验状态，允许删除非草稿状态订单

**文件**：`yigongbao-module-order/.../service/impl/OrderMainServiceImpl.java:444-467`

**问题描述**：  
方法注释写明"仅草稿状态的订单允许删除"，但实际实现中只校验了订单是否存在，**未校验 `status == DRAFT(10)`**。任何状态的订单（包括正在审核中、设计中的订单）都可以被删除，会造成数据不一致。

```java
// 当前代码（缺少状态校验）
OrderMainEntity entity = getById(id);
if (entity == null) { ... }
removeById(id); // 直接删除，没有状态判断
```

**建议修复**：
```java
if (!Objects.equals(entity.getStatus(), FlowStatusEnum.DRAFT.getValue())) {
    throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_INVALID_FOR_DELETE);
}
```

---

### P1-3 `validateAndFillMaster` 中 `strict/required` 参数语义反转，导致 DRAFT 模式下反而强制校验

**文件**：`OrderDataValidator.java:99-112`

**问题描述**：  
```java
boolean strict = (mode != ValidateMode.DRAFT);  // DRAFT=false, SUBMIT/DIRECT=true
validateOrg(entity, orgId, !strict);             // DRAFT → required=true ??? 
```
`validateOrg` 的第三个参数 `required` 表示"是否必填"。`strict=false`（DRAFT 模式），传入 `!strict = true`，即"必填"。这与设计意图（DRAFT 模式仅校验已填写的字段，不强制必填）**完全相反**。

同样问题出现在 `validateHospital`（L105）、`validateDept`（L110）、`validateDoctor`（L111）。

相比之下，`validateAndFillMasterForOrder` 的调用方式是相同的（L137-144），而 `buildAndValidateMaster`（L218-228）也是同样的问题。

**但**，查看 `validateOrg` 内部实现（L258-274），当 `orgId == null && required == true` 时会 throw，当 `orgId != null` 时直接校验。实际上 DRAFT 模式下 `orgId` 通常有值，因此 "在有值时校验" 与 "required=true" 效果相同，**所以实际功能上可能没有问题**，但语义混乱，维护风险极高。

**建议修复**：统一命名，将参数命名为 `optional`（可选时为 true），避免双重否定造成认知负担：
```java
boolean optional = (mode == ValidateMode.DRAFT);
validateOrg(entity, orgId, optional);
```
并将所有 `validateXxx(entity, id, required)` 中的 `required` 参数语义统一。

---

### P1-4 `FlowContext.validateNoExcessiveLoops` 中限制值判断使用 `>`，导致实际上限为 MAX+1 次

**文件**：`FlowContext.java:84-98`

**问题描述**：  
```java
if (auditRejectCount > MAX_AUDIT_REJECT) {  // MAX_AUDIT_REJECT = 10
```
使用 `>` 而非 `>=`，意味着当 `auditRejectCount = 10` 时**不会**抛出异常，只有到 11 次才触发。但注释和文档均描述为"最大允许 10 次"，实际允许了 11 次。

测试用例 `FlowStateMachineServiceImplTest:373-383` 也印证了这一问题：历史记录中有 10 次 `DATA_AUDIT_REJECT`，再触发一次使 count 变为 11，才期望抛出异常。这意味着测试是按照"当前有缺陷的实现"编写的，而非按照预期业务规则编写的。

**建议修复**：
```java
if (auditRejectCount >= MAX_AUDIT_REJECT) {  // 改为 >=
```
同时修正 `reworkCount` 和 `designRejectCount` 的判断，以及对应测试。

---

### P1-5 `FlowStatusTransitionRules.getTargetStatus` 中 `CANCEL` 动作目标状态错误

**文件**：`FlowStatusTransitionRules.java:234`

**问题描述**：  
```java
case CANCEL -> FlowStatusEnum.DATA_AUDIT_REJECTED.getValue();
```
`CANCEL` 动作（取消订单）的目标状态被设置为 `DATA_AUDIT_REJECTED(13)`，这是"数据审核不通过"的状态码，而非一个真正的"已取消"状态。

`FlowStatusEnum` 枚举中也没有定义 `CANCELLED` 状态，这可能是设计遗漏。将取消订单的目标状态映射为"审核不通过"在语义上是错误的，会导致：
1. 前端无法区分"被驳回"和"已取消"
2. 历史记录展示混乱
3. 已取消的订单可能被用户再次"重新提交"（因为驳回状态允许 RESUBMIT）

**建议修复**：在 `FlowStatusEnum` 中新增 `CANCELLED(14, "已取消")` 状态，并更新 `CANCEL` 动作的目标状态及相关流转规则。

---

## P2 - 中（可在本 PR 修复或创建跟踪任务）

### P2-1 `OrderDraftCleanupTask` 批量更新使用逐条 `updateById`，数据量大时性能差

**文件**：`OrderDraftCleanupTask.java:47-49`

**问题描述**：  
```java
for (OrderDraftEntity draft : expiredDrafts) {
    draft.setStatus(3);
    orderDraftMapper.updateById(draft);  // N 次 UPDATE 语句
}
```
每次执行 N 条 SQL，当过期草稿数量较大时（例如上千条），会产生大量数据库连接和事务，且没有事务管理。

**建议修复**：改为批量 UPDATE：
```java
orderDraftMapper.update(
    new OrderDraftEntity() {{ setStatus(3); }},
    new LambdaUpdateWrapper<OrderDraftEntity>()
        .lt(OrderDraftEntity::getExpiresAt, now)
        .eq(OrderDraftEntity::getIsDeleted, 0)
        .eq(OrderDraftEntity::getStatus, 1)
);
```

---

### P2-2 `OrderMainServiceImpl.createOrder` 步骤编号冲突，注释不一致

**文件**：`OrderMainServiceImpl.java:800-808`

**问题描述**：  
```java
// Step 6：保存订单主表
order.setOrderCode(orderCode);
...
save(order);

// Step 6：保存重建项目列表（全量校验）  ← 同样标注为 Step 6
```
连续两个步骤都被标注为 "Step 6"，后续的步骤编号也相应混乱（实际有 8 步但编号跳跃）。虽然不影响运行，但严重影响可读性和后续维护。

**建议修复**：将第二个 "Step 6" 改为 "Step 7"，后续编号顺延。

---

### P2-3 `OrderExportServiceImpl.formatRebuildProjectList` 中括号逻辑存在 Bug

**文件**：`OrderExportServiceImpl.java:425-431`

**问题描述**：  
```java
if (StrUtil.isNotBlank(item.getCategory())) {
    if (sb.length() > 0) {
        sb.append("(");
    }
    sb.append(item.getCategory());
    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '(') {
        sb.append(")");   // 这里的判断条件永远为 true
    }
}
```
当 `category` 有值且 `sb` 非空时，先 `append("(")`，再 `append(category)`，然后判断 `sb.charAt(sb.length() - 1) != '('`，此时末尾字符是 `category` 的最后一个字符，条件永远为 true，因此 `")"` 永远会被追加。

但更大的问题是：如果 `sb` 为空（即 projectName 和 bodyPartName 都为空），则不会追加 `"("` 直接追加 `category`，然后末尾字符也不是 `'('`，同样会追加 `")"` —— 输出格式会变成 `"category)"` 而非 `"(category)"`。

**建议修复**：重构为更清晰的逻辑：
```java
if (StrUtil.isNotBlank(item.getCategory())) {
    if (sb.length() > 0) sb.append("(");
    sb.append(item.getCategory());
    if (sb.indexOf("(") >= 0) sb.append(")");
}
```

---

### P2-4 `OrderMainServiceImpl` 与 `OrderExportServiceImpl` 存在大量代码重复

**文件**：`OrderMainServiceImpl.java` 和 `OrderExportServiceImpl.java`

**问题描述**：以下逻辑在两个类中几乎完全重复：
- `buildDataScopeCondition` 方法（两处实现内容相同）
- `fillRebuildProjectList` 方法（两处实现相同）
- `toOrderListVO` 方法（两处实现相同）
- `getCurrentUserId` / `getCurrentUserOrgId` 方法（多处重复）
- 列配置读取逻辑（`getColumnConfig` / `getSystemDefaultColumnConfig`）

这违反了 DRY 原则，后续修改需要同时维护多处。

**建议修复**：将共用逻辑抽取到独立的工具类或父类中。

---

### P2-5 `validateAndFillMaster`（草稿版）与 `validateAndFillMasterForOrder`（订单版）逻辑分叉，难以维护

**文件**：`OrderDataValidator.java:99-146`

**问题描述**：  
两个方法名称相似、职责相同（校验并填充主表关联数据），但分别针对 `OrderDraftEntity` 和 `OrderMainEntity` 实现了两套几乎相同的内部逻辑（`validateOrg` vs `fillOrgName`，`validateHospital` vs `fillHospitalName` 等）。任何校验规则的变更都需要在两处同步修改。

**建议**：通过接口或泛型抽象公共校验逻辑，避免逻辑分叉。

---

### P2-6 `FlowPhaseTransitionRules` 实现了 `FlowTransitionRule` 接口但 `FlowStatusTransitionRules` 未实现，接口定位模糊

**文件**：`FlowTransitionRule.java`，`FlowPhaseTransitionRules.java`，`FlowStatusTransitionRules.java`

**问题描述**：  
`FlowTransitionRule` 接口定义了 `canTransition` 和 `getAvailableNextPhases` 两个方法。`FlowPhaseTransitionRules` 实现了该接口，但 `FlowStatusTransitionRules` 未实现。这导致接口的语义不清晰：是否所有规则类都应实现该接口？

此外，`FlowPhaseTransitionRules` 实现了接口的两个方法，但这两个方法在整个代码库中**没有任何地方被调用**（`FlowStateMachineServiceImpl` 直接使用静态方法 `FlowPhaseTransitionRules.decideNextPhaseAndStatus`）。接口的存在价值存疑。

---

### P2-7 `FlowStatusTransitionRules.getTargetStatus` 中 `default -> null` 分支不可达

**文件**：`FlowStatusTransitionRules.java:269`

**问题描述**：  
`switch (action)` 中所有 `FlowActionEnum` 枚举值都已被显式处理，`default -> null` 分支是死代码。在 Java 21 中，对完整枚举的 switch 表达式加 `default` 分支实际上屏蔽了编译器对"新增枚举值未处理"的警告，引入了隐患。

**建议修复**：移除 `default` 分支，让编译器在新增枚举值时强制给出编译错误。

---

## P3 - 低（建议优化）

### P3-1 `FlowContext.buildFromHistory` 使用硬编码字符串匹配动作，与 `FlowActionEnum` 脱钩

**文件**：`FlowContext.java:113-119`

**问题描述**：  
```java
case "DATA_AUDIT_REJECT" -> ctx.incrementAuditReject();
```
使用硬编码字符串，而非 `FlowActionEnum.DATA_AUDIT_REJECT.getCode()`。若枚举 code 被修改，此处不会有编译错误。

同样问题出现在 `FlowStateMachineServiceImpl.applyContextAction`（L229-235），使用枚举比较（正确），但与 `buildFromHistory` 中的字符串比较不一致，逻辑分散。

**建议修复**：统一使用枚举常量引用：`FlowActionEnum.DATA_AUDIT_REJECT.getCode()`。

---

### P3-2 `FlowStatusEnum.belongsTo` 方法假设阶段步长为 10，与 `FlowPhaseEnum` 枚举值体系存在隐式耦合

**文件**：`FlowStatusEnum.java:164-171`

**问题描述**：  
```java
return statusValue >= phaseValue && statusValue < phaseValue + 10;
```
该方法隐含了"阶段值 × 10 = 对应状态值区间起点"的假设（如 ORDER=1 但状态码为 10-19，DESIGN=2 但状态码为 20-29）。而 `FlowPhaseEnum` 的值从 1 开始，状态码从 10 开始，并非直接对应关系——该方法在 `phaseValue=1, statusValue=10` 时计算 `10 >= 1 && 10 < 11`，得到 false（错误）。

实际上该方法的逻辑意图是"statusValue 在 [phaseValue*10, phaseValue*10+10) 区间内"，但实现使用的是 `phaseValue` 而非 `phaseValue * 10`，导致逻辑错误。

需要确认此方法是否有任何调用方，若有调用方则此为 P1 级别 Bug。（当前代码库中未发现调用点，但方法为 public，存在被错误使用的风险。）

---

### P3-3 `OrderDraftServiceImpl.saveDraft` 删除旧明细后批量插入时无事务嵌套保障

**文件**：`OrderDraftServiceImpl.java:238-259`

**问题描述**：  
```java
// 先删除旧明细
orderItemDraftMapper.delete(...);
// 批量插入（逐条）
for (OrderItemDraftEntity itemEntity : itemEntities) {
    orderItemDraftMapper.insert(itemEntity);  // 若中途失败，已删除的数据不会恢复
}
```
虽然 `saveDraft` 已标注 `@Transactional`，但如果插入过程中发生异常（如数据校验失败、数据库约束冲突），整个事务会回滚，这是正确的。然而日志中并未打印出"明细保存失败"的具体信息，排查困难。

**建议**：在插入循环中增加 try-catch 和更具体的日志，帮助定位失败的具体明细。

---

### P3-4 `FlowFacadeImpl.executeFlow` 日志中 `action.getCode()` 在 `action == null` 时会 NPE

**文件**：`FlowFacadeImpl.java:58`

**问题描述**：  
```java
log.info("FlowFacade 执行流程动作，orderId={}, action={}, ...",
        orderId, action.getCode(), operator.getOperatorId());
```
若 `action` 为 null，在日志打印时直接 NPE。虽然 `FlowFacade` 接口参数 `action` 未标注 `@NonNull`，实际调用方均会传入有效值，但防御性不足。

---

### P3-5 `submitDraft` 未校验提交人是否为草稿创建人

**文件**：`OrderDraftServiceImpl.java:325-370`

**问题描述**：  
`removeDraft` 和 `saveDraft` 都校验了当前用户是否为草稿所有人，但 `submitDraft` 方法中未做此校验，任何用户只要知道草稿 ID 就可以提交他人的草稿。

相比之下，`validateDraftOwner` 被 `getDraftDetail` 调用（在 Controller 层），而 `submitDraft` 在 Controller 层直接调用，未触发所有权校验。

**建议修复**：在 `submitDraft` 中增加所有权校验：
```java
if (!currentUserId.equals(entity.getOperatorId())) {
    throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_MINE);
}
```

---

### P3-6 导出接口 `exportOrders` 缺少 `@Valid` 注解，无法触发 DTO 字段校验

**文件**：`OrderController.java:170`

**问题描述**：  
```java
public void exportOrders(@RequestBody OrderExportQueryDTO dto, HttpServletResponse response) {
```
其他接口（如 `listOrders`、`createOrder`）都有 `@Valid`，而 `exportOrders` 缺少。虽然 `OrderExportQueryDTO` 字段均为可选，目前不影响功能，但不一致的风险在后续添加必填字段时容易被忽略。

---

## 文档与代码不一致

| 位置 | 文档描述 | 实际代码 | 严重程度 |
|------|---------|---------|---------|
| `FlowStateMachineService` 接口注释（L36-37）| "DESIGN_REVIEW_PASSED 为不可见状态" | 实现逻辑正确，但接口注释将其定义为业务规范而非实现细节，含义模糊 | 低 |
| `FlowDebugController.preview` 注释（L47-48）| "预览状态转换结果（不落库）" | 实际调用 `executeFlow` 写入历史记录 | **高**（已列为 P1-1） |
| `removeOrder` 方法注释（L437）| "仅草稿状态的订单允许删除" | 未做状态校验 | **高**（已列为 P1-2） |
| `FlowContext.MAX_AUDIT_REJECT` 注释（L42）| "最大允许的审核驳回次数" | 实际允许比该值多 1 次 | 中（已列为 P1-4） |
| `FlowTransitionRule` 接口注释（L12）| "供各阶段模块实现" | 仅 `FlowPhaseTransitionRules` 实现，`FlowStatusTransitionRules` 未实现 | 低（已列为 P2-6） |

---

## 安全性审查小结

| 检查项 | 状态 |
|--------|------|
| SQL 注入 | 使用 MyBatis Plus Lambda 构建，无手拼 SQL（除 `wrapper.apply("1 = 0")` 为静态常量，安全）|
| 命令注入 | 无 |
| XSS | 无直接 HTML 输出；Excel 导出中 `cell.setCellValue` 不触发 XSS |
| 数据权限（横向越权）| 订单查询列表、详情均有 `dataScopeType` 校验；草稿提交人权限存在遗漏（P3-5） |
| 未授权访问 | `FlowDebugController` 无访问控制（P0-1） |
| 敏感信息泄露 | 日志中包含 `operatorId`、`currentUserId` 等，属于业务必要信息，风险可接受 |
| 大文件 / OOM | 导出使用 `SXSSFWorkbook` 流式写入，已规避 |
| 无限循环 | `FlowContext.validateNoExcessiveLoops` 有次数限制，但存在 off-by-one 问题（P1-4） |

---

## 测试覆盖情况

`FlowStateMachineServiceImplTest` 覆盖了核心状态流转场景，测试质量较高。主要缺失：

1. **`CANCEL` 动作的测试**：当前无任何测试用例覆盖 `CANCEL` 动作，而该动作目标状态存在 Bug（P1-5）。
2. **`FlowContext` 边界值测试**：现有测试按照"有缺陷实现"编写（10 次才报错），需与 P1-4 修复同步更新。
3. **`OrderMainServiceImpl` 没有单元测试文件**：核心业务逻辑（`createFromDraft`、`auditPass`、`removeOrder` 等）缺乏测试保障。
4. **`FlowDebugController` 无测试**：虽为调试接口，但存在真实落库行为，缺少测试。

---

## 修复状态

| 问题 | 状态 | 说明 |
|------|------|------|
| P0-1 FlowDebugController 无访问控制 | ✅ 不修复 | 确认为临时调试代码，不部署生产环境 |
| P0-2 recordTransition operator NPE | ✅ 已修复 | 在方法入口补充 null 防御 |
| P1-1 preview 接口注释歧义 | ✅ 已修复 | 修正注释，明确说明历史记录会写入、订单状态不落库 |
| P1-2 removeOrder 缺少状态校验 | ✅ 已修复 | 添加 `status == DRAFT` 前置校验，使用 `ORDER_CANNOT_DELETE` 错误码 |
| P1-3 strict/required 语义反转 | ✅ 已修复 | 全部改为 `required`，消除双重否定（4 处方法均已修正） |
| P1-4 循环次数 off-by-one | ✅ 已修复 | `>` 改为 `>=`；同步修正测试用例注释 |
| P1-5 CANCEL 目标状态错误 | ✅ 已修复 | 新增 `CANCELLED(14)` 枚举值；修正规则映射；ORDER 阶段可用动作中加入 CANCEL |
| P2-1 批量更新性能 | ✅ 已修复 | `OrderDraftCleanupTask` 改为批量 UPDATE，消除 N+1 |
| P2-2 createOrder Step 注释重复 | ✅ 已修复 | Step 编号修正为连续序列 6/7/8/9 |
| P2-7 getTargetStatus default 死代码 | ✅ 已修复 | 移除 `default -> null` 分支 |
| P3-1 buildFromHistory 硬编码字符串 | ✅ 已修复 | 改为 `FlowActionEnum.XXX.getCode()` 枚举引用 |
| P3-2 FlowStatusEnum.belongsTo 逻辑错误 | ✅ 已修复 | 修正为 `phase.getValue() * 10` |
| P3-4 FlowFacadeImpl action null 日志 NPE | ✅ 已修复 | 在日志前添加 action null 校验 |
| P3-5 submitDraft 缺少所有权校验 | ✅ 已修复 | 添加与 removeDraft 一致的所有权校验 |
| P3-6 exportOrders 缺少 @Valid | ✅ 已修复 | 补充 `@Valid` 注解 |
| P2-3 formatRebuildProjectList 括号 bug | ✅ 已修复 | 括号拼接逻辑重写：sb 有内容时追加 `(category)`，否则仅追加 `category` |
| P2-4 代码重复 | ✅ 已修复 | 新增 `OrderQueryHelper` 组件，两个 Service 共用：`getCurrentUserId`、`buildDataScopeCondition`、`toOrderListVO`、`fillRebuildProjectList`、`getColumnConfig` |
| P2-5 两套主表校验逻辑 | ✅ 已修复 | 提取 `lookupOrg`、`lookupHospital`、`lookupDept`、`applyDoctorInfo` 共享方法，两条公共路径统一调用 |
| P2-6 FlowTransitionRule 接口定位模糊 | ✅ 已修复 | 删除 `FlowTransitionRule.java` 接口；移除 `FlowPhaseTransitionRules` 的 `implements` 声明及两个死代码实例方法 |
| P3-3 草稿明细日志不足 | ✅ 已修复 | 插入循环改为带 index/projectId/bodyPartId 的 try-catch 日志 |

---



1. **立即修复（P0）**：`FlowDebugController` 安全控制、`recordTransition` NPE 防御
2. **本轮修复（P1）**：Preview 接口语义错误、删除订单状态校验、CANCEL 目标状态错误、循环次数 off-by-one、`strict` 参数语义反转
3. **创建跟踪任务（P2）**：批量更新性能、代码重复、接口定位模糊
4. **条件优化（P3）**：参数防御、草稿提交权限、注释一致性
