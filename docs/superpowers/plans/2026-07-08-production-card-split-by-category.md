# 生产流转卡按产品大类拆分实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现生产流转卡按产品大类（模型类/导板类）自动拆分，一个数据包根据产品类型生成多张独立流转卡

**Architecture:** 修改DesignCompletedListener的流转卡创建逻辑，从按数据包创建改为按产品大类分组创建；增加product_category字段和联合唯一索引保证幂等性；保持事务一致性

**Tech Stack:** Spring Boot, MyBatis Plus, JUnit 5, Mockito

---

## File Structure

### Modified Files
- `sql/ddl.sql` - 数据库结构变更（增加字段和索引）
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/entity/ProductionRecordEntity.java` - 实体类增加productCategory字段
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/ProductionRecordVO.java` - VO增加productCategory和productCategoryName字段
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java` - 核心业务逻辑修改

### Test Files
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DesignCompletedListenerTest.java` - 单元测试

---

## Task 1: 数据库结构变更

**Files:**
- Modify: `sql/ddl.sql`

- [ ] **Step 1: 在production_record表增加product_category字段**

在`sql/ddl.sql`中找到`CREATE TABLE production_record`语句，在`design_package_code`字段后增加：

```sql
product_category VARCHAR(20) COMMENT '产品大类(17.1=模型类, 17.2=导板类)',
```

- [ ] **Step 2: 创建联合唯一索引**

在`CREATE TABLE production_record`语句的索引部分增加：

```sql
UNIQUE KEY uk_package_category (design_package_id, product_category),
```

- [ ] **Step 3: 验证SQL语法**

确保DDL文件的SQL语法正确，检查逗号和括号是否匹配。

- [ ] **Step 4: 提交数据库变更**

```bash
git add sql/ddl.sql
git commit -m "chore: 为production_record表增加product_category字段和索引

- 增加product_category字段用于标识流转卡的产品大类
- 增加(design_package_id, product_category)联合唯一索引保证幂等性"
```

---

## Task 2: 实体类修改

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/entity/ProductionRecordEntity.java`

- [ ] **Step 1: 在ProductionRecordEntity增加productCategory字段**

在类中增加字段（建议在designPackageCode字段后）：

```java
/**
 * 产品大类（17.1=模型类，17.2=导板类）
 */
private String productCategory;
```

- [ ] **Step 2: 验证实体类**

确保类上有`@Data`注解，字段会自动生成getter/setter方法。

- [ ] **Step 3: 提交实体类修改**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/entity/ProductionRecordEntity.java
git commit -m "feat: ProductionRecordEntity增加productCategory字段"
```

---

## Task 3: VO类修改

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/ProductionRecordVO.java`

- [ ] **Step 1: 在ProductionRecordVO增加字段**

在类中增加字段（建议在designPackageCode字段后）：

```java
/**
 * 产品大类（17.1=模型类，17.2=导板类）
 */
private String productCategory;

/**
 * 产品大类名称（冗余，用于前端显示）
 */
private String productCategoryName;
```

- [ ] **Step 2: 验证VO类**

确保类上有`@Data`注解。

- [ ] **Step 3: 提交VO修改**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/ProductionRecordVO.java
git commit -m "feat: ProductionRecordVO增加productCategory和productCategoryName字段"
```

---

## Task 4: 核心逻辑 - 注入ProductMapper依赖

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java:1-60`

- [ ] **Step 1: 在DesignCompletedListener中注入ProductMapper**

首先添加import语句：

```java
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.basic.product.entity.ProductEntity;
```

然后在类的依赖注入部分增加：

```java
private final ProductMapper productMapper;
```

确保类使用`@RequiredArgsConstructor`注解，依赖会自动注入。

- [ ] **Step 2: 验证编译**

运行编译确保ProductMapper可以正常导入：

```bash
cd yigongbao-parent
mvn compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交依赖注入**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java
git commit -m "feat: DesignCompletedListener注入ProductMapper依赖"
```

---

## Task 5: 核心逻辑 - 产品大类分组方法（TDD）

**Files:**
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DesignCompletedListenerTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java`

- [ ] **Step 1: 编写测试 - 产品大类分组逻辑**

在测试类中增加测试方法：

