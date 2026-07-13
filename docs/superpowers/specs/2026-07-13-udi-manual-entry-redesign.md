# UDI码手动录入功能设计方案

## 一、背景和目标

### 1.1 背景

当前系统中，生产产品的UDI（Unique Device Identification，医疗器械唯一标识）码在质检合格时自动生成，格式为`UDI-00000001`（固定前缀+8位递增序号）。

但实际业务中，UDI码应该由国家药监局或国际标准机构分配，系统自动生成的编号无法满足真实的UDI标准要求。

### 1.2 目标

将UDI码从"自动生成"改为"手动录入"：
- 在流转卡详情页面的产品明细列表中，允许用户直接编辑每个产品的UDI码
- 当流转卡状态为"打印中"及之后的阶段时，允许录入和修改UDI码
- 提供批量更新接口，支持单个或多个产品的UDI码保存
- 保证UDI码的全局唯一性

---

## 二、当前实现分析

### 2.1 生成时机

**位置**：`ProductionQcServiceImpl.markProductPass()` 方法（第79-84行）

**触发条件**：
- 产品状态为"加工中"或"不合格"
- 订单类型为医疗器械（ORDER_TYPE_MEDICAL = 1）
- 执行质检合格操作

**生成逻辑**：
```java
if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(record.getOrderType())) {
    String udiCode = codeGeneratorService.generate(ProductionConstants.UDI_CODE);
    product.setUdiCode(udiCode);
    product.setUdiGenerateTime(LocalDateTime.now());
    log.info("生成UDI码: productId={}, productNo={}, udiCode={}", productId, product.getProductNo(), udiCode);
}
```

### 2.2 编码规则

从`sys_code_rule`表配置：
- 规则标识：UDI_CODE
- 前缀：UDI-
- 序列号长度：8位
- 复位策略：NEVER（永不复位）
- 生成格式：`UDI-00000001`、`UDI-00000002`...

### 2.3 涉及的数据库字段

`production_product`表中UDI相关字段：
```sql
udi_code VARCHAR(200) COMMENT 'UDI码（仅医疗器械）',
udi_di VARCHAR(100) COMMENT 'UDI-DI 设备标识符',
udi_pi VARCHAR(100) COMMENT 'UDI-PI 生产标识符',
udi_generate_time DATETIME COMMENT 'UDI生成时间'
```

**现状**：
- 只使用`udi_code`和`udi_generate_time`
- `udi_di`和`udi_pi`字段未使用

### 2.4 前端展示

流转卡详情接口（`getRecordDetail`）返回的`ProductionProductVO`已包含`udiCode`字段，前端可直接展示。

---

## 三、需求确认

通过7个澄清问题，确认了以下需求细节：

| 维度 | 确认内容 |
|------|---------|
| **录入场景** | 流转卡详情页面的产品明细列表中编辑 |
| **状态条件** | 打印中(PRINTING)及之后的所有状态均可编辑 |
| **录入字段** | 仅`udi_code`字段（`udi_di`和`udi_pi`暂不使用） |
| **格式要求** | 完全自由输入，不做格式限制 |
| **可修改性** | 允许多次修改 |
| **唯一性校验** | 需要全局唯一，不允许重复 |
| **权限控制** | 不做特殊限制，查看者即可编辑 |
| **时间字段** | 继续使用`udi_generate_time`记录录入/更新时间 |
| **产品类型** | 仅医疗器械产品允许录入UDI |
| **空值处理** | 不允许，UDI码必填 |

---

## 四、方案设计

### 4.1 方案选择

**采用方案B（标准方案）**：
- 删除自动生成逻辑
- 新增批量更新UDI接口，包含完整校验
- 不过度设计，满足当前需求

**不采用方案A**（最小改动）：缺少必要的校验逻辑

**不采用方案C**（扩展方案）：增加UDI修改历史记录功能属于过度设计

### 4.2 核心修改点

1. **删除自动生成逻辑** - 移除质检合格时的UDI自动生成代码
2. **新增批量更新接口** - 提供批量更新产品UDI码的接口
3. **完整校验逻辑** - 状态校验、唯一性校验、产品类型校验
4. **前端对接** - 详情接口已返回UDI字段，无需修改

---

## 五、详细实施方案

### 5.1 删除自动生成逻辑

**文件**：`ProductionQcServiceImpl.java`

**操作**：在`markProductPass()`方法中删除第79-84行

```java
// ❌ 删除以下代码
if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(record.getOrderType())) {
    String udiCode = codeGeneratorService.generate(ProductionConstants.UDI_CODE);
    product.setUdiCode(udiCode);
    product.setUdiGenerateTime(LocalDateTime.now());
    log.info("生成UDI码: productId={}, productNo={}, udiCode={}", ...);
}
```

