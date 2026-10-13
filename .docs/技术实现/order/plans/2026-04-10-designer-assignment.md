# 设计师分配功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现订单设计师分配功能，包含 specialty 多选改造、rebuild_body_part 清理、Flow 状态机扩展、分配服务新增，以及全套单元测试、静态代码审查和文档更新。

**Architecture:** 按模块依赖顺序分阶段实施：common（错误码/配置枚举）→ flow（状态机）→ system（用户 specialty 多选）→ basic（重建项目/部位）→ order pom 补依赖 → order（分配服务/接口）→ 测试 → 代码审查 → 文档。每个阶段完成后独立可测，最终 auditPass 触发分配逻辑串联全链路。

**Tech Stack:** Java 21, Spring Boot, MyBatis Plus 3.5.8, Hutool 5.8.26, JUnit 5 + Mockito, SaToken 1.37.0, H2（测试）

---

## 影响域速查

| 模块 | 文件 | 改动类型 |
|------|------|---------|
| common | `ErrorCodeEnum` | 新增 4 个设计师错误码（723-726）+ 1 个权限错误码（727） |
| common | `SystemConfigKeyEnum` | 新增 2 个配置项 |
| flow | `FlowStatusEnum` | 新增 `PENDING_DESIGN(21)`，原 `DESIGNING(21)` 改名为 `DESIGN_IN_PROGRESS(22)`，其余顺延 |
| flow | `FlowPhaseTransitionRules` | `DATA_AUDIT_PASSED` 初始状态改为 `PENDING_DESIGN` |
| flow | `FlowStatusTransitionRules` | 新增 `PENDING_DESIGN` 转换规则，`START_DESIGN` 目标改为 `DESIGN_IN_PROGRESS` |
| flow | `FlowStatusTransitionRulesTest` | 更新所有涉及 `DESIGNING`/`21`/`22`/`25` 的测试断言 |
| flow | `FlowPhaseTransitionRulesTest` | 更新断言 `DESIGNING` → `PENDING_DESIGN` |
| system | `CreateUserDTO` / `UpdateUserDTO` | `specialty: String` → `specialtyList: List<String>` |
| system | `UserVO` | 新增 `specialtyList`、`specialtyNameList` |
| system | `UserServiceImpl` | `validateSpecialty` 改为多值遍历；`toVOWithNames` 填充多值名称 |
| system | `UserServiceImplTest` | 更新 specialty 相关测试 |
| system | `schema.sql`（system 模块） | `specialty VARCHAR(64→255)` |
| basic | `RebuildProjectEntity/DTO/VO` | 新增 `specialty` 字段 |
| basic | `RebuildProjectService` | 新增 `getSpecialtyByProjectId(Long)` 方法 |
| basic | `RebuildProjectServiceImpl` | `create`/`update` 读写 `specialty`；实现新接口方法 |
| basic | `BodyPartEntity/DTO/VO` | 移除 `designerCode` 字段 |
| basic | `BodyPartServiceImpl` | 移除 `designerCode` 读写 |
| basic | `BodyPartControllerTest` | 移除 `designerCode` 测试数据 |
| basic | `schema.sql`（basic/boot 模块） | 同步 DDL |
| order | `pom.xml` | 新增对 `yigongbao-module-system` 和 `yigongbao-module-basic` 的显式依赖 |
| order | `DesignerAssignmentService` + `Impl` | 新增 |
| order | `DesignerVO` / `AssignDesignerDTO` / `DesignerQueryDTO` | 新增 |
| order | `OrderController` | 新增 3 个接口 |
| order | `OrderMainServiceImpl.auditPass` | 集成分配触发（catch 异常，不回滚审核） |
| system | `UserMapper` | 新增 2 个设计师候选查询方法（`FIND_IN_SET`） |
| system | `UserEntity` | 新增 `@TableField(exist=false) currentLoad` 字段 |
| DB | `sql/ddl.sql` | `sys_user`、`rebuild_project`、`rebuild_body_part` |
| DB | `sql/init.sql` | `sys_config` 种子数据 |
| 文档 | 多个 .md 文件 | 同步更新 |

---

## Task 1：common — 新增错误码和系统配置枚举

**Files:**
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java`

- [ ] **Step 1: 在 ErrorCodeEnum 中追加设计师相关错误码**

在最后一个枚举值 `ORDER_MODIFY_TYPE_NOT_ALLOWED_IN_PHASE(722, ...)` 之后，分号前追加：

```java
// ==================== 设计师分配（723-729）====================
DESIGNER_NOT_FOUND(723, "设计师不存在"),
DESIGNER_ROLE_INVALID(724, "用户角色不是设计师或设计师管理员"),
DESIGNER_DISABLED(725, "设计师已被禁用"),
DESIGNER_SPECIALTY_MISMATCH(726, "设计师专业方向与订单项目专业方向不一致"),
ORDER_DESIGNER_MISMATCH(727, "非分配设计师，无权操作此订单"),
```

- [ ] **Step 2: 在 SystemConfigKeyEnum 中追加分配配置项**

```java
/**
 * 设计师分配模式（auto-自动分配，manual-手动分配）
 */
DESIGN_ASSIGN_MODE("design.assign.mode", "设计师分配模式"),

/**
 * 单个设计师最大并发设计工单数（默认 10）
 */
DESIGN_ASSIGN_MAX_CAPACITY("design.assign.max.capacity", "设计师最大并发工单数"),
```

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent && mvn compile -pl yigongbao-common -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java \
        yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java
git commit -m "feat(common): 新增设计师分配错误码(723-727)和系统配置枚举"
```

---

## Task 2：flow — 状态机扩展

**核心改动说明：**
- 新增 `PENDING_DESIGN(21)` 表示"待设计/待分配"（审核通过后进入的中间状态）
- 原 `DESIGNING(21)` 改名为 `DESIGN_IN_PROGRESS(22)`（设计师已开始设计）
- 其余设计阶段状态码顺延：`22→23`、`23→24`、`24→25`、`25→26`
- `FlowActionEnum.START_DESIGN` 已存在，无需新增
- `DESIGN_REVIEW_PASSED` 不可见状态由 24 变为 25，`isInvisibleStatus` 引用枚举常量无需额外改动

**Files:**
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowStatusEnum.java`
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/rules/FlowPhaseTransitionRules.java`
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/rules/FlowStatusTransitionRules.java`
- Modify: `yigongbao-module-flow/src/test/java/com/yigongbao/flow/rules/FlowStatusTransitionRulesTest.java`
- Modify: `yigongbao-module-flow/src/test/java/com/yigongbao/flow/rules/FlowPhaseTransitionRulesTest.java`

- [ ] **Step 1: 修改 FlowStatusEnum — 替换设计阶段枚举段**

将 `// ==================== 设计阶段（20-29）====================` 到 `DESIGN_REVIEW_REJECTED(25, "设计审核不通过"),` 整段替换为：

```java
// ==================== 设计阶段（20-29）====================
/**
 * 待设计（审核通过后进入；已分配设计师或待分配）
 */
PENDING_DESIGN(21, "待设计"),

/**
 * 设计中（设计师已开始设计）
 */
DESIGN_IN_PROGRESS(22, "设计中"),

/**
 * 设计完成
 */
DESIGN_COMPLETED(23, "设计完成"),

/**
 * 设计审核中
 */
DESIGN_REVIEWING(24, "设计审核中"),

/**
 * 设计审核通过（不可见状态，系统自动推进到下一阶段）
 */
DESIGN_REVIEW_PASSED(25, "设计审核通过"),

/**
 * 设计审核不通过
 */
DESIGN_REVIEW_REJECTED(26, "设计审核不通过"),
```

