# 订单列表 `canModify` 字段方案与实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将订单分页接口中的旧 `modifyAuditStatus` 改为当前用户是否可以打开订单修改页面的布尔字段 `canModify`，并使订单模块和设计工单模块遵循同一套角色、阶段、订单状态和待审核申请规则。

**Architecture:** 后端在两个分页 Service 中复用同一套纯判断规则：先排除无效记录和待审核修改申请，再按当前登录用户角色、订单阶段和直接修改时间窗口计算是否可以进入修改页面。`canModify` 只表示“可以打开页面”，不表示一定直接修改或一定提交申请；提交时仍由前端按角色/阶段选择直接修改或申请流程，订单阶段超时由 `full-v2` 返回 `-1` 后转申请。

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8, JUnit 5/Mockito, Vue 3, TypeScript。

---

## 一、已确认的业务语义

### 1. 字段含义

`canModify` 表示：

> 当前登录用户是否可以打开该订单的修改页面，并尝试完成一次符合权限的修改流程。

它不表示：

- 当前操作一定会直接修改成功；
- 当前操作一定会创建修改申请；
- 当前订单是否存在历史修改申请；
- 最近一条修改申请的审核结果。

### 2. 前端提交分支

| 前端场景 | 页面打开后动作 |
|---|---|
| 管理员/公司管理员 | 直接调用 `orderModifyFullV2` |
| 业务员/区域管理员，订单阶段且窗口内 | 调用 `orderModifyFullV2`，直接修改 |
| 业务员/区域管理员，订单阶段且窗口外 | 调用 `orderModifyFullV2`，收到 `-1` 后确认，再调用申请接口 |
| 业务员/区域管理员，设计阶段 | 直接调用申请接口 |
| 设计师/设计管理员，设计阶段 | 直接调用申请接口 |

因此，业务员/区域管理员在订单阶段是否超时，不影响 `canModify` 打开页面的结果。

### 3. 角色分组

| 业务组 | 设计组 | 管理员组 |
|---|---|---|
| `salesman` | `designer` | `admin` |
| `salesman-self` | `designer-manager` | `company-admin` |
| `regional-manager` |  |  |

业务管理员按业务员处理，设计管理员按设计师处理。后端申请入口、全量修改入口和列表字段必须使用同一角色分组，避免列表显示可修改但提交被拒绝。

---

## 二、输入条件定义

### 1. 当前用户角色

- `isAdmin`：`admin` 或 `company-admin`。
- `isBusiness`：`salesman`、`salesman-self`、`regional-manager`。
- `isDesigner`：`designer`、`designer-manager`。
- 其他角色：不属于上述三组。

### 2. 订单阶段

- 订单阶段：`FlowPhaseEnum.ORDER`，当前值 `10`。
- 设计阶段：`FlowPhaseEnum.DESIGN`，当前值 `20`。
- 生产及后续阶段：除订单阶段、设计阶段以外的有效阶段，包括打印、后处理、质检、仓储等阶段。
- 未知或空阶段：视为不允许非管理员修改。

### 3. 订单状态

订单状态与订单阶段不是同一概念。当前状态枚举包括：

- 订单阶段：`1010` 草稿、`1020` 数据待审核、`1030` 数据审核通过、`1040` 数据审核不通过；
- 设计阶段：`2010` 待设计、`2020` 设计中、`2030` 设计完成；
- 生产及后续：`3010` 至 `6030`；
- 终态：`8010` 已完成、`9010` 已取消。

本需求已明确管理员可在任意阶段直接修改，因此不能用“非订单/设计阶段状态”统一拒绝管理员。

当前建议的状态策略：

1. 记录不存在、已删除、状态或阶段数据无效：`false`；
2. 管理员/公司管理员：只要记录有效，按“任意阶段可直接修改”处理，返回 `true`；
3. 业务组和设计组：由阶段规则决定，生产及后续及终态均返回 `false`；
4. 不额外引入未确认的状态黑名单；如果以后明确“已完成”或“已取消”也禁止管理员修改，再增加显式规则并同步所有入口。

### 4. 未审核修改申请

只有 `order_modification_apply.status = 1`（`PENDING`，待审核）属于未审核修改申请。

以下状态为终态，不阻止重新打开修改页面：

