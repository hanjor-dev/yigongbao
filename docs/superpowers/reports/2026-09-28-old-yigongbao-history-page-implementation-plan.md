# 旧版医工宝历史订单只读页实施方案

> 日期：2026-09-28  
> 依据：旧库 DDL、`sql/old/` 实际数据导出抽样与流式统计、本地新版 `yigongbao` 数据库结构  
> 目标：新增独立菜单页，查询旧版历史订单、详情和关联文件；不恢复旧流程，不让历史数据参与新版业务

## 1. 最终方案

采用“**历史归档专表 + 独立只读接口 + 独立前端菜单**”，不把旧记录转换成新版 `order_main/design_*/production_*` 正式业务记录。

历史页面以旧数据中的可读字符串快照为准：状态显示旧状态原文，机构、医院、科室、业务员、医生、设计师和产品信息均可直接显示旧名称。旧 ID 只在后台保留为迁移幂等键和跨表关联键，不要求匹配新版 ID，也不作为前端展示字段。无查询或追溯价值的旧权限、密码、日志、流程运行字段不迁移。

这样可以避免伪造新版必填字段、污染现有统计和状态机，同时最大限度保留历史原貌。

## 2. 实际数据结论

### 2.1 订单主体

`jy_order.sql` 共 37,632 条有效 INSERT：

- `od_num` 全部非空且本次导出中无重复，可作为历史业务订单号；仍以旧 `od_id` 作为来源唯一键。
- 非空创建时间范围为 2021-07-14 至 2026-08-24。
- 新库现有 946 个正式订单，时间范围从 2026-06-26 起；新订单号没有 `JY` 前缀。两套数据时间存在重叠，但订单号命名空间当前不冲突。
- 旧业务类型只有：业务 33,232、代理 3,618、试用 548、测试 234。历史页可直接显示原文，无需转换新版字典码。
- 打印要求只有：打印 31,807、不打印 5,810、空 15。它比新版 `order_type` 更接近“是否实体交付”，但历史页无需硬映射。
- 患者性别实际保存“男/女”，不是 DDL 注释中的 `0/1`，必须以实际数据为准。

旧状态共有 12 种：

| 旧状态 | 数量 |
|---|---:|
| 生产完成 | 29,333 |
| 设计完成 | 7,387 |
| 已取消 | 330 |
| 草稿 | 159 |
| 待生产 | 106 |
| 设计中 | 95 |
| 数据审核未通过 | 92 |
| 医工审核 | 72 |
| 已完成 | 52 |
| 生产中 | 3 |
| 已发货 | 2 |
| 模型审核未通过 | 1 |

页面直接按这些字符串筛选和展示，不映射新版 `FlowStatusEnum`。

### 2.2 项目信息

`od_requirePart`、`od_rebuildSub`、`od_subjectExpl` 实际主要保存 `jy_module.id_part`，多项目时使用逗号分隔：

- 36,550 条为单项目；
- 968 条为 2 项；
- 69 条为 3 项；
- 24 条为 4 项；
- 3 条为 5 项；
- 1 条为 6 项；
- 只有 17 条三个字段的数组长度不一致。

迁移时按位置拆分三组 ID，并用 `jy_module` 转成中文名称。17 条长度异常记录按最长数组保留，缺少的说明字段置空，同时保存三个原始字符串供核验。

`jy_module` 只作为迁移期字典使用，不需要迁入新版主数据表。历史项目明细保存解析后的部位、项目和说明字符串即可。

### 2.3 收费与其他特殊字段

- `od_fee` 混合保存“是/否”、金额、协议价、待定等内容，不能转成布尔或金额，应命名为 `fee_text` 原样保存。
- `od_subjectCost` 中约 22,813 条是固定长度的十六进制样式不透明文本，14,816 条是数字，3 条为空。没有旧代码解码规则前统一按 `subject_cost_text` 保存；列表默认不展示，详情可显示“原始预计收费”。
- `od_preDelivery` 仅 114 条非空，不应作为主要时间字段。
- `od_patientTel` 仅 686 条非空；若业务不要求查看患者电话，可以不迁移，减少敏感数据。
- `is_click` 是旧页面点击状态数组，对历史查询无业务价值，不迁移。
- `jy_users` 的旧密码和盐绝不迁移。

