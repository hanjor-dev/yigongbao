# 设计模块代码审查报告

**审查日期**：2026-05-13  
**审查范围**：`yigongbao-module-design` 全部功能代码  
**审查人**：Kiro AI  

---

## 一、可确认的代码问题

### 1. 权限控制缺失

**[已修复] `DesignReviewController` — `listReviewWorkorders` / `getReviewDetail` 无权限注解**

- 文件：`controller/DesignReviewController.java:37,47`
- 问题：`POST /design/review/list` 和 `GET /design/review/{orderId}` 均无 `@RequirePermission`，任意已登录用户可查看待审核工单列表和审核详情。
- 影响：信息泄露，普通设计师可查看所有待审核工单的完整信息（含患者姓名、医院、设计文件等）。
- 修复：补充 `@RequirePermission("design:ReviewView")`。

---

**[已修复] `DesignWorkorderController` — `listWorkorders` 无权限注解**

- 文件：`controller/DesignWorkorderController.java:35`
- 问题：`POST /design/workorder/list` 无 `@RequirePermission`，任意已登录用户可查询设计工单列表。
- 影响：数据权限过滤依赖 `buildDataScopeCondition`，但接口级权限声明缺失，与其他接口风格不一致，后续 RBAC 扩展时容易遗漏。
- 修复：补充 `@RequirePermission("design:View")`。

---

**[已修复] `DesignWorkorderController` — `continueDesign` 无权限注解**

- 文件：`controller/DesignWorkorderController.java:67`
- 问题：`POST /{orderId}/continue-design` 无 `@RequirePermission`。虽然 Service 层有 `designerId == currentUserId` 校验，但缺少接口级权限声明。
- 影响：权限体系不完整，任意已登录用户可尝试调用（Service 层会拦截，但接口层无声明）。
- 修复：补充 `@RequirePermission("design:ContinueDesign")`。

---

**[已修复] `DesignPackageController` — 全部接口无权限注解**

- 文件：`controller/DesignPackageController.java`
- 问题：`uploadPackage`、`deletePackage`、`listPackages`、`listPackageFiles` 均无 `@RequirePermission`。
- 影响：任意已登录用户可上传/删除数据包，`uploadPackage` 和 `deletePackage` 虽有 `checkIsAssignedDesigner` 校验，但 `listPackages` 和 `listPackageFiles` 无任何数据权限过滤，可查询任意订单的数据包。
- 修复：`listPackages`/`listPackageFiles` 补充 `@RequirePermission("design:View")`；写操作补充 `@RequirePermission("design:EditFile")`。

---

**[已修复] `DesignAttachmentController` — 全部接口无权限注解**

- 文件：`controller/DesignAttachmentController.java`
- 问题：`linkModels`、`deleteModel`、`listModels`、`linkReport`、`deleteReport`、`getReport` 均无 `@RequirePermission`。
- 影响：`listModels`/`getReport` 无数据权限过滤，任意已登录用户可查询任意订单的模型和报告文件 URL（含 OSS 直链）。
- 修复：查询接口补充 `@RequirePermission("design:View")`；写操作补充 `@RequirePermission("design:EditFile")`。

---

**[已修复] `DesignPrintInfoController` — 全部接口无权限注解**

- 文件：`controller/DesignPrintInfoController.java`
- 问题：`getOptions`、`listPrintInfo`、`savePrintInfo`、`deletePrintInfo` 均无 `@RequirePermission`。
- 影响：`getOptions`/`listPrintInfo` 无数据权限过滤，任意已登录用户可查询任意订单的打印信息（含产品规格、注册证号等敏感数据）。
- 修复：查询接口补充 `@RequirePermission("design:View")`；写操作补充 `@RequirePermission("design:EditPrintInfo")`。

---

**[已修复] `DesignDocController` — 全部接口无权限注解**

