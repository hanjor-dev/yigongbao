# 设计师分配功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现订单设计师分配功能，包括 specialty 多选改造、rebuild_body_part 清理、Flow 状态机扩展、分配服务新增，以及全套单元测试、静态代码审查和文档更新。

**Architecture:** 按模块依赖顺序分阶段实施：common（错误码/配置枚举）→ flow（状态机）→ system（用户 specialty 多选）→ basic（重建项目/部位）→ order（分配服务/接口）。每个阶段完成后独立可测，最终 auditPass 触发分配逻辑串联全链路。

**Tech Stack:** Java 21, Spring Boot, MyBatis Plus, Hutool, JUnit 5 + Mockito, H2（测试）

---

## 影响域速查

| 模块 | 文件 | 改动类型 |
|------|------|---------|
| common | `ErrorCodeEnum` | 新增错误码 |
| common | `SystemConfigKeyEnum` | 新增配置项 |
| flow | `FlowStatusEnum` | 新增 PENDING_DESIGN(21)，顺延 DESIGNING→22 等 |
| flow | `FlowPhaseTransitionRules` | DATA_AUDIT_PASSED 初始状态改为 PENDING_DESIGN |
| flow | `FlowStatusTransitionRules` | 新增 PENDING_DESIGN 转换规则，START_DESIGN 目标改为 DESIGN_IN_PROGRESS |
| flow | `FlowStatusTransitionRulesTest` | 更新所有涉及 DESIGNING/21 的测试断言 |
| flow | `FlowPhaseTransitionRulesTest` | 更新断言 DESIGNING → PENDING_DESIGN |
| system | `CreateUserDTO` / `UpdateUserDTO` | specialty: String → List\<String\>，@Size 调整 |
| system | `UserVO` | 新增 specialtyList、specialtyNameList |
| system | `UserServiceImpl` | validateSpecialty 改为遍历多值；listVo/getUserById 填充多值名称 |
| system | `UserServiceImplTest` | 更新 specialty 相关测试 |
| system | `schema.sql`（system 模块） | specialty VARCHAR(64→255) |
| basic | `RebuildProjectEntity/DTO/VO` | 新增 specialty 字段 |
| basic | `RebuildProjectServiceImpl` | create/update 读写 specialty |
| basic | `BodyPartEntity/DTO/VO` | 移除 designerCode 字段 |
| basic | `BodyPartServiceImpl` | 移除 designerCode 读写 |
| basic | `BodyPartControllerTest` | 移除 designerCode 测试数据 |
| basic | `schema.sql`（basic/boot 模块） | 同步 DDL |
| order | `DesignerAssignmentService` + `Impl` | 新增 |
| order | `DesignerVO` / `AssignDesignerDTO` / `DesignerQueryDTO` | 新增 |
| order | `OrderController` | 新增三个接口 |
| order | `OrderMainServiceImpl.auditPass` | 集成分配触发 |
| order | `UserMapper`（order 模块引用） | 新增 FIND_IN_SET 查询方法 |
| DB | `sql/ddl.sql` | sys_user、rebuild_project、rebuild_body_part |
| DB | `sql/init.sql` | sys_config 种子数据 |
| 文档 | 多个 .md 文件 | 同步更新 |

---

## Task 1：common 模块 — 新增错误码和系统配置枚举

**Files:**
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java`

- [ ] **Step 1: 在 ErrorCodeEnum 新增设计师相关错误码**

找到 `USER_SPECIALTY_INVALID(634,...)` 之后，追加：

```java
DESIGNER_NOT_FOUND(680, "设计师不存在"),
DESIGNER_ROLE_INVALID(681, "用户角色不是设计师或设计师管理员"),
DESIGNER_DISABLED(682, "设计师已被禁用"),
DESIGNER_SPECIALTY_MISMATCH(683, "设计师专业方向与订单项目专业方向不一致"),
```

- [ ] **Step 2: 在 SystemConfigKeyEnum 新增分配相关配置项**

追加：

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
git commit -m "feat(common): 新增设计师分配相关错误码和系统配置枚举"
```

---

## Task 2：flow 模块 — 状态机扩展

**关键说明：**
- 新增 `PENDING_DESIGN(21)`（待设计/待分配），原 `DESIGNING(21)` 改为 `DESIGN_IN_PROGRESS(22)`
- 其余设计阶段状态码顺延：`DESIGN_COMPLETED(22→23)`、`DESIGN_REVIEWING(23→24)`、`DESIGN_REVIEW_PASSED(24→25)`、`DESIGN_REVIEW_REJECTED(25→26)`
- `FlowActionEnum.START_DESIGN` 已存在，无需新增
- `decideNextPhaseAndStatus` 中 DATA_AUDIT_PASSED 的初始状态从 `DESIGNING` 改为 `PENDING_DESIGN`

**Files:**
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowStatusEnum.java`
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/rules/FlowPhaseTransitionRules.java`
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/rules/FlowStatusTransitionRules.java`
- Modify: `yigongbao-module-flow/src/test/java/com/yigongbao/flow/rules/FlowStatusTransitionRulesTest.java`
- Modify: `yigongbao-module-flow/src/test/java/com/yigongbao/flow/rules/FlowPhaseTransitionRulesTest.java`

- [ ] **Step 1: 修改 FlowStatusEnum — 调整设计阶段状态码**

将设计阶段（20-29）整段替换为：

```java
// ==================== 设计阶段（20-29）====================
/**
 * 待设计（审核通过后进入，已分配设计师或待分配）
 */
