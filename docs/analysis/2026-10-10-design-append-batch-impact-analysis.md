# 追加设计批次功能影响域与实施方案

## 1. 文档信息

- 文档名称：追加设计批次功能影响域与实施方案
- 创建日期：2026-10-10
- 适用项目：医工宝后端、医工宝前端
- 后端实施分支：`codex/append-design-batch-backend`
- 前端实施分支：`codex/append-design-batch-frontend`
- 文档定位：需求分析、影响域评估、数据模型、接口契约、异常边界和实施复核记录；代码实现以当前前后端工作区为准，本文件同时标注已实现内容与待验收项
- 关联历史文档：`docs/analysis/2026-10-09-design-attachment-change-impact-analysis.md`

本文件针对“设计完成及后续状态允许追加上传打印文件数据包，并为新增数据包填写打印信息，随后仅为新增数据包生成新的生产流转数据”的新需求，统一记录后端、前端、数据库和生产流程影响。

## 2. 已确认的业务规则

### 2.1 追加入口与原设计入口分离

设计阶段原有“上传设计数据包”入口继续服务于正常设计流程。

订单达到设计完成及后续状态后，用户必须从新的“追加设计批次”入口进行操作。追加上传不得复用原入口的普通设计完成流程语义。

首次进入“追加设计批次”时创建追加批次；如果订单已经存在未完成的追加批次，则复用该批次，不再创建第二个未完成批次。一个追加批次可以上传一个或多个设计数据包，也可以在未完成期间多次返回入口继续上传。只有当前批次完成后，下一次追加才创建新的批次。对用户而言，多包一次追加和分多次追加的最终结果一致，系统必须保证每个追加批次内部数据完整、隔离、可追踪。

### 2.2 追加批次操作流程

```text
创建追加设计批次
    ↓
上传一个或多个设计数据包
    ↓
上传阶段和打印信息阶段可交替进行，批次未完成前仍可继续追加数据包
    ↓
明确提交“完成追加设计批次”，并校验全部数据包打印信息完成
    ↓
订单状态回退到设计完成（2030）
    ↓
仅针对当前追加批次生成生产记录、生产产品和生产工序
```

在当前追加批次中，只要还有一个数据包未完成打印信息，订单不得回退到设计完成，也不得开始生成该批次的生产阶段数据。

### 2.3 数据包和打印信息修改规则

- 追加数据包上传后、尚未完成打印信息时，允许删除。
- 追加批次尚未完成时，已经填写过打印信息的数据包允许再次修改。
- 追加批次全部完成、订单状态回退到设计完成后，当前批次数据包和打印信息统一锁定。
- 已完成批次的数据包不能再次选择填写或修改。
- 当前批次完成后，后续再次追加时创建新的追加批次，不修改历史批次；当前批次未完成时再次进入入口必须复用原批次。
- 原设计阶段在订单完成设计前的既有行为保持不变：原数据包仍可按照现有设计阶段规则修改打印信息。

### 2.4 文件重复校验规则

不要求校验新旧数据包之间的文件名、文件内容或文件部分重叠情况。

设计数据包为压缩文件，上传流程可以继续按照现有逻辑解析新压缩包并建立包内文件索引，但不得为了本需求额外解压历史数据包进行重复比对，也不得因新旧数据包存在重复文件而拒绝上传。

### 2.5 订单状态和生产数据规则

- 设计完成及后续状态的订单均允许创建追加批次，包括已完成状态订单。
- 当前订单状态在追加批次未完成期间保持不变。
- 当前追加批次所有数据包打印信息完成后，订单状态回退到设计完成 `2030`。
- 状态回退和追加批次生产数据生成需要作为一次受控业务操作处理。
- 只为当前追加批次的数据包生成生产数据。
- 已有数据包、已有生产记录、已有生产工序和已经完成的历史生产流程不重新生成、不重置、不回退。
- 新生产记录仍按当前实现的“数据包 + 产品”规则生成：同一数据包下不同设计产品分别生成生产记录。
- 每条新生产记录继续生成对应的 `production_product` 和 `production_process`。
- 新生产记录初始状态为设计完成，生产部门下载后按现有生产流程继续流转。
- 新生产记录仍使用原订单信息，包括订单号、医院、科室、医生、患者、邮寄和交付等冗余信息。
- 新增生产记录、产品、工序和相关台账均新增记录，不覆盖历史记录。

## 3. 当前代码流程调查结论

### 3.1 设计数据包上传

后端入口：

- `POST /design/package/upload`
- 文件：`yigongbao-module-design/.../controller/DesignPackageController.java`
- 实现：`yigongbao-module-design/.../service/impl/DesignFileServiceImpl.java`

当前上传逻辑包括：

1. 文件非空校验；
2. 经典案例订单保护；
3. 设计阶段状态校验；
4. 文件扩展名和大小校验；
5. 文件名格式校验；
6. 数据包编号、序号和订单关联处理；
7. 解析压缩包并写入 `design_package_file`；
8. 写入 `design_package`。

当前上传逻辑只允许设计阶段状态，设计完成及生产后续状态会被拒绝，需要增加独立的追加批次上传语义和状态权限。

### 3.2 当前打印信息行为

后端入口：

- `GET /design/{orderId}/package/{packageId}/print-info`
- `POST /design/{orderId}/package/{packageId}/print-info`
- `DELETE /design/{orderId}/package/{packageId}/print-info/{printInfoId}`
- 文件：`yigongbao-module-design/.../controller/DesignPrintInfoController.java`
- 实现：`yigongbao-module-design/.../service/impl/DesignPrintInfoServiceImpl.java`

当前保存接口是“整包替换”逻辑：

- 删除当前数据包的旧 `design_product_file`；
- 删除当前数据包的旧 `design_product`；
- 按请求重新插入打印信息及文件关联；
- 更新数据包级字段；
- 重置指令单、图纸确认状态。

当前 `listPrintInfo`、`savePrintInfo`、`deletePrintInfo` 使用 `checkDesignPhase`，因此设计完成及后续状态无法操作。设计阶段中已经填写过打印信息的数据包仍可继续替换、删除和修改。

