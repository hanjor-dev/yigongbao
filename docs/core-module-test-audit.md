# 核心业务模块测试审查记录

## 任务范围

对 `order`、`design`、`production` 三个核心业务模块逐个进行功能盘点、测试覆盖审查和 TDD 验证。测试应优先描述当前业务行为；只有被测试证明的明确缺陷才进入修复范围。

## 工作树

- 分支：`test/core-modules-coverage`
- 基线提交：`b4e952a`
- 工作树：`C:\Users\hanjor_prod\.config\superpowers\worktrees\医工宝\core-modules-coverage`

## 覆盖判定标准

每个核心公开服务/控制器功能至少具备：主成功路径、参数/状态边界、关键权限或业务拒绝路径、持久化/外部依赖交互验证。测试必须在当前版本代码和当前测试配置下可执行并通过。

## 模块进度

| 模块 | 功能盘点 | 测试补齐 | 基线/回归 | 状态 |
|---|---:|---:|---:|---|
| order | 接口/Service 重审完成 | 46 个 Controller 映射、订单草稿/状态/权限/修改/取消/经典案例/导出及全部 ServiceImpl 公共方法均有测试证据 | 206 run，0 failures，0 errors，0 skipped | 接口入口和已识别核心分支闭合 |
| design | 接口/Service 重审完成 | 34 个 Controller 映射、工单/文件包/文档/打印信息/截图/附件/归档及全部 ServiceImpl 公共方法均有测试证据 | 169 run，0 failures，0 errors，0 skipped | 接口入口和已识别核心分支闭合 |
| production | 接口/Service 重审完成 | 33 个 Controller 映射、记录/工序/QC/包装/仓储/产品/设备/流程配置及全部 ServiceImpl 公共方法均有测试证据 | 171 run，0 failures，0 errors，0 skipped | 接口入口和已识别核心分支闭合 |

## 问题与解决方案

| 编号 | 模块 | 位置 | 问题/证据 | 解决方案 | 是否解决 | 验证证据 |
|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — |

### 环境/工具错误

- 2026-07-17：首次统计命令使用了 Bash 风格的路径大括号展开，PowerShell 解析失败；未触及项目代码，改为显式路径后重试。
- 2026-07-17：第二次诊断命令再次使用 Bash 风格的大括号路径展开，PowerShell 解析失败；后续命令改用逐个显式路径。

## 测试记录

## 已确认问题