- [ ] **Step 2: 修改 FlowPhaseTransitionRules — DATA_AUDIT_PASSED 初始状态**

在 `decideNextPhaseAndStatus` 方法中，将：
```java
if (targetStatus == FlowStatusEnum.DATA_AUDIT_PASSED) {
    return new PhaseAndStatus(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGNING);
}
```
改为：
```java
if (targetStatus == FlowStatusEnum.DATA_AUDIT_PASSED) {
    return new PhaseAndStatus(FlowPhaseEnum.DESIGN, FlowStatusEnum.PENDING_DESIGN);
}
```

- [ ] **Step 3: 修改 FlowStatusTransitionRules — 三处更新**

**3a. STATUS_TRANSITIONS 静态块** — 将设计阶段部分替换为：

```java
// ==================== 设计阶段状态转换（20-29）====================
// PENDING_DESIGN(21) → DESIGN_IN_PROGRESS(22)（设计师开始设计）
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.PENDING_DESIGN),
        Set.of(FlowStatusEnum.DESIGN_IN_PROGRESS));

// DESIGN_IN_PROGRESS(22) → DESIGN_COMPLETED(23)（提交设计）
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_IN_PROGRESS),
        Set.of(FlowStatusEnum.DESIGN_COMPLETED));

transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_COMPLETED),
        Set.of(FlowStatusEnum.DESIGN_REVIEWING));

transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_REVIEWING),
        Set.of(FlowStatusEnum.DESIGN_REVIEW_PASSED, FlowStatusEnum.DESIGN_REVIEW_REJECTED));

// 审核驳回后可重新开始设计
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_REVIEW_REJECTED),
        Set.of(FlowStatusEnum.DESIGN_IN_PROGRESS));
```

**3b. `getAvailableActions` 方法** — DESIGN 分支替换为：

```java
case DESIGN -> switch (status) {
    case PENDING_DESIGN -> List.of(FlowActionEnum.START_DESIGN);
    case DESIGN_IN_PROGRESS -> List.of(FlowActionEnum.SUBMIT_DESIGN);
    case DESIGN_COMPLETED -> List.of(FlowActionEnum.SUBMIT_DESIGN);
    case DESIGN_REVIEWING -> List.of(FlowActionEnum.DESIGN_REVIEW_PASS, FlowActionEnum.DESIGN_REVIEW_REJECT);
    case DESIGN_REVIEW_REJECTED -> List.of(FlowActionEnum.START_DESIGN);
    default -> List.of();
};
```

**3c. `getTargetStatus` 方法** — 设计阶段动作部分替换为：

```java
// 设计阶段动作
case START_DESIGN -> FlowStatusEnum.DESIGN_IN_PROGRESS.getValue();
case SUBMIT_DESIGN -> FlowStatusEnum.DESIGN_REVIEWING.getValue();
case DESIGN_REVIEW_PASS -> FlowStatusEnum.DESIGN_REVIEW_PASSED.getValue();
case DESIGN_REVIEW_REJECT -> FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue();
```

**3d. `getValidStatusesForPhase` 方法** — DESIGN 分支替换为：

```java
case DESIGN -> Set.of(FlowStatusEnum.PENDING_DESIGN, FlowStatusEnum.DESIGN_IN_PROGRESS,
        FlowStatusEnum.DESIGN_COMPLETED, FlowStatusEnum.DESIGN_REVIEWING,
        FlowStatusEnum.DESIGN_REVIEW_REJECTED);
```

- [ ] **Step 4: 先运行测试，确认编译失败（旧枚举名 DESIGNING 已消失）**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-flow -q 2>&1 | tail -15
```

Expected: FAIL（编译错误，`DESIGNING` 找不到）

- [ ] **Step 5: 更新 FlowPhaseTransitionRulesTest**

找到测试方法 `dataAuditPassed_shouldAdvanceToDesign`，将：
```java
assertEquals(FlowStatusEnum.DESIGNING, result.initialStatus());
```
改为：
```java
assertEquals(FlowStatusEnum.PENDING_DESIGN, result.initialStatus());
```

找到含 `isInvisibleStatus(FlowStatusEnum.DESIGNING)` 的断言，将 `DESIGNING` 改为 `DESIGN_IN_PROGRESS`。

- [ ] **Step 6: 更新 FlowStatusTransitionRulesTest**

按以下对照表批量替换：

| 旧内容 | 新内容 |
|-------|-------|
| `FlowStatusEnum.DESIGNING` | `FlowStatusEnum.DESIGN_IN_PROGRESS` |
| `getAvailableActions(21, 2, 1)` 断言 `SUBMIT_DESIGN` | 改为断言 `List.of(FlowActionEnum.START_DESIGN)` + DisplayName 改为 `PENDING_DESIGN(21) → [START_DESIGN]` |
| `getAvailableActions(22, 2, 1)` 断言 `SUBMIT_DESIGN` | DisplayName 改为 `DESIGN_IN_PROGRESS(22) → [SUBMIT_DESIGN]`（断言不变） |
| `getAvailableActions(23, 2, 1)` | DisplayName 改为 `DESIGN_COMPLETED(23)` |
| `getAvailableActions(25, 2, 1)` 断言 `START_DESIGN` | 改为 `getAvailableActions(26, 2, 1)`，DisplayName 改为 `DESIGN_REVIEW_REJECTED(26)` |
| `getTargetStatus(?, START_DESIGN)` 断言 `DESIGNING` 值 `21` | 改为断言 `DESIGN_IN_PROGRESS` 值 `22` |

同时新增一条 `PENDING_DESIGN → START_DESIGN` 的目标状态测试：

```java
@Test
@DisplayName("START_DESIGN → DESIGN_IN_PROGRESS(22)")
void startDesign_shouldTarget_designInProgress() {
    Integer target = rules.getTargetStatus(21, FlowActionEnum.START_DESIGN);
    assertEquals(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(), target);
}
```

- [ ] **Step 7: 运行 flow 模块测试**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-flow
```

Expected: BUILD SUCCESS，所有测试通过

- [ ] **Step 8: Commit**

```bash
git add yigongbao-module-flow/
git commit -m "feat(flow): 新增 PENDING_DESIGN(21)，设计阶段状态码顺延，更新状态机规则和测试"
```

---

## Task 3：system — 用户 specialty 多选改造

**核心改动说明：**
- `sys_user.specialty` 存储逗号拼接多值（如 `"7.1,7.2"`），字段长度扩展为 VARCHAR(255)
- DTO 的 `specialty: String` 改为 `specialtyList: List<String>`（前端传数组，后端拼接存储）
- VO 新增 `specialtyList`（编码列表）和 `specialtyNameList`（名称列表），保留原 `specialty`/`specialtyName` 字段向后兼容
- `validateSpecialty` 改为遍历校验每项，签名同步改为接收 `List<String>`
- `UserEntity` 新增 `@TableField(exist=false) currentLoad` 字段供 order 模块分配时使用

**Files:**
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/CreateUserDTO.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/UpdateUserDTO.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/UserVO.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/entity/UserEntity.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/mapper/UserMapper.java`
- Modify: `yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserServiceImplTest.java`
- Modify: `yigongbao-module-system/src/test/resources/schema.sql`

- [ ] **Step 1: 修改 CreateUserDTO / UpdateUserDTO — specialty 改为 List**

在 `CreateUserDTO.java` 中，将：
```java
@Size(max = 64, message = "专业方向长度不能超过64个字符")
private String specialty;
```
替换为：
```java
/**
 * 专业方向字典编码列表（设计师/设计师管理员必填，如 ["7.1", "7.2"]）
 */
