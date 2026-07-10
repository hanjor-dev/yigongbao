# 订单取消审核流程设计文档

**文档版本**: 1.0  
**创建日期**: 2026-07-08  
**设计目标**: 将订单取消功能从"直接取消"改为"提交申请→审核→执行"的流程

---

## 1. 需求概述

### 1.1 背景

当前订单取消功能是直接取消，缺乏审核控制。为了加强订单管理，需要对取消操作增加审核机制。

### 1.2 核心需求

**变更内容**：将订单取消从"直接取消"改为"提交申请→审核→执行"的流程

**关键规则**：
- **发起权限**：订单创建的业务员 + 该订单所属设计师
- **审核范围**：仅设计阶段及之后的订单需要审核，订单阶段（草稿/待审核/驳回）直接取消
- **审核角色**：设计管理员
- **消息通知**：
  - 提交申请时 → 通知设计管理员
  - 审核通过时 → 通知申请人
  - 审核驳回时 → 通知申请人
- **原因填写**：取消原因和驳回原因均为选填

**业务控制**：
订单存在待审核的取消申请时，以下操作被阻止：
- ❌ 数据审核（通过/驳回）
- ❌ 订单修改
- ❌ 设计相关操作（开始设计、完成设计等）
- ❌ 生产相关操作（开始打印、质检等）
- ❌ 再次提交取消申请

---

## 2. 数据库设计

### 2.1 新建表：order_cancel_apply

```sql
CREATE TABLE order_cancel_apply (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id            BIGINT NOT NULL COMMENT '订单ID',
    apply_by            BIGINT NOT NULL COMMENT '申请人ID',
    apply_reason        VARCHAR(500) COMMENT '取消原因（选填）',
    audit_status        TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：1=待审核，2=已通过，3=已驳回',
    audit_by            BIGINT COMMENT '审核人ID',
    audit_reason        VARCHAR(500) COMMENT '审核驳回原因（选填）',
    audit_time          DATETIME COMMENT '审核时间',
    create_time         DATETIME NOT NULL COMMENT '创建时间',
    update_time         DATETIME NOT NULL COMMENT '更新时间',
    create_by           BIGINT COMMENT '创建人ID',
    update_by           BIGINT COMMENT '更新人ID',
    is_deleted          TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    
    KEY idx_order_cancel_apply_order_id (order_id),
    KEY idx_order_cancel_apply_audit_status (audit_status),
    KEY idx_order_cancel_apply_apply_by (apply_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单取消申请表';
```

**字段说明**：
- `audit_status`: 1=待审核，2=已通过，3=已驳回
- `apply_reason` 和 `audit_reason`: 均为选填字段，最大500字符

### 2.2 修改表：order_main

```sql
-- 添加字段
ALTER TABLE order_main
ADD COLUMN has_pending_cancel_apply TINYINT DEFAULT 0 COMMENT '是否有待审核的取消申请（0=否，1=是）';

-- 添加索引（性能优化：待审核检查会频繁查询该字段）
ALTER TABLE order_main
ADD KEY idx_order_main_has_pending_cancel_apply (has_pending_cancel_apply);
```

**字段作用**：
1. 性能优化：避免频繁查询 `order_cancel_apply` 表
2. 业务控制：快速判断是否需要阻止某些操作

**索引作用**：
待审核检查机制会在数据审核、订单修改、设计操作、生产操作等多个环节频繁查询该字段，添加索引可显著提升性能

---

## 3. 接口设计

### 3.1 DTO 设计

**提交取消申请 DTO**：
```java
@Data
public class CancelOrderApplyDTO {
    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    
    @Schema(description = "取消原因（选填）")
    @Length(max = 500, message = "取消原因不能超过500字")
    private String reason;
}
```

**审核取消申请 DTO**：
```java
@Data
public class AuditCancelApplyDTO {
    @Schema(description = "审核结果：true=通过，false=驳回")
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
    
    @Schema(description = "审核备注（驳回时选填）")
    @Length(max = 500, message = "审核备注不能超过500字")
    private String reason;
}
```

### 3.2 VO 设计

**取消申请详情 VO**：
```java
@Data
public class CancelApplyVO {
    private Long id;
    private Long orderId;
    private String orderCode;
    private Long applyBy;
    private String applyByName;
    private String applyReason;
    private Integer auditStatus;
    private Long auditBy;
    private String auditByName;
    private String auditReason;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
}
```

---

## 4. 核心业务流程

### 4.1 提交取消申请

**适用范围**：设计阶段及之后的订单（phase >= 20）

