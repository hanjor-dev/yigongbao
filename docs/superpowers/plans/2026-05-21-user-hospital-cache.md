# 用户医院权限缓存优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用 Spring Cache + Redis 缓存用户医院ID列表，减少订单列表查询时的数据库压力

**Architecture:** 在 `UserHospitalService.getHospitalIdsByUserId()` 方法添加 `@Cacheable` 注解，缓存查询结果到 Redis（TTL 2分钟）。在 `assignHospitals()` 方法添加 `@CacheEvict` 注解，确保权限变更时缓存失效。使用 Spring Cache 抽象层统一管理缓存生命周期。

**Tech Stack:** Spring Cache, Redis, Spring Boot 3.x, MyBatis-Plus

---

## 背景与问题

**当前问题：**
- 订单列表查询中，HOSPITALS 数据权限类型用户每次都查询 `sys_user_hospital` 表
- 高频查询造成不必要的数据库压力
- 用户医院分配关系变更频率低，适合缓存

**优化目标：**
- 缓存用户医院ID列表，减少 90% 数据库查询
- TTL 设置为 2 分钟，平衡性能与数据一致性
- 权限变更时主动失效缓存

---

## 文件结构

**新增文件：**
- `yigongbao-parent/yigongbao-framework/src/main/java/com/yigongbao/framework/config/CacheConfig.java` - Spring Cache 配置类

**修改文件：**
- `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserHospitalServiceImpl.java` - 添加缓存注解

**测试文件：**
- `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserHospitalServiceImplCacheTest.java` - 缓存行为集成测试

---

## Task 1: 添加 Spring Cache 配置类

**Goal:** 启用 Spring Cache 并配置 RedisCacheManager，设置 TTL 为 2 分钟

**Estimated time:** 3 minutes

**Files:**
- Create: `yigongbao-parent/yigongbao-framework/src/main/java/com/yigongbao/framework/config/CacheConfig.java`

**Steps:**

- [ ] 创建 `CacheConfig.java` 配置类，添加以下完整代码：

```java
package com.yigongbao.framework.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Spring Cache 配置类
 * 配置基于 Redis 的缓存管理器，支持 @Cacheable/@CacheEvict 注解
 *
 * @author hanjor
 * @date 2026-05-21
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }
}
```

- [ ] 验证配置类编译通过：
```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-framework
```

**Expected outcome:** Spring Cache 启用，RedisCacheManager 配置完成，TTL 设置为 2 分钟

---

## Task 2: 添加 @Cacheable 注解到查询方法

**Goal:** 在 `getHospitalIdsByUserId()` 方法添加缓存注解，缓存查询结果

**Estimated time:** 2 minutes

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserHospitalServiceImpl.java`

**Steps:**

- [ ] 在 `UserHospitalServiceImpl` 类顶部添加导入：
```java
import org.springframework.cache.annotation.Cacheable;
```

- [ ] 在 `getHospitalIdsByUserId()` 方法上添加 `@Cacheable` 注解（第62行）：
```java
@Override
@Cacheable(value = "user_hospitals", key = "#userId")
public List<Long> getHospitalIdsByUserId(Long userId) {
    List<Long> ids = userHospitalMapper.selectHospitalIdsByUserId(userId);
    return ids != null ? ids : new ArrayList<>();
}
```

**Cache key 说明：**
- `value = "user_hospitals"`: 缓存名称，Redis key 前缀为 `user_hospitals::`
- `key = "#userId"`: 缓存键为用户ID，完整 Redis key 为 `user_hospitals::{userId}`
- 示例：用户ID=123，Redis key = `user_hospitals::123`

- [ ] 验证编译通过：
```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-system
```

**Expected outcome:** 查询方法添加缓存，首次查询写入 Redis，后续 2 分钟内直接从缓存返回

---

## Task 3: 添加 @CacheEvict 注解到更新方法

**Goal:** 在 `assignHospitals()` 方法添加缓存失效注解，确保权限变更时缓存立即失效

**Estimated time:** 2 minutes

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserHospitalServiceImpl.java`