private List<String> specialtyList;
```
并在文件顶部补充 `import java.util.List;`（如尚未有）。

`UpdateUserDTO.java` 同样替换，字段名统一为 `specialtyList`。

- [ ] **Step 2: 修改 UserVO — 新增多值字段**

在 `specialtyName` 字段之后追加：

```java
/**
 * 专业方向字典编码列表（多选，供前端展示）
 */
private List<String> specialtyList;

/**
 * 专业方向名称列表（多选，供前端展示）
 */
private List<String> specialtyNameList;
```

- [ ] **Step 3: 修改 UserEntity — 新增 currentLoad 瞬态字段**

在 `specialty` 字段之后追加：

```java
/**
 * 当前在手工单数（非数据库字段，由自定义 SQL 查询填充）
 */
@TableField(exist = false)
private Integer currentLoad;
```

- [ ] **Step 4: 修改 UserServiceImpl — validateSpecialty 改为多值校验**

将方法签名和实现整体替换（约第 594-611 行）：

```java
/**
 * 校验设计师专业方向：当角色为 designer/designer-manager 时，至少选择一个方向且全部合法
 *
 * @param role          生效角色（null 时跳过校验）
 * @param specialtyList 专业方向字典编码列表
 */
private void validateSpecialty(RoleEntity role, List<String> specialtyList) {
    if (role == null || role.getRoleCode() == null
            || !SPECIALTY_REQUIRED_ROLES.contains(role.getRoleCode())) {
        return;
    }
    if (CollUtil.isEmpty(specialtyList)) {
        log.warn("角色为设计师/设计师管理员，但未指定专业方向，roleId={}", role.getId());
        throw new BusinessException(ErrorCodeEnum.USER_ROLE_SPECIALTY_REQUIRED);
    }
    String prefix = DictCodeConstants.USER_SPECIALTY + ".";
    for (String specialty : specialtyList) {
        if (StrUtil.isBlank(specialty) || !specialty.startsWith(prefix)) {
            log.warn("专业方向字典编码无效，specialty={}", specialty);
            throw new BusinessException(ErrorCodeEnum.USER_SPECIALTY_INVALID, prefix);
        }
        if (dictService.getByDictCode(specialty) == null) {
            log.warn("专业方向字典编码不存在，specialty={}", specialty);
            throw new BusinessException(ErrorCodeEnum.USER_SPECIALTY_INVALID, specialty);
        }
    }
}
```

- [ ] **Step 5: 修改 UserServiceImpl — create 方法中处理多值存储**

将 `validateSpecialty(roleEntity, dto.getSpecialty())` 改为：
```java
validateSpecialty(roleEntity, dto.getSpecialtyList());
```

在 `UserEntity entity = UserConvert.toEntity(dto);` 之后补充：
```java
// specialty List → 逗号拼接存储
if (CollUtil.isNotEmpty(dto.getSpecialtyList())) {
    entity.setSpecialty(CollUtil.join(dto.getSpecialtyList(), ","));
} else {
    entity.setSpecialty(null);
}
```

- [ ] **Step 6: 修改 UserServiceImpl — update 方法中处理多值存储**

将 `validateSpecialty(effectiveRole, dto.getSpecialty())` 改为：
```java
validateSpecialty(effectiveRole, dto.getSpecialtyList());
```

在实体字段赋值处（`entity.setRemark` 附近）补充：
```java
if (dto.getSpecialtyList() != null) {
    entity.setSpecialty(CollUtil.join(dto.getSpecialtyList(), ","));
}
```

- [ ] **Step 7: 修改 UserServiceImpl — toVOWithNames 中填充多值名称**

将约第 732-735 行的单值 specialtyName 填充逻辑替换为：

```java
// 填充专业方向多值列表及名称列表
if (StrUtil.isNotBlank(vo.getSpecialty())) {
    List<String> specList = StrUtil.splitToList(vo.getSpecialty(), ',');
    vo.setSpecialtyList(specList);
    List<String> nameList = specList.stream()
            .map(code -> {
                var dict = dictService.getByDictCode(code);
                return dict != null ? dict.getDictName() : code;
            })
            .collect(Collectors.toList());
    vo.setSpecialtyNameList(nameList);
    // 保持 specialtyName 向后兼容（逗号拼接）
    vo.setSpecialtyName(String.join(",", nameList));
}
```

- [ ] **Step 8: 在 UserMapper 新增设计师候选查询方法**

```java
/**
 * 查询符合专业方向的设计师候选列表（按当前在手工单数升序）
 * 用于自动分配：取负载最低的第一位
 *
 * @param specialty   项目专业方向（单值，如 "7.1"）
 * @param maxCapacity 最大并发工单数上限（不含）
 * @return 设计师列表，已按 current_load ASC 排序
 */
@Select("""
    SELECT u.*,
           (SELECT COUNT(*) FROM order_main om
            WHERE om.designer_id = u.id
              AND om.status BETWEEN 21 AND 29
              AND om.is_deleted = 0) AS current_load
    FROM sys_user u
    WHERE u.role_code IN ('designer', 'designer-manager')
      AND u.status = 1
      AND u.is_deleted = 0
      AND FIND_IN_SET(#{specialty}, u.specialty) > 0
      AND (SELECT COUNT(*) FROM order_main om
           WHERE om.designer_id = u.id
             AND om.status BETWEEN 21 AND 29
             AND om.is_deleted = 0) < #{maxCapacity}
    ORDER BY current_load ASC
    """)
List<UserEntity> selectAvailableDesigners(@Param("specialty") String specialty,
                                          @Param("maxCapacity") int maxCapacity);

/**
 * 查询符合任意一个专业方向的设计师列表（用于手动分配时的候选展示）
 * 注意：specialtyCondition 由 Service 层使用严格正则校验后拼接，防止注入
 *
 * @param specialtyCondition 已校验的 FIND_IN_SET 条件串，如
 *        "FIND_IN_SET('7.1', specialty) > 0 OR FIND_IN_SET('7.2', specialty) > 0"
 * @param maxCapacity        最大并发工单数上限
 * @return 设计师列表，已按 current_load ASC 排序
 */
@Select("""
    SELECT u.*,
           (SELECT COUNT(*) FROM order_main om
            WHERE om.designer_id = u.id
              AND om.status BETWEEN 21 AND 29
              AND om.is_deleted = 0) AS current_load
    FROM sys_user u
    WHERE u.role_code IN ('designer', 'designer-manager')
      AND u.status = 1
      AND u.is_deleted = 0
      AND (${specialtyCondition})
    ORDER BY current_load ASC
    """)
List<UserEntity> selectDesignersBySpecialties(
        @Param("specialtyCondition") String specialtyCondition,
        @Param("maxCapacity") int maxCapacity);
