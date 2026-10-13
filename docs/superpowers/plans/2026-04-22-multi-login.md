# 多登录方式 + 忘记密码重置 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有账号密码登录基础上，新增手机验证码和邮箱验证码两种登录方式，并实现通过验证码重置密码的忘记密码功能。

**Architecture:** 新增 `CaptchaService`（验证码发送+校验，Redis 存储）、`SmsService`（接口+Mock实现）、`MailService`（接口+Spring Mail实现）三个服务；重构 `LoginDTO` 为 `loginType + principal + credential` 统一结构；`AuthServiceImpl.login()` 按 loginType 分发三条路径，共用后置逻辑（状态检查、Sa-Token登录、日志记录）。

**Tech Stack:** Spring Boot, Sa-Token, Redis (`RedisTemplate<String,Object>` 已配置于 `yigongbao-framework`), Spring Mail (`JavaMailSender`), MyBatis-Plus, JUnit 5 + Mockito

---

## 文件结构

### 新增文件

| 文件 | 说明 |
|---|---|
| `yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java` | 修改：新增 4 个配置键 |
| `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java` | 修改：新增错误码 636-641 |
| `yigongbao-module-system/.../auth/enums/LoginTypeEnum.java` | 新增枚举 |
| `yigongbao-module-system/.../auth/enums/CaptchaTypeEnum.java` | 新增枚举 |
| `yigongbao-module-system/.../auth/enums/CaptchaSceneEnum.java` | 新增枚举 |
| `yigongbao-module-system/.../auth/dto/SendCaptchaDTO.java` | 新增 DTO |
| `yigongbao-module-system/.../auth/dto/ForgotPasswordResetDTO.java` | 新增 DTO |
| `yigongbao-module-system/.../auth/service/SmsService.java` | 新增接口 |
| `yigongbao-module-system/.../auth/service/MailService.java` | 新增接口 |
| `yigongbao-module-system/.../auth/service/impl/MockSmsServiceImpl.java` | 新增实现 |
| `yigongbao-module-system/.../auth/service/impl/SpringMailServiceImpl.java` | 新增实现 |
| `yigongbao-module-system/.../auth/service/CaptchaService.java` | 新增接口 |
| `yigongbao-module-system/.../auth/service/impl/CaptchaServiceImpl.java` | 新增实现 |
| `yigongbao-module-system/.../auth/service/impl/CaptchaServiceImplTest.java` | 新增测试 |

### 修改文件

| 文件 | 改动 |
|---|---|
| `yigongbao-module-system/.../auth/dto/LoginDTO.java` | 重构字段 |
| `yigongbao-module-system/.../auth/entity/LoginLogEntity.java` | 新增 loginType 字段 |
| `yigongbao-module-system/.../auth/service/AuthService.java` | 新增忘记密码接口 |
| `yigongbao-module-system/.../auth/service/impl/AuthServiceImpl.java` | 重构 login + 新增忘记密码 |
| `yigongbao-module-system/.../auth/controller/AuthController.java` | 新增端点 |
| `yigongbao-module-system/.../auth/service/impl/AuthServiceImplTest.java` | 扩展测试用例 |
| `yigongbao-module-system/.../auth/controller/AuthControllerTest.java` | 扩展测试用例 |
| `yigongbao-module-system/.../user/mapper/UserMapper.java` | 新增查询方法 |
| `yigongbao-module-system/.../user/service/impl/UserServiceImpl.java` | email 唯一性校验 |
| `yigongbao-module-system/src/test/resources/schema.sql` | 新增 login_type 字段 |
| `yigongbao-module-system/src/test/resources/application-test.yml` | 新增 captcha 配置 |
| `sql/ddl.sql` | 唯一索引 + login_type 字段 |
| `sql/init.sql` | sys_config 新配置项 |

---

### 路径前缀说明

以下任务中使用简写，完整包路径为：
- **system-auth**: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth`
- **system-user**: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user`
- **common**: `yigongbao-common/src/main/java/com/yigongbao/common`
- **test-auth**: `yigongbao-module-system/src/test/java/com/yigongbao/module/system/auth`

---

## Task 1: 公共枚举与错误码

**Files:**
- Modify: `common/enums/ErrorCodeEnum.java`
- Modify: `common/enums/SystemConfigKeyEnum.java`
- Create: `system-auth/enums/LoginTypeEnum.java`
- Create: `system-auth/enums/CaptchaTypeEnum.java`
- Create: `system-auth/enums/CaptchaSceneEnum.java`

- [ ] **Step 1: 在 ErrorCodeEnum 中新增错误码**

在 `USER_PASSWORD_WEAK(635, ...)` 后追加：

```java
// ==================== 邮箱相关 636 ====================
USER_EMAIL_EXISTS(636, "邮箱已存在"),

// ==================== 验证码相关 637-641 ====================
CAPTCHA_TOO_FREQUENT(637, "发送过于频繁，请稍后再试"),
CAPTCHA_DAILY_LIMIT(638, "今日发送次数已达上限"),
CAPTCHA_EXPIRED(639, "验证码已过期或不存在"),
CAPTCHA_ERROR(640, "验证码错误"),
CAPTCHA_TYPE_INVALID(641, "不支持的验证码类型"),

// ==================== 图形验证码 773-774 ====================
CAPTCHA_GRAPHIC_EXPIRED(773, "图形验证码已过期，请刷新"),
CAPTCHA_GRAPHIC_ERROR(774, "图形验证码错误"),
```

- [ ] **Step 2: 在 SystemConfigKeyEnum 中新增配置键**

在 `SMS_SEND_INTERVAL` 后追加（安全配置区域）：

```java
/**
 * 验证码有效期（秒）
 */
CAPTCHA_EXPIRE_SECONDS("captcha.expire.seconds", "验证码有效期（秒）"),

/**
 * 同一目标发送冷却（秒）
 */
CAPTCHA_COOLDOWN_SECONDS("captcha.cooldown.seconds", "验证码发送冷却时间（秒）"),

/**
 * 同一目标每日最大发送次数
 */
CAPTCHA_DAILY_LIMIT("captcha.daily.limit", "验证码每日最大发送次数"),

/**
 * 发件人邮箱地址
 */
MAIL_FROM("mail.from", "发件人邮箱地址"),
```

- [ ] **Step 3: 创建 LoginTypeEnum**

文件：`system-auth/enums/LoginTypeEnum.java`

```java
package com.yigongbao.module.system.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录类型枚举
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    /** 账号密码登录 */
    PASSWORD("PASSWORD"),

    /** 手机验证码登录 */
    PHONE("PHONE"),

    /** 邮箱验证码登录 */
    EMAIL("EMAIL");

    private final String value;
}
```

- [ ] **Step 4: 创建 CaptchaTypeEnum**

文件：`system-auth/enums/CaptchaTypeEnum.java`

```java
package com.yigongbao.module.system.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证码类型枚举（不含 PASSWORD，仅用于验证码发送/校验场景）
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Getter
@AllArgsConstructor
public enum CaptchaTypeEnum {

    /** 手机验证码 */
    PHONE("PHONE"),

    /** 邮箱验证码 */
    EMAIL("EMAIL");

    private final String value;
}
```

- [ ] **Step 5: 创建 CaptchaSceneEnum**

文件：`system-auth/enums/CaptchaSceneEnum.java`

```java
package com.yigongbao.module.system.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证码使用场景枚举，用于隔离 Redis key 前缀，防止跨场景复用
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Getter
@AllArgsConstructor
public enum CaptchaSceneEnum {

    /** 登录场景 */
    LOGIN("login"),

    /** 忘记密码场景 */
    FORGOT("forgot");

    private final String scene;
}
```

