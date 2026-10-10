# 设计工单附件变更需求：前后端影响域与实施方案分析

## 1. 文档目的

本文记录以下需求在后端、数据库和前端的现状、数据结构、接口链路、状态/权限限制、影响域、实施边界和测试要求，作为后续前后端实施与验收的统一依据。

需求范围：

1. 设计完成订单及其后续正常流程状态的可视化模型允许再次删除、修改、上传。
2. 设计报告由当前“每订单一份、重新上传覆盖旧文件”改为“每订单允许多个报告文件”，并且在设计完成及其后续正常流程状态支持新增、删除等增量变动操作。

本文覆盖：

- 后端接口、服务校验、状态机和权限逻辑；
- MySQL 实际表结构、文件关联方式和数据约束；
- 前端页面操作入口、上传组件、API 类型、详情展示和状态判断；
- 前后端接口契约、兼容性、测试和实施风险。

本文同时作为后端实施依据和变更记录；本轮仅实施后端，前端按第 14 节另行实施。

## 2. 核心结论

### 2.1 可视化模型

当前后端的状态白名单已经包含 `DESIGN_COMPLETED(2030)`，因此设计完成状态下模型的新增/关联和删除从后端校验上已经允许。但打印、后处理、质检、仓储和已完成等后续状态仍被拦截。

本次补充需求要求把模型附件变更权限扩展到设计完成后的全部正常流程状态。这里的“修改”定义为增量维护，不是全量重传：已有 10 个模型时，用户可以继续新增模型，也可以删除其中指定的某几个模型，其他模型保持不变。

当前没有单独的“修改模型”接口，现有接口已经覆盖该语义：

1. 通过通用文件接口上传新文件；
2. 调用模型关联接口追加绑定新文件；
3. 需要移除时调用指定模型删除接口，仅删除目标模型。

如果当前页面在 `2030` 状态下无法操作，优先检查前端状态判断；如果需求要求原子替换、保留旧版本或避免删除后上传失败，则需要新增后端替换语义。

### 2.2 设计报告

当前后端同样允许 `2030` 状态下报告替换和删除，但不允许设计完成后的后续状态：待打印、打印中、打印完成、后处理、质检、仓储和已完成。

本次需求还改变报告的数据语义：报告不再是单个“当前报告”，而是订单下多个独立报告文件。报告操作也采用增量维护：重新上传/补充上传就是新增一份报告，删除时只删除指定报告，不影响其他报告。本需求不要求提供“全部替换”或版本替换语义。

### 2.3 不应直接扩大通用设计阶段白名单

`DesignQueryHelper.checkDesignPhase()` 被数据包、打印信息、图纸、指令单、二维码等多个设计操作复用。若直接将打印、质检、仓储等状态加入该白名单，会同时放开大量不在本需求范围内的设计数据修改操作。

建议新增仅用于模型和设计报告的附件变更状态校验，不修改通用 `checkDesignPhase()` 的语义。模型和报告都应允许 `2030` 及其后的正常状态；取消状态 `9010` 继续禁止。

## 3. 当前后端调用链

### 3.1 可视化模型

入口：

- `POST /design/models/link`
- `DELETE /design/model/{modelId}?orderId={orderId}`
- `GET /design/models?orderId={orderId}`

对应代码：

- `DesignAttachmentController.linkModels()`
- `DesignAttachmentController.deleteModel()`
- `DesignFileServiceImpl.linkModels()`
- `DesignFileServiceImpl.deleteModel()`

关联流程：

```text
通用文件上传 /basic/file/upload
        ↓
获得 fileId
        ↓
/design/models/link
        ↓
file_detail.object_type = 10.6
file_detail.object_id   = orderId
        ↓
新增 design_model(order_id, file_id)
```

模型删除流程：

```text
校验经典案例 → 校验设计状态 → 校验设计师权限
        ↓
校验 design_model.order_id
        ↓
删除 design_model 记录
        ↓
删除 file_detail 对应文件及存储对象
```

### 3.2 设计报告

入口：

