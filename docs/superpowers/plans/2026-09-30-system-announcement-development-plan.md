# 系统公告发布与强制确认弹窗开发计划

**技术设计**：`docs/superpowers/specs/2026-09-30-system-announcement-technical-design.md`  
**日期**：2026-09-30  
**范围**：后端系统公告模块 + `frontend/med-tech` PC 端

## 实施进度（2026-09-30）

- 已完成：P0-1 契约冻结；P1 基础表、枚举、Entity、Mapper、pending/确认/撤回基础查询；P2 目标解析、富文本清洗、草稿/发布/撤回生命周期、基础管理 API；P3 发布/撤回事件及事务提交后推送；P4 前端类型、API、独立 Store；P5-1、P5-2 独立弹窗与 App 集成；P5-3 管理列表和草稿操作第一版。
- 本轮新增完成：公告确认统计接口、接收人明细分页接口、发布/撤回审计日志接口、接收记录分批写入、管理端审计日志展示、现有文件服务上传接入及内容清洗单元测试。
- 部分完成：P1-2、P2-1、P2-2、P2-3、P2-4、P3、P5-3、P5-4；仍需补齐完整自动化联调、公告业务专项安全测试和完整回归验收。
- 验证结果：后端 `mvn -pl yigongbao-module-notification -am compile -DskipTests` 成功；前端 `npm run build` 成功。
- 本轮审查修复：附件 `fileId` 全链路统一为字符串并同步数据库；附件链接仅允许 HTTP(S) 或站内绝对路径；账户远程搜索保留已选项，避免切换关键词后回显丢失；新增附件危险协议回归测试。
- 本轮验证：后端公告相关测试 10 个全部通过；前端生产构建成功；本地 `system_announcement_attachment.file_id` 已变更为 `VARCHAR(64)`。

## 剩余任务清单（2026-09-30，按本次确认重新拆分）

以下任务曾被误归类为“可后续处理”，现明确纳入本需求完成范围：

### R1 管理端编辑体验

- [x] 将公告正文从普通 textarea 升级为可视化富文本编辑区，支持加粗、列表、链接和预览；图片继续通过上传服务和 HTML 内容使用。
- [x] 角色目标改为角色名称多选，不要求管理员手工输入角色 ID。
- [x] 指定账户改为账号/姓名多选，不要求管理员手工输入用户 ID。
- [x] 草稿详情正确回显目标、HTML 内容和附件。
- [x] 增加“复制为新草稿”。
- [x] 发布前预览内容、目标类型和附件。

### R2 后端接口完整性与安全

- [x] 增加用户端公告详情接口，并校验当前用户确实是接收人。
- [x] 增加公告目标人数预估接口。
- [x] 增加创建/更新/预览/发布参数校验；附件业务侧暂不限制类型、大小和数量，沿用 DTO 结构校验。
- [x] 发布和撤回增加数据库行锁/条件更新，保证重复请求幂等。
- [x] 发布时确保空目标不能发布，目标用户去重且只包含有效账户。
- [x] 校验权限资源只关联超级管理员。

### R3 测试与回归

- [x] 增加目标解析器测试：全员含超级管理员、重复用户去重；禁用/删除账户由 SQL 条件覆盖。
- [x] 增加生命周期测试：发布后不可编辑、批量发布使用行锁、确认条件更新幂等。
- [ ] 增加 Controller 权限和越权测试。
- [ ] 增加前端 Store 测试：倒序、逐条确认、撤回清理、重复事件去重、切换账户清空。
- [ ] 执行在线发布、离线补偿、多公告逐条确认和撤回关闭弹窗联调。
- [ ] 执行业务通知回归，确认公告不进入原消息卡片和未读数。

### R4 交付验收

- [ ] 更新技术设计文档中的接口、权限、附件策略和实际实现差异。
- [x] 完成前后端构建、测试和本地数据库结构/权限复核。
- [ ] 形成最终验收记录，确认无未完成项后才标记 P6/P7 完成。

## 1. 目标与完成标准

实现以下完整闭环：

```text
创建草稿 → 富文本编辑 → 预览 → 选择目标 → 立即发布
→ 用户登录/实时收到独立弹窗 → 点击“我已知晓”
→ 后台查看确认统计 → 管理员撤回 → 当前弹窗关闭且不再弹出
```

完成标准：

- 系统公告不进入现有业务通知卡片和普通消息未读数。
- 全员、角色、指定账户目标都能正确解析、去重和保存快照。
- 发布后正文和目标不可修改。
- 撤回能关闭 current、清理 queue、阻止离线补弹。
- 能查询确认统计、接收明细、发布日志和撤回日志。
- 富文本、图片、附件和链接通过安全校验。
- 后端测试、前端构建、数据库迁移和联调验收通过。