**前置检查**：
1. 订单存在且未被取消
2. 申请人是订单创建人或该订单的设计师
3. 订单当前没有待审核的取消申请（`has_pending_cancel_apply = 0`）
4. 订单处于设计阶段或之后（phase >= 20）

**执行步骤**：
1. 创建取消申请记录（`order_cancel_apply` 表，状态为待审核）
2. 更新订单表：`has_pending_cancel_apply = 1`
3. 发布事件：`CancelApplySubmittedEvent`
4. 触发消息通知：通知所有设计管理员

**返回**：申请ID

### 4.2 审核取消申请（通过）

**前置检查**：
1. 申请存在且状态为待审核
2. 当前用户是设计管理员
3. 订单未被取消

**执行步骤**：
1. 更新申请记录：状态改为已通过，记录审核人和审核时间
2. 调用 `FlowFacade.executeFlow(orderId, CANCEL, operator)` 获取 `TransitionResult`
3. 使用返回的 `TransitionResult` 更新订单状态：
   ```java
   TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.CANCEL, operator);
   order.setPhase(result.getTargetPhase());
   order.setStatus(result.getFinalStatus());
   updateById(order);
   ```
4. 更新订单表：`has_pending_cancel_apply = 0`
5. 发布事件：`CancelApplyApprovedEvent`
6. 触发消息通知：通知申请人

### 4.3 审核取消申请（驳回）

**前置检查**：
1. 申请存在且状态为待审核
2. 当前用户是设计管理员

**执行步骤**：
1. 更新申请记录：状态改为已驳回，记录审核人、审核原因和审核时间
2. 更新订单表：`has_pending_cancel_apply = 0`
3. 发布事件：`CancelApplyRejectedEvent`
4. 触发消息通知：通知申请人

**说明**：订单保持原状态，不执行取消操作

### 4.4 直接取消订单（订单阶段）

**适用范围**：订单阶段（phase < 20）的订单

**前置检查**：
1. 订单存在且未被取消
2. 订单处于订单阶段（草稿、待审核、驳回状态）

**执行步骤**：
1. 调用 `FlowFacade.executeFlow(orderId, CANCEL, operator)`
2. 更新订单状态为已取消
3. 发布事件：`OrderCancelledEvent`

**说明**：订单阶段的取消不需要审核，保持原有的直接取消逻辑

---

## 5. Controller 设计

### 5.1 新增 Controller：OrderCancelApplyController

```java
@RestController
@RequestMapping("/order/cancel-apply")
@RequiredArgsConstructor
@Tag(name = "订单取消申请管理")
@RequireSign
public class OrderCancelApplyController {

    private final OrderCancelApplyService cancelApplyService;

    @Operation(summary = "提交取消申请")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.CREATE,
                  operation = "提交取消申请")
    @PostMapping
    public Result<Long> submitCancelApply(@Valid @RequestBody CancelOrderApplyDTO dto) {
        return Result.success(cancelApplyService.submitCancelApply(dto));
    }

    @Operation(summary = "审核取消申请")
    @OperationLog(module = "订单管理", businessType = OperationTypeEnum.AUDIT, 
                  operation = "审核取消申请")
    @PostMapping("/{applyId}/audit")
    public Result<Void> auditCancelApply(@PathVariable Long applyId,
                                         @Valid @RequestBody AuditCancelApplyDTO dto) {
        cancelApplyService.auditCancelApply(applyId, dto);
        return Result.success();
    }

    @Operation(summary = "查询取消申请详情")
    @GetMapping("/{applyId}")
    public Result<CancelApplyVO> getCancelApplyDetail(@PathVariable Long applyId) {
        return Result.success(cancelApplyService.getCancelApplyDetail(applyId));
    }

    @Operation(summary = "查询待审核的取消申请列表（设计管理员）")
    @PostMapping("/pending/list")
    public Result<IPage<CancelApplyVO>> listPendingApplies(@Valid @RequestBody PageDTO dto) {
        return Result.success(cancelApplyService.listPendingApplies(dto));
    }
    
    @Operation(summary = "查询我的取消申请列表")
    @PostMapping("/my-applies")
    public Result<IPage<CancelApplyVO>> listMyApplies(@Valid @RequestBody PageDTO dto) {
        return Result.success(cancelApplyService.listMyApplies(dto));
    }
    
    @Operation(summary = "查询订单的取消申请历史")
    @GetMapping("/order/{orderId}/history")
    public Result<List<CancelApplyVO>> getCancelApplyHistory(@PathVariable Long orderId) {
        return Result.success(cancelApplyService.getCancelApplyHistory(orderId));
    }
}
```

