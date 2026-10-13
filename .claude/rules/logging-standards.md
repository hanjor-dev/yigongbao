# 医工宝日志规范

## 概述

本文档定义了医工宝项目的日志记录标准和最佳实践。合理的日志输出是系统可观测性的基础，应遵循"少而精"的原则：**每条日志都应有明确的业务价值，能够帮助快速定位问题、追踪业务流程、分析系统性能**。

**核心原则**：
- 日志不是调试工具，不应记录每个方法的执行过程
- 日志是问题诊断的线索，应记录关键节点和异常情况
- 日志是业务审计的依据，应记录数据变更和状态流转
- 日志是性能分析的基础，应记录耗时操作和资源消耗

---

## 一、日志级别使用规范

### 1.1 级别定义

| 级别 | 使用场景 | 生产环境 | 示例 |
|------|---------|---------|------|
| **ERROR** | 系统异常、业务失败、需要人工介入 | ✅ 开启 | 数据库连接失败、第三方接口调用失败、业务规则校验失败 |
| **WARN** | 潜在问题、降级处理、可恢复异常 | ✅ 开启 | 缓存未命中、重试操作、使用默认值、慢查询 |
| **INFO** | 关键业务节点、状态变更、外部调用 | ✅ 开启 | 订单创建、状态流转、批量操作汇总、定时任务执行 |
| **DEBUG** | 开发调试信息、详细参数、中间结果 | ❌ 关闭 | 方法入参、SQL语句、缓存命中详情 |

### 1.2 级别选择决策树

```
是否是异常情况？
├─ 是 → 是否需要人工介入？
│         ├─ 是 → ERROR
│         └─ 否 → WARN
└─ 否 → 是否是关键业务操作？
          ├─ 是 → INFO
          └─ 否 → DEBUG 或不记录
```

---

## 二、日志记录场景规范

### 2.1 必须记录（ERROR/WARN/INFO）

#### ✅ 异常情况（ERROR）

```java
// 1. 业务异常：记录业务上下文 + 错误原因
try {
    orderService.createOrder(dto);
} catch (BusinessException e) {
    log.error("订单创建失败: userId={}, hospitalId={}, itemCount={}, reason={}", 
        dto.getUserId(), dto.getHospitalId(), dto.getItems().size(), e.getMessage());
    throw e;
}

// 2. 系统异常：记录完整堆栈
try {
    // ...
} catch (Exception e) {
    log.error("订单创建异常: userId={}, dto={}", userId, dto, e);
    throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
}

// 3. 外部依赖失败：记录接口信息
log.error("调用支付接口失败: orderId={}, payAmount={}, apiUrl={}, httpStatus={}, response={}", 
    orderId, amount, apiUrl, response.getStatus(), response.getBody());
```

#### ⚠️ 潜在问题（WARN）

```java
// 1. 缓存未命中
log.warn("用户医院权限缓存未命中: userId={}, 将查询数据库", userId);

// 2. 降级处理
log.warn("获取配置失败，使用默认值: configKey={}, defaultValue={}", key, defaultValue);

// 3. 慢查询/慢操作
if (duration > 1000) {
    log.warn("慢查询检测: method={}, params={}, duration={}ms", methodName, params, duration);
}

// 4. 数据异常但可继续
log.warn("订单项部分失败: orderId={}, totalItems={}, failedItems={}", orderId, total, failed);
```

#### ℹ️ 关键业务操作（INFO）

