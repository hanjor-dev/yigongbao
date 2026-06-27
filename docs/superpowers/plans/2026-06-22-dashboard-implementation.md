# 数据概览接口实施计划

**创建日期**: 2026-06-22  
**版本**: v1.0  
**实施阶段**: 第一阶段（核心角色，无缓存）

---

## 一、需求概述

### 1.1 背景

前端已完成首页数据概览 UI 开发，需要后端提供统一的数据接口。不同角色用户需要看到不同维度的统计数据：
- 业务员：关注自己的订单业绩
- 设计师：关注自己的工单进度
- 超级管理员：关注全局数据和系统监控

### 1.2 接口定义

```
GET /yi/dashboard/{roleCode}?timeRange={today|week|month|quarter|year}
```

**路径参数**：
- `roleCode`: 角色代码（salesman/designer/super_admin等）

**查询参数**：
- `timeRange`: 时间范围（today/week/month/quarter/year）

### 1.3 响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "cards": [],      // KPI卡片（4个）
    "charts": [],     // 图表（2-5个）
    "todos": [],      // 待办事项
    "system": {}      // 系统监控（仅super_admin）
  }
}
```

### 1.4 第一阶段目标

✅ **实现范围**：
- 3个核心角色：salesman、designer、super_admin
- 5种时间范围：today/week/month/quarter/year
- 数据权限隔离
- 响应时间 < 500ms

❌ **暂不实施**：
- Redis缓存（保持数据实时性）
- 其他8个角色（后续阶段）

---

## 二、技术方案

### 2.1 架构设计

**设计模式**: 策略模式（Strategy Pattern）

```
DashboardController (统一入口)
    ↓
DashboardService (路由分发)
    ↓
DashboardStrategy (策略接口)
    ├── SalesmanDashboardStrategy
    ├── DesignerDashboardStrategy
    └── SuperAdminDashboardStrategy
```

### 2.2 模块结构

**新增模块**: `yigongbao-module-dashboard`

```
yigongbao-module-dashboard/
├── controller/
│   └── DashboardController.java
├── service/
│   ├── IDashboardService.java
│   ├── impl/DashboardServiceImpl.java
│   └── strategy/
│       ├── DashboardStrategy.java
│       ├── SalesmanDashboardStrategy.java
│       ├── DesignerDashboardStrategy.java
│       └── SuperAdminDashboardStrategy.java
├── vo/
│   ├── DashboardVO.java
│   ├── CardVO.java
│   ├── ChartVO.java
│   ├── TodoVO.java
│   └── SystemVO.java
├── dto/
│   └── DashboardQueryDTO.java
├── enums/
│   ├── RoleCodeEnum.java
│   └── TimeRangeEnum.java
└── util/
    └── TimeRangeUtil.java
```

### 2.3 依赖关系

```xml
<dependencies>
    <!-- 订单模块（业务员数据） -->
    <dependency>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-module-order</artifactId>
    </dependency>
    
    <!-- 设计模块（设计师数据） -->
    <dependency>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-module-design</artifactId>
    </dependency>
    
    <!-- 系统模块（用户角色） -->
    <dependency>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-module-system</artifactId>
    </dependency>
</dependencies>
```

### 2.4 数据库查询规范

**【强制】数据库操作方式**：

本项目**严禁使用 XML 文件**进行数据库查询，必须使用以下方式：

1. **优先使用 MyBatis-Plus**
   - 简单查询：使用 `LambdaQueryWrapper`
   - 复杂聚合：使用 `QueryWrapper`
   - 示例见下文代码块

2. **复杂查询降级方案**
   - 使用 `@Select`/`@Insert`/`@Update`/`@Delete` 注解
   - 直接在 Mapper 接口方法上编写 SQL

**正确示例（MyBatis-Plus）**：
```java
// 业务员查询订单数
LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(OrderMainEntity::getOperatorId, userId)
       .between(OrderMainEntity::getCreateTime, startTime, endTime);
