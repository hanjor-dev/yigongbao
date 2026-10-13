# 订单修改模块代码审查报告

**审查日期：** 2026-05-20  
**审查范围：** 订单修改相关功能（Controller + ServiceImpl + 相关 DTO/VO/Entity/Enum）  
**审查入口：** `OrderModifyApplyController` → `OrderModifyApplyServiceImpl`  
**审查人：** Kiro

---

## 一、可确认的代码问题

### P1 — 高优先级（功能正确性 / 安全）

---

#### [P1-1] `directModify` 缺少数据权限校验，任意用户可修改他人订单

**文件：** `OrderModifyApplyServiceImpl.java:484`

**问题：**  
`directModify` 仅校验订单存在性，未校验当前用户是否有权修改该订单。任何已登录用户只要知道 `orderId` 即可修改他人订单，构成横向越权漏洞。

```java
// 当前代码：只查订单存在，无权限校验
OrderMainEntity order = orderMainMapper.selectById(orderId);
if (order == null) {
    throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
}
```

**影响域：** `PUT /{orderId}/direct` 接口，所有已登录用户均可触达。

**修复建议：**  
参考 `OrderMainServiceImpl.validateDataScope`，在查询订单后加数据权限校验；或将 `validateDataScope` 逻辑提取为公共方法供两个 Service 复用。

---

#### [P1-2] `getApplicableTypes` / `listAppliesByOrder` / `listModificationLogs` 缺少数据权限校验

**文件：** `OrderModifyApplyServiceImpl.java:110, 1074, 1093`

**问题：**  
三个接口均只校验订单/申请存在性，未校验当前用户是否有权查看该订单的数据。攻击者可枚举 `orderId` 获取任意订单的修改申请列表和修改留痕。

**影响域：** 信息泄露，可暴露患者姓名、医院名、修改历史等敏感数据。

**修复建议：**  
在查询前加数据权限校验（同 P1-1）。

---

#### [P1-3] `getApplyDetail` 无任何权限校验

**文件：** `OrderModifyApplyServiceImpl.java:1032`  
**Controller：** `OrderModifyApplyController.java:142`（无 `@RequirePermission`）

**问题：**  
`getApplyDetail` 接口无权限注解，且 Service 层未校验当前用户是否为申请人、审核人或有权查看该订单的用户。任意已登录用户可通过枚举 `applyId` 查看所有申请详情。

**影响域：** 信息泄露，暴露申请原因、审核意见、患者信息等。

**修复建议：**  
Controller 加 `@RequirePermission("order:View")`；Service 层校验当前用户是申请人、审核人，或对关联订单有数据权限。

---

#### [P1-4] `executeModification` 存在并发重复执行风险

**文件：** `OrderModifyApplyServiceImpl.java:399-479`

**问题：**  
状态检查（`status == APPROVED`）与状态更新（`status = COMPLETED`）之间无行级锁保护。高并发下两个请求可同时通过状态检查，导致同一申请被执行两次，产生重复的数据修改和留痕记录。

```java
// 检查
if (!ModifyApplyStatusEnum.APPROVED.getCode().equals(apply.getStatus())) { throw ...; }
// ... 大量业务逻辑 ...
// 更新（无乐观锁/悲观锁保护）
apply.setStatus(ModifyApplyStatusEnum.COMPLETED.getCode());
orderModifyApplyMapper.updateById(apply);
```

**影响域：** 数据一致性，可能导致订单字段被重复覆盖、留痕记录重复。

**修复建议：**  
将状态更新改为带条件的原子操作：
```java
int updated = orderModifyApplyMapper.update(null,
    new LambdaUpdateWrapper<OrderModifyApplyEntity>()
        .eq(OrderModifyApplyEntity::getId, applyId)
        .eq(OrderModifyApplyEntity::getStatus, ModifyApplyStatusEnum.APPROVED.getCode())
        .set(OrderModifyApplyEntity::getStatus, ModifyApplyStatusEnum.COMPLETED.getCode()));
if (updated == 0) {
    throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_STATUS_ERROR);
}
```

---

#### [P1-5] `processItemModification` 中 `BeanUtil.copyProperties` 可能覆盖安全敏感字段

