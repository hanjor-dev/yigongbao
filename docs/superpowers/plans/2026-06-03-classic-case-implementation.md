# 订单经典案例功能实施计划

## 文档信息

- **创建时间**：2026-06-03
- **设计文档**：[2026-06-03-classic-case-design.md](../specs/2026-06-03-classic-case-design.md)
- **文件清单**：[2026-06-03-classic-case-complete-file-list.md](../specs/2026-06-03-classic-case-complete-file-list.md)
- **预计工作量**：8-10小时

---

## 实施顺序

```
阶段1: 基础设施 (数据库、枚举、实体)
  ↓
阶段2: DTO/VO/Convert (数据传输对象)
  ↓
阶段3: 文件迁移服务 (核心复杂逻辑)
  ↓
阶段4: 经典案例服务 (业务逻辑)
  ↓
阶段5: 保护机制 (跨模块保护)
  ↓
阶段6: Controller层 (接口暴露)
  ↓
阶段7: 测试验证 (单元测试 + 集成测试)
```

---

## 阶段1：基础设施搭建

### 任务1.1：执行数据库DDL脚本

**文件**：`sql/ddl.sql`

**操作**：
1. 在order_main表添加4个字段（is_classic_case, classic_case_time, classic_case_by, classic_case_remark）
2. 创建索引 idx_order_classic_case

**DDL语句**：
```sql
ALTER TABLE order_main 
ADD COLUMN is_classic_case TINYINT DEFAULT 0 COMMENT '是否经典案例：0-否，1-是';

ALTER TABLE order_main 
ADD COLUMN classic_case_time DATETIME COMMENT '标记为经典案例的时间';

ALTER TABLE order_main 
ADD COLUMN classic_case_by BIGINT COMMENT '标记为经典案例的操作人ID';

ALTER TABLE order_main 
ADD COLUMN classic_case_remark VARCHAR(500) COMMENT '标记为经典案例的备注';

CREATE INDEX idx_order_classic_case 
ON order_main(is_classic_case, create_time DESC);
```

**验证**：执行后查询表结构确认字段和索引已创建

---

### 任务1.2：扩展ErrorCodeEnum错误码

**文件**：`yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**操作**：在枚举类中添加4个经典案例相关错误码（620-623）

**代码**：
```java
// 经典案例相关错误码（620-629）
CLASSIC_CASE_PROTECTED(620, "经典案例订单受保护，不可删除或修改"),
CLASSIC_CASE_ALREADY_MARKED(621, "订单已标记为经典案例"),
CLASSIC_CASE_ORDER_NOT_COMPLETED(622, "只有已完成订单才能标记为经典案例"),
CLASSIC_CASE_FILE_MIGRATE_FAILED(623, "经典案例文件迁移失败"),
```

**位置**：错误码按业务分组，添加到订单相关错误码之后

---

### 任务1.3：扩展OrderMainEntity实体类

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/entity/OrderMainEntity.java`

**操作**：添加4个新字段

**代码**：
```java
/**
 * 是否经典案例：0-否，1-是
 */
private Integer isClassicCase;

/**
 * 标记为经典案例的时间
 */
private LocalDateTime classicCaseTime;

/**
 * 标记为经典案例的操作人ID
 */
private Long classicCaseBy;

/**
 * 标记为经典案例的备注
 */
private String classicCaseRemark;
```

---

## 阶段2：DTO/VO/Convert层

### 任务2.1：创建MarkClassicCaseDTO

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/MarkClassicCaseDTO.java`

**字段**：
- orderId (Long, @NotNull)
- remark (String, @NotBlank)

---

### 任务2.2：创建ClassicCaseQueryDTO

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/ClassicCaseQueryDTO.java`

**字段**：
- orderCode (String, 模糊查询)
- patientName (String)
- hospitalId (Long)
- bodyPartId (Long)
- startTime (LocalDateTime)
- endTime (LocalDateTime)
- pageNum (Integer, 默认1)
- pageSize (Integer, 默认10)

---

