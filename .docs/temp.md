# 订单修改审核功能实现计划（v2.0 可行性分析补充）

## Context

订单在初次审核通过后（phase=1 或 phase=2），业务员需要修改某些字段时必须走审批流程。设计文档（v2.0）已基本完成，本计划记录代码评审后发现的关键问题和修正方案，确保实现前所有逻辑无歧义。

---

## 已确认正确的设计决策

| 项 | 状态 |
|---|---|
| 不继承 BaseEntity 的 `OrderModificationLogEntity` | ✅ 正确 |
| `OrderModifyApplyServiceImpl` 直接注入 `OrderMainMapper`（规避循环依赖） | ✅ 正确 |
| `executeModification()` 作为单一 `@Transactional` 方法 | ✅ 正确 |
| MySQL 8 函数索引处理并发控制 + 应用层双重校验 | ✅ 正确 |
| 移除申请编号、直接使用 ID | ✅ 正确 |
| `FileBizTypeEnum.getDictCode()` 返回 `"10.1"/"10.2"` | ✅ 确认存在 |
| `OrderFileEntity.fileId` 字段存在 | ✅ 确认存在 |
| `isUrgent` 为 `Integer`（0/1） | ✅ 确认正确 |

---

## 需要在实现前解决的问题（关键修正）

### 问题 1：`apply_type` 字段大小和命名【CRITICAL】

**现状：** v2.0 DDL 定义了 `apply_type VARCHAR(20)`（单数），但业务支持多选（逗号分隔）。

**计算：** `"modify_apply:info,modify_apply:image,modify_apply:item"` = 53 字符，超过 VARCHAR(20) 会截断。

**修正方案：**
- DDL 改为 `apply_types VARCHAR(100)` （复数，匹配 DTO 命名 `applyTypes`）
- 实体类字段：`private String applyTypes`
- 所有 `apply.setApplyType()` 改为 `apply.setApplyTypes()`

---

### 问题 2：`ExecuteModificationDTO.items[].id` 语义歧义【CRITICAL】

**现状：** 复用 `OrderItemDraftItemDTO`，其 `id` 字段注释为"草稿明细ID"，用在 `executeModification()` 时语义混乱——客户端无从知晓应传草稿 ID 还是正式订单项 ID。

**修正方案：** 新建独立的 `ExecuteModificationItemDTO`，明确字段语义：

```java
@Data
public class ExecuteModificationItemDTO implements Serializable {
    /**
     * 要更新的订单明细ID（传null表示新增）
     * 必须属于当前订单，否则报错
     */
    private Long orderItemId;          // 明确语义：order_item 主键
    private Long bodyPartId;
    private Long projectId;
    private String projectDesc;
    private String formingRequirement;
    private String otherRequirement;
    private Integer sortOrder;
}
```

并在 `processItemModification()` 增加 ID 归属校验：

```java
// 校验所有提供的 ID 确实属于当前订单
Set<Long> validItemIds = oldItems.stream()
    .map(OrderItemEntity::getId)
    .collect(Collectors.toSet());
for (ExecuteModificationItemDTO item : dto.getItems()) {
    if (item.getOrderItemId() != null && !validItemIds.contains(item.getOrderItemId())) {
        throw new BusinessException("param-error", "重建项目ID不属于当前订单");
    }
}
```

---

### 问题 3：`fillAreaFromHospital()` 是 `OrderMainServiceImpl` 的私有方法【HIGH】

**现状：** `processInfoModification()` 中调用了 `fillAreaFromHospital(order, dto.getHospitalId())`，但该方法是 `OrderMainServiceImpl` 的 `private` 方法，无法从 `OrderModifyApplyServiceImpl` 跨类调用。

**修正方案：** `OrderModifyApplyServiceImpl` 额外注入 `HospitalMapper`，直接复现相同逻辑（4 行代码，无需抽提工具类）：