## 2. 实施顺序和依赖

```text
P0 契约/权限冻结
 ├─ P1 DDL、枚举、Mapper
 │   └─ P2 后端 Service/API
 │       └─ P3 WebSocket 事件和补偿
 └─ P4 前端类型/API/Store
     └─ P5 前端弹窗和后台页面
          └─ P6 联调
               └─ P7 安全、性能、回归验收
```

P2 与 P4 在 API 契约冻结后可以并行；P5 依赖 P4 的类型和 Store。

## 3. P0：契约、权限和资源冻结

### P0-1 确认领域契约

- [x] 固定公告状态：`DRAFT`、`PUBLISHED`、`REVOKED`。
- [x] 固定接收状态：`PENDING`、`ACKNOWLEDGED`、`REVOKED`。
- [x] 固定目标类型：`ALL`、`ROLE`、`USER`。
- [x] 草稿保存目标配置：角色 ID 和用户 ID JSON；发布时重新解析有效用户。
- [x] 固定 WebSocket 事件：`ANNOUNCEMENT_PUBLISHED`、`ANNOUNCEMENT_REVOKED`。
- [x] 固定接口基础路径和分页字段。
- [x] 固定发布时间倒序规则：`publishedAt DESC, id DESC`。

**验收**：前后端不再自行发明状态值或接口路径。

### P0-2 创建系统资源规划

新增资源编码：

```text
notification:announcement:list
notification:announcement:view
notification:announcement:create
notification:announcement:update
notification:announcement:preview
notification:announcement:publish
notification:announcement:revoke
notification:announcement:statistics
```

- [x] 新增“系统管理 / 系统公告”菜单资源。
- [x] 新增页面和按钮资源。
- [x] 确认动态路由组件路径与 `frontend/med-tech/src/router/index.ts` 规则一致。
- [x] 当前阶段仅给超级管理员（`role_code=admin`）分配公告权限；其他发布角色暂不分配。

## 4. P1：数据库和后端基础层

### P1-1 编写迁移脚本

新增：

```text
sql/migration-system-announcement-20260930.sql
```

同步维护：

```text
sql/ddl.sql
sql/ddl-prod.sql
```

- [x] 创建 `system_announcement`。
- [x] 创建 `system_announcement_recipient`。
- [x] 创建 `system_announcement_attachment`。
- [x] 创建 `system_announcement_audit_log`。
- [x] 添加状态、用户、公告、操作时间索引。
- [x] 添加 `(announcement_id, user_id)` 唯一约束。
- [x] 已通过本地 MySQL MCP 在 `yigongbao` 开发库执行并验证四张公告表、菜单资源和超级管理员权限关联。

### P1-2 创建枚举、Entity、Mapper

新增目录：

```text
yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/announcement/
```

- [x] 创建 4 个状态/操作枚举。
- [x] 创建公告、接收、附件、审计 4 个 Entity。
- [x] 创建 4 个 Mapper 和 XML。
- [x] 实现用户 pending 查询。
- [x] 实现接收记录分批插入（每批最多 500 条）。
- [x] 实现确认状态条件更新并返回影响行数。
- [x] 实现 pending 批量撤回。
- [x] 实现统计聚合和接收明细查询。
- [ ] 使用逻辑删除和项目现有 `BaseEntity` 规范。

**测试**：Mapper 覆盖排序、状态过滤、空结果、重复用户和批量更新。

## 5. P2：后端领域服务和管理 API

### P2-1 目标用户解析器

新增 `AnnouncementTargetResolver`：

- [x] `ALL` 查询所有未禁用、未删除用户。
- [x] `ROLE` 查询一个或多个角色的有效用户。
- [x] `USER` 校验指定账户状态。
- [x] 多角色结果去重。
- [x] 生成账号、名称、角色快照。
- [ ] 支持分批查询和分批插入。
- [ ] 空目标阻止发布。

**测试**：全员包含超级管理员；禁用/删除用户排除；多角色账户只生成一条记录。

### P2-2 富文本和附件安全

新增 `AnnouncementContentSanitizer`：

- [ ] 定义 HTML 标签和属性白名单。
- [ ] 删除脚本、事件属性、危险协议和任意 iframe。
- [ ] 从清洗后的 HTML 生成纯文本摘要。
- [ ] 校验附件文件 ID、类型、大小、数量和访问权限（当前上传走现有文件服务，公告业务侧专项校验待补）。
- [ ] 预览和正式发布共用清洗逻辑。

**测试**：XSS、`javascript:` 链接、非法附件、合法图片和链接。

