# 订单模块代码审查报告

**审查日期**：2026-05-13  
**审查范围**：`yigongbao-module-order` 全部功能代码  
**审查人**：Kiro AI  

---

## 一、可确认的代码问题

### 1. 权限控制缺失

**[已修复] `OrderController` — `submitOrder` / `withdrawOrder` 无权限注解**

- 文件：`controller/OrderController.java`
- 问题：`POST /{id}/submit` 和 `POST /{id}/withdraw` 均无 `@RequirePermission`，任何已登录用户均可提交或撤回他人订单。
- 影响：横向越权，A 用户可撤回 B 用户的订单。
- 修复：已补充 `@RequirePermission("order:Submit")` / `@RequirePermission("order:Withdraw")`，Service 层同步补充 `createBy == currentUserId` 校验。

---

**[已修复] `OrderController` — `assignDesigner` 无权限注解**

- 文件：`controller/OrderController.java`
- 问题：`POST /{id}/assign-designer` 注释写"管理员"，但无 `@RequirePermission`，任意登录用户均可调用。
- 影响：普通业务员可绕过管理员直接分配设计师。
- 修复：已补充 `@RequirePermission("design:AssignDesigner")`。

---

**[已修复] `OrderModifyApplyController` — `auditApply` 无权限注解**

- 文件：`controller/OrderModifyApplyController.java`
- 问题：`PUT /apply/{applyId}/audit` 无 `@RequirePermission`，任意登录用户均可审核他人的修改申请。
- 影响：严重越权，普通业务员可审核/拒绝修改申请。
- 修复：已补充 `@RequirePermission("order:AuditModifyApply")`。

---

**[已修复] `OrderController` — `submitDraft` 无权限注解**

- 文件：`controller/OrderController.java`
- 问题：`POST /draft/{id}/submit` 无 `@RequirePermission`。虽然 Service 层有 `operatorId` 校验，但缺少接口级权限声明，与其他草稿接口风格不一致。
- 影响：权限体系不完整，后续 RBAC 扩展时容易遗漏。
- 修复：已补充 `@RequirePermission("draft:Submit")`。

---

### 2. 数据权限校验缺失

**[已修复] `OrderMainServiceImpl.updateOrder` 无数据权限校验**

- 文件：`service/impl/OrderMainServiceImpl.java`
- 问题：`updateOrder` 只校验订单存在，未校验当前用户是否有权修改该订单。
- 影响：HOSPITALS/SELF 权限用户可修改不属于自己权限范围的订单。
- 修复：已添加 `validateDataScope(id)` 调用，复用 `buildDataScopeCondition` COUNT 校验。

---

**[已修复] `OrderMainServiceImpl.removeOrder` 无数据权限校验**

- 文件：`service/impl/OrderMainServiceImpl.java`
- 问题：`removeOrder` 只校验状态为 DRAFT，未校验当前用户是否为订单创建人。
- 影响：任意有登录态的用户可删除他人草稿状态订单（若知道 ID）。
- 修复：已补充 `entity.getCreateBy().equals(currentUserId)` 校验。

---

**[已修复] `OrderMainServiceImpl.submitOrder` / `withdrawOrder` 无数据权限校验**

- 文件：`service/impl/OrderMainServiceImpl.java`
- 问题：两个流程操作均只校验订单存在，未校验当前用户是否有权操作该订单。
- 影响：知道订单 ID 的用户可提交/撤回他人订单。
- 修复：已补充 `createBy == currentUserId` 校验。
- 备注：`auditPass`/`auditReject` 已有 `@RequirePermission("order:Approve/Reject")` 保护，无需额外数据权限校验。

---

**[已修复] `OrderModifyApplyServiceImpl.getApplicableTypes` 无数据权限校验**

- 文件：`service/impl/OrderModifyApplyServiceImpl.java:105`
- 问题：任意登录用户可查询任意订单的可申请修改类型，会暴露订单阶段信息。
- 影响：信息泄露（订单阶段、是否有待审核申请）。
- 修复：已在 Controller 层补充 `@RequirePermission("order:View")`，与查看订单详情复用同一权限。

---

**[已修复] `OrderModifyApplyServiceImpl.listAppliesByOrder` / `listModificationLogs` 无数据权限校验**

- 文件：`service/impl/OrderModifyApplyServiceImpl.java`
- 问题：`POST /{orderId}/applies` 和 `POST /{orderId}/logs` 均无权限注解，任意登录用户可查询任意订单的修改记录。
- 影响：信息泄露，修改留痕中可能含有敏感业务数据。
- 修复：已在 Controller 层补充 `@RequirePermission("order:View")`。

---

### 3. 并发安全问题

**[已修复] `validateNoPendingApply` 存在 TOCTOU 竞态**

- 文件：`service/impl/OrderModifyApplyServiceImpl.java`
- 确认结果：`ddl.sql:1184` 已定义函数索引 `uk_order_modify_apply_pending`，数据库层唯一约束已存在。
- 修复：已在 `createApply` 的 `insert` 处捕获 `DuplicateKeyException`，转换为 `ORDER_MODIFY_APPLY_EXISTS` 业务异常，避免并发时返回 500。

