# 账户详情管理机构对象返回 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 账户详情在保留 `managedOrgIds` 的同时返回稳定、可直接回显的 `managedOrgs[{id, orgName}]`。

**Architecture:** 在管理机构服务层新增一次性快照查询，由同一批有效关系和机构数据派生额外机构 ID、精简对象及有效机构 ID，确保顺序和过滤规则一致。账户详情仅对区域管理员读取快照，但对所有角色显式初始化 `managedOrgs=[]`；账户列表和保存请求保持原状。

**Tech Stack:** Java 21、Spring Boot 3.2、MyBatis-Plus、Lombok、JUnit 5、Mockito、AssertJ、Maven

---

## 文件结构

- 新建 `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/ManagedOrgSimpleVO.java`：详情接口中的 `id + orgName` 精简对象。
- 新建 `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/ManagedOrgScopeVO.java`：同一次查询派生的三个机构范围字段。
- 修改 `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/UserVO.java`：增加只读响应字段 `managedOrgs`。
- 修改 `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/UserManagedOrgService.java`：增加快照查询契约。
- 修改 `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserManagedOrgServiceImpl.java`：一次关系查询、一次机构查询并保持关系顺序。
- 修改 `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java`：详情响应填充快照；列表逻辑不填充新字段。
- 修改 `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserManagedOrgServiceImplTest.java`：覆盖快照过滤、顺序和空名称规则。
- 修改 `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserServiceImplTest.java`：覆盖区域管理员与普通账户详情行为。
- 新建 `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/controller/UserControllerManagedOrgResponseTest.java`：使用 standalone MockMvc 覆盖详情与列表 JSON 契约，避开现有 Spring 集成测试环境对邮件组件的无关依赖。

本计划的固定基线提交为 `43e5845f8489fe6685b57e0109af4a04822aefc3`；最终复审直接使用该完整 SHA，不依赖跨命令 Shell 变量或相对提交数量。

### Task 1: 管理机构快照服务

**Files:**
- Create: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/ManagedOrgSimpleVO.java`
- Create: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/ManagedOrgScopeVO.java`
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/UserManagedOrgService.java`
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserManagedOrgServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserManagedOrgServiceImplTest.java`

- [ ] **Step 1: 写快照服务失败测试**

增加测试，模拟关系顺序 `[30, 10, 20, 40, 50, 60, 70]`，其中 `10` 是主机构、`20/30` 是有效机构、`40` 停用、`50` 类型错误、`60` 已删除、`70` 不存在；机构批量查询乱序返回。断言：

```java
ManagedOrgScopeVO scope = service.getManagedOrgScope(7L, 10L);

assertThat(scope.getManagedOrgIds()).containsExactly(30L, 20L);
assertThat(scope.getManagedOrgs())
        .extracting(ManagedOrgSimpleVO::getId, ManagedOrgSimpleVO::getOrgName)
        .containsExactly(tuple(30L, "服务商乙"), tuple(20L, "经销商甲"));
assertThat(scope.getEffectiveOrgIds()).containsExactly(10L, 30L, 20L);
verify(userManagedOrgMapper, times(1)).selectOrgIdsByUserId(7L);
verify(orgMapper, times(1)).selectList(any());
verify(orgMapper, never()).selectById(any());
verifyNoInteractions(userMapper);
verifyNoMoreInteractions(orgMapper);
```

捕获唯一一次 `selectList` 的查询条件，验证候选集合包含主机构及去重后的关系机构。另增测试：`orgName=null` 转为空字符串；空串和纯空白名称原样保留；使用参数化测试覆盖主机构停用、已删除、类型错误三种情况，均完整断言有效额外机构仍保留但 `effectiveOrgIds` 为空；无额外机构时仅返回有效主机构。

在同一 RED 阶段增加 Java 序列化往返测试：把 `ManagedOrgSimpleVO` 写入 `ObjectOutputStream` 再读回，断言 `id/orgName` 不变。该测试在类型不存在或未实现 `Serializable` 时必须失败。

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-system -am '-Dtest=UserManagedOrgServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: FAIL，原因是 `ManagedOrgScopeVO`、`ManagedOrgSimpleVO` 或 `getManagedOrgScope` 尚不存在。

- [ ] **Step 3: 实现精简 VO 与快照契约**

`ManagedOrgSimpleVO` 固定字段：

```java
@Data
@AllArgsConstructor
public class ManagedOrgSimpleVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String orgName;
}
```

`ManagedOrgScopeVO` 固定字段并默认空集合：

```java
@Data
public class ManagedOrgScopeVO {
    private List<Long> managedOrgIds = List.of();
    private List<ManagedOrgSimpleVO> managedOrgs = List.of();
    private List<Long> effectiveOrgIds = List.of();
}
```

服务接口新增：

```java
ManagedOrgScopeVO getManagedOrgScope(Long userId, Long primaryOrgId);
```

- [ ] **Step 4: 实现一次性快照查询**