```java
@Test
void testGroupDesignProductsByCategory() {
    // Arrange: 准备测试数据
    Long packageId = 1L;
    
    // 模拟3个设计产品：2个模型类 + 1个导板类
    DesignProductEntity product1 = new DesignProductEntity();
    product1.setId(1L);
    product1.setProductId(101L);
    product1.setProductName("模型A");
    
    DesignProductEntity product2 = new DesignProductEntity();
    product2.setId(2L);
    product2.setProductId(101L);
    product2.setProductName("模型B");
    
    DesignProductEntity product3 = new DesignProductEntity();
    product3.setId(3L);
    product3.setProductId(102L);
    product3.setProductName("导板A");
    
    when(designProductMapper.selectList(any())).thenReturn(Arrays.asList(product1, product2, product3));
    
    // 模拟产品大类查询
    ProductEntity modelProduct = new ProductEntity();
    modelProduct.setId(101L);
    modelProduct.setCategory("17.1");
    
    ProductEntity guideProduct = new ProductEntity();
    guideProduct.setId(102L);
    guideProduct.setCategory("17.2");
    
    when(productMapper.selectBatchIds(any())).thenReturn(Arrays.asList(modelProduct, guideProduct));
    
    // Act: 调用分组方法（待实现）
    Map<String, List<DesignProductEntity>> grouped = listener.groupByProductCategory(packageId);
    
    // Assert: 验证分组结果
    assertNotNull(grouped);
    assertEquals(2, grouped.size());
    assertTrue(grouped.containsKey("17.1"));
    assertTrue(grouped.containsKey("17.2"));
    assertEquals(2, grouped.get("17.1").size());
    assertEquals(1, grouped.get("17.2").size());
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd yigongbao-parent
mvn test -Dtest=DesignCompletedListenerTest#testGroupDesignProductsByCategory
```

Expected: FAIL - 方法不存在

- [ ] **Step 3: 实现产品大类分组方法**

首先确保已添加必要的import语句：

```java
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
```

然后在DesignCompletedListener中增加方法：

```java
/**
 * 按产品大类分组设计产品
 *
 * @param packageId 数据包ID
 * @return 按产品大类分组的设计产品Map，key为产品大类编码（17.1/17.2）
 */
private Map<String, List<DesignProductEntity>> groupByProductCategory(Long packageId) {
    // 1. 查询数据包下的所有设计产品
    List<DesignProductEntity> designProducts = designProductMapper.selectList(
        new LambdaQueryWrapper<DesignProductEntity>()
            .eq(DesignProductEntity::getPackageId, packageId));
    
    if (designProducts.isEmpty()) {
        return Collections.emptyMap();
    }
    
    // 2. 批量查询产品大类信息
    Set<Long> productIds = designProducts.stream()
        .map(DesignProductEntity::getProductId)
        .collect(Collectors.toSet());
    List<ProductEntity> products = productMapper.selectBatchIds(productIds);
    
    // 3. 构建 productId -> category 的映射（过滤category为null的产品）
    Map<Long, String> productCategoryMap = products.stream()
        .filter(p -> p.getCategory() != null)
        .collect(Collectors.toMap(ProductEntity::getId, ProductEntity::getCategory));
    
    // 4. 按产品大类分组（只保留模型类和导板类）
    Map<String, List<DesignProductEntity>> groupedByCategory = designProducts.stream()
        .filter(dp -> {
            String category = productCategoryMap.get(dp.getProductId());
            return "17.1".equals(category) || "17.2".equals(category);
        })
        .collect(Collectors.groupingBy(dp -> 
            productCategoryMap.get(dp.getProductId())));
    
    // 5. 记录被忽略的产品
    long ignoredCount = designProducts.size() - 
        groupedByCategory.values().stream().mapToLong(List::size).sum();
    if (ignoredCount > 0) {
        log.warn("数据包包含非模型/导板类产品，已忽略: packageId={}, ignoredCount={}", 
            packageId, ignoredCount);
    }
    
    return groupedByCategory;
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
cd yigongbao-parent
mvn test -Dtest=DesignCompletedListenerTest#testGroupDesignProductsByCategory
```

Expected: PASS

- [ ] **Step 5: 提交分组逻辑**

```bash
git add yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DesignCompletedListenerTest.java
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java
git commit -m "feat: 实现按产品大类分组设计产品的逻辑

- 增加groupByProductCategory方法
- 支持模型类（17.1）和导板类（17.2）分组
- 过滤其他产品大类并记录警告日志
- 增加单元测试覆盖"
```

