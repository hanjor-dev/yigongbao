# 旧版医工宝历史数据迁移可行性分析

> **后续更新：** 旧库实际数据导出已完成抽样和流式统计。本文件保留 DDL 阶段的可行性分析；最终落地方案以 [`2026-09-28-old-yigongbao-history-page-implementation-plan.md`](2026-09-28-old-yigongbao-history-page-implementation-plan.md) 为准。

> 分析日期：2026-09-28  
> 旧库来源：`sql/old/old_yigongbao.sql`（仅 DDL，无数据、无 INSERT）  
> 新库来源：本地 MySQL MCP，数据库 `yigongbao` 的实时表结构  
> 目标：提供独立的只读“历史订单”列表、详情及附件查询，不让历史数据进入新版流程

旧库逐表、逐行 DDL 证据见 [`docs/research/old-schema-analysis.md`](../../research/old-schema-analysis.md)。本报告在其基础上与新库实时结构交叉比对。

本次读取新库时的基线规模为：`order_main=946`、`order_item=1028`、`order_file=957`、`file_detail=19701`、有效机构 `565`、有效用户 `111`、医生 `224`、医院科室 `40`、产品 `9`、规格 `48`、注册证 `3`。这些数量只用于说明目标库已经存在正式业务数据和主键/唯一值冲突风险，不是迁移后的预期数量。

## 一、结论

可以实现历史订单查询，但**不建议把旧数据直接灌入新版现有的 `order_main`、设计、生产等业务表**。

原因不是简单的字段重命名，而是两代系统的数据模型不同：旧版订单以字符串快照和扁平字段为主，新版依赖规范化主数据 ID、严格状态机、设计数据包、产品规格、文件中心和多张流程表。旧库 DDL 中缺少若干新版必填信息，也没有业务表外键；直接迁移会迫使脚本伪造订单类型、机构、产品、规格、状态和文件 ID，并可能使历史订单误入统计、权限和流程逻辑。

推荐采用：

1. 在新版数据库新增一组 `legacy_*` 归档表，保存旧版原始主键、原始值和可查询的结构化字段。
2. 历史页只读访问 `legacy_*` 表，不调用新版订单状态流转、修改、审核、生产、质检、仓储接口。
3. 实体文件迁入新版文件存储并登记到 `file_detail`，但由 `legacy_order_file` 关联；不要写入 `order_file`，避免被新版业务详情误认为正式订单附件。
4. 对医院、业务员、医生、产品等只做“可选匹配”，同时保留旧值快照。匹配失败不应阻断历史数据导入。
5. 在正式迁移前，必须取得旧库数据快照和旧文件根目录，先运行数据画像与文件存在性检查。当前仅凭 DDL 不能判断实际行数、状态取值、脏数据和文件是否仍存在。

## 二、最小可用迁移范围

### 2.1 P0：历史订单列表与基础详情

必须迁移：

- `jy_order`：历史订单主数据。
- `jy_user_order`、`jy_users`：补充订单与业务员关系；当 `jy_order.od_user_id` 可用时以其为主，关联表用于校验和补缺。
- `jy_order.od_requirePart / od_rebuildSub / od_subjectExpl / od_print / od_others`：旧版把订单项目直接放在主表，应在归档模型中拆成一条历史订单项目记录，或继续作为历史详情快照展示。

列表建议至少支持：旧订单号、创建时间、状态原文、医院、科室、医生、患者、业务员、设计师、业务类型、交付时间。

### 2.2 P0：附件查询与下载

必须迁移：

- `jy_files`：CT、影像报告、其他附件。
- `jy_order_filemodel`：订单与 `jy_files`、`jy_model` 的桥接关系。
- `jy_model`：设计模型、重建报告、备份文件等路径。
- `jy_picture`：按数据包保存的图片/图纸。
- `jy_stlfile`：生产 STL 文件及产品快照。

数据库记录本身不等于文件可用。迁移必须同时完成：旧路径解析、实际文件存在性检查、复制/上传、大小与校验和记录、下载验证。若旧文件目录或对象存储已丢失，DDL 和数据库路径都无法恢复文件内容。

### 2.3 P1：设计与生产详情

若历史详情需要展示设计包、生产指令、流转卡、产品与出入库信息，再迁移：