```java
// 1. 数据创建：记录实体ID和关键字段
log.info("创建订单: orderId={}, userId={}, hospitalId={}, totalAmount={}, itemCount={}", 
    order.getId(), order.getUserId(), order.getHospitalId(), order.getTotalAmount(), items.size());

// 2. 数据修改：记录变更内容
log.info("更新订单状态: orderId={}, {} -> {}, operator={}, reason={}", 
    orderId, oldStatus, newStatus, operatorId, reason);

// 3. 数据删除：记录删除对象
log.info("删除医院: hospitalId={}, hospitalName={}, deleteBy={}", id, name, userId);

// 4. 批量操作：记录汇总信息
log.info("批量导入医院: 总数={}, 成功={}, 失败={}, 耗时={}ms", total, success, fail, duration);

// 5. 定时任务：记录执行结果
log.info("定时任务执行完成: taskName={}, processedCount={}, duration={}ms", taskName, count, duration);

// 6. 外部接口调用：记录关键参数和结果
log.info("调用第三方接口: api={}, orderId={}, status={}, duration={}ms", apiName, orderId, status, duration);
```

### 2.2 不应该记录

#### ❌ 简单查询操作

```java
// ❌ 错误：简单查询不需要记录
log.info("根据ID查询用户，id={}", id);
log.info("查询用户成功，id={}", id);

// ✅ 正确：不记录，或仅DEBUG级别
log.debug("查询用户: id={}", id);
```

#### ❌ 无意义的成功确认

```java
// ❌ 错误
log.info("查询字典成功，dictCode={}", dictCode);
log.info("操作成功");
log.info("数据校验通过");

// ✅ 正确：删除这些日志
```

#### ❌ 重复信息

```java
// ❌ 错误：Controller已有请求日志，Service不需要重复
// Controller层（由 ResultInterceptor 统一记录）
log.debug("请求开始：POST /api/order/create");

// Service层（不需要再记录"开始处理"）
log.info("开始创建订单，userId={}", userId);  // ❌ 删除

// ✅ 正确：Service只记录关键节点
log.info("创建订单: orderId={}, userId={}, amount={}", orderId, userId, amount);
```

#### ❌ 正常流程的每一步

```java
// ❌ 错误：记录每个步骤
log.info("开始校验订单参数");
log.info("参数校验通过");
log.info("开始保存订单");
log.info("订单保存成功");
log.info("开始保存订单项");
log.info("订单项保存成功");

// ✅ 正确：只记录最终结果
log.info("创建订单: orderId={}, itemCount={}", orderId, items.size());
```

---

## 三、日志内容格式规范

### 3.1 格式模板

```
动作描述: 业务标识=值, 关键参数=值, 结果信息
```

### 3.2 内容要素

| 要素 | 说明 | 示例 |
|------|------|------|
| **动作描述** | 简洁明确的动词短语 | 创建订单、更新状态、删除医院、批量导入 |
| **业务标识** | 唯一标识（ID、编码） | orderId=123, hospitalCode=H001 |
| **关键参数** | 影响业务逻辑的参数 | userId=456, status=APPROVED |
| **结果信息** | 状态变更、影响记录数、耗时 | 成功=10, 失败=2, 耗时=500ms |
| **异常信息** | 错误原因、堆栈（ERROR级别） | reason=库存不足, exception=e |

### 3.3 格式示例

```java
// ✅ 优秀示例
log.info("创建订单: orderId={}, userId={}, hospitalId={}, amount={}, itemCount={}", 
    order.getId(), order.getUserId(), order.getHospitalId(), order.getTotalAmount(), items.size());

log.info("订单状态变更: orderId={}, {} -> {}, operator={}, reason={}", 
    orderId, oldStatus.getDesc(), newStatus.getDesc(), operatorId, reason);

log.error("订单创建失败: userId={}, hospitalId={}, itemCount={}, reason={}", 
    dto.getUserId(), dto.getHospitalId(), dto.getItems().size(), e.getMessage(), e);

log.warn("慢查询检测: method=OrderMapper.listByCondition, params={}, duration={}ms", 
    params, duration);

// ❌ 错误示例
log.info("操作成功");  // 缺少业务上下文
log.info("查询数据");  // 缺少参数和结果
log.error("出错了");   // 缺少错误原因
```

### 3.4 参数占位符规范