- [ ] **Step 6: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-common,yigongbao-module-system -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add yigongbao-parent/yigongbao-common/src yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth/enums
git commit -m "feat(auth): 新增登录类型、验证码类型、场景枚举及错误码"
```

---

## Task 2: SQL 变更（DDL + 初始数据）

**Files:**
- Modify: `sql/ddl.sql`
- Modify: `sql/init.sql`
- Modify: `yigongbao-module-system/src/test/resources/schema.sql`
- Modify: `yigongbao-module-system/src/test/resources/application-test.yml`

- [ ] **Step 1: 更新 sql/ddl.sql**

在 `sys_user` 表现有两个 `CREATE UNIQUE INDEX` 后追加：

```sql
-- email 唯一（逻辑删除兼容）
CREATE UNIQUE INDEX uk_email
    ON sys_user ((CASE WHEN is_deleted = 0 THEN email ELSE NULL END));

-- employee_no 唯一（本期不作登录凭据，同步补上约束）
CREATE UNIQUE INDEX uk_employee_no
    ON sys_user ((CASE WHEN is_deleted = 0 THEN employee_no ELSE NULL END));
```

在 `sys_login_log` 建表语句中，`username VARCHAR(64)` 字段后追加：

```sql
login_type      VARCHAR(16)     COMMENT '登录方式（PASSWORD/PHONE/EMAIL）',
```

- [ ] **Step 2: 更新 sql/init.sql**

在现有 `sms.send.interval` 配置插入语句后追加（security 组）：

```sql
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('captcha.expire.seconds',  '验证码有效期',           '300',  'number',  'security', '验证码有效期（秒）',               1, 0, 5, 1),
('captcha.cooldown.seconds','验证码发送冷却',          '60',   'number',  'security', '同一目标发送冷却（秒）',           1, 0, 6, 1),
('captcha.daily.limit',     '验证码每日发送上限',      '10',   'number',  'security', '同一目标每日最大发送次数',         1, 0, 7, 1),
('mail.from',               '发件人邮箱地址',          '',     'string',  'security', '发件人邮箱地址（必填，否则邮件发送失败）', 1, 0, 8, 1);
```

- [ ] **Step 3: 更新 schema.sql（测试 H2）**

在 `sys_login_log` 建表的 `username VARCHAR(64)` 字段后追加：

```sql
login_type      VARCHAR(16)     COMMENT '登录方式（PASSWORD/PHONE/EMAIL）',
```

（H2 不支持函数索引，`sys_user` 表无需添加唯一索引）

- [ ] **Step 4: 更新 application-test.yml**

在 `yigongbao.config` 安全配置区域追加：

```yaml
    # 验证码配置
    captcha-expire-seconds: 300
    captcha-cooldown-seconds: 60
    captcha-daily-limit: 10
    mail-from: "test@example.com"
```

- [ ] **Step 5: Commit**

```bash
git add sql/ yigongbao-parent/yigongbao-module-system/src/test/resources/
git commit -m "feat(auth): 更新DDL新增email/employee_no唯一索引、login_type字段及配置数据"
```

---

## Task 3: DTO 重构 + 新增 DTO

**Files:**
- Modify: `system-auth/dto/LoginDTO.java`
- Modify: `system-auth/entity/LoginLogEntity.java`
- Create: `system-auth/dto/SendCaptchaDTO.java`
- Create: `system-auth/dto/ForgotPasswordResetDTO.java`

- [ ] **Step 1: 重构 LoginDTO**

完整替换 `system-auth/dto/LoginDTO.java`：

```java
package com.yigongbao.module.system.auth.dto;

import com.yigongbao.module.system.auth.enums.LoginTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 统一登录 DTO
 * loginType=PASSWORD 时 principal=用户名，credential=密码
 * loginType=PHONE    时 principal=手机号，credential=短信验证码
 * loginType=EMAIL    时 principal=邮箱，  credential=邮箱验证码
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
public class LoginDTO {

    /**
     * 登录类型（PASSWORD/PHONE/EMAIL）
     */
    @NotNull(message = "登录类型不能为空")
    private LoginTypeEnum loginType;

    /**
     * 登录凭据主体（用户名/手机号/邮箱）
     */
    @NotBlank(message = "登录账号不能为空")
    private String principal;

    /**
     * 登录凭据（密码/验证码）
     */
    @NotBlank(message = "密码或验证码不能为空")
    private String credential;

    /**
     * 图形验证码 ID（仅 PASSWORD 类型必传）
     */
    private String captchaId;

    /**
     * 图形验证码内容（仅 PASSWORD 类型必传）
     */
    private String captchaCode;
}
```

- [ ] **Step 2: 更新 LoginLogEntity 新增 loginType 字段**

在 `username` 字段后新增：

```java
/**
 * 登录方式（PASSWORD/PHONE/EMAIL）
 */
private String loginType;
```

- [ ] **Step 3: 创建 SendCaptchaDTO**

文件：`system-auth/dto/SendCaptchaDTO.java`

```java
package com.yigongbao.module.system.auth.dto;

import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送验证码 DTO
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
public class SendCaptchaDTO {

    /**
     * 验证码类型（PHONE/EMAIL）
     */
    @NotNull(message = "验证码类型不能为空")
    private CaptchaTypeEnum captchaType;

    /**
     * 目标（手机号或邮箱）
     */
    @NotBlank(message = "目标不能为空")
    private String target;
}
```

- [ ] **Step 4: 创建 ForgotPasswordResetDTO**

文件：`system-auth/dto/ForgotPasswordResetDTO.java`

```java
package com.yigongbao.module.system.auth.dto;

import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码重置 DTO
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
public class ForgotPasswordResetDTO {

    /**
     * 验证码类型（PHONE/EMAIL）
     */
    @NotNull(message = "验证码类型不能为空")
    private CaptchaTypeEnum captchaType;

    /**
     * 目标（手机号或邮箱）
     */
    @NotBlank(message = "目标不能为空")
    private String target;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captcha;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String newPassword;
}
```

- [ ] **Step 5: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-system -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth/dto yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth/entity
git commit -m "feat(auth): 重构LoginDTO为统一结构，新增SendCaptchaDTO、ForgotPasswordResetDTO"
```

---

## Task 4: 通知服务（SmsService + MailService）

**Files:**
- Create: `system-auth/service/SmsService.java`
- Create: `system-auth/service/MailService.java`
- Create: `system-auth/service/impl/MockSmsServiceImpl.java`
- Create: `system-auth/service/impl/SpringMailServiceImpl.java`

- [ ] **Step 1: 添加 spring-boot-starter-mail 依赖**

在 `yigongbao-parent/pom.xml` 的 `<dependencyManagement>` 中找到其他 starter，追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
    <version>${spring-boot.version}</version>
</dependency>
```

在 `yigongbao-module-system/pom.xml` 的 `<dependencies>` 中追加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

- [ ] **Step 2: 创建 SmsService 接口**

文件：`system-auth/service/SmsService.java`

```java
package com.yigongbao.module.system.auth.service;

/**
 * 短信发送服务接口
 * 当前由 MockSmsServiceImpl 提供 Mock 实现（打日志）
 * 生产环境接入短信服务商后，新增实现类并移除 @Profile("!prod") 限制即可
 *
 * @author hanjor
 * @date 2026-04-22
 */
public interface SmsService {

    /**
     * 发送短信
     *
     * @param phone   手机号
     * @param content 短信内容
     */
    void send(String phone, String content);
}
```

- [ ] **Step 3: 创建 MailService 接口**

文件：`system-auth/service/MailService.java`

```java
package com.yigongbao.module.system.auth.service;

/**
 * 邮件发送服务接口
 *
 * @author hanjor
 * @date 2026-04-22
 */