```

> **安全说明：** `specialtyCondition` 使用 `${}` 拼接，Service 层**必须**用正则 `^\d+\.\d+$` 对每个 specialty 值做白名单校验（见 Task 6 Step 6），长度限制 ≤ 16 字符，校验不通过直接拒绝，不进入 SQL。

- [ ] **Step 9: 更新 schema.sql — specialty 字段长度**

在 `yigongbao-module-system/src/test/resources/schema.sql` 中将：
```sql
specialty           VARCHAR(64)     COMMENT '专业方向',
```
改为：
```sql
specialty           VARCHAR(255)    COMMENT '专业方向（多选逗号拼接，如 7.1,7.2）',
```

同时找到 designer1 用户的测试数据 INSERT 行，将 `specialty` 值从 `'7.1.2'`（三级）统一改为 `'7.1'`（二级）。

- [ ] **Step 10: 更新 UserServiceImplTest**

将所有 `dto.setSpecialty("7.1")` 改为 `dto.setSpecialtyList(List.of("7.1"))`。

新增以下测试用例：

```java
@Test
@DisplayName("创建设计师 — 多个合法专业方向，成功")
void createUser_designerWithMultipleSpecialties_success() {
    // given: specialtyList = ["7.1", "7.2"], roleCode = "designer"
    // mock: dictService.getByDictCode("7.1") 和 "7.2" 均返回非 null
    // then: 不抛异常，verify save 被调用一次
}

@Test
@DisplayName("创建设计师 — 专业方向编码格式无效，抛 USER_SPECIALTY_INVALID")
void createUser_invalidSpecialtyCode_throwsException() {
    // given: specialtyList = ["invalid"]
    // then: assertThrows BusinessException, errorCode == USER_SPECIALTY_INVALID
}

@Test
@DisplayName("创建设计师 — 未传专业方向，抛 USER_ROLE_SPECIALTY_REQUIRED")
void createUser_designerWithoutSpecialty_throwsException() {
    // given: specialtyList = null
    // then: assertThrows BusinessException, errorCode == USER_ROLE_SPECIALTY_REQUIRED
}
```

- [ ] **Step 11: 运行 system 模块测试**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-system
```

Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add yigongbao-module-system/
git commit -m "feat(system): specialty 改为多选逗号拼接，UserMapper 新增设计师候选查询方法"
```

---

## Task 4：basic — 移除 designerCode + 新增 rebuild_project.specialty

### Part A：移除 rebuild_body_part.designer_code

**Files:**
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/bodyPart/entity/BodyPartEntity.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/bodyPart/dto/CreateBodyPartDTO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/bodyPart/dto/UpdateBodyPartDTO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/bodyPart/vo/BodyPartVO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/bodyPart/vo/BodyPartDetailVO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/bodyPart/service/impl/BodyPartServiceImpl.java`
- Modify: `yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/bodyPart/controller/BodyPartControllerTest.java`
- Modify: `yigongbao-module-basic/src/test/resources/schema.sql`
- Modify: `yigongbao-boot/src/test/resources/schema.sql`

- [ ] **Step 1: 移除 BodyPartEntity / DTO / VO 中的 designerCode 字段**

在以下 5 个文件中删除 `designerCode` 字段声明（含 Javadoc 注释）：
- `BodyPartEntity.java`
- `CreateBodyPartDTO.java`
- `UpdateBodyPartDTO.java`
- `BodyPartVO.java`
- `BodyPartDetailVO.java`

- [ ] **Step 2: 移除 BodyPartServiceImpl 中的 designerCode 读写**

- 在 `updateBodyPart` 方法中删除：`entity.setDesignerCode(dto.getDesignerCode());`
- 在 `toDetailVO` 方法中删除：`vo.setDesignerCode(entity.getDesignerCode());`

- [ ] **Step 3: 更新 BodyPartControllerTest**

删除以下三处（出现位置不同行）：
- `vo.setDesignerCode("A");`（两处）
- 请求体 JSON 中的 `"designerCode", "A"` 键值对

- [ ] **Step 4: 更新两个 schema.sql 的 rebuild_body_part 表**

在 `yigongbao-module-basic/src/test/resources/schema.sql` 和 `yigongbao-boot/src/test/resources/schema.sql` 中：
- 删除建表语句中的 `designer_code VARCHAR(...) ...` 列定义
- 删除 INSERT 测试数据中对应列的值

### Part B：新增 rebuild_project.specialty

**Files:**
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/entity/RebuildProjectEntity.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/dto/CreateRebuildProjectDTO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/dto/UpdateRebuildProjectDTO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/vo/RebuildProjectVO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/service/RebuildProjectService.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/service/impl/RebuildProjectServiceImpl.java`
- Modify: `yigongbao-module-basic/src/test/resources/schema.sql`
- Modify: `yigongbao-boot/src/test/resources/schema.sql`

- [ ] **Step 5: RebuildProjectEntity 新增 specialty 字段**

在 `remark` 字段之前插入：
```java
/**
 * 专业方向字典编码（单值，如 "7.1"，关联 sys_dict；用于自动匹配设计师）
 */
private String specialty;
```

- [ ] **Step 6: CreateRebuildProjectDTO / UpdateRebuildProjectDTO 新增 specialty 字段**

在两个 DTO 的 `remark` 字段之前均新增：
```java
/**
 * 专业方向字典编码（单值，如 "7.1"）
 */
private String specialty;
```

- [ ] **Step 7: RebuildProjectVO 新增 specialty 和 specialtyName 字段**

```java
/**
 * 专业方向字典编码
 */
private String specialty;

/**
 * 专业方向名称（来自 sys_dict）
 */
private String specialtyName;
```

- [ ] **Step 8: RebuildProjectService 接口新增查询方法**

```java
/**
 * 根据项目ID查询专业方向字典编码
 * 供 order 模块的设计师分配逻辑使用
 *
 * @param projectId 重建项目ID
 * @return 专业方向字典编码（如 "7.1"），项目不存在或未设置时返回 null
 */
String getSpecialtyByProjectId(Long projectId);
```

- [ ] **Step 9: RebuildProjectServiceImpl 实现新方法 + create/update 写入 specialty**

在 `createProject` 方法（`entity.setCode(...)` 之后）追加：
```java
entity.setSpecialty(dto.getSpecialty());
```

在 `updateProject` 方法（`entity.setRemark(...)` 之后）追加：
```java
entity.setSpecialty(dto.getSpecialty());
```

在 VO 转换方法（`toVO` 或 `toDetailVO`）中追加 specialtyName 翻译（注入 `DictService`）：
```java
if (StrUtil.isNotBlank(entity.getSpecialty())) {
    vo.setSpecialty(entity.getSpecialty());
    var dict = dictService.getByDictCode(entity.getSpecialty());
    vo.setSpecialtyName(dict != null ? dict.getDictName() : null);
}
```

实现新接口方法：
```java
@Override
public String getSpecialtyByProjectId(Long projectId) {
    if (projectId == null) {
        return null;
    }
    RebuildProjectEntity project = getById(projectId);
    return project != null ? project.getSpecialty() : null;
}
```

- [ ] **Step 10: 更新两个 schema.sql — rebuild_project 新增 specialty 列**

在 `yigongbao-module-basic/src/test/resources/schema.sql` 和 `yigongbao-boot/src/test/resources/schema.sql` 的 rebuild_project 建表语句中，在 `remark` 列之前新增：
```sql
specialty           VARCHAR(64)     DEFAULT NULL COMMENT '专业方向字典编码（单值，如 7.1）',
```

同时更新 INSERT 测试数据，为各项目行补充 specialty 值（如颅骨重建对应 `'7.1'`）。

- [ ] **Step 11: 运行 basic 模块测试**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-basic
```

Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add yigongbao-module-basic/ yigongbao-boot/src/test/resources/schema.sql
git commit -m "feat(basic): 移除 body_part.designerCode；rebuild_project 新增 specialty 字段"
```

---

## Task 5：DB — 更新主 DDL 和种子数据

**Files:**
- Modify: `sql/ddl.sql`
- Modify: `sql/init.sql`（若无此文件，在 ddl.sql 末尾追加 INSERT）

- [ ] **Step 1: ddl.sql — sys_user.specialty 字段扩展**

将：
```sql
specialty           VARCHAR(64)     DEFAULT NULL COMMENT '专业方向',
```
改为：
```sql
specialty           VARCHAR(255)    DEFAULT NULL COMMENT '专业方向（多选逗号拼接，如 7.1,7.2）',
```

- [ ] **Step 2: ddl.sql — rebuild_body_part 移除 designer_code**

删除：
```sql
designer_code   VARCHAR(10)     DEFAULT NULL COMMENT '设计师编号（如A/B/C）',
```

- [ ] **Step 3: ddl.sql — rebuild_project 新增 specialty**

在 `remark` 列之前新增：
```sql
specialty             VARCHAR(64)     DEFAULT NULL COMMENT '专业方向字典编码（单值，如 7.1）',
```

- [ ] **Step 4: 新增 sys_config 种子数据**

```sql
INSERT INTO sys_config (config_key, config_value, config_name, remark, is_deleted) VALUES
('design.assign.mode', 'manual', '设计师分配模式', 'auto-自动分配，manual-手动分配', 0),
('design.assign.max.capacity', '10', '设计师最大并发工单数', '超出此数量不参与自动分配', 0);
```

- [ ] **Step 5: Commit**

```bash
git add sql/
git commit -m "feat(db): DDL 更新（specialty 扩展/designer_code 移除/specialty 新增）和配置种子数据"
```

---

## Task 6：order — pom 补依赖 + 分配服务 + 接口

### Part A：pom 补依赖

**Files:**
- Modify: `yigongbao-module-order/pom.xml`

- [ ] **Step 1: 在 order/pom.xml 新增对 system 和 basic 的显式依赖**

在现有 `yigongbao-module-flow` 依赖之后追加：

```xml
<!-- system：用户、设计师信息查询 -->
<dependency>
    <groupId>com.yigongbao</groupId>
    <artifactId>yigongbao-module-system</artifactId>