### 3.3 当前设计完成和生产记录生成

设计完成入口位于：

- `yigongbao-module-design/.../service/impl/DesignWorkorderServiceImpl.java`

正常完成设计时：

1. 校验订单处于设计中；
2. 校验设计数据包、打印信息、指令单、图纸及确认状态；
3. 调用普通状态机完成 `2020 → 2030`；
4. 更新订单；
5. 发布 `DesignCompletedEvent`。

生产监听器位于：

- `yigongbao-module-production/.../listener/DesignCompletedListener.java`

正常完成设计时，监听器会查询订单下全部数据包，按数据包下产品分组，并生成：

- `production_record`；
- `production_product`；
- `production_process`。

追加完成时复用同一监听器，但事件携带明确的 `packageIds`，监听器只查询这些数据包；生产记录从数据包上的 `batchId` 继承追加批次关联。监听器已有按数据包和产品的幂等判断，不能通过再次发布不带范围的普通设计完成事件来处理追加批次。

### 3.4 当前订单状态机

文件：

- `yigongbao-module-flow/.../rules/FlowStatusTransitionRules.java`
- `yigongbao-module-flow/.../enums/FlowActionEnum.java`

当前主要规则：

```text
设计中 2020 → 设计完成 2030
设计完成 2030 → 待打印 3010
```

没有“生产中或已完成 → 设计完成”的普通状态转换，因此不能简单扩大 `COMPLETE_DESIGN` 的适用范围。应新增受控的追加批次完成动作，或提供专用的订单追加设计完成服务，确保状态历史、并发控制和生产生成保持一致。

### 3.5 当前前端流程

主要文件：

- `frontend/med-tech/src/views/business/design.vue`
- `frontend/med-tech/src/views/business/designComponents/fileDialog.vue`
- `frontend/med-tech/src/views/business/designComponents/designPrintInfoDialog.vue`

当前前端：

- 文件上传弹窗直接调用上传接口；
- 数据包上传后立即落库；
- 打印信息弹窗加载全部数据包；
- 默认选中最后一个数据包；
- 任意数据包都可以选择；
- 已填写数据包仍然可以修改；
- 后续状态菜单虽然显示上传和填写打印信息，但后端当前会拒绝。

追加需求需要新增批次入口及批次上下文，不能仅修改旧入口的状态判断。

## 4. 数据库和数据模型影响

### 4.1 现有核心表

```text
order_main
order_flow_status_history
design_package
design_package_file
design_product
design_product_file
production_record
production_product
production_process
production_process_transfer
```

本地数据库调查到的当前规模约为：

- `design_package`：约 956 条；
- `design_product`：约 1681 条；
- `production_record`：约 843 条；
- `production_product`：约 1570 条；
- `production_process`：约 4428 条；
- `order_flow_status_history`：约 11516 条。

当前订单和生产记录中已经存在设计完成、待打印、打印中、打印完成、后处理、仓储、已完成等状态数据，说明本需求会直接作用于已经存在历史生产数据的订单，不能采用覆盖旧记录的方案。

### 4.2 推荐新增追加批次表

建议新增：

```text
design_package_batch
```

建议字段：

```text
id
order_id
batch_no
batch_type
status
source_order_status
created_by
create_time
update_time
completed_time
version
```

推荐状态：

```text
UPLOADING
PRINT_INFO_EDITING
COMPLETED
CANCELLED
```

`batch_type` 可区分：

```text
NORMAL
ADDITIONAL
```

历史正常设计流程可以不强制迁移为追加批次；如果需要完整追溯，可以为历史设计数据包补建一个初始批次。

### 4.3 原表继续保存新增业务数据

新增批次表不是原业务数据的替代表。

新增数据仍然写入原表：

```text
design_package
design_package_file
design_product
design_product_file
production_record
production_product
production_process
order_flow_status_history
```

推荐增加关联字段：

`design_package`：

```text
batch_id
```

`production_record`：

```text
batch_id
```

`design_product` 和 `design_product_file` 可以通过 `package_id`、`design_product_id` 间接关联批次，不一定重复增加 `batch_id`。

现有按照 `order_id`、`package_id`、`production_record_id` 查询的功能仍然可以查到新增数据。批次表只用于：

- 当前批次筛选；
- 当前批次编辑权限；
- 当前批次完成判断；
- 追加批次生产数据生成范围；
- 新旧生产阶段隔离。

普通订单详情和生产台账查询不能强制要求关联批次表，否则可能导致历史数据或新增数据遗漏。

## 5. 后端影响域和实施方案

### 5.1 追加批次服务

新增批次服务或扩展设计文件服务，建议提供：

```text
createOrReuseBatch(orderId)
uploadPackage(batchId, orderId, file)
listBatchPackages(orderId, batchId)
savePrintInfo(orderId, packageId, batchId, data)
completeBatch(orderId, batchId)
```

本需求不新增“取消追加批次”业务动作。未完成批次如需放弃，按现有数据包删除和权限规则处理；`CANCELLED` 可以作为数据模型的预留状态，但本需求不得提供前端取消按钮或可调用的取消接口。

批次状态建议明确为：

```text
UPLOADING → PRINT_INFO_EDITING → COMPLETED
```

当前代码以 `UPLOADING/PRINT_INFO_EDITING` 均代表“未完成、可继续编辑”的状态，完成前不要求前端区分两者；如果后续保留 `PRINT_INFO_EDITING`，应在首次保存打印信息时更新该状态。无论采用哪种状态值，只有显式调用 `completeBatch` 且全部数据包均满足打印信息完成校验后才进入 `COMPLETED`。前端不得仅根据状态字符串自行推断权限，必须使用接口返回的能力字段。