- `POST /design/report/link`
- `DELETE /design/report/{fileId}?orderId={orderId}`
- `GET /design/report?orderId={orderId}`

对应代码：

- `DesignAttachmentController.linkReport()`
- `DesignAttachmentController.deleteReport()`
- `DesignFileServiceImpl.linkReport()`
- `DesignFileServiceImpl.deleteReport()`

当前报告关联流程：

```text
通用文件上传 /basic/file/upload
        ↓
获得 fileId
        ↓
/design/report/link
        ↓
将新文件追加关联为 object_type=10.5、object_id=orderId
```

需求变更后的推荐操作：

```text
新增报告：上传文件 → 校验订单/权限/文件类型 → 直接新增 10.5 关联

删除报告：校验指定 fileId 属于订单 → 删除该文件，不影响其他报告
```

如果未来需要“用新文件替换指定旧报告”，可以由前端先新增后删除，但当前需求明确不要求专门替换接口或版本语义。

## 4. 当前校验规则

### 4.1 状态校验

`DesignQueryHelper` 当前允许的设计状态为：

| 状态 | 含义 | 当前附件操作 |
|---|---|---|
| `1030` | 数据审核通过 | 允许模型/报告绑定与删除 |
| `2010` | 待设计 | 允许模型/报告绑定与删除 |
| `2020` | 设计中 | 允许模型/报告绑定与删除 |
| `2030` | 设计完成 | 当前已允许；需求继续保留 |
| `3010` | 待打印 | 当前不允许；需求后允许 |
| `3020` | 打印中 | 当前不允许；需求后允许 |
| `3030` | 打印完成 | 当前不允许；需求后允许 |
| `4010` | 后处理中 | 当前不允许；需求后允许 |
| `5010~5050` | 质检/返工/包装 | 当前不允许；需求后允许 |
| `6010~6030` | 仓储阶段 | 当前不允许；需求后允许 |
| `8010` | 已完成 | 当前不允许；需求后允许 |
| `9010` | 已取消 | 不属于正常后续状态，建议继续禁止 |

通用校验位置：

`yigongbao-module-design/.../helper/DesignQueryHelper.java` 的 `ALLOWED_DESIGN_STATUSES` 和 `checkDesignPhase()`。

### 4.2 设计师权限

`checkIsAssignedDesigner()` 的规则：

- 具有 `design:EditFile` 权限的管理员类角色跳过本人校验；
- 普通设计师必须等于 `order_main.designer_id`；
- 该规则同时用于数据包、模型、报告等设计文件操作。

即使开放后续状态，也应继续保留该权限校验，不能仅因订单已进入生产阶段而放弃操作者限制。

本次权限规则沿用现状：拥有 `design:EditFile` 权限的设计师管理员按现有逻辑跳过“必须是订单指定设计师”的限制；普通设计师仍只能操作自己被分配的订单。不新增针对后续状态的角色权限模型。

### 4.3 经典案例保护

模型新增、模型删除、报告新增、报告删除均调用 `orderMainService.checkNotClassicCase()`。

经典案例订单不能因本需求而获得普通附件的修改能力。后续实现必须保留该保护。

### 4.4 通用文件上传校验

`/basic/file/upload` 本身只校验：

- 文件非空；
- 全局文件大小；
- 文件名安全性；
- `bizType` 合法性；
- 业务类型的扩展名和大小配置。

该接口没有 `orderId` 状态校验。因此当前系统的订单状态限制发生在“模型/报告绑定”阶段，而不是文件物理上传阶段。

这会带来一个现存行为：如果通用文件上传成功，但后续绑定因状态不允许失败，可能遗留未关联文件。

## 5. 文件业务类型与配置

文件业务类型定义在 `FileBizTypeEnum`：

| 业务 | dict code | 配置前缀 | 当前配置 |
|---|---|---|---|
| 设计报告 | `10.5` | `design.report` | `.pdf,.doc,.docx,.xls,.xlsx,.pptx`，最大 50 MB |
| 可视化模型 | `10.6` | `design.model` | `.stl,.obj,.ply,.3mf`，最大 200 MB |