### P2-3 生命周期 Service

新增 `SystemAnnouncementServiceImpl`：

- [x] 创建草稿。
- [x] 更新草稿。
- [x] 清洗后的预览。
- [x] 发布事务。
- [x] 撤回事务。
- [x] 管理端列表和详情。
- [x] 用户 pending 查询。
- [x] 用户确认。
- [x] 统计和接收明细。
- [x] 发布/撤回审计日志。
- [ ] 状态行锁和幂等处理。

**验收**：发布后不能编辑；重复发布、撤回、确认不产生重复状态变化或统计。

### P2-4 Controller 和权限

新增 `SystemAnnouncementController`：

- [x] 实现管理端 page、detail、create、update、preview、publish、revoke、statistics、recipients。
- [ ] 实现用户端 pending、detail、acknowledge。
- [ ] 所有管理接口添加 `@RequirePermission`。
- [ ] 用户 ID 从 Sa-Token 获取。
- [ ] 统一参数、长度、分页和附件数量校验。
- [ ] 返回统一 `Result`。

**验收**：无权限账户不能调用管理接口；用户不能读取其他用户的接收记录。

## 6. P3：WebSocket 事件和离线补偿

涉及：

```text
NotificationPushService.java
NotificationWebSocketHandler.java
WebSocketSessionManager.java
```

- [x] 增加 `ANNOUNCEMENT_PUBLISHED`。
- [x] 增加 `ANNOUNCEMENT_REVOKED`。
- [x] 只发送公告 ID 和时间，不发送完整 HTML。
- [x] 在事务提交后推送。
- [x] 推送失败不回滚已发布公告。
- [x] pending 查询过滤 `REVOKED`。
- [x] 事件重复不导致重复弹窗。
- [x] 本期明确不记录 Socket 推送统计指标。
- [x] 保留 pending 查询作为离线和推送失败兜底；多 Session 专项改造不纳入本期。

**验收**：在线发布实时到达；离线登录能补偿；撤回事件丢失后仍不会通过 pending 再显示。

## 7. P4：前端类型、API 和 Store

### P4-1 类型和 API

新增：

```text
frontend/med-tech/src/types/announcement.ts
frontend/med-tech/src/api/announcement.ts
```

- [x] 定义公告、目标、接收、统计和附件类型。
- [x] 实现管理端 API。
- [x] 实现用户端 pending、详情、acknowledge API。
- [ ] 区分草稿详情、管理列表和待确认数据结构。

### P4-2 独立 Store

新增：`frontend/med-tech/src/stores/announcement.ts`

- [x] 登录后拉取 pending。
- [x] 按发布时间倒序排序。
- [x] 保证 current 非空时不切换下一条。
- [x] 确认成功后才移出 current。
- [x] 处理发布事件和撤回事件。
- [x] 撤回同时清理 current、queue。
- [x] 重复事件去重。
- [x] 退出登录和切换账户清空状态。
- [ ] 不修改现有 `notificationStore`。

**测试**：单弹窗、倒序、逐条确认、撤回清理、重复事件去重。

## 8. P5：前端弹窗和后台页面

### P5-1 独立弹窗

新增：`frontend/med-tech/src/components/announcementPopup.vue`

- [x] 使用独立居中 Dialog。
- [x] 只显示“我已知晓”。
- [x] 禁止遮罩、ESC 和关闭图标关闭。
- [x] 展示安全富文本、图片、附件和链接。
- [x] 支持撤回导致的程序化关闭。
- [x] 与业务 `notificationPopup.vue` 并列存在但互不共享状态。

### P5-2 App 集成

修改：`frontend/med-tech/src/App.vue`

- [x] 挂载 `announcementPopup`。
- [x] 根据登录用户初始化公告 Store。
- [x] 退出登录时清理公告队列。
- [ ] 确保业务通知初始化和业务卡片行为不变。

### P5-3 管理列表和详情

新增：

```text
frontend/med-tech/src/views/system/announcement.vue
frontend/med-tech/src/views/system/announcementDetail.vue
```

- [x] 标题、状态、目标、人数、发布时间列表。
- [x] 状态筛选。
- [x] 草稿支持编辑、发布。
- [x] 已发布支持统计、接收人明细、撤回。
- [x] 已撤回支持统计和接收人明细；历史审计详情仍待补充。
- [ ] 支持复制为新草稿。
- [x] 根据权限隐藏按钮。

### P5-4 编辑器

新增：`frontend/med-tech/src/views/system/announcementEditor.vue`

