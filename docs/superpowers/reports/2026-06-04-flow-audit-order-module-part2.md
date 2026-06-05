# 订单模块状态机调用分析（续）

**接上文**：订单模块调用点8-12的详细分析

---

### 8. resubmit() - 重新提交订单

**位置**：L888-955  
**动作**：FlowActionEnum.RESUBMIT  
**状态流转**：DATA_AUDIT_REJECTED → PENDING_DATA_AUDIT

**代码实现**：
```java
String operatorName = getUserRealName(currentUserId);
TransitionResult result = flowFacade.executeFlow(
    id, FlowActionEnum.RESUBMIT, 
    new FlowOperator(currentUserId, operatorName, "重新提交"));

boolean updated = false;
if (isTrialOrder) {
    // 试用订单：先尝试设计驳回路径（重置所有审核字段）
    LambdaUpdateWrapper<OrderMainEntity> designRejectUw = new LambdaUpdateWrapper<>();
    designRejectUw.eq(OrderMainEntity::getId, id)
          .eq(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.REJECTED)
          .set(OrderMainEntity::getRegionalAuditStatus, null)
          .set(OrderMainEntity::getRegionalAuditTime, null)
          .set(OrderMainEntity::getRegionalAuditBy, null)
          .set(OrderMainEntity::getRegionalAuditRemark, null)
          .set(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.PENDING)
          .set(OrderMainEntity::getDesignAuditTime, null)
          .set(OrderMainEntity::getDesignAuditBy, null)
          .set(OrderMainEntity::getDesignAuditRemark, null)
          .set(OrderMainEntity::getPhase, result.getTargetPhase())
          .set(OrderMainEntity::getStatus, result.getFinalStatus())
          .set(OrderMainEntity::getCurrentHandlerId, currentUserId);
    updated = update(designRejectUw);
    
    if (!updated) {
        // 设计驳回路径失败，尝试区域驳回路径（只重置区域审核字段）
        LambdaUpdateWrapper<OrderMainEntity> regionalRejectUw = new LambdaUpdateWrapper<>();
        regionalRejectUw.eq(OrderMainEntity::getId, id)
              .eq(OrderMainEntity::getRegionalAuditStatus, AuditStatusConstants.REJECTED)
              .set(OrderMainEntity::getRegionalAuditStatus, null)
              .set(OrderMainEntity::getRegionalAuditTime, null)
              .set(OrderMainEntity::getRegionalAuditBy, null)
              .set(OrderMainEntity::getRegionalAuditRemark, null)
              .set(OrderMainEntity::getPhase, result.getTargetPhase())
              .set(OrderMainEntity::getStatus, result.getFinalStatus())
              .set(OrderMainEntity::getCurrentHandlerId, currentUserId);
        updated = update(regionalRejectUw);
    }
} else {
    // 非试用订单：只重置设计审核字段
    LambdaUpdateWrapper<OrderMainEntity> uw = new LambdaUpdateWrapper<>();
    uw.eq(OrderMainEntity::getId, id)
      .eq(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.REJECTED)
      .set(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.PENDING)
      .set(OrderMainEntity::getDesignAuditTime, null)
      .set(OrderMainEntity::getDesignAuditBy, null)
      .set(OrderMainEntity::getDesignAuditRemark, null)
      .set(OrderMainEntity::getPhase, result.getTargetPhase())
      .set(OrderMainEntity::getStatus, result.getFinalStatus())
      .set(OrderMainEntity::getCurrentHandlerId, currentUserId);
    updated = update(uw);
}

if (!updated) {
    throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
}
```

**评估**：✅ **正确**

**设计亮点**：
1. **多路径尝试**：通过原子更新的eq条件判断驳回级别
   - 试用订单：先尝试设计驳回路径，失败则尝试区域驳回路径
   - 非试用订单：只有一个路径
2. **原子操作**：每个update都是原子的，eq条件失败则返回false
3. **审核字段重置**：
   - 设计驳回：重置所有审核字段（区域+设计）
   - 区域驳回：只重置区域审核字段
4. **状态同步**：正确使用 result.getTargetPhase() 和 result.getFinalStatus()

**为什么使用多路径尝试？**
- 重新提交时，前端不知道是区域驳回还是设计驳回
- 通过原子更新的eq条件自动判断驳回级别
- 避免了先查询再更新的并发问题

