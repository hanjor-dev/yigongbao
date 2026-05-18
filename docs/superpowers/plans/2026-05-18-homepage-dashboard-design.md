# 医工宝系统首页数据看板设计方案

**文档版本**: 1.0  
**创建日期**: 2026-05-18  
**设计目标**: 为不同角色设计个性化的首页数据看板，提供直观的数据展示和快捷操作入口

---

## 一、设计概述

### 1.1 背景

医工宝系统包含11个核心角色，涵盖业务线、设计线、生产线和管理层。不同角色的工作重点和关注数据维度差异显著，需要为每个角色设计个性化的首页看板，提升工作效率和用户体验。

### 1.2 设计目标

1. **个性化展示**：每个角色看到最相关的数据和操作入口
2. **数据直观**：混合使用数字卡片和可视化图表，核心指标一目了然
3. **实时更新**：定时刷新（每5分钟），保证数据时效性
4. **灵活筛选**：支持时间维度筛选（今日/本周/本月/本季度/本年/自定义）
5. **可扩展性**：核心角色深度定制，新增角色使用通用模板

### 1.3 用户需求确认

- **可视化需求**：混合模式（核心指标用数字卡片，趋势数据用图表）
- **数据更新**：定时刷新（每5分钟自动刷新）
- **时间筛选**：需要时间维度筛选功能
- **业务员数据范围**：仅看自己创建的订单（operator_id = 当前用户）
- **扩展策略**：核心角色定制 + 通用模板

---

## 二、设计原则

### 2.1 角色定制策略

**核心角色（11个）**：深度定制，提供最优用户体验
- 业务线：业务员、区域管理员
- 设计线：设计师、设计管理员
- 生产线：生产员、生产管理员、质管、库管
- 管理层：财务、公司管理员、超级管理员

**新增角色**：使用通用模板，根据 data_scope_type 自动适配数据范围

### 2.2 数据权限适配

| data_scope_type | 数据范围 | SQL WHERE 条件 |
|-----------------|----------|----------------|
| self | 仅自己创建的数据 | `operator_id = 当前用户ID` |
| hospitals | 关联医院的数据 | `hospital_id IN (SELECT hospital_id FROM sys_user_hospital WHERE user_id = 当前用户ID)` |
| dept | 本部门的数据 | `operator_dept_id = 当前用户部门ID` |
| org | 本机构的数据 | `org_id = 当前用户机构ID` |
| all | 所有数据 | 无限制 |

### 2.3 布局规范

**统一布局结构**（所有角色共享）：

1. **顶部区域**：时间筛选器 + 刷新按钮
   - 时间选项：今日/本周/本月/本季度/本年/自定义
   - 自动刷新：每5分钟
   - 手动刷新按钮

2. **数据卡片区**（第一屏）：3-6个核心指标卡片
   - 显示数值 + 环比变化
   - 点击可跳转到详情页

3. **图表区域**（第二屏）：1-4个可视化图表
   - 趋势图、对比图、占比图等
   - 支持图表内时间切换

4. **快捷操作/列表区**（第三屏）：
   - 待办事项列表或快捷操作入口

---

## 三、业务线角色设计

### 3.1 业务员角色

**角色特点**：
- 数据范围：仅看自己创建的订单（operator_id = 当前用户）
- 关注重点：订单获取、订单进展、客户维护
- 核心KPI：订单数量、订单金额、完成率

**数据卡片区（4个核心指标）**：

1. **我的订单总数**
   - 数值：当前时间段内创建的订单总数
   - 环比：与上一时间段对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE operator_id = ? AND create_time BETWEEN ? AND ?`

2. **待处理订单**
   - 数值：状态为"待审核"或"已驳回"的订单数
   - 点击跳转到订单列表
   - SQL: `SELECT COUNT(*) FROM order_main WHERE operator_id = ? AND status IN (1020, 1040)`

3. **进行中订单**
   - 数值：已通过审核、未完成的订单数
   - 按阶段分组显示（设计中、生产中、质检中等）
   - SQL: `SELECT phase, COUNT(*) FROM order_main WHERE operator_id = ? AND phase BETWEEN 20 AND 70 GROUP BY phase`

4. **已完成订单**
   - 数值：状态为"已完成"的订单数
   - 完成率：已完成/总订单数
   - SQL: `SELECT COUNT(*) FROM order_main WHERE operator_id = ? AND phase = 80`

**图表区域（2个图表）**：

1. **订单趋势图（折线图）**
   - X轴：时间（按日/周/月聚合）
   - Y轴：订单数量
   - 多条线：新建订单、完成订单、驳回订单
   - SQL: `SELECT DATE(create_time) as date, COUNT(*) FROM order_main WHERE operator_id = ? GROUP BY DATE(create_time)`

2. **订单阶段分布（饼图）**
   - 显示当前各阶段订单的占比
   - 点击扇区可跳转到对应阶段的订单列表
   - SQL: `SELECT phase_name, COUNT(*) FROM order_main WHERE operator_id = ? AND is_deleted = 0 GROUP BY phase`

**快捷操作区**：

1. **待办事项列表**（最多显示5条）
   - 待审核的修改申请
   - 被驳回需要重新提交的订单
   - 点击可直接跳转到对应页面

2. **快捷按钮**
   - "新建订单"按钮
   - "我的草稿"按钮

---

### 3.2 区域管理员角色

**角色特点**：
- 数据范围：本部门所有订单（operator_dept_id = 当前用户部门）
- 关注重点：部门业绩、团队管理、订单质量
- 核心KPI：部门订单量、完成率、驳回率、人均产能

**数据卡片区（6个核心指标）**：

1. **部门订单总数**
   - 数值：当前时间段内部门创建的订单总数
   - 环比：与上一时间段对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE operator_dept_id = ? AND create_time BETWEEN ? AND ?`