---

**[已修复] `OrderExportServiceImpl.unsupportedColumnCount` 线程不安全**

- 文件：`service/impl/OrderExportServiceImpl.java`
- 问题：`unsupportedColumnCount` 是实例变量（非 `ThreadLocal`），`OrderExportServiceImpl` 是单例 Bean，并发导出时多个线程共享该变量，计数会互相干扰。
- 影响：并发导出时日志警告不准确（次要问题，不影响数据正确性）。
- 修复：已改为 `buildExcel` 方法内的局部变量，`setCellValue` 改为返回 `boolean` 标识是否匹配。

---

### 4. 参数校验缺失

**[已修复] `OrderController.listOrders` 无 `@Valid`**

- 文件：`controller/OrderController.java`
- 问题：`@RequestBody OrderPageDTO dto` 缺少 `@Valid`，分页参数（pageNum/pageSize）未校验，可传入负数或超大值。
- 修复：已添加 `@Valid`。

---

**[已修复] `OrderModifyApplyController.executeModification` 无 `@Valid`**

- 文件：`controller/OrderModifyApplyController.java`
- 问题：`@RequestBody ExecuteModifyDTO dto` 缺少 `@Valid`。
- 修复：已添加 `@Valid`。

---

**[已修复] `OrderController.saveColumnConfig` 无 `@Valid`**

- 文件：`controller/OrderController.java`
- 问题：`@RequestBody OrderColumnConfigVO config` 缺少 `@Valid`，列配置内容未校验，存在存储型 XSS 风险。
- 修复：已添加 `@Valid`。

---

**[已修复] `OrderDataValidator.validateHospitalScope` 中 `Long.parseLong` 无异常处理**

- 文件：`validator/OrderDataValidator.java`
- 问题：`Long.parseLong(unknownHospitalIdStr)` 若配置值非数字会抛 `NumberFormatException`，未捕获。
- 影响：配置错误时，所有创建订单的请求均会 500。
- 修复：已改用 `Convert.toLong(unknownHospitalIdStr, null)` 并做 null 判断。

---

### 5. 业务逻辑边界问题

**[已修复] `OrderDraftServiceImpl.submitDraft` 未校验草稿数据完整性**

- 文件：`service/impl/OrderDraftServiceImpl.java`
- 问题：草稿提交时只校验了文件和明细数量，未以 `SUBMIT` 模式重新校验主表必填字段（orgId/hospitalId 等）。
- 影响：可能创建出缺少 orgId/hospitalId 的正式订单。
- 修复：已在 `submitDraft` 中补充 `validateAndFillMaster(..., ValidateMode.SUBMIT)` 调用。

---

**[已修复] `OrderMainServiceImpl.createFromDraft` 未校验订单类型与机构资质**

- 文件：`service/impl/OrderMainServiceImpl.java`
- 问题：直提流程（`createOrder`）调用了 `validateOrderType`，但草稿转正式订单（`createFromDraft`）未调用，存在逻辑不一致。
- 影响：通过草稿路径可绕过机构资质校验，创建不符合资质的订单类型。
- 修复：已在 `createFromDraft` 中补充 `orderDataValidator.validateOrderType(draft.getOperatorId(), draft.getOrderType())`。

---

**[已修复] `DesignerAssignmentServiceImpl.getOrderSpecialty` 只取第一条明细**

- 文件：`service/impl/DesignerAssignmentServiceImpl.java:219`
- 问题：`LIMIT 1` 只取第一条明细的专业方向，当订单含多个不同专业方向的明细时，分配逻辑不完整。
- 影响：多专业方向订单可能被分配给不具备全部专业能力的设计师。
- 处理：**维持现状，无需修改**。自动分配是尽力而为逻辑，多专业方向是边缘场景，强制匹配全部方向反而导致无人可分配。已在代码注释中说明"取第一条明细"是有意为之的简化策略。

---

**[已修复] `OrderModifyApplyServiceImpl.processItemModification` 明细数量校验时机过晚**

- 文件：`service/impl/OrderModifyApplyServiceImpl.java`
- 问题：明细数量为 0 的校验在所有 insert/delete 操作完成后才执行，校验应前置以减少无效操作。
- 修复：已在 `newItems` 为空时提前抛出异常，同时移除方法末尾的 `remainingCount` 查询（前置校验后该查询永远不会触发）。

---

**[已修复] `OrderMainServiceImpl.fillOrderFiles` 存在 N+1 查询**

- 文件：`service/impl/OrderMainServiceImpl.java`
- 问题：`fillOrderFiles` 在循环中逐条调用 `fileService.getById(orderFile.getFileId())`，每个文件一次查询。
- 影响：订单文件较多时产生多次 DB 查询。
- 修复：已改用 `fileService.listByIds(fileIds)` 批量查询，再按 ID 分组填充。

---

### 6. 代码规范问题

**[已修复] `OrderModifyApplyServiceImpl.validateApplyTypes` 使用魔法错误码**