```java
// 在 OrderModifyApplyServiceImpl 中直接实现
private void fillAreaFromHospital(OrderMainEntity order, Long hospitalId) {
    if (hospitalId == null) return;
    HospitalEntity hospital = hospitalMapper.selectById(hospitalId);
    if (hospital != null) {
        order.setAreaId(hospital.getAreaId());
        order.setAreaName(hospital.getAreaName());
        order.setFullAreaName(hospital.getFullAreaName());
    }
}
```

需额外注入：`private final HospitalMapper hospitalMapper;`

---

### 问题 4：`processInfoModification()` 中的伪反射模式【HIGH】

**现状：** 设计文档展示了 `getFieldValue(order, field)` 和 `updateOrderFields(order, dto, allowedFields)` 等未定义的辅助方法，暗示使用反射，与代码库风格不符。

**修正方案：** 使用显式字段映射，与现有代码风格完全一致：

```java
/**
 * 更新订单基础信息字段（显式赋值，无反射）
 */
private void applyInfoFields(OrderMainEntity order, ExecuteModificationDTO dto) {
    if (dto.getPatientName()           != null) order.setPatientName(dto.getPatientName());
    if (dto.getPatientAge()            != null) order.setPatientAge(dto.getPatientAge());
    if (dto.getPatientGender()         != null) order.setPatientGender(dto.getPatientGender());
    if (dto.getDoctorId()              != null) order.setDoctorId(dto.getDoctorId());
    if (dto.getDoctorPhone()           != null) order.setDoctorPhone(dto.getDoctorPhone());
    if (dto.getIsPostal()              != null) order.setIsPostal(dto.getIsPostal());
    if (dto.getPostalAddress()         != null) order.setPostalAddress(dto.getPostalAddress());
    if (dto.getExpectedDeliveryDate()  != null) order.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
    if (dto.getHospitalId() != null) {
        order.setHospitalId(dto.getHospitalId());
        fillAreaFromHospital(order, dto.getHospitalId());
    }
}

/**
 * 逐字段对比记录留痕（显式比对，无反射）
 */
private void logInfoFieldChanges(OrderMainEntity before, ExecuteModificationDTO dto,
        Long orderId, String orderCode, Long applyId, Long modifierId, String modifierName) {
    recordIfChanged(orderId, orderCode, applyId, "patientName", "患者姓名",
            before.getPatientName(), dto.getPatientName(), modifierId, modifierName);
    recordIfChanged(orderId, orderCode, applyId, "patientAge", "患者年龄",
            before.getPatientAge(), dto.getPatientAge(), modifierId, modifierName);
    recordIfChanged(orderId, orderCode, applyId, "patientGender", "患者性别",
            before.getPatientGender(), dto.getPatientGender(), modifierId, modifierName);
    recordIfChanged(orderId, orderCode, applyId, "doctorId", "关联医生",
            before.getDoctorId(), dto.getDoctorId(), modifierId, modifierName);
    recordIfChanged(orderId, orderCode, applyId, "doctorPhone", "医生电话",
            before.getDoctorPhone(), dto.getDoctorPhone(), modifierId, modifierName);
    recordIfChanged(orderId, orderCode, applyId, "isPostal", "是否邮寄",
            before.getIsPostal(), dto.getIsPostal(), modifierId, modifierName);
    recordIfChanged(orderId, orderCode, applyId, "postalAddress", "邮寄地址",
            before.getPostalAddress(), dto.getPostalAddress(), modifierId, modifierName);
    recordIfChanged(orderId, orderCode, applyId, "expectedDeliveryDate", "期望交付时间",
            before.getExpectedDeliveryDate(), dto.getExpectedDeliveryDate(), modifierId, modifierName);
    recordIfChanged(orderId, orderCode, applyId, "hospitalId", "医院",
            before.getHospitalId(), dto.getHospitalId(), modifierId, modifierName);
}

private void recordIfChanged(Long orderId, String orderCode, Long applyId,
        String fieldName, String fieldLabel,
        Object oldValue, Object newValue, Long modifierId, String modifierName) {
    if (newValue != null && !Objects.equals(oldValue, newValue)) {
        recordModificationLog(orderId, orderCode, applyId,
                fieldName, fieldLabel, oldValue, newValue, modifierId, modifierName);
    }
}
```