</dependency>

<!-- basic：重建项目专业方向查询 -->
<dependency>
    <groupId>com.yigongbao</groupId>
    <artifactId>yigongbao-module-basic</artifactId>
</dependency>
```

- [ ] **Step 2: 编译验证依赖正确**

```bash
cd yigongbao-parent && mvn compile -pl yigongbao-module-order -am -q
```

Expected: BUILD SUCCESS

### Part B：新增 VO / DTO

**Files:**
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/DesignerVO.java`
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/order/AssignDesignerDTO.java`
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/order/DesignerQueryDTO.java`

- [ ] **Step 3: 创建 DesignerVO**

```java
package com.yigongbao.module.order.vo.order;

import lombok.Data;
import java.util.List;

/**
 * 可分配设计师 VO
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Data
public class DesignerVO {
    /** 设计师用户ID */
    private Long userId;
    /** 姓名 */
    private String realName;
    /** 专业方向字典编码列表 */
    private List<String> specialtyList;
    /** 专业方向名称列表 */
    private List<String> specialtyNameList;
    /** 当前在手工单数 */
    private Integer currentLoad;
    /** 最大并发工单数（来自系统配置） */
    private Integer maxCapacity;
}
```

- [ ] **Step 4: 创建 AssignDesignerDTO**

```java
package com.yigongbao.module.order.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手动分配设计师 DTO
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Data
public class AssignDesignerDTO {
    @NotNull(message = "设计师ID不能为空")
    private Long designerId;
}
```

- [ ] **Step 5: 创建 DesignerQueryDTO**

```java
package com.yigongbao.module.order.dto.order;

import lombok.Data;
import java.util.List;

/**
 * 查询可分配设计师 DTO
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Data
public class DesignerQueryDTO {
    /** 订单涉及的专业方向字典编码列表，如 ["7.1"] */
    private List<String> specialties;
}
```

### Part C：Service 接口

**Files:**
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/DesignerAssignmentService.java`

- [ ] **Step 6: 创建 DesignerAssignmentService 接口**

```java
package com.yigongbao.module.order.service;

import com.yigongbao.module.order.dto.order.DesignerQueryDTO;
import com.yigongbao.module.order.vo.order.DesignerVO;

import java.util.List;

/**
 * 设计师分配 Service
 *
 * @author hanjor
 * @date 2026-04-10
 */
public interface DesignerAssignmentService {

    /**
     * 审核通过后触发分配（根据系统配置决定自动或跳过；分配失败不影响审核结果）
     *
     * @param orderId 订单ID
     */
    void triggerAssignmentAfterAudit(Long orderId);

    /**
     * 自动分配设计师（专业方向匹配 + 负载均衡）
     *
     * @param orderId 订单ID
     * @return 分配到的设计师用户ID，无可分配时返回 null
     */
    Long autoAssignDesigner(Long orderId);

    /**
     * 手动分配设计师（仅管理员；订单必须处于 PENDING_DESIGN 状态）
     *
     * @param orderId    订单ID
     * @param designerId 设计师用户ID
     */
    void manualAssignDesigner(Long orderId, Long designerId);

    /**
     * 设计师开始设计（仅分配给本人的订单；订单必须处于 PENDING_DESIGN 状态）
     *
     * @param orderId 订单ID
     */
    void startDesign(Long orderId);

    /**
     * 查询可分配设计师列表（按专业方向过滤 + 负载排序）
     *
     * @param dto 查询条件
     * @return 匹配的设计师列表
     */
    List<DesignerVO> listAvailableDesigners(DesignerQueryDTO dto);
}
```

### Part D：Service 实现

**Files:**
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/DesignerAssignmentServiceImpl.java`

- [ ] **Step 7: 创建 DesignerAssignmentServiceImpl**