不能把某一个数据包的打印信息保存成功直接视为批次完成：用户已确认一个未完成批次仍可继续上传更多数据包，系统无法从单次保存请求推断用户是否已经上传完毕。因此需要一个轻量的“完成追加设计批次”动作（可以由复用的打印信息弹窗底部按钮触发），由后端最终校验并锁定批次。当前批次由新的“追加设计批次”入口创建或复用；只有当前批次已完成后，才创建下一独立追加批次。

接口边界有两种可行形态：可以新增批次专用资源路径，也可以在现有设计数据包、打印信息和文档接口上增加可选 `batchId`。当前代码采用后者，以下是实际接口契约；后续不得同时维护两套语义不同的接口：

```text
POST   /design/package-batch?orderId={orderId}                  创建或复用未完成批次
GET    /design/package-batch?orderId={orderId}                  查询订单批次
GET    /design/package-batch/{batchId}?orderId={orderId}        查询指定批次
POST   /design/package-batch/{batchId}/complete?orderId={orderId}

POST   /design/package/upload?orderId={orderId}&batchId={batchId}
GET    /design/packages?orderId={orderId}&batchId={batchId}
DELETE /design/package/{packageId}?orderId={orderId}&batchId={batchId}

GET    /design/workorder/{orderId}/package/{packageId}/print-info/options?batchId={batchId}
GET    /design/workorder/{orderId}/package/{packageId}/print-info?batchId={batchId}
POST   /design/workorder/{orderId}/package/{packageId}/print-info?batchId={batchId}
DELETE /design/workorder/{orderId}/package/{packageId}/print-info/{printInfoId}?batchId={batchId}

指令单/图纸下载、预览、版本、修订和确认接口均以可选 batchId 传递批次上下文。
```

不需要单独的 `print-info/start` 接口。上传接口和打印信息接口都可以在批次未完成时调用；批次只有在显式调用 `/complete` 且全部数据包均已填写打印信息后才进入 `COMPLETED`。这样既支持一个批次内分多次上传，也不会因第一个数据包保存成功而提前完成批次。

`/complete` 必须在锁定批次的事务中判断本批次存在至少一个数据包、所有数据包均有打印信息，再执行订单回退和生产数据生成。打印信息保存本身不应自动完成批次；前端在完成动作成功后刷新订单状态和生产记录。

### 5.2 数据包校验

追加上传时：

- 校验订单状态属于追加批次专用白名单。按当前状态枚举，建议为 `2030, 3010, 3020, 3030, 3040, 4010, 5010, 5020, 5030, 5040, 5050, 6010, 6020, 6030, 8010`；明确排除 `9010` 取消状态、设计完成前状态以及未纳入本需求的异常/审核状态。不要直接复用 `ALLOWED_DESIGN_ATTACHMENT_STATUSES`，因为该集合还包含 1030/2010/2020，主要用于模型和报告的订单级增量维护；
- 校验订单未取消；
- 校验订单不是经典案例；
- 校验当前用户权限和设计师/管理员权限；
- 校验数据包属于当前批次；
- 保留现有文件类型、大小、文件名和压缩包解析校验；
- 不执行新旧数据包文件重复比对。

新接口需要继续使用现有操作日志注解/记录机制记录批次创建、数据包上传、删除、打印信息保存和批次完成；本需求不要求对打印信息做版本化日志，但不能因拆分新接口而丢失现有操作审计。

删除追加数据包时：

- 只允许删除当前未完成批次的数据包；
- 未填写打印信息的数据包允许删除；
- 已产生打印信息的数据包是否允许删除应由批次状态和现有数据完整性规则共同控制；推荐只允许删除未完成打印信息的数据包；
- 删除时清理包内文件关联和存储文件。
- 数据库存储失败、解压失败或关联失败时，清理已经上传到对象存储的外层压缩包和包内文件，避免留下孤儿文件；删除动作本身应保持数据库删除和文件清理的一致性。

### 5.3 打印信息保存

打印信息保存接口需要区分两种模式：

#### 原设计阶段模式

继续保留当前整包替换能力，允许设计完成前修改已有数据包。

#### 追加批次模式

保存时校验：

- 数据包属于当前追加批次；
- 批次未完成；
- 当前用户有权限；
- 所有文件 ID 属于当前数据包；
- 产品、规格、材质、颜色和数量校验保持现有逻辑。

允许整包替换，但只作用于当前批次的数据包。

打印信息选项接口也必须执行订单、批次和数据包归属校验。当前 `getOptions` 的校验弱于列表/保存接口，不能因为它只是读取产品选项就允许跨订单或跨批次读取包级信息；追加模式下应只返回当前批次目标数据包的可编辑上下文。

当批次全部数据包均有打印信息时，触发最终完成动作。完成动作必须在事务中：

1. 锁定订单和追加批次；
2. 再次校验批次全部数据包均已填写打印信息；
3. 批次状态改为 `COMPLETED`；
4. 批次数据包锁定；
5. 订单状态回退为 2030；
6. 写入订单状态历史；
7. 仅生成当前批次生产数据。

### 5.4 生产数据生成

复用现有 `DesignCompletedListener`，通过事件中的可选数据包范围实现定向生成：

```text
DesignCompletedEvent(orderId, packageIds)
```

正常完成设计仍使用事件无 `packageIds` 的订单级全量语义；追加批次必须使用带 `packageIds` 的批次级、数据包级语义。实现阶段不应再维护另一套复制的生产记录创建逻辑。

追加生成规则：

- 按当前批次数据包查询设计产品；
- 按 `packageId + productId` 分组；
- 每个产品生成一个 `production_record`；
- 按设计产品数量展开 `production_product`；
- 按订单类型生成生产工序；
- 生成流转卡编号和生产批号；
- 复制订单基础信息；
- 新记录初始状态为 2030；
- 通过 `batch_id` 保留追加批次关联。

必须增加幂等约束，避免重复提交造成相同批次和数据包重复生成生产数据。

### 5.5 状态回退

推荐新增专用动作，例如：

```text
COMPLETE_ADDITIONAL_DESIGN_BATCH
```

该动作只允许追加批次完成时调用，不能让普通 `COMPLETE_DESIGN` 直接作用于生产中或已完成订单。

