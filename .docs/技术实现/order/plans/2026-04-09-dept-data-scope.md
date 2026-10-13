# DEPT 数据权限扩展实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 引入 `DEPT` 部门范围数据权限类型，同步将订单表中语义为"医院科室"的 `dept_id/dept_name` 字段彻底重命名为 `hospital_dept_id/hospital_dept_name`，消除与新增的提单人部门字段 `operator_dept_id/operator_dept_name` 之间的歧义。

**Architecture:** 变更分三条主线并行推进：①字段消歧重命名（纯机械替换，不改业务逻辑）；②新增冗余字段（DB + Entity + Service 赋值）；③新增 DEPT 权限类型（枚举 + QueryHelper 过滤分支）。三条主线最终汇聚在 VO/Export 层。

**Tech Stack:** Java 21, MyBatis-Plus 3.5.8, Spring Boot 3, MySQL 8（生产）/ H2（测试）

---

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `sql/ddl.sql` | 修改 | order_main/order_draft 字段重命名 + order_main 新增2列 |
| `yigongbao-module-order/src/test/resources/schema.sql` | 修改 | H2测试库同步 |
| `yigongbao-common/.../OrderMainEntity.java` | 修改 | 字段重命名 + 新增2字段 |
| `yigongbao-module-order/.../OrderDraftEntity.java` | 修改 | 字段重命名 |
| `yigongbao-common/.../DataScopeTypeEnum.java` | 修改 | 新增 DEPT 枚举值 |
| `yigongbao-module-order/.../CreateOrderDTO.java` | 修改 | 字段重命名 |
| `yigongbao-module-order/.../UpdateOrderDTO.java` | 修改 | 字段重命名 |
| `yigongbao-module-order/.../CreateOrderDraftDTO.java` | 修改 | 字段重命名 |
| `yigongbao-module-order/.../ExecuteModificationDTO.java` | 修改 | 字段重命名 |
| `yigongbao-module-order/.../OrderDetailVO.java` | 修改 | 字段重命名 + 新增2字段 |
| `yigongbao-module-order/.../OrderListVO.java` | 修改 | 字段重命名 + 新增2字段 |
| `yigongbao-module-order/.../OrderDraftDetailVO.java` | 修改 | 字段重命名 |
| `yigongbao-module-order/.../ModifyApplyDetailVO.java` | 修改 | 字段重命名 |
| `yigongbao-module-order/.../OrderColumnConfigVO.java` | 修改 | 无字段变更，注释说明新列 |
| `yigongbao-module-order/.../OrderDataValidator.java` | 修改 | 参数名/变量名重命名 |
| `yigongbao-module-order/.../OrderQueryHelper.java` | 修改 | 字段引用重命名 + DEPT分支 + getCurrentUserDeptId() |
| `yigongbao-module-order/.../OrderMainServiceImpl.java` | 修改 | createOrder/createFromDraft 新增部门字段赋值 |
| `yigongbao-module-order/.../OrderDraftServiceImpl.java` | 修改 | 字段引用重命名 |
| `yigongbao-module-order/.../OrderExportServiceImpl.java` | 修改 | case 重命名 + 新增 operatorDeptName case |

---

## Task 1：DDL — 数据库字段变更

**Files:**
- Modify: `sql/ddl.sql`
- Modify: `yigongbao-module-order/src/test/resources/schema.sql`

> **注意**：生产环境执行需 ALTER TABLE，此处同步更新建表脚本（便于重建环境）。对已存在的生产库，需额外执行文末提供的迁移脚本。

- [ ] **Step 1：修改生产 DDL — order_draft 表**

在 `sql/ddl.sql` 中，找到 `order_draft` 表定义，将：
```sql
dept_id         BIGINT          COMMENT '科室ID',
dept_name       VARCHAR(100)     COMMENT '科室名称',
```
替换为：
```sql
hospital_dept_id   BIGINT          COMMENT '医院科室ID',
hospital_dept_name VARCHAR(100)    COMMENT '医院科室名称（冗余）',
```

- [ ] **Step 2：修改生产 DDL — order_main 表**