**文件：** `OrderModifyApplyServiceImpl.java:826, 848`

**问题：**  
修改已有项目时，直接将前端传入的 `itemMap`（`Map<String, Object>`）通过 `BeanUtil.copyProperties` 复制到 `OrderItemEntity`，仅排除了 `orderItemId`。前端可传入 `orderId`、`orderCode`、`isDeleted`、`createBy` 等系统字段并覆盖。

```java
BeanUtil.copyProperties(itemMap, oldItem, "orderItemId");  // 仅排除 orderItemId
```

**影响域：** 数据完整性，可能被恶意篡改订单归属（`orderId`）或软删除标记（`isDeleted`）。

**修复建议：**  
明确排除系统字段：
```java
BeanUtil.copyProperties(itemMap, oldItem, "orderItemId", "orderId", "orderCode",
    "isDeleted", "createBy", "createTime", "updateBy", "updateTime");
```
或改为只复制白名单字段（从 `itemSubFields` 配置中读取）。

---

### P2 — 中优先级（业务逻辑 / 边界场景）

---

#### [P2-1] `directModify` 在 ORDER 阶段未校验完整性，允许空操作

**文件：** `OrderModifyApplyServiceImpl.java:514-532`

**问题：**  
`directModify` 在 ORDER 阶段触发 INFO 修改的条件是 `!modifications.isEmpty()`，但 `modifications` 可能只包含 `items` 或 `imageDataFileIds` 等 key，导致 INFO 修改被跳过但不报错。与 `executeModification` 中的 `validateModificationCompleteness` 校验逻辑不一致。

**影响域：** 用户传入空 `infoFields` 时静默成功，无任何修改发生，但接口返回 200，前端无法感知。

**修复建议：**  
`directModify` 也应调用 `validateModificationCompleteness`，或在入口处校验 dto 不为空且至少包含一种有效修改内容。

---

#### [P2-2] `triggerPostModifyFlow` 在 ORDER 阶段对非预期状态仅 warn，不抛异常

**文件：** `OrderModifyApplyServiceImpl.java:965-967`

**问题：**  
ORDER 阶段下，若订单状态既不是 `DATA_AUDIT_REJECTED` 也不是 `PENDING_DATA_AUDIT`（例如 `DRAFT`、`CANCELLED`），仅打印 warn 日志，修改已落库但流转未发生，订单状态与实际数据不一致。

**影响域：** 数据一致性，修改成功但订单流程未推进，可能导致审核员看到已修改但状态异常的订单。

**修复建议：**  
明确业务规则：ORDER 阶段允许修改的状态范围，对不在范围内的状态在修改前（而非修改后）抛出异常，避免数据已写入但流转失败的中间态。

---

#### [P2-3] `processInfoModification` 无法将字段值清空为 null

**文件：** `OrderModifyApplyServiceImpl.java:662-665`

**问题：**  
`convertFieldValue` 对文本型字段返回 `null` 时跳过赋值；`BeanUtil.setFieldValue` 仅在 `converted != null` 时执行。这意味着用户无法通过传 `""` 或 `null` 来清空一个可选字段（如 `remark`、`doctorPhone`）。

```java
Object converted = convertFieldValue(fc.getType(), modifications.get(fieldName));
if (converted != null) {  // null 时跳过，无法清空字段
    BeanUtil.setFieldValue(order, fieldName, converted);
}
```

**影响域：** 功能缺失，用户无法清空非必填字段。

**修复建议：**  
区分"未传入"（key 不在 modifications 中）和"传入 null/空"（key 存在但值为 null/空）两种语义，对后者允许赋 null。

---

#### [P2-4] `validateFieldsInScope` 中 `applyTypeCodes` 未做 null/blank 防护

**文件：** `OrderModifyApplyServiceImpl.java:1122`

**问题：**  
与 `executeModification` 中已修复的同类问题不同，`validateFieldsInScope` 中仍直接 `split(",")` 而无 null 检查，若数据库中 `applyTypeCodes` 为 null 会抛 NPE。

