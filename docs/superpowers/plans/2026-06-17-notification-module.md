# 消息通知模块实施计划

**需求文档**：`.docs/技术实现/消息通知模块需求分析.md`  
**日期**：2026-06-17  
**分支**：`feature/notification-module`（建议从 `dev` 拉）

---

## 一、实施概览

### 目标
实现第一阶段消息通知模块，覆盖需求文档第七节的全部 8 个事件，支持 WebSocket 实时推送和离线补弹。

### 实施顺序原则
依赖关系决定顺序：公共基础（common 事件类）→ 独立模块（notification 核心）→ 事件发布方（order/production）→ 系统集成（boot 依赖注册）。

### 工作量估算
| 阶段 | 内容 | 估时 |
|------|------|------|
| P1 | 模块骨架 + DDL + 枚举 | 2h |
| P2 | 核心 Service（send 三重载 + 数据权限过滤）| 3h |
| P3 | WebSocket 连接管理 + 推送 | 3h |
| P4 | 消息查询 / 操作 REST 接口 | 2h |
| P5 | common 事件类 + 各模块发布 | 3h |
| P6 | NotificationEventListener | 2h |
| P7 | 联调自测 | 2h |

---

## 二、前置准备

### 2.1 数据库 DDL（优先执行）

在 `sql/ddl.sql` 末尾追加，执行后再动代码。

```sql
CREATE TABLE notification_message
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    message_type   VARCHAR(20)  NOT NULL COMMENT '展现类型：MESSAGE/POPUP',
    category       VARCHAR(20)  NOT NULL COMMENT '业务分类：ORDER/APPROVAL/DESIGN/PRODUCTION',
    title          VARCHAR(200) NOT NULL COMMENT '消息标题',
    content        TEXT         COMMENT '消息正文',
    biz_type       VARCHAR(50)  COMMENT '业务数据类型：ORDER/PRODUCTION_CARD',
    biz_id         BIGINT       COMMENT '业务数据ID',
    biz_data       JSON         COMMENT '扩展业务数据（订单号、患者姓名等）',
    biz_status     VARCHAR(20)  COMMENT '业务状态：NULL=正常，CLAIMED=已被他人接收',
    jump_url       VARCHAR(500) COMMENT '前端路由跳转路径',
    receiver_id    BIGINT       NOT NULL COMMENT '接收人 sys_user.id',
    is_read        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    read_time      DATETIME     COMMENT '阅读时间',
    is_confirmed   TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未确认 1=已确认（POPUP专用）',
    confirmed_time DATETIME     COMMENT '确认时间',
    create_time    DATETIME     COMMENT '创建时间',
    create_by      BIGINT       COMMENT '创建人（系统填-1）',
    update_time    DATETIME     COMMENT '更新时间',
    update_by      BIGINT       COMMENT '更新人',
    is_deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_nm_receiver_read (receiver_id, is_read, create_time),
    KEY idx_nm_biz (biz_type, biz_id),
    KEY idx_nm_category_time (category, create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '消息通知表';
```

> `biz_status` 无唯一约束，不需要函数索引。

---

## 三、P1：模块骨架

### 3.1 创建 Maven 模块

在 `yigongbao-parent/` 下新建目录 `yigongbao-module-notification/`，创建 `pom.xml`：

```xml
<parent>
    <groupId>com.yigongbao</groupId>
    <artifactId>yigongbao-parent</artifactId>
    <version>1.0.0</version>
</parent>
<artifactId>yigongbao-module-notification</artifactId>

<dependencies>
    <dependency>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-common</artifactId>
    </dependency>
    <dependency>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-framework</artifactId>
    </dependency>
    <dependency>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-module-system</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
</dependencies>
```

同步修改：
- `yigongbao-parent/pom.xml` → `<modules>` 中添加 `yigongbao-module-notification`
- `yigongbao-boot/pom.xml` → `<dependencies>` 中添加对 `yigongbao-module-notification` 的依赖

### 3.2 包结构

```
src/main/java/com/yigongbao/module/notification/
├── entity/
├── mapper/
├── service/
│   └── impl/
├── controller/
├── listener/
├── websocket/
├── dto/
├── vo/
└── enums/
```