状态变更必须：

- 使用订单行锁或乐观锁；
- 同步更新 `order_main.phase` 为设计阶段 `20`，不能只更新 `status=2030`；
- 使用 `order_main.version` 或等价的条件更新防止订单在追加完成时被其他生产操作覆盖；
- 写入 `order_flow_status_history`；
- 使用当前操作人写入操作人、动作和批次号备注；
- 按现有完成设计语义更新当前处理人字段，不能遗留为已完成生产环节的旧处理人；
- 保留原订单基础信息；
- 不直接覆盖原始 `design_submit_time`，追加批次完成时间以 `design_package_batch.completed_time` 记录；
- 不修改旧生产记录状态；
- 不重新发布会全量扫描所有数据包的普通设计完成事件。

订单回退应复用 Flow 领域的阶段/状态和历史记录服务，或新增只允许追加批次完成场景调用的受控方法，不能通过普通订单更新接口静默修改状态。

### 5.6 生产卡事件、通知和流转卡数据

当前 `DesignCompletedListener` 创建生产记录后会发布 `ProductionCardsCreatedEvent`。通知模块使用事务提交后的事件为生产员和生产管理员发送“新的生产流转卡待接收”通知。

追加批次完成时仍发布 `DesignCompletedEvent`，但只携带当前追加批次的数据包 ID；该事件在完成事务内同步触发生产记录、生产产品和工序创建，以便订单、批次和生产数据一起回滚。生产记录创建完成后发布的 `ProductionCardsCreatedEvent` 只携带新生成的 `production_record.id` 列表，并且必须在事务提交后发布，用于通知生产部门，不能在生产数据回滚时提前通知。

追加生产记录详情通过 `design_package_id` 查询数据包、指令单和图纸。当前生产记录生成本身只依赖设计产品和数据包文件，不会自动生成指令单和图纸；因此实施时必须明确保持这一现有行为：若追加批次只要求打印信息和生产阶段数据，则不额外伪造指令单/图纸记录。生产详情在缺少指令单或图纸时应保持可查询，相关文件字段为空，而不能导致新生产记录不可用。

### 5.7 权限校验边界

新增 `design:AppendBatch` 只负责前端入口和接口资源权限。当前设计文件服务还会调用 `DesignQueryHelper.checkIsAssignedDesigner`，该方法通过既有 `design:EditFile` 权限判断设计管理员、系统管理员等角色是否跳过“必须是指定设计师”的限制。

追加批次接口必须继续复用这一既有权限逻辑：

- 不因为新增按钮权限而自动扩大订单数据操作范围；
- 普通设计师仍按照当前指定设计师校验；
- 设计管理员、系统管理员等当前已具备 `design:EditFile` 语义的角色继续按现有规则处理；
- 所有 `batchId`、`packageId`、`orderId` 三者关系必须由后端再次校验。

审查本地最新 `sys_resource` 数据时未发现 `design:EditFile` 资源编码，而代码中的 `checkIsAssignedDesigner` 仍以 `StpUtil.hasPermission("design:EditFile")` 作为管理员跳过指定设计师校验的依据。因此实施前后端联调时必须验证真实登录角色的权限集合和实际行为，不能仅依据角色名称假设设计管理员可以修改全部订单。本需求的权限 SQL 只复制 `design:Upload` 的角色，不新增或改变既有 `design:EditFile` 语义；如果现网确实依赖该权限的通配授权，应保持现状，如果现网不存在该授权则按当前实际行为处理，避免借本需求扩大权限。

## 6. 前端影响域和实施方案

### 6.1 新增追加设计批次入口

建议在设计工单操作菜单增加：

```text
追加设计批次
```

该入口只在后端允许的状态和权限范围内显示或可操作。原“上传设计文件”入口保持正常设计流程语义，不作为追加批次入口使用。

### 6.1.1 操作列入口必须采用互斥、显式的状态矩阵

当前 `src/views/business/design.vue` 的 `getRowActions` 对未被前面分支命中的状态使用“其他状态默认全部操作”兜底，因此会在设计完成及生产后续状态同时显示“上传设计文件”和“填写打印信息”。这与新需求不一致，也会把前端入口暴露给后端必然拒绝的请求：普通数据包上传、普通打印信息接口、二维码上传以及指令单/图纸修订接口仍使用设计阶段校验。

实施时不能继续保留该全量兜底，应改为基于明确能力集合生成菜单。至少遵循以下规则：

| 状态范围 | 正常上传入口 | 追加设计批次 | 普通填写打印信息 | 说明 |
| --- | --- | --- | --- | --- |
| 设计未完成且当前原流程允许上传的状态（当前前端显式集合为 `1030、2020`） | 显示 | 隐藏 | 显示 | 继续使用旧 `fileDialog` 和旧打印信息弹窗；具体状态以现有后端状态校验和当前 UI 既有行为的交集为准，不能用“非 2030 都允许”替代 |
| 2030 及允许追加的后续正常状态 | 隐藏 | 显示 | 显示追加批次上下文 | 进入追加流程；复用原打印信息弹窗，但历史数据包只读，仅当前追加批次数据包可填写 |
| 已存在未完成追加批次 | 隐藏 | 显示“继续追加设计批次”，点击后复用该批次 | 显示批次上下文 | 不创建并行批次；继续进入复用的上传页或打印信息弹窗，历史包只读、当前批次包可编辑 |
| 已取消 9010 | 隐藏 | 隐藏 | 隐藏 | 只保留查看/历史查询等明确的只读入口 |

“上传设计文件”和“追加设计批次”必须是互斥入口；不能仅依靠按钮文本区分，也不能让两个入口最终调用同一个不带 `batchId` 的上传方法。入口显示、点击前再次校验、接口权限和后端状态校验应形成四层防线。

同一矩阵还需要覆盖以下操作，避免只修复两个上传按钮而留下其它错误入口：

