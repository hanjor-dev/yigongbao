# 移除设计审核流程 - 设计文档

**文档日期**：2026-06-03  
**状态**：设计阶段  
**作者**：Claude

## 1. 背景与目标

### 1.1 当前问题

当前系统中设计流程固定为：
```
设计中 → 提交设计审核 → 审核中 → 审核通过 → 进入生产
```

客户需求发生变化，不再需要设计审核环节。

### 1.2 目标

1. **删除审核功能**：完全移除审核相关代码
2. **简化流程**：设计师完成设计 → 直接进入下一阶段
3. **彻底清理**：删除审核相关的Controller、Service、DTO、状态、动作

## 2. 核心需求

### 2.1 流程需求

**新的设计流程：**
```
设计中(2020)
  → 完成设计
  → 设计完成(2030)
  → 分支：
     - 需要实体交付：创建流转卡 → 生产员下载数据包 → 待打印(3010)
     - 不需要实体交付：管理员手动完成 → 订单完成(8010)
```

### 2.2 校验规则

**完成设计时的校验：**

| 订单类型 | 校验项 |
|---------|--------|
| 需要实体交付 | • 必须有数据包<br>• 必须有打印信息<br>• 必须有指令单<br>• 必须有图纸<br>• 图纸必须已确认<br>• 指令单必须已确认 |
| 不需要实体交付 | • 只需要有STL重建模型 |

**手动完成订单的前置条件：**
- 订单状态为"设计完成"（2030）
- 订单不需要实体交付（needsPhysicalDelivery = 0）

## 3. 技术实现

### 3.1 状态与动作定义

**FlowStatusEnum 使用现有状态：**
- `DESIGN_IN_PROGRESS(2020)` - 设计中
- `DESIGN_COMPLETED(2030)` - 设计完成 ✅ **复用现有状态**

**FlowStatusEnum 删除状态（不再使用）：**
- ~~`DESIGN_REVIEWING(2040)`~~ - 删除
- ~~`DESIGN_REVIEW_PASSED(2050)`~~ - 删除
- ~~`DESIGN_REVIEW_REJECTED(2060)`~~ - 删除

**关键修改：** 修改FlowStatusTransitionRules，允许2030停留或直接跨阶段流转

**FlowStatusTransitionRules.java 修改（第69-70行）：**

当前代码：
```java
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_COMPLETED),
        Set.of(FlowStatusEnum.DESIGN_REVIEWING));
```

修改为：
```java
// DESIGN_COMPLETED(2030) 可以直接跨阶段流转：
// - 下载数据包（生产员操作）→ 跨阶段流转到 PENDING_PRINT(3010)
// - 手动完成（管理员操作，仅不需要实体交付）→ 跨阶段流转到 ORDER_COMPLETED(8010)
transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_COMPLETED),
        Set.of(FlowStatusEnum.PENDING_PRINT, FlowStatusEnum.ORDER_COMPLETED));
```

**说明：**
- 删除到DESIGN_REVIEWING的流转
- 新增到PENDING_PRINT的流转（生产员下载数据包触发）
- 新增到ORDER_COMPLETED的流转（管理员手动完成订单）

**FlowActionEnum 新增：**
```java
/**
 * 完成设计
 */
COMPLETE_DESIGN("COMPLETE_DESIGN", "完成设计")
```

**FlowActionEnum 删除（审核相关）：**
- ~~`SUBMIT_DESIGN`~~ - 删除（原"提交设计审核"）
- ~~`DESIGN_REVIEW_PASS`~~ - 删除
- ~~`DESIGN_REVIEW_REJECT`~~ - 删除
- `CONTINUE_DESIGN` - 保留（审核驳回后继续修改，未来可能还有其他用途）

### 3.2 接口变更

**DesignWorkorderController 新增：**

| 接口 | 说明 | 权限 |
|-----|------|------|
| `POST /{orderId}/complete-design` | 完成设计 | design:CompleteDesign |

**DesignWorkorderController 删除：**

| 接口 | 说明 |
|-----|------|
| ~~`POST /{orderId}/submit-design`~~ | 删除（原"提交设计审核"） |

**DesignReviewController 完整删除：**

整个 Controller 及其所有接口全部删除：
- ~~`POST /design/review/list`~~ - 删除
- ~~`GET /design/review/{orderId}`~~ - 删除
- ~~`POST /design/review/{orderId}/pass`~~ - 删除
- ~~`POST /design/review/{orderId}/reject`~~ - 删除

**OrderController 新增：**

| 接口 | 说明 | 权限 |
|-----|------|------|
| `POST /{orderId}/manual-complete` | 手动完成订单（不需要实体交付） | order:ManualComplete |

### 3.3 Service 层核心方法

**DesignWorkorderService 新增：**

```java
/**
 * 完成设计
 * 根据 needsPhysicalDelivery 执行不同的校验和处理
 */
void completeDesign(Long orderId);
```

**DesignWorkorderService 删除：**

```java
// 删除 submitDesign 方法（原"提交设计审核"）
void submitDesign(Long orderId, SubmitDesignDTO dto);
```

**DesignReviewService 完整删除：**

整个 Service 接口及其实现类全部删除，包括：
- `IPage<DesignWorkorderListVO> listReviewWorkorders(DesignWorkorderQueryDTO queryDTO)`
- `DesignReviewDetailVO getReviewDetail(Long orderId)`
- `void reviewPass(Long orderId, ReviewPassDTO dto)`
- `void reviewReject(Long orderId, ReviewRejectDTO dto)`

**OrderService 新增：**

