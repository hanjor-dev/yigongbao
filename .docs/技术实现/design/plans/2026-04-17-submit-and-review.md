# 提交设计与设计审核 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现设计模块的最后两个功能：Task 07（提交设计：continueDesign + submitDesign）和 Task 08（设计审核：列表/详情/通过/驳回）。

**Architecture:** Task 07 在现有 `DesignWorkorderService/Controller` 上扩展两个方法；Task 08 新建 `DesignReviewController` 复用工单查询逻辑，在现有 `DesignReviewService` 上增加业务方法，审核历史追加写入 `design_review` 表。状态流转统一通过 `FlowFacade.executeFlow()` 驱动。`DESIGN_REVIEW_PASS` 只需调用一次，flow 模块内部根据 `needsPhysicalDelivery` 自动完成到 3010/7010 的跳转。

**Tech Stack:** Spring Boot, MyBatis-Plus, SaToken (StpUtil), FlowFacade, Hutool, JUnit 5 + Mockito

---

## 文件改动清单

| 操作 | 文件路径 | 说明 |
|------|----------|------|
| **Modify** | `yigongbao-module-flow/.../enums/FlowActionEnum.java` | 新增 `CONTINUE_DESIGN` 动作 |
| Modify | `module/design/vo/SubmitCheckVO.java` | 新增 `hasRevisedDocs` 字段 |
| Modify | `module/design/service/DesignWorkorderService.java` | 新增 `continueDesign`、`submitDesign` 方法声明 |
| Modify | `module/design/service/impl/DesignWorkorderServiceImpl.java` | 实现两个新方法，`buildSubmitCheck` 增加修订版校验 |
| Modify | `module/design/controller/DesignWorkorderController.java` | 新增两个端点 |
| Create | `module/design/vo/DesignReviewHistoryVO.java` | 审核历史单条记录 VO |
| Create | `module/design/vo/DesignReviewDetailVO.java` | 审核详情 VO（含历史列表） |
| Create | `module/design/dto/ReviewPassDTO.java` | 审核通过请求体 |
| Create | `module/design/dto/ReviewRejectDTO.java` | 审核驳回请求体 |
| Modify | `module/design/service/DesignReviewService.java` | 新增业务方法声明 |
| Modify | `module/design/service/impl/DesignReviewServiceImpl.java` | 实现审核列表/详情/通过/驳回 |
| Create | `module/design/controller/DesignReviewController.java` | 审核 Controller（4个端点） |
| Modify | `test/.../DesignWorkorderServiceImplTest.java` | 新增 continueDesign/submitDesign 测试 |
| Create | `test/.../DesignReviewServiceImplTest.java` | 审核服务单元测试 |

**模块根路径**（以下简写，完整路径前缀）：
- design 模块 main: `D:\01_Project\02_Personal\医工宝\yigongbao-parent\yigongbao-module-design\src\main\java\com\yigongbao\module\design\`
- design 模块 test: `D:\01_Project\02_Personal\医工宝\yigongbao-parent\yigongbao-module-design\src\test\java\com\yigongbao\module\design\`
- flow 模块 main: `D:\01_Project\02_Personal\医工宝\yigongbao-parent\yigongbao-module-flow\src\main\java\com\yigongbao\flow\`

---

## Task 0：FlowActionEnum 新增 CONTINUE_DESIGN 动作

**Files:**
- Modify: `D:\01_Project\02_Personal\医工宝\yigongbao-parent\yigongbao-module-flow\src\main\java\com\yigongbao\flow\enums\FlowActionEnum.java`

- [ ] **Step 1：在设计阶段动作区块末尾追加 CONTINUE_DESIGN**

在 `DESIGN_REVIEW_REJECT` 枚举值之后、打印阶段注释之前插入：

```java
/**
 * 驳回后继续修改
 */
CONTINUE_DESIGN("CONTINUE_DESIGN", "继续修改"),
```

- [ ] **Step 2：验证 flow 模块编译通过**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn compile -pl yigongbao-module-flow -am -DskipTests -q
```
预期：BUILD SUCCESS

- [ ] **Step 3：同时验证 flow 模块现有流转规则包含 CONTINUE_DESIGN 的转换**

检查 `FlowStatusTransitionRules.java` 是否已有 `2060 → 2020` 的规则。如果没有，需要在该文件中添加：`DESIGN_REVIEW_REJECTED` 可通过 `CONTINUE_DESIGN` 动作转换到 `DESIGN_IN_PROGRESS`。

> **注意**：查看 `FlowStatusTransitionRules.java` 确认转换规则是否存在。若不存在，找到设计阶段规则区块，按现有格式追加：
> ```java
> // 设计审核不通过 → 继续修改 → 设计中
> .put(buildKey(FlowStatusEnum.DESIGN_REVIEW_REJECTED, FlowActionEnum.CONTINUE_DESIGN),
>      FlowStatusEnum.DESIGN_IN_PROGRESS)
> ```

- [ ] **Step 4：Commit**

```bash
git add yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowActionEnum.java
git add yigongbao-module-flow/src/main/java/com/yigongbao/flow/rules/FlowStatusTransitionRules.java
git commit -m "feat(flow): FlowActionEnum 新增 CONTINUE_DESIGN 动作"
```

---

## Task 1：扩展 SubmitCheckVO，增加修订版校验字段

**Files:**
- Modify: `vo/SubmitCheckVO.java`

- [ ] **Step 1：在 SubmitCheckVO 新增 `hasRevisedDocs` 字段**

在现有6个 Boolean 字段后追加：

```java
/**
 * 是否每个数据包的指令单和图纸都已上传修订版（模式A下必填）
 * 模式B（在线编辑）下该字段始终为 true
 */
private Boolean hasRevisedDocs;
```

同时更新 `canSubmit` 的 Javadoc，说明模式A下需要全部7项为 true。

- [ ] **Step 2：构建验证**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn compile -pl yigongbao-module-design -am -DskipTests -q
```
预期：BUILD SUCCESS

- [ ] **Step 3：Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/SubmitCheckVO.java
git commit -m "feat(design): SubmitCheckVO 增加 hasRevisedDocs 字段"
```

---

## Task 2：实现 continueDesign（驳回后继续修改）

**Files:**
- Modify: `service/DesignWorkorderService.java`
- Modify: `service/impl/DesignWorkorderServiceImpl.java`
- Modify: `controller/DesignWorkorderController.java`