```java
package com.yigongbao.module.order.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.order.dto.order.DesignerQueryDTO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderMainEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.service.DesignerAssignmentService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.DesignerVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 设计师分配 Service 实现
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Slf4j
@Service
public class DesignerAssignmentServiceImpl implements DesignerAssignmentService {

    private static final List<String> DESIGNER_ROLES = List.of("designer", "designer-manager");
    /** 专业方向二级编码严格白名单正则：仅允许 \d+\.\d+ 格式，如 7.1 */
    private static final java.util.regex.Pattern SPECIALTY_PATTERN =
            java.util.regex.Pattern.compile("^\\d+\\.\\d+$");

    private final OrderMainService orderMainService;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ConfigService configService;
    private final DictService dictService;
    private final RebuildProjectService rebuildProjectService;
    private final FlowFacade flowFacade;

    /**
     * 手写构造函数，对 OrderMainService 使用 @Lazy 打破循环依赖
     * （OrderMainServiceImpl 注入了 DesignerAssignmentService，
     *  DesignerAssignmentServiceImpl 反向注入 OrderMainService）
     */
    public DesignerAssignmentServiceImpl(
            @org.springframework.context.annotation.Lazy OrderMainService orderMainService,
            OrderItemMapper orderItemMapper,
            UserMapper userMapper,
            ConfigService configService,
            DictService dictService,
            RebuildProjectService rebuildProjectService,
            FlowFacade flowFacade) {
        this.orderMainService = orderMainService;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.configService = configService;
        this.dictService = dictService;
        this.rebuildProjectService = rebuildProjectService;
        this.flowFacade = flowFacade;
    }

    @Override
    public void triggerAssignmentAfterAudit(Long orderId) {
        log.info("触发设计师分配，orderId={}", orderId);
        String mode = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_ASSIGN_MODE.getKey());
        if (!"auto".equals(mode)) {
            log.info("当前为手动分配模式，跳过自动分配，orderId={}", orderId);
            return;
        }
        try {
            Long designerId = autoAssignDesigner(orderId);
            if (designerId == null) {
                log.warn("自动分配未找到合适设计师，订单进入待分配状态，orderId={}", orderId);
            } else {
                log.info("自动分配成功，orderId={}, designerId={}", orderId, designerId);
            }
        } catch (Exception e) {
            // 分配失败不影响审核结果，仅记录日志，管理员后续手动分配
            log.warn("自动分配异常，订单保持 PENDING_DESIGN 状态，orderId={}, error={}", orderId, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoAssignDesigner(Long orderId) {
        log.info("自动分配设计师，orderId={}", orderId);
        // 1. 获取订单专业方向
        String specialty = getOrderSpecialty(orderId);
        if (StrUtil.isBlank(specialty)) {
            log.warn("订单无可用专业方向，跳过自动分配，orderId={}", orderId);
            return null;
        }
        // 2. 获取容量上限配置
        int maxCapacity = getMaxCapacity();
        // 3. 查询候选设计师（FIND_IN_SET 匹配，按工单数 ASC）
        List<UserEntity> candidates = userMapper.selectAvailableDesigners(specialty, maxCapacity);
        if (CollUtil.isEmpty(candidates)) {
            log.warn("无满足条件的设计师，specialty={}, maxCapacity={}", specialty, maxCapacity);
            return null;
        }
        // 4. 取负载最低的第一位
        UserEntity designer = candidates.get(0);
        // 5. 更新订单 designerId / designerName
        updateOrderDesigner(orderId, designer);
        return designer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAssignDesigner(Long orderId, Long designerId) {
        log.info("手动分配设计师，orderId={}, designerId={}", orderId, designerId);
        // 1. 校验订单存在且状态为 PENDING_DESIGN
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.PENDING_DESIGN.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许分配，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }
        // 2. 校验设计师存在、角色合法、状态正常
        UserEntity designer = userMapper.selectById(designerId);
        if (designer == null || designer.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCodeEnum.DESIGNER_NOT_FOUND);
        }
        if (!DESIGNER_ROLES.contains(designer.getRoleCode())) {
            throw new BusinessException(ErrorCodeEnum.DESIGNER_ROLE_INVALID);
        }
        if (designer.getStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.DESIGNER_DISABLED);
        }
        // 3. 校验设计师 specialty 包含订单项目方向
        String orderSpecialty = getOrderSpecialty(orderId);
        if (StrUtil.isNotBlank(orderSpecialty) && !isSpecialtyMatch(designer.getSpecialty(), orderSpecialty)) {
            log.warn("设计师专业方向不匹配，designerId={}, designerSpecialty={}, orderSpecialty={}",
                    designerId, designer.getSpecialty(), orderSpecialty);
            throw new BusinessException(ErrorCodeEnum.DESIGNER_SPECIALTY_MISMATCH);
        }
        // 4. 更新订单
        updateOrderDesigner(orderId, designer);
        log.info("手动分配成功，orderId={}, designerId={}", orderId, designerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startDesign(Long orderId) {
        log.info("设计师开始设计，orderId={}", orderId);
        Long currentUserId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.PENDING_DESIGN.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许开始设计，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }
        if (!currentUserId.equals(order.getDesignerId())) {
            log.warn("非分配设计师，无权开始设计，orderId={}, designerId={}, currentUserId={}",
                    orderId, order.getDesignerId(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_DESIGNER_MISMATCH);
        }
        // 通过 FlowFacade 执行 START_DESIGN，状态 PENDING_DESIGN → DESIGN_IN_PROGRESS
        flowFacade.executeFlow(orderId, FlowActionEnum.START_DESIGN,
                FlowOperator.of(currentUserId, null));
        log.info("开始设计成功，orderId={}", orderId);
    }

    @Override
    public List<DesignerVO> listAvailableDesigners(DesignerQueryDTO dto) {
        log.info("查询可分配设计师，specialties={}", dto.getSpecialties());
        int maxCapacity = getMaxCapacity();
        List<String> specialties = dto.getSpecialties();
        if (CollUtil.isEmpty(specialties)) {
            return List.of();
        }
        // 严格白名单校验：只允许 \d+\.\d+ 格式，长度 ≤ 16，防止 SQL 注入
        String condition = specialties.stream()
                .filter(s -> StrUtil.isNotBlank(s)
                        && s.length() <= 16
                        && SPECIALTY_PATTERN.matcher(s).matches())
                .map(s -> String.format("FIND_IN_SET('%s', specialty) > 0", s))
                .collect(Collectors.joining(" OR "));
        if (StrUtil.isBlank(condition)) {
            log.warn("所有专业方向编码均未通过白名单校验，返回空列表");
            return List.of();
        }
        List<UserEntity> users = userMapper.selectDesignersBySpecialties(condition, maxCapacity);
        return users.stream().map(u -> toDesignerVO(u, maxCapacity)).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 获取订单的专业方向（通过 order_item.projectId → rebuild_project.specialty）
     */
    private String getOrderSpecialty(Long orderId) {
        List<OrderItemEntity> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .eq(OrderItemEntity::getOrderId, orderId)
                        .eq(OrderItemEntity::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (CollUtil.isEmpty(items)) {
            return null;
        }
        Long projectId = items.get(0).getProjectId();
        if (projectId == null) {
            return null;
        }
        return rebuildProjectService.getSpecialtyByProjectId(projectId);
    }

    /**
     * 检查设计师 specialty（逗号拼接多值）是否包含指定 orderSpecialty
     */
    private boolean isSpecialtyMatch(String designerSpecialty, String orderSpecialty) {
        if (StrUtil.isBlank(designerSpecialty)) {
            return false;
        }
        return StrUtil.splitToList(designerSpecialty, ',').contains(orderSpecialty);
    }

    /**
     * 更新订单的设计师冗余字段
     */
    private void updateOrderDesigner(Long orderId, UserEntity designer) {
        OrderMainEntity order = orderMainService.getById(orderId);
        order.setDesignerId(designer.getId());
        order.setDesignerName(designer.getRealName());
        orderMainService.updateById(order);
    }

    private int getMaxCapacity() {
        String val = configService.getConfigValue(
                SystemConfigKeyEnum.DESIGN_ASSIGN_MAX_CAPACITY.getKey());
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("设计师最大容量配置无效，使用默认值 10，configVal={}", val);
            return 10;
        }
    }

    private DesignerVO toDesignerVO(UserEntity user, int maxCapacity) {
        DesignerVO vo = new DesignerVO();
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setCurrentLoad(user.getCurrentLoad() != null ? user.getCurrentLoad() : 0);
        vo.setMaxCapacity(maxCapacity);
        if (StrUtil.isNotBlank(user.getSpecialty())) {
            List<String> specList = StrUtil.splitToList(user.getSpecialty(), ',');
            vo.setSpecialtyList(specList);
            List<String> nameList = specList.stream()
                    .map(code -> {
                        var dict = dictService.getByDictCode(code);
                        return dict != null ? dict.getDictName() : code;
                    })
                    .collect(Collectors.toList());
            vo.setSpecialtyNameList(nameList);
        }
        return vo;
    }
}
```

### Part E：Controller 扩展

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderController.java`

- [ ] **Step 8: 在 OrderController 注入 DesignerAssignmentService 并新增 3 个接口**

在 Controller 的注入字段中追加：
```java
private final DesignerAssignmentService designerAssignmentService;
```

新增接口方法：
```java
/**
 * 查询可分配设计师列表（管理员）
 */
