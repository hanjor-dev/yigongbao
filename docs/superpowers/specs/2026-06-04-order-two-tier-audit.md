# 订单两级审核系统设计文档

> **创建日期**: 2026-06-04  
> **设计目标**: 基于订单业务类型实现差异化审核流程（单级/两级），通过角色权限控制实现灵活的审核机制  
> **版本**: v1.2

---

## 编码规范约束

### 📖 必读文档

**在开始开发前，必须完整阅读项目编码规范：**

- **文档路径**：`.claude/rules/java-coding-standards.md`
- **文档路径**：`.claude/rules/logging-standards.md`

### ⚠️ 重点强调规范

#### 1. 注释规范（强制）

- ✅ **ServiceImpl 必须添加方法级注释和行级注释**
- ✅ 公共方法必须使用 Javadoc 注释（功能、参数、返回值、异常）
- ✅ 关键业务逻辑必须添加行内注释说明

```java
/**
 * 审核订单（区域管理员/设计管理员）
 * 
 * @param orderId 订单ID
 * @param operatorId 操作人ID
 * @throws BusinessException 订单不存在、权限不足、状态不正确
 */
@Transactional(rollbackFor = Exception.class)
public void auditPass(Long orderId, Long operatorId) {
    // 获取当前用户角色
    String roleCode = getCurrentUserRoleCode(operatorId);
    
    // 根据订单类型和角色判断审核逻辑
    if ("11.3".equals(order.getBusinessType())) {
        // 试用订单：两级审核
        // ...
    }
}
```

#### 2. 日志规范（强制）

- ✅ **Controller 层禁止输出日志**，由 ServiceImpl 负责
- ✅ 关键操作必须记录日志：数据创建、修改、删除、状态变更
- ✅ 异常必须记录日志：ERROR 级别，包含堆栈信息

```java
// ✅ 正确：记录关键操作
log.info("区域审核通过: orderId={}, auditor={}", orderId, currentUserId);

// ✅ 正确：记录异常
log.error("订单审核失败: orderId={}, reason={}", orderId, e.getMessage(), e);

// ❌ 错误：不记录简单查询
// log.info("查询订单: orderId={}", orderId);
```

#### 3. 魔法值规范（强制）

- ❌ **禁止直接使用数字 0/1/2 表示状态**
- ✅ 必须使用常量或枚举

```java
// ❌ 错误：使用魔法值
if (order.getRegionalAuditStatus() == 0) { ... }

// ✅ 正确：使用常量
public static final int AUDIT_STATUS_PENDING = 0;
public static final int AUDIT_STATUS_PASSED = 1;
public static final int AUDIT_STATUS_REJECTED = 2;

if (order.getRegionalAuditStatus() == AUDIT_STATUS_PENDING) { ... }
```

#### 4. 异常处理规范（强制）

- ✅ 优先使用 `ErrorCodeEnum` 抛出业务异常
- ✅ 所有审核方法必须添加 `@Transactional(rollbackFor = Exception.class)`
- ❌ Controller 层禁止 try-catch

```java
// ✅ 正确
throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
throw new BusinessException(ErrorCodeEnum.REGIONAL_AUDIT_PENDING);
```

#### 5. 其他关键规范

- ✅ 使用 `LambdaUpdateWrapper` 进行条件更新（并发控制）
- ✅ 审核状态字段命名：`regionalAuditStatus`, `designAuditStatus`（驼峰命名）
- ✅ 数据库字段命名：`regional_audit_status`, `design_audit_status`（下划线分隔）
- ✅ 优先使用 Hutool 工具类（`StrUtil`, `CollUtil` 等）

---

## 一、需求背景

### 1.1 业务场景

当前系统中，所有订单提交后都由**设计管理员**统一审核。但实际业务中，不同类型的订单需要不同的审核流程：

- **业务订单（11.1）/ 测试订单（11.2）/ 代理订单（11.4）**: 
  - 单级审核：设计管理员审核通过后直接进入设计阶段
  
- **试用订单（11.3）**:
  - 两级审核：先由区域管理员审核，通过后再由设计管理员审核
  - 区域管理员需要评估试用资格、客户背景等因素

### 1.2 核心诉求

1. **不改变状态机结构**: 保持现有的 4 个审核状态不变
2. **基于角色权限控制**: 利用现有角色体系（regional-manager, designer-manager）
3. **审核进度透明**: 前端清晰展示当前审核环节和进度
4. **驳回后精准重审**: 被驳回后回到对应审核环节，已通过的审核仍然有效

---

## 二、角色定义

| 角色ID | 角色编码 | 角色名称 | 账户类型 | 数据权限 | 审核职责 |
|-------|---------|---------|---------|---------|---------|
| 3 | regional-manager | 区域管理员 | 6.2（业务线） | dept（部门级） | 试用订单一级审核 |
| 5 | designer-manager | 设计管理员 | 6.1（企业内部） | all（全部数据） | 所有订单审核（试用订单二级审核）|

---

## 三、数据模型设计

### 3.1 OrderMainEntity 新增字段

