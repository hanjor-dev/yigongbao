# 订单修改流转逻辑修正 + 乐观锁防并发审核 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修正订单修改后的状态流转逻辑（区分 ORDER/DESIGN 阶段），并通过乐观锁防止审核员基于过期数据做出错误审核决定。

**Architecture:**
- 流转逻辑修正：在 `OrderModifyApplyServiceImpl` 的 `directModify` 和 `executeModification` 中，根据 phase + status 决定执行哪个 FlowAction（ORDER 阶段 DATA_AUDIT_REJECTED → RESUBMIT；DESIGN 阶段 DESIGN_REVIEW_REJECTED → CONTINUE_DESIGN + SUBMIT_DESIGN）。
- 乐观锁：`order_main.version` 字段已存在，在 `FlowFacade.executeFlow` 中增加 `expectedVersion` 参数，执行审核类动作（DATA_AUDIT_PASS/REJECT、DESIGN_REVIEW_PASS/REJECT）前校验版本号，不匹配则拒绝；订单修改时递增 version。

**Tech Stack:** Java 21, Spring Boot 3.x, MyBatis Plus 3.5.8, JUnit 5 + Mockito

---

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `yigongbao-module-order/.../OrderModifyApplyServiceImpl.java` | 修改 | 修正 directModify / executeModification 的流转逻辑 |
| `yigongbao-module-flow/.../FlowFacade.java` | 修改 | 新增带 expectedVersion 的 executeFlow 重载 |
| `yigongbao-module-flow/.../FlowFacadeImpl.java` | 修改 | 实现新重载，版本校验 + 版本递增 |
| `yigongbao-module-flow/.../FlowOrderService.java` | 修改 | 新增 updateVersionIncrement 方法 |
| `yigongbao-module-flow/.../FlowOrderServiceImpl.java` | 修改 | 实现版本递增更新 |
| `yigongbao-common/.../ErrorCodeEnum.java` | 修改 | 新增 ORDER_VERSION_CONFLICT 错误码 |
| `yigongbao-module-order/.../OrderModifyDirectServiceTest.java` | 修改 | 补充流转逻辑单元测试 |

---

## Task 1: 修正 directModify / executeModification 的流转逻辑

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java`
- Modify: `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java`

**背景：**
当前两个方法末尾的流转逻辑统一使用 RESUBMIT，导致 DESIGN 阶段报"状态转换不合法"。
正确逻辑：
- ORDER 阶段 + DATA_AUDIT_REJECTED (1040) → RESUBMIT → PENDING_DATA_AUDIT (1010)
- ORDER 阶段 + PENDING_DATA_AUDIT (1010) → 无需流转（已在待审核）
- DESIGN 阶段 + DESIGN_REVIEW_REJECTED (2070) → CONTINUE_DESIGN → DESIGN_IN_PROGRESS (2020)，再 SUBMIT_DESIGN → DESIGN_REVIEWING (2040)
- DESIGN 阶段其他状态（PENDING_DESIGN/DESIGN_IN_PROGRESS/DESIGN_COMPLETED/DESIGN_REVIEWING）→ 无需流转

- [ ] **Step 1: 提取流转逻辑为私有方法**

在 `OrderModifyApplyServiceImpl` 中提取一个私有方法，替换两处重复的流转代码块：

```java
/**
 * 订单修改完成后，根据当前阶段和状态决定是否触发流转
 * ORDER 阶段：DATA_AUDIT_REJECTED → RESUBMIT
 * DESIGN 阶段：DESIGN_REVIEW_REJECTED → CONTINUE_DESIGN + SUBMIT_DESIGN
 */
