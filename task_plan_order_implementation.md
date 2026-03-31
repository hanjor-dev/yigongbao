# Task Plan: 医工宝订单模块 - 第一阶段实施

> 文档状态：**执行规划阶段 - 详细实施清单已生成**
> 版本：v3.0
> 创建日期：2026-03-31
> 作者：hanjor
> 目标分支：feature/yigongbao-module-order

## Goal

基于已确认的订单状态机设计方案，实施第一阶段核心功能：创建 `yigongbao-module-order` 模块骨架、实现订单阶段（ORDER Phase）的完整 CRUD + 状态机能力 + 草稿管理，为后续设计/生产等阶段打好基础。

## 当前阶段

Phase 2: Planning & Structure — **执行规划阶段**

## 实施清单（严格按依赖顺序）

---

### 阶段 0：基础设施（枚举、骨架、常量）

> 先创建枚举和常量（放在 common，供所有模块复用），再创建模块骨架。

#### 0.1 common 层：枚举（5个文件）

- [ ] `common/enums/order/OrderTypeEnum.java` — 订单类型（1-医疗器械，2-非医疗器械，3-服务）
- [ ] `common/enums/order/OrderPhaseEnum.java` — 阶段枚举（ORDER=1, DESIGN=2, PRODUCTION=3）
- [ ] `common/enums/order/OrderStatusEnum.java` — 订单阶段状态枚举（PENDING/SUBMITTED/CONFIRMED/PROCESSING/COMPLETED/CANCELLED）
- [ ] `common/enums/order/OrderActionEnum.java` — 动作枚举（CREATE/SUBMIT/WITHDRAW/AUDIT_PASS/AUDIT_REJECT/RESUBMIT）
- [ ] `common/enums/order/OrderBusinessTypeEnum.java` — 业务类型（dictCode: 11.1/11.2/11.3/11.4）
- [ ] `common/enums/order/OrderPatientGenderEnum.java` — 患者性别（dictCode: 12.1/12.2）

#### 0.2 common 层：状态转换规则

- [ ] `common/rules/PhaseTransitionRule.java` — 阶段转换规则接口
- [ ] `common/rules/OrderPhaseTransitionRules.java` — 阶段转换规则实现（ORDER→DESIGN→PRODUCTION）
- [ ] `common/rules/OrderStatusTransitionRules.java` — 状态转换规则（订单阶段内部状态转换）

#### 0.3 common 层：扩展现有常量

- [ ] 扩展 `ErrorCodeEnum.java` — 添加订单相关错误码 675-701
- [ ] 扩展 `DictCodeConstants.java` — 添加 ORDER_BUSINESS_TYPE(11), PATIENT_GENDER(12)
- [ ] 扩展 `SystemConfigKeyEnum.java` — 添加 order.image.required / order.draft.expire.days / order.modify.window.minutes
  > 注意：CodeRuleConstants 中 ORDER_NO / ORDER_ITEM_NO 已存在，无需重复添加

#### 0.4 模块骨架

- [ ] 创建 `yigongbao-module-order/pom.xml` 并注册到父 pom
- [ ] 在 `yigongbao-boot/pom.xml` 中添加 `yigongbao-module-order` 依赖
- [ ] 创建 `yigongbao-module-order` 包结构（controller/service/mapper/entity/dto/vo/enums/rules/convert/task）

> **common 层枚举说明**：
> 方案文档 2.3 节规定：订单相关枚举（OrderPhaseEnum、OrderStatusEnum、OrderActionEnum、OrderBusinessTypeEnum、OrderPatientGenderEnum）放在 `yigongbao-common` 的 `enums/order/` 目录下，供未来 design/production 模块复用。
> OrderTypeEnum 也放在 common，因为各阶段都需引用。

---

### 阶段 1：草稿管理（Entity + Mapper + Service + Controller）

> 草稿独立表：order_draft + order_item_draft

#### 1.1 草稿 Entity

- [ ] `OrderDraftEntity.java` — 订单草稿表（order_draft）
- [ ] `OrderItemDraftEntity.java` — 草稿明细表（order_item_draft）

#### 1.2 草稿 Mapper

- [ ] `OrderDraftMapper.java`
- [ ] `OrderItemDraftMapper.java`

#### 1.3 草稿 DTO

- [ ] `dto/draft/CreateOrderDraftDTO.java` — 嵌套重建项目列表（批量保存）
- [ ] `dto/draft/UpdateOrderDraftDTO.java`

#### 1.4 草稿 VO

- [ ] `vo/draft/OrderDraftVO.java`
- [ ] `vo/draft/OrderDraftDetailVO.java`

#### 1.5 草稿 Convert

- [ ] `convert/OrderDraftConvert.java`

#### 1.6 草稿 Service

- [ ] `OrderDraftService.java` — 接口
- [ ] `OrderDraftServiceImpl.java` — 实现（含文件校验：调用 FileService.listByBiz 校验 10.1/10.2）

#### 1.7 草稿 Controller

- [ ] `OrderDraftController.java`（创建/列表/详情/更新/删除/提交）

> **草稿提交流程关键点**：
> 1. 校验必填字段（org_id, hospital_id, patient_name, business_type 等）
> 2. 校验 order_item_draft 至少 1 条
> 3. 校验文件（通过 FileService.listByBiz 查 file_detail）：bizType=order_draft, bizId=draftId
> 4. 调用 CodeGeneratorService 生成 ORDER_NO
> 5. 开启事务：创建 order_main、order_item、order_file；更新草稿状态；记录状态历史
> 6. 返回 orderId 和 orderCode

