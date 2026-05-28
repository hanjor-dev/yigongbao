# 生产模块异常处理简化计划（最终版）

**日期**：2026-05-28  
**版本**：2.0（最终确认版）

---

## 一、简化原则

**系统只负责正常流程推进，异常由生产员线下处理。**

- 工序阶段：只记录操作（设备、参数、时间），完成后自动推进
- QC 阶段：质检员可标记产品合格/不合格（保留追溯记录），但系统不触发任何自动回退
- 不合格产品由生产员线下处理完成后，质检员重新标记合格

---

## 二、简化后的主流程

```
【医疗器械订单】
设计审核通过 → 自动创建流转卡（2050）+ 产品 + 5个工序记录（print/wash/cure/clean_dry/pack）
    ↓
下载数据包 → 2050 → 3010
    ↓
分配打印机
    ↓
设备 IDLE→BUSY → 3020（打印中）
    ↓
设备 BUSY→IDLE → 3030（打印完成）
    ↓ 聚合触发 COMPLETE_PRINT → 订单进入后处理阶段（流转卡仍是 3030）
    ↓
startProcess(wash) → 流转卡 3030 → 4010，currentProcess=wash
finishProcess(wash) → currentProcess 自动推进到 cure
startProcess(cure) → finishProcess(cure) → currentProcess 推进到 clean_dry
startProcess(clean_dry) → finishProcess(clean_dry) → 4010 → 5010，currentProcess=null
    ↓ 聚合触发 COMPLETE_POST_PROCESSING → 订单进入质检阶段
    ↓
质检员逐产品标记合格（markProductPass）
    [不合格：markProductFail 记录原因，线下处理后重新 markProductPass]
全部合格（无 FAIL/IN_PROCESS 产品）→ transferToPacking → 5050
    ↓ 聚合触发 QC_PASS → 订单进入包装阶段
    ↓
填写包装信息 → 包装完成 → 6010
    ↓ 聚合触发 COMPLETE_WAREHOUSE_IN → 订单完成

【非医疗器械订单】
设计审核通过 → 自动创建流转卡（2050）+ 产品 + 2个工序记录（print/pack）
    ↓
下载数据包 → 2050 → 3010 → 分配打印机 → 打印（设备推送）→ 3030
    ↓ 聚合触发 COMPLETE_PRINT → Flow 规则直接跳转到质检阶段，流转卡 3030 → 5010
    ↓
质检员逐产品标记合格 → transferToPacking → 5050 → 包装 → 6010
```

---

## 三、需要删除的内容

### 3.1 完全删除的接口

| 接口 | 路径 | 原因 |
|------|------|------|
| 打印失败处理 | POST /process/{recordId}/print-failure | 线下处理，系统不介入 |
| 打印检验不合格处理 | POST /process/{recordId}/print-inspection-fail | 同上 |
| 打印首检确认 | POST /record/{id}/confirm-print-inspection | 打印完成后直接可开始后处理 |
| 提交工序质检结果 | POST /process/{id}/submit-qc | 工序完成后直接推进 |
| 标记产品质检不合格（redo） | POST /qc/product/{productId}/redo | 改为 markProductFail，不触发回退 |
| 指定 redo 重做工序 | POST /qc/redo/{productId}/assign | 删除 |
| redo 产品列表 | POST /qc/redo/list | 删除 |

### 3.2 需要删除的文件

| 文件 | 原因 |
|------|------|
| `SubmitProcessQcDTO.java` | submitProcessQc 删除 |
| `ProcessProductResultDTO.java` | 同上 |
| `ProductionProcessProductResultEntity.java` | 工序质检记录表不再使用 |
| `ProductionProcessProductResultMapper.java` | 同上 |
| `QcHandleTypeEnum.java` | redo 处理方式枚举不再需要 |
| `ProductionRedoPageDTO.java` | redo 产品列表删除 |

### 3.3 需要删除的数据库表

| 表 | 原因 |
|------|------|
| `production_process_product_result` | 工序质检记录，不再使用 |

### 3.4 需要删除的字段

