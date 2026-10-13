# 订单修改申请功能实施计划

**文档版本**：v1.0  
**创建日期**：2026-06-08  
**负责人**：开发团队  
**预计工期**：5 个工作日

---

## 一、需求背景

### 1.1 业务需求

**原始需求描述：**
> 修改订单，要求是5或者10分钟内允许业务员修改，设计师不得修改，而且超过这个时间的，就需要申请，比如设计管理员确认他的申请后才会生效，操作逻辑应该是他点击修改后点击提交，在时间段内直接生效，在超过规定时间后，点击提交会提示允许修改时间超限，是否提交至设计部门确认，修改内容10分钟后清空。在确认前，订单内容不变，但是修改后的内容只保存10分钟后清理，在确认后，内容直接替换生效。

**核心业务规则：**
1. **时间窗口控制**：订单创建后 N 分钟内（5或10分钟，可配置）可直接修改
2. **角色权限控制**：仅业务员可修改订单，设计师无权修改
3. **超时申请机制**：超过时间窗口后需提交申请，由设计管理员审核
4. **暂存期限**：修改内容暂存 10 分钟，超时自动清理
5. **审核生效**：审核通过后内容直接替换订单数据

### 1.2 业务价值

- ✅ 防止订单进入设计阶段后随意修改，保证设计工作的稳定性
- ✅ 给予业务员一定的纠错时间窗口，提升业务效率
- ✅ 通过审核机制确保重大修改有设计部门确认，降低业务风险
- ✅ 自动清理机制防止脏数据积累

---

## 二、现状分析

### 2.1 现有修改订单功能

**已有接口：**
- `PUT /order/modify/{orderId}/direct` - 直接修改（差量更新）
- `PUT /order/modify/{orderId}/full` - 全量修改（前端传完整数据）
- `POST /order/modify/{orderId}/logs` - 查询修改留痕记录

**已有表结构：**
- `order_main` - 订单主表（含 `version` 乐观锁）
- `order_item` - 订单明细表
- `order_file` - 订单文件关联表
- `order_modification_log` - 订单修改留痕表

**存在问题：**
- ❌ 无时间窗口判断机制
- ❌ 无角色权限控制（业务员 vs 设计师）
- ❌ 无申请审核流程
- ❌ 无修改内容暂存机制
- ❌ 无定时清理机制

### 2.2 技术现状

**技术栈：**
- Spring Boot 3.x + MyBatis Plus 3.5.8
- SaToken 1.37.0（权限认证）
- MySQL 8.0
- Redis（缓存 + 分布式锁）

**相关配置：**
- 动态配置存储：`sys_config` 表
- 枚举映射：`mybatis-plus.type-enums-package`
- 事务控制：`@Transactional(rollbackFor = Exception.class)`

---

## 三、解决方案概述

### 3.1 核心设计思路

**方案选型：新增修改申请表 + 差异计算**

```
时间窗口内
    ↓
直接修改（现有逻辑）
    ↓
立即生效

超过时间窗口
    ↓
创建申请记录
    ↓
暂存修改内容（完整JSON + 差异JSON）
    ↓
设计管理员审核
    ├─ 通过 → 执行修改逻辑
    └─ 驳回 → 通知申请人

定时任务（每5分钟）
    ↓
清理过期申请（超过10分钟未审核）
```

### 3.2 关键技术点

| 技术点 | 实现方式 |
|--------|----------|
| **时间窗口判断** | `ChronoUnit.MINUTES.between(订单创建时间, 当前时间)` |
| **角色权限控制** | 通过 SaToken 获取用户角色，判断 `roleCode` |
| **差异计算** | 后端对比当前订单数据与提交数据，生成结构化差异JSON |
| **内容暂存** | 存储完整JSON（用于执行）+ 差异JSON（用于展示） |
| **定时清理** | `@Scheduled` 定时任务，更新过期申请状态 |
| **审核通知** | 可选：WebSocket 推送 / 短信 / 站内消息 |

### 3.3 数据流转图

```
用户提交修改
    ↓
判断角色（设计师直接拒绝）
    ↓
判断时间窗口
    ├─ 在窗口内 → 调用现有 modifyOrderFull() → 立即生效
    └─ 超窗口 → 计算差异 → 创建申请记录 → 等待审核
                                    ↓
                            设计管理员审核
                                    ├─ 通过 → 读取 JSON → 执行修改 → 更新状态
                                    └─ 驳回 → 记录原因 → 通知申请人
```

---

## 四、数据库设计

### 4.1 新增表：order_modification_apply（订单修改申请表）

```sql
-- ============================================================
-- 订单修改申请表
-- 用于暂存超时修改申请的内容，支持审核流程
-- ============================================================
DROP TABLE IF EXISTS order_modification_apply;
CREATE TABLE order_modification_apply (
    -- ==================== 主键 ====================
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- ==================== 订单信息 ====================
    order_id        BIGINT          NOT NULL COMMENT '订单ID',
    order_code      VARCHAR(50)     COMMENT '订单编号（冗余）',
    
    -- ==================== 申请类型 ====================
    apply_type      VARCHAR(20)     NOT NULL COMMENT '申请类型：FULL=全量修改',
    
    -- ==================== 修改内容（双JSON存储）====================
    modification_content TEXT        NOT NULL COMMENT '修改内容（完整OrderModifyFullDTO的JSON，用于审核通过后执行）',
    modification_diff    TEXT        COMMENT '变更差异（结构化差异JSON，用于审核界面展示）',
    
    -- ==================== 申请信息 ====================
    apply_user_id   BIGINT          NOT NULL COMMENT '申请人ID',
    apply_user_name VARCHAR(100)    COMMENT '申请人姓名（冗余）',
    apply_time      DATETIME        NOT NULL COMMENT '申请时间',
    expire_time     DATETIME        NOT NULL COMMENT '过期时间（申请时间 + 10分钟）',
    
    -- ==================== 审核信息 ====================
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0=待审核，1=已通过，2=已驳回，3=已过期',
    audit_user_id   BIGINT          COMMENT '审核人ID',
    audit_user_name VARCHAR(100)    COMMENT '审核人姓名（冗余）',
    audit_time      DATETIME        COMMENT '审核时间',
    audit_remark    VARCHAR(500)    COMMENT '审核备注（驳回原因）',
    
    -- ==================== 公共字段 ====================
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          COMMENT '创建人ID',
    update_by       BIGINT          COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    
    PRIMARY KEY (id),
    KEY idx_order_modification_apply_order_id (order_id),
    KEY idx_order_modification_apply_status (status),
    KEY idx_order_modification_apply_expire_time (expire_time),
    KEY idx_order_modification_apply_apply_time (apply_time),
    KEY idx_order_modification_apply_apply_user (apply_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单修改申请表';
```