PENDING_DESIGN(21, "待设计"),

/**
 * 设计中
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
 * 设计审核通过（不可见状态，系统自动推进）
 */
DESIGN_REVIEW_PASSED(25, "设计审核通过"),

/**
 * 设计审核不通过
 */
DESIGN_REVIEW_REJECTED(26, "设计审核不通过"),
```

- [ ] **Step 2: 修改 FlowPhaseTransitionRules — DATA_AUDIT_PASSED 初始状态改为 PENDING_DESIGN**

在 `decideNextPhaseAndStatus` 方法中（约第156行），将：
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

- [ ] **Step 3: 修改 FlowStatusTransitionRules — 更新设计阶段转换规则**

**3a. 更新 STATUS_TRANSITIONS 静态初始化块**（设计阶段部分，约第67-78行）：

```java
// ==================== 设计阶段状态转换（20-29）====================
// PENDING_DESIGN → DESIGN_IN_PROGRESS（设计师开始设计）
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.PENDING_DESIGN),
        Set.of(FlowStatusEnum.DESIGN_IN_PROGRESS));

// DESIGN_IN_PROGRESS → DESIGN_COMPLETED（提交设计）
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_IN_PROGRESS),
        Set.of(FlowStatusEnum.DESIGN_COMPLETED));

transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_COMPLETED),
        Set.of(FlowStatusEnum.DESIGN_REVIEWING));

transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_REVIEWING),
        Set.of(FlowStatusEnum.DESIGN_REVIEW_PASSED, FlowStatusEnum.DESIGN_REVIEW_REJECTED));

// 审核驳回后重新开始设计
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_REVIEW_REJECTED),
        Set.of(FlowStatusEnum.DESIGN_IN_PROGRESS));
```

**3b. 更新 `getAvailableActions` 方法**（DESIGN 分支，约第150-157行）：

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

**3c. 更新 `getTargetStatus` 方法**（设计阶段动作部分，约第247行）：

```java
// 设计阶段动作
case START_DESIGN -> FlowStatusEnum.DESIGN_IN_PROGRESS.getValue();
case SUBMIT_DESIGN -> FlowStatusEnum.DESIGN_REVIEWING.getValue();
case DESIGN_REVIEW_PASS -> FlowStatusEnum.DESIGN_REVIEW_PASSED.getValue(); // 不可见状态
case DESIGN_REVIEW_REJECT -> FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue();
```

**3d. 更新 `getValidStatusesForPhase` 方法**（DESIGN 分支，约第364行）：

```java
case DESIGN -> Set.of(FlowStatusEnum.PENDING_DESIGN, FlowStatusEnum.DESIGN_IN_PROGRESS,
        FlowStatusEnum.DESIGN_COMPLETED, FlowStatusEnum.DESIGN_REVIEWING,
        FlowStatusEnum.DESIGN_REVIEW_REJECTED);
```

- [ ] **Step 4: 先运行测试，确认测试失败（预期）**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-flow -q 2>&1 | tail -20
```

Expected: FAIL — 测试引用了旧的 `DESIGNING` 枚举，编译报错或断言失败

- [ ] **Step 5: 更新 FlowPhaseTransitionRulesTest**

找到 `dataAuditPassed_shouldAdvanceToDesign` 测试（约第256-265行），将断言改为：

```java
assertEquals(FlowStatusEnum.PENDING_DESIGN, result.initialStatus());
```

找到 `assertFalse(FlowPhaseTransitionRules.isInvisibleStatus(FlowStatusEnum.DESIGNING))` 所在测试，将 `DESIGNING` 改为 `DESIGN_IN_PROGRESS`。

- [ ] **Step 6: 更新 FlowStatusTransitionRulesTest**

批量替换所有出现的旧枚举和状态码：

| 旧值 | 新值 |
|------|------|
| `FlowStatusEnum.DESIGNING` | `FlowStatusEnum.DESIGN_IN_PROGRESS` |
| `getAvailableActions(21, 2, 1)` → `SUBMIT_DESIGN` | 改为 `getAvailableActions(21, 2, 1)` → `START_DESIGN`（PENDING_DESIGN 的可用动作）|
| `getAvailableActions(22, 2, 1)` → `SUBMIT_DESIGN` | 改为 `getAvailableActions(22, 2, 1)` → `SUBMIT_DESIGN`（DESIGN_IN_PROGRESS）|
| 旧 `25`（DESIGN_REVIEW_REJECTED）→ `START_DESIGN` | 改为 `getAvailableActions(26, 2, 1)` |
| DisplayName 中 `DESIGNING(21)` | 改为 `PENDING_DESIGN(21)` |
| DisplayName 中 `DESIGN_COMPLETED(22)` | 改为 `DESIGN_IN_PROGRESS(22)` |
| DisplayName 中 `DESIGN_REVIEWING(23)` | 改为 `DESIGN_COMPLETED(23)` |
| DisplayName 中 `DESIGN_REVIEW_REJECTED(25)` | 改为 `DESIGN_REVIEW_REJECTED(26)` |

同时新增 PENDING_DESIGN → START_DESIGN 的 getTargetStatus 测试：

```java
@Test
@DisplayName("START_DESIGN → DESIGN_IN_PROGRESS(22)")
void startDesign_shouldTarget_designInProgress() {
    Integer target = rules.getTargetStatus(21, FlowActionEnum.START_DESIGN);
    assertEquals(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(), target);
}
```