### 任务2.3：创建ClassicCaseVO

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/ClassicCaseVO.java`

**字段**：
- id, orderCode, patientName, patientAge, patientGender
- hospitalName, bodyPartName, projectName
- classicCaseTime, classicCaseRemark, createTime

---

### 任务2.4：创建ClassicCaseConvert转换器

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/convert/ClassicCaseConvert.java`

**方法**：
- `ClassicCaseVO toVO(OrderMainEntity entity)` - 单个转换
- `List<ClassicCaseVO> toVOList(List<OrderMainEntity> entities)` - 批量转换

**实现方式**：使用 `BeanUtil.copyProperties()`

---

## 阶段3：文件迁移服务（核心复杂模块）

### 任务3.1：创建IClassicCaseFileService接口

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/IClassicCaseFileService.java`

**方法**：
```java
List<String> collectOrderFileIds(Long orderId);
void migrateFilesToClassicCase(Long orderId, String orderCode);
```

---

### 任务3.2：实现文件ID收集逻辑（方式一：7类file_id来源）

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/ClassicCaseFileServiceImpl.java`

**需要查询的Mapper**：
- OrderFileMapper - 查询order_file表的file_id
- DesignPackageMapper - 查询design_package表的file_id
- DesignPackageFileMapper - 查询design_package_file表的file_id ⚠️
- DesignPackageFileScreenshotMapper - 查询design_package_file_screenshot表的file_id ⚠️
- DesignModelMapper - 查询design_model表的file_id
- DesignInstructionMapper - 查询template_file_id和revised_file_id
- DesignDrawingMapper - 查询template_file_id和revised_file_id

**核心方法**：
```java
private List<String> collectOrderFileIds(Long orderId) {
    List<String> fileIds = new ArrayList<>();
    
    // 1. 订单文件
    fileIds.addAll(orderFileMapper.selectFileIdsByOrderId(orderId));
    
    // 2. 数据包文件
    fileIds.addAll(designPackageMapper.selectFileIdsByOrderId(orderId));
    
    // 3. 数据包内文件 ⚠️
    fileIds.addAll(designPackageFileMapper.selectFileIdsByOrderId(orderId));
    
    // 4. 文件截图 ⚠️
    fileIds.addAll(designPackageFileScreenshotMapper.selectFileIdsByOrderId(orderId));
    
    // 5. 3D模型
    fileIds.addAll(designModelMapper.selectFileIdsByOrderId(orderId));
    
    // 6. 指令单
    fileIds.addAll(designInstructionMapper.selectFileIdsByOrderId(orderId));
    
    // 7. 图纸
    fileIds.addAll(designDrawingMapper.selectFileIdsByOrderId(orderId));
    
    // 去重过滤
    return fileIds.stream()
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
}
```

---

### 任务3.3：实现文件迁移逻辑（方式一：通过file_detail表）

**核心逻辑**：
1. 根据收集的fileIds批量查询file_detail表
2. 遍历每个文件，调用x-file-storage复制到新路径
3. 批量更新file_detail表的path、basePath、url字段
4. **特殊处理**：同步更新design_package_file表的file_url字段

**新路径规则**：
```
/{platform}/classic-cases/{orderCode}/{业务类型}/{filename}
```

**业务类型映射**：
- order-files - 订单文件
- design-packages - 数据包
- design-files - 设计文件
- models - 3D模型
- instructions - 指令单
- drawings - 图纸
- screenshots - 截图

---

### 任务3.4：实现URL迁移逻辑（方式二：production_record表）

**查询**：ProductionRecordMapper.selectByOrderId(orderId)

**处理流程**：
1. 获取flow_card_file_url字段值
2. 解析URL提取文件路径（如：/orders/2024/03/flow_card.xlsx）
3. 调用x-file-storage复制文件到classic-cases/{orderCode}/flow-cards/
4. 构建新URL
5. 更新production_record表的flow_card_file_url字段

---

### 任务3.5：实现事务性迁移方法

