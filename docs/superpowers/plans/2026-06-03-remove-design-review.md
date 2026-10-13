# 移除设计审核功能 - 实施计划

**创建日期**: 2026-06-03  
**相关设计文档**: [docs/superpowers/specs/2026-06-03-design-review-configurable.md](../specs/2026-06-03-design-review-configurable.md)  
**检查点标签**: `checkpoint-before-remove-design-review`  
**预估工作量**: 2-3天

---

## ⚠️ 实施前必读

**【强制要求】开始实施前必须完整阅读编码规范文档：**

📖 **编码规范文档路径**: `.docs/技术实现/java-coding-standards.mdc`

### 关键编码规范强调

#### 1. 注释规范（必须严格遵守）

**ServiceImpl 层注释要求**：
- ✅ **方法级注释**：每个公共方法必须添加 Javadoc 注释（功能、参数、返回值、异常）
- ✅ **行级注释**：关键业务逻辑必须添加行内注释说明

```java
/**
 * 完成设计
 * 根据 needsPhysicalDelivery 执行不同的校验
 *
 * @param orderId 订单ID
 * @throws BusinessException 订单不存在或状态错误
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void completeDesign(Long orderId) {
    // 根据ID查询订单实体
    OrderMainEntity order = orderMainService.getById(orderId);
    // 校验订单是否存在
    if (order == null) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }
    // ... 其他逻辑
}
```

#### 2. 日志规范（必须严格遵守）

**【强制】Controller 层禁止输出日志**，日志记录由 ServiceImpl 负责：

```java
// ❌ 错误：Controller 不应记录日志
@PostMapping("/complete-design")
public Result<Void> completeDesign(@PathVariable Long orderId) {
    log.info("完成设计请求，orderId={}", orderId);  // 删除此行
    designWorkorderService.completeDesign(orderId);
    return Result.success();
}

// ✅ 正确：ServiceImpl 记录日志
@Override
public void completeDesign(Long orderId) {
    log.info("完成设计: orderId={}", orderId);
    // 业务逻辑...
    log.info("完成设计成功: orderId={}, status={}", orderId, newStatus);
}
```

#### 3. 禁止魔法值（必须严格遵守）

**状态值常量**：
```java
// ❌ 错误
entity.setStatus(1);
if (order.getNeedsPhysicalDelivery() == 1) { }

// ✅ 正确
entity.setStatus(StatusConstants.NORMAL);
if (order.getNeedsPhysicalDelivery() == StatusConstants.YES) { }
```

**异常处理优先使用 ErrorCodeEnum**：
```java
// ✅ 优先使用
throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);

// ⚠️ 备选（仅当 ErrorCodeEnum 无合适枚举值时）
throw new BusinessException(400, "订单状态不允许完成设计");
```

#### 4. 命名规范

- 方法命名：`completeDesign`、`manualCompleteOrder`（驼峰命名）
- 类命名：`DesignCompletedEvent`、`DesignCompletedListener`（帕斯卡命名）
- 变量命名：`orderId`、`needsPhysical`（驼峰命名）

#### 5. 其他关键规范

- ✅ 使用 `@RequiredArgsConstructor` 注入依赖
- ✅ 统一返回 `Result.success()` / `Result.error()`
- ✅ 事务注解：`@Transactional(rollbackFor = Exception.class)`
- ✅ 优先使用 Hutool 工具类（`StrUtil`、`CollUtil` 等）

---

## 实施策略

**顺序说明**:
1. 先删除无依赖的文件（最安全）
2. 再修改基础枚举（影响最广）
3. 然后修改流转规则和业务逻辑
4. 最后补充测试和验证

**每个步骤的时间控制**: 2-5分钟为宜，涉及复杂逻辑的可适当延长

**提交策略**: 每个 Phase 完成后提交一次，便于问题回滚

---

## 文件结构概览