**字段设计说明：**

| 字段 | 说明 | 业务价值 |
|------|------|----------|
| `modification_content` | 完整的 `OrderModifyFullDTO` JSON | 审核通过后直接反序列化执行修改 |
| `modification_diff` | 结构化差异 JSON | 审核界面清晰展示"改了什么" |
| `expire_time` | 过期时间（申请时间+10分钟） | 定时任务根据此字段清理过期申请 |
| `status` | 0=待审核，1=已通过，2=已驳回，3=已过期 | 支持完整的申请生命周期管理 |

### 4.2 配置项（sys_config 表）

```sql
-- 订单修改时间窗口配置（分钟）
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, status) 
VALUES (
    'order.modify.time.window', 
    '订单修改时间窗口', 
    '10', 
    'number', 
    'order', 
    '订单创建后允许直接修改的时间窗口，单位：分钟。超过此时间需提交申请审核。', 
    1, 
    1
);

-- 修改申请暂存期限配置（分钟）
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, status) 
VALUES (
    'order.modify.apply.expire.minutes', 
    '修改申请暂存期限', 
    '10', 
    'number', 
    'order', 
    '修改申请的暂存期限，单位：分钟。超过此时间未审核的申请将自动过期。', 
    1, 
    1
);
```

### 4.3 枚举定义

**新增枚举类：**

```java
// 申请状态枚举
public enum ApplyStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    EXPIRED(3, "已过期");
    
    private final Integer code;
    private final String desc;
}

// 申请类型枚举
public enum ApplyTypeEnum {
    FULL("FULL", "全量修改");
    
    private final String code;
    private final String desc;
}
```

---

## 五、接口设计

### 5.1 修改现有接口：全量修改订单

**接口路径：** `PUT /order/modify/{orderId}/full`

**调整内容：** 增加时间窗口和角色权限判断

**请求参数：** `OrderModifyFullDTO`（保持不变）

**响应数据：**
```json
// 成功（时间窗口内）
{
  "code": 200,
  "message": "修改成功",
  "data": null
}

// 失败（超过时间窗口）
{
  "code": 40001,
  "message": "订单修改时间已超限，请提交申请",
  "data": {
    "timeWindow": 10,
    "elapsedMinutes": 15,
    "needApply": true
  }
}

// 失败（角色无权限）
{
  "code": 403,
  "message": "设计师无权修改订单",
  "data": null
}
```

**业务逻辑调整：**
1. 判断当前用户角色（设计师直接拒绝）
2. 计算订单创建时间到当前时间的分钟数
3. 如果在时间窗口内，执行现有修改逻辑
4. 如果超过时间窗口，返回特殊错误码，前端引导用户提交申请

---

### 5.2 新增接口：提交修改申请

**接口路径：** `POST /order/modify/{orderId}/apply`

**请求参数：**
```json
{
  "orderId": 123,
  "modifyData": {
    // 完整的 OrderModifyFullDTO 内容
    "patientName": "李四",
    "hospitalId": 2,
    "items": [...]
  }
}
```

**响应数据：**
```json
{
  "code": 200,
  "message": "申请已提交，等待设计管理员审核",
  "data": {
    "applyId": 456,
    "expireTime": "2024-06-08 10:40:00"
  }
}
```

**业务逻辑：**
1. 查询当前订单数据
2. 计算差异（调用 `OrderDiffCalculator`）
3. 创建申请记录，存储 `modification_content` + `modification_diff`
4. 设置过期时间 = 当前时间 + 10分钟
5. 返回申请ID和过期时间

---

### 5.3 新增接口：查询修改申请列表（设计管理员）

**接口路径：** `POST /order/modify/apply/list`

**请求参数：**
```json
{
  "status": 0,  // 0=待审核，1=已通过，2=已驳回，3=已过期，null=全部
  "orderCode": "ORD-2024-001234",  // 可选
  "applyUserName": "张三",  // 可选
  "applyTimeStart": "2024-06-01 00:00:00",  // 可选
  "applyTimeEnd": "2024-06-08 23:59:59",  // 可选
  "pageNum": 1,
  "pageSize": 20
}
```