Long count = orderMapper.selectCount(wrapper);
```

**错误示例（禁止）**：
```xml
<!-- ❌ 禁止在 XML 文件中编写 SQL -->
<select id="countOrders" resultType="long">
    SELECT COUNT(*) FROM order_main WHERE operator_id = #{userId}
</select>
```

### 2.5 数据库优化

**新增索引**：

```sql
-- 业务员查询优化
CREATE INDEX idx_order_operator_time 
ON order_main(operator_id, create_time, status);

-- 设计师查询优化
CREATE INDEX idx_order_designer_phase 
ON order_main(designer_id, phase, create_time, status);

-- 时间范围查询优化
CREATE INDEX idx_order_create_time 
ON order_main(create_time, status);
```

**预期效果**：
- 业务员查询：< 50ms
- 设计师查询：< 50ms
- 超级管理员查询：100-300ms

---

## 三、安全设计（重要）

### 3.1 权限验证流程

**【强制】双重权限验证**：

```
1. 角色权限验证：用户的角色代码 是否匹配 请求的 roleCode
2. 数据权限验证：使用 OrderQueryHelper/DesignQueryHelper 过滤数据
```

**实现逻辑**：

```java
@Service
public class DashboardServiceImpl implements IDashboardService {
    
    @Override
    public DashboardVO getDashboard(String roleCode, Long userId, TimeRangeEnum timeRange) {
        // 1. 获取当前用户信息
        UserEntity currentUser = userService.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        
        // 2. 【关键】角色权限验证：用户角色 vs 请求角色
        if (!validateRoleAccess(currentUser, roleCode)) {
            log.warn("角色权限校验失败: userId={}, userRole=, requestRole={}", 
                userId, currentUser.getRoleCode(), roleCode);
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, "无权访问该角色数据");
        }
        
        // 3. 选择策略执行查询（策略内部会应用数据权限过滤）
        DashboardStrategy strategy = strategyMap.get(roleCode);
        return strategy.buildDashboard(userId, timeRange);
    }
    
    /**
     * 验证用户是否有权限访问指定角色的数据
     */
    private boolean validateRoleAccess(UserEntity user, String requestRoleCode) {
        String userRoleCode = user.getRoleCode();
        
        // 超级管理员可以访问所有角色数据
        if ("super_admin".equals(userRoleCode)) {
            return true;
        }
        
        // 其他角色只能访问自己的数据
        return userRoleCode.equals(requestRoleCode);
    }
}
```

### 3.2 数据越权防护

**潜在越权风险**：

| 风险场景 | 攻击方式 | 防护措施 |
|---------|---------|---------|
| 角色伪造 | 业务员请求 `/dashboard/super_admin` | 角色权限验证（见 3.1） |
| 数据泄露 | 直接修改 userId 参数 | 从 SaToken 获取 userId，不信任前端传参 |
| SQL注入 | 时间范围参数注入 | 使用枚举验证，MyBatis-Plus 参数化查询 |

**【强制】安全规则**：

```java
// ✅ 正确：从 SaToken 获取当前登录用户 ID
Long userId = StpUtil.getLoginIdAsLong();

// ❌ 错误：从请求参数获取 userId（可被篡改）
Long userId = request.getParameter("userId");
```

### 3.3 查询权限隔离

**MyBatis-Plus 查询示例**：

```java
@Service
public class SalesmanDashboardStrategy implements DashboardStrategy {
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private UserService userService;
    
    @Override
    public DashboardVO buildDashboard(Long userId, TimeRangeEnum timeRange) {
        // 获取时间范围
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(timeRange);
        
        // 构建查询条件
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(OrderMainEntity::getCreateTime, range[0], range[1]);
        
        // 【关键】应用数据权限过滤
        UserEntity currentUser = userService.getById(userId);
        OrderQueryHelper.buildDataScopeCondition(wrapper, currentUser);
        
        // 执行查询（已自动加上 operator_id = userId 过滤）
        List<OrderMainEntity> orders = orderMapper.selectList(wrapper);
        
        // 构建响应
        return buildVO(orders);
    }
}
```

**查询聚合示例（使用 QueryWrapper）**：

```java
// 统计订单数
LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(OrderMainEntity::getOperatorId, userId)
       .between(OrderMainEntity::getCreateTime, startTime, endTime);