```java
/**
 * 区域管理员审核状态：0-未审核，1-已通过，2-已驳回
 * 仅试用订单（businessType=11.3）需要此字段
 */
@TableField("regional_audit_status")
private Integer regionalAuditStatus;

/**
 * 区域管理员审核备注（驳回原因）
 */
@TableField("regional_audit_remark")
private String regionalAuditRemark;

/**
 * 区域管理员审核时间
 */
@TableField("regional_audit_time")
private LocalDateTime regionalAuditTime;

/**
 * 区域管理员审核人ID
 */
@TableField("regional_audit_by")
private Long regionalAuditBy;

/**
 * 设计管理员审核状态：0-未审核，1-已通过，2-已驳回
 * 所有订单都需要此字段
 */
@TableField("design_audit_status")
private Integer designAuditStatus;

/**
 * 设计管理员审核备注（驳回原因）
 */
@TableField("design_audit_remark")
private String designAuditRemark;

/**
 * 设计管理员审核时间
 */
@TableField("design_audit_time")
private LocalDateTime designAuditTime;

/**
 * 设计管理员审核人ID
 */
@TableField("design_audit_by")
private Long designAuditBy;
```

### 3.2 数据库迁移 SQL

```sql
ALTER TABLE order_main
ADD COLUMN regional_audit_status TINYINT DEFAULT NULL 
    COMMENT '区域管理员审核状态：0-未审核，1-已通过，2-已驳回（仅试用订单）',
ADD COLUMN regional_audit_remark VARCHAR(500) DEFAULT NULL 
    COMMENT '区域管理员审核备注',
ADD COLUMN regional_audit_time DATETIME DEFAULT NULL 
    COMMENT '区域管理员审核时间',
ADD COLUMN regional_audit_by BIGINT DEFAULT NULL 
    COMMENT '区域管理员审核人ID',
ADD COLUMN design_audit_status TINYINT DEFAULT 0 
    COMMENT '设计管理员审核状态：0-未审核，1-已通过，2-已驳回',
ADD COLUMN design_audit_remark VARCHAR(500) DEFAULT NULL 
    COMMENT '设计管理员审核备注',
ADD COLUMN design_audit_time DATETIME DEFAULT NULL 
    COMMENT '设计管理员审核时间',
ADD COLUMN design_audit_by BIGINT DEFAULT NULL 
    COMMENT '设计管理员审核人ID';

-- 索引优化（用于审核列表查询）
-- 区域管理员查询索引：包含部门ID用于数据权限过滤
CREATE INDEX idx_order_regional_audit 
    ON order_main(business_type, regional_audit_status, status, operator_dept_id);
-- 设计管理员查询索引
CREATE INDEX idx_order_design_audit 
    ON order_main(design_audit_status, status);
```

---

## 四、业务流程设计

### 4.1 业务/测试/代理订单流程（单级审核）

**适用订单类型**: businessType = '11.1' / '11.2' / '11.4'

```
草稿(1010)
  ↓ [SUBMIT_ORDER] 
  ↓ 初始化：regionalAuditStatus=null, designAuditStatus=0
待审核(1020)
  ↓ [DATA_AUDIT_PASS by designer-manager]
  ↓ 更新：designAuditStatus=1, designAuditTime, designAuditBy
审核通过(1030)
  ↓ 自动推进到设计阶段
待设计(2010)
```

**驳回场景**:
```
待审核(1020)
  ↓ [DATA_AUDIT_REJECT by designer-manager]
  ↓ 更新：designAuditStatus=2, designAuditRemark
审核不通过(1040)
  ↓ [RESUBMIT]
  ↓ 重置：designAuditStatus=0, designAuditRemark=null
待审核(1020)
```

### 4.2 试用订单流程（两级审核）

**适用订单类型**: businessType = '11.3'

```
草稿(1010)
  ↓ [SUBMIT_ORDER]
  ↓ 初始化：regionalAuditStatus=0, designAuditStatus=0
待审核(1020) - 第一级：区域管理员审核
  ↓ [DATA_AUDIT_PASS by regional-manager]
  ↓ 更新：regionalAuditStatus=1, regionalAuditTime, regionalAuditBy
待审核(1020) - 第二级：设计管理员审核
  ↓ [DATA_AUDIT_PASS by designer-manager]
  ↓ 更新：designAuditStatus=1, designAuditTime, designAuditBy
审核通过(1030)
  ↓ 自动推进到设计阶段
待设计(2010)
```

**区域管理员驳回场景**:
```
待审核(1020) [regionalAuditStatus=0]
  ↓ [DATA_AUDIT_REJECT by regional-manager]
  ↓ 更新：regionalAuditStatus=2, regionalAuditRemark
审核不通过(1040)
  ↓ [RESUBMIT]
  ↓ 重置：regionalAuditStatus=0, regionalAuditRemark=null
待审核(1020) - 回到第一级审核
```

**设计管理员驳回场景**:
```
待审核(1020) [regionalAuditStatus=1, designAuditStatus=0]
  ↓ [DATA_AUDIT_REJECT by designer-manager]
  ↓ 更新：designAuditStatus=2, designAuditRemark
审核不通过(1040)
  ↓ [RESUBMIT]
  ↓ 重置：regionalAuditStatus=0（防止修改内容绕过区域审核）
  ↓ 重置：designAuditStatus=0, designAuditRemark=null
待审核(1020) - 回到第一级审核（区域管理员重新审核）
```

