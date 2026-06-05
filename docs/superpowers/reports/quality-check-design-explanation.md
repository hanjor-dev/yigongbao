# 质检环节设计说明

## 问题背景

用户反馈：非医疗器械订单设计完成后创建流转卡时，只初始化了打印和包装工序，缺少质检工序记录。

## 系统设计架构

### 工序 vs 质检的设计区别

**工序（Process）**：
- 存储在 `production_process` 表
- 包含类型：PRINT（打印）、WASH（清洗）、CURE（固化）、CLEAN_DRY（清洁干燥）、PACK（包装）
- 需要分配设备、记录操作员、开始时间、完成时间
- 通过工序管理模块操作

**质检（QC）**：
- 不是工序记录，是**流程状态**
- 流程状态值：QC_IN_PROGRESS(5010)、QC_PASSED(5020)、QC_FAILED(5030)、REWORK(5040)、PACKING(5050)
- 通过质检管理模块操作（ProductionQcService）
- 按产品粒度进行质检，标记合格/不合格

### 为什么质检不是工序？

1. **操作模式不同**：
   - 工序：需要设备、有明确的开始/结束时间、工序完成后整体流转
   - 质检：按产品逐个检查、可以部分合格部分不合格、不需要设备分配

2. **数据粒度不同**：
   - 工序：流转卡级别（整张流转卡的工序状态）
   - 质检：产品级别（每个产品的质检结果）

3. **流程控制不同**：
   - 工序：顺序执行，一个完成才能开始下一个
   - 质检：可以对流转卡中的产品逐个质检，不要求全部一次性完成

## 实际业务流程

### 医疗器械订单

```
创建流转卡（初始化5个工序记录）：
├─ 打印工序 (print)
├─ 清洗工序 (wash)
├─ 固化工序 (cure)
├─ 清洁干燥工序 (clean_dry)
└─ 包装工序 (pack)

执行流程：
1. 开始打印 → 打印中 (3020)
2. 打印完成 → 打印完成 (3030)
3. 开始后处理 → 后处理中 (4010)
4. 后处理完成 → **自动流转到质检中 (5010)** ← 质检环节开始
5. 质检完成 → 包装中 (5050)
6. 开始包装工序
7. 包装完成 → 入库
```

### 非医疗器械订单

```
创建流转卡（初始化2个工序记录）：
├─ 打印工序 (print)
└─ 包装工序 (pack)

执行流程：
1. 开始打印 → 打印中 (3020)
2. 打印完成 → 打印完成 (3030)
3. **自动流转到质检中 (5010)** ← 质检环节开始（DeviceStatusListener.java:97-107）
4. 质检完成 → 包装中 (5050)
5. 开始包装工序
6. 包装完成 → 入库
```

## 代码实现位置

### 工序初始化
**文件**：`DesignCompletedListener.java`  
**方法**：`createProcessRecords(Long recordId, Integer orderType)`  
**代码**：253-275行

```java
private void createProcessRecords(Long recordId, Integer orderType) {
    List<ProcessTypeEnum> processTypes = new ArrayList<>();
    processTypes.add(ProcessTypeEnum.PRINT);
    if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(orderType)) {
        // 医疗器械添加后处理工序
        processTypes.add(ProcessTypeEnum.WASH);
        processTypes.add(ProcessTypeEnum.CURE);
        processTypes.add(ProcessTypeEnum.CLEAN_DRY);
    }
    processTypes.add(ProcessTypeEnum.PACK);
    // ... 创建工序记录
}
```

### 质检环节触发
**医疗器械**：`ProductionProcessServiceImpl.finishProcess()` 后处理完成后流转到QC  
**非医疗器械**：`DeviceStatusListener.java` 打印完成后直接流转到QC

```java
// 非医疗器械订单打印完成后 Flow 直接跳 QC
if (isNonMedical) {
    recordMapper.update(null,
        new LambdaUpdateWrapper<ProductionRecordEntity>()
            .eq(ProductionRecordEntity::getOrderId, orderId)
            .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.PRINT_COMPLETED.getValue())
            .set(ProductionRecordEntity::getStatus, FlowStatusEnum.QC_IN_PROGRESS.getValue()));
}
```

### 质检操作
**文件**：`ProductionQcServiceImpl.java`  
**功能**：
- `markProductPass()` - 标记产品质检合格
- `markProductFail()` - 标记产品质检不合格
- `transferToPacking()` - 质检完成，流转到包装

## 前端交互

### 工序列表接口
**接口**：`GET /production/process/{recordId}/list`  
**返回**：工序记录列表（打印、清洗、固化、清洁干燥、包装）  
**不包含**：质检环节（因为质检不是工序）

### 质检列表接口
**接口**：`POST /production/qc/list`  
**返回**：质检中的流转卡列表  
**操作**：逐个产品标记合格/不合格

## 结论

**用户反馈的"缺少质检工序初始化记录"不是缺陷**，而是系统的正确设计：

1. ✅ 非医疗器械订单正确初始化了2个工序：打印 + 包装
2. ✅ 质检不应该出现在工序列表中，它是独立的流程环节
3. ✅ 打印完成后自动流转到质检状态（QC_IN_PROGRESS）
4. ✅ 质检通过独立的质检管理模块操作

**前端展示建议**：
- 工序列表：显示工序记录（打印、后处理、包装）
- 质检环节：单独的质检管理页面，按产品粒度操作
- 流程状态：在流转卡详情中显示当前状态（包括质检中状态）

---

**创建时间**：2026-06-05  
**文档版本**：1.0