```java
// ✅ 正确：使用占位符（性能更好，避免字符串拼接）
log.info("创建订单: orderId={}, amount={}", orderId, amount);

// ❌ 错误：字符串拼接
log.info("创建订单: orderId=" + orderId + ", amount=" + amount);

// ✅ 正确：复杂对象使用 JSON
log.info("创建订单: orderId={}, dto={}", orderId, JSONUtil.toJsonStr(dto));

// ⚠️ 注意：避免记录敏感信息
log.info("用户登录: username={}, password=***", username);  // 密码脱敏
```

---

## 四、分层日志规范

### 4.1 Controller 层

**【强制】Controller 层禁止输出日志**，统一由 `ResultInterceptor` 记录请求日志。

```java
// ❌ 错误：Controller 不应该记录日志
@PostMapping("/create")
public Result<Long> createOrder(@RequestBody CreateOrderDTO dto) {
    log.info("接收创建订单请求，dto={}", dto);  // ❌ 删除
    Long orderId = orderService.createOrder(dto);
    log.info("订单创建成功，orderId={}", orderId);  // ❌ 删除
    return Result.success(orderId);
}

// ✅ 正确：Controller 不记录日志
@PostMapping("/create")
public Result<Long> createOrder(@RequestBody CreateOrderDTO dto) {
    Long orderId = orderService.createOrder(dto);
    return Result.success(orderId);
}
```

**例外情况**：仅在 Controller 捕获特定异常需要特殊处理时，可记录 ERROR 日志。

### 4.2 Service 层

**Service 层是日志记录的主要位置**，应记录：
- 数据修改操作（create/update/delete）
- 状态变更操作
- 批量操作汇总
- 业务异常

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements IOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(CreateOrderDTO dto) {
        // ❌ 删除：不需要记录"开始处理"
        // log.info("开始创建订单，userId={}", dto.getUserId());
        
        // 业务逻辑...
        OrderEntity order = new OrderEntity();
        // ...
        save(order);
        
        // ✅ 记录：数据创建
        log.info("创建订单: orderId={}, userId={}, hospitalId={}, amount={}, itemCount={}", 
            order.getId(), order.getUserId(), order.getHospitalId(), 
            order.getTotalAmount(), dto.getItems().size());
        
        return order.getId();
    }

    @Override
    public OrderVO getById(Long id) {
        // ❌ 删除：简单查询不记录
        // log.info("根据ID查询订单，id={}", id);
        
        OrderEntity order = getById(id);
        if (order == null) {
            // ✅ 记录：业务异常
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        
        // ❌ 删除：查询成功不记录
        // log.info("查询订单成功，orderId={}", id);
        
        return OrderConvert.INSTANCE.toVO(order);
    }
}
```

### 4.3 Mapper 层

**【强制】Mapper 层禁止记录日志**，SQL 执行由 MyBatis 日志框架记录（DEBUG 级别）。

---

## 五、特殊场景日志规范

### 5.1 循环中的日志

```java
// ❌ 错误：循环内记录每条
for (OrderItemDTO item : items) {
    log.info("处理订单项: itemId={}", item.getId());  // ❌ 日志爆炸
    // ...
}

