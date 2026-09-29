# 旧医工宝历史订单列表页实施计划

## 1. 本阶段范围

本阶段只实现一个独立的“历史订单列表”菜单页，并提供受控的“增量迁移”入口：

- 分页查询历史订单；
- 按订单号、状态、业务类型、医院、地区、科室、医生、患者、创建时间等条件筛选；
- 展示已确认可获取的历史字段；
- 支持排序、空值/异常值展示和数据来源标识；
- 管理员可手动触发旧库到历史归档表的增量迁移，并查看任务结果；
- 预留点击详情、文件查询的入口，但本阶段不实现详情接口、流程操作和文件下载。

不改造新订单流程，不把历史订单写入 `order_main`，不参与新系统订单统计和流程状态机。增量按钮只读旧库、写入历史归档表，不执行旧库写操作。

## 2. 推荐总体方案

采用“旧库抽取 → 独立历史归档表 → 新系统只读查询接口 → 独立前端列表页”的链路。

```text
旧 jy_order / jy_instruct / jy_module
              ↓ 一次性迁移 + 可重复校验
       legacy_order_list（历史列表主表）
              ↓ 只读 API
       /legacy-order/page
              ↓
      历史订单列表菜单页
```

`legacy_order_list` 保存列表所需的标准化字段、旧值原文、转换状态和来源信息。旧表原始字段不删除、不覆盖，必要时以 JSON 或扩展列保存。

增量迁移建议采用异步任务：前端点击后创建任务，后台在独立事务中按批次读取旧库并幂等 upsert，前端通过任务状态查询展示进度。不能在 HTTP 请求内同步执行整批迁移。

## 3. 实施阶段与任务

### 阶段一：冻结列表需求和验收口径

1. 确认列表列集合：本阶段以 27 个字段为基础，但前端默认显示建议先选 15～20 个高价值字段，其余通过列设置或展开区域展示。
2. 确认筛选项：订单号、旧状态、业务类型、打印要求、所属部门、医院、地区、科室、医生、患者、创建时间范围。
3. 确认排序：默认创建时间倒序；支持订单号、创建时间、状态排序。
4. 确认空值显示：空字符串、NULL、解析失败统一显示 `-`；异常原文通过悬浮提示或来源标记查看。
5. 确认权限：历史列表使用新系统登录态和菜单权限，不套用新订单的数据范围规则；本阶段授权角色固定为超级管理员 `admin`、公司管理员 `company-admin`、设计师管理员 `designer-manager`，三者可查看历史列表并触发增量迁移。普通设计师不是授权角色。
6. 确认数据截止点：第一次迁移覆盖旧库全量，后续是否允许增量补迁，确定迁移快照时间。

**产出物**：列表原型/字段清单、筛选排序说明、验收样例、权限口径。

### 阶段二：建立字段映射和转换契约

1. 固化 27 字段映射表：旧字段、目标字段、数据类型、转换规则、空值规则、异常规则、来源状态。
2. 定义映射状态：
   - `DIRECT`：直接取旧快照；
   - `NORMALIZED`：类型或编码规范化；
   - `ASSEMBLED`：跨表聚合或数组组装；
   - `RAW_ONLY`：只保留原文，不能安全转成新版语义；
   - `MISSING`：旧库无可靠来源。
3. `status` 明确采用旧值原样保存策略：`od_status` 中的“已完成”“生产完成”“设计完成”等字符串直接写入 `status_raw`，列表默认展示 `status_raw`；不转换为新版整数状态码，不调用新版流程状态字典。若将来需要颜色或统计，只增加独立的 `legacy_status_config`，不得覆盖原状态字符串。
4. 业务类型、性别、打印要求同样优先保存旧原文；标准化字段仅作为可选辅助列，不能替换旧值。
5. 定义日期和金额转换规则：转换失败不丢原值，标准列置 NULL 并记录原因。
6. 定义 `rebuildProjectList` 的 JSON 结构：保存三组原始数组、模块 ID、模块名称、位置索引、数组长度不一致标识。
7. 定义邮寄字段聚合规则：按订单关联多个 `jy_instruct`，任一“是”则标记是；地址去重合并；保留来源指令 ID。

**产出物及位置**：

