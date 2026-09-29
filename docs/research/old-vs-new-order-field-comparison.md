# 旧医工宝与新医工宝订单字段完整对比

更新时间：2026-09-28

## 1. 结论先行

旧库可以支撑“历史订单列表 + 历史详情 + 关联文件索引”的独立只读页面，但不能把全部历史记录无损伪装成新系统订单。建议新增历史订单归档模型和历史详情接口，保留旧值、旧 ID、原始数组和原始路径；不要强行写入 `order_main/order_item/order_file` 后复用现有流程。

按新版本实际 VO 和当前默认列配置评估：

| 范围 | 可直接复用/展示 | 可转换或推导 | 只能保留旧原文/需降级 | 旧库无法获得或语义不一致 |
|---|---:|---:|---:|---:|
| 订单列表默认列 | 约 10 项 | 约 12 项 | 约 5 项 | 约 8 项 |
| 订单详情顶层字段 | 约 12 项 | 约 15 项 | 约 8 项 | 约 15 项 |
| 明细、文件、设计包 | 订单项目和大部分文件可关联 | 可组装历史结构 | URL/状态/版本等需降级 | 新版审核/文件对象语义无法还原 |

这里的“可转换”表示可以为历史页提供可读结果，不表示满足新流程字段的完整约束。

## 2. 新版本字段来源

- 列表返回 `OrderListVO`，实际转换位于 `yigongbao-module-order/.../OrderQueryHelper.java:313`；重建项目由 `order_item` 批量填充（约 392 行起）。
- 详情返回 `OrderDetailVO`，主表字段通过 `BeanUtils.copyProperties` 复制，明细来自 `order_item`，文件由 `fillOrderFiles` 组装；字段定义见 `OrderDetailVO.java:29-400`。
- 当前默认列来自 `sys_config.config_key='order.column.config'`（id=18，JSON）；另外还有用户个人列配置。默认列包含订单编号、状态、加急、业务类型、订单类型、实体交付、提单机构、业务员及电话、部门、医院、地区、科室、医生及电话、患者、邮寄、设计师、期望交付、费用、影像评估、重建项目、四类时间、创建时间、设计师备注、虚拟单号和操作列。
- 新系统字典/枚举是代码值：订单类型 1/2，实体交付 0/1/2，性别 `10.1/10.2`，阶段和状态为流程枚举；旧库主要存中文或自由文本，不能直接当作新代码值。

## 3. 订单列表字段映射

旧主订单表为 `jy_order`，订单号 `od_num`；相关旧字段和样本统计见 `docs/research/old-schema-analysis.md`、`docs/research/old-data-attachment-production-samples.md`。