private void triggerPostModifyFlow(Long orderId, Integer phase, Integer status,
        Long modifierId, String modifierName) {
    if (FlowPhaseEnum.ORDER.getValue().equals(phase)) {
        if (FlowStatusEnum.DATA_AUDIT_REJECTED.getValue().equals(status)) {
            log.info("订单修改后触发 RESUBMIT，orderId={}", orderId);
            flowFacade.executeFlow(orderId, FlowActionEnum.RESUBMIT,
                    new FlowOperator(modifierId, modifierName, "修改后重新提交审核"));
        } else if (FlowStatusEnum.PENDING_DATA_AUDIT.getValue().equals(status)) {
            log.info("订单已处于待审核状态，无需流转，orderId={}", orderId);
        } else {
            log.warn("ORDER 阶段当前状态不触发自动流转，orderId={}, status={}", orderId, status);
        }
    } else if (FlowPhaseEnum.DESIGN.getValue().equals(phase)) {
        if (FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue().equals(status)) {
            log.info("设计审核不通过后修改，触发 CONTINUE_DESIGN + SUBMIT_DESIGN，orderId={}", orderId);
            flowFacade.executeFlow(orderId, FlowActionEnum.CONTINUE_DESIGN,
                    new FlowOperator(modifierId, modifierName, "修改后继续设计"));
            flowFacade.executeFlow(orderId, FlowActionEnum.SUBMIT_DESIGN,
                    new FlowOperator(modifierId, modifierName, "修改后重新提交设计审核"));
        } else {
            log.info("DESIGN 阶段当前状态无需自动流转，orderId={}, status={}", orderId, status);
        }
    }
}
```

- [ ] **Step 2: 替换 directModify 末尾的流转代码块**

将 `directModify` 方法末尾（步骤11）的 if/else if/else 流转代码块替换为：

```java
// 11. 执行修改后流转
triggerPostModifyFlow(orderId, order.getPhase(), order.getStatus(), modifierId, modifierName);
```

注意：`order.getStatus()` 此时读取的是修改前的状态（`order` 对象在步骤1查询后未更新 status 字段），这是正确的——流转判断基于修改前的状态。

- [ ] **Step 3: 替换 executeModification 末尾的流转代码块**

将 `executeModification` 方法末尾（步骤10）的 if/else if/else 流转代码块替换为：

```java
// 10. 执行修改后流转
triggerPostModifyFlow(orderId, order.getPhase(), order.getStatus(), modifierId, modifierName);
```

- [ ] **Step 4: 补充单元测试**

在 `OrderModifyDirectServiceTest` 中新增测试方法，验证 `triggerPostModifyFlow` 的各分支（通过反射或将方法改为 package-private）：

```java
// 需要 mock flowFacade，通过 @Mock 注入
@Mock private FlowFacade flowFacade;

@Test
void triggerPostModifyFlow_OrderPhase_DataAuditRejected_CallsResubmit() {
    // 通过反射调用 triggerPostModifyFlow
    // 验证 flowFacade.executeFlow 被调用一次，action=RESUBMIT
    verify(flowFacade, times(1)).executeFlow(
        eq(1L), eq(FlowActionEnum.RESUBMIT), any(FlowOperator.class));
}

@Test
void triggerPostModifyFlow_OrderPhase_PendingDataAudit_NoFlow() {
    // 验证 flowFacade.executeFlow 未被调用
    verify(flowFacade, never()).executeFlow(any(), any(), any());
}

@Test
void triggerPostModifyFlow_DesignPhase_DesignReviewRejected_CallsTwoActions() {
    // 验证 flowFacade.executeFlow 被调用两次：CONTINUE_DESIGN + SUBMIT_DESIGN
    verify(flowFacade, times(1)).executeFlow(
        eq(1L), eq(FlowActionEnum.CONTINUE_DESIGN), any(FlowOperator.class));
    verify(flowFacade, times(1)).executeFlow(
        eq(1L), eq(FlowActionEnum.SUBMIT_DESIGN), any(FlowOperator.class));
}

@Test
void triggerPostModifyFlow_DesignPhase_DesignInProgress_NoFlow() {
    // 验证 flowFacade.executeFlow 未被调用
    verify(flowFacade, never()).executeFlow(any(), any(), any());
}
```

- [ ] **Step 5: 运行测试**

```bash
cd yigongbao-parent
mvn test -Dtest=OrderModifyDirectServiceTest -pl yigongbao-module-order
```

Expected: 所有测试 PASS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java
git add yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java
git commit -m "fix: 修正订单修改后的状态流转逻辑，区分 ORDER/DESIGN 阶段"
```

---

## Task 2: 新增 ORDER_VERSION_CONFLICT 错误码

**Files:**
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

- [ ] **Step 1: 在 ErrorCodeEnum 中新增错误码**

在现有 order 相关错误码末尾（ORDER_MODIFY_INCOMPLETE(732) 之后）追加：

```java
ORDER_VERSION_CONFLICT(733, "订单数据已被修改，请刷新页面后重新操作"),
```

- [ ] **Step 2: Commit**

```bash
git add yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java
git commit -m "feat: 新增 ORDER_VERSION_CONFLICT 错误码（乐观锁冲突）"
```

---

## Task 3: FlowOrderService 新增版本递增更新方法