---

## Task 6: 核心逻辑 - 修改createProductionRecord方法签名

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java`

- [ ] **Step 1: 修改createProductionRecord方法增加category参数**

找到`createProductionRecord`方法，修改签名：

```java
private ProductionRecordEntity createProductionRecord(
    OrderMainEntity order, 
    DesignPackageEntity pkg,
    String category  // 新增参数
)
```

- [ ] **Step 2: 在方法中设置productCategory字段**

在`record.setStatus(...)`之前增加：

```java
record.setProductCategory(category);
```

- [ ] **Step 3: 修改extractMaterialFromDesignProducts方法**

修改方法签名为直接接收产品列表，删除内部查询代码：

```java
private String extractMaterialFromDesignProducts(List<DesignProductEntity> designProducts) {
    // 删除原有的查询代码：
    // List<DesignProductEntity> designProducts = designProductMapper.selectList(...)
    
    if (designProducts.isEmpty()) {
        return null;
    }

    // 拼接颜色+材质，去重后用顿号分隔
    java.util.Set<String> materialDescriptions = designProducts.stream()
            .map(dp -> {
                String color = dp.getColorName();
                String material = dp.getMaterialName();
                if (material == null || material.isBlank()) {
                    return null;
                }
                if (color != null && !color.isBlank()) {
                    return color + material;
                }
                return material;
            })
            .filter(desc -> desc != null)
            .collect(java.util.stream.Collectors.toSet());

    if (materialDescriptions.isEmpty()) {
        return null;
    }
    return String.join("、", materialDescriptions);
}
```

- [ ] **Step 4: 修改createProductRecords方法**

修改方法签名为直接接收产品列表，删除内部查询代码：

```java
private int createProductRecords(ProductionRecordEntity record, List<DesignProductEntity> designProducts) {
    // 删除原有的查询代码：
    // List<DesignProductEntity> designProducts = designProductMapper.selectList(...)
    
    if (designProducts.isEmpty()) {
        return 0;
    }

    int totalCount = 0;
    for (DesignProductEntity dp : designProducts) {
        // 取该产品关联的第一个文件作为打印文件
        DesignProductFileEntity dpFile = designProductFileMapper.selectOne(
                new LambdaQueryWrapper<DesignProductFileEntity>()
                        .eq(DesignProductFileEntity::getDesignProductId, dp.getId())
                        .orderByAsc(DesignProductFileEntity::getSortOrder)
                        .last("LIMIT 1"));

        // 按数量展开创建产品记录
        int qty = dp.getQuantity() != null && dp.getQuantity() > 0 ? dp.getQuantity() : 1;
        for (int i = 0; i < qty; i++) {
            ProductionProductEntity product = new ProductionProductEntity();
            product.setProductionRecordId(record.getId());
            product.setPrintFileId(dpFile != null ? dpFile.getPackageFileId() : null);
            product.setProductNo(codeGeneratorService.generate(ProductionConstants.PRODUCT_NO));
            product.setProductName(dp.getProductName());
            product.setSpecName(dp.getSpecName());
            product.setCertNo(dp.getCertNo());
            product.setMaterialName(dp.getMaterialName());
            product.setColorName(dp.getColorName());
            product.setFileName(dpFile != null ? dpFile.getPackageFileName() : null);
            product.setStatus(ProductStatusEnum.PENDING.getCode());
            productMapper.insert(product);
        }
        totalCount += qty;
    }
    return totalCount;
}
```

- [ ] **Step 5: 编译验证**

```bash
cd yigongbao-parent
mvn compile -DskipTests
```

Expected: 编译错误，因为onDesignCompleted方法中调用这些方法的地方还没有更新

- [ ] **Step 6: 提交方法签名修改**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java
git commit -m "refactor: 修改方法签名以支持产品大类参数

- createProductionRecord增加category参数
- extractMaterialFromDesignProducts改为接收产品列表
- createProductRecords改为接收产品列表"
```

---

## Task 7: 核心逻辑 - 重构onDesignCompleted方法