### 待删除文件 (7个)
```
yigongbao-module-design/src/main/java/com/yigongbao/module/design/
├── controller/DesignReviewController.java              # 删除
├── service/DesignReviewService.java                    # 删除
├── service/impl/DesignReviewServiceImpl.java           # 删除
├── dto/ReviewPassDTO.java                              # 删除
├── dto/ReviewRejectDTO.java                            # 删除
├── vo/DesignReviewDetailVO.java                        # 删除
└── vo/DesignReviewHistoryVO.java                       # 删除

yigongbao-module-production/src/main/java/com/yigongbao/module/production/
└── listener/DesignReviewPassedListener.java            # 删除

yigongbao-module-design/src/test/java/com/yigongbao/module/design/
└── service/impl/DesignReviewServiceImplTest.java       # 删除
```

### 待修改文件 (主要)
```
yigongbao-module-flow/src/main/java/com/yigongbao/flow/
├── enums/FlowStatusEnum.java                           # 删除3个状态
├── enums/FlowActionEnum.java                           # 删除3个动作，新增1个
├── rules/FlowStatusTransitionRules.java                # 修改转换规则
├── rules/FlowPhaseTransitionRules.java                 # 删除review处理
├── context/FlowContext.java                            # 删除reject计数
└── service/impl/FlowStateMachineServiceImpl.java       # 删除reject处理

yigongbao-module-design/src/main/java/com/yigongbao/module/design/
├── controller/DesignWorkorderController.java           # 删除submitDesign，新增completeDesign
├── service/impl/DesignWorkorderServiceImpl.java        # 删除submitDesign/continueDesign，新增completeDesign
├── helper/DesignQueryHelper.java                       # 删除DESIGN_REVIEW_REJECTED
└── dto/DesignWorkorderQueryDTO.java                    # 更新注释

yigongbao-module-order/src/main/java/com/yigongbao/module/order/
├── controller/OrderController.java                     # 新增manualCompleteOrder
├── service/impl/OrderMainServiceImpl.java              # 删除auto-resubmit，新增manualCompleteOrder
└── service/impl/OrderModifyApplyServiceImpl.java       # 删除auto-resubmit

yigongbao-module-production/src/main/java/com/yigongbao/module/production/
├── listener/DesignCompletedListener.java               # 新增
└── record/service/impl/ProductionRecordServiceImpl.java # 修改下载逻辑
```

### 待新增文件 (2个)
```
yigongbao-common/src/main/java/com/yigongbao/common/event/
└── DesignCompletedEvent.java                           # 新增

yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/
└── DesignCompletedListener.java                        # 新增
```

---

## Phase 1: 删除审核相关文件（无依赖项）

**目标**: 删除审核模块的所有独立文件，为后续修改清理障碍  
**预计时间**: 20分钟  
**风险**: 低（这些文件将被完全移除，不影响其他模块）

### 步骤 1.1: 删除 DesignReviewController
```bash
cd yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller
rm DesignReviewController.java
```

### 步骤 1.2: 删除 DesignReviewService 接口
```bash
cd yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service
rm DesignReviewService.java
```

### 步骤 1.3: 删除 DesignReviewServiceImpl
```bash
cd yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl
rm DesignReviewServiceImpl.java
```

### 步骤 1.4: 删除审核 DTO
```bash
cd yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto
rm ReviewPassDTO.java ReviewRejectDTO.java
```

### 步骤 1.5: 删除审核 VO
```bash
cd yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo
rm DesignReviewDetailVO.java DesignReviewHistoryVO.java
```

### 步骤 1.6: 删除监听器
```bash
cd yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener
rm DesignReviewPassedListener.java
```

### 步骤 1.7: 删除测试类
```bash
cd yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl
rm DesignReviewServiceImplTest.java
```

### 步骤 1.8: 提交 Phase 1
```bash
cd yigongbao-parent
git add -A
git commit -m "refactor: Phase 1 - 删除审核相关文件

- 删除 DesignReviewController
- 删除 DesignReviewService 及实现类
- 删除审核 DTO 和 VO
- 删除 DesignReviewPassedListener
- 删除测试类

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 2: 修改 Flow 模块枚举

**目标**: 删除审核相关状态和动作，新增完成设计动作  
**预计时间**: 15分钟  
**风险**: 中（枚举是基础，影响面广）

### 步骤 2.1: 修改 FlowStatusEnum - 删除审核状态

**文件**: `yigongbao-parent/yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowStatusEnum.java`

删除第60-73行的三个审核状态：
```java
// 删除以下内容：
/**
 * 设计审核中
 */