2. **待审核订单**
   - 数值：等待审核的订单数（包括数据审核、修改申请审核）
   - 点击跳转到审核列表
   - SQL: `SELECT COUNT(*) FROM order_main WHERE operator_dept_id = ? AND status = 1020`

3. **进行中订单**
   - 数值：各阶段进行中的订单数
   - 饼图小图标显示阶段分布
   - SQL: `SELECT COUNT(*) FROM order_main WHERE operator_dept_id = ? AND phase BETWEEN 20 AND 70`

4. **已完成订单**
   - 数值：已完成订单数
   - 完成率百分比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE operator_dept_id = ? AND phase = 80`

5. **驳回订单数**
   - 数值：被驳回的订单数
   - 驳回率：驳回数/总订单数
   - 点击查看驳回原因分析
   - SQL: `SELECT COUNT(*) FROM order_main WHERE operator_dept_id = ? AND status IN (1040, 2040)`

6. **人均订单量**
   - 数值：部门订单总数 / 部门业务员人数
   - 显示排名前3的业务员

**图表区域（3个图表）**：

1. **部门订单趋势图（折线图）**
   - X轴：时间（按日/周/月聚合）
   - Y轴：订单数量
   - 多条线：新建、完成、驳回
   - SQL: `SELECT DATE(create_time) as date, status, COUNT(*) FROM order_main WHERE operator_dept_id = ? GROUP BY DATE(create_time), status`

2. **业务员业绩对比（柱状图）**
   - X轴：业务员姓名
   - Y轴：订单数量
   - 分组：完成订单（绿色）、进行中（蓝色）、驳回（红色）
   - SQL: `SELECT operator_name, phase, COUNT(*) FROM order_main WHERE operator_dept_id = ? GROUP BY operator_name, phase`

3. **订单阶段分布（环形图）**
   - 显示各阶段订单占比
   - 中心显示总订单数
   - SQL: `SELECT phase_name, COUNT(*) FROM order_main WHERE operator_dept_id = ? GROUP BY phase`

**快捷操作区**：

1. **待办事项列表**
   - 待审核的订单（数据审核）
   - 待审核的修改申请
   - 异常订单（超时未处理）

2. **快捷按钮**
   - "审核订单"按钮
   - "团队报表"按钮
   - "分配设计师"按钮

---

## 四、设计线角色设计

### 4.1 设计师角色

**角色特点**：
- 数据范围：仅看分配给自己的工单（designer_id = 当前用户）
- 关注重点：待处理工单、设计进度、审核通过率
- 核心KPI：工单数量、完成率、平均耗时、审核通过率

**数据卡片区（5个核心指标）**：

1. **我的工单总数**
   - 数值：当前时间段内分配给我的工单数
   - 环比：与上一时间段对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE designer_id = ? AND design_start_time BETWEEN ? AND ?`

2. **待开始工单**
   - 数值：状态为"待设计"的工单数
   - 点击跳转到工单列表
   - SQL: `SELECT COUNT(*) FROM order_main WHERE designer_id = ? AND status = 2010`

3. **设计中工单**
   - 数值：状态为"设计中"的工单数
   - 显示平均已用时长
   - SQL: `SELECT COUNT(*) FROM order_main WHERE designer_id = ? AND status = 2020`

4. **待审核工单**
   - 数值：已提交审核、等待审核结果的工单数
   - SQL: `SELECT COUNT(*) FROM order_main WHERE designer_id = ? AND status = 2030`

5. **已完成工单**
   - 数值：审核通过、已完成的工单数
   - 审核通过率：通过数 / (通过数 + 驳回数)
   - SQL: `SELECT COUNT(*) FROM order_main WHERE designer_id = ? AND status >= 3010`

**图表区域（2个图表）**：

1. **工单完成趋势图（折线图）**
   - X轴：时间（按日/周聚合）
   - Y轴：工单数量
   - 多条线：新接工单、完成工单、驳回工单
   - SQL: `SELECT DATE(design_start_time) as date, COUNT(*) FROM order_main WHERE designer_id = ? GROUP BY DATE(design_start_time)`

2. **工单状态分布（饼图）**
   - 显示当前各状态工单的占比
   - 待开始、设计中、待审核、已完成
   - SQL: `SELECT status_name, COUNT(*) FROM order_main WHERE designer_id = ? AND phase = 20 GROUP BY status`

**快捷操作区**：

1. **待办工单列表**（最多显示5条）
   - 优先显示加急工单
   - 显示订单编号、患者姓名、期望交付时间
   - 点击可直接进入设计页面

2. **快捷按钮**
   - "开始设计"按钮
   - "上传设计文件"按钮
   - "提交审核"按钮

---

### 4.2 设计管理员角色

**角色特点**：
- 数据范围：所有设计工单（data_scope_type='all'）
- 关注重点：团队进度、质量管理、资源分配
- 核心KPI：团队工单量、平均耗时、审核通过率、设计师负载

