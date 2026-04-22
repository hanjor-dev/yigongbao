# 多登录方式 + 忘记密码重置 设计文档

**日期**：2026-04-22
**作者**：hanjor
**状态**：已确认，待实现

---

## 一、背景与目标

当前系统仅支持 `username + password` 单一登录方式。本次扩展支持：

1. **手机号 + 短信验证码** 登录
2. **邮箱 + 邮件验证码** 登录
3. **忘记密码**：通过手机或邮箱验证码校验后重设密码

前提：手机号、邮箱在用户创建/更新时严格校验唯一性（email 补建唯一索引）。

---

## 二、接口清单

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| `POST` | `/system/auth/captcha` | 发送登录验证码 | 否 |
| `POST` | `/system/auth/login` | 统一登录（PASSWORD/PHONE/EMAIL） | 否 |
| `POST` | `/system/auth/forgot-password/captcha` | 忘记密码：发送验证码 | 否 |
| `POST` | `/system/auth/forgot-password/reset` | 忘记密码：验证码校验 + 设置新密码 | 否 |

现有 `PUT /system/auth/password`（已登录修改密码）保持不变。

---

## 三、DTO 结构

### LoginDTO（重构）

```json
{
  "loginType": "PHONE",
  "principal": "13800138000",
  "credential": "123456"
}
```

- `loginType`：`PASSWORD` / `PHONE` / `EMAIL`
- `principal`：用户名 / 手机号 / 邮箱
- `credential`：密码 / 验证码

### SendCaptchaDTO

```json
{
  "captchaType": "PHONE",
  "target": "13800138000"
}
```

### ForgotPasswordResetDTO

```json
{
  "captchaType": "PHONE",
  "target": "13800138000",
  "captcha": "123456",
  "newPassword": "abc123"
}
```

---

## 四、枚举

### LoginTypeEnum

```java
PASSWORD, PHONE, EMAIL
```

### CaptchaSceneEnum

```java
LOGIN("login"),
FORGOT("forgot")
```

用于隔离 Redis key 前缀，防止登录验证码与忘记密码验证码跨场景复用。

---

## 五、数据层变更

### 5.1 DDL 变更（sql/ddl.sql）

**sys_user 补充唯一索引：**

```sql
-- email 唯一（逻辑删除兼容，函数索引）
CREATE UNIQUE INDEX uk_email
    ON sys_user ((CASE WHEN is_deleted = 0 THEN email ELSE NULL END));

-- employee_no 唯一（本期不作登录凭据，但同步补上约束）
CREATE UNIQUE INDEX uk_employee_no
    ON sys_user ((CASE WHEN is_deleted = 0 THEN employee_no ELSE NULL END));
```

**sys_login_log 新增字段：**

```sql
ALTER TABLE sys_login_log
    ADD COLUMN login_type VARCHAR(16) COMMENT '登录方式（PASSWORD/PHONE/EMAIL）' AFTER username;
```

### 5.2 sys_config 新增配置项（sql/init.sql）

在 `security` 组追加：

| config_key | 默认值 | 说明 |
|---|---|---|
| `captcha.expire.seconds` | `300` | 验证码有效期（秒） |
| `captcha.cooldown.seconds` | `60` | 同一目标发送冷却（秒） |
| `captcha.daily.limit` | `10` | 同一目标每日最大发送次数 |
| `mail.from` | — | 发件人邮箱地址（必填） |

> `sms.send.interval` 保留向后兼容，新代码统一读 `captcha.cooldown.seconds`。

### 5.3 SystemConfigKeyEnum 新增键

```java
CAPTCHA_EXPIRE_SECONDS("captcha.expire.seconds"),
CAPTCHA_COOLDOWN_SECONDS("captcha.cooldown.seconds"),
CAPTCHA_DAILY_LIMIT("captcha.daily.limit"),
MAIL_FROM("mail.from")
```

---

## 六、Redis 存储结构

| Key 模式 | TTL | 说明 |
|---|---|---|
| `captcha:{scene}:{type}:{target}` | `captcha.expire.seconds`（默认 300s） | 验证码值 |
| `captcha:cooldown:{scene}:{type}:{target}` | `captcha.cooldown.seconds`（默认 60s） | 冷却标志 |
| `captcha:daily:{scene}:{type}:{target}:{date}` | 当天剩余秒数 | 每日发送计数 |