- `jy_instruct`：生产指令/数据包/产品快照。
- `jy_flow`：旧版流转卡及打印、后处理参数。
- `jy_stlfile`：产品、状态、出库信息。
- `jy_picture`：数据包图片。
- `jy_module`、`jy_productstyle`、`jy_register`：仅用于解释旧名称或层级，不建议反向创建新版主数据。

### 2.4 默认不迁移

- `act_*`：Activiti 流程运行/历史表。新页面只读，不恢复旧流程，且旧业务表 DDL 中没有明确的流程实例外键。
- `qrtz_*`：定时任务运行数据。
- 旧 `sys_*` 权限、菜单、角色、登录日志：不用于新版登录授权。
- `jy_msgprompt`：旧消息提示；除非用户明确要求查看历史通知。
- `student`、代码生成表：与历史订单无关。
- 旧密码：旧 `jy_users` 和 `jy_doctor` 的密码/盐不应迁入新版账号体系。

## 三、核心表映射

映射等级：A=可直接或轻度转换；B=需字典/名称匹配或生成值；C=语义不完整，只能保留旧值；X=新版无可靠目标或不应迁移到业务表。

| 旧表 | 新版现有表候选 | 等级 | 结论 |
|---|---|---:|---|
| `jy_order` | `order_main` + `order_item` | B/C | 基本展示字段可映射，但新版必填的 `public_order_code`、`order_type`、`business_type`、`org_id` 无稳定来源；旧状态是自由文本。建议写 `legacy_order`。 |
| `jy_files` | `file_detail` + `order_file` | B | 三组名称/路径可拆成多条文件；必须先找到实体文件并生成新版文件 ID/URL。建议 `file_detail` + `legacy_order_file`。 |
| `jy_order_filemodel` | 无完全等价表 | A | 作为旧订单与旧文件/模型的桥接，迁入 `legacy_order_file` 的来源关联字段。 |
| `jy_model` | `design_model` + `file_detail` | B/C | 主模型文件可映射；重建报告、备份、其他文件在新 `design_model` 无字段，应拆为历史附件并保留来源类型。 |
| `jy_instruct` | `design_package`、`design_product`、`design_instruction`、`production_record` | C | 一行混合包、产品、指令信息；缺新版包文件 ID、产品/规格 ID、产品分类和明确版本信息。建议 `legacy_design_package/product`。 |
| `jy_picture` | `design_drawing` + `file_detail` | B/C | 可按 `inst_packnum` 关联包；缺版本序号、分类、确认状态等，不能无损形成新版图纸版本。 |
| `jy_flow` | `production_record` + `production_process` | C | 可保留批号、设备、作业员和时间；缺新版流转卡状态、订单类型、设计包 ID、产品 ID 等必填值，工序参数需按旧字段转换。 |
| `jy_stlfile` | `production_product` + `file_detail` | B/C | 产品快照和出库文本可保留；缺可靠 `production_record_id`、`print_file_id` 和新版产品状态枚举映射。 |
| `jy_users` | `sys_user` | C/X | 姓名、电话、工号可匹配；新版必填机构、账户类型、BCrypt 密码无法从 DDL可靠转换。只保留历史人员快照或映射到现有用户。 |
| `jy_hospital` | `sys_area` / `sys_org` | C | 旧表混合“医院或地区”，仅靠层级和名称不能可靠判断节点类型。订单中已有医院/地区文本，可直接展示。 |
| `jy_doctor` | `doctor` | B/C | 姓名/医院/科室可匹配；旧 `do_code` 新版无对应字段，医院和科室仍需名称匹配。 |
| `jy_office` | `hospital_dept` | B | 可按名称匹配，但旧表没有医院归属，新版科室也未直接绑定医院；同名科室需保留旧文本。 |
| `jy_pro` | `rebuild_body_part` + `rebuild_project` | B/C | 旧表一行同时保存部位、项目、说明；只能按名称尝试匹配。 |
| `jy_module` | `rebuild_project` | C | 都有层级，但旧表缺部位 ID、分类编码、编码、级别等新版必填语义。 |
| `jy_productstyle` | `product_spec` | C | 旧层级节点缺产品 ID、注册证 ID；不能仅按节点名直接导入新版规格。 |
| `jy_register` | `registration_cert`、材质/颜色字典 | C | 同一列同时表示“材质/颜色/注册证号”，必须依据层级和真实数据辨别，DDL 不足以制定规则。 |