**说明**：设计管理员驳回后，业务员可能修改订单的关键业务信息（金额、产品、客户资质等），这些修改可能影响区域管理员的判断。为防止审核绕过风险，重新提交时需要区域管理员重新审核。

---

## 五、权限控制设计

### 5.1 审核权限矩阵

| 角色 | 可审核订单类型 | 前置条件 | 执行动作 |
|-----|---------------|---------|---------|
| regional-manager | 仅 11.3（试用） | regionalAuditStatus=0 | 通过/驳回（一级审核）|
| designer-manager | 全部订单 | 11.3: regionalAuditStatus=1<br>其他: 无前置 | 通过/驳回 |

**角色冲突处理策略**：如果用户同时拥有两个角色，按优先级处理：`designer-manager` > `regional-manager`，直接使用设计管理员权限审核。

### 5.2 权限判断逻辑

**OrderMainServiceImpl.auditPass() 核心逻辑**:

```java
// 获取当前用户角色
Long currentUserId = StpUtil.getLoginIdAsLong();
String roleCode = getCurrentUserRoleCode(currentUserId); // 辅助方法获取角色编码
String businessType = order.getBusinessType();

if ("11.3".equals(businessType)) {
    // 试用订单：两级审核
    if ("regional-manager".equals(roleCode)) {
        // 第一级：区域管理员审核（使用乐观锁防止并发）
        LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderMainEntity::getId, orderId)
               .eq(OrderMainEntity::getRegionalAuditStatus, 0)  // 前置状态必须为待审核
               .eq(OrderMainEntity::getVersion, order.getVersion());
        
        order.setRegionalAuditStatus(1);
        order.setRegionalAuditTime(LocalDateTime.now());
        order.setRegionalAuditBy(currentUserId);
        
        boolean success = update(order, wrapper);
        if (!success) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
        }
        
        log.info("区域审核通过: orderId=, auditor={}", orderId, currentUserId);
        // 保持 PENDING_DATA_AUDIT 状态，等待设计管理员审核
        
    } else if ("designer-manager".equals(roleCode)) {
        // 第二级：设计管理员审核
        if (order.getRegionalAuditStatus() == null || order.getRegionalAuditStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.REGIONAL_AUDIT_PENDING);
        }
        
        LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderMainEntity::getId, orderId)
               .eq(OrderMainEntity::getDesignAuditStatus, 0)
               .eq(OrderMainEntity::getVersion, order.getVersion());
        
        order.setDesignAuditStatus(1);
        order.setDesignAuditTime(LocalDateTime.now());
        order.setDesignAuditBy(currentUserId);
        
        boolean success = update(order, wrapper);
        if (!success) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
        }
        
        log.info("设计审核通过: orderId={}, auditor={}", orderId, currentUserId);
        // 执行状态流转：PENDING_DATA_AUDIT → DATA_AUDIT_PASSED
        flowFacade.executeFlow(orderId, FlowActionEnum.DATA_AUDIT_PASS, operator);
        
    } else {
        throw new BusinessException(ErrorCodeEnum.NO_AUDIT_PERMISSION);
    }
    
} else {
    // 业务/测试/代理订单：单级审核（仅设计管理员）
    if (!"designer-manager".equals(roleCode)) {
        throw new BusinessException(ErrorCodeEnum.NO_AUDIT_PERMISSION);
    }
    
    LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
    wrapper.eq(OrderMainEntity::getId, orderId)
           .eq(OrderMainEntity::getDesignAuditStatus, 0)
           .eq(OrderMainEntity::getVersion, order.getVersion());
    
    order.setDesignAuditStatus(1);
    order.setDesignAuditTime(LocalDateTime.now());
    order.setDesignAuditBy(currentUserId);
    
    boolean success = update(order, wrapper);
    if (!success) {
        throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
    }
    
    log.info("设计审核通过: orderId={}, auditor={}", orderId, currentUserId);
    // 执行状态流转
    flowFacade.executeFlow(orderId, FlowActionEnum.DATA_AUDIT_PASS, operator);
}
```

**OrderMainServiceImpl.auditReject() 核心逻辑**:

```java
Long currentUserId = StpUtil.getLoginIdAsLong();
String roleCode = getCurrentUserRoleCode(currentUserId);
String businessType = order.getBusinessType();

if ("11.3".equals(businessType)) {
    // 试用订单
    if ("regional-manager".equals(roleCode)) {
        // 区域管理员驳回（使用乐观锁）
        LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderMainEntity::getId, orderId)
               .eq(OrderMainEntity::getRegionalAuditStatus, 0)
               .eq(OrderMainEntity::getVersion, order.getVersion());
        
        order.setRegionalAuditStatus(2);
        order.setRegionalAuditRemark(auditRemark);
        order.setRegionalAuditTime(LocalDateTime.now());
        order.setRegionalAuditBy(currentUserId);
        
        boolean success = update(order, wrapper);
        if (!success) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
        }
        
        log.warn("区域审核驳回: orderId={}, auditor={}, reason={}", orderId, currentUserId, auditRemark);
        
    } else if ("designer-manager".equals(roleCode)) {
        // 设计管理员驳回
        if (order.getRegionalAuditStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.REGIONAL_AUDIT_PENDING);
        }
        
        LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderMainEntity::getId, orderId)
               .eq(OrderMainEntity::getDesignAuditStatus, 0)
               .eq(OrderMainEntity::getVersion, order.getVersion());
        
        order.setDesignAuditStatus(2);
        order.setDesignAuditRemark(auditRemark);
        order.setDesignAuditTime(LocalDateTime.now());
        order.setDesignAuditBy(currentUserId);
        
        boolean success = update(order, wrapper);
        if (!success) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
        }
        
        log.warn("设计审核驳回: orderId={}, auditor={}, reason={}", orderId, currentUserId, auditRemark);
    } else {
        throw new BusinessException(ErrorCodeEnum.NO_AUDIT_PERMISSION);
    }
} else {
    // 其他订单：仅设计管理员
    if (!"designer-manager".equals(roleCode)) {
        throw new BusinessException(ErrorCodeEnum.NO_AUDIT_PERMISSION);
    }
    
    LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
    wrapper.eq(OrderMainEntity::getId, orderId)
           .eq(OrderMainEntity::getDesignAuditStatus, 0)
           .eq(OrderMainEntity::getVersion, order.getVersion());
    
    order.setDesignAuditStatus(2);
    order.setDesignAuditRemark(auditRemark);
    order.setDesignAuditTime(LocalDateTime.now());
    order.setDesignAuditBy(currentUserId);
    
    boolean success = update(order, wrapper);
    if (!success) {
        throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
    }
    
    log.warn("设计审核驳回: orderId={}, auditor={}, reason={}", orderId, currentUserId, auditRemark);
}

// 执行状态流转：PENDING_DATA_AUDIT → DATA_AUDIT_REJECTED
flowFacade.executeFlow(orderId, FlowActionEnum.DATA_AUDIT_REJECT, operator);
```

### 5.3 辅助方法

```java
/**
 * 获取当前用户的审核角色编码
 * 优先级：designer-manager > regional-manager
 */
private String getCurrentUserRoleCode(Long userId) {
    // 从会话中获取角色编码（登录时已存入）
    String roleCode = (String) StpUtil.getSession().get("roleCode");
    if (StrUtil.isNotBlank(roleCode)) {
        return roleCode;
    }
    
    // 兜底：查询数据库（生产环境不应走到这里）
    SysUser user = userService.getById(userId);
    if (user == null) {
        throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
    }
    
    SysRole role = roleService.getById(user.getRoleId());
    if (role == null) {
        throw new BusinessException(ErrorCodeEnum.ROLE_NOT_FOUND);
    }
    
    return role.getRoleCode();
}
```

---

## 六、重新提交逻辑

### 6.1 业务规则

- **试用订单**：
  - 区域管理员驳回（regionalAuditStatus=2）：重置区域审核状态，回到第一级审核
  - 设计管理员驳回（designAuditStatus=2）：**同时重置区域和设计审核状态**，回到第一级审核
  - **原因**：业务员重新提交时可能修改订单关键信息（金额、产品、客户资质等），必须由区域管理员重新评估，防止审核绕过风险

- **其他订单**：
  - 设计管理员驳回（designAuditStatus=2）：重置设计审核状态

### 6.2 实现逻辑

```java
@Transactional(rollbackFor = Exception.class)
public void resubmit(Long orderId) {
    OrderMainEntity order = getById(orderId);
    
    // 校验当前状态必须是 DATA_AUDIT_REJECTED
    if (!FlowStatusEnum.DATA_AUDIT_REJECTED.getCode().equals(order.getStatus())) {
        throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
    }
    
    String businessType = order.getBusinessType();
    
    if ("11.3".equals(businessType)) {
        // 试用订单：所有驳回场景都需要重置所有审核状态
        if (order.getRegionalAuditStatus() != null && order.getRegionalAuditStatus() == 2) {
            // 区域管理员驳回 → 重置区域审核相关字段
            order.setRegionalAuditStatus(0);
            order.setRegionalAuditRemark(null);
            order.setRegionalAuditTime(null);
            order.setRegionalAuditBy(null);
            log.info("重新提交订单（区域驳回）: orderId={}, 回到区域审核", orderId);
            
        } else if (order.getDesignAuditStatus() == 2) {
            // 设计管理员驳回 → 重置所有审核相关字段（防止修改内容绕过区域审核）
            order.setRegionalAuditStatus(0);
            order.setRegionalAuditRemark(null);
            order.setRegionalAuditTime(null);
            order.setRegionalAuditBy(null);
            
            order.setDesignAuditStatus(0);
            order.setDesignAuditRemark(null);
            order.setDesignAuditTime(null);
            order.setDesignAuditBy(null);
            log.info("重新提交订单（设计驳回）: orderId={}, 回到区域审核（防止绕过审核）", orderId);
        }
    } else {
        // 其他订单
        if (order.getDesignAuditStatus() == 2) {
            order.setDesignAuditStatus(0);
            order.setDesignAuditRemark(null);
            order.setDesignAuditTime(null);
            order.setDesignAuditBy(null);
            log.info("重新提交订单: orderId={}, 回到设计审核", orderId);
        }
    }
    
    updateById(order);
    
    // 执行状态流转：DATA_AUDIT_REJECTED → PENDING_DATA_AUDIT
    FlowContext context = FlowContext.builder()
        .orderId(orderId)
        .action(FlowActionEnum.RESUBMIT)
        .operator(StpUtil.getLoginIdAsLong())
        .build();
    flowFacade.executeFlow(context);
}
```