- [x] 使用可视化 contenteditable 编辑区，避免新增编辑器依赖。
- [x] 图片/附件通过现有 `/basic/file/upload` 文件服务上传。
- [x] 编辑器草稿可保存上传文件 URL、名称、类型和大小。
- [x] 链接插入。
- [x] 目标类型切换。
- [x] 角色多选。
- [x] 账户多选。
- [ ] 显示目标人数预估。
- [x] 保存草稿、预览。
- [x] 发布前提示“发布后不可修改”。

## 9. P6：联调和回归

### P6-1 公告主流程

- [ ] 创建并保存草稿。
- [ ] 富文本、图片、附件、链接正确回显。
- [ ] 全员发布并验证包含超级管理员。
- [ ] 按角色发布并验证用户快照。
- [ ] 指定账户发布并验证去重。
- [ ] 在线用户实时收到公告。
- [ ] 离线用户登录后收到公告。
- [ ] 多条公告按发布时间倒序逐条显示。
- [ ] 点击“我已知晓”后统计更新。
- [ ] 管理员撤回后当前弹窗关闭、队列清理。
- [ ] 撤回后离线用户不再弹出。

### P6-2 现有通知回归

- [ ] 业务通知仍使用原有卡片。
- [ ] 普通消息铃铛未读数不包含公告。
- [ ] 业务通知确认接口不变。
- [ ] WebSocket AUTH、PING、踢出逻辑不被破坏。
- [ ] 业务消息断线重连和离线补推正常。

## 10. P7：安全、性能和最终验收

### P7-1 安全

- [ ] HTML 白名单过滤通过。
- [ ] 附件类型、大小、权限通过。
- [ ] 管理接口权限通过。
- [ ] 用户越权读取测试通过。
- [ ] 链接协议和外链属性检查通过。
- [ ] 日志不输出完整富文本或大批量敏感用户数据。

### P7-2 性能

- [ ] 全员查询和 pending 查询命中索引。
- [ ] 接收记录批量插入无超大 SQL。
- [ ] WebSocket 不传正文和附件。
- [ ] 大目标发布记录耗时和目标数量。
- [ ] 统计接口不阻塞公告列表查询。

### P7-3 最终验收

- [ ] 后端模块测试通过。
- [ ] 前端 `npm run build` 通过。
- [ ] 数据库迁移在测试环境成功。
- [ ] 发布、确认、撤回、离线补偿全流程通过。
- [ ] 权限、XSS、附件、越权测试通过。
- [ ] 技术设计文档与实际字段、接口、资源编码一致。

## 11. 文件变更清单

### 后端新增

```text
yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/announcement/**
yigongbao-parent/yigongbao-module-notification/src/main/resources/mapper/announcement/**
sql/migration-system-announcement-20260930.sql
```

### 后端可能修改

```text
yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/websocket/NotificationWebSocketHandler.java
yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/impl/NotificationPushService.java
yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/websocket/WebSocketSessionManager.java
sql/ddl.sql
sql/ddl-prod.sql
```

### 前端新增

```text
frontend/med-tech/src/api/announcement.ts
frontend/med-tech/src/types/announcement.ts
frontend/med-tech/src/stores/announcement.ts
frontend/med-tech/src/components/announcementPopup.vue
frontend/med-tech/src/views/system/announcement.vue
frontend/med-tech/src/views/system/announcementEditor.vue
frontend/med-tech/src/views/system/announcementDetail.vue
```

### 前端可能修改

```text
frontend/med-tech/src/App.vue
frontend/med-tech/src/router/system.ts
frontend/med-tech/package.json
frontend/med-tech/package-lock.json
```

## 12. 风险处理

| 风险 | 处理 |
|---|---|
| 全员目标过大 | 分批查询、分批插入、配置目标上限和耗时日志 |
| WebSocket 丢事件 | pending 查询作为最终补偿 |
| 撤回与确认并发 | 状态条件更新，只有 pending 能确认 |
| 富文本 XSS | 前后端双重校验，后端白名单清洗 |
| 多标签页重复弹出 | Store 去重，必要时使用 BroadcastChannel |
| 业务通知受影响 | 公告使用独立 Store、组件和接口 |
| 发布后误修改 | 服务端状态校验和数据库行锁 |
| 统计不一致 | 按影响行数递增，详情接口支持校准 |

## 13. Definition of Done

- [ ] 迁移脚本、后端代码、权限资源、前端页面均已实现。
- [ ] 后端接口有权限、参数、状态和越权测试。
- [ ] 前端 PC 构建通过，强制弹窗交互符合需求。
- [ ] 发布、确认、撤回、离线补偿、实时推送全部验证。
- [ ] 现有业务通知回归通过。
- [ ] 富文本、图片、附件、链接安全检查通过。
- [ ] 技术设计文档与实现一致。
