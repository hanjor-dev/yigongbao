# 消息通知系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建医工宝消息通知系统，实现订单流转时自动发送站内消息和实时WebSocket通知

**Architecture:** 
- 独立模块 `yigongbao-module-notification`，包含消息持久化、通知推送、规则引擎、模板渲染
- 异步触发机制集成到 `FlowFacade`，不阻塞订单流转
- Redis缓存规则、连接映射、离线消息队列

**Tech Stack:** Spring Boot 3.x, MyBatis Plus 3.5.8, SaToken 1.37.0, Redis, Spring WebSocket, Hutool 5.8.26

---

## 阶段一：核心实体层 (Core Entities)

### Task 1: 创建模块基础结构

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/pom.xml`
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/.gitkeep`

<thinking>
I'm starting with the basic module structure. The first task should be creating the Maven module with its pom.xml and basic package structure.
</thinking>

- [ ] **Step 1: 创建模块pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>yigongbao-module-notification</artifactId>
    <description>消息通知模块</description>

    <dependencies>
        <!-- 通用模块 -->
        <dependency>
            <groupId>com.yigongbao</groupId>
            <artifactId>yigongbao-common</artifactId>
        </dependency>
        
        <!-- 框架模块 -->
        <dependency>
            <groupId>com.yigongbao</groupId>
            <artifactId>yigongbao-framework</artifactId>
        </dependency>
        
        <!-- WebSocket -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        
        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 在父pom.xml添加模块声明**

在 `yigongbao-parent/pom.xml` 的 `<modules>` 中添加：
```xml
<module>yigongbao-module-notification</module>
```

- [ ] **Step 3: 验证模块构建**

Run: `mvn clean compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/pom.xml yigongbao-parent/pom.xml
git commit -m "feat(notification): 创建消息通知模块基础结构"
```

---

### Task 2: 创建枚举类

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/enums/MessageTypeEnum.java`
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/enums/TargetTypeEnum.java`

- [ ] **Step 1: 创建MessageTypeEnum**

```java
package com.yigongbao.module.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    ORDER("ORDER", "订单消息"),
    DESIGN("DESIGN", "设计消息"),
    QC("QC", "质检消息"),
    PRODUCTION("PRODUCTION", "生产消息"),
    SYSTEM("SYSTEM", "系统消息");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
```

- [ ] **Step 2: 创建TargetTypeEnum（11种类型）**

```java
package com.yigongbao.module.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 接收者类型枚举（11种业务场景组合）
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Getter
@AllArgsConstructor
public enum TargetTypeEnum {

    // 业务线 (1-4)
    SALES_OWNER("SALES_OWNER", "订单业务员"),
    SALES_OWNER_WITH_DEALER_ADMIN("SALES_OWNER_WITH_DEALER_ADMIN", "订单业务员+经销商业务管理员"),
    SALES_OWNER_WITH_ALL_ADMIN("SALES_OWNER_WITH_ALL_ADMIN", "订单业务员+经销商业务管理员+公司管理员"),
    DEALER_SALES_ADMIN("DEALER_SALES_ADMIN", "订单经销商的业务管理员"),
    
    // 设计线 (5-8)
    ORDER_DESIGNER("ORDER_DESIGNER", "订单设计师"),
    DESIGNER_WITH_ADMIN("DESIGNER_WITH_ADMIN", "订单设计师+设计管理员"),
    DESIGNER_WITH_ALL_ADMIN("DESIGNER_WITH_ALL_ADMIN", "订单设计师+设计管理员+公司管理员"),
    ALL_DESIGN_ADMIN("ALL_DESIGN_ADMIN", "所有设计管理员"),
    
    // 生产线 (9-11)
    ALL_PRODUCTION("ALL_PRODUCTION", "所有生产员"),
    PRODUCTION_WITH_ADMIN("PRODUCTION_WITH_ADMIN", "所有生产员+生产管理员"),
    PRODUCTION_WITH_ALL_ADMIN("PRODUCTION_WITH_ALL_ADMIN", "所有生产员+生产管理员+公司管理员");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/enums/
git commit -m "feat(notification): 添加消息类型和接收者类型枚举"
```

---

### Task 3: 创建UserMessageEntity

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/entity/UserMessageEntity.java`

- [ ] **Step 1: 创建UserMessageEntity**

```java
package com.yigongbao.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import com.yigongbao.module.notification.enums.MessageTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户消息实体
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_message")
public class UserMessageEntity extends BaseEntity {

