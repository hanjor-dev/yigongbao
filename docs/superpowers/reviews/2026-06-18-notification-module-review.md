# 消息通知模块代码审查报告

**审查日期**: 2026-06-18  
**审查范围**: yigongbao-module-notification 全部功能及关联代码  
**审查人**: Claude  

---

## 一、审查概要

### 1.1 审查维度

1. 核心流程逻辑正确性、接口功能覆盖度
2. 参数校验、异常处理、数据权限、操作权限
3. 边界场景异常处理
4. 代码规范（注释、日志、代码优雅度）
5. 安全性
6. 架构合理性、性能考虑

### 1.2 审查结果统计

| 问题等级 | 数量 | 说明 |
|---------|------|------|
| Critical（严重） | 3 | 事务边界、空列表误操作、事件监听同步耦合 |
| High（高） | 3 | NPE 风险、异常隔离缺失 |
| Medium（中） | 6 | WebSocket 资源泄漏、查询参数缺失、校验不足 |
| Low（低） | 3 | 文档缺失、语义不严谨 |
| **需业务确认的风险** | 8 | 数据完整性、前端路由对齐、性能压测 |

**总计**: 15个可确认代码问题 + 8个待确认风险

---

## 二、可确认的代码问题


### 2.1 Critical 严重问题

#### 问题 1：事务未提交时执行 WebSocket 推送（竞态条件）

**文件路径**: `NotificationServiceImpl.java:92-102`

**问题描述**:
```java
@Transactional(rollbackFor = Exception.class)
public void send(List<Long> userIds, NotificationDTO dto) {
    // ...
    saveBatch(entities);  // 数据库写入，事务尚未提交
    pushService.pushToUsers(userIds, entities);  // WebSocket 推送
    log.info("通知发送完成: ...");  // 方法结束后事务才提交
}
```

代码注释声称"事务提交后推送"，但实际 `pushService.pushToUsers()` 在 `@Transactional` 方法内部调用，此时事务尚未提交。消息已有数据库生成的 ID，但对其他事务不可见。

**影响域**:
- 用户通过 WebSocket 收到推送后立即调用查询接口，可能查不到消息（`SELECT` 读不到未提交事务的数据）
- 并发场景下竞态概率较高（推送延迟 < 100ms，事务提交延迟 ~10-50ms）

**修复建议**:
```java
@Transactional(rollbackFor = Exception.class)
public void send(List<Long> userIds, NotificationDTO dto) {
    // ...
    saveBatch(entities);
    
    // 注册事务提交后回调
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pushService.pushToUsers(userIds, entities);
                log.info("通知发送完成: ...");
            }
        }
    );
}
```

---

#### 问题 2：事件监听器在父事务中同步执行，异常导致业务回滚

**文件路径**: `NotificationEventListener.java` 全部 `@EventListener` 方法

**问题描述**:
Spring 的 `@EventListener` 默认在事件发布者的事务内同步执行。当 `OrderMainServiceImpl.submitOrder()` 发布 `OrderSubmittedEvent` 时，`NotificationEventListener.onOrderSubmitted()` 在同一事务中执行。若通知发送失败（DB 错误、网络超时），异常传播回 `submitOrder()`，导致整个订单提交事务回滚。

**影响域**:
- **订单提交** → 通知失败 → 订单未创建
- **设计师分配** → 通知失败 → 分配记录回滚
- **生产卡创建** → 通知失败 → 流转卡创建回滚

违背了事件驱动解耦原则，辅助功能（通知）故障影响核心业务。

**修复建议**（二选一）:

**方案 A**（推荐）：事务提交后触发
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onOrderSubmitted(OrderSubmittedEvent event) {
    // 父事务已提交，通知失败不影响订单创建
}
```

**方案 B**：异步执行
```java
@Async
@EventListener
public void onOrderSubmitted(OrderSubmittedEvent event) {
    // 独立线程，需确保 @EnableAsync 已配置
}
```

---

#### 问题 3：`batchMarkRead` 空列表参数误触发全部标记已读

**文件路径**: `NotificationServiceImpl.java:139-145`

**问题描述**:
```java
if (Boolean.TRUE.equals(markAll) || CollectionUtils.isEmpty(ids)) {
    baseMapper.markAllRead(receiverId, category);  // 标记全部
} else {
    baseMapper.batchMarkRead(ids, receiverId);
}
```

当前端传入 `markAll=false` 且 `ids=[]`（空数组）时，`CollectionUtils.isEmpty(ids)` 为 `true`，进入 `markAllRead` 分支，用户所有未读消息被误标记。

**影响域**:
- 前端逻辑缺陷（未选中任何消息点击"批量已读"）可能清空用户所有未读状态
- 数据不可逆恢复

**修复建议**:
```java
if (Boolean.TRUE.equals(markAll)) {
    baseMapper.markAllRead(receiverId, category);
} else if (CollectionUtils.isEmpty(ids)) {
    log.warn("批量标记已读参数异常: ids 为空且 markAll=false, receiverId={}", receiverId);
    return;  // 或抛出 BusinessException
} else {
    baseMapper.batchMarkRead(ids, receiverId);
}
```

---

### 2.2 High 高危问题

#### 问题 4：`processingCenterId` 为 null 导致 NPE

**文件路径**: `NotificationEventListener.java:146`

**问题描述**:
```java
Map<Long, List<ProductionRecordEntity>> byCenterId = records.stream()
    .collect(Collectors.groupingBy(ProductionRecordEntity::getProcessingCenterId));
```

`Collectors.groupingBy` 不允许 null key，若某条流转卡的 `processingCenterId` 为 null（数据异常），抛出 `NullPointerException`，整批生产卡通知失败。

**影响域**:
- 设计完成后生产通知全部丢失
- 异常传播到 `DesignCompletedListener`，可能影响流转卡创建流程

**修复建议**:
```java
Map<Long, List<ProductionRecordEntity>> byCenterId = records.stream()
    .filter(r -> r.getProcessingCenterId() != null)
    .collect(Collectors.groupingBy(ProductionRecordEntity::getProcessingCenterId));

