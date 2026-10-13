# 订单修改执行逻辑配置驱动重构 实施计划

> **For agentic workers:** 按任务顺序逐步执行，每个步骤完成后打勾。

**Goal:** 彻底消除 `OrderModifyApplyServiceImpl` 中 `processInfoModification`、`applyInfoFields`、`logInfoFieldChanges`、`compareAndLogItemFields` 的硬编码字段名，改为基于 `sys_config` 配置 + BeanUtil 反射的完全配置驱动实现，使配置增删字段后 Service 层无需任何代码改动。

**Architecture:**
- `sys_config` 中每个字段配置新增 `group` 属性，标记需要走 `OrderDataValidator` 的字段组（`hospital_doctor`）
- `ModifyApplyFieldConfigDTO.FieldConfig` 新增 `group`、`subFields` 字段；`TypeConfig` 新增 `getFieldsByGroup()`、`getItemSubFields()` 方法
- `OrderModifyApplyServiceImpl` 删除4个硬编码私有方法，用3个通用反射工具方法 + 重写后的 `processInfoModification` 替代；14.3 留痕也改为配置驱动

**Tech Stack:** Java 21, Hutool BeanUtil 5.8.26, MyBatis Plus 3.5.8, JUnit 5 + Mockito

---

## 文件改动清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `sql/init.sql` | 修改 | `order.modify.field.config` 中每个字段加 `group`；补齐漏配字段 `isUrgent`、`hospitalId` |
| `ModifyApplyFieldConfigDTO.java` | 修改 | `FieldConfig` 加 `group`/`subFields`；`TypeConfig` 加 `getFieldsByGroup()`/`getItemSubFields()` |
| `OrderModifyApplyServiceImpl.java` | 修改 | 删除旧4方法+4转换辅助方法；新增3个通用工具方法；重写 `processInfoModification`；改造 item 留痕 |
| `OrderModifyApplyServiceImplTest.java` | 修改 | 增加新场景测试（配置驱动路径、未知字段静默忽略、group 驱动 validator） |
| `03_订单修改审核实现方案.md` | 修改 | 更新 executeModification 实现描述，记录配置驱动重构内容（新增 v6.0）|
| `19_订单模块接口文档.md` | 修改 | 更新 19.21 执行订单修改的请求参数说明 |

---

## Task 1：更新 sys_config 配置 JSON

**Files:**
- Modify: `sql/init.sql`（搜索 `order.modify.field.config`，找到第 167 行附近）

### 目标

在 14.1 的每个字段加 `"group"` 属性，补齐漏配的 `hospitalId`/`isUrgent`；14.3 的 subFields 保持不变（group 在 subFields 层面暂不需要，item 字段没有特殊处理路径）。

- [ ] **Step 1.1：定位并替换 init.sql 中的 JSON 配置**

将 `order.modify.field.config` 的 JSON 值改为：

```json
{
  "14.1": {
    "name": "基础信息",
    "fields": [
      {"field":"hospitalId","label":"医院","type":"autocomplete","required":false,"group":"hospital_doctor"},
      {"field":"hospitalDeptId","label":"科室","type":"autocomplete","required":false,"group":"hospital_doctor"},
      {"field":"doctorId","label":"关联医生","type":"autocomplete","required":false,"group":"hospital_doctor"},
      {"field":"doctorName","label":"医生姓名","type":"text","required":false,"group":"hospital_doctor"},
      {"field":"doctorPhone","label":"医生电话","type":"text","required":false,"group":"hospital_doctor"},
      {"field":"patientName","label":"患者姓名","type":"text","required":false},
      {"field":"patientAge","label":"患者年龄","type":"number","required":false},
      {"field":"patientGender","label":"患者性别","type":"select","required":false,"options":[{"value":"12.1","label":"男"},{"value":"12.2","label":"女"}]},
      {"field":"isUrgent","label":"是否加急","type":"switch","required":false},
      {"field":"isPostal","label":"是否邮寄","type":"switch","required":false},
      {"field":"postalAddress","label":"邮寄地址","type":"textarea","required":false},
      {"field":"expectedDeliveryDate","label":"期望交付时间","type":"datetime","required":false}
    ]
  },
  "14.2": {
    "name": "影像文件",
    "fields": [
      {"field":"imageDataFileIds","label":"影像数据文件","type":"file","required":false},
      {"field":"imageReportFileIds","label":"影像报告文件","type":"file","required":false}
    ]
  },
  "14.3": {
    "name": "重建项目",
    "fields": [
      {
        "field":"items","label":"重建项目明细","type":"array","required":false,
        "subFields":[
          {"field":"bodyPartId","label":"部位","type":"select"},
          {"field":"projectId","label":"重建项目","type":"select"},
          {"field":"projectDesc","label":"项目说明","type":"textarea"},
          {"field":"formingRequirement","label":"成形需求","type":"textarea"},
          {"field":"otherRequirement","label":"其他要求","type":"textarea"}
        ]
      }
    ]
  }
}
```