**Steps:**

- [ ] 在 `UserHospitalServiceImpl` 类顶部添加导入：
```java
import org.springframework.cache.annotation.CacheEvict;
```

- [ ] 在 `assignHospitals()` 方法上添加 `@CacheEvict` 注解：
```java
@Override
@Transactional(rollbackFor = Exception.class)
@CacheEvict(value = "user_hospitals", key = "#userId")
public void assignHospitals(Long userId, List<Long> hospitalIds) {
    // 现有逻辑保持不变...
}
```

**Cache eviction 说明：**
- 当调用 `assignHospitals()` 方法时，自动删除 Redis 中 `user_hospitals::{userId}` 的缓存
- 下次查询该用户医院列表时，会重新从数据库加载最新数据

- [ ] 验证编译通过：
```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-system
```

**Expected outcome:** 权限变更时缓存自动失效，确保数据一致性

---

## Task 4: 添加缓存行为集成测试

**Goal:** 验证缓存读取、写入、失效行为正确

**Estimated time:** 5 minutes

**Files:**
- Create: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserHospitalServiceImplCacheTest.java`

**Steps:**

- [ ] 创建集成测试类，添加以下完整代码：

```java
package com.yigongbao.module.system.user.service.impl;

import com.yigongbao.module.system.user.service.UserHospitalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserHospitalServiceImplCacheTest {

    @Autowired
    private UserHospitalService userHospitalService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testCacheHit() {
        Long userId = 1L;
        
        // 清空缓存
        redisTemplate.delete("user_hospitals::" + userId);
        
        // 第一次查询：缓存未命中，查询数据库
        List<Long> result1 = userHospitalService.getHospitalIdsByUserId(userId);
        assertNotNull(result1);
        
        // 验证缓存已写入
        Object cached = cacheManager.getCache("user_hospitals").get(userId);
        assertNotNull(cached, "缓存应该已写入");
        
        // 第二次查询：缓存命中，不查询数据库
        List<Long> result2 = userHospitalService.getHospitalIdsByUserId(userId);
        assertEquals(result1, result2, "两次查询结果应该一致");
    }

    @Test
    void testCacheEviction() {
        Long userId = 1L;
        
        // 先查询一次，写入缓存
        userHospitalService.getHospitalIdsByUserId(userId);
        
        // 验证缓存存在
        Object cached1 = cacheManager.getCache("user_hospitals").get(userId);
        assertNotNull(cached1, "缓存应该存在");
        
        // 调用 assignHospitals，触发缓存失效
        userHospitalService.assignHospitals(userId, List.of(1L, 2L));
        
        // 验证缓存已失效
        Object cached2 = cacheManager.getCache("user_hospitals").get(userId);
        assertNull(cached2, "缓存应该已失效");
    }
}
```

- [ ] 运行测试验证缓存行为：
```bash
cd yigongbao-parent
mvn test -Dtest=UserHospitalServiceImplCacheTest -pl yigongbao-module-system
```

**Expected outcome:** 测试通过，验证缓存读取、写入、失效行为正确

---

## 风险控制与回滚方案

### 风险识别

**风险1：数据不一致（中等风险）**
- **场景：** 缓存失效逻辑遗漏，导致权限变更后缓存未更新
- **影响：** 用户在 2 分钟内使用旧权限查询订单
- **缓解措施：**
  - 在所有修改用户医院关联的方法上添加 `@CacheEvict`
  - 集成测试验证缓存失效行为
  - TTL 设置为 2 分钟（而非 5 分钟），缩短不一致窗口

**风险2：Redis 故障（低风险）**
- **场景：** Redis 服务不可用
- **影响：** 缓存失效，所有查询回退到数据库
- **缓解措施：**
  - Spring Cache 自动降级，Redis 故障时直接查询数据库
  - 不影响业务功能，仅性能下降

**风险3：缓存穿透（低风险）**
- **场景：** 恶意请求大量不存在的 userId
- **影响：** 缓存无效，直接打到数据库
- **缓解措施：**
  - 当前配置 `disableCachingNullValues()`，空结果不缓存
  - 如需防护，可改为缓存空结果（TTL 30秒）

### 回滚方案

**回滚步骤：**

1. **禁用 Spring Cache（最快回滚）**
   - 在 `CacheConfig` 类上添加 `@Profile("!prod")` 注解
   - 重启应用，缓存功能自动禁用
   - 回退到原有查询逻辑

2. **删除缓存注解（完全回滚）**
   - 移除 `UserHospitalServiceImpl` 中的 `@Cacheable` 和 `@CacheEvict` 注解
   - 删除 `CacheConfig.java` 配置类
   - 重新编译部署

**回滚决策标准：**
- 缓存命中率 < 50%：说明缓存效果不佳，考虑回滚
- 数据不一致投诉 > 5 次/天：说明缓存失效逻辑有问题，立即回滚
- Redis 故障频繁（> 3 次/周）：考虑禁用缓存

---

## 监控与验证

### 上线后验证步骤

**1. 验证缓存写入（5分钟内）**
```bash
# 连接 Redis
redis-cli