public interface MailService {

    /**
     * 发送邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void send(String to, String subject, String content);
}
```

- [ ] **Step 4: 创建 MockSmsServiceImpl**

文件：`system-auth/service/impl/MockSmsServiceImpl.java`

```java
package com.yigongbao.module.system.auth.service.impl;

import com.yigongbao.module.system.auth.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 短信服务 Mock 实现（仅非生产环境生效）
 * 通过日志模拟短信发送，后续接入服务商时新增实现类并切换 Profile 即可
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Service
@Profile("!prod")
@Slf4j
public class MockSmsServiceImpl implements SmsService {

    @Override
    public void send(String phone, String content) {
        log.info("【短信模拟】手机号={}，内容={}", phone, content);
    }
}
```

- [ ] **Step 5: 创建 SpringMailServiceImpl**

文件：`system-auth/service/impl/SpringMailServiceImpl.java`

```java
package com.yigongbao.module.system.auth.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.service.MailService;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

/**
 * 邮件服务实现（Spring Mail / SMTP）
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringMailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;
    private final ConfigService configService;

    @Override
    public void send(String to, String subject, String content) {
        // 读取发件人，若未配置则 fail-fast
        String from = configService.getConfigValue(SystemConfigKeyEnum.MAIL_FROM.getKey());
        if (StrUtil.isBlank(from)) {
            log.error("邮件发件人未配置，config_key={}", SystemConfigKeyEnum.MAIL_FROM.getKey());
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        log.info("发送邮件，to={}, subject={}", to, subject);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
        log.info("邮件发送成功，to={}", to);
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
cd yigongbao-parent
mvn compile -pl yigongbao-module-system -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth/service yigongbao-parent/pom.xml yigongbao-parent/yigongbao-module-system/pom.xml
git commit -m "feat(auth): 新增SmsService/MailService接口及Mock短信、Spring Mail邮件实现"
```

---

## Task 5: CaptchaService（验证码核心服务）

**Files:**
- Create: `system-auth/service/CaptchaService.java`
- Create: `system-auth/service/impl/CaptchaServiceImpl.java`
- Create: `test-auth/service/impl/CaptchaServiceImplTest.java`

- [ ] **Step 1: 先写测试（TDD）**

文件：`test-auth/service/impl/CaptchaServiceImplTest.java`

```java
package com.yigongbao.module.system.auth.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.enums.CaptchaSceneEnum;
import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import com.yigongbao.module.system.auth.service.MailService;
import com.yigongbao.module.system.auth.service.SmsService;
import com.yigongbao.module.system.config.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CaptchaService 单元测试")
class CaptchaServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private SmsService smsService;
    @Mock
    private MailService mailService;
    @Mock
    private ConfigService configService;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    private static final String PHONE = "13800138000";
    private static final String EMAIL = "test@example.com";
    private static final String SCENE = CaptchaSceneEnum.LOGIN.getScene();
    private static final String TYPE_PHONE = CaptchaTypeEnum.PHONE.getValue();
    private static final String TYPE_EMAIL = CaptchaTypeEnum.EMAIL.getValue();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_EXPIRE_SECONDS.getKey())).thenReturn("300");
        when(configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_COOLDOWN_SECONDS.getKey())).thenReturn("60");
        when(configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_DAILY_LIMIT.getKey())).thenReturn("10");
    }

    // ==================== sendCaptcha 测试 ====================

    @Test
    @DisplayName("sendCaptcha: 冷却中时抛出 CAPTCHA_TOO_FREQUENT")
    void sendCaptcha_whenCoolingDown_shouldThrowTooFrequent() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.sendCaptcha(TYPE_PHONE, PHONE, SCENE));
        assertEquals(ErrorCodeEnum.CAPTCHA_TOO_FREQUENT.getCode(), ex.getCode());
        verify(smsService, never()).send(any(), any());
    }

    @Test
    @DisplayName("sendCaptcha: 每日次数已达上限时抛出 CAPTCHA_DAILY_LIMIT")
    void sendCaptcha_whenDailyLimitReached_shouldThrowDailyLimit() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        String dailyKey = "captcha:daily:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE + ":" + java.time.LocalDate.now().toString().replace("-", "");
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(false);
        when(valueOps.get(dailyKey)).thenReturn("10");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.sendCaptcha(TYPE_PHONE, PHONE, SCENE));
        assertEquals(ErrorCodeEnum.CAPTCHA_DAILY_LIMIT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("sendCaptcha: PHONE 正常发送时调用 SmsService")
    void sendCaptcha_phone_shouldCallSmsService() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(false);
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);

        captchaService.sendCaptcha(TYPE_PHONE, PHONE, SCENE);

        verify(smsService, times(1)).send(eq(PHONE), anyString());
        verify(mailService, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("sendCaptcha: EMAIL 正常发送时调用 MailService")
    void sendCaptcha_email_shouldCallMailService() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":" + TYPE_EMAIL + ":" + EMAIL;
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(false);
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);

        captchaService.sendCaptcha(TYPE_EMAIL, EMAIL, SCENE);

        verify(mailService, times(1)).send(eq(EMAIL), anyString(), anyString());
        verify(smsService, never()).send(any(), any());
    }

    @Test
    @DisplayName("sendCaptcha: 不支持的类型时抛出 CAPTCHA_TYPE_INVALID")
    void sendCaptcha_invalidType_shouldThrowInvalid() {
        String cooldownKey = "captcha:cooldown:" + SCENE + ":UNKNOWN:" + PHONE;
        when(redisTemplate.hasKey(cooldownKey)).thenReturn(false);
        when(valueOps.get(anyString())).thenReturn(null);
        when(valueOps.increment(anyString())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.sendCaptcha("UNKNOWN", PHONE, SCENE));
        assertEquals(ErrorCodeEnum.CAPTCHA_TYPE_INVALID.getCode(), ex.getCode());
    }

    // ==================== verifyCaptcha 测试 ====================

    @Test
    @DisplayName("verifyCaptcha: 验证码不存在时抛出 CAPTCHA_EXPIRED")
    void verifyCaptcha_whenNotExists_shouldThrowExpired() {
        String captchaKey = "captcha:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(valueOps.get(captchaKey)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyCaptcha(TYPE_PHONE, PHONE, SCENE, "123456"));
        assertEquals(ErrorCodeEnum.CAPTCHA_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("verifyCaptcha: 错误次数达到5次时删除 key 并抛出 CAPTCHA_EXPIRED")
    void verifyCaptcha_whenAttemptsExceeded_shouldDeleteAndThrowExpired() {
        String captchaKey = "captcha:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        String attemptsKey = "captcha:attempts:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(valueOps.get(captchaKey)).thenReturn("123456");
        when(valueOps.get(attemptsKey)).thenReturn("5");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyCaptcha(TYPE_PHONE, PHONE, SCENE, "999999"));
        assertEquals(ErrorCodeEnum.CAPTCHA_EXPIRED.getCode(), ex.getCode());
        verify(redisTemplate, times(1)).delete(captchaKey);
    }

    @Test
    @DisplayName("verifyCaptcha: 验证码不匹配时递增错误次数并抛出 CAPTCHA_ERROR")
    void verifyCaptcha_whenMismatch_shouldIncrementAttemptsAndThrowError() {
        String captchaKey = "captcha:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        String attemptsKey = "captcha:attempts:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(valueOps.get(captchaKey)).thenReturn("123456");
        when(valueOps.get(attemptsKey)).thenReturn("2");
        when(valueOps.increment(attemptsKey)).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> captchaService.verifyCaptcha(TYPE_PHONE, PHONE, SCENE, "999999"));
        assertEquals(ErrorCodeEnum.CAPTCHA_ERROR.getCode(), ex.getCode());
        verify(valueOps, times(1)).increment(attemptsKey);
    }

    @Test
    @DisplayName("verifyCaptcha: 验证码匹配时删除 captcha key 和 attempts key")
    void verifyCaptcha_whenMatch_shouldDeleteBothKeys() {
        String captchaKey = "captcha:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        String attemptsKey = "captcha:attempts:" + SCENE + ":" + TYPE_PHONE + ":" + PHONE;
        when(valueOps.get(captchaKey)).thenReturn("123456");
        when(valueOps.get(attemptsKey)).thenReturn(null);

        assertDoesNotThrow(() -> captchaService.verifyCaptcha(TYPE_PHONE, PHONE, SCENE, "123456"));
        verify(redisTemplate, times(1)).delete(captchaKey);
        verify(redisTemplate, times(1)).delete(attemptsKey);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败（CaptchaServiceImpl 尚未存在）**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-system -Dtest=CaptchaServiceImplTest -q 2>&1 | tail -5
```

Expected: FAIL（编译错误或类不存在）

- [ ] **Step 3: 创建 CaptchaService 接口**

文件：`system-auth/service/CaptchaService.java`

```java
package com.yigongbao.module.system.auth.service;

/**
 * 验证码服务接口
 *
 * @author hanjor
 * @date 2026-04-22
 */
public interface CaptchaService {