### 3.3 枚举类（4个）

**位置**：`enums/` 包下

| 类名 | 枚举值 |
|------|--------|
| `MessageTypeEnum` | `MESSAGE`, `POPUP` |
| `MessageCategoryEnum` | `ORDER`, `APPROVAL`, `DESIGN`, `PRODUCTION` |
| `BizTypeEnum` | `ORDER`, `PRODUCTION_CARD` |
| `BizStatusEnum` | `CLAIMED` |

> 枚举类遵循项目规范：`@Getter @AllArgsConstructor`，含 `code` 和 `desc` 字段。

---

## 四、P2：核心 Service

### 4.1 Entity

`NotificationMessageEntity` 继承 `BaseEntity`，字段对应 DDL，枚举字段用 String 类型存储枚举 code（与现有项目一致）。

注意：`bizData` 字段类型用 `String`（JSON 字符串），不要用 `Map`，避免 MyBatis-Plus 类型处理问题。

### 4.2 Mapper

`NotificationMessageMapper` 继承 `BaseMapper<NotificationMessageEntity>`。

需在 Mapper 中手写的方法（BaseMapper 不够用）：
```java
// 查询未读数量（按分类分组）
List<CategoryUnreadCountVO> selectUnreadCountByCategory(@Param("receiverId") Long receiverId);

// 批量更新已读
void batchMarkRead(@Param("ids") List<Long> ids, @Param("receiverId") Long receiverId);

// 批量标记全部已读（按分类）
void markAllReadByCategory(@Param("receiverId") Long receiverId, @Param("category") String category);

// CLAIMED 批量更新（ProductionCardClaimedEvent 专用）
void batchMarkClaimed(@Param("recordId") Long recordId, @Param("claimedByUserId") Long claimedByUserId);
```

XML 写在 `resources/mapper/NotificationMessageMapper.xml`。

**`batchMarkClaimed` SQL**（对应需求文档 7.2.8）：
```sql
UPDATE notification_message
SET biz_status = 'CLAIMED',
    is_confirmed = 1,
    confirmed_time = NOW(),
    update_time = NOW()
WHERE biz_type = 'PRODUCTION_CARD'
  AND biz_id = #{recordId}
  AND receiver_id != #{claimedByUserId}
  AND is_confirmed = 0
  AND is_deleted = 0
```

### 4.3 DTO 类

**`NotificationDTO`**（消息内容，调用 send 时传入）：
```java
String title;
String content;
MessageTypeEnum messageType;
MessageCategoryEnum category;
String bizType;        // BizTypeEnum.code
Long bizId;
String bizData;        // JSON 字符串
String jumpUrl;
```

**`NotificationContext`**（按角色推送时的数据权限上下文）：
```java
Long hospitalId;  // HOSPITALS scope
Long orgId;       // ORG / DEPT scope
Long centerId;    // CENTER scope
```

提供静态工厂方法：`NotificationContext.ofHospital(id)`, `ofOrg(id)`, `ofCenter(id)`，提升调用处可读性。

**`MessageQueryDTO`**（消息列表查询）：
```java
String category;
Integer isRead;
String messageType;
Integer pageNum;
Integer pageSize;
```

### 4.4 INotificationService 接口

```java
public interface INotificationService {
    void send(String roleCode, NotificationContext context, NotificationDTO dto);
    void send(Long userId, NotificationDTO dto);
    void send(List<Long> userIds, NotificationDTO dto);

    IPage<MessageVO> listMessages(MessageQueryDTO query);
    Map<String, Object> getUnreadCount();
    void markRead(Long id);
    void batchMarkRead(List<Long> ids, String category, Boolean markAll);
    void confirm(Long id);
    void deleteMessage(Long id);
}
```

### 4.5 NotificationServiceImpl 实现

**关键点：`send(roleCode, context, dto)` 数据权限过滤**

读取 `sys_role.data_scope_type`（通过 system 模块的 RoleService 查询），根据 `DataScopeTypeEnum` 分支查询目标用户 ID：