---

## 6. Service 设计

### 6.1 OrderCancelApplyService 接口

```java
public interface OrderCancelApplyService {
    /**
     * 提交取消申请
     */
    @Transactional(rollbackFor = Exception.class)
    Long submitCancelApply(CancelOrderApplyDTO dto);
    
    /**
     * 审核取消申请
     */
    @Transactional(rollbackFor = Exception.class)
    void auditCancelApply(Long applyId, AuditCancelApplyDTO dto);
    
    /**
     * 查询申请详情
     */
    CancelApplyVO getCancelApplyDetail(Long applyId);
    
    /**
     * 查询待审核列表（设计管理员）
     */
    IPage<CancelApplyVO> listPendingApplies(PageDTO dto);
    
    /**
     * 检查订单是否有待审核的取消申请
     */
    boolean hasPendingCancelApply(Long orderId);
    
    /**
     * 查询我的取消申请列表
     */
    IPage<CancelApplyVO> listMyApplies(PageDTO dto);
    
    /**
     * 查询订单的取消申请历史
     */
    List<CancelApplyVO> getCancelApplyHistory(Long orderId);
}
```

### 6.2 Service 实现关键逻辑

**提交取消申请**：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public Long submitCancelApply(CancelOrderApplyDTO dto) {
    Long currentUserId = getCurrentUserId();
    OrderMainEntity order = orderMainService.getById(dto.getOrderId());
    
    // 前置检查1：订单存在
    if (order == null) {
        log.warn("订单不存在: orderId={}", dto.getOrderId());
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }
    
    // 前置检查2：订单未被取消
    if (order.getStatus().equals(FlowStatusEnum.CANCELLED.getValue())) {
        log.warn("订单已取消，无法提交取消申请: orderId={}", dto.getOrderId());
        throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_CANCELLED);
    }
    
    // 前置检查3：订单没有待审核的取消申请
    if (order.getHasPendingCancelApply().equals(StatusConstants.YES)) {
        log.warn("订单已有待审核的取消申请: orderId={}", dto.getOrderId());
        throw new BusinessException(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING);
    }
    
    // 前置检查4：订单处于设计阶段或之后
    if (order.getPhase() < 20) {
        log.warn("订单阶段不允许提交取消申请: orderId={}, phase={}", dto.getOrderId(), order.getPhase());
        throw new BusinessException(ErrorCodeEnum.ORDER_PHASE_NOT_ALLOW_APPLY);
    }
    
    // 权限检查：只有订单创建人或该订单的设计师可以申请
    boolean isCreator = Objects.equals(order.getCreateBy(), currentUserId);
    boolean isDesigner = Objects.equals(order.getDesignerId(), currentUserId);
    if (!isCreator && !isDesigner) {
        log.warn("无权提交取消申请: orderId={}, userId={}", dto.getOrderId(), currentUserId);
        throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
    }
    
    // 创建申请记录
    OrderCancelApplyEntity apply = new OrderCancelApplyEntity();
    apply.setOrderId(dto.getOrderId());
    apply.setApplyBy(currentUserId);
    apply.setApplyReason(dto.getReason());
    apply.setAuditStatus(1); // 待审核
    save(apply);
    
    // 更新订单标志
    orderMainService.update(new LambdaUpdateWrapper<OrderMainEntity>()
        .eq(OrderMainEntity::getId, dto.getOrderId())
        .set(OrderMainEntity::getHasPendingCancelApply, StatusConstants.YES));
    
    // 发布事件
    String applyByName = getUserRealName(currentUserId);
    eventPublisher.publishEvent(new CancelApplySubmittedEvent(
        this, apply.getId(), order.getId(), order.getOrderCode(),
        currentUserId, applyByName, dto.getReason()));
    
    log.info("创建取消申请: applyId={}, orderId={}, applyBy={}, reason={}", 
        apply.getId(), dto.getOrderId(), currentUserId, dto.getReason());
    
    return apply.getId();
}
```

**审核通过**：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void auditCancelApply(Long applyId, AuditCancelApplyDTO dto) {
    // 权限检查
    String roleCode = getCurrentUserRoleCode();
    if (!RoleCodeConstants.DESIGN_ADMIN.equals(roleCode)) {
        throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
    }
    
    // 前置检查1：申请是否存在
    OrderCancelApplyEntity apply = getById(applyId);
    if (apply == null) {
        log.warn("取消申请不存在: applyId={}", applyId);
        throw new BusinessException(ErrorCodeEnum.CANCEL_APPLY_NOT_FOUND);
    }
    
    // 前置检查2：申请状态检查
    if (!apply.getAuditStatus().equals(1)) {
        log.warn("取消申请已审核: applyId={}, status={}", applyId, apply.getAuditStatus());
        throw new BusinessException(ErrorCodeEnum.CANCEL_APPLY_ALREADY_AUDITED);
    }
    
    if (dto.getApproved()) {
        // 前置检查3：订单状态检查（审核通过分支）
        OrderMainEntity order = orderMainService.getById(apply.getOrderId());
        if (order.getStatus().equals(FlowStatusEnum.CANCELLED.getValue())) {
            log.warn("订单已取消，无法审核通过: orderId={}", apply.getOrderId());
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_CANCELLED);
        }
        // 审核通过：取消订单
        Long currentUserId = getCurrentUserId();
        String operatorName = getUserRealName(currentUserId);
        
        TransitionResult result = flowFacade.executeFlow(
            apply.getOrderId(), FlowActionEnum.CANCEL, 
            new FlowOperator(currentUserId, operatorName, null));
        
        OrderMainEntity order = orderMainService.getById(apply.getOrderId());
        order.setPhase(result.getTargetPhase());
        order.setStatus(result.getFinalStatus());
        order.setHasPendingCancelApply(StatusConstants.NO);
        orderMainService.updateById(order);
        
        apply.setAuditStatus(2); // 已通过
        apply.setAuditBy(currentUserId);
        apply.setAuditTime(LocalDateTime.now());
        updateById(apply);
        
        eventPublisher.publishEvent(new CancelApplyApprovedEvent(...));
        log.info("取消申请审核通过: applyId={}, orderId={}, auditBy={}, 订单已取消", 
            applyId, apply.getOrderId(), currentUserId);
    } else {
        // 审核驳回
        apply.setAuditStatus(3); // 已驳回
        apply.setAuditBy(getCurrentUserId());
        apply.setAuditReason(dto.getReason());
        apply.setAuditTime(LocalDateTime.now());
        updateById(apply);
        
        // 更新订单标志
        orderMainService.update(new LambdaUpdateWrapper<OrderMainEntity>()
            .eq(OrderMainEntity::getId, apply.getOrderId())
            .set(OrderMainEntity::getHasPendingCancelApply, StatusConstants.NO));
        
        eventPublisher.publishEvent(new CancelApplyRejectedEvent(...));
        log.info("取消申请审核驳回: applyId={}, orderId={}, auditBy={}, reason={}", 
            applyId, apply.getOrderId(), getCurrentUserId(), dto.getReason());
    }
}
```

