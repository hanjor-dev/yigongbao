# 仓储入库出库功能实施计划

## 📋 文档信息

- **创建日期**：2026-06-11
- **作者**：hanjor
- **版本**：1.0
- **状态**：待实施

---

## ⚠️ 前置准备（必读）

### 重要提醒

**在开始实施前，必须完整阅读并理解以下文档：**

📖 **[.claude/rules/java-coding-standards.md](../../../.claude/rules/java-coding-standards.md)** - 项目 Java 编码规范

**关键规范要点（实施时必须遵守）：**

1. **命名规范**
   - Controller 以 `Controller` 结尾
   - Service 以 `Service/ServiceImpl` 结尾
   - DTO/VO 以 `DTO/VO` 结尾
   - 方法命名：查询用 `get/list`，新增用 `save/create`，更新用 `update`

2. **分层架构**
   - Controller：参数校验、调用 Service、返回 Result，**禁止记录日志**
   - ServiceImpl：业务逻辑、事务管理，**必须记录日志**，必须添加方法级和行级注释
   - Mapper：数据库 CRUD，禁止业务逻辑

3. **异常处理**
   - 优先使用 `ErrorCodeEnum`
   - Controller 禁止 try-catch
   - Service 使用 `@Transactional(rollbackFor = Exception.class)`

4. **日志规范**
   - Controller 层禁止输出日志
   - Service 层必须记录：INFO（关键操作）、WARN（可恢复异常）、ERROR（系统异常含堆栈）
   - 格式：`动作描述: 业务标识=值, 关键参数=值, 结果信息`

5. **数据库规范**
   - Entity 继承 `BaseEntity`（自动包含公共字段）
   - 状态字段使用数值型
   - 索引命名：`idx_表名前缀_字段名`

---

## 📖 需求概述

### 业务背景

当前生产模块的包装完成流程直接流转到"已完成"状态，缺少仓储管理环节。需要增加完整的入库、出库流程，实现产品级别的精细化管理。

### 核心需求

1. **包装完成后流转到"待入库"状态**（而非直接完成）
2. **产品级别的入库管理**：库管可以对每个产品逐一进行入库操作
3. **产品级别的出库管理**：库管可以对每个产品逐一进行出库操作
4. **三级聚合流转**：产品状态变更 → 流转卡状态聚合 → 订单状态聚合
5. **"已出库"作为最终态**：暂不自动流转到"已完成"状态

### 业务价值

- ✅ 实现产品库存的精细化管理
- ✅ 支持产品追溯（入库时间、库位、出库时间、收货人）
- ✅ 完善业务流程闭环（生产 → 质检 → 包装 → 入库 → 出库）
- ✅ 为后续库存盘点、库存报表提供数据基础

---

## 🎯 状态体系设计

### 1. FlowStatusEnum 调整（流转卡/订单状态）

```java
// 包装阶段（质检阶段 5010-5090）
PACKING(5050, "包装中"),

// ==================== 仓储阶段（6010-6090）====================
PENDING_WAREHOUSE_IN(6010, "待入库"),      // 🆕 新增
WAREHOUSED(6020, "已入库"),                // 🆕 新增
WAREHOUSE_OUT(6030, "已出库"),             // 🆕 新增 - 最终态

// ==================== 已完成（8010）保留备用 ====================
COMPLETED(8010, "已完成"),  // 保留，暂不自动流转
```

**编码规则说明**：
- 仓储阶段使用 6010-6090 编码段
- 每个状态间隔 10，保证后续可插入新状态
- 状态值 / 100 = 阶段值（60），符合 `belongsTo(phase)` 判断规则

### 2. ProductStatusEnum 调整（产品状态）

```java
PENDING("pending", "待生产"),
IN_PROCESS("in_process", "生产中"),
FAIL("fail", "质检不合格"),
PASS("pass", "质检合格"),
PENDING_WAREHOUSE_IN("pending_warehouse_in", "待入库"),  // 🆕 新增
WAREHOUSED("warehoused", "已入库"),                      // 🆕 新增
WAREHOUSE_OUT("warehouse_out", "已出库"),                // 🆕 新增 - 最终态
COMPLETED("completed", "已完成"),                        // 保留备用
CANCELLED("cancelled", "已废弃");
```

### 3. FlowActionEnum 新增（状态流转动作）

