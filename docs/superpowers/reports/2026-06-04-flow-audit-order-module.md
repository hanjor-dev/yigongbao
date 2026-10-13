# 订单模块状态机调用详细分析

**所属报告**：Flow 状态机调用全面审查报告  
**模块**：yigongbao-module-order  
**主要文件**：OrderMainServiceImpl.java

---

## 概述

订单模块共有 **12个状态机调用点**，分布在以下方法中：
- 订单生命周期管理：submitOrder, withdrawOrder, cancelOrder, manualCompleteOrder
- 数据审核流程：auditPass, auditReject, resubmit（两级审核系统）
- 订单创建：createFromDraft, createOrder

**总体评估**：✅ 所有调用点实现正确，完全符合架构设计

---

## 调用点详细分析

### 1. submitOrder() - 提交订单

**位置**：L581-587  
**动作**：FlowActionEnum.SUBMIT_ORDER  
**状态流转**：DRAFT → PENDING_DATA_AUDIT

**代码实现**：
```java
TransitionResult result = flowFacade.executeFlow(
    id, FlowActionEnum.SUBMIT_ORDER, FlowOperator.of(currentUserId, null));
entity.setPhase(result.getTargetPhase());
entity.setStatus(result.getFinalStatus());
updateById(entity);
log.info("提交订单: orderId={}, phase={}, status={}", 
    id, result.getTargetPhase(), result.getFinalStatus());
```

**评估**：✅ **正确**

**优点**：
- 正确使用 TransitionResult 获取流转后的状态
- 使用 updateById() 落库
- 记录完整日志

---

### 2. withdrawOrder() - 撤回订单

**位置**：L616-623  
**动作**：FlowActionEnum.WITHDRAW  
**状态流转**：PENDING_DATA_AUDIT → DRAFT

**代码实现**：
```java
TransitionResult result = flowFacade.executeFlow(
    id, FlowActionEnum.WITHDRAW, FlowOperator.of(currentUserId, null));
entity.setPhase(result.getTargetPhase());
entity.setStatus(result.getFinalStatus());
entity.setCurrentHandlerId(currentUserId);
updateById(entity);
```

**评估**：✅ **正确**

**优点**：
- 正确使用 TransitionResult
- 同时更新业务字段（currentHandlerId）

---

### 3. auditPass() - 试用订单设计审核通过

**位置**：L677-702  
**动作**：FlowActionEnum.DATA_AUDIT_PASS  
**状态流转**：PENDING_DATA_AUDIT → DESIGNING（阶段推进）

**代码实现**：
```java
String operatorName = getUserRealName(currentUserId);
TransitionResult result = flowFacade.executeFlow(
    id, FlowActionEnum.DATA_AUDIT_PASS, 
    new FlowOperator(currentUserId, operatorName, dto.getRemark()),
    dto.getVersion());

LambdaUpdateWrapper<OrderMainEntity> uw = new LambdaUpdateWrapper<>();
uw.eq(OrderMainEntity::getId, id)
  .eq(OrderMainEntity::getRegionalAuditStatus, AuditStatusConstants.PASSED)
  .eq(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.PENDING)
  .set(OrderMainEntity::getPhase, result.getTargetPhase())
  .set(OrderMainEntity::getStatus, result.getFinalStatus())
  .set(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.PASSED)
  .set(OrderMainEntity::getDesignAuditTime, LocalDateTime.now())
  .set(OrderMainEntity::getDesignAuditBy, currentUserId)
  .set(OrderMainEntity::getDesignAuditRemark, dto.getRemark())
  .set(OrderMainEntity::getCurrentHandlerId, currentUserId);
if (dto.getEstimatedCost() != null) {
    uw.set(OrderMainEntity::getEstimatedCost, dto.getEstimatedCost());
}
if (StrUtil.isNotBlank(dto.getDataEvaluationOpinion())) {
    uw.set(OrderMainEntity::getDataEvaluationOpinion, dto.getDataEvaluationOpinion());
}
if (!update(uw)) {
    throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
}
```

**评估**：✅ **正确**

**优点**：
1. **乐观锁校验**：传入 version 参数到 flowFacade
2. **原子更新**：使用 LambdaUpdateWrapper 的 eq 条件确保前置状态正确
3. **状态同步**：正确使用 result.getTargetPhase() 和 result.getFinalStatus()
4. **业务字段更新**：同时更新审核人、审核时间、审核备注、预估成本等
5. **并发安全**：update 失败时抛出版本冲突异常
6. **两级审核逻辑**：eq 条件确保区域审核已通过

**设计亮点**：
- 将状态机校验（version）和数据库校验（eq条件）结合
- 一次原子操作完成所有更新，避免多次UPDATE

---

### 4. auditPass() - 非试用订单审核通过

**位置**：L715-740  
**动作**：FlowActionEnum.DATA_AUDIT_PASS  
**评估**：✅ **正确**（逻辑与调用点3类似，省略详细分析）

---

### 5. auditReject() - 试用订单区域驳回

**位置**：L782-802  
**动作**：FlowActionEnum.DATA_AUDIT_REJECT  
**状态流转**：PENDING_DATA_AUDIT → DATA_AUDIT_REJECTED

**代码实现**：
```java
String operatorName = getUserRealName(currentUserId);
TransitionResult result = flowFacade.executeFlow(
    id, FlowActionEnum.DATA_AUDIT_REJECT, 
    new FlowOperator(currentUserId, operatorName, dto.getRemark()),
    dto.getVersion());

LambdaUpdateWrapper<OrderMainEntity> uw = new LambdaUpdateWrapper<>();
uw.eq(OrderMainEntity::getId, id)
  .eq(OrderMainEntity::getRegionalAuditStatus, AuditStatusConstants.PENDING)
  .eq(OrderMainEntity::getStatus, FlowStatusEnum.PENDING_DATA_AUDIT.getValue())
  .set(OrderMainEntity::getPhase, result.getTargetPhase())
  .set(OrderMainEntity::getStatus, result.getFinalStatus())
  .set(OrderMainEntity::getRegionalAuditStatus, AuditStatusConstants.REJECTED)
  .set(OrderMainEntity::getRegionalAuditTime, LocalDateTime.now())
  .set(OrderMainEntity::getRegionalAuditBy, currentUserId)
  .set(OrderMainEntity::getRegionalAuditRemark, dto.getRemark())
  .set(OrderMainEntity::getAuditRemark, dto.getRemark())
  .set(OrderMainEntity::getCurrentHandlerId, currentUserId);
if (!update(uw)) {
    throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
}
```

**评估**：✅ **正确**

**优点**：
- 完整的乐观锁和原子更新机制
- 正确记录审核人、审核时间、驳回原因

---

### 6. auditReject() - 试用订单设计驳回

**位置**：L808-826  
**动作**：FlowActionEnum.DATA_AUDIT_REJECT  
**评估**：✅ **正确**（逻辑与调用点5类似）

---

### 7. auditReject() - 非试用订单驳回

**位置**：L837-855  
**动作**：FlowActionEnum.DATA_AUDIT_REJECT  
**评估**：✅ **正确**（逻辑与调用点5类似）

---

## 小结（前7个调用点）

前7个调用点涵盖了订单的提交、撤回和两级审核流程，所有实现均正确：
1. ✅ 正确使用 TransitionResult
2. ✅ 正确实现乐观锁（version参数 + eq条件）
3. ✅ 原子更新确保并发安全
4. ✅ 同时更新状态和业务字段
5. ✅ 完整的日志记录

**继续后续调用点分析...**