**Files:**
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/service/FlowOrderService.java`
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/service/impl/FlowOrderServiceImpl.java`

**说明：** 订单修改时需要递增 version，使审核员持有的旧版本号失效。

- [ ] **Step 1: 在 FlowOrderService 接口新增方法**

```java
/**
 * 递增订单版本号（乐观锁）
 * 订单数据被修改时调用，使持有旧版本号的审核操作失效
 *
 * @param id 订单ID
 */
void incrementVersion(Long id);
```

- [ ] **Step 2: 在 FlowOrderServiceImpl 实现该方法**

```java
@Override
public void incrementVersion(Long id) {
    log.info("递增订单版本号，orderId={}", id);
    LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
    wrapper.eq(OrderMainEntity::getId, id)
           .setSql("version = version + 1");
    flowOrderMapper.update(null, wrapper);
}
```

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-flow/src/main/java/com/yigongbao/flow/service/FlowOrderService.java
git add yigongbao-module-flow/src/main/java/com/yigongbao/flow/service/impl/FlowOrderServiceImpl.java
git commit -m "feat: FlowOrderService 新增 incrementVersion 方法"
```

---

## Task 4: FlowFacade 新增带版本校验的 executeFlow 重载

**Files:**
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/facade/FlowFacade.java`
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/facade/impl/FlowFacadeImpl.java`

**说明：**
- 审核类动作（DATA_AUDIT_PASS/REJECT、DESIGN_REVIEW_PASS/REJECT）调用时传入 `expectedVersion`
- FlowFacadeImpl 在执行前重新查询订单，校验 `order.version == expectedVersion`，不匹配则抛 ORDER_VERSION_CONFLICT
- 原有 `executeFlow(orderId, action, operator)` 保持不变，供不需要版本校验的场景使用

- [ ] **Step 1: 在 FlowFacade 接口新增重载方法**

```java
/**
 * 执行流程动作（带乐观锁版本校验）
 * 用于审核类动作，防止基于过期数据的错误审核
 *
 * @param orderId         订单ID
 * @param action          动作枚举
 * @param operator        操作人信息
 * @param expectedVersion 调用方加载订单时的版本号，与当前 DB 版本不一致时抛出 ORDER_VERSION_CONFLICT
 * @return 转换结果
 */
TransitionResult executeFlow(Long orderId, FlowActionEnum action, FlowOperator operator, Integer expectedVersion);
```

- [ ] **Step 2: 在 FlowFacadeImpl 实现新重载**

定义审核类动作集合，并在执行前校验版本：

```java
private static final Set<FlowActionEnum> AUDIT_ACTIONS = Set.of(
    FlowActionEnum.DATA_AUDIT_PASS,
    FlowActionEnum.DATA_AUDIT_REJECT,
    FlowActionEnum.DESIGN_REVIEW_PASS,
    FlowActionEnum.DESIGN_REVIEW_REJECT
);

@Override
public TransitionResult executeFlow(Long orderId, FlowActionEnum action,
        FlowOperator operator, Integer expectedVersion) {
    if (operator == null) {
        operator = new FlowOperator();
    }
    // 版本校验：仅对审核类动作生效
    if (expectedVersion != null && AUDIT_ACTIONS.contains(action)) {
        OrderMainEntity order = flowOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!expectedVersion.equals(order.getVersion())) {
            log.warn("订单版本冲突，orderId={}, expectedVersion={}, actualVersion={}",
                    orderId, expectedVersion, order.getVersion());
            throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
        }
    }
    log.info("FlowFacade 执行流程动作（带版本校验），orderId={}, action=, expectedVersion={}",
            orderId, action.getCode(), expectedVersion);
    return flowStateMachineService.executeTransition(orderId, action, operator);
}
```

需要在 `FlowFacadeImpl` 中注入 `FlowOrderService`：

```java
private final FlowStateMachineService flowStateMachineService;
private final FlowOrderService flowOrderService;  // 新增注入
```

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-flow/src/main/java/com/yigongbao/flow/facade/FlowFacade.java
git add yigongbao-module-flow/src/main/java/com/yigongbao/flow/facade/impl/FlowFacadeImpl.java
git commit -m "feat: FlowFacade 新增带乐观锁版本校验的 executeFlow 重载"
```

---

