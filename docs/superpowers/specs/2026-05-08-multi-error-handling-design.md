# 多接口错误处理优化设计方案

**日期**：2026-05-08  
**作者**：hanjor  
**状态**：设计中

## 1. 背景和问题

### 1.1 问题描述

当前系统中，前端页面在加载时会并行调用多个后端接口获取数据。如果多个接口同时报错，前端会弹出多个错误提示，严重影响用户体验。

### 1.2 典型场景

页面加载时并行请求多个数据接口（如订单详情页同时请求订单信息、用户信息、医院信息），如果多个接口同时失败，用户会看到多个错误弹窗。

### 1.3 期望效果

按照错误的严重程度或影响范围排序，只展示最重要的一条错误信息，其他错误不展示。

---

## 2. 方案选择

### 2.1 候选方案

**方案1：前端错误队列 + 优先级过滤（纯前端方案）**
- 优点：不需要改动后端，实施快速
- 缺点：优先级规则硬编码在前端，维护成本高

**方案2：后端统一错误码 + 前端去重（推荐）**
- 优点：优先级规则由后端统一管理，扩展性好
- 缺点：需要改动后端 ErrorCodeEnum

**方案3：前端请求编排 + 错误短路**
- 优点：减少无效请求
- 缺点：改动较大，可能影响页面加载速度

### 2.2 最终选择

**选择方案2**，理由：
1. 优先级规则应该由后端定义，保证前后端一致
2. 扩展性好，未来可以添加更多错误元数据
3. 改动相对可控，不影响现有业务逻辑

---

## 3. 详细设计

### 3.1 架构概览

**核心思路**：在后端为每个错误码定义优先级，前端收集短时间内的所有错误，按优先级排序后只展示最高优先级的错误。

**涉及的改动**：
1. 后端：`ErrorCodeEnum` 添加 `priority` 字段
2. 后端：`Result` 响应结构添加 `priority` 字段
3. 前端：HTTP 响应拦截器添加错误收集和过滤逻辑

### 3.2 优先级分级方案

基于错误的影响范围，分为5级：

| 优先级 | 级别 | 说明 | 包含的错误 |
|--------|------|------|-----------|
| 1 | 认证/授权错误 | 阻断所有操作，必须先解决 | 401, 403, 611, 612, 605, 608等 |
| 2 | 系统错误 | 服务不可用，影响全局 | 500, 503, 780(限流), 781-784(签名验证)等 |
| 3 | 核心业务错误 | 影响主要业务流程 | 用户相关(601-636)、订单相关(677-732)、设计相关(758-772) |
| 4 | 辅助业务错误 | 影响辅助功能 | 机构(613-627)、医院(642-647)、产品(648-649)、医生(674-676) |
| 5 | 配置/基础数据错误 | 影响基础功能 | 配置(635-638)、字典(639-640)、资源(631-634) |

**说明**：
- 数字越小优先级越高
- 同一优先级的错误按时间顺序展示第一个
- 被过滤的错误不展示，不记录

### 3.3 后端改动

#### 3.3.1 ErrorCodeEnum 改动

在 `ErrorCodeEnum` 中添加 `priority` 字段：

```java
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {
    // 原有字段
    private final Integer code;
    private final String message;
    
    // 新增字段
    private final Integer priority; // 1=最高优先级，5=最低优先级
}
```

**示例**：
```java
UNAUTHORIZED(401, "未登录或登录已过期，请重新登录", 1),
SERVER_ERROR(500, "系统繁忙，请稍后再试", 2),
USER_NOT_FOUND(601, "用户不存在", 3),
ORG_NOT_FOUND(613, "机构不存在", 4),
CONFIG_NOT_FOUND(635, "配置不存在", 5),
```

#### 3.3.2 Result 响应结构改动

在 `Result` 类中添加 `priority` 字段：

```java
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private Integer priority; // 新增：错误优先级
    
    public static <T> Result<T> error(ErrorCodeEnum errorCode) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMessage(errorCode.getMessage());
        result.setPriority(errorCode.getPriority()); // 从枚举获取优先级
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
```

#### 3.3.3 GlobalExceptionHandler

无需额外改动，只要确保使用 `Result.error(ErrorCodeEnum)` 即可自动包含 `priority`。

### 3.4 前端改动

#### 3.4.1 错误收集机制

在 HTTP 响应拦截器中添加错误收集逻辑：