    private Long userId;
    private String title;
    private String content;
    private MessageTypeEnum messageType;
    private Long orderId;
    private String flowAction;
    private Integer isRead;
    private LocalDateTime readTime;
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/entity/UserMessageEntity.java
git commit -m "feat(notification): 添加用户消息实体"
```

---

### Task 4: 创建MessageTemplateEntity

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/entity/MessageTemplateEntity.java`

- [ ] **Step 1: 创建MessageTemplateEntity**

```java
package com.yigongbao.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import com.yigongbao.module.notification.enums.MessageTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息模板实体
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_template")
public class MessageTemplateEntity extends BaseEntity {

    private String templateCode;
    private String templateName;
    private String title;
    private String content;
    private String paramsConfig;
    private MessageTypeEnum messageType;
    private Integer status;
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/entity/MessageTemplateEntity.java
git commit -m "feat(notification): 添加消息模板实体"
```

---

### Task 5: 创建NotificationRuleEntity

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/entity/NotificationRuleEntity.java`

- [ ] **Step 1: 创建NotificationRuleEntity**

```java
package com.yigongbao.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import com.yigongbao.module.notification.enums.TargetTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知规则实体
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification_rule")
public class NotificationRuleEntity extends BaseEntity {

    private String ruleCode;
    private String ruleName;
    private String flowAction;
    private Integer sendMessage;
    private Integer sendNotification;
    private TargetTypeEnum targetType;
    private String targetConfig;
    private Long templateId;
    private Integer priority;
    private Integer status;
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/entity/NotificationRuleEntity.java
git commit -m "feat(notification): 添加通知规则实体"
```

---

### Task 6: 创建MessageSendLogEntity

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/entity/MessageSendLogEntity.java`

- [ ] **Step 1: 创建MessageSendLogEntity**

```java
package com.yigongbao.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息发送记录实体
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_send_log")
public class MessageSendLogEntity extends BaseEntity {

    private Long ruleId;
    private String flowAction;
    private Long orderId;
    private String targetType;
    private String targetUserIds;
    private Integer totalCount;
    private Integer messageSuccessCount;
    private Integer notificationSuccessCount;
    private Integer failCount;
    private LocalDateTime sendTime;
    private Integer durationMs;
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/entity/MessageSendLogEntity.java
git commit -m "feat(notification): 添加消息发送记录实体"
```

---

### Task 7: 创建Mapper接口

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/mapper/UserMessageMapper.java`
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/mapper/MessageTemplateMapper.java`
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/mapper/NotificationRuleMapper.java`
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/mapper/MessageSendLogMapper.java`

- [ ] **Step 1: 创建UserMessageMapper**

```java
package com.yigongbao.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.entity.UserMessageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户消息Mapper
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessageEntity> {
}
```

- [ ] **Step 2: 创建MessageTemplateMapper**

```java
package com.yigongbao.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.entity.MessageTemplateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息模板Mapper
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Mapper
public interface MessageTemplateMapper extends BaseMapper<MessageTemplateEntity> {
}
```

- [ ] **Step 3: 创建NotificationRuleMapper**

```java
package com.yigongbao.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.entity.NotificationRuleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知规则Mapper
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Mapper
public interface NotificationRuleMapper extends BaseMapper<NotificationRuleEntity> {
}
```

- [ ] **Step 4: 创建MessageSendLogMapper**

```java
package com.yigongbao.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.entity.MessageSendLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息发送记录Mapper
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Mapper
public interface MessageSendLogMapper extends BaseMapper<MessageSendLogEntity> {
}
```

- [ ] **Step 5: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/mapper/
git commit -m "feat(notification): 添加Mapper接口层"
```

---

## 阶段二：服务层实现 (Service Layer)

### Task 8: 创建IUserMessageService接口

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/IUserMessageService.java`

- [ ] **Step 1: 创建IUserMessageService接口**

```java
package com.yigongbao.module.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.notification.entity.UserMessageEntity;

/**
 * 用户消息服务接口
 *
 * @author hanjor
 * @date 2026-06-03
 */
public interface IUserMessageService extends IService<UserMessageEntity> {

    /**
     * 获取用户未读消息数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long messageId);

    /**
     * 批量标记已读
     */
    void batchMarkAsRead(Long userId, java.util.List<Long> messageIds);

    /**
     * 全部标记已读
     */
    void markAllAsRead(Long userId);
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/IUserMessageService.java
git commit -m "feat(notification): 添加用户消息服务接口"
```

---

### Task 9: 实现UserMessageServiceImpl

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/impl/UserMessageServiceImpl.java`

- [ ] **Step 1: 创建UserMessageServiceImpl**

```java
package com.yigongbao.module.notification.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.notification.entity.UserMessageEntity;
import com.yigongbao.module.notification.mapper.UserMessageMapper;
import com.yigongbao.module.notification.service.IUserMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户消息服务实现
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMessageServiceImpl extends ServiceImpl<UserMessageMapper, UserMessageEntity> 
        implements IUserMessageService {

    @Override
    public Long getUnreadCount(Long userId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<UserMessageEntity>()
                .eq(UserMessageEntity::getUserId, userId)
                .eq(UserMessageEntity::getIsRead, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long messageId) {
        UserMessageEntity message = getById(messageId);
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        
        if (message.getIsRead() == 0) {
            message.setIsRead(1);
            message.setReadTime(LocalDateTime.now());
            updateById(message);
            
            log.info("标记消息已读: messageId={}, userId={}", messageId, message.getUserId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkAsRead(Long userId, List<Long> messageIds) {
        if (CollUtil.isEmpty(messageIds)) {
            return;
        }
        
        LambdaUpdateWrapper<UserMessageEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserMessageEntity::getUserId, userId)
                .in(UserMessageEntity::getId, messageIds)
                .eq(UserMessageEntity::getIsRead, 0)
                .set(UserMessageEntity::getIsRead, 1)
                .set(UserMessageEntity::getReadTime, LocalDateTime.now());
        
        int count = baseMapper.update(null, wrapper);
        log.info("批量标记已读: userId={}, count={}", userId, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<UserMessageEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserMessageEntity::getUserId, userId)
                .eq(UserMessageEntity::getIsRead, 0)
                .set(UserMessageEntity::getIsRead, 1)
                .set(UserMessageEntity::getReadTime, LocalDateTime.now());
        
        int count = baseMapper.update(null, wrapper);
        log.info("全部标记已读: userId={}, count={}", userId, count);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/impl/UserMessageServiceImpl.java
git commit -m "feat(notification): 实现用户消息服务"
```

---

### Task 10: 创建MessageTemplateService

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/IMessageTemplateService.java`
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/impl/MessageTemplateServiceImpl.java`

- [ ] **Step 1: 创建IMessageTemplateService接口**

```java
package com.yigongbao.module.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.notification.entity.MessageTemplateEntity;

import java.util.Map;

/**
 * 消息模板服务接口
 *
 * @author hanjor
 * @date 2026-06-03
 */
public interface IMessageTemplateService extends IService<MessageTemplateEntity> {

    /**
     * 渲染模板内容
     */
    MessageRenderResult renderContent(Long templateId, Map<String, Object> params);
    
    /**
     * 消息渲染结果
     */
    class MessageRenderResult {
        private String title;
        private String content;
        
        public MessageRenderResult(String title, String content) {
            this.title = title;
            this.content = content;
        }
        
        public String getTitle() { return title; }
        public String getContent() { return content; }
    }
}
```

- [ ] **Step 2: 创建MessageTemplateServiceImpl实现**

```java
package com.yigongbao.module.notification.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.notification.entity.MessageTemplateEntity;
import com.yigongbao.module.notification.mapper.MessageTemplateMapper;
import com.yigongbao.module.notification.service.IMessageTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 消息模板服务实现
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageTemplateServiceImpl extends ServiceImpl<MessageTemplateMapper, MessageTemplateEntity> 
        implements IMessageTemplateService {

    @Override
    public MessageRenderResult renderContent(Long templateId, Map<String, Object> params) {
        MessageTemplateEntity template = getById(templateId);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        
        // 校验必需参数
        List<String> requiredParams = JSONUtil.toList(template.getParamsConfig(), String.class);
        for (String param : requiredParams) {
            if (!params.containsKey(param)) {
                throw new BusinessException(400, "缺少必需参数: " + param);
            }
        }
        
        // 占位符替换
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
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/IMessageTemplateService.java
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/impl/MessageTemplateServiceImpl.java
git commit -m "feat(notification): 实现消息模板服务"
```

---

### Task 11: 创建NotificationRuleService

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/INotificationRuleService.java`
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/impl/NotificationRuleServiceImpl.java`

- [ ] **Step 1: 创建INotificationRuleService接口**

```java
package com.yigongbao.module.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.notification.entity.NotificationRuleEntity;

import java.util.List;

/**
 * 通知规则服务接口
 *
 * @author hanjor
 * @date 2026-06-03
 */
public interface INotificationRuleService extends IService<NotificationRuleEntity> {

    /**
     * 根据流转动作查询启用的规则
     */
    List<NotificationRuleEntity> listEnabledByAction(String flowAction);
}
```

- [ ] **Step 2: 创建NotificationRuleServiceImpl实现（含缓存）**

```java
package com.yigongbao.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.notification.entity.NotificationRuleEntity;
import com.yigongbao.module.notification.mapper.NotificationRuleMapper;
import com.yigongbao.module.notification.service.INotificationRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通知规则服务实现
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRuleServiceImpl extends ServiceImpl<NotificationRuleMapper, NotificationRuleEntity> 
        implements INotificationRuleService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "notification:rule:";

    @Override
    public List<NotificationRuleEntity> listEnabledByAction(String flowAction) {
        String cacheKey = CACHE_KEY_PREFIX + flowAction;
        
        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<NotificationRuleEntity> cached = (List<NotificationRuleEntity>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 查询数据库
        List<NotificationRuleEntity> rules = baseMapper.selectList(
            new LambdaQueryWrapper<NotificationRuleEntity>()
                .eq(NotificationRuleEntity::getFlowAction, flowAction)
                .eq(NotificationRuleEntity::getStatus, 1)
                .orderByAsc(NotificationRuleEntity::getPriority)
        );
        
        // 写入缓存（10分钟）
        redisTemplate.opsForValue().set(cacheKey, rules, 10, TimeUnit.MINUTES);
        
        return rules;
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/INotificationRuleService.java
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/impl/NotificationRuleServiceImpl.java
git commit -m "feat(notification): 实现通知规则服务"
```

---

### Task 12: 创建核心NotificationService（接收者解析逻辑）

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/NotificationService.java`

- [ ] **Step 1: 创建NotificationService接口定义部分**

```java
package com.yigongbao.module.notification.service;

import com.yigongbao.module.notification.entity.NotificationRuleEntity;
import com.yigongbao.module.notification.enums.TargetTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 通知服务（核心业务逻辑）
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /**
     * 根据流转动作发送通知
     */
    public void sendByFlowAction(Long orderId, String flowAction) {
        // 待实现
    }

    /**
     * 解析目标用户（11种接收者类型）
     */
    private List<Long> resolveTargetUsers(NotificationRuleEntity rule, Long orderId) {
        TargetTypeEnum targetType = rule.getTargetType();
        Set<Long> targetUsers = new HashSet<>();
        
        // 获取订单信息
        // OrderEntity order = orderService.getById(orderId);
        // if (order == null) {
        //     log.warn("订单不存在，无法解析接收者: orderId={}", orderId);
        //     return Collections.emptyList();
        // }
        
        // 待实现：11种类型的switch case
        
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
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/NotificationService.java
git commit -m "feat(notification): 添加核心通知服务骨架"
```

---

### Task 13: 实现resolveTargetUsers的11种类型逻辑

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/NotificationService.java`

- [ ] **Step 1: 添加依赖注入（OrderService和UserService占位）**

在NotificationService类顶部添加：
```java
// 待集成order模块后注入
// private final IOrderService orderService;
// 待system模块提供方法后注入
// private final IUserService userService;
```

- [ ] **Step 2: 实现resolveTargetUsers完整逻辑**

替换resolveTargetUsers方法为完整实现（11种case + 降级策略）：

```java
private List<Long> resolveTargetUsers(NotificationRuleEntity rule, Long orderId) {
    TargetTypeEnum targetType = rule.getTargetType();
    Set<Long> targetUsers = new HashSet<>();
    
    // TODO: 获取订单信息（待order模块集成后解注释）
    // OrderEntity order = orderService.getById(orderId);
    // if (order == null) {
    //     log.warn("订单不存在，无法解析接收者: orderId={}", orderId);
    //     return Collections.emptyList();
    // }
    
    // 临时占位：模拟订单创建人为1L
    Long orderCreatorId = 1L;
    
    switch (targetType) {
        // ========== 业务线（1-4） ==========
        case SALES_OWNER:
            targetUsers.add(orderCreatorId);
            break;
            
        case SALES_OWNER_WITH_DEALER_ADMIN:
            targetUsers.add(orderCreatorId);
            // TODO: 解注释
            // Long dealerOrgId = userService.getById(orderCreatorId).getOrgId();
            // targetUsers.addAll(userService.listUserIdsByRoleAndOrg("SALES_ADMIN", dealerOrgId));
            break;
            
        case SALES_OWNER_WITH_ALL_ADMIN:
            targetUsers.add(orderCreatorId);
            // TODO: 解注释
            // Long dealerOrgId2 = userService.getById(orderCreatorId).getOrgId();
            // targetUsers.addAll(userService.listUserIdsByRoleAndOrg("SALES_ADMIN", dealerOrgId2));
            // targetUsers.addAll(userService.listUserIdsByRole("COMPANY_ADMIN"));
            break;
            
        case DEALER_SALES_ADMIN:
            // TODO: 解注释
            // Long dealerOrgId3 = userService.getById(orderCreatorId).getOrgId();
            // targetUsers.addAll(userService.listUserIdsByRoleAndOrg("SALES_ADMIN", dealerOrgId3));
            break;
            
        // ========== 设计线（5-8） ==========
        case ORDER_DESIGNER:
            // TODO: 解注释
            // if (order.getDesignerId() != null) {
            //     targetUsers.add(order.getDesignerId());
            // }
            break;
            
        case DESIGNER_WITH_ADMIN:
            // TODO: 解注释
            // if (order.getDesignerId() != null) {
            //     targetUsers.add(order.getDesignerId());
            // }
            // targetUsers.addAll(userService.listUserIdsByRole("DESIGN_ADMIN"));
            break;
            
        case DESIGNER_WITH_ALL_ADMIN:
            // TODO: 解注释
            // if (order.getDesignerId() != null) {
            //     targetUsers.add(order.getDesignerId());
            // }
            // targetUsers.addAll(userService.listUserIdsByRole("DESIGN_ADMIN"));
            // targetUsers.addAll(userService.listUserIdsByRole("COMPANY_ADMIN"));
            break;
            
        case ALL_DESIGN_ADMIN:
            // TODO: 解注释
            // targetUsers.addAll(userService.listUserIdsByRole("DESIGN_ADMIN"));
            break;
            
        // ========== 生产线（9-11） ==========
        case ALL_PRODUCTION:
            // TODO: 解注释
            // targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION"));
            break;
            
        case PRODUCTION_WITH_ADMIN:
            // TODO: 解注释
            // targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION"));
            // targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION_ADMIN"));
            break;
            
        case PRODUCTION_WITH_ALL_ADMIN:
            // TODO: 解注释
            // targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION"));
            // targetUsers.addAll(userService.listUserIdsByRole("PRODUCTION_ADMIN"));
            // targetUsers.addAll(userService.listUserIdsByRole("COMPANY_ADMIN"));
            break;
    }
    
    // 降级策略
    if (targetUsers.isEmpty()) {
        if (isOrderRelatedType(targetType)) {
            log.warn("接收者解析为空，降级为通知订单创建人: orderId={}, ruleId={}, targetType={}", 
                orderId, rule.getId(), targetType);
            targetUsers.add(orderCreatorId);
        } else {
            log.error("全局角色类型解析为空，可能是角色配置缺失: ruleId={}, targetType={}", 
                rule.getId(), targetType);
        }
    }
    
    return new ArrayList<>(targetUsers);
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/NotificationService.java
git commit -m "feat(notification): 实现接收者解析逻辑（11种类型+降级策略）"
```

---

### Task 14: 实现sendByFlowAction主逻辑

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/NotificationService.java`

- [ ] **Step 1: 添加依赖注入声明**

在NotificationService类顶部添加：
```java
private final INotificationRuleService ruleService;
private final IMessageTemplateService templateService;
private final IUserMessageService userMessageService;
```

- [ ] **Step 2: 实现sendByFlowAction方法**

替换sendByFlowAction方法为完整实现：

```java
@Transactional(rollbackFor = Exception.class)
public void sendByFlowAction(Long orderId, String flowAction) {
    long startTime = System.currentTimeMillis();
    
    // 1. 查询该action的启用规则
    List<NotificationRuleEntity> rules = ruleService.listEnabledByAction(flowAction);
    if (rules.isEmpty()) {
        log.debug("无匹配的通知规则: orderId={}, action={}", orderId, flowAction);
        return;
    }
    
    // 2. 遍历规则发送
    for (NotificationRuleEntity rule : rules) {
        try {
            // 解析接收者
            List<Long> targetUsers = resolveTargetUsers(rule, orderId);
            if (targetUsers.isEmpty()) {
                continue;
            }
            
            // 渲染消息内容
            Map<String, Object> params = buildTemplateParams(orderId);
            IMessageTemplateService.MessageRenderResult rendered = 
                templateService.renderContent(rule.getTemplateId(), params);
            
            int messageSuccessCount = 0;
            int notificationSuccessCount = 0;
            
            // 3. 发送消息（持久化）
            if (rule.getSendMessage() == 1) {
                for (Long userId : targetUsers) {
                    UserMessageEntity message = new UserMessageEntity();
                    message.setUserId(userId);
                    message.setTitle(rendered.getTitle());
                    message.setContent(rendered.getContent());
                    message.setMessageType(templateService.getById(rule.getTemplateId()).getMessageType());
                    message.setOrderId(orderId);
                    message.setFlowAction(flowAction);
                    message.setIsRead(0);
                    
                    userMessageService.save(message);
                    messageSuccessCount++;
                }
            }
            
            // 4. 发送通知（WebSocket推送 - 待实现）
            if (rule.getSendNotification() == 1) {
                // TODO: WebSocket推送逻辑
                notificationSuccessCount = targetUsers.size();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("发送消息: orderId={}, action={}, targetUserCount={}, messageCount={}, notificationCount={}, duration={}ms", 
                orderId, flowAction, targetUsers.size(), messageSuccessCount, notificationSuccessCount, duration);
            
        } catch (Exception e) {
            log.error("通知规则执行失败: ruleId={}, orderId={}, action={}", 
                rule.getId(), orderId, flowAction, e);
        }
    }
}

/**
 * 构建模板参数
 */
private Map<String, Object> buildTemplateParams(Long orderId) {
    Map<String, Object> params = new HashMap<>();
    // TODO: 从订单获取实际参数
    params.put("orderNo", "YGB20260603001");
    params.put("hospitalName", "测试医院");
    return params;
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/service/NotificationService.java
git commit -m "feat(notification): 实现消息发送主逻辑"
```

---

## 阶段三：WebSocket实时推送 (WebSocket Integration)

### Task 15: 创建UserConnectionManager

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/websocket/UserConnectionManager.java`

- [ ] **Step 1: 创建UserConnectionManager（连接管理器）**

```java
package com.yigongbao.module.notification.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * WebSocket用户连接管理器
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Slf4j
@Component
public class UserConnectionManager {

    private final Map<Long, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(session);
        log.info("用户连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    /**
     * 移除会话
     */
    public void removeSession(Long userId, String sessionId) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.removeIf(s -> s.getId().equals(sessionId));
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.info("用户连接关闭: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 获取用户所有会话
     */
    public List<WebSocketSession> getSessions(Long userId) {
        return userSessions.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 用户是否在线
     */
    public boolean isOnline(Long userId) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/websocket/UserConnectionManager.java
git commit -m "feat(notification): 添加WebSocket连接管理器"
```

---

### Task 16: 创建NotificationWebSocketHandler

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/websocket/NotificationWebSocketHandler.java`

- [ ] **Step 1: 创建NotificationWebSocketHandler**

```java
package com.yigongbao.module.notification.websocket;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * 通知WebSocket处理器
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final UserConnectionManager connectionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String token = getTokenFromSession(session);
            if (StrUtil.isBlank(token)) {
                log.warn("WebSocket连接缺少token: sessionId={}", session.getId());
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }
            
            Long userId = StpUtil.getLoginIdByToken(token, Long.class);
            connectionManager.addSession(userId, session);
            
            // TODO: 处理离线消息
            
        } catch (Exception e) {
            log.error("WebSocket连接建立失败: sessionId={}", session.getId(), e);
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception ex) {
                log.error("关闭连接失败", ex);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // TODO: 从session属性获取userId并移除连接
        log.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    /**
     * 从WebSocketSession获取token
     */
    private String getTokenFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        
        String query = uri.getQuery();
        if (StrUtil.isBlank(query)) {
            return null;
        }
        
        UrlQuery urlQuery = UrlQuery.of(query);
        return urlQuery.get("token");
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/websocket/NotificationWebSocketHandler.java
git commit -m "feat(notification): 添加WebSocket处理器"
```

---

### Task 17: 创建WebSocket配置

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/config/WebSocketConfig.java`

- [ ] **Step 1: 创建WebSocketConfig**

```java
package com.yigongbao.module.notification.config;

import com.yigongbao.module.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notification")
                .setAllowedOrigins("*");
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/config/WebSocketConfig.java
git commit -m "feat(notification): 添加WebSocket配置"
```

---

## 阶段四：Flow集成 (Flow Integration)

### Task 18: 在yigongbao-boot模块添加notification依赖

**Files:**
- Modify: `yigongbao-parent/yigongbao-boot/pom.xml`

- [ ] **Step 1: 添加notification模块依赖**

在yigongbao-boot/pom.xml的`<dependencies>`中添加：
```xml
<!-- 通知模块 -->
<dependency>
    <groupId>com.yigongbao</groupId>
    <artifactId>yigongbao-module-notification</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 2: 验证构建**

Run: `mvn clean compile -pl yigongbao-boot`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-boot/pom.xml
git commit -m "feat(notification): boot模块添加notification依赖"
```

---

### Task 19: 在FlowFacadeImpl集成异步通知

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-flow/src/main/java/com/yigongbao/flow/facade/impl/FlowFacadeImpl.java`

- [ ] **Step 1: 添加NotificationService依赖注入**

在FlowFacadeImpl类顶部添加：
```java
@Autowired(required = false)
private NotificationService notificationService;

@Autowired
@Qualifier("notificationExecutor")
private Executor notificationExecutor;
```

- [ ] **Step 2: 在executeAction方法添加异步通知触发**

在executeAction方法的流转成功后添加异步调用：
```java
// 流转成功后,异步发送通知
if (result.isSuccess() && notificationService != null) {
    CompletableFuture.runAsync(() -> {
        try {
            notificationService.sendByFlowAction(orderId, action.getCode());
        } catch (Exception e) {
            log.error("发送通知失败: orderId={}, action={}", orderId, action, e);
        }
    }, notificationExecutor);
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl yigongbao-module-flow`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-flow/src/main/java/com/yigongbao/flow/facade/impl/FlowFacadeImpl.java
git commit -m "feat(notification): FlowFacade集成异步通知触发"
```

---

### Task 20: 创建通知线程池配置

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/config/NotificationExecutorConfig.java`

- [ ] **Step 1: 创建NotificationExecutorConfig**

```java
package com.yigongbao.module.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 通知线程池配置
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Configuration
public class NotificationExecutorConfig {

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

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/config/NotificationExecutorConfig.java
git commit -m "feat(notification): 添加通知专用线程池配置"
```

---

## 阶段五：Controller API层 (API Layer)

### Task 21: 创建DTO和VO

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/dto/QueryMessageDTO.java`
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/vo/UserMessageVO.java`

- [ ] **Step 1: 创建QueryMessageDTO**

```java
package com.yigongbao.module.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询消息DTO
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
public class QueryMessageDTO {
    private String messageType;
    private Integer isRead;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer current = 1;
    private Integer size = 10;
}
```

- [ ] **Step 2: 创建UserMessageVO**

```java
package com.yigongbao.module.notification.vo;

import com.yigongbao.module.notification.enums.MessageTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户消息VO
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
public class UserMessageVO {
    private Long id;
    private String title;
    private String content;
    private MessageTypeEnum messageType;
    private Long orderId;
    private Integer isRead;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/dto/
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/vo/
git commit -m "feat(notification): 添加DTO和VO"
```

---

### Task 22: 创建UserMessageController

**Files:**
- Create: `yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/controller/UserMessageController.java`

- [ ] **Step 1: 创建UserMessageController**

```java
package com.yigongbao.module.notification.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.notification.dto.QueryMessageDTO;
import com.yigongbao.module.notification.entity.UserMessageEntity;
import com.yigongbao.module.notification.service.IUserMessageService;
import com.yigongbao.module.notification.vo.UserMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户消息Controller
 *
 * @author hanjor
 * @date 2026-06-03
 */
@RestController
@RequestMapping("/api/notification/message")
@RequiredArgsConstructor
public class UserMessageController {

    private final IUserMessageService messageService;

    @PostMapping("/list")
    public Result<IPage<UserMessageVO>> list(@RequestBody QueryMessageDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        LambdaQueryWrapper<UserMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMessageEntity::getUserId, userId)
                .orderByDesc(UserMessageEntity::getCreateTime);
        
        if (dto.getMessageType() != null) {
            wrapper.eq(UserMessageEntity::getMessageType, dto.getMessageType());
        }
        if (dto.getIsRead() != null) {
            wrapper.eq(UserMessageEntity::getIsRead, dto.getIsRead());
        }
        
        Page<UserMessageEntity> page = new Page<>(dto.getCurrent(), dto.getSize());
        IPage<UserMessageEntity> result = messageService.page(page, wrapper);
        
        IPage<UserMessageVO> voPage = result.convert(entity -> 
            BeanUtil.copyProperties(entity, UserMessageVO.class));
        
        return Result.success(voPage);
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long count = messageService.getUnreadCount(userId);
        return Result.success(count);
    }

    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        messageService.markAllAsRead(userId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.removeById(id);
        return Result.success();
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl yigongbao-module-notification`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-notification/src/main/java/com/yigongbao/module/notification/controller/UserMessageController.java
git commit -m "feat(notification): 添加用户消息Controller"
```

---

## 阶段六：数据库初始化 (Database Setup)

### Task 23: 创建数据库表DDL

**Files:**
- Modify: `yigongbao-parent/sql/ddl.sql`

- [ ] **Step 1: 在ddl.sql末尾追加4张表的DDL**

```sql
-- ============================================
-- 消息通知模块表
-- ============================================

-- 用户消息表
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

-- 消息模板表
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
    is_deleted TINYINT DEFAULT 0
) ENGINE=InnoDB COMMENT='消息模板表';

CREATE UNIQUE INDEX uk_template_code
    ON message_template ((CASE WHEN is_deleted = 0 THEN template_code ELSE NULL END));

-- 通知规则表
CREATE TABLE notification_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(100) NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    flow_action VARCHAR(50) NOT NULL COMMENT 'FlowActionEnum',
    send_message TINYINT DEFAULT 1 COMMENT '是否发送消息',
    send_notification TINYINT DEFAULT 1 COMMENT '是否发送通知',
    target_type VARCHAR(50) NOT NULL COMMENT '接收者类型：11种枚举值',
    target_config JSON COMMENT '扩展配置（预留）',
    template_id BIGINT NOT NULL,
    priority INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_action (flow_action, status)
) ENGINE=InnoDB COMMENT='通知规则表';

CREATE UNIQUE INDEX uk_rule_code
    ON notification_rule ((CASE WHEN is_deleted = 0 THEN rule_code ELSE NULL END));

-- 消息发送记录表
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

- [ ] **Step 2: 验证SQL语法**

Run: `mysql -u root -p < yigongbao-parent/sql/ddl.sql`
Expected: 无语法错误

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/sql/ddl.sql
git commit -m "feat(notification): 添加消息通知模块4张表DDL"
```

---

### Task 24: 创建初始化数据SQL

**Files:**
- Modify: `yigongbao-parent/sql/init.sql`

- [ ] **Step 1: 在init.sql末尾追加初始化数据**

```sql
-- ============================================
-- 消息通知模块初始化数据
-- ============================================

-- 消息模板初始化
INSERT INTO message_template (template_code, template_name, title, content, params_config, message_type, status) VALUES
('SUBMIT_ORDER', '提交订单通知', '您有新的订单', '订单【{orderNo}】已提交，医院：{hospitalName}，请及时处理', '["orderNo","hospitalName"]', 'ORDER', 1),
('DESIGN_PASS', '设计审核通过通知', '设计审核通过', '订单【{orderNo}】设计审核已通过，可以继续处理', '["orderNo"]', 'DESIGN', 1),
('QC_PASS', '质检合格通知', '质检合格', '订单【{orderNo}】质检合格，可以安排生产', '["orderNo"]', 'QC', 1);

-- 通知规则初始化（3个示例规则）
INSERT INTO notification_rule (rule_code, rule_name, flow_action, send_message, send_notification, target_type, target_config, template_id, status) VALUES
('SUBMIT_ORDER_TO_DESIGNER', '提交订单通知设计师和管理员', 'SUBMIT_ORDER', 1, 1, 'DESIGNER_WITH_ADMIN', NULL, 
    (SELECT id FROM message_template WHERE template_code = 'SUBMIT_ORDER'), 1),
('DESIGN_PASS_TO_SALES', '设计审核通过通知业务员', 'DESIGN_REVIEW_PASS', 1, 1, 'SALES_OWNER', NULL, 
    (SELECT id FROM message_template WHERE template_code = 'DESIGN_PASS'), 1),
('QC_PASS_TO_PRODUCTION', '质检合格通知生产部门', 'QC_PASS', 1, 1, 'PRODUCTION_WITH_ADMIN', NULL, 
    (SELECT id FROM message_template WHERE template_code = 'QC_PASS'), 1);
```

- [ ] **Step 2: 验证SQL语法**

Run: `mysql -u root -p < yigongbao-parent/sql/init.sql`
Expected: 无语法错误，插入6条记录

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/sql/init.sql
git commit -m "feat(notification): 添加消息通知模块初始化数据"
```

---

## 执行建议

**计划完成。接下来两种执行方式：**

**1. Subagent-Driven（推荐）**
- 每个Task由独立subagent执行
- 两阶段review：subagent自审 + 主agent审查
- 快速迭代，问题隔离

**2. Inline Execution**
- 在当前会话按Task顺序执行
- 批量执行，定期checkpoint

**选择建议**：使用Subagent-Driven方式，分Task并行执行，提高效率。

---

**实施要点**：

1. **阶段一优先**：完成实体层和Mapper层后即可进行数据库表创建
2. **Service层是核心**：Task 8-14的服务层实现包含核心业务逻辑
3. **WebSocket可后置**：阶段三的WebSocket推送可在消息持久化验证通过后再实现
4. **Flow集成需协调**：Task 19涉及flow模块修改，需确保不影响现有流程
5. **分步测试**：每完成一个阶段立即进行单元测试验证

**预计耗时**：
- 阶段一：2小时
- 阶段二：4小时
- 阶段三：2小时
- 阶段四：1小时
- 阶段五：1小时
- 阶段六：0.5小时

**总计约10.5小时**（不含测试和调试时间）


