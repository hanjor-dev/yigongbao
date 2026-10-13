# 医工宝消息通知系统设计方案

**文档版本**: 1.4  
**创建日期**: 2026-06-03  
**更新日期**: 2026-06-03  
**设计方案**: 完整配置化方案（方案二）  
**状态**: 待最终确认

---

## 一、需求概述

### 1.1 背景

医工宝系统需要一套完整的消息通知系统,在订单流转时能够实时推送消息或通知到目标用户或角色对应的批量用户。

### 1.2 核心目标

- **订单流转触发**：订单状态变更时自动触发消息/通知
- **双通道支持**：消息（站内信持久化）+ 通知（WebSocket实时推送）
- **完全可配置**：触发规则、接收者规则、消息/通知组合策略均可配置
- **模板化内容**：支持消息模板和占位符替换
- **完整管理**：发送记录、阅读统计、失败重试机制

### 1.3 功能需求

#### 消息系统（站内信）
- 消息列表查询（分页）、标记已读/未读、未读消息数量统计
- 消息删除、消息详情查看、按类型/时间筛选
- 全部标记已读、消息分类管理

#### 通知系统（实时推送）
- WebSocket实时推送、离线消息缓存
- 用户上线后自动补发、推送失败重试

#### 管理功能
- 消息模板管理（CRUD）、通知规则管理（CRUD）
- 发送记录查询、阅读统计分析

---

## 二、系统架构设计

### 2.1 模块结构

```
yigongbao-module-notification/
├── entity/              # UserMessageEntity, NotificationRuleEntity, MessageTemplateEntity, MessageSendLogEntity
├── enums/               # MessageTypeEnum, TargetTypeEnum
├── service/             # IUserMessageService, INotificationRuleService, NotificationService
├── websocket/           # NotificationWebSocketHandler, UserConnectionManager
├── controller/          # UserMessageController, NotificationRuleController
├── mapper/              # 对应Mapper
└── integration/         # FlowNotificationListener
```

### 2.2 核心流程

```
订单流转 → FlowFacade.executeAction() → 异步触发 NotificationService.sendByFlowAction()
  ↓
1. 查询该action的启用规则
2. 解析接收者 + 渲染消息内容
3. 发送消息（user_message表）和/或 WebSocket推送
4. 记录发送日志
```

---

## 三、数据库设计

**说明**：所有Entity类继承`BaseEntity`，自动包含以下公共字段：
- `id` (BIGINT): 主键
- `create_time` (DATETIME): 创建时间（自动填充）
- `update_time` (DATETIME): 更新时间（自动填充）
- `create_by` (BIGINT): 创建人ID
- `update_by` (BIGINT): 更新人ID
- `is_deleted` (TINYINT): 逻辑删除标记（@TableLogic）

下方建表语句为简化展示，仅列出核心业务字段。

### 3.1 用户消息表（user_message）

```sql
CREATE TABLE user_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(50) NOT NULL COMMENT '消息分类',
    order_id BIGINT COMMENT '关联订单ID',
    flow_action VARCHAR(50) COMMENT '触发的流转动作',
    is_read TINYINT DEFAULT 0,
    read_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_user_read (user_id, is_read, create_time),
    INDEX idx_msg_type_user (message_type, user_id, create_time),
    INDEX idx_order (order_id)
) ENGINE=InnoDB COMMENT='用户消息表';
```

### 3.2 消息模板表（message_template）

```sql
CREATE TABLE message_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(100) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(200) NOT NULL,
    title VARCHAR(200) NOT NULL COMMENT '支持占位符：{orderNo}',
    content TEXT NOT NULL,
    params_config JSON COMMENT '["orderNo","hospitalName"]',
    message_type VARCHAR(50) NOT NULL,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE INDEX uk_code ((CASE WHEN is_deleted = 0 THEN template_code ELSE NULL END))
) ENGINE=InnoDB COMMENT='消息模板表';
```

### 3.3 通知规则表（notification_rule）

```sql
CREATE TABLE notification_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(100) NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    flow_action VARCHAR(50) NOT NULL COMMENT 'FlowActionEnum',
    send_message TINYINT DEFAULT 1 COMMENT '是否发送消息',
    send_notification TINYINT DEFAULT 1 COMMENT '是否发送通知',
    target_type VARCHAR(50) NOT NULL COMMENT '接收者类型：11种枚举值（见4.1节）',
    target_config JSON COMMENT '扩展配置（预留，当前版本可为空）',
    template_id BIGINT NOT NULL,
    priority INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE INDEX uk_code ((CASE WHEN is_deleted = 0 THEN rule_code ELSE NULL END)),
    INDEX idx_action (flow_action, status)
) ENGINE=InnoDB COMMENT='通知规则表';
```