- [ ] **Step 7: 运行 flow 模块测试，验证全部通过**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-flow
```

Expected: BUILD SUCCESS，所有测试通过

- [ ] **Step 8: Commit**

```bash
git add yigongbao-module-flow/
git commit -m "feat(flow): 新增 PENDING_DESIGN(21) 状态，设计阶段状态码顺延，更新状态机规则和测试"
```

---

## Task 3：system 模块 — 用户 specialty 多选改造

**关键说明：**
- `sys_user.specialty` 存储逗号拼接多值（如 `"7.1,7.2"`），字段长度扩展为 VARCHAR(255)
- DTO 中 specialty 类型改为 `List<String>`，前端传数组，后端拼接存储
- VO 中新增 `specialtyList`（编码列表）和 `specialtyNameList`（名称列表），保留原 `specialty` 和 `specialtyName` 字段用于兼容
- `validateSpecialty` 改为遍历校验每项

**Files:**
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/CreateUserDTO.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/UpdateUserDTO.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/UserVO.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java`
- Modify: `yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserServiceImplTest.java`
- Modify: `yigongbao-module-system/src/test/resources/schema.sql`

- [ ] **Step 1: 修改 CreateUserDTO 和 UpdateUserDTO — specialty 改为 List\<String\>**

在 `CreateUserDTO.java` 中，将：
```java
@Size(max = 64, message = "专业方向长度不能超过64个字符")
private String specialty;
```
改为：
```java
/**
 * 专业方向字典编码列表（设计师/设计师管理员必填，如 ["7.1", "7.2"]）
 */
private List<String> specialtyList;
```
同步修改 `UpdateUserDTO.java`，移除旧 specialty 字段，添加同样的 `specialtyList`。

- [ ] **Step 2: 修改 UserVO — 新增多值字段**

在 `specialty` 和 `specialtyName` 字段之后追加：

```java
/**
 * 专业方向字典编码列表（多选展示）
 */
private List<String> specialtyList;

/**
 * 专业方向名称列表（多选展示）
 */
private List<String> specialtyNameList;
```

- [ ] **Step 3: 修改 UserServiceImpl — validateSpecialty 改为多值校验**

将现有 `validateSpecialty(RoleEntity role, String specialty)` 方法签名和实现替换为：

```java
/**
 * 校验设计师专业方向：当角色为 designer/designer-manager 时，至少选择一个专业方向且全部合法
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

- [ ] **Step 4: 修改 UserServiceImpl — create 方法中存储多值**

在 `create` 方法中（约第308行），将：
```java
validateSpecialty(roleEntity, dto.getSpecialty());
```
改为：
```java
validateSpecialty(roleEntity, dto.getSpecialtyList());
```

在 `UserConvert.toEntity(dto)` 转换之后，补充将 List 拼接为逗号字符串：
```java
UserEntity entity = UserConvert.toEntity(dto);
// specialty List → 逗号拼接存储
if (CollUtil.isNotEmpty(dto.getSpecialtyList())) {
    entity.setSpecialty(CollUtil.join(dto.getSpecialtyList(), ","));
}
```

- [ ] **Step 5: 修改 UserServiceImpl — update 方法中同步处理**

在 `update` 方法中（约第405-407行），将：
```java
validateSpecialty(effectiveRole, dto.getSpecialty());
```
改为：
```java
validateSpecialty(effectiveRole, dto.getSpecialtyList());
```

在实体字段赋值处补充：
```java
if (dto.getSpecialtyList() != null) {
    entity.setSpecialty(CollUtil.join(dto.getSpecialtyList(), ","));
}
```

- [ ] **Step 6: 修改 UserServiceImpl — toVOWithNames 中填充多值名称**

在 `toVOWithNames` 方法（约第731-735行），将现有的 specialtyName 单值填充逻辑替换为：

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

- [ ] **Step 7: 更新 schema.sql — specialty 字段长度扩展**

在 `yigongbao-module-system/src/test/resources/schema.sql` 中，将：
```sql
specialty           VARCHAR(64)     COMMENT '专业方向',
```
改为：
```sql
specialty           VARCHAR(255)    COMMENT '专业方向（多选逗号拼接，如 7.1,7.2）',
```

- [ ] **Step 8: 更新测试数据中设计师 specialty 为多值示例**

在 `schema.sql` 测试数据中，找到 designer1 用户的 INSERT 行，将 specialty 值从 `'7.1.2'` 改为 `'7.1'`（统一到二级）。

- [ ] **Step 9: 更新 UserServiceImplTest — specialty 相关测试**

找到 `create` 和 `update` 相关测试，将所有 `dto.setSpecialty("7.1")` 改为 `dto.setSpecialtyList(List.of("7.1"))`。

新增测试用例：

```java
@Test
@DisplayName("创建设计师用户 — 多个专业方向，成功")
void createUser_designerWithMultipleSpecialties_shouldSuccess() {
    // given
    CreateUserDTO dto = buildCreateDTO();
    dto.setRoleId(2L);
    dto.setSpecialtyList(List.of("7.1", "7.2"));
    RoleEntity designerRole = new RoleEntity();
    designerRole.setRoleCode("designer");
    // mock roleService, dictService 返回对应字典
    // when + then: 不抛出异常，verify save 被调用
}

@Test
@DisplayName("创建设计师用户 — 专业方向编码无效，抛出 USER_SPECIALTY_INVALID")
void createUser_invalidSpecialtyCode_shouldThrowException() {
    // given: specialtyList = ["invalid"]
    // then: assertThrows(BusinessException.class, ...) 且 errorCode == USER_SPECIALTY_INVALID
}