Long count = orderMapper.selectCount(wrapper);

// 按状态分组统计
QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
wrapper.select("status, COUNT(*) as count")
       .eq("operator_id", userId)
       .between("create_time", startTime, endTime)
       .groupBy("status");
List<Map<String, Object>> statusCount = orderMapper.selectMaps(wrapper);

// 求和金额
QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
wrapper.select("SUM(estimated_cost) as total")
       .eq("operator_id", userId)
       .eq("status", 80);
Map<String, Object> result = orderMapper.selectOne(wrapper);
```

---

## 四、异常处理

### 4.1 异常分类

| 异常类型 | HTTP状态码 | ErrorCode | 处理方式 |
|---------|-----------|-----------|---------|
| 角色不存在 | 400 | 400 | 抛出 BusinessException |
| 无权限访问 | 403 | 403 | 抛出 BusinessException |
| 时间范围错误 | 400 | 400 | 抛出 IllegalArgumentException |
| 数据查询失败 | 500 | 500 | 记录 ERROR 日志，抛出异常 |
| 用户未登录 | 401 | 401 | SaToken 拦截器自动处理 |

### 4.2 异常处理实现

**Controller 层**：

```java
@RestController
@RequestMapping("/yi/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final IDashboardService dashboardService;
    
    @GetMapping("/{roleCode}")
    @OperationLog(module = "数据概览", operationType = "查询")
    public Result<DashboardVO> getDashboard(
        @PathVariable String roleCode,
        @RequestParam String timeRange
    ) {
        // 1. 获取当前登录用户（SaToken 已验证登录状态）
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 2. 验证并转换时间范围参数
        TimeRangeEnum timeRangeEnum;
        try {
            timeRangeEnum = TimeRangeEnum.fromCode(timeRange);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, 
                "无效的时间范围: " + timeRange);
        }
        
        // 3. 验证角色代码
        try {
            RoleCodeEnum.fromCode(roleCode);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, 
                "不支持的角色代码: " + roleCode);
        }
        
        // 4. 执行查询（异常由 GlobalExceptionHandler 统一处理）
        DashboardVO data = dashboardService.getDashboard(roleCode, userId, timeRangeEnum);
        
        return Result.success(data);
    }
}
```

**Service 层**：

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class SalesmanDashboardStrategy implements DashboardStrategy {
    
    @Override
    public DashboardVO buildDashboard(Long userId, TimeRangeEnum timeRange) {
        try {
            log.info("构建业务员数据概览: userId={}, timeRange={}", userId, timeRange);
            
            // 查询数据
            DashboardVO vo = new DashboardVO();
            vo.setCards(buildCards(userId, timeRange));
            vo.setCharts(buildCharts(userId, timeRange));
            vo.setTodos(buildTodos(userId));
            
            log.info("业务员数据概览构建完成: userId={}, cardCount={}, chartCount={}", 
                userId, vo.getCards().size(), vo.getCharts().size());
            
            return vo;
            
        } catch (Exception e) {
            log.error("构建业务员数据概览失败: userId={}, timeRange={}, error={}", 
                userId, timeRange, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "数据查询失败");
        }
    }
}
```

### 4.3 日志记录规范

**【强制】日志输出位置**：

- ❌ Controller 层：禁止输出日志（由 @OperationLog 统一记录）
- ✅ Service 层：必须记录关键业务操作和异常

**日志示例**：