### 3.4 发送记录表（message_send_log）

```sql
CREATE TABLE message_send_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    flow_action VARCHAR(50) NOT NULL,
    order_id BIGINT,
    target_type VARCHAR(50) NOT NULL,
    target_user_ids JSON NOT NULL,
    total_count INT DEFAULT 0,
    message_success_count INT DEFAULT 0,
    notification_success_count INT DEFAULT 0,
    fail_count INT DEFAULT 0,
    send_time DATETIME NOT NULL,
    duration_ms INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    INDEX idx_rule (rule_id, send_time),
    INDEX idx_order (order_id)
) ENGINE=InnoDB COMMENT='消息发送记录表';
```

---

## 四、核心设计要点

### 4.1 核心枚举

**MessageTypeEnum**: ORDER, DESIGN, QC, PRODUCTION, SYSTEM  

**TargetTypeEnum**（11种接收者类型）：

| 枚举值 | 说明 | 业务线 |
|--------|------|--------|
| `SALES_OWNER` | 订单业务员 | 业务线 |
| `SALES_OWNER_WITH_DEALER_ADMIN` | 订单业务员+经销商业务管理员 | 业务线 |
| `SALES_OWNER_WITH_ALL_ADMIN` | 订单业务员+经销商业务管理员+公司管理员 | 业务线 |
| `DEALER_SALES_ADMIN` | 订单经销商的业务管理员 | 业务线 |
| `ORDER_DESIGNER` | 订单设计师 | 设计线 |
| `DESIGNER_WITH_ADMIN` | 订单设计师+设计管理员 | 设计线 |
| `DESIGNER_WITH_ALL_ADMIN` | 订单设计师+设计管理员+公司管理员 | 设计线 |
| `ALL_DESIGN_ADMIN` | 所有设计管理员 | 设计线 |
| `ALL_PRODUCTION` | 所有生产员 | 生产线 |
| `PRODUCTION_WITH_ADMIN` | 所有生产员+生产管理员 | 生产线 |
| `PRODUCTION_WITH_ALL_ADMIN` | 所有生产员+生产管理员+公司管理员 | 生产线 |

**角色编码约定**（需在sys_role表中预定义）：

| 角色名称 | roleCode | 说明 |
|---------|---------|------|
| 业务员 | `SALES` | 订单创建人默认角色 |
| 业务管理员 | `SALES_ADMIN` | 经销商机构内的业务管理 |
| 设计师 | `DESIGNER` | 订单设计人员 |
| 设计管理员 | `DESIGN_ADMIN` | 设计部门管理员 |
| 生产员 | `PRODUCTION` | 生产执行人员 |
| 生产管理员 | `PRODUCTION_ADMIN` | 生产部门管理员 |
| 公司管理员 | `COMPANY_ADMIN` | 公司级管理员（跨部门） |

### 4.2 接收者解析逻辑（含降级处理）