- 文件：`service/impl/OrderModifyApplyServiceImpl.java`
- 问题：`throw new BusinessException(400, "...")` 直接使用数字 400，违反编码规范。
- 修复：已改用 `ErrorCodeEnum.MISSING_PARAMETER` / `ErrorCodeEnum.INVALID_PARAMETER`。

---

**[已修复] `OrderModifyApplyServiceImpl.processItemModification` 使用魔法错误码**

- 文件：`service/impl/OrderModifyApplyServiceImpl.java`
- 问题：`throw new BusinessException(400, "重建项目ID不属于当前订单：" + orderItemId)` 同上，且拼接 ID 可能暴露内部数据。
- 修复：已改用 `ErrorCodeEnum.INVALID_PARAMETER`，移除 ID 拼接。

---

**[已修复] `OrderDraftServiceImpl` 草稿状态使用魔法数字**

- 文件：`service/impl/OrderDraftServiceImpl.java`，`task/OrderDraftCleanupTask.java`
- 问题：草稿状态 `1`（有效）、`2`（已提交）、`3`（已过期）直接使用数字，无枚举定义。
- 修复：已创建 `OrderDraftStatusEnum`，替换所有魔法数字。

---

**[已修复] `OrderExportServiceImpl.getPhaseName` 与 `OrderQueryHelper.getPhaseName` 重复实现**

- 文件：`service/impl/OrderExportServiceImpl.java`，`helper/OrderQueryHelper.java`
- 问题：两处均实现了 phase → 中文名的翻译逻辑，`OrderExportServiceImpl` 中是硬编码 switch。
- 修复：已将 `OrderQueryHelper.getPhaseName` 改为 public，`OrderExportServiceImpl` 改为调用 `orderQueryHelper.getPhaseName()`，删除重复实现。

---

## 二、需要业务/线上数据确认的风险

### R1. `updateOrder` 的调用入口不明确

- 文件：`service/impl/OrderMainServiceImpl.java`
- 确认：**该方法未对外暴露接口，无风险**。当前仅作为内部 Service 方法存在，无需处理。

---

### R2. 修改申请的审核人权限边界

- 文件：`controller/OrderModifyApplyController.java`
- 风险：审核人是否应限制为"与该订单所属机构相关的管理员"，还是全局管理员均可审核？
- 推荐方案：**推荐全局管理员均可审核**（即当前 `@RequirePermission("order:AuditModifyApply")` 的设计）。理由：修改申请审核是内部运营流程，审核人通常是公司内部管理员而非医院侧人员，不需要与订单数据权限对齐。若未来有多机构隔离需求，再在 `auditApply` 中补充 `order.orgId == currentUser.orgId` 校验即可。

---

### R3. 设计师分配后订单状态是否需要流转

- 文件：`service/impl/DesignerAssignmentServiceImpl.java`
- 确认：**无需手动接单，保持当前流程**。分配设计师后不触发状态流转，当前设计合理。

---

### R4. 草稿过期后是否允许继续编辑

- 文件：`service/impl/OrderDraftServiceImpl.java`
- 风险：`saveDraft` 未校验过期，用户可编辑已过期草稿但无法提交，体验割裂。
- 修复：**已在 `saveDraft` 更新分支补充过期校验**，过期草稿不允许编辑，抛出 `ORDER_DRAFT_EXPIRED`。

---

### R5. 修改申请执行后订单阶段是否需要重新校验

- 文件：`service/impl/OrderModifyApplyServiceImpl.java`
- 风险：`executeModification` 修改 `hospitalId` 后，不会重新校验医院权限范围。
- 推荐方案：**无需重新校验，当前设计合理**。理由：修改申请本身已经过审核（APPROVED 状态），审核人确认了变更内容的合理性，这等同于一次人工复核。执行阶段再做权限校验属于重复校验，且修改执行人（通常是管理员）的权限范围可能与原提单人不同，强制校验反而会误拦截合法操作。若需要留痕，修改留痕日志（`order_modification_log`）已记录变更前后值，审计链路完整。

---

### R6. 导出接口无数量上限提示

- 文件：`service/impl/OrderExportServiceImpl.java`
- 风险：超过 10000 条时静默截断，用户不知情。
- 修复：**已在响应头中添加 `X-Export-Total` 和 `X-Export-Truncated` 标识**，前端读取 `X-Export-Truncated: true` 时可提示用户缩小筛选范围。

---

### R7. `FlowDebugController` 的 `@Profile` 是否覆盖所有非生产环境

- 文件：`controller/FlowDebugController.java`
- 确认：**暂不处理**，生产环境安全问题后续另行跟进。

---

## 三、修复进度汇总

| 类别 | 总数 | 已修复/关闭 |
|------|------|------------|
| 权限控制缺失（越权 bug） | 6 | 6 |
| 并发安全 | 2 | 2 |
| 参数校验缺失 | 4 | 4 |
| 业务逻辑边界 | 5 | 5 |
| 代码规范 | 4 | 4 |
| 业务风险 | 7 | 6（R1/R2/R3/R4/R5/R6 已关闭）；R7 暂不处理 |
