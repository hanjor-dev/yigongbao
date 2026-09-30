# 系统公告发布与强制确认弹窗技术设计

**状态**：待开发  
**日期**：2026-09-30  
**范围**：医工宝后端、`frontend/med-tech` PC 端

## 1. 目标与范围

现有系统已经具备订单、审核、生产等业务消息通知。本需求新增“系统公告”能力：具备权限的后台账户编辑富文本，选择全员、角色或指定账户后立即发布；用户登录或在线时以独立强制确认弹窗接收；管理员撤回后，已打开弹窗立即关闭，队列中的公告和后续离线补弹也全部停止。

系统公告与现有业务通知必须保持两条独立链路：

| 项目 | 现有业务通知 | 系统公告 |
|---|---|---|
| 来源 | 业务事件 | 后台人工发布 |
| 展现 | `notificationPopup.vue` 业务卡片 | 独立居中弹窗 |
| 目标 | 业务规则解析 | 全员、角色、指定账户 |
| 内容 | 业务摘要 | 富文本、图片、附件、链接 |
| 关闭 | 业务确认/查看详情 | 仅“我已知晓” |
| 撤回 | 无批次撤回 | 立即关闭并阻止后续显示 |

本期不实现定时发布、自动失效、移动端界面、邮件/短信推送和发布审批流。

## 2. 已确认业务规则

1. 只支持立即发布，不支持定时发布。
2. 不设置失效时间，公告持续有效直到确认或撤回。
3. 已发布公告不可编辑，需要修改时复制为新公告。
4. 撤回会关闭在线用户当前正在显示的公告，并清理队列。
5. 弹窗只显示“我已知晓”，禁止遮罩关闭、ESC 关闭和关闭图标关闭。
6. 用户未确认时持续保留；异常退出或重新登录后继续弹出。
7. 全员包括所有未禁用、未删除账户，包括超级管理员。
8. 角色目标按发布时解析用户快照，后续角色变化不影响本次公告。
9. 统计目标人数、确认人数、未确认人数、撤回人数和确认率。
10. 保留发布和撤回审计日志。
11. 支持富文本图片、附件和链接。
12. 同一时间只显示一条，多个待确认公告按发布时间倒序逐个显示。

## 3. 现有实现基线

后端已有 `yigongbao-module-notification`，包括：

- `notification_message` 用户消息表。
- `MessageTypeEnum` 的 `MESSAGE`、`POPUP`。
- `NotificationServiceImpl` 的查询、已读、确认、离线补推。
- `NotificationWebSocketHandler` 的首帧 `AUTH` 和 Sa-Token 校验。
- `NotificationPushService` 的在线推送。

前端 [App.vue](../../../frontend/med-tech/src/App.vue) 会初始化 `notificationStore`，现有 [notificationPopup.vue](../../../frontend/med-tech/src/components/notificationPopup.vue) 是业务卡片，[notificationBell.vue](../../../frontend/med-tech/src/components/notificationBell.vue) 是业务消息入口。

公告不得进入现有 `notificationStore.popupQueue`，不得复用现有业务弹窗组件，否则会混淆未读数、确认状态和撤回范围。

## 4. 领域模型和状态机

### 4.1 核心对象

- **公告**：一次后台发布的不可变通知内容和生命周期。
- **目标**：发布时选择的全员、角色或账户集合。
- **接收记录**：某个用户对某条公告的确认状态快照。
- **审计日志**：发布、撤回等管理动作的不可变记录。
- **确认**：用户点击“我已知晓”并由服务端成功写入。

### 4.2 公告状态

```text
DRAFT --publish--> PUBLISHED --revoke--> REVOKED
```

- `DRAFT` 可编辑、预览、发布。
- `PUBLISHED` 只可查看、统计、撤回，正文和目标不可修改。
- `REVOKED` 只可查看历史和统计，不允许恢复发布。

### 4.3 接收状态

```text
PENDING --acknowledge--> ACKNOWLEDGED
PENDING --revoke--> REVOKED
```

撤回时已经确认的记录保持 `ACKNOWLEDGED`，保证历史统计准确；只有撤回时仍为 `PENDING` 的记录转为 `REVOKED`。

## 5. 架构设计

```text
后台页面
  └─ REST → SystemAnnouncementController
              └─ SystemAnnouncementService
                  ├─ 内容清洗与附件校验
                  ├─ 目标用户解析与快照
                  ├─ 公告/接收/审计事务
                  └─ 提交后 WebSocket 事件

登录或重连 → GET /notification/announcements/pending
在线发布   → ANNOUNCEMENT_PUBLISHED
管理员撤回 → ANNOUNCEMENT_REVOKED
                         ↓
                 announcementStore
                         ↓
                 announcementPopup.vue
```