- **填写打印信息**：追加流程继续进入现有打印信息弹窗，但必须携带 `batchId` 或等价的追加上下文。弹窗可以展示订单下全部数据包，历史数据包的打印信息只能查看，当前追加批次的数据包才允许填写；当前追加批次未完成前允许修改，批次完成/订单回退到 2030 后锁定。
- **指令单和图纸**：追加流程继续复用现有 `designDocumentPreviewDialog`。历史数据包对应的指令单和图纸只读，不能上传修订版；当前追加批次的新数据包可以继续使用现有修订上传能力，但接口必须携带并校验批次/数据包上下文，不能误修改历史数据包。
- **生成二维码、订单取消及其他现有入口**：按当前逻辑保持，不纳入本需求的入口行为调整范围。追加批次实现不得因为新增 `batchId` 或状态回退而扩大这些操作的权限和可用范围；仅需确保新增入口不会错误复用它们的 handler。
- **查看详情**：详情弹窗中的“开始设计/完成设计”按钮必须继续严格限定为 2010/2020；追加批次完成不得复用“完成设计”按钮，避免对生产中或已完成订单触发普通状态机。

建议把状态能力集中定义为 `getDesignActionCapabilities(status, row)`，再由该函数生成菜单以及弹窗内部的可编辑属性，不要在模板和多个 handler 中散落数字比较。此次不改变二维码、取消和其他现有入口的状态逻辑；新增能力主要是 `appendBatch`、`batchId`、历史数据包只读和当前批次可编辑。未知状态仍不得因为“其他状态”兜底而出现上传入口。

### 6.2 追加批次上传弹窗

追加设计批次和原“上传设计文件”页面可以复用同一个 `fileDialog` 组件，不需要复制页面。组件增加隐藏的流程模式和批次上下文，例如：

```text
mode = NORMAL_DESIGN | APPEND_BATCH
batchId = null | 当前追加批次 ID
```

在 `APPEND_BATCH` 模式下，组件的上传接口需要携带：

```text
orderId
batchId
file
```

前端需要保存当前批次 ID，并允许一次上传多个数据包或多次上传。上传成功后刷新当前批次数据包列表；原模式保持当前行为，不得因复用组件而隐式创建追加批次。

上传弹窗从“追加设计批次”入口打开时，应先创建批次并保存 `batchId`。原“上传设计文件”入口不得隐式创建追加批次。打印信息弹窗需要提供“完成追加设计批次”动作，调用批次 `/complete` 接口，而不是依赖最后一次保存自动完成；完成前用户可以返回上传页面继续增加数据包。

### 6.3 追加批次打印信息弹窗

追加流程直接复用现有 `designPrintInfoDialog`，增加隐藏的 `batchId`/模式参数，不另建一套打印信息页面。数据包列表可以展示订单下全部数据包，但必须明确区分历史数据包和当前追加批次数据包：

- 历史数据包仍可选择查看，但所有表单字段和保存、删除等修改操作只读；
- 当前追加批次所有数据包均可选择并编辑；
- 当前批次未完成前可以反复修改，也可以返回上传弹窗继续上传或删除尚未锁定的数据包；
- 当前批次完成后只读；
- 不能仅根据 `printInfoCount` 推断是否允许编辑；
- 应由后端返回 `batchId`、`batchStatus`、`editable`、`canDelete` 等字段，前端据此控制选中数据包的编辑能力。

当用户明确完成追加批次且后端完成校验成功后，前端应刷新订单详情和订单列表，以反映订单回退到设计完成及新生产记录生成；单个数据包保存成功不能直接触发状态回退。

### 6.4 指令单和图纸预览弹窗

追加流程直接复用现有 `designDocumentPreviewDialog`，增加 `batchId`/模式参数和包级编辑能力：

- 历史数据包的指令单、图纸、历史版本、下载和预览保持可用；
- 历史数据包隐藏或禁用“上传修订版”；
- 历史数据包在 Excel 预览弹窗中也不得显示或允许“确认指令单/确认图纸”操作；确认按钮必须由 `canEditDocuments` 能力控制；
- 当前追加批次新数据包允许上传修订版；
- 上传修订接口必须校验当前数据包属于指定追加批次，不能只依赖前端禁用控件；
- 订单状态回退到 2030 后，当前追加批次完成锁定，对应数据包也转为只读；批次未完成时如果继续上传新包，旧包仍按当前批次能力返回，不得被误判为历史只读包。

### 6.5 前端数据接口建议

复用旧弹窗意味着不能只修改前端按钮和组件 Props，后端现有响应对象也必须提供“这个数据包是否属于当前追加批次、是否允许编辑”的事实依据。前端不应根据订单状态、`printInfoCount` 或“是否最后一个数据包”自行推断只读状态。

#### 6.5.1 数据包接口

现有接口：

```text
POST   /design/package/upload
DELETE /design/package/{packageId}?orderId={orderId}
GET    /design/packages?orderId={orderId}
GET    /design/package/{packageId}/files?orderId={orderId}
```

调整要求：

- 上传和删除接口增加可选 `batchId`；原设计模式不传，追加模式必须传，后端必须校验订单、批次、数据包三者归属及批次状态。
- 数据包列表接口建议增加可选 `batchId` 查询条件，但追加打印信息弹窗需要展示订单全部数据包，因此也应支持“返回全部包并标记当前批次”的模式，而不是只返回当前批次导致历史包无法查看。
- 列表和工单详情中的 `DesignPackageVO` 必须补充包级能力字段：

```text
batchId                 // 历史原始数据包可为空，追加包为追加批次 ID
batchNo
batchStatus             // NONE/UPLOADING/PRINT_INFO_EDITING/COMPLETED 等
isCurrentBatch
editable                // 当前数据包整体是否允许修改
canDelete
canEditPrintInfo
canEditDocuments
printInfoCompleted
printInfoCount
```

