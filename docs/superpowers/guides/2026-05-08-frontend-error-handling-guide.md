# 前端多接口错误处理实现指南

**日期**：2026-05-08  
**作者**：hanjor  
**后端实现状态**：✅ 已完成

---

## 概述

本指南用于指导前端开发人员实现基于优先级的多接口错误处理机制。后端已完成所有改动，所有错误响应现在都包含 `priority` 字段（1-5，数字越小优先级越高）。

**核心目标**：当页面并行调用多个接口时，如果多个接口同时报错，只展示最高优先级的一条错误信息。

---

## 后端改动总结

后端已完成以下改动（前端无需关注实现细节）：

1. ✅ `ErrorCodeEnum` 添加 `priority` 字段（1-5级）
2. ✅ `Result` 响应结构添加 `priority` 字段
3. ✅ 所有错误响应都包含正确的 `priority`

**错误优先级分级**：

| 优先级 | 说明 | 示例错误 |
|--------|------|----------|
| 1 | 认证/授权错误 | 401未登录、403无权限、密码错误 |
| 2 | 系统错误 | 500系统繁忙、503服务不可用、限流 |
| 3 | 核心业务错误 | 用户不存在、订单不存在、设计相关错误 |
| 4 | 辅助业务错误 | 机构不存在、医院不存在、产品不存在 |
| 5 | 配置/基础数据错误 | 配置不存在、字典不存在、参数错误 |

---

## 前端实现步骤

### 步骤1：修改 HTTP 响应拦截器

**文件位置**：前端项目的 HTTP 拦截器文件（如 `src/utils/request.js` 或 `src/api/interceptor.js`）

#### 1.1 添加错误队列变量

在拦截器文件顶部添加：

```javascript
// 错误队列
let errorQueue = [];
// 是否正在收集错误
let isCollecting = false;
```

#### 1.2 修改响应错误拦截器

```javascript
axios.interceptors.response.use(
  response => response,
  error => {
    const { response } = error;
    
    if (response && response.data) {
      const { code, message, priority } = response.data;
      
      // 收集错误到队列
      errorQueue.push({ code, message, priority: priority || 999 });
      
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

**关键点说明**：
- `priority || 999`：如果后端返回的 priority 为 null/undefined，设置为最低优先级999
- `isCollecting` 标志位确保时间窗口不会被重置
- 第一个错误触发时间窗口，后续错误只加入队列
- 500ms 后必定展示，不会无限延迟

---

### 步骤2：实现错误展示逻辑

在同一个拦截器文件中添加 `showHighestPriorityError()` 函数：

```javascript
/**
 * 展示最高优先级的错误
 */
function showHighestPriorityError() {
  if (errorQueue.length === 0) return;
  
  // 按 priority 排序（数字越小优先级越高）
  errorQueue.sort((a, b) => a.priority - b.priority);
  
  // 只展示最高优先级的错误
  const highestError = errorQueue[0];
  
  // 展示错误提示（根据你的UI框架调整）
  // 示例：Element Plus
  ElMessage.error(highestError.message);
  
  // 示例：Ant Design Vue
  // message.error(highestError.message);
  
  // 示例：Naive UI
  // window.$message.error(highestError.message);
  
  // 可选：开发环境下记录被过滤的错误到控制台
  if (process.env.NODE_ENV === 'development' && errorQueue.length > 1) {
    console.warn('已过滤的错误：', errorQueue.slice(1));
  }
}
```

**注意事项**：
- 根据项目使用的UI框架调整错误提示方法
- 开发环境下建议记录被过滤的错误，方便调试

---

### 步骤3：编写前端单元测试

**文件位置**：前端项目的测试文件（如 `src/utils/__tests__/request.test.js`）

#### 3.1 测试错误收集机制

```javascript
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
// 或者使用 Jest: import { describe, it, expect, jest, beforeEach, afterEach } from '@jest/globals';