| 编号 | 模块 | 位置 | 问题/证据 | 解决方案 | 是否解决 | 验证证据 |
|---|---|---|---|---|---|---|
| CORE-ENV-001 | order/basic | `application-test.yml` | Spring Boot 3.2 禁止在 profile-specific 配置中设置 `spring.profiles.active`，基线启动时抛出 `InvalidConfigDataPropertyException` | 删除测试 profile 文件中的自激活配置，继续由 `@ActiveProfiles("test")` 选择 profile | 已修复，待完整回归 | 定向测试已越过该异常，但暴露出下一个 Bean 配置问题 |
| CORE-ENV-002 | basic | `BasicTestApplication` | 正式启动类有 `@EnableFileStorage`，测试启动类缺失该注解 | 为 `BasicTestApplication` 补充 `@EnableFileStorage` | 已解决 | `BodyPartControllerTest` 已进入业务断言阶段 |
| CORE-ENV-003 | basic | `application-test.yml` | `SignAspect` 的 `${app.sign.secret}` 未配置，导致上下文创建失败 | 在模块测试配置中加入固定测试密钥 | 已解决 | 定向测试上下文启动成功 |
| CORE-BASE-001 | basic（依赖基线） | `BodyPartControllerTest` | 13 个测试中 6 个断言失败：当前实现返回 200/650/651，旧用例期望 400/662/663 | 暂不修改，待确认是否属于当前版本契约；不阻断核心模块隔离测试 | 未解决 | `mvn -pl yigongbao-module-basic -Dtest=BodyPartControllerTest test`：13 run，6 failures |
| CORE-ENV-004 | 构建环境 | 本机 Maven 仓库 `D:\08_Maven_Repo\com\yigongbao\yigongbao-module-order\maven-metadata-local.xml` | `mvn ... install` 无法解析含 NUL 字节的本地元数据 | 绕过 install，使用已编译依赖直接执行模块测试；后续必要时清理该单个元数据文件 | 未解决 | 安装阶段失败，编译阶段成功 |
| ORDER-BUG-001 | order | `OrderClassicCaseServiceImpl` | `needsPhysicalDelivery` 可为空时直接拆箱，触发 NPE | 改为 null-safe 判断 | 已解决 | order 124 全绿 |
| ORDER-BUG-002 | order | `OrderMainServiceImpl.listOrders` | 忽略 `patientName` 查询条件 | 增加患者姓名 `like` 条件 | 已解决 | order 124 全绿 |
| DESIGN-BUG-001 | design | `DesignWorkorderServiceImpl.completeDesign` | 未校验当前用户是否为分配设计师 | 增加与启动设计一致的设计师权限校验 | 已解决 | design 全量回归全绿 |
| PRODUCTION-BUG-001 | production | `ProductNumberServiceImpl.generateFormalNumbers` | 仓库无正式编号唯一约束时，同一编号可重复写入 | 生成前查询编号唯一性，重复时抛 `PRODUCT_NUMBER_DUPLICATE` | 已解决 | production 121 全绿 |
| ORDER-BUG-003 | order | `OrderMainServiceImpl.directCancelOrder` | 乐观锁更新取消状态后再次 `updateById` 写回旧订单实体，可能覆盖新状态 | 删除旧实体回写，仅保留条件更新 | 已解决 | 先由回归测试失败证明，再修复后通过 |
| ORDER-BUG-004 | order | `OrderModifyTimeWindowChecker.isWithinTimeWindow` | 未来时间不足 1 分钟时 `ChronoUnit.MINUTES` 截断为 0，错误判定为窗口内 | 先比较时间戳，未来创建时间直接返回 false | 已解决 | 边界回归测试通过 |

| 日期 | 模块/命令 | 结果 | 失败摘要 |
|---|---|---|---|
| 2026-07-17 | `mvn -q -pl yigongbao-module-production test` | 通过 | 121 tests，0 failures，0 errors |
| 2026-07-17 | `mvn -q -pl yigongbao-module-order test` | 通过 | 124 tests，0 failures，0 errors |
| 2026-07-17 | `mvn -q -pl yigongbao-module-design test` | 通过 | 159 tests，0 failures，0 errors，0 skipped |
| 2026-07-17 | `mvn -q -pl yigongbao-module-order -Dtest=OrderModifyTimeWindowCheckerTest,OrderDraftServiceImplPermissionTest,OrderMainServiceImplStateTransitionTest test` | 通过 | 新增订单边界/状态测试 7 tests，0 failures，0 errors |
| 2026-07-17 | `mvn -q -pl yigongbao-module-design -Dtest=DesignSimpleServicesTest,DesignScreenshotServiceImplTest test` | 通过 | 新增设计基础服务/截图测试 10 tests，0 failures，0 errors |
| 2026-07-17 | `mvn -q -pl yigongbao-module-production -Dtest=DeviceUsageCounterServiceImplTest test` | 通过 | 新增设备计数器测试 3 tests，0 failures，0 errors |
| 2026-07-17 | `mvn -q -pl yigongbao-module-order -Dtest=OrderModifyApplyServiceImplBoundaryTest test` | 通过 | 新增修改申请拒绝路径 3 tests，0 failures，0 errors |
| 2026-07-17 | `mvn -q -pl yigongbao-module-order -Dtest=OrderExportServiceImplTest test` | 通过 | 新增导出字段/空配置/空统计测试 3 tests，0 failures，0 errors |

## order 模块盘点（第一轮）

### 业务入口