DESIGN_REVIEWING(2040, "设计审核中"),

/**
 * 设计审核通过（等待生产员下载数据包）
 */
DESIGN_REVIEW_PASSED(2050, "设计审核通过"),

/**
 * 设计审核不通过
 */
DESIGN_REVIEW_REJECTED(2060, "设计审核不通过"),
```

### 步骤 2.2: 修改 FlowActionEnum - 删除审核动作，新增完成设计

**文件**: `yigongbao-parent/yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowActionEnum.java`

1. 在 START_DESIGN 之后新增 COMPLETE_DESIGN：
```java
/**
 * 完成设计
 */
COMPLETE_DESIGN("COMPLETE_DESIGN", "完成设计"),
```

2. 删除第64-81行的审核动作：
```java
// 删除以下内容：
/**
 * 提交设计
 */
SUBMIT_DESIGN("SUBMIT_DESIGN", "提交设计"),

/**
 * 设计审核通过
 */
DESIGN_REVIEW_PASS("DESIGN_REVIEW_PASS", "设计审核通过"),

/**
 * 设计审核驳回
 */
DESIGN_REVIEW_REJECT("DESIGN_REVIEW_REJECT", "设计审核驳回"),
```

### 步骤 2.3: 提交 Phase 2
```bash
cd yigongbao-parent
git add -A
git commit -m "refactor: Phase 2 - 修改 Flow 枚举定义

- FlowStatusEnum: 删除 DESIGN_REVIEWING(2040)、DESIGN_REVIEW_PASSED(2050)、DESIGN_REVIEW_REJECTED(2060)
- FlowActionEnum: 删除 SUBMIT_DESIGN、DESIGN_REVIEW_PASS、DESIGN_REVIEW_REJECT
- FlowActionEnum: 新增 COMPLETE_DESIGN

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 3: 修改 Flow 转换规则

**目标**: 更新状态转换规则，移除审核相关逻辑  
**预计时间**: 25分钟  
**风险**: 高（核心流转逻辑）

### 步骤 3.1: 修改 FlowStatusTransitionRules 转换规则

**文件**: `yigongbao-parent/yigongbao-module-flow/src/main/java/com/yigongbao/flow/rules/FlowStatusTransitionRules.java`

修改第69-82行，删除审核转换规则，改为：
```java
// DESIGN_COMPLETED 可跨阶段流转
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_COMPLETED),
        Set.of(FlowStatusEnum.PENDING_PRINT, FlowStatusEnum.ORDER_COMPLETED));
```

### 步骤 3.2: 修改 getAvailableActions 方法

同文件第154-162行，DESIGN case 改为：
```java
case DESIGN -> switch (status) {
    case PENDING_DESIGN -> List.of(FlowActionEnum.START_DESIGN);
    case DESIGN_IN_PROGRESS -> List.of(FlowActionEnum.COMPLETE_DESIGN);
    case DESIGN_COMPLETED -> List.of();
    default -> List.of();
};
```

### 步骤 3.3: 修改 getTargetStatus 方法

同文件第260-266行，删除审核映射，新增：
```java
case COMPLETE_DESIGN -> FlowStatusEnum.DESIGN_COMPLETED.getValue();
```