**修复建议：**  
加 `StrUtil.isBlank` 检查，与 `executeModification` 保持一致。

---

#### [P2-5] `replaceOrderFiles` 留痕的 `fieldName` 和 `fieldLabel` 均使用 `fileCategory` 字典编码

**文件：** `OrderModifyApplyServiceImpl.java:942-946`

**问题：**  
```java
recordModificationLog(order.getId(), order.getOrderCode(), applyId,
        fileCategory, fileCategory,  // fieldName 和 fieldLabel 均为字典编码如 "IMAGE_DATA"
        ...);
```
留痕记录的 `fieldLabel` 应为可读中文名（如"影像数据"），而非字典编码。

**影响域：** 审计日志可读性差，运营/审计人员查看留痕时无法理解字段含义。

**修复建议：**  
使用 `FileBizTypeEnum.getByDictCode(fileCategory).getName()` 获取中文名作为 `fieldLabel`。

---

#### [P2-6] `createApply` 未校验订单状态，仅校验阶段

**文件：** `OrderModifyApplyServiceImpl.java:235-239`

**问题：**  
校验仅限于 `phase=10 或 20`，但在这两个阶段内，某些状态下（如 `CANCELLED`、`DRAFT`）发起修改申请在业务上无意义。

**影响域：** 业务逻辑合理性，可能产生无效申请。

**修复建议：**  
需与业务确认：哪些 phase+status 组合允许发起申请，补充状态白名单校验。

---

### P3 — 低优先级（代码规范 / 可维护性）

---

#### [P3-1] 废弃接口仍完整保留，无下线计划

**文件：** `OrderModifyApplyController.java:60-138`

**问题：**  
`createApply`、`executeModification`、`withdrawApply`、`auditApply` 均标注 `@Deprecated(since = "2026-05-20", forRemoval = true)`，但无实际下线时间点约束，且对应 Service 方法仍完整实现。

**修复建议：**  
在 JIRA/项目管理工具中创建下线任务，明确版本号；或在接口层直接返回 `410 Gone`，强制客户端迁移。

---

#### [P3-2] `buildModificationsMap` 将结构化 DTO 转为 `Map<String, Object>` 增加类型不安全性

**文件：** `OrderModifyApplyServiceImpl.java:179-214`

**问题：**  
将强类型 DTO 转为 `Map<String, Object>` 后，后续所有处理均依赖字符串 key 和 `Convert.convert` 类型转换，丢失编译期类型检查，且 `@SuppressWarnings("unchecked")` 出现两处。

**影响域：** 可维护性，字段名拼写错误在编译期无法发现。

**修复建议：**  
长期可考虑直接传递 DTO 对象而非 Map，减少中间转换层。短期可在 Map key 处使用常量而非字符串字面量。

---

#### [P3-3] `getCurrentUserName` 每次调用都查一次数据库

**文件：** `OrderModifyApplyServiceImpl.java:1198-1207`

**问题：**  
`directModify` 和 `executeModification` 中，`modifierName` 通过 `getCurrentUserName()` 获取，该方法每次都调用 `userService.getById(userId)`。在同一请求中多次调用时（如留痕记录循环）会产生重复查询。

**影响域：** 性能，N 条留痕记录 = N 次用户查询（实际上 `recordModificationLog` 不调用此方法，但调用链需确认）。

**修复建议：**  
在方法入口处一次性获取用户信息，作为局部变量传递（当前 `directModify` 已这样做，但 `getCurrentUserName` 本身仍有隐患）。

---

#### [P3-4] `processItemModification` 查询旧 items 时未过滤软删除记录

**文件：** `OrderModifyApplyServiceImpl.java:795-799`

**问题：**  
```java
List<OrderItemEntity> oldItems = orderItemMapper.selectList(
    new LambdaQueryWrapper<OrderItemEntity>()
        .eq(OrderItemEntity::getOrderId, order.getId())
        .orderByAsc(OrderItemEntity::getSortOrder)
);
```
未加 `.eq(OrderItemEntity::getIsDeleted, 0)` 过滤条件。若 MyBatis-Plus 全局配置了逻辑删除自动过滤则无问题，但若未配置或配置异常，已删除的 item 会被纳入处理。