```java
// 包装完成 → 待入库
TRANSFER_TO_WAREHOUSE("包装完成，流转到待入库"),

// 所有流转卡入库完成 → 订单入库完成
COMPLETE_WAREHOUSE_IN("入库完成"),

// 所有流转卡出库完成 → 订单出库完成（最终态）
COMPLETE_WAREHOUSE_OUT("出库完成");
```

---

## 🔍 影响域分析

### 1. 枚举类修改

#### 1.1 FlowStatusEnum.java
- **位置**：`yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowStatusEnum.java`
- **修改内容**：
  - ❌ 删除：`WAREHOUSE_IN(6010, "入库中")` 和 `WAREHOUSED(6020, "已入库")`
  - ✅ 新增：
    ```java
    PENDING_WAREHOUSE_IN(6010, "待入库"),
    WAREHOUSED(6020, "已入库"),
    WAREHOUSE_OUT(6030, "已出库"),
    ```
- **影响范围**：全局（订单、流转卡、Flow 状态机）
- **风险评估**：⚠️ 高风险 - 已有数据库记录使用旧状态码 6010/6020
- **应对措施**：
  - 先执行数据迁移 SQL，将现有数据的状态码调整
  - 或者保留旧状态码映射，仅在新数据中使用新状态

#### 1.2 ProductStatusEnum.java
- **位置**：`yigongbao-module-production/src/main/java/com/yigongbao/module/production/enums/ProductStatusEnum.java`
- **修改内容**：
  - ✅ 新增 3 个状态（见上）
- **影响范围**：所有产品状态查询、更新逻辑
- **风险评估**：✅ 低风险 - 纯新增，不影响现有逻辑

#### 1.3 FlowActionEnum.java
- **位置**：`yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowActionEnum.java`
- **修改内容**：
  - ✅ 新增 3 个 Action
- **影响范围**：Flow 状态机的动作定义
- **风险评估**：✅ 低风险 - 纯新增

---

### 2. 数据库表结构修改

#### 2.1 production_product 表
- **表名**：`production_product`
- **修改类型**：添加字段
- **新增字段**：
  ```sql
  -- 入库相关字段
  warehouse_in_time DATETIME COMMENT '入库时间',
  warehouse_in_user_id BIGINT COMMENT '入库人ID',
  warehouse_location VARCHAR(100) COMMENT '库位',
  warehouse_in_remark VARCHAR(500) COMMENT '入库备注',
  
  -- 出库相关字段
  warehouse_out_time DATETIME COMMENT '出库时间',
  warehouse_out_user_id BIGINT COMMENT '出库人ID',
  warehouse_out_remark VARCHAR(500) COMMENT '出库备注',
  recipient VARCHAR(50) COMMENT '收货人',
  recipient_phone VARCHAR(20) COMMENT '收货电话'
  ```
- **影响范围**：ProductionProductEntity、产品查询/更新逻辑
- **风险评估**：✅ 低风险 - 新增字段，兼容现有数据

#### 2.2 索引添加（性能优化）
- **建议索引**：
  ```sql
  -- 按状态查询产品（入库/出库列表）
  CREATE INDEX idx_production_product_status 
      ON production_product(status, production_record_id);
  
  -- 按流转卡查询待入库/已入库产品（聚合判断）
  CREATE INDEX idx_production_product_record_status
      ON production_product(production_record_id, status);
  ```

---

### 3. Entity 实体类修改

#### 3.1 ProductionProductEntity.java
- **位置**：`yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/entity/ProductionProductEntity.java`
- **修改内容**：新增字段（对应数据库字段）
  ```java
  // 入库相关
  private LocalDateTime warehouseInTime;
  private Long warehouseInUserId;
  private String warehouseLocation;
  private String warehouseInRemark;
  
  // 出库相关
  private LocalDateTime warehouseOutTime;
  private Long warehouseOutUserId;
  private String warehouseOutRemark;
  private String recipient;
  private String recipientPhone;
  ```
- **注意事项**：字段名使用驼峰命名，MyBatis-Plus 自动映射下划线

---

### 4. 现有功能修改

#### 4.1 ProductionPackServiceImpl.transferToWarehouse()
- **位置**：`yigongbao-module-production/src/main/java/com/yigongbao/module/production/pack/service/impl/ProductionPackServiceImpl.java`
- **当前逻辑**：
  ```java
  包装中(PACKING) → 入库中(WAREHOUSE_IN) → 聚合触发 → 已完成
  ```
