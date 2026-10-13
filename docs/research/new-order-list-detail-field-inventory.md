# 新版订单列表与详情字段清单

## 1. 结论摘要

新版订单查询不是单一表的简单回显：订单主信息来自 `order_main`，列表转换还会翻译字典/流程名称并批量补充 `order_item` 项目；详情在主订单、订单明细、订单文件之外，还由 Controller 追加设计数据包和设计报告。历史只读页不需要复刻这些流程字段，但若要保持“订单详情”的信息覆盖，应至少保存订单主快照、项目快照、影像/报告附件元数据，以及可选的设计/生产文件元数据。

## 2. 接口入口与查询参数

| 用途 | HTTP 接口 | 请求体/参数 | 证据 |
|---|---|---|---|
| 订单分页 | `POST /order/page` | `OrderPageDTO`：`pageNum`、`pageSize`、统一关键词 `orderCode`、`hospitalId`、`areaId`、`doctorName`、`patientName`、`businessType`、`needsPhysicalDelivery`、创建时间起止、`bodyPartIds`、`projectIds`、`operatorId`、`status`、`phase`、`sortField`、`sortOrder` | `.../OrderController.java:101-105`；`.../OrderPageDTO.java:11-120` |
| 订单详情 | `GET /order/{id}` | 路径 `id`（新版 `order_main.id`） | `.../OrderController.java:113-127` |
| 列配置读取 | `GET /order/column-config` | 无；用户个人配置优先于系统默认 | `.../OrderController.java:205-211`；`.../OrderQueryHelper.java:440-496` |

列表查询默认按创建时间倒序，排序字段经过白名单映射；关键词会匹配订单号、虚拟单号、机构、操作员、医院、患者、医生及 `order_item.project_name`。证据：`.../OrderMainServiceImpl.java:256-343`、`.../OrderQueryHelper.java:55-80`。

## 3. 新版订单列表 VO

来源：`.../OrderListVO.java:19-359`；实际填充：`.../OrderQueryHelper.java:303-381`。除特别注明外，以下字段由 `order_main` 同名字段直接复制；`*Name`、`statusColor` 等为翻译/计算字段。

| 字段 | 含义/来源性质 |
|---|---|
| `id` | 订单主键，直接来自 `order_main.id` |
| `orderCode`、`publicOrderCode` | 订单编号、对外虚拟单号，直接字段 |
| `orderType`、`orderTypeName` | 订单类型码；名称由类型枚举翻译 |
| `needsPhysicalDelivery`、`needsPhysicalDeliveryName` | 实体交付码及展示名称，名称计算 |
| `businessType`、`businessTypeName` | 业务类型字典码及字典名称，名称查询字典 |
| `orgId`、`orgName` | 提单机构 ID/冗余名称，主表直接字段 |
| `operatorId`、`operatorName`、`operatorPhone` | 操作员 ID、姓名、电话，主表冗余字段 |
| `operatorDeptId`、`operatorDeptName` | 提单人所属部门 ID/名称，主表冗余字段 |
| `hospitalId`、`hospitalName` | 医院 ID/名称，主表冗余字段 |
| `areaId`、`areaName`、`fullAreaName` | 地区 ID、名称、完整路径，主表冗余字段 |
| `hospitalDeptId`、`hospitalDeptName` | 医院科室 ID/名称，主表冗余字段 |
| `doctorId`、`doctorName`、`doctorPhone` | 医生信息，主表字段 |
| `patientName`、`patientAge`、`patientGender`、`patientGenderName` | 患者信息；性别名称由字典常量翻译 |
| `isClassicCase` | 经典案例标记，VO 有定义但当前 `toOrderListVO` 未填充，不能假定列表接口一定返回值 |
| `isUrgent`、`isPostal`、`postalAddress` | 加急、邮寄标记及地址，主表字段 |
| `designerId`、`designerName` | 设计师，主表冗余字段 |
| `expectedDeliveryDate`、`estimatedCost` | 期望交付时间、预估费用，主表字段 |
| `dataEvaluationOpinion`、`designerRemark` | 影像评估意见、设计师备注；后者在 VO 中定义并由 helper 填充 |
| `phase`、`phaseName` | 流程阶段码及阶段名称；名称由流程枚举翻译 |
| `status`、`statusName`、`statusColor` | 流程状态码、名称、标签颜色；名称/颜色均为计算字段 |
| `regionalAudit`、`designAudit` | 审核信息对象，由 `OrderConvert.fillAuditInfo` 根据主表审核字段组装；列表查询在 `OrderMainServiceImpl.java:357-359` 填充 |
| `designStartTime`、`designSubmitTime`、`productionStartTime`、`productionEndTime`、`createTime` | 生命周期时间，主表字段 |
| `version` | 乐观锁版本，主表字段 |
| `rebuildProjectList[]` | 列表项目摘要，批量从 `order_item` 填充，非主表字段；每项含 `bodyPartName`、`projectName`、`categoryCode`、`categoryName`、`count`、`projectDesc`、`formingRequirement`、`otherRequirement`（`OrderListVO.java:305-359`） |

