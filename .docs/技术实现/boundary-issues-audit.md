# 删改状态操作边界问题审计报告

> 审计日期：2026-04-28  
> 审计范围：yigongbao-parent 全部模块  
> 审计方法：逐模块分析 ServiceImpl 中 remove/delete/updateStatus/update 方法的关联数据处理完整性

---

## 优先级说明

| 级别 | 含义 |
|------|------|
| P0 | 数据一致性破坏，已有数据立即出错 |
| P1 | 安全/权限漏洞，或数据脏引用 |
| P2 | 存储泄漏或潜在一致性风险 |
| P3 | 设计缺陷，当前无直接影响 |

---

## 一、module-system

### 1.1 OrgServiceImpl（机构）

#### [P0] 删除机构未清理 sys_dept_org 关联
- **触发操作**：`removeOrg(id)`
- **遗漏处理**：已清理 sys_org_hospital、sys_user_hospital、hospital_group_template_detail，但未清理 `sys_dept_org` 中 `org_id = id` 的记录，导致部门仍持有对已删除机构的引用，部门详情查询出现幽灵机构名称。
- **建议**：
```java
deptOrgMapper.delete(new LambdaQueryWrapper<DeptOrgEntity>()
    .eq(DeptOrgEntity::getOrgId, id));
```

#### [P0] 更新机构名称未同步 sys_user 冗余字段
- **触发操作**：`updateOrg(id, dto)` 修改 orgName
- **遗漏处理**：`sys_user.org_name` 是冗余字段，机构名称变更后未同步更新，用户列表持续显示旧机构名称。
- **建议**：检测到 orgName 变更时，批量更新 `sys_user.org_name WHERE org_id = id AND is_deleted = 0`。

#### [P1] 禁用机构时未联动禁用关联用户
- **触发操作**：`updateStatus(id, 0)`
- **遗漏处理**：仅更新 sys_org.status，该机构下的用户仍可正常登录和操作。
- **建议**：禁用机构时同步禁用该机构下所有用户，或在登录鉴权时联查机构状态。

---

### 1.2 DeptServiceImpl（部门）

#### [P0] 更新部门名称未同步 sys_user 冗余字段
- **触发操作**：`updateDept(id, dto)` 修改 deptName
- **遗漏处理**：`sys_user.dept_name` 是冗余字段，部门名称变更后未同步更新。
- **建议**：检测到 deptName 变更时，批量更新 `sys_user.dept_name WHERE dept_id = id AND is_deleted = 0`。

#### [P1] 禁用部门时未联动禁用关联用户
- **触发操作**：`updateStatus(id, 0)`
- **遗漏处理**：仅更新 sys_dept.status，该部门下的用户仍可正常使用。
- **建议**：与机构禁用同理，禁用部门时应同步禁用或提示该部门下的用户。

---

### 1.3 UserServiceImpl（用户）

#### [P1] 删除用户未清空 sys_dept.leader_user_id
- **触发操作**：`removeUser(id)`
- **遗漏处理**：已清理 sys_user_hospital，但未检查并清空 `sys_dept.leader_user_id = id` 的记录，删除后部门负责人字段成为悬空引用。
- **建议**：
```java
// 清空以该用户为负责人的部门记录
deptMapper.clearLeaderByUserId(id);
// UPDATE sys_dept SET leader_user_id = NULL WHERE leader_user_id = ? AND is_deleted = 0
```

#### [P2] 变更用户 orgId/deptId 时未清理旧医院权限
- **触发操作**：`updateUser(id, dto)` 变更 orgId 或 deptId
- **遗漏处理**：用户调转机构/部门时，若前端未同时传入新 hospitalIds，sys_user_hospital 中旧的医院权限将残留，用户持有跨机构医院权限。
- **建议**：当 orgId 或 deptId 发生变更时，强制清空 sys_user_hospital，要求前端重新分配医院权限。

#### [P1] 禁用用户时无会话失效处理
- **触发操作**：`updateStatus(id, 0)`
- **遗漏处理**：仅更新数据库状态，被禁用的用户在 token 过期前仍可继续操作。
- **建议**：禁用时通知认证模块使其 token 立即失效（Redis 黑名单或 token 版本号递增）。

---

### 1.4 RoleServiceImpl（角色）

