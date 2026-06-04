# 订单两级审核系统设计文档

> **创建日期**: 2026-06-04  
> **设计目标**: 基于订单业务类型实现差异化审核流程（单级/两级），通过角色权限控制实现灵活的审核机制  
> **版本**: v1.0

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
CREATE INDEX idx_order_regional_audit 
    ON order_main(business_type, regional_audit_status, status);
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
  ↓ 重置：designAuditStatus=0, designAuditRemark=null
  ↓ 保持：regionalAuditStatus=1（区域审核仍然有效）
待审核(1020) - 直接回到第二级审核
```

---

## 五、权限控制设计

### 5.1 审核权限矩阵

| 角色 | 可审核订单类型 | 前置条件 | 执行动作 |
|-----|---------------|---------|---------|
| regional-manager | 仅 11.3（试用） | regionalAuditStatus=0 | 通过/驳回（一级审核）|
| designer-manager | 全部订单 | 11.3: regionalAuditStatus=1<br>其他: 无前置 | 通过/驳回 |

### 5.2 权限判断逻辑

**OrderMainServiceImpl.auditPass() 核心逻辑**:

```java
String currentRoleCode = StpUtil.getLoginIdAsString(); // 从SaToken获取角色
String businessType = order.getBusinessType();

if ("11.3".equals(businessType)) {
    // 试用订单：两级审核
    if ("regional-manager".equals(currentRoleCode)) {
        // 第一级：区域管理员审核
        if (order.getRegionalAuditStatus() != null && order.getRegionalAuditStatus() != 0) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
        }
        order.setRegionalAuditStatus(1);
        order.setRegionalAuditTime(LocalDateTime.now());
        order.setRegionalAuditBy(currentUserId);
        // 保持 PENDING_DATA_AUDIT 状态，等待设计管理员审核
        updateById(order);
        
    } else if ("designer-manager".equals(currentRoleCode)) {
        // 第二级：设计管理员审核
        if (order.getRegionalAuditStatus() == null || order.getRegionalAuditStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.REGIONAL_AUDIT_NOT_PASSED);
        }
        if (order.getDesignAuditStatus() != 0) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
        }
        order.setDesignAuditStatus(1);
        order.setDesignAuditTime(LocalDateTime.now());
        order.setDesignAuditBy(currentUserId);
        // 执行状态流转：PENDING_DATA_AUDIT → DATA_AUDIT_PASSED
        flowFacade.executeFlow(orderId, FlowActionEnum.DATA_AUDIT_PASS, operator);
        
    } else {
        throw new BusinessException(ErrorCodeEnum.NO_AUDIT_PERMISSION);
    }
    
} else {
    // 业务/测试/代理订单：单级审核（仅设计管理员）
    if (!"designer-manager".equals(currentRoleCode)) {
        throw new BusinessException(ErrorCodeEnum.NO_AUDIT_PERMISSION);
    }
    if (order.getDesignAuditStatus() != 0) {
        throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
    }
    order.setDesignAuditStatus(1);
    order.setDesignAuditTime(LocalDateTime.now());
    order.setDesignAuditBy(currentUserId);
    // 执行状态流转
    flowFacade.executeFlow(orderId, FlowActionEnum.DATA_AUDIT_PASS, operator);
}
```

**OrderMainServiceImpl.auditReject() 核心逻辑**:

```java
String currentRoleCode = StpUtil.getLoginIdAsString();
String businessType = order.getBusinessType();

if ("11.3".equals(businessType)) {
    // 试用订单
    if ("regional-manager".equals(currentRoleCode)) {
        // 区域管理员驳回
        if (order.getRegionalAuditStatus() != 0) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
        }
        order.setRegionalAuditStatus(2);
        order.setRegionalAuditRemark(auditRemark);
        order.setRegionalAuditTime(LocalDateTime.now());
        order.setRegionalAuditBy(currentUserId);
        
    } else if ("designer-manager".equals(currentRoleCode)) {
        // 设计管理员驳回
        if (order.getRegionalAuditStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.REGIONAL_AUDIT_NOT_PASSED);
        }
        if (order.getDesignAuditStatus() != 0) {
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_AUDITED);
        }
        order.setDesignAuditStatus(2);
        order.setDesignAuditRemark(auditRemark);
        order.setDesignAuditTime(LocalDateTime.now());
        order.setDesignAuditBy(currentUserId);
    }
} else {
    // 其他订单：仅设计管理员
    if (!"designer-manager".equals(currentRoleCode)) {
        throw new BusinessException(ErrorCodeEnum.NO_AUDIT_PERMISSION);
    }
    order.setDesignAuditStatus(2);
    order.setDesignAuditRemark(auditRemark);
    order.setDesignAuditTime(LocalDateTime.now());
    order.setDesignAuditBy(currentUserId);
}