**数据卡片区（6个核心指标）**：

1. **团队工单总数**
   - 数值：当前时间段内所有设计工单数
   - 环比：与上一时间段对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE phase >= 20 AND design_start_time BETWEEN ? AND ?`

2. **待分配工单**
   - 数值：已进入设计阶段但未分配设计师的工单数
   - 点击跳转到分配页面
   - SQL: `SELECT COUNT(*) FROM order_main WHERE phase = 20 AND designer_id IS NULL`

3. **设计中工单**
   - 数值：当前正在设计的工单数
   - 显示各设计师的工单数分布
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status = 2020`

4. **待审核工单**
   - 数值：等待审核的工单数
   - 平均等待时长
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status = 2030`

5. **审核通过率**
   - 数值：审核通过率百分比
   - 计算：通过数 / (通过数 + 驳回数)
   - 趋势：与上周对比
   - SQL: `SELECT COUNT(CASE WHEN review_result = 1 THEN 1 END) / COUNT(*) FROM design_review WHERE review_time BETWEEN ? AND ?`

6. **平均完成时长**
   - 数值：从开始设计到审核通过的平均时长（小时）
   - 趋势：与上周对比
   - SQL: `SELECT AVG(TIMESTAMPDIFF(HOUR, design_start_time, design_submit_time)) FROM order_main WHERE design_submit_time BETWEEN ? AND ?`

**图表区域（3个图表）**：

1. **设计师工作负载对比（柱状图）**
   - X轴：设计师姓名
   - Y轴：工单数量
   - 分组：待开始（灰色）、设计中（蓝色）、已完成（绿色）
   - 用于识别负载不均衡问题
   - SQL: `SELECT designer_name, status, COUNT(*) FROM order_main WHERE phase = 20 GROUP BY designer_name, status`

2. **审核通过率趋势图（折线图）**
   - X轴：时间（按周聚合）
   - Y轴：通过率百分比
   - 多条线：整体通过率、各设计师通过率
   - SQL: `SELECT WEEK(review_time), designer_name, AVG(review_result) FROM design_review GROUP BY WEEK(review_time), designer_name`

3. **工单阶段流转分析（桑基图/漏斗图）**
   - 显示工单从待分配→设计中→待审核→通过/驳回的流转情况
   - 识别瓶颈环节
   - SQL: 需要关联 order_flow_status_history 表

**快捷操作区**：

1. **待办事项列表**
   - 待分配的工单（按加急优先级排序）
   - 待审核的工单
   - 超时未完成的工单（预警）

2. **快捷按钮**
   - "分配设计师"按钮
   - "审核工单"按钮
   - "团队报表"按钮

---

## 五、生产线角色设计

### 5.1 生产员角色

**角色特点**：
- 数据范围：仅看分配给自己的生产任务（production_worker_id = 当前用户）
- 关注重点：待处理任务、生产进度、完成质量
- 核心KPI：任务数量、完成率、平均耗时、返工率

**数据卡片区（5个核心指标）**：

1. **我的任务总数**
   - 数值：当前时间段内分配给我的生产任务数
   - 环比：与上一时间段对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE production_worker_id = ? AND production_start_time BETWEEN ? AND ?`

2. **待开始任务**
   - 数值：状态为"待生产"的任务数
   - 点击跳转到任务列表
   - SQL: `SELECT COUNT(*) FROM order_main WHERE production_worker_id = ? AND status = 3010`

3. **生产中任务**
   - 数值：状态为"生产中"的任务数
   - 显示平均已用时长
   - SQL: `SELECT COUNT(*) FROM order_main WHERE production_worker_id = ? AND status IN (3020, 4010, 4020)`

4. **待质检任务**
   - 数值：已完成生产、等待质检的任务数
   - SQL: `SELECT COUNT(*) FROM order_main WHERE production_worker_id = ? AND status = 5010`

5. **已完成任务**
   - 数值：质检通过的任务数
   - 返工率：返工数 / 总任务数
   - SQL: `SELECT COUNT(*) FROM order_main WHERE production_worker_id = ? AND status >= 5020`

**图表区域（2个图表）**：

1. **任务完成趋势图（折线图）**
   - X轴：时间（按日/周聚合）
   - Y轴：任务数量
   - 多条线：新接任务、完成任务、返工任务
   - SQL: `SELECT DATE(production_start_time) as date, COUNT(*) FROM order_main WHERE production_worker_id = ? GROUP BY DATE(production_start_time)`

2. **任务状态分布（饼图）**
   - 显示当前各状态任务的占比
   - 待开始、生产中、待质检、已完成
   - SQL: `SELECT status_name, COUNT(*) FROM order_main WHERE production_worker_id = ? AND phase BETWEEN 30 AND 50 GROUP BY status`

**快捷操作区**：

1. **待办任务列表**（最多显示5条）
   - 优先显示加急任务
   - 显示订单编号、产品名称、期望交付时间
   - 点击可直接进入生产页面

2. **快捷按钮**
   - "开始生产"按钮
   - "提交质检"按钮

---

### 5.2 生产管理员角色

**角色特点**：
- 数据范围：所有生产任务（data_scope_type='all'）
- 关注重点：团队产能、质量管理、资源分配
- 核心KPI：团队任务量、平均耗时、质检通过率、生产员负载