- `editable`、`canDelete`、`canEditPrintInfo`、`canEditDocuments` 必须由后端结合当前用户权限、订单状态、批次状态、数据包状态以及请求模式计算，不允许前端只看 `batchId != null` 就开放操作。原数据包仅在原设计未完成状态（`1030/2010/2020`）保留旧规则的写能力；订单进入 `2030` 及后续状态后，无论是否处于追加上下文，都必须返回只读能力。
- `DesignPackageFileVO` 建议补充 `selectableForPrint`。历史数据包文件仍可查看，但在只读包中不能作为新增或修改打印信息的可选文件；追加包文件只有在 `canEditPrintInfo=true` 时可选。

#### 6.5.2 打印信息接口

现有接口：

```text
GET    /design/workorder/{orderId}/package/{packageId}/print-info/options
GET    /design/workorder/{orderId}/package/{packageId}/print-info
POST   /design/workorder/{orderId}/package/{packageId}/print-info
DELETE /design/workorder/{orderId}/package/{packageId}/print-info/{printInfoId}
```

复用旧打印信息弹窗时，以上接口增加可选 `batchId`；追加模式下必须传入。响应字段建议如下：

```text
PrintInfoOptionsVO:
batchId
packageId
batchStatus
editable
canSave
canDelete
allowedPackageFileIds
...

PrintInfoListVO:
batchId
packageId
batchStatus
editable
canSave
canDelete
printInfoCompleted
productMark
packQuantity
remark
items
```

其中：

- 历史包的查询接口仍返回完整打印信息和选项，保证旧数据可查看；但 `editable=false`、`canSave=false`、`canDelete=false`。
- 当前追加批次未完成时返回 `editable=true`、`canSave=true`，是否允许删除由现有打印信息规则和批次状态共同决定。
- 当前追加批次完成后所有写能力返回 `false`。
- `allowedPackageFileIds` 用于限制打印模型选择范围，后端保存时仍必须再次校验，不能只依靠前端下拉选项。
- 打印信息保存、删除即使前端因只读状态不发请求，后端也必须对历史包和已完成批次拒绝写操作。
- 原有不带 `batchId` 的打印信息保存、删除接口仅保留设计未完成阶段（`1030/2010/2020`）的历史兼容能力；订单进入 `2030` 或任一后续状态后，旧包即使绕过前端直接调用旧接口也必须拒绝修改。追加包的写操作必须携带 `batchId`，并由批次状态和包归属共同校验。
- 批次完成接口必须明确要求 `batchId`，并禁止空批次完成；批次内数据包全部被删除后不能回退订单状态，也不能生成空生产数据。

#### 6.5.3 指令单和图纸接口

现有下载、预览、版本查询接口继续支持历史包只读访问；以下修订/确认类接口增加可选 `batchId`，追加模式下必须传入：

```text
POST /design/workorder/{orderId}/package/{packageId}/instruction/upload-revised/{id}
POST /design/workorder/{orderId}/package/{packageId}/drawing/upload-revised/{id}
POST /design/workorder/{orderId}/package/{packageId}/instruction/confirm/{id}
POST /design/workorder/{orderId}/package/{packageId}/drawing/confirm/{id}
```

当前实现不要求修改 `DesignDocVersionVO`；使用订单详情/数据包列表返回的包级能力字段控制修订入口。若后续需要在版本列表内部独立渲染权限，再另行增加以下字段，并同步前后端契约：

```text
canUploadRevision
canConfirm
readOnly
batchId
```

规则为：历史数据包返回 `readOnly=true`、`canUploadRevision=false`；当前追加批次未完成且数据包满足现有文档规则时允许修订/确认；批次完成后全部转为只读。下载、预览、历史版本查询不应因只读标识而被禁止。

后端文档服务的所有写接口必须显式校验 `batchId` 与 `packageId` 归属，不能因为当前 URL 仍是旧的 `/design/workorder/{orderId}/package/{packageId}/...` 就认为调用方有权修改该包。

#### 6.5.4 工单详情、模型和报告接口

现有 `GET /design/workorder/{orderId}` 返回的 `packageList` 应复用上述增强后的 `DesignPackageVO`，这样详情弹窗、打印信息弹窗、文档预览弹窗使用同一套只读能力字段，避免三个页面各自推断状态。

可视化模型和设计报告目前是订单级附件，不属于设计数据包；本需求仍允许按既有订单级逻辑追加或删除，因此不强行把 `batchId` 加到模型/报告 VO。若后续要求把模型或报告也归属到追加批次，则必须另行增加关联字段和查询过滤，不能仅在前端增加隐藏标识。

设计报告的接口契约必须与现有多文件需求保持一致：

```text
POST /design/report/link?orderId={orderId}
请求体：{ "fileIds": ["file-id-1", "file-id-2"] }
GET  /design/report?orderId={orderId}
响应：FileVO[]，不存在报告时返回 []，不再返回单个 report 字段
```

`/design/report/link` 必须逐个校验文件存在、业务类型为设计报告、未被其他订单/业务关联；任一文件关联失败时，回滚本次关联并清理本次上传产生的孤儿文件。模型接口同样校验业务类型和订单归属。报告不允许取消订单操作，不要求版本日志、通知或审核触发；删除全部报告或模型不改变订单状态。

追加批次 VO 建议返回：

```text
batchId
batchNo
orderId
status
packageCount
completedPackageCount
canEdit
```

前端展示可以保持统一提示风格，不需要针对每个订单状态设计复杂的差异化提示；但历史数据包和当前追加批次数据包在选择能力上必须有明确区别。

#### 6.5.5 前后端接口契约验收表

以下字段是前端复用旧页面时必须使用的稳定契约；字段缺失、类型改变或 `null/[]` 语义改变都应视为联调阻断问题：