## 四、订单字段映射

| 旧字段 | 新版候选字段 | 等级 | 转换/风险 |
|---|---|---:|---|
| `od_id` | `legacy_order.source_id`；若直迁则新生成 `order_main.id` | A | 不应复用旧 ID，避免与现有 946 条新版订单冲突。 |
| `od_num` | `order_main.order_code` | A | 需检查空值、重复值及与新版订单号冲突；归档表建议唯一键为 `(source_system, source_id)`，订单号只建普通索引。 |
| 无 | `order_main.public_order_code` | X/B | 旧库无来源；直迁只能按新版规则生成。归档方案无需伪造，可单独生成历史展示号。 |
| 无明确字段 | `order_main.order_type` | X | 新版要求 1=医疗器械、2=非医疗器械；旧 `od_orderType` 注释为“业务类型”，不能等同。 |
| `od_orderType` | `order_main.business_type` | B/C | 必须先统计真实取值，再建立到 11.1/11.2/11.3/11.4 的字典表；未知值保留原文。 |
| `od_dept` / `od_deptcode` | `org_name` / `org_id` 或 `operator_dept_*` | C | “所属部门”究竟是提单机构还是人员部门不明确；需要旧系统代码或数据样本确认。 |
| `od_clerkname` | `operator_name` | A | 姓名快照可直接展示。 |
| `od_clerkTel` | `operator_phone` | A | 建议清洗空格、区号，但保留原值。 |
| `od_user_id` | `operator_id` | B | 只能先关联 `jy_users`，再按工号/账号/电话匹配新版 `sys_user`；匹配失败允许为空。 |
| `od_dist` | `area_name/full_area_name` | A/C | 可作为历史地区文本；无可靠 `area_id`。 |
| `od_hospital` | `hospital_name` | A | 可直接保存快照；`hospital_id` 仅在名称唯一且人工确认后回填。 |
| `od_office` | `hospital_dept_name` | A | 文本可直接保存；ID 仅可选匹配。 |
| `od_doctor` / `od_docTel` | `doctor_name` / `doctor_phone` | A | `doctor_id` 仅可选匹配。 |
| `od_docid` | `doctor_id` | B/C | 旧值需确认是 `jy_doctor.do_id` 还是其他来源，再建立映射表。 |
| `od_patient` / `od_patientAge` / `od_patientSex` | `patient_name/age/gender` | A/B | 年龄需将字符串安全转整数；0/1 性别需转新版字典码。未知值保留原文。 |
| `od_patientTel` | 无 | X | 新版订单主表无患者电话，必须放入归档字段；不应丢失。 |
| `od_requirePart` | `order_item.body_part_name` | A/B | 文本可保留；ID 需按名称匹配 `rebuild_body_part`。若旧值含多项/分隔符，需数据画像。 |
| `od_rebuildSub` | `order_item.project_name` | A/B | 文本可保留；ID 和分类编码不能仅靠 DDL确定。 |
| `od_subjectExpl` | `order_item.project_desc` | A | 可直接迁移。 |
| `od_print` | `order_item.forming_requirement` | B | 语义接近但需业务确认；也可原样放历史详情。 |
| `od_others` | `order_item.other_requirement` | A | 可直接迁移。 |
| `od_subjectCost` / `od_fee` | `estimated_cost` 或历史收费字段 | C | 两者都是字符串且含义可能分别为预计收费/收费情况；只对纯数值解析，原文必须保留。 |
| `od_delivery` / `od_preDelivery` | `actual_complete_time` / `expected_delivery_date` | B | `od_delivery` 注释是交付日期，不一定等于实际完成时间；需旧业务规则确认。 |
| `od_assess` | `data_evaluation_opinion` | A | 可直接迁移。 |
| `od_status` | `status` + `phase` | C | 必须依据旧库 `DISTINCT od_status` 和旧代码建立映射；不得按文本猜测。 |
| `od_createtime` / `od_subTime` | `create_time` / 状态历史时间 | A/B | 提交时间在新版主表无直接字段，可保留归档字段或生成一条历史状态记录。 |
| `od_designername` | `designer_name` | A | ID 只能匹配现有用户。 |
| `ds_time/dc_time/ps_time/pc_time` | 设计/生产开始结束时间 | B | 缩写无注释；必须从旧代码或样本确认含义后再映射。 |
| `is_design/is_done/is_click` | 无一一对应字段 | C/X | 只能保留原值；不要与新版流程状态重复推断。 |