### 步骤 3.4: 提交 Phase 3
```bash
cd yigongbao-parent
git add -A
git commit -m "refactor: Phase 3 - 修改 Flow 状态转换规则

- 删除审核相关状态转换
- 新增 COMPLETE_DESIGN 动作处理

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 4: 修改其他 Flow 模块文件

**目标**: 清理 Flow 模块中审核相关的逻辑  
**预计时间**: 20分钟

### 步骤 4.1: 修改 FlowPhaseTransitionRules - 删除 DESIGN_REVIEW_PASSED 处理

**文件**: `FlowPhaseTransitionRules.java`  
删除第162-164行 DESIGN_REVIEW_PASSED 处理逻辑。

### 步骤 4.2: 修改 FlowContext - 删除 designRejectCount

**文件**: `FlowContext.java`  
删除 incrementDesignReject() 方法。

### 步骤 4.3: 修改 FlowStateMachineServiceImpl - 删除 DESIGN_REVIEW_REJECT

**文件**: `FlowStateMachineServiceImpl.java`  
删除第222行处理。

### 步骤 4.4: 提交 Phase 4
```bash
cd yigongbao-parent && git add -A && git commit -m "refactor: Phase 4 - 清理 Flow 模块审核逻辑

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 5: 创建新事件和监听器

**预计时间**: 15分钟

### 步骤 5.1: 创建 DesignCompletedEvent
新建: `yigongbao-common/src/main/java/com/yigongbao/common/event/DesignCompletedEvent.java`

### 步骤 5.2: 创建 DesignCompletedListener
新建: `yigongbao-module-production/.../listener/DesignCompletedListener.java`  
复制 DesignReviewPassedListener 逻辑，改为监听 DesignCompletedEvent，状态用 2030。

### 步骤 5.3: 提交
```bash
cd yigongbao-parent && git add -A && git commit -m "feat: Phase 5 - 创建设计完成事件

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 6: 实现完成设计功能

**目标**: 新增完成设计接口和方法，删除旧的提交审核方法  
**预计时间**: 30分钟

### 步骤 6.1: 新增 completeDesign 接口方法到 DesignWorkorderService

**文件**: `DesignWorkorderService.java`

```java
/**
 * 完成设计
 * 根据 needsPhysicalDelivery 执行不同的校验：
 * - 需要实体交付：校验数据包、打印信息、指令单、图纸及确认状态
 * - 不需要实体交付：只校验 STL 重建模型
 */
void completeDesign(Long orderId);
```

### 步骤 6.2: 实现 completeDesign 方法

**文件**: `DesignWorkorderServiceImpl.java`

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void completeDesign(Long orderId) {
    OrderMainEntity order = orderMainService.getById(orderId);
    if (order == null) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }
    if (!FlowStatusEnum.DESIGN_IN_PROGRESS.getValue().equals(order.getStatus())) {
        throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
    }

    boolean needsPhysical = order.getNeedsPhysicalDelivery() != null && order.getNeedsPhysicalDelivery() == 1;

    if (needsPhysical) {
        // 校验：数据包、打印信息、指令单、图纸
        validatePhysicalDelivery(orderId);
    } else {
        // 校验：只需 STL 重建模型
        validateNonPhysicalDelivery(orderId);
    }

    Long userId = StpUtil.getLoginIdAsLong();
    TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.COMPLETE_DESIGN,
            FlowOperator.of(userId, null), order.getVersion());

    OrderMainEntity update = new OrderMainEntity();
    update.setId(orderId);
    update.setPhase(result.getTargetPhase());
    update.setStatus(result.getFinalStatus());
    orderMainService.updateById(update);

    eventPublisher.publishEvent(new DesignCompletedEvent(this, orderId));

    log.info("完成设计: orderId={}, {} -> {}", orderId, 
        FlowStatusEnum.DESIGN_IN_PROGRESS.getName(), 
        FlowStatusEnum.DESIGN_COMPLETED.getName());
}

private void validatePhysicalDelivery(Long orderId) {
    // 数据包校验
    long pkgCount = designPackageMapper.selectCount(
        new LambdaQueryWrapper<DesignPackageEntity>().eq(DesignPackageEntity::getOrderId, orderId));
    if (pkgCount == 0) {
        throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_REQUIRED);
    }

    // 打印信息校验
    DesignPrintInfoEntity printInfo = designPrintInfoMapper.selectOne(
        new LambdaQueryWrapper<DesignPrintInfoEntity>().eq(DesignPrintInfoEntity::getOrderId, orderId));
    if (printInfo == null) {
        throw new BusinessException(ErrorCodeEnum.PRINT_INFO_REQUIRED);
    }

    // 指令单校验
    DesignInstructionEntity instruction = designInstructionMapper.selectOne(
        new LambdaQueryWrapper<DesignInstructionEntity>().eq(DesignInstructionEntity::getOrderId, orderId));
    if (instruction == null || instruction.getIsConfirmed() != StatusConstants.YES) {
        throw new BusinessException(ErrorCodeEnum.INSTRUCTION_NOT_CONFIRMED);
    }

    // 图纸校验
    long drawingCount = designDrawingMapper.selectCount(
        new LambdaQueryWrapper<DesignDrawingEntity>()
            .eq(DesignDrawingEntity::getOrderId, orderId)
            .eq(DesignDrawingEntity::getIsConfirmed, StatusConstants.YES));
    if (drawingCount == 0) {
        throw new BusinessException(ErrorCodeEnum.DRAWING_NOT_CONFIRMED);
    }
}

private void validateNonPhysicalDelivery(Long orderId) {
    long modelCount = packageFileMapper.selectCount(
        new LambdaQueryWrapper<PackageFileEntity>()
            .eq(PackageFileEntity::getOrderId, orderId)
            .eq(PackageFileEntity::getFileType, "STL_REBUILD"));
    if (modelCount == 0) {
        throw new BusinessException(ErrorCodeEnum.STL_MODEL_REQUIRED);
    }
}
```