**方法签名**：
```java
@Transactional(rollbackFor = Exception.class)
public void migrateFilesToClassicCase(Long orderId, String orderCode)
```

**流程**：
1. 收集所有file_id（方式一）
2. 批量迁移file_detail关联的文件
3. 更新design_package_file.file_url字段
4. 迁移production_record的flow_card文件（方式二）
5. 记录日志

**异常处理**：任何步骤失败时回滚事务，原文件保留

---

## 阶段4：经典案例业务服务

### 任务4.1：创建IOrderClassicCaseService接口

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/IOrderClassicCaseService.java`

**方法**：
- `void markAsClassicCase(MarkClassicCaseDTO dto)`
- `IPage<ClassicCaseVO> listClassicCases(ClassicCaseQueryDTO dto)`
- `ClassicCaseVO getClassicCaseDetail(Long orderId)`
- `boolean isClassicCase(Long orderId)`

---

### 任务4.2：实现markAsClassicCase标记方法

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderClassicCaseServiceImpl.java`

**流程**：
```java
@Transactional(rollbackFor = Exception.class)
public void markAsClassicCase(MarkClassicCaseDTO dto) {
    // 1. 查询订单
    OrderMainEntity order = orderMainMapper.selectById(dto.getOrderId());
    if (order == null) {
        throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
    }
    
    // 2. 验证订单状态（phase必须为80-已完成）
    if (!Integer.valueOf(80).equals(order.getPhase())) {
        throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_ORDER_NOT_COMPLETED);
    }
    
    // 3. 检查幂等性
    if (StatusConstants.YES.equals(order.getIsClassicCase())) {
        throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_ALREADY_MARKED);
    }
    
    // 4. 更新订单字段
    order.setIsClassicCase(StatusConstants.YES);
    order.setClassicCaseTime(LocalDateTime.now());
    order.setClassicCaseBy(StpUtil.getLoginIdAsLong());
    order.setClassicCaseRemark(dto.getRemark());
    orderMainMapper.updateById(order);
    
    // 5. 调用文件迁移服务
    try {
        classicCaseFileService.migrateFilesToClassicCase(
            order.getId(), 
            order.getOrderCode()
        );
    } catch (Exception e) {
        log.error("文件迁移失败: orderId={}, orderCode={}", 
            order.getId(), order.getOrderCode(), e);
        throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_FILE_MIGRATE_FAILED);
    }
    
    // 6. 记录日志
    log.info("标记订单为经典案例: orderId={}, orderCode={}, operator={}, remark={}", 
        order.getId(), order.getOrderCode(), order.getClassicCaseBy(), dto.getRemark());
}
```

---

### 任务4.3：实现listClassicCases查询列表方法

**查询逻辑**：
1. 构建QueryWrapper（is_classic_case=1）
2. 添加可选过滤条件（orderCode模糊查询、patientName、hospitalId、bodyPartId、时间范围）
3. 执行分页查询
4. 转换Entity为VO（需关联查询hospitalName、bodyPartName等）
5. 返回IPage结果

---

### 任务4.4：实现getClassicCaseDetail详情查询方法

**流程**：
1. 根据orderId查询order_main
2. 验证is_classic_case=1
3. 转换为ClassicCaseVO
4. 返回结果

---

### 任务4.5：实现isClassicCase检查方法

**简单实现**：
```java
public boolean isClassicCase(Long orderId) {
    OrderMainEntity order = orderMainMapper.selectById(orderId);
    return order != null && StatusConstants.YES.equals(order.getIsClassicCase());
}
```

---

## 阶段5：删除和修改保护机制

### 任务5.1：在OrderServiceImpl中添加保护检查方法

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderServiceImpl.java`

**方法**：
```java
/**
 * 检查订单是否为经典案例，是则抛出异常
 */
