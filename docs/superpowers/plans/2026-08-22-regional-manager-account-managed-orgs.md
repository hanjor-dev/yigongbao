# 区域管理员账户级多机构权限实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将区域管理员从角色级部门范围改为账户级“主所属机构 + 可选额外管理机构”并集范围，并交付可审计的线上数据库迁移脚本。

**Architecture:** `sys_user.org_id` 继续作为必填主机构，新表 `sys_user_managed_org` 仅保存额外管理机构。系统服务统一计算有效机构集合，订单、医院选项和区域管理员看板均复用该集合；订单数据直接按 `order_main.org_id` 过滤。现有区域管理员不自动回填额外机构，迁移后默认只管理主机构。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、JUnit 5/Mockito、MySQL 8

---

### Task 1: 权限模型与数据库结构

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/DataScopeTypeEnum.java`
- Create: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/entity/UserManagedOrgEntity.java`
- Create: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/mapper/UserManagedOrgMapper.java`
- Create: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/UserManagedOrgService.java`
- Create: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserManagedOrgServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserManagedOrgServiceImplTest.java`

- [ ] 写失败测试：正常的主机构进入有效集合、额外集合可空、重复主机构被去除；主机构停用、删除或类型非法时有效集合为空并拒绝访问，直到管理员修复。
- [ ] 运行定向测试并确认因实现缺失而失败。
- [ ] 实现 `USER_ORGS` 范围和有效机构集合服务。
- [ ] 运行定向测试并确认通过。

### Task 2: 账户创建、修改和详情回显

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/CreateUserDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/UpdateUserDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/UserVO.java`
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserServiceImplTest.java`

- [ ] 写失败测试：区域管理员可不传额外机构，且只能配置正常的经销商/服务商。
- [ ] 写失败测试：非区域管理员提交管理机构明确拒绝；角色切换时旧关系被清理。
- [ ] 写失败测试：更新时 `managedOrgIds=null` 保持原关系，`managedOrgIds=[]` 清空额外关系但保留主机构权限。
- [ ] 实现 DTO、校验、事务覆盖保存和详情回显。
- [x] 复用账户修改接口操作日志记录提交的授权集合；本期不新增前后快照表。机构被停用、删除或改为非经销商/服务商时，由实时查询自动排除，不缓存有效机构集合。
- [ ] 运行系统模块测试。

### Task 3: 订单与医院权限链路

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserHospitalServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/helper/OrderQueryHelper.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/validator/OrderDataValidator.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderMainServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderDraftServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderExportServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderCancelApplyServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyApplyServiceImpl.java`
- Inspect: `yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/service/impl/ImagingServiceImpl.java`
- Inspect: `yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/v1/service/impl/ViewerServiceImpl.java`
- Inspect: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignDocServiceImpl.java`
- Inspect: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/controller/FileController.java`
- Test: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserHospitalServiceImplTest.java`
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/helper/OrderQueryHelperTest.java`
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderMainServiceImplTest.java`
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderDraftServiceImplTest.java`
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderExportServiceImplTest.java`

- [ ] 写失败测试：订单范围按有效机构 ID 并集过滤。
- [ ] 写失败测试：区域管理员可选医院为全部有效机构关联医院并集。
- [ ] 写失败测试：医院提交校验仅允许该并集内医院。
- [ ] 写失败测试：创建订单的 `org_id` 必须等于主机构，不能使用额外管理机构提单。
- [ ] 审计详情、修改、动作、导出、取消申请、修改申请和草稿提交等全部按订单 ID 操作入口，统一调用有效机构鉴权；医院在创建、修改和草稿转正式时重新校验。
- [ ] 核查影像、查看器、设计文档下载和通用文件下载入口；能解析 orderId 的入口复用订单有效机构鉴权。通用文件 ID 下载无法可靠反查订单，作为既有跨模块安全改造项明确记录，不伪称由本需求闭环。
- [ ] 实现统一过滤并运行：`mvn -pl yigongbao-module-order -am "-Dtest=OrderQueryHelperTest,OrderMainServiceImpl*Test,OrderDraftServiceImplTest,OrderExportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，预期 0 failure。

### Task 4: 区域管理员看板

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-dashboard/src/main/java/com/yigongbao/module/dashboard/service/strategy/RegionalManagerDashboardStrategy.java`
- Test: `yigongbao-parent/yigongbao-module-dashboard/src/test/java/com/yigongbao/module/dashboard/service/strategy/RegionalManagerDashboardStrategyTest.java`

- [ ] 写失败测试：看板按订单 `org_id` 的有效机构并集统计，不再按部门人员集合。
- [ ] 实现看板条件和机构内业务员统计。
- [ ] 运行看板模块测试。

### Task 5: 数据库基线与线上迁移

**Files:**
- Modify: `sql/ddl.sql`
- Modify: `sql/ddl-prod.sql`
- Modify: `sql/init.sql`
- Modify: `yigongbao-parent/yigongbao-module-system/src/test/resources/schema.sql`
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/resources/schema.sql`
- Modify: `yigongbao-parent/yigongbao-module-dashboard/src/test/resources/schema.sql`
- Create: `sql/migration-regional-manager-managed-orgs-2026-08-22.sql`
- Modify: `docs/superpowers/specs/2026-08-22-regional-manager-multi-org-scope-design.md`

- [ ] 在基线 DDL 增加额外管理机构关系表及 `user_orgs` 注释。
- [ ] 将区域管理员初始化权限从 `dept` 改为 `user_orgs`。
- [ ] 编写幂等迁移：建表、更新角色、不回填关系、迁移前后核验、回退说明。
- [ ] 明确迁移零回填：关系表初始记录数为 0，所有现有区域管理员默认仅主机构，并输出待人工配置账户清单。
- [ ] 逐项修订规格：`effectiveOrgIds = {sys_user.org_id} ∪ extraManagedOrgIds`；额外集合可空、不保存主机构、无 `is_primary`；响应同时返回 `managedOrgIds` 与 `effectiveOrgIds`。最终采用实时数据库查询、不缓存集合；非法或失效的额外机构实时排除，账户保存时阻止写入非法机构。

### Task 6: 前端接入合同与通知影响

**Files:**
- Modify: `docs/superpowers/specs/2026-08-22-regional-manager-multi-org-scope-design.md`
- Inspect: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/listener/NotificationEventListener.java`

- [ ] 前端源码不在本仓库，明确为外部实施依赖；不修改不可维护的 `frontend/dist`。交付账户表单 API 合同和验收清单：区域管理员显示必填主机构和可选额外机构多选；候选仅正常经销商/服务商；主机构锁定且不进入额外集合；角色切换清空额外选择。后端可独立上线，但人工配置入口需前端仓库完成后才闭环。
- [ ] 已确认现有订单提交通知只发设计管理员，没有区域管理员通知；本次不新增通知。规格记录未来若新增，必须按订单 `org_id` 反查覆盖账户。
- [ ] 授权审计复用现有账户修改接口操作日志，完整请求包含角色、主机构、`managedOrgIds`、操作人和备注；本期不新增影响数量快照表，并在规格中明确该边界。

### Task 7: 全量验证

- [ ] 运行 common/system/order/dashboard 定向与回归测试。
- [ ] 运行 Maven 编译/打包检查。
- [ ] 检查 SQL 中表、索引、角色更新和零回填约束。
- [ ] 检查 `git diff`，确认未覆盖用户无关改动。