- `2`：已通过；
- `3`：已驳回；
- `4`：已过期。

按当前确认的需求，存在待审核修改申请时，`canModify = false`。该条件作为全局阻断条件，优先级高于角色和阶段判断；这意味着管理员也不能通过列表页面打开该订单修改页，直到申请被处理、驳回或过期。

### 5. 待审核取消申请

申请接口当前禁止存在待审核取消申请的订单提交修改申请。为避免列表显示可以进入、提交时才失败，`canModify` 应将待审核取消申请作为阻断条件，返回 `false`。

管理员是否允许在待审核取消申请存在时直接修改，当前代码和需求没有单独确认。方案按“待审核申请阻断修改页面”处理；如果业务要求管理员仍可直改，只需对管理员增加例外，但必须同步 `full-v2` 和列表字段。

---

## 三、完整条件分支与返回结果

以下矩阵均以“订单记录有效、在当前用户数据权限范围内”为前提。

### 1. 待审核修改申请优先级

| 角色 | 阶段 | 是否有 `status=1` 修改申请 | `canModify` | 说明 |
|---|---|---:|---:|---|
| 管理员/公司管理员 | 任意有效阶段 | 是 | `false` | 待审核申请全局阻断 |
| 业务员/区域管理员 | 订单阶段 | 是 | `false` | 不允许再次进入修改页 |
| 业务员/区域管理员 | 设计阶段 | 是 | `false` | 不允许再次提交申请 |
| 设计师/设计管理员 | 设计阶段 | 是 | `false` | 不允许再次提交申请 |
| 任意角色 | 任意阶段 | 否 | 进入角色/阶段判断 | 继续计算 |

### 2. 无待审核修改申请时的角色×阶段矩阵

| 角色 | 订单阶段 | 设计阶段 | 生产及后续阶段 | 页面内流程 |
|---|---:|---:|---:|---|
| 管理员/公司管理员 | `true` | `true` | `true` | 直接修改 |
| 业务员/区域管理员 | `true` | `true` | `false` | 订单阶段按时间决定直改或转申请；设计阶段直接申请 |
| 设计师/设计管理员 | `false` | `true` | `false` | 设计阶段直接申请 |
| 其他角色 | `false` | `false` | `false` | 无修改权限 |

### 3. 业务组订单阶段时间窗口分支

| 角色 | 阶段 | 直接修改窗口 | `canModify` | 提交结果 |
|---|---|---:|---:|---|
| 业务员/区域管理员 | 订单阶段 | 内 | `true` | `full-v2` 返回 `1`，直接修改 |
| 业务员/区域管理员 | 订单阶段 | 外 | `true` | `full-v2` 返回 `-1`，前端确认后调用申请接口 |
| 业务员/区域管理员 | 订单阶段 | 边界时刻 | `true` | 由后端精确时间判断；到达配置边界仍在窗口内 |
| 业务员/区域管理员 | 订单阶段 | 无法确定创建时间 | `true` | 页面可打开，提交接口负责最终校验；不应误报已直接修改 |

`canModify` 不直接使用 `canApply` 的语义。否则订单阶段窗口内会错误返回 `false`，用户无法进入直接修改页面；订单阶段窗口外也会错误阻止用户进入页面并触发 `-1` 转申请流程。

### 4. 订单状态分支

| 订单记录/状态条件 | 管理员/公司管理员 | 业务组 | 设计组 | 说明 |
|---|---:|---:|---:|---|
| 订单存在、未删除、阶段有效 | 继续按角色规则 | 继续按角色规则 | 继续按角色规则 | 正常记录 |
| 订单不存在或无数据权限 | `false` | `false` | `false` | 不暴露订单信息 |
| 订单已删除 | `false` | `false` | `false` | 列表正常不会返回，作为防御规则 |
| 阶段为空或无法识别 | `true` | `false` | `false` | 管理员跨阶段能力；非管理员拒绝 |
| 订单状态属于生产/完成/取消且阶段非订单/设计 | `true` | `false` | `false` | 管理员按任意阶段规则，其他角色按阶段规则 |
| 状态与阶段数据明显不一致 | `true` | `false` | `false` | 管理员仍按有效订单处理；非管理员保守拒绝 |

