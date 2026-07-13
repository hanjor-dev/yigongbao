# UDI手动录入功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将生产产品的UDI码从质检时自动生成改为手动录入模式，提供批量更新接口并包含完整校验逻辑

**Architecture:** 删除ProductionQcServiceImpl中的自动生成逻辑，新增批量更新UDI接口（Controller→Service→Mapper），包含状态、订单类型、唯一性三重校验，遵循TDD开发流程

**Tech Stack:** Spring Boot, MyBatis Plus, Lombok, Validation API, JUnit 5, Mockito

---

## 文件结构映射

### 新增文件
- `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/dto/BatchUpdateUdiDTO.java` - 批量更新UDI请求DTO
- `yigongbao-module-production/src/test/java/com/yigongbao/module/production/qc/service/impl/BatchUpdateUdiTest.java` - 批量更新UDI单元测试

### 修改文件
- `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java` - 新增4个错误码(832-835)
- `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImpl.java:79-84` - 删除UDI自动生成逻辑
- `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/IProductionQcService.java` - 新增batchUpdateUdi方法定义
- `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImpl.java` - 实现batchUpdateUdi方法
- `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/controller/ProductionQcController.java` - 新增batchUpdateUdi接口

---

## Task 1: 新增错误码定义

**Files:**
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

- [ ] **Step 1: 定位错误码插入位置**

在ErrorCodeEnum.java中找到生产模块错误码区域（约800-832行），在ASSIGN_DEVICE_FAILED(831)之后插入新错误码。

- [ ] **Step 2: 添加3个新错误码**

```java
PRODUCTION_RECORD_ALREADY_CLAIMED(832, "流转卡已被其他用户认领", 3),
UDI_CODE_EXISTS(833, "UDI码已存在", 3),
RECORD_STATUS_NOT_ALLOW_UPDATE_UDI(834, "流转卡状态不允许更新UDI", 3),
NON_MEDICAL_NOT_ALLOW_UDI(835, "非医疗器械产品不允许录入UDI", 3),
```

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-common -DskipTests
```

预期输出：BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java
git commit -m "feat(common): 新增UDI相关错误码(833-835)"
```

---

## Task 2: 删除UDI自动生成逻辑

**Files:**
- Modify: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImpl.java:79-84`

- [ ] **Step 1: 定位自动生成代码**

在ProductionQcServiceImpl.markProductPass()方法中找到第79-84行的UDI自动生成逻辑。

- [ ] **Step 2: 删除自动生成代码**

删除以下代码块：
```java
if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(record.getOrderType())) {
    String udiCode = codeGeneratorService.generate(ProductionConstants.UDI_CODE);
    product.setUdiCode(udiCode);
    product.setUdiGenerateTime(LocalDateTime.now());
    log.info("生成UDI码: productId={}, productNo={}, udiCode={}", productId, product.getProductNo(), udiCode);
}
```

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production -DskipTests
```

预期输出：BUILD SUCCESS

- [ ] **Step 4: 运行现有质检测试**

```bash
mvn test -pl yigongbao-module-production -Dtest=ProductionQcServiceImplTest
```

预期：所有测试通过（markProductPass相关测试可能需要调整）

- [ ] **Step 5: 提交**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImpl.java
git commit -m "refactor(production): 删除质检合格时的UDI自动生成逻辑"
```

---

## Task 3: 创建BatchUpdateUdiDTO

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/dto/BatchUpdateUdiDTO.java`

- [ ] **Step 1: 创建DTO文件**

创建新文件，定义批量更新UDI的请求参数，包含流转卡ID和产品UDI列表，使用Validation注解校验必填字段。

- [ ] **Step 2: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production -DskipTests
```

预期输出：BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/dto/BatchUpdateUdiDTO.java
git commit -m "feat(production): 新增批量更新UDI请求DTO"
```

---

## Task 4: 新增Service接口方法

**Files:**
- Modify: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/IProductionQcService.java`

- [ ] **Step 1: 在接口中新增方法定义**

在IProductionQcService接口中添加：

```java
/**
 * 批量更新产品UDI码
 *
 * @param dto 批量更新请求
 * @throws BusinessException 流转卡状态不允许、UDI码重复、非医疗器械等
 */
void batchUpdateUdi(BatchUpdateUdiDTO dto);
```

- [ ] **Step 2: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production -DskipTests
```

预期输出：编译失败（ProductionQcServiceImpl未实现新方法）

- [ ] **Step 3: 提交**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/IProductionQcService.java
git commit -m "feat(production): 新增批量更新UDI接口方法定义"
```

---

## Task 5: 实现Service方法

**Files:**
- Modify: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImpl.java`

- [ ] **Step 1: 实现batchUpdateUdi方法**

在ProductionQcServiceImpl中实现方法，包含三重校验：状态校验、订单类型校验、UDI唯一性校验（批量查询优化）。

- [ ] **Step 2: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production -DskipTests
```

预期输出：BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImpl.java
git commit -m "feat(production): 实现批量更新UDI方法，包含完整校验逻辑"
```

---

## Task 6: 新增Controller接口

**Files:**
- Modify: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/controller/ProductionQcController.java`

- [ ] **Step 1: 新增HTTP接口**

在ProductionQcController中添加：

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

- [ ] **Step 2: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production -DskipTests
```

预期输出：BUILD SUCCESS

- [ ] **Step 3: 启动应用测试接口**

```bash
mvn -pl yigongbao-boot spring-boot:run
```

使用Postman测试：POST http://localhost:8080/api/production/qc/batch-update-udi

- [ ] **Step 4: 提交**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/controller/ProductionQcController.java
git commit -m "feat(production): 新增批量更新UDI的HTTP接口"
```

---

## Task 7: 编写单元测试

**Files:**
- Create: `yigongbao-module-production/src/test/java/com/yigongbao/module/production/qc/service/impl/BatchUpdateUdiTest.java`

- [ ] **Step 1: 创建测试文件，编写测试用例**

测试场景：
1. 状态校验：待打印状态不允许更新
2. 状态校验：打印中状态允许更新
3. 订单类型校验：非医疗器械不允许
4. 唯一性校验：重复UDI抛出异常
5. 唯一性校验：同一产品更新自己的UDI允许
6. 批量更新：多个产品成功更新
7. 空值校验：UDI为空抛出异常

- [ ] **Step 2: 运行测试**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-production -Dtest=BatchUpdateUdiTest
```

预期输出：所有测试通过

- [ ] **Step 3: 运行所有生产模块测试**

```bash
mvn test -pl yigongbao-module-production
```

预期输出：所有测试通过

- [ ] **Step 4: 提交**

```bash
git add yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/qc/service/impl/BatchUpdateUdiTest.java
git commit -m "test(production): 添加批量更新UDI单元测试"
```

---

## 实施完成检查清单

- [ ] 所有错误码已添加并编译通过
- [ ] UDI自动生成逻辑已删除
- [ ] BatchUpdateUdiDTO已创建
- [ ] Service接口和实现已完成
- [ ] Controller接口已完成
- [ ] 单元测试已通过
- [ ] 应用可正常启动
- [ ] Postman手工测试通过

**预计总耗时**：2-3小时

**注意事项**：
1. 每个任务完成后立即提交，保持小步快跑
2. 编译失败时立即修复，不要累积问题
3. 测试驱动开发，先写测试再实现
4. Service实现中的批量查询优化是性能关键点

