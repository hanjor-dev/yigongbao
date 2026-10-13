# 旧库附件、设计与生产链路数据抽样及流式统计

## 1. 范围与结论

本文只分析以下旧库导出文件，不涉及订单主表和新库：

| 文件 | 大小（MiB） | 完整 INSERT 数 |
|---|---:|---:|
| `sql/old/jy_files.sql` | 8.73 | 35,638 |
| `sql/old/jy_order_filemodel.sql` | 3.08 | 36,476 |
| `sql/old/jy_model.sql` | 83.93 | 243,826 |
| `sql/old/jy_picture.sql` | 8.26 | 30,226 |
| `sql/old/jy_instruct.sql` | 13.77 | 24,555 |
| `sql/old/jy_flow.sql` | 19.72 | 19,446 |
| `sql/old/jy_stlfile.sql` | 44.82 | 71,656 |

结论如下：

1. **附件、设计、生产数据可以按旧订单号展示，不需要强行映射到新版业务实体。** `jy_files.order_num`、`jy_model.od_num`、`jy_instruct.od_num`、`jy_picture.pic_stb2`、`jy_stlfile.od_num` 都保存了 `JY + 12 位数字`的订单号；除 `jy_files` 最早的 45 行为空外，其余所检查表均完整。
2. **生产链路内部关联总体可靠。** `jy_instruct.inst_id` 到 `jy_flow.inst_id` 全量命中；数据包号和订单号也能交叉校验。`jy_picture` 全量可关联生产指令，`jy_stlfile` 仅有 1 行未找到对应指令。
3. **`jy_order_filemodel` 不能作为唯一关联来源。** 它几乎只保留 `fileid` 关系；仅有的 24 个 `model_id` 在当前 `jy_model` 导出中全部悬空。设计模型应优先按 `jy_model.od_num` 关联订单。
4. **最大的实施阻碍不是数据库字段，而是物理文件。** SQL 中大多只存旧服务器的 Windows 目录和文件名；`jy_stlfile` 甚至没有路径字段。若旧服务器目录、文件实体或隐含的目录拼接规则已丢失，页面只能展示附件元数据，无法下载/预览。
5. **字符串原样归档是正确方向。** 产品名称、规格、材质、颜色等大量保存为旧字典 ID 的逗号分隔串；数据包号还夹杂姓名或“打印文件”等自由文本。历史查询页应保留原值，可额外生成便于展示的解析结果，但不能为了进入新版规范表而丢弃原串。

## 2. 统计与抽样方法

- 大文件没有整体加载到内存。统计脚本以流式方式扫描文本，并跟踪 SQL 引号及转义，直到遇到**引号外的分号**才认定一条完整 `INSERT` 结束，再解析 `VALUES`。
- 上表行数、空值数量、格式数量、状态分布及关联命中数均为对当前导出文件的**全量计数**，不是抽样估算。
- 当前七个文件中，每条 INSERT 实际都占一个物理行；最大单条语句长度为 1,254 字符。但统计没有依赖“一行一条”，可处理字段内换行。
- 文中用于说明字段语义的具体值来自 DDL 后首批记录、异常记录和少量离散值样本。这类“字段含义/路径如何拼接”的判断属于**抽样推断**，仍需用旧文件服务器验证。
- 可复查的位置：各表 DDL 与首批数据分别在 `jy_files.sql:L1-L19`、`jy_order_filemodel.sql:L1-L11`、`jy_model.sql:L1-L21`、`jy_picture.sql:L1-L17`、`jy_instruct.sql:L1-L33`、`jy_flow.sql:L1-L47`、`jy_stlfile.sql:L1-L38`。

## 3. 订单号和关联键的全量质量

### 3.1 订单号

| 表/字段 | 总行数 | NULL/空串 | 符合 `JY\d{12}` | 唯一订单数 | 建议 |
|---|---:|---:|---:|---:|---|
| `jy_files.order_num` | 35,638 | 45 / 0 | 35,593 | 35,586 | 优先关联；45 行回退到关联表或文件名提取 |
| `jy_model.od_num` | 243,826 | 0 / 0 | 243,826 | 35,197 | 设计文件的首选订单关联键 |
| `jy_instruct.od_num` | 24,555 | 0 / 0 | 24,555 | 23,206 | 一个订单可有多个生产数据包 |
| `jy_stlfile.od_num` | 71,656 | 0 / 0 | 71,656 | 23,002 | 可直接作为历史订单详情过滤条件 |
| `jy_picture.pic_stb2` | 30,226 | 0 / 0 | 30,226 | 15,122 | 虽名为备用字段，实际是稳定订单号 |