列表分页转换和项目批量填充位置：`.../OrderMainServiceImpl.java:345-365`。前端内置默认可见列为订单编号、订单类型、业务类型、医院、患者、重建项目、加急、邮寄、医生、设计师、期望交付、预计费用：`frontend/med-tech/src/views/business/orderComponents/orderList.vue:950-963`。

## 4. 系统默认订单列配置

代码读取 `sys_config` 的 `ORDER_COLUMN_CONFIG`（键值在 `SystemConfigKeyEnum` 中），运行时优先个人用户配置；仓库初始化配置位于 `sql/init.sql:339`。该配置比前端内置列更完整，字段为：

`orderCode`、`statusName`、`isUrgent`、`businessTypeName`、`orderTypeName`、`needsPhysicalDeliveryName`、`orgName`、`operatorName`、`operatorPhone`、`operatorDeptName`、`hospitalName`、`areaName`、`hospitalDeptName`、`doctorName`、`doctorPhone`、`patientName`、`patientAge`、`patientGenderName`、`isPostal`、`postalAddress`、`designerName`、`expectedDeliveryDate`、`estimatedCost`、`dataEvaluationOpinion`、`rebuildProjectList`、`designStartTime`、`designSubmitTime`、`productionStartTime`、`productionEndTime`、`createTime`、`action`。

其中 `action` 是前端操作列，不是 VO 数据字段；`statusName` 等展示名称必须在历史页改成旧状态原文或自定义历史状态名称，不能把旧状态码直接当新版流程状态码。

## 5. 新版订单详情 VO

来源：`.../OrderDetailVO.java:21-490`；组装流程：`.../OrderMainServiceImpl.java:402-437`。详情包含以下主订单字段（大部分与列表重叠）：

| 分组 | 字段 |
|---|---|
| 主键/类型 | `id`、`orderCode`、`publicOrderCode`、`orderType`、`orderTypeName`、`needsPhysicalDelivery`、`needsPhysicalDeliveryName`、`businessType`、`businessTypeName` |
| 机构/医院/人员 | `orgId`、`orgName`、`operatorId`、`operatorName`、`operatorPhone`、`operatorDeptId`、`operatorDeptName`、`hospitalId`、`hospitalName`、`areaId`、`areaName`、`fullAreaName`、`hospitalDeptId`、`hospitalDeptName`、`doctorId`、`doctorName`、`doctorPhone` |
| 患者/业务 | `patientName`、`patientAge`、`patientGender`、`patientGenderName`、`isUrgent`、`isPostal`、`postalAddress` |
| 时效/流程 | `expectedDeliveryDate`、`designStartTime`、`designSubmitTime`、`actualCompleteTime`、`phase`、`phaseName`、`status`、`statusName`、`statusColor` |
| 当前处理与审核 | `currentHandlerId`、`currentHandlerName`、`designerId`、`designerName`、`producerId`、`auditRemark`、`designReviewRemark`、`estimatedCost`、`dataEvaluationOpinion`、`designerRemark`、`auditProgress`、`auditStage`、`regionalAudit`、`designAudit` |
| 时间/版本 | `createTime`、`updateTime`、`version` |
| 详情计算字段 | `itemCount`、`availableActions`；前者为明细计数，后者由流程引擎计算。历史只读页不应暴露/实现 `availableActions` |

详情额外字段与实体映射要注意：`producerName` 在前端类型声明中存在，但后端 `OrderDetailVO` 只有 `producerId`（`OrderDetailVO.java:261-265`），因此前端不能把 `producerName` 视为稳定后端字段；`auditProgress`、`auditStage` 也属于面向流程按钮的计算描述。

## 6. 详情嵌套结构和文件字段

### 6.1 `items[]` 订单明细