    /**
     * 发送验证码
     *
     * @param captchaType 验证码类型（PHONE/EMAIL）
     * @param target      目标（手机号或邮箱）
     * @param scene       使用场景（login/forgot）
     */
    void sendCaptcha(String captchaType, String target, String scene);

    /**
     * 校验验证码（匹配后立即删除，防重放攻击；错误5次后删除key强制重发）
     *
     * @param captchaType 验证码类型（PHONE/EMAIL）
     * @param target      目标（手机号或邮箱）
     * @param scene       使用场景（login/forgot）
     * @param code        用户输入的验证码
     */
    void verifyCaptcha(String captchaType, String target, String scene, String code);
}
```

- [ ] **Step 4: 创建 CaptchaServiceImpl**

文件：`system-auth/service/impl/CaptchaServiceImpl.java`

```java
package com.yigongbao.module.system.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.auth.enums.CaptchaTypeEnum;
import com.yigongbao.module.system.auth.service.CaptchaService;
import com.yigongbao.module.system.auth.service.MailService;
import com.yigongbao.module.system.auth.service.SmsService;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * 验证码 Service 实现类
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsService smsService;
    private final MailService mailService;
    private final ConfigService configService;

    /** 最大错误次数，超出后删除 key 强制重新发送 */
    private static final int MAX_ATTEMPTS = 5;

    @Override
    public void sendCaptcha(String captchaType, String target, String scene) {
        log.info("发送验证码，captchaType={}, scene={}", captchaType, scene);

        // 1. 冷却检查
        String cooldownKey = buildCooldownKey(scene, captchaType, target);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            log.warn("验证码发送过于频繁，target={}", target);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_TOO_FREQUENT);
        }

        // 2. 每日次数检查
        String dailyKey = buildDailyKey(scene, captchaType, target);
        String dailyCountStr = (String) redisTemplate.opsForValue().get(dailyKey);
        int dailyLimit = getDailyLimit();
        if (dailyCountStr != null && Integer.parseInt(dailyCountStr) >= dailyLimit) {
            log.warn("验证码每日发送次数已达上限，target={}", target);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_DAILY_LIMIT);
        }

        // 3. 生成 6 位数字验证码
        String code = RandomUtil.randomNumbers(6);
        log.info("生成验证码，scene={}, captchaType={}, target={}", scene, captchaType, target);

        // 4. 写入 Redis：验证码 key + 冷却 key + 每日计数原子自增
        int expireSeconds = getExpireSeconds();
        int cooldownSeconds = getCooldownSeconds();
        String captchaKey = buildCaptchaKey(scene, captchaType, target);
        redisTemplate.opsForValue().set(captchaKey, code, expireSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS);
        // 原子自增每日计数，首次写入后设置 TTL 至当天结束
        Long newCount = redisTemplate.opsForValue().increment(dailyKey);
        if (newCount != null && newCount == 1) {
            // 首次写入，设置到今天结束
            long secondsUntilMidnight = Duration.between(LocalDateTime.now(),
                    LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)).getSeconds();
            redisTemplate.expire(dailyKey, secondsUntilMidnight, TimeUnit.SECONDS);
        }

        // 5. 发送
        dispatch(captchaType, target, code);
        log.info("验证码发送成功，scene={}, captchaType={}", scene, captchaType);
    }

    @Override
    public void verifyCaptcha(String captchaType, String target, String scene, String code) {
        String captchaKey = buildCaptchaKey(scene, captchaType, target);
        String attemptsKey = buildAttemptsKey(scene, captchaType, target);

        // 1. 验证码是否存在
        String storedCode = (String) redisTemplate.opsForValue().get(captchaKey);
        if (StrUtil.isBlank(storedCode)) {
            log.warn("验证码不存在或已过期，scene={}, captchaType={}", scene, captchaType);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_EXPIRED);
        }

        // 2. 错误次数检查
        String attemptsStr = (String) redisTemplate.opsForValue().get(attemptsKey);
        if (attemptsStr != null && Integer.parseInt(attemptsStr) >= MAX_ATTEMPTS) {
            log.warn("验证码错误次数已达上限，删除 key 强制重新发送，scene={}, captchaType={}", scene, captchaType);
            redisTemplate.delete(captchaKey);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_EXPIRED);
        }

        // 3. 校验
        if (!storedCode.equals(code)) {
            // 错误次数原子自增，TTL 跟随验证码 key（取剩余 TTL）
            redisTemplate.opsForValue().increment(attemptsKey);
            Long ttl = redisTemplate.getExpire(captchaKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                redisTemplate.expire(attemptsKey, ttl, TimeUnit.SECONDS);
            }
            log.warn("验证码不匹配，scene={}, captchaType={}", scene, captchaType);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR);
        }

        // 4. 匹配成功，删除验证码 key 和错误次数 key（一次性消费）
        redisTemplate.delete(captchaKey);
        redisTemplate.delete(attemptsKey);
        log.info("验证码校验成功，scene={}, captchaType={}", scene, captchaType);
    }

    // ==================== 私有方法 ====================

    private void dispatch(String captchaType, String target, String code) {
        if (CaptchaTypeEnum.PHONE.getValue().equals(captchaType)) {
            smsService.send(target, "您的验证码为：" + code + "，" + getExpireSeconds() / 60 + "分钟内有效。");
        } else if (CaptchaTypeEnum.EMAIL.getValue().equals(captchaType)) {
            mailService.send(target, "【医工宝】验证码",
                    "您的验证码为：" + code + "，" + getExpireSeconds() / 60 + "分钟内有效，请勿泄露。");
        } else {
            log.error("不支持的验证码类型，captchaType={}", captchaType);
            throw new BusinessException(ErrorCodeEnum.CAPTCHA_TYPE_INVALID);
        }
    }

    private String buildCaptchaKey(String scene, String type, String target) {
        return "captcha:" + scene + ":" + type + ":" + target;
    }

    private String buildCooldownKey(String scene, String type, String target) {
        return "captcha:cooldown:" + scene + ":" + type + ":" + target;
    }

    private String buildDailyKey(String scene, String type, String target) {
        String date = LocalDate.now().toString().replace("-", "");
        return "captcha:daily:" + scene + ":" + type + ":" + target + ":" + date;
    }

    private String buildAttemptsKey(String scene, String type, String target) {
        return "captcha:attempts:" + scene + ":" + type + ":" + target;
    }

    private int getExpireSeconds() {
        String val = configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_EXPIRE_SECONDS.getKey());
        try { return StrUtil.isNotBlank(val) ? Integer.parseInt(val) : 300; } catch (NumberFormatException e) { return 300; }
    }

    private int getCooldownSeconds() {
        String val = configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_COOLDOWN_SECONDS.getKey());
        try { return StrUtil.isNotBlank(val) ? Integer.parseInt(val) : 60; } catch (NumberFormatException e) { return 60; }
    }

    private int getDailyLimit() {
        String val = configService.getConfigValue(SystemConfigKeyEnum.CAPTCHA_DAILY_LIMIT.getKey());
        try { return StrUtil.isNotBlank(val) ? Integer.parseInt(val) : 10; } catch (NumberFormatException e) { return 10; }
    }
}
```

- [ ] **Step 5: 运行测试，确认通过**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-system -Dtest=CaptchaServiceImplTest -q
```