| 接口/对象 | 后端返回或接收结构 | 前端用途 |
| --- | --- | --- |
| `POST /design/package-batch` | `DesignPackageBatchVO`：`batchId、batchNo、orderId、status、sourceOrderStatus、packageCount、completedPackageCount、canEdit、createTime、completedTime` | 保存批次上下文；未完成批次再次进入时复用 `batchId` |
| `GET /design/package-batch` | `DesignPackageBatchVO[]`，按创建时间倒序 | 判断是否存在未完成批次，并展示历史批次 |
| `POST /design/package-batch/{batchId}/complete` | 无业务数据的成功响应；失败返回统一业务错误 | 只有用户明确点击完成且成功后刷新订单/批次/生产数据 |
| `GET /design/packages` | 不带 `batchId` 时返回订单全部包；带 `batchId` 时返回该追加批次包（上传页使用），均返回 `DesignPackageVO[]` | 订单详情/打印信息/文档页使用全量查询，追加上传页只操作当前批次，历史包不可出现在上传删除列表 |
| `POST /design/package/upload` | 单个 `DesignPackageVO`，追加模式必须回传 `batchId` | 上传后追加到当前批次，不得隐式创建新批次 |
| 打印信息 options/list | 必须包含 `batchId、packageId、batchStatus、editable、canSave、canDelete、printInfoCompleted`；options 另含 `allowedPackageFileIds` | 控制旧弹窗只读/可编辑和可选包内文件，后端仍需重复校验 |
| `GET /design/report` | `DesignReportVO[]`；无报告返回 `[]` | 禁止继续读取旧的单值 `report` 字段 |
| `POST /design/report/link` | 请求 `{fileIds: string[]}`，成功返回关联后的 `DesignReportVO[]` | 支持多报告一次关联；任一文件校验失败时整批失败并清理本次孤儿文件 |

所有接口都必须使用项目统一的 `Result` 包装；`batchId` 在原设计模式下允许不传，在追加上传、打印信息保存/删除、文档修订/确认等写操作中必须传入。后端必须同时校验 `orderId → batchId → packageId → fileId` 的归属链，不能将“返回只读字段”当作安全控制。

### 6.6 系统资源和角色权限

如果设计工单列表操作列增加“追加设计批次”按钮，前端不能只增加按钮编码，还需要同步初始化系统资源和角色权限。

本地数据库调查结果：

现有“上传设计文件”资源为：

```text
sys_resource.id       = 1204
resource_name         = 上传设计文件
resource_code         = design:Upload
resource_type         = 3（按钮）
parent_id             = 102
```

当前拥有该权限的角色为：

| role_id | role_code | role_name |
|---:|---|---|
| 1 | `admin` | 超级管理员 |
| 4 | `designer` | 设计师 |
| 5 | `designer-manager` | 设计管理员 |
| 11 | `company-admin` | 公司管理员 |

因此“追加设计批次”应创建独立按钮资源，建议使用：

```text
resource_name = 追加设计批次
resource_code = design:AppendBatch
resource_type = 3
parent_id = 上传设计文件资源的 parent_id
```

授权角色不得手工写死为其他角色，而应从当前持有 `design:Upload` 的角色集合复制，确保两个按钮权限一致。对应统一数据库变更 SQL 已另存为：

`yigongbao-parent/sql/migration/2026-10-10-design-append-batch.sql`

该脚本统一包含追加批次表、原有表结构扩展、索引、按钮资源和角色权限关联，并具备幂等性：重复执行不会重复创建表、字段、索引、资源或角色关联。

历史数据不执行 `batch_id` 回填，原有业务查询继续按照 `order_id`、`package_id` 和 `production_record_id` 查询，新增数据也继续写入原业务表，不会因批次表导致现有查询遗漏。

## 7. 状态聚合和生产流程风险

当前生产模块存在按订单聚合生产记录状态的逻辑。追加批次上线后，如果仍把旧记录和新记录混合参与当前批次聚合，可能出现：

- 旧记录已完成，导致新批次被误判为已达到生产阶段；
- 新批次状态推进影响历史记录判断；
- 订单回退到 2030 后，旧生产记录被错误重置或重新推进。

建议：

- 追加批次生成和批次完成判断只针对当前 `batch_id`；
- 旧生产记录只作为历史记录保留；
- 新生产记录下载后，仅推进新记录及当前追加批次相关状态；
- 需要重点改造 `triggerFlowIfAllReach`、`triggerFlowIfAllExact`、`reconcileOrderProductionStatus` 及其所有调用方，不能继续无条件按 `order_id` 汇总全部历史生产记录；
- `downloadDataPackage`、设备状态监听、打印/后处理/质检/仓储服务中的订单聚合推进，都必须能够识别当前追加批次；
- 追加批次的新记录初始为 2030，第一次下载时仍按当前逻辑进入 3010，但只用当前批次记录判断本批次是否全部下载；
- 订单级状态如果仍需要聚合，应明确“当前追加批次优先”的规则；
- 所有按订单全量查询的普通展示接口仍保留全部历史和新增数据。

### 7.1 当前代码复核结论（已闭环项与实施验收项）

本轮代码复核确认以下关键闭环已经纳入当前工作区实现或契约：

1. 批次完成通过专用流程动作执行，同时更新 `order_main.phase=DESIGN`、`status=2030`、当前处理人和版本，并写入 `order_flow_status_history`；不复用普通“完成设计”状态机，也不直接静默更新订单状态。
2. 批次创建锁定订单行并复用未完成批次；批次完成锁定批次，并使用订单版本条件更新作为并发闸门。完成失败时事务回滚，不能只留下批次完成或订单回退的一半结果。
3. `DesignCompletedEvent` 携带当前批次的数据包 ID；生产监听器只为这些包创建生产记录、生产产品和工序，并把 `batch_id` 写入新增生产记录。已有生产记录不会因追加完成而重新生成或重置。
4. 下载、设备状态、打印、后处理、质检、包装、仓储和生产状态对追加记录的订单聚合已增加 `batch_id` 口径；历史 `batch_id IS NULL` 记录仍走原逻辑，但当订单已存在追加数据时不得让旧记录聚合结果推进追加批次的生产状态。
5. 生产卡创建后的通知事件采用事务提交后发布；生产数据创建仍在原设计完成事务上下文内，以保证订单、批次和生产数据的一致性。
6. 前端未知或异常订单状态不再兜底显示设计文件上传、打印信息和文档修改入口；原设计入口与追加入口按明确状态集合互斥显示。
7. 前后端能力字段以实际返回的包级字段为准：`DesignPackageVO.editable/canDelete/canEditPrintInfo/canEditDocuments`，以及打印信息 options/list 的 `editable/canSave/canDelete/printInfoCompleted`。`DesignDocVersionVO` 不承担包级编辑权限。