当前源码包含订单草稿、订单创建/查询/详情、提交/撤回/审核/驳回/重提、取消、手动完成、设计师查询与分配、列配置、导出、流程选择/调试、订单修改申请、取消申请、经典案例、文件与订单明细等入口。核心实现服务包括 `OrderDraftServiceImpl`、`OrderMainServiceImpl`、`OrderItemServiceImpl`、`OrderFileServiceImpl`、`DesignerAssignmentServiceImpl`、`OrderCancelApplyServiceImpl`、`OrderModifyApplyServiceImpl`、`OrderModifyFullServiceImpl`、`OrderClassicCaseServiceImpl`、`ClassicCaseFileServiceImpl` 和导出服务。

### 现有测试基线

- 11 个测试类，模块测试共执行 123 个用例。
- 通过：控制器取消申请 6、经典案例控制器 3、差异计算 5、查询 Helper 及部分服务测试。
- 失败：2 failures、17 errors。
- 主要问题分组：
  - 测试装配过时：`ClassicCaseFileServiceImplTest` 未提供新增的 `FileStorageService`/`FileRecorderService` mock。
  - 反射注入错误：`OrderClassicCaseServiceImplTest` 对 `ServiceImpl.baseMapper` 的注入在当前版本失败。
  - 构造依赖漏 mock：`DesignerAssignmentServiceImplTest` 未提供 `ApplicationEventPublisher`，部分测试还依赖已注释的专业方向校验。
- `OrderMainServiceImplListOrdersTest` 未 mock 新增的 `OrderConvert`，导致 VO 转换路径 NPE；患者姓名条件断言与当前 wrapper 实现不一致。

### 已处理的 order 问题

| 编号 | 类型 | 处理 | 验证 |
|---|---|---|---|
| ORDER-BUG-001 | 明确业务 bug | `OrderClassicCaseServiceImpl` 对可空 `needsPhysicalDelivery` 使用直接拆箱，改为 null-safe 比较 | 新增 null 回归用例；`OrderClassicCaseServiceImplTest` 10/10 通过 |
| ORDER-TEST-001 | 测试过时 | `ClassicCaseFileServiceImplTest` 改按当前 `FileStorageService.move()` 和 `FileRecorderService.getById()` 验证文件 ID 迁移 | `ClassicCaseFileServiceImplTest` 3/3 通过 |
| ORDER-TEST-002 | 测试装配过时 | `OrderClassicCaseServiceImplTest` 补齐当前构造依赖，移除错误的 `baseMapper` 反射注入，详情断言按当前 converter 输入准备 | `OrderClassicCaseServiceImplTest` 10/10 通过 |
| ORDER-TEST-003 | 测试装配/契约过时 | `DesignerAssignmentServiceImplTest` 补 `ApplicationEventPublisher` 和权限查询 mock；专业方向不匹配用例改为验证当前版本已取消该校验 | `DesignerAssignmentServiceImplTest` 20/20 通过 |
| ORDER-BUG-002 | 明确业务 bug | `OrderMainServiceImpl.listOrders` 忽略 DTO 的 `patientName` 条件；补充独立 `like` 条件 | `OrderMainServiceImplListOrdersTest` 29/29 通过 |

### order 阶段结果

2026-07-17：`mvn -pl yigongbao-module-order test` 通过，124 tests、0 failures、0 errors。已完成本轮核心服务/控制器回归；草稿、主订单状态流转、修改申请、导出和流程调试仍属于后续扩展覆盖项。

### order 下一步

## design 模块盘点（第一轮）

### 业务入口

当前源码包含设计工单查询/详情/启动/完成、设计文件包上传删除与模型关联、设计产品/产品文件、指令书与图纸生成/预览/下载/修订/确认、打印信息、截图、附件及经典案例文件监听等入口。核心实现服务包括 `DesignWorkorderServiceImpl`、`DesignFileServiceImpl`、`DesignDocServiceImpl`、`DesignPrintInfoServiceImpl`、`DesignProductServiceImpl` 和 `DesignProductFileServiceImpl`。