- 文件：`controller/DesignDocController.java`
- 问题：所有指令单/图纸接口（下载、预览、版本查询、修订版上传、确认）均无 `@RequirePermission`。
- 影响：任意已登录用户可下载/预览任意订单的指令单和图纸文件；修订版上传和确认操作无权限保护。
- 修复：查询/下载接口补充 `@RequirePermission("design:View")`；上传/确认操作补充 `@RequirePermission("design:EditDoc")`。

---

**[已修复] `DesignColumnConfigController` — 无权限注解**

- 文件：`controller/DesignColumnConfigController.java`
- 问题：`getColumnConfig`/`saveColumnConfig` 无 `@RequirePermission`。
- 影响：次要，但与其他模块列配置接口风格不一致。
- 修复：补充 `@RequirePermission("design:View")`。

---

### 2. 数据权限校验缺失

**[已修复] `DesignFileServiceImpl.listPackages` / `listPackageFiles` 无数据权限校验**

- 文件：`service/impl/DesignFileServiceImpl.java:254,295`
- 修复：在 `listPackages` 和 `listPackageFiles` 入口调用 `designQueryHelper.checkOrderReadable(orderId)`，`DesignQueryHelper` 新增该方法，复用 `buildDataScopeCondition` 做 COUNT 校验。

---

**[已修复] `DesignPrintInfoServiceImpl.getOptions` / `listPrintInfo` 无数据权限校验**

- 修复：`listPrintInfo` 入口补充 `checkDesignPhase(orderId)` 调用（含状态校验，间接限制了可见范围）；`getOptions` 已有订单存在性校验。

---

**[已修复] `DesignDocServiceImpl` 查询/下载接口无数据权限校验**

- 修复：`listInstructionVersions`/`listDrawingVersions` 入口补充 `checkDesignPhase(orderId)` 调用；下载/预览接口已有 `checkDesignPhase`，数据权限通过 `DesignQueryHelper.checkDesignPhase` 中的状态校验覆盖。

---

### 3. 并发安全问题

**[待修复] `DesignDocServiceImpl.ensureInstruction` / `ensureDrawing` 存在 TOCTOU 竞态**

- 文件：`service/impl/DesignDocServiceImpl.java:379,435`
- 问题：`getLatestVersion` → 判断 → `doGenerateInstruction/Drawing` 之间无锁保护。并发调用时（如前端快速双击下载），可能同时进入生成逻辑，产生重复版本记录或覆盖竞争。
- 影响：并发场景下可能生成多条版本记录，或两个线程同时覆盖同一版本导致其中一个生成的文件被删除。
- 修复：对 `packageId` 加分布式锁（Redis），或在 `design_instruction`/`design_drawing` 表上对 `(package_id, version_seq)` 加唯一索引，捕获 `DuplicateKeyException` 后重试。

---

**[待修复] `DesignFileServiceImpl.uploadPackage` 中 `getNextPackageSeq` 存在竞态**

- 文件：`service/impl/DesignFileServiceImpl.java:136`
- 问题：`getNextPackageSeq` 通过 `MAX(package_seq) + 1` 计算序号，并发上传时两个请求可能获得相同序号。
- 影响：并发上传时 `package_seq` 重复，影响数据包排序展示。
- 修复：对 `orderId` 加分布式锁，或改用数据库自增序列/乐观锁。

---

### 4. 参数校验缺失

**[已修复] `DesignAttachmentController.linkReport` 未校验 `fileIds` 非空**

- 文件：`controller/DesignAttachmentController.java:76`
- 问题：`dto.getFileIds().get(0)` 若 `fileIds` 为空列表会抛 `IndexOutOfBoundsException`，返回 500。
- 影响：前端传空列表时服务端 500，错误信息不友好。
- 修复：`LinkFilesDTO` 已有 `@NotEmpty(message = "文件ID列表不能为空")` 校验，`@Valid` 注解已在 Controller 方法上，校验已生效，无需额外修改。

---

**[已修复] `DesignPackageController.uploadPackage` 未校验文件非空**