当前配置来源为 `sys_config`，不需要为本需求新增数据库字段或表。

## 6. 数据库结构

### 6.1 `design_model`

实际数据库表：

```text
id          bigint 主键
order_id    bigint 订单ID
file_id     varchar(32) 文件ID
is_deleted  tinyint 逻辑删除标记
```

现有索引只有：

- 主键 `id`
- `idx_design_model_order_id(order_id)`

没有外键，也没有 `(order_id, file_id)` 唯一约束。模型数量和重复关系由应用代码控制。

### 6.2 `file_detail`

模型和报告的物理文件都在 `file_detail`：

```text
id           varchar(32) 主键
object_type  varchar(32) 业务类型
object_id    varchar(32) 业务ID
url          varchar(512) 存储地址
is_deleted   tinyint 逻辑删除标记
```

查询订单设计报告的条件为：

```text
object_type = '10.5'
object_id   = orderId
```

现有索引为 `idx_file_detail_object(object_type, object_id)`，没有数据库级别的“一订单一报告”唯一约束。因此数据库结构天然可以支持多报告，主要变化在服务语义、返回模型和增量新增/单项删除接口行为。

### 6.3 数据库约束结论

通过 MySQL 最新结构确认：

- `design_model` 和 `file_detail` 之间没有数据库外键；
- 文件删除、业务记录删除和存储对象删除依赖应用层；
- 逻辑删除和物理存储删除的最终一致性由服务代码负责；
- 本需求原则上不需要调整表结构，但如果要增加版本、替换审计或恢复能力，则需要另行设计字段/表。

## 7. 下游影响域

### 7.1 完成设计校验

`DesignWorkorderServiceImpl.buildSubmitCheck()` 会检查：

- `design_model` 是否存在有效模型；
- `file_detail` 中是否存在至少一份 `10.5` 设计报告。

多报告模式下，完成设计校验仍只需要判断报告数量是否大于 0，不应要求固定一份，也不应因为新增第二份报告而失败。

如果设计完成后删除全部模型或全部报告，订单状态不会自动回退，也不会重新执行完成设计校验。订单可能继续保持 `2030`、生产中或已完成，但附件完整性已经变为不满足。

本次需求已明确以下行为：

- 允许删除后暂时缺少附件；
- 删除后不自动回退订单状态，也不强制重新进入设计或审核流程；
- 本需求不记录“设计附件已修改”的额外日志，也不通知生产/质检人员；
- 已开始生产或已出库的订单也允许删除模型。

### 7.2 影像和可视化查看器

`design_model` 被影像模块直接读取：

- `ImagingServiceImpl.getModels()`；
- `ViewerServiceImpl.getStlFileList()`。

删除模型会导致历史查看器无法返回该模型。业务已确认已开始打印或已出库订单也允许删除模型，因此实现时保留现有删除语义，不额外引入隐藏、版本或恢复机制。

### 7.3 设计报告返回模型和详情接口

当前 `DesignFileService.getReport()` 返回单个 `FileVO`，`DesignWorkorderDetailVO` 也只有单值 `report` 字段，工单详情填充逻辑调用 `vo.setReport(designFileService.getReport(orderId))`。

多报告后端至少需要调整：

- `DesignFileService.getReport()` 改为 `getReports()`，返回 `List<FileVO>`；
- `DesignAttachmentController` 查询报告接口返回列表；
- `DesignWorkorderDetailVO.report` 直接改为 `reports` 列表，不保留单值字段作为正式契约；
- `DesignWorkorderServiceImpl.fillDesignFiles()` 改为填充全部报告；
- 所有报告查询排序必须稳定，建议按创建时间倒序、文件 ID 作为次排序；
- 删除接口继续按指定 `fileId` 删除单份报告；
- 新增报告不能再调用“删除全部旧报告”的逻辑；
- `linkReport()` 保留现有路径并改为批量追加绑定，一次接收多个 `fileIds`；不新增报告替换接口。