### 6.2 OrderMainService 修改

**cancelOrder() 方法改造**：

```java
@Override
public void cancelOrder(Long id) {
    Long currentUserId = getCurrentUserId();
    OrderMainEntity order = getById(id);
    
    // 校验订单存在和状态
    if (order == null) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }
    if (order.getStatus().equals(FlowStatusEnum.CANCELLED.getValue())) {
        throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_CANCELLED);
    }
    
    // 根据订单阶段判断取消方式
    if (order.getPhase() < 20) {
        // 订单阶段：直接取消
        directCancelOrder(id, order, currentUserId);
    } else {
        // 设计阶段及之后：需要提交取消申请
        throw new BusinessException(ErrorCodeEnum.ORDER_NEED_CANCEL_APPLY);
    }
}

private void directCancelOrder(Long id, OrderMainEntity order, Long currentUserId) {
    String operatorName = getUserRealName(currentUserId);
    TransitionResult result = flowFacade.executeFlow(
        id, FlowActionEnum.CANCEL, new FlowOperator(currentUserId, operatorName, null));
    
    order.setPhase(result.getTargetPhase());
    order.setStatus(result.getFinalStatus());
    updateById(order);
    
    eventPublisher.publishEvent(new OrderCancelledEvent(this, id));
    log.info("直接取消订单: orderId={}", id);
}
```

---

## 7. 待审核检查机制

### 7.1 需要添加检查的方法

在以下方法的开头添加检查逻辑：

**OrderMainService**：
- `auditPass()` - 数据审核通过
- `auditReject()` - 数据审核驳回

**OrderModifyApplyService**：
- `submitApply()` - 提交订单修改申请

**DesignService**（设计模块）：
- `startDesign()` - 开始设计
- `completeDesign()` - 完成设计