@Test
@DisplayName("创建设计师用户 — 未传专业方向，抛出 USER_ROLE_SPECIALTY_REQUIRED")
void createUser_designerWithoutSpecialty_shouldThrowException() {
    // given: specialtyList = null
    // then: assertThrows USER_ROLE_SPECIALTY_REQUIRED
}
```

- [ ] **Step 10: 运行 system 模块测试**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-system
```

Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add yigongbao-module-system/
git commit -m "feat(system): sys_user.specialty 改为多选逗号拼接，DTO/VO/Service 联动更新"
```

---

## Task 4：basic 模块 — 移除 designerCode + 新增 rebuild_project.specialty

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

- [ ] **Step 1: 移除 BodyPartEntity 中的 designerCode 字段**

删除：
```java
/**
 * 设计师编号（如A/B/C）
 */
private String designerCode;
```

- [ ] **Step 2: 移除 CreateBodyPartDTO / UpdateBodyPartDTO 中的 designerCode 字段**

同上，删除两个 DTO 中的 `designerCode` 字段声明。

- [ ] **Step 3: 移除 BodyPartVO / BodyPartDetailVO 中的 designerCode 字段**

删除两个 VO 中的 `designerCode` 字段声明。

- [ ] **Step 4: 移除 BodyPartServiceImpl 中的 designerCode 读写**

- 在 `updateBodyPart` 方法中删除：`entity.setDesignerCode(dto.getDesignerCode());`
- 在 `toDetailVO` 方法中删除：`vo.setDesignerCode(entity.getDesignerCode());`

- [ ] **Step 5: 更新 BodyPartControllerTest**

找到以下三处，全部删除或注释：
- `vo.setDesignerCode("A");`（出现两次）
- 请求体 JSON 中的 `"designerCode", "A"`

- [ ] **Step 6: 更新两个 schema.sql 中的 rebuild_body_part 表定义**

在 `yigongbao-module-basic/src/test/resources/schema.sql` 和 `yigongbao-boot/src/test/resources/schema.sql` 中，删除：
```sql
designer_code   VARCHAR(10)     DEFAULT NULL COMMENT '设计师编号（如A/B/C）',
```

同时删除 INSERT 测试数据中对应的 `designer_code` 列值。

### Part B：新增 rebuild_project.specialty

**Files:**
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/entity/RebuildProjectEntity.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/dto/CreateRebuildProjectDTO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/dto/UpdateRebuildProjectDTO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/vo/RebuildProjectVO.java`
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/service/impl/RebuildProjectServiceImpl.java`
- Modify: `yigongbao-module-basic/src/test/resources/schema.sql`
- Modify: `yigongbao-boot/src/test/resources/schema.sql`

- [ ] **Step 7: RebuildProjectEntity 新增 specialty 字段**

在 `remark` 字段之前插入：
```java
/**
 * 专业方向字典编码（单值，如 "7.1"，关联 sys_dict）
 */
private String specialty;
```

- [ ] **Step 8: CreateRebuildProjectDTO / UpdateRebuildProjectDTO 新增 specialty 字段**

在两个 DTO 中均新增：
```java
/**
 * 专业方向字典编码（单值，如 "7.1"）
 */
private String specialty;
```

- [ ] **Step 9: RebuildProjectVO 新增 specialty 和 specialtyName 字段**

```java
/**
 * 专业方向字典编码
 */
private String specialty;

/**
 * 专业方向名称（冗余，来自 sys_dict）
 */
private String specialtyName;
```

- [ ] **Step 10: RebuildProjectServiceImpl — create/update 读写 specialty**

在 `createProject` 方法中（entity.setCode 之后）新增：
```java
entity.setSpecialty(dto.getSpecialty());
```

在 `updateProject` 方法中（entity.setRemark 之后）新增：
```java
entity.setSpecialty(dto.getSpecialty());
```

在 `toVO` / `toDetailVO` 的转换处，如果有 specialtyName 需要翻译，注入 `DictService` 并填充：
```java
if (StrUtil.isNotBlank(entity.getSpecialty())) {
    vo.setSpecialty(entity.getSpecialty());
    var dict = dictService.getByDictCode(entity.getSpecialty());
    vo.setSpecialtyName(dict != null ? dict.getDictName() : null);
}
```

- [ ] **Step 11: 更新两个 schema.sql — rebuild_project 新增 specialty 列**

在 rebuild_project 表定义的 `remark` 列之前加上：
```sql
specialty           VARCHAR(64)     DEFAULT NULL COMMENT '专业方向字典编码（单值，如 7.1）',
```

同时更新 INSERT 测试数据，为各项目补充 `specialty` 值（如颅骨重建补 `'7.1'`）。

- [ ] **Step 12: 运行 basic 模块测试**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-basic
```

Expected: BUILD SUCCESS

- [ ] **Step 13: Commit**

```bash
git add yigongbao-module-basic/ yigongbao-boot/src/test/resources/schema.sql
git commit -m "feat(basic): 移除 body_part.designerCode，rebuild_project 新增 specialty 字段"
```

---

## Task 5：DB — 更新主 DDL 和种子数据

**Files:**
- Modify: `sql/ddl.sql`
- Modify: `sql/init.sql`（若存在种子数据文件，否则记录 SQL 待手动执行）

- [ ] **Step 1: 更新 ddl.sql — sys_user.specialty 扩展**