在 `sql/ddl.sql` 中，找到 `order_main` 表定义，将：
```sql
dept_id         BIGINT          COMMENT '科室ID',
dept_name       VARCHAR(100)    COMMENT '科室名称（冗余）',
```
替换为：
```sql
hospital_dept_id    BIGINT          COMMENT '医院科室ID',
hospital_dept_name  VARCHAR(100)    COMMENT '医院科室名称（冗余）',
operator_dept_id    BIGINT          COMMENT '提单人所属部门ID（冗余自 sys_user.dept_id）',
operator_dept_name  VARCHAR(128)    COMMENT '提单人所属部门名称（冗余自 sys_user.dept_name）',
```
新增的两列紧跟 `hospital_dept_name` 之后，放在 `doctor_id` 之前。

- [ ] **Step 3：修改 H2 测试 schema — order_draft 表**

在 `yigongbao-module-order/src/test/resources/schema.sql` 中，找到 `order_draft` 表，将：
```sql
dept_id BIGINT COMMENT '科室ID',
dept_name VARCHAR(128) COMMENT '科室名称',
```
替换为：
```sql
hospital_dept_id BIGINT COMMENT '医院科室ID',
hospital_dept_name VARCHAR(128) COMMENT '医院科室名称',
```

- [ ] **Step 4：修改 H2 测试 schema — order_main 表**

在 `yigongbao-module-order/src/test/resources/schema.sql` 中，找到 `order_main` 表，将：
```sql
dept_id BIGINT COMMENT '科室ID',
dept_name VARCHAR(128) COMMENT '科室名称',
```
替换为：
```sql
hospital_dept_id BIGINT COMMENT '医院科室ID',
hospital_dept_name VARCHAR(128) COMMENT '医院科室名称',
operator_dept_id BIGINT COMMENT '提单人所属部门ID',
operator_dept_name VARCHAR(128) COMMENT '提单人所属部门名称',
```

- [ ] **Step 5：编写生产迁移 SQL（注释形式追加到 ddl.sql 末尾）**

在 `sql/ddl.sql` 末尾追加以下迁移注释块（不自动执行，DBA 手动执行）：
```sql
-- ============================================================
-- 迁移脚本：DEPT 数据权限扩展字段变更（2026-04-09）
-- 仅对已存在的生产库执行，新建环境直接使用上方建表语句
-- ============================================================
-- ALTER TABLE order_draft
--     CHANGE COLUMN dept_id    hospital_dept_id   BIGINT          COMMENT '医院科室ID',
--     CHANGE COLUMN dept_name  hospital_dept_name VARCHAR(100)    COMMENT '医院科室名称（冗余）';
--
-- ALTER TABLE order_main
--     CHANGE COLUMN dept_id    hospital_dept_id   BIGINT          COMMENT '医院科室ID',
--     CHANGE COLUMN dept_name  hospital_dept_name VARCHAR(100)    COMMENT '医院科室名称（冗余）',
--     ADD COLUMN operator_dept_id   BIGINT         COMMENT '提单人所属部门ID（冗余自 sys_user.dept_id）' AFTER hospital_dept_name,
--     ADD COLUMN operator_dept_name VARCHAR(128)   COMMENT '提单人所属部门名称（冗余自 sys_user.dept_name）' AFTER operator_dept_id;
```

- [ ] **Step 6：Commit**
```bash
git add sql/ddl.sql yigongbao-parent/yigongbao-module-order/src/test/resources/schema.sql
git commit -m "feat(db): 重命名医院科室字段为 hospital_dept_id/name，order_main 新增 operator_dept 冗余字段"
```

---

## Task 2：Entity 层 — OrderMainEntity & OrderDraftEntity

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/entity/OrderMainEntity.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/entity/OrderDraftEntity.java`

- [ ] **Step 1：修改 OrderMainEntity — 重命名医院科室字段**

将第 106-114 行的科室字段区块替换为：
```java
// ==================== 医院与科室 ====================
/**
 * 医院ID
 */
private Long hospitalId;

/**
 * 医院名称（冗余）
 */
private String hospitalName;

/**
 * 地区ID（冗余自医院）
 */
private Long areaId;

/**
 * 地区名称（冗余自医院）
 */
private String areaName;

/**
 * 完整地区路径名称（冗余自医院，如"广东省/广州市/天河区"）
 */
private String fullAreaName;

/**
 * 医院科室ID
 */
private Long hospitalDeptId;

/**
 * 医院科室名称（冗余）
 */
private String hospitalDeptName;

/**
 * 提单人所属部门ID（冗余自 sys_user.dept_id，创建时填充，后续不可修改）
 */
private Long operatorDeptId;

/**
 * 提单人所属部门名称（冗余自 sys_user.dept_name，创建时填充，后续不可修改）
 */