// 执行状态流转：PENDING_DATA_AUDIT → DATA_AUDIT_REJECTED
flowFacade.executeFlow(orderId, FlowActionEnum.DATA_AUDIT_REJECT, operator);
```

---

## 六、重新提交逻辑

### 6.1 业务规则

- **试用订单**：
  - 区域管理员驳回（regionalAuditStatus=2）：重置 regionalAuditStatus=0，回到第一级审核
  - 设计管理员驳回（designAuditStatus=2）：重置 designAuditStatus=0，保持 regionalAuditStatus=1，直接回到第二级审核

- **其他订单**：
  - 设计管理员驳回（designAuditStatus=2）：重置 designAuditStatus=0

### 6.2 实现逻辑

```java
public void resubmit(Long orderId) {
    OrderMainEntity order = getById(orderId);
    
    // 校验当前状态必须是 DATA_AUDIT_REJECTED
    if (!FlowStatusEnum.DATA_AUDIT_REJECTED.getCode().equals(order.getStatus())) {
        throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
    }
    
    String businessType = order.getBusinessType();
    
    if ("11.3".equals(businessType)) {
        // 试用订单
        if (order.getRegionalAuditStatus() != null && order.getRegionalAuditStatus() == 2) {
            // 区域管理员驳回 → 重置区域审核状态
            order.setRegionalAuditStatus(0);
            order.setRegionalAuditRemark(null);
            order.setRegionalAuditTime(null);
            order.setRegionalAuditBy(null);
        } else if (order.getDesignAuditStatus() == 2) {
            // 设计管理员驳回 → 只重置设计审核状态
            order.setDesignAuditStatus(0);
            order.setDesignAuditRemark(null);
            order.setDesignAuditTime(null);
            order.setDesignAuditBy(null);
            // regionalAuditStatus 保持 =1，无需重新区域审核
        }
    } else {
        // 其他订单
        if (order.getDesignAuditStatus() == 2) {
            order.setDesignAuditStatus(0);
            order.setDesignAuditRemark(null);
            order.setDesignAuditTime(null);
            order.setDesignAuditBy(null);
        }
    }
    
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
    LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
    
    // 筛选条件
    wrapper.eq(OrderMainEntity::getBusinessType, "11.3")  // 仅试用订单
           .eq(OrderMainEntity::getStatus, FlowStatusEnum.PENDING_DATA_AUDIT.getCode())
           .eq(OrderMainEntity::getRegionalAuditStatus, 0)  // 待区域审核
           .eq(OrderMainEntity::getIsDeleted, 0)
           .orderByDesc(OrderMainEntity::getCreateTime);
    
    // 数据权限：仅查询本部门的订单
    Long currentUserId = StpUtil.getLoginIdAsLong();
    SysUser currentUser = userService.getById(currentUserId);
    wrapper.eq(OrderMainEntity::getOperatorDeptId, currentUser.getDeptId());
    
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
    REGIONAL_AUDIT_NOT_PASSED(651, "区域管理员尚未审核通过，无法操作"),
    NO_AUDIT_PERMISSION(652, "无权限审核该订单"),
    AUDIT_STATUS_ERROR(653, "审核状态异常"),
    ORDER_STATUS_ERROR(654, "订单状态不正确，无法执行此操作");
}
```

---

## 九、实施要点

### 9.1 核心原则

1. **不改变状态机结构**：保持 FlowStatusEnum 的 4 个审核状态不变
2. **不修改 Flow 模块**：FlowFacade、FlowActionEnum 保持不变
3. **不影响现有功能**：非试用订单的审核流程完全兼容

### 9.2 关键实现文件

| 文件 | 修改内容 |
|------|---------|
| OrderMainEntity.java | 新增 8 个审核字段 |
| OrderVO.java | 新增审核进度展示字段 |
| OrderMainServiceImpl.java | 修改 auditPass/auditReject/resubmit 方法 |
| OrderConvert.java | 新增审核信息转换逻辑 |
| ErrorCodeEnum.java | 新增 5 个审核相关错误码 |
| sql/migration/*.sql | 数据库字段迁移脚本 |

### 9.3 数据迁移注意事项

```sql
-- 为现有订单初始化审核状态字段
UPDATE order_main 
SET design_audit_status = CASE 
    WHEN status = 1030 THEN 1  -- 已审核通过
    WHEN status = 1040 THEN 2  -- 已驳回
    ELSE 0  -- 待审核
END
WHERE status IN (1020, 1030, 1040);

-- 试用订单：如果已审核通过，默认认为区域审核和设计审核都已通过
UPDATE order_main 
SET regional_audit_status = 1
WHERE business_type = '11.3' 
  AND status = 1030;
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

**文档版本**: v1.0  
**创建日期**: 2026-06-04  
**作者**: Kiro AI  
**审核状态**: 待审核