// ✅ 正确：只记录汇总
log.info("批量处理订单项: totalCount={}", items.size());
for (OrderItemDTO item : items) {
    // ...
}
log.info("订单项处理完成: 成功={}, 失败={}", successCount, failCount);
```

### 5.2 定时任务日志

```java
@Scheduled(cron = "0 0 2 * * ?")
public void syncOrderStatus() {
    long startTime = System.currentTimeMillis();
    
    // ✅ 记录：任务开始
    log.info("定时任务开始: taskName=syncOrderStatus");
    
    try {
        int count = doSync();
        long duration = System.currentTimeMillis() - startTime;
        
        // ✅ 记录：任务完成
        log.info("定时任务完成: taskName=syncOrderStatus, processedCount={}, duration={}ms", 
            count, duration);
    } catch (Exception e) {
        // ✅ 记录：任务失败
        log.error("定时任务失败: taskName=syncOrderStatus", e);
    }
}
```

### 5.3 异步操作日志

```java
@Async
public void sendNotification(Long orderId) {
    // ✅ 记录：异步任务开始（便于追踪）
    log.info("发送订单通知: orderId={}", orderId);
    
    try {
        // ...
        log.info("订单通知发送成功: orderId={}", orderId);
    } catch (Exception e) {
        log.error("订单通知发送失败: orderId={}", orderId, e);
    }
}
```

### 5.4 外部接口调用日志

```java
public PaymentResult callPaymentApi(PaymentRequest request) {
    long startTime = System.currentTimeMillis();
    
    // ✅ 记录：接口调用开始
    log.info("调用支付接口: orderId={}, amount={}, apiUrl={}", 
        request.getOrderId(), request.getAmount(), apiUrl);
    
    try {
        PaymentResult result = restTemplate.postForObject(apiUrl, request, PaymentResult.class);
        long duration = System.currentTimeMillis() - startTime;
        
        // ✅ 记录：接口调用成功
        log.info("支付接口调用成功: orderId={}, status={}, duration={}ms", 
            request.getOrderId(), result.getStatus(), duration);
        
        return result;
    } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        
        // ✅ 记录：接口调用失败
        log.error("支付接口调用失败: orderId={}, duration={}ms, error={}", 
            request.getOrderId(), duration, e.getMessage(), e);
        throw new BusinessException(ErrorCodeEnum.PAYMENT_API_ERROR);
    }
}
```

---

## 六、日志配置规范

### 6.1 Logback 配置示例

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- 开发环境 -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
        <!-- MyBatis SQL 日志 -->
        <logger name="com.yigongbao.module.*.mapper" level="DEBUG"/>
    </springProfile>
    
    <!-- 生产环境 -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
        <!-- 关闭 DEBUG 日志 -->
        <logger name="com.yigongbao" level="INFO"/>
    </springProfile>
</configuration>
```

### 6.2 日志文件分割

```xml
<!-- 按日期和大小分割 -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/yigongbao.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/yigongbao.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>

<!-- 错误日志单独文件 -->
<appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/error.log</file>
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
        <level>ERROR</level>
        <onMatch>ACCEPT</onMatch>
        <onMismatch>DENY</onMismatch>
    </filter>
</appender>
```

---

## 七、实施指南

### 7.1 清理步骤

1. **识别冗余日志**
   - 简单查询的 INFO 日志
   - "操作成功"类的确认日志
   - Service 层的"开始处理"日志

2. **保留关键日志**
   - 数据修改操作（create/update/delete）
   - 状态变更操作
   - 批量操作汇总
   - 异常日志

3. **调整日志级别**
   - 简单查询改为 DEBUG 或删除
   - 关键操作保持 INFO
   - 异常保持 ERROR/WARN

### 7.2 验证标准

清理后的日志应满足：
- ✅ 生产环境 INFO 日志减少 80% 以上
- ✅ 每条日志都有明确的业务价值
- ✅ 通过业务 ID 可追踪完整流程
- ✅ 出现问题时能快速定位原因
- ✅ 关键操作有耗时记录

### 7.3 Code Review 检查项

- [ ] Controller 层是否有日志输出？（应删除）
- [ ] 简单查询是否记录 INFO 日志？（应删除或改 DEBUG）
- [ ] 数据修改操作是否记录日志？（应保留）
- [ ] 异常是否记录完整上下文？（应包含业务 ID 和堆栈）
- [ ] 日志格式是否符合规范？（动作 + 业务标识 + 关键参数）
- [ ] 是否记录敏感信息？（应脱敏）

---

**文档版本**：1.0  
**最后更新**：2026-05-22  
**相关文档**：[java-coding-standards.md](./java-coding-standards.md)