数据库是最终事实来源，WebSocket 只负责实时加速；事件丢失时，登录、重连和主动刷新必须能够补偿。

## 6. 数据库设计

### 6.1 公告主表 `system_announcement`

```sql
CREATE TABLE system_announcement
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    title              VARCHAR(200) NOT NULL COMMENT '标题',
    content_html       MEDIUMTEXT   NOT NULL COMMENT '清洗后的HTML',
    content_text       TEXT         NOT NULL COMMENT '纯文本摘要',
    status             VARCHAR(20)  NOT NULL COMMENT 'DRAFT/PUBLISHED/REVOKED',
    target_type        VARCHAR(20)  NOT NULL COMMENT 'ALL/ROLE/USER',
    target_role_ids    JSON                  COMMENT '草稿阶段选择的角色ID列表',
    target_user_ids    JSON                  COMMENT '草稿阶段选择的用户ID列表',
    force_confirm      TINYINT      NOT NULL DEFAULT 1 COMMENT '强制确认',
    target_count       INT          NOT NULL DEFAULT 0 COMMENT '目标人数',
    acknowledged_count INT          NOT NULL DEFAULT 0 COMMENT '确认人数缓存',
    revoked_count      INT          NOT NULL DEFAULT 0 COMMENT '撤回时未确认人数',
    published_at       DATETIME              COMMENT '发布时间',
    published_by       BIGINT                COMMENT '发布人',
    revoked_at         DATETIME              COMMENT '撤回时间',
    revoked_by         BIGINT                COMMENT '撤回人',
    create_time        DATETIME     NOT NULL,
    create_by          BIGINT       NOT NULL,
    update_time        DATETIME     NOT NULL,
    update_by          BIGINT,
    is_deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sa_status_time (status, published_at),
    KEY idx_sa_creator (create_by, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告主表';
```

发布后禁止更新 `title`、`content_html`、`content_text`、`target_type`；统计缓存只能由确认和撤回流程更新。

### 6.2 接收表 `system_announcement_recipient`

```sql
CREATE TABLE system_announcement_recipient
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    announcement_id    BIGINT      NOT NULL,
    user_id            BIGINT      NOT NULL,
    user_name_snapshot VARCHAR(100),
    username_snapshot  VARCHAR(100),
    role_snapshot      VARCHAR(500),
    delivery_status    VARCHAR(20) NOT NULL COMMENT 'PENDING/ACKNOWLEDGED/REVOKED',
    acknowledged_at    DATETIME,
    revoked_at         DATETIME,
    create_time        DATETIME    NOT NULL,
    update_time        DATETIME,
    is_deleted          TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sa_recipient (announcement_id, user_id),
    KEY idx_sar_user_status (user_id, delivery_status, announcement_id),
    KEY idx_sar_announcement_status (announcement_id, delivery_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告接收记录';
```

角色目标必须在发布时转为用户记录；后续角色变化不重新计算历史公告。

### 6.3 附件表 `system_announcement_attachment`

```sql
CREATE TABLE system_announcement_attachment
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    announcement_id BIGINT       NOT NULL,
    file_id         VARCHAR(64),
    file_name       VARCHAR(255) NOT NULL,
    file_url        VARCHAR(1000) NOT NULL,
    file_type       VARCHAR(100),
    file_size       BIGINT,
    sort            INT          NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL,
    create_by       BIGINT       NOT NULL,
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_saa_announcement (announcement_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告附件';
```

优先复用现有统一文件存储和权限体系，避免公告模块重复实现文件上传。

### 6.4 审计表 `system_announcement_audit_log`

```sql
CREATE TABLE system_announcement_audit_log
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    announcement_id BIGINT      NOT NULL,
    operation_type  VARCHAR(20) NOT NULL COMMENT 'PUBLISH/REVOKE',
    operator_id     BIGINT      NOT NULL,
    operator_name   VARCHAR(100),
    before_status   VARCHAR(20),
    after_status    VARCHAR(20) NOT NULL,
    target_type     VARCHAR(20),
    target_count    INT,
    client_ip       VARCHAR(64),
    remark          VARCHAR(500),
    operation_time  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_saal_announcement_time (announcement_id, operation_time),
    KEY idx_saal_operator_time (operator_id, operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告审计日志';
```

统一操作日志可以额外记录，但不能依赖日志文本恢复公告状态。

## 7. 后端模块设计

建议新增包：

