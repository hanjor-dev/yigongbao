# 生产流转卡按产品大类拆分设计方案

**文档版本**：1.0  
**创建日期**：2026-07-08  
**设计目标**：实现生产流转卡按产品大类（模型类/导板类）自动拆分

---

## 1. 背景和需求

### 1.1 当前问题

当前系统在设计完成后，为每个数据包（DesignPackage）创建一张生产流转卡（ProductionRecord）。这导致以下问题：

- 一个数据包可能包含模型类和导板类产品
- 模型和导板需要使用不同的打印机器和材料
- 生产人员无法根据产品类型快速分配任务
- 流转卡无法按打印设备进行分类管理

### 1.2 业务需求

**核心需求**：自动识别指令单（数据包）里的打印要求，按产品类型拆分流转卡。

**具体规则**：
- 如果一张指令单包含模型和导板，需要分成两张流转卡
- 不同产品大类的流转卡要分开
- 一个案例有2-3种产品大类，就需要2-3张流转卡

### 1.3 产品大类定义

系统中的产品大类（Product.category）：
- **17.1** = 模型类 (MODEL)
- **17.2** = 导板类 (GUIDE)

---

## 2. 设计目标

1. **按产品大类拆分**：一个数据包按产品大类（模型类/导板类）拆分成多张流转卡
2. **独立批号**：每张流转卡拥有独立的流转卡编号和生产批号
3. **按需生成**：只生成实际存在的产品大类流转卡，不生成空卡
4. **材质不影响拆分**：同一产品大类内的不同材质/颜色不影响拆分
5. **向后兼容**：保持现有数据模型和API接口不变

---

## 3. 拆分规则总结

| 场景 | 当前行为 | 新行为 |
|------|---------|--------|
| 数据包包含模型+导板 | 1张流转卡 | 2张流转卡（模型1张，导板1张） |
| 数据包只有模型 | 1张流转卡 | 1张流转卡（模型） |
| 数据包只有导板 | 1张流转卡 | 1张流转卡（导板） |
| 数据包无设计产品 | 1张流转卡（产品数0） | 0张流转卡 |
| 包含其他大类产品 | 包含在流转卡中 | 忽略，记录警告日志 |

---

## 4. 整体架构

### 4.1 改动范围

**核心改动点**：`DesignCompletedListener.java`

**主要修改的方法**：
1. `onDesignCompleted()` - 修改遍历逻辑，从按数据包创建改为按数据包+产品大类创建
2. `createProductionRecord()` - 增加产品大类参数
3. `createProductRecords()` - 修改为只创建指定产品大类的产品记录
4. `extractMaterialFromDesignProducts()` - 增加产品大类过滤

**新增依赖**：
- 注入 `ProductMapper`（基础产品表的Mapper），用于查询产品大类信息

### 4.2 数据流变化

**当前流程**：
```
设计完成事件 → 查询数据包列表 → 遍历数据包 
→ 为每个数据包创建1张流转卡 → 创建产品记录 → 创建工序记录
```

**新流程**：
```
设计完成事件 → 查询数据包列表 → 遍历数据包 
→ 查询该数据包的所有设计产品 → 关联查询产品大类 → 按产品大类分组（17.1/17.2）
→ 为每个产品大类创建1张流转卡 → 创建该大类的产品记录 → 创建工序记录
```

**关键变化**：从"一个数据包一张卡"变为"一个数据包的一个产品大类一张卡"。

---

## 5. 核心实现逻辑

### 5.1 产品大类分组逻辑