```java
// 入口日志（INFO）
log.info("构建数据概览: roleCode={}, userId={}, timeRange={}", roleCode, userId, timeRange);

// 关键节点日志（INFO）
log.info("订单统计完成: userId={}, totalOrders={}, duration={}ms", userId, count, duration);

// 异常日志（ERROR）
log.error("数据查询失败: userId={}, sql={}, error={}", userId, sql, e.getMessage(), e);

// 性能日志（WARN）
if (duration > 500) {
    log.warn("慢查询检测: method={}, userId={}, duration={}ms", method, userId, duration);
}
```

---

## 五、数据权限设计

### 5.1 权限矩阵

| 角色 | 数据范围 | SQL过滤条件 | 实现方式 |
|------|---------|------------|---------|
| salesman | 自己的订单 | `operator_id = userId` | OrderQueryHelper.buildDataScopeCondition(SELF) |
| designer | 自己的工单 | `designer_id = userId AND phase = 20` | DesignQueryHelper.buildDataScopeCondition(SELF) |
| super_admin | 全部数据 | 无限制 | buildDataScopeCondition(ALL) |

### 3.2 权限实现示例

```java
// 复用现有权限逻辑
QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
wrapper.between("create_time", startTime, endTime);

// 应用数据权限
UserEntity currentUser = userService.getById(userId);
OrderQueryHelper.buildDataScopeCondition(wrapper, currentUser);

// 执行查询
List<OrderMainEntity> orders = orderMapper.selectList(wrapper);
```

---

## 四、核心实现

### 4.1 业务员（salesman）

#### KPI卡片（4个）

| key | title | 数据来源 |
|-----|-------|---------|
| myOrders | 我的订单 | `COUNT(*) WHERE operator_id = ? AND create_time BETWEEN ? AND ?` |
| pendingOrders | 待处理订单 | `COUNT(*) WHERE operator_id = ? AND status IN (10,20)` |
| completedOrders | 已完成订单 | `COUNT(*) WHERE operator_id = ? AND status = 80` |
| myRevenue | 我的业绩 | `SUM(estimated_cost) WHERE operator_id = ? AND status = 80` |

#### 图表（2个）

**1. 订单趋势（折线图）**

使用 MyBatis-Plus QueryWrapper：

```java
// today: 按小时分组
QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
wrapper.select("HOUR(create_time) as hour, COUNT(*) as count")
       .eq("operator_id", userId)
       .apply("DATE(create_time) = CURDATE()")
       .groupBy("HOUR(create_time)")
       .orderBy(true, true, "hour");
List<Map<String, Object>> hourlyData = orderMapper.selectMaps(wrapper);

// week: 按星期分组
QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
wrapper.select("DAYOFWEEK(create_time) as weekday, COUNT(*) as count")
       .eq("operator_id", userId)
       .apply("YEARWEEK(create_time) = YEARWEEK(NOW())")
       .groupBy("DAYOFWEEK(create_time)")
       .orderBy(true, true, "weekday");
List<Map<String, Object>> weeklyData = orderMapper.selectMaps(wrapper);
```

**2. 订单阶段分布（饼图）**

```java
QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
wrapper.select("phase, COUNT(*) as count")
       .eq("operator_id", userId)
       .between("create_time", startTime, endTime)
       .groupBy("phase");
List<Map<String, Object>> phaseData = orderMapper.selectMaps(wrapper);
```

#### 待办事项

- 待跟进客户（暂返回mock数据）
- 待报价订单：`status IN (10, 20)`
- 客户催单：`is_urgent = 1`

---

### 4.2 设计师（designer）

#### KPI卡片（4个）

| key | title | 数据来源 |
|-----|-------|---------|
| myWorkorders | 我的工单 | `COUNT(*) WHERE designer_id = ? AND phase = 20` |
| pendingReview | 待审核 | `COUNT(*) WHERE designer_id = ? AND status = 50` |
| reworkCount | 返工次数 | `COUNT(*) WHERE designer_id = ? AND status = 45` |
| completedWorkorders | 已完成 | `COUNT(*) WHERE designer_id = ? AND status >= 60` |