**生产相关Service**：
- 开始打印、完成打印、质检等操作

### 7.2 检查代码示例

```java
// 在方法开头添加
if (cancelApplyService.hasPendingCancelApply(orderId)) {
    throw new BusinessException(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING);
}
```

---

## 8. 事件设计

### 8.1 CancelApplySubmittedEvent

```java
@Getter
public class CancelApplySubmittedEvent extends ApplicationEvent {
    private final Long applyId;
    private final Long orderId;
    private final String orderCode;
    private final Long applyBy;
    private final String applyByName;
    private final String applyReason;
}
```

**触发时机**：用户提交取消申请时  
**通知对象**：所有设计管理员

### 8.2 CancelApplyApprovedEvent

```java
@Getter
public class CancelApplyApprovedEvent extends ApplicationEvent {
    private final Long applyId;
    private final Long orderId;
    private final String orderCode;
    private final Long applyBy;
    private final String applyByName;
    private final Long auditBy;
    private final String auditByName;
}
```

**触发时机**：设计管理员审核通过时  
**通知对象**：申请人

### 8.3 CancelApplyRejectedEvent

```java
@Getter
public class CancelApplyRejectedEvent extends ApplicationEvent {
    private final Long applyId;
    private final Long orderId;
    private final String orderCode;
    private final Long applyBy;
    private final String applyByName;
    private final Long auditBy;
    private final String auditByName;
    private final String auditReason;
}
```

**触发时机**：设计管理员审核驳回时  
**通知对象**：申请人

---

## 9. 权限控制

### 9.1 提交取消申请权限

**规则**：只有订单创建人或该订单的设计师可以提交取消申请

**实现**：
```java
boolean isCreator = Objects.equals(order.getCreateBy(), currentUserId);
boolean isDesigner = Objects.equals(order.getDesignerId(), currentUserId);

if (!isCreator && !isDesigner) {
    throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
}
```

### 9.2 审核取消申请权限

**规则**：只有设计管理员可以审核取消申请

**实现**：
```java
String roleCode = getCurrentUserRoleCode();
if (!RoleCodeConstants.DESIGN_ADMIN.equals(roleCode)) {
    throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
}
```

---

## 10. 错误码定义

### 10.1 ErrorCodeEnum 新增

```java
// 取消申请相关
ORDER_CANCEL_APPLY_PENDING(621, "订单存在待审核的取消申请"),
ORDER_NEED_CANCEL_APPLY(622, "该订单需要提交取消申请"),
CANCEL_APPLY_NOT_FOUND(623, "取消申请不存在"),
CANCEL_APPLY_ALREADY_AUDITED(624, "取消申请已审核"),
ORDER_PHASE_NOT_ALLOW_APPLY(625, "订单阶段不允许提交取消申请"),
```

---

## 11. 实施要点

### 11.1 核心改动点

1. ✅ 新建 `order_cancel_apply` 表存储取消申请
2. ✅ `order_main` 表新增 `has_pending_cancel_apply` 字段
3. ✅ 新增 `OrderCancelApplyController` 和 `OrderCancelApplyService`
4. ✅ 修改 `OrderMainService.cancelOrder()` 方法，区分直接取消和申请取消
5. ✅ 在关键方法中添加待审核检查，阻止操作
6. ✅ 新增3个事件和对应的消息通知
7. ✅ 新增4个错误码

### 11.2 实施顺序

1. 数据库变更（DDL）
2. 创建 Entity/DTO/VO
3. 实现 OrderCancelApplyService
4. 实现 OrderCancelApplyController
5. 修改 OrderMainService.cancelOrder()
6. 在相关Service中添加待审核检查
7. 实现事件监听和消息通知
8. 单元测试和集成测试

### 11.3 测试要点

**功能测试**：
1. 订单阶段（phase < 20）直接取消成功
2. 设计阶段（phase >= 20）需要提交申请
3. 只有业务员和设计师可以提交申请
4. 只有设计管理员可以审核
5. 审核通过后订单被取消
6. 审核驳回后订单保持原状态
7. 有待审核申请时，相关操作被阻止
8. 消息通知正确发送

**边界测试**：
1. 重复提交取消申请（应被阻止）
2. 已取消的订单再次取消（应被阻止）
3. 已审核的申请再次审核（应被阻止）

---

## 12. 风险评估

### 12.1 技术风险

**风险点**：`has_pending_cancel_apply` 字段与实际申请状态不一致

**缓解措施**：
- 使用事务保证数据一致性
- 提供修复脚本用于数据校验和修复