## 五、文件字段映射

### 5.1 `jy_files`

一条旧记录最多拆成三类文件：

| 旧字段组合 | 新文件类别 | 处理 |
|---|---|---|
| `CT_name + CT_path + CT_size` | `10.1` 影像数据 | 生成 `file_detail`；`CT_size` 为 `mediumtext`，仅在可解析为字节数时写 `size`。 |
| `report_name + report_path` | `10.2` 影像报告 | 生成 `file_detail`。 |
| `oth_name + others_path` | `10.3` 订单其他附件 | 生成 `file_detail`；需确认是否用分隔符保存多个文件。 |

`order_num` 可作为订单号兜底关联；优先使用 `jy_order_filemodel.od_id -> jy_order.od_id`，避免订单号格式或重复问题。

### 5.2 `jy_model / jy_picture / jy_stlfile`

- `jy_model.model_name/model_path/model_size/ori_name`：主模型文件。
- `jy_model.rebuild_name/rebuild_path`：重建报告，建议单独一条历史附件。
- `jy_model.backup_path/oth_name`：需数据样本确认文件类型。
- `jy_picture.pic_name/pic_path/pic_stb1`：数据包图片/图纸；由 `inst_packnum` 和 `pic_stb2(订单编号)` 关联。
- `jy_stlfile.stlf_name`：只有文件名，无明确路径字段；需确认实际存储目录规则，否则只能展示元数据、无法下载。

新版 `file_detail` 至少要求 `id` 和可访问 `url`。旧库只有相对路径时，不能仅插入数据库记录，必须先把文件复制到新版存储或提供稳定兼容下载服务。

## 六、设计与生产字段映射要点

### 6.1 `jy_instruct`

- `inst_packnum` -> 历史包编号 / `design_package.package_code`。
- `od_num` -> 订单号。
- `inst_filenum` -> `design_package.file_count`，需字符串转整数。
- `inst_time` -> 预交付时间快照。
- `inst_identify` -> `design_package.product_mark`。
- `inst_mail/inst_mailaddress` -> 是否邮寄/地址；`inst_mail` 实际取值需画像。
- `prod_register/prod_name/prod_style/prod_material/prod_color/prod_num` -> 历史产品快照。
- `stl_arrs` -> 旧 STL 集合；格式未知，必须检查是否为 JSON、ID 列表还是文件名列表。
- `inst_stb1/2/3` -> 患者/生产员/医院（依赖注释中的备用字段语义）。

无法可靠补齐新版 `design_package.file_id`、`design_product.product_id/spec_id`，所以不适合直接写新版设计表。

### 6.2 `jy_flow`

- `flow_id` -> 历史流转卡源 ID。
- `inst_id` -> `jy_instruct.inst_id`。
- `inst_packnum` -> 包编号。
- `flow_batchnum` -> `production_batch_no`。
- `flow_prodnum/flow_serialnum` -> 产品编号/序列号快照。
- `prod_*`、`flow_color`、`prod_num` -> 产品快照。
- `flow_starttime/flow_overtime` -> 打印开始/完成时间，但二者是 `varchar(64)`，需严格解析并记录失败原因。
- `flow_materialnum/flow_pcnum/flow_3dnum/flow_worker` -> 原材料批号、电脑设备、打印机、作业员。
- `flow_thick/flow_power` -> 打印参数；可进入历史工序参数 JSON。
- `flow_soak* / flow_weld* / flow_clean* / flow_heat*` -> 后处理参数和时间；旧字段不能完整还原新版固定工序、设备 ID 和扫码交接记录。
- `again_arrs` -> 重打信息，格式未知。
- `flow_stb1` -> 订单编号兜底。

旧流转卡没有整体状态列，不能可靠生成新版 `production_record.status`。

### 6.3 `jy_stlfile`

- `prod_identifier` -> `production_product.product_no`。
- `prod_name/prod_style/prod_material/stl_stb3` -> 产品、规格、材质、颜色快照。
- `stlf_weight` -> `weight`，需字符串转 decimal。
- `stl_status` -> 新版产品状态，需真实取值映射。
- `stlf_outbound` -> 出库状态/说明，需真实取值映射。
- `stl_stb2` -> 开始时间（按旧注释）。
- `inst_packnum/od_num/prod_id` -> 用于关联旧指令、流转卡和订单；其中 `prod_id` 是字符串，需确认其真实来源。

