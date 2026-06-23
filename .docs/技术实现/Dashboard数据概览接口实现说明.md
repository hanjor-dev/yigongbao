# Dashboard 数据概览接口实现说明

## 文档信息

- **创建时间**: 2026-06-23
- **模块**: yigongbao-module-dashboard
- **接口地址**: `GET /yi/dashboard/{roleCode}?timeRange={today|week|month|quarter|year}`
- **版本**: v1.0

---

## 目录

1. [概述](#概述)
2. [角色实现总览](#角色实现总览)
3. [核心角色实现详情](#核心角色实现详情)
   - [业务员 (salesman)](#业务员-salesman)
   - [设计师 (designer)](#设计师-designer)
   - [超级管理员 (super_admin)](#超级管理员-super_admin)
4. [管理角色实现详情](#管理角色实现详情)
   - [区域管理员 (regional-manager)](#区域管理员-regional-manager)
   - [设计管理员 (designer-manager)](#设计管理员-designer-manager)
   - [生产管理员 (production-manager)](#生产管理员-production-manager)
   - [财务 (finance)](#财务-finance)
5. [辅助角色实现详情](#辅助角色实现详情)
6. [数据字典](#数据字典)

---

## 概述

本文档详细说明了 Dashboard 数据概览接口为各个角色提供的数据查询实现。接口采用策略模式，针对不同角色提供定制化的数据视图，包括 KPI 卡片、可视化图表和待办事项。

### 核心设计

- **策略模式**: 每个角色对应一个独立的 Strategy 类
- **数据权限**: 基于角色和用户ID进行数据过滤
- **时间范围**: 支持 today/week/month/quarter/year 五种时间范围
- **数据来源**: 主要基于 order_main 表，部分角色涉及 production 相关表

---

## 角色实现总览

| 角色代码 | 角色名称 | 实现状态 | 数据来源 | 说明 |
|---------|---------|---------|---------|------|
| salesman | 业务员 | ✅ 完全实现 | order_main | 基于 operator_id 过滤 |
| designer | 设计师 | ✅ 完全实现 | order_main | 基于 designer_id + phase=20 过滤 |
| super_admin | 超级管理员 | ✅ 完全实现 | order_main + sys_user | 全局数据，无权限限制 |
| regional-manager | 区域管理员 | ✅ 完全实现 | order_main | 部门内所有业务员数据 |
| designer-manager | 设计管理员 | ✅ 完全实现 | order_main | 设计阶段全局数据 |
| production-manager | 生产管理员 | ⚠️ 部分实现 | order_main | 生产阶段数据，部分图表待实现 |
| finance | 财务 | ✅ 完全实现 | order_main | 基于 estimated_cost 统计营收 |
| production-worker | 生产员 | ❌ 待实现 | production_record/product | 需要生产模块支持 |
| qc | 质检员 | ❌ 待实现 | production_product | 需要生产模块支持 |
| qc-manager | 质检主管 | ❌ 待实现 | production_product | 需要生产模块支持 |
| warehouse-manager | 仓管 | ❌ 待实现 | production_product | 需要生产模块支持 |

---

## 核心角色实现详情

### 业务员 (salesman)

**策略类**: `SalesmanDashboardStrategy`  
**数据权限**: `operator_id = userId`  
**实现状态**: ✅ 完全实现

#### KPI 卡片（4个）

| 卡片Key | 标题 | 查询逻辑 |
|---------|------|---------|
| myOrders | 我的订单 | `SELECT COUNT(*) FROM order_main WHERE operator_id = ? AND create_time BETWEEN ? AND ?` |
| pendingOrders | 待处理订单 | `WHERE operator_id = ? AND status IN (10, 20) AND create_time BETWEEN ? AND ?` |
| completedOrders | 已完成订单 | `WHERE operator_id = ? AND status = 80 AND create_time BETWEEN ? AND ?` |
| myRevenue | 我的业绩 | `SELECT SUM(estimated_cost)/10000 WHERE operator_id = ? AND status = 80 AND create_time BETWEEN ? AND ?` |

#### 图表（2个）

**1. 订单趋势 (orderTrend) - 折线图**

查询逻辑：
```sql
-- today: 按小时分组（每2小时一个点）
SELECT HOUR(create_time) as hour, COUNT(*) as count
FROM order_main
WHERE operator_id = ? AND DATE(create_time) = CURDATE()
GROUP BY HOUR(create_time)

-- week: 按星期分组
SELECT DAYOFWEEK(create_time) as weekday, COUNT(*) as count
FROM order_main
WHERE operator_id = ? AND YEARWEEK(create_time) = YEARWEEK(NOW())
GROUP BY DAYOFWEEK(create_time)

-- month/quarter/year: 按日期分组
SELECT DATE(create_time) as date, COUNT(*) as count
FROM order_main
WHERE operator_id = ? AND create_time BETWEEN ? AND ?
GROUP BY DATE(create_time)
```

**2. 订单阶段分布 (orderPhase) - 饼图**

查询逻辑：
```sql
SELECT phase, COUNT(*) as count
FROM order_main
WHERE operator_id = ? AND create_time BETWEEN ? AND ?
GROUP BY phase
```

阶段映射：
- 10 → 订单阶段
- 20 → 设计阶段
- 30 → 打印阶段
- 40 → 后处理阶段
- 50 → 质检阶段
- 60 → 仓储阶段
- 70 → 确认阶段
- 80 → 已完成

#### 待办事项（2个）

| 待办ID | 标题 | 查询逻辑 |
|--------|------|---------|
| 1 | 待报价订单 | `WHERE operator_id = ? AND status IN (10, 20)` |
| 2 | 客户催单 | `WHERE operator_id = ? AND is_urgent = 1 AND status != 80` |

---

### 设计师 (designer)

**策略类**: `DesignerDashboardStrategy`  
**数据权限**: `designer_id = userId AND phase = 20`  
**实现状态**: ✅ 完全实现

#### KPI 卡片（4个）

| 卡片Key | 标题 | 查询逻辑 |
|---------|------|---------|
| myWorkorders | 我的工单 | `SELECT COUNT(*) WHERE designer_id = ? AND phase = 20 AND create_time BETWEEN ? AND ?` |
| pendingReview | 待审核 | `WHERE designer_id = ? AND phase = 20 AND status = 50` |
| reworkCount | 返工次数 | `WHERE designer_id = ? AND phase = 20 AND status = 45 AND create_time BETWEEN ? AND ?` |
| completedWorkorders | 已完成 | `WHERE designer_id = ? AND phase = 20 AND status >= 60 AND create_time BETWEEN ? AND ?` |

#### 图表（2个）

**1. 工单趋势 (workorderTrend) - 折线图**

查询逻辑：与业务员订单趋势类似，但增加条件 `phase = 20`

```sql
-- 示例（week）
SELECT DAYOFWEEK(create_time) as weekday, COUNT(*) as count
FROM order_main
WHERE designer_id = ? AND phase = 20 AND YEARWEEK(create_time) = YEARWEEK(NOW())
GROUP BY DAYOFWEEK(create_time)
```

**2. 工单状态分布 (workorderStatus) - 饼图**

查询逻辑：
```sql
SELECT status, COUNT(*) as count
FROM order_main
WHERE designer_id = ? AND phase = 20 AND create_time BETWEEN ? AND ?
GROUP BY status
```

状态映射：
- status >= 60 → 已通过
- status = 50 → 待审核
- status = 45 → 需返工
- status >= 30 → 设计中

#### 待办事项（3个）

| 待办ID | 标题 | 查询逻辑 |
|--------|------|---------|
| 1 | 紧急工单 | `WHERE designer_id = ? AND phase = 20 AND is_urgent = 1 AND status < 60` |
| 2 | 待提交审核 | `WHERE designer_id = ? AND phase = 20 AND status IN (30, 40)` |
| 3 | 返工修改 | `WHERE designer_id = ? AND phase = 20 AND status = 45` |

---

### 超级管理员 (super_admin)

**策略类**: `SuperAdminDashboardStrategy`  
**数据权限**: 无限制（全局数据）  
**实现状态**: ✅ 完全实现

#### KPI 卡片（4个）

| 卡片Key | 标题 | 查询逻辑 |
|---------|------|---------|
| totalOrders | 订单总数 | `SELECT COUNT(*) FROM order_main WHERE create_time BETWEEN ? AND ?` |
| totalRevenue | 总营收 | `SELECT SUM(estimated_cost)/10000 WHERE status = 80 AND create_time BETWEEN ? AND ?` |
| totalUsers | 用户总数 | `SELECT COUNT(*) FROM sys_user WHERE status = 1` |
| avgOrderCycle | 平均订单周期 | `SELECT AVG(TIMESTAMPDIFF(HOUR, create_time, actual_complete_time)) WHERE status = 80` |

#### 图表（5个）

**1. 同比数据对比 (yearComparison) - 折线图**

查询逻辑：
```sql
-- 今年每月订单数 vs 去年同期
SELECT YEAR(create_time) as year, MONTH(create_time) as month, COUNT(*) as count
FROM order_main
WHERE YEAR(create_time) IN (YEAR(NOW()), YEAR(NOW())-1)
GROUP BY YEAR(create_time), MONTH(create_time)
```

返回两条 series：今年、去年

**2. 环比数据对比 (monthComparison) - 折线图**

查询逻辑：
```sql
-- 本月每周 vs 上月对应周
SELECT YEAR(create_time) as year, MONTH(create_time) as month, 
       WEEK(create_time) as week, COUNT(*) as count
FROM order_main
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 2 MONTH)
GROUP BY YEAR(create_time), MONTH(create_time), WEEK(create_time)
```

返回两条 series：本月、上月

**3. 订单趋势 (orderTrend) - 折线图**

与业务员订单趋势类似，但无权限过滤。

**4. 各部门业绩 (deptPerformance) - 柱状图**

查询逻辑：
```sql
SELECT operator_dept_name, COUNT(*) as count
FROM order_main
WHERE create_time BETWEEN ? AND ? AND operator_dept_name IS NOT NULL
GROUP BY operator_dept_name
ORDER BY count DESC
LIMIT 10
```

**5. 用户活跃热力图 (userActivityHeatmap) - 热力图**

查询逻辑：
```sql
SELECT 
  FLOOR(HOUR(login_time) / 2) * 2 as hour_slot,
  DAYOFWEEK(login_time) - 1 as weekday,
  COUNT(*) as count
FROM sys_login_log
WHERE login_time BETWEEN ? AND ?
GROUP BY hour_slot, weekday
```

返回 7×12 的矩阵数据（周一到周日 × 0-22点，每2小时一格）

#### 系统监控（SystemVO）

| 字段 | 说明 | 获取方式 |
|------|------|---------|
| healthStatus | 健康状态 | 检查 MySQL/Redis 连接状态 |
| avgResponseTime | 平均响应时间 | 从日志或 APM 获取 |
| onlineUsers | 在线用户数 | `SELECT COUNT(DISTINCT user_id) FROM sa_token_session WHERE last_active_time > NOW() - INTERVAL 30 MINUTE` |
| avgOrderCycle | 平均订单周期 | 同 KPI 卡片 |

#### 待办事项

返回空列表

---

## 管理角色实现详情

### 区域管理员 (regional-manager)

**策略类**: `RegionalManagerDashboardStrategy`  
**数据权限**: 部门内所有业务员 (`dept_id = user.deptId`)  
**实现状态**: ✅ 完全实现

#### KPI 卡片（4个）

| 卡片Key | 标题 | 查询逻辑 |
|---------|------|---------|
| deptOrders | 部门订单 | `SELECT COUNT(*) WHERE operator_id IN (operatorIds) AND create_time BETWEEN ? AND ?` |
| pendingOrders | 待处理 | `WHERE operator_id IN (operatorIds) AND status IN (10, 20, 30) AND create_time BETWEEN ? AND ?` |
| completedOrders | 已完成 | `WHERE operator_id IN (operatorIds) AND status = 80 AND create_time BETWEEN ? AND ?` |
| topSalesman | 最佳业务员 | `SELECT real_name FROM sys_user WHERE id IN (operatorIds) LIMIT 1` |

**说明**: `operatorIds` 通过查询 `sys_user` 表获取：`SELECT id FROM sys_user WHERE dept_id = user.getDeptId()`

#### 图表（3个）

**1. 订单趋势 (orderTrend) - 折线图**

查询逻辑：
```sql
-- today: 按小时分组
SELECT HOUR(create_time) as hour, COUNT(*) as count
FROM order_main
WHERE operator_id IN (operatorIds) AND DATE(create_time) = CURDATE()
GROUP BY HOUR(create_time)

-- week: 按星期分组
SELECT DAYOFWEEK(create_time) as weekday, COUNT(*) as count
FROM order_main
WHERE operator_id IN (operatorIds) AND YEARWEEK(create_time) = YEARWEEK(NOW())
GROUP BY DAYOFWEEK(create_time)

-- month/quarter/year: 按日期分组
SELECT DATE(create_time) as date, COUNT(*) as count
FROM order_main
WHERE operator_id IN (operatorIds) AND create_time BETWEEN ? AND ?
GROUP BY DATE(create_time)
```

**2. 业务员业绩排行 (salesmanPerformance) - 柱状图**

查询逻辑：
```sql
SELECT operator_id, operator_name, COUNT(*) as count
FROM order_main
WHERE operator_id IN (operatorIds) AND create_time BETWEEN ? AND ?
GROUP BY operator_id
ORDER BY count DESC
LIMIT 10
```

**3. 每日提单次数 (dailySubmissions) - 柱状图**

查询逻辑：
```sql
SELECT DATE(create_time) as date, COUNT(*) as count
FROM order_main
WHERE operator_id IN (operatorIds) AND create_time BETWEEN ? AND ?
GROUP BY DATE(create_time)
ORDER BY date ASC
```

X轴显示格式：`日期.getDayOfMonth() + "日"`

#### 待办事项

返回空列表

---

### 设计管理员 (designer-manager)

**策略类**: `DesignerManagerDashboardStrategy`  
**数据权限**: 设计阶段全局数据 (`phase = 20`)  
**实现状态**: ✅ 完全实现

#### KPI 卡片（4个）

| 卡片Key | 标题 | 查询逻辑 |
|---------|------|---------|
| allWorkorders | 全部工单 | `SELECT COUNT(*) WHERE phase = 20 AND create_time BETWEEN ? AND ?` |
| pendingReview | 待审核 | `WHERE phase = 20 AND status = 50 AND create_time BETWEEN ? AND ?` |
| reworkCount | 返工次数 | `WHERE phase = 20 AND status = 45 AND create_time BETWEEN ? AND ?` |
| avgPassRate | 平均通过率 | `(已通过数 / 总数) * 100`，其中已通过数 = `WHERE phase = 20 AND status >= 60` |

#### 图表（3个）

**1. 工单趋势 (workorderTrend) - 折线图**

查询逻辑：与区域管理员订单趋势类似，但过滤条件为 `phase = 20`

**2. 设计师工作量 (designerWorkload) - 柱状图**

查询逻辑：
```sql
SELECT designer_id, designer_name, COUNT(*) as count
FROM order_main
WHERE phase = 20 AND create_time BETWEEN ? AND ?
GROUP BY designer_id
ORDER BY count DESC
LIMIT 10
```

**3. 审核通过率趋势 (reviewPassRate) - 折线图**

查询逻辑：
```sql
-- today: 按小时分组
SELECT HOUR(create_time) as hour,
       SUM(CASE WHEN status >= 60 THEN 1 ELSE 0 END) as passed,
       COUNT(*) as total
FROM order_main
WHERE phase = 20 AND DATE(create_time) = CURDATE()
GROUP BY HOUR(create_time)

-- week: 按星期分组
SELECT DAYOFWEEK(create_time) as weekday,
       SUM(CASE WHEN status >= 60 THEN 1 ELSE 0 END) as passed,
       COUNT(*) as total
FROM order_main
WHERE phase = 20 AND YEARWEEK(create_time) = YEARWEEK(NOW())
GROUP BY DAYOFWEEK(create_time)

-- month/quarter/year: 按日期分组
SELECT DATE(create_time) as date,
       SUM(CASE WHEN status >= 60 THEN 1 ELSE 0 END) as passed,
       COUNT(*) as total
FROM order_main
WHERE phase = 20 AND create_time BETWEEN ? AND ?
GROUP BY DATE(create_time)
```

通过率计算：`(passed / total) * 100`

#### 待办事项

返回空列表

---

### 生产管理员 (production-manager)

**策略类**: `ProductionManagerDashboardStrategy`  
**数据权限**: 生产阶段全局数据 (`phase = 30`)  
**实现状态**: ⚠️ 部分实现

#### KPI 卡片（4个）

| 卡片Key | 标题 | 查询逻辑 |
|---------|------|---------|
| inProduction | 生产中 | `SELECT COUNT(*) WHERE phase = 30 AND status IN (61, 62) AND create_time BETWEEN ? AND ?` |
| pendingQc | 待质检 | `WHERE phase = 30 AND status = 63 AND create_time BETWEEN ? AND ?` |
| completed | 已完成 | `WHERE phase = 30 AND status = 70 AND create_time BETWEEN ? AND ?` |
| avgQualityRate | 平均质检率 | `(已完成数 / 总数) * 100` |

#### 图表（3个）

**1. 生产趋势 (productionTrend) - 折线图**

查询逻辑：与区域管理员订单趋势类似，但过滤条件为 `phase = 30`

**2. 生产工人工作量 (workerWorkload) - 柱状图**

当前状态：返回"暂无数据"（需要 production_record 表支持）

**3. 质检通过率趋势 (qualityRateTrend) - 折线图**

查询逻辑：
```sql
-- 示例（week）
SELECT DAYOFWEEK(create_time) as weekday,
       SUM(CASE WHEN status = 70 THEN 1 ELSE 0 END) as passed,
       COUNT(*) as total
FROM order_main
WHERE phase = 30 AND YEARWEEK(create_time) = YEARWEEK(NOW())
GROUP BY DAYOFWEEK(create_time)
```

通过率计算：`(passed / total) * 100`

#### 待办事项

返回空列表

---

### 财务 (finance)

**策略类**: `FinanceDashboardStrategy`  
**数据权限**: 无限制（全局数据）  
**实现状态**: ✅ 完全实现

#### KPI 卡片（4个）

| 卡片Key | 标题 | 查询逻辑 |
|---------|------|---------|
| totalRevenue | 总营收 | `SELECT SUM(estimated_cost)/10000 WHERE status = 80 AND create_time BETWEEN ? AND ?` |
| receivableAmount | 应收账款 | 返回 "0.0"（需要收款相关表支持） |
| receivedAmount | 已收账款 | 返回 "0.0"（需要收款相关表支持） |
| overdueAmount | 逾期账款 | 返回 "0.0"（需要收款相关表支持） |

**说明**: 
- 营收金额单位：元 → 万元（除以 10000）
- 应收/已收/逾期账款需要财务相关表支持，当前返回固定值 0

#### 图表（3个）

**1. 营收趋势 (revenueTrend) - 折线图**

查询逻辑：
```sql
-- today: 按小时分组
SELECT HOUR(create_time) as hour, 
       IFNULL(SUM(estimated_cost), 0) / 10000 as revenue
FROM order_main
WHERE status = 80 AND DATE(create_time) = CURDATE()
GROUP BY HOUR(create_time)

-- week: 按星期分组
SELECT DAYOFWEEK(create_time) as weekday,
       IFNULL(SUM(estimated_cost), 0) / 10000 as revenue
FROM order_main
WHERE status = 80 AND YEARWEEK(create_time) = YEARWEEK(NOW())
GROUP BY DAYOFWEEK(create_time)

-- month/quarter/year: 按日期分组
SELECT DATE(create_time) as date,
       IFNULL(SUM(estimated_cost), 0) / 10000 as revenue
FROM order_main
WHERE status = 80 AND create_time BETWEEN ? AND ?
GROUP BY DATE(create_time)
```

**2. 回款状态 (paymentStatus) - 饼图**

当前状态：返回"暂无数据"（需要财务收款表支持）

**3. 各部门业绩 (deptRevenue) - 柱状图**

查询逻辑：
```sql
SELECT operator_dept_name, 
       IFNULL(SUM(estimated_cost), 0) / 10000 as revenue
FROM order_main
WHERE status = 80 AND create_time BETWEEN ? AND ?
  AND operator_dept_name IS NOT NULL
GROUP BY operator_dept_name
ORDER BY revenue DESC
LIMIT 10
```

#### 待办事项（2个）

| 待办ID | 标题 | 说明 |
|--------|------|------|
| 1 | 逾期账款 | count=0（需要财务表支持） |
| 2 | 待开发票 | count=0（需要财务表支持） |

---

## 辅助角色实现详情

### 生产员 (production-worker)

**策略类**: `ProductionWorkerDashboardStrategy`  
**数据权限**: `producer_id = userId` 或 `create_by = userId`  
**实现状态**: ❌ 待实现（需要 production_record/production_product 表支持）

#### 当前返回数据

**KPI 卡片（4个）**:
- myTasks（我的任务）: 0
- completedTasks（已完成）: 0
- qualityRate（质检通过率）: "0.0"
- avgProductionTime（平均生产时长）: "0"

**图表（4个）**:
- yearProduction（同比生产对比）: 12个月数据全为 0
- monthProduction（环比生产对比）: 4周数据全为 0
- productionTrend（生产趋势）: 返回"暂无数据"
- reworkTrend（返工趋势）: 返回"暂无数据"

**待办事项**: 空列表

#### 未来实现方案

依赖表：`production_record`, `production_product`

查询逻辑示例：
```sql
-- KPI：质检通过率
SELECT (SUM(CASE WHEN qc_result='pass' THEN 1 ELSE 0 END) * 100.0 / COUNT(*))
FROM production_product
WHERE create_by = ? AND create_time BETWEEN ? AND ?

-- KPI：平均生产时长
SELECT AVG(TIMESTAMPDIFF(HOUR, print_start_time, print_finish_time))
FROM production_record
WHERE producer_id = ? OR create_by = ?
```

---

### 质检员 (qc)

**策略类**: `QcDashboardStrategy`  
**数据权限**: `qc_user_id = userId`  
**实现状态**: ❌ 待实现（需要 production_product 表支持）

#### 当前返回数据

**KPI 卡片（4个）**:
- totalQC（质检总数）: 0
- passCount（通过数量）: 0
- failCount（不合格数量）: 0
- passRate（通过率）: "0.0"

**图表（2个）**:
- qcResultTrend（质检结果趋势）: 返回"暂无数据"
- productTypeDistribution（产品类型分布）: 返回"暂无数据"

**待办事项**: 空列表

**日志警告**: `质检员数据概览暂不支持真实数据查询，需要 production_product 表支持`

#### 未来实现方案

依赖表：`production_product`

查询逻辑示例：
```sql
-- KPI：质检总数
SELECT COUNT(*) FROM production_product 
WHERE qc_user_id = ? AND qc_time BETWEEN ? AND ?

-- KPI：通过数量
SELECT COUNT(*) FROM production_product 
WHERE qc_user_id = ? AND qc_result = 'pass' AND qc_time BETWEEN ? AND ?

-- 图表：质检结果趋势
SELECT DATE(qc_time) as date,
       SUM(CASE WHEN qc_result='pass' THEN 1 ELSE 0 END) as pass_count,
       SUM(CASE WHEN qc_result='redo' THEN 1 ELSE 0 END) as fail_count
FROM production_product
WHERE qc_user_id = ? AND qc_time BETWEEN ? AND ?
GROUP BY DATE(qc_time)
```

---

### 质检主管 (qc-manager)

**策略类**: `QcManagerDashboardStrategy`  
**数据权限**: 无限制（全局质检数据）  
**实现状态**: ❌ 待实现（需要 production_product 表支持）

#### 当前返回数据

与质检员相同，但数据范围为全局。

**日志警告**: `质检主管数据概览暂不支持真实数据查询，需要 production_product 表支持`

---

### 仓管 (warehouse-manager)

**策略类**: `WarehouseManagerDashboardStrategy`  
**数据权限**: 无限制（全局仓储数据）  
**实现状态**: ❌ 待实现（需要 production_product 表支持）

#### 当前返回数据

**KPI 卡片（4个）**:
- totalStock（库存总数）: 0
- inboundToday（今日入库）: 0
- outboundToday（今日出库）: 0
- lowStockCount（低库存预警）: 0

**图表（2个）**:
- stockTrend（出入库趋势）: 返回"暂无数据"
- stockDistribution（库存分布）: 返回"暂无数据"

**待办事项（2个）**:
- 低库存预警: count=0
- 待入库单据: count=0

**日志警告**: `仓管数据概览暂不支持真实数据查询，需要 production_product 表支持`

#### 未来实现方案

依赖表：`production_product`

查询逻辑示例：
```sql
-- KPI：库存总数
SELECT COUNT(*) FROM production_product
WHERE warehouse_in_time IS NOT NULL 
  AND warehouse_out_time IS NULL

-- KPI：今日入库
SELECT COUNT(*) FROM production_product
WHERE DATE(warehouse_in_time) = CURDATE()

-- 图表：出入库趋势
-- 入库
SELECT DATE(warehouse_in_time) as date, COUNT(*) as count
FROM production_product
WHERE warehouse_in_time BETWEEN ? AND ?
GROUP BY DATE(warehouse_in_time)

-- 出库
SELECT DATE(warehouse_out_time) as date, COUNT(*) as count
FROM production_product
WHERE warehouse_out_time BETWEEN ? AND ?
GROUP BY DATE(warehouse_out_time)
```

---

## 数据字典

### order_main 表字段说明

| 字段名 | 类型 | 说明 | 使用场景 |
|--------|------|------|---------|
| id | BIGINT | 主键 | - |
| create_time | DATETIME | 创建时间 | 时间范围过滤、趋势统计 |
| operator_id | BIGINT | 业务员ID | 业务员、区域管理员数据权限 |
| operator_name | VARCHAR | 业务员姓名 | 业务员业绩排行 |
| operator_dept_name | VARCHAR | 业务员部门名称 | 各部门业绩统计 |
| designer_id | BIGINT | 设计师ID | 设计师数据权限 |
| designer_name | VARCHAR | 设计师姓名 | 设计师工作量统计 |
| status | INT | 订单状态 | 订单阶段判断、完成状态 |
| phase | INT | 订单阶段 | 阶段过滤（设计/生产） |
| estimated_cost | DECIMAL | 预估成本 | 营收统计 |
| is_urgent | TINYINT | 是否加急 | 催单待办统计 |
| actual_complete_time | DATETIME | 实际完成时间 | 订单周期计算 |

### status 字段值说明

| 值 | 说明 | 使用场景 |
|---|------|---------|
| 10 | 待报价 | 待处理订单统计 |
| 20 | 已报价 | 待处理订单统计 |
| 30-44 | 设计中 | 设计师工单统计 |
| 45 | 需返工 | 返工次数统计 |
| 50 | 待审核 | 待审核统计 |
| 60-79 | 已通过各阶段 | 已完成统计 |
| 80 | 已完成 | 完成订单、营收统计 |

### phase 字段值说明

| 值 | 说明 | 使用角色 |
|---|------|---------|
| 10 | 订单阶段 | 业务员 |
| 20 | 设计阶段 | 设计师、设计管理员 |
| 30 | 打印阶段 | 生产管理员 |
| 40 | 后处理阶段 | - |
| 50 | 质检阶段 | - |
| 60 | 仓储阶段 | - |
| 70 | 确认阶段 | - |
| 80 | 已完成 | - |

### 时间范围参数说明

| 参数值 | 说明 | X轴标签格式 | 分组粒度 |
|--------|------|------------|---------|
| today | 今日 | 0时, 2时, ..., 22时 | 按小时分组，每2小时一个点 |
| week | 本周 | 周一, 周二, ..., 周日 | 按星期分组 |
| month | 本月 | 1日, 5日, 10日, ... | 按日期分组 |
| quarter | 本季度 | 1月, 2月, 3月 | 按月分组 |
| year | 本年 | 1月, 2月, ..., 12月 | 按月分组 |

### 响应数据结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "cards": [
      {
        "key": "myOrders",
        "title": "我的订单",
        "value": 45,
        "unit": "单",
        "link": "/order"
      }
    ],
    "charts": [
      {
        "key": "orderTrend",
        "title": "订单趋势",
        "type": "line",
        "data": {
          "xAxis": ["周一", "周二", "周三", "周四", "周五", "周六", "周日"],
          "series": [
            {
              "name": "订单数",
              "data": [5, 8, 6, 9, 7, 5, 5]
            }
          ]
        }
      }
    ],
    "todos": [
      {
        "id": 1,
        "title": "待报价订单",
        "count": 5,
        "link": "/order?status=quote_needed",
        "urgent": false
      }
    ],
    "system": {
      "healthStatus": "healthy",
      "avgResponseTime": "120ms",
      "onlineUsers": 156,
      "avgOrderCycle": "432h"
    }
  }
}
```

**说明**:
- `cards`: KPI 卡片数组
- `charts`: 图表数组
- `todos`: 待办事项数组
- `system`: 系统监控信息（仅 super_admin 角色返回）

---

## 实现总结

### 已完成角色（7个）

1. **salesman** - 业务员自己的订单数据
2. **designer** - 设计师自己的工单数据
3. **super_admin** - 全局数据 + 系统监控
4. **regional-manager** - 部门内业务员数据
5. **designer-manager** - 设计阶段全局数据
6. **production-manager** - 生产阶段数据（部分图表基于 order_main）
7. **finance** - 财务营收数据（基于 estimated_cost）

### 待实现角色（4个）

需要生产模块的 Mapper 支持：
1. **production-worker** - 依赖 production_record/production_product 表
2. **qc** - 依赖 production_product 表
3. **qc-manager** - 依赖 production_product 表
4. **warehouse-manager** - 依赖 production_product 表

### 性能优化建议

1. **数据库索引**:
   - `idx_order_operator_time`: (operator_id, create_time, status)
   - `idx_order_designer_phase`: (designer_id, phase, create_time, status)
   - `idx_order_create_time`: (create_time, status)
   - `idx_order_phase_status`: (phase, status, create_time)

2. **查询优化**:
   - 使用 `IFNULL()` 避免空值计算错误
   - 时间范围查询使用 `BETWEEN` 或 MySQL 内置函数
   - 避免 `SELECT *`，只查询需要的字段
   - 聚合函数配合 `CASE WHEN` 减少多次查询

3. **未来扩展**:
   - 考虑引入 Redis 缓存（TTL: today=60s, week/month=300s）
   - 对于 super_admin 的全局统计，可使用异步预计算
   - 活跃热力图数据量大时，可按周或月预聚合

---

**文档版本**: 1.0  
**最后更新**: 2026-06-23  
**维护人员**: Dashboard 模块开发团队