---

### 5.2 新增批量更新UDI接口

#### 5.2.1 创建DTO

**新建文件**：`yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/dto/BatchUpdateUdiDTO.java`

```java
package com.yigongbao.module.production.qc.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 批量更新产品UDI码 DTO
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Data
public class BatchUpdateUdiDTO {
    /** 流转卡ID */
    @NotNull(message = "流转卡ID不能为空")
    private Long recordId;

    /** 产品UDI列表 */
    @NotEmpty(message = "产品UDI列表不能为空")
    @Valid
    private List<ProductUdiItem> products;

    /**
     * 产品UDI项
     */
    @Data
    public static class ProductUdiItem {
        /** 产品ID */
        @NotNull(message = "产品ID不能为空")
        private Long productId;

        /** UDI码 */
        @NotBlank(message = "UDI码不能为空")
        private String udiCode;
    }
}
```

#### 5.2.2 Service接口

**文件**：`IProductionQcService.java`

**新增方法**：
```java
/**
 * 批量更新产品UDI码
 *
 * @param dto 批量更新请求
 * @throws BusinessException 流转卡状态不允许、UDI码重复、非医疗器械等
 */
void batchUpdateUdi(BatchUpdateUdiDTO dto);
```

#### 5.2.3 Service实现

**文件**：`ProductionQcServiceImpl.java`

**实现逻辑**：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void batchUpdateUdi(BatchUpdateUdiDTO dto) {
    // 1. 校验流转卡状态
    ProductionRecordEntity record = recordMapper.selectById(dto.getRecordId());
    if (record == null) {
        throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
    }
    if (record.getStatus() < FlowStatusEnum.PRINTING.getValue()) {
        throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_UPDATE_UDI);
    }

    // 2. 校验订单类型（仅医疗器械）
    if (!ProductionConstants.ORDER_TYPE_MEDICAL.equals(record.getOrderType())) {
        throw new BusinessException(ErrorCodeEnum.NON_MEDICAL_NOT_ALLOW_UDI);
    }

    // 3. 批量校验UDI唯一性
    for (BatchUpdateUdiDTO.ProductUdiItem item : dto.getProducts()) {
        long duplicateCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getUdiCode, item.getUdiCode())
                .ne(ProductionProductEntity::getId, item.getProductId()));
        if (duplicateCount > 0) {
            throw new BusinessException(ErrorCodeEnum.UDI_CODE_EXISTS);
        }
    }

    // 4. 批量更新产品
    LocalDateTime now = LocalDateTime.now();
    for (BatchUpdateUdiDTO.ProductUdiItem item : dto.getProducts()) {
        ProductionProductEntity product = new ProductionProductEntity();
        product.setId(item.getProductId());
        product.setUdiCode(item.getUdiCode());
        product.setUdiGenerateTime(now);
        productMapper.updateById(product);
    }

    log.info("批量更新产品UDI码: recordId={}, productCount={}", dto.getRecordId(), dto.getProducts().size());
}
```

#### 5.2.4 Controller接口

**文件**：`ProductionQcController.java`

**新增接口**：
```java
/**
 * 批量更新产品UDI码
 */
