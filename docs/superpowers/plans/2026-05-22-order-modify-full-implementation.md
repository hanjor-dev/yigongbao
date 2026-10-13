# 订单修改功能重构 - 实施计划

## 计划信息

| 项目 | 内容 |
|------|------|
| 计划标题 | 订单修改功能重构 - 全量Diff方案实施 |
| 创建日期 | 2026-05-22 |
| 设计文档 | [2026-05-22-order-modify-full-refactor.md](../specs/2026-05-22-order-modify-full-refactor.md) |
| 预计工时 | 10.5小时 |
| 状态 | 待执行 |

---

## 任务列表

### 阶段1：基础准备（1h）

#### 任务1：创建DTO类（0.5h）

**目标**：创建请求DTO、配置DTO和变更记录类

**文件**：
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/modify/OrderModifyFullDTO.java`
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/modify/ModifyFullConfigDTO.java`
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/dto/modify/ObjectChange.java`

**实现要点**：
- OrderModifyFullDTO：包含患者、医生、医院、交付、项目、影像字段
- ModifyFullConfigDTO：配置结构，支持按阶段配置允许的对象
- ObjectChange：记录对象变更（对象类型、标签、旧值、新值）

#### 任务2：实现配置加载（0.5h）

**目标**：从sys_config加载配置，解析JSON

**文件**：
- 修改 `ModifyFullConfigDTO.java`（添加解析方法）
- 在 `OrderModifyFullServiceImpl` 中实现配置加载

**实现要点**：
- 使用 Jackson ObjectMapper 解析JSON
- 提供默认配置兜底
- 配置key：`order.modify.full.config`

#### 任务3：添加配置数据（0h）

**目标**：在sys_config表中添加配置数据

**SQL**：
```sql
INSERT INTO sys_config (config_key, config_value, config_desc, create_time, update_time)
VALUES ('order.modify.full.config', '{配置JSON}', '订单全量修改配置', NOW(), NOW());
```

---

### 阶段2：核心功能（5h）

#### 任务4：实现简单对象Diff（1h）

**目标**：实现patient、doctor、hospital、delivery的diff算法

**文件**：
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyFullServiceImpl.java`

**实现要点**：
- 提取字段值（按配置的fields列表）
- 对比新旧值
- 生成格式化描述

**方法**：
- `diffPatient()`
- `diffDoctor()`
- `diffHospital()`
- `diffDelivery()`

#### 任务5：实现重建项目Diff（2h）

**目标**：实现items的详细diff算法

**实现要点**：
- 按orderItemId匹配新旧项目
- 分别对比核心字段和描述字段
- 生成详细变更描述（包含具体字段变化）

**方法**：
- `diffItems()`
- `diffCoreFields()`
- `diffDescFields()`
- `formatValue()`

#### 任务6：实现影像文件Diff（0.5h）

**目标**：实现images的diff算法

**实现要点**：
- 查询当前文件列表
- 对比新旧文件ID列表
- 生成变更描述

**方法**：
- `diffImages()`

#### 任务7：实现变更应用逻辑（1.5h）

**目标**：将变更写入数据库

**实现要点**：
- 应用简单对象变更（更新order表）
- 应用重建项目变更（增删改order_item表）
- 应用影像文件变更（增删改order_file表）
- 调用OrderDataValidator校验数据

**方法**：
- `applyChanges()`
- `applySimpleObjectChange()`
- `applyItemsChange()`
- `applyImagesChange()`

---

### 阶段3：接口层（0.5h）

#### 任务8：实现Service主方法（0.5h）

**目标**：实现完整的修改流程

**文件**：
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/OrderModifyFullService.java`（接口）
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderModifyFullServiceImpl.java`（实现）

**实现要点**：
- 查询当前订单
- 按对象diff
- 过滤无变化
- 校验变更
- 应用变更
- 记录日志
- 递增版本号
- 触发流转

**方法**：
- `modifyOrderFull(Long orderId, OrderModifyFullDTO dto)`

#### 任务9：实现Controller（0.5h）

**目标**：新增接口方法

**文件**：
- `yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderModifyApplyController.java`

**实现要点**：
- 新增方法：`modifyOrderFull()`
- 路径：`PUT /order/modify/{orderId}/full`
- 权限：`@RequirePermission("order:Modify")`

---

### 阶段4：测试（3h）

#### 任务10：编写单元测试（2h）

**目标**：覆盖主要场景

**文件**：
- `yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderModifyFullServiceImplTest.java`

**测试场景**：
1. 修改患者信息
2. 修改医生信息
3. 修改交付信息
4. 修改重建项目（核心字段）
5. 修改重建项目（描述字段）
6. 新增重建项目
7. 删除重建项目
8. 混合操作
9. 修改影像文件
10. 异常场景（订单不存在、阶段不允许、对象不允许）

#### 任务11：编写集成测试（1h）

**目标**：完整流程测试

**文件**：
- `yigongbao-module-order/src/test/java/com/yigongbao/module/order/controller/OrderModifyApplyControllerTest.java`

**测试场景**：
1. 完整流程（订单阶段修改 → 审核通过）
2. 完整流程（设计阶段修改 → 重新审核）

---

### 阶段5：文档（1h）

#### 任务12：更新接口文档（1h）

**目标**：补充新接口说明

**文件**：
- `.docs/接口文档/19_订单模块接口文档.md`

**内容**：
- 新增接口：19.22 全量修改订单
- 请求参数说明
- 响应示例
- 错误码说明

---

## 实施顺序

1. 任务1 → 任务2 → 任务3（基础准备）
2. 任务4 → 任务5 → 任务6（Diff算法）
3. 任务7（变更应用）
4. 任务8 → 任务9（接口层）
5. 任务10 → 任务11（测试）
6. 任务12（文档）

---

## 关键文件清单

**新增文件**：
- `OrderModifyFullDTO.java`
- `ModifyFullConfigDTO.java`
- `ObjectChange.java`
- `OrderModifyFullService.java`
- `OrderModifyFullServiceImpl.java`
- `OrderModifyFullServiceImplTest.java`

**修改文件**：
- `OrderModifyApplyController.java`（新增方法）
- `19_订单模块接口文档.md`（新增章节）

---

## 验收标准

1. ✅ 所有单元测试通过
2. ✅ 所有集成测试通过
3. ✅ 接口文档更新完整
4. ✅ 代码符合编码规范
5. ✅ 日志记录符合预期格式
6. ✅ 配置可正常加载和解析

---

**计划状态**：待执行

**下一步**：开始执行任务1