**数据卡片区（6个核心指标）**：

1. **团队任务总数**
   - 数值：当前时间段内所有生产任务数
   - 环比：与上一时间段对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE phase >= 30 AND production_start_time BETWEEN ? AND ?`

2. **待分配任务**
   - 数值：已进入生产阶段但未分配生产员的任务数
   - 点击跳转到分配页面
   - SQL: `SELECT COUNT(*) FROM order_main WHERE phase >= 30 AND production_worker_id IS NULL`

3. **生产中任务**
   - 数值：当前正在生产的任务数
   - 显示各生产员的任务数分布
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status IN (3020, 4010, 4020)`

4. **待质检任务**
   - 数值：等待质检的任务数
   - 平均等待时长
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status = 5010`

5. **质检通过率**
   - 数值：质检通过率百分比
   - 计算：通过数 / (通过数 + 返工数)
   - 趋势：与上周对比
   - SQL: `SELECT COUNT(CASE WHEN qc_result = 1 THEN 1 END) / COUNT(*) FROM order_main WHERE qc_time BETWEEN ? AND ?`

6. **平均生产时长**
   - 数值：从开始生产到质检通过的平均时长（小时）
   - 趋势：与上周对比
   - SQL: `SELECT AVG(TIMESTAMPDIFF(HOUR, production_start_time, qc_time)) FROM order_main WHERE qc_time BETWEEN ? AND ?`

**图表区域（3个图表）**：

1. **生产员工作负载对比（柱状图）**
   - X轴：生产员姓名
   - Y轴：任务数量
   - 分组：待开始（灰色）、生产中（蓝色）、已完成（绿色）
   - 用于识别负载不均衡问题
   - SQL: `SELECT production_worker_name, status, COUNT(*) FROM order_main WHERE phase BETWEEN 30 AND 50 GROUP BY production_worker_name, status`

2. **质检通过率趋势图（折线图）**
   - X轴：时间（按周聚合）
   - Y轴：通过率百分比
   - 多条线：整体通过率、各生产员通过率
   - SQL: `SELECT WEEK(qc_time), production_worker_name, AVG(qc_result) FROM order_main GROUP BY WEEK(qc_time), production_worker_name`

3. **生产阶段流转分析（漏斗图）**
   - 显示任务从待分配→生产中→后处理→质检→通过/返工的流转情况
   - 识别瓶颈环节
   - SQL: 需要关联 order_flow_status_history 表

**快捷操作区**：

1. **待办事项列表**
   - 待分配的任务（按加急优先级排序）
   - 待质检的任务
   - 超时未完成的任务（预警）
   - 返工任务（需要重新分配）

2. **快捷按钮**
   - "分配生产员"按钮
   - "质检审核"按钮
   - "团队报表"按钮

---

### 5.3 质管角色

**角色特点**：
- 数据范围：所有待质检和已质检的订单（data_scope_type='all'）
- 关注重点：质检任务、质量问题、返工率
- 核心KPI：待质检数量、质检通过率、问题分类统计

**数据卡片区（5个核心指标）**：

1. **待质检任务**
   - 数值：当前待质检的任务数
   - 环比：与上一时间段对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status = 5010`

2. **今日已质检**
   - 数值：今日已完成质检的任务数
   - 通过数/返工数分别显示
   - SQL: `SELECT COUNT(*), SUM(CASE WHEN qc_result = 1 THEN 1 ELSE 0 END) FROM order_main WHERE DATE(qc_time) = CURDATE()`

3. **质检通过率**
   - 数值：当前时间段质检通过率
   - 趋势：与上周对比
   - SQL: `SELECT COUNT(CASE WHEN qc_result = 1 THEN 1 END) / COUNT(*) FROM order_main WHERE qc_time BETWEEN ? AND ?`

4. **返工任务数**
   - 数值：当前返工中的任务数
   - 点击查看返工原因分析
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status = 5030`

5. **平均质检时长**
   - 数值：平均每个任务的质检耗时（分钟）
   - 趋势：与上周对比
   - SQL: `SELECT AVG(TIMESTAMPDIFF(MINUTE, qc_start_time, qc_time)) FROM order_main WHERE qc_time BETWEEN ? AND ?`

**图表区域（2个图表）**：

1. **质检结果趋势图（折线图）**
   - X轴：时间（按日聚合）
   - Y轴：任务数量
   - 多条线：质检通过、质检不通过（返工）
   - SQL: `SELECT DATE(qc_time), qc_result, COUNT(*) FROM order_main WHERE qc_time BETWEEN ? AND ? GROUP BY DATE(qc_time), qc_result`

2. **质量问题分类统计（柱状图）**
   - X轴：问题类型（尺寸偏差、表面瑕疵、材料问题等）
   - Y轴：问题数量
   - 用于识别高频质量问题
   - SQL: `SELECT qc_issue_type, COUNT(*) FROM order_qc_record WHERE qc_result = 0 AND qc_time BETWEEN ? AND ? GROUP BY qc_issue_type`

**快捷操作区**：

1. **待质检任务列表**（最多显示5条）
   - 按提交时间排序
   - 显示订单编号、产品名称、生产员
   - 点击可直接进入质检页面

2. **快捷按钮**
   - "开始质检"按钮
   - "质量报表"按钮

---

### 5.4 库管角色

**角色特点**：
- 数据范围：所有入库和出库的订单（data_scope_type='all'）
- 关注重点：库存管理、入库出库、库存预警
- 核心KPI：待入库数量、库存总量、出库完成率

**数据卡片区（5个核心指标）**：

1. **待入库任务**
   - 数值：质检通过、待入库的任务数
   - 点击跳转到入库页面
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status = 6010`