| scope | SQL 查询逻辑 |
|-------|-------------|
| `ALL` | `WHERE r.role_code=? AND u.is_deleted=0` |
| `ORG` | `+ AND u.org_id=#{context.orgId}` |
| `DEPT` | `+ AND u.org_id=#{context.orgId}` （第一阶段等同ORG）|
| `HOSPITALS` | `JOIN sys_user_hospital uh ON uh.user_id=u.id AND uh.hospital_id=#{context.hospitalId}` |
| `CENTER` | `+ AND u.center_id=#{context.centerId}` |
| `SELF` | 空列表（角色推送不适用 SELF，记 WARN 日志）|

查询逻辑建议封装在 system 模块的 `UserService` 或新增 `NotificationUserQueryService` 中，通知模块调用该接口，避免通知模块直接写 sys_user 查询 SQL。

**`send(List<Long> userIds, dto)` 核心逻辑**：
1. 过滤空列表
2. 批量构建 `NotificationMessageEntity` 列表（每个 userId 一条记录）
3. `saveBatch(entities)`
4. 调用 `NotificationPushService.pushToUsers(userIds, entities)` 推送在线用户

### 4.6 NotificationPushService

负责 WebSocket 推送，独立出来不污染业务 Service：
```java
public class NotificationPushService {
    void pushToUsers(List<Long> userIds, List<NotificationMessageEntity> messages);
    void pushToUser(Long userId, NotificationMessageEntity message);
}
```

---

## 五、P3：WebSocket

### 5.1 WebSocketConfig

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler(), "/api/notification/websocket")
                .setAllowedOrigins("*");
    }
}
```

### 5.2 WebSocketSessionManager

维护 `userId → WebSocketSession` 的 `ConcurrentHashMap`，提供：
```java
void addSession(Long userId, WebSocketSession session);
void removeSession(Long userId);
WebSocketSession getSession(Long userId);
boolean isOnline(Long userId);
```

### 5.3 NotificationWebSocketHandler

继承 `TextWebSocketHandler`，在 `afterConnectionEstablished` 中完成 Token 认证并注册 Session：

**Token 传递方式**：客户端建立连接后，第一帧发送 `{"type":"AUTH","token":"xxxx"}`，handler 解析后调用 `StpUtil.checkByToken(token)` 验证，成功则将 `userId` 注册到 `WebSocketSessionManager`。

生命周期管理：
- `afterConnectionEstablished`：等待首帧 AUTH
- `handleTextMessage`：处理 AUTH 帧；收到心跳 `{"type":"PING"}` 回复 `{"type":"PONG"}`
- `afterConnectionClosed`：从 SessionManager 移除

**推送消息格式**（JSON 字符串发送）：
```json
{
  "type": "NEW_MESSAGE",
  "data": {
    "id": 1001,
    "messageType": "POPUP",
    "category": "PRODUCTION",
    "title": "...",
    "bizStatus": null,
    "createTime": "2026-06-17 10:30:00"
  }
}
```

---

## 六、P4：REST 接口

### 6.1 NotificationController

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/notification/messages` | 分页查询（带 unreadCount） |
| GET | `/api/notification/messages/unread-count` | 未读数量 |
| PUT | `/api/notification/messages/{id}/read` | 标记已读 |
| PUT | `/api/notification/messages/batch-read` | 批量标记已读 |
| PUT | `/api/notification/messages/{id}/confirm` | 确认弹窗 |
| DELETE | `/api/notification/messages/{id}` | 删除消息 |

所有接口从 SaToken 上下文获取 `receiverId`（`StpUtil.getLoginIdAsLong()`），禁止前端传入。

### 6.2 MessageVO

```java
Long id;
String messageType;
String category;
String title;
String content;
String bizType;
Long bizId;
Object bizData;     // 反序列化为 Map
String bizStatus;
String jumpUrl;
Integer isRead;
Integer isConfirmed;
LocalDateTime createTime;
```

---

## 七、P5：事件类与事件发布

### 7.1 在 `yigongbao-common` 新增事件类

**位置**：`src/main/java/com/yigongbao/common/event/`

需新增的 7 个事件类（参考现有 `OrderCancelledEvent` 写法）：