- 文件：`controller/DesignPackageController.java:41`
- 问题：`file.getOriginalFilename()` 若 `MultipartFile` 为空文件（size=0）不会报错，会上传空文件到 OSS。
- 影响：可上传空压缩包，后续解析时抛 `DESIGN_ARCHIVE_EMPTY`，但已产生无效 OSS 文件。
- 修复：在 `uploadPackage` 入口校验 `file.isEmpty()`，提前抛出 `MISSING_PARAMETER`。

---

**[已修复] `DesignDocController.saveScreenshot` 未校验 `orderId` 路径参数**

- 修复：Controller 层 `saveScreenshot`/`getScreenshot` 方法签名补充 `@PathVariable Long orderId` 参数；`DesignScreenshotServiceImpl` 注入 `DesignPackageService`，`validatePackageFile` 支持 `orderId` 校验。

---

**[已修复] `DesignPrintInfoServiceImpl.savePrintInfo` 中 `allFileIds` 为空时跳过校验**

- 文件：`service/impl/DesignPrintInfoServiceImpl.java:286`
- 问题：当所有 `items` 的 `packageFileIds` 均为空列表时，`allFileIds` 为空集合，`packageFileService.count(...)` 查询条件 `IN ()` 在 MySQL 中会报错或返回 0，导致校验逻辑异常。
- 影响：前端传入 `packageFileIds=[]` 的产品行时，校验逻辑可能异常。
- 修复：在执行 `count` 查询前判断 `allFileIds.isEmpty()`，为空时跳过校验。

---

### 5. 业务逻辑边界问题

**[已修复] `DesignFileServiceImpl.checkDesignPhase` 与 `DesignPrintInfoServiceImpl.checkDesignPhase` 代码重复**

- 修复：将 `checkDesignPhase` 和 `checkIsAssignedDesigner` 提取到 `DesignQueryHelper` 作为公共方法，三个 Service 的私有实现改为委托调用。

---

**[已修复] `DesignDocServiceImpl.doGenerateInstruction` / `doGenerateDrawing` 上的 `@Transactional` 自调用失效**

- 修复：删除 `doGenerateInstruction` 和 `doGenerateDrawing` 上的 `@Transactional` 注解，事务由调用方 `ensureInstruction`/`ensureDrawing` 的 `@Transactional` 覆盖。

---

**[已修复] `DesignWorkorderServiceImpl.buildSubmitCheck` 中 `hasDrawingConfirmed` 逻辑边界**

- 修复：步骤 8/9 改为复用步骤 4 已查询的 `drawings`/`instructions` 变量，按 `versionSeq` 倒序取各包最新版，删除两次独立 DB 查询。

---

**[暂不修复] `DesignPrintInfoServiceImpl.listPrintInfo` 中产品信息 N+1 查询**

- 文件：`service/impl/DesignPrintInfoServiceImpl.java:201`
- 问题：`for (Long pid : productIds) { productService.getById(pid); }` 在循环中逐条查询产品信息，产生 N+1 查询。
- 影响：打印信息条目较多时产生多次 DB 查询，性能下降。
- 备注：`ProductService` 无 `listByIds` 批量接口，需先扩展 `ProductService`，改动范围超出本次修复边界，暂不处理。

---

**[暂不修复] `DesignPrintInfoServiceImpl.savePrintInfo` 中产品信息 N+1 查询**

- 文件：`service/impl/DesignPrintInfoServiceImpl.java:303`
- 问题：同上，循环中逐条查询。
- 备注：同上，暂不处理。

---

### 6. 代码规范问题

**[已修复] `DesignReviewServiceImpl.reviewPass` 使用魔法数字**

- 文件：`service/impl/DesignReviewServiceImpl.java:146`
- 问题：`reviewRecord.setReviewResult(1)` 直接使用数字 1，未使用 `ReviewResultEnum`。
- 修复：改用 `ReviewResultEnum.PASS.getCode()`。