if (records.size() != byCenterId.values().stream().mapToInt(List::size).sum()) {
    log.error("流转卡 processingCenterId 为 null，已跳过: recordIds={}", 
        records.stream().filter(r -> r.getProcessingCenterId() == null)
               .map(ProductionRecordEntity::getId).toList());
}
```

---

#### 问题 5：`newDesignerId` 为 null 导致数据库约束违反

**文件路径**: `NotificationEventListener.java:91`

**问题描述**:
```java
notificationService.send(event.getNewDesignerId(), NotificationDTO.builder()...);
```

若 `DesignerAssignedEvent.newDesignerId` 为 null（事件发布处逻辑缺陷），`send(null, dto)` → `send(singletonList(null), dto)` → 创建 `receiver_id=null` 的记录，违反 NOT NULL 约束，抛出 SQL 异常。

**影响域**:
- 设计师分配失败（若监听器在事务内）
- 通知丢失

**修复建议**:
```java
@EventListener
public void onDesignerAssigned(DesignerAssignedEvent event) {
    if (event.getNewDesignerId() == null) {
        log.error("DesignerAssignedEvent.newDesignerId 为 null，跳过通知: orderId={}", event.getOrderId());
        return;
    }
    // ...
}
```

---

#### 问题 6：事件监听器无异常隔离，单点故障影响全局

**文件路径**: `NotificationEventListener.java` 全部方法

**问题描述**:
所有监听器方法无 `try-catch` 包裹，任何异常（数据库连接失败、NPE、业务逻辑错误）直接传播到事件发布者。结合问题 2（同步执行在父事务中），单个通知失败导致核心业务回滚。

**影响域**:
- 通知模块的任何 bug 都成为核心业务的单点故障
- 生产环境通知数据库短暂不可用（如主从切换）期间，订单提交、设计分配等全部失败

**修复建议**:
```java
@TransactionalEventListener(phase = AFTER_COMMIT)  // 配合问题 2 修复
public void onOrderSubmitted(OrderSubmittedEvent event) {
    try {
        log.info("收到订单提交事件: ...");
        // 原有逻辑
    } catch (Exception e) {
        log.error("订单提交通知发送失败: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
        // 不向外抛出，保证父业务不受影响
    }
}
```


### 2.3 Medium 中等问题

#### 问题 7：WebSocket 会话替换未关闭旧连接

**文件路径**: `WebSocketSessionManager.java:20-22`

**问题描述**:
```java
public void add(Long userId, WebSocketSession session) {
    sessions.put(userId, session);  // 直接覆盖，旧 session 未关闭
}
```

同一用户从不同浏览器/标签页连接时，旧会话被静默替换但未关闭，导致连接泄漏。

**影响域**:
- 服务器资源浪费（僵尸连接占用内存、文件描述符）
- 旧会话永不接收消息但仍占用服务端资源

**修复建议**:
```java
public void add(Long userId, WebSocketSession session) {
    WebSocketSession oldSession = sessions.put(userId, session);
    if (oldSession != null && oldSession.isOpen()) {
        try {
            oldSession.close(CloseStatus.NORMAL.withReason("新会话已建立"));
            log.info("关闭旧 WebSocket 会话: userId={}", userId);
        } catch (IOException e) {
            log.warn("关闭旧会话失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
```

---

#### 问题 8：WebSocket 无 AUTH 超时机制

**文件路径**: `NotificationWebSocketHandler.java:32-59`

**问题描述**:
客户端连接后若不发送 AUTH 帧，会话永久保持 `OPEN` 状态但未关联 `userId`，无法被 `sessionManager` 管理，形成资源泄漏。

**影响域**:
- 恶意客户端可耗尽服务器连接数
- 正常客户端网络异常也可能触发（连接成功但 AUTH 帧丢失）

**修复建议**:
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    // 30秒内必须完成 AUTH，否则关闭连接
    session.getAttributes().put("authDeadline", System.currentTimeMillis() + 30_000);
}

@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Long deadline = (Long) session.getAttributes().get("authDeadline");
    if (deadline != null && System.currentTimeMillis() > deadline) {
        session.close(CloseStatus.POLICY_VIOLATION.withReason("AUTH timeout"));
        return;
    }
    // 原有逻辑...
    if (AUTH.equals(type)) {
        // AUTH 成功后移除 deadline
        session.getAttributes().remove("authDeadline");
        // ...
    }
}
```

或使用 Spring `@Scheduled` 定期清理未认证连接。

---

#### 问题 9：`isConfirmed` 字段无法查询，离线弹窗功能受阻

**文件路径**: `MessageQueryDTO.java`

**问题描述**:
需求文档要求"用户登录后拉取 `isConfirmed=0` 的 POPUP 消息逐个弹窗"，但 `MessageQueryDTO` 无 `isConfirmed` 参数，前端无法过滤未确认弹窗。

**影响域**:
- 离线弹窗功能无法实现，或需前端拉取所有未读消息后客户端过滤（性能差）
- 功能缺失

**修复建议**:
```java
@Data
public class MessageQueryDTO {
    private String category;
    private Integer isRead;
    private Integer isConfirmed;  // 新增
    private String messageType;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
```

并在 `NotificationServiceImpl.listMessages()` 中补充查询条件：
```java
.eq(query.getIsConfirmed() != null, NotificationMessageEntity::getIsConfirmed, query.getIsConfirmed())
```

---

#### 问题 10：分页参数无校验，可能导致性能问题

**文件路径**: `MessageQueryDTO.java`, `NotificationController.java:22-25`

**问题描述**:
`pageSize` 无上限校验，客户端可传 `pageSize=99999`，导致：
- 数据库单次查询大量记录
- OOM 风险
- 慢查询影响其他用户

**影响域**:
- 恶意攻击或前端 bug 可拖垮服务
- 生产环境稳定性风险

**修复建议**:
```java
@Data
public class MessageQueryDTO {
    private String category;
    private Integer isRead;
    private String messageType;
    
    @Min(1)
    private Integer pageNum = 1;
    
    @Min(1)
    @Max(100)
    private Integer pageSize = 20;
}
```

并在 Controller 或全局参数校验器中启用 `@Valid`：
```java
public Result<IPage<MessageVO>> listMessages(@RequestBody @Valid MessageQueryDTO query) {
```

---

#### 问题 11：`title` 字段未校验非空

**文件路径**: `NotificationServiceImpl.java:167-176`

**问题描述**:
`buildEntity()` 直接复制 `dto.getTitle()`，但数据库列 `title VARCHAR(200) NOT NULL`。若事件发布处传入 `title=null`，数据库 INSERT 失败。

**影响域**:
- 运行时 SQL 异常
- 通知丢失（若在事务内导致业务回滚更严重）

**修复建议**:
在 `NotificationDTO` 上添加校验注解（不推荐，因为内部 DTO 通常不校验），或在 `buildEntity` 前检查：
```java
if (dto.getTitle() == null || dto.getTitle().isBlank()) {
    log.error("通知 title 为空，跳过发送: category={}, bizId={}", dto.getCategory(), dto.getBizId());
    return Collections.emptyList();  // 或在 send() 入口校验
}
```

---

#### 问题 12：`confirm` 接口未校验消息类型

**文件路径**: `NotificationServiceImpl.java:148-156`

**问题描述**:
`confirm()` 方法对所有消息类型生效，包括 `MESSAGE` 类型（语义上不需要"确认"）。

**影响域**:
- 功能语义不严谨
- 前端误调用导致数据不一致（MESSAGE 类型被标记为 `isConfirmed=1`）

**修复建议**:
在 `LambdaUpdateWrapper` 中添加类型检查：
```java
update(new LambdaUpdateWrapper<NotificationMessageEntity>()
    .eq(NotificationMessageEntity::getId, id)
    .eq(NotificationMessageEntity::getReceiverId, receiverId)
    .eq(NotificationMessageEntity::getMessageType, MessageTypeEnum.POPUP.getCode())  // 新增
    .set(...));
```

或在 Service 层校验后抛出异常：
```java
NotificationMessageEntity msg = getById(id);
if (!MessageTypeEnum.POPUP.getCode().equals(msg.getMessageType())) {
    throw new BusinessException("仅 POPUP 类型消息可确认");
}
```

---

### 2.4 Low 低优先级问题

#### 问题 13：`BatchReadDTO` 缺少类级别 Javadoc

**文件路径**: `BatchReadDTO.java`

**问题描述**:
无类注释，无法从代码快速理解 `markAll` 与 `ids`/`category` 的交互逻辑。

**修复建议**:
```java
/**
 * 批量标记已读 DTO
 * 三种模式：
 * 1. markAll=true, category=null → 标记该用户所有未读消息
 * 2. markAll=true, category≠null → 标记该分类下所有未读消息
 * 3. markAll=false, ids≠empty → 标记指定 ID 列表
 */
@Data
public class BatchReadDTO {
```

---

#### 问题 14：`deleteMessage` 不区分"已删除"与"不存在"

**文件路径**: `NotificationServiceImpl.java:160-165`

**问题描述**:
`remove()` 的 WHERE 子句匹配 0 行时静默成功，返回 `Result.success()`，前端无法区分消息已被删除还是从未存在。

**影响域**:
- 用户体验细节（重复点击删除返回成功，但逻辑上应提示"消息不存在"）
- 对业务功能无实质影响

**修复建议**:
```java
boolean success = remove(new LambdaQueryWrapper<NotificationMessageEntity>()
    .eq(NotificationMessageEntity::getId, id)
    .eq(NotificationMessageEntity::getReceiverId, receiverId));
if (!success) {
    throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "消息不存在或无权限");
}
```

---

#### 问题 15：`OrderCancelledEvent` 未被通知模块监听

**文件路径**: `NotificationEventListener.java`

**问题描述**:
`OrderCancelledEvent` 在 common 模块已定义，production 模块的 `OrderCancelledListener` 已监听，但通知模块未处理该事件。若第一阶段设计不包含订单取消通知，应在文档中明确说明。

**影响域**:
- 功能缺失（若需求要求通知订单相关人员）
- 或是有意为之但未记录

**修复建议**:
若需要通知，添加监听器：
```java
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onOrderCancelled(OrderCancelledEvent event) {
    // 通知订单关联的所有处理人（设计师、生产员等）
}
```

若第一阶段不做，在需求文档或代码中注释说明。

---

## 三、需业务/线上数据确认的风险


### 3.1 数据完整性风险

#### 风险 1：试用订单的 `hospitalId` 可能为 null

**文件路径**: `NotificationEventListener.java:52`, `OrderMainServiceImpl.java:601`

**风险描述**:
`OrderSubmittedEvent` 携带 `entity.getHospitalId()`。若数据库中试用订单的 `hospitalId` 列允许 NULL（数据质量问题），`NotificationContext.ofHospital(null)` 导致 `resolveUserIds` 返回空列表，区域管理员收不到通知。

**确认方式**:
```sql
SELECT COUNT(*) FROM order_main 
WHERE business_type = '11.3' AND hospital_id IS NULL AND is_deleted = 0;
```

**建议**:
- 若存在：修复历史数据，添加 NOT NULL 约束
- 若不存在：在 `onOrderSubmitted` 开头加断言日志

---

#### 风险 2：角色编码与枚举不一致

**文件路径**: `NotificationServiceImpl.java:54`

**风险描述**:
`findDataScopeTypeByRoleCode` 查询 `sys_role` 表。若表中角色编码与 `RoleCodeEnum` 不一致（如代码使用 `production-worker` 但数据库是 `PRODUCTION_WORKER`），返回 null，`getByCodeOrDefault(null)` 返回 `SELF`，通知静默失败。

**确认方式**:
```sql
SELECT role_code FROM sys_role 
WHERE role_code IN ('production-worker', 'production-manager', 'designer-manager', 'regional-manager');
```

对比 `RoleCodeEnum` 的枚举值，确认大小写、连字符一致。

---

#### 风险 3：`processingCenterId` 数据质量

**已在问题 4 覆盖**，需执行 SQL 确认是否存在 null 值：
```sql
SELECT COUNT(*) FROM production_record 
WHERE processing_center_id IS NULL AND is_deleted = 0;
```

---

### 3.2 业务逻辑确认风险

#### 风险 4：`DesignerAssignedEvent` 是否保证 `newDesignerId` 非空

**文件路径**: `DesignerAssignmentServiceImpl.java:144, 199`

**风险描述**:
代码审查确认事件发布处从 `designer.getId()` 获取，理论上非 null。但需确认业务流程是否可能出现"取消分配"（将 `designerId` 设为 null）场景。

**建议**: 与业务确认是否存在"解除设计师分配"功能。若有，事件发布处应校验。

---

#### 风险 5：`OrderCancelledEvent` 是否需要通知

**已在问题 15 覆盖**。需业务明确第一阶段范围。

---

### 3.3 前端对接风险

#### 风险 6：`jumpUrl` 路径与前端路由不一致

**文件路径**: `NotificationEventListener.java` 多处硬编码

**风险描述**:
通知中的跳转路径：
- `/order/detail/{id}`
- `/design/list`
- `/production/record/{id}`

这些路径未经前端确认，可能与实际 Vue Router 配置不符，导致点击跳转 404。

**建议**: 提供路径清单给前端，确认后集中定义为常量类。

---

#### 风险 7：`MessageVO.bizData` 是 String 但需求文档示例是 Object

**文件路径**: `MessageVO.java:22`

**风险描述**:
需求文档的 API 示例中 `bizData` 显示为 JSON 对象，但实际返回的是 String。前端需 `JSON.parse(bizData)` 处理。

**建议**: 在接口文档明确标注或改为返回 `Map<String, Object>`（需反序列化）。

---

### 3.4 安全性风险

#### 风险 8：`rejectReason` 未转义可能导致 XSS

**文件路径**: `NotificationEventListener.java:79`

**风险描述**:
```java
.content("驳回原因：" + event.getRejectReason())
```

`rejectReason` 是用户输入，若前端渲染消息内容时未转义（如使用 `v-html`），可能引发 XSS。

**建议**: 
- 后端：在 `buildEntity` 时对 `content` 字段 HTML 转义（如使用 `HtmlUtils.htmlEscape`）
- 前端：使用 `v-text` 或框架默认转义，避免 `v-html`

---

### 3.5 性能风险

#### 风险 9：高并发下 WebSocket 推送阻塞事务

**文件路径**: `NotificationServiceImpl.java:101`

**风险描述**:
`send(List<100>, dto)` 同步执行 100 次 `session.sendMessage()`。若某个客户端网络慢，整个推送循环阻塞，事务持有时间延长（配合问题 1，事务未提交期间推送）。

**建议**: 
- 短期：修复问题 1（事务提交后推送）
- 长期：考虑推送改为异步队列（如 Spring `@Async` + 线程池）

---

## 四、审查总结与修复优先级

### 4.1 修复优先级

| 优先级 | 问题编号 | 预计修复时间 | 阻塞上线 |
|--------|---------|------------|---------|
| **P0**（立即修复） | 问题 2, 6 | 1h | **是** |
| **P1**（上线前必须修复） | 问题 1, 3, 4, 5 | 2h | **是** |
| **P2**（首个迭代修复） | 问题 7, 8, 9, 10, 11 | 3h | 否 |
| **P3**（后续优化） | 问题 12, 13, 14, 15 | 1h | 否 |

**P0 说明**: 问题 2 和 6 组合导致通知失败直接回滚核心业务，**必须先修复再测试**。

---

### 4.2 核心架构建议

1. **事件监听解耦**（问题 2）:
   ```java
   @TransactionalEventListener(phase = AFTER_COMMIT)
   ```
   
2. **异常隔离**（问题 6）:
   ```java
   try { /* 通知逻辑 */ } catch (Exception e) { log.error(...); }
   ```

3. **事务边界修正**（问题 1）:
   ```java
   TransactionSynchronizationManager.registerSynchronization(...)
   ```

---

### 4.3 接口功能覆盖评估

| 需求功能 | 接口实现 | 覆盖度 | 备注 |
|---------|---------|-------|------|
| 消息列表查询 | `POST /messages` | ✅ 完整 | 需补充 `isConfirmed` 参数 |
| 未读数量 | `GET /unread-count` | ✅ 完整 | - |
| 标记已读 | `PUT /{id}/read` | ✅ 完整 | - |
| 批量已读 | `PUT /batch-read` | ⚠️ 有缺陷 | 问题 3 |
| 确认弹窗 | `PUT /{id}/confirm` | ✅ 完整 | - |
| 删除消息 | `DELETE /{id}` | ✅ 完整 | - |
| 离线弹窗拉取 | **缺失** | ❌ 无法实现 | 问题 9 |
| 实时 WebSocket 推送 | 已实现 | ⚠️ 有缺陷 | 问题 1, 7, 8 |

**结论**: 核心功能基本覆盖，但离线弹窗功能因缺少 `isConfirmed` 查询参数**无法正常工作**。

---

### 4.4 数据权限与操作权限评估

| 操作 | 权限检查 | 评估结果 |
|------|---------|---------|
| 查询消息列表 | `WHERE receiver_id = #{receiverId}` | ✅ 安全 |
| 标记已读 | `WHERE receiver_id = #{receiverId}` | ✅ 安全 |
| 确认消息 | `WHERE receiver_id = #{receiverId}` | ✅ 安全 |
| 删除消息 | `WHERE receiver_id = #{receiverId}` | ✅ 安全 |

**结论**: 所有操作均正确绑定 `receiverId`（从 SaToken 获取），**无越权风险**。

---

### 4.5 代码规范评估

| 维度 | 评分 | 问题 |
|------|------|------|
| 类/方法注释 | 90/100 | 问题 13（BatchReadDTO 缺注释） |
| 行级注释 | 85/100 | 关键逻辑已注释，分支判断可补充 |
| 日志完整性 | 95/100 | 所有关键操作有日志，格式统一 |
| 异常处理 | 60/100 | 问题 6（监听器无异常隔离）、问题 11（title 未校验） |
| 代码优雅度 | 80/100 | 已使用 BeanUtils、Stream、常量，但事务边界设计有缺陷 |

---

## 五、修复检查清单

上线前必须完成：

- [ ] **问题 2**: 所有 `@EventListener` 改为 `@TransactionalEventListener(AFTER_COMMIT)`
- [ ] **问题 6**: 所有监听器方法加 `try-catch` 包裹
- [ ] **问题 1**: `send(List, dto)` 使用事务提交后回调推送 WebSocket
- [ ] **问题 3**: 修复 `batchMarkRead` 空列表逻辑
- [ ] **问题 4**: `onProductionCardsCreated` 过滤 null `processingCenterId`
- [ ] **问题 5**: `onDesignerAssigned` 校验 `newDesignerId` 非空
- [ ] **问题 9**: `MessageQueryDTO` 补充 `isConfirmed` 字段
- [ ] **问题 10**: 分页参数加 `@Min/@Max` 校验
- [ ] **风险 6**: 前端确认 `jumpUrl` 路径正确性
- [ ] **风险 1-3**: SQL 确认数据完整性（hospitalId/role_code/processingCenterId）

建议修复后：
- [ ] **问题 7**: WebSocket 会话替换关闭旧连接
- [ ] **问题 8**: WebSocket AUTH 超时机制
- [ ] **风险 8**: `rejectReason` HTML 转义

---

**审查完成时间**: 2026-06-18  
**下一步**: 按优先级修复问题，修复后进行集成测试和压测。