#### [P0] 更新角色名称未同步 sys_user 冗余字段
- **触发操作**：`updateRole(id, dto)` 修改 roleName
- **遗漏处理**：`sys_user.role_name` 是冗余字段，角色名称变更后未同步更新。
- **建议**：检测到 roleName 变更时，批量更新 `sys_user.role_name WHERE role_id = id AND is_deleted = 0`。

#### [P1] 禁用角色时未联动禁用关联用户
- **触发操作**：`updateStatus(id, 0)`
- **遗漏处理**：仅更新 sys_role.status，持有该角色的用户仍可正常登录，权限校验未联查角色状态。
- **建议**：禁用角色时同步禁用该角色下所有用户，或在权限校验链路中增加角色状态检查。

#### [P3] 删除角色时操作顺序建议调整
- **触发操作**：`removeRole(id)`
- **问题**：先 `removeById(id)` 再 `roleResourceMapper.deleteByRoleId(id)`，建议调换为先清子后删父，符合惯例。

---

### 1.5 DictServiceImpl（字典）

#### [P1] 删除字典叶子节点时未检查业务表引用
- **触发操作**：`remove(id)`
- **遗漏处理**：仅检查是否有子节点，未检查 sys_org.org_type、sys_org.hospital_level、sys_org.hospital_type、sys_user.specialty 等字段是否引用该字典编码。删除后这些字段值成为无效编码。
- **建议**：删除前检查关键业务表是否有引用，或改为仅允许禁用而不允许删除叶子节点。

#### [P2] 禁用字典节点后下拉接口仍返回已禁用项
- **触发操作**：`updateStatus(id, 0)`
- **遗漏处理**：`listOptions`、`listByTypeCode` 等面向前端的查询接口未过滤 status，禁用的字典项仍会出现在选项中。
- **建议**：上述接口增加 `.eq(DictEntity::getStatus, StatusConstants.NORMAL)` 过滤条件。

---

### 1.6 ConfigServiceImpl（配置）

#### [P2] 删除非系统内置配置时无业务引用检查
- **触发操作**：`deleteConfig(id)`
- **遗漏处理**：业务代码通过 `configService.getConfigValue(key)` 硬编码引用配置键（如 DEFAULT_PASSWORD、MANUFACTURER_ORG_ID），删除后运行时返回 null 无任何提示。
- **建议**：将关键业务配置统一标记为 isSystem=1，或在 deleteConfig 中检查 SystemConfigKeyEnum 禁止删除枚举中定义的 key。

#### [P3] 系统内置配置完全不可修改，运维不友好
- **触发操作**：`updateConfig(id, dto)`
- **问题**：isSystem=1 的配置连 configValue 也不可改，只能直接操作数据库。
- **建议**：区分"配置键不可改"和"配置值可改"，允许修改 configValue，禁止修改 configKey 和 isSystem。

---

### 1.7 ResourceServiceImpl（资源）

#### [P3] 资源缺少 updateStatus 接口，无法临时禁用
- **触发操作**：需要临时下线某菜单/按钮
- **遗漏处理**：只能删除，但删除会被角色关联阻止，无法临时禁用。
- **建议**：补充 `updateStatus` 接口，禁用后 getResourceTree/getUserMenuTree 已过滤 status=NORMAL，自然不会出现在菜单树中。

#### [P2] 更新资源编码时已登录用户权限缓存不失效
- **触发操作**：`updateResource(id, dto)` 修改 resourceCode
- **遗漏处理**：已登录用户的权限列表在 token 刷新前不会更新，可能导致权限判断失效。
- **建议**：修改 resourceCode 时通知在线用户刷新权限缓存，或限制已分配给角色的资源不允许修改 resourceCode。

---

## 二、module-basic

### 2.1 BodyPart（重建部位）

#### [P1] 删除部位时未检查关联的重建项目
- **触发操作**：`removeBodyPart(id)`
- **遗漏处理**：`rebuild_project` 通过 `body_part_id` 引用部位，删除前未检查，导致孤儿项目数据。
- **建议**：删除前查询 rebuild_project 表，若存在引用则抛出异常（如 BODY_PART_HAS_PROJECTS）。