### 2.4 关联数据规模

| 来源表 | 行数 | 用途 |
|---|---:|---|
| `jy_files` | 35,638 | CT、影像报告、其他附件 |
| `jy_order_filemodel` | 36,476 | 订单与普通附件/模型桥接 |
| `jy_model` | 243,826 | 设计模型、报告和备份路径 |
| `jy_picture` | 30,226 | 数据包图片/图纸/PDF |
| `jy_instruct` | 24,555 | 数据包和生产指令快照 |
| `jy_flow` | 19,446 | 流转卡、批号、设备和生产参数 |
| `jy_stlfile` | 71,656 | 包内文件和生产产品快照；内容并不全是 STL |

已确认的核心关联：

- `jy_flow` 可通过 `inst_id`、包号或订单号关联 `jy_instruct`；本次统计 19,446 条均可关联。
- `jy_picture` 可通过包号和订单号关联指令/订单。
- `jy_stlfile` 71,656 条中有 71,655 条可通过包号和订单号关联指令，仅 1 条悬空，应进入迁移异常表而不是丢弃。
- `jy_order_filemodel.fileid` 的 36,452 个非空引用中 36,379 个命中，73 个悬空；仅有的 24 个 `model_id` 全部悬空，因此设计模型必须按 `jy_model.od_num` 关联。

生产字段应继续遵循“原串优先”：`jy_flow.flow_stb2` 实际保存“生产中/生产完成”，多个产品字段是位置对齐的逗号数组；旧时间字段还存在字面量 `null`、`1899-12-31`、`NaN-aN-aN aN:aN` 等脏值。归档表保留原始时间字符串，另外提供可空的标准时间列；转换失败不能阻断整单迁移。
- `jy_order_filemodel.fileid` 大部分能命中 `jy_files`；其中少量空值/悬空需记录。该桥接表的 `model_id` 可靠性较差，模型应优先按 `jy_model.od_num` 关联订单。

## 3. 迁移数据范围

### 3.1 必迁

- `jy_order`：历史订单列表和基本详情。
- `jy_module`：迁移时解析项目 ID；只作为 ETL 字典。
- `jy_files`、`jy_order_filemodel`：订单原始资料和附件关系。
- `jy_model`：模型、重建报告、备份等设计文件元数据。
- `jy_instruct`：历史数据包和产品指令快照。
- `jy_picture`：设计/生产包图片和文档。
- `jy_flow`：生产详情。
- `jy_stlfile`：包内文件和产品详情。
- 上述记录对应的实体文件存储。

### 3.2 可选

- `jy_users`：仅当页面需要按旧业务员工号筛选；默认订单已有姓名和电话快照，无需迁移整表。
- `jy_doctor`、`jy_hospital`、`jy_office`：仅作为迁移校验或筛选候选字典；默认直接使用订单快照。
- `jy_productstyle`、`jy_register`：用于把生产数组中的产品、规格、材质、颜色 ID 转为名称；解析后不迁移旧字典表。
- `jy_msgprompt`：只有明确要求展示旧通知时才迁移。

### 3.3 不迁

- `act_*` 流程表：不恢复旧审批流程。
- `sys_oper_log`、`sys_logininfor`：体量大且不属于历史订单详情。
- 旧角色、菜单、岗位、密码、登录状态和定时任务表。
- `is_click` 等纯旧页面状态字段。

## 4. 推荐归档表

### 4.1 `legacy_order`

一张历史订单一行。建议字段：

```text
id                         新版归档主键
source_order_id            jy_order.od_id，迁移幂等键
order_no                   jy_order.od_num
legacy_status              旧状态原文
business_type_text         业务/代理/试用/测试
print_requirement_text     打印/不打印
fee_text                   收费情况原文
subject_cost_text          预计收费原始内容
dept_name/dept_code
clerk_name/clerk_phone
region_name/hospital_name/hospital_dept_name
doctor_name/doctor_phone
patient_name/patient_age_text/patient_gender_text
delivery_time/pre_delivery_time
data_evaluation_text
other_requirement_text
designer_name
design_start_time/design_complete_time
production_start_time/production_complete_time
create_time/submit_time
project_summary             解析后的项目摘要，供列表展示
source_project_part_ids     原 od_requirePart
source_project_ids          原 od_rebuildSub
source_project_desc_ids     原 od_subjectExpl
migration_batch/status/error
```