说明：最后两行是基于“管理员任意阶段可修改”的落地解释。若后续业务确认终态也不允许管理员修改，应将管理员结果改为 `false`，并同步后端直接修改入口。

### 5. 待审核取消申请分支

| 角色 | 阶段原本允许打开 | 待审核取消申请 | `canModify` |
|---|---:|---:|---:|
| 管理员/公司管理员 | `true` | 是 | `false`（按本方案全局阻断） |
| 业务组 | `true` | 是 | `false` |
| 设计组 | `true` | 是 | `false` |
| 任意角色 | `false` | 是/否 | `false` |

---

## 四、抽象为代码判断逻辑

### 1. 单订单决策函数

建议抽象为一个返回决策结果的方法，而不是在两个 Service 中各自拼接条件：

```java
ModifyPageAccessDecision decideCanModify(
    OrderMainEntity order,
    String roleCode,
    boolean hasPendingModifyApply,
    boolean hasPendingCancelApply,
    LocalDateTime now
)
```

最小布尔实现为：

```java
boolean canModify(OrderMainEntity order, String roleCode,
                  boolean hasPendingModifyApply,
                  boolean hasPendingCancelApply,
                  LocalDateTime now) {
    if (order == null || order.getIsDeleted() != NOT_DELETED) {
        return false;
    }
    if (hasPendingModifyApply || hasPendingCancelApply) {
        return false;
    }

    if (isAdmin(roleCode)) {
        return true;
    }

    if (isBusiness(roleCode)) {
        return isOrderPhase(order) || isDesignPhase(order);
    }

    if (isDesigner(roleCode)) {
        return isDesignPhase(order);
    }

    return false;
}
```

时间窗口只影响订单阶段提交路径，不影响 `canModify`：

```java
if (isBusiness(roleCode) && isOrderPhase(order)) {
    if (isWithinDirectWindow(order.getCreateTime(), now)) {
        // full-v2 直接修改
    } else {
        // full-v2 返回 -1，前端确认后调用 apply
    }
}
```

### 2. 列表批量查询

两个分页 Service 必须避免逐行调用 `hasPendingApply`：

1. 查询当前页订单；
2. 收集当前页订单 ID；
3. 批量查询待审核修改申请的订单 ID；
4. 批量查询待审核取消申请的订单 ID；
5. 获取当前登录用户角色一次；
6. 对当前页每条订单调用纯内存判断函数；
7. 写入 `canModify`。

推荐 SQL 条件：

```sql
WHERE order_id IN (...)
  AND status = 1
  AND is_deleted = 0
```

修改申请表建议具备联合索引：

```sql
(order_id, status, is_deleted)
```

### 3. 接口契约

两个分页 VO：

```java
private boolean canModify;
```

删除或废弃：

```java
private Integer modifyAuditStatus;
```

两个接口返回字段保持同名、同类型、同语义：

- `POST /order/page`
- 设计工单分页查询接口

前端同步：

- `OrderVO.modifyAuditStatus` 改为 `canModify: boolean`；
- `DesignWorkorderListVO` 对应类型同步修改；
- 修改入口使用 `row.canModify` 控制按钮或页面进入；
- 不再使用 `0/1/2` 推断申请状态；
- 申请状态仍由修改申请列表接口的 `status/statusDesc` 表达。

---

## 五、实施步骤

### Task 1：先补充后端决策测试

**Files:**

- Test: `yigongbao-parent/yigongbao-module-order/src/test/.../OrderMainServiceImpl...Test.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/.../DesignWorkorderServiceImplTest.java`

- [ ] 覆盖管理员在订单、设计、生产及后续阶段均为 `true`。
- [ ] 覆盖业务组订单阶段窗口内/窗口外均为 `true`。
- [ ] 覆盖业务组设计阶段为 `true`，生产及后续为 `false`。
- [ ] 覆盖设计组仅设计阶段为 `true`。
- [ ] 覆盖 `regional-manager` 与业务员一致。
- [ ] 覆盖 `designer-manager` 与设计师一致。
- [ ] 覆盖有 `status=1` 待审核修改申请时所有角色为 `false`。
- [ ] 覆盖状态 `2/3/4` 不阻止重新打开。
- [ ] 覆盖待审核取消申请、无权限、已删除/无效订单的结果。
- [ ] 先运行测试，确认新增行为在旧代码下失败。