### 步骤 6.3: 新增 completeDesign 接口到 Controller

**文件**: `DesignWorkorderController.java`

在 submitDesign 接口位置（第86行）附近新增：

```java
@PostMapping("/{orderId}/complete-design")
@RequirePermission("design:CompleteDesign")
public Result<Void> completeDesign(@PathVariable Long orderId) {
    designWorkorderService.completeDesign(orderId);
    return Result.success();
}
```

### 步骤 6.4: 提交
```bash
cd yigongbao-parent && git add -A && git commit -m "feat: Phase 6 - 新增完成设计功能

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 7: 实现手动完成订单功能

**目标**: 新增手动完成订单接口（仅限不需要实体交付的订单）  
**预计时间**: 20分钟

### 步骤 7.1: 新增 manualCompleteOrder 方法到 OrderMainService

**文件**: `OrderMainService.java`

```java
/**
 * 手动完成订单（仅限不需要实体交付的订单）
 * 前置条件：订单状态为设计完成(2030)，needsPhysicalDelivery=0
 */
void manualCompleteOrder(Long orderId);
```

### 步骤 7.2: 实现 manualCompleteOrder 方法

**文件**: `OrderMainServiceImpl.java`

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void manualCompleteOrder(Long orderId) {
    OrderMainEntity order = getById(orderId);
    if (order == null) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }
    
    // 校验：必须是不需要实体交付的订单
    if (order.getNeedsPhysicalDelivery() == null || order.getNeedsPhysicalDelivery() == 1) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY);
    }
    
    // 校验：状态必须是设计完成
    if (!FlowStatusEnum.DESIGN_COMPLETED.getValue().equals(order.getStatus())) {
        throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
    }
    
    Long userId = StpUtil.getLoginIdAsLong();
    TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.COMPLETE,
            FlowOperator.of(userId, null), order.getVersion());
    
    OrderMainEntity update = new OrderMainEntity();
    update.setId(orderId);
    update.setPhase(result.getTargetPhase());
    update.setStatus(result.getFinalStatus());
    updateById(update);
    
    log.info("手动完成订单: orderId={}, {} -> {}", orderId,
        FlowStatusEnum.DESIGN_COMPLETED.getName(),
        FlowStatusEnum.COMPLETED.getName());
}
```

### 步骤 7.3: 新增 manualCompleteOrder 接口到 Controller

**文件**: `OrderController.java`

```java
@PostMapping("/{orderId}/manual-complete")
@RequirePermission("order:ManualComplete")
public Result<Void> manualCompleteOrder(@PathVariable Long orderId) {
    orderMainService.manualCompleteOrder(orderId);
    return Result.success();
}
```