```text
com.yigongbao.module.notification.announcement
├── controller/SystemAnnouncementController.java
├── dto/AnnouncementCreateDTO.java
├── dto/AnnouncementPageDTO.java
├── dto/AnnouncementTargetDTO.java
├── entity/*AnnouncementEntity.java
├── enums/Announcement*Enum.java
├── mapper/*AnnouncementMapper.java
├── service/ISystemAnnouncementService.java
├── service/AnnouncementTargetResolver.java
├── service/AnnouncementContentSanitizer.java
├── service/impl/SystemAnnouncementServiceImpl.java
└── vo/*AnnouncementVO.java
```

### 7.1 目标解析

发布请求只提交目标描述，不信任前端最终用户列表：

```json
{
  "targetType": "ROLE",
  "roleIds": [2, 5],
  "userIds": []
}
```

解析规则：

- `ALL`：查询所有未禁用、未删除用户。
- `ROLE`：查询拥有所选角色且用户有效的账户，多个角色去重。
- `USER`：校验指定账户存在、未禁用、未删除。
- 发布前再次查询用户状态，避免页面打开后用户状态变化。
- 写入名称、账号、角色快照。
- 空目标阻止发布。

### 7.2 发布事务

1. 获取当前账户和客户端 IP。
2. 查询公告并加行锁，确认是 `DRAFT`。
3. 清洗正文、校验附件和链接。
4. 解析、去重、快照目标用户。
5. 更新公告为 `PUBLISHED`。
6. 批量插入接收记录。
7. 写入 `PUBLISH` 审计日志。
8. 事务提交后推送 WebSocket。

WebSocket 失败不能回滚已发布公告；离线查询承担补偿责任。

### 7.3 撤回事务

1. 查询公告并加行锁，确认是 `PUBLISHED`。
2. 更新公告为 `REVOKED`，保存撤回人和时间。
3. 仅将 `PENDING` 接收记录更新为 `REVOKED`。
4. 保存撤回人数并写入 `REVOKE` 日志。
5. 提交后推送撤回事件。

重复撤回必须幂等，不重复写日志和推送。

### 7.4 确认事务

```sql
UPDATE system_announcement_recipient
SET delivery_status = 'ACKNOWLEDGED',
    acknowledged_at = NOW(),
    update_time = NOW()
WHERE announcement_id = #{announcementId}
  AND user_id = #{currentUserId}
  AND delivery_status = 'PENDING';
```

只有影响行数为 1 时才递增 `acknowledged_count`。已撤回公告不能确认。

## 8. API 契约

基础路径：`/notification/announcements`。

### 管理端

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/page` | `notification:announcement:list` | 分页列表 |
| GET | `/{id}` | `notification:announcement:view` | 详情 |
| POST | `/` | `notification:announcement:create` | 创建草稿 |
| PUT | `/{id}` | `notification:announcement:update` | 更新草稿 |
| POST | `/{id}/preview` | `notification:announcement:preview` | 清洗后预览 |
| POST | `/{id}/publish` | `notification:announcement:publish` | 立即发布 |
| POST | `/{id}/revoke` | `notification:announcement:revoke` | 撤回 |
| GET | `/{id}/statistics` | `notification:announcement:statistics` | 统计 |
| POST | `/{id}/recipients/page` | `notification:announcement:statistics` | 接收明细 |

### 用户端

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/pending` | 当前用户待确认公告 |
| GET | `/{id}` | 当前用户有权查看的详情 |
| PUT | `/{id}/acknowledge` | 确认公告 |

当前用户从 Sa-Token 获取，禁止前端传 `userId` 代替身份。

### 创建/更新请求

```json
{
  "title": "系统维护通知",
  "contentHtml": "<p>系统将于今晚进行维护。</p>",
  "attachments": [{"fileId": "1892345678901234567", "fileName": "维护说明.pdf"}],
  "target": {"targetType": "ROLE", "roleIds": [2, 5], "userIds": []}
}
```

发布接口不接收可修改正文，发布后正文和目标只能读取。

统计返回：

```json
{
  "targetCount": 128,
  "acknowledgedCount": 93,
  "pendingCount": 20,
  "revokedCount": 15,
  "acknowledgeRate": 72.66
}
```

## 9. WebSocket 协议

沿用现有 `/notification/websocket` 和首帧 `AUTH`，新增：

```json
{
  "type": "ANNOUNCEMENT_PUBLISHED",
  "data": {"announcementId": 1001, "publishedAt": "2026-09-30T15:30:00"}
}
```

```json
{
  "type": "ANNOUNCEMENT_REVOKED",
  "data": {"announcementId": 1001}
}
```