protected void checkNotClassicCase(Long orderId, String operation) {
    OrderMainEntity order = getById(orderId);
    if (order != null && StatusConstants.YES.equals(order.getIsClassicCase())) {
        log.warn("尝试{}经典案例订单: orderId={}", operation, orderId);
        throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_PROTECTED);
    }
}
```

---

### 任务5.2：在订单删除方法中添加保护

**修改方法**：
- `OrderServiceImpl.removeOrder(Long orderId)`
- `OrderServiceImpl.removeOrderItem(Long itemId)` - 需要先查询item的orderId

**示例**：
```java
@Override
public void removeOrder(Long orderId) {
    checkNotClassicCase(orderId, "删除");
    // 原有删除逻辑
    removeById(orderId);
}
```

---

### 任务5.3：在订单更新方法中添加保护

**修改方法**：
- `OrderServiceImpl.updateOrder(UpdateOrderDTO dto)`

**保护逻辑**：
```java
@Override
public void updateOrder(UpdateOrderDTO dto) {
    checkNotClassicCase(dto.getOrderId(), "修改");
    // 原有更新逻辑
}
```

---

### 任务5.4：在设计模块添加保护（可选，根据需求范围）

**涉及Service**：
- DesignPackageServiceImpl
- DesignProductServiceImpl
- DesignModelServiceImpl
- DesignReviewServiceImpl

**保护方式**：
1. 在删除/修改方法中注入IOrderClassicCaseService
2. 通过orderId检查isClassicCase()
3. 如果是经典案例则抛出异常

---

### 任务5.5：在生产模块添加保护（可选）

**涉及Service**：
- ProductionProcessServiceImpl

**保护方式**：同5.4

---

## 阶段6：Controller层

### 任务6.1：创建OrderClassicCaseController

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderClassicCaseController.java`

**注解**：
- `@RestController`
- `@RequestMapping("/order/classic-case")`
- `@RequiredArgsConstructor`

**依赖注入**：
```java
private final IOrderClassicCaseService classicCaseService;
```

---

### 任务6.2：实现标记接口

**路径**：POST `/order/classic-case/mark`

**方法**：
```java
@PostMapping("/mark")
public Result<Void> markAsClassicCase(@RequestBody @Validated MarkClassicCaseDTO dto) {
    classicCaseService.markAsClassicCase(dto);
    return Result.success();
}
```

---

### 任务6.3：实现查询列表接口

**路径**：POST `/order/classic-case/list`

**方法**：
```java
@PostMapping("/list")
public Result<IPage<ClassicCaseVO>> list(@RequestBody ClassicCaseQueryDTO dto) {
    return Result.success(classicCaseService.listClassicCases(dto));
}
```

---

### 任务6.4：实现查询详情接口

**路径**：GET `/order/classic-case/{orderId}`

**方法**：
```java
@GetMapping("/{orderId}")
public Result<ClassicCaseVO> detail(@PathVariable Long orderId) {
    return Result.success(classicCaseService.getClassicCaseDetail(orderId));
}
```

---

## 阶段7：测试验证

### 任务7.1：编写OrderClassicCaseServiceImpl单元测试

**文件**：`yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderClassicCaseServiceImplTest.java`

**测试用例**：
1. `testMarkAsClassicCase_Success()` - 正常标记
2. `testMarkAsClassicCase_OrderNotFound()` - 订单不存在
3. `testMarkAsClassicCase_OrderNotCompleted()` - 订单未完成
4. `testMarkAsClassicCase_AlreadyMarked()` - 重复标记
5. `testListClassicCases()` - 查询列表
6. `testGetClassicCaseDetail()` - 查询详情
7. `testIsClassicCase()` - 检查方法

**Mock对象**：
- OrderMainMapper
- ClassicCaseFileService
- OrderFileMapper
- DesignPackageMapper等

---

### 任务7.2：编写ClassicCaseFileServiceImpl单元测试

**测试用例**：
1. `testCollectOrderFileIds()` - 文件ID收集
2. `testMigrateFilesToClassicCase()` - 文件迁移
3. `testMigrateWithTransaction_Rollback()` - 失败回滚

---

### 任务7.3：编写OrderClassicCaseController集成测试