- **修改后逻辑**：
  ```java
  包装中(PACKING) → 待入库(PENDING_WAREHOUSE_IN) → 聚合触发（订单级别）
  质检合格产品(PASS) → 待入库(PENDING_WAREHOUSE_IN)
  ```
- **核心变更**：
  1. 流转卡状态改为 `PENDING_WAREHOUSE_IN`
  2. 更新所有 `PASS` 状态的产品为 `PENDING_WAREHOUSE_IN`
  3. 聚合 Action 改为 `TRANSFER_TO_WAREHOUSE`
  4. 不再调用 `COMPLETE_WAREHOUSE_IN`（由入库完成后触发）
- **异常处理**：保持现有逻辑，幂等性保护

#### 4.2 ProductionRecordServiceImpl 聚合逻辑
- **位置**：`yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- **现有方法**：
  - `triggerFlowIfAllExact()` - 精确匹配聚合
  - `triggerFlowAndSync()` - Flow 状态流转并回写
- **新增调用场景**：
  - 包装完成 → 待入库（精确匹配）
  - 入库完成 → 已入库（精确匹配）
  - 出库完成 → 已出库（精确匹配，最终态）
- **无需修改**：方法逻辑保持不变，仅调用参数变化

#### 4.3 聚合逻辑安全判断（重要）

**问题场景**：
- 如果流转卡下所有产品都质检不合格（FAIL），包装完成后流转卡状态变为 PENDING_WAREHOUSE_IN
- 但这些产品不会被流转到 PENDING_WAREHOUSE_IN 状态（只有PASS状态才会）
- 聚合检查时，pendingCount = 0，会错误地触发流转卡状态流转

**解决方案**：
在 `checkAndTransferRecordToWarehoused()` 和 `checkAndTransferRecordToWarehouseOut()` 中，除了检查待处理数量为 0，还必须确保至少有一个产品已完成操作：

```java
// 入库聚合逻辑
if (pendingCount == 0 && warehousedCount > 0) {
    // 确保至少有产品入库了，才触发流转卡聚合
}

// 出库聚合逻辑  
if (warehousedCount == 0 && warehouseOutCount > 0) {
    // 确保至少有产品出库了，才触发流转卡聚合
}
```

**实际情况分析**：
- 质检完成流转到包装时，`ProductionQcServiceImpl.transferToPacking()` 已校验不允许有 FAIL 状态产品
- 所以理论上不会出现"所有产品都不合格"的场景
- 但为了代码健壮性，仍建议添加此安全判断

---

## 📦 新建模块结构

### warehouse 子模块完整结构

```
yigongbao-module-production/src/main/java/com/yigongbao/module/production/
└── warehouse/
    ├── controller/
    │   └── WarehouseController.java           # 仓储管理控制器
    ├── service/
    │   ├── IWarehouseService.java            # 仓储服务接口
    │   └── impl/
    │       └── WarehouseServiceImpl.java     # 仓储服务实现
    ├── dto/
    │   ├── WarehouseInProductDTO.java        # 产品入库请求 DTO
    │   ├── WarehouseOutProductDTO.java       # 产品出库请求 DTO
    │   └── WarehouseRecordPageDTO.java       # 流转卡列表查询 DTO
    └── vo/
        └── WarehouseRecordVO.java            # 流转卡详情 VO（可复用 ProductionRecordVO）
```

### 测试模块结构

```
yigongbao-module-production/src/test/java/com/yigongbao/module/production/
└── warehouse/
    ├── WarehouseServiceImplTest.java         # Service 单元测试
    └── WarehouseControllerTest.java          # Controller 接口测试