ID 只应作为旧库内部关联键，并同时保存 `legacy_*_id` 用于追溯；不需要也不应尝试把这些 ID 解释为新版主键。

### 3.2 关联命中

以下为全量统计：

- `jy_order_filemodel.fileid`：36,452 个非空引用中，36,379 个能命中 `jy_files.fileid`，73 个悬空。
- `jy_order_filemodel.model_id`：仅 24 个非空引用，全部无法命中当前 `jy_model.model_id`。关联表里的模型 ID 为 69～92，而当前模型导出最小 ID 为 141，明显是历史删除或导出截断后的遗留关系。
- `jy_instruct.inst_packnum`：24,555 行全部唯一，适合做生产数据包业务键；但只有 20,992 个符合简单的 `JY订单号-序号` 格式，其余可能带“打印文件”、患者姓名或 UUID，必须按原串精确关联，不可自行裁剪。
- `jy_flow`：19,446/19,446 的 `inst_id` 命中 `jy_instruct.inst_id`；19,446/19,446 的 `inst_packnum` 精确命中；19,446/19,446 的 `flow_stb1` 精确命中某个 `jy_instruct.od_num`。
- `jy_picture`：30,226/30,226 的 `inst_packnum` 精确命中指令数据包；30,226/30,226 的 `pic_stb2` 命中指令订单号。
- `jy_stlfile`：71,655/71,656 同时可按数据包号、订单号关联指令。唯一悬空记录为 `stlf_id=51038`、订单 `JY202511010006`（`jy_stlfile.sql:L47219`）。
- `jy_instruct.stl_arrs`：16,724 个非空串共解析出 41,911 个数字 token，全部能命中 `jy_stlfile.stlf_id`；只有 `inst_id=23676` 的原值为 `/`，无可解析 ID（`jy_instruct.sql:L22480`）。
- `jy_flow.stl_arrs`：19,445 个非空串共解析出 41,887 个数字 token，全部能命中 `jy_stlfile.stlf_id`。

因此生产详情的推荐连接顺序是：

```text
历史订单号
  -> jy_instruct.od_num
     -> jy_flow.inst_id = jy_instruct.inst_id
     -> jy_picture.inst_packnum = jy_instruct.inst_packnum
     -> jy_stlfile.inst_packnum = jy_instruct.inst_packnum
```

订单号与数据包号可以作为冗余校验。不要仅从数据包号截取订单号，因为数据包号存在自由文本后缀。

## 4. 各表实际内容与迁移建议

### 4.1 `jy_files`：业务员上传的 CT、报告及其他附件

实际数据形态：

- `CT_name` 多为订单号加 `-CT` 的压缩包名，扩展名全量分布主要是 `.zip` 35,175、`.rar` 83、`.7z` 16；364 行无有效名称。
- `CT_path` 非空 35,274 行，其中 `D:/yj` 35,264 行、`C:/yj` 10 行；另有 25 个 NULL 和 339 个空串。
- `report_path` 只有 902 行非空，通常形如 `D:/yj/JY202106250001-report`；`report_name` 大量为空或看起来没有扩展名，不能假定均是 ZIP。
- `others_path` 只有 523 行非空，通常形如 `D:/yj/{订单号}-others`；`oth_name` 只有 463 行非空，常见值本身没有扩展名。
- `CT_size` 35,593 行非空，样本为十进制数字字符串，推测是字节数；字段类型却是 `mediumtext`，迁移时应原样保存并仅在能安全转换时生成数值列。
- 首批示例见 `jy_files.sql:L17-L19`，显示“目录字段 + 文件名字段”并非统一拼接方式。

建议把一行拆成最多三类逻辑附件（CT、报告、其他），同时保存原始路径、原始名称和原始大小。真正复制文件时必须先在旧服务器用样本验证：路径是文件目录、无扩展名基路径，还是解压目录。

### 4.2 `jy_order_filemodel`：不完整的旧关联表

该表没有主键和外键。全量 36,476 行中：

- 36,452 行只有 `fileid`；
- 24 行只有 `model_id`；
- `model_id` 的 24 行全部悬空；
- `fileid` 另有 73 个悬空引用，并存在重复关联。