将：
```sql
specialty           VARCHAR(64)     DEFAULT NULL COMMENT '专业方向',
```
改为：
```sql
specialty           VARCHAR(255)    DEFAULT NULL COMMENT '专业方向（多选逗号拼接，如 7.1,7.2）',
```

- [ ] **Step 2: 更新 ddl.sql — rebuild_body_part 移除 designer_code**

删除：
```sql
designer_code   VARCHAR(10)     DEFAULT NULL COMMENT '设计师编号（如A/B/C）',
```

- [ ] **Step 3: 更新 ddl.sql — rebuild_project 新增 specialty**

在 `remark` 列之前新增：
```sql
specialty             VARCHAR(64)     DEFAULT NULL COMMENT '专业方向字典编码（单值，如 7.1）',
```

- [ ] **Step 4: 在 init.sql（或记录备用 SQL）中新增系统配置种子数据**

```sql
INSERT INTO sys_config (config_key, config_value, config_name, remark, is_deleted) VALUES
('design.assign.mode', 'manual', '设计师分配模式', 'auto-自动分配，manual-手动分配', 0),
('design.assign.max.capacity', '10', '设计师最大并发工单数', '超出此数量不参与自动分配', 0);
```

- [ ] **Step 5: Commit**

```bash
git add sql/
git commit -m "feat(db): 更新 DDL（specialty 扩展/designer_code 移除/specialty 新增）和配置种子数据"
```

---

## Task 6：order 模块 — 新增分配服务和接口

**Files:**
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/DesignerAssignmentService.java`
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/DesignerAssignmentServiceImpl.java`
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/DesignerVO.java`
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/order/AssignDesignerDTO.java`
- Create: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/order/DesignerQueryDTO.java`
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderController.java`
- Modify: `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderMainServiceImpl.java`
- Modify: `yigongbao-module-order/src/test/resources/schema.sql`（补充 sys_user 和 rebuild_project 的 specialty 字段）

### Part A：VO / DTO

- [ ] **Step 1: 创建 DesignerVO**

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
    /** 专业方向编码列表 */
    private List<String> specialtyList;
    /** 专业方向名称列表 */
    private List<String> specialtyNameList;
    /** 当前在手工单数 */
    private Integer currentLoad;
    /** 最大并发工单数（来自系统配置） */
    private Integer maxCapacity;
}
```

- [ ] **Step 2: 创建 AssignDesignerDTO**

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

- [ ] **Step 3: 创建 DesignerQueryDTO**

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

### Part B：Service 接口

- [ ] **Step 4: 创建 DesignerAssignmentService 接口**

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
     * 审核通过后触发分配（根据系统配置决定自动或跳过）
     *
     * @param orderId 订单ID
     */
    void triggerAssignmentAfterAudit(Long orderId);

    /**
     * 自动分配设计师
     *
     * @param orderId 订单ID
     * @return 分配到的设计师用户ID，无可分配时返回 null
     */
    Long autoAssignDesigner(Long orderId);

    /**
     * 手动分配设计师（仅管理员，订单必须处于 PENDING_DESIGN 状态）
     *
     * @param orderId    订单ID
     * @param designerId 设计师用户ID
     */
    void manualAssignDesigner(Long orderId, Long designerId);

    /**
     * 设计师开始设计（仅设计师本人，订单必须处于 PENDING_DESIGN 状态且已分配给本人）
     *
     * @param orderId 订单ID
     */
    void startDesign(Long orderId);

    /**
     * 查询可分配设计师列表
     *
     * @param dto 查询条件（专业方向过滤）
     * @return 匹配的设计师列表
     */
    List<DesignerVO> listAvailableDesigners(DesignerQueryDTO dto);
}
```

### Part C：Mapper 扩展

- [ ] **Step 5: 在 UserMapper（system 模块）中新增设计师候选查询方法**

> **注意：** order 模块通过 system 模块的 UserService/UserMapper 查询用户。检查 order 模块是否已依赖 system 模块；若已依赖则直接注入 UserMapper，若未依赖则通过 UserService 的新方法暴露。

在 `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/mapper/UserMapper.java` 中新增：

```java
/**
 * 查询符合专业方向的设计师候选列表（按当前工单数升序）
 *
 * @param specialty     项目专业方向（单值，如 "7.1"）
 * @param maxCapacity   最大并发工单数上限
 * @return 设计师用户列表（已按工单数 ASC 排序）
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
```

同时新增按专业方向列表查询（供手动分配列表使用）：

```java
/**
 * 查询符合任意一个专业方向的设计师列表（手动分配时展示）
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
List<UserEntity> selectDesignersBySpecialties(@Param("specialtyCondition") String specialtyCondition);
```

> **说明：** `specialtyCondition` 由 Service 层拼接为 `FIND_IN_SET('7.1', specialty) > 0 OR FIND_IN_SET('7.2', specialty) > 0`。因使用 `${}` 直接拼接，Service 层必须对输入做白名单校验（只允许 `7.` 开头的字典编码），防止 SQL 注入。

### Part D：Service 实现

- [ ] **Step 6: 创建 DesignerAssignmentServiceImpl**