- 字段映射文档：`docs/research/legacy-order-27-field-mapping.md`；
- 实施计划：`docs/plans/2026-09-28-legacy-order-list-implementation-plan.md`；
- 转换规则单元测试：`yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/legacy/LegacyOrderFieldConverterTest.java`；
- 旧状态原文样本：`sql/old/jy_order.sql` 及 `docs/research/old-data-attachment-production-samples.md`。
- 权限矩阵：本计划“阶段六补充：菜单资源和角色权限配置”及 `docs/api/legacy-order-list.md`。

### 阶段三：设计历史列表归档表

建议至少建立以下表：

#### 3.1 `legacy_order_list`

保存分页、筛选、排序直接需要的字段：

```text
id                         新归档主键
source_system              OLD_YIGONGBAO
source_order_id            od_id
source_order_code          od_num
status_raw（直接保存 od_status 原文）
business_type_raw
print_raw/physical_delivery_label
legacy_dept_name
operator_name/operator_phone
hospital_name/area_name/hospital_dept_name
doctor_name/doctor_phone
patient_name/patient_age_raw/patient_age_number
patient_gender_raw/patient_gender_label
postal_flag/postal_address_display
designer_name
expected_delivery_raw/expected_delivery_time
estimated_cost_raw/estimated_cost_number/cost_parse_status
data_evaluation_opinion
rebuild_project_summary/rebuild_project_json/project_parse_status
design_start_raw/design_start_time
design_submit_raw/design_submit_time
production_start_raw/production_start_time
production_end_raw/production_end_time
create_time_raw/create_time
source_snapshot_at
mapping_version
is_deleted
```

#### 3.2 `legacy_order_source_ext`

保存不适合进入列表主表的原始字段和异常信息，例如 `od_fee`、`od_user_id`、`od_docid`、`od_patientTel`、`od_delivery`、`od_subTime`、`od_others`、四个时间原始值、原始三组项目数组。

#### 3.3 `legacy_order_project`

如果项目筛选或统计需要结构化查询，则单独保存：

```text
legacy_order_id, source_order_id, item_index,
require_part_id, require_part_name,
rebuild_sub_id, rebuild_sub_name,
subject_expl_id, subject_expl_name,
other_requirement_raw, print_requirement_raw,
source_raw_json
```

若本阶段仅展示项目摘要，可先只保存 JSON，避免过早设计复杂关系表。

#### 3.4 索引

至少建立：

- `(source_order_code)`；
- `(status_raw)`；
- `(business_type_raw)`；
- `(hospital_name)`、`(doctor_name)`、`(patient_name)`；
- `(create_time)`；
- 组合索引 `(create_time, id)` 用于稳定分页。

订单号必须允许重复，不能对 `source_order_code` 建唯一约束。

**产出物及位置**：

- 数据库迁移 SQL：`yigongbao-parent/sql/migration/2026-09-28-add-legacy-order-list.sql`；
- 归档表数据字典：`docs/research/legacy-order-list-schema.md`；
- 后续实体类位置：`yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/entity/legacy/`。

### 阶段四：开发迁移抽取和转换程序

1. 从旧库副本或 SQL 导出读取，不直接在生产旧库上执行大查询。
2. 以 `od_id` 分段或按主键游标批量读取，避免一次性加载几十 MB 文件。
3. 先抽取 `jy_order`，建立 `od_id/od_num` 索引关系。
4. 抽取 `jy_module`，建立旧模块 ID→名称缓存。
5. 按订单号聚合 `jy_instruct`，计算邮寄字段和来源指令数。
6. 转换 27 个字段，所有转换失败记录到迁移错误表：

```text
legacy_migration_error(
  batch_id, source_table, source_id, source_order_code,
  field_name, raw_value, error_code, error_message
)
```

7. 使用批次号、映射版本和来源主键实现幂等：重复执行只更新同一来源记录，不产生重复历史订单。
8. 生成迁移统计：源订单数、目标订单数、重复订单号、空订单号、各状态数量、各字段非空率、转换失败数量。
9. 对异常日期、金额、项目数组、状态分别输出复核清单。

**产出物及位置**：

- 迁移任务实体：`yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/entity/legacy/LegacyMigrationTaskEntity.java`；
- 迁移错误实体：同目录 `LegacyMigrationErrorEntity.java`；
- 迁移 Mapper：`.../mapper/legacy/LegacyOrderArchiveMapper.java`、`LegacyMigrationTaskMapper.java`；
- 字段转换器：`.../service/legacy/LegacyOrderFieldConverter.java`；
- 批量迁移服务：`.../service/legacy/LegacyOrderMigrationService.java`；
- 旧库读取 DAO：`.../mapper/legacy/OldYigongbaoOrderReader.java` 或独立 MyBatis mapper；
- 任务执行器：`.../task/LegacyOrderMigrationTaskRunner.java`；
- 迁移统计报告：`docs/research/legacy-order-migration-validation.md`；
- 运行配置说明：`docs/operations/legacy-order-migration.md`。