```

---

## 💻 核心代码实现示例

### 1. DTO 类定义

#### WarehouseInProductDTO.java
```java
package com.yigongbao.module.production.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseInProductDTO {
    @NotBlank(message = "库位不能为空")
    private String location;
    
    private String remark;
}
```

#### WarehouseOutProductDTO.java
```java
package com.yigongbao.module.production.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseOutProductDTO {
    @NotBlank(message = "收货人不能为空")
    private String recipient;
    
    @NotBlank(message = "收货电话不能为空")
    private String recipientPhone;
    
    private String remark;
}
```

---

### 2. Service 核心方法（关键逻辑）

#### 产品入库方法
```java
/**
 * 对单个产品进行入库操作
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void warehouseInProduct(Long productId, WarehouseInProductDTO dto) {
    ProductionProductEntity product = productMapper.selectById(productId);
    if (product == null) {
        throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
    }
    
    // 校验状态：只有待入库状态才能入库
    if (!ProductStatusEnum.PENDING_WAREHOUSE_IN.getCode().equals(product.getStatus())) {
        log.warn("产品状态不允许入库: productId={}, currentStatus={}", 
            productId, product.getStatus());
        throw new BusinessException(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_IN);
    }
    
    // 更新产品状态和入库信息
    product.setStatus(ProductStatusEnum.WAREHOUSED.getCode());
    product.setWarehouseInTime(LocalDateTime.now());
    product.setWarehouseInUserId(StpUtil.getLoginIdAsLong());
    product.setWarehouseLocation(dto.getLocation());
    product.setWarehouseInRemark(dto.getRemark());
    productMapper.updateById(product);
    
    log.info("产品入库: productId={}, productNo={}, location={}", 
        productId, product.getProductNo(), dto.getLocation());
    
    // 检查流转卡下所有产品是否都已入库
    checkAndTransferRecordToWarehoused(product.getProductionRecordId());
}

/**
 * 检查流转卡下所有产品是否都已入库，如果是则流转流转卡状态
 */
private void checkAndTransferRecordToWarehoused(Long recordId) {
    // 查询待入库的产品数量
    long pendingCount = productMapper.selectCount(
        new LambdaQueryWrapper<ProductionProductEntity>()
            .eq(ProductionProductEntity::getProductionRecordId, recordId)
            .eq(ProductionProductEntity::getStatus, 
                ProductStatusEnum.PENDING_WAREHOUSE_IN.getCode())
    );
    
    if (pendingCount > 0) {
        log.info("流转卡下仍有待入库产品: recordId={}, pendingCount={}", 
            recordId, pendingCount);
        return;
    }
    
    // ⚠️ 安全判断：确保至少有一个产品已入库
    long warehousedCount = productMapper.selectCount(
        new LambdaQueryWrapper<ProductionProductEntity>()
            .eq(ProductionProductEntity::getProductionRecordId, recordId)
            .eq(ProductionProductEntity::getStatus, 
                ProductStatusEnum.WAREHOUSED.getCode())
    );
    
    if (warehousedCount == 0) {
        log.warn("流转卡下没有已入库产品，跳过聚合: recordId={}", recordId);
        return;
    }
    
    // 流转卡状态：PENDING_WAREHOUSE_IN → WAREHOUSED
    ProductionRecordEntity record = recordMapper.selectById(recordId);
    if (record == null || 
        !FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue().equals(record.getStatus())) {
        return;  // 幂等性保护
    }
    
    record.setStatus(FlowStatusEnum.WAREHOUSED.getValue());
    recordMapper.updateById(record);
    
    log.info("流转卡全部产品已入库: recordId={}, recordNo={}", 
        recordId, record.getRecordNo());
    
    // 聚合触发订单级别流转
    recordService.triggerFlowIfAllExact(
        record.getOrderId(),
        FlowStatusEnum.WAREHOUSED.getValue(),
        FlowActionEnum.COMPLETE_WAREHOUSE_IN
    );
}
```

#### 产品出库方法（逻辑类似）
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void warehouseOutProduct(Long productId, WarehouseOutProductDTO dto) {
    // 1. 查询产品并校验状态（只有已入库状态才能出库）
    // 2. 更新产品状态和出库信息
    // 3. 调用 checkAndTransferRecordToWarehouseOut(recordId)
    // 逻辑与入库类似，状态流转为：WAREHOUSED → WAREHOUSE_OUT
}
```

---

### 3. Controller 接口定义