新版产品状态包含 `pending / in_process / fail / pass / pending_warehouse_in / warehoused / warehouse_out / completed / cancelled`。旧 `stl_status` 和 `stlf_outbound` 必须基于实际离散值建立映射，不能从字段名直接推断。

## 七、当前确认的关键缺口

### 7.1 会阻止“直接写新版业务表”的缺口

1. `order_main.public_order_code`：旧库无字段，新版非空且唯一。
2. `order_main.order_type`：旧库没有可靠的医疗器械/非医疗器械标识。
3. `order_main.business_type`：只有旧自由文本 `od_orderType`，需实际取值映射。
4. `order_main.org_id`：旧库主要保存部门/医院/地区文本，缺可靠提单机构 ID。
5. `design_package.file_id/package_seq`：旧指令没有新版数据包文件中心 ID，序号规则也未明确。
6. `design_product.product_id/spec_id`：旧版只有产品/规格字符串。
7. `production_record.record_no/order_type/design_package_id/status`：旧版无全部可靠来源。
8. `production_product.production_record_id/print_file_id/status`：关联和状态都需数据推断，打印文件路径还可能缺失。
9. 新版唯一约束：订单号、公开单号、包编号、流转卡编号、产品编号可能与现有数据冲突。

这些缺口不会阻止“归档表 + 独立只读页面”方案。

### 7.2 当前材料不足以判断的事项

1. 旧库各表实际行数及孤儿记录比例。
2. `od_num`、`inst_packnum`、产品编号是否唯一。
3. `od_status`、`od_orderType`、`inst_mail`、`stl_status`、`stlf_outbound` 等真实离散值。
4. 备用字段和数组字段实际格式：`stl_arrs`、`again_arrs`、`oth_name` 等。
5. 旧文件根目录、路径拼接规则、字符编码以及文件是否仍存在。
6. `ds_time/dc_time/ps_time/pc_time` 的准确业务含义。
7. 旧业务员、医生、医院与新版主数据的真实匹配率。
8. 患者等敏感信息的历史查看权限、脱敏和审计要求。

## 八、推荐归档模型

建议至少新增：

### 8.1 `legacy_order`

保存：迁移批次、`source_id`、旧订单号、所有列表筛选字段、状态原文、业务类型原文、人员/医院/患者快照、交付/流程时间、原始布尔值、`raw_data JSON`、匹配到的新版机构/用户/医生 ID（均可空）、迁移校验状态。

关键约束：

- `UNIQUE(source_system, source_id)`，保证可重复执行。
- `INDEX(order_no, create_time, hospital_name, patient_name, clerk_name, legacy_status)`。
- 不设置触发新版流程所需的状态和处理人语义。

### 8.2 `legacy_order_item`

保存旧订单中的部位、重建项目、项目说明、打印要求、其他要求和费用原文。即使当前一张旧订单通常只有一组字段，也用一对多结构，方便处理分隔值和未来发现的多项目数据。

### 8.3 `legacy_order_file`

保存：来源表/来源主键、历史订单 ID、文件类型、原始文件名、原始路径、原始大小文本、实体文件状态、校验和、新版 `file_detail.id`、失败原因。下载只允许 `file_status=AVAILABLE`。

### 8.4 `legacy_design_package / legacy_design_product`

分别保存 `jy_instruct` 的包信息和产品快照；用旧 `inst_id`、`inst_packnum` 与订单关联。不要强制要求新版产品/规格 ID。

### 8.5 `legacy_production_record / legacy_production_product`

分别保存 `jy_flow` 和 `jy_stlfile`。工序参数可用 JSON 保存，但常用筛选字段（批号、产品号、开始/结束时间、状态原文、出库原文）应独立成列。

### 8.6 `legacy_id_mapping / legacy_migration_run`

- `legacy_id_mapping`：记录旧表主键与归档记录、新版可选主数据的匹配结果、方法和置信度。
- `legacy_migration_run`：记录批次、开始/结束时间、源行数、成功/跳过/失败数量、文件校验结果和脚本版本。

## 九、正式迁移前的数据画像清单

取得旧库只读连接后，至少执行以下检查：