它只适合作为 `jy_files.order_num` 为空时的补救来源，且需要再连接旧订单表确认 `od_id`；不能作为设计文件迁移的主链路。示例见 `jy_order_filemodel.sql:L9-L11`。

### 4.3 `jy_model`：设计模型、重建报告和其他设计包

实际不是“一订单一模型”，而是“一文件一行”：243,826 行对应 35,197 个订单。

- `od_num` 全部是标准订单号，是可靠主关联键。
- `model_name` 非空 242,868 行，全部为 `.stl`；`model_path` 通常只保存 `D:/yj`（242,832 行）或 `C:/yj`（36 行），958 行为空。
- `rebuild_path` 非空 30,042 行，常见为 `D:/yj/{订单号}-rebuild`；对应 `rebuild_name` 扩展名包括 PDF、PPTX、XLSX、ZIP、DOCX、STL 等，不能按单一类型处理。
- `backup_path` 非空 185,295 行，常见为 `D:/yj/{订单号}-othermodel`；`oth_name` 非空 140,386 行，主要是 ZIP/7Z。同一压缩包名会重复出现在多个 STL 行中，迁移时需要按 `(od_num, backup_path, oth_name)` 去重。
- `model_size` 仅 41 行为 NULL，其余是数字；`use_able` 243,826 行全部为 NULL，没有迁移价值但可保留原值。
- 示例见 `jy_model.sql:L19-L21`。

建议把模型行原样归档，并另外按路径和文件名生成去重后的“历史附件”视图。不要把每个旧 STL 强行对应新版产品或规格；页面直接显示 `ori_name`、`model_name` 等字符串即可。

### 4.4 `jy_instruct`：生产指令/数据包主记录

- `inst_id` 是连接流转卡的稳定旧主键；`inst_packnum` 全量唯一，是连接图纸和包内文件的稳定业务键。
- 同一订单可有多个指令：24,555 行对应 23,206 个订单。
- `inst_filenum` 是字符串，常见值为 `1`～`7`，也存在 `0`，不应当用作关联或完整性依据。
- `inst_mail` 的全量分布为“否”16,435、“是”642、NULL 7,478。
- 产品注册证、名称、规格、材质、颜色、数量等字段可能是逗号分隔的旧字典 ID 并与产品位置对齐；建议保留整串，展示层可按逗号拆列，但不要直接映射为新版字典 ID。
- `stl_arrs` 同时使用逗号和 `/`。样本表明 `/` 可能表达产品组边界，逗号表达组内文件，迁移关系表时应保存 `group_index` 和 `item_index`，并始终保留原串。
- `inst_stb1` 实际是患者，`inst_stb3` 实际是医院；`inst_stb2` 全部为 NULL。

### 4.5 `jy_flow`：生产流转卡

- 与指令的三个冗余关联键都全量命中：`inst_id`、`inst_packnum`、`flow_stb1`（订单号）。主连接应使用 `inst_id`，其余两者用于校验和查询。
- `prod_name`、`prod_style`、`prod_material`、`flow_color`、`prod_num` 是位置对齐的逗号分隔串；示例 `103,103`、`110,110`、`102,102`、`106,106`、`1,1` 见 `jy_flow.sql:L45-L47`。这些多数是旧字典/编码，不是可直接展示的中文名称。
- `flow_stb2` 实际状态：生产中 15,050、生产完成 3,967、NULL 429。
- `again_arrs` 只有 5 行非空，格式类似 `1,564`，含义不能仅凭 DDL确定，原样保存即可。
- `flow_starttime`、`flow_overtime` 虽是 varchar，除 NULL/空值外还出现 9 行字面量字符串 `'null'`；其他时间字段还可见 `1899-12-31`、`NaN-aN-aN aN:aN` 等哨兵/脏值。例见 `jy_flow.sql:L52`。归档层应保存原串；如生成标准时间列，转换失败时置 NULL 并记录异常原因。

### 4.6 `jy_stlfile`：名称具有误导性的“数据包文件明细”

该表并非只有 STL。`stlf_name` 的全量扩展名主要为：

- `.stl` 63,338；
- `.pdf` 4,744；
- 无扩展名 1,213；
- `.xlsx` 1,018；
- `.dcm` 971；
- 另有 `.je`、`.pptx`、`.xls`、`.slc`、`.png`、`.htm`、`.rar` 等。

关键事实：