#### [P2] 禁用部位时未级联禁用关联项目
- **触发操作**：`updateStatus(id, DISABLED)`
- **遗漏处理**：部位禁用后其下重建项目仍为启用状态，但在树形接口中不可见，可能导致数据不一致。
- **建议**：禁用部位时同步将该部位下所有启用的重建项目也置为 DISABLED。

---

### 2.2 RebuildProject（重建项目）

#### [P1] 删除项目时未检查订单明细引用
- **触发操作**：`removeProject(id)`
- **遗漏处理**：只检查了子项目，未检查 order_item / order_item_draft 中是否存在 `project_id = id` 的记录，删除后订单明细出现悬空引用。
- **建议**：删除前查询 order_item（is_deleted=0）和 order_item_draft（is_deleted=0）中是否存在引用，有则拒绝删除。

#### [P2] 禁用项目时未提示进行中订单
- **触发操作**：`updateStatus(id, DISABLED)`
- **遗漏处理**：已存在的草稿明细或进行中订单明细仍引用该项目，没有任何提示或拦截。
- **建议**：禁用时检查是否有草稿/进行中订单引用该项目，若有则给出警告提示。

#### [P0] 本地缓存在数据变更后不失效
- **触发操作**：`createProject` / `updateProject` / `removeProject` / `updateStatus`
- **遗漏处理**：`bodyPartNameCache` 和 `projectNameCache` 是 ConcurrentHashMap 实例变量，永不失效，任何写操作后缓存中的旧数据不会被清除，导致接口返回过期数据。
- **建议**：所有写操作完成后调用 `bodyPartNameCache.clear()` 和 `projectNameCache.clear()`，或改用 Spring Cache + @CacheEvict。

#### [P3] 更新项目名称时冗余字段处理需明确语义
- **触发操作**：`updateProject(id, dto)`
- **遗漏处理**：order_item / order_item_draft 中存储了冗余字段 project_name，更新后不会同步。
- **建议**：明确业务语义——若历史快照（订单创建时的名称）是预期行为则文档说明；若需同步则批量更新。

---

### 2.3 RegistrationCert（注册证）

#### [P1] 删除注册证时未检查规格引用
- **触发操作**：`remove(id)`
- **遗漏处理**：`product_spec` 通过 `cert_id` 引用注册证，删除后规格中的 cert_id 成为悬空引用。
- **建议**：删除前查询 product_spec 中 `cert_id = id` 且未软删除的记录，若存在则拒绝删除。

#### [P0] 更新注册证号时未同步规格冗余字段
- **触发操作**：`update(id, dto)` 修改 certCode
- **遗漏处理**：`product_spec.cert_no` 是从注册证 cert_code 冗余过来的字段，更新后不会同步。
- **建议**：若 certCode 发生变化，批量更新 `product_spec.cert_no WHERE cert_id = id`。

#### [P1] 注册证过期/禁用时未级联禁用关联规格
- **触发操作**：`refreshExpiredStatus`（定时任务）或手动禁用
- **遗漏处理**：注册证过期后，关联规格仍为启用状态，可在新建订单时被选用，存在合规风险。
- **建议**：注册证状态变为 DISABLED 时，同步将关联的 product_spec 也置为 DISABLED。

---

### 2.4 Product / ProductSpec（产品/规格）

#### [P2] 产品和规格缺少独立的 updateStatus 方法
- **触发操作**：产品/规格状态变更
- **遗漏处理**：状态变更混在 update 中，没有禁用时的关联检查（如禁用产品时未级联禁用规格）。
- **建议**：补充独立的 `updateStatus` 方法，禁用产品时同步禁用其下所有启用的规格。

---

## 三、module-order

### 3.1 OrderMainServiceImpl（订单主表）

#### [P2] 删除订单时未清理流程历史记录
- **触发操作**：`removeOrder(id)`
- **遗漏处理**：已清理 order_item 和 order_file，但未清理 FlowFacade 写入的流程历史记录（flow_history / flow_log 等表）。
- **建议**：在 removeOrder 中调用 flowFacade 提供的清理接口，或通过事件监听自动清理。

#### [P2] 删除订单时未清理修改申请记录
- **触发操作**：`removeOrder(id)`
- **遗漏处理**：order_modify_apply 和 order_modification_log 中可能存在关联记录，删除时未清理。
- **建议**：删除时顺带清理 `order_modify_apply WHERE order_id = id` 和 `order_modification_log WHERE order_id = id`。

