# 历史订单列表 27 字段逐项映射与处理规则

## 结论

这 27 个字段足以构成历史列表，但不能全部“直接 SELECT 后写入”。建议每个字段同时保存：

- `raw_value`：旧库原始值，永不覆盖；
- `display_value`：历史页展示值；
- `normalized_value`：仅对能可靠转换的字段生成；
- `mapping_status`：`DIRECT`、`NORMALIZED`、`ASSEMBLED`、`RAW_ONLY`、`MISSING`。

旧表 DDL 中 `od_num` 仅保证非空、不保证唯一；因此归档主键必须使用 `od_id`，订单号只作为展示和跨表关联键。字段定义见 `sql/old/old_yigongbao.sql:859-894`。

## 逐字段规则

| # | 列表字段 | 旧来源 | 类型 | 二次处理与风险 | 建议结果 |
|---:|---|---|---|---|---|
| 1 | `id` | `jy_order.od_id` | bigint | 旧主键可直接读取；不能直接当新库 `order_main.id`，要避免与新库 ID 冲突 | `legacyOrderId=od_id`，`DIRECT` |
| 2 | `orderCode` | `od_num` | varchar(32) | 先保留原文；展示时可去首尾空格，但关联和审计使用原值。校验空值、格式、重复订单号 | `rawOrderCode` + `displayOrderCode`，`NORMALIZED` |
| 3 | `status` | `od_status`（用户写的 `od.status` 应修正为此字段） | varchar(32) | 直接保存和展示旧字符串，例如“已完成”“生产完成”“设计完成”；不要转换成新版整数状态。若需要颜色/统计，另建辅助配置，不覆盖原值 | `statusRaw`，`DIRECT/RAW_ONLY` |
| 4 | `businessTypeName` | `od_orderType` | varchar(64) | 旧值为业务、代理、试用、测试等中文快照，不等同新系统业务类型 code。trim，空串转 null；可建独立旧业务类型字典 | `businessTypeRaw/display`，`DIRECT`（不写新字典 code） |
| 5 | `needsPhysicalDeliveryName` | `od_print` | varchar(10) | 仅按“打印/不打印”展示；空串、NULL、其他值标未知。不能推导新版“实体交付/异地打印”的完整语义 | `physicalDeliveryRaw/display`，`NORMALIZED` |
| 6 | `orgName` | `od_dept` | varchar(32) | DDL 注释为“所属部门”，不一定是机构；直接映射会把部门误标成机构。建议列名改为“旧所属部门”，或保留 `legacyOrgName` 并标注来源字段 | `legacyDeptName`，`RAW_ONLY`（语义待确认） |
| 7 | `operatorName` | `od_clerkname` | varchar(32) | 订单快照，可直接展示；trim 空白，不回查新用户覆盖旧名称 | `operatorName`，`DIRECT` |
| 8 | `operatorPhone` | `od_clerkTel` | varchar(32) | 直接保留字符串；不要强制手机号正则，历史号码可能含分机/格式异常 | `operatorPhone`，`DIRECT` |
| 9 | `hospitalName` | `od_hospital` | varchar(32) | 文本快照，直接展示；不要按名称盲关联新医院 ID | `hospitalName`，`DIRECT` |
| 10 | `areaName` | `od_dist` | varchar(32) | 文本地区，直接展示；不能拆成新版 `areaId/fullAreaName`，除非另有可靠行政区映射表 | `areaName`，`DIRECT` |
| 11 | `hospitalDeptName` | `od_office` | varchar(32) | 文本科室快照，直接展示；不回查新科室覆盖 | `hospitalDeptName`，`DIRECT` |
| 12 | `doctorName` | `od_doctor` | varchar(63) | 直接取订单快照；优先订单主表值，其他表只做核对，不用新医生表覆盖 | `doctorName`，`DIRECT` |
| 13 | `doctorPhone` | `od_docTel` | varchar(12) | 直接保留；空值不补写新医生电话，避免历史时点信息被篡改 | `doctorPhone`，`DIRECT` |
| 14 | `patientName` | `od_patient` | varchar(63) | 直接展示；注意脱敏和权限；不要与新患者实体合并 | `patientName`，`DIRECT` |
| 15 | `patientAge` | `od_patientAge` | varchar(10) | 保留原文；可尝试生成整数年龄，但“新生儿、岁、空”等值不能丢弃。非法解析时 normalized=null | `patientAgeRaw` + nullable `patientAgeNumber`，`NORMALIZED` |
| 16 | `patientGenderName` | `od_patientSex` | char | DDL 注释是 0/1，但实际样本有“男/女”；兼容 `0/男→男`、`1/女→女`，其他值原样显示，不能直接写新版性别 code | `genderRaw` + `genderDisplay`，`NORMALIZED` |
| 17 | `isPostal/postalAddress` | `jy_instruct.inst_mail/inst_mailaddress` | 跨表、多行 | 按 `od_num` 关联多个指令。规则建议：任一指令“是”则 `isPostal=1`；全部明确“否”则 0；其余未知。地址取非空去重后合并，并保留每个指令的原值、`inst_id`、`inst_packnum` | `ASSEMBLED`；不能从 `jy_order` 单表直接取 |
| 18 | `designerName` | `od_designername` | varchar(32) | 订单设计师姓名快照，可直接展示；空串转 null；没有设计师 ID，不回填新用户 | `designerName`，`DIRECT` |
| 19 | `expectedDeliveryDate` | `od_preDelivery` | datetime | 可直接取 datetime；同时记录 NULL/异常。不要用 `od_delivery` 替代，后者语义不同；列表展示统一时区/格式 | `expectedDeliveryDate` + raw，`NORMALIZED` |
| 20 | `estimatedCost` | `od_subjectCost` | varchar(32) | 实际包含数字、协议价、待定、空值及十六进制样式字符串。必须原文保留；仅纯十进制/小数生成 `estimatedCostNumber`，其余 number=null 并标记原因 | `costRaw` + nullable numeric，`RAW_ONLY/NORMALIZED` |
| 21 | `dataEvaluationOpinion` | `od_assess` | varchar(64) | 直接展示旧评估意见；不改名为新版审核意见。trim 后为空转 null；如导出字符长度异常，先核对编码，不静默截断 | `assessmentRaw/display`，`DIRECT` |
| 22 | `rebuildProjectList` | `od_requirePart`、`od_rebuildSub`、`od_subjectExpl` + `jy_module` | 逗号分隔 ID/文本 | 按逗号拆数组、保留原串和顺序；用 `jy_module` 查名称，查不到保留 ID。三组数组按位置组装，长度不一致时不能错位硬拼；旧数据存在 17 条长度不一致。`od_others` 可作为补充要求，`od_print` 只能弱映射成打印要求 | `ASSEMBLED`，同时保存三组 raw 数组和解析错误 |
| 23 | `designStartTime` | `ds_time` | datetime | 字段无 DDL 业务注释，需用旧代码/流程样本确认；可读 datetime 直接转换，否则保留 raw。不能凭字段名保证含义 | `designStartRaw` + nullable time，`NORMALIZED/RAW_ONLY` |
| 24 | `designSubmitTime` | `dc_time` | datetime | 同上；需确认 `dc` 是否设计完成/提交。非法/哨兵值不得写入标准时间 | `designSubmitRaw` + nullable time，`NORMALIZED/RAW_ONLY` |
| 25 | `productionStartTime` | `ps_time` | datetime | 可先取订单字段；建议与 `jy_flow` 首条生产时间交叉核对，冲突时两者都保留并标来源，不覆盖 | `productionStartRaw` + `productionStartDerived`，`NORMALIZED/ASSEMBLED` |
| 26 | `productionEndTime` | `pc_time` | datetime | 与 `jy_flow` 完成事件交叉核对；处理 1899-12-31、`NaN-aN-aN aN:aN` 等异常哨兵，标准时间置 null、原值保留 | `productionEndRaw` + nullable time，`NORMALIZED/ASSEMBLED` |
| 27 | `createTime` | `od_createtime` | datetime | 可直接读取；保留原时区/格式策略，空值单独统计；用于默认排序时按标准时间列 | `createTime`，`DIRECT` |