这是后端 API 契约变化，即使数据库不需要迁移，也会影响前端类型、工单详情接口和测试断言。

### 7.4 经典案例文件迁移

`DesignClassicCaseFileListener` 会处理经典案例文件迁移，并同步更新 `design_model.file_id`。模型替换实现不能绕过该文件关联语义，也不能破坏文件移动后的业务引用更新。

## 8. 现有实现风险

### 8.1 报告替换不是原子安全替换

当前 `linkReport()` 的顺序是：

1. 查询旧报告；
2. 删除旧报告文件；
3. 关联新文件。

如果第 3 步失败，旧报告已经被删除。后续实现应考虑：

- 先校验新文件完整性和业务类型；
- 先完成新文件关联，再清理旧文件；
- 或保留旧文件直到新绑定成功；
- 外部存储删除失败时不能影响数据库事务判断。

本次已确认：通用文件上传成功但后续模型/报告关联失败时，后端自动清理未关联的孤儿文件及其存储对象。

### 8.2 绑定接口未严格校验文件业务类型

模型关联目前主要校验文件存在，未严格确认 `file_detail.object_type` 是否为 `10.6`。

报告关联同样只校验文件存在，未在关联前严格拒绝非 `10.5` 文件。

本次已确认必须补充文件业务类型归属校验：

- 模型只能绑定 `10.6`；
- 报告只能绑定 `10.5`；
- 文件不能已绑定其他订单或其他业务类型，除非明确允许转移关联。

### 8.3 模型和报告按增量方式维护，不需要全量替换接口

本需求中的“修改”是增量维护：模型和报告均允许在已有文件集合上追加文件，或删除指定文件，不要求先清空全部文件，也不要求一次性重建整个集合。

后端只需要明确两个动作：

- `add`：新增一份模型/报告；
- `delete`：删除指定模型/报告；

当前模型接口已经具备批量新增和单项删除能力，原则上不需要新增接口。报告接口需要修正现有覆盖行为后，也可以通过“单次新增 + 单项删除”满足需求。

## 9. 推荐后端实施边界

### 9.1 校验方法

不要扩大通用 `checkDesignPhase()`。建议新增类似以下语义的方法：

```text
checkDesignAttachmentMutationAllowed(orderId)
```

职责：

1. 查询订单并校验订单存在；
2. 拒绝取消订单；
3. 对模型：允许 `1030、2010、2020、2030` 以及 `3010~3030、4010、5010~5050、6010~6030、8010`；
4. 对报告：允许同一组正常状态，并支持订单下多份报告；
5. 保留经典案例保护；
6. 保留设计权限和指定设计师校验。

模型和报告最好分别使用更明确的方法，避免未来状态规则再次混淆：

```text
checkModelMutationAllowed(orderId)
checkDesignReportMutationAllowed(orderId)
```

状态集合建议集中定义为“设计附件可变更状态”，明确排除 `9010` 取消状态，不要使用 `status >= 2030` 这样的数值范围判断，以免误包含取消状态或未来新增的非目标状态。

### 9.2 报告

报告需求至少涉及：

- `linkReport()` 改为新增单份报告，不再删除全部旧报告；
- `deleteReport()` 保留按 `fileId` 删除单份报告；
- `getReport()` 改为返回全部报告的 `getReports()`；
- 工单详情中的单值 `report` 改为列表 `reports`；
- 文件类型和订单归属校验。

接口判断：本需求不新增“报告替换接口”，直接修改现有 `/design/report/link` 为批量追加接口。

### 9.3 模型

模型操作状态需要从设计阶段扩展到全部正常后续状态。按增量新增/单项删除的语义，现有接口基本足够，后端重点是：

- 新增后续状态白名单，不要扩大通用设计阶段白名单；
- 补充模型文件类型/归属校验；
- 评估生产后删除对影像查看器的影响；
- 检查生产/查看器是否仍引用旧模型文件。

## 10. 测试影响

需要补充或调整 `DesignFileServiceImplTest`：