**Files:**
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DesignCompletedListenerTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java`

- [ ] **Step 1: 编写测试 - 标准拆分场景**

```java
@Test
void testOnDesignCompleted_SplitByCategory() {
    // Arrange
    Long orderId = 1L;
    Long packageId = 1L;
    
    OrderMainEntity order = new OrderMainEntity();
    order.setId(orderId);
    order.setOrderCode("ORD001");
    order.setOrderType(1);
    
    DesignPackageEntity pkg = new DesignPackageEntity();
    pkg.setId(packageId);
    pkg.setPackageCode("PKG001");
    
    when(orderMainMapper.selectById(orderId)).thenReturn(order);
    when(designPackageMapper.selectList(any())).thenReturn(Arrays.asList(pkg));
    
    // 模拟2个模型 + 1个导板
    DesignProductEntity modelProduct1 = new DesignProductEntity();
    modelProduct1.setId(1L);
    modelProduct1.setProductId(101L);
    modelProduct1.setQuantity(1);
    
    DesignProductEntity modelProduct2 = new DesignProductEntity();
    modelProduct2.setId(2L);
    modelProduct2.setProductId(101L);
    modelProduct2.setQuantity(1);
    
    DesignProductEntity guideProduct = new DesignProductEntity();
    guideProduct.setId(3L);
    guideProduct.setProductId(102L);
    guideProduct.setQuantity(1);
    
    when(designProductMapper.selectList(any())).thenReturn(
        Arrays.asList(modelProduct1, modelProduct2, guideProduct));
    
    // 模拟产品大类查询
    ProductEntity productModel = new ProductEntity();
    productModel.setId(101L);
    productModel.setCategory("17.1");
    
    ProductEntity productGuide = new ProductEntity();
    productGuide.setId(102L);
    productGuide.setCategory("17.2");
    
    when(productMapper.selectBatchIds(any())).thenReturn(
        Arrays.asList(productModel, productGuide));
    
    // Mock幂等性检查返回null（不存在）
    when(recordMapper.selectOne(any())).thenReturn(null);
    
    // Mock编码生成器
    when(codeGeneratorService.generate(anyString())).thenReturn("MOCK_CODE");
    
    // Act
    listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));
    
    // Assert
    verify(recordMapper, times(2)).insert(any()); // 应该创建2张流转卡（模型1张，导板1张）
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd yigongbao-parent
mvn test -Dtest=DesignCompletedListenerTest#testOnDesignCompleted_SplitByCategory
```

Expected: FAIL

- [ ] **Step 3: 重构onDesignCompleted方法**

修改数据包循环内的逻辑：

```java
for (DesignPackageEntity pkg : packages) {
    try {
        // 按产品大类分组
        Map<String, List<DesignProductEntity>> groupedByCategory = 
            groupByProductCategory(pkg.getId());
        
        if (groupedByCategory.isEmpty()) {
            log.warn("数据包无有效产品，跳过流转卡创建: packageId={}, packageCode={}", 
                pkg.getId(), pkg.getPackageCode());
            continue;
        }
        
        // 为每个产品大类创建流转卡
        for (Map.Entry<String, List<DesignProductEntity>> entry : groupedByCategory.entrySet()) {
            String category = entry.getKey();
            List<DesignProductEntity> categoryProducts = entry.getValue();
            
            // 幂等性检查：检查该数据包+产品大类的流转卡是否已存在
            ProductionRecordEntity existingRecord = recordMapper.selectOne(
                new LambdaQueryWrapper<ProductionRecordEntity>()
                    .eq(ProductionRecordEntity::getDesignPackageId, pkg.getId())
                    .eq(ProductionRecordEntity::getProductCategory, category)
                    .last("LIMIT 1"));
            
            if (existingRecord != null) {
                log.info("数据包的该产品大类流转卡已存在，跳过创建: packageId={}, category={}, recordNo={}",
                    pkg.getId(), category, existingRecord.getRecordNo());
                continue;
            }
            
            // 创建流转卡
            ProductionRecordEntity record = createProductionRecord(order, pkg, category);
            
            // 提取材质信息
            String material = extractMaterialFromDesignProducts(categoryProducts);
            record.setMaterial(material);
            
            // 创建产品记录
            int productCount = createProductRecords(record, categoryProducts);
            
            // 创建工序记录
            createProcessRecords(record.getId(), order.getOrderType());
            
            // 更新流转卡产品总数和二维码
            record.setTotalProductCount(productCount);
            String qrContent = String.format("RECORD:%s|BATCH:%s", 
                record.getRecordNo(), record.getProductionBatchNo());
            record.setQrCodeUrl(qrContent);
            recordMapper.updateById(record);
            
            createdRecordIds.add(record.getId());
            
            log.info("创建生产流转卡: recordNo={}, packageId={}, category={}, productCount={}", 
                record.getRecordNo(), pkg.getId(), category, productCount);
        }
        
    } catch (Exception e) {
        log.error("创建生产流转卡失败: orderId={}, packageId={}, packageCode={}",
            orderId, pkg.getId(), pkg.getPackageCode(), e);
        throw e;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
cd yigongbao-parent
mvn test -Dtest=DesignCompletedListenerTest#testOnDesignCompleted_SplitByCategory
```

Expected: PASS

- [ ] **Step 5: 提交核心逻辑重构**

```bash
git add yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DesignCompletedListenerTest.java
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DesignCompletedListener.java
git commit -m "feat: 重构onDesignCompleted方法支持按产品大类拆分流转卡

- 使用groupByProductCategory方法分组
- 为每个产品大类创建独立流转卡
- 产品大类层级的幂等性检查
- 保持事务一致性"
```

---

## Task 8: 补充测试用例

**Files:**
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DesignCompletedListenerTest.java`

- [ ] **Step 1: 增加测试 - 单一产品大类**

```java
@Test
void testOnDesignCompleted_SingleCategory() {
    // Arrange: 只有模型类产品
    Long orderId = 1L;
    Long packageId = 1L;
    
    OrderMainEntity order = new OrderMainEntity();
    order.setId(orderId);
    order.setOrderCode("ORD001");
    order.setOrderType(1);
    
    DesignPackageEntity pkg = new DesignPackageEntity();
    pkg.setId(packageId);
    pkg.setPackageCode("PKG001");
    
    when(orderMainMapper.selectById(orderId)).thenReturn(order);
    when(designPackageMapper.selectList(any())).thenReturn(Arrays.asList(pkg));
    
    // 模拟3个模型类产品
    DesignProductEntity product1 = new DesignProductEntity();
    product1.setId(1L);
    product1.setProductId(101L);
    product1.setQuantity(1);
    
    DesignProductEntity product2 = new DesignProductEntity();
    product2.setId(2L);
    product2.setProductId(101L);
    product2.setQuantity(1);
    
    DesignProductEntity product3 = new DesignProductEntity();
    product3.setId(3L);
    product3.setProductId(101L);
    product3.setQuantity(1);
    
    when(designProductMapper.selectList(any())).thenReturn(
        Arrays.asList(product1, product2, product3));
    
    // 模拟产品大类查询 - 只有模型类
    ProductEntity productModel = new ProductEntity();
    productModel.setId(101L);
    productModel.setCategory("17.1");
    
    when(productMapper.selectBatchIds(any())).thenReturn(Arrays.asList(productModel));
    when(recordMapper.selectOne(any())).thenReturn(null);
    when(codeGeneratorService.generate(anyString())).thenReturn("MOCK_CODE");
    
    // Act
    listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));
    
    // Assert: 应该只创建1张流转卡（模型类）
    verify(recordMapper, times(1)).insert(any());
}
```

- [ ] **Step 2: 增加测试 - 空数据包**

```java
@Test
void testOnDesignCompleted_EmptyPackage() {
    // Arrange: 数据包无设计产品
    Long orderId = 1L;
    Long packageId = 1L;
    
    OrderMainEntity order = new OrderMainEntity();
    order.setId(orderId);
    order.setOrderCode("ORD001");
    order.setOrderType(1);
    
    DesignPackageEntity pkg = new DesignPackageEntity();
    pkg.setId(packageId);
    pkg.setPackageCode("PKG001");
    
    when(orderMainMapper.selectById(orderId)).thenReturn(order);
    when(designPackageMapper.selectList(any())).thenReturn(Arrays.asList(pkg));
    
    // 模拟空的设计产品列表
    when(designProductMapper.selectList(any())).thenReturn(Collections.emptyList());
    
    // Act
    listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));
    
    // Assert: 应该不创建流转卡
    verify(recordMapper, never()).insert(any());
}
```

- [ ] **Step 3: 增加测试 - 幂等性保护**

```java
@Test
void testOnDesignCompleted_IdempotencyCheck() {
    // Arrange: 模型类流转卡已存在，只应创建导板类流转卡
    Long orderId = 1L;
    Long packageId = 1L;
    
    OrderMainEntity order = new OrderMainEntity();
    order.setId(orderId);
    order.setOrderCode("ORD001");
    order.setOrderType(1);
    
    DesignPackageEntity pkg = new DesignPackageEntity();
    pkg.setId(packageId);
    pkg.setPackageCode("PKG001");
    
    when(orderMainMapper.selectById(orderId)).thenReturn(order);
    when(designPackageMapper.selectList(any())).thenReturn(Arrays.asList(pkg));
    
    // 模拟2个模型 + 1个导板
    DesignProductEntity modelProduct1 = new DesignProductEntity();
    modelProduct1.setId(1L);
    modelProduct1.setProductId(101L);
    modelProduct1.setQuantity(1);
    
    DesignProductEntity modelProduct2 = new DesignProductEntity();
    modelProduct2.setId(2L);
    modelProduct2.setProductId(101L);
    modelProduct2.setQuantity(1);
    
    DesignProductEntity guideProduct = new DesignProductEntity();
    guideProduct.setId(3L);
    guideProduct.setProductId(102L);
    guideProduct.setQuantity(1);
    
    when(designProductMapper.selectList(any())).thenReturn(
        Arrays.asList(modelProduct1, modelProduct2, guideProduct));
    
    // 模拟产品大类查询
    ProductEntity productModel = new ProductEntity();
    productModel.setId(101L);
    productModel.setCategory("17.1");
    
    ProductEntity productGuide = new ProductEntity();
    productGuide.setId(102L);
    productGuide.setCategory("17.2");
    
    when(productMapper.selectBatchIds(any())).thenReturn(
        Arrays.asList(productModel, productGuide));
    
    // 模拟模型类流转卡已存在
    ProductionRecordEntity existingModelRecord = new ProductionRecordEntity();
    existingModelRecord.setId(100L);
    existingModelRecord.setRecordNo("REC001");
    existingModelRecord.setProductCategory("17.1");
    
    // 第一次查询返回已存在的模型类流转卡，第二次查询返回null（导板类不存在）
    when(recordMapper.selectOne(any())).thenReturn(existingModelRecord, null);
    when(codeGeneratorService.generate(anyString())).thenReturn("MOCK_CODE");
    
    // Act
    listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));
    
    // Assert: 应该只创建1张流转卡（跳过模型类，只创建导板类）
    verify(recordMapper, times(1)).insert(any());
}
```

- [ ] **Step 4: 运行所有测试**

```bash
cd yigongbao-parent
mvn test -Dtest=DesignCompletedListenerTest
```

Expected: ALL PASS

- [ ] **Step 5: 提交测试用例**

```bash
git add yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DesignCompletedListenerTest.java
git commit -m "test: 补充DesignCompletedListener测试用例

- 单一产品大类场景
- 空数据包场景
- 幂等性保护场景"
```

---

## Task 9: 运行完整测试套件

**Files:**
- N/A

- [ ] **Step 1: 运行生产模块所有测试**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-production
```

Expected: ALL PASS

- [ ] **Step 2: 运行完整项目测试**

```bash
cd yigongbao-parent
mvn clean test
```

Expected: ALL PASS

- [ ] **Step 3: 编译打包验证**

```bash
cd yigongbao-parent
mvn clean package -DskipTests
```

Expected: BUILD SUCCESS

---

## 实施后验证清单

- [ ] 数据库字段和索引已创建
- [ ] ProductionRecordEntity包含productCategory字段
- [ ] ProductionRecordVO包含productCategory和productCategoryName字段
- [ ] DesignCompletedListener注入了ProductMapper
- [ ] 产品大类分组逻辑正确（只处理17.1和17.2）
- [ ] 幂等性保护在产品大类层级生效
- [ ] 一个数据包可以生成多张流转卡（按产品大类）
- [ ] 每张流转卡有独立的流转卡编号和生产批号
- [ ] 所有单元测试通过
- [ ] 完整测试套件通过

---

## 注意事项

1. **事务一致性**：整个onDesignCompleted方法在一个事务中，同一数据包的所有流转卡要么全部成功要么全部回滚
2. **幂等性保护**：联合唯一索引(design_package_id, product_category)提供数据库层面保护
3. **日志记录**：关键步骤都有日志，便于问题追踪
4. **向后兼容**：历史流转卡的product_category字段为NULL，不影响现有功能

---

**计划创建时间**：2026-07-10
**预计实施时间**：2-3小时