### 12.2 业务风险

**风险点**：用户提交取消申请后，审核延迟导致订单继续推进

**缓解措施**：
- 提交申请后立即阻止相关操作
- 设计管理员端增加待审核申请提醒
- 设置审核超时预警

---

## 13. 后续优化

1. 增加取消申请的批量审核功能
2. 增加取消申请的审核历史记录
3. 增加取消申请的统计分析
4. 优化消息通知模板

---

## 14. 前端影响域

### 14.1 订单详情页改动

**新增展示内容**：
- 取消申请状态标识：
  - 无申请：不显示
  - 申请中：显示"取消申请审核中"标签（黄色）
  - 已通过：不显示（订单已取消）
  - 已驳回：显示"取消申请已驳回"提示，可查看驳回原因

**新增操作区域**：
- 取消申请历史卡片（折叠展示）：
  - 申请人、申请时间、申请原因
  - 审核人、审核时间、审核结果、驳回原因
  - 支持查看多次申请记录

### 14.2 订单操作按钮逻辑调整

**原有逻辑**：
- 所有订单显示"取消订单"按钮

**新逻辑**：
```javascript
// 伪代码
if (order.phase < 20) {
    // 订单阶段：显示"取消订单"按钮，直接调用取消接口
    showButton("取消订单", () => cancelOrder(orderId));
} else if (order.hasPendingCancelApply) {
    // 有待审核申请：显示"取消申请审核中"，按钮置灰禁用
    showDisabledButton("取消申请审核中");
} else {
    // 设计阶段及之后：显示"申请取消"按钮，跳转到申请页面
    showButton("申请取消", () => navigateToCancelApply(orderId));
}
```

### 14.3 新增页面

**页面1：取消申请提交页**
- 路径：`/order/cancel-apply/create?orderId={id}`
- 内容：
  - 订单基本信息展示（只读）
  - 取消原因输入框（选填，最多500字）
  - 提交/取消按钮

**页面2：取消申请审核页（设计管理员）**
- 路径：`/order/cancel-apply/audit`
- 内容：
  - 待审核列表（表格）：订单号、申请人、申请时间、操作
  - 审核弹窗：
    - 订单详情链接
    - 申请原因展示
    - 审核操作：通过/驳回
    - 驳回原因输入框（选填）

**页面3：我的取消申请页（业务员/设计师）**
- 路径：`/order/cancel-apply/my`
- 内容：
  - 我的申请列表（表格）：订单号、申请时间、状态、审核结果
  - 状态筛选：全部/待审核/已通过/已驳回

### 14.4 订单列表改动

**新增状态标识**：
- 订单状态列增加子状态显示：
  - "申请取消中"（取消申请待审核时显示）
  - 样式：黄色标签，与其他状态并列显示

**筛选条件**：
- 建议在高级筛选中增加"取消申请状态"筛选项

### 14.5 消息通知入口

**通知中心**：
- 设计管理员接收"新的取消申请"通知，点击跳转到审核页
- 申请人接收"审核结果"通知，点击跳转到订单详情页

---

## 15. 基础设施层设计

### 15.1 Entity 设计

```java
package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 订单取消申请实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_cancel_apply")
public class OrderCancelApplyEntity extends BaseEntity {
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 申请人ID
     */
    private Long applyBy;
    
    /**
     * 取消原因（选填）
     */
    private String applyReason;
    
    /**
     * 审核状态：1=待审核，2=已通过，3=已驳回
     */
    private Integer auditStatus;
    
    /**
     * 审核人ID
     */
    private Long auditBy;
    
    /**
     * 审核驳回原因（选填）
     */
    private String auditReason;
    
    /**
     * 审核时间
     */
    private LocalDateTime auditTime;
}
```

**说明**：
- 继承 `BaseEntity`，自动拥有 id、createTime、updateTime、createBy、updateBy、isDeleted 字段
- 字段与数据库表一一对应

### 15.2 Mapper 设计

```java
package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单取消申请 Mapper
 */
@Mapper
public interface OrderCancelApplyMapper extends BaseMapper<OrderCancelApplyEntity> {
    // 使用 MyBatis-Plus 提供的基础方法，无需自定义 SQL
}
```

### 15.3 Convert 设计