private String operatorDeptName;
```

- [ ] **Step 2：修改 OrderDraftEntity — 重命名医院科室字段**

找到 OrderDraftEntity 中的 `deptId` 和 `deptName` 字段（约第 100、105 行），替换为：
```java
/**
 * 医院科室ID
 */
private Long hospitalDeptId;

/**
 * 医院科室名称（冗余）
 */
private String hospitalDeptName;
```

- [ ] **Step 3：编译验证**
```bash
cd yigongbao-parent && mvn compile -pl yigongbao-common,yigongbao-module-order -am -q 2>&1 | head -30
```
预期：编译报错（其他文件还未改，引用了旧字段名）。**忽略此处报错，继续下一 Task**。

- [ ] **Step 4：Commit**
```bash
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/entity/OrderMainEntity.java
git add yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/entity/OrderDraftEntity.java
git commit -m "feat(entity): 重命名医院科室字段，OrderMainEntity 新增 operatorDeptId/Name"
```

---

## Task 3：DataScopeTypeEnum — 新增 DEPT 枚举值

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/DataScopeTypeEnum.java`

- [ ] **Step 1：新增 DEPT 枚举值**

在 `ALL("all", "全部");` 之前插入：
```java
/**
 * 部门范围数据
 * 用户只能查看和操作同部门成员创建的数据
 * 部门通过 sys_user.dept_id 关联，业务管理员与其下属业务员同属一个部门
 */
DEPT("dept", "部门范围"),
```

完整枚举顺序应为：`SELF → HOSPITALS → ORG → DEPT → ALL`

- [ ] **Step 2：Commit**
```bash
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/DataScopeTypeEnum.java
git commit -m "feat(enum): DataScopeTypeEnum 新增 DEPT 部门范围类型"
```

---

## Task 4：DTO 层 — 4个 DTO 字段重命名

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/order/CreateOrderDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/order/UpdateOrderDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/draft/CreateOrderDraftDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/modify/ExecuteModificationDTO.java`

- [ ] **Step 1：修改 CreateOrderDTO**

将 `deptId` 字段及其注释替换为：
```java
/**
 * 医院科室ID
 */
private Long hospitalDeptId;
```
将 `deptName` 字段及其注释替换为：
```java
/**
 * 医院科室名称（可选传入，会被服务端覆盖）
 */
private String hospitalDeptName;
```

- [ ] **Step 2：修改 UpdateOrderDTO**

将 `deptId` 字段及其注释替换为：
```java
/**
 * 医院科室ID
 */
private Long hospitalDeptId;
```
（UpdateOrderDTO 中没有 `deptName`，只改 `deptId`）

- [ ] **Step 3：修改 CreateOrderDraftDTO**

同 CreateOrderDTO，将 `deptId` → `hospitalDeptId`，`deptName` → `hospitalDeptName`。

- [ ] **Step 4：修改 ExecuteModificationDTO**

将 `deptId` 字段及其注释替换为：
```java
/**
 * 医院科室ID
 */
private Long hospitalDeptId;
```
（ExecuteModificationDTO 中没有 `deptName`，只改 `deptId`）

- [ ] **Step 5：Commit**
```bash
git add yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/
git commit -m "feat(dto): DTO 层医院科室字段重命名为 hospitalDeptId/Name"
```

---

## Task 5：VO 层 — 5个 VO 字段重命名 + 新增部门字段

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/OrderDetailVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/OrderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/draft/OrderDraftDetailVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/modify/ModifyApplyDetailVO.java`

- [ ] **Step 1：修改 OrderDetailVO**

将 `deptId` → `hospitalDeptId`，`deptName` → `hospitalDeptName`。

在 `operatorPhone` 字段之后（机构信息区块末尾），新增：
```java
/**
 * 提单人所属部门ID
 */
private Long operatorDeptId;

/**
 * 提单人所属部门名称
 */
private String operatorDeptName;
```

- [ ] **Step 2：修改 OrderListVO**

将 `deptId` → `hospitalDeptId`，`deptName` → `hospitalDeptName`。

在 `operatorPhone` 字段之后，新增：
```java
/**
 * 提单人所属部门ID
 */
private Long operatorDeptId;

/**
 * 提单人所属部门名称
 */
private String operatorDeptName;
```

- [ ] **Step 3：修改 OrderDraftDetailVO**