> 变更点：14.1 中新增 `hospitalId`/`hospitalDeptId`/`doctorName` 三个字段并打 `group`；`doctorPhone` 补 `group`；新增 `isUrgent`；去掉原来没有 `group` 的 `doctorId` 并补上 `group`。

---

## Task 2：扩展 ModifyApplyFieldConfigDTO

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/modify/ModifyApplyFieldConfigDTO.java`

### 目标

1. `FieldConfig` 新增 `group`（字段分组）和 `subFields`（array 类型子字段列表）
2. `TypeConfig` 新增：
   - `getFieldsByGroup(String group)`：返回指定 group 的字段名列表
   - `getItemSubFields()`：返回 items 数组字段的 subFields 列表（用于 14.3）
3. `ModifyApplyFieldConfigDTO` 新增：
   - `getItemSubFields()`：顶层快捷方法，直接取 14.3 的 item 子字段配置

- [ ] **Step 2.1：修改 `FieldConfig` 内部类，新增 `group` 和 `subFields` 字段**

```java
@Data
public static class FieldConfig {
    private String field;
    private String label;
    private String type;
    private Boolean required;
    /** 字段分组，如 "hospital_doctor" 表示需走 OrderDataValidator */
    private String group;
    /** array 类型字段的子字段配置列表 */
    private List<FieldConfig> subFields;
}
```

- [ ] **Step 2.2：在 `TypeConfig` 中新增 `getFieldsByGroup()` 和 `getItemSubFields()` 方法**

```java
/**
 * 获取指定 group 的字段名列表
 */
public List<String> getFieldsByGroup(String group) {
    if (fields == null) return List.of();
    return fields.stream()
            .filter(f -> group.equals(f.getGroup()))
            .map(FieldConfig::getField)
            .toList();
}

/**
 * 获取 array 类型字段（items）的子字段配置列表
 * 用于 14.3 重建项目留痕时遍历字段
 */
public List<FieldConfig> getItemSubFields() {
    if (fields == null) return List.of();
    return fields.stream()
            .filter(f -> "array".equals(f.getType()) && f.getSubFields() != null)
            .flatMap(f -> f.getSubFields().stream())
            .toList();
}
```

- [ ] **Step 2.3：（无需新增顶层快捷方法）**

> `ModifyApplyFieldConfigDTO` 处于 `dto.modify` 包，不应依赖 `enums` 包的 `ModifyApplyTypeEnum`。
> `getItemSubFields()` 直接在 `OrderModifyApplyServiceImpl` 中调用：
> ```java
> fieldConfig.getTypeConfig(ModifyApplyTypeEnum.ITEM.getDictCode()).getItemSubFields()
> ```
> 无需在 DTO 层新增快捷方法。

---

## Task 3：重构 OrderModifyApplyServiceImpl

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java`

### 3.1 删除旧方法

删除以下全部私有方法（共8个）：
- `processInfoModification(...)` — 约第 431 行
- `applyInfoFields(...)` — 约第 460 行
- `logInfoFieldChanges(...)` — 约第 577 行
- `compareAndLogItemFields(...)` — 约第 713 行
- `getStringValue(...)` — 约第 514 行
- `getLongValue(...)` — 约第 526 行
- `getIntegerValue(...)` — 约第 538 行
- `getLocalDateTimeValue(...)` — 约第 550 行

- [ ] **Step 3.1：删除上述8个旧方法**

### 3.2 新增3个通用工具方法

- [ ] **Step 3.2：新增 `convertFieldValue` 类型转换方法**