事件只传 ID 和时间，不传完整 HTML。发布事件在事务提交后发送。撤回事件收到后，前端必须关闭当前公告并移除队列项；若事件丢失，`pending` 接口仍不能返回撤回公告。

当前 `WebSocketSessionManager` 是单用户单会话。若本期不改造成多 Session，至少用 pending 查询兜底；后续可改为 `Map<userId, Map<sessionId, WebSocketSession>>`，并用浏览器 `BroadcastChannel` 防止同一账户多标签页重复弹出。

## 10. 前端 PC 端设计

新增：

```text
frontend/med-tech/src/api/announcement.ts
frontend/med-tech/src/types/announcement.ts
frontend/med-tech/src/stores/announcement.ts
frontend/med-tech/src/components/announcementPopup.vue
frontend/med-tech/src/views/system/announcement.vue
frontend/med-tech/src/views/system/announcementEditor.vue
frontend/med-tech/src/views/system/announcementDetail.vue
```

### 10.1 Store

```ts
const queue = ref<AnnouncementPendingVO[]>([])
const current = ref<AnnouncementPendingVO | null>(null)
```

核心规则：

- 登录后先拉取 pending，按 `publishedAt DESC, id DESC` 入队。
- current 非空时不显示下一条。
- 确认成功后才移除 current 并显示下一条。
- 发布事件触发刷新并去重。
- 撤回事件同时清理 current 和 queue。
- 退出登录、切换账户时清空全部状态。
- 不改变现有 `notificationStore`。

### 10.2 独立弹窗

```vue
<el-dialog
  v-model="visible"
  :close-on-click-modal="false"
  :close-on-press-escape="false"
  :show-close="false"
>
  <!-- 标题、清洗后的正文、附件、链接 -->
  <el-button type="primary" @click="acknowledge">我已知晓</el-button>
</el-dialog>
```

在 [App.vue](../../../frontend/med-tech/src/App.vue) 中与业务弹窗并列挂载，但不共享 Store、队列、确认接口或未读数。

后台页面建议挂到动态系统资源下“系统管理 / 系统公告”，包含列表、草稿编辑、预览、发布、撤回、统计和接收明细。

## 11. 富文本、图片和附件安全

- 后端使用 HTML 白名单清洗；禁止 `script`、事件属性、危险协议和任意 iframe。
- 预览和正式发布使用同一清洗逻辑。
- `content_text` 从清洗结果生成。
- 图片和附件复用统一上传接口；当前阶段不增加公告业务侧 MIME、扩展名、大小和数量限制，沿用现有文件服务的通用安全策略。
- 下载时再次校验公告访问权限。
- 附件地址只允许 `http`、`https` 或站内绝对路径；新窗口链接添加 `rel="noopener noreferrer"`。文件 ID 按字符串处理，避免雪花 ID 在浏览器中发生数值精度丢失。
- 前端展示富文本时不能把数据库内容视为天然安全。

## 12. 权限、日志和统计

权限资源：

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

前端权限只控制界面；后端 `@RequirePermission` 是最终安全边界。超级管理员默认拥有权限，但不能用前端硬编码角色替代后端鉴权。

发布/撤回日志至少保存公告 ID、操作类型、操作人、前后状态、目标类型、目标人数、时间和客户端 IP。公告统计以接收表为事实来源，主表缓存字段用于列表性能，定期或详情查询可做校准。

## 13. 一致性、性能和测试要求

- 发布、撤回使用状态条件和行锁。
- 接收表以 `(announcement_id, user_id)` 唯一约束防重。
- 全员解析、接收插入分批执行，避免超大 SQL。
- pending 查询使用用户、状态、公告 ID 索引。
- WebSocket 只传 ID，不发送正文和附件；当前阶段不记录推送数量、成功率和失败率指标，保留 pending 查询作为离线和推送失败兜底。
- 统计不在普通列表查询中扫描全部接收记录。

后端测试至少覆盖：目标解析、超级管理员纳入、禁用用户排除、角色快照、状态流转、重复确认、撤回并发、XSS 清洗、权限越权和事务回滚。

前端测试至少覆盖：时间倒序、单弹窗、逐条确认、撤回关闭、队列清理、重复事件去重、退出清理以及禁止普通关闭。

## 14. 部署和回滚

部署顺序：数据库迁移 → 后端兼容版本 → 权限和菜单资源 → 前端版本 → 发布/确认/撤回验收。

回滚不删除公告、接收记录和审计日志；前端回滚不会破坏数据。后端回滚前确认旧版本不会误读新表，功能关闭时保留 pending 查询的安全过滤。