**修复建议：**  
显式加 `.eq(OrderItemEntity::getIsDeleted, 0)`，与代码库其他查询保持一致（参考 `OrderMainServiceImpl` 中的同类查询）。

---

## 二、需要业务/线上数据确认的风险

---

#### [R1] `directModify` 在设计阶段是否应允许修改影像文件？

**位置：** `OrderModifyApplyServiceImpl.java:162-174`（`determineAllowedTypesByPhase`）

**风险：**  
设计阶段（phase=20）仅允许修改重建项目（14.3），不允许修改影像文件（14.2）。但设计师在设计过程中可能需要补充影像数据。

**需确认：** 设计阶段是否有补充影像文件的业务场景？若有，当前限制会阻断该流程。

---

#### [R2] ORDER 阶段 `PENDING_DATA_AUDIT` 状态下直接修改后不触发流转，是否符合预期？

**位置：** `OrderModifyApplyServiceImpl.java:963-964`

**风险：**  
订单处于待审核状态时，`directModify` 执行成功后不触发任何流转（仅打印 info 日志）。审核员此时看到的是已被修改的数据，但订单仍处于待审核状态，审核员不会收到任何通知。

**需确认：** 待审核状态下的修改是否需要通知审核员重新审核？是否需要重置审核状态？

---

#### [R3] 修改申请的 `applyReason` 是否需要长度限制？

**位置：** `CreateModifyApplyDTO.java:28`（`@NotBlank` 但无 `@Size`）

**风险：**  
`applyReason` 仅校验非空，无最大长度限制。若数据库字段为 `VARCHAR(500)` 而前端未限制，超长输入会导致数据库写入异常。

**需确认：** 数据库 `order_modify_apply.apply_reason` 字段长度，补充对应的 `@Size` 校验。

---

#### [R4] 废弃的申请审核流程与直接修改流程是否存在数据共存问题？

**位置：** 整体架构

**风险：**  
系统同时存在两套流程：旧的"申请→审核→执行"流程和新的"直接修改"流程。`validateNoBlockingModifyApply` 会阻断存在 PENDING/APPROVED 申请时的主流程操作。若线上存量数据中有未完成的旧流程申请，可能导致订单被永久阻断。

**需确认：** 线上是否存在 status=PENDING 或 APPROVED 的历史申请记录？切换到直接修改流程前是否需要数据清理？

---

#### [R5] `processInfoModification` 中 `hospital_doctor` 分组字段修改时，若仅传 `doctorPhone` 不传 `doctorId`/`doctorName`，行为是否符合预期？

**位置：** `OrderModifyApplyServiceImpl.java:643-651`

**风险：**  
```java
boolean hasDoctorChange = doctorId != null || StrUtil.isNotBlank(doctorName);
if (hospitalId != null || hasDoctorChange) {
    orderDataValidator.validateAndFillForModify(order, hospitalId, doctorId, doctorName, doctorPhone);
}
```
若用户只传 `doctorPhone`（不传 `doctorId` 和 `doctorName`），`hasDoctorChange=false`，`validateAndFillForModify` 不会被调用，`doctorPhone` 的修改也不会生效（因为 `doctorPhone` 属于 `hospital_doctor` 分组，不走普通字段赋值路径）。

**需确认：** 是否存在只修改 `doctorPhone` 的业务场景？若有，当前逻辑会静默丢失该修改。

---

## 三、总结

| 优先级 | 数量 | 核心问题 |
|--------|------|----------|
| P1（高） | 5 | 横向越权（P1-1/P1-2/P1-3）、并发重复执行（P1-4）、不安全的字段复制（P1-5） |
| P2（中） | 6 | 空操作校验缺失、流转状态边界、字段清空语义、NPE 风险、留痕可读性、申请状态校验 |
| P3（低） | 4 | 废弃接口、类型安全、性能、软删除过滤 |
| 风险确认 | 5 | 需业务/数据确认后决策 |

**最高优先级修复：P1-1（`directModify` 越权）和 P1-3（`getApplyDetail` 无权限）**，这两个问题在当前生产环境中可被任意已登录用户利用。
