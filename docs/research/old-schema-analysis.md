# 旧医工宝历史订单数据结构分析

## 1. 范围与结论

本文只分析旧库 DDL `sql/old/old_yigongbao.sql`，不包含旧库实际数据，也不对新库结构作任何判断。DDL 只有建表语句，没有字典数据或样例数据，因此本文能确认的是“字段和约束是否存在”，不能确认订单号实际唯一性、状态真实取值、隐含关联的命中率或磁盘文件是否仍存在。

为实现“历史订单列表、订单详情、关联文件查询”，旧库最小必需数据集是 `jy_order` 加四类文件实体（`jy_files`、`jy_model`、`jy_picture`、`jy_stlfile`）及其关联依据；如详情还要展示生产信息，则增加 `jy_instruct`、`jy_flow`。人员、医生、医院、科室和项目字典多数已经冗余为订单文本，所以对只读历史展示不是硬依赖，但应作为补全、核验和筛选维度迁移。订单主表字段见 [DDL L857-L901](../../sql/old/old_yigongbao.sql#L857-L901)，四类文件表分别见 [DDL L702-L716](../../sql/old/old_yigongbao.sql#L702-L716)、[DDL L801-L817](../../sql/old/old_yigongbao.sql#L801-L817)、[DDL L911-L923](../../sql/old/old_yigongbao.sql#L911-L923)、[DDL L958-L991](../../sql/old/old_yigongbao.sql#L958-L991)。

最关键的可行性风险不是“缺少订单内容”，而是：业务表没有外键约束；`od_num` 没有唯一约束；同一附件可能同时通过中间表和订单号连接；状态值没有可验证字典；文件表大多只保存路径而不保存文件内容。因此，仅凭 DDL 可以设计迁移模型，但不能保证每条关联都能无损恢复，必须先对旧库实例做数据剖析和文件存储盘点。

## 2. 订单列表和详情的主数据

### 2.1 `jy_order`：唯一核心主表

`jy_order.od_id` 是数据库主键，`od_num` 是业务订单号但只声明为 `NOT NULL`，没有唯一约束。迁移时必须保留旧 `od_id` 作为来源主键，同时把 `od_num` 当作可能重复的业务标识，不能仅以 `od_num` 做无条件 upsert。[DDL L857-L862](../../sql/old/old_yigongbao.sql#L857-L862)

建议历史列表至少保留以下字段：

| 展示/筛选维度 | 旧字段 | 说明 |
|---|---|---|
| 来源主键、订单号 | `od_id`, `od_num` | `od_id` 是可靠行标识，`od_num` 是跨业务表最常见的隐含关联键。[DDL L859-L861](../../sql/old/old_yigongbao.sql#L859-L861) |
| 组织与业务员 | `od_dept`, `od_deptcode`, `od_clerkname`, `od_clerkTel`, `od_user_id` | 同时有文本快照和用户 ID；历史展示应优先保留快照，不应只依赖现存用户。[DDL L862-L865](../../sql/old/old_yigongbao.sql#L862-L865) [DDL L889-L890](../../sql/old/old_yigongbao.sql#L889-L890) |
| 地域与医院 | `od_dist`, `od_hospital`, `od_office` | 均为文本，并非字典外键。[DDL L867-L869](../../sql/old/old_yigongbao.sql#L867-L869) |
| 医患 | `od_doctor`, `od_docTel`, `od_docid`, `od_patient`, `od_patientAge`, `od_patientSex`, `od_patientTel` | 包含个人信息；`od_patientSex` 注释仅定义 `0` 男、`1` 女。[DDL L870-L875](../../sql/old/old_yigongbao.sql#L870-L875) [DDL L895-L895](../../sql/old/old_yigongbao.sql#L895-L895) |
| 业务与项目 | `od_orderType`, `od_requirePart`, `od_rebuildSub`, `od_subjectExpl`, `od_pro` | 都是文本快照，DDL 未声明对字典表的引用。[DDL L876-L879](../../sql/old/old_yigongbao.sql#L876-L879) [DDL L896-L896](../../sql/old/old_yigongbao.sql#L896-L896) |
| 费用与要求 | `od_fee`, `od_subjectCost`, `od_others`, `od_print` | 金额/收费字段使用 `varchar`，迁移时不要直接按数值解析而丢弃非标准内容。[DDL L866-L866](../../sql/old/old_yigongbao.sql#L866-L866) [DDL L880-L882](../../sql/old/old_yigongbao.sql#L880-L882) |
| 时间 | `od_delivery`, `od_preDelivery`, `od_createtime`, `od_subTime`, `ds_time`, `dc_time`, `ps_time`, `pc_time` | 除四个缩写时间外均有业务注释；缩写字段语义需从旧代码或实际数据补证。[DDL L883-L888](../../sql/old/old_yigongbao.sql#L883-L888) [DDL L891-L894](../../sql/old/old_yigongbao.sql#L891-L894) |
| 状态/标志 | `od_assess`, `od_status`, `is_design`, `is_done`, `is_click` | 只有 `od_assess`、`od_status` 有泛化注释，其余没有取值定义。[DDL L885-L886](../../sql/old/old_yigongbao.sql#L885-L886) [DDL L897-L899](../../sql/old/old_yigongbao.sql#L897-L899) |
| 设计师 | `od_designername` | 只有姓名快照，没有设计师用户 ID。[DDL L890-L890](../../sql/old/old_yigongbao.sql#L890-L890) |

### 2.2 列表必须采用来源隔离

由于需求是只读历史页，推荐在目标侧为每行保存 `source_system = OLD_YIGONGBAO`、`source_order_id = od_id`、`source_order_no = od_num`，并以 `(source_system, source_order_id)` 做幂等键。原因是旧 DDL 只保证 `od_id` 主键，不保证 `od_num` 唯一。[DDL L859-L861](../../sql/old/old_yigongbao.sql#L859-L861)

`od_status`、`od_orderType`、`od_fee` 等字段应先原样迁移，再由展示层做兼容映射。DDL 没有证明这些字段对应哪一种字典，强行转换可能造成信息损失。[DDL L866-L886](../../sql/old/old_yigongbao.sql#L866-L886)

## 3. 关联文件：四套来源、两种关联模式

### 3.1 `jy_files`：CT、放射报告和其他文件

该表保存 CT 名称/路径、放射报告名称/路径、其他路径与其他名称，并有 `order_num`。因此订单到文件可直接按 `jy_order.od_num = jy_files.order_num` 推断关联；另外 `jy_order_filemodel` 也可通过 `fileid` 关联。DDL 没有定义任一外键，二者发生冲突时不能靠约束判定哪一个正确。[DDL L702-L716](../../sql/old/old_yigongbao.sql#L702-L716) [DDL L903-L909](../../sql/old/old_yigongbao.sql#L903-L909)

需迁移字段为 `fileid`、`CT_name`、`CT_path`、`report_name`、`report_path`、`others_path`、`oth_name`、`fcreate_time`、`CT_size`、`order_num`。其中 `CT_size` 类型是 `MEDIUMTEXT` 而不是数值；名称/主要路径只有 30 个字符，必须检查历史值是否截断、是否为相对路径。[DDL L704-L714](../../sql/old/old_yigongbao.sql#L704-L714)

### 3.2 `jy_model`：设计模型、重建报告和备份

模型表包含 `model_name/model_path`、`rebuild_name/rebuild_path`、`backup_path`、原始名 `ori_name`、其他名 `oth_name`、大小 `model_size`、创建时间和可用标志 `use_able`。它既可按 `od_num` 直接关联，也可通过 `jy_order_filemodel.model_id` 关联。[DDL L801-L817](../../sql/old/old_yigongbao.sql#L801-L817) [DDL L903-L909](../../sql/old/old_yigongbao.sql#L903-L909)

`model_path` 只有 30 个字符，而其他模型路径是 100 个字符；`use_able` 没有枚举说明。迁移必须原样保留路径和标志，并通过真实文件盘验证可下载性。[DDL L805-L815](../../sql/old/old_yigongbao.sql#L805-L815)

### 3.3 `jy_picture`：生产数据包图片

图片保存 `pic_name`、`pic_path`、原始名 `pic_stb1`，可由 `pic_stb2`（注释明确为订单编号）直接关联订单，也可先按 `inst_packnum` 关联生产指令单。`pic_stb3` 语义未知。[DDL L911-L923](../../sql/old/old_yigongbao.sql#L911-L923)

### 3.4 `jy_stlfile`：STL/生产文件及产品快照

STL 表通过 `od_num` 连接订单，并同时保存 `inst_packnum`。除文件名 `stlf_name` 外，它保存产品、医院、患者、医生、科室、业务员、出库与状态等快照；这些字段可用于生产详情，也可用于订单关联异常时的人工核验。[DDL L958-L988](../../sql/old/old_yigongbao.sql#L958-L988)

DDL 没有 `stlf_path` 或其他明确路径字段，只有 `stlf_name`。如果 STL 文件实际存于固定目录、对象存储或由名称拼接路径，仅凭数据库 DDL 无法恢复下载地址，这是文件迁移中的明确缺口。[DDL L962-L965](../../sql/old/old_yigongbao.sql#L962-L965)

### 3.5 中间表与文件责任人表

`jy_order_filemodel(od_id, fileid, model_id)` 是订单、普通文件、模型的三元关联，但整表没有主键、唯一约束和外键，`fileid`、`model_id` 又允许为空，可能存在重复、悬空或同一行两种附件同时有值的情况。[DDL L903-L909](../../sql/old/old_yigongbao.sql#L903-L909)

`jy_file_user` 和 `jy_model_user` 分别记录业务员与文件、设计师与模型的关系，但都把 `jy_user_id` 单列设为主键。这意味着 DDL 最多允许每个用户关联一个文件/模型，却不限制多个用户指向同一附件；它们也没有外键。除非历史页面需要展示附件经办人，否则不是最小迁移依赖。[DDL L694-L700](../../sql/old/old_yigongbao.sql#L694-L700) [DDL L819-L825](../../sql/old/old_yigongbao.sql#L819-L825)

### 3.6 建议的关联优先级

1. 以 `jy_order_filemodel.od_id -> jy_order.od_id` 为普通文件和模型的首选关联，但在导入前去重并验证引用存在。
2. 以各附件表中的 `order_num/od_num/pic_stb2 -> jy_order.od_num` 作为补充关联和交叉核验，不应覆盖主键关联冲突。
3. 以 `inst_packnum` 串联指令单、图片和 STL，作为生产数据包维度的补充关系。[DDL L775-L776](../../sql/old/old_yigongbao.sql#L775-L776) [DDL L917-L919](../../sql/old/old_yigongbao.sql#L917-L919) [DDL L982-L984](../../sql/old/old_yigongbao.sql#L982-L984)
4. 所有冲突、悬空和一对多异常都进入迁移异常表，不静默舍弃。

## 4. 可选的生产详情链路

`jy_instruct` 以 `inst_id` 为主键，含订单号 `od_num` 和数据包号 `inst_packnum`，可展示预交货、联系人、客户、邮寄信息、备注以及产品名称、规格、材质、颜色、数量等内容。[DDL L771-L797](../../sql/old/old_yigongbao.sql#L771-L797)

`jy_flow.inst_id` 从命名和注释上指向生产指令单，同时 `flow_stb1` 注释为订单编号；它保存生产批号、产品编号、设备、作业员、打印/清洗/固化等生产参数。DDL 没有外键，因此这是隐含关联，需以数据命中率确认。[DDL L718-L757](../../sql/old/old_yigongbao.sql#L718-L757)

若历史详情只要求“下单时内容与附件”，这两张表可以不导入；若用户把生产记录、邮寄信息、打印参数理解为订单详情，则它们属于必要数据，且 `jy_picture`、`jy_stlfile` 应按数据包一并迁移。

## 5. 人员、组织、医院和项目字典

### 5.1 旧业务用户体系

`jy_users` 是医疗业务用户表，包含员工编号、公司、部门、职位、区域、姓名、电话、角色和创建时间。`jy_order.od_user_id` 可按命名推断关联 `jy_users.jy_user_id`，但两者类型分别为 `BIGINT` 和 `INT`，且没有外键。[DDL L889-L889](../../sql/old/old_yigongbao.sql#L889-L889) [DDL L1002-L1019](../../sql/old/old_yigongbao.sql#L1002-L1019)

`jy_user_order` 还以 `jy_user_id + od_num` 表示用户订单关联，但主键是独立的自增 `od_user_id`，未对这对业务字段做唯一约束。它可能表达额外可见人或历史分配关系，语义不能仅从 DDL 确定。[DDL L993-L1000](../../sql/old/old_yigongbao.sql#L993-L1000)

订单已经冗余 `od_clerkname`、`od_clerkTel`、`od_dept` 等快照，所以历史只读展示应保留这些订单字段；不要因当前用户不存在而丢弃订单。若需要按旧人员归属授权或筛选，则迁移 `jy_users` 和 `jy_user_order`，并保留旧 ID 到新主体的映射。

### 5.2 医生、医院与科室

`jy_doctor` 提供医生编码、姓名、职称、医院、地区、科室、角色等，但订单的 `od_docid` 没有外键，医生表本身也以文本保存医院/地区/科室。[DDL L678-L692](../../sql/old/old_yigongbao.sql#L678-L692) [DDL L895-L895](../../sql/old/old_yigongbao.sql#L895-L895)

`jy_hospital` 是带 `parent_id` 的简单树，`jy_office` 是科室名称表；订单只保存医院、地区、科室文本，没有对应 ID。[DDL L762-L769](../../sql/old/old_yigongbao.sql#L762-L769) [DDL L849-L855](../../sql/old/old_yigongbao.sql#L849-L855) [DDL L867-L870](../../sql/old/old_yigongbao.sql#L867-L870)

因此，对历史展示最可靠的是订单文本快照；医生/医院/科室表只能用于补全或筛选，不能假定名称可无歧义映射到目标主数据。

### 5.3 项目和产品辅助表

`jy_pro` 保存“所需部位—重建项目—项目说明”组合；`jy_module` 是项目层级；`jy_productstyle` 是型号层级；`jy_register` 是材质/颜色/注册证号层级。它们均未被订单或生产表以外键引用，相关业务表主要存文本值。[DDL L925-L932](../../sql/old/old_yigongbao.sql#L925-L932) [DDL L827-L836](../../sql/old/old_yigongbao.sql#L827-L836) [DDL L934-L956](../../sql/old/old_yigongbao.sql#L934-L956)

这些表不是历史订单可读性的必要条件。若历史页需要按旧分类树筛选，应迁移为“旧值字典”而不是直接合并到新主数据。

### 5.4 `sys_user/sys_dept` 与业务用户的关系不明

DDL 还包含系统用户 `sys_user`、部门 `sys_dept` 及用户角色/岗位关联表；系统用户通过 `dept_id` 连接部门的意图明显，但 DDL 同样没有声明外键。[DDL L1209-L1227](../../sql/old/old_yigongbao.sql#L1209-L1227) [DDL L1438-L1463](../../sql/old/old_yigongbao.sql#L1438-L1463) [DDL L1482-L1496](../../sql/old/old_yigongbao.sql#L1482-L1496)

订单明确出现的是 `jy_users` 风格字段，而不是 `sys_user.user_id`。在没有旧代码或数据比对前，不能把 `jy_order.od_user_id` 映射到 `sys_user.user_id`。系统用户/部门只在需要还原旧后台权限、操作人或流程人员时考虑；纯只读历史订单不应依赖它们。

## 6. 状态字典与流程历史

### 6.1 业务状态没有可证明的字典

通用字典表 `sys_dict_type` 和 `sys_dict_data` 能表达 `dict_type -> dict_value/dict_label`，但 DDL 没有初始化数据，也没有任何外键把 `jy_order.od_status`、`od_orderType`、`od_fee`、`jy_model.use_able`、`jy_stlfile.stl_status/stlf_outbound` 或 `jy_msgprompt.msg_state` 连接到具体 `dict_type`。[DDL L1229-L1264](../../sql/old/old_yigongbao.sql#L1229-L1264) [DDL L838-L845](../../sql/old/old_yigongbao.sql#L838-L845)

当前唯一有明确业务取值注释的是订单患者性别 `od_patientSex`：`0` 男、`1` 女。[DDL L872-L875](../../sql/old/old_yigongbao.sql#L872-L875) 其他状态应当：

- 原样迁移旧值；
- 从旧库实际 `DISTINCT` 值、旧版代码常量和旧字典数据三方核对；
- 显示层允许“未知旧状态（原值）”，不要把未知值转换为空。

### 6.2 Activiti 流程表不是只读订单的必要依赖

旧库包含 Activiti 历史流程实例、任务、活动、变量、意见和附件。`act_hi_procinst.BUSINESS_KEY_` 是潜在业务键，任务/活动/变量均通过 `PROC_INST_ID_` 串联。[DDL L150-L176](../../sql/old/old_yigongbao.sql#L150-L176) [DDL L178-L235](../../sql/old/old_yigongbao.sql#L178-L235)

DDL 并未证明 `BUSINESS_KEY_` 等于 `jy_order.od_num` 或 `od_id`。如果需求明确“不需要操作各种流程”，最小迁移可以不迁 Activiti 表；如果详情要展示历史审批轨迹、审批意见或流程附件，则必须通过旧代码/数据确认业务键规则，并额外处理 `act_hi_comment.FULL_MSG_`、`act_hi_attachment.CONTENT_ID_` 等二进制内容引用。[DDL L61-L90](../../sql/old/old_yigongbao.sql#L61-L90)

## 7. 数据质量与不可恢复风险清单

| 风险 | DDL 证据 | 影响与处理 |
|---|---|---|
| 订单号非唯一 | `jy_order` 仅 `od_id` 为主键，`od_num` 无唯一约束。[DDL L859-L861](../../sql/old/old_yigongbao.sql#L859-L861) | 不能只按订单号幂等；统计重复订单号，并用旧 `od_id` 固定来源身份。 |
| 业务域无外键 | `jy_order_filemodel` 只定义三列，没有约束；其他 `jy_*` 关联字段同样无 `FOREIGN KEY`。[DDL L903-L909](../../sql/old/old_yigongbao.sql#L903-L909) | 必须统计悬空关联和冲突；不可静默 inner join 丢数据。 |
| 中间表无主键/唯一键 | `jy_order_filemodel` 没有主键。[DDL L903-L909](../../sql/old/old_yigongbao.sql#L903-L909) | 可能重复，导入前需按业务规则去重并留异常记录。 |
| 关联键双轨 | 普通文件/模型同时存在 `od_num` 和 `jy_order_filemodel` 连接方式。[DDL L702-L714](../../sql/old/old_yigongbao.sql#L702-L714) [DDL L801-L815](../../sql/old/old_yigongbao.sql#L801-L815) [DDL L903-L909](../../sql/old/old_yigongbao.sql#L903-L909) | 两种关系可能不一致；需建立冲突优先级和人工复核清单。 |
| 关联字段缺索引 | DDL 只给这些业务表定义主键，没有为 `od_num/order_num/pic_stb2/inst_packnum` 定义索引。[DDL L702-L1000](../../sql/old/old_yigongbao.sql#L702-L1000) | 大数据量联查/抽取可能很慢；迁移应分批，必要时在副本上加临时索引。 |
| 状态枚举缺失 | 状态字段是自由文本/数字，通用字典没有可证明的绑定。[DDL L885-L899](../../sql/old/old_yigongbao.sql#L885-L899) [DDL L1229-L1264](../../sql/old/old_yigongbao.sql#L1229-L1264) | 不能仅凭字段名映射新状态；须保留原值并做样本核对。 |
| 文件不在数据库内 | 业务文件表保存名称与路径，没有文件 blob；STL 甚至没有明确路径字段。[DDL L702-L716](../../sql/old/old_yigongbao.sql#L702-L716) [DDL L958-L988](../../sql/old/old_yigongbao.sql#L958-L988) | 只有数据库备份无法完成附件迁移；必须取得旧文件服务器/共享盘/对象存储及路径拼接规则。 |
| 路径字段过短/不一致 | `CT_path/report_path/model_path` 为 30 字符，其他路径为 100 或 255 字符。[DDL L706-L710](../../sql/old/old_yigongbao.sql#L706-L710) [DDL L805-L809](../../sql/old/old_yigongbao.sql#L805-L809) [DDL L915-L920](../../sql/old/old_yigongbao.sql#L915-L920) | 检查截断、相对/绝对路径、编码、大小写及重复文件名。 |
| 软删口径缺失 | `jy_order` 及四类文件表没有删除标志；`sys_user/sys_dept` 才有 `del_flag`。[DDL L857-L901](../../sql/old/old_yigongbao.sql#L857-L901) [DDL L1209-L1225](../../sql/old/old_yigongbao.sql#L1209-L1225) [DDL L1438-L1461](../../sql/old/old_yigongbao.sql#L1438-L1461) | 无法从订单表区分逻辑删除；默认全量保留，除非旧代码或业务给出可信规则。 |
| 个人敏感信息 | 订单保存医生/患者姓名、电话、年龄、性别，生产文件也复制医患信息。[DDL L870-L875](../../sql/old/old_yigongbao.sql#L870-L875) [DDL L971-L978](../../sql/old/old_yigongbao.sql#L971-L978) | 历史页必须沿用数据权限、访问审计和脱敏策略。 |
| 用户身份歧义 | 业务用户 `jy_users` 与系统用户 `sys_user` 并存，DDL 没有二者映射。[DDL L1002-L1019](../../sql/old/old_yigongbao.sql#L1002-L1019) [DDL L1438-L1463](../../sql/old/old_yigongbao.sql#L1438-L1463) | 不能按相同数值 ID 自动合并；需通过员工编号/账号/姓名电话组合人工核验。 |
| 语义不明字段 | `ds_time/dc_time/ps_time/pc_time`、`is_click`、多种 `stb` 备用字段无完整语义。[DDL L891-L899](../../sql/old/old_yigongbao.sql#L891-L899) | 先原样落入来源扩展字段，待旧代码和业务确认后再结构化。 |

## 8. 迁移前必须补齐的证据

在决定最终映射前，应对旧库实际实例完成以下只读核验；这些结果无法由 DDL 推导：

1. 统计 `jy_order.od_num` 重复、空白和前后空格；统计各附件订单号字段对订单的命中率。
2. 对 `jy_order_filemodel` 检查完全重复行、空关联、悬空 `od_id/fileid/model_id`，以及与附件表订单号冲突的行。
3. 分别提取 `od_status`、`od_orderType`、`od_fee`、`is_design`、`is_done`、`is_click`、`use_able`、`stl_status`、`stlf_outbound`、`msg_state` 的 `DISTINCT` 值和数量；再核对旧系统代码与 `sys_dict_data` 实际内容。
4. 核验 `jy_order.od_user_id` 到 `jy_users.jy_user_id` 的命中率，以及 `od_docid` 到 `jy_doctor.do_id` 的命中率；不要先假设它们指向 `sys_user`。
5. 盘点所有路径字段的前缀、最大长度、空值率、非法字符、编码和文件存在率；单独查明 STL 文件路径生成规则。
6. 确认旧文件存储介质、访问凭据、目录根路径和是否仍有备份。没有这些，数据库迁移只能显示附件元数据，无法提供下载/预览。
7. 若要显示流程轨迹，抽样验证 `act_hi_procinst.BUSINESS_KEY_` 与订单 `od_id/od_num` 的对应规则，再决定是否迁移流程评论、变量和附件。
8. 让业务确认历史详情边界：是否包括生产指令、流转卡、图片、STL、邮寄信息和流程轨迹。该选择直接决定 `jy_instruct/jy_flow` 与 Activiti 表是否属于必迁范围。

## 9. 旧库侧迁移数据集建议

### 必迁

- `jy_order`：历史列表和详情主体。
- `jy_files`、`jy_model`、`jy_picture`、`jy_stlfile`：四类附件/文件元数据。
- `jy_order_filemodel`：普通文件和模型的主键关联依据；即使最终以订单号补链，也要迁入临时区用于核验。
- 旧文件存储中的实际文件：数据库 DDL 不包含这些内容。

### 条件必迁

- `jy_instruct`、`jy_flow`：详情包含生产/邮寄/打印信息时。
- `jy_users`、`jy_user_order`、`jy_file_user`、`jy_model_user`：需要旧人员归属、经办人或权限筛选时。
- `jy_doctor`、`jy_hospital`、`jy_office`、`jy_pro`、`jy_module`、`jy_productstyle`、`jy_register`：需要按旧主数据补全或筛选时。
- `act_hi_*` 相关表：需要历史流程轨迹、意见或流程附件时。

### 不应作为硬依赖

- `sys_user/sys_dept`：与订单业务用户体系的关系没有 DDL 证据。
- `sys_dict_data/sys_dict_type`：可作为状态映射候选证据，但 DDL 没有业务字段到字典类型的绑定。
- `jy_msgprompt`：消息状态表仅有订单号、消息状态和备用字段，不是订单详情的事实来源。[DDL L838-L845](../../sql/old/old_yigongbao.sql#L838-L845)
- `jy_data`：只有 `data_type/data_name/data_route` 三个通用字段，DDL 没有显示其与订单或附件的关系。[DDL L669-L676](../../sql/old/old_yigongbao.sql#L669-L676)

结论上，旧库具备展示历史订单主体与多数附件元数据所需的字段；真正可能阻断“附件可查看”的关键缺失是文件实体和 STL 路径规则，而不是订单字段。状态标准化、人员映射与完整关联不能从 DDL 单独完成，但可通过“保留原值/旧 ID + 数据剖析 + 异常隔离”的方式实现可审计迁移。