| 新 `OrderListVO`/默认列 | 旧来源 | 结论 | 处理建议 |
|---|---|---|---|
| `id` | 无新库 ID；可用旧 `jy_order.od_id` | 转换 | 作为 `legacy_order_id`，不要当新 `order_main.id` |
| `orderCode` | `jy_order.od_num` | 直接 | 保留原订单号；旧样本为 `JY...` 且基本唯一 |
| `publicOrderCode` | 无对应字段 | 缺失 | null；如需展示另建“旧系统外部单号”原文 |
| `statusName`/`status` | `jy_order.od_status` | 直接取值 | 历史列表直接保存和展示旧状态原文；不转换新状态码。若需要颜色/统计，另做辅助配置。旧库实际有 12 种状态 |
| `statusColor` | 无 | 缺失/派生 | 历史页按旧状态配置颜色，不写新流程颜色 |
| `isUrgent` | 无可靠订单级字段 | 缺失 | null/“未知” |
| `businessTypeName` | `od_orderType`（业务/代理/试用/测试） | 原文可用 | 作为 `legacy_business_type`；不要当新字典 code |
| `orderTypeName` | 无可靠医疗器械/非医疗器械字段 | 缺失 | null 或“旧系统未区分” |
| `needsPhysicalDeliveryName` | `od_print`（打印/不打印） | 部分转换 | 仅映射“打印/不打印”；不能推出“实体交付/异地打印”完整语义 |
| `orgName` | 无明确机构字段；`od_dept` 更像业务部门 | 不应猜测 | 单独展示旧部门/来源组织原文 |
| `operatorName` | `od_clerkname` | 直接 | 原文 |
| `operatorPhone` | `od_clerkTel` | 直接但稀疏 | 原文；缺失保留空 |
| `operatorDeptName` | `od_dept` | 可转换 | 原文；无新部门 ID |
| `operatorId` | `od_user_id` | 仅旧 ID | 保存 `legacy_operator_id`，不能直接关联新用户 |
| `hospitalName` | `od_hospital` | 直接 | 原文 |
| `hospitalId` | 无稳定新医院 ID | 缺失 | 可建立旧医院维表后再映射 |
| `areaName` | `od_dist` | 直接 | 原文 |
| `areaId/fullAreaName` | 无完整行政区层级/ID | 部分 | 只保留旧地区字符串 |
| `hospitalDeptName` | `od_office` | 直接 | 原文 |
| `hospitalDeptId` | 无 | 缺失 | null |
| `doctorName` | `od_docname` | 直接 | 原文 |
| `doctorPhone` | `od_doctel` | 直接 | 原文 |
| `doctorId` | `od_docid` | 仅旧 ID | 保存旧医生 ID，不直接写新 doctor_id |
| `patientName` | `od_patient` | 直接 | 原文 |
| `patientAge` | `od_patientAge` | 类型转换 | 非法/空值保留原文，历史页可用字符串年龄 |
| `patientGenderName` | `od_patientSex`（实际为男/女） | 轻转换 | 展示原文；不强转 10.1/10.2 |
| `isPostal/postalAddress` | `jy_instruct.inst_mail/inst_mailaddress` | 仅部分订单可推导 | 以指令表为准并标注来源；不要从订单主表强填 |
| `designerName` | 无可靠订单级设计师字段；可从流程/模型间接推断 | 不完整 | 仅展示流程表推断结果，标“推断” |
| `designerId` | 无 | 缺失 | null |
| `expectedDeliveryDate` | `od_preDelivery`（仅 114 条非空） | 可转换但稀疏 | 解析失败保留原文；不要用 `od_delivery` 代替而不标注语义 |
| `estimatedCost` | `od_subjectCost`（数字、协议价、待定、十六进制样式混杂） | 不可安全数值化 | 历史页以字符串展示，同时保留原文 |
| `dataEvaluationOpinion` | `od_assess` | 原文可用 | 作为旧影像评估原文；不保证等价新审核意见 |
| `rebuildProjectList` | `od_requirePart/od_rebuildSub/od_subjectExpl` + `jy_module` | 可组装 | 保留 ID 数组；能查到模块名称则展示名称，否则展示 ID。数组长度存在 17 条不一致记录 |
| `designStartTime` | `ds_time`（旧字段语义需核验） | 可转换/不确定 | 保存原始值和解析值，无法解析不丢弃 |
| `designSubmitTime` | `dc_time`（旧字段语义需核验） | 可转换/不确定 | 同上 |
| `productionStartTime` | `ps_time` 或流程首条生产记录 | 可推导 | 以 `jy_flow` 时间线重建并标来源 |
| `productionEndTime` | `pc_time` 或流程完成记录 | 可推导 | 以流程表重建；异常值如 1899-12-31、`NaN-aN-aN` 必须保留原文并置空解析值 |
| `createTime` | `od_createtime` | 直接 | 旧样本范围 2021-07-14 至 2026-08-24 |
| `designerRemark` | 无明确对应 | 缺失 | null；不要把 `od_others` 冒充设计师备注 |
| `regionalAudit/designAudit` | 无审核人/审核时间/审核备注完整结构 | 缺失 | 可展示旧状态，但不生成新版 `AuditInfo` |
| `version` | 无 | 缺失 | 固定 null/1，仅历史页面内部版本可另设 |

## 4. 订单详情顶层字段

详情复用列表的大部分基础字段，另有处理人、审核、项目、文件和设计包。

| `OrderDetailVO` 字段组 | 旧数据可得性 | 说明 |
|---|---|---|
| 基础身份：`id/orderCode/publicOrderCode` | 部分 | `od_id/od_num` 可得，虚拟单号不可得 |
| 类型：`orderType* / businessType* / needsPhysicalDelivery*` | 部分 | 业务类型和打印可展示；新版 code/name 不能完整还原 |
| 组织人员：机构、业务员、医院、科室、医生 | 名称大多可得，ID 多不可得 | 保存旧 ID 与名称双份，禁止按名称盲配新组织 |
| 患者：姓名、年龄、性别 | 可得 | 年龄/性别按字符串兼容；旧订单电话在 `od_patientTel` 中仅少量有值，详情可额外展示 |
| `isUrgent` | 不可得 | 旧库无稳定来源 |
| `isPostal/postalAddress` | 部分 | 从指令表关联，可能一个订单多个指令 |
| `expectedDeliveryDate` | 稀疏 | `od_preDelivery` 原文/解析值 |
| `actualCompleteTime` | 不可靠 | 可从流程完成事件推导，不能直接等同旧 `od_delivery` |
| `currentHandlerId/currentHandlerName` | 不可完整还原 | `jy_flow.flow_worker` 只能构造历史处理人轨迹，不代表当前处理人 |
| `designerId/designerName/producerId` | 不完整 | 只能从模型或流程记录推断；不生成新用户关联 |
| `auditRemark/designReviewRemark/auditProgress/auditStage` | 不可无损还原 | `od_assess` 是影像评估文本，不等同新版审核结构 |
| `regionalAudit/designAudit` | 缺失 | 旧表没有审核人、时间、备注的成组记录 |
| `estimatedCost` | 仅字符串 | `od_subjectCost` 混合格式，不能安全转 `BigDecimal` |
| `dataEvaluationOpinion` | 可得原文 | 来自 `od_assess`，显示为“旧系统影像评估意见” |
| `designerRemark` | 缺失 | 无明确旧字段 |
| 时间、`version/updateTime` | 创建时间可得，其他部分可推导，版本不可得 | 对异常日期保留 raw 值 |