1. `2030` 状态下模型新增成功；
2. `3010、3030、4010、5010、6010、8010` 等后续状态下模型新增、删除成功；
3. `2030` 及后续状态下报告新增多份成功；
4. 删除一份报告不会影响同订单其他报告；
5. 在已有报告基础上新增报告成功，旧报告不受影响；
6. 取消状态下报告和模型操作失败；
7. 经典案例订单仍然失败；
8. 非指定设计师仍然失败；
9. 模型绑定非 `10.6` 文件失败；
10. 报告绑定非 `10.5` 文件失败；
11. 新报告绑定失败时已有报告仍存在；
12. 工单详情返回完整报告列表；
13. 完成设计校验在存在一份或多份报告时均通过；
14. 删除模型后影像查询结果符合预期。

现有测试主要以 `DESIGN_IN_PROGRESS(2020)` 为默认状态，并将非设计阶段作为失败场景，新增状态白名单后需要同步更新测试夹具和断言。

## 11. 当前本地数据库观察

通过 MySQL MCP 查询到的当前有效订单状态分布包含：

- `2030` 设计完成：428 条；
- `3010~3030` 打印阶段：23 条；
- `4010` 后处理：未见当前有效数据；
- `5010~5050` 质检/包装：未见当前有效数据；
- `6010~6030` 仓储阶段：726 条；
- `8010` 已完成：33 条。

当前数据库中：

- `design_model` 约 12,460 条记录；
- `file_detail` 约 25,208 条记录；
- `design_review` 当前无有效记录。

以上数据说明模型和报告开放到后续状态会覆盖历史生产、仓储和已完成订单；多报告改造还会改变现有报告查询契约，必须重点评估权限、审计、历史查看和接口兼容影响。

## 12. 业务确认结果

本次已确认的实施约束如下：

1. 模型覆盖全部 `2030` 后续正常状态，取消状态 `9010` 除外。
2. “修改模型”定义为在已有集合上新增或删除指定模型，不要求全量替换。
3. 多报告同样只要求新增和删除指定报告，不要求专门的替换接口；现有 `/design/report/link` 已确定改为一次接收多个 `fileIds` 并批量追加。
4. 设计报告不允许取消订单操作。
5. 报告新增、删除、内容变动不记录版本或额外操作日志。
6. 设计完成后的报告修改不触发通知或重新审核。
7. 已开始打印或已出库的订单允许删除模型。
8. 删除全部模型或报告后，订单保持原状态。
9. 沿用当前权限逻辑：拥有 `design:EditFile` 的设计师管理员按现状可操作所有订单，普通设计师受指定设计师限制。
10. 工单详情接口将原单值 `report` 直接改为 `reports` 列表字段，不保留单值字段作为正式契约。
11. 现有 `/design/report/link` 改为一次接收多个 `fileIds` 并批量追加关联。
12. 报告查询接口直接返回数组列表，不保留单对象返回兼容层。
13. 模型和报告关联时必须校验文件业务类型归属。
14. 上传后关联失败时自动清理未关联的孤儿文件及存储对象。
15. 已开始打印或已出库订单允许删除模型。
16. 前端不针对不同订单状态展示差异化提示，保持统一操作反馈。

## 13. 代码、数据库与前端结构索引

- 设计附件控制器：`yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignAttachmentController.java`
- 设计文件服务：`yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignFileServiceImpl.java`
- 设计阶段公共校验：`yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/DesignQueryHelper.java`
- 设计完成校验：`yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`
- 状态枚举：`yigongbao-parent/yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowStatusEnum.java`
- 文件业务类型：`yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/FileBizTypeEnum.java`
- 通用文件服务：`yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/impl/FileServiceImpl.java`
- 可视化模型表：`sql/ddl-prod.sql` 中 `design_model`
- 文件表：`sql/ddl-prod.sql` 中 `file_detail`
- 影像模型读取：`yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/service/impl/ImagingServiceImpl.java`
- 查看器模型读取：`yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/v1/service/impl/ViewerServiceImpl.java`

## 14. 前端项目分析