---

## 七、前端展示设计（方案B：详细展示）

### 7.1 VO 对象设计

```java
@Data
public class OrderVO {
    // ... 现有字段
    
    /**
     * 审核进度描述（前端直接展示）
     * 如："等待区域管理员审核" / "等待设计管理员审核" / "区域管理员驳回"
     */
    private String auditProgress;
    
    /**
     * 当前审核环节（用于前端判断显示哪些操作按钮）
     * 枚举值：REGIONAL_PENDING / DESIGN_PENDING / PASSED / REGIONAL_REJECTED / DESIGN_REJECTED
     */
    private String auditStage;
    
    /**
     * 区域审核信息（仅试用订单返回）
     */
    private AuditInfo regionalAudit;
    
    /**
     * 设计审核信息
     */
    private AuditInfo designAudit;
}

@Data
public class AuditInfo {
    /**
     * 审核状态：0-未审核，1-已通过，2-已驳回
     */
    private Integer status;
    
    /**
     * 状态描述："待审核" / "已通过" / "已驳回"
     */
    private String statusDesc;
    
    /**
     * 审核人ID
     */
    private Long auditorId;
    
    /**
     * 审核人姓名
     */
    private String auditorName;
    
    /**
     * 审核时间
     */
    private LocalDateTime auditTime;
    
    /**
     * 审核备注（驳回原因）
     */
    private String remark;
}
```

### 7.2 审核进度计算逻辑

```java
/**
 * 计算审核进度描述
 */
private String calculateAuditProgress(OrderMainEntity order) {
    String businessType = order.getBusinessType();
    
    if ("11.3".equals(businessType)) {
        // 试用订单
        Integer regionalStatus = order.getRegionalAuditStatus();
        Integer designStatus = order.getDesignAuditStatus();
        
        if (regionalStatus == null || regionalStatus == 0) {
            return "等待区域管理员审核";
        } else if (regionalStatus == 2) {
            return "区域管理员驳回";
        } else if (regionalStatus == 1 && designStatus == 0) {
            return "等待设计管理员审核";
        } else if (designStatus == 2) {
            return "设计管理员驳回";
        } else if (designStatus == 1) {
            return "数据审核通过";
        }
    } else {
        // 其他订单
        Integer designStatus = order.getDesignAuditStatus();
        
        if (designStatus == 0) {
            return "等待设计管理员审核";
        } else if (designStatus == 2) {
            return "设计管理员驳回";
        } else if (designStatus == 1) {
            return "数据审核通过";
        }
    }
    
    return "未知状态";
}

/**
 * 计算当前审核环节
 */
private String calculateAuditStage(OrderMainEntity order) {
    String businessType = order.getBusinessType();
    
    if ("11.3".equals(businessType)) {
        Integer regionalStatus = order.getRegionalAuditStatus();
        Integer designStatus = order.getDesignAuditStatus();
        
        if (regionalStatus == 2) {
            return "REGIONAL_REJECTED";
        } else if (designStatus == 2) {
            return "DESIGN_REJECTED";
        } else if (regionalStatus == null || regionalStatus == 0) {
            return "REGIONAL_PENDING";
        } else if (regionalStatus == 1 && designStatus == 0) {
            return "DESIGN_PENDING";
        } else if (designStatus == 1) {
            return "PASSED";
        }
    } else {
        Integer designStatus = order.getDesignAuditStatus();
        
        if (designStatus == 2) {
            return "DESIGN_REJECTED";
        } else if (designStatus == 0) {
            return "DESIGN_PENDING";
        } else if (designStatus == 1) {
            return "PASSED";
        }
    }
    
    return "UNKNOWN";
}
```

### 7.3 前端页面展示逻辑

#### 7.3.1 订单列表 - 状态列展示

**试用订单（businessType=11.3）**：

| auditStage | 显示文本 | 颜色 |
|-----------|---------|------|
| REGIONAL_PENDING | 🟡 等待区域管理员审核 | warning |
| DESIGN_PENDING | 🟡 等待设计管理员审核 | warning |
| PASSED | ✅ 数据审核通过 | success |
| REGIONAL_REJECTED | ❌ 区域管理员驳回 | danger |
| DESIGN_REJECTED | ❌ 设计管理员驳回 | danger |

**其他订单（businessType=11.1/11.2/11.4）**：

