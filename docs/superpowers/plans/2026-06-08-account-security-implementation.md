# 账户安全功能实施计划

**创建时间**：2026-06-08  
**负责人**：hanjor  
**预计工期**：2-3 天  
**优先级**：P0（高优先级）

---

## 一、需求背景

### 1.1 需求来源

需求编号：#73  
需求描述：
> "关于账户安全，同账户不允许多地同时登录，最新登录并通过验证时要踢出历史登录并在线的同一账户并提示账户异常，账户在ip******省市登录，您已被系统踢出，请及时确认密钥是否泄露或修改密码。并且用户个人中心需要展示最近30条登录信息"

### 1.2 需求拆解

| 需求点 | 描述 | 优先级 | 预计工时 |
|--------|------|--------|----------|
| **R1** | 禁止同账户多地同时登录 | P0 | 0.5h |
| **R2** | 新登录成功时自动踢出历史在线会话 | P0 | 0h（配置自动实现） |
| **R3** | 被踢出时提示用户（IP + 省市信息） | P1 | 4h |
| **R4** | 用户个人中心展示最近30条登录信息 | P1 | 3h |

**总计**：约 7.5 小时（1 个工作日）

### 1.3 当前系统状态

- ✅ 认证框架：SaToken 1.37.0
- ✅ 并发登录状态：`is-concurrent: true`（**需修改**）
- ✅ 登录日志：已有 `LoginLogEntity`（需新增 `location` 字段）
- ✅ IP 解析库：已集成 `ip2region`（`IpLocationUtil` 工具类可用）
- ❌ WebSocket：未实现（**本期不实施，预留注释**）

---

## 二、技术方案

### 2.1 方案选型

**选择方案**：最小改动方案（方案A）

**核心思路**：
1. 修改 SaToken 配置 `is-concurrent: false`，禁止并发登录
2. 登录日志增加 `location` 字段，记录 IP 归属地
3. 新增接口：获取最新登录记录、获取登录历史
4. 前端拦截 401 错误，调用接口获取被踢出信息并提示
5. WebSocket 推送功能预留注释，后续统一实现

**优点**：
- ✅ 改动最小，配置级实现
- ✅ 无需 WebSocket，快速上线
- ✅ 复用现有 IP 解析工具

**缺点**：
- ❌ 不是实时通知（用户下次请求才知道被踢出）

### 2.2 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| SaToken | 1.37.0 | 认证框架，禁止并发登录 |
| ip2region | 2.7.0 | IP 归属地解析（已集成） |
| MyBatis Plus | 3.5.8 | 数据库操作 |
| Redis | - | Session 存储 |

---

## 三、实施步骤

### 阶段1：配置修改（0.5h）

**任务清单**：
- [ ] 修改 `application-dev.yml`：`is-concurrent: false`
- [ ] 修改 `application-prod.yml`：`is-concurrent: false`
- [ ] 提交代码：`git commit -m "feat: 禁止同账户并发登录"`

**验证标准**：
- 同一账号在 A 设备登录后，再从 B 设备登录，A 设备的 token 应失效

---

### 阶段2：数据库表结构调整（0.5h）

**任务清单**：
- [ ] 编写 DDL：`ALTER TABLE sys_login_log ADD COLUMN location`
- [ ] 本地开发环境执行 DDL
- [ ] 修改 `LoginLogEntity`：增加 `location` 字段
- [ ] 更新 `sql/ddl.sql`（记录表结构变更）
- [ ] 提交代码：`git commit -m "feat: 登录日志增加归属地字段"`

**DDL 语句**（已修正）：
```sql
-- 增加归属地字段（使用ALGORITHM=INPLACE减少锁表）
ALTER TABLE sys_login_log 
ADD COLUMN location VARCHAR(100) DEFAULT NULL COMMENT 'IP归属地（省市信息）' 
AFTER user_agent,
ALGORITHM=INPLACE, LOCK=NONE;

-- 增加索引（用于查询用户登录历史），使用IF NOT EXISTS避免重复创建
CREATE INDEX IF NOT EXISTS idx_user_login_time 
ON sys_login_log(user_id, login_time DESC);
```

**⚠️ 注意事项**：
- 如果`sys_login_log`表数据量大（>10万行），建议先在测试库执行DDL统计耗时
- MySQL 5.6+ 支持`ALGORITHM=INPLACE`，可在线执行，不阻塞DML操作

**验证标准**：
- 表结构变更成功，无数据丢失
- 索引创建成功

---

### 阶段3：登录逻辑改动（1h）✅ 已优化