- [ ] **Step 1：在 DesignWorkorderService 接口声明方法**

在 `startDesign` 方法后追加：

```java
/**
 * 驳回后继续修改
 * 状态流转：设计审核不通过(2060) → 设计中(2020)
 *
 * @param orderId 订单ID
 */
void continueDesign(Long orderId);
```

- [ ] **Step 2：在 DesignWorkorderServiceImpl 实现 continueDesign**

在 `startDesign` 方法后追加：

```java
/**
 * 驳回后继续修改
 * 校验：订单状态必须为 DESIGN_REVIEW_REJECTED(2060)，且当前用户是分配设计师
 *
 * @param orderId 订单ID
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void continueDesign(Long orderId) {
    log.info("设计师继续修改，orderId={}", orderId);

    // 1. 校验订单存在
    OrderMainEntity order = orderMainService.getById(orderId);
    if (order == null) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }

    // 2. 校验订单状态（必须是设计审核不通过）
    if (!FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue().equals(order.getStatus())) {
        log.warn("订单状态不允许继续修改，orderId={}, status={}", orderId, order.getStatus());
        throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
    }

    // 3. 校验当前登录用户是该订单的分配设计师
    Long currentUserId = StpUtil.getLoginIdAsLong();
    if (!currentUserId.equals(order.getDesignerId())) {
        log.warn("非分配设计师，无权继续修改，orderId={}, designerId={}, currentUserId={}",
                orderId, order.getDesignerId(), currentUserId);
        throw new BusinessException(ErrorCodeEnum.ORDER_DESIGNER_MISMATCH);
    }

    // 4. 查询当前用户姓名
    UserEntity currentUser = userService.getById(currentUserId);
    String currentUserName = currentUser != null ? currentUser.getRealName() : null;

    // 5. 执行状态流转：DESIGN_REVIEW_REJECTED → DESIGN_IN_PROGRESS
    TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.CONTINUE_DESIGN,
            FlowOperator.of(currentUserId, currentUserName));

    // 6. 回写订单表
    OrderMainEntity update = new OrderMainEntity();
    update.setId(orderId);
    update.setPhase(result.getTargetPhase());
    update.setStatus(result.getFinalStatus());
    update.setCurrentHandlerId(currentUserId);
    update.setCurrentHandlerName(currentUserName);
    orderMainService.updateById(update);

    log.info("继续修改成功，orderId={}, phase={}, status={}",
            orderId, result.getTargetPhase(), result.getFinalStatus());
}
```

- [ ] **Step 3：在 DesignWorkorderController 新增端点**

在 `startDesign` 端点后追加：

```java
/**
 * 驳回后继续修改
 * POST /design/workorder/{orderId}/continue-design
 */
@PostMapping("/{orderId}/continue-design")
@Operation(summary = "驳回后继续修改")
public Result<Void> continueDesign(@PathVariable Long orderId) {
    designWorkorderService.continueDesign(orderId);
    return Result.success();
}
```

- [ ] **Step 4：写单元测试**

在 `DesignWorkorderServiceImplTest.java` 末尾追加 `ContinueDesign` 嵌套类：

```java
@Nested
class ContinueDesign {

    @Test
    void success() {
        // Arrange
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue());
        order.setDesignerId(100L);
        when(orderMainService.getById(1L)).thenReturn(order);

        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setRealName("设计师A");
        when(userService.getById(100L)).thenReturn(user);

        TransitionResult mockResult = TransitionResult.of(20, FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.CONTINUE_DESIGN), any()))
                .thenReturn(mockResult);
        when(orderMainService.updateById(any())).thenReturn(true);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            // Act & Assert
            assertDoesNotThrow(() -> designWorkorderService.continueDesign(1L));
            verify(flowFacade).executeFlow(eq(1L), eq(FlowActionEnum.CONTINUE_DESIGN), any());
            verify(orderMainService).updateById(any());
        }
    }

    @Test
    void orderNotFound() {
        when(orderMainService.getById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> designWorkorderService.continueDesign(999L));
    }

    @Test
    void wrongStatus() {
        // 状态不是2060，校验在 StpUtil 之前，无需 mock StpUtil
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        when(orderMainService.getById(1L)).thenReturn(order);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            assertThrows(BusinessException.class, () -> designWorkorderService.continueDesign(1L));
        }
    }

    @Test
    void notAssignedDesigner() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue());
        order.setDesignerId(200L); // 不是当前用户
        when(orderMainService.getById(1L)).thenReturn(order);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            assertThrows(BusinessException.class, () -> designWorkorderService.continueDesign(1L));
        }
    }
}
```

- [ ] **Step 5：运行 continueDesign 测试，确认全部通过**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn test -pl yigongbao-module-design -Dtest="DesignWorkorderServiceImplTest" -DfailIfNoTests=false
```
预期：ContinueDesign 的4个测试通过

- [ ] **Step 6：Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignWorkorderService.java
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignWorkorderController.java
git add yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImplTest.java
git commit -m "feat(design): 实现 continueDesign 驳回后继续修改"
```

---

## Task 3：实现 submitDesign（提交设计审核）

**Files:**
- Modify: `service/DesignWorkorderService.java`
- Modify: `service/impl/DesignWorkorderServiceImpl.java`
- Modify: `controller/DesignWorkorderController.java`

- [ ] **Step 1：在 DesignWorkorderService 接口声明方法**

```java
/**
 * 提交设计审核
 * 状态流转：设计中(2020) → 设计审核中(2040)
 * 提交前执行完整校验（7项），模式A下额外校验修订版文件
 *
 * @param orderId 订单ID
 */
void submitDesign(Long orderId);
```

- [ ] **Step 2：修改 buildSubmitCheck，增加 designMode 参数和第7项修订版校验**

将现有私有方法签名从 `buildSubmitCheck(Long orderId)` 改为 `buildSubmitCheck(Long orderId, Integer designMode)`。

同时更新 `getWorkorderDetail` 中的调用处：
```java
vo.setSubmitCheck(buildSubmitCheck(orderId, order.getDesignMode()));
```

在 `buildSubmitCheck` 方法中，现有6项校验之后、`canSubmit` 计算逻辑之前，插入第7项：