示例：
- `captcha:login:PHONE:13800138000` → `"123456"`，TTL 300s
- `captcha:cooldown:login:PHONE:13800138000` → `"1"`，TTL 60s
- `captcha:daily:login:PHONE:13800138000:20260422` → `"3"`，TTL 至当天结束

---

## 七、新增服务

### 7.1 CaptchaService

位置：`auth/service/CaptchaService.java` + `impl/CaptchaServiceImpl.java`

```java
// 发送验证码
void sendCaptcha(String captchaType, String target, String scene);

// 校验验证码（匹配后立即删除，防重放）
void verifyCaptcha(String captchaType, String target, String scene, String code);
```

**sendCaptcha 流程：**
1. 冷却检查：Redis `captcha:cooldown:*` 存在 → 抛 `CAPTCHA_TOO_FREQUENT`
2. 每日次数检查：计数 ≥ `captcha.daily.limit` → 抛 `CAPTCHA_DAILY_LIMIT`
3. 生成 6 位数字验证码（`RandomUtil.randomNumbers(6)`）
4. 写入 Redis：验证码 key（TTL=有效期）+ 冷却 key（TTL=冷却时间）+ 每日计数 +1
5. 分发：`PHONE` → `SmsService.send()`，`EMAIL` → `MailService.send()`

**verifyCaptcha 流程：**
1. 读取 Redis 验证码 key，不存在 → 抛 `CAPTCHA_EXPIRED`
2. 不匹配 → 抛 `CAPTCHA_ERROR`
3. 匹配 → 删除 key（一次性消费）

### 7.2 SmsService / MailService

```
auth/service/SmsService.java             ← void send(String phone, String content)
auth/service/MailService.java            ← void send(String to, String subject, String content)
auth/service/impl/MockSmsServiceImpl.java    ← log.info 模拟，@Primary
auth/service/impl/SpringMailServiceImpl.java ← JavaMailSender 真实发送
```

**短信 Mock**：`log.info("【短信模拟】手机号={}，验证码={}", phone, content)`。后续接入服务商只需新增实现类并切换 `@Primary`，无需改动调用方。

**邮件实现**：注入 `JavaMailSender`，发件人从 `ConfigService.getConfigValue(MAIL_FROM)` 读取。

---

## 八、登录逻辑重构

### AuthServiceImpl.login() 分发逻辑

```
loginType == PASSWORD → 现有逻辑（selectByUsername + BCrypt 校验 + 锁定机制）
loginType == PHONE    → selectByPhone(phone) + captchaService.verifyCaptcha(PHONE, phone, "login", code)
loginType == EMAIL    → selectByEmail(email) + captchaService.verifyCaptcha(EMAIL, email, "login", code)
```

三条路径**共用后置逻辑**（提取私有方法）：
1. 用户存在性检查 → `USER_NOT_FOUND` / `USERNAME_OR_PASSWORD_ERROR`
2. 用户状态检查（禁用）→ `USER_DISABLED`
3. `StpUtil.login(userId)`
4. Session 写入 `username` / `realName`
5. `saveLoginLog`（含 `loginType` 字段）

**锁定机制仅作用于 PASSWORD 路径**（验证码路径无失败计数）。

### UserMapper 新增

```java
@Select("SELECT * FROM sys_user WHERE phone = #{phone} AND is_deleted = 0 LIMIT 1")
UserEntity selectByPhone(String phone);

@Select("SELECT * FROM sys_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
UserEntity selectByEmail(String email);
```

---

## 九、忘记密码流程

### POST /forgot-password/captcha

1. 按 `captchaType` 查找用户（`selectByPhone` / `selectByEmail`）
2. 用户不存在 → 抛 `USER_NOT_FOUND`
3. 用户已禁用 → 抛 `USER_DISABLED`
4. `captchaService.sendCaptcha(type, target, "forgot")`

### POST /forgot-password/reset