**任务清单**：
- [ ] 修改 `AuthServiceImpl.saveLoginLog()` 方法实现：在方法内部调用 `IpLocationUtil.getLocation(ip)`
- [ ] 修改 `LoginLogEntity`：增加 `location` 字段（已在阶段2完成）
- [ ] 增加 WebSocket 推送预留注释（TODO）
- [ ] 编写单元测试：`AuthServiceImplTest.testLoginWithLocation()`
- [ ] 提交代码：`git commit -m "feat: 登录时记录IP归属地"`

**✅ 优化说明**：
- 原方案需修改方法签名 + 10处调用点 = 11处改动
- 优化后仅需修改 `saveLoginLog` 方法内部 = 1处改动
- 零遗漏风险，零重复代码

**关键代码修改**：
- `AuthServiceImpl.java:349` - `saveLoginLog()` 方法内部增加 `String location = IpLocationUtil.getLocation(ip);`
- ✅ 所有调用点（10处）无需修改

**核心代码**：
```java
private void saveLoginLog(Long userId, String principal, String loginType, 
                          String ip, String userAgent, Integer status, String failReason) {
    try {
        // ✅ 在方法内部解析IP归属地
        String location = IpLocationUtil.getLocation(ip);
        
        LoginLogEntity logEntity = new LoginLogEntity();
        logEntity.setUserId(userId);
        logEntity.setUsername(principal);
        logEntity.setLoginType(loginType);
        logEntity.setIp(ip);
        logEntity.setUserAgent(userAgent);
        logEntity.setLocation(location);  // 新增
        logEntity.setLoginTime(LocalDateTime.now());
        logEntity.setLoginStatus(status);
        logEntity.setFailReason(failReason);
        loginLogMapper.insert(logEntity);
    } catch (Exception e) {
        log.error("保存登录日志异常", e);
    }
}
```

**WebSocket 预留注释模板**（已修正）：
```java
// TODO: WebSocket 推送被踢出通知（待实现）
// ⚠️ 重要：必须在 StpUtil.login(userId) 调用之前获取旧会话
// 实现思路：
// 1. 【登录前】List<String> oldTokens = StpUtil.getTokenValueListByLoginId(userId);
// 2. 提取旧会话的tokenValue（用于标识WebSocket连接）
// 3. StpUtil.login(userId);  // SaToken自动踢出旧会话
// 4. 【登录后】通过WebSocket向oldTokens推送消息：
//    { "type": "KICKED_OUT", "newLoginIp": "xxx", "location": "省市", "time": "时间" }
// 5. 前端收到消息后，弹窗提示并清除本地token，跳转到登录页
// 注意：WebSocket连接需与token绑定，以便通过token找到对应连接
```

**验证标准**：
- 登录成功后，`sys_login_log` 表的 `location` 字段有值
- 单元测试通过

---

### 阶段4：新增查询接口（3h）

#### 4.1 后端接口开发

**任务清单**：
- [ ] 创建 `LoginLogVO`：登录日志视图对象
- [ ] 修改 `AuthService` 接口：增加 `getPreviousLogin()` 和 `getLoginHistory()`
- [ ] 实现 `AuthServiceImpl`：查询逻辑
- [ ] 修改 `AuthController`：增加接口
- [ ] 编写单元测试：`AuthServiceImplTest`
- [ ] 提交代码：`git commit -m "feat: 新增登录历史查询接口"`

**接口设计**：

##### 接口1：获取上一次登录记录 ✅ 已修正

```
GET /system/auth/previous-login
```

**⚠️ 修正说明**：
- 原接口名 `latest-login` 会返回当前登录记录，不是"被踢出时的上一次登录"
- 修正为 `previous-login`，SQL 使用 `LIMIT 1 OFFSET 1` 跳过最新一条

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "userId": 1,
    "username": "admin",
    "loginType": "PASSWORD",
    "ip": "59.172.31.22",
    "location": "中国 湖北省 武汉市",
    "userAgent": "Mozilla/5.0...",
    "loginTime": "2026-06-08 14:30:00",
    "loginStatus": 1
  }
}
```

**SQL 实现**：
```sql
SELECT * FROM sys_login_log 
WHERE user_id = ? AND login_status = 1 
ORDER BY login_time DESC 
LIMIT 1 OFFSET 1  -- 跳过最新一条，取倒数第2条
```

##### 接口2：获取登录历史

```
GET /system/auth/login-history?limit=30
```

**请求参数**：
- `limit`（可选）：返回记录数，默认 30，最大 100

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "userId": 1,
      "username": "admin",
      "loginType": "PASSWORD",
      "ip": "59.172.31.22",
      "location": "中国 湖北省 武汉市",
      "userAgent": "Mozilla/5.0...",
      "loginTime": "2026-06-08 14:30:00",
      "loginStatus": 1
    }
    // ... 更多记录
  ]
}
```