---

### 9. cancelOrder() - 取消订单

**位置**：L976-982  
**动作**：FlowActionEnum.CANCEL  
**状态流转**：任意状态 → CANCELLED

**代码实现**：
```java
String operatorName = getUserRealName(currentUserId);
TransitionResult result = flowFacade.executeFlow(
    id, FlowActionEnum.CANCEL, 
    new FlowOperator(currentUserId, operatorName, null));
entity.setPhase(result.getTargetPhase());
entity.setStatus(result.getFinalStatus());
updateById(entity);
log.info("取消订单: orderId={}, phase={}, status={}", 
    id, result.getTargetPhase(), result.getFinalStatus());
```

**评估**：✅ **正确**

**优点**：
- 简洁明了
- 正确使用 TransitionResult
- 完整日志记录

---

### 10. manualCompleteOrder() - 手动完成订单

**位置**：L1017-1024  
**动作**：FlowActionEnum.COMPLETE  
**状态流转**：DESIGN_COMPLETED → COMPLETED

**代码实现**：
```java
String operatorName = getUserRealName(currentUserId);
TransitionResult result = flowFacade.executeFlow(
    orderId, FlowActionEnum.COMPLETE, 
    new FlowOperator(currentUserId, operatorName, "手动完成"));
entity.setPhase(result.getTargetPhase());
entity.setStatus(result.getFinalStatus());
updateById(entity);
log.info("手动完成订单: orderId={}, {} -> {}, operator={}",
    orderId, FlowStatusEnum.DESIGN_COMPLETED.getValue(), 
    result.getFinalStatus(), currentUserId);
```

**评估**：✅ **正确**

**场景说明**：仅用于不需要实体交付的订单（needsPhysicalDelivery=0），设计完成后直接手动标记为完成。

---

### 11. createFromDraft() - 从草稿创建订单

**位置**：L1116  
**动作**：FlowActionEnum.CREATE  
**特殊性**：仅记录历史，不改变状态

**代码实现**：
```java
// 订单创建时已设置状态
order.setPhase(FlowPhaseEnum.ORDER.getValue());
order.setStatus(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
save(order);

// CREATE 动作仅记录历史
flowFacade.executeFlow(orderId, FlowActionEnum.CREATE,
    new FlowOperator(draft.getOperatorId(), operatorName, "从草稿创建"));
```

**评估**：✅ **正确**

**设计说明**：
- CREATE 动作不改变状态（参见 FlowStateMachineServiceImpl L115-122）
- 订单状态在创建时已设置，无需获取 TransitionResult
- 只用于记录状态历史，便于追溯订单创建来源

---

### 12. createOrder() - 直接创建订单

**位置**：L1227-1228  
**动作**：FlowActionEnum.CREATE  
**评估**：✅ **正确**（逻辑与调用点11相同）

**代码实现**：
```java
order.setPhase(FlowPhaseEnum.ORDER.getValue());
order.setStatus(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
save(order);

flowFacade.executeFlow(orderId, FlowActionEnum.CREATE,
    new FlowOperator(currentUserId, currentUser.getRealName(), "直提创建"));
```

---

## 订单模块总结

### 统计数据

| 调用场景 | 调用点数量 | 评估结果 |
|---------|-----------|---------|
| 订单生命周期 | 4 | ✅ 全部正确 |
| 数据审核（两级） | 6 | ✅ 全部正确 |
| 订单创建 | 2 | ✅ 全部正确 |
| **总计** | **12** | **✅ 100%正确** |

### 实现亮点

1. **乐观锁机制**
   - FlowFacade 层：version 参数校验
   - 数据库层：LambdaUpdateWrapper 的 eq 条件
   - 双重保障，确保并发安全

2. **原子更新**
   - 所有审核操作使用 LambdaUpdateWrapper
   - eq 条件确保前置状态正确
   - 一次UPDATE完成所有字段更新

3. **两级审核逻辑**
   - 试用订单：区域管理员 → 设计管理员
   - 非试用订单：设计管理员直接审核
   - 重新提交时通过多路径尝试自动判断驳回级别

4. **状态同步一致性**
   - 所有调用点均正确使用 result.getTargetPhase() 和 result.getFinalStatus()
   - 状态机计算和数据库更新完全同步

### 无需改进项

订单模块的所有状态机调用点实现质量很高，无需改进。