---

### 3.2 OrderModifyApplyServiceImpl（修改申请）

#### [P1] 执行明细修改时未校验最小数量
- **触发操作**：`executeModification` → `processItemModification`，newItems 为空列表
- **遗漏处理**：当 newItems 为空时会删除订单所有明细，导致订单明细数为 0，与提交时"至少1条明细"的校验矛盾。
- **建议**：在 processItemModification 执行删除后，校验剩余明细数量 >= 1，否则抛出异常。

---

## 四、module-design

### 4.1 DesignFileServiceImpl（数据包/文件）

#### [P2] 删除数据包时未清理截图记录和 OSS 文件
- **触发操作**：`deletePackage(orderId, packageId)`
- **遗漏处理**：删除了 design_package_file 记录，但 `design_package_file_screenshot` 表中以 package_file_id 为外键的截图记录和对应 OSS 文件均未清理，形成孤儿数据。
- **建议**：删除 design_package_file 之前，先查出所有 package_file_id，调用 screenshotService 删除截图记录并清理截图 OSS 文件。

#### [P2] deleteModel 未解除 linkFile 业务关联
- **触发操作**：`deleteModel(orderId, modelId)`
- **遗漏处理**：linkModels 时调用了 `fileService.linkFile` 建立业务关联，但 deleteModel 只调用 `fileService.deleteById`，若 FileService 内部维护了 biz_type/biz_id 关联表，则该关联记录会残留。
- **建议**：确认 `fileService.deleteById` 是否已内部级联清理业务关联记录；若未级联，需在删除前调用 unlinkFile。

---

### 4.2 DesignDocServiceImpl（指令单/图纸）

#### [P2] 覆盖版本时旧 OSS 文件未删除
- **触发操作**：打印信息变化后触发覆盖，`doGenerateInstruction` / `doGenerateDrawing` 中 `toOverride != null` 分支
- **遗漏处理**：覆盖现有版本时直接用新 fileVO.getId() 覆写 templateFileId，旧的 templateFileId 对应的 OSS 文件没有调用 `fileService.deleteById` 清理，每次打印信息变化都会产生一个孤儿 OSS 文件。
- **建议**：覆盖前保存旧的 templateFileId，覆盖完成后调用 `fileService.deleteById(oldTemplateFileId)` 清理旧文件。

#### [P2] 上传修订版文件时旧修订版 OSS 文件未删除
- **触发操作**：`uploadRevisedInstruction` / `uploadRevisedDrawing`
- **遗漏处理**：重复上传修订版时直接覆写 revisedFileId，旧的 revisedFileId 对应 OSS 文件未清理。
- **建议**：上传前检查 `entity.getRevisedFileId() != null`，若存在则先调用 `fileService.deleteById` 删除旧修订版文件。

---

### 4.3 DesignScreenshotServiceImpl（截图）

#### [P2] 更新截图时旧截图 OSS 文件未删除
- **触发操作**：`saveScreenshot`（upsert 的更新分支，existing != null）
- **遗漏处理**：更新截图时直接覆写 existing.setFileId，旧的 fileId 对应的 OSS 截图文件未调用 `fileService.deleteById` 清理。
- **建议**：在 updateById(existing) 之前，先保存 oldFileId = existing.getFileId()，更新后调用 `fileService.deleteById(oldFileId)` 清理旧截图。

---

## 五、module-flow / module-imaging

**无明显问题。**
- flow 模块职责单一，只做状态流转计算和历史记录，不涉及实体删除/更新的关联清理。
- imaging 模块为纯只读聚合查询，不包含任何删除、更新或状态变更操作。

---

## 六、汇总优先级矩阵