Expected: Tests run: 8, Failures: 0, Errors: 0

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth/service yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/auth/service
git commit -m "feat(auth): 新增CaptchaService验证码服务（TDD，含冷却/每日上限/错误次数限制）"
```

---

## Task 6: UserMapper 新增查询 + UserService email 唯一性校验

**Files:**
- Modify: `system-user/mapper/UserMapper.java`
- Modify: `system-user/service/impl/UserServiceImpl.java`
- Modify: `test-auth/../user/service/impl/UserServiceImplTest.java`（追加用例）

- [ ] **Step 1: UserMapper 新增 selectByPhone / selectByEmail**

在 `system-user/mapper/UserMapper.java` 的现有 `selectByUsername` 后追加：

```java
/**
 * 根据手机号查询用户
 *
 * @param phone 手机号
 * @return 用户实体
 */
@Select("SELECT * FROM sys_user WHERE phone = #{phone} AND is_deleted = 0 LIMIT 1")
UserEntity selectByPhone(String phone);

/**
 * 根据邮箱查询用户
 *
 * @param email 邮箱
 * @return 用户实体
 */
@Select("SELECT * FROM sys_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
UserEntity selectByEmail(String email);
```

- [ ] **Step 2: UserServiceImpl 新增 email 唯一性校验**

在 `createUser` 方法的 `isPhoneExists` 校验后追加：

```java
// 校验邮箱是否已存在
if (StrUtil.isNotBlank(dto.getEmail()) && isEmailExists(dto.getEmail())) {
    log.warn("邮箱已存在，email={}", dto.getEmail());
    throw new BusinessException(ErrorCodeEnum.USER_EMAIL_EXISTS);
}
```

在 `updateUser` 方法的手机号唯一性校验后追加：

```java
// 校验邮箱是否与其他用户重复
if (StrUtil.isNotBlank(dto.getEmail()) && !dto.getEmail().equals(entity.getEmail())) {
    if (isEmailExistsExcludingId(dto.getEmail(), id)) {
        log.warn("邮箱已存在，email={}", dto.getEmail());
        throw new BusinessException(ErrorCodeEnum.USER_EMAIL_EXISTS);
    }
}
```

在私有方法区追加两个方法（与现有 `isPhoneExists` 系列方法放在一起）：

```java
/**
 * 校验邮箱是否存在
 */
private boolean isEmailExists(String email) {
    return count(new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getEmail, email)) > 0;
}

/**
 * 校验邮箱是否存在（排除指定ID）
 */
private boolean isEmailExistsExcludingId(String email, Long excludeId) {
    return count(new LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::getEmail, email)
            .ne(UserEntity::getId, excludeId)) > 0;
}
```

- [ ] **Step 3: 在 UserServiceImplTest 中追加 email 唯一性测试用例**

在现有测试类中追加（参考现有 `isPhoneExists` 相关测试用例的 mock 写法，`count()` 实际通过 `userMapper.selectCount()` 执行）：

```java
@Test
@DisplayName("createUser: 邮箱已存在时抛出 USER_EMAIL_EXISTS")
void createUser_whenEmailExists_shouldThrowException() {
    CreateUserDTO dto = buildMinimalCreateDTO();
    dto.setEmail("existing@example.com");

    when(userMapper.selectByUsername(any())).thenReturn(null);
    // selectCount 第一次调用（username 不重复）返回 0
    // 第二次调用（phone 不重复）返回 0
    // 第三次调用（email 已存在）返回 1
    when(userMapper.selectCount(any())).thenReturn(0L, 0L, 1L);

    BusinessException ex = assertThrows(BusinessException.class,
            () -> userService.createUser(dto));
    assertEquals(ErrorCodeEnum.USER_EMAIL_EXISTS.getCode(), ex.getCode());
}
```

> **提示**：若现有 `createUser` 用例中 `count()` 调用次序与上述不符（例如 email 校验在 username/phone 之前），根据实际源码中调用顺序调整 `thenReturn(...)` 参数的顺序即可。

- [ ] **Step 4: 运行 UserServiceImplTest**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-system -Dtest=UserServiceImplTest -q
```

Expected: 所有测试通过

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user
git commit -m "feat(user): UserMapper新增selectByPhone/Email；UserService补充email唯一性校验"
```

---

## Task 7: AuthService 重构（login 分发 + 忘记密码）

**Files:**
- Modify: `system-auth/service/AuthService.java`
- Modify: `system-auth/service/impl/AuthServiceImpl.java`
- Modify: `test-auth/service/impl/AuthServiceImplTest.java`

- [ ] **Step 1: AuthService 接口新增方法（共3个）**

在 `system-auth/service/AuthService.java` 追加：

```java
/**
 * 获取图形验证码
 *
 * @return captchaId + imageBase64
 */
GraphicCaptchaVO getGraphicCaptcha();

/**
 * 发送登录验证码（手机号/邮箱）
 *
 * @param dto 发送验证码请求
 */
void sendLoginCaptcha(SendCaptchaDTO dto);

/**
 * 忘记密码：发送验证码
 * 无论目标是否注册，均返回成功（防止用户枚举攻击）
 *
 * @param dto 发送验证码请求
 */
void sendForgotPasswordCaptcha(SendCaptchaDTO dto);

/**
 * 忘记密码：验证码校验 + 重置密码
 *
 * @param dto 重置密码请求
 */
void resetPassword(ForgotPasswordResetDTO dto);
```

- [ ] **Step 2: 重构 AuthServiceImpl.login() 及更新 saveLoginLog 签名**

首先更新 `saveLoginLog` 私有方法签名（在文件中搜索 `saveLoginLog`，找到其私有方法定义），将签名改为包含 `loginType` 参数：

```java
// 原签名（不含 loginType）
private void saveLoginLog(Long userId, String username, String ip, String userAgent, int status, String remark)