- 表内只有文件名，**没有物理路径字段**。需要从旧应用配置、代码或文件服务器规则推导“数据包目录 + `stlf_name`”，仅凭此 SQL 无法恢复下载地址。
- `prod_id` 71,656 行全部为空，不能依赖；应通过 `inst_packnum`、`od_num` 和 `stlf_id` 关联。
- `stlf_outbound` 为 71,640 个 NULL、16 个空串，实际没有业务信息。
- `stl_status` 全量值为：已填写流转卡 32,348、`0` 13,293、生产结束 8,740、NULL 17,275。状态应按原字符串展示，不要强行套新版状态机。样本见 `jy_stlfile.sql:L8127`、`L17330`。
- `stl_stb1` 看起来是业务标识/条码，`stl_stb2` 是开始时间，`stl_stb3` 是颜色旧编码；这些备用字段在后期数据中有实际含义，应保留。

### 4.7 `jy_picture`：指令图纸/指令单 PDF

- 30,226 行全部有 `pic_name`、`pic_path`、`inst_packnum`、`pic_stb2`。
- 文件名全部为 PDF；路径根分为 `D:/PicturePDF` 15,579 行和 `D:/InstructPDF` 14,647 行。
- `pic_stb1` 是原始文件名，`pic_stb2` 是订单号，`pic_stb3` 全部为 NULL。
- 示例 `D:/PicturePDF/{订单号}-picture/{数据包号}` 见 `jy_picture.sql:L15-L17`。`pic_path` 样本看起来仍像无 `.pdf` 后缀的基路径，必须在旧服务器验证是目录还是实际文件基名。

## 5. 面向只读历史页面的落库建议

无需把这些记录塞入新版设计、生产和文件业务表。可采用下列独立归档结构，字段允许原样字符串：

1. `legacy_order`：由订单主数据任务维护，仅提供历史订单主键和 `order_num`。
2. `legacy_attachment`：统一保存来源表、来源 ID、订单号、数据包号、附件类型、原始名称、原始路径、原始大小、文件扩展名、是否已找到实体文件、新存储文件 ID/URL。
3. `legacy_design_file`：保存 `jy_model` 全字段或关键字段，尤其是 `model_id`、`od_num`、三组名称/路径、`ori_name`、`model_size`。
4. `legacy_production_instruction`：一比一保存 `jy_instruct`，所有逗号串、`stl_arrs` 和备用字段保留原值。
5. `legacy_production_flow`：一比一保存 `jy_flow`，以 `(legacy_inst_id, inst_packnum, order_num)` 连接指令；所有时间先保存原串，可附加 nullable 的规范时间列。
6. `legacy_package_file`：一比一保存 `jy_stlfile`，再以 `legacy_stlf_id` 生成指令/流转卡到包内文件的关系表。
7. `legacy_instruction_picture`：保存 `jy_picture`；实体文件确认后也可汇总进统一附件表。

历史订单详情页可以分为“业务上传附件、设计文件、生产指令、流转卡、数据包文件/图纸”几个标签页。列表和详情优先显示旧字符串；旧字典值只有在有可靠字典映射时才附加中文解释，绝不能覆盖原值。

## 6. 实施前必须补齐的验证

数据库关系足以支持只读查询，但文件能力仍取决于以下外部材料：

1. 获取旧服务器 `C:/yj`、`D:/yj`、`D:/PicturePDF`、`D:/InstructPDF` 以及生产数据包目录的实际文件或备份。
2. 每类随机选取至少若干条记录，验证“路径 + 文件名”的真实拼接规则、大小写、扩展名和解压目录规则。
3. 从旧应用源码或配置中查明 `jy_stlfile.stlf_name` 对应的根目录；这是当前 SQL 无法补出的关键路径。
4. 文件复制后计算哈希并记录成功、缺失、重名、无法解码四类结果，页面对缺失文件显示明确状态，不能生成假下载链接。
5. 保留本次发现的异常：73 个悬空 `fileid`、24 个悬空 `model_id`、1 个无指令的包文件、`stl_arrs='/'`、自由文本数据包号、时间哨兵值及疑似乱码文件名。

因此，从数据库角度不存在阻止“历史订单只读页”上线的关键字段缺失；**唯一可能使“查看关联文件”无法完整实现的关键缺失，是旧物理文件及其路径规则**。即使文件缺失，订单、设计、指令和生产元数据仍可完整归档并查询。