### 现有测试基线

- 10 个测试类，首次执行 `mvn -pl yigongbao-module-design test` 编译阶段发现 9 处测试仍调用已变更为 `(orderId, version)` 的启动/完成接口，已统一补充版本参数。
- 修正接口签名后：121 tests，33 failures，26 errors，4 skipped。
- 主要失败分组：文档、文件、打印信息测试未提供当前新增的 `DesignQueryHelper`；工单测试未提供取消申请服务和订单文件 Mapper；`ArchiveParserUtilTest` 对当前不支持格式错误码的旧断言仍为 737，实际实现返回 762。

### design 当前处理记录

| 编号 | 类型 | 处理 | 验证 |
|---|---|---|---|
| DESIGN-TEST-001 | 测试过时 | 启动/完成设计测试调用补充当前 API 要求的版本参数 | 已解决；全模块回归通过 |
| DESIGN-TEST-002 | 测试装配过时 | 为文档、文件、打印信息测试补充 `DesignQueryHelper` mock，并准备当前权限/状态错误路径 | 已解决；全模块回归通过 |
| DESIGN-TEST-003 | 测试装配过时 | 为工单测试补充当前构造依赖、Lambda 元数据和流程结果 mock | 已解决；全模块回归通过 |
| DESIGN-BUG-001 | 明确业务 bug | `completeDesign` 缺少当前登录用户与分配设计师一致性校验；已有失败用例证明非分配设计师可继续执行 | 增加与 `startDesign` 一致的设计师校验 | 已解决；`DesignWorkorderServiceImplTest` 24/24 |
| DESIGN-BASE-001 | 测试契约过时 | 归档解析及权限断言使用旧错误码，当前实现/枚举分别为 762、765、766 | 测试改用 `ErrorCodeEnum` 当前契约，避免硬编码旧码 | 已解决；design 全模块回归通过 |

### design 阶段目标

装配恢复后，逐个覆盖工单权限/状态/启动完成、文件包解析与关联、文档两种类型的生成预览下载修订确认、打印信息保存与查询，并对新增回归用例执行全模块测试。

### design 阶段结果

2026-07-17：`mvn -q -pl yigongbao-module-design test` 通过，159 tests、0 failures、0 errors、0 skipped。归档测试优先读取外部样例，样例缺失时生成临时 ZIP/TAR/7z fixture，因此 ZIP、TAR、7z 和无过滤 ZIP 路径均在当前环境实际执行。

### design 第二轮补充

- `DesignSimpleServicesTest`：图纸/指令单版本查询、数据包序号、规格引用检查，共 6 tests。
- `DesignScreenshotServiceImplTest`：截图归属拒绝、空输入和删除边界，共 4 tests。
- `DesignSimpleServicesTest` 另覆盖产品文件空输入保护，共新增 2 tests。

## production 模块盘点（第一轮）

### 业务入口

当前源码包含生产流转卡、生产产品与编号、生产工序、生产包、质量检验/UDI、仓库、设备状态监听、设计完成监听和流转卡 Excel 导出等入口。现有测试共 13 个测试类，覆盖服务单测、监听器、Excel 构建器以及产品编号集成场景。

### 基线结果

- 首次 `mvn -pl yigongbao-module-production test`：121 tests，1 failure，21 errors；主要错误为集成测试 Spring 上下文缺 `FlowFacade`，以及 `FileStorageService`、`FlowOrderService` 等跨模块依赖未提供。
- `BatchUpdateUdiTest` 的成功用例按旧实现验证 `updateBatchById`，当前实现已改为逐条 `lambdaUpdate`，导致测试链返回 null；该项按当前实现更新测试装配。
- 产品编号单测存在生成编号重复断言失败，需在上下文恢复后区分测试数据隔离问题与编号生成业务问题。

### production 当前处理记录