---

### 阶段 2：订单管理（Entity + Mapper + Service + Controller）

> 正式订单：order_main + order_item + order_file

#### 2.1 订单 Entity

- [ ] `OrderMainEntity.java` — 订单主表（order_main）
- [ ] `OrderItemEntity.java` — 订单明细表（order_item）
- [ ] `OrderFileEntity.java` — 订单文件关联表（order_file）

#### 2.2 订单 Mapper

- [ ] `OrderMainMapper.java`
- [ ] `OrderItemMapper.java`
- [ ] `OrderFileMapper.java`

#### 2.3 订单 DTO

- [ ] `dto/order/CreateOrderDTO.java`
- [ ] `dto/order/UpdateOrderDTO.java`
- [ ] `dto/order/AuditOrderDTO.java`

#### 2.4 订单 VO

- [ ] `vo/order/OrderMainVO.java`
- [ ] `vo/order/OrderDetailVO.java`
- [ ] `vo/order/OrderListVO.java`

#### 2.5 订单 Convert

- [ ] `convert/OrderMainConvert.java`
- [ ] `convert/OrderItemConvert.java`

#### 2.6 订单 Service

- [ ] `OrderMainService.java` — 接口
- [ ] `OrderMainServiceImpl.java` — 实现（含10分钟修改窗口判断、权限校验）

---

### 阶段 3：状态机 + 状态历史

#### 3.1 状态历史 Entity + Mapper + Service

- [ ] `OrderStatusHistoryEntity.java`
- [ ] `OrderStatusHistoryMapper.java`
- [ ] `OrderStatusHistoryService.java`
- [ ] `OrderStatusHistoryServiceImpl.java`

#### 3.2 状态机核心

- [ ] `OrderStateMachineService.java` — 接口（查询可执行动作、执行状态转换）
- [ ] `OrderStateMachineServiceImpl.java` — 实现（调用 OrderStatusTransitionRules 校验转换合法性）

---

### 阶段 4：Controller 层聚合

- [ ] `OrderMainController.java`（列表/详情/更新/删除/状态操作/状态历史/查询可执行动作）

---

### 阶段 5：定时任务

- [ ] `OrderDraftCleanupTask.java` — 草稿过期清理（每天凌晨2点）

---

### 阶段 6：schema.sql 测试数据库脚本

- [ ] `schema.sql` — H2 测试脚本，包含：order_draft / order_item_draft / order_main / order_item / order_file / order_status_history 表结构 + 字典数据 + 系统配置

---

### 阶段 7：单元测试

#### 7.1 ServiceImpl 单元测试

- [ ] `OrderStateMachineServiceImplTest.java`
- [ ] `OrderDraftServiceImplTest.java`
- [ ] `OrderMainServiceImplTest.java`

#### 7.2 Controller 接口测试

- [ ] `OrderDraftControllerTest.java`
- [ ] `OrderMainControllerTest.java`

---

### 阶段 8：代码审查 + 收尾

- [ ] 代码格式自查（遵循 java-coding-standards.mdc）
- [ ] 检查所有 ServiceImpl 方法是否添加方法注释和行级注释
- [ ] 检查所有 ServiceImpl 方法是否添加日志
- [ ] 检查 Controller 层是否禁止输出日志
- [ ] 更新 task_plan.md 记录实施结果

---

## 关键约束与设计决策

| # | 决策 | 说明 |
|---|------|------|
| 1 | 文件通过 FileService 上传 | bizType = "order_draft"（草稿）/ "order_main"（正式订单） |
| 2 | 提交校验调用 FileService | FileService.listByBiz(bizType, bizId) 查 file_detail |
| 3 | 草稿明细嵌套 DTO | 一次请求批量保存草稿 + 所有明细 |
| 4 | 字典编码 dict_code | business_type: 11.1/11.2/11.3/11.4; patient_gender: 12.1/12.2 |
| 5 | 枚举放在 common | OrderPhaseEnum/StatusEnum/ActionEnum 等供 design/production 复用 |
| 6 | ErrorCode 675-701 | ErrorCodeEnum 已定义到 674，新增订单相关错误码 675-701 |
| 7 | ORDER_NO 已存在 | CodeRuleConstants 中已有 ORDER_NO 和 ORDER_ITEM_NO，无需重复定义 |

## 依赖关系图

```
common (枚举/常量/规则接口)
   │
   ├── OrderTypeEnum        ──┐
   ├── OrderPhaseEnum        │
   ├── OrderStatusEnum        │
   ├── OrderActionEnum        ├── order 模块依赖
   ├── OrderBusinessTypeEnum ──┤
   ├── OrderPatientGenderEnum │
   └── PhaseTransitionRule   ──┘
           │
           ▼
   ┌─────────────────────────────┐
   │ yigongbao-module-order      │
   │  ├── Entity + Mapper        │
   │  ├── DTO + VO + Convert     │
   │  ├── Service (草稿/订单/状态机) │
   │  ├── Controller             │
   │  └── Task                   │
   └─────────────────────────────┘
```

## 错误记录

| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
| - | - | - | - |

---

*最后更新：2026-03-31*