注意：`before` 快照需在调用 `applyInfoFields()` 之前采集（浅拷贝保存 entity 引用的字段值）。

---

### 问题 5：`processItemModification()` 更新项目时遗漏字段填充

**现状：** 对已有项目（更新分支）执行 BeanUtils.copyProperties 后，没有调用 `orderDataValidator.validateAndFillItemsForOrder()` 重新填充 `bodyPartName`、`projectName` 等冗余字段。

**修正方案：** 在更新分支也调用校验方法：

```java
// 更新分支：重新校验+填充冗余字段
orderDataValidator.validateAndFillItemsForOrder(List.of(oldItem), ValidateMode.DIRECT);
orderItemMapper.updateById(oldItem);
```

---

### 问题 6：`replaceOrderFiles()` 缺少文件存在性校验

**现状：** 直接插入新文件关联，未校验文件 ID 是否在 `file_detail` 表中存在。

**修正方案：** 插入前调用 `OrderMainServiceImpl` 中已有的文件校验逻辑。由于该方法也是私有的，`OrderModifyApplyServiceImpl` 需注入 `FileService`（basic 模块），使用相同逻辑校验：

```java
// 注入 FileService
private final FileService fileService;

// 在 processImageModification() 中校验
if (dto.getImageDataFileIds() != null) {
    List<FileVO> found = fileService.listByIds(dto.getImageDataFileIds());
    if (found.size() != dto.getImageDataFileIds().size()) {
        throw new BusinessException(ErrorCodeEnum.ORDER_FILE_NOT_FOUND, "影像数据文件");
    }
    replaceOrderFiles(order, dto.getImageDataFileIds(),
            FileBizTypeEnum.IMAGE_DATA.getDictCode(), applyId, modifierId, modifierName);
}
```

---

### 问题 7：MySQL 函数索引 DDL 语法修正

**现状：** `UNIQUE KEY ... ON table_name ((...))` 为非标准写法，在 CREATE TABLE 内部应使用：

```sql
UNIQUE INDEX uk_order_pending_apply ((CASE WHEN is_deleted = 0 AND status = 'PENDING' THEN order_id ELSE NULL END)),
```

或在 CREATE TABLE 外单独执行：

```sql
CREATE UNIQUE INDEX uk_order_pending_apply 
ON order_modify_apply ((CASE WHEN is_deleted = 0 AND status = 'PENDING' THEN order_id ELSE NULL END));
```

按项目现有习惯（已有函数索引均在 CREATE TABLE 后单独追加），保持一致性，采用 CREATE TABLE 后追加写法。

---

### 问题 8：`DictService` 依赖可移除

**现状：** v2.0 在依赖列表中列出 `DictService`，但 `validateApplyTypes()` 已通过枚举校验，无需 DB 查询。

**结论：** 移除 `DictService` 注入，除非后续需要运行时动态增减申请类型。

---

### 问题 9：`CanApplyModifyResult` 完整定义

**位置：** `order.vo.modify.CanApplyModifyResult`

```java
@Data
public class CanApplyModifyResult {
    /** 是否可以发起申请 */
    private boolean canApply;
    /** 允许的申请类型编码列表 */
    private List<String> allowedTypes;
    /** 允许的申请类型中文名 */
    private String allowedTypesText;
    /** 不可申请时的原因代码 */
    private String reason;

    public static CanApplyModifyResult yes(List<String> allowedTypes) {
        CanApplyModifyResult r = new CanApplyModifyResult();
        r.setCanApply(true);
        r.setAllowedTypes(allowedTypes);
        r.setAllowedTypesText(ModifyApplyTypeEnum.toNamesText(String.join(",", allowedTypes)));
        return r;
    }

    public static CanApplyModifyResult no(String reason) {
        CanApplyModifyResult r = new CanApplyModifyResult();
        r.setCanApply(false);
        r.setAllowedTypes(Collections.emptyList());
        r.setAllowedTypesText("");
        r.setReason(reason);
        return r;
    }
}
```

