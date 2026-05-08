# 限流防刷 + 请求签名验证 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 SaToken Token 认证基础上，新增两层安全防护：① 全局限流防刷（基础层）；② 核心接口请求签名验证（增强层）。

**Architecture:** 纯 AOP 注解体系，复用现有 `RedisTemplate<String, Object>`（已配置于 `yigongbao-framework`）。新增 `@RateLimit` + `RateLimitAspect`（固定窗口 Lua 脚本）和 `@RequireSign` + `SignAspect`（MD5 签名 + nonce 防重放）。无需引入新依赖。

**Tech Stack:** Spring Boot 3.2.5, Redis (Lettuce), AOP (`spring-aspects`), `org.springframework.util.DigestUtils`（spring-web 自带）, JUnit 5 + Mockito

---

## 文件清单

### 修改文件

| 文件 | 改动 |
|---|---|
| `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java` | 新增错误码 780-784 |
| `yigongbao-boot/src/main/resources/application-dev.yml` | 新增 `app.sign.secret` 配置 |

### 新增文件

| 文件 | 说明 |
|---|---|
| `yigongbao-framework/src/main/java/com/yigongbao/framework/annotation/RateLimit.java` | 限流注解 |
| `yigongbao-framework/src/main/java/com/yigongbao/framework/annotation/RequireSign.java` | 签名验证注解 |
| `yigongbao-framework/src/main/java/com/yigongbao/framework/aspect/RateLimitAspect.java` | 限流切面，`@Order(1)` |
| `yigongbao-framework/src/main/java/com/yigongbao/framework/aspect/SignAspect.java` | 签名验证切面，`@Order(2)` |

---

## 实现步骤

- [ ] **Step 1：ErrorCodeEnum 新增错误码**

  在 `ORG_USERNAME_PREFIX_EXISTS(779, ...)` 末尾分号改逗号，追加：

  ```java
  // ==================== 限流 780 ====================
  RATE_LIMIT_EXCEEDED(429, "操作过于频繁，请稍后再试"),

  // ==================== 签名验证 781-784 ====================
  SIGN_PARAM_MISSING(781, "缺少签名参数：%s"),
  SIGN_TIMESTAMP_EXPIRED(782, "请求已过期，请检查系统时间"),
  SIGN_NONCE_USED(783, "重复请求"),
  SIGN_INVALID(784, "签名验证失败");
  ```

  > `GlobalExceptionHandler` 对 `BusinessException` 统一返回 HTTP 200，业务码在 body，**无需修改异常处理器**。

- [ ] **Step 2：application-dev.yml 新增配置**

  ```yaml
  app:
    sign:
      secret: ${APP_SIGN_SECRET:dev-secret-change-in-prod}
  ```

  生产环境通过环境变量 `APP_SIGN_SECRET` 注入，不提交明文到 git。

- [ ] **Step 3：新增 `@RateLimit` 注解**

  ```java
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface RateLimit {
      int limit() default 60;    // 窗口内最大请求数
      int window() default 60;   // 时间窗口（秒）
      Dimension dimension() default Dimension.USER;

      enum Dimension { IP, USER }  // USER：已登录按用户ID，未登录降级为IP
  }
  ```

- [ ] **Step 4：新增 `RateLimitAspect`**

  Redis key 格式：
  - `rate:ip:{ip}:{uri}` — IP 维度
  - `rate:user:{userId}:{uri}` — 用户维度（uri 中 `/` 替换为 `_`）

  Lua 脚本（固定窗口，INCR + EXPIRE 原子操作）：
  ```lua
  local c = redis.call('INCR', KEYS[1])
  if c == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end
  if c > tonumber(ARGV[2]) then return 0 end
  return 1
  ```

  关键逻辑：
  - `@Order(1)` — 先于 SignAspect 执行
  - Redis 不可用时 catch 异常 → fail-open 放行 + WARN 日志
  - `getClientIp()` 逻辑复制自 `OperationLogAspect`（`IpLocationUtil` 不含此方法）

  使用示例：
  ```java
  // 登录接口：10次/分钟，强制 IP 维度
  @RateLimit(limit = 10, window = 60, dimension = RateLimit.Dimension.IP)
  public Result<LoginVO> login(...) {}

  // 通用接口：60次/分钟，按用户（默认）
  @RateLimit
  public Result<IPage<OrderVO>> listOrders(...) {}
  ```