**响应数据：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "applyId": 456,
        "orderId": 123,
        "orderCode": "ORD-2024-001234",
        "applyUserName": "张三",
        "applyTime": "2024-06-08 10:30:00",
        "expireTime": "2024-06-08 10:40:00",
        "status": 0,
        "statusDesc": "待审核",
        "changeCount": 3,  // 变更字段数量
        "changeSummary": "患者姓名、医院、是否加急"  // 变更摘要
      }
    ],
    "total": 50,
    "size": 20,
    "current": 1,
    "pages": 3
  }
}
```

---

### 5.4 新增接口：查询申请详情

**接口路径：** `GET /order/modify/apply/{applyId}`

**响应数据：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "applyId": 456,
    "orderId": 123,
    "orderCode": "ORD-2024-001234",
    "applyUserName": "张三",
    "applyTime": "2024-06-08 10:30:00",
    "expireTime": "2024-06-08 10:40:00",
    "status": 0,
    "statusDesc": "待审核",
    
    // 变更差异（用于审核界面展示）
    "diff": {
      "basicInfo": [
        {
          "fieldLabel": "患者姓名",
          "oldValue": "张三",
          "newValue": "李四"
        },
        {
          "fieldLabel": "医院",
          "oldDisplay": "北京协和医院",
          "newDisplay": "上海瑞金医院"
        }
      ],
      "items": {
        "changeType": "MODIFIED",
        "added": [
          {
            "projectName": "髋关节重建",
            "bodyPartName": "髋关节"
          }
        ],
        "deleted": [],
        "modified": []
      }
    }
  }
}
```

---

### 5.5 新增接口：审核修改申请

**接口路径：** `PUT /order/modify/apply/{applyId}/audit`

**请求参数：**
```json
{
  "result": 1,  // 1=通过，2=驳回
  "remark": "审核意见或驳回原因"
}
```

**响应数据：**
```json
{
  "code": 200,
  "message": "审核成功",
  "data": null
}
```

**业务逻辑：**
1. 查询申请记录，校验状态（必须是待审核）
2. 校验是否过期
3. 如果审核通过：
   - 读取 `modification_content` JSON
   - 反序列化为 `OrderModifyFullDTO`
   - 调用现有 `modifyOrderFull()` 方法执行修改
   - 更新申请状态为"已通过"
4. 如果审核驳回：
   - 更新申请状态为"已驳回"
   - 记录驳回原因
5. 记录审核人和审核时间

---

### 5.6 新增接口：查询我的申请记录（业务员）

**接口路径：** `POST /order/modify/apply/my-list`

**请求参数：**
```json
{
  "status": null,  // 可选
  "pageNum": 1,
  "pageSize": 20
}
```

**响应数据：** 与 5.3 类似，只返回当前用户的申请记录

---

### 5.7 接口权限配置

| 接口 | 角色要求 |
|------|----------|
| `PUT /order/modify/{orderId}/full` | 业务员（salesman） |
| `POST /order/modify/{orderId}/apply` | 业务员（salesman） |
| `POST /order/modify/apply/list` | 设计管理员（designer-manager） |
| `GET /order/modify/apply/{applyId}` | 设计管理员（designer-manager） |
| `PUT /order/modify/apply/{applyId}/audit` | 设计管理员（designer-manager） |
| `POST /order/modify/apply/my-list` | 业务员（salesman） |

---

## 六、核心实现逻辑

### 6.1 时间窗口判断工具类

**文件位置：** `yigongbao-module-order/utils/OrderModifyTimeWindowChecker.java`

```java
@Component
@RequiredArgsConstructor
public class OrderModifyTimeWindowChecker {
    
    private final SystemConfigService configService;
    
    /**
     * 判断订单是否在修改时间窗口内
     * @param orderCreateTime 订单创建时间
     * @return true=在窗口内可直接修改，false=需要申请
     */
    public boolean isWithinTimeWindow(LocalDateTime orderCreateTime) {
        // 从配置表读取时间窗口（分钟）
        Integer timeWindow = configService.getConfigValueAsInt(
            SystemConfigKeyEnum.ORDER_MODIFY_TIME_WINDOW.getKey(), 10
        );
        
        // 计算时间差（分钟）
        long elapsedMinutes = ChronoUnit.MINUTES.between(orderCreateTime, LocalDateTime.now());
        
        return elapsedMinutes <= timeWindow;
    }
    
    /**
     * 获取已过时间（分钟）
     */
    public long getElapsedMinutes(LocalDateTime orderCreateTime) {
        return ChronoUnit.MINUTES.between(orderCreateTime, LocalDateTime.now());
    }
}
```

**配置键枚举：**
```java
// SystemConfigKeyEnum 中新增
ORDER_MODIFY_TIME_WINDOW("order.modify.time.window", "订单修改时间窗口"),
ORDER_MODIFY_APPLY_EXPIRE_MINUTES("order.modify.apply.expire.minutes", "修改申请暂存期限");
```

---

### 6.2 角色权限校验

**文件位置：** `yigongbao-module-order/service/impl/OrderModifyApplyServiceImpl.java`

```java
/**
 * 校验当前用户是否有权限修改订单
 * @throws BusinessException 设计师无权限
 */
private void checkModifyPermission() {
    Long userId = StpUtil.getLoginIdAsLong();
    UserEntity user = userService.getById(userId);
    
    if (user == null) {
        throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
    }
    
    // 设计师不得修改订单
    if (RoleCodeEnum.DESIGNER.getCode().equals(user.getRoleCode())) {
        throw new BusinessException(ErrorCodeEnum.NO_PERMISSION, "设计师无权修改订单");
    }
    
    // 仅业务员可修改
    if (!RoleCodeEnum.SALESMAN.getCode().equals(user.getRoleCode())) {
        throw new BusinessException(ErrorCodeEnum.NO_PERMISSION, "仅业务员可修改订单");
    }
}

/**
 * 校验审核权限（设计管理员）
 */
private void checkAuditPermission() {
    Long userId = StpUtil.getLoginIdAsLong();
    UserEntity user = userService.getById(userId);
    
    if (user == null) {
        throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
    }
    
    if (!RoleCodeEnum.DESIGNER_MANAGER.getCode().equals(user.getRoleCode())) {
        throw new BusinessException(ErrorCodeEnum.NO_PERMISSION, "仅设计管理员可审核申请");
    }
}
```