```java
public List<Long> resolveTargetUsers(NotificationRuleEntity rule, Long orderId) {
    TargetTypeEnum targetType = TargetTypeEnum.valueOf(rule.getTargetType());
    Set<Long> targetUsers = new HashSet<>();  // 使用Set自动去重
    
    // 获取订单信息
    OrderEntity order = orderService.getById(orderId);
    if (order == null) {
        log.warn("订单不存在，无法解析接收者: orderId={}", orderId);
        return Collections.emptyList();
    }
    
    switch (targetType) {
        //========== 业务线（1-4） ==========
        case SALES_OWNER:
            targetUsers.add(order.getCreateBy());
            break;
            
        case SALES_OWNER_WITH_DEALER_ADMIN:
            targetUsers.add(order.getCreateBy());
            Long dealerOrgId = userService.getById(order.getCreateBy()).getOrgId();
            targetUsers.addAll(userService.listUserIdsByRoleAndOrg("SALES_ADMIN", dealerOrgId));
            break;
            
        case SALES_OWNER_WITH_ALL_ADMIN:
            targetUsers.add(order.getCreateBy());
            Long dealerOrgId2 = userService.getById(order.getCreateBy()).getOrgId();
            targetUsers.addAll(userService.listUserIdsByRoleAndOrg("SALES_ADMIN", dealerOrgId2));
            targetUsers.addAll(userService.listUserIdsByRole("COMPANY_ADMIN"));
            break;
            
        case DEALER_SALES_ADMIN:
            Long dealerOrgId3 = userService.getById(order.getCreateBy()).getOrgId();
            targetUsers.addAll(userService.listUserIdsByRoleAndOrg("SALES_ADMIN", dealerOrgId3));
            break;
            
        //========== 设计线（5-8） ==========
        case ORDER_DESIGNER:
            if (order.getDesignerId() != null) {
                targetUsers.add(order.getDesignerId());
            }
            break;
            
        case DESIGNER_WITH_ADMIN:
            if (order.getDesignerId() != null) {
                targetUsers.add(order.getDesignerId());
            }
            targetUsers.addAll(userService.listUserIdsByRole("DESIGN_ADMIN"));
            break;
            
        case DESIGNER_WITH_ALL_ADMIN:
            if (order.getDesignerId() != null) {
                targetUsers.add(order.getDesignerId());
            }
            targetUsers.addAll(userService.listUserIdsByRole("DESIGN_ADMIN"));
            targetUsers.addAll(userService.listUserIdsByRole("COMPANY_ADMIN"));
            break;
            
        case ALL_DESIGN_ADMIN:
            targetUsers.addAll(userService.listUserIdsByRole("DESIGN_ADMIN"));
            break;
            
        //========== 生产线（9-11） ==========
        case ALL_PRODUCTION:
            targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION"));
            break;
            
        case PRODUCTION_WITH_ADMIN:
            targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION"));
            targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION_ADMIN"));
            break;
            
        case PRODUCTION_WITH_ALL_ADMIN:
            targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION"));
            targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION_ADMIN"));
            targetUsers.addAll(userService.listUserIdsByRole("COMPANY_ADMIN"));
            break;
    }
    
    // 降级策略：根据类型判断
    if (targetUsers.isEmpty()) {
        if (isOrderRelatedType(targetType)) {
            // 订单相关类型：降级为订单创建人
            log.warn("接收者解析为空，降级为通知订单创建人: orderId={}, ruleId={}, targetType=", 
                orderId, rule.getId(), targetType);
            targetUsers.add(order.getCreateBy());
        } else {
            // 全局角色类型：记录错误，不降级
            log.error("全局角色类型解析为空，可能是角色配置缺失: ruleId={}, targetType={}", 
                rule.getId(), targetType);
        }
    }
    
    return new ArrayList<>(targetUsers);
}

/**
 * 判断是否为订单相关类型
 */
private boolean isOrderRelatedType(TargetTypeEnum type) {
    return type == TargetTypeEnum.SALES_OWNER 
        || type == TargetTypeEnum.SALES_OWNER_WITH_DEALER_ADMIN
        || type == TargetTypeEnum.SALES_OWNER_WITH_ALL_ADMIN
        || type == TargetTypeEnum.DEALER_SALES_ADMIN
        || type == TargetTypeEnum.ORDER_DESIGNER
        || type == TargetTypeEnum.DESIGNER_WITH_ADMIN
        || type == TargetTypeEnum.DESIGNER_WITH_ALL_ADMIN;
}
```

**依赖的UserService方法**：
```java
// 根据角色查询所有用户ID
List<Long> listUserIdsByRole(String roleCode);

// 根据角色和机构查询用户ID
List<Long> listUserIdsByRoleAndOrg(String roleCode, Long orgId);
```

**scope参数说明**：

| scope值 | 说明 | 过滤逻辑 |
|---------|------|----------|
| `hospitals` | 当前订单所属医院范围 | 只查询有该医院数据权限的用户 |
| `org` | 当前订单所属机构范围 | 只查询同机构下的用户 |
| `all` | 全局范围 | 查询所有拥有该角色的用户 |

### 4.3 模板渲染（含参数校验）

```java
public class MessageRenderResult {
    private String title;
    private String content;
}

public MessageRenderResult renderContent(Long templateId, Map<String, Object> params) {
    MessageTemplateEntity template = getById(templateId);
    if (template == null) {
        throw new BusinessException(ErrorCodeEnum.TEMPLATE_NOT_FOUND);
    }
    
    // 校验必需参数
    List<String> requiredParams = JSONUtil.toList(template.getParamsConfig(), String.class);
    for (String param : requiredParams) {
        if (!params.containsKey(param)) {
            throw new BusinessException(ErrorCodeEnum.TEMPLATE_PARAM_MISSING, param);
        }
    }
    
    // 占位符替换（title和content）
    String title = template.getTitle();
    String content = template.getContent();
    for (Map.Entry<String, Object> entry : params.entrySet()) {
        String placeholder = "{" + entry.getKey() + "}";
        String value = String.valueOf(entry.getValue());
        title = title.replace(placeholder, value);
        content = content.replace(placeholder, value);
    }
    
    return new MessageRenderResult(title, content);
}
```