- [ ] **Step 5：新增 `@RequireSign` 注解**

  ```java
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface RequireSign {}
  ```

- [ ] **Step 6：新增 `SignAspect`**

  请求 Header：`X-App-Key` | `X-Timestamp`（秒级） | `X-Nonce`（6位随机串） | `X-Signature`

  签名算法：`MD5(appKey + timestamp + nonce + appSecret)`，使用 `org.springframework.util.DigestUtils`

  Redis key：`sign:nonce:{nonce}`，value=`"1"`，TTL=5min，SET NX

  验证流程：
  1. Header 存在性校验 → 缺失抛 `SIGN_PARAM_MISSING`
  2. timestamp 在 5 分钟内 → 超时抛 `SIGN_TIMESTAMP_EXPIRED`
  3. nonce SET NX → 已存在抛 `SIGN_NONCE_USED`
  4. 重新计算 sign 比对 → 不一致删除 nonce 并抛 `SIGN_INVALID`（删除允许客户端修正后重试）

  关键逻辑：
  - `@Order(2)` — 在 RateLimitAspect 之后执行
  - `@Value("${app.sign.secret}")` 注入 appSecret

  使用示例：
  ```java
  // 文件上传：限流 + 签名双重保护
  @RateLimit(limit = 20, window = 60)
  @RequireSign
  public Result<String> upload(...) {}

  // 订单导出
  @RequireSign
  public void exportOrders(...) {}
  ```

- [ ] **Step 7：前端集成签名（方案 A）**

  安全定位：防低级爬虫和脚本，appSecret 接受被逆向（可做代码混淆降低风险）。

  安装依赖：
  ```bash
  npm install md5
  ```

  新增签名工具函数 `src/utils/sign.js`（或 `.ts`）：
  ```js
  import md5 from 'md5'

  const APP_KEY = 'web'
  const APP_SECRET = 'your-secret'  // 与后端 app.sign.secret 一致

  export function buildSignHeaders() {
    const timestamp = Math.floor(Date.now() / 1000).toString()
    const nonce = Math.random().toString(36).slice(2, 8)  // 6位随机串
    const sign = md5(APP_KEY + timestamp + nonce + APP_SECRET)
    return {
      'X-App-Key': APP_KEY,
      'X-Timestamp': timestamp,
      'X-Nonce': nonce,
      'X-Signature': sign
    }
  }
  ```

  在 Axios 请求拦截器中按需注入（`src/utils/request.js` 或类似文件）：
  ```js
  import { buildSignHeaders } from './sign'

  // 需要签名的接口路径前缀（与后端 @RequireSign 标注的接口对应）
  const SIGN_REQUIRED_PATHS = ['/files/upload', '/orders/export']

  axios.interceptors.request.use(config => {
    const needsSign = SIGN_REQUIRED_PATHS.some(p => config.url?.includes(p))
    if (needsSign) {
      Object.assign(config.headers, buildSignHeaders())
    }
    return config
  })
  ```

  > 也可以全量注入（所有请求都带签名 Header），后端只对标注 `@RequireSign` 的接口校验，其余接口忽略这些 Header。

---

## 关键设计决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| 限流算法 | 固定窗口 | 无需新依赖；登录 10次/分钟场景下边界突刺可接受 |
| Redis 不可用 | fail-open | 避免 Redis 故障导致全站不可用 |
| nonce 签名失败后 | 删除 nonce | 允许客户端修正参数后重试，提升可用性 |
| AOP 顺序 | 限流(1) → 签名(2) | 限流更轻量，先拦截高频请求避免无效签名计算 |

---

## 验证方式

```bash
# 1. 限流：登录接口连续发 11 次，第 11 次返回 {"code":429}
for i in {1..11}; do curl -s -X POST http://localhost:8080/api/system/auth/login \
  -H "Content-Type: application/json" -d '{"phone":"13800000000","password":"wrong"}'; echo; done

# 2. 签名缺 Header：返回 {"code":781}
curl http://localhost:8080/api/orders/export

# 3. nonce 重放：相同 nonce 发两次，第二次返回 {"code":783}
# 4. 签名错误：X-Signature 传错误值，返回 {"code":784}
# 5. 时间戳过期：X-Timestamp 传 6 分钟前秒级时间戳，返回 {"code":782}
```