```java
/**
 * 按字段配置的 type 将原始值转换为目标类型
 * type 映射规则：
 *   text/textarea/autocomplete/select → String
 *   number/switch → Integer
 *   datetime → LocalDateTime（支持 ISO 字符串或毫秒时间戳）
 *   file/array → 保留原值（不做单值转换）
 *   未知 type → 原值返回
 */
private Object convertFieldValue(String type, Object raw) {
    if (raw == null || type == null) return null;
    return switch (type) {
        case "text", "textarea", "autocomplete" -> {
            String s = Convert.convert(String.class, raw);
            yield StrUtil.isBlank(s) ? null : s;
        }
        case "select" -> {
            // select 可能是字符串（字典码）或数值
            String s = Convert.convert(String.class, raw);
            yield StrUtil.isBlank(s) ? null : s;
        }
        case "number", "switch" -> Convert.convert(Integer.class, raw);
        case "datetime" -> {
            if (raw instanceof LocalDateTime ldt) yield ldt;
            if (raw instanceof String str && StrUtil.isNotBlank(str)) {
                try {
                    yield cn.hutool.core.date.DateUtil.parseLocalDateTime(str);
                } catch (Exception e) {
                    log.warn("datetime 解析失败，raw={}", raw);
                    yield null;
                }
            }
            if (raw instanceof Number num) {
                yield LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(num.longValue()),
                        java.time.ZoneId.of("Asia/Shanghai"));
            }
            yield null;
        }
        default -> raw;
    };
}
```

- [ ] **Step 3.3：新增 `snapshotFields` 字段快照方法**

```java
/**
 * 用反射快照 Entity 中指定字段的当前值
 *
 * @param entity     实体对象
 * @param fieldNames 要快照的字段名列表（来自配置的 allowedFields）
 * @return 字段名 → 当前值 的 Map（字段不存在时静默跳过）
 */
private Map<String, Object> snapshotFields(Object entity, List<String> fieldNames) {
    Map<String, Object> snapshot = new HashMap<>();
    for (String field : fieldNames) {
        try {
            snapshot.put(field, BeanUtil.getFieldValue(entity, field));
        } catch (Exception e) {
            log.warn("反射读取字段失败，字段可能不存在，field={}", field);
        }
    }
    return snapshot;
}
```

- [ ] **Step 3.4：新增 `recordChangesFromSnapshots` 留痕对比方法**

```java
/**
 * 对比赋值前后的两份快照，逐字段记录变更留痕
 * 只对 fieldConfigs 中定义的字段进行比对（白名单控制）
 *
 * @param orderId      订单ID
 * @param orderCode    订单编号
 * @param applyId      申请ID
 * @param before       赋值前快照（字段名 → 旧值）
 * @param after        赋值后快照（字段名 → 新值）
 * @param fieldConfigs 字段配置列表（来自配置，含 label）
 * @param modifierId   操作人ID
 * @param modifierName 操作人姓名
 */
private void recordChangesFromSnapshots(Long orderId, String orderCode, Long applyId,
        Map<String, Object> before, Map<String, Object> after,
        List<ModifyApplyFieldConfigDTO.FieldConfig> fieldConfigs,
        Long modifierId, String modifierName) {
    for (ModifyApplyFieldConfigDTO.FieldConfig fc : fieldConfigs) {
        String field = fc.getField();
        Object oldVal = before.get(field);
        Object newVal = after.get(field);
        recordIfChanged(orderId, orderCode, applyId, field, fc.getLabel(),
                oldVal, newVal, modifierId, modifierName);
    }
}
```

### 3.3 重写 processInfoModification

- [ ] **Step 3.5：新增重写后的 `processInfoModification` 方法**