实施验收仍必须逐项确认：

- 完成动作对空批次、无打印信息数据包、已完成批次、取消批次、跨订单/跨批次 ID 的拒绝结果；
- 同一订单并发创建/完成批次、完成与上传或生产流转并发时的唯一成功结果；
- 追加完成后 `order_main.current_handler_id/current_handler_name` 与现有完成设计语义一致，不遗留旧生产环节处理人；
- 追加生产记录的“数据包 + 产品”数量、工序数量和幂等性；
- 旧订单详情、生产台账、数据包列表仍能同时查询历史数据和新增数据；
- 目标数据库执行统一 SQL 后字段、索引、按钮资源、角色关联和重复执行幂等性；
- 线上实际角色权限与 `design:Upload` 复制结果一致，且没有借本需求扩大设计管理员或其他角色的数据范围。

## 8. 异常、并发和事务要求

必须覆盖以下场景：

1. 同一订单重复创建追加批次；
2. 同一批次同时上传多个文件；
3. 同一数据包重复保存打印信息；
4. 最后一个数据包保存时与另一个数据包保存并发；
5. 订单状态在追加过程中发生其他生产流转；
6. 生产记录创建成功但订单状态更新失败；
7. 订单状态更新成功但生产记录创建失败；
8. 数据包上传存储成功但数据库事务失败；
9. 删除数据包后残留包内文件或对象存储文件；
10. 前端伪造历史批次或其他订单的数据包 ID。

建议：

- 追加批次完成使用事务；
- 完成时锁定订单和批次；
- 打印信息保存校验批次归属；
- 生产记录生成使用批次和数据包幂等校验；
- 追加批次未完成前允许继续新增数据包；完成动作获取批次锁后再次查询数据包集合，完成校验和锁定必须在同一事务内进行；
- 如果用户需要开启完全独立的追加批次，可以创建新的追加批次；不能把新批次数据包混入旧批次；
- 批次完成动作需要防止两个请求同时将同一批次完成并重复创建生产记录；
- 生产通知必须使用提交后事件，避免事务回滚后仍通知生产部门；
- 文件存储失败时清理孤儿文件；
- 所有资源删除操作校验订单、批次和数据包三者关系；
- 不允许通过直接修改订单状态跳过生产数据创建。

## 9. 测试方案

### 9.1 后端测试

- 2030 及后续状态可以创建追加批次；
- 8010 已完成订单可以追加；
- 取消订单和经典案例订单拒绝追加；
- 原设计入口和追加入口权限边界正确；
- 一个批次上传多个数据包；
- 多次上传均关联同一追加批次；
- 新旧数据包文件重复不影响上传；
- 未填写打印信息的数据包允许删除；
- 设计完成前旧数据包仍可修改；
- 追加批次中已填写数据包可以再次修改；
- 历史批次数据包不能被追加批次接口修改；
- 部分数据包完成打印信息时订单不回退；
- 未调用批次完成动作时，即使当前所有数据包已有打印信息，订单也不回退；
- 批次完成动作校验全部数据包后订单只回退一次；
- 批次完成前允许继续上传数据包，完成动作与上传/保存并发时结果明确且不重复生成；
- 只为当前批次生成生产记录；
- 旧生产记录不重复生成、不重置；
- 每个数据包和产品生成正确生产记录；
- 生产产品数量按现有数量规则展开；
- 医疗器械和非医疗器械工序数量正确；
- 重复提交不重复生成生产数据；
- 事务失败后订单、批次和生产数据全部回滚；
- 状态历史记录正确。

### 9.2 前端测试

- 新追加设计批次入口正确显示；
- 可在一个批次内上传多个数据包；
- 当前批次未完成时重复进入新入口复用原批次，完成后再次进入才创建新的追加批次；
- 打印信息弹窗展示订单全部数据包，但只允许编辑当前追加批次；
- 当前批次未完成前可以修改已填写数据包；
- 历史批次不可选、不可修改；
- 未填写打印信息的数据包可以删除；
- 用户明确点击完成追加批次并成功后刷新订单状态和生产记录；单个数据包保存成功不应自动刷新为设计完成；
- 页面刷新后批次状态和编辑能力保持一致；
- 订单详情、生产台账和数据包列表可以显示新增数据。

## 10. 实施顺序

建议按以下顺序实施：

1. 新增追加批次表和实体、Mapper、状态枚举；
2. 完善数据包 VO 和批次查询接口；
3. 增加追加批次创建、上传、列表、删除接口；
4. 扩展打印信息查询和保存接口的批次校验；
5. 抽取生产记录定向生成服务；
6. 增加追加批次完成事务和订单状态回退；
7. 修改生产状态聚合逻辑；
8. 增加后端单元测试和集成测试；
9. 修改前端追加批次入口；
10. 修改前端批次上传流程；
11. 修改前端打印信息批次筛选和编辑能力；
12. 完成前后端联调和回归测试；
13. 对订单详情、生产台账、文件查询和历史流程进行全面审查。

## 11. 结论

本需求不应实现为简单放宽原数据包上传和打印信息接口的状态限制，而应新增“追加设计批次”业务上下文。

关键原则如下：

1. 追加入口与原设计入口分离；
2. 每次追加创建独立批次；
3. 原有设计和生产业务表继续保存全部新增数据；
4. 批次表只补充批次边界、编辑权限和生产生成范围；
5. 批次未完成前允许修改本批次打印信息；
6. 批次全部完成后统一锁定；
7. 订单状态统一回退到设计完成；
8. 只为当前批次生成新的生产阶段数据；
9. 历史数据包和生产数据不重置、不覆盖、不重复生成；
10. 所有订单级普通查询继续返回历史数据和新增数据。