约束和索引：

- `UNIQUE(source_order_id)`；
- `UNIQUE(order_no)` 可在本批数据验证后建立；
- 索引：`create_time`、`legacy_status`、`business_type_text`、`hospital_name`、`clerk_name`；
- 37,632 条规模无需引入搜索引擎。

### 4.2 `legacy_order_project`

每个历史项目一行：`legacy_order_id`、序号、部位名称、项目名称、说明文字、三个源 ID。前端只展示名称；源 ID 仅供追溯和幂等更新。

### 4.3 `legacy_order_file`

统一承接 `jy_files/jy_model/jy_picture/jy_stlfile` 的可下载文件：

```text
legacy_order_id
legacy_package_id          可空
source_table/source_id/source_slot
file_category              IMAGE_DATA/REPORT/OTHER/MODEL/PICTURE/PACKAGE_FILE
display_name
source_name/source_path
file_size_text
storage_file_id            复制到新版存储后的 file_detail.id，可空
download_url               可空
file_status                PENDING/AVAILABLE/MISSING/FAILED
checksum/error_message
```

唯一键使用 `(source_table, source_id, source_slot)`。一条 `jy_files` 可拆成 CT、报告、其他附件三行；一条 `jy_model` 可拆成模型、重建报告、备份等多行。

### 4.4 `legacy_order_package`

来自 `jy_instruct`：保存源 `inst_id`、包号、文件数、交付/联系人/客户/邮寄/备注、产品标识、产品/规格/材质/颜色/数量原文及 STL 数组原文。以字符串快照为主，不要求新版产品、规格和注册证 ID。

### 4.5 `legacy_order_production`

来自 `jy_flow`：保存源 `flow_id/inst_id`、包号、批号、产品编号、产品/规格/材质/颜色/数量、打印开始结束、设备编号、作业员、原材料批号和后处理参数。多值数组可保存解析后的 JSON 和原始字符串。

### 4.6 `legacy_package_file`

来自 `jy_stlfile`：保存源 `stlf_id`、包号、文件名、产品编号、产品/规格/材质/颜色、数量、重量、状态原文、出库原文和相关时间。

该旧表名称具有误导性：71,656 条中除 STL 外，还包含 PDF、XLSX、DCM、PPTX、图片和无扩展名文件；`prod_id` 全部为空。因此它应建模为“历史数据包文件”，而不是强行建模为新版生产产品。实际分类按扩展名和来源信息判断。

### 4.7 `legacy_migration_run/error`

记录迁移批次、源文件校验和、各表读入/成功/跳过/失败数量、缺失文件数及单条异常。迁移必须可重复执行并可对账。

## 5. 文件迁移

SQL 导出只有路径和文件名，没有文件内容。样本路径包含 `C:/yj`、`D:/PicturePDF/...`，部分字段保存目录而不是完整文件路径；`jy_stlfile` 只有文件名和包号。

实施前必须取得旧服务器文件目录或备份，并确认路径拼接规则。推荐：

1. 根据来源表和字段生成候选完整路径；
2. 检查文件存在、大小和扩展名；
3. 复制/上传到新版文件存储；
4. 写入 `file_detail`；
5. 回填 `legacy_order_file.storage_file_id/download_url`；
6. 计算 SHA-256，抽样执行真实下载；
7. 找不到的文件保留元数据并标记 `MISSING`，页面显示“历史文件缺失”，不生成失效链接。

若暂时拿不到实体文件，可先上线订单与详情查询，但附件功能只能展示名称和“待迁移/缺失”状态。

## 6. 后端设计

建议放在 `yigongbao-module-order` 下的独立 `legacy` 包，避免新建 Maven 模块，同时与正式订单服务完全隔离：

```text
legacy/controller
legacy/service
legacy/mapper
legacy/entity
legacy/dto
legacy/vo
```