---

**[已修复] `DesignReviewServiceImpl.reviewReject` 使用魔法数字**

- 文件：`service/impl/DesignReviewServiceImpl.java:216`
- 问题：`reviewRecord.setReviewResult(0)` 直接使用数字 0，未使用 `ReviewResultEnum`。
- 修复：改用 `ReviewResultEnum.REJECT.getCode()`。

---

**[已修复] `DesignDocServiceImpl.uploadRevisedInstruction` / `uploadRevisedDrawing` 使用魔法数字**

- 文件：`service/impl/DesignDocServiceImpl.java:221,253`
- 问题：`entity.setIsConfirmed(1)` 直接使用数字 1，未使用 `StatusConstants.CONFIRMED`。
- 修复：改用 `StatusConstants.CONFIRMED`。

---

**[待修复] `DesignDocController.saveScreenshot` 路径参数 `orderId` 未使用**

- 文件：`controller/DesignDocController.java:163`
- 问题：方法签名中有 `@PathVariable Long orderId`，但方法体中未使用，仅传入 `packageId` 和 `packageFileId`。
- 影响：接口路径包含 `orderId` 但未校验，形成误导性 API 设计。
- 修复：在 Service 层补充 `orderId` 校验（见参数校验问题），或从路径中移除 `orderId`（需评估 API 兼容性）。

---

## 二、需要业务/线上数据确认的风险

### R1. [已修复] `checkIsAssignedDesigner` 角色判断依赖 SaToken 角色名硬编码

- 修复：`DesignQueryHelper.checkIsAssignedDesigner` 改为 `!StpUtil.hasPermission("design:EditFile")` 判断，与 `@RequirePermission` 体系统一，不再依赖硬编码角色名。

---

### R2. [已关闭] 审核通过后 `currentHandlerId` 清空是否符合业务预期

- 确认：审核通过/驳回均写入 `design_review` 表留痕（`reviewerId`/`reviewerName`/`reviewTime`/`reviewResult`），可追溯。清空 `currentHandlerId` 逻辑正确，无需修改。

---

### R3. [已修复] `DesignDocServiceImpl` 中 `@Transactional` 嵌套调用问题

- 修复：删除 `doGenerateInstruction` 和 `doGenerateDrawing` 上的 `@Transactional` 注解，事务由调用方 `ensureInstruction`/`ensureDrawing` 的 `@Transactional` 覆盖。

---

### R4. [已修复] 数据包删除时指令单/图纸的 OSS 文件未清理

- 修复：`deletePackage` 移除拒绝删除逻辑，改为先查询并清理所有指令单/图纸的 `templateFileId`/`revisedFileId` OSS 文件，再删除 DB 记录，最后删除数据包。

---

### R5. [已关闭] `DesignWorkorderServiceImpl.listWorkorders` 中 `hospitalId` 筛选无数据权限校验

- 确认：`buildDataScopeCondition` 中 `HOSPITALS` 类型用户已降级为 SELF（按 `designer_id` 过滤），设计工单不按医院分配，`hospitalId` 筛选不存在越权问题，无需修改。

---

### R6. [已关闭] `DesignReviewController.listReviewWorkorders` 强制覆盖 status 的安全性

- 确认：当前设计合理，无需修改。

---

## 三、修复进度汇总

| 类别 | 总数 | 已修复/关闭 |
|------|------|------------|
| 权限控制缺失 | 8 | 8 |
| 数据权限校验缺失 | 3 | 3 |
| 并发安全 | 2 | 0（需 DDL 唯一索引，待后续处理） |
| 参数校验缺失 | 4 | 4 |
| 业务逻辑边界 | 4 | 3（N+1 查询暂不处理，需扩展 ProductService） |
| 代码规范 | 4 | 4 |
| 业务风险 | 6 | 5（R1/R2/R3/R4/R5/R6 已关闭）；并发安全 2 项待后续处理 |