---

### 6.3 差异计算核心逻辑

**文件位置：** `yigongbao-module-order/convert/OrderDiffCalculator.java`

```java
@Component
@RequiredArgsConstructor
public class OrderDiffCalculator {
    
    private final OrgService orgService;
    private final HospitalDeptService hospitalDeptService;
    
    /**
     * 计算订单修改差异
     */
    public OrderModificationDiff calculateDiff(
            OrderEntity currentOrder,
            List<OrderItemEntity> currentItems,
            List<OrderFileEntity> currentFiles,
            OrderModifyFullDTO modifyDto) {
        
        OrderModificationDiff diff = new OrderModificationDiff();
        
        // 1. 基础信息差异
        diff.setBasicInfo(calculateBasicInfoDiff(currentOrder, modifyDto));
        
        // 2. 订单项差异
        diff.setItems(calculateItemsDiff(currentItems, modifyDto.getItems()));
        
        // 3. 文件差异（简化统计）
        diff.setFiles(calculateFilesDiff(currentFiles, modifyDto));
        
        return diff;
    }
    
    /**
     * 计算基础信息字段差异
     */
    private List<FieldDiff> calculateBasicInfoDiff(OrderEntity current, OrderModifyFullDTO dto) {
        List<FieldDiff> diffs = new ArrayList<>();
        
        // 患者姓名
        addDiffIfChanged(diffs, "patientName", "患者姓名", 
            current.getPatientName(), dto.getPatientName());
        
        // 医院（需要查询显示名称）
        if (!Objects.equals(current.getHospitalId(), dto.getHospitalId())) {
            String oldDisplay = current.getHospitalName();
            String newDisplay = orgService.getById(dto.getHospitalId()).getOrgName();
            diffs.add(new FieldDiff("hospitalId", "医院",
                String.valueOf(current.getHospitalId()), 
                String.valueOf(dto.getHospitalId()),
                oldDisplay, newDisplay));
        }
        
        // 是否加急
        addDiffIfChanged(diffs, "isUrgent", "是否加急",
            current.getIsUrgent(), dto.getIsUrgent(),
            current.getIsUrgent() == 1 ? "是" : "否",
            dto.getIsUrgent() == 1 ? "是" : "否");
        
        // ... 其他字段类似处理
        
        return diffs;
    }
    
    /**
     * 辅助方法：如果值变化则添加差异记录
     */
    private void addDiffIfChanged(List<FieldDiff> diffs, String fieldName, String fieldLabel,
                                   Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            diffs.add(new FieldDiff(fieldName, fieldLabel, 
                String.valueOf(oldValue), String.valueOf(newValue)));
        }
    }
    
    private void addDiffIfChanged(List<FieldDiff> diffs, String fieldName, String fieldLabel,
                                   Object oldValue, Object newValue,
                                   String oldDisplay, String newDisplay) {
        if (!Objects.equals(oldValue, newValue)) {
            diffs.add(new FieldDiff(fieldName, fieldLabel,
                String.valueOf(oldValue), String.valueOf(newValue),
                oldDisplay, newDisplay));
        }
    }
}
```

**差异数据结构：**
```java
@Data
public class OrderModificationDiff {
    private List<FieldDiff> basicInfo;  // 基础信息差异
    private ItemsDiff items;  // 订单项差异
    private FilesDiff files;  // 文件差异
}

@Data
@AllArgsConstructor
public class FieldDiff {
    private String fieldName;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
    private String oldDisplay;  // 可选，用于显示友好名称
    private String newDisplay;  // 可选
}
```

---

### 6.4 修改申请服务核心方法