```java
/**
 * 手动完成订单（仅限不需要实体交付的订单）
 */
void manualCompleteOrder(Long orderId);
```

### 3.4 生产模块变更

**事件监听器调整：**

| 变更类型 | 说明 |
|---------|------|
| 新增监听器 | `onDesignCompleted`：监听设计完成事件，创建流转卡 |
| 删除监听器 | ~~`onDesignReviewPassed`~~：删除审核通过事件监听 |

**数据包下载逻辑调整：**
- 允许状态：从"审核通过后的状态"改为"设计完成（2030）"
- 下载触发：下载数据包后自动流转到"待打印（3010）"

### 3.5 核心流程图

**需要实体交付的订单：**
```
设计中(2020) 
  → 完成设计 
  → 设计完成(2030) 
  → 创建流转卡 
  → 生产员下载数据包 
  → 待打印(3010)
```

**不需要实体交付的订单：**
```
设计中(2020) 
  → 完成设计 
  → 设计完成(2030) 
  → 管理员手动完成 
  → 订单完成(8010)
```

## 4. 影响范围

### 4.1 代码变更

| 模块 | 变更类型 | 文件数 |
|-----|---------|--------|
| 公共模块 | 删除枚举值 | 2 |
| 设计模块 | 删除Controller/Service/DTO，新增接口方法 | ~8 |
| 订单模块 | 新增接口、方法 | 2 |
| 生产模块 | 新增监听器、修改下载逻辑、删除监听器 | 2 |

### 4.2 数据库变更

| 变更类型 | 说明 |
|---------|------|
| 无需变更 | 审核相关表（design_review）保留但不再使用 |
| 状态枚举 | 代码层面删除审核相关状态 |

### 4.3 前端变更

| 变更类型 | 说明 |
|---------|------|
| 删除按钮 | 设计工单详情页删除"提交审核"按钮 |
| 新增按钮 | 设计工单详情页新增"完成设计"按钮 |
| 新增按钮 | 订单管理页新增"手动完成"按钮（仅不需要实体交付） |
| 删除页面 | 删除设计审核列表页面 |
| 删除页面 | 删除设计审核详情页面 |
| 删除按钮 | 删除审核通过/驳回按钮 |

## 5. 实施步骤

### 5.1 后端实现（优先级：P0）

1. **枚举定义修改**
   - FlowStatusEnum 删除审核相关状态（2040/2050/2060）
   - FlowActionEnum 新增 COMPLETE_DESIGN 动作
   - FlowActionEnum 删除审核相关动作（SUBMIT_DESIGN/DESIGN_REVIEW_PASS/DESIGN_REVIEW_REJECT）
   - 修改 FlowStatusTransitionRules 的转换规则

2. **删除审核模块**
   - 删除 DesignReviewController
   - 删除 DesignReviewService 及其实现类
   - 删除审核相关 DTO（ReviewPassDTO/ReviewRejectDTO）
   - 删除审核相关 VO（DesignReviewDetailVO/DesignReviewHistoryVO）

3. **设计模块接口开发**
   - DesignWorkorderController 新增 completeDesign 接口
   - DesignWorkorderController 删除 submitDesign 接口
   - DesignWorkorderService 实现 completeDesign 方法
   - DesignWorkorderService 删除 submitDesign 方法
   - 修改校验逻辑：根据 needsPhysicalDelivery 执行不同校验

4. **订单模块接口开发**
   - OrderController 新增 manualCompleteOrder 接口
   - OrderService 实现 manualCompleteOrder 方法

5. **生产模块调整**
   - 新增 DesignCompletedEvent 事件
   - 新增 onDesignCompleted 监听器
   - 删除 onDesignReviewPassed 监听器
   - 修改数据包下载逻辑的状态校验（允许 2030 状态下载）

6. **单元测试**
   - completeDesign 方法测试（两种订单类型）
   - manualCompleteOrder 方法测试
   - 校验逻辑测试
   - 删除审核相关测试类

### 5.2 前端实现（优先级：P0）

1. **设计工单详情页**
   - 删除"提交审核"按钮
   - 新增"完成设计"按钮
   
2. **设计审核模块**
   - 删除设计审核列表页面
   - 删除设计审核详情页面
   - 删除审核通过/驳回按钮
   - 删除相关路由配置

3. **订单管理页**
   - 新增"手动完成"按钮（仅对不需要实体交付的订单显示）

## 6. 风险评估

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 删除代码导致引用错误 | 编译失败、运行时异常 | 全局搜索引用点，确保完全删除 |
| 状态流转错误 | 订单卡在中间状态 | 完善状态校验和错误提示，充分测试 |
| 生产流转卡创建失败 | 实体交付订单无法进入生产 | 添加事务保护和异常处理 |
| 历史审核数据查询失败 | 前端页面报错 | 虽然删除审核功能，但保留审核表和历史记录 |

## 7. 总结

本设计方案通过删除设计审核功能，简化设计流程为：设计中 → 完成设计 → 设计完成 → 进入生产/手动完成。核心改动包括：
1. **删除审核模块**：完全移除审核相关的 Controller、Service、DTO、VO
2. **删除审核状态和动作**：移除 FlowStatusEnum 和 FlowActionEnum 中的审核相关枚举
3. **新增完成设计功能**：新增 COMPLETE_DESIGN 动作和 completeDesign 接口
4. **调整生产流程**：流转卡创建从审核通过事件改为设计完成事件
5. **支持两种订单类型**：根据 needsPhysicalDelivery 执行不同校验和处理

**预估工作量：2-3天**

---

**审批：** □ 产品经理  □ 技术负责人  □ 项目经理