```java
// 7. 修订版文件：模式A（线下修改）下每个数据包的指令单和图纸都必须有 revised_file_id
// designMode=null 视为模式A（保守处理）
boolean isOfflineMode = !DesignModeEnum.ONLINE.getValue().equals(designMode);
if (isOfflineMode && !packages.isEmpty()) {
    // 查询各包的最新指令单版本（按 version_seq 倒序，取每包第一条）
    List<DesignInstructionEntity> allInstructions = designInstructionMapper.selectList(
            new LambdaQueryWrapper<DesignInstructionEntity>()
                    .in(DesignInstructionEntity::getPackageId, packageIds)
                    .eq(DesignInstructionEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                    .orderByDesc(DesignInstructionEntity::getVersionSeq));
    Map<Long, DesignInstructionEntity> latestInstructionByPkg = allInstructions.stream()
            .collect(Collectors.toMap(
                    DesignInstructionEntity::getPackageId,
                    i -> i,
                    (existing, newer) -> existing)); // 已倒序，保留第一条（最新版本）

    // 查询各包的最新图纸版本
    List<DesignDrawingEntity> allDrawings = designDrawingMapper.selectList(
            new LambdaQueryWrapper<DesignDrawingEntity>()
                    .in(DesignDrawingEntity::getPackageId, packageIds)
                    .eq(DesignDrawingEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                    .orderByDesc(DesignDrawingEntity::getVersionSeq));
    Map<Long, DesignDrawingEntity> latestDrawingByPkg = allDrawings.stream()
            .collect(Collectors.toMap(
                    DesignDrawingEntity::getPackageId,
                    d -> d,
                    (existing, newer) -> existing));

    boolean allInstructionRevised = packageIds.stream().allMatch(pkgId -> {
        DesignInstructionEntity inst = latestInstructionByPkg.get(pkgId);
        return inst != null && StrUtil.isNotBlank(inst.getRevisedFileId());
    });
    boolean allDrawingRevised = packageIds.stream().allMatch(pkgId -> {
        DesignDrawingEntity drawing = latestDrawingByPkg.get(pkgId);
        return drawing != null && StrUtil.isNotBlank(drawing.getRevisedFileId());
    });

    check.setHasRevisedDocs(allInstructionRevised && allDrawingRevised);
} else {
    // 模式B 或无数据包，跳过修订版校验
    check.setHasRevisedDocs(true);
}
```

在 `canSubmit` 计算部分，`hasReport` 检查之后追加：

```java
} else if (!Boolean.TRUE.equals(check.getHasRevisedDocs())) {
    check.setCanSubmit(false);
    check.setBlockReason("请上传修订版指令单和图纸");
} else {
    check.setCanSubmit(true);
    check.setBlockReason(null);
}
```

- [ ] **Step 3：在 DesignWorkorderServiceImpl 实现 submitDesign**

在 `continueDesign` 方法后追加：

```java
/**
 * 提交设计审核
 * 校验：订单状态必须为 DESIGN_IN_PROGRESS(2020)，且当前用户是分配设计师
 * 提交前执行完整的 7 项校验（模式A下包括修订版文件校验）
 *
 * @param orderId 订单ID
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void submitDesign(Long orderId) {
    log.info("设计师提交设计审核，orderId={}", orderId);

    // 1. 校验订单存在
    OrderMainEntity order = orderMainService.getById(orderId);
    if (order == null) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }

    // 2. 校验订单状态（必须是设计中）
    if (!FlowStatusEnum.DESIGN_IN_PROGRESS.getValue().equals(order.getStatus())) {
        log.warn("订单状态不允许提交设计，orderId={}, status={}", orderId, order.getStatus());
        throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
    }

    // 3. 校验当前登录用户是该订单的分配设计师
    Long currentUserId = StpUtil.getLoginIdAsLong();
    if (!currentUserId.equals(order.getDesignerId())) {
        log.warn("非分配设计师，无权提交设计，orderId={}, designerId={}, currentUserId={}",
                orderId, order.getDesignerId(), currentUserId);
        throw new BusinessException(ErrorCodeEnum.ORDER_DESIGNER_MISMATCH);
    }

    // 4. 执行提交前完整校验（含修订版文件检查）
    SubmitCheckVO check = buildSubmitCheck(orderId, order.getDesignMode());
    if (!Boolean.TRUE.equals(check.getCanSubmit())) {
        log.warn("提交设计校验未通过，orderId={}, blockReason={}", orderId, check.getBlockReason());
        throw new BusinessException(400, check.getBlockReason());
    }

    // 5. 查询当前用户姓名
    UserEntity currentUser = userService.getById(currentUserId);
    String currentUserName = currentUser != null ? currentUser.getRealName() : null;

    // 6. 执行状态流转：DESIGN_IN_PROGRESS → DESIGN_REVIEWING(2040)
    TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.SUBMIT_DESIGN,
            FlowOperator.of(currentUserId, currentUserName));

    // 7. 回写订单表（含设计提交时间）
    OrderMainEntity update = new OrderMainEntity();
    update.setId(orderId);
    update.setPhase(result.getTargetPhase());
    update.setStatus(result.getFinalStatus());
    update.setDesignSubmitTime(LocalDateTime.now());
    update.setCurrentHandlerId(currentUserId);
    update.setCurrentHandlerName(currentUserName);
    orderMainService.updateById(update);

    log.info("提交设计审核成功，orderId={}, phase={}, status={}",
            orderId, result.getTargetPhase(), result.getFinalStatus());
}
```

- [ ] **Step 4：在 DesignWorkorderController 新增端点**

```java
/**
 * 提交设计审核
 * POST /design/workorder/{orderId}/submit-design
 */
@PostMapping("/{orderId}/submit-design")
@Operation(summary = "提交设计审核")
public Result<Void> submitDesign(@PathVariable Long orderId) {
    designWorkorderService.submitDesign(orderId);
    return Result.success();
}
```

- [ ] **Step 5：写单元测试**

在 `DesignWorkorderServiceImplTest.java` 追加 `SubmitDesign` 嵌套类：