## 5. 明细项目映射

新版 `items` 每项有 `bodyPartId/bodyPartName/projectId/projectName/projectEstimatedHours/projectDesc/formingRequirement/otherRequirement/sortOrder`。

- `bodyPartId/bodyPartName`：`od_requirePart` 中的 `jy_module` ID 及模块名称，可按逗号拆分。
- `projectId/projectName`：`od_rebuildSub` 中的模块 ID 及名称，可按逗号拆分。
- `projectDesc`：可尝试用 `od_subjectExpl` 关联 `jy_module` 名称，但旧数据中该字段也存在多值/长文本，必须保留原数组和原文。
- `otherRequirement`：可映射 `od_others` 原文。
- `formingRequirement`：`od_print` 只能作为弱映射（打印/不打印），不能假定等同新成型要求。
- `projectEstimatedHours`：旧库没有可靠对应，置空。
- `sortOrder`：使用旧数组顺序，并保留原始 ID 数组。

因此，详情页应同时展示“结构化拆分结果”和“旧字段原文”，以便用户核对。

## 6. 文件、数据包和报告

新版详情文件对象要求 `fileId/fileName/fileCategory/fileUrl/downloadUrl/thUrl/fileSize/fileExt`；设计包还要求包号、序号、上传时间、包内文件、最新说明书/图纸版本等。

旧库可建立如下只读关联：

| 新详情区域 | 旧表/关联 | 覆盖与限制 |
|---|---|---|
| 影像/原始文件 | `jy_files.order_num -> jy_order.od_num` | 35,638 行，35,586 个订单；45 行订单号为空。路径和文件分类可保留，但 URL、缩略图、对象存储 ID 不存在 |
| 设计模型 | `jy_model.od_num -> jy_order.od_num` | 243,826 行，35,197 个订单；应按订单号关联，不依赖 `jy_order_filemodel.model_id`（24 个旧 model_id 全部悬空） |
| 设计指令/数据包 | `jy_instruct.od_num`，包号 `inst_packnum` | 24,555 行、23,206 个订单；包号有自由文本，不能全部转换新版 packageCode |
| 说明书/图纸 PDF | `jy_picture.pic_stb2`，并按包/订单关联 | 30,226 行，全部可按订单关联；新版版本、确认状态等元数据缺失 |
| STL/生产文件 | `jy_stlfile`，由 `jy_instruct.stl_arrs` 或 `jy_flow.stl_arrs` 的 ID 数组关联 | 71,656 行，除 1 条外可关联；表本身没有路径字段，且扩展名不只 STL |
| 订单文件桥接 | `jy_order_filemodel` | `fileid` 仍有 73 个悬空引用，不能作为唯一链路；保留原桥接记录供审计 |

物理文件是迁移成败的关键：旧表路径主要指向 `D:/yj`、`C:/yj`、`D:/PicturePDF`、`D:/InstructPDF` 等 Windows 目录；数据库没有 OSS 对象键。若旧磁盘、共享目录或备份不可访问，数据库迁移仍可完成，但只能展示文件名、旧路径和“文件不可用”，不能提供下载。

## 7. 推荐实现边界

1. 新增 `legacy_order`、`legacy_order_item`、`legacy_order_file`、`legacy_order_flow`（或 JSON 快照）等归档表，主键使用新生成的归档 ID，另存 `legacy_order_id/order_no`。
2. 列表接口只返回历史页需要的字段，优先使用字符串：订单号、旧状态、医院、科室、医生、患者、业务类型、旧地区、创建时间、文件数量、可用文件数量。
3. 详情接口返回三层：标准化可读字段、旧表原始字段、关联文件索引；不要复用新订单的提交/审核/修改接口。
4. 所有转换字段增加 `source_field` 或 `mapping_status`（direct/converted/raw/missing），对日期、金额、人员 ID 保留 raw 值；状态字段直接保留旧字符串。
5. 文件迁移分为“元数据迁移”和“物理文件迁移”：先完成数据库只读查询，再按路径清单复制到新受控存储并生成下载 URL；无法复制的文件不得伪造 URL。
6. 旧状态、旧业务类型、旧模块 ID 建立独立字典映射表；未知值继续显示原文，避免污染新字典和新流程统计。

## 8. 验收标准

- 旧订单总数、订单号去重数、状态分布与导出核对一致。
- 每条历史订单可打开详情；项目数组长度、原始数组、旧状态原文可核对。
- 文件页能区分“数据库有记录/物理文件已迁移/物理文件缺失/路径待确认”。
- 新订单流程、权限、状态统计不读取历史归档表，历史页不会触发新订单写操作。
- 对 1899 年、`null`、空字符串、非法日期、混合金额和悬空文件引用有可追溯处理结果。