`OrderDetailVO.OrderItemVO`（`OrderDetailVO.java:367-427`）字段：`id`、`bodyPartId`、`bodyPartName`、`projectId`、`projectName`、`projectEstimatedHours`、`projectDesc`、`formingRequirement`、`otherRequirement`、`sortOrder`。数据由 `order_item` 查询，按 `sortOrder` 升序（`OrderMainServiceImpl.java:423-430`）。`categoryCode/categoryName` 虽出现在前端 TypeScript 的 `OrderItemVO`（`frontend/med-tech/src/api/order.ts:254-268`），但后端详情内嵌类没有这两个字段，属于版本漂移，历史页应按实际后端响应兼容。

### 6.2 影像、报告、审批文件

`imageDataFiles[]`、`imageReportFiles[]`、`approvalFiles[]` 的元素为 `OrderDetailVO.OrderFileVO`（`OrderDetailVO.java:429-490`）：`fileId`、`fileName`、`fileCategory`、`fileCategoryName`、`fileUrl`、`downloadUrl`、`thUrl`、`fileSize`、`fileSizeText`、`fileExt`。订单详情通过 `fillOrderFiles` 查询并映射订单文件；这些字段中的 URL/缩略图/短时效下载地址是新版文件服务计算出来的，不是订单主表直接字段。

### 6.3 设计数据包与报告

Controller 在 `OrderMainServiceImpl.getOrderDetail` 之后调用 `DesignFileQueryService`，再写入 `packageList` 和 `report`（`OrderController.java:113-126`）。结构定义在 `DesignFileDetailVO.java:14-71`：

- `packageList[]`：`id`、`orderId`、`orderCode`、`publicOrderCode`、`packageCode`、`packageSeq`、`fileId`、`fileName`、`fileUrl`、`downloadUrl`、`fileSize`、`fileCount`、`uploadTime`、`files[]`、`latestInstruction`、`latestDrawing`、`latestDrawings`。
- `package.files[]`：`id`、`packageId`、`fileName`、`fileExt`、`filePath`、`fileSize`、`sortOrder`、`hasPrintInfo`、`fileUrl`、`downloadUrl`。
- `latestInstruction/latestDrawing(s)`：`id`、`version`、`versionSeq`、`sourceType`、`templateFileId`、`templateFileUrl`、`templateDownloadUrl`、`revisedFileId`、`revisedFileUrl`、`revisedDownloadUrl`、`generateTime`、`revisedUploadTime`、`isConfirmed`、`productCategory`、`confirmTime`。
- `report`：通用 `FileVO`，前端主要使用 `fileName`、`fileSizeText`、`fileUrl/downloadUrl`；实际完整字段应以 `.../module/basic/file/vo/FileVO.java` 为准。

这些字段属于设计模块和文件模块关联数据，不能只靠 `order_main` 还原。历史迁移若不要求复刻新版设计包结构，可以在历史表中统一保存文件名、类别、旧路径、文件大小、存在性和下载代理信息。

## 7. 对历史只读页的字段取舍建议

建议保留新版列表/详情中对查询最有价值、且不依赖新版流程的快照字段：订单编号、旧状态原文、业务类型原文、医院/科室/医生/患者、项目/部位/说明、创建/交付/完成时间、旧设计师/业务员、费用原文、邮寄/加急原文、旧订单 ID，以及所有文件的旧文件名、类别、路径、扩展名、大小和可用状态。

以下新版字段无需强行迁移：`publicOrderCode`、新版 `phase/status`、`availableActions`、流程颜色、当前处理人、新版机构/医院/医生 ID、乐观锁 `version`、新版设计包 ID；如果确实需要保留，可作为 `legacy_*_raw` 字符串或原始 JSON 保存，避免伪造新版业务语义。新版 `packageList`、生产关联和下载 URL 也应按“历史附件/历史数据包”独立建模，不能写入新版设计/生产流程表。

## 8. 证据文件索引

- 后端接口：`yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderController.java`
- 列表与详情 VO：`.../vo/order/OrderListVO.java`、`.../vo/order/OrderDetailVO.java`
- 主表字段：`yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/entity/OrderMainEntity.java`
- 查询/翻译/列配置：`.../helper/OrderQueryHelper.java`、`.../service/impl/OrderMainServiceImpl.java`
- 设计文件嵌套 VO：`.../vo/order/DesignFileDetailVO.java`
- 前端列配置与类型：`frontend/med-tech/src/views/business/orderComponents/orderList.vue`、`frontend/med-tech/src/api/order.ts`
- 系统默认列配置：`sql/init.sql:339`（运行时来源为 `sys_config` 的订单列配置键）