2. **今日入库数**
   - 数值：今日已完成入库的任务数
   - 环比：与昨日对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE DATE(storage_in_time) = CURDATE()`

3. **当前库存总量**
   - 数值：当前在库的订单数
   - 按产品类型分组显示
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status = 6020`

4. **待出库任务**
   - 数值：客户已确认、待出库的任务数
   - 点击跳转到出库页面
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status = 7010`

5. **今日出库数**
   - 数值：今日已完成出库的任务数
   - 环比：与昨日对比
   - SQL: `SELECT COUNT(*) FROM order_main WHERE DATE(storage_out_time) = CURDATE()`

**图表区域（2个图表）**：

1. **出入库趋势图（折线图）**
   - X轴：时间（按日聚合）
   - Y轴：数量
   - 两条线：入库数量、出库数量
   - SQL: `SELECT DATE(storage_in_time) as date, COUNT(*) FROM order_main GROUP BY DATE(storage_in_time) UNION SELECT DATE(storage_out_time), COUNT(*) FROM order_main GROUP BY DATE(storage_out_time)`

2. **库存产品分布（饼图）**
   - 显示当前库存中各产品类型的占比
   - 点击可查看该类型的详细库存
   - SQL: `SELECT product_name, COUNT(*) FROM order_main WHERE status = 6020 GROUP BY product_name`

**快捷操作区**：

1. **待办任务列表**
   - 待入库任务（按质检完成时间排序）
   - 待出库任务（按客户确认时间排序）
   - 库存预警（超过30天未出库）

2. **快捷按钮**
   - "入库登记"按钮
   - "出库登记"按钮
   - "库存盘点"按钮

---

## 六、管理层角色设计

### 6.1 财务角色

**角色特点**：
- 数据范围：所有订单的财务数据（data_scope_type='all'）
- 关注重点：订单金额、收款情况、财务报表
- 核心KPI：订单总额、已收款、待收款、收款率

**数据卡片区（6个核心指标）**：

1. **订单总金额**
   - 数值：当前时间段内所有订单的总金额
   - 环比：与上一时间段对比
   - SQL: `SELECT SUM(total_amount) FROM order_main WHERE create_time BETWEEN ? AND ?`

2. **已收款金额**
   - 数值：已收款的总金额
   - 收款率：已收款 / 订单总金额
   - SQL: `SELECT SUM(paid_amount) FROM order_main WHERE payment_status = 2`

3. **待收款金额**
   - 数值：未收款或部分收款的订单总金额
   - 点击查看待收款订单列表
   - SQL: `SELECT SUM(total_amount - paid_amount) FROM order_main WHERE payment_status IN (0, 1)`

4. **逾期未收款**
   - 数值：超过约定收款期限仍未收款的金额
   - 逾期订单数量
   - SQL: `SELECT SUM(total_amount - paid_amount), COUNT(*) FROM order_main WHERE payment_status IN (0, 1) AND payment_deadline < NOW()`

5. **本月新增订单额**
   - 数值：本月新增订单的总金额
   - 与上月对比
   - SQL: `SELECT SUM(total_amount) FROM order_main WHERE MONTH(create_time) = MONTH(NOW())`

6. **平均订单金额**
   - 数值：当前时间段内订单的平均金额
   - 趋势：与上一时间段对比
   - SQL: `SELECT AVG(total_amount) FROM order_main WHERE create_time BETWEEN ? AND ?`

**图表区域（3个图表）**：

1. **订单金额趋势图（折线图）**
   - X轴：时间（按月聚合）
   - Y轴：金额
   - 多条线：订单总额、已收款、待收款
   - SQL: `SELECT MONTH(create_time), SUM(total_amount), SUM(paid_amount) FROM order_main GROUP BY MONTH(create_time)`

2. **收款状态分布（饼图）**
   - 显示未收款、部分收款、已收款的订单金额占比
   - SQL: `SELECT payment_status_name, SUM(total_amount) FROM order_main GROUP BY payment_status`

3. **部门业绩对比（柱状图）**
   - X轴：部门名称
   - Y轴：订单金额
   - 用于识别高产出部门
   - SQL: `SELECT operator_dept_name, SUM(total_amount) FROM order_main WHERE create_time BETWEEN ? AND ? GROUP BY operator_dept_name`

**快捷操作区**：

1. **待办事项列表**
   - 逾期未收款订单（按逾期天数排序）
   - 本周到期应收款订单
   - 异常订单（金额异常、折扣异常）

2. **快捷按钮**
   - "收款登记"按钮
   - "财务报表"按钮
   - "导出账单"按钮

---

### 6.2 公司管理员角色

**角色特点**：
- 数据范围：全公司所有数据（data_scope_type='all'）
- 关注重点：全流程监控、团队效率、业务健康度
- 核心KPI：订单总量、各阶段转化率、平均周期、异常订单

**数据卡片区（6个核心指标）**：

1. **订单总量**
   - 数值：当前时间段内订单总数
   - 环比：与上一时间段对比
   - 按状态分组：进行中/已完成/已取消
   - SQL: `SELECT COUNT(*), phase FROM order_main WHERE create_time BETWEEN ? AND ? GROUP BY phase`

2. **订单完成率**
   - 数值：已完成订单 / 总订单数
   - 趋势：与上周对比
   - SQL: `SELECT COUNT(CASE WHEN phase = 80 THEN 1 END) / COUNT(*) FROM order_main WHERE create_time BETWEEN ? AND ?`

3. **平均订单周期**
   - 数值：从创建到完成的平均天数
   - 趋势：与上周对比
   - SQL: `SELECT AVG(DATEDIFF(complete_time, create_time)) FROM order_main WHERE phase = 80 AND complete_time BETWEEN ? AND ?`

4. **各阶段订单分布**
   - 数值：当前各阶段的订单数量
   - 小型柱状图显示分布
   - SQL: `SELECT phase_name, COUNT(*) FROM order_main WHERE is_deleted = 0 GROUP BY phase`

5. **异常订单数**
   - 数值：超时、驳回、返工等异常订单数
   - 异常率：异常数 / 总订单数
   - SQL: `SELECT COUNT(*) FROM order_main WHERE status IN (1040, 2040, 5030) OR (phase < 80 AND DATEDIFF(NOW(), create_time) > expected_days)`

6. **团队人效**
   - 数值：人均订单完成数
   - 计算：已完成订单数 / 总员工数
   - SQL: `SELECT COUNT(*) / (SELECT COUNT(*) FROM sys_user WHERE status = 1) FROM order_main WHERE phase = 80 AND complete_time BETWEEN ? AND ?`

**图表区域（4个图表）**：

1. **订单全流程漏斗图**
   - 显示订单从创建→审核→设计→生产→质检→入库→出库→完成的转化情况
   - 识别流失最严重的环节
   - SQL: 需要关联 order_flow_status_history 表统计各阶段数量

2. **各阶段平均耗时对比（柱状图）**
   - X轴：阶段名称（设计、生产、质检等）
   - Y轴：平均耗时（天）
   - 用于识别瓶颈阶段
   - SQL: `SELECT phase_name, AVG(phase_duration) FROM order_phase_duration GROUP BY phase_name`

3. **订单趋势分析（折线图）**
   - X轴：时间（按周/月聚合）
   - Y轴：订单数量
   - 多条线：新建订单、完成订单、取消订单
   - SQL: `SELECT DATE(create_time), COUNT(*), phase FROM order_main GROUP BY DATE(create_time), phase`

4. **部门业绩矩阵（气泡图）**
   - X轴：订单数量
   - Y轴：完成率
   - 气泡大小：订单金额
   - 用于识别高产高效部门
   - SQL: `SELECT dept_name, COUNT(*), AVG(CASE WHEN phase=80 THEN 1 ELSE 0 END), SUM(total_amount) FROM order_main GROUP BY operator_dept_name`

**快捷操作区**：

1. **关键指标监控**
   - 今日新增订单数
   - 今日完成订单数
   - 当前异常订单数（红色预警）
   - 各阶段待处理任务数

2. **快捷按钮**
   - "综合报表"按钮
   - "异常订单"按钮
   - "团队管理"按钮

---

### 6.3 超级管理员角色

**角色特点**：
- 数据范围：全系统所有数据（data_scope_type='all'）
- 关注重点：系统运营、数据分析、决策支持
- 核心KPI：与公司管理员类似，但增加系统级指标

**数据卡片区（6个核心指标）**：

与公司管理员相同的6个核心指标，额外增加：

7. **系统活跃用户数**
   - 数值：今日/本周活跃用户数
   - 趋势：与上周对比
   - SQL: `SELECT COUNT(DISTINCT user_id) FROM sys_operation_log WHERE DATE(create_time) = CURDATE()`

8. **数据增长情况**
   - 数值：本月新增订单数、新增客户数、新增医院数
   - 环比增长率
   - SQL: 多表统计

**图表区域（4个图表）**：

与公司管理员相同的4个图表，额外增加：

5. **用户活跃度分析（热力图）**
   - X轴：时间（小时）
   - Y轴：星期
   - 颜色深度：活跃用户数
   - 用于识别系统使用高峰期
   - SQL: `SELECT HOUR(create_time), DAYOFWEEK(create_time), COUNT(DISTINCT user_id) FROM sys_operation_log GROUP BY HOUR(create_time), DAYOFWEEK(create_time)`

**快捷操作区**：

1. **系统监控面板**
   - 系统健康状态（绿色/黄色/红色）
   - 今日API调用量
   - 数据库连接数
   - 缓存命中率

2. **快捷按钮**
   - "系统配置"按钮
   - "用户管理"按钮
   - "数据导出"按钮
   - "系统日志"按钮

---

## 七、通用模板设计（新增角色适配）

### 7.1 设计思路

对于未来新增的角色，系统提供一套通用模板，根据角色的 `data_scope_type` 自动适配数据范围，无需为每个新角色单独开发看板。

### 7.2 通用模板结构

**数据卡片区（4个通用指标）**：

1. **我的订单总数**
   - 根据 data_scope_type 自动过滤数据范围
   - SQL: `SELECT COUNT(*) FROM order_main WHERE {data_scope_condition} AND create_time BETWEEN ? AND ?`

2. **进行中订单**
   - 显示未完成的订单数
   - SQL: `SELECT COUNT(*) FROM order_main WHERE {data_scope_condition} AND phase < 80`

3. **已完成订单**
   - 显示已完成的订单数
   - SQL: `SELECT COUNT(*) FROM order_main WHERE {data_scope_condition} AND phase = 80`

4. **本周新增订单**
   - 显示本周新增的订单数
   - SQL: `SELECT COUNT(*) FROM order_main WHERE {data_scope_condition} AND WEEK(create_time) = WEEK(NOW())`

**图表区域（2个通用图表）**：

1. **订单趋势图（折线图）**
   - X轴：时间（按日聚合）
   - Y轴：订单数量
   - 单条线：新增订单数
   - SQL: `SELECT DATE(create_time), COUNT(*) FROM order_main WHERE {data_scope_condition} GROUP BY DATE(create_time)`

2. **订单阶段分布（饼图）**
   - 显示各阶段订单占比
   - SQL: `SELECT phase_name, COUNT(*) FROM order_main WHERE {data_scope_condition} GROUP BY phase`

**快捷操作区**：

1. **待办事项列表**
   - 显示与该角色相关的待处理订单（前5条）
   - 根据角色权限动态显示

2. **快捷按钮**
   - 根据角色权限动态生成操作按钮

### 7.3 数据范围条件映射

根据角色的 `data_scope_type` 自动生成 SQL WHERE 条件：

| data_scope_type | SQL WHERE 条件 |
|-----------------|----------------|
| self | `operator_id = 当前用户ID` |
| hospitals | `hospital_id IN (SELECT hospital_id FROM sys_user_hospital WHERE user_id = 当前用户ID)` |
| dept | `operator_dept_id = 当前用户部门ID` |
| org | `org_id = 当前用户机构ID` |
| all | 无限制（或 `1=1`） |

### 7.4 实现逻辑

```java
// 伪代码示例
public DashboardVO getGenericDashboard(Long userId) {
    // 1. 获取用户角色
    SysRole role = roleService.getRoleByUserId(userId);
    
    // 2. 根据 data_scope_type 构建数据范围条件
    String dataScopeCondition = buildDataScopeCondition(role.getDataScopeType(), userId);
    
    // 3. 查询通用指标数据
    DashboardVO dashboard = new DashboardVO();
    dashboard.setTotalOrders(orderMapper.countByCondition(dataScopeCondition));
    dashboard.setInProgressOrders(orderMapper.countInProgress(dataScopeCondition));
    dashboard.setCompletedOrders(orderMapper.countCompleted(dataScopeCondition));
    
    // 4. 查询图表数据
    dashboard.setTrendData(orderMapper.getTrendData(dataScopeCondition));
    dashboard.setPhaseDistribution(orderMapper.getPhaseDistribution(dataScopeCondition));
    
    return dashboard;
}
```

---

## 八、技术实现建议

### 8.1 前端架构

**组件设计**：

1. **DashboardLayout 组件**（布局容器）
   - 顶部时间筛选器
   - 自动刷新控制
   - 响应式布局（支持PC/平板/手机）

2. **DataCard 组件**（数据卡片）
   - 支持数值显示、环比变化、趋势图标
   - 点击跳转功能
   - 加载骨架屏

3. **ChartContainer 组件**（图表容器）
   - 封装 ECharts/AntV G2
   - 支持多种图表类型（折线图、柱状图、饼图、漏斗图等）
   - 图表内时间切换

4. **TodoList 组件**（待办列表）
   - 支持优先级排序
   - 点击跳转到详情页

**状态管理**：

- 使用 Vuex/Pinia 管理全局状态
- 缓存用户选择的时间范围
- 缓存图表数据（避免重复请求）

### 8.2 后端API设计

**接口规范**：

```
GET /api/dashboard/{roleCode}
```

**请求参数**：

```json
{
  "timeRange": "today|week|month|quarter|year|custom",
  "startTime": "2026-05-01 00:00:00",
  "endTime": "2026-05-18 23:59:59"
}
```

**响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "cards": [
      {
        "key": "totalOrders",
        "title": "订单总数",
        "value": 1234,
        "change": "+12.5%",
        "trend": "up",
        "link": "/orders"
      }
    ],
    "charts": [
      {
        "key": "orderTrend",
        "title": "订单趋势图",
        "type": "line",
        "data": {...}
      }
    ],
    "todos": [
      {
        "id": 1,
        "title": "待审核订单",
        "count": 5,
        "link": "/orders/pending"
      }
    ]
  }
}
```