### 阶段四补充：增量迁移按钮和同步任务

#### 4.1 旧库连接配置

1. 在后端配置文件/配置中心中增加旧库连接：JDBC URL、用户名、密码、只读账号、连接超时、批量大小、最大执行时间。
2. 密码必须使用现有密钥管理或加密配置，禁止写入前端、日志、SQL 错误和接口返回。
3. 旧库账号只授予 `SELECT` 权限；应用启动时执行一次连接健康检查，失败时按钮显示不可用原因。
4. 不允许前端传入 JDBC URL、表名、SQL 或数据库凭据。

#### 4.2 迁移任务表

新增 `legacy_migration_task`：

```text
id, task_type, status, requested_by, requested_at,
started_at, finished_at, source_snapshot_at,
watermark_before, watermark_after,
total_read, inserted_count, updated_count,
skipped_count, error_count, error_message,
mapping_version, batch_size
```

状态建议：`PENDING`、`RUNNING`、`SUCCESS`、`PARTIAL_SUCCESS`、`FAILED`、`CANCEL_REQUESTED`、`CANCELLED`。

如需明细审计，再增加 `legacy_migration_task_item`，记录来源表、来源主键、订单号、操作类型、错误码和错误信息。

#### 4.3 增量识别策略

旧 `jy_order` 没有可靠的 `update_time`。因此不能只用“上次最大 `od_id`”保证状态更新不丢失，应采用分层策略：

1. **新增订单**：以 `(od_createtime, od_id)` 作为游标，读取上次水位之后的新记录。
2. **近期变更回看**：每次任务向前回看可配置窗口（例如 7～30 天），对窗口内订单重新计算并 upsert，覆盖旧订单状态、打印要求、设计师和时间字段变化。
3. **历史修复/长期未变更订单**：提供管理员手动“全量校验”任务，或按月份/年份分片重扫；不能假设旧订单状态永远不变。
4. **最佳方案**：如果后续能修改旧系统，在旧库增加变更日志/更新时间/触发器；否则只能接受“窗口外状态变化无法被纯增量自动发现”的限制，并在页面显示最后同步时间和同步策略。

#### 4.4 幂等和写入规则

1. 以 `(source_system, source_order_id)` 作为唯一键；绝不以订单号单独 upsert。
2. 同一来源记录重复读取时执行更新，不新增重复归档行。
3. 每条记录保存 `source_row_hash`；哈希未变化时跳过字段转换，仅计入 `skipped_count`。
4. 哈希变化时更新标准化字段和原始字段，并记录迁移任务 ID、映射版本和更新时间。
5. `legacy_order_project` 等子表按来源主键/索引重建，避免旧项目数组变化后产生脏的残留项目。
6. 迁移任务每批提交一次事务，不能把几万条记录放进单个长事务。

#### 4.5 并发控制

1. 同一时间只允许一个 `INCREMENTAL` 任务运行；数据库使用分布式锁或任务表唯一运行约束。
2. 如果用户重复点击，接口返回当前运行中的任务 ID，不创建第二个任务。
3. 如后台保留全量校验能力，它只作为内部运维任务，不在本页面增加按钮或资源；运行任何迁移任务时按钮显示“任务进行中”。
4. 任务进程异常退出后，超过租约时间的 `RUNNING` 任务标记为 `FAILED`，允许管理员重试。
5. 重试必须复用同一来源幂等键，不能简单重新插入。

#### 4.6 接口设计

```text
POST /api/legacy-order/migration/incremental
GET  /api/legacy-order/migration/tasks/{taskId}
GET  /api/legacy-order/migration/tasks/latest
```

创建任务请求建议包含：

```text
mode: INCREMENTAL
lookbackDays: 默认配置值，管理员可选范围
```

本页面只提供 `INCREMENTAL`；不新增全量校验、取消任务、查看任务等前端按钮资源。迁移任务进度和结果作为增量迁移操作的返回内容展示。

创建接口立即返回 `taskId`，不等待任务完成。任务状态接口返回进度、读取数量、插入数量、更新数量、跳过数量、错误数量、开始/结束时间和最后错误摘要，但不返回数据库密码或完整 SQL。