**SQL 实现**：
```sql
SELECT * FROM sys_login_log 
WHERE user_id = ? AND login_status = 1  -- ✅ 只返回成功记录
ORDER BY login_time DESC 
LIMIT ?
```

#### 4.2 前端对接

**任务清单**（前端团队）：
- [ ] 用户个人中心增加"登录历史"模块
- [ ] 展示最近 30 条登录记录（时间、IP、归属地、设备）
- [ ] 统一拦截 401 错误，调用 `/previous-login` 接口获取被踢出信息 ✅ 已修正
- [ ] 弹窗提示：`账户异常，账户在 ${location}(${ip}) 登录，您已被系统踢出，请及时确认密钥是否泄露或修改密码`

**验证标准**：
- 接口返回正确的登录历史数据
- 前端能正常展示
- 被踢出时能弹窗提示

---

## 四、改动清单

### 4.1 配置文件改动

| 文件 | 改动内容 | 影响范围 |
|------|----------|----------|
| `application-dev.yml` | `is-concurrent: false` | 开发环境 |
| `application-prod.yml` | `is-concurrent: false` | 生产环境 |

### 4.2 数据库改动

| 表名 | 改动类型 | 改动内容 |
|------|----------|----------|
| `sys_login_log` | ALTER | 增加 `location` 字段 |
| `sys_login_log` | INDEX | 增加 `idx_user_login_time` 索引 |

### 4.3 代码改动（已优化）

| 文件 | 改动类型 | 改动内容 | 行数 |
|------|----------|----------|------|
| `LoginLogEntity.java` | 新增字段 | `location` | +3 |
| `AuthServiceImpl.java` | 修改方法实现 | `saveLoginLog()` 内部解析IP（✅ 零调用点改动） | +2 |
| `AuthServiceImpl.java` | 新增注释 | WebSocket 预留 TODO | +7 |
| `LoginLogVO.java` | 新建 | 登录日志视图对象 | +30 |
| `AuthService.java` | 新增接口 | `getPreviousLogin()` | +5 |
| `AuthService.java` | 新增接口 | `getLoginHistory()` | +5 |
| `AuthServiceImpl.java` | 新增实现 | 查询逻辑 | +30 |
| `AuthController.java` | 新增接口 | 2 个 GET 接口 | +20 |
| `AuthServiceImplTest.java` | 新增测试 | 单元测试（含并发测试） | +60 |

**总代码行数**：约 **162 行**（优化前 170 行）

**✅ 优化对比**：
- 原方案：修改方法签名 + 10处调用点 = 11处改动
- 优化后：仅修改方法内部 = 1处改动
- 减少改动点：10处 ✅

---

## 五、测试计划

### 5.1 单元测试

**测试类**：`AuthServiceImplTest`

**测试用例**（已补充）：

| 用例ID | 测试场景 | 预期结果 |
|--------|----------|----------|
| T1 | 登录成功，记录 location | `sys_login_log.location` 有值 |
| T2 | 内网IP登录 | `location` = "内网IP" |
| T3 | 无效IP登录 | `location` = "未知" |
| T4 | 查询上一次登录记录 | 返回倒数第2条成功记录 ✅ 已修正 |
| T5 | 查询登录历史（limit=30） | 返回 30 条成功记录 |
| T6 | 查询登录历史（limit=5） | 返回 5 条成功记录 |
| **T7** | **并发登录限制** ✅ 新增 | **同账号B设备登录后，A设备token失效** |

**T7 测试代码示例**：
```java
@Test
void testConcurrentLoginDisabled() {
    LoginDTO dto = new LoginDTO();
    dto.setLoginType(LoginTypeEnum.PASSWORD);
    dto.setPrincipal("admin");
    dto.setCredential("123456");
    
    // 1. 设备A登录
    LoginVO loginVO1 = authService.login(dto);
    String tokenA = loginVO1.getToken();
    StpUtil.checkByToken(tokenA);  // 验证有效
    
    // 2. 设备B登录（同账号）
    LoginVO loginVO2 = authService.login(dto);
    String tokenB = loginVO2.getToken();
    
    // 3. 验证A的token已失效
    assertThrows(NotLoginException.class, () -> {
        StpUtil.checkByToken(tokenA);
    });
    
    // 4. 验证B的token有效
    assertDoesNotThrow(() -> {
        StpUtil.checkByToken(tokenB);
    });
}
```

### 5.2 集成测试