分析目录：`D:/01_Project/02_Personal/医工宝/frontend/med-tech`。

### 14.1 设计工单操作菜单

`src/views/business/design.vue` 的 `getRowActions()` 当前行为：

- `2010` 待设计：不显示“上传设计文件”；
- `2020` 设计中：显示“上传设计文件”；
- 其他非取消状态：默认显示“上传设计文件”；
- `9010` 已取消：不显示上传操作；
- 上传菜单统一受 `design:Upload` 权限控制。

因此，前端列表层面对 `2030`、打印、后处理、质检、仓储和已完成状态并没有额外的状态硬编码拦截，模型/报告后续状态开放主要需要确保后端状态校验和弹窗内文件操作一致。取消订单前端已经不显示上传入口，但后端仍必须继续拦截。

### 14.2 模型上传与删除现状

`src/views/business/designComponents/fileDialog.vue` 中模型配置已经具备增量维护所需能力：

- 使用 `/basic/file/upload-multiple`；
- `uploadBatch: true`；
- 支持多文件批量上传；
- 打开弹窗时通过 `designModelList()` 加载全部已有模型；
- 已有模型逐项展示；
- 删除已有模型调用 `designModelDelete(modelId, orderId)`；
- 确认时只收集本次新增文件的 `fileId`，调用 `designModelLink(orderId, newModelFileIds)`。

这与“保留既有 10 个模型，在其基础上新增或删除指定模型”的业务语义一致。模型前端原则上不需要新增替换接口，主要需要：

1. 配合后端开放 `2030` 及后续正常状态；
2. 删除成功后刷新或保持本地列表状态；
3. 继续只关联 `!isExisting` 的新增文件，避免重复关联；
4. 检查后端失败时已上传但未绑定的文件提示和清理问题。

### 14.3 设计报告上传现状

当前报告配置存在明确的单文件限制：

- `uploadApi: '/yi/basic/file/upload'`；
- `uploadBatch: false`；
- `initExisting()` 调用 `designReportGet()`，只接收单个报告对象；
- 初始化后只构造一个已有报告项；
- 确认时虽收集 `newReportFileIds` 数组，但当前界面实际只允许单文件上传；
- `designReportLink(orderId, newReportFileIds)` 调用后端覆盖式关联；
- 删除已有报告按单个 `fileId` 调用 `designReportDelete()`。

因此当前前端无法正确表达多报告集合。后端改成多报告后，前端必须同步调整：

1. `designReportGet()` 返回 `DesignReportVO[]`；
2. `initExisting()` 将所有报告映射为多个 `isExisting` 文件项；
3. 报告上传改为支持多文件，使用 `/basic/file/upload-multiple` 和 `uploadBatch: true`；
4. 确认时将本次新增的全部 `fileId` 追加绑定，不删除已有报告；
5. 删除按钮继续按单个报告文件删除；
6. 后端现有报告关联接口统一支持一次传入全部 `fileIds` 并批量追加，前端一次请求完成本次新增报告关联。

### 14.4 通用上传组件能力

`src/components/uploadFileCustom.vue` 已经支持：

- 每种文件类型独立维护文件列表；
- `uploadBatch` 配置；
- 批量上传接口返回数组并按顺序回填每个文件的 `fileId`；
- 已有文件 `isExisting` 标识；
- 已有文件单项删除钩子 `onDelete`；
- 清空时逐项调用删除钩子。

因此多报告不需要重写通用上传组件，只需要调整设计文件弹窗的报告类型配置和 API 类型。需要重点验证批量接口返回顺序与上传文件顺序一致，否则会出现文件名和 `fileId` 错配。

### 14.5 API 类型和工单详情影响

`src/api/design.ts` 当前存在以下单值契约：

- `DesignWorkorderDetailVO.report: DesignReportVO | null`；
- `designReportGet(): Promise<DesignReportVO | null>`。

本需求已确定工单详情和报告查询直接切换为列表，因此前端应改为：

```ts
reports: DesignReportVO[]
```

并同步修改：