### 步骤 7.4: 提交
```bash
cd yigongbao-parent && git add -A && git commit -m "feat: Phase 7 - 新增手动完成订单功能

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 8: 修改生产模块下载逻辑

**目标**: 修改数据包下载逻辑，允许设计完成(2030)状态下载  
**预计时间**: 15分钟

### 步骤 8.1: 修改 ProductionRecordServiceImpl 下载校验

**文件**: `ProductionRecordServiceImpl.java`  
**位置**: 第305行和第322行

将状态检查从 DESIGN_REVIEW_PASSED(2050) 改为 DESIGN_COMPLETED(2030)：

```java
// 第305行附近
if (!FlowStatusEnum.DESIGN_COMPLETED.getValue().equals(record.getStatus())) {
    throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_STATUS_ERROR);
}

// 第322行附近（聚合判断）
boolean allDownloaded = records.stream()
    .allMatch(r -> !FlowStatusEnum.DESIGN_COMPLETED.getValue().equals(r.getStatus()));
```

### 步骤 8.2: 提交
```bash
cd yigongbao-parent && git add -A && git commit -m "refactor: Phase 8 - 修改生产数据包下载逻辑

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 9: 删除旧的 Design 模块方法

**目标**: 删除 submitDesign、continueDesign 方法和接口  
**预计时间**: 15分钟

### 步骤 9.1: 删除 DesignWorkorderController 中的 submitDesign 接口

**文件**: `DesignWorkorderController.java`  
**位置**: 第86行附近

删除整个 submitDesign 方法（包括注解）。

### 步骤 9.2: 删除 DesignWorkorderServiceImpl 中的方法

**文件**: `DesignWorkorderServiceImpl.java`

1. 删除 submitDesign 方法（第452-504行）
2. 删除 continueDesign 方法（第385-437行）

### 步骤 9.3: 修改 DesignQueryHelper

**文件**: `DesignQueryHelper.java`  
**位置**: 第77行

从状态列表中删除 DESIGN_REVIEW_REJECTED。

### 步骤 9.4: 更新 DesignWorkorderQueryDTO 注释

**文件**: `DesignWorkorderQueryDTO.java`  
**位置**: 第28行

将注释从 "2010/2020/2040/2060" 改为 "2010/2020/2030"。

### 步骤 9.5: 提交
```bash
cd yigongbao-parent && git add -A && git commit -m "refactor: Phase 9 - 删除旧的设计模块方法

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 10: 删除 Order 模块自动重新提交逻辑

**目标**: 删除订单修改后自动触发审核的逻辑  
**预计时间**: 10分钟

### 步骤 10.1: 删除 OrderMainServiceImpl 中的自动重新提交逻辑

**文件**: `OrderMainServiceImpl.java`  
**位置**: 第408-417行

删除订单修改后自动重新提交设计审核的代码块。

### 步骤 10.2: 删除 OrderModifyApplyServiceImpl 中的自动触发逻辑

**文件**: `OrderModifyApplyServiceImpl.java`  
**位置**: 第639-642行

删除修改申请通过后自动触发 CONTINUE_DESIGN + SUBMIT_DESIGN 的逻辑。

### 步骤 10.3: 提交
```bash
cd yigongbao-parent && git add -A && git commit -m "refactor: Phase 10 - 删除订单模块自动重新提交逻辑

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 11: 更新和删除测试文件

**目标**: 删除审核相关测试，更新设计工单测试  
**预计时间**: 20分钟

### 步骤 11.1: 删除 DesignReviewServiceImplTest

**文件**: `DesignReviewServiceImplTest.java`

```bash
cd yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl
rm DesignReviewServiceImplTest.java
```

### 步骤 11.2: 更新 DesignWorkorderServiceImplTest

**文件**: `DesignWorkorderServiceImplTest.java`

1. 删除 submitDesign 相关测试方法
2. 删除 continueDesign 相关测试方法
3. 新增 completeDesign 测试方法（需要实体交付 + 不需要实体交付两个场景）