| auditStage | 显示文本 | 颜色 |
|-----------|---------|------|
| DESIGN_PENDING | 🟡 等待设计管理员审核 | warning |
| PASSED | ✅ 数据审核通过 | success |
| DESIGN_REJECTED | ❌ 设计管理员驳回 | danger |

#### 7.3.2 订单详情页 - 审核进度时间轴

**试用订单显示**：

```html
<el-timeline>
  <el-timeline-item timestamp="2026-06-04 09:30" placement="top">
    <h4>订单提交</h4>
    <p>业务员：张三</p>
  </el-timeline-item>
  
  <el-timeline-item 
    :icon="regionalAudit.status === 1 ? 'el-icon-check' : 'el-icon-close'"
    :type="regionalAudit.status === 1 ? 'success' : 'danger'"
    timestamp="2026-06-04 10:30">
    <h4>区域管理员审核</h4>
    <p>审核人：李四（区域管理员）</p>
    <p>结果：{{ regionalAudit.statusDesc }}</p>
    <p v-if="regionalAudit.remark">备注：{{ regionalAudit.remark }}</p>
  </el-timeline-item>
  
  <el-timeline-item 
    v-if="regionalAudit.status === 1"
    :icon="designAudit.status === 1 ? 'el-icon-check' : 'el-icon-close'"
    :type="designAudit.status === 1 ? 'success' : 'danger'"
    timestamp="2026-06-04 14:20">
    <h4>设计管理员审核</h4>
    <p>审核人：王五（设计管理员）</p>
    <p>结果：{{ designAudit.statusDesc }}</p>
    <p v-if="designAudit.remark">备注：{{ designAudit.remark }}</p>
  </el-timeline-item>
</el-timeline>
```

**其他订单显示**：

```html
<el-timeline>
  <el-timeline-item timestamp="2026-06-04 09:30" placement="top">
    <h4>订单提交</h4>
    <p>业务员：张三</p>
  </el-timeline-item>
  
  <el-timeline-item 
    :icon="designAudit.status === 1 ? 'el-icon-check' : 'el-icon-close'"
    :type="designAudit.status === 1 ? 'success' : 'danger'"
    timestamp="2026-06-04 14:20">
    <h4>设计管理员审核</h4>
    <p>审核人：王五（设计管理员）</p>
    <p>结果：{{ designAudit.statusDesc }}</p>
    <p v-if="designAudit.remark">备注：{{ designAudit.remark }}</p>
  </el-timeline-item>
</el-timeline>
```

### 7.4 审核列表查询逻辑

#### 7.4.1 区域管理员审核列表

```java
/**
 * 区域管理员查询待审核订单列表
 */
public IPage<OrderVO> listRegionalAuditOrders(OrderQueryDTO dto) {
    // 从会话获取部门ID（登录时已存入，避免每次查库）
    Long deptId = (Long) StpUtil.getSession().get("deptId");
    if (deptId == null) {
        throw new BusinessException(ErrorCodeEnum.SESSION_EXPIRED);
    }
    
    LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(OrderMainEntity::getBusinessType, "11.3")  // 仅试用订单
           .eq(OrderMainEntity::getStatus, FlowStatusEnum.PENDING_DATA_AUDIT.getCode())
           .eq(OrderMainEntity::getRegionalAuditStatus, 0)  // 待区域审核
           .eq(OrderMainEntity::getOperatorDeptId, deptId)  // 数据权限：本部门
           .eq(OrderMainEntity::getIsDeleted, 0)
           .orderByDesc(OrderMainEntity::getCreateTime);
    
    IPage<OrderMainEntity> page = page(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
    return page.convert(this::convertToVO);
}
```

#### 7.4.2 设计管理员审核列表

```java
/**
 * 设计管理员查询待审核订单列表
 */
public IPage<OrderVO> listDesignAuditOrders(OrderQueryDTO dto) {
    LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
    
    wrapper.eq(OrderMainEntity::getStatus, FlowStatusEnum.PENDING_DATA_AUDIT.getCode())
           .eq(OrderMainEntity::getDesignAuditStatus, 0)  // 待设计审核
           .eq(OrderMainEntity::getIsDeleted, 0);
    
    // 试用订单：必须区域审核已通过
    wrapper.and(w -> w.ne(OrderMainEntity::getBusinessType, "11.3")
                      .or()
                      .apply("business_type = '11.3' AND regional_audit_status = 1"));
    
    wrapper.orderByDesc(OrderMainEntity::getCreateTime);
    
    IPage<OrderMainEntity> page = page(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
    return page.convert(this::convertToVO);
}
```

---

## 八、错误码扩展

### 8.1 ErrorCodeEnum 新增枚举值

```java
public enum ErrorCodeEnum {
    // ... 现有错误码
    
    /**
     * 审核相关错误码（650-659）
     */
    ORDER_ALREADY_AUDITED(650, "该订单已审核，无法重复操作"),
    REGIONAL_AUDIT_PENDING(651, "区域管理员尚未审核，无法操作"),
    REGIONAL_AUDIT_REJECTED(652, "区域管理员已驳回该订单"),
    NO_AUDIT_PERMISSION(653, "无权限审核该订单"),
    AUDIT_STATUS_ERROR(654, "审核状态异常"),
    ORDER_STATUS_ERROR(655, "订单状态不正确，无法执行此操作"),
    SESSION_EXPIRED(656, "会话已过期，请重新登录"),
    USER_NOT_FOUND(657, "用户不存在"),
    ROLE_NOT_FOUND(658, "角色不存在");
}
```

