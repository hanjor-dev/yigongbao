# 医工宝后端代码全面 Review 报告

**审查日期**：2026-04-08  
**审查范围**：`yigongbao-parent` 全模块（common、framework、module-system、module-basic、module-flow、module-order、boot）  
**审查方法**：静态代码分析，结合 `sql/ddl.sql` 表结构、CLAUDE.md 和 java-coding-standards.md 规范

---

## 总体评估：**REQUEST_CHANGES**

发现问题共 **19 项**（P0: 2, P1: 5, P2: 8, P3: 4）。其中 P0 问题涉及生产安全和数据完整性，必须修复后方可上线。

---

## 一、P0 - 严重问题（必须修复，阻塞合并）

### P0-1：FlowDebugController 无环境隔离，可在生产绕过所有业务规则

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/flow/FlowDebugController.java`

**问题描述**：

该 Controller 暴露了 `/api/order/debug/*` 系列接口，包含以下高危端点：

- `POST /api/order/debug/reset`：直接修改 `order_main` 的 `phase` 和 `status` 字段，完全绕过状态机。任何知道该 URL 的人均可将任意订单设置为任意状态。
- `POST /api/order/debug/execute`：在生产环境中直接执行流转动作（写 DB）。
- `POST /api/order/debug/preview`：接口注释标注"不落库"，但实际上调用了 `flowFacade.executeFlow()`，该方法会写入 `order_flow_status_history` 表，**实际落库**，描述与行为不符。

当前代码没有任何 Spring Profile 条件守卫、`@ConditionalOnProperty`、或管理员权限校验。

**影响**：高。任意认证用户可通过 reset 接口将已完成的订单改回草稿状态，或将草稿直接跳过所有审核环节标记为"已完成"，导致业务数据错乱。

**修复方案**：

```java
// 方案一：Spring Profile 条件守卫（推荐）
@Profile({"dev", "test"})
@RestController
@RequestMapping("/api/order/debug")
public class FlowDebugController { ... }

// 方案二：@ConditionalOnProperty
@ConditionalOnProperty(name = "app.debug.flow.enable", havingValue = "true")
@RestController
public class FlowDebugController { ... }
```

同时修复 `preview` 端点的描述，或将其改为只调用预览逻辑（不执行 `flowFacade.executeFlow()`）。

---

### P0-2：ErrorCodeEnum 存在重复错误码 689

**文件**：`yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**问题描述**：

```java
ORDER_FILE_NOT_UPLOADED(689, "订单文件未上传"),
ORDER_FILE_NOT_FOUND(689, "订单文件不存在"),
```

两个不同语义的枚举值共用错误码 689。客户端收到 689 响应时无法区分是"未上传"还是"找不到记录"，日志追查和监控告警也会混淆。

**影响**：中高。影响所有订单文件相关接口的错误响应语义正确性，运维时无法通过错误码精确定位问题类型。

**修复方案**：

```java
ORDER_FILE_NOT_UPLOADED(689, "订单文件未上传"),
ORDER_FILE_NOT_FOUND(690, "订单文件不存在"),   // 修改此处
```

---

## 二、P1 - 高优先级问题（建议在本 PR 修复）

### P1-1：`COMPLETE_POST_PROCESSING` 动作导致 phase/status 不一致

**文件**：`yigongbao-module-flow/src/main/java/com/yigongbao/module/flow/rule/FlowPhaseTransitionRules.java`  
**相关文件**：`FlowStatusTransitionRules.java`

**问题描述**：

`FlowStatusTransitionRules.getTargetStatus()` 中：

```java
case COMPLETE_POST_PROCESSING -> FlowStatusEnum.QC_IN_PROGRESS;  // status = 51
```

`FlowPhaseTransitionRules.decideNextPhaseAndStatus()` 中完全没有处理 `QC_IN_PROGRESS(51)` 的分支。

流转结果：订单 `status` 更新为 51（`QC_IN_PROGRESS`，属于 QC 阶段 phase=5），但 `phase` 仍然停留在 POST_PROCESSING(4)。

**影响**：严重逻辑错误。`phase=4, status=51` 是一个非法状态组合，后续所有依赖 phase 进行业务判断的代码（如 `listOrders()` 的 `phase=1` 过滤、`getAvailableActions()` 的 phase switch 分支）都会产生错误结果，导致订单在后处理完成后卡死在错误状态。

**修复方案**：在 `FlowPhaseTransitionRules.decideNextPhaseAndStatus()` 中增加对应分支：

```java
case QC_IN_PROGRESS -> new PhaseStatusPair(FlowPhaseEnum.QC, FlowStatusEnum.QC_IN_PROGRESS);
```

或在 `FlowStatusTransitionRules.getTargetStatus()` 中将 `COMPLETE_POST_PROCESSING` 的目标改为一个 POST_PROCESSING 阶段内的状态，并在 `FlowPhaseTransitionRules` 中触发 phase 推进。

---

### P1-2：`REWORK` 状态死锁，订单无法继续流转

**文件**：`yigongbao-module-flow/src/main/java/com/yigongbao/module/flow/rule/FlowStatusTransitionRules.java`

**问题描述**：

当 QC 阶段执行 `QC_FAIL` 动作后，订单进入 `REWORK(54)` 状态。但 `getAvailableActions()` 的 `case QC:` 分支中没有处理 `status=REWORK` 的情况（仅处理了 `QC_IN_PROGRESS` 和 `QC_PASSED`），导致 `getAvailableActions()` 返回空列表。

空列表意味着没有任何动作可执行，订单永久卡在 REWORK 状态，无法推进也无法回退。

**影响**：严重。返工状态是 QC 阶段的核心业务场景（产品不合格需返工），此 bug 会导致任何进入返工的订单彻底卡死。

**修复方案**：

在 `FlowStatusTransitionRules.getAvailableActions()` 的 QC phase 分支中补充：

```java
case REWORK -> List.of(FlowActionEnum.REWORK_COMPLETE);
```

同时在 `FlowPhaseTransitionRules.decideNextPhaseAndStatus()` 中处理 `REWORK_COMPLETE` 的目标状态（回到 `QC_IN_PROGRESS` 重新质检，或直接进入下一阶段）。

---

### P1-3：`OrderDraftServiceImpl.listDrafts()` — itemCount 始终为 0

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/draft/service/impl/OrderDraftServiceImpl.java`  
**行号**：约第 110-130 行

**问题描述**：

```java
// 填充数据（操作的是 entity 对象）
entity.setItemCount(count);
entity.setExpiresAt(expiresAt);

// 转换（使用 toOrderDraftVO 方法，内部重新从 entity 读取字段，但 entity 中无对应列）
IPage<OrderDraftVO> voPage = pageResult.convert(this::toOrderDraftVO);
```

`itemCount` 和 `expiresAt` 是填充到内存中的 `OrderDraftEntity` 对象上，但 `toOrderDraftVO()` 是通过 `BeanUtils.copyProperties(entity, vo)` 从数据库映射的字段复制，而 `order_draft` 表中根本没有 `item_count` 列（该字段是临时计算的），导致转换后 VO 的 `itemCount` 永远是 null 或 0。

**影响**：前端草稿列表中"明细数量"字段永远显示 0，功能性 bug。

**修复方案**：在 `pageResult.convert()` 的 lambda 中直接构建 VO 并填充计算字段，不要先填充 entity：

```java
IPage<OrderDraftVO> voPage = pageResult.convert(entity -> {
    OrderDraftVO vo = toOrderDraftVO(entity);
    // 查询并填充 itemCount
    int count = orderItemDraftMapper.countByDraftId(entity.getId());
    vo.setItemCount(count);
    // 填充 expiresAt
    vo.setExpiresAt(entity.getCreateTime().plusDays(draftExpireDays));
    return vo;
});
```

---

### P1-4：`OrderDraftServiceImpl.removeDraft()` — NullPointerException 风险

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/draft/service/impl/OrderDraftServiceImpl.java`  
**行号**：约第 267 行

**问题描述**：

```java
Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
// ...
if (!currentUserId.equals(entity.getOperatorId())) {  // ← NPE if currentUserId == null
    throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
}
```

当 `StpUtil.isLogin()` 返回 false 时，`currentUserId` 为 null，接下来 `currentUserId.equals(...)` 触发 NPE，而非返回业务异常。

**影响**：中。未登录请求调用此接口会触发 500 而非 401 未授权响应，日志中会出现 NPE 堆栈污染。

**修复方案**：

```java
Long currentUserId = StpUtil.getLoginIdAsLong();  // SaToken 会在未登录时自动抛出 NotLoginException
// 或者显式守卫：
if (currentUserId == null) {
    throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
}
```

---

### P1-5：`OrderMainServiceImpl.removeOrder()` — 未校验状态即允许删除，且遗漏关联文件清理

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/main/service/impl/OrderMainServiceImpl.java`

**问题描述**：

1. `removeOrder()` 直接执行软删除，没有校验订单当前状态。理论上只有草稿或特定状态的订单才应允许删除；生产中/已完成的订单被意外删除会严重影响业务数据。
2. 删除订单时没有同步软删除 `order_item`（明细）和 `order_file`（附件）关联记录，导致孤儿数据残留。
3. 正确逻辑是只允许删除草稿，订单数据一旦正式提交，任何人不允许删除！**重要！**

**影响**：高。任意状态订单可被删除；明细和附件记录游离于已删除的主单之外，影响数据完整性和统计准确性。

**修复方案**：

```java
public void removeOrder(Long id) {
    OrderMainEntity entity = getById(id);
    if (entity == null) {
        throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
    }
    // 仅允许删除特定状态的订单（根据业务规则调整）
    if (!DELETABLE_STATUSES.contains(entity.getStatus())) {
        throw new BusinessException(ErrorCodeEnum.ORDER_CANNOT_DELETE);
    }
    // 级联软删除子记录
    orderItemMapper.softDeleteByOrderId(id);
    orderFileMapper.softDeleteByOrderId(id);
    removeById(id);
}
```

---

## 三、P2 - 中优先级问题（建议修复或创建 follow-up）

### P2-1：`DoctorServiceImpl.fillExtraFields()` — N+1 查询问题

**文件**：`yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/doctor/service/impl/DoctorServiceImpl.java`  
**行号**：第 330-354 行

**问题描述**：

`fillExtraFields()` 在每条医生记录上分别调用 `hospitalService.getById()` 和 `hospitalDeptService.getById()`。在 `listDoctors()`（分页）和 `listAll()`（全量）中对每条记录都触发，导致 N+1 查询：查询 20 条医生 = 1次主查询 + 最多 40 次关联查询。

**修复方案**：先批量收集 hospitalId 和 hospitalDeptId，一次性 `selectBatchIds()` 查出，再 map 填充：

```java
List<Long> hospitalIds = list.stream().map(DoctorEntity::getHospitalId).filter(Objects::nonNull).distinct().toList();
Map<Long, HospitalVO> hospitalMap = hospitalService.listByIds(hospitalIds).stream()
    .collect(Collectors.toMap(HospitalVO::getId, v -> v));
// 再循环填充 vo.setHospitalName(hospitalMap.get(entity.getHospitalId()).getHospitalName())
```

---

### P2-2：`OrderMainServiceImpl` — 订单文件相关方法中 N+1 查询

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/main/service/impl/OrderMainServiceImpl.java`

**问题描述**：

`fillOrderFiles()` 和 `validateFileIdsExist()` 中对每个 fileId 逐一查询文件记录，而非批量查询。

**修复方案**：使用 `selectBatchIds()` 一次性查出所有文件，再做映射。

---

### P2-3：`ResourceServiceImpl.deleteResource()` — 物理删除绕过软删除机制

**文件**：`yigongbao-module-system/src/main/java/com/yigongbao/module/system/resource/service/impl/ResourceServiceImpl.java`

**问题描述**：

```java
baseMapper.deleteById(id);  // 物理删除
```

`sys_resource` 表含有 `is_deleted` 字段，其他模块均通过 MyBatis Plus 的 `@TableLogic` 软删除（`removeById()`）。此处直接调用 `baseMapper.deleteById()` 绕过了逻辑删除机制，导致记录从数据库中物理消失，无法追溯历史权限变更。

**修复方案**：改为 `removeById(id)`（继承自 `ServiceImpl` 的软删除方法）。

---

### P2-4：`listOrders()` — phase 过滤硬编码 1，无法查询其他阶段订单

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/main/service/impl/OrderMainServiceImpl.java`

**问题描述**：

```java
wrapper.eq(OrderMainEntity::getPhase, 1);  // 硬编码 phase=1
```

`listOrders()` 始终只查询 ORDER 阶段（phase=1）的订单。随着系统上线，订单会流转到 DESIGN(2)、PRINT(3) 等后续阶段，这些订单在此接口中完全不可见。

**影响**：中高。已流转的订单从列表中消失，前端无法查看全部订单。

**修复方案**：移除或改为从 `OrderListDTO` 接收 phase 过滤参数，默认不过滤 phase（查全部），或按业务需求允许前端传入 phase 范围。

---

### P2-5：`listOrders()` — 缺少数据范围（dataScopeType）过滤

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/main/service/impl/OrderMainServiceImpl.java`

**问题描述**：

`listOrders()` 没有根据当前用户的 `dataScopeType` 过滤订单。所有角色（无论是业务员、主管还是管理员）都能看到全部订单，完全绕过了 `UserHospitalService.getDataScopeType()` 已实现的数据权限体系。

**影响**：高。数据越权，普通业务员可看到其他业务员/其他医院的订单。

**修复方案**：当前已经实现了 OrderQueryHelper，建议将该功能引入订单查询逻辑中。


---

### P2-6：`HospitalServiceImpl.listMyOptions()` — userId 参数完全被忽略

**文件**：`yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/hospital/service/impl/HospitalServiceImpl.java`  
**行号**：第 235-251 行

**问题描述**：

方法注释声称"根据用户权限过滤"，但实现直接查询所有正常状态医院，userId 参数没有被使用。注释还说"实际逻辑在 Controller 层根据角色的 dataScopeType 进行过滤"，但查阅调用处的 Controller 也没有过滤逻辑。

**影响**：方法名和注释具有误导性，且实际上所有用户拿到的都是全量医院列表。如果业务上需要按 dataScopeType 限制可见医院，此处需要实现。

**修复方案**：
- 需要按权限过滤，将 `UserHospitalServiceImpl.getMyHospitalOptions()` 的逻辑迁移至此。

---

### P2-7：`UserServiceImpl.changePassword()` — 未校验新密码与旧密码相同

**文件**：`yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java`

**问题描述**：

`UserServiceImpl.changePassword()` 允许用户将密码改成与当前密码相同的值，没有报错。对比 `AuthServiceImpl.changePassword()` 已有此校验：

```java
if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
    throw new BusinessException("新密码不能与旧密码相同");
}
```

**修复方案**：在 `UserServiceImpl.changePassword()` 中加入相同的校验逻辑。

---

### P2-8：`AuthServiceImpl.getClientIp()` — X-Forwarded-For 可被伪造，影响审计日志准确性

**文件**：`yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth/service/impl/AuthServiceImpl.java`

**问题描述**：

```java
String ip = request.getHeader("X-Forwarded-For");
```

`X-Forwarded-For` Header 是用户可控的，攻击者可以伪造任意 IP 值。虽然当前账号锁定机制是基于用户名（非IP），伪造 IP 不会绕过锁定，但所有审计日志记录的 IP 将不可信，影响安全事件溯源。

**修复方案**：如系统部署于可信反向代理之后，应验证 `X-Forwarded-For` 中的 IP 数量并只取最后一跳；若无反向代理，直接使用 `request.getRemoteAddr()`。建议在 `application.yml` 中配置信任的代理 IP 列表。

---

## 四、P3 - 低优先级问题（可选优化）

### P3-1：`CodeGeneratorServiceImpl.getOrCreateSequence()` — 高并发首次创建竞争条件

**文件**：`yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/code/service/impl/CodeGeneratorServiceImpl.java`

**问题描述**：

当某个 `ruleCode` 对应的序列记录不存在时，先查询（select）再插入（insert）存在 TOCTOU 竞争：两个并发线程可能同时判断"不存在"并尝试 insert，导致唯一键冲突异常（被 catch 包装为 `CODE_GENERATE_FAILED`）。

**影响**：低。仅在某 ruleCode 有史以来第一次生成时（系统首次使用该编码规则）发生，概率极低且已有 3 次重试机制。

**修复方案**：改为数据库层面的 `INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE`，或者在 `@Transactional` 中用 `SELECT FOR UPDATE` 加锁。

---

### P3-2：`FlowContext.validateNoExcessiveLoops()` — 边界值逻辑使用 `>` 而非 `>=`

**文件**：`yigongbao-module-flow/src/main/java/com/yigongbao/module/flow/context/FlowContext.java`

**问题描述**：

```java
if (context.getAuditRejectCount() > MAX_AUDIT_REJECT) {  // MAX=10，允许第11次才拦截
```

使用 `>` 而非 `>=`，实际效果是允许比配置上限多执行一次。若 `MAX_AUDIT_REJECT=10`，实际允许 11 次驳回才报错。

**修复方案**：将所有循环次数校验改为 `>=`，与注释和配置名称的语义保持一致。

---

### P3-3：`OrderController` 部分列表接口使用 GET + queryString，违反 CLAUDE.md 规范

**文件**：`yigongbao-module-order/src/main/java/com/yigongbao/module/order/main/controller/OrderController.java`

**问题描述**：

CLAUDE.md 明确规定：**list/page/tree 类型查询接口由 GET + queryString 方式改为 POST + JSON Body 方式**。但 OrderController 中部分列表接口仍使用 `@GetMapping` + `@RequestParam`。

**修复方案**：将查询列表接口改为 `@PostMapping` + `@RequestBody DTO`，与项目其他模块保持一致。

---

### P3-4：`USER_ROLE_NOT_FOUND(622)` 与 `ROLE_NOT_FOUND(626)` 语义重复

**文件**：`yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**问题描述**：

两个错误码语义相同（角色不存在），仅名称前缀不同（USER_ROLE vs ROLE）。使用方可能随意选择，导致同一业务场景返回不同错误码，前端处理逻辑复杂化。

**修复方案**：合并为一个 `ROLE_NOT_FOUND(626)`，删除 `USER_ROLE_NOT_FOUND(622)`，统一所有调用处。

---

## 五、数据库与代码对应关系审查

| 表名 | 对应 Entity | 审查结果 |
|------|------------|---------|
| `order_draft` | `OrderDraftEntity` | 字段对应正确；缺少 `item_count` 列（由代码计算），但代码填充逻辑有 bug（P1-3） |
| `order_item_draft` | `OrderItemDraftEntity` | 对应正确 |
| `order_main` | `OrderMainEntity` | 字段对应正确；phase/status 存在逻辑错误（P1-1） |
| `order_item` | `OrderItemEntity` | 对应正确 |
| `order_file` | `OrderFileEntity` | 对应正确；删除主单时未级联软删除（P1-5） |
| `order_flow_status_history` | `OrderFlowStatusHistoryEntity` | 对应正确 |
| `sys_user` | `UserEntity` | 对应正确 |
| `sys_role` | `RoleEntity` | 对应正确 |
| `sys_resource` | `ResourceEntity` | 物理删除绕过软删除（P2-3） |
| `sys_code_sequence` | `CodeSequenceEntity` | 对应正确；并发竞争风险（P3-1） |
| `basic_hospital` | `HospitalEntity` | 对应正确 |
| `basic_doctor` | `DoctorEntity` | 对应正确；N+1 查询（P2-1） |

所有业务表均使用函数唯一索引（`CASE WHEN is_deleted = 0 THEN field ELSE NULL END`），符合编码规范 §7.4 的要求。

---

## 六、问题汇总

| 编号 | 级别 | 模块 | 问题描述 | 修复优先级 |
|------|------|------|---------|-----------|
| P0-1 | P0 | module-order | FlowDebugController 无环境隔离 | 上线前必须修复 |
| P0-2 | P0 | common | ErrorCodeEnum 重复错误码 689 | 上线前必须修复 |
| P1-1 | P1 | module-flow | COMPLETE_POST_PROCESSING 导致 phase/status 不一致 | 本 PR 修复 |
| P1-2 | P1 | module-flow | REWORK 状态死锁，订单无法继续流转 | 本 PR 修复 |
| P1-3 | P1 | module-order | listDrafts itemCount 始终为 0 | 本 PR 修复 |
| P1-4 | P1 | module-order | removeDraft NPE 风险 | 本 PR 修复 |
| P1-5 | P1 | module-order | removeOrder 未校验状态且未清理关联数据 | 本 PR 修复 |
| P2-1 | P2 | module-basic | DoctorService fillExtraFields N+1 查询 | Follow-up |
| P2-2 | P2 | module-order | 订单文件相关方法 N+1 查询 | Follow-up |
| P2-3 | P2 | module-system | ResourceService 物理删除绕过软删除 | 本 PR 或 Follow-up |
| P2-4 | P2 | module-order | listOrders phase 硬编码为 1 | 本 PR 修复 |
| P2-5 | P2 | module-order | listOrders 缺少 dataScopeType 过滤 | 本 PR 修复 |
| P2-6 | P2 | module-basic | HospitalService listMyOptions 忽略 userId | Follow-up |
| P2-7 | P2 | module-system | UserService changePassword 未校验新旧密码相同 | Follow-up |
| P2-8 | P2 | module-system | X-Forwarded-For 可被伪造影响审计日志 | Follow-up |
| P3-1 | P3 | module-basic | CodeGenerator 高并发首次创建竞争条件 | 可选 |
| P3-2 | P3 | module-flow | FlowContext 边界值使用 > 而非 >= | 可选 |
| P3-3 | P3 | module-order | OrderController 列表接口违反 POST+JSON Body 规范 | 可选 |
| P3-4 | P3 | common | USER_ROLE_NOT_FOUND 与 ROLE_NOT_FOUND 语义重复 | 可选 |

---

## 七、下一步行动建议

**立即处理（上线前）**：
1. 为 `FlowDebugController` 添加 `@Profile({"dev","test"})` 注解
2. 修复 `ErrorCodeEnum` 中 689 重复错误码

**本迭代内修复（P1）**：
3. 补全 `FlowPhaseTransitionRules.decideNextPhaseAndStatus()` 中 `QC_IN_PROGRESS` 分支
4. 在 `FlowStatusTransitionRules.getAvailableActions()` 中补充 `REWORK` 状态的可用动作
5. 修复 `listDrafts()` VO 填充逻辑（直接在 convert lambda 中填充计算字段）
6. 修复 `removeDraft()` NPE
7. 为 `removeOrder()` 增加状态校验和关联数据软删除

**下个迭代跟进（P2）**：
8. 修复 `listOrders()` 的 `phase=1` 硬编码
9. 为 `listOrders()` 增加 dataScopeType 数据范围过滤
10. 批量化 Doctor/Order file 关联数据查询，消除 N+1
11. 修复 `ResourceService.deleteResource()` 改为软删除

---

**报告版本**：1.0  
**生成时间**：2026-04-08  
**审查人**：Claude Code (claude-sonnet-4-6)