---

## 实现步骤（最终版）

### Phase 1：基础配置层（无互相依赖）

1. **`ErrorCodeEnum`** — 新增 715-722 错误码
2. **`SystemConfigKeyEnum`** — 新增 `ORDER_MODIFY_FIELD_CONFIG` 配置键（先确认文件路径）
3. **枚举** — 新建 `ModifyApplyTypeEnum`、`ModifyApplyStatusEnum`
4. **DDL**：
   - `sql/ddl.sql` — 新建 `order_modify_apply`（`apply_types VARCHAR(100)`）、`order_modification_log` 表
   - `sql/init.sql` — 新增 `sys_dict` 字典数据、`sys_config` 字段配置记录
   - `schema.sql` — 替换旧版 stub 表（H2 兼容：TEXT→VARCHAR(5000)，无函数索引）

### Phase 2：数据层

5. **`OrderModifyApplyEntity`** — 继承 `BaseEntity`，字段：orderId, orderCode, `applyTypes`（注意已改名）, applyReason, status, rejectReason, applicantId, applicantName, auditorId, auditorName, auditTime
6. **`OrderModificationLogEntity`** — 不继承 `BaseEntity`，字段：id, orderId, orderCode, applyId, fieldName, fieldLabel, oldValue, newValue, modifierId, modifierName, createTime（@TableField INSERT）
7. **`OrderModifyApplyMapper`**、**`OrderModificationLogMapper`**

### Phase 3：DTO/VO 层

8. **新建 `ExecuteModificationItemDTO`**（替代复用 `OrderItemDraftItemDTO`，明确 `orderItemId` 语义）
9. **`ExecuteModificationDTO`** — 字段与 v2.0 设计一致，`items` 改为 `List<ExecuteModificationItemDTO>`，移除 `designStartTime/designSubmitTime`（无关字段）
10. **其他 DTO**：`CreateModifyApplyDTO`、`AuditModifyApplyDTO`、`ModifyApplyPageQueryDTO`、`ModificationLogPageQueryDTO`、`ModifyApplyFieldConfigDTO`
11. **VO**：`ModifyApplyVO`、`ModifyApplyListVO`、`ModifyApplyDetailVO`、`ModifyApplyFieldConfigVO`、`ModificationLogVO`、`CanApplyModifyResult`

### Phase 4：Service 层

12. **`OrderModifyApplyService` 接口**，方法清单（与 v2.0 一致，含 `canApplyModify`、`validateFieldsInScope`）
13. **`OrderModifyApplyServiceImpl`** — 注入（不含 `DictService`，增加 `HospitalMapper`、`FileService`）：
    - `OrderModifyApplyMapper`, `OrderModificationLogMapper`
    - `OrderMainMapper`（读订单，不用 Service）
    - `OrderItemMapper`, `OrderFileMapper`
    - `HospitalMapper`（填充地区冗余字段）
    - `FileService`（校验文件存在）
    - `OrderDataValidator`（重建项目校验）
    - `ConfigService`（读字段配置）
    
    实现所有方法，按上述修正策略：
    - `applyInfoFields()` + `logInfoFieldChanges()` 显式实现
    - `processItemModification()` 使用 `ExecuteModificationItemDTO.orderItemId` + 归属校验 + 更新时调用 validator
    - `processImageModification()` 先校验文件存在再替换

14. **`OrderMainServiceImpl` 不改动**，原有 `updateOrder()` 逻辑保持不变

### Phase 5：Controller 层

15. **新建 `OrderModifyApplyController`**（`@RequestMapping("/order")`），11 个端点（含 `GET /order/{id}/can-apply-modify`）
16. **`OrderController` 新增**：
    - `GET /order/{id}/modify-applies`
    - `GET /order/{id}/modification-logs`
    - `PUT /order/{id}/modification`（专用修改接口，独立于原 `updateOrder` 路径）

    注意：`PUT /order/{id}/modification` 与 `PUT /order/{id}` 是两个不同路径，不冲突。