```java
package com.yigongbao.module.order.convert;

import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import com.yigongbao.module.order.vo.order.CancelApplyVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 订单取消申请转换器
 * 使用 BeanUtils.copyProperties 进行转换（项目规范）
 */
@Component
public class OrderCancelApplyConvert {
    
    /**
     * Entity 转 VO
     */
    public CancelApplyVO toVO(OrderCancelApplyEntity entity) {
        if (entity == null) {
            return null;
        }
        CancelApplyVO vo = new CancelApplyVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
    
    /**
     * Entity 转 VO（带用户名填充）
     * 
     * @param entity 实体
     * @param applyByName 申请人姓名
     * @param auditByName 审核人姓名
     * @param orderCode 订单编号
     */
    public CancelApplyVO toVO(OrderCancelApplyEntity entity, String applyByName, 
                              String auditByName, String orderCode) {
        CancelApplyVO vo = toVO(entity);
        if (vo != null) {
            vo.setApplyByName(applyByName);
            vo.setAuditByName(auditByName);
            vo.setOrderCode(orderCode);
        }
        return vo;
    }
}
```

**说明**：
- 根据项目规范使用 `BeanUtils.copyProperties`，而非 MapStruct
- 提供带名称填充的重载方法，方便业务层使用

### 15.4 OrderMainEntity 修改

```java
// 在 OrderMainEntity 中新增字段
/**
 * 是否有待审核的取消申请（0=否，1=是）
 */
private Integer hasPendingCancelApply;
```

---

## 16. 消息通知实现

### 16.1 事件监听器设计

**模块位置**：`yigongbao-module-order`

**监听器类**：`OrderCancelApplyEventListener`

```java
package com.yigongbao.module.order.listener;

import com.yigongbao.common.constant.RoleCodeConstants;
import com.yigongbao.common.event.CancelApplyApprovedEvent;
import com.yigongbao.common.event.CancelApplyRejectedEvent;
import com.yigongbao.common.event.CancelApplySubmittedEvent;
import com.yigongbao.module.system.message.service.MessageService;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单取消申请事件监听器
 * 负责发送消息通知
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelApplyEventListener {
    
    private final MessageService messageService;
    private final UserService userService;
    
    /**
     * 监听取消申请提交事件
     * 通知所有设计管理员
     */
    @EventListener
    @Async
    public void handleCancelApplySubmitted(CancelApplySubmittedEvent event) {
        // 获取所有设计管理员
        List<Long> adminIds = userService.getUserIdsByRoleCode(RoleCodeConstants.DESIGN_ADMIN);
        
        if (adminIds.isEmpty()) {
            log.warn("未找到设计管理员，无法发送取消申请通知: applyId={}", event.getApplyId());
            return;
        }
        
        // 构建消息内容
        String title = "新的订单取消申请";
        String content = String.format(
            "订单 %s 有新的取消申请待审核\n申请人：%s\n申请原因：%s",
            event.getOrderCode(),
            event.getApplyByName(),
            event.getApplyReason() != null ? event.getApplyReason() : "无"
        );
        
        // 发送站内消息
        messageService.sendToUsers(adminIds, title, content, 
            "/order/cancel-apply/audit", event.getApplyId());
        
        log.info("发送取消申请通知: applyId={}, adminCount={}", event.getApplyId(), adminIds.size());
    }
    
    /**
     * 监听审核通过事件
     * 通知申请人
     */
    @EventListener
    @Async
    public void handleCancelApplyApproved(CancelApplyApprovedEvent event) {
        String title = "订单取消申请已通过";
        String content = String.format(
            "您的订单 %s 取消申请已审核通过\n审核人：%s\n订单已取消",
            event.getOrderCode(),
            event.getAuditByName()
        );
        
        messageService.sendToUser(event.getApplyBy(), title, content, 
            "/order/detail/" + event.getOrderId(), event.getOrderId());
        
        log.info("发送审核通过通知: applyId={}, applyBy={}", event.getApplyId(), event.getApplyBy());
    }
    
    /**
     * 监听审核驳回事件
     * 通知申请人
     */
    @EventListener
    @Async
    public void handleCancelApplyRejected(CancelApplyRejectedEvent event) {
        String title = "订单取消申请已驳回";
        String content = String.format(
            "您的订单 %s 取消申请已被驳回\n审核人：%s\n驳回原因：%s",
            event.getOrderCode(),
            event.getAuditByName(),
            event.getAuditReason() != null ? event.getAuditReason() : "无"
        );
        
        messageService.sendToUser(event.getApplyBy(), title, content, 
            "/order/detail/" + event.getOrderId(), event.getOrderId());
        
        log.info("发送审核驳回通知: applyId={}, applyBy={}", event.getApplyId(), event.getApplyBy());
    }
}
```