**安全提示**：如果前端将消息内容渲染为HTML，参数值应进行HTML转义以防止XSS攻击。建议在渲染前使用`HtmlUtil.escape(value)`处理用户输入的参数值。

### 4.4 WebSocket协议（含鉴权）

**连接URL**: `ws://localhost:8080/ws/notification?token={satoken}`  

**token提取方法**:
```java
private String getTokenFromSession(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null) return null;
    
    String query = uri.getQuery();
    if (StrUtil.isBlank(query)) return null;
    
    // 解析query参数: token=xxx
    Map<String, String> params = HttpUtil.decodeParamMap(query, StandardCharsets.UTF_8);
    return params.get("token");
}
```

**连接鉴权**:
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    try {
        // 从URL参数获取token
        String token = getTokenFromSession(session);
        
        // 验证token并获取userId（使用SaToken）
        Long userId = StpUtil.getLoginIdByToken(token);
        
        // 保存连接
        connectionManager.addSession(userId, session);
        
        // 处理离线消息
        notificationService.processOfflineMessages(userId);
        
        log.info("用户连接建立: userId={}, sessionId={}", userId, session.getId());
    } catch (NotLoginException e) {
        log.warn("WebSocket鉴权失败: sessionId={}, error={}", session.getId(), e.getMessage());
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (IOException ex) {
            log.error("关闭未鉴权连接失败", ex);
        }
    }
}
```

**推送消息格式**:
```json
{
  "type": "NOTIFICATION",
  "data": {
    "messageId": 123,
    "title": "您有新的订单",
    "content": "订单【YGB20260603001】已提交",
    "messageType": "ORDER",
    "orderId": 456,
    "timestamp": 1717392718915
  }
}
```

**连接管理**:
- UserConnectionManager维护 userId -> WebSocketSession 映射
- **多连接策略**：同一用户允许多个session并存（多设备/多标签页），推送时向所有session发送
- 用户上线后自动补发Redis中的离线消息
- 推送失败自动保存到Redis离线队列

### 4.5 日志记录规范

**【强制】Controller层禁止输出日志**，所有业务日志由ServiceImpl层负责记录。

**必须记录的日志场景**：

1. **消息发送**（INFO级别）：
```java
log.info("发送消息: orderId={}, action={}, targetUserCount={}, messageCount={}, notificationCount={}", 
    orderId, action, targetUsers.size(), messageSuccessCount, notificationSuccessCount);
```

2. **规则执行失败**（ERROR级别）：
```java
log.error("通知规则执行失败: ruleId={}, orderId={}, action={}", 
    rule.getId(), orderId, action, e);