实现必须：关系查询一次；候选机构 `IN` 查询一次且同时包含主机构与去重后的关系机构；不调用 `userMapper` 或 `orgMapper.selectById`；使用 `LinkedHashSet` 去重；从额外关系排除主机构；按关系 ID 顺序重建对象；复用 `isActiveBusinessOrg`；名称 `null` 转 `""`；仅主机构有效时生成 `effectiveOrgIds`。

- [ ] **Step 5: 运行快照服务测试并确认 GREEN**

Run: 同 Step 2。

Expected: `UserManagedOrgServiceImplTest` 全部通过，0 failures。

### Task 2: 账户详情返回 managedOrgs

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/UserVO.java`
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserServiceImplTest.java`
- Create: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/controller/UserControllerManagedOrgResponseTest.java`

- [ ] **Step 1: 写账户详情失败测试**

区域管理员测试模拟一个 `ManagedOrgScopeVO`，断言详情同时返回：

```java
assertThat(result.getManagedOrgIds()).containsExactly(20L, 30L);
assertThat(result.getManagedOrgs()).extracting(ManagedOrgSimpleVO::getOrgName)
        .containsExactly("经销商甲", "服务商乙");
assertThat(result.getEffectiveOrgIds()).containsExactly(10L, 20L, 30L);
verify(userManagedOrgService).getManagedOrgScope(1L, 10L);
verify(userManagedOrgService, never()).getManagedOrgIds(anyLong());
verify(userManagedOrgService, never()).getEffectiveOrgIds(anyLong());
verifyNoMoreInteractions(userManagedOrgService);
```

普通角色测试断言 `managedOrgs` 是空数组，并验证 `getManagedOrgScope` 从未调用。另验证 `listUser` 和 `exportUsers` 均不调用 `getManagedOrgScope`。

新建 standalone MockMvc 测试，直接构造 `UserController` 并模拟 `UserService`；为消息转换器配置与生产一致的 `JsonInclude.Include.NON_NULL`。断言：区域管理员对象的 `id`、`orgName` 和顺序完整匹配；非区域管理员详情存在 `"managedOrgs":[]`；空名称序列化为 `""`；列表中的 `managedOrgs=null` 时字段不出现。

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-system -am '-Dtest=UserServiceImplTest,UserControllerManagedOrgResponseTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: FAIL，原因是 `UserVO.managedOrgs` 或详情填充逻辑不存在。

- [ ] **Step 3: 实现详情响应字段**

在 `UserVO` 增加：

```java
private List<ManagedOrgSimpleVO> managedOrgs;
```

保持共享 `toVOWithNames` 不处理新字段，避免影响 `exportUsers`。在 `getUserById` 调用 `toVOWithNames` 后执行 `vo.setManagedOrgs(Collections.emptyList())`；仅该详情方法识别区域管理员并调用一次 `getManagedOrgScope(vo.getId(), vo.getOrgId())`，使用同一对象填充三个字段。为避免重复查询，应将 `toVOWithNames` 内原有区域管理员 `getManagedOrgIds/getEffectiveOrgIds` 逻辑移到详情专用代码中。

- [ ] **Step 4: 运行详情测试并确认 GREEN**

Run: 同 Step 2。

Expected: `UserServiceImplTest` 全部通过，0 failures。

### Task 3: 回归验证与复审

**Files:**
- Verify only; production changes仅在发现本功能问题时追加。

- [ ] **Step 1: 运行系统权限相关测试**

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-system -am '-Dtest=UserManagedOrgServiceImplTest,UserServiceImplTest,UserControllerManagedOrgResponseTest,UserHospitalServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: 全部通过，0 failures / 0 errors。

- [ ] **Step 2: 运行完整构建**

```powershell
mvn -f yigongbao-parent/pom.xml -DskipTests package
```

Expected: 13 个 Reactor 模块全部 `SUCCESS`。

- [ ] **Step 3: 核查 API 契约与变更边界**

检查：`managedOrgIds` 仍为 `List<Long>`；创建/编辑 DTO 未变化；账户列表没有填充 `managedOrgs`；详情空集合能被序列化；无数据库迁移；无 N+1 查询。

- [ ] **Step 4: 运行差异检查并请求代码复审**

```powershell
git diff --check
git status --short
git diff HEAD
```

复审重点：同快照一致性、空值语义、主机构重复过滤、无效机构过滤、查询次数和无关文件隔离。

- [ ] **Step 5: 修复复审问题并重新验证**

若有问题，先补失败测试，再做最小修复，重复 Task 3 的测试与构建命令。

- [ ] **Step 6: 按仓库规范创建唯一提交**

提交前依次运行：

```powershell
git status
git diff HEAD
git branch --show-current
git log --oneline -10
```

只暂存本功能的 VO、服务、测试和本计划文件，然后创建一个中文 Conventional Commit：

```text
feat: 返回账户管理机构名称列表
```

提交后使用以下固定命令做最终边界核对：

```powershell
git diff '43e5845f8489fe6685b57e0109af4a04822aefc3..HEAD'
```