---

## 九、事务管理与并发控制

### 9.1 事务管理

所有审核操作必须使用 `@Transactional` 注解保证事务一致性：

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderMainServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements IOrderService {
    
    @Transactional(rollbackFor = Exception.class)
    public void auditPass(Long orderId, Long operatorId) {
        // 1. 更新审核状态字段
        // 2. 调用 flowFacade.executeFlow()
        // 两步必须在同一事务中，任一失败则全部回滚
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void auditReject(Long orderId, String remark, Long operatorId) {
        // 同上
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long orderId) {
        // 同上
    }
}
```

### 9.2 并发控制策略

使用 **乐观锁 + 前置状态检查** 防止并发审核：

```java
// 方案：MyBatis-Plus UpdateWrapper 前置条件检查
LambdaUpdateWrapper<OrderMainEntity> wrapper = new LambdaUpdateWrapper<>();
wrapper.eq(OrderMainEntity::getId, orderId)
       .eq(OrderMainEntity::getRegionalAuditStatus, 0)  // 前置状态必须为待审核
       .eq(OrderMainEntity::getVersion, order.getVersion());  // 乐观锁

order.setRegionalAuditStatus(1);
boolean success = update(order, wrapper);
if (!success) {
    throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
}
```

**说明**：
- `version` 字段由 MyBatis-Plus 自动管理，更新时自动递增
- 前置状态检查确保只有待审核状态才能执行审核操作
- 两者结合可有效防止并发重复审核

---

## 十、会话管理

### 10.1 登录时存储用户信息

在用户登录成功后，需要将角色编码和部门ID存入 SaToken 会话，避免后续每次查询数据库：

```java
@Service
public class AuthServiceImpl implements IAuthService {
    
    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 验证用户名密码
        SysUser user = validateUser(dto);
        
        // 2. 查询用户角色
        SysRole role = roleService.getById(user.getRoleId());
        
        // 3. 登录并存储会话信息
        StpUtil.login(user.getId());
        StpUtil.getSession().set("roleCode", role.getRoleCode());
        StpUtil.getSession().set("deptId", user.getDeptId());
        StpUtil.getSession().set("userName", user.getUserName());
        
        // 4. 返回登录信息
        return buildLoginVO(user, role);
    }
}
```

### 10.2 审核时获取会话信息

```java
// 获取角色编码（避免查库）
String roleCode = (String) StpUtil.getSession().get("roleCode");

// 获取部门ID（用于数据权限过滤）
Long deptId = (Long) StpUtil.getSession().get("deptId");
```

**优势**：
- 减少数据库查询，提升性能
- 会话信息由 SaToken 管理，自动处理过期和清理
- 登录一次，全局可用

---

## 十一、实施要点

### 11.1 核心原则

1. **不改变状态机结构**：保持 FlowStatusEnum 的 4 个审核状态不变
2. **不修改 Flow 模块**：FlowFacade、FlowActionEnum 保持不变
3. **不影响现有功能**：非试用订单的审核流程完全兼容
4. **事务一致性优先**：审核状态更新和流程流转必须在同一事务中
5. **并发控制严格**：使用乐观锁防止重复审核

### 11.2 关键实现文件

| 文件 | 修改内容 | 关键点 |
|------|---------|-------|
| OrderMainEntity.java | 新增 8 个审核字段 | 确保与 BaseEntity 正确继承 |
| OrderVO.java | 新增审核进度展示字段 | auditProgress, auditStage, AuditInfo |
| OrderMainServiceImpl.java | 修改 auditPass/auditReject/resubmit | 添加 @Transactional，使用 UpdateWrapper |
| OrderConvert.java | 新增审核信息转换逻辑 | 计算 auditProgress 和 auditStage |
| AuthServiceImpl.java | 登录时存储会话信息 | roleCode, deptId 存入 Session |
| ErrorCodeEnum.java | 新增 9 个审核相关错误码 | 651-658 |
| sql/migration/*.sql | 数据库字段迁移脚本 | 包含索引和历史数据处理 |

### 9.3 数据迁移注意事项

```sql
-- 1. 为所有已通过审核的订单（包括设计阶段、生产阶段等）设置设计审核状态为已通过
UPDATE order_main 
SET design_audit_status = 1
WHERE status >= 1030 AND status != 1040 AND is_deleted = 0;

-- 2. 为已驳回的订单设置设计审核状态为已驳回
UPDATE order_main 
SET design_audit_status = 2
WHERE status = 1040 AND is_deleted = 0;

-- 3. 为待审核的订单设置设计审核状态为待审核
UPDATE order_main 
SET design_audit_status = 0
WHERE status = 1020 AND is_deleted = 0;