// 新签名（含 loginType）
private void saveLoginLog(Long userId, String username, String loginType, String ip, String userAgent, int status, String remark)
```

同步更新 `saveLoginLog` 方法体，将 `loginType` 写入 `LoginLogEntity`：

```java
entity.setLoginType(loginType);
```

然后更新文件中所有 `saveLoginLog` 调用处（在 `login()` 方法及异常分支中），在 `username` 参数后插入 `loginType` 参数。

完整重写 `login()` 方法，按 `loginType` 分发：

```java
@Override
public LoginVO login(LoginDTO dto) {
    log.info("用户登录，loginType={}", dto.getLoginType());
    String ip = getClientIp();
    String userAgent = getUserAgent();

    try {
        UserEntity user = resolveUser(dto, ip, userAgent);
        // 共用后置：状态检查
        checkUserStatus(user, dto.getPrincipal(), ip, userAgent);
        // 执行登录
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("realName", user.getRealName());
        String token = StpUtil.getTokenValue();
        // 记录登录日志
        saveLoginLog(user.getId(), user.getUsername(), dto.getLoginType().name(), ip, userAgent, 1, null);
        // 构建返回
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(UserConvert.toVO(user));
        loginVO.setMenus(resourceService.getUserMenuTree(user.getId()));
        loginVO.setPermissions(resourceService.getUserPermissions(user.getId()));
        log.info("用户登录成功，userId={}, loginType={}", user.getId(), dto.getLoginType());
        return loginVO;
    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        log.error("用户登录异常，loginType={}", dto.getLoginType(), e);
        saveLoginLog(null, dto.getPrincipal(), dto.getLoginType() != null ? dto.getLoginType().name() : null,
                ip, userAgent, 0, "系统异常");
        throw e;
    }
}
```

新增私有方法 `resolveUser()`（按 loginType 路由）：

```java
/**
 * 按登录类型解析用户，PASSWORD 路径包含密码校验和锁定机制
 */
private UserEntity resolveUser(LoginDTO dto, String ip, String userAgent) {
    return switch (dto.getLoginType()) {
        case PASSWORD -> resolveByPassword(dto, ip, userAgent);
        case PHONE -> resolveByPhone(dto);
        case EMAIL -> resolveByEmail(dto);
    };
}

private UserEntity resolveByPassword(LoginDTO dto, String ip, String userAgent) {
    UserEntity user = userMapper.selectByUsername(dto.getPrincipal());
    if (user == null) {
        log.warn("用户名不存在，username={}", dto.getPrincipal());
        saveLoginLog(null, dto.getPrincipal(), LoginTypeEnum.PASSWORD.name(), ip, userAgent, 0, "用户不存在");
        throw new BusinessException(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR);
    }
    // 锁定检查
    if (isAccountLocked(user)) {
        int remaining = calculateRemainingLockMinutes(user);
        saveLoginLog(user.getId(), user.getUsername(), LoginTypeEnum.PASSWORD.name(), ip, userAgent, 0, "账户已锁定");
        throw new BusinessException(ErrorCodeEnum.ACCOUNT_LOCKED, remaining);
    }
    // 密码校验
    if (!passwordEncoder.matches(dto.getCredential(), user.getPassword())) {
        handleLoginFailure(user, dto.getPrincipal(), ip, userAgent);
        throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
    }
    resetLoginFailCount(user);
    return user;
}

private UserEntity resolveByPhone(LoginDTO dto) {
    UserEntity user = userMapper.selectByPhone(dto.getPrincipal());
    if (user == null) {
        log.warn("手机号不存在，phone={}", dto.getPrincipal());
        throw new BusinessException(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR);
    }
    captchaService.verifyCaptcha(CaptchaTypeEnum.PHONE.getValue(), dto.getPrincipal(),
            CaptchaSceneEnum.LOGIN.getScene(), dto.getCredential());
    return user;
}

private UserEntity resolveByEmail(LoginDTO dto) {
    UserEntity user = userMapper.selectByEmail(dto.getPrincipal());
    if (user == null) {
        log.warn("邮箱不存在，email={}", dto.getPrincipal());
        throw new BusinessException(ErrorCodeEnum.USERNAME_OR_PASSWORD_ERROR);
    }
    captchaService.verifyCaptcha(CaptchaTypeEnum.EMAIL.getValue(), dto.getPrincipal(),
            CaptchaSceneEnum.LOGIN.getScene(), dto.getCredential());
    return user;
}

private void checkUserStatus(UserEntity user, String principal, String ip, String userAgent) {
    if (Integer.valueOf(StatusConstants.DISABLED).equals(user.getStatus())) {
        log.warn("用户已禁用，userId={}", user.getId());
        saveLoginLog(user.getId(), user.getUsername(), null, ip, userAgent, 0, "用户已禁用");
        throw new BusinessException(ErrorCodeEnum.USER_DISABLED);
    }
}
```

新增忘记密码方法：

```java
@Override
public void sendForgotPasswordCaptcha(SendCaptchaDTO dto) {
    log.info("忘记密码：发送验证码，captchaType={}", dto.getCaptchaType());
    // 查找用户（不论是否存在，均返回成功，防止枚举攻击）
    UserEntity user = CaptchaTypeEnum.PHONE.equals(dto.getCaptchaType())
            ? userMapper.selectByPhone(dto.getTarget())
            : userMapper.selectByEmail(dto.getTarget());
    if (user == null || Integer.valueOf(StatusConstants.DISABLED).equals(user.getStatus())) {
        // 静默丢弃，不发验证码，不抛异常
        log.info("忘记密码：用户不存在或已禁用，静默处理，target={}", dto.getTarget());
        return;
    }
    captchaService.sendCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(),
            CaptchaSceneEnum.FORGOT.getScene());
    log.info("忘记密码：验证码发送成功，captchaType={}", dto.getCaptchaType());
}

@Override
@Transactional(rollbackFor = Exception.class)
public void resetPassword(ForgotPasswordResetDTO dto) {
    log.info("忘记密码：重置密码，captchaType={}", dto.getCaptchaType());
    // 1. 校验验证码
    captchaService.verifyCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(),
            CaptchaSceneEnum.FORGOT.getScene(), dto.getCaptcha());
    // 2. 查找用户
    UserEntity user = CaptchaTypeEnum.PHONE.equals(dto.getCaptchaType())
            ? userMapper.selectByPhone(dto.getTarget())
            : userMapper.selectByEmail(dto.getTarget());
    if (user == null) {
        throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
    }
    // 3. 密码强度校验（复用 UserServiceImpl 逻辑，此处内联同样规则）
    if (!isPasswordStrong(dto.getNewPassword())) {
        throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_WEAK);
    }
    // 4. 更新密码
    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    userMapper.updateById(user);
    // 5. 踢出所有在线 Session
    StpUtil.logoutByLoginId(user.getId());
    log.info("忘记密码：密码重置成功，userId={}", user.getId());
}
```

同时更新 `saveLoginLog` 方法签名，加入 `loginType` 参数，更新全部调用处。

新增 `sendLoginCaptcha` 实现（与 `sendForgotPasswordCaptcha` 放在一起）：

```java
@Override
public void sendLoginCaptcha(SendCaptchaDTO dto) {
    log.info("发送登录验证码，captchaType={}", dto.getCaptchaType());
    captchaService.sendCaptcha(dto.getCaptchaType().getValue(), dto.getTarget(),
            CaptchaSceneEnum.LOGIN.getScene());
}
```

新增 `GraphicCaptchaVO`：

文件：`system-auth/vo/GraphicCaptchaVO.java`

```java
package com.yigongbao.module.system.auth.vo;

import lombok.Data;

/**
 * 图形验证码响应 VO
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Data
public class GraphicCaptchaVO {

    /** 验证码 ID，前端登录时随 LoginDTO 回传 */
    private String captchaId;

    /** Base64 格式图片，前缀 data:image/png;base64, */
    private String imageBase64;
}
```

新增 `getGraphicCaptcha` 实现：

```java
/** 图形验证码有效期（秒），硬编码 5 分钟 */
private static final long GRAPHIC_CAPTCHA_TTL = 300L;