```java
package com.yigongbao.module.order.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.order.dto.order.DesignerQueryDTO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.service.DesignerAssignmentService;
import com.yigongbao.module.order.vo.order.DesignerVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
@RequiredArgsConstructor
public class DesignerAssignmentServiceImpl implements DesignerAssignmentService {

    private static final List<String> DESIGNER_ROLES = List.of("designer", "designer-manager");

    private final OrderMainServiceImpl orderMainService;  // 通过字段注入避免循环依赖，或使用 ApplicationContext
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ConfigService configService;
    private final DictService dictService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerAssignmentAfterAudit(Long orderId) {
        log.info("触发设计师分配，orderId={}", orderId);
        String mode = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_ASSIGN_MODE.getKey());
        if ("auto".equals(mode)) {
            Long designerId = autoAssignDesigner(orderId);
            if (designerId == null) {
                log.warn("自动分配未找到合适设计师，订单进入待分配状态，orderId={}", orderId);
            } else {
                log.info("自动分配成功，orderId={}, designerId={}", orderId, designerId);
            }
        } else {
            log.info("手动分配模式，跳过自动分配，orderId={}", orderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoAssignDesigner(Long orderId) {
        // 1. 获取订单专业方向（从 order_item 关联 rebuild_project）
        String specialty = getOrderSpecialty(orderId);
        if (StrUtil.isBlank(specialty)) {
            log.warn("订单明细中无法获取专业方向，跳过自动分配，orderId={}", orderId);
            return null;
        }
        // 2. 获取容量上限配置
        int maxCapacity = getMaxCapacity();
        // 3. 查询候选设计师（FIND_IN_SET 匹配，按工单数 ASC，已在 Mapper 中排序）
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
        var order = orderMainService.getById(orderId);
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
        // 3. 校验设计师 specialty 包含订单专业方向
        String orderSpecialty = getOrderSpecialty(orderId);
        if (StrUtil.isNotBlank(orderSpecialty)
                && !isSpecialtyMatch(designer.getSpecialty(), orderSpecialty)) {
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
        Long currentUserId = getCurrentUserId();
        var order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.PENDING_DESIGN.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }
        if (!currentUserId.equals(order.getDesignerId())) {
            log.warn("非本人不可开始设计，orderId={}, designerId={}, currentUserId={}",
                    orderId, order.getDesignerId(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }
        // 通过 FlowFacade 执行 START_DESIGN
        orderMainService.executeFlowAction(orderId, FlowActionEnum.START_DESIGN, null);
        log.info("开始设计成功，orderId={}", orderId);
    }

    @Override
    public List<DesignerVO> listAvailableDesigners(DesignerQueryDTO dto) {
        int maxCapacity = getMaxCapacity();
        List<String> specialties = dto.getSpecialties();
        if (CollUtil.isEmpty(specialties)) {
            return List.of();
        }
        // 构建 FIND_IN_SET 条件（白名单校验：只允许 7. 开头）
        String condition = specialties.stream()
                .filter(s -> StrUtil.isNotBlank(s) && s.startsWith("7."))
                .map(s -> String.format("FIND_IN_SET('%s', specialty) > 0",
                        s.replace("'", "")))  // 防注入
                .collect(Collectors.joining(" OR "));
        if (StrUtil.isBlank(condition)) {
            return List.of();
        }
        List<UserEntity> users = userMapper.selectDesignersBySpecialties(condition);
        return users.stream().map(u -> toDesignerVO(u, maxCapacity)).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 获取订单的专业方向（从 order_item → rebuild_project.specialty）
     */
    private String getOrderSpecialty(Long orderId) {
        // 查询 order_item 关联的 project_id，再查 rebuild_project.specialty
        // 由于 order_item 冗余存储了大量字段但未冗余 specialty，需要关联查询
        // 此处通过 OrderItemMapper 查 project_id，再通过 RebuildProjectMapper 查 specialty
        List<OrderItemEntity> items = orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItemEntity>()
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
        // 注入 RebuildProjectMapper 查询 specialty（basic 模块 mapper）
        // 如果 order 模块未直接依赖 basic 模块 mapper，通过 RebuildProjectService 暴露
        return getRebuildProjectSpecialty(projectId);
    }

    /**
     * 检查设计师 specialty（逗号拼接）是否包含指定 specialty
     */
    private boolean isSpecialtyMatch(String designerSpecialty, String orderSpecialty) {
        if (StrUtil.isBlank(designerSpecialty)) {
            return false;
        }
        List<String> list = StrUtil.splitToList(designerSpecialty, ',');
        return list.contains(orderSpecialty);
    }

    /**
     * 更新订单的设计师信息
     */
    private void updateOrderDesigner(Long orderId, UserEntity designer) {
        // 使用 orderMainService.update 更新 designerId / designerName
        var order = orderMainService.getById(orderId);
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
            log.warn("设计师最大容量配置无效，使用默认值 10，val={}", val);
            return 10;
        }
    }

    private DesignerVO toDesignerVO(UserEntity user, int maxCapacity) {
        DesignerVO vo = new DesignerVO();
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());
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
        vo.setMaxCapacity(maxCapacity);
        // currentLoad 从 SQL 的 current_load 字段取（需要 UserEntity 有此字段或用 Map 接收）
        // 若 UserEntity 没有 currentLoad 字段，可新增 @TableField(exist=false) 的 transient 字段
        return vo;
    }

    private Long getCurrentUserId() {
        return cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
    }

    /**
     * 从 rebuild_project 表查询 specialty
     * 需要 order 模块能访问 basic 模块的 RebuildProjectMapper 或通过 Service 接口
     */
    private String getRebuildProjectSpecialty(Long projectId) {
        // 实现方式：注入 RebuildProjectMapper（如果模块依赖允许）
        // 或：通过 RebuildProjectService 提供的 getById 方法获取
        // 在实际实现中确认 order → basic 的依赖链路，选择适合的方式
        return null; // 占位，Task 6 Step 6a 补充具体实现
    }
}
```