### Phase 6：测试

17. `OrderModifyApplyServiceImplTest` — 核心场景：
    - 创建申请（阶段限制、多类型多选、并发控制）
    - 撤回申请（权限、状态）
    - 审核通过/拒绝
    - 执行修改（info 字段校验、item ID 归属校验、image 文件替换、留痕记录）

---

## 关键文件清单

| 操作 | 文件 |
|------|------|
| 修改 | `yigongbao-common/.../enums/ErrorCodeEnum.java` |
| 修改 | `yigongbao-common/.../enums/SystemConfigKeyEnum.java` |
| 修改 | `sql/ddl.sql`, `sql/init.sql` |
| 修改 | `yigongbao-module-order/src/test/resources/schema.sql` |
| 新建 | `yigongbao-module-order/.../enums/ModifyApplyTypeEnum.java` |
| 新建 | `yigongbao-module-order/.../enums/ModifyApplyStatusEnum.java` |
| 新建 | `yigongbao-module-order/.../entity/OrderModifyApplyEntity.java` |
| 新建 | `yigongbao-module-order/.../entity/OrderModificationLogEntity.java` |
| 新建 | `yigongbao-module-order/.../mapper/OrderModifyApplyMapper.java` |
| 新建 | `yigongbao-module-order/.../mapper/OrderModificationLogMapper.java` |
| 新建 | `yigongbao-module-order/.../dto/modify/ExecuteModificationItemDTO.java` |
| 新建 | `yigongbao-module-order/.../dto/modify/ExecuteModificationDTO.java` |
| 新建 | `yigongbao-module-order/.../dto/modify/CreateModifyApplyDTO.java` |
| 新建 | `yigongbao-module-order/.../dto/modify/AuditModifyApplyDTO.java` |
| 新建 | `yigongbao-module-order/.../dto/modify/ModifyApplyPageQueryDTO.java` |
| 新建 | `yigongbao-module-order/.../dto/modify/ModificationLogPageQueryDTO.java` |
| 新建 | `yigongbao-module-order/.../dto/modify/ModifyApplyFieldConfigDTO.java` |
| 新建 | `yigongbao-module-order/.../vo/modify/ModifyApplyVO.java` |
| 新建 | `yigongbao-module-order/.../vo/modify/ModifyApplyListVO.java` |
| 新建 | `yigongbao-module-order/.../vo/modify/ModifyApplyDetailVO.java` |
| 新建 | `yigongbao-module-order/.../vo/modify/ModifyApplyFieldConfigVO.java` |
| 新建 | `yigongbao-module-order/.../vo/modify/ModificationLogVO.java` |
| 新建 | `yigongbao-module-order/.../vo/modify/CanApplyModifyResult.java` |
| 新建 | `yigongbao-module-order/.../service/OrderModifyApplyService.java` |
| 新建 | `yigongbao-module-order/.../service/impl/OrderModifyApplyServiceImpl.java` |
| 新建 | `yigongbao-module-order/.../controller/OrderModifyApplyController.java` |
| 修改 | `yigongbao-module-order/.../controller/OrderController.java` |
| 新建 | `yigongbao-module-order/src/test/.../OrderModifyApplyServiceImplTest.java` |

---

## 验证方式

1. **单元测试：** `mvn test -pl yigongbao-module-order -Dtest=OrderModifyApplyServiceImplTest`
2. **手动集成验证流程：**
   - 创建订单 → 完成初次数据审核（status=DATA_AUDIT_PASSED）
   - 调用 `GET /order/{id}/can-apply-modify` 确认返回 `canApply=true`
   - 发起申请（info+item 类型），确认并发控制（第二次调用返回 715 错误）
   - 管理员审核通过（APPROVE）
   - 执行 `PUT /order/{id}/modification`（携带 applyId + 修改字段）
   - `GET /order/{id}/modification-logs` 确认留痕存在
   - 尝试传入不属于当前订单的 `orderItemId`，应报错
   - 验证设计阶段（phase=2）发起 info 类型申请，应返回 722 错误