### 步骤 11.3: 更新 FlowStatusTransitionRulesTest

**文件**: `FlowStatusTransitionRulesTest.java`

删除审核状态相关的测试用例（DESIGN_REVIEWING, DESIGN_REVIEW_PASSED, DESIGN_REVIEW_REJECTED）。

### 步骤 11.4: 更新 FlowPhaseTransitionRulesTest

**文件**: `FlowPhaseTransitionRulesTest.java`

删除 DESIGN_REVIEW_PASSED 相关测试用例。

### 步骤 11.5: 提交
```bash
cd yigongbao-parent && git add -A && git commit -m "test: Phase 11 - 更新和删除测试文件

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Phase 12: 测试与验证

**目标**: 验证所有修改正确无误  
**预计时间**: 30分钟

### 步骤 12.1: 编译检查

```bash
cd yigongbao-parent
mvn clean compile -DskipTests
```

预期：编译成功，无报错。

### 步骤 12.2: 运行单元测试

```bash
mvn test -pl yigongbao-module-flow
mvn test -pl yigongbao-module-design
mvn test -pl yigongbao-module-order
mvn test -pl yigongbao-module-production
```

预期：所有测试通过。

### 步骤 12.3: 验证新功能

**需要实体交付的订单流程：**
1. 创建订单（needsPhysicalDelivery=1）
2. 设计师开始设计 → 状态变为 DESIGN_IN_PROGRESS(2020)
3. 设计师完成设计 → 状态变为 DESIGN_COMPLETED(2030)
4. 生产员下载数据包 → 状态变为 PENDING_PRINT(3010)

**不需要实体交付的订单流程：**
1. 创建订单（needsPhysicalDelivery=0）
2. 设计师开始设计 → 状态变为 DESIGN_IN_PROGRESS(2020)
3. 设计师完成设计 → 状态变为 DESIGN_COMPLETED(2030)
4. 管理员手动完成 → 状态变为 ORDER_COMPLETED(8010)

### 步骤 12.4: 验证删除内容

检查以下内容已完全删除：
- [ ] DesignReviewController.java
- [ ] DesignReviewService.java
- [ ] DesignReviewServiceImpl.java
- [ ] 审核相关 DTO/VO
- [ ] DESIGN_REVIEWING/DESIGN_REVIEW_PASSED/DESIGN_REVIEW_REJECTED 状态
- [ ] SUBMIT_DESIGN/DESIGN_REVIEW_PASS/DESIGN_REVIEW_REJECT 动作

### 步骤 12.5: 最终提交

```bash
cd yigongbao-parent
git add -A
git commit -m "chore: Phase 12 - 完成测试与验证

所有阶段完成：
- Phase 1: 删除审核相关文件
- Phase 2: 修改 Flow 枚举定义
- Phase 3: 修改 Flow 状态转换规则
- Phase 4: 清理 Flow 模块审核逻辑
- Phase 5: 创建设计完成事件
- Phase 6: 新增完成设计功能
- Phase 7: 新增手动完成订单功能
- Phase 8: 修改生产数据包下载逻辑
- Phase 9: 删除旧的设计模块方法
- Phase 10: 删除订单模块自动重新提交逻辑
- Phase 11: 更新和删除测试文件
- Phase 12: 测试与验证

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## 实施完成

所有 12 个阶段完成后，设计审核功能已完全移除，新的简化流程已实施。

**关键变更总结：**
- ✅ 删除 7 个审核相关文件
- ✅ 删除 3 个审核状态 + 3 个审核动作
- ✅ 修改 9 个 Flow 模块文件
- ✅ 新增完成设计功能（双重校验路径）
- ✅ 新增手动完成订单功能
- ✅ 修改生产流转卡创建逻辑

**新流程：**
```
设计中(2020) → 完成设计 → 设计完成(2030)
  ├─ 需要实体交付 → 下载数据包 → 待打印(3010)
  └─ 不需要实体交付 → 手动完成 → 订单完成(8010)
```