```java
@Nested
class SubmitDesign {

    @Test
    void success() {
        // Arrange
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(100L);
        order.setDesignMode(DesignModeEnum.OFFLINE.getValue()); // 模式A

        when(orderMainService.getById(1L)).thenReturn(order);

        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setRealName("设计师A");
        when(userService.getById(100L)).thenReturn(user);

        // Mock 数据包（1个）
        DesignPackageEntity pkg = new DesignPackageEntity();
        pkg.setId(10L);
        pkg.setOrderId(1L);
        when(designPackageMapper.selectList(any())).thenReturn(List.of(pkg));

        // Mock 打印信息（存在）
        DesignProductEntity prod = new DesignProductEntity();
        prod.setPackageId(10L);
        when(designProductMapper.selectList(any())).thenReturn(List.of(prod));

        // Mock 指令单（有修订版）
        DesignInstructionEntity inst = new DesignInstructionEntity();
        inst.setPackageId(10L);
        inst.setVersionSeq(1);
        inst.setRevisedFileId("revised-inst-1");
        when(designInstructionMapper.selectList(any())).thenReturn(List.of(inst));

        // Mock 图纸（有修订版）
        DesignDrawingEntity drawing = new DesignDrawingEntity();
        drawing.setPackageId(10L);
        drawing.setVersionSeq(1);
        drawing.setRevisedFileId("revised-drawing-1");
        when(designDrawingMapper.selectList(any())).thenReturn(List.of(drawing));

        // Mock 模型和报告存在
        when(designModelMapper.selectCount(any())).thenReturn(1L);
        when(fileService.listByBiz(any(), any())).thenReturn(List.of(new Object()));

        TransitionResult mockResult = TransitionResult.of(20, FlowStatusEnum.DESIGN_REVIEWING.getValue());
        when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.SUBMIT_DESIGN), any()))
                .thenReturn(mockResult);
        when(orderMainService.updateById(any())).thenReturn(true);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

            // Act & Assert
            assertDoesNotThrow(() -> designWorkorderService.submitDesign(1L));
            verify(flowFacade).executeFlow(eq(1L), eq(FlowActionEnum.SUBMIT_DESIGN), any());
            verify(orderMainService).updateById(argThat(u -> u.getDesignSubmitTime() != null));
        }
    }

    @Test
    void orderNotFound() {
        when(orderMainService.getById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> designWorkorderService.submitDesign(999L));
    }

    @Test
    void wrongStatus() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.PENDING_DESIGN.getValue()); // 不是2020
        when(orderMainService.getById(1L)).thenReturn(order);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            assertThrows(BusinessException.class, () -> designWorkorderService.submitDesign(1L));
        }
    }

    @Test
    void notAssignedDesigner() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(200L); // 不是当前用户
        when(orderMainService.getById(1L)).thenReturn(order);

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            assertThrows(BusinessException.class, () -> designWorkorderService.submitDesign(1L));
        }
    }

    @Test
    void submitCheckFailed_noPackage() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(100L);
        order.setDesignMode(DesignModeEnum.OFFLINE.getValue());
        when(orderMainService.getById(1L)).thenReturn(order);
        when(userService.getById(100L)).thenReturn(new UserEntity());
        when(designPackageMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(designModelMapper.selectCount(any())).thenReturn(0L);
        when(fileService.listByBiz(any(), any())).thenReturn(Collections.emptyList());

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> designWorkorderService.submitDesign(1L));
            assertTrue(ex.getMessage().contains("数据包"));
        }
    }

    @Test
    void submitCheckFailed_missingRevisedDocs_offlineMode() {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(100L);
        order.setDesignMode(DesignModeEnum.OFFLINE.getValue()); // 模式A，需要修订版

        when(orderMainService.getById(1L)).thenReturn(order);
        when(userService.getById(100L)).thenReturn(new UserEntity());

        DesignPackageEntity pkg = new DesignPackageEntity();
        pkg.setId(10L);
        pkg.setOrderId(1L);
        when(designPackageMapper.selectList(any())).thenReturn(List.of(pkg));

        DesignProductEntity prod = new DesignProductEntity();
        prod.setPackageId(10L);
        when(designProductMapper.selectList(any())).thenReturn(List.of(prod));

        // 指令单无修订版
        DesignInstructionEntity inst = new DesignInstructionEntity();
        inst.setPackageId(10L);
        inst.setVersionSeq(1);
        inst.setRevisedFileId(null);
        when(designInstructionMapper.selectList(any())).thenReturn(List.of(inst));

        // 图纸无修订版
        DesignDrawingEntity drawing = new DesignDrawingEntity();
        drawing.setPackageId(10L);
        drawing.setVersionSeq(1);
        drawing.setRevisedFileId(null);
        when(designDrawingMapper.selectList(any())).thenReturn(List.of(drawing));

        when(designModelMapper.selectCount(any())).thenReturn(1L);
        when(fileService.listByBiz(any(), any())).thenReturn(List.of(new Object()));

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> designWorkorderService.submitDesign(1L));
            assertTrue(ex.getMessage().contains("修订版"));
        }
    }
}
```

- [ ] **Step 6：运行全部 DesignWorkorderServiceImplTest 测试**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn test -pl yigongbao-module-design -Dtest="DesignWorkorderServiceImplTest" -DfailIfNoTests=false
```
预期：所有测试通过（含 Task 2 的 ContinueDesign）

- [ ] **Step 7：Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/
git add yigongbao-module-design/src/test/java/com/yigongbao/module/design/
git commit -m "feat(design): 实现 submitDesign 提交设计审核（含7项校验）"
```

---

## Task 4：创建审核相关 VO 和 DTO

**Files:**
- Create: `vo/DesignReviewHistoryVO.java`
- Create: `vo/DesignReviewDetailVO.java`
- Create: `dto/ReviewPassDTO.java`
- Create: `dto/ReviewRejectDTO.java`

- [ ] **Step 1：创建 DesignReviewHistoryVO**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计审核历史记录 VO
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
public class DesignReviewHistoryVO {

    private Long id;

    /** 审核人姓名 */
    private String reviewerName;

    /** 审核结果：0=驳回，1=通过 */
    private Integer reviewResult;

    /** 审核结果名称 */
    private String reviewResultName;

    /** 审批意见（通过时） */
    private String comment;

    /** 驳回原因（驳回时） */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewTime;
}
```

- [ ] **Step 2：创建 DesignReviewDetailVO**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 设计审核详情 VO
 * 在工单详情基础上追加审核历史列表
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DesignReviewDetailVO extends DesignWorkorderDetailVO {

    /**
     * 审核历史记录列表（时间倒序）
     */
    private List<DesignReviewHistoryVO> reviewHistory;
}
```

- [ ] **Step 3：创建 ReviewPassDTO**