## Task 5: 订单修改时递增 version

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java`

**说明：** 在 `directModify` 和 `executeModification` 中，修改完成后（`orderMainMapper.updateById(order)` 之后）调用 `flowFacade` 的版本递增。但 `OrderModifyApplyServiceImpl` 没有直接访问 `FlowOrderService`，应通过已有的 `orderMainMapper` 直接执行 SQL 递增，或在 `triggerPostModifyFlow` 之前调用。

最简方案：在 `OrderModifyApplyServiceImpl` 中注入 `FlowOrderService` 并调用 `incrementVersion`。

但 `FlowOrderService` 在 flow 模块，order 模块已依赖 flow 模块，可以直接注入。

- [ ] **Step 1: 在 OrderModifyApplyServiceImpl 注入 FlowOrderService**

在类的依赖注入字段中新增：

```java
private final com.yigongbao.flow.service.FlowOrderService flowOrderService;
```

- [ ] **Step 2: 在 directModify 中，infoModified 回写后递增版本**

在 `directModify` 的步骤10（`if (infoModified) { orderMainMapper.updateById(order); }`）之后，无论是否有 info 修改，都递增版本：

```java
// 递增版本号，使持有旧版本的审核操作失效
flowOrderService.incrementVersion(orderId);
```

- [ ] **Step 3: 在 executeModification 中同样递增版本**

在 `executeModification` 的 `if (infoModified) { orderMainMapper.updateById(order); }` 之后：

```java
// 递增版本号，使持有旧版本的审核操作失效
flowOrderService.incrementVersion(orderId);
```

- [ ] **Step 4: 运行编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-order -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java
git commit -m "feat: 订单修改完成后递增 version，使旧版本审核操作失效"
```

---

## Task 6: 审核接口接收并传递 expectedVersion

**说明：** 审核员加载订单详情时，前端记录 `version` 字段；提交审核时将 `version` 传给后端。需要修改审核相关的 DTO 和 Service 调用。

涉及的审核接口：
1. 数据审核（DATA_AUDIT_PASS / DATA_AUDIT_REJECT）— 在 `yigongbao-module-order` 的订单审核 Service 中
2. 设计审核（DESIGN_REVIEW_PASS / DESIGN_REVIEW_REJECT）— 在 `yigongbao-module-design` 的设计审核 Service 中

先定位这两处审核调用的具体文件。

- [ ] **Step 1: 定位数据审核调用位置**

搜索 `FlowActionEnum.DATA_AUDIT_PASS` 的调用位置：

```bash
cd yigongbao-parent
grep -r "DATA_AUDIT_PASS\|DATA_AUDIT_REJECT" --include="*.java" -l
```

- [ ] **Step 2: 定位设计审核调用位置**

```bash
grep -r "DESIGN_REVIEW_PASS\|DESIGN_REVIEW_REJECT" --include="*.java" -l
```

- [ ] **Step 3: 在数据审核 DTO 中新增 version 字段**

找到数据审核的 DTO（如 `AuditOrderDTO` 或类似），新增：

```java
/**
 * 订单版本号（乐观锁）
 * 前端加载订单详情时记录，提交审核时传入，防止基于过期数据的错误审核
 */
private Integer version;
```

- [ ] **Step 4: 在设计审核 DTO 中新增 version 字段**

同上，找到设计审核 DTO，新增 `version` 字段。

- [ ] **Step 5: 修改数据审核 Service，使用带版本校验的 executeFlow**

将原来的：
```java
flowFacade.executeFlow(orderId, FlowActionEnum.DATA_AUDIT_PASS, operator);
```
改为：
```java
flowFacade.executeFlow(orderId, FlowActionEnum.DATA_AUDIT_PASS, operator, dto.getVersion());
```
DATA_AUDIT_REJECT 同理。

- [ ] **Step 6: 修改设计审核 Service，使用带版本校验的 executeFlow**

DESIGN_REVIEW_PASS 和 DESIGN_REVIEW_REJECT 同理。

- [ ] **Step 7: 运行编译验证**

```bash
cd yigongbao-parent
mvn compile -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: 审核接口传入 version 字段，启用乐观锁版本校验"
```

---

## Task 7: 全量测试验证

- [ ] **Step 1: 运行 order 模块全量测试**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-order
```

Expected: 所有测试 PASS，无 FAIL

- [ ] **Step 2: 运行 flow 模块全量测试**

```bash
mvn test -pl yigongbao-module-flow
```

Expected: 所有测试 PASS

- [ ] **Step 3: 运行全量构建**

```bash
mvn clean package -DskipTests -q
```

Expected: BUILD SUCCESS