| 实体 | 字段 | 原因 |
|------|------|------|
| `ProductionRecordEntity` | `hasRedoProduct` | 不再有 redo 状态，同步清理 ProductionRecordVO 和接口文档 |
| `ProductionProductEntity` | `redoProcessType` | 不再有工序级 redo |

**保留的质检字段**（`qcResult`/`qcRemark`/`qcTime`/`qcUserId`）：继续用于记录质检信息，`qcResult` 值域从 `pass/redo` 改为 `pass/fail`。`QcResultEnum.REDO` 改为 `QcResultEnum.FAIL`。

---

## 四、需要新增的内容

### 4.1 新增 markProductFail 接口

替代原来的 `markProductRedo`，只记录不合格原因，不触发任何回退。

```
POST /qc/product/{productId}/fail
Body: { "reason": "表面气泡" }
```

逻辑：
- 产品状态 IN_PROCESS → FAIL
- 写入 `qcResult=fail`、`qcRemark`、`qcTime`、`qcUserId`
- 流转卡 `unqualifiedCount + 1`（累计不合格标记次数，不随重新合格而减少，前端展示时需说明）
- 不触发任何流转

质检员重新质检时调 `markProductPass`，产品从 FAIL → PASS。

**注意**：FAIL 状态的产品必须先重新标记为 PASS（或 CANCELLED），`transferToPacking` 才能通过校验。

### 4.2 ProductStatusEnum 新增 FAIL 状态

```java
FAIL("fail", "质检不合格")
```

替代原来的 REDO（REDO 语义是"待重做"，FAIL 语义是"不合格，待处理"）。

---

## 五、需要修改的内容

### 5.1 DeviceStatusListener — 恢复自动触发 COMPLETE_PRINT

打印完成后直接触发聚合，不再等待首检确认。

```java
// BUSY→IDLE 时：
records.forEach(record -> {
    record.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
    record.setCurrentProcess(null);
    record.setPrintFinishTime(now);
    recordMapper.updateById(record);
});
recordService.triggerFlowIfAllReach(orderId, PRINT_COMPLETED, COMPLETE_PRINT);
```

### 5.2 finishProcess — 恢复后处理工序自动推进

```java
if (WASH) → currentProcess = cure
if (CURE) → currentProcess = clean_dry
if (CLEAN_DRY) → status = QC_IN_PROGRESS, currentProcess = null
              → triggerFlowIfAllReach(COMPLETE_POST_PROCESSING)
```

### 5.3 startProcess — 恢复只允许 PENDING 状态开始

```java
if (!PENDING.equals(process.getStatus())) {
    throw new BusinessException(400, "工序已开始或已完成，无法重复开始");
}
```

### 5.4 fillProcess — 无需修改

上一轮重构后已是纯参数填写，不含任何状态判断逻辑，确认保持现状即可。

### 5.5 markProductPass — 允许 FAIL 状态的产品重新标记合格

```java
// 允许 IN_PROCESS 或 FAIL 状态的产品标记合格
if (!IN_PROCESS.equals(status) && !FAIL.equals(status)) {
    throw new BusinessException(400, "产品当前状态不允许质检");
}
```

### 5.6 transferToPacking — 校验条件排除 FAIL

```java
// 校验：所有产品都是 PASS 或 CANCELLED（FAIL 的产品不能流转）
long notPassCount = productMapper.selectCount(
    .ne(status, PASS)
    .ne(status, CANCELLED));
if (notPassCount > 0) throw ...
```

---

## 六、实施顺序

**第零步：修复 Flow 规则——非医疗器械订单打印完成后直接进入质检**

问题：`FlowPhaseTransitionRules.decideNextPhaseAndStatus` 的 `COMPLETE_PRINT` 分支固定跳到 `POST_PROCESSING`，非医疗器械订单（无后处理工序）打印完成后流程死锁。另外，Flow 只更新 `order_main`，流转卡状态需要单独回写。

修改文件：
- `FlowPhaseTransitionRules.java`：`decideNextPhaseAndStatus` 增加 `orderType` 参数，`COMPLETE_PRINT` 分支根据 `orderType` 决定跳 POST_PROCESSING（医疗器械）还是 QC（非医疗器械）
- `FlowStateMachineServiceImpl.java`：调用 `decideNextPhaseAndStatus` 时传入 `order.getOrderType()`
- `FlowPhaseTransitionRulesTest.java`：更新相关测试用例