> **注意：** `getRebuildProjectSpecialty` 的具体实现取决于模块依赖。需检查 `yigongbao-module-order` 的 `pom.xml` 是否已依赖 `yigongbao-module-basic`。若未依赖，需要通过 `RebuildProjectService` 接口（basic 模块）暴露 `getSpecialtyByProjectId(Long)` 方法供 order 调用。

- [ ] **Step 6a: 检查模块依赖，处理 getRebuildProjectSpecialty**

```bash
grep -n "module-basic" /d/01_Project/02_Personal/医工宝/yigongbao-parent/yigongbao-module-order/pom.xml
```

- 若存在依赖 → 直接注入 `RebuildProjectMapper`，调用 `rebuildProjectMapper.selectById(projectId).getSpecialty()`
- 若不存在依赖 → 在 `RebuildProjectService` 中新增方法 `String getSpecialtyByProjectId(Long projectId)`，order 模块通过接口调用

### Part E：Controller 扩展

- [ ] **Step 7: 在 OrderController 新增三个接口**

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

同时在 OrderController 中注入 `DesignerAssignmentService`。

### Part F：集成 auditPass

- [ ] **Step 8: 修改 OrderMainServiceImpl.auditPass — 集成分配触发**

在 `auditPass` 方法的 `updateById(entity)` 之后、log.info 之前，追加：

```java
// 触发设计师分配（在事务内，保证原子性）
designerAssignmentService.triggerAssignmentAfterAudit(id);
```

同时在 `OrderMainServiceImpl` 中注入 `DesignerAssignmentService`（使用 `@Lazy` 避免循环依赖，或通过 `ApplicationContext` 懒加载）：

```java
@Lazy
private final DesignerAssignmentService designerAssignmentService;
```

- [ ] **Step 9: 编译验证**

```bash
cd yigongbao-parent && mvn compile -pl yigongbao-module-order -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add yigongbao-module-order/ yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/mapper/UserMapper.java
git commit -m "feat(order): 新增设计师分配 Service/接口，auditPass 集成分配触发"
```

---

## Task 7：单元测试 — 设计师分配服务

**Files:**
- Create: `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/DesignerAssignmentServiceImplTest.java`

- [ ] **Step 1: 创建测试类骨架**

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignerAssignmentServiceImplTest {

    @Mock private OrderMainServiceImpl orderMainService;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private UserMapper userMapper;
    @Mock private ConfigService configService;
    @Mock private DictService dictService;

    @InjectMocks private DesignerAssignmentServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper（若 DesignerAssignmentServiceImpl 未继承 ServiceImpl，可跳过）
    }
}
```

- [ ] **Step 2: 编写 triggerAssignmentAfterAudit 测试**

```java
@Test
@DisplayName("自动分配模式 — 触发 autoAssignDesigner")
void trigger_autoMode_shouldCallAutoAssign() {
    when(configService.getConfigValue("design.assign.mode")).thenReturn("auto");
    // mock autoAssignDesigner 被调用（spy 或 verify）
}

@Test
@DisplayName("手动分配模式 — 跳过自动分配")
void trigger_manualMode_shouldSkip() {
    when(configService.getConfigValue("design.assign.mode")).thenReturn("manual");
    // verify userMapper 从未被调用
}
```

- [ ] **Step 3: 编写 autoAssignDesigner 测试**

```java
@Test
@DisplayName("自动分配 — 找到设计师，更新订单 designerId")
void autoAssign_withCandidate_shouldUpdateOrder() {
    // mock orderItemMapper 返回含 projectId 的明细
    // mock rebuildProject 返回 specialty="7.1"
    // mock userMapper.selectAvailableDesigners 返回一个设计师
    // mock configService 返回 maxCapacity=10
    // 执行后 verify orderMainService.updateById 被调用
}

@Test
@DisplayName("自动分配 — 无候选设计师，返回 null")
void autoAssign_noCandidate_shouldReturnNull() {
    // mock userMapper 返回空列表
    // 结果为 null
}

@Test
@DisplayName("自动分配 — 订单无明细，返回 null")
void autoAssign_noOrderItem_shouldReturnNull() {
    // mock orderItemMapper 返回空列表
}
```

- [ ] **Step 4: 编写 manualAssignDesigner 测试**

```java
@Test
@DisplayName("手动分配 — 正常流程")
void manualAssign_success() { ... }

@Test
@DisplayName("手动分配 — 订单不存在，抛 ORDER_NOT_FOUND")
void manualAssign_orderNotFound_shouldThrow() { ... }

@Test
@DisplayName("手动分配 — 订单状态非 PENDING_DESIGN，抛 ORDER_STATUS_ERROR")
void manualAssign_wrongStatus_shouldThrow() { ... }

@Test
@DisplayName("手动分配 — 设计师不存在，抛 DESIGNER_NOT_FOUND")
void manualAssign_designerNotFound_shouldThrow() { ... }

@Test
@DisplayName("手动分配 — 设计师角色不合法，抛 DESIGNER_ROLE_INVALID")
void manualAssign_wrongRole_shouldThrow() { ... }

@Test
@DisplayName("手动分配 — 设计师已禁用，抛 DESIGNER_DISABLED")
void manualAssign_designerDisabled_shouldThrow() { ... }