**接口实现**：

1. **DashboardController**：接收请求，调用对应角色的 Service
2. **DashboardService**：根据角色代码路由到具体实现
   - `SalesmanDashboardService`
   - `DesignerDashboardService`
   - `GenericDashboardService`（通用模板）
3. **DashboardMapper**：执行SQL查询，返回原始数据
4. **DashboardConverter**：将原始数据转换为前端所需格式

### 8.3 缓存策略

**Redis缓存设计**：

1. **缓存键规则**：
   ```
   dashboard:{roleCode}:{userId}:{timeRange}:{startTime}:{endTime}
   ```

2. **缓存时长**：
   - 实时数据（今日）：1分钟
   - 历史数据（本周/本月）：5分钟
   - 长期数据（本季度/本年）：30分钟

3. **缓存更新策略**：
   - 定时刷新：每5分钟自动刷新缓存
   - 主动失效：订单状态变更时清除相关缓存
   - 手动刷新：用户点击刷新按钮时清除缓存

**缓存实现示例**：

```java
@Service
public class DashboardServiceImpl implements DashboardService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public DashboardVO getDashboard(String roleCode, Long userId, DashboardQueryDTO query) {
        // 1. 构建缓存键
        String cacheKey = buildCacheKey(roleCode, userId, query);
        
        // 2. 尝试从缓存获取
        DashboardVO cached = (DashboardVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 3. 查询数据库
        DashboardVO dashboard = queryDashboardData(roleCode, userId, query);
        
        // 4. 写入缓存
        long ttl = calculateTTL(query.getTimeRange());
        redisTemplate.opsForValue().set(cacheKey, dashboard, ttl, TimeUnit.SECONDS);
        
        return dashboard;
    }
}
```