| 事件类 | 字段 |
|--------|------|
| `OrderSubmittedEvent` | `Long orderId, String businessType, Long hospitalId, Long orgId, Long createBy` |
| `RegionalAuditPassedEvent` | `Long orderId, Long orgId` |
| `AuditRejectedEvent` | `Long orderId, Long createBy, String rejectReason` |
| `DesignerAssignedEvent` | `Long orderId, Long newDesignerId, Long oldDesignerId` |
| `OrderModifyApplySubmittedEvent` | `Long orderId, Long orgId, Long applyUserId` |
| `OrderModifyApplyRejectedEvent` | `Long applyId, Long applyUserId, String rejectReason` |
| `ProductionCardsCreatedEvent` | `List<Long> recordIds` |

> `ProductionCardClaimedEvent` 已在需求文档中定义，由通知模块内部处理，建议也放 common。字段：`Long recordId, Long claimedByUserId`。

### 7.2 在各模块发布事件

#### 7.2.1 `yigongbao-module-order` → `OrderMainServiceImpl`

注入 `ApplicationEventPublisher context`（已有）。

| 发布位置 | 事件 | 条件 |
|---------|------|------|
| `submitOrder()` 成功后 | `OrderSubmittedEvent` | 无条件 |
| `rejectAudit()` 成功后 | `AuditRejectedEvent` | 无条件 |
| `passAudit()` 区域审核分支（~670行）成功后 | `RegionalAuditPassedEvent` | `businessType` 为试用订单 |

#### 7.2.2 `yigongbao-module-order` → `OrderModifyApplyServiceImpl`

| 发布位置 | 事件 |
|---------|------|
| `submitApply()` 成功后 | `OrderModifyApplySubmittedEvent` |
| `auditApply()` 驳回分支成功后 | `OrderModifyApplyRejectedEvent` |

#### 7.2.3 `yigongbao-module-design` → `DesignerAssignmentServiceImpl`

| 发布位置 | 事件 |
|---------|------|
| 分配设计师成功后 | `DesignerAssignedEvent`（首次分配 oldDesignerId=null，重新分配则有值）|

#### 7.2.4 `yigongbao-module-production` → `DesignCompletedListener`

在创建流转卡批量 insert 完成后（现约 157 行处）追加：
```java
context.publishEvent(new ProductionCardsCreatedEvent(this, recordIds));
```

> `recordIds` 为本次 insert 成功的所有流转卡 ID 列表，需在循环外收集。

#### 7.2.5 `yigongbao-module-production` → 下载数据包 Service

在流转卡状态变为 `PENDING_PRINT(3010)` 时发布：
```java
context.publishEvent(new ProductionCardClaimedEvent(this, recordId, claimedByUserId));
```

---

## 八、P6：NotificationEventListener