**文件**：`yigongbao-boot/src/test/java/com/yigongbao/controller/OrderClassicCaseControllerTest.java`

**使用**：`@SpringBootTest` + MockMvc

**测试用例**：
1. `testMarkAsClassicCase()` - 标记接口
2. `testListClassicCases()` - 查询列表接口
3. `testGetDetail()` - 查询详情接口

---

### 任务7.4：编写保护机制测试

**测试**：
1. 尝试删除经典案例订单 - 应抛出异常620
2. 尝试修改经典案例订单 - 应抛出异常620
3. 删除普通订单 - 应成功

---

### 任务7.5：手动测试文件迁移

**验证点**：
1. 文件是否复制到新路径（/classic-cases/{orderCode}/）
2. file_detail表的path、url字段是否更新
3. design_package_file.file_url是否更新
4. production_record.flow_card_file_url是否更新
5. 原文件是否保留（事务提交前）
6. 失败时是否回滚

---

## 关键风险点

### ⚠️ 风险1：文件迁移失败导致数据不一致

**缓解措施**：
- 使用@Transactional确保数据库回滚
- 文件复制失败时不删除原文件
- 记录详细的错误日志

### ⚠️ 风险2：遗漏保护检查导致经典案例被误删

**缓解措施**：
- 在所有删除/更新方法前添加检查
- 编写完整的保护机制测试用例

### ⚠️ 风险3：production_record表URL解析失败

**缓解措施**：
- 增加URL格式验证
- 记录详细日志
- 失败时抛出明确异常

### ⚠️ 风险4：design_package_file表双字段更新遗漏

**缓解措施**：
- 在代码注释中明确标注该表需要特殊处理
- 编写专门的测试用例验证file_url字段更新

---

## 实施检查清单

- [ ] 阶段1：基础设施搭建（DDL、枚举、实体）
  - [ ] 1.1 执行数据库DDL
  - [ ] 1.2 扩展ErrorCodeEnum
  - [ ] 1.3 扩展OrderMainEntity
  
- [ ] 阶段2：DTO/VO/Convert层
  - [ ] 2.1 创建MarkClassicCaseDTO
  - [ ] 2.2 创建ClassicCaseQueryDTO
  - [ ] 2.3 创建ClassicCaseVO
  - [ ] 2.4 创建ClassicCaseConvert
  
- [ ] 阶段3：文件迁移服务
  - [ ] 3.1 创建IClassicCaseFileService接口
  - [ ] 3.2 实现文件ID收集逻辑（7类来源）
  - [ ] 3.3 实现文件迁移逻辑（方式一）
  - [ ] 3.4 实现URL迁移逻辑（方式二）
  - [ ] 3.5 实现事务性迁移方法
  
- [ ] 阶段4：经典案例业务服务
  - [ ] 4.1 创建IOrderClassicCaseService接口
  - [ ] 4.2 实现markAsClassicCase
  - [ ] 4.3 实现listClassicCases
  - [ ] 4.4 实现getClassicCaseDetail
  - [ ] 4.5 实现isClassicCase
  
- [ ] 阶段5：删除和修改保护
  - [ ] 5.1 添加checkNotClassicCase方法
  - [ ] 5.2 订单删除保护
  - [ ] 5.3 订单更新保护
  - [ ] 5.4 设计模块保护（可选）
  - [ ] 5.5 生产模块保护（可选）
  
- [ ] 阶段6：Controller层
  - [ ] 6.1 创建OrderClassicCaseController
  - [ ] 6.2 实现标记接口
  - [ ] 6.3 实现查询列表接口
  - [ ] 6.4 实现查询详情接口
  
- [ ] 阶段7：测试验证
  - [ ] 7.1 OrderClassicCaseServiceImpl单元测试
  - [ ] 7.2 ClassicCaseFileServiceImpl单元测试
  - [ ] 7.3 OrderClassicCaseController集成测试
  - [ ] 7.4 保护机制测试
  - [ ] 7.5 文件迁移手动测试

---

**计划文档完成**