| 优先级 | 模块 | 问题描述 |
|--------|------|---------|
| P0 | module-system / OrgServiceImpl | 删除机构未清理 sys_dept_org |
| P0 | module-system / OrgServiceImpl | 更新机构名称未同步 sys_user.org_name |
| P0 | module-system / DeptServiceImpl | 更新部门名称未同步 sys_user.dept_name |
| P0 | module-system / RoleServiceImpl | 更新角色名称未同步 sys_user.role_name |
| P0 | module-basic / RebuildProject | 本地缓存写操作后不失效 |
| P0 | module-basic / RegistrationCert | 更新注册证号未同步 product_spec.cert_no |
| P1 | module-system / UserServiceImpl | 删除用户未清空 sys_dept.leader_user_id |
| P1 | module-system / UserServiceImpl | 禁用用户时无会话失效处理 |
| P1 | module-system / RoleServiceImpl | 禁用角色未联动禁用关联用户 |
| P1 | module-system / OrgServiceImpl | 禁用机构未联动禁用关联用户 |
| P1 | module-system / DeptServiceImpl | 禁用部门未联动禁用关联用户 |
| P1 | module-system / DictServiceImpl | 删除字典未检查业务表引用 |
| P1 | module-basic / BodyPart | 删除部位未检查关联重建项目 |
| P1 | module-basic / RebuildProject | 删除项目未检查订单明细引用 |
| P1 | module-basic / RegistrationCert | 注册证过期/禁用未级联禁用规格 |
| P1 | module-order / OrderModifyApply | 执行明细修改未校验最小数量 |
| P2 | module-system / UserServiceImpl | 变更 orgId/deptId 未清理旧医院权限 |
| P2 | module-system / DictServiceImpl | 禁用字典后下拉接口仍返回已禁用项 |
| P2 | module-system / ConfigServiceImpl | 删除非系统配置无业务引用检查 |
| P2 | module-system / ResourceServiceImpl | 更新资源编码时权限缓存不失效 |
| P2 | module-basic / BodyPart | 禁用部位未级联禁用关联项目 |
| P2 | module-basic / RebuildProject | 禁用项目未提示进行中订单 |
| P2 | module-basic / Product | 禁用产品未级联禁用规格 |
| P2 | module-order / OrderMain | 删除订单未清理流程历史和修改申请记录 |
| P2 | module-design / DesignFileServiceImpl | 删除数据包未清理截图记录和 OSS 文件 |
| P2 | module-design / DesignDocServiceImpl | 覆盖/上传修订版时旧 OSS 文件未删除 |
| P2 | module-design / DesignScreenshotServiceImpl | 更新截图时旧 OSS 文件未删除 |
| P3 | module-system / RoleServiceImpl | 删除角色操作顺序建议调整 |
| P3 | module-system / ConfigServiceImpl | 系统内置配置完全不可修改，运维不友好 |
| P3 | module-system / ResourceServiceImpl | 资源缺少 updateStatus 接口 |
| P3 | module-basic / RebuildProject | 项目名称冗余字段更新语义需明确 |

---

## 七、修复建议优先顺序

**第一批（P0，立即修复）**：
1. OrgServiceImpl.removeOrg — 补充清理 sys_dept_org
2. OrgServiceImpl.updateOrg — 检测 orgName 变更时同步 sys_user.org_name
3. DeptServiceImpl.updateDept — 检测 deptName 变更时同步 sys_user.dept_name
4. RoleServiceImpl.updateRole — 检测 roleName 变更时同步 sys_user.role_name
5. RebuildProjectServiceImpl — 所有写操作后清空本地缓存
6. RegistrationCertServiceImpl.update — certCode 变更时同步 product_spec.cert_no

**第二批（P1，近期修复）**：
7. UserServiceImpl.removeUser — 清空 sys_dept.leader_user_id
8. UserServiceImpl.updateStatus — 禁用时使 token 失效
9. DictServiceImpl.remove — 删除前检查业务表引用
10. BodyPartServiceImpl.removeBodyPart — 删除前检查 rebuild_project 引用
11. RebuildProjectServiceImpl.removeProject — 删除前检查 order_item 引用
12. RegistrationCertServiceImpl — 过期/禁用时级联禁用 product_spec
13. OrderModifyApplyServiceImpl — 执行明细修改后校验最小数量

**第三批（P2，计划修复）**：
14. DictServiceImpl — 下拉接口过滤已禁用字典项
15. DesignDocServiceImpl — 覆盖/修订版时清理旧 OSS 文件
16. DesignScreenshotServiceImpl — 更新截图时清理旧 OSS 文件
17. DesignFileServiceImpl — 删包时清理截图记录和 OSS 文件
18. OrderMainServiceImpl — 删除订单时清理流程历史和修改申请

---

*文档生成时间：2026-04-28*
