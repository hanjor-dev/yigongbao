# Remove Order Modify Apply/Audit Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the apply/audit workflow from order modification system, allowing direct order modifications with phase-based type restrictions and field-level validation.

**Architecture:** Replace apply-driven modification flow with direct modification API. Validation logic shifts from "apply record stores allowed types" to "order phase determines allowed types". Order phase allows all three types (INFO/IMAGE/ITEM), design phase only allows ITEM. Field whitelist validation remains via sys_config.

**Tech Stack:** Spring Boot 3.x, MyBatis Plus 3.5.8, SaToken 1.37.0, Java 21

---

## File Structure

### Files to Modify

**Controller Layer:**
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java`
  - Add new `directModify` endpoint
  - Deprecate apply/audit endpoints

**Service Layer:**
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/OrderModifyApplyService.java`
  - Add `directModify` method signature
  
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java`
  - Implement `directModify` method
  - Extract `determineAllowedTypesByPhase` helper
  - Extract `buildModificationsMap` helper
  - Refactor `executeModification` to support null applyId

**DTO Layer:**
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/modify/DirectModifyDTO.java` (NEW)
  - Wrapper DTO for direct modification request

**Database Layer:**
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/entity/OrderModificationLogEntity.java`
  - Make `applyId` field nullable (already nullable in DB)

### Files to Create for Testing

- `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java`
  - Unit tests for direct modification logic
  
- `yigongbao-module-order/src/test/java/com/yigongbao/module/order/controller/OrderModifyDirectControllerTest.java`
  - Integration tests for direct modification endpoint

---

## Task 1: Add Phase-Based Type Determination Logic

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java:100-150`
- Test: `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java`

- [ ] **Step 1: Write failing test for ORDER phase type determination**

Create test file and add:

```java
package com.yigongbao.module.order.service.impl;

import com.yigongbao.flow.enums.FlowPhaseEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderModifyDirectServiceTest {

    @InjectMocks
    private OrderModifyApplyServiceImpl service;

    @Test
    void testDetermineAllowedTypesByPhase_OrderPhase_ReturnsAllTypes() {
        Set<String> result = service.determineAllowedTypesByPhase(FlowPhaseEnum.ORDER.getValue());
        
        assertEquals(3, result.size());
        assertTrue(result.contains("14.1"));
        assertTrue(result.contains("14.2"));
        assertTrue(result.contains("14.3"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDetermineAllowedTypesByPhase_OrderPhase_ReturnsAllTypes -pl yigongbao-module-order`

Expected: FAIL with "method determineAllowedTypesByPhase does not exist"

- [ ] **Step 3: Implement determineAllowedTypesByPhase method**

Add to `OrderModifyApplyServiceImpl.java` after line 148:

```java
/**
 * 根据订单阶段判断允许的修改类型
 * <p>
 * 订单阶段（phase=10）：允许全部三种类型（14.1/14.2/14.3）<br>
 * 设计阶段（phase=20）：仅允许重建项目（14.3）<br>
 * 其他阶段：抛出异常
 *
 * @param phase 订单阶段值
 * @return 允许的类型编码集合
 */
private Set<String> determineAllowedTypesByPhase(Integer phase) {
    if (FlowPhaseEnum.ORDER.getValue().equals(phase)) {
        return Set.of(
            ModifyApplyTypeEnum.INFO.getDictCode(),
            ModifyApplyTypeEnum.IMAGE.getDictCode(),
            ModifyApplyTypeEnum.ITEM.getDictCode()
        );
    } else if (FlowPhaseEnum.DESIGN.getValue().equals(phase)) {
        return Set.of(ModifyApplyTypeEnum.ITEM.getDictCode());
    } else {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_APPLICABLE_STATUS);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDetermineAllowedTypesByPhase_OrderPhase_ReturnsAllTypes -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 5: Write test for DESIGN phase**

Add to test file:

```java
@Test
void testDetermineAllowedTypesByPhase_DesignPhase_ReturnsItemOnly() {
    Set<String> result = service.determineAllowedTypesByPhase(FlowPhaseEnum.DESIGN.getValue());
    
    assertEquals(1, result.size());
    assertTrue(result.contains("14.3"));
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDetermineAllowedTypesByPhase_DesignPhase_ReturnsItemOnly -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 7: Write test for invalid phase**

Add to test file:

```java
@Test
void testDetermineAllowedTypesByPhase_InvalidPhase_ThrowsException() {
    assertThrows(BusinessException.class, () -> {
        service.determineAllowedTypesByPhase(30);
    });
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDetermineAllowedTypesByPhase_InvalidPhase_ThrowsException -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 9: Commit phase determination logic**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java
git add yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java
git commit -m "feat: add phase-based modification type determination"
```

---

## Task 2: Extract Modifications Map Builder

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java:340-375`
- Test: `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java`

- [ ] **Step 1: Write failing test for buildModificationsMap**

Add to test file:

```java
@Test
void testBuildModificationsMap_WithInfoFields_ReturnsCorrectMap() {
    ExecuteModifyDTO dto = new ExecuteModifyDTO();
    ExecuteModifyDTO.ModifyField field = new ExecuteModifyDTO.ModifyField();
    field.setField("patientName");
    field.setValue("张三");
    dto.setInfoFields(List.of(field));
    
    Map<String, Object> result = service.buildModificationsMap(dto);
    
    assertNotNull(result);
    assertTrue(result.containsKey("patientName"));
    assertEquals("张三", result.get("patientName"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testBuildModificationsMap_WithInfoFields_ReturnsCorrectMap -pl yigongbao-module-order`

Expected: FAIL with "method buildModificationsMap does not exist"

- [ ] **Step 3: Extract buildModificationsMap from executeModification**

Refactor lines 340-375 in `OrderModifyApplyServiceImpl.java` into new method after line 449:

```java
/**
 * 将 ExecuteModifyDTO 转换为 Map 结构（供内部处理方法使用）
 */
private Map<String, Object> buildModificationsMap(ExecuteModifyDTO dto) {
    Map<String, Object> modifications = new HashMap<>();
    if (dto != null) {
        if (dto.getInfoFields() != null) {
            dto.getInfoFields().forEach(f -> {
                if (StrUtil.isNotBlank(f.getField())) {
                    modifications.put(f.getField(), f.getValue());
                }
            });
        }
        if (dto.getItems() != null) {
            List<Map<String, Object>> itemMaps = dto.getItems().stream().map(item -> {
                Map<String, Object> m = new HashMap<>();
                if (item.getOrderItemId() != null) {
                    m.put("orderItemId", item.getOrderItemId());
                }
                if (item.getFields() != null) {
                    item.getFields().forEach(f -> {
                        if (StrUtil.isNotBlank(f.getField())) {
                            m.put(f.getField(), f.getValue());
                        }
                    });
                }
                return m;
            }).collect(Collectors.toList());
            modifications.put("items", itemMaps);
        }
        if (dto.getImageDataFileIds() != null) {
            modifications.put("imageDataFileIds", dto.getImageDataFileIds());
        }
        if (dto.getImageReportFileIds() != null) {
            modifications.put("imageReportFileIds", dto.getImageReportFileIds());
        }
    }
    return modifications;
}
```

- [ ] **Step 4: Update executeModification to use extracted method**

Replace lines 340-375 in `executeModification` with:

```java
Map<String, Object> modifications = buildModificationsMap(dto);
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testBuildModificationsMap_WithInfoFields_ReturnsCorrectMap -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 6: Commit modifications map builder**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java
git add yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java
git commit -m "refactor: extract buildModificationsMap helper method"
```

---

## Task 3: Add directModify Method to Service Interface

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/OrderModifyApplyService.java:139`

- [ ] **Step 1: Add directModify method signature to interface**

Add after line 139 in `OrderModifyApplyService.java`:

```java
/**
 * 直接修改订单（无需申请审核流程）
 * 根据订单当前阶段判断允许的修改类型：
 * - 订单阶段（phase=10）：允许全部三种类型（14.1/14.2/14.3）
 * - 设计阶段（phase=20）：仅允许重建项目（14.3）
 *
 * @param orderId 订单ID
 * @param dto     修改内容
 */
void directModify(Long orderId, ExecuteModifyDTO dto);
```

- [ ] **Step 2: Commit interface change**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/OrderModifyApplyService.java
git commit -m "feat: add directModify method signature to service interface"
```

---

## Task 4: Implement directModify Method

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java:450`
- Test: `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java`

- [ ] **Step 1: Write failing test for directModify with ORDER phase**

Add to test file:

```java
@Mock
private OrderMainMapper orderMainMapper;
@Mock
private OrderModificationLogMapper orderModificationLogMapper;
@Mock
private ConfigService configService;
@Mock
private OrderDataValidator orderDataValidator;

@Test
void testDirectModify_OrderPhase_InfoModification_Success() {
    Long orderId = 1L;
    OrderMainEntity order = new OrderMainEntity();
    order.setId(orderId);
    order.setPhase(FlowPhaseEnum.ORDER.getValue());
    order.setOrderCode("ORD001");
    
    ExecuteModifyDTO dto = new ExecuteModifyDTO();
    ExecuteModifyDTO.ModifyField field = new ExecuteModifyDTO.ModifyField();
    field.setField("patientName");
    field.setValue("李四");
    dto.setInfoFields(List.of(field));
    
    when(orderMainMapper.selectById(orderId)).thenReturn(order);
    
    assertDoesNotThrow(() -> service.directModify(orderId, dto));
    
    verify(orderMainMapper).selectById(orderId);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDirectModify_OrderPhase_InfoModification_Success -pl yigongbao-module-order`

Expected: FAIL with "method directModify does not exist"

- [ ] **Step 3: Implement directModify method**

Add after line 449 in `OrderModifyApplyServiceImpl.java`:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void directModify(Long orderId, ExecuteModifyDTO dto) {
    log.info("直接修改订单，orderId={}", orderId);
    
    // 1. 查询订单
    OrderMainEntity order = orderMainMapper.selectById(orderId);
    if (order == null) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }
    
    // 2. 根据阶段判断允许的修改类型
    Set<String> allowedTypes = determineAllowedTypesByPhase(order.getPhase());
    
    // 3. 加载字段配置
    ModifyApplyFieldConfigDTO fieldConfig = loadFieldConfig();
    
    // 4. 校验字段白名单
    if (dto != null && dto.getInfoFields() != null && !dto.getInfoFields().isEmpty()) {
        ModifyApplyFieldConfigDTO.TypeConfig infoTypeConfig =
                fieldConfig.getTypeConfig(ModifyApplyTypeEnum.INFO.getDictCode());
        validateInfoFieldsInWhitelist(dto.getInfoFields(), infoTypeConfig);
    }
    
    // 5. 构建修改内容 Map
    Map<String, Object> modifications = buildModificationsMap(dto);
    
    // 6. 获取当前操作人
    Long modifierId = StpUtil.getLoginIdAsLong();
    String modifierName = getCurrentUserName();
    
    // 7. 处理基础信息修改（14.1）
    boolean infoModified = false;
    if (allowedTypes.contains(ModifyApplyTypeEnum.INFO.getDictCode()) 
            && !modifications.isEmpty()) {
        infoModified = processInfoModification(order, modifications, null, 
                modifierId, modifierName, fieldConfig);
    }
    
    // 8. 处理重建项目修改（14.3）
    if (allowedTypes.contains(ModifyApplyTypeEnum.ITEM.getDictCode()) 
            && modifications.containsKey("items")) {
        processItemModification(order, modifications, null, 
                modifierId, modifierName, fieldConfig);
    }
    
    // 9. 处理影像文件修改（14.2）
    if (allowedTypes.contains(ModifyApplyTypeEnum.IMAGE.getDictCode())) {
        processImageModification(order, modifications, null, 
                modifierId, modifierName);
    }
    
    // 10. 仅 INFO 类型修改了 order 实体字段时才回写 DB
    if (infoModified) {
        orderMainMapper.updateById(order);
    }
    
    log.info("直接修改订单成功，orderId={}", orderId);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDirectModify_OrderPhase_InfoModification_Success -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 5: Commit directModify implementation**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java
git add yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java
git commit -m "feat: implement directModify method for direct order modification"
```

---

## Task 5: Add Validation Tests for directModify

**Files:**
- Test: `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java`

- [ ] **Step 1: Write test for order not found**

Add to test file:

```java
@Test
void testDirectModify_OrderNotFound_ThrowsException() {
    Long orderId = 999L;
    ExecuteModifyDTO dto = new ExecuteModifyDTO();
    
    when(orderMainMapper.selectById(orderId)).thenReturn(null);
    
    BusinessException ex = assertThrows(BusinessException.class, 
        () -> service.directModify(orderId, dto));
    assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(), ex.getCode());
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDirectModify_OrderNotFound_ThrowsException -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 3: Write test for DESIGN phase restricts to ITEM only**

Add to test file:

```java
@Test
void testDirectModify_DesignPhase_InfoModification_Ignored() {
    Long orderId = 1L;
    OrderMainEntity order = new OrderMainEntity();
    order.setId(orderId);
    order.setPhase(FlowPhaseEnum.DESIGN.getValue());
    order.setOrderCode("ORD001");
    
    ExecuteModifyDTO dto = new ExecuteModifyDTO();
    ExecuteModifyDTO.ModifyField field = new ExecuteModifyDTO.ModifyField();
    field.setField("patientName");
    field.setValue("李四");
    dto.setInfoFields(List.of(field));
    
    when(orderMainMapper.selectById(orderId)).thenReturn(order);
    
    assertDoesNotThrow(() -> service.directModify(orderId, dto));
    
    // INFO modification should be ignored in DESIGN phase
    verify(orderMainMapper, never()).updateById(any());
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDirectModify_DesignPhase_InfoModification_Ignored -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 5: Write test for invalid phase**

Add to test file:

```java
@Test
void testDirectModify_InvalidPhase_ThrowsException() {
    Long orderId = 1L;
    OrderMainEntity order = new OrderMainEntity();
    order.setId(orderId);
    order.setPhase(30); // Invalid phase
    
    ExecuteModifyDTO dto = new ExecuteModifyDTO();
    
    when(orderMainMapper.selectById(orderId)).thenReturn(order);
    
    assertThrows(BusinessException.class, 
        () -> service.directModify(orderId, dto));
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectServiceTest#testDirectModify_InvalidPhase_ThrowsException -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 7: Commit validation tests**

```bash
git add yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyDirectServiceTest.java
git commit -m "test: add validation tests for directModify method"
```

---

## Task 6: Add Controller Endpoint for Direct Modification

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java:85`

- [ ] **Step 1: Add directModify endpoint to controller**

Add after line 84 in `OrderModifyApplyController.java`:

```java
@Operation(summary = "直接修改订单（无需申请审核）",
        description = "根据订单当前阶段判断允许的修改类型：\n"
                + "订单阶段（phase=10）：允许全部三种类型（14.1基础信息/14.2影像文件/14.3重建项目）\n"
                + "设计阶段（phase=20）：仅允许重建项目（14.3）\n"
                + "参数说明同 executeModification 接口")
@RequirePermission(value = "order:Modify")
@PutMapping("/{orderId}/direct")
public Result<Void> directModify(@PathVariable Long orderId,
        @Valid @RequestBody ExecuteModifyDTO dto) {
    orderModifyApplyService.directModify(orderId, dto);
    return Result.success();
}
```

- [ ] **Step 2: Commit controller endpoint**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java
git commit -m "feat: add directModify controller endpoint"
```

---

## Task 7: Refactor executeModification to Support Null applyId

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java:520-577`

- [ ] **Step 1: Update processInfoModification to handle null applyId**

Modify line 575 in `OrderModifyApplyServiceImpl.java` to pass null-safe applyId:

```java
return recordChangesFromSnapshots(order.getId(), order.getOrderCode(), applyId,
        beforeSnapshot, afterSnapshot, allFields, modifierId, modifierName);
```

No change needed - already supports null applyId.

- [ ] **Step 2: Update processItemModification to handle null applyId**

Verify lines 742, 762, 771 in `processItemModification` already pass applyId correctly (supports null).

No change needed - already supports null applyId.

- [ ] **Step 3: Update recordModificationLog to handle null applyId**

Verify the method already handles null applyId correctly. Check implementation around line 850.

No change needed if already nullable.

- [ ] **Step 4: Run existing executeModification tests**

Run: `mvn test -Dtest=*OrderModifyApplyServiceImplTest -pl yigongbao-module-order`

Expected: All existing tests still PASS

- [ ] **Step 5: Commit refactoring**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java
git commit -m "refactor: ensure executeModification supports null applyId"
```

---

## Task 8: Deprecate Apply/Audit Endpoints

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java:48-142`

- [ ] **Step 1: Add @Deprecated to createApply endpoint**

Add `@Deprecated` annotation before line 58:

```java
@Deprecated(since = "2026-05-20", forRemoval = true)
@Operation(summary = "发起修改申请（已废弃，请使用直接修改接口）")
@RequirePermission(value = "order:ApplyModify")
@PostMapping("/{orderId}/apply")
public Result<ModifyApplyVO> createApply(@PathVariable Long orderId,
        @Valid @RequestBody CreateModifyApplyDTO dto) {
    return Result.success(orderModifyApplyService.createApply(orderId, dto));
}
```

- [ ] **Step 2: Add @Deprecated to auditApply endpoint**

Add `@Deprecated` annotation before line 112:

```java
@Deprecated(since = "2026-05-20", forRemoval = true)
@Operation(summary = "审核修改申请（已废弃）")
@RequirePermission(value = "order:Approve")
@PutMapping("/apply/{applyId}/audit")
public Result<Void> auditApply(@PathVariable Long applyId,
        @Valid @RequestBody AuditModifyApplyDTO dto) {
    orderModifyApplyService.auditApply(applyId, dto);
    return Result.success();
}
```

- [ ] **Step 3: Add @Deprecated to withdrawApply endpoint**

Add `@Deprecated` annotation before line 104:

```java
@Deprecated(since = "2026-05-20", forRemoval = true)
@Operation(summary = "撤回修改申请（已废弃）")
@RequirePermission(value = "order:MyApplyWithdraw")
@DeleteMapping("/apply/{applyId}")
public Result<Void> withdrawApply(@PathVariable Long applyId) {
    orderModifyApplyService.withdrawApply(applyId);
    return Result.success();
}
```

- [ ] **Step 4: Add @Deprecated to executeModification endpoint**

Add `@Deprecated` annotation before line 66:

```java
@Deprecated(since = "2026-05-20", forRemoval = true)
@Operation(summary = "执行订单修改（已废弃，请使用直接修改接口）",
        description = "必须提供已审核通过（APPROVED 状态）的 applyId...")
@RequirePermission(value = "order:Modify")
@PutMapping("/execute/{applyId}")
public Result<Void> executeModification(@PathVariable Long applyId,
        @Valid @RequestBody ExecuteModifyDTO dto) {
    orderModifyApplyService.executeModification(applyId, dto);
    return Result.success();
}
```

- [ ] **Step 5: Commit deprecation annotations**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java
git commit -m "deprecate: mark apply/audit endpoints as deprecated"
```

---

## Task 9: Add Integration Tests for Controller Endpoint

**Files:**
- Create: `yigongbao-module-order/src/test/java/com/yigongbao/module/order/controller/OrderModifyDirectControllerTest.java`

- [ ] **Step 1: Create controller integration test file**

Create new test file:

```java
package com.yigongbao.module.order.controller;

import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderModifyApplyController.class)
class OrderModifyDirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderModifyApplyService orderModifyApplyService;

    @Test
    void testDirectModify_Success() throws Exception {
        Long orderId = 1L;
        String requestBody = """
            {
                "infoFields": [
                    {"field": "patientName", "value": "张三"}
                ]
            }
            """;

        doNothing().when(orderModifyApplyService).directModify(eq(orderId), any(ExecuteModifyDTO.class));

        mockMvc.perform(put("/order/modify/{orderId}/direct", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderModifyApplyService).directModify(eq(orderId), any(ExecuteModifyDTO.class));
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `mvn test -Dtest=OrderModifyDirectControllerTest#testDirectModify_Success -pl yigongbao-module-order`

Expected: PASS

- [ ] **Step 3: Commit integration test**

```bash
git add yigongbao-module-order/src/test/java/com/yigongbao/module/order/controller/OrderModifyDirectControllerTest.java
git commit -m "test: add integration test for directModify endpoint"
```

---

## Task 10: Update API Documentation

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java:32-43`

- [ ] **Step 1: Update controller class documentation**

Update class-level comment at line 32-37:

```java
/**
 * 订单修改申请 Controller
 * 
 * 【推荐使用】直接修改接口：/{orderId}/direct
 * 【已废弃】申请审核流程接口：/{orderId}/apply, /apply/{applyId}/audit 等
 *
 * @author hanjor
 * @date 2026-04-09
 */
```

- [ ] **Step 2: Commit documentation update**

```bash
git add yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java
git commit -m "docs: update controller documentation for direct modify"
```

---

## Task 11: Final Verification and Testing

**Files:**
- All modified files

- [ ] **Step 1: Run all unit tests**

Run: `mvn test -pl yigongbao-module-order`

Expected: All tests PASS

- [ ] **Step 2: Run full build**

Run: `mvn clean package -DskipTests -pl yigongbao-module-order`

Expected: BUILD SUCCESS

- [ ] **Step 3: Start application and verify Swagger UI**

Run: `mvn -pl yigongbao-boot spring-boot:run`

Navigate to: `http://localhost:8080/api/swagger-ui.html`

Verify:
- New endpoint `/order/modify/{orderId}/direct` is visible
- Deprecated endpoints show deprecation notice
- API documentation is correct

- [ ] **Step 4: Manual API test - ORDER phase allows all types**

```bash
curl -X PUT "http://localhost:8080/api/order/modify/1/direct" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "infoFields": [
      {"field": "patientName", "value": "测试患者"}
    ]
  }'
```

Expected: 200 OK, modification recorded in `order_modification_log`

- [ ] **Step 5: Manual API test - DESIGN phase restricts to ITEM only**

```bash
curl -X PUT "http://localhost:8080/api/order/modify/2/direct" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "items": [
      {
        "orderItemId": 1,
        "fields": [{"field": "projectDesc", "value": "更新描述"}]
      }
    ]
  }'
```

Expected: 200 OK, item modification successful

- [ ] **Step 6: Verify modification logs**

Query database:

```sql
SELECT * FROM order_modification_log 
WHERE order_id IN (1, 2) 
ORDER BY create_time DESC 
LIMIT 10;
```

Expected: Records exist with `apply_id = NULL`, showing direct modifications

- [ ] **Step 7: Final commit**

```bash
git add .
git commit -m "feat: complete direct order modification without apply/audit workflow"
```

---

## Summary

This plan removes the apply/audit workflow from order modification system while maintaining field-level validation and phase-based restrictions.

**Key Changes:**
- New `directModify(orderId, dto)` method replaces `executeModification(applyId, dto)`
- Modification type control shifts from apply record to order phase
- ORDER phase (10): allows all three types (INFO/IMAGE/ITEM)
- DESIGN phase (20): allows only ITEM type
- Field whitelist validation remains via sys_config
- Modification logs support null applyId
- Old apply/audit endpoints marked as @Deprecated

**Testing Strategy:**
- Unit tests for phase determination logic
- Unit tests for direct modification flow
- Integration tests for controller endpoint
- Manual verification via Swagger UI and database queries

**Migration Path:**
- New direct modification API available immediately
- Old apply/audit endpoints remain functional (deprecated)
- Frontend can migrate gradually to new API
- Historical apply records remain queryable

**Total Tasks:** 11
**Estimated Time:** 2-3 hours
**Risk Level:** Low (additive change, no breaking changes)

---

## Next Steps

After plan approval, choose execution approach:
1. **Subagent-Driven (recommended)**: Fresh subagent per task with review between tasks
2. **Inline Execution**: Execute tasks in this session with checkpoints