只提供查询接口：

- `POST /legacy/order/page`：分页查询；
- `GET /legacy/order/{id}`：基本信息、项目、包、生产记录和产品详情；
- `GET /legacy/order/{id}/files`：统一附件列表；
- `GET /legacy/order/file/{id}/download`：鉴权后下载可用文件；
- 可选 `POST /legacy/order/export`：导出当前筛选结果。

不提供创建、编辑、删除、审核、分配设计师、生产、质检、仓储等写接口。历史服务不得注入或调用新版状态机服务。

权限建议新增：

- `legacy:order:query`
- `legacy:order:detail`
- `legacy:order:file`
- `legacy:order:export`（可选）

当前未定义历史数据的行级归属规则。第一版建议仅授予指定管理角色查看全量历史数据；若必须按旧业务员/区域隔离，再单独建立“旧人员/区域到新版用户或机构”的人工映射表，不能按姓名自动授权。

## 7. 前端页面

新增独立菜单“历史订单”，不复用新版订单的流程操作区。

### 7.1 列表字段

- 旧订单号；
- 创建时间；
- 旧状态；
- 业务类型；
- 打印要求；
- 医院、科室；
- 医生、患者；
- 业务员；
- 项目摘要；
- 设计师；
- 交付日期；
- 文件可用数/缺失数；
- 操作：查看详情。

建议筛选：订单号/患者/医院/医生/业务员综合关键字，旧状态，业务类型，打印要求，创建时间范围，医院，设计师。

### 7.2 详情结构

1. 基本信息：订单、业务、医院、医患、业务员、设计师和时间。
2. 项目信息：按 `legacy_order_project` 展示解析后的多项目。
3. 设计/数据包：历史指令、产品字符串、模型和图片。
4. 生产记录：批号、设备、作业员、打印及后处理参数、产品状态和出库原文。
5. 历史文件：按类别分组，显示可用/缺失状态，只对 `AVAILABLE` 提供下载。

页面不出现任何流程按钮；状态用中性色标签并明确标注“旧系统状态”。

## 8. 迁移程序

推荐先把相关 SQL 导入隔离的旧库暂存 schema，再由一次性 Java/批处理迁移程序执行 `stage -> legacy_*`，不要在应用启动时自动迁移。

迁移顺序：

1. 导入 `jy_order/jy_module` 和必要字典；
2. 迁移 `legacy_order`；
3. 拆分并解析 `legacy_order_project`；
4. 迁移 `jy_instruct -> legacy_order_package`；
5. 迁移 `jy_flow -> legacy_order_production`；
6. 迁移 `jy_stlfile -> legacy_package_file`；
7. 汇总四类来源生成 `legacy_order_file`；
8. 迁移实体文件并回填文件状态；
9. 对账后开放菜单权限。

大 SQL 中存在字段内换行，若不导入 MySQL而直接解析文件，必须按完整 INSERT 语句解析，不能按物理行处理。

## 9. 验收标准

- `legacy_order` = 37,632，源订单号无丢失；
- 12 种状态和 4 种业务类型数量与源统计一致；
- 项目拆分数量对账，17 条长度异常全部有告警且仍可查看；
- 每张子表按来源表进行成功、悬空、跳过数量对账；
- 随机抽查早期、中期、末期订单，以及单项目、多项目、取消、审核未通过、设计完成、生产完成订单；
- 附件按来源类型抽查，数据库记录、文件校验和、实际下载三者一致；
- 缺失文件只显示状态，不产生 404 链接；
- 历史查询不改变新版订单、统计、消息、状态机和权限范围；
- 所有接口只有读权限，敏感字段按角色控制并记录下载日志。

## 10. 仍需业务确认

1. 历史订单是指定角色看全量，还是需要按旧业务员/地区做行级权限。
2. 患者电话是否需要迁移和展示；默认建议不迁。
3. “原始预计收费”的不透明文本是否需要寻找旧代码解码；默认不在列表展示。
4. 历史详情是否必须展示全部生产参数，还是只展示订单、项目、产品和附件。
5. 旧实体文件存储是否仍可访问；这是附件下载能否完整实现的唯一硬前提。