describe('错误收集机制', () => {
  beforeEach(() => {
    vi.useFakeTimers(); // 使用假定时器
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('应该收集500ms内的所有错误', async () => {
    // Mock 多个接口错误
    const errors = [
      { code: 601, message: '用户不存在', priority: 3 },
      { code: 401, message: '未登录', priority: 1 },
      { code: 500, message: '系统繁忙', priority: 2 }
    ];

    // 模拟触发多个错误
    errors.forEach(err => {
      // 触发错误拦截器逻辑
      // 具体实现根据你的拦截器结构调整
    });

    // 验证错误队列包含所有错误
    expect(errorQueue.length).toBe(3);
  });
});
```

#### 3.2 测试优先级排序

```javascript
describe('优先级排序', () => {
  it('应该只展示最高优先级的错误', async () => {
    const mockMessage = vi.fn();
    
    // Mock 不同优先级的错误
    errorQueue = [
      { code: 601, message: '用户不存在', priority: 3 },
      { code: 401, message: '未登录', priority: 1 },
      { code: 500, message: '系统繁忙', priority: 2 }
    ];

    showHighestPriorityError();

    // 验证只展示了优先级1的错误
    expect(mockMessage).toHaveBeenCalledWith('未登录');
    expect(mockMessage).toHaveBeenCalledTimes(1);
  });
});
```

#### 3.3 测试时间窗口机制

```javascript
describe('时间窗口机制', () => {
  it('应该在500ms后展示错误', async () => {
    const mockMessage = vi.fn();
    
    // 触发第一个错误
    // ... 触发错误拦截器逻辑

    // 快进500ms
    vi.advanceTimersByTime(500);

    // 验证错误已展示
    expect(mockMessage).toHaveBeenCalled();
  });

  it('不应该无限延迟展示', async () => {
    // 触发第一个错误
    // ... 触发错误拦截器逻辑

    // 在时间窗口内触发更多错误
    vi.advanceTimersByTime(200);
    // ... 触发更多错误

    vi.advanceTimersByTime(200);
    // ... 触发更多错误

    // 快进到500ms
    vi.advanceTimersByTime(100);

    // 验证错误已展示（不会因为新错误而延迟）
    expect(isCollecting).toBe(false);
  });
});
```

---

## 测试验证

### 手工测试场景

#### 场景1：页面加载时多个接口报错

1. 打开浏览器开发者工具
2. 访问一个会并行调用多个接口的页面（如订单详情页）
3. 模拟多个接口同时返回错误（可以通过后端或Mock）
4. **预期结果**：只弹出一个最高优先级的错误提示

#### 场景2：不同优先级组合

测试以下组合，验证只展示最高优先级的错误：

| 组合 | 预期展示 |
|------|----------|
| 认证错误(1) + 业务错误(3) | 认证错误 |
| 系统错误(2) + 业务错误(3) | 系统错误 |
| 多个业务错误(3) | 第一个业务错误 |

#### 场景3：时间窗口机制

1. 触发第一个错误
2. 在500ms内触发更多错误
3. **预期结果**：500ms后展示最高优先级的错误，不会无限延迟

---

## 常见问题

### Q1: 如果后端返回的 priority 为 null 怎么办？

A: 代码中已处理：`priority: priority || 999`，会将 null/undefined 设置为最低优先级999。

### Q2: 500ms 的延迟会影响用户体验吗？

A: 相比多个错误弹窗，500ms 的延迟是可接受的。用户通常不会感知到这个延迟。

### Q3: 如何在开发环境下查看被过滤的错误？

A: 代码中已包含开发环境日志：
```javascript
if (process.env.NODE_ENV === 'development' && errorQueue.length > 1) {
  console.warn('已过滤的错误：', errorQueue.slice(1));
}
```

### Q4: 如果需要展示多个错误怎么办？

A: 修改 `showHighestPriorityError()` 函数，改为展示前N个最高优先级的错误：
```javascript
// 展示前3个最高优先级的错误
const topErrors = errorQueue.slice(0, 3);
topErrors.forEach(err => ElMessage.error(err.message));
```

---

## 完成检查清单

- [ ] HTTP 响应拦截器添加错误收集逻辑
- [ ] 实现 `showHighestPriorityError()` 函数
- [ ] 编写单元测试（错误收集、优先级排序、时间窗口）
- [ ] 手工测试：页面加载时多个接口报错
- [ ] 手工测试：不同优先级组合
- [ ] 手工测试：时间窗口机制
- [ ] 验证开发环境下控制台日志正常

---

## 参考资料

- 后端设计文档: `docs/superpowers/specs/2026-05-08-multi-error-handling-design.md`
- 后端实施计划: `docs/superpowers/plans/2026-05-08-multi-error-handling.md`
- 后端提交记录: `feat(error): 为错误码添加优先级支持`

---

**如有问题，请联系后端开发人员或查看设计文档。**