#### 图表（2个）

- 工单趋势（折线图）：与业务员类似，过滤条件改为 `designer_id = ? AND phase = 20`
- 工单状态分布（饼图）：按 status 分组统计

#### 待办事项

- 紧急工单：`is_urgent = 1`
- 待提交审核：`status IN (30, 40)`
- 返工修改：`status = 45`

---

### 4.3 超级管理员（super_admin）

#### KPI卡片（4个）

| key | title | 数据来源 |
|-----|-------|---------|
| totalOrders | 订单总数 | `COUNT(*) WHERE create_time BETWEEN ? AND ?` |
| totalRevenue | 总营收 | `SUM(estimated_cost) WHERE status = 80` |
| totalUsers | 用户总数 | `COUNT(*) FROM sys_user WHERE status = 1` |
| avgOrderCycle | 平均订单周期 | `AVG(TIMESTAMPDIFF(HOUR, create_time, actual_complete_time))` |

#### 图表（5个）

1. 同比数据对比（今年 vs 去年）
2. 环比数据对比（本月 vs 上月）
3. 订单趋势（无权限过滤）
4. 各部门业绩（JOIN sys_dept）
5. 用户活跃热力图（需要 sys_login_log 表）

#### 系统监控

- healthStatus: 检查MySQL/Redis连接状态
- avgResponseTime: 从日志或APM获取
- onlineUsers: 统计活跃session数
- avgOrderCycle: 平均订单周期

---

## 五、实施步骤

### 阶段1：基础设施（1-2天）

**任务清单**：
- [ ] 创建 `yigongbao-module-dashboard` 模块
- [ ] 创建 VO 类：DashboardVO/CardVO/ChartVO/TodoVO/SystemVO
- [ ] 创建 DTO 类：DashboardQueryDTO
- [ ] 创建枚举：RoleCodeEnum/TimeRangeEnum
- [ ] 实现 TimeRangeUtil 工具类
- [ ] 创建 DashboardStrategy 接口

**验收标准**：
- 模块编译通过
- TimeRangeUtil 单元测试通过

---

### 阶段2：核心角色实现（3-4天）

**任务清单**：
- [ ] 实现 SalesmanDashboardStrategy
  - [ ] 实现 buildCards() 方法
  - [ ] 实现 buildCharts() 方法
  - [ ] 实现 buildTodos() 方法
- [ ] 实现 DesignerDashboardStrategy
  - [ ] 实现 buildCards() 方法
  - [ ] 实现 buildCharts() 方法
  - [ ] 实现 buildTodos() 方法
- [ ] 实现 SuperAdminDashboardStrategy
  - [ ] 实现 buildCards() 方法
  - [ ] 实现 buildCharts() 方法
  - [ ] 实现 buildSystemVO() 方法
- [ ] 实现 DashboardServiceImpl（路由分发）
- [ ] 实现 DashboardController

**验收标准**：
- 每个角色的单元测试通过
- 接口返回结构与前端定义匹配

---

### 阶段3：性能优化（1天）

**任务清单**：
- [ ] 添加数据库索引
- [ ] 使用 EXPLAIN 分析 SQL 性能
- [ ] 添加 @OperationLog 记录响应时间
- [ ] 慢查询优化（如有必要）

**验收标准**：
- 业务员/设计师查询 < 50ms
- 超级管理员查询 < 300ms
- 无慢查询告警

---

### 阶段4：测试与联调（1天）

**任务清单**：
- [ ] 单元测试（覆盖率 > 80%）
- [ ] Postman 集成测试
- [ ] 验证数据权限隔离
- [ ] 前端联调
- [ ] 响应结构校验

**测试用例**：
```bash
# 业务员今日数据
GET /yi/dashboard/salesman?timeRange=today
Authorization: Bearer {业务员token}

# 设计师本周数据
GET /yi/dashboard/designer?timeRange=week
Authorization: Bearer {设计师token}

# 超级管理员本年数据
GET /yi/dashboard/super_admin?timeRange=year
Authorization: Bearer {管理员token}
```