```java
/**
 * 处理基础信息修改（配置驱动：快照→赋值→留痕，全部通过反射+配置白名单）
 * <p>
 * 流程：
 * 1. 从配置取 14.1 的字段列表（含 group）
 * 2. 反射快照所有配置字段的旧值
 * 3. group=hospital_doctor 的字段：走 OrderDataValidator 统一处理（含冗余字段同步）
 * 4. 其他字段：反射赋值（仅处理 modifications 中存在的 key）
 * 5. 再次快照新值，与旧值对比，调用 recordChangesFromSnapshots 记录留痕
 */
private void processInfoModification(OrderMainEntity order, Map<String, Object> modifications,
        Long applyId, Long modifierId, String modifierName, ModifyApplyFieldConfigDTO fieldConfig) {

    ModifyApplyFieldConfigDTO.TypeConfig typeConfig =
            fieldConfig.getTypeConfig(ModifyApplyTypeEnum.INFO.getDictCode());
    if (typeConfig == null || CollUtil.isEmpty(typeConfig.getFields())) {
        log.warn("未找到 14.1 字段配置，跳过基础信息修改");
        return;
    }

    List<ModifyApplyFieldConfigDTO.FieldConfig> allFieldConfigs = typeConfig.getFields();
    List<String> allFieldNames = typeConfig.getAllowedFields(); // 配置中的全部字段名

    // 1. 快照赋值前的旧值（反射）
    Map<String, Object> beforeSnapshot = snapshotFields(order, allFieldNames);

    // 2. hospital_doctor 分组字段：走 validator（含冗余字段同步、存在性校验）
    List<String> hdFields = typeConfig.getFieldsByGroup("hospital_doctor");
    boolean hasHdChange = hdFields.stream().anyMatch(modifications::containsKey);
    if (hasHdChange) {
        Long hospitalId    = modifications.containsKey("hospitalId")
                ? Convert.convert(Long.class, modifications.get("hospitalId")) : null;
        Long hospitalDeptId = modifications.containsKey("hospitalDeptId")
                ? Convert.convert(Long.class, modifications.get("hospitalDeptId")) : null;
        Long doctorId      = modifications.containsKey("doctorId")
                ? Convert.convert(Long.class, modifications.get("doctorId")) : null;
        String doctorName  = modifications.containsKey("doctorName")
                ? Convert.convert(String.class, modifications.get("doctorName")) : null;
        String doctorPhone = modifications.containsKey("doctorPhone")
                ? Convert.convert(String.class, modifications.get("doctorPhone")) : null;
        orderDataValidator.validateAndFillForModify(
                order, hospitalId, hospitalDeptId, doctorId, doctorName, doctorPhone);
    }

    // 3. 普通字段（非 hospital_doctor 组）：反射赋值
    for (ModifyApplyFieldConfigDTO.FieldConfig fc : allFieldConfigs) {
        if ("hospital_doctor".equals(fc.getGroup())) continue; // 已由 validator 处理
        if (!modifications.containsKey(fc.getField())) continue; // 前端未传此字段，不修改
        Object newVal = convertFieldValue(fc.getType(), modifications.get(fc.getField()));
        if (newVal != null) {
            try {
                BeanUtil.setFieldValue(order, fc.getField(), newVal);
            } catch (Exception e) {
                log.warn("反射赋值失败，字段可能不存在于实体，field={}", fc.getField());
            }
        }
    }

    // 4. 快照赋值后的新值（反射），与旧值对比留痕
    Map<String, Object> afterSnapshot = snapshotFields(order, allFieldNames);
    recordChangesFromSnapshots(order.getId(), order.getOrderCode(), applyId,
            beforeSnapshot, afterSnapshot, allFieldConfigs, modifierId, modifierName);
}
```

### 3.4 改造 item 留痕为配置驱动

在 `processItemModification` 内，将 `compareAndLogItemFields(...)` 调用替换为配置驱动的内联逻辑：

- [ ] **Step 3.6：在 `processItemModification` 中替换 `compareAndLogItemFields` 调用**

找到 `processItemModification` 方法，在方法开头从 `fieldConfig` 取 item 子字段配置，并替换 `compareAndLogItemFields` 调用：