```java
@Tag(name = "仓储管理")
@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseController {
    
    private final IWarehouseService warehouseService;
    
    @Operation(summary = "分页查询待入库流转卡列表")
    @PostMapping("/in/records/list")
    public Result<IPage<ProductionRecordVO>> listPendingInRecords(
        @RequestBody WarehouseRecordPageDTO dto) {
        return Result.success(warehouseService.listPendingInRecords(dto));
    }
    
    @Operation(summary = "查询流转卡下待入库产品列表")
    @GetMapping("/in/records/{recordId}/products")
    public Result<List<ProductionProductVO>> listPendingInProducts(
        @PathVariable Long recordId) {
        return Result.success(warehouseService.listPendingInProducts(recordId));
    }
    
    @Operation(summary = "对单个产品进行入库操作")
    @OperationLog(module = "仓储管理", businessType = OperationTypeEnum.UPDATE, 
                  operation = "产品入库")
    @PostMapping("/in/products/{productId}")
    public Result<Void> warehouseInProduct(
        @PathVariable Long productId,
        @Validated @RequestBody WarehouseInProductDTO dto) {
        warehouseService.warehouseInProduct(productId, dto);
        return Result.success();
    }
    
    // 出库接口（结构类似，路径为 /warehouse/out/...）
}
```

---

### 4. 包装完成逻辑修改

```java
// ProductionPackServiceImpl.transferToWarehouse() 修改
@Override
@Transactional(rollbackFor = Exception.class)
public void transferToWarehouse(Long recordId) {
    ProductionRecordEntity record = recordMapper.selectById(recordId);
    if (record == null) {
        throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
    }
    
    // 幂等性保护
    if (!FlowStatusEnum.PACKING.getValue().equals(record.getStatus())) {
        log.warn("流转卡状态不允许流转: recordId={}, currentStatus={}", 
            recordId, record.getStatus());
        throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW);
    }
    
    // 🔄 流转卡状态：PACKING → PENDING_WAREHOUSE_IN
    record.setStatus(FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue());
    recordMapper.updateById(record);
    
    // 🔄 批量更新产品状态：PASS → PENDING_WAREHOUSE_IN
    productMapper.update(null, 
        new LambdaUpdateWrapper<ProductionProductEntity>()
            .eq(ProductionProductEntity::getProductionRecordId, recordId)
            .eq(ProductionProductEntity::getStatus, ProductStatusEnum.PASS.getCode())
            .set(ProductionProductEntity::getStatus, 
                ProductStatusEnum.PENDING_WAREHOUSE_IN.getCode())
    );
    
    // 同步更新包装工序完成时间
    processMapper.update(null, 
        new LambdaUpdateWrapper<ProductionProcessEntity>()
            .eq(ProductionProcessEntity::getProductionRecordId, recordId)
            .eq(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PACK.getCode())
            .set(ProductionProcessEntity::getEndTime, LocalDateTime.now())
            .set(ProductionProcessEntity::getStatus, ProcessStatusEnum.COMPLETED.getCode())
    );
    
    log.info("包装完成，流转到待入库: recordId={}, recordNo={}", 
        recordId, record.getRecordNo());
    
    // 🎯 聚合触发
    recordService.triggerFlowIfAllExact(
        record.getOrderId(),
        FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue(),
        FlowActionEnum.TRANSFER_TO_WAREHOUSE
    );
}
```

---

## 🚀 分阶段实施计划

### 阶段 0：准备工作（预计 0.5 天）

**目标**：完成开发环境准备和风险评估

#### 任务清单
- [ ] 阅读并理解项目编码规范文档
- [ ] 评估现有数据库中 FlowStatusEnum 6010/6020 的使用情况
- [ ] 确定数据迁移策略（是否需要迁移旧数据）
- [ ] 创建 feature 分支：`feature/warehouse-in-out`

#### 数据迁移评估步骤

**0.1 检查现有数据**
```sql
-- 检查 production_record 表中是否有使用旧状态码的数据
SELECT status, COUNT(*) 
FROM production_record 
WHERE status IN (6010, 6020) 
GROUP BY status;

-- 检查 order_main 表
SELECT status, COUNT(*) 
FROM order_main 
WHERE status IN (6010, 6020) 
GROUP BY status;
```

**0.2 数据迁移策略**

**场景A：无生产数据（开发/测试环境）**
- 直接修改枚举定义即可
- 无需数据迁移

**场景B：有生产数据（生产环境）**
- 旧状态码含义：WAREHOUSE_IN(6010, "入库中"), WAREHOUSED(6020, "已入库")
- 新状态码含义：PENDING_WAREHOUSE_IN(6010, "待入库"), WAREHOUSED(6020, "已入库")
- 6010 的语义发生变化，6020 保持不变