| 编号 | 类型 | 处理 | 验证 |
|---|---|---|---|
| PRODUCTION-ENV-001 | 测试环境 | 生产集成测试配置补充 `FlowFacade`、`FlowOrderService`、`FileStorageService`、邮件发送器和历史记录服务 mock bean | 已解决；production 全量通过 |
| PRODUCTION-TEST-001 | 测试过时 | UDI 成功用例改按当前逐条 `lambdaUpdate` 链验证 | 已解决；production 全量通过 |
| PRODUCTION-TEST-002 | 测试环境/契约 | 编号集成测试补充 H2 测试 schema；临时编号改按当前雪花 ID 格式断言；并发测试改为稳定验证编号/计数不变量；无效设备编号按当前业务异常断言 | 已解决；12/12 集成用例通过 |

| PRODUCTION-ENV-002 | 测试环境 | `ProductNumberIntegrationTest` 上下文可启动，但 H2 缺少生产相关表 | 新增仅测试使用的 `schema.sql`，覆盖计数器、设备、流转卡和产品表 | 已解决 | `ProductNumberIntegrationTest` 12/12 通过 |

### production 阶段结果

2026-07-17：`mvn -q -pl yigongbao-module-production test` 通过，130 tests、0 failures、0 errors；其中包含 12 个产品编号集成测试。新增的正式编号唯一性校验由单测和集成测试共同覆盖。

### production 第二轮补充

- `DeviceUsageCounterServiceImplTest` 覆盖首次插入、乐观锁成功更新、连续冲突重试失败，共 3 tests。
- `ProductionRecordServiceImplTest` 另覆盖批号预览、批号提交、设备配置读取，共新增 3 tests。
- `ProductionRecordAssignDeviceTest` 覆盖设备分配的记录/设备拒绝、工序更新、计数器递增和正式编号生成，共 3 tests。

### 第二轮 order 补充

- `OrderModifyTimeWindowCheckerTest` 覆盖空时间、窗口内、过期和未来时间。
- `OrderDraftServiceImplPermissionTest` 覆盖草稿不存在、他人草稿、已提交草稿删除拒绝。
- `OrderMainServiceImplStateTransitionTest` 覆盖直接取消的乐观锁更新及事件发布，并锁定 ORDER-BUG-003。
- `OrderModifyApplyServiceImplBoundaryTest` 覆盖修改申请空参数、订单不存在、非管理员审核拒绝。
- `OrderModifyApplyServiceImplBoundaryTest` 另覆盖成功提交时差异计算、申请持久化、过期配置和事件发布。
- `OrderDraftFileServiceImplTest` 覆盖草稿文件关联替换与空列表清理。
- `OrderModifyFullServiceImplBoundaryTest` 覆盖订单不存在和设计师阶段权限拒绝。
- `OrderMainServiceImplStateTransitionTest` 另覆盖草稿转待审核正式订单及文件/明细复制空集路径。
- `OrderMainServiceImplStateTransitionTest` 另覆盖提交、撤回、审核通过、审核驳回必填、手动完成成功路径。
- `OrderMainServiceImplStateTransitionTest` 另覆盖直接创建时当前用户不存在拒绝、成功建单及 CREATE 流程记录。
- `OrderModifyApplyServiceImplBoundaryTest` 另覆盖审核驳回持久化和通知事件。
- 新增 `OrderControllerTest` 8 tests，覆盖创建参数校验、提交/撤回、审核驳回、重提/取消/手动完成版本传递及导出字段入口。
- 新增 `DesignWorkorderControllerTest` 3 tests，覆盖开始/完成设计版本参数校验与传递；补充设计模块测试启动配置。
- 新增 `ProductionRecordControllerTest` 4 tests，覆盖设备分配/批号提交参数校验及批号生成入口；使用独立 WebMvc 测试配置避免误加载生产 Mapper。
- 新增 `OrderModifyApplyControllerTest` 4 tests，覆盖审核结果校验、审核委托、v2 返回值及申请提交响应组装。
- 新增 `DesignPackageControllerTest` 4 tests、`DesignDocControllerTest` 5 tests，覆盖文件包和文档版本/确认/截图的关键路径与 multipart 参数。
- 新增 `ProductionProcessControllerTest` 3 tests、`ProductionQcControllerTest` 3 tests，覆盖工序设备参数、完成工序参数、质检失败原因和 UDI 请求校验。
- 新增 `ProductionPackControllerTest` 3 tests、`WarehouseControllerTest` 2 tests、`ProcessConfigControllerTest` 2 tests，覆盖包装设备、仓储出入库和工序配置 JSON/步骤入口。
- 新增 `DesignPrintInfoControllerTest` 3 tests、`DesignAttachmentControllerTest` 3 tests，覆盖打印信息清空语义、删除和附件关联参数。
- 新增 `FlowSelectControllerTest` 2 tests、`FlowDebugControllerTest` 2 tests；激活 debug 测试 profile 时发现并修复 `application-test.yml` 缩进错误。
- 新增 `DesignColumnConfigControllerTest` 2 tests、`ProductionColumnConfigControllerTest` 2 tests、`ProductionProductControllerTest` 1 test。
- 扩展 `ProductionRecordControllerTest` 4 tests，覆盖数据包下载、设备配置、打印机列表和取消预览；扩展 `DesignWorkorderControllerTest` 3 tests，覆盖工单列表、详情和分配历史。
- 扩展 `OrderCancelApplyControllerTest` 3 tests，覆盖待审核列表、我的申请列表和订单历史；扩展 `DesignDocControllerTest` 3 tests，覆盖指令单下载、修订上传和确认。