```

3. **WebSocket推送**（INFO级别）：
```java
log.info("WebSocket推送: userId={}, messageId={}, success={}", userId, messageId, success);
```

4. **接收者解析为空**（WARN级别）：
```java
log.warn("接收者解析为空，降级为通知订单创建人: orderId={}, ruleId={}", orderId, rule.getId());
```

5. **模板渲染失败**（ERROR级别）：
```java
log.error("模板渲染失败: templateId={}, orderId={}, params={}", templateId, orderId, params, e);
```

---

## 五、API接口设计

### 5.1 用户端API

| 接口 | 方法 | 说明 | 返回值 |
|------|------|------|--------|
| `/api/notification/message/list` | POST | 分页查询我的消息 | `Result<IPage<UserMessageVO>>` |
| `/api/notification/message/unread-count` | GET | 获取未读消息数 | `Result<Long>` |
| `/api/notification/message/read/{id}` | PUT | 标记已读 | `Result<Void>` |
| `/api/notification/message/read-batch` | PUT | 批量标记已读 | `Result<Void>` |
| `/api/notification/message/read-all` | PUT | 全部标记已读 | `Result<Void>` |
| `/api/notification/message/{id}` | DELETE | 删除消息（逻辑删除） | `Result<Void>` |

**QueryMessageDTO 定义**:
```java
@Data
public class QueryMessageDTO {
    private String messageType;       // 可选，按类型过滤（ORDER/DESIGN/QC等）
    private Integer isRead;           // 可选，0=未读/1=已读/null=全部
    private LocalDateTime startTime;  // 可选，时间范围起始
    private LocalDateTime endTime;    // 可选，时间范围结束
    private Integer current = 1;      // 当前页码
    private Integer size = 10;        // 每页大小
}
```

### 5.2 管理端API

| 接口 | 方法 | 说明 | 返回值 |
|------|------|------|--------|
| `/api/notification/rule/list` | POST | 分页查询规则 | `Result<IPage<NotificationRuleVO>>` |
| `/api/notification/rule` | POST | 创建规则 | `Result<Long>` |
| `/api/notification/rule/{id}` | PUT | 更新规则 | `Result<Void>` |
| `/api/notification/rule/{id}/status` | PUT | 启用/禁用规则 | `Result<Void>` |
| `/api/notification/template/*` | * | 模板管理接口（类似结构） | - |

**权限控制**：所有管理端接口需添加权限注解
- `/rule` POST: `@RequirePermission("notification:rule:create")`
- `/rule` PUT: `@RequirePermission("notification:rule:update")`
- `/rule/{id}/status` PUT: `@RequirePermission("notification:rule:status")`
- `/rule` 查询: `@RequirePermission("notification:rule:query")`
- 模板接口权限前缀：`notification:template:*`

**模板管理接口**：

| 接口 | 方法 | 说明 | 返回值 |
|------|------|------|--------|
| `/api/notification/template/list` | POST | 分页查询模板 | `Result<IPage<TemplateVO>>` |
| `/api/notification/template` | POST | 创建模板 | `Result<Long>` |
| `/api/notification/template/{id}` | PUT | 更新模板 | `Result<Void>` |
| `/api/notification/template/{id}` | DELETE | 删除模板 | `Result<Void>` |
| `/api/notification/template/{id}/status` | PUT | 启用/禁用模板 | `Result<Void>` |

**发送记录接口**：

| 接口 | 方法 | 说明 | 返回值 |
|------|------|------|--------|
| `/api/notification/log/list` | POST | 分页查询发送记录 | `Result<IPage<SendLogVO>>` |
| `/api/notification/log/stats` | GET | 统计发送情况 | `Result<SendStatsVO>` |

---

## 六、集成点设计

### 6.1 订单流程集成（含事务边界）

**线程池配置**（需在配置类中定义）：
```java
@Configuration
public class NotificationConfig {
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

**集成位置**: `FlowFacadeImpl.executeAction()`

```java
@Autowired
@Qualifier("notificationExecutor")
private Executor notificationExecutor;

@Override
@Transactional(rollbackFor = Exception.class)
public TransitionResult executeAction(Long orderId, FlowActionEnum action, Long operatorId) {
    // 1. 执行流转（主事务）
    TransitionResult result = stateMachineService.executeTransition(orderId, action, operatorId);
    
    // 2. 流转成功后,异步发送通知（使用专用线程池，不在主事务内）
    if (result.isSuccess()) {
        CompletableFuture.runAsync(() -> {
            try {
                notificationService.sendByFlowAction(orderId, action);
            } catch (Exception e) {
                log.error("发送通知失败: orderId={}, action={}", orderId, action, e);
            }
        }, notificationExecutor);
    }
    
    return result;
}
```

**事务边界说明**:

1. **主事务**：订单流转操作在主事务内，确保原子性
2. **通知发送事务**：
   ```java
   @Service
   public class NotificationServiceImpl {
       @Transactional(rollbackFor = Exception.class)
       public void sendByFlowAction(Long orderId, FlowActionEnum action) {
           // 消息持久化在独立事务内
           // 确保消息保存成功
       }
   }
   ```
3. **发送日志事务**：使用独立事务，避免日志记录失败影响消息发送
   ```java
   @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
   public void saveSendLog(MessageSendLogEntity log) {
       // 独立事务记录发送日志
   }
   ```
4. **WebSocket推送**：不在事务内（异步执行），推送失败不回滚消息记录

**集成策略**:
- 异步执行,不阻塞订单流转
- 异常隔离,通知失败不影响订单状态
- 记录发送日志,便于排查问题

---

## 七、技术要点

### 7.1 Redis缓存设计

```
notification:rule:{action}           # 规则缓存,10分钟
notification:conn:user:{userId}      # 用户连接映射
notification:offline:{userId}        # 离线消息队列,最多100条（FIFO）
notification:dedup:{userId}:{ruleId}:{orderId}  # 消息去重,5分钟
```

**离线消息处理策略**:
- 超过100条时，使用Lua脚本保证原子性（LPUSH+LTRIM）
```lua
-- 原子操作：push并保留最新100条
redis.call('LPUSH', KEYS[1], ARGV[1])
redis.call('LTRIM', KEYS[1], 0, 99)
return 1
```
- 用户上线后，LRANGE读取全部离线消息并删除
- 离线消息保留7天TTL，过期自动清理

### 7.2 性能优化

- 规则缓存减少数据库查询（10分钟TTL）
- **缓存失效策略**：NotificationRuleServiceImpl的update/delete/updateStatus方法中，删除对应缓存key
```java
// 示例：更新规则后删除缓存
String cacheKey = "notification:rule:" + rule.getFlowAction();
redisTemplate.delete(cacheKey);
```
- 批量查询用户避免N+1问题
- WebSocket推送使用专用线程池（notificationExecutor）
- 消息去重：`SETNX notification:dedup:{userId}:{ruleId}:{orderId} 1 EX 300`

### 7.3 容错机制

- 推送失败自动保存到离线队列（被动补发：用户上线后自动重发）
- **主动重试策略**：定时任务（如每10分钟）扫描message_send_log表中失败记录，对24小时内的失败记录重试最多3次
```java
@Scheduled(cron = "0 */10 * * * ?")
public void retryFailedNotifications() {
    // 查询24小时内失败次数<3的记录
    // 重新执行推送
    // 更新失败次数或标记为最终失败
}
```
- 用户重连后自动补发离线消息
- 接收者解析失败时降级为通知创建人

---

## 八、初始化数据示例

```sql
-- 消息模板
INSERT INTO message_template (template_code, template_name, title, content, params_config, message_type, status) VALUES
('SUBMIT_ORDER', '提交订单通知', '您有新的订单', '订单【{orderNo}】已提交,医院：{hospitalName},请及时处理', '["orderNo","hospitalName"]', 'ORDER', 1);