### Task 2：后端 VO 和批量填充逻辑

**Files:**

- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/OrderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderMainServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`
- Possibly create: shared order modification access decision class under order module

- [ ] 将两个 VO 字段改为 `boolean canModify`。
- [ ] 将申请查询从“取最新状态”改为“只生成待审核订单 ID 集合”。
- [ ] 批量纳入待审核取消申请判断。
- [ ] 复用统一角色/阶段决策方法。
- [ ] 保证空订单页不执行无意义的 `IN` 查询。

### Task 3：后端提交入口对齐

**Files:**

- Modify: `OrderModifyApplyServiceImpl.java`
- Modify: `OrderModifyFullServiceImpl.java`（如需补充与列表一致的待审核校验）
- Modify: 角色集合和申请权限判断

- [ ] 将业务管理员和设计管理员纳入与对应角色相同的权限分组。
- [ ] 使 `canModify` 返回 `true` 的角色最终不会被申请入口无条件拒绝。
- [ ] 使待审核申请阻断页面后的提交逻辑不产生前后端语义冲突。
- [ ] 保留管理员任意阶段直接修改规则。
- [ ] 保留 `full-v2` 超时返回 `-1`、不自动创建申请的规则。
- [ ] 重新运行后端测试。

### Task 4：前端契约和三个提交分支

**Files:**

- Modify: `D:\01_Project\02_Personal\医工宝前端\med-tech\src\api\order.ts`
- Modify: `D:\01_Project\02_Personal\医工宝前端\med-tech\src\views\business\orderComponents\orderList.vue`
- Modify: `D:\01_Project\02_Personal\医工宝前端\med-tech\src\views\business\orderComponents\orderCreateDialog.vue`
- Modify: `D:\01_Project\02_Personal\医工宝前端\med-tech\src\views\business\orderComponents\fileDialog.vue`

- [ ] 将订单行字段改为 `canModify`。
- [ ] 订单阶段和设计阶段统一用 `canModify` 控制是否进入页面。
- [ ] 保留业务员订单阶段 `full-v2 -> -1 -> confirm -> apply`。
- [ ] 保留设计阶段直接调用申请接口。
- [ ] 修复附件修改流程忽略 `-1` 的问题。
- [ ] 不能因 `-1` 显示“修改成功”。

### Task 5：集成验证和兼容清理

- [ ] 后端订单模块测试通过。
- [ ] 设计模块相关测试通过。
- [ ] 前端 TypeScript 检查和构建通过。
- [ ] 搜索确认不存在生产代码对 `modifyAuditStatus` 的旧依赖。
- [ ] 验证分页每页只执行批量申请查询，不产生 N+1 查询。
- [ ] 使用浏览器网络面板验证三条提交路径。
- [ ] 更新接口文档和前后端字段说明。

---

## 六、自审结论

### 已闭合的逻辑

- `canModify` 与 `canApply` 已明确区分；
- 订单阶段超时仍返回 `true`，保证可以进入页面并由 `-1` 转申请；
- 管理员/公司管理员不受阶段限制；
- 业务员/区域管理员与设计师/设计管理员分别按组处理；
- 只有 `PENDING=1` 阻止新的修改流程，`2/3/4` 不阻止；
- 两个分页接口使用同一语义和同一字段类型；
- 批量查询避免 N+1。

### 实施前必须保持的边界

- “管理员任意阶段可修改”不能被普通角色阶段判断覆盖；
- “存在待审核修改申请时返回 `false`”必须与 `full-v2` 的实际权限保持一致；
- 待审核取消申请是否阻断管理员直改是唯一需要在实施时按最终业务选择确认的例外点；本方案默认全局阻断；
- 不把订单历史申请状态混入 `canModify`，申请历史应继续使用申请列表状态字段表达。

### 最终抽象

```text
canModify = validOrder
         && noPendingModifyApply
         && noPendingCancelApply
         && (
                admin
             || (business && phase in {ORDER, DESIGN})
             || (designer && phase == DESIGN)
            )
```

订单阶段时间窗口只决定提交时的执行分支，不改变 `canModify`。