@Test
@DisplayName("手动分配 — 专业方向不匹配，抛 DESIGNER_SPECIALTY_MISMATCH")
void manualAssign_specialtyMismatch_shouldThrow() { ... }
```

- [ ] **Step 5: 编写 startDesign 测试**

```java
@Test
@DisplayName("开始设计 — 正常，触发 FlowFacade START_DESIGN")
void startDesign_success() { ... }

@Test
@DisplayName("开始设计 — 订单非 PENDING_DESIGN 状态，抛 ORDER_STATUS_ERROR")
void startDesign_wrongStatus_shouldThrow() { ... }

@Test
@DisplayName("开始设计 — 当前用户非分配设计师，抛 ORDER_STATUS_ERROR")
void startDesign_notAssignedDesigner_shouldThrow() { ... }
```

- [ ] **Step 6: 运行测试验证全部通过**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-order -Dtest=DesignerAssignmentServiceImplTest
```

Expected: BUILD SUCCESS

- [ ] **Step 7: 运行全量测试**

```bash
cd yigongbao-parent && mvn test
```

Expected: BUILD SUCCESS，所有模块测试通过

- [ ] **Step 8: Commit**

```bash
git add yigongbao-module-order/src/test/
git commit -m "test(order): 新增 DesignerAssignmentServiceImpl 单元测试"
```

---

## Task 8：静态代码审查

- [ ] **Step 1: 运行全量编译和测试，确认无报错**

```bash
cd yigongbao-parent && mvn clean test
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 自查清单（逐项确认）**

针对本次所有改动，逐文件检查：

| 检查项 | 关注文件 |
|-------|---------|
| 新方法是否有 Javadoc + 行内注释 | DesignerAssignmentServiceImpl、UserServiceImpl 改动处 |
| Service 关键节点是否有 log.info/warn | DesignerAssignmentServiceImpl 所有方法 |
| 异常是否全部使用 ErrorCodeEnum | manualAssignDesigner 各校验点 |
| @Transactional 是否正确加在写操作上 | triggerAssignmentAfterAudit、autoAssign、manualAssign |
| SQL 拼接是否存在注入风险 | selectDesignersBySpecialties 的 condition 拼接 |
| DTO/VO 字段是否有必要校验注解 | AssignDesignerDTO（@NotNull 已加） |
| 循环依赖是否正确处理 | OrderMainServiceImpl ↔ DesignerAssignmentServiceImpl |
| schema.sql 列数与 INSERT 数据列数是否匹配 | basic/boot 两个 schema.sql |
| FlowStatusEnum 中 DESIGN_REVIEW_PASSED 不可见状态是否已更新 | FlowPhaseTransitionRules.isInvisibleStatus |
| 测试 mock 是否覆盖所有分支（成功+失败） | DesignerAssignmentServiceImplTest |

- [ ] **Step 3: 修复所有发现的问题**

对 Step 2 中发现的每个问题逐一修复。

- [ ] **Step 4: 再次运行全量测试确认**

```bash
cd yigongbao-parent && mvn clean test
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: 静态代码审查修复（注释/日志/校验/注入安全）"
```

---

## Task 9：文档更新

**需要更新的文档：**

| 文档 | 更新内容 |
|------|---------|
| `.docs/技术实现/system/05_用户管理功能实现方案.md` | 补充 specialty 多选改造说明（List\<String\> DTO、specialtyList/specialtyNameList VO） |
| `.docs/技术实现/basic/04_部位和重建项目功能实现方案.md` | 补充 designer_code 移除说明 + rebuild_project 新增 specialty 字段说明 |
| `.docs/技术实现/order/02_订单模块功能实现方案.md` | 补充 auditPass 触发分配的集成说明 |
| `.docs/技术实现/order/04_设计师分配设计实现方案.md` | 更新文档状态为"已实现"，修正 F1 描述（START_DESIGN 已存在），补充 specialty 统一二级的说明 |
| `.docs/接口文档/02_用户管理接口文档.md` | 更新 specialty 字段类型为数组，响应中新增 specialtyList/specialtyNameList |
| `.docs/接口文档/15_重建项目与部位管理.md` | 更新 rebuild_project 新增 specialty 字段，body_part 移除 designerCode |
| `.docs/接口文档/19_订单模块接口文档.md` | 新增三个接口文档：`/designers/available`、`/{id}/assign-designer`、`/{id}/start-design` |
| `.docs/需求分析/v1/用户体系分析.md`（已标记 M） | 同步用户专业方向多选的需求变化 |

- [ ] **Step 1: 更新设计方案文档（04_设计师分配设计实现方案.md）**

在版本记录中新增：
```
| 1.3 | 2026-04-10 | 确认实现：specialty 统一二级字典（7.1/7.2等）；FlowActionEnum.START_DESIGN 已存在无需新增；文档状态更新为已实现 | hanjor |
```

将文档状态从"规划中"改为"已实现"。

- [ ] **Step 2: 更新其他受影响文档**

按上表逐一更新，重点补充：
- 接口入参/出参的字段变化
- DDL 变化（新增/删除的字段）
- 状态码变化（设计阶段 21-26 的新映射）

- [ ] **Step 3: Commit**

```bash
git add .docs/
git commit -m "docs: 同步更新设计师分配相关技术文档和接口文档"
```

---

## 整体执行顺序总结

```
Task 1（common）→ Task 2（flow）→ Task 3（system）→ Task 4（basic）
    → Task 5（DB DDL）→ Task 6（order）→ Task 7（测试）
    → Task 8（代码审查）→ Task 9（文档）
```

每个 Task 结束后均有独立的 `mvn test` 验证和 git commit，确保每步可回滚。