# 查看缓存键
KEYS user_hospitals::*

# 查看某个用户的缓存内容
GET user_hospitals::1

# 查看 TTL
TTL user_hospitals::1
```

**预期结果：**
- 存在 `user_hospitals::{userId}` 键
- TTL 约为 120 秒（2分钟）
- 内容为 JSON 格式的医院ID列表

**2. 验证缓存命中（观察日志）**
- 同一用户连续查询订单列表 2 次
- 第一次：日志显示查询数据库
- 第二次：无数据库查询日志（缓存命中）

**3. 验证缓存失效（功能测试）**
- 修改用户医院分配
- 立即查询该用户订单列表
- 应返回最新权限范围的订单

### 性能监控指标

**关键指标：**
- **缓存命中率**：目标 > 80%
- **平均响应时间**：订单列表查询应降低 30-50ms
- **数据库查询次数**：`sys_user_hospital` 表查询应减少 90%

**监控方式：**
- 使用 Redis INFO 命令查看缓存命中率
- 应用日志记录查询耗时
- 数据库慢查询日志监控

---

## 实施检查清单

**实施前：**
- [ ] 确认 Redis 服务正常运行
- [ ] 确认测试环境可用
- [ ] 备份当前代码版本

**实施中：**
- [ ] Task 1: 添加 CacheConfig 配置类
- [ ] Task 2: 添加 @Cacheable 注解
- [ ] Task 3: 添加 @CacheEvict 注解
- [ ] Task 4: 运行集成测试，验证通过
- [ ] 编译打包，确认无错误

**上线后：**
- [ ] 验证缓存写入（Redis KEYS 命令）
- [ ] 验证缓存命中（观察日志）
- [ ] 验证缓存失效（功能测试）
- [ ] 监控缓存命中率（目标 > 80%）
- [ ] 监控响应时间（目标降低 30-50ms）
- [ ] 观察 24 小时，无异常后关闭监控

**回滚准备：**
- [ ] 准备回滚脚本（禁用 @EnableCaching）
- [ ] 确认回滚流程（< 5 分钟完成）

---

## 总结

**预期收益：**
- 减少 90% 的 `sys_user_hospital` 表查询
- 订单列表查询响应时间降低 30-50ms
- 数据库负载降低，提升系统整体性能

**实施风险：**
- 数据不一致窗口：最长 2 分钟（可接受）
- Redis 故障影响：自动降级，不影响功能

**实施时间：**
- 开发时间：15 分钟
- 测试时间：10 分钟
- 上线验证：10 分钟
- 总计：35 分钟

**下一步行动：**
使用 `superpowers:subagent-driven-development` 或 `superpowers:executing-plans` 技能按任务顺序实施本计划。