@PostMapping("/batch-update-udi")
public Result<Void> batchUpdateUdi(@RequestBody @Valid BatchUpdateUdiDTO dto) {
    qcService.batchUpdateUdi(dto);
    return Result.success();
}
```

---

### 5.3 错误码定义

**文件**：`ErrorCodeEnum.java`

**新增错误码**：
```java
// 生产模块 - UDI相关错误码（833-835）
UDI_CODE_EXISTS(833, "UDI码已存在", 3),
RECORD_STATUS_NOT_ALLOW_UPDATE_UDI(834, "流转卡状态不允许更新UDI", 3),
NON_MEDICAL_NOT_ALLOW_UDI(835, "非医疗器械产品不允许录入UDI", 3),
```

---

## 六、测试计划

### 6.1 单元测试

**测试文件**：`ProductionQcServiceImplTest.java`

**测试场景**：

#### 测试1：状态校验
- **待打印状态**：调用`batchUpdateUdi`，预期抛出`RECORD_STATUS_NOT_ALLOW_UPDATE_UDI`异常
- **打印中状态**：调用`batchUpdateUdi`，预期成功
- **已完成状态**：调用`batchUpdateUdi`，预期成功

#### 测试2：订单类型校验
- **医疗器械订单**：允许更新UDI
- **非医疗器械订单**：抛出`NON_MEDICAL_NOT_ALLOW_UDI`异常

#### 测试3：唯一性校验
- **新UDI码**：允许保存
- **重复UDI码**：抛出`UDI_CODE_EXISTS`异常
- **同一产品更新自己的UDI**：允许（不视为重复）

#### 测试4：批量更新
- **单个产品更新**：成功
- **多个产品批量更新**：全部成功
- **部分产品UDI重复**：全部回滚（事务保证）

#### 测试5：空值校验
- **UDI码为空字符串**：DTO校验失败
- **UDI码为null**：DTO校验失败

#### 测试6：时间字段更新
- 更新UDI后，`udi_generate_time`字段应更新为当前时间

### 6.2 接口测试

**测试工具**：Postman / Swagger

**接口**：`POST /production/qc/batch-update-udi`

**测试用例**：

```json
{
  "recordId": 1,
  "products": [
    {
      "productId": 101,
      "udiCode": "01234567890123456789"
    },
    {
      "productId": 102,
      "udiCode": "01234567890123456790"
    }
  ]
}
```

**预期结果**：
- 流转卡状态>=PRINTING：成功，返回200
- 流转卡状态<PRINTING：失败，返回834错误码
- UDI重复：失败，返回833错误码
- 非医疗器械：失败，返回835错误码

### 6.3 功能测试

**测试流程**：
1. 创建医疗器械订单并进入打印中状态
2. 在流转卡详情页面查看产品列表
3. 编辑产品UDI码并保存
4. 验证保存成功后，UDI码正确显示
5. 尝试修改已有UDI码，验证可以修改
6. 尝试输入重复UDI码，验证提示错误

---

## 七、影响范围分析

### 7.1 代码修改

| 模块 | 文件 | 修改类型 | 影响范围 |
|------|------|---------|---------|
| 生产模块 | `ProductionQcServiceImpl.java` | 删除代码 | 移除质检合格时的UDI自动生成 |
| 生产模块 | `IProductionQcService.java` | 新增方法 | 新增接口定义 |
| 生产模块 | `ProductionQcServiceImpl.java` | 新增方法 | 实现批量更新UDI |
| 生产模块 | `ProductionQcController.java` | 新增接口 | 提供HTTP接口 |
| 生产模块 | `BatchUpdateUdiDTO.java` | 新增文件 | 请求参数封装 |
| 通用模块 | `ErrorCodeEnum.java` | 新增枚举 | 3个新错误码 |

### 7.2 数据库变更

**无需变更**，现有表结构已满足需求。

### 7.3 前端修改

**需要修改**：
1. 流转卡详情页面的产品列表
2. 增加UDI码编辑输入框（当状态>=PRINTING时可编辑）
3. 增加保存按钮，调用`/production/qc/batch-update-udi`接口
4. 增加错误提示（重复、状态不允许等）

### 7.4 向下兼容性

**完全兼容**：
- 已有的UDI码数据不受影响
- 流转卡详情接口无变化
- 其他模块无影响

---

## 八、风险评估

### 8.1 数据风险

**风险**：UDI码唯一性依赖后端校验，如果校验逻辑有bug，可能产生重复UDI

**缓解措施**：
1. 完整的单元测试覆盖唯一性校验逻辑
2. 数据库层面可考虑添加唯一索引（但需要处理null值）

### 8.2 用户体验风险

**风险**：用户可能不清楚什么时候可以编辑UDI

**缓解措施**：
1. 前端根据状态禁用/启用编辑框
2. 状态不允许时给出明确提示

### 8.3 数据质量风险

**风险**：用户可能输入格式不规范的UDI码

**缓解措施**：
1. 前端给出UDI码格式说明
2. 后端虽不强制格式，但保证唯一性和必填

---

## 九、实施计划

### 9.1 开发顺序

1. **后端开发**（预计1工作日）
   - 删除自动生成逻辑
   - 新增DTO、Service、Controller
   - 新增错误码
   - 编写单元测试

2. **接口联调**（预计0.5工作日）
   - 使用Postman测试接口
   - 验证各种校验逻辑

3. **前端开发**（预计1工作日）
   - 流转卡详情页面增加UDI编辑功能
   - 状态判断和编辑框禁用逻辑
   - 调用后端接口

4. **集成测试**（预计0.5工作日）
   - 完整业务流程测试
   - 边界场景测试

### 9.2 部署计划

**部署方式**：滚动发布

**回滚方案**：
- 如果发现问题，回滚代码到上一版本
- 已录入的UDI码数据不受影响

---

## 十、后续优化建议

1. **UDI码格式校验**：如果后续需要支持特定格式，可增加正则校验
2. **批量导入**：如果产品数量很多，可考虑提供Excel批量导入UDI功能
3. **UDI修改历史**：如果需要审计，可增加UDI修改历史记录表
4. **UDI-DI和UDI-PI字段**：如果后续需要完整的UDI结构，可启用这两个字段

---

**文档版本**：1.0  
**最后更新**：2026-07-13  
**作者**：Kiro AI Assistant