最终回归已覆盖三个核心模块的业务 Service、状态流转、权限边界、文件关联、归档解析和全部 Controller 入口；后续新增业务行为仍应沿用测试先行流程。

## 接口入口重审结果

2026-07-17 追加重审：以 Controller 映射为入口向下追踪 Service 调用，并对每个模块的 Service 接口和 `*ServiceImpl` 公共方法检索测试证据。结果为：order 46/46、design 34/34、production 33/33 的 Controller 映射均有请求级测试路径；三个模块的 Service 接口方法和 ServiceImpl 公共方法均能映射到测试类中的测试符号。

追加的重点分支包括：订单草稿未登录/不存在/新增保存、订单更新和删除状态边界、经典案例标记回滚、自定义导出；设计图纸版本查询映射；生产流转卡 Excel 缓存/缺失记录、Flow 同步成功/空结果/拒绝；以及所有新增 Controller 委托、参数校验和响应组装路径。

最终回归命令及报告：

- `mvn -q -pl yigongbao-module-order test`：206 tests，0 failures，0 errors，0 skipped
- `mvn -q -pl yigongbao-module-design test`：169 tests，0 failures，0 errors，0 skipped
- `mvn -q -pl yigongbao-module-production test`：171 tests，0 failures，0 errors，0 skipped

覆盖矩阵详见 [`interface-service-test-matrix.md`](interface-service-test-matrix.md)。

## 核心实现映射审计（最终轮）

对三个模块的 `*ServiceImpl`、`*Helper` 和 Controller 源文件逐一按类名检索测试证据后，order、production 的自定义实现均能映射到至少一个测试类；design 仅有以下两个实现没有独立测试类：

- `DesignModelServiceImpl`
- `DesignPackageFileServiceImpl`

两者均仅继承 MyBatis-Plus `ServiceImpl`，没有新增方法、权限判断、状态流转或数据转换逻辑；其业务入口由 `DesignFileServiceImpl` 负责并已覆盖模型/数据包文件关联、查询和删除。因此不为无自定义行为的继承 CRUD 制造重复测试，作为低风险继承框架能力记录。

最终闭合证据：三个模块所有核心自定义实现均已映射到测试类；所有 Controller 均有请求级测试；归档格式测试已使用外部样例或临时 fixture 实际执行；最新 Surefire 报告无 failures、errors 或 skipped。