```java
// processItemModification 方法签名增加 fieldConfig 参数：
private void processItemModification(OrderMainEntity order, Map<String, Object> modifications,
        Long applyId, Long modifierId, String modifierName,
        ModifyApplyFieldConfigDTO fieldConfig) {   // ← 新增参数

    // ... 现有代码 ...

    // 取 14.3 item 子字段配置（用于留痕）
    List<ModifyApplyFieldConfigDTO.FieldConfig> itemFieldConfigs =
            fieldConfig.getTypeConfig(ModifyApplyTypeEnum.ITEM.getDictCode()) != null
            ? fieldConfig.getTypeConfig(ModifyApplyTypeEnum.ITEM.getDictCode()).getItemSubFields()
            : List.of();

    // ... 在修改已有 item 的分支，用配置驱动替换 compareAndLogItemFields 调用：
    // 原：compareAndLogItemFields(order, oldItem, itemMap, applyId, modifierId, modifierName);
    // 改为：
    for (ModifyApplyFieldConfigDTO.FieldConfig fc : itemFieldConfigs) {
        Object oldVal = BeanUtil.getFieldValue(oldItem, fc.getField());
        Object newVal = convertFieldValue(fc.getType(), itemMap.get(fc.getField()));
        recordIfChanged(order.getId(), order.getOrderCode(), applyId,
                "item_" + oldItem.getId() + "_" + fc.getField(),
                fc.getLabel(), oldVal, newVal, modifierId, modifierName);
    }
```

- [ ] **Step 3.7：更新 `executeModification` 中 `processItemModification` 调用，传入 `fieldConfig`**

```java
// 原：
processItemModification(order, modifications, applyId, modifierId, modifierName);
// 改为：
processItemModification(order, modifications, applyId, modifierId, modifierName, fieldConfig);
```

- [ ] **Step 3.8：验证编译通过**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-order -q
```

预期：BUILD SUCCESS，无编译错误。

---

## Task 4：更新单元测试

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImplTest.java`

### 目标

现有测试的 `configService.getConfigValue` 默认返回 null（导致 `loadFieldConfig` 返回空配置，白名单为空，所有字段都不处理）。需要为 `executeModification` 相关测试提供正确的配置 mock，同时新增配置驱动场景的测试。

- [ ] **Step 4.1：在测试类中新增配置 JSON 常量**

```java
// 在测试类顶部新增常量
private static final String FIELD_CONFIG_JSON =
    "{\"14.1\":{\"name\":\"基础信息\",\"fields\":[" +
    "{\"field\":\"hospitalId\",\"label\":\"医院\",\"type\":\"autocomplete\",\"group\":\"hospital_doctor\"}," +
    "{\"field\":\"hospitalDeptId\",\"label\":\"科室\",\"type\":\"autocomplete\",\"group\":\"hospital_doctor\"}," +
    "{\"field\":\"doctorId\",\"label\":\"关联医生\",\"type\":\"autocomplete\",\"group\":\"hospital_doctor\"}," +
    "{\"field\":\"doctorName\",\"label\":\"医生姓名\",\"type\":\"text\",\"group\":\"hospital_doctor\"}," +
    "{\"field\":\"doctorPhone\",\"label\":\"医生电话\",\"type\":\"text\",\"group\":\"hospital_doctor\"}," +
    "{\"field\":\"patientName\",\"label\":\"患者姓名\",\"type\":\"text\"}," +
    "{\"field\":\"patientAge\",\"label\":\"患者年龄\",\"type\":\"number\"}," +
    "{\"field\":\"patientGender\",\"label\":\"患者性别\",\"type\":\"select\"}," +
    "{\"field\":\"isUrgent\",\"label\":\"是否加急\",\"type\":\"switch\"}," +
    "{\"field\":\"isPostal\",\"label\":\"是否邮寄\",\"type\":\"switch\"}," +
    "{\"field\":\"postalAddress\",\"label\":\"邮寄地址\",\"type\":\"textarea\"}," +
    "{\"field\":\"expectedDeliveryDate\",\"label\":\"期望交付时间\",\"type\":\"datetime\"}" +
    "]}," +
    "\"14.3\":{\"name\":\"重建项目\",\"fields\":[{\"field\":\"items\",\"label\":\"重建项目明细\",\"type\":\"array\",\"subFields\":[" +
    "{\"field\":\"bodyPartId\",\"label\":\"部位\",\"type\":\"select\"}," +
    "{\"field\":\"projectId\",\"label\":\"重建项目\",\"type\":\"select\"}," +
    "{\"field\":\"projectDesc\",\"label\":\"项目说明\",\"type\":\"textarea\"}," +
    "{\"field\":\"formingRequirement\",\"label\":\"成形需求\",\"type\":\"textarea\"}," +
    "{\"field\":\"otherRequirement\",\"label\":\"其他要求\",\"type\":\"textarea\"}" +
    "]}]}}";
```

- [ ] **Step 4.2：在 `ExecuteModificationTests` 的 `@BeforeEach` 或各测试方法中 mock 配置**