将 `deptId` → `hospitalDeptId`，`deptName` → `hospitalDeptName`。
（无需新增 operatorDeptId/Name，草稿不参与部门权限过滤）

- [ ] **Step 4：修改 ModifyApplyDetailVO**

将 `deptName` → `hospitalDeptName`。

- [ ] **Step 5：Commit**
```bash
git add yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/
git commit -m "feat(vo): VO 层医院科室字段重命名，OrderDetailVO/ListVO 新增 operatorDeptId/Name"
```

---

## Task 6：OrderDataValidator — 参数名与变量名更新

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/validator/OrderDataValidator.java`

- [ ] **Step 1：更新 validateAndFillMaster 方法**

将方法签名中的参数名 `deptId` 改为 `hospitalDeptId`：
```java
public void validateAndFillMaster(OrderDraftEntity entity,
        Long orgId, Long hospitalId, Long hospitalDeptId,
        Long doctorId, String doctorName, String doctorPhone, Long creatorId,
        ValidateMode mode) {
```
将方法体内：
```java
HospitalDeptVO dept = lookupHospitalDept(deptId);
if (dept != null) {
    entity.setDeptName(dept.getHospitalDeptName());
}
```
改为：
```java
HospitalDeptVO dept = lookupHospitalDept(hospitalDeptId);
if (dept != null) {
    entity.setHospitalDeptName(dept.getHospitalDeptName());
}
```
同步更新方法注释中 `@param deptId` → `@param hospitalDeptId 医院科室ID`。

- [ ] **Step 2：更新 validateAndFillMasterForOrder 方法**

同上，将参数名 `deptId` → `hospitalDeptId`，调用和赋值一并更新：
```java
public void validateAndFillMasterForOrder(OrderMainEntity entity,
        Long orgId, Long hospitalId, Long hospitalDeptId,
        Long doctorId, String doctorName, String doctorPhone, Long creatorId,
        ValidateMode mode) {
    // ...
    HospitalDeptVO dept = lookupHospitalDept(hospitalDeptId);
    if (dept != null) {
        entity.setHospitalDeptName(dept.getHospitalDeptName());
    }
```

- [ ] **Step 3：更新 validateAndFillForModify 方法**

将参数名 `deptId` → `hospitalDeptId`，方法体中：
```java
if (deptId != null) {
    HospitalDeptVO dept = lookupHospitalDept(deptId);
    if (dept != null) {
        entity.setDeptName(dept.getHospitalDeptName());
    }
}
```
改为：
```java
if (hospitalDeptId != null) {
    HospitalDeptVO dept = lookupHospitalDept(hospitalDeptId);
    if (dept != null) {
        entity.setHospitalDeptName(dept.getHospitalDeptName());
    }
}
```

- [ ] **Step 4：Commit**
```bash
git add yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/validator/OrderDataValidator.java
git commit -m "feat(validator): OrderDataValidator 医院科室参数名重命名为 hospitalDeptId"
```

---

## Task 7：OrderQueryHelper — 字段引用更新 + DEPT 分支

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/helper/OrderQueryHelper.java`

- [ ] **Step 1：更新 toOrderListVO 中的科室字段引用**

将第 239-240 行：
```java
vo.setDeptId(entity.getDeptId());
vo.setDeptName(entity.getDeptName());
```
替换为：
```java
vo.setHospitalDeptId(entity.getHospitalDeptId());
vo.setHospitalDeptName(entity.getHospitalDeptName());
vo.setOperatorDeptId(entity.getOperatorDeptId());
vo.setOperatorDeptName(entity.getOperatorDeptName());
```

- [ ] **Step 2：新增 getCurrentUserDeptId() 私有方法**

在 `getCurrentUserOrgId()` 方法之后插入：
```java
/**
 * 获取当前登录用户的所属部门ID，未登录或无部门返回 null
 *
 * @return 当前用户所属部门ID
 */
public Long getCurrentUserDeptId() {
    Long userId = getCurrentUserId();
    if (userId == null) {
        return null;
    }
    UserEntity user = userService.getById(userId);
    return user != null ? user.getDeptId() : null;
}
```

- [ ] **Step 3：在 buildDataScopeCondition 中新增 DEPT 分支**

在 `case ALL:` 之前插入（`switch` 内，紧跟 `case ORG` 的 `break` 之后）：
```java
case DEPT:
    // 看同部门成员创建的订单（以提单人的部门ID过滤）
    Long deptId = getCurrentUserDeptId();
    if (deptId != null) {
        wrapper.eq(OrderMainEntity::getOperatorDeptId, deptId);
    } else {
        // 用户未配置部门，降级为仅看自己，避免泄露全量数据
        log.warn("DEPT 类型用户未配置部门，降级为 SELF，userId={}", currentUserId);
        wrapper.eq(currentUserId != null, OrderMainEntity::getCreateBy, currentUserId);
    }
    break;
```

同步更新方法注释，补充 DEPT 说明：
```java
 * - DEPT：只看同部门成员创建的订单（按 operator_dept_id 过滤）
```

- [ ] **Step 4：编译验证**
```bash
cd yigongbao-parent && mvn compile -pl yigongbao-module-order -am -q 2>&1 | head -50
```
预期：还有 Service 层未改，会有编译错误。继续下一 Task。

- [ ] **Step 5：Commit**
```bash
git add yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/helper/OrderQueryHelper.java
git commit -m "feat(helper): OrderQueryHelper 新增 DEPT 数据权限分支和 getCurrentUserDeptId"
```

---

## Task 8：Service 层 — OrderMainServiceImpl & OrderDraftServiceImpl

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderMainServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderDraftServiceImpl.java`

- [ ] **Step 1：修改 OrderMainServiceImpl — createOrder 方法**

在 `createOrder` 方法中，找到调用 `validateAndFillMasterForOrder` 的代码段（约第 742-746 行）：
```java
orderDataValidator.validateAndFillMasterForOrder(
        order,
        dto.getOrgId(), dto.getHospitalId(), dto.getDeptId(),
        dto.getDoctorId(), dto.getDoctorName(), dto.getDoctorPhone(),
        currentUserId, OrderDataValidator.ValidateMode.DIRECT);
```
替换为：
```java
orderDataValidator.validateAndFillMasterForOrder(
        order,
        dto.getOrgId(), dto.getHospitalId(), dto.getHospitalDeptId(),
        dto.getDoctorId(), dto.getDoctorName(), dto.getDoctorPhone(),
        currentUserId, OrderDataValidator.ValidateMode.DIRECT);
```

在 `save(order)` 之前，新增提单人部门字段赋值（放在 `order.setCurrentHandlerId(currentUserId)` 之后）：
```java
// 提单人部门信息冗余写入（创建时固化，后续不可修改）
order.setOperatorDeptId(currentUser.getDeptId());
order.setOperatorDeptName(currentUser.getDeptName());
```

同步更新方法注释中的字段说明：将 `deptName: 从医院科室表查询覆盖` 改为 `hospitalDeptName: 从医院科室表查询覆盖`。

- [ ] **Step 2：修改 OrderMainServiceImpl — createFromDraft 方法**

找到草稿创建部分，在获取 `operatorName` 后，同步读取提单人的部门信息。在 `flowFacade.executeFlow(...)` 之前、`UserEntity user = userService.getById(draft.getOperatorId())` 查询之后，追加：
```java
// 提单人部门信息冗余写入（草稿提交时从提单人账号读取，创建后固化）
if (user != null) {
    order.setOperatorDeptId(user.getDeptId());
    order.setOperatorDeptName(user.getDeptName());
}
```

注意：`createFromDraft` 中已有 `UserEntity user = userService.getById(draft.getOperatorId())` 查询，直接复用该变量即可，无需重复查询。

- [ ] **Step 3：修改 OrderMainServiceImpl — updateOrder 方法（拒绝更新部门字段）**

`updateOrder` 方法中已通过 `BeanUtils.copyProperties(dto, entity, "id", ...)` 排除不可变字段。
确认 `BeanUtils.copyProperties` 的第三个参数（忽略字段列表）不包含 `operatorDeptId/operatorDeptName`。

> 此处无需额外操作：`UpdateOrderDTO` 中不含 `operatorDeptId/operatorDeptName` 字段，`BeanUtils.copyProperties` 不会覆盖 Entity 上已有的值。无需修改。

- [ ] **Step 4：修改 OrderDraftServiceImpl — 科室字段引用更新**

在 `OrderDraftServiceImpl` 中，找到所有 `setDeptId`、`setDeptName`、`getDeptId`、`getDeptName` 调用，替换为对应的 `hospitalDept` 版本：
- `entity.setDeptId(...)` → `entity.setHospitalDeptId(...)`
- `entity.setDeptName(...)` → `entity.setHospitalDeptName(...)`
- `vo.setDeptId(...)` → `vo.setHospitalDeptId(...)`
- `vo.setDeptName(...)` → `vo.setHospitalDeptName(...)`
- `dto.getDeptId()` → `dto.getHospitalDeptId()`

以及在调用 `validateAndFillMaster(..., dto.getDeptId(), ...)` 的地方改为 `dto.getHospitalDeptId()`。

- [ ] **Step 5：编译验证**
```bash
cd yigongbao-parent && mvn compile -pl yigongbao-module-order -am -q 2>&1 | head -50
```
预期：仍可能有 Export 或其他文件报错，继续下一 Task。

- [ ] **Step 6：Commit**
```bash
git add yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/
git commit -m "feat(service): createOrder/createFromDraft 写入 operatorDeptId/Name，科室字段引用更新"
```

---

## Task 9：OrderExportServiceImpl — 列名更新 + 新增部门列

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderExportServiceImpl.java`

- [ ] **Step 1：更新 setCellValue 中的科室 case**

将：
```java
case "deptName":
    cell.setCellValue(StrUtil.nullToEmpty(order.getDeptName()));
    break;
```
替换为：
```java
case "hospitalDeptName":
    cell.setCellValue(StrUtil.nullToEmpty(order.getHospitalDeptName()));
    break;
```

- [ ] **Step 2：新增 operatorDeptName case**

在 `case "operatorName":` 之后插入：
```java
case "operatorDeptName":
    cell.setCellValue(StrUtil.nullToEmpty(order.getOperatorDeptName()));
    break;
```

- [ ] **Step 3：编译验证（全模块）**
```bash
cd yigongbao-parent && mvn compile -pl yigongbao-module-order -am -q 2>&1 | head -50
```
预期：**无报错**。若仍有错误，根据错误信息定位遗漏的字段引用并修复。

- [ ] **Step 4：Commit**
```bash
git add yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderExportServiceImpl.java
git commit -m "feat(export): Export 列名更新为 hospitalDeptName，新增 operatorDeptName 导出列"
```

---

## Task 10：全量编译 + 运行测试

- [ ] **Step 1：全量编译**
```bash
cd yigongbao-parent && mvn clean compile -q 2>&1 | tail -20
```
预期：`BUILD SUCCESS`，无任何 error。

- [ ] **Step 2：运行 order 模块测试**
```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-order 2>&1 | tail -30
```
预期：所有测试 PASS。若有测试失败，根据报错定位失败的测试方法，检查是否有测试用例仍使用旧字段名 `deptId`/`deptName`，统一更新为 `hospitalDeptId`/`hospitalDeptName`。

- [ ] **Step 3：运行全量测试**
```bash
cd yigongbao-parent && mvn test -q 2>&1 | tail -30
```
预期：`BUILD SUCCESS`，无 FAILED。

- [ ] **Step 4：最终 Commit**
```bash
git add -A
git status
# 确认只有测试文件的修改（如有），没有意外改动
git commit -m "test: 修复测试用例中的旧字段名引用（hospitalDeptId/Name）"
```

---

## 附录：init.sql 和 DefaultConfigProperties 已在设计阶段完成

以下两项已在设计（brainstorming）阶段完成，**无需在本计划中重复执行**：

- `sql/init.sql`：`order.column.config` 已升级为完整 JSON 结构，包含 `hospitalDeptName` 和 `operatorDeptName` 列定义
- `yigongbao-common/.../DefaultConfigProperties.java`：兜底默认值已同步为新结构

---

## 验收检查清单

- [ ] `mvn clean compile` 无报错
- [ ] `mvn test` 全量通过
- [ ] `order_main` 表含 `hospital_dept_id`、`hospital_dept_name`、`operator_dept_id`、`operator_dept_name` 四列
- [ ] `order_draft` 表含 `hospital_dept_id`、`hospital_dept_name`，不含 `operator_dept_*`
- [ ] `DataScopeTypeEnum` 含 `DEPT("dept", "部门范围")` 枚举值
- [ ] `OrderQueryHelper.buildDataScopeCondition` 包含 `case DEPT` 分支
- [ ] `createOrder` 写入 `operatorDeptId/operatorDeptName`
- [ ] `createFromDraft` 写入 `operatorDeptId/operatorDeptName`
- [ ] 全局搜索 `deptId`（在 order 模块范围内）结果为 0（已全部替换）
- [ ] 全局搜索 `setDeptName`（在 order 模块范围内）结果为 0