```java
// 步骤1：查询数据包下的所有设计产品
List<DesignProductEntity> designProducts = designProductMapper.selectList(
    new LambdaQueryWrapper<DesignProductEntity>()
        .eq(DesignProductEntity::getPackageId, packageId));

if (designProducts.isEmpty()) {
    log.warn("数据包无设计产品，跳过流转卡创建: packageId={}", packageId);
    return Collections.emptyList();
}

// 步骤2：批量查询产品大类信息
Set<Long> productIds = designProducts.stream()
    .map(DesignProductEntity::getProductId)
    .collect(Collectors.toSet());
List<ProductEntity> products = productMapper.selectBatchIds(productIds);

// 构建 productId -> category 的映射
Map<Long, String> productCategoryMap = products.stream()
    .collect(Collectors.toMap(ProductEntity::getId, ProductEntity::getCategory));

// 步骤3：按产品大类分组（只保留模型类和导板类）
Map<String, List<DesignProductEntity>> groupedByCategory = designProducts.stream()
    .filter(dp -> {
        String category = productCategoryMap.get(dp.getProductId());
        return "17.1".equals(category) || "17.2".equals(category);
    })
    .collect(Collectors.groupingBy(dp -> 
        productCategoryMap.get(dp.getProductId())));

// 记录被忽略的产品
long ignoredCount = designProducts.size() - 
    groupedByCategory.values().stream().mapToLong(List::size).sum();
if (ignoredCount > 0) {
    log.warn("数据包包含非模型/导板类产品，已忽略: packageId={}, packageCode={}, ignoredCount={}", 
        packageId, pkg.getPackageCode(), ignoredCount);
}
```

### 5.2 流转卡创建逻辑

```java
// 步骤4：为每个产品大类创建流转卡
List<Long> createdRecordIds = new ArrayList<>();

for (Map.Entry<String, List<DesignProductEntity>> entry : groupedByCategory.entrySet()) {
    String category = entry.getKey();
    List<DesignProductEntity> categoryProducts = entry.getValue();
    
    try {
        // 创建流转卡（传入产品大类信息）
        ProductionRecordEntity record = createProductionRecord(order, pkg, category);
        
        // 只创建该产品大类的产品记录
        int productCount = createProductRecords(record, categoryProducts);
        
        // 创建工序记录
        createProcessRecords(record.getId(), order.getOrderType());
        
        // 更新流转卡的产品总数和二维码
        record.setTotalProductCount(productCount);
        String qrContent = String.format("RECORD:%s|BATCH:%s",
            record.getRecordNo(), record.getProductionBatchNo());
        record.setQrCodeUrl(qrContent);
        recordMapper.updateById(record);
        
        createdRecordIds.add(record.getId());
        
        log.info("创建生产流转卡: recordNo={}, packageId={}, category={}, productCount={}", 
            record.getRecordNo(), packageId, category, productCount);
            
    } catch (Exception e) {
        log.error("创建生产流转卡失败: orderId={}, packageId={}, category={}",
            orderId, packageId, category, e);
        throw e;
    }
}
```

### 5.3 方法签名变更

**createProductionRecord()** 方法增加 `category` 参数：
```java
private ProductionRecordEntity createProductionRecord(
    OrderMainEntity order, 
    DesignPackageEntity pkg, 
    String category  // 新增参数：产品大类
)
```

**createProductRecords()** 方法改为直接接收产品列表：
```java
private int createProductRecords(
    ProductionRecordEntity record, 
    List<DesignProductEntity> designProducts  // 直接传入已过滤的产品列表
)
```

**extractMaterialFromDesignProducts()** 方法改为直接接收产品列表：
```java
private String extractMaterialFromDesignProducts(
    List<DesignProductEntity> designProducts  // 直接传入已过滤的产品列表
)
```

---

## 6. 数据模型变化

### 6.1 表结构变更

**新增字段**：在 `production_record` 表增加产品大类字段

```sql
ALTER TABLE production_record 
ADD COLUMN product_category VARCHAR(20) COMMENT '产品大类(17.1=模型类, 17.2=导板类)' 
AFTER design_package_code;
```

**字段说明**：
- 字段名：`product_category`
- 类型：`VARCHAR(20)`
- 允许NULL：是（历史数据为NULL）
- 注释：产品大类(17.1=模型类, 17.2=导板类)