```sql
-- 行数
SELECT 'jy_order' t, COUNT(*) c FROM jy_order
UNION ALL SELECT 'jy_files', COUNT(*) FROM jy_files
UNION ALL SELECT 'jy_model', COUNT(*) FROM jy_model
UNION ALL SELECT 'jy_instruct', COUNT(*) FROM jy_instruct
UNION ALL SELECT 'jy_flow', COUNT(*) FROM jy_flow
UNION ALL SELECT 'jy_stlfile', COUNT(*) FROM jy_stlfile;

-- 订单号质量
SELECT COUNT(*) total,
       SUM(od_num IS NULL OR TRIM(od_num)='') blank_no,
       COUNT(DISTINCT od_num) distinct_no
FROM jy_order;
SELECT od_num, COUNT(*) c FROM jy_order GROUP BY od_num HAVING c > 1;

-- 关键枚举真实取值
SELECT od_status, COUNT(*) c FROM jy_order GROUP BY od_status ORDER BY c DESC;
SELECT od_orderType, COUNT(*) c FROM jy_order GROUP BY od_orderType ORDER BY c DESC;
SELECT inst_mail, COUNT(*) c FROM jy_instruct GROUP BY inst_mail ORDER BY c DESC;
SELECT stl_status, stlf_outbound, COUNT(*) c
FROM jy_stlfile GROUP BY stl_status, stlf_outbound ORDER BY c DESC;

-- 孤儿关系
SELECT COUNT(*) FROM jy_order_filemodel r
LEFT JOIN jy_order o ON o.od_id=r.od_id WHERE o.od_id IS NULL;
SELECT COUNT(*) FROM jy_flow f
LEFT JOIN jy_instruct i ON i.inst_id=f.inst_id
WHERE f.inst_id IS NOT NULL AND i.inst_id IS NULL;

-- 字符串日期/数值异常需按旧库版本补充 STR_TO_DATE/REGEXP 检查
SELECT flow_starttime, COUNT(*) c
FROM jy_flow GROUP BY flow_starttime ORDER BY c DESC LIMIT 100;
SELECT stlf_weight, COUNT(*) c
FROM jy_stlfile GROUP BY stlf_weight ORDER BY c DESC LIMIT 100;
```

此外应导出所有路径列，按旧服务器实际路径规则逐一检查文件存在性，并生成 `source_path, size, sha256, result` 清单。

## 十、实施顺序与验收

1. 冻结旧系统写入并做数据库、文件双备份。
2. 对旧库执行数据画像，确认状态、数组、路径和备用字段规则。
3. 建立归档表和映射表；先迁 1% 样本或 100 个代表订单。
4. 完成主数据可选匹配，任何不唯一匹配进入人工复核，不猜 ID。
5. 先迁订单，再迁订单项目、设计包/产品、生产记录/产品，最后迁文件。
6. 文件使用“复制成功 + 校验和一致 + 可下载”作为成功标准；数据库路径存在但实体文件缺失计为失败。
7. 全量迁移采用幂等 upsert，按迁移批次记录结果；失败可按批次清理归档表，不触碰现有新版订单。
8. 历史接口只开放查询、预览、下载和导出；后端不提供新增、编辑、删除、审核、生产、质检、入出库动作。
9. 权限至少按当前用户机构范围或专门的“历史订单查看”资源控制；患者姓名、电话按现行合规要求脱敏并记录访问日志。
10. 验收对账：源/目标行数、每单关联数、孤儿数、字段转换失败数、文件成功/缺失数、随机抽样详情一致性、下载成功率。

## 十一、最终判断

- **能否实现只读历史订单页面：能。** 旧表包含订单、人员文本、医院/医生/患者、项目、设计、生产和附件路径等主要展示信息。
- **能否无条件完整迁入新版现有业务表：不能。** 多个新版必填字段和规范化关联在旧库中没有可靠来源。
- **是否存在无法恢复的数据：有可能。** 特别是实体文件、STL 路径、状态语义、备用字段格式；必须查看旧库数据和旧服务器文件后才能定论。
- **当前是否具备执行正式迁移的条件：尚不具备。** 还缺旧库数据只读访问/脱敏快照、旧文件存储、旧状态与路径规则。
- **推荐方案：归档专表 + 文件中心复用 + 独立只读接口/页面。** 这与“只查询、不操作流程”的需求最匹配，也能最大限度保真并隔离新版业务风险。