在 `ExecuteModificationTests` 内嵌类新增 `@BeforeEach`（若已有则合并）：

```java
@BeforeEach
void setUp() {
    when(configService.getConfigValue(SystemConfigKeyEnum.ORDER_MODIFY_FIELD_CONFIG.getKey()))
            .thenReturn(FIELD_CONFIG_JSON);
}
```

- [ ] **Step 4.3：新增测试：配置驱动赋值（普通字段通过反射设置）**

```java
@Test
void 基础信息修改_配置存在字段_通过反射赋值成功() {
    try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

        when(orderModifyApplyMapper.selectById(APPLY_ID))
                .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.1"));
        OrderMainEntity order = buildOrder(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
        order.setPatientAge(25);
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        ExecuteModifyDTO dto = new ExecuteModifyDTO();
        ExecuteModifyDTO.ModifyField ageField = new ExecuteModifyDTO.ModifyField();
        ageField.setField("patientAge");
        ageField.setValue(30);
        dto.setInfoFields(List.of(ageField));

        service.executeModification(APPLY_ID, dto);

        // 验证订单年龄已被修改
        assertThat(order.getPatientAge()).isEqualTo(30);
        verify(orderMainMapper).updateById(any(OrderMainEntity.class));
        verify(orderModificationLogMapper).insert(any(OrderModificationLogEntity.class));
    }
}
```

- [ ] **Step 4.4：新增测试：配置不存在的字段静默忽略**

```java
@Test
void 基础信息修改_配置不存在字段_静默忽略不抛异常() {
    try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

        when(orderModifyApplyMapper.selectById(APPLY_ID))
                .thenReturn(buildApply(ModifyApplyStatusEnum.APPROVED.getCode(), "14.1"));
        OrderMainEntity order = buildOrder(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
        when(orderMainMapper.selectById(ORDER_ID)).thenReturn(order);

        // 传一个配置中不存在的字段（orderCode 不在 14.1 配置中）
        ExecuteModifyDTO dto = new ExecuteModifyDTO();
        ExecuteModifyDTO.ModifyField unknown = new ExecuteModifyDTO.ModifyField();
        unknown.setField("orderCode");
        unknown.setValue("HACKED-CODE");
        dto.setInfoFields(List.of(unknown));

        // 不应抛出异常，orderCode 应保持原值
        assertThatCode(() -> service.executeModification(APPLY_ID, dto))
                .doesNotThrowAnyException();
        assertThat(order.getOrderCode()).isEqualTo(ORDER_CODE); // orderCode 未被修改
    }
}
```

- [ ] **Step 4.5：验证现有测试仍然通过（全量运行）**

```bash
cd yigongbao-parent
mvn clean test -pl yigongbao-module-order
```

预期：117+ tests，全部 PASS。

---

## Task 5：运行全量测试验证

- [ ] **Step 5.1：运行 order 模块测试**

```bash
cd yigongbao-parent
mvn clean test -pl yigongbao-module-order
```

预期：BUILD SUCCESS，所有测试通过。

- [ ] **Step 5.2：确认无残留的硬编码字段名（旧方法）**

```bash
grep -rn "oldPatientName\|oldHospitalId\|oldDoctorId\|oldIsUrgent\|getStringValue\|getLongValue\|getIntegerValue\|getLocalDateTimeValue\|compareAndLogItemFields\|applyInfoFields\|logInfoFieldChanges" \
  yigongbao-parent/yigongbao-module-order/src/main/java/ --include="*.java"
```

预期：无输出（全部已删除）。

---

## Task 6：更新技术文档

**Files:**
- Modify: `.docs/技术实现/order/03_订单修改审核实现方案.md`

- [ ] **Step 6.1：在版本记录表中新增 v6.0 条目**

```markdown
| **6.0** | **2026-04-12** | **重构：executeModification 执行逻辑改为配置驱动。删除 processInfoModification/applyInfoFields/logInfoFieldChanges/compareAndLogItemFields 四个硬编码方法，改用 BeanUtil 反射 + 配置字段列表驱动；14.1 字段配置新增 group 属性（hospital_doctor 组走 OrderDataValidator），补齐漏配字段 hospitalId/isUrgent；ModifyApplyFieldConfigDTO 新增 group/subFields/getFieldsByGroup()/getItemSubFields()；新增 convertFieldValue/snapshotFields/recordChangesFromSnapshots 三个通用工具方法** | **hanjor** |
```