#### 4.7 前端按钮交互

1. 在历史订单列表工具栏增加“增量迁移”按钮，仅对有权限的用户显示。
2. 点击后弹出确认框，明确显示：
   - 读取旧库数据并写入历史归档；
   - 不修改旧系统和新订单；
   - 本次同步策略和回看天数；
   - 可能存在旧库无更新时间导致的同步限制。
3. 提交成功后显示任务状态抽屉/弹窗，展示进度和结果。
4. 任务运行期间按钮禁用，其他用户看到“迁移进行中”。
5. 成功后提供“刷新列表”；部分成功显示错误数量和错误清单入口；失败显示可重试按钮。
6. 不在列表刷新时自动触发迁移，避免用户误以为每次查询都会访问旧库。

#### 4.8 安全和审计

1. 增量迁移按钮使用唯一按钮权限 `legacyOrder:Migrate`。
2. 每次触发记录操作人、IP、时间、模式、回看窗口、源快照时间和结果。
3. 所有失败必须可定位到来源表、来源主键、订单号和字段；敏感患者信息在普通日志中脱敏。
4. 接口增加频率限制和最小触发间隔，防止重复点击导致旧库压力过大。
5. 旧库查询设置只读、连接池上限和超时；任务达到最大运行时间自动停止并标记失败。

**阶段四补充产出物及位置**：

- 旧库数据源配置：后端实际配置文件对应的 `application-*.yml` 配置段；密码只进入部署密钥，不提交仓库；
- 迁移任务表 DDL：`yigongbao-parent/sql/migration/2026-09-28-add-legacy-order-list.sql`；
- 增量任务服务：`.../service/legacy/LegacyOrderMigrationService.java`；
- 任务状态接口：`.../controller/legacy/LegacyOrderMigrationController.java`；
- 任务 DTO/VO：`.../dto/legacy/`、`.../vo/legacy/`；
- 任务执行器：`.../task/LegacyOrderMigrationTaskRunner.java`；
- 权限资源：同一迁移 SQL 中的 `sys_resource/sys_role_resource` 幂等初始化；
- 审计日志：沿用系统操作日志模块；若没有合适事件类型，新增 `LEGACY_ORDER_MIGRATION` 事件类型并在 `docs/operations/legacy-order-migration.md` 记录字段。

### 阶段五：开发后端只读列表接口

建议新增独立模块或在订单模块下新增 legacy 子包，不复用新订单查询 Service。

#### 5.1 接口

```text
POST /api/legacy-order/page
POST /api/legacy-order/export（如本阶段需要导出）
GET  /api/legacy-order/column-config（可选）
```

请求参数：

```text
pageNum/pageSize
orderCode
status
businessType
printRequirement
deptName
hospitalName
areaName
hospitalDeptName
doctorName
patientName
createTimeStart/createTimeEnd
sortField/sortOrder
```

#### 5.2 返回模型

列表返回标准展示字段，同时可返回：

```text
sourceSystem
sourceOrderId
mappingWarnings
fileCount（若迁移阶段已统计，可选）
```

不要返回新订单的 `availableActions`，不要提供编辑、提交、审核、取消等操作。

#### 5.3 查询安全和性能

1. 排序字段使用白名单，禁止前端直接拼接 SQL。
2. 所有字符串查询使用参数绑定；模糊查询限制最小长度或合理索引策略。
3. 默认分页大小限制为 20/50/100，禁止无界查询。
4. 采用稳定排序：`create_time DESC, id DESC`。
5. 历史表只读权限与新订单写权限分离。

**产出物及位置**：

- Controller：`yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/legacy/LegacyOrderController.java`；
- 查询 DTO：`.../dto/legacy/LegacyOrderPageDTO.java`；
- 列表 VO：`.../vo/legacy/LegacyOrderListVO.java`；
- 查询 Service：`.../service/legacy/LegacyOrderQueryService.java`；
- Mapper/XML：`.../mapper/legacy/LegacyOrderListMapper.java` 和 `src/main/resources/mapper/legacy/LegacyOrderListMapper.xml`；
- 接口测试：`yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/legacy/LegacyOrderControllerTest.java`；
- API 说明：`docs/api/legacy-order-list.md`。

### 阶段六：开发前端历史订单列表页