@Override
public GraphicCaptchaVO getGraphicCaptcha() {
    // 生成 4 位字母+数字图形验证码
    cn.hutool.captcha.LineCaptcha captcha =
            cn.hutool.captcha.CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
    String captchaId = cn.hutool.core.util.IdUtil.fastSimpleUUID();
    String code = captcha.getCode().toLowerCase();

    // 写入 Redis，TTL 5 分钟
    redisTemplate.opsForValue().set("graphic:captcha:" + captchaId, code,
            GRAPHIC_CAPTCHA_TTL, java.util.concurrent.TimeUnit.SECONDS);

    GraphicCaptchaVO vo = new GraphicCaptchaVO();
    vo.setCaptchaId(captchaId);
    vo.setImageBase64("data:image/png;base64," + captcha.getImageBase64());
    log.info("图形验证码生成成功，captchaId={}", captchaId);
    return vo;
}
```

同时在 `resolveByPassword` 方法**最开头**（在 `selectByUsername` 之前）加入图形验证码校验：

```java
private UserEntity resolveByPassword(LoginDTO dto, String ip, String userAgent) {
    // 图形验证码校验（PASSWORD 路径专属）
    verifyGraphicCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());

    // 后续原有逻辑不变...
    UserEntity user = userMapper.selectByUsername(dto.getPrincipal());
    ...
}

/**
 * 校验图形验证码，匹配后删除 key（一次性消费）
 */
private void verifyGraphicCaptcha(String captchaId, String captchaCode) {
    String key = "graphic:captcha:" + captchaId;
    if (StrUtil.isBlank(captchaId)) {
        throw new BusinessException(ErrorCodeEnum.CAPTCHA_GRAPHIC_EXPIRED);
    }
    String stored = (String) redisTemplate.opsForValue().get(key);
    if (StrUtil.isBlank(stored)) {
        throw new BusinessException(ErrorCodeEnum.CAPTCHA_GRAPHIC_EXPIRED);
    }
    if (!stored.equals(StrUtil.isBlank(captchaCode) ? "" : captchaCode.toLowerCase())) {
        // 不匹配立即删除，防止暴力枚举
        redisTemplate.delete(key);
        throw new BusinessException(ErrorCodeEnum.CAPTCHA_GRAPHIC_ERROR);
    }
    // 匹配，消费
    redisTemplate.delete(key);
}
```

- [ ] **Step 3: 注入 CaptchaService**

在 `AuthServiceImpl` 的字段声明中追加：

```java
private final CaptchaService captchaService;
```

（`@RequiredArgsConstructor` 自动处理）

- [ ] **Step 4: 更新 AuthServiceImplTest**

在 `@Mock` 注解区追加：
```java
@Mock
private CaptchaService captchaService;
```

更新 `setUp()` 中的 `loginDTO`（旧字段已不存在，改用新结构）：

```java
loginDTO = new LoginDTO();
loginDTO.setLoginType(LoginTypeEnum.PASSWORD);
loginDTO.setPrincipal("admin");
loginDTO.setCredential("123456");
```

更新所有引用 `loginDTO.setUsername()` / `loginDTO.setPassword()` 的地方为 `setPrincipal()` / `setCredential()`，同时更新 `userMapper.selectByUsername` mock 保持原样。

追加图形验证码测试用例（在 PASSWORD 登录测试用例附近）：

```java
@Test
@DisplayName("login: PASSWORD 类型图形验证码过期时抛出 CAPTCHA_GRAPHIC_EXPIRED")
void login_password_whenGraphicCaptchaExpired_shouldThrow() {
    loginDTO.setLoginType(LoginTypeEnum.PASSWORD);
    loginDTO.setCaptchaId("uuid-123");
    loginDTO.setCaptchaCode("abcd");

    when(redisTemplate.opsForValue().get("graphic:captcha:uuid-123")).thenReturn(null);

    BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
    assertEquals(ErrorCodeEnum.CAPTCHA_GRAPHIC_EXPIRED.getCode(), ex.getCode());
}

@Test
@DisplayName("login: PASSWORD 类型图形验证码错误时抛出 CAPTCHA_GRAPHIC_ERROR")
void login_password_whenGraphicCaptchaWrong_shouldThrow() {
    loginDTO.setLoginType(LoginTypeEnum.PASSWORD);
    loginDTO.setCaptchaId("uuid-123");
    loginDTO.setCaptchaCode("wrong");

    when(redisTemplate.opsForValue().get("graphic:captcha:uuid-123")).thenReturn("abcd");

    BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
    assertEquals(ErrorCodeEnum.CAPTCHA_GRAPHIC_ERROR.getCode(), ex.getCode());
    verify(redisTemplate).delete("graphic:captcha:uuid-123");
}
```

> 图形验证码校验需要 `RedisTemplate`，在 `AuthServiceImpl` 测试的 `@Mock` 中加入：
> ```java
> @Mock
> private RedisTemplate<String, Object> redisTemplate;
> @Mock
> private ValueOperations<String, Object> valueOps;
> ```
> 并在 `setUp()` 中加 `when(redisTemplate.opsForValue()).thenReturn(valueOps);`

追加新场景测试用例：

```java
@Test
@DisplayName("login: PHONE 类型验证码正确时登录成功")
void login_phoneType_whenCaptchaCorrect_shouldSuccess() {
    loginDTO.setLoginType(LoginTypeEnum.PHONE);
    loginDTO.setPrincipal("13800000001");
    loginDTO.setCredential("123456");

    when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
    doNothing().when(captchaService).verifyCaptcha(eq("PHONE"), eq("13800000001"), eq("login"), eq("123456"));
    stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
    when(loginLogMapper.insert(any())).thenReturn(1);
    when(resourceService.getUserMenuTree(1L)).thenReturn(new ArrayList<>());
    when(resourceService.getUserPermissions(1L)).thenReturn(List.of());

    LoginVO result = authService.login(loginDTO);
    assertNotNull(result);
    assertEquals("mock-token", result.getToken());
}

@Test
@DisplayName("login: PHONE 类型验证码错误时抛出异常")
void login_phoneType_whenCaptchaError_shouldThrow() {
    loginDTO.setLoginType(LoginTypeEnum.PHONE);
    loginDTO.setPrincipal("13800000001");
    loginDTO.setCredential("999999");

    when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
    doThrow(new BusinessException(ErrorCodeEnum.CAPTCHA_ERROR))
            .when(captchaService).verifyCaptcha(eq("PHONE"), eq("13800000001"), eq("login"), eq("999999"));

    BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginDTO));
    assertEquals(ErrorCodeEnum.CAPTCHA_ERROR.getCode(), ex.getCode());
}

@Test
@DisplayName("login: EMAIL 类型验证码正确时登录成功")
void login_emailType_whenCaptchaCorrect_shouldSuccess() {
    testUser.setEmail("admin@example.com");
    loginDTO.setLoginType(LoginTypeEnum.EMAIL);
    loginDTO.setPrincipal("admin@example.com");
    loginDTO.setCredential("654321");

    when(userMapper.selectByEmail("admin@example.com")).thenReturn(testUser);
    doNothing().when(captchaService).verifyCaptcha(eq("EMAIL"), eq("admin@example.com"), eq("login"), eq("654321"));
    stpUtilMockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");
    when(loginLogMapper.insert(any())).thenReturn(1);
    when(resourceService.getUserMenuTree(1L)).thenReturn(new ArrayList<>());
    when(resourceService.getUserPermissions(1L)).thenReturn(List.of());

    LoginVO result = authService.login(loginDTO);
    assertNotNull(result);
}

@Test
@DisplayName("sendForgotPasswordCaptcha: 用户不存在时静默处理不抛异常")
void sendForgotPasswordCaptcha_whenUserNotExists_shouldSilentlyReturn() {
    SendCaptchaDTO dto = new SendCaptchaDTO();
    dto.setCaptchaType(CaptchaTypeEnum.PHONE);
    dto.setTarget("13900000000");

    when(userMapper.selectByPhone("13900000000")).thenReturn(null);

    assertDoesNotThrow(() -> authService.sendForgotPasswordCaptcha(dto));
    verify(captchaService, never()).sendCaptcha(any(), any(), any());
}