**测试场景**：

| 场景 | 操作步骤 | 预期结果 |
|------|----------|----------|
| **并发登录限制** | 1. A 设备登录账号 admin<br>2. B 设备登录账号 admin | A 设备 token 失效，下次请求返回 401 |
| **被踢出提示** | 1. A 设备登录<br>2. B 设备登录<br>3. A 设备发起请求 | A 设备收到 401，调用接口获取被踢出信息并提示 |
| **登录历史展示** | 个人中心查看登录历史 | 展示最近 30 条记录，包含 IP、归属地、时间 |

### 5.3 性能测试

| 指标 | 目标 | 测试方法 |
|------|------|----------|
| IP 解析耗时 | < 1ms | 单次调用 `IpLocationUtil.getLocation()` |
| 登录接口响应时间 | < 500ms | JMeter 压测，100 并发 |
| 查询登录历史响应时间 | < 200ms | 查询 30 条记录 |

---

## 六、风险评估

### 6.1 技术风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| **并发登录限制影响用户体验** | 用户同时在多设备使用时频繁被踢出 | 中 | 1. 文档说明新规则<br>2. 提供"记住我"功能延长 token 有效期 |
| **IP 解析失败** | `location` 字段为空或"未知" | 低 | ip2region 离线库稳定，容错处理已实现 |
| **登录历史数据量过大** | 查询性能下降 | 低 | 已增加索引 `idx_user_login_time`，限制查询条数 |

### 6.2 业务风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| **用户投诉频繁被踢出** | 用户满意度下降 | 中 | 1. 提前通知用户新规则<br>2. 提供申诉通道 |
| **IP 归属地不准确** | 用户质疑安全提示 | 低 | ip2region 数据库定期更新 |

---

## 七、上线计划

### 7.1 上线前检查清单

- [ ] 代码提交并通过 Code Review
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试全部通过
- [ ] 生产环境 DDL 准备完毕（`sql/ddl.sql` 已更新）
- [ ] 前后端联调完成
- [ ] 用户文档更新（账户安全规则说明）
- [ ] 回滚方案准备（恢复 `is-concurrent: true`）

### 7.2 上线步骤

**时间窗口**：非高峰期（建议晚上 22:00-24:00）

| 步骤 | 操作 | 负责人 | 预计耗时 |
|------|------|--------|----------|
| 1 | 备份生产数据库 | DBA | 5min |
| 2 | 执行 DDL（增加字段和索引） | DBA | 2min |
| 3 | 部署后端代码 | DevOps | 5min |
| 4 | 修改配置 `is-concurrent: false` | DevOps | 1min |
| 5 | 重启应用 | DevOps | 2min |
| 6 | 冒烟测试（登录、查询历史） | 测试 | 5min |
| 7 | 监控观察（15min） | 运维 | 15min |

**总耗时**：约 35 分钟

### 7.3 监控指标

上线后需重点关注：
- 登录接口响应时间（目标 < 500ms）
- 登录成功率（目标 > 99%）
- 401 错误数量（是否异常增长）
- Redis 连接数（Session 存储是否正常）

### 7.4 回滚方案

**触发条件**：
- 登录成功率 < 95%
- 登录接口响应时间 > 2s
- 大量用户投诉无法登录

**回滚步骤**：
1. 修改配置：`is-concurrent: true`
2. 重启应用
3. 验证登录功能恢复

**回滚耗时**：< 5 分钟

---

## 八、后续优化计划

### 8.1 WebSocket 实时推送（Phase 2）

**预计时间**：2-3 天  
**优先级**：P2

**功能**：
- 新登录成功时，实时推送被踢出消息到旧会话
- 用户无需等到下次请求才知道被踢出

**技术方案**：
- Spring WebSocket
- Redis Pub/Sub（分布式环境下的消息广播）

### 8.2 登录异常检测（Phase 3）

**预计时间**：3-5 天  
**优先级**：P3

**功能**：
- 检测异常登录行为（异地登录、凌晨登录、短时间多次登录）
- 触发二次验证（短信验证码）
- 发送邮件/短信通知用户

---

## 九、总结

本实施计划采用**最小改动方案**，通过配置级修改和少量代码改动，即可实现账户安全功能的核心需求。

**关键优势**：
- ✅ 改动最小，风险可控
- ✅ 快速上线（2-3 天）
- ✅ 无需引入新技术栈
- ✅ 预留 WebSocket 扩展接口

**待后续优化**：
- WebSocket 实时推送（Phase 2）
- 登录异常检测（Phase 3）

---

**文档版本**：v1.0  
**最后更新**：2026-06-08