- `designReportGet()` 返回列表；
- `designWorkorderDetailDialog.vue` 的报告区域从单卡片改为 `v-for` 多卡片；
- 区块显示条件由 `workorder.report` 改为 `workorder.reports?.length`；
- 报告数量从 `report ? 1 : 0` 改为 `reports.length`；
- 下载动作逐项使用对应报告的下载地址；
- 空状态判断同步改为模型列表和报告列表共同判断。

### 14.6 前端配置值与后端配置一致性

弹窗会从配置中心加载：

- `design.model.allowed_extensions`；
- `design.model.max_size_mb`；
- `design.report.allowed_extensions`；
- `design.report.max_size_mb`。

本地前端默认值与后端当前配置并不完全一致，例如模型和报告默认扩展名集合较窄。正常情况下配置接口成功后会覆盖默认值，但应检查配置接口失败时是否会造成前端误拒绝合法文件。最终限制仍以后端校验为准。

### 14.7 前端实施边界

模型：

- 不新增替换接口；
- 保留批量追加上传；
- 保留已有模型逐项删除；
- 保留统一的操作反馈，不新增按状态区分的提示。

报告：

- 改为多文件选择和展示；
- 新上传报告只追加，不覆盖已有报告；
- 已有报告逐项删除；
- 工单详情字段由 `report` 改为 `reports`；
- 不增加版本、审核、通知或操作日志 UI；
- 不为取消订单开放入口。

### 14.8 前端测试影响

建议至少覆盖：

1. `2030`、`3010`、`3030`、`4010`、`6010`、`8010` 状态显示上传入口；
2. `9010` 不显示上传入口；
3. 已有 10 个模型时新增 2 个，原 10 个仍保留；
4. 删除单个模型后其他模型仍展示；
5. 已有多份报告时新增报告不覆盖旧报告；
6. 删除单份报告不影响其他报告；
7. 工单详情正确展示多份报告和总数；
8. 批量报告上传时每个文件与返回 `fileId` 正确对应；
9. 后端拒绝操作时文件列表和弹窗状态不会误显示为已绑定；
10. 取消订单不显示上传入口，即使直接调用接口也能正确提示失败。

### 14.9 前端分析结论

前端模型能力基本已经符合增量维护要求，主要是配合后端状态放开并验证后续状态下的操作反馈。

前端报告需要实质改造，原因不是权限按钮，而是当前数据模型、上传配置和详情展示全部按单文件设计。后端不需要新增“替换报告”接口，但现有报告关联接口必须取消覆盖语义；前端推荐将其改为批量追加关联，以一次操作完成多报告上传。

## 15. 后端实施记录

本轮已在隔离分支 `codex/design-attachment-analysis` 完成后端实现，未修改 `dev` 分支：

1. 新增仅用于模型/报告附件的状态校验，保留通用 `checkDesignPhase()` 原语义；允许设计阶段既有状态及设计完成后的正常状态，排除 `9010` 已取消。
2. 模型继续使用批量追加和单项删除接口；删除模型在生产、出库及已完成等允许状态下可用。
3. `/design/report/link` 改为批量接收 `fileIds` 并追加关联，不再覆盖历史报告；报告查询、设计工单详情、订单详情统一返回 `reports` 列表。
4. 模型/报告关联严格校验文件业务类型和当前业务归属，阻止跨业务或已归属文件被重复绑定。
5. 关联事务失败时通过独立事务清理本次上传且尚未关联业务的文件，降低数据库回滚后遗留孤儿文件的风险。
6. 补充了状态白名单、批量报告追加、报告保留、业务类型拒绝、孤儿清理、取消状态拒绝和列表契约相关测试。

验证结果：设计附件相关定向测试通过（83 个测试，0 失败、0 错误），并完成编译和 diff 检查。执行完整 `mvn test` 时，测试在未涉及本次改动的 `yigongbao-module-basic` 基线测试阶段停止，报告 41 个失败和 11 个错误，主要涉及既有测试 Mock 注入、Redis 连接及测试上下文问题；因此不能将完整 reactor 测试标记为通过。