### 8.4 性能优化

**数据库优化**：

1. **索引优化**：
   - `order_main` 表必须建立的索引：
     - `idx_operator_id_create_time`（业务员查询）
     - `idx_designer_id_design_start_time`（设计师查询）
     - `idx_production_worker_id`（生产员查询）
     - `idx_phase_status`（阶段状态查询）
     - `idx_create_time`（时间范围查询）

2. **查询优化**：
   - 避免 SELECT *，只查询需要的字段
   - 使用覆盖索引减少回表
   - 大数据量查询使用分页

3. **统计表设计**（可选）：
   - 创建 `dashboard_statistics` 表，定时（每小时）预计算统计数据
   - 适用于历史数据查询，减轻实时查询压力

**前端优化**：

1. **懒加载**：图表区域使用懒加载，滚动到可视区域时才加载
2. **骨架屏**：数据加载时显示骨架屏，提升用户体验
3. **防抖节流**：时间筛选器变化时使用防抖，避免频繁请求

### 8.5 权限控制

**数据权限**：

- 所有查询必须基于用户的 `data_scope_type` 进行数据过滤
- 使用 MyBatis-Plus 的数据权限插件自动注入 WHERE 条件
- 敏感数据（金额、客户信息）根据角色权限脱敏

**功能权限**：