```java
package com.yigongbao.module.design.dto;

import lombok.Data;

/**
 * 审核通过请求体
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
public class ReviewPassDTO {

    /**
     * 审批意见（选填）
     */
    private String comment;
}
```

- [ ] **Step 4：创建 ReviewRejectDTO**

```java
package com.yigongbao.module.design.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审核驳回请求体
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
public class ReviewRejectDTO {

    /**
     * 驳回原因（必填）
     */
    @NotBlank(message = "驳回原因不能为空")
    private String rejectReason;
}
```

- [ ] **Step 5：编译验证**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn compile -pl yigongbao-module-design -am -DskipTests -q
```
预期：BUILD SUCCESS

- [ ] **Step 6：Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignReviewHistoryVO.java
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignReviewDetailVO.java
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/ReviewPassDTO.java
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/ReviewRejectDTO.java
git commit -m "feat(design): 新增审核模块 VO 和 DTO"
```

---

## Task 5：实现 DesignReviewService 业务方法

**Files:**
- Modify: `service/DesignReviewService.java`
- Modify: `service/impl/DesignReviewServiceImpl.java`

- [ ] **Step 1：替换 DesignReviewService 接口**

```java
package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.ReviewPassDTO;
import com.yigongbao.module.design.dto.ReviewRejectDTO;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.vo.DesignReviewDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;

/**
 * 设计审核服务接口
 *
 * @author hanjor
 * @date 2026-04-17
 */
public interface DesignReviewService extends IService<DesignReviewEntity> {

    /**
     * 分页查询待审核工单列表
     * 固定 status=2040（设计审核中），复用工单查询逻辑
     *
     * @param queryDTO 查询参数
     * @return 分页工单列表
     */
    IPage<DesignWorkorderListVO> listReviewWorkorders(DesignWorkorderQueryDTO queryDTO);

    /**
     * 获取审核详情（工单详情 + 审核历史）
     *
     * @param orderId 订单ID
     * @return 审核详情 VO
     */
    DesignReviewDetailVO getReviewDetail(Long orderId);

    /**
     * 审核通过
     * 状态流转：设计审核中(2040) → 设计审核通过(2050，不可见) → 待打印(3010) 或 待客户确认(7010)
     * flow 模块根据 needsPhysicalDelivery 自动完成分支跳转，无需业务层二次调用
     *
     * @param orderId 订单ID
     * @param dto     审核通过请求体
     */
    void reviewPass(Long orderId, ReviewPassDTO dto);

    /**
     * 审核驳回
     * 状态流转：设计审核中(2040) → 设计审核不通过(2060)
     *
     * @param orderId 订单ID
     * @param dto     审核驳回请求体（含必填驳回原因）
     */
    void reviewReject(Long orderId, ReviewRejectDTO dto);
}
```

- [ ] **Step 2：替换 DesignReviewServiceImpl 实现**