**验收标准**：
- 所有测试用例通过
- 前端对接无报错
- 数据准确性验证通过

---

## 六、关键代码示例

### 6.1 DashboardStrategy 接口

```java
public interface DashboardStrategy {
    /**
     * 构建数据概览
     */
    DashboardVO buildDashboard(Long userId, TimeRangeEnum timeRange);
}
```

### 6.2 TimeRangeUtil 工具类

```java
public class TimeRangeUtil {
    public static LocalDateTime[] getStartAndEndTime(TimeRangeEnum timeRange) {
        LocalDateTime now = LocalDateTime.now();
        switch (timeRange) {
            case TODAY:
                return new LocalDateTime[]{
                    now.toLocalDate().atStartOfDay(),
                    now.toLocalDate().atTime(23, 59, 59)
                };
            case WEEK:
                // 本周一 ~ 本周日
            // ...
        }
    }
    
    public static List<String> getXAxisLabels(TimeRangeEnum timeRange) {
        // 生成 X 轴标签
    }
}
```

### 6.3 Controller 层

```java
@RestController
@RequestMapping("/yi/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final IDashboardService dashboardService;
    
    @GetMapping("/{roleCode}")
    @OperationLog(module = "数据概览", operationType = "查询")
    public Result<DashboardVO> getDashboard(
        @PathVariable String roleCode,
        @RequestParam String timeRange
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        TimeRangeEnum timeRangeEnum = TimeRangeEnum.fromCode(timeRange);
        
        DashboardVO data = dashboardService.getDashboard(roleCode, userId, timeRangeEnum);
        return Result.success(data);
    }
}
```

---

## 七、风险与依赖

### 7.1 技术风险

| 风险项 | 影响 | 缓解措施 |
|-------|------|---------|
| 查询性能不达标 | 响应慢，用户体验差 | 添加索引，优化SQL，按需启用缓存 |
| 数据量过大 | 超级管理员查询慢 | 分页查询，异步预计算 |
| 时间范围计算错误 | 数据统计不准确 | 单元测试覆盖，边界值测试 |

### 7.2 依赖项

**已具备**：
- ✅ order_main 表
- ✅ OrderQueryHelper 数据权限过滤
- ✅ FlowStatusEnum/FlowPhaseEnum

**需确认**：
- ⚠️ sys_login_log 表（用户活跃热力图）
- ⚠️ 客户模块（业务员待办"待跟进客户"）

---

## 八、验收标准

### 8.1 功能完整性

- [x] 3个角色的接口正常返回
- [x] KPI卡片数据准确
- [x] 图表数据格式正确
- [x] 待办事项统计准确

### 8.2 性能指标

- [x] 业务员查询 < 50ms
- [x] 设计师查询 < 50ms
- [x] 超级管理员查询 < 300ms
- [x] 无慢查询（> 1s）

### 8.3 数据安全

- [x] 业务员只能查看自己的订单
- [x] 设计师只能查看自己的工单
- [x] 无越权访问漏洞

### 8.4 前端对接

- [x] 响应结构与前端 TypeScript 接口匹配
- [x] 所有字段类型正确
- [x] 无字段缺失或多余

---

## 九、后续优化方向

### 9.1 性能优化（按需）

如果响应时间 > 500ms：
- 添加 Redis 缓存（支持 forceRefresh 参数）
- 定时任务预计算统计数据
- 使用物化视图

### 9.2 功能扩展

第二阶段：
- design_manager（设计主管）
- area_manager（区域经理）
- production_manager（生产主管）

第三阶段：
- production（生产工人）
- quality（质检员）
- warehouse（仓库管理员）
- finance（财务）
- company_admin（公司管理员）

---

**文档完成，预计实施周期：6-8天**