1. 新增菜单，例如“历史订单”或“旧系统订单”。
2. 页面独立路由、独立 API 客户端，不复用新订单列表的编辑/流程按钮。
3. 第一版建议列：订单编号、旧状态、业务类型、打印要求、所属部门、医院、地区、科室、医生、患者、性别、设计师、预估交付、预估费用、创建时间、项目摘要。
4. 对列表 27 字段按列配置决定显示，不要求与新订单列表完全一致。
5. 异常值显示：
   - 空值：`-`；
   - 状态：直接显示旧库原文，例如“已完成”“生产完成”“设计完成”；空值显示 `-`，不追加新版状态名称；
   - 金额非数值：原文；
   - 时间解析失败：原文或异常图标；
   - 项目数组不一致：摘要后显示警告标记。
6. 支持分页、筛选、排序、重置条件。
7. 暂不实现详情跳转；操作列可以显示“详情待上线”或先不显示。
8. 对患者、电话等敏感字段遵循现有脱敏和权限规范。

**产出物及位置**：

- API：`frontend/med-tech/src/api/legacyOrder.ts`；
- 页面：`frontend/med-tech/src/views/business/legacyOrder.vue`；
- 类型：`frontend/med-tech/src/views/business/legacyOrder.types.ts`；
- 路由/菜单注册：按现有路由组织方式修改 `frontend/med-tech/src/router/` 下的业务路由文件；
- 权限指令/按钮控制：沿用现有权限工具，使用实际资源编码 `legacyOrder:Migrate`；
- 页面测试：`frontend/med-tech/src/views/business/__tests__/legacyOrder.spec.ts`。

### 阶段六补充：菜单资源和角色权限配置

新增页面必须同时配置数据库资源和前端路由，不能只在前端写死菜单。根据当前 `sys_resource` 的实际数据格式，本页只新增一个二级菜单资源和一个迁移按钮资源，不新增列表查询、任务查看、全量校验、任务取消等额外资源。

#### 6.1 资源层级

建议新增：

| 类型 | 名称 | resource_code | 建议路径/组件 |
|---|---|---|---|
| 二级菜单 | 历史订单 | `LegacyOrder` | `/legacyOrder` / `business/legacyOrder.vue` |
| 按钮 | 增量迁移 | `legacyOrder:Migrate` | 无 |

`resource_type` 使用现有约定：二级菜单为 2，按钮为 3；按钮的 `parent_id` 指向历史订单菜单资源 ID。资源 ID 不在代码中硬编码，角色关联 SQL 应通过 `resource_code` 查询资源 ID。

#### 6.2 角色授权

按当前代码中的角色编码授权：

| 角色名称 | role_code | 菜单访问 | 增量迁移按钮 |
|---|---|---:|---:|
| 超级管理员 | `admin` | 是 | 是 |
| 公司管理员 | `company-admin` | 是 | 是 |
| 设计师管理员 | `designer-manager` | 是 | 是 |

用户提出的“设计师管理员”在代码枚举中对应 `RoleCodeEnum.DESIGNER_MANAGER`，实际编码为 `designer-manager`；不能使用 `designer`（普通设计师）。

#### 6.3 SQL 和权限验证

在 `yigongbao-parent/sql/migration/2026-09-28-add-legacy-order-list.sql` 中：

1. 按当前表字段格式插入一个 `resource_type=2` 的二级菜单：`parent_id` 指向业务运营一级菜单（当前示例为 `resource_code='Business'`），`resource_code='LegacyOrder'`，`path='/legacyOrder'`，`component='business/legacyOrder.vue'`；
2. 插入一个 `resource_type=3` 的按钮：`parent_id` 指向 `LegacyOrder` 菜单，`resource_name='增量迁移'`，`resource_code='legacyOrder:Migrate'`；
3. 使用 `INSERT ... SELECT ... WHERE NOT EXISTS` 按 `resource_code` 幂等插入；
4. 通过 `sys_role.role_code` 查询 `admin`、`company-admin`、`designer-manager` 的 role_id；
5. 仅将上述两个资源关联到三个角色，不新增其他资源或权限；
6. 不直接假设角色 ID、菜单 ID 或按钮 ID；
7. 若业务运营一级菜单或任一角色不存在，迁移 SQL 应失败并给出明确提示，不能静默漏授权。

#### 6.4 后端权限校验