```java
package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.ReviewPassDTO;
import com.yigongbao.module.design.dto.ReviewRejectDTO;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.service.DesignReviewService;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignReviewDetailVO;
import com.yigongbao.module.design.vo.DesignReviewHistoryVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设计审核服务实现类
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesignReviewServiceImpl extends ServiceImpl<DesignReviewMapper, DesignReviewEntity>
        implements DesignReviewService {

    private final OrderMainService orderMainService;
    private final UserService userService;
    private final DesignWorkorderService designWorkorderService;
    private final FlowFacade flowFacade;

    /**
     * 分页查询待审核工单列表
     * 强制 status=2040，复用 DesignWorkorderService.listWorkorders 查询逻辑
     *
     * @param queryDTO 查询参数
     * @return 分页工单列表
     */
    @Override
    public IPage<DesignWorkorderListVO> listReviewWorkorders(DesignWorkorderQueryDTO queryDTO) {
        log.info("查询待审核工单列表，queryDTO={}", queryDTO);
        // 强制覆盖 status 为 2040（设计审核中），前端传入值无效
        queryDTO.setStatus(FlowStatusEnum.DESIGN_REVIEWING.getValue());
        return designWorkorderService.listWorkorders(queryDTO);
    }

    /**
     * 获取审核详情
     * 在工单详情基础上追加审核历史记录列表（时间倒序）
     *
     * @param orderId 订单ID
     * @return 审核详情 VO
     */
    @Override
    public DesignReviewDetailVO getReviewDetail(Long orderId) {
        log.info("查询审核详情，orderId={}", orderId);

        // 1. 获取工单详情（复用现有逻辑）
        DesignWorkorderDetailVO workorderDetail = designWorkorderService.getWorkorderDetail(orderId);

        // 2. 构建审核详情 VO，复制工单详情字段
        DesignReviewDetailVO detailVO = new DesignReviewDetailVO();
        BeanUtils.copyProperties(workorderDetail, detailVO);

        // 3. 查询审核历史记录（时间倒序，追加写入不覆盖）
        List<DesignReviewEntity> reviews = list(
                new LambdaQueryWrapper<DesignReviewEntity>()
                        .eq(DesignReviewEntity::getOrderId, orderId)
                        .orderByDesc(DesignReviewEntity::getReviewTime));
        detailVO.setReviewHistory(reviews.stream()
                .map(this::toHistoryVO)
                .collect(Collectors.toList()));

        return detailVO;
    }

    /**
     * 审核通过
     * 调用一次 DESIGN_REVIEW_PASS，flow 模块内部根据 needsPhysicalDelivery 自动跳转到
     * 待打印(3010) 或 待客户确认(7010)，TransitionResult 返回最终状态落库
     *
     * @param orderId 订单ID
     * @param dto     审核通过请求体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPass(Long orderId, ReviewPassDTO dto) {
        log.info("审核通过，orderId={}", orderId);

        // 1. 校验订单存在且状态为 2040
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.DESIGN_REVIEWING.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许审核通过，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 2. 获取当前审核人信息
        Long reviewerId = StpUtil.getLoginIdAsLong();
        UserEntity reviewer = userService.getById(reviewerId);
        String reviewerName = reviewer != null ? reviewer.getRealName() : null;

        // 3. 写入审核记录（result=1 通过）
        DesignReviewEntity reviewRecord = new DesignReviewEntity();
        reviewRecord.setOrderId(orderId);
        reviewRecord.setReviewerId(reviewerId);
        reviewRecord.setReviewerName(reviewerName);
        reviewRecord.setReviewResult(1);
        reviewRecord.setComment(dto != null ? dto.getComment() : null);
        reviewRecord.setReviewTime(LocalDateTime.now());
        save(reviewRecord);

        // 4. 执行状态流转：DESIGN_REVIEWING → (2050 不可见) → 3010 或 7010
        // flow 模块内部根据 order.needsPhysicalDelivery 自动完成分支跳转
        TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.DESIGN_REVIEW_PASS,
                FlowOperator.of(reviewerId, reviewerName));

        // 5. 回写订单表（最终状态）
        OrderMainEntity update = new OrderMainEntity();
        update.setId(orderId);
        update.setPhase(result.getTargetPhase());
        update.setStatus(result.getFinalStatus());
        update.setCurrentHandlerId(null);
        update.setCurrentHandlerName(null);
        orderMainService.updateById(update);

        log.info("审核通过成功，orderId={}, finalPhase={}, finalStatus={}",
                orderId, result.getTargetPhase(), result.getFinalStatus());
    }

    /**
     * 审核驳回
     * 状态流转：DESIGN_REVIEWING(2040) → DESIGN_REVIEW_REJECTED(2060)
     * 驳回原因写入 order_main.design_review_remark 供列表/详情展示
     *
     * @param orderId 订单ID
     * @param dto     审核驳回请求体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewReject(Long orderId, ReviewRejectDTO dto) {
        log.info("审核驳回，orderId={}", orderId);

        // 1. 校验订单存在且状态为 2040
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.DESIGN_REVIEWING.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许审核驳回，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 2. 获取当前审核人信息
        Long reviewerId = StpUtil.getLoginIdAsLong();
        UserEntity reviewer = userService.getById(reviewerId);
        String reviewerName = reviewer != null ? reviewer.getRealName() : null;

        // 3. 写入审核记录（result=0 驳回）
        DesignReviewEntity reviewRecord = new DesignReviewEntity();
        reviewRecord.setOrderId(orderId);
        reviewRecord.setReviewerId(reviewerId);
        reviewRecord.setReviewerName(reviewerName);
        reviewRecord.setReviewResult(0);
        reviewRecord.setRejectReason(dto.getRejectReason());
        reviewRecord.setReviewTime(LocalDateTime.now());
        save(reviewRecord);

        // 4. 执行状态流转：DESIGN_REVIEWING → DESIGN_REVIEW_REJECTED(2060)
        FlowOperator operator = FlowOperator.of(reviewerId, reviewerName);
        operator.setRemark(dto.getRejectReason());
        TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.DESIGN_REVIEW_REJECT, operator);

        // 5. 回写订单表（含驳回原因快照，供列表/详情页展示，不清空当前处理人）
        OrderMainEntity update = new OrderMainEntity();
        update.setId(orderId);
        update.setPhase(result.getTargetPhase());
        update.setStatus(result.getFinalStatus());
        update.setDesignReviewRemark(dto.getRejectReason());
        update.setCurrentHandlerId(order.getDesignerId());
        update.setCurrentHandlerName(order.getDesignerName());
        orderMainService.updateById(update);

        log.info("审核驳回成功，orderId={}, phase={}, status={}",
                orderId, result.getTargetPhase(), result.getFinalStatus());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将审核记录实体转换为历史 VO
     * 注意：reviewResult 是 Integer，用 equals 比较避免自动拆箱风险
     */
    private DesignReviewHistoryVO toHistoryVO(DesignReviewEntity entity) {
        DesignReviewHistoryVO vo = new DesignReviewHistoryVO();
        vo.setId(entity.getId());
        vo.setReviewerName(entity.getReviewerName());
        vo.setReviewResult(entity.getReviewResult());
        vo.setReviewResultName(Integer.valueOf(1).equals(entity.getReviewResult()) ? "通过" : "驳回");
        vo.setComment(entity.getComment());
        vo.setRejectReason(entity.getRejectReason());
        vo.setReviewTime(entity.getReviewTime());
        return vo;
    }
}
```

- [ ] **Step 3：编译验证**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn compile -pl yigongbao-module-design -am -DskipTests -q
```
预期：BUILD SUCCESS

- [ ] **Step 4：Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignReviewService.java
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignReviewServiceImpl.java
git commit -m "feat(design): 实现 DesignReviewService 审核通过/驳回业务逻辑"
```

---

## Task 6：创建 DesignReviewController

**Files:**
- Create: `controller/DesignReviewController.java`

- [ ] **Step 1：创建 Controller 文件**

```java
package com.yigongbao.module.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.ReviewPassDTO;
import com.yigongbao.module.design.dto.ReviewRejectDTO;
import com.yigongbao.module.design.service.DesignReviewService;
import com.yigongbao.module.design.vo.DesignReviewDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设计审核 Controller
 *
 * @author hanjor
 * @date 2026-04-17
 */
@RestController
@RequestMapping("/design/review")
@RequiredArgsConstructor
@Tag(name = "设计审核")
public class DesignReviewController {

    private final DesignReviewService designReviewService;

    /**
     * 分页查询待审核工单列表
     * 固定 status=2040，前端无需传 status 参数
     */
    @PostMapping("/list")
    @Operation(summary = "待审核工单列表")
    public Result<IPage<DesignWorkorderListVO>> listReviewWorkorders(
            @RequestBody DesignWorkorderQueryDTO queryDTO) {
        return Result.success(designReviewService.listReviewWorkorders(queryDTO));
    }

    /**
     * 获取审核详情（工单详情 + 审核历史）
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "审核详情")
    public Result<DesignReviewDetailVO> getReviewDetail(@PathVariable Long orderId) {
        return Result.success(designReviewService.getReviewDetail(orderId));
    }

    /**
     * 审核通过
     * 状态流转：2040 → 3010（需实体交付）或 7010（不需实体交付）
     */
    @PostMapping("/{orderId}/pass")
    @Operation(summary = "审核通过")
    public Result<Void> reviewPass(@PathVariable Long orderId,
                                   @RequestBody(required = false) ReviewPassDTO dto) {
        designReviewService.reviewPass(orderId, dto != null ? dto : new ReviewPassDTO());
        return Result.success();
    }

    /**
     * 审核驳回
     * 状态流转：2040 → 2060
     */
    @PostMapping("/{orderId}/reject")
    @Operation(summary = "审核驳回")
    public Result<Void> reviewReject(@PathVariable Long orderId,
                                     @Valid @RequestBody ReviewRejectDTO dto) {
        designReviewService.reviewReject(orderId, dto);
        return Result.success();
    }
}
```