@Test
@DisplayName("resetPassword: 验证码正确时重置密码并踢出 Session")
void resetPassword_whenCaptchaCorrect_shouldResetAndLogout() {
    ForgotPasswordResetDTO dto = new ForgotPasswordResetDTO();
    dto.setCaptchaType(CaptchaTypeEnum.PHONE);
    dto.setTarget("13800000001");
    dto.setCaptcha("123456");
    dto.setNewPassword("newPass1");

    doNothing().when(captchaService).verifyCaptcha(any(), any(), any(), any());
    when(userMapper.selectByPhone("13800000001")).thenReturn(testUser);
    when(passwordEncoder.encode("newPass1")).thenReturn("encoded");
    when(userMapper.updateById(any())).thenReturn(1);

    assertDoesNotThrow(() -> authService.resetPassword(dto));
    verify(userMapper, times(1)).updateById(any());
    stpUtilMockedStatic.verify(() -> StpUtil.logoutByLoginId(1L), times(1));
}
```

- [ ] **Step 5: 运行 AuthServiceImplTest**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-system -Dtest=AuthServiceImplTest -q
```

Expected: 所有测试通过

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth/service yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/auth/service
git commit -m "feat(auth): 重构login为三路分发，新增忘记密码发验证码和重置密码方法"
```

---

## Task 8: AuthController 新增端点 + 集成测试

**Files:**
- Modify: `system-auth/controller/AuthController.java`
- Modify: `test-auth/controller/AuthControllerTest.java`

- [ ] **Step 1: AuthController 新增 5 个端点**

在现有端点后追加：

```java
/**
 * 获取图形验证码
 */
@Operation(summary = "获取图形验证码")
@GetMapping("/graphic-captcha")
public Result<GraphicCaptchaVO> getGraphicCaptcha() {
    return Result.success(authService.getGraphicCaptcha());
}

/**
 * 发送登录验证码
 */
@Operation(summary = "发送登录验证码（手机号/邮箱）")
@PostMapping("/captcha")
public Result<Void> sendLoginCaptcha(@Validated @RequestBody SendCaptchaDTO dto) {
    authService.sendLoginCaptcha(dto);
    return Result.success();
}

/**
 * 忘记密码：发送验证码
 */
@Operation(summary = "忘记密码：发送验证码")
@PostMapping("/forgot-password/captcha")
public Result<Void> sendForgotPasswordCaptcha(@Validated @RequestBody SendCaptchaDTO dto) {
    authService.sendForgotPasswordCaptcha(dto);
    return Result.success();
}

/**
 * 忘记密码：验证码校验 + 重置密码
 */
@Operation(summary = "忘记密码：验证码校验并重置密码")
@PostMapping("/forgot-password/reset")
public Result<Void> resetPassword(@Validated @RequestBody ForgotPasswordResetDTO dto) {
    authService.resetPassword(dto);
    return Result.success();
}
```

- [ ] **Step 2: 更新 AuthControllerTest**

在已有测试的 login 测试中，将 `requestBody.put("username", ...)` 改为新字段结构：

```java
// 原：requestBody.put("username", "admin"); requestBody.put("password", "123456");
// 改为：
requestBody.put("loginType", "PASSWORD");
requestBody.put("principal", "admin");
requestBody.put("credential", "123456");
```

更新所有 login 测试用例的 requestBody 构建方式，然后追加新场景：

```java
@Test
@DisplayName("login: loginType 为空时返回 400")
void login_whenLoginTypeNull_shouldReturn400() throws Exception {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("principal", "admin");
    requestBody.put("credential", "123456");
    // loginType 缺失

    mockMvc.perform(post("/system/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
}

@Test
@DisplayName("login: loginType 非法值时返回 400")
void login_whenLoginTypeInvalid_shouldReturn400() throws Exception {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("loginType", "INVALID_TYPE");
    requestBody.put("principal", "admin");
    requestBody.put("credential", "123456");

    mockMvc.perform(post("/system/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
}

@Test
@DisplayName("sendForgotPasswordCaptcha: captchaType 为空时返回 400")
void sendForgotPasswordCaptcha_whenTypeNull_shouldReturn400() throws Exception {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("target", "13800138000");
    // captchaType 缺失

    mockMvc.perform(post("/system/auth/forgot-password/captcha")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
}

@Test
@DisplayName("sendForgotPasswordCaptcha: 目标不存在时仍返回成功（防枚举攻击）")
void sendForgotPasswordCaptcha_whenTargetNotExists_shouldReturnSuccess() throws Exception {
    // 注意：Controller 测试中 CaptchaService 依赖 Redis，该测试会因 Redis 不可用而失败
    // 此场景在 AuthServiceImplTest 中已覆盖（单元测试），Controller 层仅验证参数格式
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("captchaType", "PHONE");
    requestBody.put("target", "13900000000");

    // 仅验证接口可调通（不验证业务逻辑，业务测试在 ServiceImplTest 中）
    mockMvc.perform(post("/system/auth/forgot-password/captcha")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk());
}
```

> **注意**：`AuthControllerTest` 是集成测试（`@SpringBootTest`），`CaptchaServiceImpl` 依赖真实 Redis。需在 `application-test.yml` 中禁用 Redis 自动连接，或在测试类中用 `@MockBean` 替换 `CaptchaService`。**推荐方案**：在测试类中添加：
> ```java
> @MockBean
> private CaptchaService captchaService;
> @MockBean
> private JavaMailSender javaMailSender;
> ```
> 这样 `sendLoginCaptcha` 和 `sendForgotPasswordCaptcha` 端点可正常调用，不依赖 Redis。

- [ ] **Step 3: 运行 AuthControllerTest**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-system -Dtest=AuthControllerTest -q
```

Expected: 所有测试通过

- [ ] **Step 4: 运行全量测试**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-system -q
```

Expected: BUILD SUCCESS，所有测试通过

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/auth/controller yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/auth/controller
git commit -m "feat(auth): 新增发送验证码、忘记密码4个端点及集成测试"
```

---

## Task 9: Spring Mail 配置 + 最终验证

**Files:**
- Modify: `yigongbao-boot/src/main/resources/application-dev.yml`

- [ ] **Step 1: 在 application-dev.yml 中追加 Spring Mail 配置**

```yaml
spring:
  mail:
    host: smtp.example.com        # 替换为实际 SMTP 服务器
    port: 465
    username: ${MAIL_USERNAME:}   # 通过环境变量注入
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.ssl.enable: true
```

- [ ] **Step 2: 运行全模块编译**

```bash
cd yigongbao-parent
mvn clean package -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 运行全量测试**

```bash
cd yigongbao-parent
mvn test -q
```

Expected: BUILD SUCCESS，无 FAIL

- [ ] **Step 4: 最终 Commit**

```bash
git add yigongbao-parent/yigongbao-boot/src/main/resources/application-dev.yml
git commit -m "feat(auth): 添加Spring Mail配置；多登录方式功能完整实现"
```

---

## 验收标准

1. `POST /system/auth/login` 支持 `loginType=PASSWORD/PHONE/EMAIL` 三种方式
2. `POST /system/auth/captcha` 可发送登录验证码（PHONE 打日志，EMAIL 通过 SMTP 发送）
3. `POST /system/auth/forgot-password/captcha` 无论目标是否存在均返回 200
4. `POST /system/auth/forgot-password/reset` 验证码正确时重置密码并踢出 Session
5. 创建/更新用户时 email 重复抛 `USER_EMAIL_EXISTS(636)`
6. 全量测试通过：`mvn test -pl yigongbao-module-system -q`