**索引建议**：如果后续需要频繁按产品大类查询流转卡，可添加索引：
```sql
ALTER TABLE production_record ADD INDEX idx_product_category (product_category);
```

### 6.2 实体类变更

**ProductionRecordEntity** 新增字段：
```java
/**
 * 产品大类（17.1=模型类，17.2=导板类）
 */
private String productCategory;
```

### 6.3 数据关系变化

**原关系**：`DesignPackage 1 : 1 ProductionRecord`

**新关系**：`DesignPackage 1 : N ProductionRecord`（一个数据包可能对应多张流转卡）

现有的数据模型已支持这种一对多关系，无需额外调整。

---

## 7. 边界情况和错误处理

### 7.1 边界情况

| 场景 | 处理方式 | 日志级别 |
|------|---------|---------|
| 数据包无设计产品 | 不创建流转卡，直接返回 | WARN |
| 产品ID关联不到基础产品表 | 过滤该产品，记录日志 | WARN |
| 产品大类为空或无效 | 过滤该产品，记录日志 | WARN |
| 产品大类不是17.1或17.2 | 过滤该产品，记录日志 | WARN |
| 分组后某大类无有效产品 | 不为该大类创建流转卡 | INFO |
| 所有产品都被过滤 | 不创建任何流转卡 | WARN |

### 7.2 幂等性保护

**检查逻辑**：在处理每个数据包时，检查是否已存在流转卡

```java
// 幂等性检查：跳过已创建流转卡的数据包
ProductionRecordEntity existingRecord = recordMapper.selectOne(
    new LambdaQueryWrapper<ProductionRecordEntity>()
        .eq(ProductionRecordEntity::getDesignPackageId, pkg.getId())
        .last("LIMIT 1"));
        
if (existingRecord != null) {
    log.info("数据包已存在流转卡，跳过创建: packageId={}, packageCode=, recordNo={}",
        pkg.getId(), pkg.getPackageCode(), existingRecord.getRecordNo());
    continue;
}
```

**说明**：如果数据包已存在任何流转卡（无论是旧的还是新的），都跳过该数据包的处理，避免重复创建。

### 7.3 事务处理

整个监听器方法使用 `@Transactional` 注解，确保：
- 同一数据包的多张流转卡要么全部创建成功，要么全部回滚
- 流转卡、产品记录、工序记录的创建保持原子性

---

## 8. 测试策略

### 8.1 单元测试覆盖

**测试类**：`DesignCompletedListenerTest`

**核心测试场景**：

| 测试场景 | 输入 | 预期输出 |
|---------|------|---------|
| 标准拆分 | 数据包包含2个模型+1个导板 | 生成2张流转卡（模型卡1张2个产品，导板卡1张1个产品） |
| 单一模型 | 数据包只有3个模型产品 | 生成1张流转卡（模型卡3个产品） |
| 单一导板 | 数据包只有1个导板产品 | 生成1张流转卡（导板卡1个产品） |
| 空数据包 | 数据包无设计产品 | 不生成流转卡 |
| 混合大类 | 包含模型、导板和其他大类 | 只为模型和导板生成流转卡，其他忽略 |
| 无效产品ID | 产品ID在Product表中不存在 | 过滤该产品，正常处理其他产品 |
| 幂等性 | 数据包已存在流转卡 | 跳过该数据包，不创建新卡 |

### 8.2 测试验证点

**流转卡级别**：
- ✓ 流转卡数量正确
- ✓ 每张流转卡的 `product_category` 字段值正确（17.1或17.2）
- ✓ 每张流转卡的 `design_package_id` 关联正确
- ✓ 流转卡编号（recordNo）和生产批号（productionBatchNo）独立生成
- ✓ `totalProductCount` 统计正确

**产品记录级别**：
- ✓ 每张流转卡的产品记录只包含对应大类的产品
- ✓ 产品数量展开正确（quantity字段）
- ✓ 产品编号（productNo）独立生成