1. `captchaService.verifyCaptcha(type, target, "forgot", captcha)`
2. 按 `captchaType` 查找用户
3. 新密码强度校验（`isPasswordStrong`，复用现有方法）
4. `passwordEncoder.encode(newPassword)` 更新密码
5. `StpUtil.logoutByLoginId(userId)` 踢出所有在线 Session

> 密码重置后强制踢出，防止密码泄露后攻击者仍持有有效 Token。

---

## 十、UserService 唯一性校验补充

`createUser`：新增 `isEmailExists(email)` → 抛 `USER_EMAIL_EXISTS`
`updateUser`：新增 `isEmailExistsExcludingId(email, id)` → 抛 `USER_EMAIL_EXISTS`

---

## 十一、新增错误码

| 错误码 | 枚举值 | 说明 |
|---|---|---|
| 636 | `USER_EMAIL_EXISTS` | 邮箱已存在 |
| 637 | `CAPTCHA_TOO_FREQUENT` | 发送过于频繁，请稍后再试 |
| 638 | `CAPTCHA_DAILY_LIMIT` | 今日发送次数已达上限 |
| 639 | `CAPTCHA_EXPIRED` | 验证码已过期或不存在 |
| 640 | `CAPTCHA_ERROR` | 验证码错误 |
| 641 | `CAPTCHA_TYPE_INVALID` | 不支持的验证码类型 |

---

## 十二、Spring Mail 配置

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 465
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.ssl.enable: true
```

发件人地址通过 `sys_config.mail.from` 配置，运行时读取，不硬编码。

---

## 十三、测试策略

| 测试类 | 覆盖点 |
|---|---|
| `CaptchaServiceImplTest` | 冷却拦截、每日上限、验证码过期、验证码错误、正常发送、正常校验（含 key 删除） |
| `AuthServiceImplTest` | PASSWORD/PHONE/EMAIL 三条登录路径成功+失败、用户禁用、loginType 非法值 |
| `AuthControllerTest` | 参数校验（principal/credential 为空、loginType 非法枚举）、统一响应格式 |
| `UserServiceImplTest` | createUser email 重复、updateUser email 排除自身 |

> `CaptchaServiceImplTest` 中 `RedisTemplate` 使用 Mockito Mock，不依赖真实 Redis。

---

## 十四、改动文件清单

### 新增（10 个）

| 文件 | 说明 |
|---|---|
| `auth/dto/SendCaptchaDTO` | 发送验证码请求 |
| `auth/dto/ForgotPasswordResetDTO` | 忘记密码重置请求 |
| `auth/enums/LoginTypeEnum` | PASSWORD / PHONE / EMAIL |
| `auth/enums/CaptchaSceneEnum` | LOGIN / FORGOT |
| `auth/service/CaptchaService` | 验证码服务接口 |
| `auth/service/impl/CaptchaServiceImpl` | 验证码服务实现 |
| `auth/service/SmsService` | 短信服务接口 |
| `auth/service/MailService` | 邮件服务接口 |
| `auth/service/impl/MockSmsServiceImpl` | 短信 Mock（@Primary） |
| `auth/service/impl/SpringMailServiceImpl` | 邮件真实实现 |

### 修改（9 个）

| 文件 | 改动 |
|---|---|
| `auth/dto/LoginDTO` | 重构为 loginType + principal + credential |
| `auth/entity/LoginLogEntity` | 新增 loginType 字段 |
| `auth/service/AuthService` | 新增忘记密码接口方法 |
| `auth/service/impl/AuthServiceImpl` | login 分发 + 忘记密码实现 |
| `auth/controller/AuthController` | 新增 4 个端点 |
| `user/mapper/UserMapper` | 新增 selectByPhone、selectByEmail |
| `user/service/impl/UserServiceImpl` | email 唯一性校验 |
| `common/enums/ErrorCodeEnum` | 新增 636-641 |
| `common/enums/SystemConfigKeyEnum` | 新增 4 个配置键 |

### SQL 变更（2 个）

| 文件 | 改动 |
|---|---|
| `sql/ddl.sql` | email/employee_no 唯一索引 + login_log 新增 login_type 字段 |
| `sql/init.sql` | sys_config 新增 4 个配置项 |