### 16.2 UserService 新增方法

```java
/**
 * 根据角色编码获取用户ID列表
 * 
 * @param roleCode 角色编码
 * @return 用户ID列表
 */
List<Long> getUserIdsByRoleCode(String roleCode);
```

**实现示例**：
```java
@Override
public List<Long> getUserIdsByRoleCode(String roleCode) {
    return baseMapper.selectList(
        new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getRoleCode, roleCode)
            .eq(UserEntity::getStatus, StatusConstants.NORMAL)
    ).stream()
     .map(UserEntity::getId)
     .collect(Collectors.toList());
}
```

### 16.3 MessageService 接口说明

**假设现有接口**（如不存在需要创建）：

```java
/**
 * 发送站内消息给单个用户
 * 
 * @param userId 用户ID
 * @param title 消息标题
 * @param content 消息内容
 * @param linkUrl 跳转链接
 * @param linkParam 链接参数
 */
void sendToUser(Long userId, String title, String content, String linkUrl, Long linkParam);

/**
 * 发送站内消息给多个用户
 * 
 * @param userIds 用户ID列表
 * @param title 消息标题
 * @param content 消息内容
 * @param linkUrl 跳转链接
 * @param linkParam 链接参数
 */
void sendToUsers(List<Long> userIds, String title, String content, String linkUrl, Long linkParam);
```

### 16.4 消息通知渠道

**当前方案**：站内消息

**可选扩展**：
- 短信通知（紧急情况）
- 邮件通知（审核结果）
- 微信消息推送

---

## 17. 部署方案

### 17.1 部署步骤

**阶段1：数据库变更（停机窗口）**

```sql
-- Step 1: 创建取消申请表
CREATE TABLE order_cancel_apply (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id            BIGINT NOT NULL COMMENT '订单ID',
    apply_by            BIGINT NOT NULL COMMENT '申请人ID',
    apply_reason        VARCHAR(500) COMMENT '取消原因（选填）',
    audit_status        TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：1=待审核，2=已通过，3=已驳回',
    audit_by            BIGINT COMMENT '审核人ID',
    audit_reason        VARCHAR(500) COMMENT '审核驳回原因（选填）',
    audit_time          DATETIME COMMENT '审核时间',
    create_time         DATETIME NOT NULL COMMENT '创建时间',
    update_time         DATETIME NOT NULL COMMENT '更新时间',
    create_by           BIGINT COMMENT '创建人ID',
    update_by           BIGINT COMMENT '更新人ID',
    is_deleted          TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    
    KEY idx_order_cancel_apply_order_id (order_id),
    KEY idx_order_cancel_apply_audit_status (audit_status),
    KEY idx_order_cancel_apply_apply_by (apply_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单取消申请表';

-- Step 2: 为订单表添加字段
ALTER TABLE order_main
ADD COLUMN has_pending_cancel_apply TINYINT DEFAULT 0 COMMENT '是否有待审核的取消申请（0=否，1=是）';

-- Step 3: 验证数据
SELECT COUNT(*) FROM order_cancel_apply;  -- 应该为0
SELECT COUNT(*) FROM order_main WHERE has_pending_cancel_apply IS NULL;  -- 应该为0
```

**阶段2：代码部署**

```bash
# 1. 备份当前版本
git tag backup-before-cancel-apply-$(date +%Y%m%d)

# 2. 部署新版本代码
mvn clean package -DskipTests
# 部署到服务器...

# 3. 重启应用
```

**阶段3：验证**

1. 验证数据库连接正常
2. 访问新增接口确认可用
3. 测试订单取消功能

### 17.2 回滚方案

**场景1：代码回滚**

```bash
git checkout backup-before-cancel-apply-YYYYMMDD
mvn clean package -DskipTests
# 重新部署...
```

**场景2：数据库回滚（慎重）**

```sql
-- 删除新增字段
ALTER TABLE order_main DROP COLUMN has_pending_cancel_apply;

-- 删除新增表
DROP TABLE IF EXISTS order_cancel_apply;
```

**建议**：优先回滚代码，保留数据库变更

### 17.3 上线检查清单

**上线前**：
- [ ] DDL脚本已在测试环境验证
- [ ] 代码已通过单元测试和集成测试
- [ ] 前端页面已联调测试
- [ ] 回滚方案已准备

**上线后**：
- [ ] 接口可用性验证通过
- [ ] 订单阶段直接取消功能正常
- [ ] 设计阶段申请流程正常
- [ ] 消息通知发送正常

---

**文档结束**