**材质字段**：
- ✓ `material` 字段只包含对应产品大类的材质信息
- ✓ 多种材质用顿号分隔

**工序记录**：
- ✓ 工序记录按订单类型创建（医疗器械5个，非医疗2个）
- ✓ 每张流转卡的工序记录独立

### 8.3 集成测试

**测试场景**：完整的设计完成到流转卡创建流程

```java
@Test
@Transactional
void testDesignCompletedEventIntegration() {
    // 1. 创建订单和数据包
    // 2. 创建设计产品（模型+导板）
    // 3. 发布设计完成事件
    // 4. 验证流转卡创建结果
}
```

---

## 9. 实施计划

### 9.1 实施步骤

1. **数据库变更**（DDL）
   - 执行 ALTER TABLE 添加 `product_category` 字段
   - 更新 schema.sql（生产环境）
   - 更新测试环境的建表脚本

2. **实体类修改**
   - ProductionRecordEntity 添加 productCategory 字段

3. **核心逻辑实现**
   - 注入 ProductMapper 依赖
   - 重构 onDesignCompleted() 方法
   - 修改 createProductionRecord() 添加 category 参数
   - 修改 createProductRecords() 和 extractMaterialFromDesignProducts() 方法

4. **单元测试**
   - 编写 DesignCompletedListenerTest 测试用例
   - 覆盖所有场景和边界情况

5. **集成测试**
   - 在测试环境验证完整流程
   - 验证与前端页面的兼容性

6. **代码审查和部署**
   - 提交代码审查
   - 部署到测试环境
   - 部署到生产环境

### 9.2 回滚方案

如果部署后发现问题，可以快速回滚：

**代码回滚**：
- 回滚到上一个稳定版本

**数据处理**：
- 新字段 `product_category` 允许NULL，历史数据不受影响
- 新逻辑创建的流转卡可以正常使用
- 如需清理，可按 `product_category IS NOT NULL` 条件删除

### 9.3 注意事项

1. **历史数据**：
   - 历史流转卡的 `product_category` 字段为NULL
   - 前端和报表查询需要兼容NULL值
   - 可选：编写数据迁移脚本，为历史流转卡补充 `product_category` 字段

2. **前端适配**：
   - 流转卡列表页面可能需要显示产品大类信息
   - 筛选条件可能需要增加产品大类筛选
   - 详情页面需要明确显示产品大类

3. **API兼容性**：
   - ProductionRecordVO 增加 `productCategory` 字段
   - 保持向后兼容，旧的API响应增加新字段不影响前端

---

## 10. 附录

### 10.1 相关实体和表

**核心表**：
- `production_record` - 生产流转卡表
- `production_product` - 生产产品表
- `design_package` - 设计数据包表
- `design_product` - 设计产品表（打印信息）
- `product` - 基础产品表（包含产品大类）

**关键字段映射**：
- `product.category` → `production_record.product_category`
- 17.1 = 模型类 (MODEL)
- 17.2 = 导板类 (GUIDE)

### 10.2 日志记录规范

**关键日志**：
```java
// 监听到事件
log.info("监听到设计完成事件: orderId={}, orderCode={}", orderId, orderCode);

// 按产品大类分组
log.info("数据包产品大类分组: packageId={}, 模型类={}, 导板类={}, 忽略={}", 
    packageId, modelCount, guideCount, ignoredCount);

// 创建流转卡
log.info("创建生产流转卡: recordNo={}, packageId={}, category={}, productCount={}", 
    recordNo, packageId, category, productCount);

// 完成处理
log.info("设计完成自动创建流转卡完成: orderId={}, packageCount={}, recordCount={}",
    orderId, packageCount, recordCount);

// 警告：忽略产品
log.warn("数据包包含非模型/导板类产品，已忽略: packageId={}, ignoredCount={}", 
    packageId, ignoredCount);
```

---

**文档结束**