@PostMapping("/designers/available")
@OperationLog("查询可分配设计师")
public Result<List<DesignerVO>> listAvailableDesigners(@RequestBody DesignerQueryDTO dto) {
    return Result.success(designerAssignmentService.listAvailableDesigners(dto));
}

/**
 * 手动分配设计师（管理员）
 */
@PostMapping("/{id}/assign-designer")
@OperationLog("手动分配设计师")
public Result<Void> assignDesigner(@PathVariable Long id,
                                   @RequestBody @Validated AssignDesignerDTO dto) {
    designerAssignmentService.manualAssignDesigner(id, dto.getDesignerId());
    return Result.success();
}

/**
 * 设计师开始设计
 */
@PostMapping("/{id}/start-design")
@OperationLog("开始设计")
public Result<Void> startDesign(@PathVariable Long id) {
    designerAssignmentService.startDesign(id);
    return Result.success();
}
```

### Part F：集成 auditPass

**Files:**
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderMainServiceImpl.java`

- [ ] **Step 9: OrderMainServiceImpl 注入 DesignerAssignmentService（@Lazy 解循环依赖）**

在 OrderMainServiceImpl 类中移除 `@RequiredArgsConstructor`（如果使用的话），改为手写构造函数，或在字段上用 `@Autowired @Lazy`：

```java
@Lazy
@Autowired
private DesignerAssignmentService designerAssignmentService;
```

> **说明：** `@RequiredArgsConstructor` 生成的构造函数不支持 `@Lazy`，对 `designerAssignmentService` 单独使用字段注入 + `@Lazy` 是最简洁的方式，其余字段保持构造函数注入不变。

- [ ] **Step 10: 在 auditPass 方法中集成分配触发**

在 `updateById(entity);` 之后，`log.info("审核通过成功...")` 之前插入：

```java
// 触发设计师分配（catch 异常保证分配失败不回滚审核结果）
designerAssignmentService.triggerAssignmentAfterAudit(id);
```

- [ ] **Step 11: 编译验证**

```bash
cd yigongbao-parent && mvn compile -pl yigongbao-module-order -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add yigongbao-module-order/ yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/mapper/UserMapper.java
git commit -m "feat(order): 新增设计师分配服务和接口，auditPass 集成分配触发"
```

---

## Task 7：单元测试

**Files:**
- Create: `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/DesignerAssignmentServiceImplTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.yigongbao.module.order.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.order.dto.order.DesignerQueryDTO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderMainEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.DesignerVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignerAssignmentServiceImplTest {

    @Mock private OrderMainService orderMainService;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private UserMapper userMapper;
    @Mock private ConfigService configService;
    @Mock private DictService dictService;
    @Mock private RebuildProjectService rebuildProjectService;
    @Mock private FlowFacade flowFacade;

    @InjectMocks private DesignerAssignmentServiceImpl service;

    // ==================== triggerAssignmentAfterAudit ====================

    @Test
    @DisplayName("trigger — 手动模式，不调用 autoAssign")
    void trigger_manualMode_shouldSkipAutoAssign() {
        when(configService.getConfigValue("design.assign.mode")).thenReturn("manual");
        service.triggerAssignmentAfterAudit(1L);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("trigger — 自动模式，无明细时不抛异常")
    void trigger_autoMode_noItems_shouldNotThrow() {
        when(configService.getConfigValue("design.assign.mode")).thenReturn("auto");
        when(configService.getConfigValue("design.assign.max.capacity")).thenReturn("10");
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        assertDoesNotThrow(() -> service.triggerAssignmentAfterAudit(1L));
    }

    // ==================== autoAssignDesigner ====================

    @Test
    @DisplayName("autoAssign — 找到候选设计师，更新订单 designerId")
    void autoAssign_withCandidate_shouldUpdateOrder() {
        // given
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1");
        when(configService.getConfigValue("design.assign.max.capacity")).thenReturn("10");
        UserEntity designer = new UserEntity();
        designer.setId(100L);
        designer.setRealName("张三");
        when(userMapper.selectAvailableDesigners("7.1", 10)).thenReturn(List.of(designer));
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        when(orderMainService.getById(1L)).thenReturn(order);
        // when
        Long result = service.autoAssignDesigner(1L);
        // then
        assertEquals(100L, result);
        verify(orderMainService).updateById(argThat(o -> Long.valueOf(100L).equals(o.getDesignerId())));
    }

    @Test
    @DisplayName("autoAssign — 无候选设计师，返回 null")
    void autoAssign_noCandidate_shouldReturnNull() {
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1");
        when(configService.getConfigValue("design.assign.max.capacity")).thenReturn("10");
        when(userMapper.selectAvailableDesigners("7.1", 10)).thenReturn(List.of());
        assertNull(service.autoAssignDesigner(1L));
    }

    @Test
    @DisplayName("autoAssign — 订单无明细，返回 null")
    void autoAssign_noOrderItems_shouldReturnNull() {
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        assertNull(service.autoAssignDesigner(1L));
    }

    // ==================== manualAssignDesigner ====================

    @Test
    @DisplayName("manualAssign — 正常流程，更新订单设计师")
    void manualAssign_success() {
        OrderMainEntity order = buildPendingDesignOrder(1L);
        when(orderMainService.getById(1L)).thenReturn(order);
        UserEntity designer = buildDesigner(100L, "7.1");
        when(userMapper.selectById(100L)).thenReturn(designer);
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1");
        service.manualAssignDesigner(1L, 100L);
        verify(orderMainService).updateById(argThat(o -> Long.valueOf(100L).equals(o.getDesignerId())));
    }

    @Test
    @DisplayName("manualAssign — 订单不存在，抛 ORDER_NOT_FOUND")
    void manualAssign_orderNotFound_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 订单状态非 PENDING_DESIGN，抛 ORDER_STATUS_ERROR")
    void manualAssign_wrongStatus_shouldThrow() {
        OrderMainEntity order = new OrderMainEntity();
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue()); // 22
        when(orderMainService.getById(1L)).thenReturn(order);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.ORDER_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 设计师不存在，抛 DESIGNER_NOT_FOUND")
    void manualAssign_designerNotFound_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        when(userMapper.selectById(100L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.DESIGNER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 设计师角色不合法，抛 DESIGNER_ROLE_INVALID")
    void manualAssign_wrongRole_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setRoleCode("sales");
        user.setStatus(1);
        user.setIsDeleted(0);
        when(userMapper.selectById(100L)).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.DESIGNER_ROLE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 设计师已禁用，抛 DESIGNER_DISABLED")
    void manualAssign_designerDisabled_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        UserEntity user = new UserEntity();
        user.setId(100L);
        user.setRoleCode("designer");
        user.setStatus(0); // 禁用
        user.setIsDeleted(0);
        when(userMapper.selectById(100L)).thenReturn(user);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.DESIGNER_DISABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("manualAssign — 专业方向不匹配，抛 DESIGNER_SPECIALTY_MISMATCH")
    void manualAssign_specialtyMismatch_shouldThrow() {
        when(orderMainService.getById(1L)).thenReturn(buildPendingDesignOrder(1L));
        UserEntity designer = buildDesigner(100L, "7.2"); // 设计师是 7.2
        when(userMapper.selectById(100L)).thenReturn(designer);
        OrderItemEntity item = new OrderItemEntity();
        item.setProjectId(10L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(rebuildProjectService.getSpecialtyByProjectId(10L)).thenReturn("7.1"); // 订单是 7.1
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualAssignDesigner(1L, 100L));
        assertEquals(ErrorCodeEnum.DESIGNER_SPECIALTY_MISMATCH.getCode(), ex.getCode());
    }

    // ==================== startDesign ====================

    @Test
    @DisplayName("startDesign — 订单状态非 PENDING_DESIGN，抛 ORDER_STATUS_ERROR")
    void startDesign_wrongStatus_shouldThrow() {
        OrderMainEntity order = new OrderMainEntity();
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(1L);
        when(orderMainService.getById(1L)).thenReturn(order);
        // mock SaToken（static）需要 mockStatic，此处仅验证逻辑分支
        // 因 SaToken 是静态调用，建议将 getCurrentUserId 抽为 protected 方法方便覆盖
        assertThrows(BusinessException.class, () -> service.startDesign(1L));
    }

    // ==================== listAvailableDesigners ====================

    @Test
    @DisplayName("listAvailableDesigners — 非法 specialty 编码被过滤，返回空列表")
    void listDesigners_invalidSpecialty_shouldReturnEmpty() {
        DesignerQueryDTO dto = new DesignerQueryDTO();
        dto.setSpecialties(List.of("invalid", "7.", "../hack"));
        List<DesignerVO> result = service.listAvailableDesigners(dto);
        assertTrue(result.isEmpty());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("listAvailableDesigners — 合法 specialty，返回设计师列表")
    void listDesigners_validSpecialty_shouldReturnList() {
        DesignerQueryDTO dto = new DesignerQueryDTO();
        dto.setSpecialties(List.of("7.1"));
        when(configService.getConfigValue("design.assign.max.capacity")).thenReturn("10");
        UserEntity designer = buildDesigner(100L, "7.1");
        designer.setCurrentLoad(3);
        when(userMapper.selectDesignersBySpecialties(anyString(), eq(10))).thenReturn(List.of(designer));
        List<DesignerVO> result = service.listAvailableDesigners(dto);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getUserId());
        assertEquals(3, result.get(0).getCurrentLoad());
    }

    // ==================== 辅助方法 ====================

    private OrderMainEntity buildPendingDesignOrder(Long id) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(id);
        order.setStatus(FlowStatusEnum.PENDING_DESIGN.getValue());
        return order;
    }

    private UserEntity buildDesigner(Long id, String specialty) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRealName("设计师" + id);
        user.setRoleCode("designer");
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setSpecialty(specialty);
        return user;
    }
}
```