-- 4. 试用订单：如果已通过审核，区域审核也标记为已通过
UPDATE order_main 
SET regional_audit_status = 1
WHERE business_type = '11.3' 
  AND status >= 1030 
  AND status != 1040
  AND is_deleted = 0;

-- 5. 试用订单：如果待审核，区域审核标记为待审核
UPDATE order_main 
SET regional_audit_status = 0
WHERE business_type = '11.3' 
  AND status = 1020
  AND is_deleted = 0;
```

---

## 十、测试验证

### 10.1 单元测试

**测试类**: `OrderMainServiceImplTest`

**测试场景**:

1. **试用订单 - 正常流程**
   - 区域管理员审核通过 → 设计管理员审核通过
   - 验证审核状态字段正确更新
   - 验证状态流转正确

2. **试用订单 - 区域驳回场景**
   - 区域管理员驳回 → 重新提交 → 回到区域审核
   - 验证 regionalAuditStatus 重置为 0

3. **试用订单 - 设计驳回场景**
   - 区域通过 → 设计驳回 → 重新提交 → 回到设计审核
   - 验证 regionalAuditStatus 保持为 1
   - 验证 designAuditStatus 重置为 0

4. **其他订单 - 正常流程**
   - 设计管理员审核通过
   - 验证 regionalAuditStatus 为 null

5. **权限控制**
   - 区域管理员审核非试用订单 → 抛出权限异常
   - 设计管理员审核未经区域审核的试用订单 → 抛出异常
   - 重复审核 → 抛出异常

### 10.2 集成测试

**测试流程**:

1. 创建试用订单 → 提交
2. 区域管理员登录 → 审核列表看到订单 → 审核通过
3. 设计管理员登录 → 审核列表看到订单 → 审核通过
4. 验证订单进入设计阶段

### 10.3 前端验证

**验证要点**:

1. 订单列表显示正确的审核进度（等待区域/设计管理员审核）
2. 审核详情页时间轴显示完整
3. 不同角色看到的审核列表正确过滤
4. 审核按钮根据角色和订单类型正确显示/隐藏

---

## 十一、技术风险与应对

### 11.1 数据一致性风险

**风险**: 审核状态字段与订单状态不一致

**应对**:
- 在 auditPass/auditReject 方法中使用事务（@Transactional）
- 审核状态字段更新和状态流转在同一事务中完成
- 添加数据库约束检查

### 11.2 并发审核风险

**风险**: 多个审核人同时审核同一订单

**应对**:
- 使用乐观锁（OrderMainEntity 已有 version 字段）
- 审核前检查审核状态，已审核则抛出异常
- 审核接口幂等性保证

### 11.3 性能风险

**风险**: 审核列表查询增加复杂条件，可能影响性能

**应对**:
- 添加复合索引：`idx_order_regional_audit(business_type, regional_audit_status, status)`
- 添加索引：`idx_order_design_audit(design_audit_status, status)`
- 分页查询，限制每页数量

---

## 十二、总结

### 12.1 设计优势

1. ✅ **最小侵入性**: 不改变状态机结构，不修改 Flow 模块
2. ✅ **灵活扩展**: 通过字段组合支持未来更复杂的审核流程
3. ✅ **权限天然隔离**: 基于现有角色体系，无需新增权限配置
4. ✅ **审核进度透明**: 前端清晰展示当前审核环节
5. ✅ **可追溯性强**: 记录每个审核环节的完整信息

### 12.2 实施步骤

1. 数据库迁移：添加 8 个审核字段和 2 个索引
2. Entity 层：OrderMainEntity 新增字段
3. VO 层：OrderVO 新增审核进度字段，AuditInfo 新增
4. Service 层：修改 auditPass/auditReject/resubmit 方法
5. Controller 层：新增区域审核接口（复用现有接口，通过角色区分）
6. 转换层：OrderConvert 新增审核信息转换逻辑
7. 枚举层：ErrorCodeEnum 新增错误码
8. 单元测试：覆盖所有审核场景
9. 集成测试：验证完整流程
10. 前端联调：审核列表、详情页、时间轴展示

---

## 十三、修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| v1.0 | 2026-06-04 | 初版创建 | Kiro AI |
| v1.1 | 2026-06-04 | 修复关键问题：<br>1. 修正角色识别逻辑（使用正确API）<br>2. 添加事务管理和并发控制<br>3. 优化数据迁移SQL脚本<br>4. 完善索引设计（添加operator_dept_id）<br>5. 添加会话管理说明<br>6. 拆分错误码定义<br>7. 优化查询性能（使用会话缓存） | Kiro AI |
| v1.2 | 2026-06-04 | 调整重新提交逻辑（采用方案A）：<br>1. 设计管理员驳回后，重置所有审核状态<br>2. 回到区域审核环节，防止修改内容绕过审核<br>3. 更新业务流程说明和实现代码<br>**原因**：防止业务员在重新提交时修改关键业务信息（金额、产品、客户资质等）绕过区域管理员审核 | Kiro AI |

---

**文档版本**: v1.2  
**创建日期**: 2026-06-04  
**最后更新**: 2026-06-04  
**作者**: Kiro AI  
**审核状态**: 待审核