## 四类处理清单

### 可直接取值（清洗空白即可）

`status`、`operatorName`、`operatorPhone`、`hospitalName`、`areaName`、`hospitalDeptName`、`doctorName`、`doctorPhone`、`patientName`、`designerName`、`dataEvaluationOpinion`、`createTime`。这些字段是旧订单快照，历史页应优先相信旧值，而不是重新关联新版组织/人员表。

### 需要标准化转换

`id`（改为来源 ID）、`orderCode`（校验/去首尾空格但保留 raw）、`businessTypeName`（旧字典独立展示）、`needsPhysicalDeliveryName`（仅打印语义）、`patientAge`、`patientGenderName`、`expectedDeliveryDate`、`estimatedCost`、四个阶段时间。`status` 不属于转换字段，默认直接保存旧字符串。

### 需要跨表组装

`isPostal/postalAddress` 需要按订单聚合 `jy_instruct`；`rebuildProjectList` 需要拆分三组数组并关联 `jy_module`；生产起止时间最好与 `jy_flow` 交叉核验。组装结果必须保留来源行 ID，便于解释“一个订单为何出现多个地址/项目/时间”。

### 必须修复或保留原文

- 空串、NULL、`/`、字面量 `null`、非法日期、1899 年哨兵值不得直接展示为正常值。
- `od_subjectCost` 不得强转金额后丢掉“协议价/待定”等文本。
- 状态、业务类型、性别、打印要求不得未经字典证据转换成新版 code；本列表阶段状态直接保留旧字符串。
- `orgName=od_dept` 存在“部门/机构”语义风险，建议历史列表直接显示“旧所属部门”。
- `designStartTime/designSubmitTime` 的缩写语义必须从旧代码或人工抽样确认后再定名。

## 建议的归档字段

```text
legacy_order_id, legacy_order_code,
raw_status, raw_business_type, raw_print,
legacy_dept_name, operator_name, operator_phone,
hospital_name, area_name, office_name,
doctor_name, doctor_phone, patient_name,
patient_age_raw, patient_age_number, patient_gender_raw,
postal_flag, postal_addresses_json,
designer_name, pre_delivery_raw, pre_delivery_time,
subject_cost_raw, subject_cost_number, subject_cost_parse_status,
assessment_raw,
require_part_raw, rebuild_sub_raw, subject_expl_raw,
rebuild_projects_json, project_parse_status,
design_start_raw, design_start_time,
design_submit_raw, design_submit_time,
production_start_raw, production_start_time,
production_end_raw, production_end_time,
create_time_raw, create_time
```

列表接口可以只返回展示字段，但详情接口应能查看 raw 值、转换状态和来源表/来源 ID。这样既满足历史查询，也不会因为一次错误映射破坏原始证据。