-- 通知规则示例
INSERT INTO notification_rule (rule_code, rule_name, flow_action, send_message, send_notification, target_type, target_config, template_id, status) VALUES
('SUBMIT_ORDER_TO_DESIGNER', '提交订单通知设计师和管理员', 'SUBMIT_ORDER', 1, 1, 'DESIGNER_WITH_ADMIN', NULL, 1, 1),
('DESIGN_PASS_TO_SALES', '设计审核通过通知业务员', 'DESIGN_REVIEW_PASS', 1, 1, 'SALES_OWNER', NULL, 1, 1),
('QC_PASS_TO_PRODUCTION', '质检合格通知生产部门', 'QC_PASS', 1, 1, 'PRODUCTION_WITH_ADMIN', NULL, 1, 1);
```

---

## 九、实施建议

### 9.1 分阶段实施

**阶段一（核心功能,2周）**:
- 数据库表创建
- 消息系统（持久化、查询、已读管理）
- 基础通知规则（硬编码或简单配置）

**阶段二（WebSocket推送,1周）**:
- WebSocket连接管理
- 实时通知推送
- 离线消息补发

**阶段三（完整配置化,2周）**:
- 规则配置管理界面
- 模板配置管理界面
- 发送记录统计分析

### 9.2 测试要点

- 订单流转触发通知的准确性
- WebSocket连接稳定性和断线重连
- 大量用户并发推送的性能
- 离线消息补发的可靠性
- 规则配置的灵活性验证

### 9.3 监控指标

- 消息发送成功率
- WebSocket连接数和推送耗时
- 离线消息积压数量
- 规则执行失败次数

---

**修订记录**：
- v1.1 (2026-06-03): 添加BaseEntity公共字段说明、日志记录规范
- v1.2 (2026-06-03): 添加接收者解析降级处理、模板参数校验、WebSocket鉴权、事务边界说明、API返回值、消息去重机制
- v1.3 (2026-06-03): 自审查修复15个问题（3个Critical、5个Major、7个Minor）：空值检查、并发安全、缓存失效、模板渲染完整性、WebSocket实现细节、线程池配置、API完整性、重试机制、索引优化
- v1.4 (2026-06-03): 重新定义接收者类型体系，从4种通用类型扩展为11种业务场景驱动的组合类型（业务线4种、设计线4种、生产线3种），简化target_config配置，新增7种角色编码约定，优化降级策略（区分订单相关/全局类型）