- `/api/legacy-order/page`：由 `LegacyOrder` 菜单权限保护；
- `/api/legacy-order/migration/incremental`：要求 `legacyOrder:Migrate`；
- 不新增其他按钮权限；迁移任务状态由页面内部展示，不配置独立资源。
- 前端隐藏按钮不能替代后端鉴权；
- 历史列表不套用新订单的医院/机构数据范围，默认按上述角色授予历史全量只读权限，除非后续另行定义历史数据范围规则。

#### 6.5 权限测试

在 `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/legacy/LegacyOrderPermissionTest.java` 验证：

- 三个指定角色能打开菜单和调用列表接口；
- 三个指定角色能触发增量迁移；
- 普通设计师、业务员、生产员不能查看菜单或调用接口；
- 无 `legacyOrder:Migrate` 权限直接调用迁移接口返回 403/统一无权限错误；
- 数据库中不出现多余的历史订单列表按钮资源。

**产出物**：资源 SQL、角色资源关联 SQL、前后端权限编码清单、权限测试报告。

### 阶段七：数据与接口联调

1. 使用 20～100 条代表性订单联调：正常订单、草稿、取消、无邮寄、多指令、多项目、异常金额、异常日期、缺失医院/医生。
2. 前后端逐字段核对：页面值、接口值、归档表值、旧 SQL 原值四方一致。
3. 核对分页总数、排序稳定性、重复订单号展示、空值行为。
4. 核对项目摘要是否出现数组错位。
5. 核对状态和业务类型筛选使用旧值原文，不误套新系统字典；例如筛选“已完成”必须直接命中 `status_raw='已完成'`。

**产出物**：联调记录、字段核对表、问题清单和修复结果。

### 阶段八：全量迁移和验收

1. 在正式环境执行全量迁移前做数据库备份。
2. 记录批次号、源快照时间、目标数据量、程序版本和映射版本。
3. 迁移完成后执行自动校验：
   - `jy_order` 行数与 `legacy_order_list` 行数一致；
   - `od_id` 无重复、无丢失；
   - `od_num` 重复情况与源库一致；
   - `status_raw` 状态分布一致，字符串逐项核对；
   - 创建时间最小/最大值一致；
   - 27 字段非空率与预期一致；
   - 异常值数量与迁移错误表一致。
4. 小范围用户试用历史列表，确认筛选、排序和展示语义。
5. 通过菜单权限灰度开放，再全量开放。

6. 验证增量按钮：首次新增记录、重复点击、任务失败重试、窗口内状态变化、窗口外状态变化提示和权限拒绝。

**产出物**：正式迁移报告、验收报告、上线记录、回滚方案。

## 4. 推荐里程碑

| 里程碑 | 完成标准 |
|---|---|
| M1 字段和页面口径冻结 | 27 字段映射、列清单、筛选、权限确认 |
| M2 归档模型完成 | DDL、索引、错误表、映射版本确定 |
| M3 迁移工具完成 | 小批量可重复迁移，统计和错误清单可生成 |
| M4 增量迁移完成 | 任务表、连接配置、幂等、并发锁、重试、审计测试通过 |
| M5 后端完成 | 分页、筛选、排序、权限、迁移接口测试通过 |
| M6 前端完成 | 菜单、列表、迁移按钮、进度和异常展示通过 |
| M7 联调验收 | 与旧库抽样核对，增量场景测试通过 |
| M8 正式上线 | 全量迁移、校验、灰度和监控完成 |

## 5. 本阶段明确不做的内容

- 不写入 `order_main`、`order_item`、`order_file`；
- 不复用新版订单流程状态机；
- 不提供历史订单编辑、提交、审核、取消、重建等操作；
- 不实现详情接口和文件下载；
- 不在没有可靠映射时把旧医院、医生、机构、人员绑定到新版 ID；
- 不把旧金额、旧状态、旧时间异常值静默转换后丢弃原文；状态默认直接展示旧字符串。

## 6. 第一批开发建议

实际开发顺序建议为：

1. 先完成 `legacy_order_list` DDL 和 27 字段转换单元测试；
2. 再完成 100 条样本迁移及校验报告；
3. 完成后端 `/legacy-order/page` 和只读列表；
4. 完成增量任务表、旧库只读连接和幂等 upsert；
5. 完成迁移按钮、任务进度和权限审计；
6. 用接口数据完成前端筛选和异常展示；
7. 联调通过后执行首次全量迁移；
8. 最后开放增量迁移权限和正式数据。

这样可以在不影响现有新订单功能的情况下，先验证字段转换和查询模型是否正确。