- [ ] **Step 2: 运行新测试，验证通过**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-order -Dtest=DesignerAssignmentServiceImplTest
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 运行全量测试**

```bash
cd yigongbao-parent && mvn test
```

Expected: BUILD SUCCESS，所有模块测试通过

- [ ] **Step 4: Commit**

```bash
git add yigongbao-module-order/src/test/
git commit -m "test(order): 新增 DesignerAssignmentServiceImplTest 单元测试"
```

---

## Task 8：静态代码审查

- [ ] **Step 1: 运行全量编译和测试**

```bash
cd yigongbao-parent && mvn clean test
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 逐项自查**

| 检查项 | 关注位置 | 标准 |
|-------|---------|------|
| 所有新增 Service 方法有 Javadoc + 行内注释 | `DesignerAssignmentServiceImpl` 全部方法 | 无缺失 |
| 关键节点有 `log.info/warn` | `triggerAssignmentAfterAudit`、`autoAssignDesigner`、`manualAssignDesigner`、`startDesign` | 入参、分支、结果均有日志 |
| 异常全部使用 `ErrorCodeEnum` | 所有 `throw new BusinessException(...)` | 无裸字符串 |
| `@Transactional` 在写操作上 | `autoAssignDesigner`、`manualAssignDesigner`、`startDesign` | 已加 |
| `triggerAssignmentAfterAudit` 内 catch 异常不上抛 | `try-catch` 块 | 只 log.warn，不 rethrow |
| SQL 拼接白名单正则通过测试 | `SPECIALTY_PATTERN` + `listAvailableDesigners` | 非法值被过滤 |
| `@TableField(exist=false)` 在 `UserEntity.currentLoad` 上 | `UserEntity` | 已加 |
| `@Lazy` 在 `DesignerAssignmentServiceImpl` 构造函数参数上 | 构造函数 | 已加 |
| `OrderMainServiceImpl.designerAssignmentService` 字段使用 `@Autowired @Lazy` | `OrderMainServiceImpl` | 已加 |
| 两个 schema.sql 的 INSERT 列数与建表列数一致 | basic/boot 两个 schema.sql | 已对齐 |
| `FlowStatusEnum.DESIGN_REVIEW_PASSED` 仍在 `isInvisibleStatus` 中 | `FlowPhaseTransitionRules` | 枚举常量引用，值已变 25 但代码无需改 |

- [ ] **Step 3: 修复所有发现的问题**

对 Step 2 中每个不符合项逐一修复，修复后再次运行 `mvn clean test` 确认通过。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: 静态代码审查修复（注释/日志/事务/安全校验）"
```

---

## Task 9：文档更新

- [ ] **Step 1: 更新 04_设计师分配设计实现方案.md**

在版本记录表中追加：
```
| 1.3 | 2026-04-10 | 实现确认：specialty 统一二级字典（7.x）；FlowActionEnum.START_DESIGN 已存在无需新增；错误码从 723 起；状态文档状态改为"已实现" | hanjor |
```

将文档顶部 `**状态**：规划中` 改为 `**状态**：已实现`。

- [ ] **Step 2: 更新 05_用户管理功能实现方案.md**

补充 specialty 多选改造说明：
- `CreateUserDTO.specialtyList: List<String>`（原 `specialty: String`）
- `UserVO` 新增 `specialtyList`、`specialtyNameList`
- `validateSpecialty` 改为遍历多值校验

- [ ] **Step 3: 更新 04_部位和重建项目功能实现方案.md**

- 补充 `rebuild_body_part.designer_code` 字段已移除说明
- 补充 `rebuild_project.specialty` 新增字段说明
- 补充 `RebuildProjectService.getSpecialtyByProjectId` 新接口方法说明

- [ ] **Step 4: 更新 02_订单模块功能实现方案.md**

在 `auditPass` 相关章节补充：
- 审核通过后调用 `triggerAssignmentAfterAudit`，根据系统配置决定自动/手动
- 分配失败不回滚审核结果

- [ ] **Step 5: 更新接口文档**

- `02_用户管理接口文档.md`：更新 specialty 字段类型为数组，响应新增 `specialtyList`/`specialtyNameList`
- `15_重建项目与部位管理.md`：body_part 移除 designerCode；rebuild_project 新增 specialty
- `19_订单模块接口文档.md`：新增三个接口（`/designers/available`、`/{id}/assign-designer`、`/{id}/start-design`）

- [ ] **Step 6: Commit**

```bash
git add .docs/
git commit -m "docs: 同步更新设计师分配相关技术文档和接口文档"
```

---

## 执行顺序总结

```
Task 1 common
  └─→ Task 2 flow
        └─→ Task 3 system
              └─→ Task 4 basic
                    └─→ Task 5 DB
                          └─→ Task 6 order（含 pom 补依赖）
                                └─→ Task 7 测试
                                      └─→ Task 8 代码审查
                                            └─→ Task 9 文档
```

每个 Task 结束均有独立 `mvn test` 验证 + git commit，确保每步可独立回滚。