**文件位置：** `yigongbao-module-order/service/impl/OrderModifyApplyServiceImpl.java`

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderModifyApplyServiceImpl implements OrderModifyApplyService {
    
    private final OrderModifyApplyMapper applyMapper;
    private final OrderModifyFullService orderModifyFullService;
    private final OrderDiffCalculator diffCalculator;
    private final OrderModifyTimeWindowChecker timeWindowChecker;
    private final SystemConfigService configService;
    
    /**
     * 提交修改申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitApply(Long orderId, OrderModifyFullDTO dto) {
        // 1. 校验权限
        checkModifyPermission();
        
        // 2. 查询订单数据
        OrderEntity order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        
        // 3. 校验是否存在待审核的申请（同一订单同一时间只能有一个待审核申请）
        LambdaQueryWrapper<OrderModificationApplyEntity> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(OrderModificationApplyEntity::getOrderId, orderId)
                   .eq(OrderModificationApplyEntity::getStatus, ApplyStatusEnum.PENDING.getCode());
        Long existingCount = applyMapper.selectCount(checkWrapper);
        if (existingCount > 0) {
            throw new BusinessException(ErrorCodeEnum.APPLY_ALREADY_EXISTS);
        }
        
        // 4. 计算差异
        List<OrderItemEntity> currentItems = orderItemService.listByOrderId(orderId);
        List<OrderFileEntity> currentFiles = orderFileService.listByOrderId(orderId);
        OrderModificationDiff diff = diffCalculator.calculateDiff(order, currentItems, currentFiles, dto);
        
        // 5. 创建申请记录
        OrderModificationApplyEntity apply = new OrderModificationApplyEntity();
        apply.setOrderId(orderId);
        apply.setOrderCode(order.getOrderCode());
        apply.setApplyType(ApplyTypeEnum.FULL.getCode());
        apply.setModificationContent(JSONUtil.toJsonStr(dto));  // 完整JSON
        apply.setModificationDiff(JSONUtil.toJsonStr(diff));  // 差异JSON
        apply.setApplyUserId(StpUtil.getLoginIdAsLong());
        apply.setApplyUserName(StpUtil.getTokenSession().getString("realName"));
        apply.setApplyTime(LocalDateTime.now());
        
        // 5. 设置过期时间
        Integer expireMinutes = configService.getConfigValueAsInt(
            SystemConfigKeyEnum.ORDER_MODIFY_APPLY_EXPIRE_MINUTES.getKey(), 10
        );
        apply.setExpireTime(LocalDateTime.now().plusMinutes(expireMinutes));
        apply.setStatus(ApplyStatusEnum.PENDING.getCode());
        
        applyMapper.insert(apply);
        
        log.info("创建订单修改申请: applyId={}, orderId={}, userId={}, expireTime={}", 
            apply.getId(), orderId, apply.getApplyUserId(), apply.getExpireTime());
        
        return apply.getId();
    }
    
    /**
     * 审核修改申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditApply(Long applyId, AuditApplyDTO dto) {
        // 1. 校验审核权限
        checkAuditPermission();
        
        // 2. 查询申请记录
        OrderModificationApplyEntity apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        
        // 2. 校验状态
        if (!ApplyStatusEnum.PENDING.getCode().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.APPLY_STATUS_INVALID, "申请已处理");
        }
        
        // 3. 校验是否过期
        if (LocalDateTime.now().isAfter(apply.getExpireTime())) {
            throw new BusinessException(ErrorCodeEnum.APPLY_EXPIRED, "申请已过期");
        }
        
        // 4. 审核通过：执行修改
        if (dto.getResult() == 1) {
            // 反序列化修改内容
            String jsonContent = apply.getModificationContent();
            OrderModifyFullDTO modifyDto = JSONUtil.toBean(jsonContent, OrderModifyFullDTO.class);
            
            // 调用现有修改逻辑
            orderModifyFullService.modifyOrderFull(apply.getOrderId(), modifyDto);
            
            apply.setStatus(ApplyStatusEnum.APPROVED.getCode());
            log.info("审核通过并执行修改: applyId={}, orderId={}, auditor={}", 
                applyId, apply.getOrderId(), StpUtil.getLoginIdAsLong());
        }
        // 5. 审核驳回
        else if (dto.getResult() == 2) {
            apply.setStatus(ApplyStatusEnum.REJECTED.getCode());
            apply.setAuditRemark(dto.getRemark());
            log.info("审核驳回: applyId={}, orderId={}, reason={}", 
                applyId, apply.getOrderId(), dto.getRemark());
        }
        
        // 6. 更新审核信息
        apply.setAuditUserId(StpUtil.getLoginIdAsLong());
        apply.setAuditUserName(StpUtil.getTokenSession().getString("realName"));
        apply.setAuditTime(LocalDateTime.now());
        applyMapper.updateById(apply);
    }
}
```

---

## 七、前端展示设计

### 7.1 修改订单流程（业务员）

**场景1：时间窗口内直接修改**

```
用户点击"修改订单"
    ↓
进入修改表单页面
    ↓
用户修改数据并点击"提交"
    ↓
调用 PUT /order/modify/{orderId}/full
    ↓
后端判断在时间窗口内
    ↓
返回 code=200
    ↓
前端提示"修改成功"，刷新订单详情
```

**场景2：超时需要申请**

```
用户点击"修改订单"
    ↓
进入修改表单页面
    ↓
用户修改数据并点击"提交"
    ↓
调用 PUT /order/modify/{orderId}/full
    ↓
后端判断超过时间窗口
    ↓
返回 code=40001（特殊错误码）
    ↓
前端弹窗提示：
    "订单修改时间已超限（已过 15 分钟），
     是否提交至设计部门确认？
     修改内容将暂存 10 分钟。"
    [取消] [确认提交]
    ↓
用户点击"确认提交"
    ↓
调用 POST /order/modify/{orderId}/apply
    ↓
返回申请ID和过期时间
    ↓
前端提示："申请已提交，等待设计管理员审核"
    ↓
跳转到"我的申请记录"页面
```

### 7.2 审核界面设计（设计管理员）

**页面布局：**

```
┌─────────────────────────────────────────────────────────┐
│ 订单修改审核                                              │
├─────────────────────────────────────────────────────────┤
│ 【申请信息】                                              │
│ 订单编号：ORD-2024-001234     申请人：张三（业务员）     │
│ 申请时间：2024-06-08 10:30:00                            │
│ 剩余时间：⏱ 8 分钟 30 秒（倒计时）                      │
├─────────────────────────────────────────────────────────┤
│ 【基础信息变更】                                          │
│ ┌────────┬──────────────┬──────────────┐              │
│ │ 字段   │ 修改前       │ 修改后       │              │
│ ├────────┼──────────────┼──────────────┤              │
│ │ 患者姓名│ 张三         │ 李四 🔴      │              │
│ │ 医院   │ 北京协和医院 │ 上海瑞金医院 🔴 │           │
│ │ 是否加急│ 否           │ 是 🔴        │              │
│ └────────┴──────────────┴──────────────┘              │
│                                                          │
│ 【重建项目变更】                                          │
│ ➕ 新增：髋关节重建（左侧）                               │
│ ➖ 删除：膝关节重建                                       │
├─────────────────────────────────────────────────────────┤
│              ┌──────────┬─────────────┐                │
│              │ [驳回 ]  │ [ 通过 ]    │                │
│              └──────────┴─────────────┘                │
└─────────────────────────────────────────────────────────┘
```

**关键UI元素：**
- 🔴 红色标记：表示该字段发生变更
- ⏱ 倒计时：实时显示剩余时间，过期自动禁用审核按钮
- 表格对比：清晰展示修改前后的值

### 7.3 我的申请记录（业务员）

**列表字段：**
- 申请时间
- 订单编号
- 变更摘要（如：患者姓名、医院、是否加急）
- 状态（🟡待审核/🟢已通过/🔴已驳回/⚫已过期）
- 剩余时间（待审核时显示倒计时）

---

## 八、定时任务设计

### 8.1 过期申请清理任务

**文件位置：** `yigongbao-module-order/task/OrderModifyApplyCleanTask.java`

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderModifyApplyCleanTask {
    
    private final OrderModifyApplyService applyService;
    
    /**
     * 清理过期的修改申请
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanExpiredApplications() {
        log.info("定时任务开始: 清理过期修改申请");
        
        try {
            LambdaUpdateWrapper<OrderModificationApplyEntity> wrapper = new LambdaUpdateWrapper<>();
            wrapper.set(OrderModificationApplyEntity::getStatus, ApplyStatusEnum.EXPIRED.getCode())
                   .eq(OrderModificationApplyEntity::getStatus, ApplyStatusEnum.PENDING.getCode())
                   .lt(OrderModificationApplyEntity::getExpireTime, LocalDateTime.now());
            
            int count = applyService.update(wrapper);
            log.info("定时任务完成: 清理过期申请数量={}", count);
            
        } catch (Exception e) {
            log.error("定时任务失败: 清理过期申请异常", e);
        }
    }
}
```

**任务配置：**
- 执行频率：每5分钟（`0 */5 * * * ?`）
- 执行逻辑：将 `expire_time < NOW() AND status = 0` 的记录更新为 `status = 3`

---

## 九、测试计划

### 9.1 单元测试

#### 9.1.1 时间窗口判断测试

**测试类：** `OrderModifyTimeWindowCheckerTest`

**测试用例：**
- ✅ 订单创建5分钟内，返回 true
- ✅ 订单创建10分钟时，返回 true（边界值）
- ✅ 订单创建11分钟后，返回 false
- ✅ 配置值读取正确

#### 9.1.2 差异计算测试

**测试类：** `OrderDiffCalculatorTest`

**测试用例：**
- ✅ 基础信息字段修改，正确生成差异
- ✅ 字段未修改，差异列表为空
- ✅ 订单项新增/删除，正确识别
- ✅ 外键字段（如医院ID）正确显示名称

#### 9.1.3 申请服务测试

**测试类：** `OrderModifyApplyServiceImplTest`

**测试用例：**
- ✅ 提交申请成功，生成申请ID
- ✅ 设计师提交申请，抛出权限异常
- ✅ 审核通过，订单数据正确更新
- ✅ 审核驳回，状态正确更新
- ✅ 审核已处理的申请，抛出状态异常
- ✅ 审核过期申请，抛出过期异常

### 9.2 接口测试

#### 9.2.1 修改订单接口测试

**测试场景：**
1. **时间窗口内修改**
   - 请求：订单创建5分钟内，提交修改
   - 预期：返回 200，订单数据立即更新
   
2. **超时修改**
   - 请求：订单创建15分钟后，提交修改
   - 预期：返回 40001，提示需要申请
   
3. **设计师修改**
   - 请求：设计师角色提交修改
   - 预期：返回 403，提示无权限

#### 9.2.2 申请接口测试

**测试场景：**
1. **提交申请成功**
   - 请求：提交修改申请
   - 预期：返回 200，生成申请ID和过期时间
   
2. **审核通过**
   - 请求：设计管理员审核通过
   - 预期：返回 200，订单数据更新
   
3. **审核驳回**
   - 请求：设计管理员审核驳回
   - 预期：返回 200，申请状态更新为已驳回

### 9.3 集成测试

#### 9.3.1 完整流程测试

**测试场景：超时申请→审核通过→数据生效**

```java
@Test
void testFullApprovalFlow() {
    // 1. 创建订单（模拟15分钟前）
    OrderEntity order = createOrder();
    order.setCreateTime(LocalDateTime.now().minusMinutes(15));
    
    // 2. 提交修改申请
    OrderModifyFullDTO dto = buildModifyDto();
    Long applyId = applyService.submitApply(order.getId(), dto);
    assertNotNull(applyId);
    
    // 3. 审核通过
    AuditApplyDTO auditDto = new AuditApplyDTO();
    auditDto.setResult(1);
    applyService.auditApply(applyId, auditDto);
    
    // 4. 验证订单数据已更新
    OrderEntity updatedOrder = orderService.getById(order.getId());
    assertEquals(dto.getPatientName(), updatedOrder.getPatientName());
}
```

### 9.4 定时任务测试

**测试类：** `OrderModifyApplyCleanTaskTest`

**测试用例：**
- ✅ 过期申请状态更新为"已过期"
- ✅ 未过期申请不受影响
- ✅ 已审核申请不受影响

### 9.5 手动测试

**测试清单：**

| 测试项 | 测试步骤 | 预期结果 | 状态 |
|--------|---------|---------|------|
| 时间窗口内修改 | 创建订单后5分钟内修改 | 立即生效 | ☐ |
| 超时修改提示 | 创建订单15分钟后修改 | 弹窗提示需要申请 | ☐ |
| 提交申请 | 确认申请后提交 | 生成申请记录，跳转申请列表 | ☐ |
| 审核界面展示 | 设计管理员查看申请详情 | 清晰展示变更内容 | ☐ |
| 审核通过 | 设计管理员审核通过 | 订单数据更新，申请状态更新 | ☐ |
| 审核驳回 | 设计管理员审核驳回 | 申请状态更新，记录驳回原因 | ☐ |
| 倒计时显示 | 查看待审核申请 | 倒计时准确显示剩余时间 | ☐ |
| 过期自动清理 | 等待申请过期 | 定时任务自动更新状态 | ☐ |
| 权限控制 | 设计师尝试修改订单 | 直接拒绝，提示无权限 | ☐ |

---

## 十、实施步骤

### 10.1 开发阶段（3个工作日）

#### Day 1：数据库和基础服务

**上午：**
1. 执行 DDL 语句，创建 `order_modification_apply` 表
2. 插入配置项到 `sys_config` 表
3. 创建实体类 `OrderModificationApplyEntity`
4. 创建 Mapper 接口 `OrderModifyApplyMapper`
5. 创建枚举类 `ApplyStatusEnum`、`ApplyTypeEnum`

**下午：**
1. 创建差异数据结构类（`OrderModificationDiff`、`FieldDiff`）
2. 实现 `OrderDiffCalculator` 差异计算服务
3. 实现 `OrderModifyTimeWindowChecker` 时间窗口判断工具
4. 编写单元测试（差异计算、时间窗口判断）

**产出：**
- ✅ 数据库表结构就绪
- ✅ 基础工具类和服务完成
- ✅ 单元测试通过

#### Day 2：核心业务逻辑

**上午：**
1. 实现 `OrderModifyApplyServiceImpl.submitApply()` - 提交申请
2. 实现 `OrderModifyApplyServiceImpl.auditApply()` - 审核申请
3. 实现 `OrderModifyApplyServiceImpl.listApplies()` - 查询申请列表
4. 修改现有 `OrderModifyFullService.modifyOrderFull()` - 增加时间窗口和权限判断

**下午：**
1. 创建 Controller 层接口（5个新接口）
2. 创建 VO/DTO 类
3. 编写 Service 单元测试
4. 编写 Controller 接口测试

**产出：**
- ✅ 核心业务逻辑完成
- ✅ API 接口实现完成
- ✅ 单元测试和接口测试通过

#### Day 3：定时任务和集成测试

**上午：**
1. 实现定时任务 `OrderModifyApplyCleanTask`
2. 配置定时任务 `@Scheduled` 注解
3. 编写定时任务测试

**下午：**
1. 执行集成测试（完整流程测试）
2. 修复测试发现的问题
3. 代码自查（遵循编码规范、日志规范）
4. 提交 Code Review

**产出：**
- ✅ 定时任务实现完成
- ✅ 集成测试通过
- ✅ 代码自查完成

---

### 10.2 前端开发阶段（2个工作日）

#### Day 4：前端页面开发

**上午：**
1. 修改订单表单页面 - 增加超时提示弹窗
2. 创建"我的申请记录"页面（业务员）
3. 创建"修改申请列表"页面（设计管理员）

**下午：**
1. 创建"申请详情审核"页面（设计管理员）
2. 实现差异对比UI组件（表格形式）
3. 实现倒计时组件

**产出：**
- ✅ 前端页面开发完成
- ✅ UI组件实现完成

#### Day 5：前后端联调和测试

**全天：**
1. 前后端接口联调
2. 测试完整业务流程
3. 修复联调发现的问题
4. 执行手动测试清单
5. 性能测试（并发提交申请、审核）

**产出：**
- ✅ 前后端联调完成
- ✅ 手动测试通过
- ✅ 性能测试达标

---

### 10.3 上线部署阶段（1个工作日）

#### Day 6：生产环境部署

**上午：**
1. 备份生产数据库
2. 执行 DDL 语句（创建表、插入配置）
3. 部署后端服务
4. 部署前端资源

**下午：**
1. 验证功能可用性（冒烟测试）
2. 监控日志和错误
3. 准备回滚方案（如有问题）
4. 编写上线报告

**产出：**
- ✅ 生产环境部署完成
- ✅ 功能验证通过
- ✅ 上线报告提交

---

### 10.4 开发分工建议

| 模块 | 负责人 | 工作量 |
|------|--------|--------|
| 数据库设计 | 后端开发 | 0.5天 |
| 差异计算服务 | 后端开发 | 1天 |
| 申请业务逻辑 | 后端开发 | 1天 |
| 定时任务 | 后端开发 | 0.5天 |
| 单元测试 | 后端开发 | 贯穿开发 |
| 前端页面开发 | 前端开发 | 1.5天 |
| 前后端联调 | 前后端协作 | 0.5天 |

**总工期：** 5个工作日（后端3天 + 前端2天，部分并行）

---

## 十一、风险评估与应对

### 11.1 技术风险

#### 风险1：差异计算性能问题

**风险描述：** 订单数据量大时，差异计算可能耗时较长

**风险等级：** 🟡 中

**应对措施：**
- 限制差异计算的字段范围，只对比关键字段
- 对于订单项较多的订单，采用摘要统计而非详细对比
- 设置计算超时时间，超时则降级为简化展示

#### 风险2：并发审核冲突

**风险描述：** 多个审核人同时审核同一申请，导致数据不一致

**风险等级：** 🟢 低

**应对措施：**
- 使用乐观锁（version字段）控制并发更新
- 审核前校验状态，确保只有"待审核"状态可以操作
- 后审核者会收到"申请已处理"的提示

#### 风险3：定时任务失效

**风险描述：** 定时任务异常或服务重启，导致过期申请未清理

**风险等级：** 🟡 中

**应对措施：**
- 定时任务使用 try-catch 包裹，防止异常中断
- 记录清理日志，便于监控和排查
- 服务重启后定时任务自动恢复执行
- 手动清理工具：提供管理员手动触发清理的接口

---

### 11.2 业务风险

#### 风险1：修改现有接口影响其他调用方

**风险描述：** 修改 `PUT /order/modify/{orderId}/full` 接口，增加时间窗口和权限判断，可能影响现有调用方

**风险等级：** 🔴 高

**应对措施：**
- **上线前必须确认**：当前系统中是否有其他模块/角色调用此接口
- **向后兼容方案**：
  - 方案A：新增 `/order/modify/{orderId}/full-v2` 接口，保留旧接口不变（推荐）
  - 方案B：在接口中增加参数 `skipCheck=true`，允许管理员跳过校验
- **灰度发布**：先在测试环境验证，确认无影响后再上线生产
- **回滚预案**：保留旧版本代码，出现问题可快速回滚

#### 风险2：时间窗口配置不当

**风险描述：** 时间窗口设置过短或过长，影响业务效率

**风险等级：** 🟡 中

**应对措施：**
- 配置化时间窗口，支持动态调整
- 初期设置为10分钟，根据业务反馈调整
- 提供配置修改审计日志

#### 风险2：申请积压

**风险描述：** 设计管理员未及时审核，导致大量申请过期

**风险等级：** 🟢 低

**应对措施：**
- 申请列表默认按申请时间升序，优先展示即将过期的
- 待审核申请显示倒计时，提醒审核人
- 可选：接入消息通知（站内消息/邮件）

#### 风险3：恶意频繁申请

**风险描述：** 业务员频繁提交申请，浪费审核资源

**风险等级：** 🟢 低

**应对措施：**
- 同一订单同一时间只能有一个待审核申请
- 提交申请时校验是否存在待审核记录
- 可选：限制单个用户单日申请次数

---

### 11.3 数据风险

#### 风险1：修改内容 JSON 过大

**风险描述：** 订单数据复杂时，JSON 字符串可能超过 TEXT 字段限制

**风险等级：** 🟢 低

**应对措施：**
- TEXT 字段最大 65535 字节，足够存储绝大多数订单
- 如需支持超大订单，将字段类型改为 MEDIUMTEXT
- 前端提交前校验订单项数量，限制在合理范围

#### 风险2：过期申请数据清理

**风险描述：** 大量过期申请占用存储空间

**风险等级：** 🟢 低

**应对措施：**
- 定时任务只更新状态，不删除记录（便于历史追溯）
- 可选：定期归档过期申请到历史表
- 可选：超过6个月的过期申请物理删除

---

### 11.4 时间风险

#### 风险1：差异计算逻辑复杂

**风险描述：** 订单字段众多，差异计算实现耗时可能超预期

**风险等级：** 🟡 中

**应对措施：**
- 优先实现核心字段的差异计算（患者姓名、医院、项目）
- 次要字段可简化为"有变更"标识，不展示详细对比
- 分阶段实现：v1 基础字段，v2 完善细节

#### 风险2：前端审核界面调整

**风险描述：** 审核界面需求可能与设计管理员实际使用习惯不符

**风险等级：** 🟡 中

**应对措施：**
- 提前与设计管理员沟通，确认界面原型
- 预留界面调整时间（0.5天）
- 采用组件化设计，便于快速调整布局

---

### 11.5 回滚方案

#### 场景1：功能上线后发现严重BUG

**回滚步骤：**
1. 立即下线前端页面入口（隐藏"修改订单"按钮）
2. 回滚后端代码到上一版本
3. 保留 `order_modification_apply` 表和数据（不影响现有订单）
4. 修复BUG后重新上线

#### 场景2：定时任务异常

**处理步骤：**
1. 停止定时任务执行
2. 手动执行SQL清理过期申请
3. 修复定时任务代码
4. 重新部署并验证

---

## 十二、附录

### 12.1 ErrorCodeEnum 扩展

```java
// 新增错误码
ORDER_MODIFY_TIME_EXCEEDED(40001, "订单修改时间已超限，请提交申请"),
APPLY_STATUS_INVALID(40002, "申请状态不允许此操作"),
APPLY_EXPIRED(40003, "申请已过期"),
APPLY_ALREADY_EXISTS(40004, "该订单已有待审核的修改申请");
```

### 12.2 配置键常量

```java
// SystemConfigKeyEnum 中新增
ORDER_MODIFY_TIME_WINDOW("order.modify.time.window", "订单修改时间窗口"),
ORDER_MODIFY_APPLY_EXPIRE_MINUTES("order.modify.apply.expire.minutes", "修改申请暂存期限");
```

### 12.3 关键API清单

| 接口路径 | 方法 | 说明 | 角色要求 |
|---------|------|------|---------|
| `/order/modify/{orderId}/full` | PUT | 全量修改订单（增加时间窗口判断） | 业务员 |
| `/order/modify/{orderId}/apply` | POST | 提交修改申请 | 业务员 |
| `/order/modify/apply/list` | POST | 查询申请列表 | 设计管理员 |
| `/order/modify/apply/{applyId}` | GET | 查询申请详情 | 设计管理员 |
| `/order/modify/apply/{applyId}/audit` | PUT | 审核申请 | 设计管理员 |
| `/order/modify/apply/my-list` | POST | 我的申请记录 | 业务员 |

---

## 十三、总结

### 核心设计亮点

1. **时间窗口机制**：平衡灵活性和稳定性，给业务员纠错空间
2. **双JSON存储**：完整JSON用于执行，差异JSON用于展示，职责分离
3. **自动清理机制**：防止脏数据积累，保证系统健壮性
4. **审核流程完整**：支持通过/驳回/过期多种状态，便于追溯

### 关键技术点

- 时间窗口判断：`ChronoUnit.MINUTES.between()`
- 差异计算：递归对比订单数据结构
- 定时清理：`@Scheduled` + 状态批量更新
- 乐观锁：防止并发审核冲突

### 预期收益

- ✅ 订单修改更规范，减少随意修改导致的设计返工
- ✅ 业务员有纠错时间，提升业务效率
- ✅ 审核机制保证重大修改有设计部门确认
- ✅ 完整的审计记录，便于问题追溯

---

**文档完成日期**：2026-06-08  
**预计开发完成日期**：2026-06-15  
**预计上线日期**：2026-06-16