**推荐方案**：将现有 6010/6020 状态的数据标记为已完成
```sql
-- 生产环境数据迁移（谨慎执行）
UPDATE production_record 
SET status = 8010  -- COMPLETED
WHERE status IN (6010, 6020);

UPDATE order_main 
SET status = 8010  -- COMPLETED
WHERE status IN (6010, 6020);
```

**备选方案**：如果需要保留中间状态，创建临时状态码映射
```sql
-- 将旧的 6010 映射到新的 6020（已入库）
UPDATE production_record SET status = 6020 WHERE status = 6010;
```

---

### 阶段 1：基础设施（预计 1 天）

**目标**：完成枚举、数据库、实体类的修改

#### 任务清单

**1.1 枚举类修改**
- [ ] 修改 `FlowStatusEnum.java` - 调整仓储阶段状态码
- [ ] 修改 `FlowActionEnum.java` - 新增 3 个 Action
- [ ] 修改 `ProductStatusEnum.java` - 新增 3 个状态

**1.2 数据库修改**
- [ ] 编写 DDL 脚本添加 9 个新字段
- [ ] 编写索引创建脚本
- [ ] 执行数据库变更

**1.3 实体类修改**
- [ ] 修改 `ProductionProductEntity.java` 新增字段

**1.4 错误码新增**
- [ ] 修改 `ErrorCodeEnum.java` 新增错误码：
  - `PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_IN(606, "产品状态不允许入库")`
  - `PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_OUT(607, "产品状态不允许出库")`

**1.5 确认 FlowPhaseEnum（重要）**
- [ ] 检查 `FlowPhaseEnum.java` 是否已有 `WAREHOUSE(60, "仓储")` 阶段
- [ ] 如果没有则需要新增（因为新状态码 6010-6090 需要对应阶段 60）
- [ ] 确认 `belongsTo(phase)` 判断逻辑正确

---

### 阶段 2：修改现有功能（预计 0.5 天）

**目标**：调整包装完成流程

#### 任务清单
- [ ] 修改 `ProductionPackServiceImpl.transferToWarehouse()`
- [ ] 更新单元测试

---

### 阶段 3：入库功能实现（预计 1.5 天）

**目标**：实现产品入库管理功能

#### 任务清单
- [ ] 创建 DTO/VO 类
- [ ] 创建 Service 接口和实现
- [ ] 创建 Controller
- [ ] 编写单元测试

---

### 阶段 4：出库功能实现（预计 1.5 天）

**目标**：实现产品出库管理功能

#### 任务清单
- [ ] 创建出库 DTO
- [ ] 实现出库 Service 方法
- [ ] 新增出库 Controller 接口
- [ ] 编写单元测试

---

### 阶段 5：集成测试与文档（预计 1 天）

**目标**：完成测试和文档

#### 任务清单
- [ ] 完整业务流程测试
- [ ] 更新接口文档
- [ ] 更新数据库文档

---

## ⚠️ 风险点与注意事项

### 1. 数据迁移风险
- ⚠️ FlowStatusEnum 状态码重新定义，需评估现有数据影响

### 2. 并发安全
- ✅ 使用幂等性保护，聚合前再次查询状态

### 3. 性能优化
- ✅ 添加必要索引，使用 COUNT 查询

### 4. 日志规范
- ✅ Controller 层禁止日志
- ✅ Service 层必须记录关键操作
- ✅ 格式：`动作: 业务标识=值, 参数=值`

---

## 📋 实施检查清单

### 开发前
- [ ] 阅读编码规范文档
- [ ] 理解业务流程
- [ ] 创建 feature 分支

### 开发后
- [ ] 代码格式符合规范
- [ ] 注释完整
- [ ] 日志符合规范
- [ ] 单元测试通过
- [ ] 接口文档更新
- [ ] Code Review 通过

---

## 📌 总结

本实施计划严格遵循项目编码规范，分 5 个阶段实施，预计总工期 **5-6 天**。

核心保障：
1. ✅ 代码质量 - 严格遵循规范
2. ✅ 功能完整 - 入库、出库、聚合逻辑完整
3. ✅ 异常处理 - 使用 ErrorCodeEnum
4. ✅ 测试覆盖 - 单元测试 + 集成测试
5. ✅ 风险可控 - 识别并应对关键风险

---

**文档版本**：1.0  
**最后更新**：2026-06-11  
**作者**：hanjor