### 8.1 类结构

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final INotificationService notificationService;
    private final ProductionRecordService productionRecordService; // 查询流转卡信息
    // 其他 service...

    @EventListener
    public void onOrderSubmitted(OrderSubmittedEvent event) { ... }

    @EventListener
    public void onRegionalAuditPassed(RegionalAuditPassedEvent event) { ... }

    @EventListener
    public void onAuditRejected(AuditRejectedEvent event) { ... }

    @EventListener
    public void onDesignerAssigned(DesignerAssignedEvent event) { ... }

    @EventListener
    public void onModifyApplySubmitted(OrderModifyApplySubmittedEvent event) { ... }

    @EventListener
    public void onModifyApplyRejected(OrderModifyApplyRejectedEvent event) { ... }

    @EventListener
    public void onProductionCardsCreated(ProductionCardsCreatedEvent event) { ... }

    @EventListener
    public void onProductionCardClaimed(ProductionCardClaimedEvent event) { ... }
}
```

> 监听方法默认是**同步**调用（Spring Event 默认）。如需异步，在方法上加 `@Async`，需确保 `@EnableAsync` 已在 framework 模块中开启（检查现有配置）。建议第一阶段同步，稳定后再改异步。

### 8.2 各事件处理逻辑

**`onOrderSubmitted`**：
- 试用订单（`businessType=11.3` 或对应枚举值）→ `send("regional-manager", Context.ofHospital(hospitalId), dto)`
- 其他 → `send("designer-manager", Context.ofOrg(orgId), dto)`
- `messageType=POPUP, category=APPROVAL`

**`onRegionalAuditPassed`**：
- `send("designer-manager", Context.ofOrg(orgId), dto)`
- `messageType=POPUP, category=APPROVAL`

**`onAuditRejected`**：
- `send(createBy, dto)`
- `messageType=MESSAGE, category=ORDER`

**`onDesignerAssigned`**：
- 推新设计师：`send(newDesignerId, dto)` → `POPUP, DESIGN`
- 若 `oldDesignerId != null`，推旧设计师：`send(oldDesignerId, dto)` → `MESSAGE, DESIGN`（"您的设计任务已被重新分配"）

**`onModifyApplySubmitted`**：
- `send("designer-manager", Context.ofOrg(orgId), dto)` → `POPUP, APPROVAL`

**`onModifyApplyRejected`**：
- `send(applyUserId, dto)` → `MESSAGE, ORDER`

**`onProductionCardsCreated`**：
- 遍历 `recordIds`，每张流转卡：
  1. 查询 `ProductionRecordEntity` 获取 `processingCenterId` 和展示信息
  2. `send("production-worker", Context.ofCenter(processingCenterId), dto)` → `POPUP, PRODUCTION`
  3. `send("production-manager", Context.ofCenter(processingCenterId), dto)` → `POPUP, PRODUCTION`
  - `bizType=PRODUCTION_CARD, bizId=recordId`

**`onProductionCardClaimed`**：
- 调用 `notificationMessageMapper.batchMarkClaimed(recordId, claimedByUserId)`
- 不推送，不写新记录

---

## 九、P7：联调自测清单

按照以下顺序自测：

- [ ] WebSocket 连接建立，AUTH 帧认证通过
- [ ] 心跳 PING/PONG 正常
- [ ] 调用 `send(userId, dto)` 后：DB 有记录，在线用户收到 WebSocket 推送
- [ ] 按角色推送：ORG scope，CENTER scope，HOSPITALS scope 分别验证推送范围正确
- [ ] `GET /unread-count` 返回正确分类未读数
- [ ] `PUT /{id}/read` 标记已读，`isRead=1, readTime` 已更新
- [ ] `PUT /batch-read`（markAll=true）批量已读
- [ ] `PUT /{id}/confirm` 确认弹窗，`isConfirmed=1, confirmedTime` 已更新
- [ ] `DELETE /{id}` 软删除，下次查询不再出现
- [ ] 离线消息：用户下线时触发事件 → DB 写入 → 用户登录后前端拉 `isConfirmed=0, POPUP` 消息列表
- [ ] CLAIMED 流程：生产员A下载数据包 → 其他生产员的同 recordId 通知 `biz_status=CLAIMED, is_confirmed=1`

---

## 十、注意事项

1. **事务边界**：`send(List<Long>, dto)` 的 `saveBatch` 和 WebSocket 推送要分开，推送失败不能回滚消息写入。实现时先 commit，再 push（`@Transactional` 方法内完成 saveBatch，方法返回后推送）。

2. **跨模块依赖**：`NotificationEventListener` 依赖 `ProductionRecordService`（查询流转卡信息）。`yigongbao-module-notification` pom 需要添加对 `yigongbao-module-production` 的依赖，或通过接口 + Spring 注入解耦。优先直接依赖，保持简单。

3. **businessType 试用订单判断**：确认 `businessType=11.3` 对应的枚举或常量，在 order 模块中找到对应定义后使用常量而非魔法值。

4. **消息标题内容**：`NotificationEventListener` 中硬编码标题模板（需求文档明确第一阶段不做消息模板），如："流转卡{recordNo}设计已完成，请接收生产"。`bizData` 存 JSON 字符串（`JSONUtil.toJsonStr(map)`）。

5. **WebSocket 并发**：`WebSocketSessionManager` 使用 `ConcurrentHashMap`；推送时 `session.sendMessage()` 需 catch `IOException` 并记录 WARN 日志（不抛出，避免影响其他用户推送）。