- [ ] **Step 2：编译验证**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn compile -pl yigongbao-module-design -am -DskipTests -q
```
预期：BUILD SUCCESS

- [ ] **Step 3：Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignReviewController.java
git commit -m "feat(design): 新增 DesignReviewController 审核端点"
```

---

## Task 7：DesignReviewService 单元测试

**Files:**
- Create: `test/.../service/impl/DesignReviewServiceImplTest.java`

- [ ] **Step 1：创建测试类**

完整路径：`D:\01_Project\02_Personal\医工宝\yigongbao-parent\yigongbao-module-design\src\test\java\com\yigongbao\module\design\service\impl\DesignReviewServiceImplTest.java`

```java
package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.ReviewPassDTO;
import com.yigongbao.module.design.dto.ReviewRejectDTO;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignReviewDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignReviewServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-17
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignReviewServiceImplTest {

    @Mock private DesignReviewMapper designReviewMapper;
    @Mock private OrderMainService orderMainService;
    @Mock private UserService userService;
    @Mock private DesignWorkorderService designWorkorderService;
    @Mock private FlowFacade flowFacade;

    @InjectMocks
    private DesignReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper（继承 ServiceImpl 时必须）
        Field baseMapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                .getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(reviewService, designReviewMapper);
    }

    // ==================== listReviewWorkorders ====================

    @Nested
    class ListReviewWorkorders {

        @Test
        void forcesStatusTo2040() {
            // Arrange：前端传入错误的 status
            DesignWorkorderQueryDTO queryDTO = new DesignWorkorderQueryDTO();
            queryDTO.setStatus(2020);
            when(designWorkorderService.listWorkorders(any())).thenReturn(new Page<>());

            // Act
            reviewService.listReviewWorkorders(queryDTO);

            // Assert：status 被强制覆盖为 2040
            assertEquals(FlowStatusEnum.DESIGN_REVIEWING.getValue(), queryDTO.getStatus());
            verify(designWorkorderService).listWorkorders(queryDTO);
        }
    }

    // ==================== getReviewDetail ====================

    @Nested
    class GetReviewDetail {

        @Test
        void success_withReviewHistory() {
            // Arrange
            DesignWorkorderDetailVO workorderDetail = new DesignWorkorderDetailVO();
            workorderDetail.setId(1L);
            workorderDetail.setOrderCode("ORD-001");
            when(designWorkorderService.getWorkorderDetail(1L)).thenReturn(workorderDetail);

            DesignReviewEntity review = new DesignReviewEntity();
            review.setId(10L);
            review.setOrderId(1L);
            review.setReviewerName("审核员A");
            review.setReviewResult(0); // 驳回
            review.setRejectReason("图纸不完整");
            review.setReviewTime(LocalDateTime.now());
            when(designReviewMapper.selectList(any())).thenReturn(List.of(review));

            // Act
            DesignReviewDetailVO detail = reviewService.getReviewDetail(1L);

            // Assert
            assertNotNull(detail);
            assertEquals("ORD-001", detail.getOrderCode());
            assertEquals(1, detail.getReviewHistory().size());
            assertEquals("驳回", detail.getReviewHistory().get(0).getReviewResultName());
            assertEquals("图纸不完整", detail.getReviewHistory().get(0).getRejectReason());
        }

        @Test
        void success_passResultName() {
            DesignWorkorderDetailVO workorderDetail = new DesignWorkorderDetailVO();
            workorderDetail.setId(1L);
            when(designWorkorderService.getWorkorderDetail(1L)).thenReturn(workorderDetail);

            DesignReviewEntity review = new DesignReviewEntity();
            review.setId(11L);
            review.setReviewResult(1); // 通过
            review.setReviewTime(LocalDateTime.now());
            when(designReviewMapper.selectList(any())).thenReturn(List.of(review));

            DesignReviewDetailVO detail = reviewService.getReviewDetail(1L);
            assertEquals("通过", detail.getReviewHistory().get(0).getReviewResultName());
        }

        @Test
        void success_emptyHistory() {
            DesignWorkorderDetailVO workorderDetail = new DesignWorkorderDetailVO();
            workorderDetail.setId(1L);
            when(designWorkorderService.getWorkorderDetail(1L)).thenReturn(workorderDetail);
            when(designReviewMapper.selectList(any())).thenReturn(Collections.emptyList());

            DesignReviewDetailVO detail = reviewService.getReviewDetail(1L);
            assertTrue(detail.getReviewHistory().isEmpty());
        }
    }

    // ==================== reviewPass ====================

    @Nested
    class ReviewPass {

        @Test
        void success() {
            // Arrange
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_REVIEWING.getValue(), 1);
            when(orderMainService.getById(1L)).thenReturn(order);
            setupReviewer(100L, "审核员A");
            when(designReviewMapper.insert(any())).thenReturn(1);

            // flow 模块内部自动跳转到 3010（needsPhysicalDelivery=1）
            TransitionResult result = TransitionResult.ofWithPhaseChange(30, 2050, 3010);
            when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.DESIGN_REVIEW_PASS), any()))
                    .thenReturn(result);
            when(orderMainService.updateById(any())).thenReturn(true);

            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                ReviewPassDTO dto = new ReviewPassDTO();
                dto.setComment("设计合格");

                assertDoesNotThrow(() -> reviewService.reviewPass(1L, dto));
                // 验证写入了审核通过记录
                verify(designReviewMapper).insert(argThat(r -> Integer.valueOf(1).equals(r.getReviewResult())));
                // 验证只调用了一次 FlowFacade（flow 模块内部处理分支）
                verify(flowFacade, times(1)).executeFlow(eq(1L), eq(FlowActionEnum.DESIGN_REVIEW_PASS), any());
                // 验证落库状态为最终可见状态（3010）
                verify(orderMainService).updateById(argThat(u -> Integer.valueOf(3010).equals(u.getStatus())));
            }
        }

        @Test
        void success_noPhysicalDelivery() {
            // needsPhysicalDelivery=0，flow 自动跳转到 7010
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_REVIEWING.getValue(), 0);
            when(orderMainService.getById(1L)).thenReturn(order);
            setupReviewer(100L, "审核员A");
            when(designReviewMapper.insert(any())).thenReturn(1);

            TransitionResult result = TransitionResult.ofWithPhaseChange(70, 2050, 7010);
            when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.DESIGN_REVIEW_PASS), any()))
                    .thenReturn(result);
            when(orderMainService.updateById(any())).thenReturn(true);

            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertDoesNotThrow(() -> reviewService.reviewPass(1L, null));
                verify(orderMainService).updateById(argThat(u -> Integer.valueOf(7010).equals(u.getStatus())));
            }
        }

        @Test
        void orderNotFound() {
            when(orderMainService.getById(999L)).thenReturn(null);
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewPass(999L, null));
            }
        }

        @Test
        void wrongStatus() {
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(), 1);
            when(orderMainService.getById(1L)).thenReturn(order);
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewPass(1L, null));
            }
        }
    }

    // ==================== reviewReject ====================

    @Nested
    class ReviewReject {

        @Test
        void success() {
            // Arrange
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_REVIEWING.getValue(), 1);
            order.setDesignerId(200L);
            order.setDesignerName("设计师A");
            when(orderMainService.getById(1L)).thenReturn(order);
            setupReviewer(100L, "审核员A");
            when(designReviewMapper.insert(any())).thenReturn(1);

            TransitionResult result = TransitionResult.of(20, FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue());
            when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.DESIGN_REVIEW_REJECT), any()))
                    .thenReturn(result);
            when(orderMainService.updateById(any())).thenReturn(true);

            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                ReviewRejectDTO dto = new ReviewRejectDTO();
                dto.setRejectReason("图纸格位不清晰");

                assertDoesNotThrow(() -> reviewService.reviewReject(1L, dto));
                // 验证写入了驳回记录，驳回原因正确
                verify(designReviewMapper).insert(argThat(r ->
                        Integer.valueOf(0).equals(r.getReviewResult())
                                && "图纸格位不清晰".equals(r.getRejectReason())));
                // 验证驳回原因写入 order_main.design_review_remark
                verify(orderMainService).updateById(argThat(u ->
                        "图纸格位不清晰".equals(u.getDesignReviewRemark())));
            }
        }

        @Test
        void orderNotFound() {
            when(orderMainService.getById(999L)).thenReturn(null);
            ReviewRejectDTO dto = new ReviewRejectDTO();
            dto.setRejectReason("原因");
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewReject(999L, dto));
            }
        }

        @Test
        void wrongStatus() {
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(), 1);
            when(orderMainService.getById(1L)).thenReturn(order);
            ReviewRejectDTO dto = new ReviewRejectDTO();
            dto.setRejectReason("原因");
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewReject(1L, dto));
            }
        }
    }

    // ==================== 辅助方法 ====================

    private OrderMainEntity buildOrder(int status, int needsPhysicalDelivery) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(status);
        order.setPhase(20);
        order.setNeedsPhysicalDelivery(needsPhysicalDelivery);
        return order;
    }

    private void setupReviewer(Long userId, String name) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRealName(name);
        when(userService.getById(userId)).thenReturn(user);
    }
}
```