- [ ] **Step 6.2：更新文档中 `executeModification` 实现描述章节**

找到文档中描述 `executeModification` 执行流程的章节，将硬编码的字段列表描述改为：

> **基础信息修改（14.1）**：从 `sys_config` 加载字段配置白名单；`hospital_doctor` 组字段（hospitalId/hospitalDeptId/doctorId/doctorName/doctorPhone）走 `OrderDataValidator.validateAndFillForModify()`；其余字段通过 `BeanUtil.setFieldValue()` 反射赋值；赋值前后两次快照对比，记录留痕。配置增删字段后 Service 层无需任何代码改动。
>
> **重建项目修改（14.3）**：item 留痕对比改为遍历 `sys_config` 中 items.subFields 配置，用 `BeanUtil.getFieldValue()` 反射取旧值，配置驱动记录。

---

## Task 7：更新接口文档

**Files:**
- Modify: `.docs/接口文档/19_订单模块接口文档.md`

- [ ] **Step 7.1：更新 19.21 执行订单修改 的请求体参数说明**

将 14.1 基础信息的字段列表更新为：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| hospitalId | Long | 医院 ID |
| hospitalDeptId | Long | 科室 ID |
| doctorId | Long | 医生 ID |
| doctorName | String | 医生姓名（doctorId 为 null 时触发快速创建） |
| doctorPhone | String | 医生电话 |
| patientName | String | 患者姓名 |
| patientAge | Integer | 患者年龄 |
| patientGender | String | 患者性别（字典码，如 "12.1" 男） |
| isUrgent | Integer | 是否加急（0/1） |
| isPostal | Integer | 是否邮寄（0/1） |
| postalAddress | String | 邮寄地址 |
| expectedDeliveryDate | String | 期望交付时间（ISO 8601） |

> 备注：实际可修改字段以 `sys_config.order.modify.field.config` 配置为准，代码层动态读取，无需同步修改代码。

---

## Task 8：代码审查 & Git Commit

- [ ] **Step 8.1：自我代码审查清单**

检查以下项目：
- [ ] 旧4个方法已完全删除，无残留调用
- [ ] `convertFieldValue` 覆盖了所有 type（text/textarea/autocomplete/select/number/switch/datetime）
- [ ] `snapshotFields` 反射异常已静默处理（warn 日志）
- [ ] `processInfoModification` 中 hospital_doctor 分组字段走 validator，其余字段走反射赋值
- [ ] `processItemModification` 留痕遍历配置字段，不再硬编码5个字段名
- [ ] 测试 mock 了 `configService.getConfigValue` 返回正确 JSON
- [ ] 新增2个测试用例（配置驱动赋值、未知字段忽略）
- [ ] 文档版本号已更新

- [ ] **Step 8.2：最终测试验证**

```bash
cd yigongbao-parent
mvn clean test -pl yigongbao-module-order
```

预期：BUILD SUCCESS。

- [ ] **Step 8.3：Git Commit**

```bash
cd "D:\01_Project\02_Personal\医工宝"
git add yigongbao-parent/yigongbao-module-order/src/ sql/init.sql .docs/
git commit -m "refactor(order): executeModification 改为配置驱动，消除所有硬编码字段名

- sys_config order.modify.field.config 新增 group 属性，补齐 hospitalId/isUrgent 漏配字段
- ModifyApplyFieldConfigDTO.FieldConfig 新增 group/subFields
- TypeConfig 新增 getFieldsByGroup()/getItemSubFields()
- 删除 applyInfoFields/logInfoFieldChanges/processInfoModification(旧)/compareAndLogItemFields/四个类型转换辅助方法
- 新增 convertFieldValue/snapshotFields/recordChangesFromSnapshots 三个通用工具方法
- processInfoModification 改为：反射快照 → validator(hospital_doctor组) → 反射赋值 → 快照对比留痕
- processItemModification 留痕改为遍历配置 subFields 反射取旧值
- 新增测试：配置驱动赋值验证、未知字段静默忽略验证
- 更新技术文档 v6.0 和接口文档 19.21"
```