注意：`DeviceStatusListener` 的非医疗器械流转卡状态回写逻辑（3030→5010）与第三步的恢复自动触发合并，在第三步一次性完成。

**第一步：删除工序质检相关代码**
- 删除 `submitProcessQc`（Controller、Service、ServiceImpl）
- 删除 `SubmitProcessQcDTO`、`ProcessProductResultDTO`
- 删除 `ProductionProcessProductResultEntity`、Mapper
- 删除 `ProductionProcessServiceImpl` 中的 `processProductResultMapper` 注入字段及所有引用
- 删除 `ProductionProcessServiceImpl` 中的 `tryAdvanceProcess` 私有方法

**第二步：恢复 finishProcess 自动推进**
- 恢复 wash/cure/clean_dry 的 currentProcess 推进逻辑（推进逻辑直接内联在 finishProcess 中，不需要 tryAdvanceProcess）
- 删除 `finishProcess` 中打印工序的分支（PRINT 工序完成由设备推送处理，不走此方法）
- 注意：非医疗器械订单没有 wash/cure/clean_dry 工序记录，不会调用这些分支，无需加状态判断
- 恢复 startProcess 只允许 PENDING 状态开始

**第三步：删除打印首检和打印失败处理**
- 删除 `confirmPrintInspection`（`ProductionRecordController`、`IProductionRecordService`、`ProductionRecordServiceImpl`）
- 删除 `handlePrintFailure`、`handlePrintInspectionFail`（`ProductionProcessController`、`IProductionProcessService`、`ProductionProcessServiceImpl`）
- 改造 `DeviceStatusListener`：恢复自动触发 `COMPLETE_PRINT`，同时加入非医疗器械逻辑（触发后批量将该订单状态为 3030 的流转卡更新为 5010）；两处改动合并在同一次修改中完成

**第四步：改造 QC 阶段**
- 新增 `ProductStatusEnum.FAIL`，删除 `ProductStatusEnum.REDO`（全局搜索确认 REDO 的所有引用都在本步骤中清理）
- 新增 `markProductFail` 接口（`ProductionQcController`、`IProductionQcService`、`ProductionQcServiceImpl`）
- 删除 `markProductRedo`、`assignRedoProcess`、`listRedoProducts`（三层同步删除）
- 修改 `markProductPass` 允许 FAIL 状态
- 修改 `transferToPacking` 校验逻辑

**第五步：清理字段和枚举**
- 删除 `ProductionRecordEntity.hasRedoProduct`，同步清理 `ProductionRecordVO`
- 删除 `ProductionProductEntity.redoProcessType`，同步清理 `ProductionProductVO`、`ProductionProductDetailVO`
- 将 `QcResultEnum.REDO` 改为 `QcResultEnum.FAIL`（code: `fail`）；改前全局搜索 `QcResultEnum.REDO` 确认所有引用都已在第四步清理
- 删除 `QcHandleTypeEnum.java`；改前全局搜索确认无残留引用
- 删除 `ProductionRedoPageDTO.java`
- 更新 `sql/ddl.sql`：删除 `production_process_product_result` 表，删除 `has_redo_product`、`redo_process_type` 字段
- 检查 `sql/init.sql` 是否有引用被删除字段的数据，若有则同步清理

**第六步：同步更新接口文档和测试计划**

每步完成后编译验证，确认无误后进行下一步。

---

## 七、简化效果对比

| 项目 | 简化前 | 简化后 |
|------|--------|--------|
| 接口数量 | 31 个 | ~22 个 |
| 产品状态数 | 5（in_process/redo/pass/completed/cancelled） | 5（in_process/fail/pass/completed/cancelled） |
| 工序推进方式 | finishProcess + submitProcessQc | finishProcess 直接推进 |
| 打印完成触发 | 操作员首检确认 | 设备推送自动触发 |
| 异常处理 | 系统级（回退、重做、废弃） | 线下处理，系统只记录 |
| 数据库表 | production_process_product_result | 删除 |

---

**文档版本**：2.0  
**状态**：待实施