- [ ] **Step 2：运行审核服务测试**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn test -pl yigongbao-module-design -Dtest="DesignReviewServiceImplTest" -DfailIfNoTests=false
```
预期：所有测试通过

- [ ] **Step 3：运行全部 design 模块测试**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn test -pl yigongbao-module-design
```
预期：所有测试通过，BUILD SUCCESS

- [ ] **Step 4：Commit**

```bash
git add yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignReviewServiceImplTest.java
git commit -m "test(design): 新增 DesignReviewServiceImpl 单元测试"
```

---

## Task 8：全量构建验证 + 文档更新

- [ ] **Step 1：全量构建（含所有模块）**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn clean package -DskipTests
```
预期：BUILD SUCCESS

- [ ] **Step 2：运行全量测试**

```bash
cd D:\01_Project\02_Personal\医工宝\yigongbao-parent
mvn test
```
预期：所有测试通过

- [ ] **Step 3：更新 00_设计阶段实现方案.md**

文件：`D:\01_Project\02_Personal\医工宝\.docs\技术实现\design\00_设计阶段实现方案.md`

将 Task 07、Task 08 状态从 `⏳ 待开发` 更新为 `✅ 已完成`，补充完成日期 2026-04-17。

- [ ] **Step 4：新增 07_提交设计.md 文档**

文件：`D:\01_Project\02_Personal\医工宝\.docs\技术实现\design\07_提交设计.md`

参考已有 `06_工单查询.md` 格式，内容涵盖：
- 接口：`POST /{orderId}/continue-design`、`POST /{orderId}/submit-design`
- 7项提交前校验逻辑（含 `hasRevisedDocs` 模式A/B 差异）
- 状态流转：`2060→2020`（CONTINUE_DESIGN）、`2020→2040`（SUBMIT_DESIGN）
- 关键实现：`buildSubmitCheck` 参数变更、修订版校验的 Collector 去重逻辑

- [ ] **Step 5：新增 08_设计审核.md 文档**

文件：`D:\01_Project\02_Personal\医工宝\.docs\技术实现\design\08_设计审核.md`

内容涵盖：
- 接口：4个端点（list/detail/pass/reject）
- 审核通过：单次 FlowFacade 调用，flow 内部自动分支，`TransitionResult.getFinalStatus()` 落库
- 审核驳回：`design_review_remark` 快照写入
- 历史记录：追加写入策略，不覆盖
- `DesignReviewDetailVO extends DesignWorkorderDetailVO` 继承设计

- [ ] **Step 6：更新需求分析文档实现状态**

文件：`D:\01_Project\02_Personal\医工宝\.docs\需求分析\v1\业务流程_工单设计v2.md`

在「十一、实现状态」末尾追加两行：
```markdown
| 提交设计（continueDesign/submitDesign） | ✅ 已完成 | `POST /{orderId}/continue-design`、`POST /{orderId}/submit-design` |
| 设计审核（列表/详情/通过/驳回） | ✅ 已完成 | `POST /design/review/list` 等4个端点 |
```

- [ ] **Step 7：最终 Commit 并 Push**

```bash
git add .
git commit -m "feat(design): 完成设计模块全部功能——提交设计 + 设计审核"
git push
```