```javascript
// 错误队列
let errorQueue = [];
let isCollecting = false; // 是否正在收集错误

// 响应拦截器
axios.interceptors.response.use(
  response => response,
  error => {
    const { response } = error;
    if (response && response.data) {
      const { code, message, priority } = response.data;
      
      // 收集错误
      errorQueue.push({ code, message, priority });
      
      // 如果不在收集期，开启新的收集窗口
      if (!isCollecting) {
        isCollecting = true;
        setTimeout(() => {
          showHighestPriorityError();
          errorQueue = []; // 清空队列
          isCollecting = false; // 结束收集期
        }, 500); // 固定500ms时间窗口
      }
    }
    return Promise.reject(error);
  }
);
```

**关键点**：
- 使用 `isCollecting` 标志位，确保时间窗口不会被重置
- 第一个错误触发时间窗口，后续错误只加入队列
- 500ms 后必定展示，不会无限延迟

#### 3.4.2 错误展示逻辑

从错误队列中选择最高优先级的错误并展示：

```javascript
function showHighestPriorityError() {
  if (errorQueue.length === 0) return;
  
  // 按priority 排序（数字越小优先级越高）
  errorQueue.sort((a, b) => a.priority - b.priority);
  
  // 只展示最高优先级的错误
  const highestError = errorQueue[0];
  
  // 展示错误提示
  Message.error(highestError.message);
  
  // 可选：记录被过滤的错误到控制台（开发环境）
  if (process.env.NODE_ENV === 'development' && errorQueue.length > 1) {
    console.warn('已过滤的错误：', errorQueue.slice(1));
  }
}
```

### 3.5 数据流

**完整流程**：

1. **后端抛出异常** → `GlobalExceptionHandler` 捕获
2. **构造响应** → `Result.error(ErrorCodeEnum)` 包含 `priority`
3. **返回前端** → HTTP 响应包含 `{code, message, priority}`
4. **前端拦截** → 响应拦截器收集错误到队列
5. **时间窗口** → 500ms 内收集所有错误
6. **排序过滤** → 按 `priority` 排序，取最高优先级
7. **展示错误** → `Message.error()` 展示给用户

**示例场景**：

页面加载时并行请求3个接口：
- 接口A：返回 `USER_NOT_FOUND` (priority=3)
- 接口B：返回 `UNAUTHORIZED` (priority=1)
- 接口C：返回 `SERVER_ERROR` (priority=2)

→ 前端只展示 `UNAUTHORIZED` (priority=1)

---

## 4. 测试策略

### 4.1 后端测试

1. **单元测试**：验证每个 `ErrorCodeEnum` 都有正确的 `priority` 值
2. **单元测试**：验证 `Result.error()` 正确返回 `priority`
3. **集成测试**：验证异常响应包含 `priority` 字段

### 4.2 前端测试

1. **单元测试**：验证错误收集逻辑（时间窗口、队列管理）
2. **单元测试**：验证优先级排序逻辑
3. **集成测试**：模拟多个接口同时报错，验证只展示最高优先级错误

### 4.3 手工测试

1. 页面加载时触发多个接口错误，验证只弹出一个提示
2. 验证不同优先级组合的展示效果
3. 验证时间窗口机制（连续错误不会无限延迟）

---

## 5. 实施计划

### 5.1 后端改动

1. 修改 `ErrorCodeEnum`，为所有错误码添加 `priority` 字段（约300+个错误码）
2. 修改 `Result` 类，添加 `priority` 字段
3. 验证 `GlobalExceptionHandler` 使用 `Result.error(ErrorCodeEnum)`
4. 编写单元测试

### 5.2 前端改动

1. 修改 HTTP 响应拦截器，添加错误收集逻辑
2. 实现错误展示逻辑
3. 编写单元测试和集成测试

### 5.3 联调测试

1. 前后端联调，验证端到端流程
2. 手工测试各种错误组合场景
3. 性能测试（确保500ms延迟不影响用户体验）

---

## 6. 风险和注意事项

### 6.1 风险

1. **优先级分配错误**：如果某个错误码的优先级分配不合理，可能导致重要错误被过滤
2. **时间窗口过长**：500ms的延迟可能影响用户体验（但相比多个弹窗，这是可接受的）
3. **前端兼容性**：需要确保所有前端项目都更新拦截器逻辑

### 6.2 注意事项

1. 优先级分配需要仔细review，确保符合业务逻辑
2. 被过滤的错误应该在开发环境记录到控制台，方便调试
3. 如果未来需要展示多个错误，可以扩展为"展示前N个最高优先级错误"

---

## 7. 后续优化

1. **动态优先级**：根据业务场景动态调整优先级（如订单页面中订单错误优先级更高）
2. **错误聚合**：相同类型的错误聚合展示（如"3个接口加载失败"）
3. **错误重试**：对于某些可重试的错误，自动重试而不展示给用户

---

## 8. 参考资料

- 现有错误码定义：`yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`
- 统一响应结构：`yigongbao-common/src/main/java/com/yigongbao/common/result/Result.java`