- 快捷操作按钮根据用户权限动态显示
- 使用 `@RequirePermission` 注解控制接口访问权限

---

## 九、总结与下一步

### 9.1 设计总结

本设计方案为医工宝系统的11个核心角色提供了个性化的首页数据看板，主要特点：

1. **角色定制化**：每个角色看到最相关的数据和操作入口，提升工作效率
2. **数据权限适配**：基于 `data_scope_type` 自动过滤数据范围，确保数据安全
3. **混合可视化**：核心指标用数字卡片，趋势数据用图表，直观易懂
4. **实时更新**：每5分钟自动刷新，保证数据时效性
5. **灵活筛选**：支持多种时间维度筛选，满足不同分析需求
6. **可扩展性**：核心角色深度定制 + 通用模板，支持未来新增角色

### 9.2 核心价值

**对业务线**：
- 业务员快速了解自己的订单进展，及时处理待办事项
- 区域管理员掌握团队业绩，识别问题订单和高产业务员

**对设计线**：
- 设计师清晰看到待处理工单，合理安排工作优先级
- 设计管理员监控团队负载，优化资源分配，提升审核通过率

**对生产线**：
- 生产员、质管、库管各司其职，快速处理本阶段任务
- 生产管理员全局把控生产进度，识别瓶颈环节

**对管理层**：
- 财务实时掌握收款情况，降低坏账风险
- 公司管理员和超级管理员全流程监控，数据驱动决策

### 9.3 实施计划

**第一阶段（2周）**：
1. 完成数据库索引优化
2. 实现后端API（优先实现业务员、设计师、生产员3个角色）
3. 搭建前端组件库（DataCard、ChartContainer、TodoList）

**第二阶段（2周）**：
1. 完成剩余8个核心角色的后端实现
2. 实现前端页面和路由
3. 集成缓存机制

**第三阶段（1周）**：
1. 实现通用模板（支持新增角色）
2. 性能测试和优化
3. 用户验收测试

**第四阶段（1周）**：
1. 修复测试问题
2. 编写使用文档
3. 上线部署

### 9.4 风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 数据量大导致查询慢 | 用户体验差 | 1. 优化索引 2. 使用缓存 3. 考虑统计表 |
| 角色权限复杂 | 数据泄露风险 | 1. 严格测试数据权限 2. 使用数据权限插件 |
| 图表渲染慢 | 页面卡顿 | 1. 懒加载 2. 数据分页 3. 使用Web Worker |
| 需求变更频繁 | 开发周期延长 | 1. 核心功能优先 2. 预留扩展接口 |

### 9.5 后续优化方向

1. **个性化配置**：允许用户自定义看板布局和显示指标
2. **智能预警**：基于历史数据，智能预测异常订单并提前预警
3. **移动端适配**：优化移动端显示，支持随时随地查看数据
4. **数据导出**：支持将看板数据导出为Excel/PDF报表
5. **AI分析**：引入AI分析，提供业务洞察和优化建议

---

**文档完成日期**：2026-05-18  
**文档状态**：待评审  
**下一步**：提交产品评审，收集反馈后进入开发阶段

