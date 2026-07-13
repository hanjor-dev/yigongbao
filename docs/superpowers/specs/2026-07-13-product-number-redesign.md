# 产品编号规则调整设计文档

**文档版本**: 1.0  
**创建日期**: 2026-07-13  
**设计方案**: 方案A - 分配设备时同步生成  
**状态**: 待评审

---

## 1. 概述

### 1.1 背景

当前产品编号采用简单的序列号格式（PD-000001），无法体现生产批次、产品类型、设备信息等关键业务信息。为了提升生产管理的可追溯性和规范性，需要调整产品编号规则为更具业务含义的组合编号。

### 1.2 目标

- 将产品编号从简单格式（PD-000001）调整为组合格式（260630B03700201）
- 编号中包含生产批号、产品类型、设备编号、上机次数、产品流水号
- 确保产品编号全局唯一且可追溯
- 最小化对现有代码的影响

### 1.3 范围

**涉及模块**：
- `yigongbao-module-production`（生产模块）
- 数据库表：`production_product`、`device`、新增`device_daily_usage_counter`

**不涉及**：
- 订单模块、设计模块（仅读取数据）
- 历史数据迁移（历史产品编号保持不变）

---

## 2. 需求总结

### 2.1 新编号规则

产品编号格式：**生产批号(6位) + 产品代码(1位) + 设备编号(3位) + 上机次数(3位) + 产品流水号(2位)**

**示例**：`260630B03700201`

**字段说明**：

| 字段 | 位数 | 说明 | 示例 |
|------|------|------|------|
| 生产批号 | 6位 | 年月日（YYMMDD格式） | 260630 |
| 产品代码 | 1位 | 产品类型代码（A/B/C/D/X） | B |
| 设备编号 | 3位 | 打印设备编号（补齐3位） | 037 |
| 上机次数 | 3位 | 设备当日上机次数（补齐3位） | 002 |
| 产品流水号 | 2位 | 流转卡内产品序号（补齐2位） | 01 |

### 2.2 产品代码映射规则

采用**精准匹配**（equals）产品名称：

| 代码 | 产品名称 |
|------|----------|
| A | 医用个性化手术导板 |
| B | 定制式3D打印骨模型 |
| C | 定制式神经外科手术导板 |
| D | 定制式放射粒子手术导板 |
| X | 其他（未匹配以上类型） |

### 2.3 关键业务规则

1. **生成时机**：
   - 流转卡创建时：生成临时编号（PD-000001格式）
   - 分配打印设备时：生成正式编号，覆盖临时编号

2. **上机次数**：
   - 分配打印设备时，设备当日上机次数+1（确保产品编号的唯一性和顺序性）
   - 同一设备多个流转卡分配会依次累加
   - 每天00:00:00自动清零
   - **语义说明**：上机次数代表"设备分配顺序"，即该设备当天第几次被分配使用

3. **产品流水号**：
   - 主排序：按设计产品表的`sort_order`字段
   - 次排序：如果`sort_order`相同，按产品创建时间

4. **唯一性约束**：
   - 产品编号必须全局唯一
   - 生成前校验唯一性，重复则抛出异常

---

## 3. 方案设计

### 3.1 整体架构

```
流转卡创建 → 临时编号(PD-000001)
     ↓
分配打印设备 → 上机次数+1 → 生成正式编号(260630B03700201)
     ↓
开始打印 → （无产品编号相关操作）
```

### 3.2 核心流程

**流程1：流转卡创建**
1. `DesignCompletedListener.createProductRecords()` 创建产品记录
2. 调用`codeGeneratorService.generate(PRODUCT_NO)` 生成临时编号
3. 临时编号格式：PD-000001

**流程2：分配打印设备**
1. 调用`ProductionRecordService.assignDevice()` 分配设备
2. 更新流转卡的`print_device_id`等字段
3. 累加设备当日上机次数（`device_daily_usage_counter`表）
4. 调用新服务`ProductNumberService.generateFormalNumbers()` 生成正式编号
5. 批量更新产品表的`product_no`字段

**流程3：开始打印**
1. 调用`ProductionProcessService.startProcess(PRINT)` 开始打印
2. 无需更新上机次数（已在分配设备时累加）
3. 产品编号已生成，无需额外操作

---

## 4. 数据库设计

### 4.1 新增表：device_daily_usage_counter

**表名**：`device_daily_usage_counter`  
**用途**：记录每个设备每天的上机次数

```sql
CREATE TABLE device_daily_usage_counter (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    device_id       BIGINT NOT NULL COMMENT '设备ID（关联device表）',
    usage_date      DATE NOT NULL COMMENT '使用日期',
    usage_count     INT NOT NULL DEFAULT 0 COMMENT '当日上机次数',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_device_date (device_id, usage_date),
    KEY idx_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备每日上机次数统计表';
```

**字段说明**：
- `device_id`：关联device表的主键
- `usage_date`：使用日期（DATE类型，自动处理跨天）
- `usage_count`：当天累计上机次数
- `version`：乐观锁版本号，防止并发更新冲突

**索引说明**：
- `uk_device_date`：唯一索引，确保每个设备每天只有一条记录
- `idx_usage_date`：日期索引，便于定期清理历史数据

### 4.2 修改表：production_product

**变更**：无需修改表结构

**说明**：
- `product_no`字段已存在，类型为`VARCHAR(50)`，足以容纳新编号（15位）
- 保持`NOT NULL`约束（临时编号也是有值的）
- 唯一索引`uk_product_no`保持不变

---

## 5. 核心组件设计

### 5.1 ProductNumberService（新增）

**包路径**：`com.yigongbao.module.production.product.service`

**职责**：负责产品编号的生成和管理

**核心方法**：

```java
public interface IProductNumberService {
    /**
     * 为流转卡下的所有产品生成正式编号
     * 
     * @param recordId 流转卡ID
     * @param deviceId 设备ID
     * @param usageCount 设备当日上机次数
     */
    void generateFormalNumbers(Long recordId, Long deviceId, Integer usageCount);
    
    /**
     * 生成单个产品的正式编号
     * 
     * @param batchNo 生产批号（YYMMDD）
     * @param productName 产品名称
     * @param deviceNo 设备编号
     * @param usageCount 上机次数
     * @param sequenceNo 产品流水号
     * @return 正式产品编号
     */
    String generateSingleNumber(String batchNo, String productName, 
                                String deviceNo, Integer usageCount, 
                                Integer sequenceNo);
    
    /**
     * 根据产品名称获取产品代码
     * 
     * @param productName 产品名称
     * @return 产品代码（A/B/C/D/X）
     */
    String getProductTypeCode(String productName);
    
    /**
     * 校验产品编号唯一性
     * 
     * @param productNo 产品编号
     * @return true=唯一，false=重复
     */
    boolean checkUniqueness(String productNo);
}
```

### 5.2 DeviceUsageCounterService（新增）

**包路径**：`com.yigongbao.module.production.device.service`

**职责**：管理设备每日上机次数

**核心方法**：

```java
public interface IDeviceUsageCounterService {
    /**
     * 获取并累加设备当日上机次数
     * 
     * @param deviceId 设备ID
     * @return 累加后的当日上机次数
     */
    Integer incrementAndGet(Long deviceId);
    
    /**
     * 查询设备当日上机次数（不累加）
     * 
     * @param deviceId 设备ID
     * @return 当日上机次数，如果当天未使用则返回0
     */
    Integer getTodayCount(Long deviceId);
}
```

### 5.3 ProductionRecordServiceImpl（修改）

**修改方法**：`assignDevice()`

**变更内容**：
1. 保存设备信息后，调用`ProductNumberService.generateFormalNumbers()`
2. 在同一事务中完成设备分配和编号生成

**伪代码**：

```java
@Transactional(rollbackFor = Exception.class)
public void assignDevice(Long recordId, AssignDeviceDTO dto) {
    // 1. 更新流转卡设备信息（原有逻辑）
    ProductionRecordEntity record = recordMapper.selectById(recordId);
    record.setPrintDeviceId(dto.getPrintDeviceId());
    // ... 其他设备字段
    recordMapper.updateById(record);
    
    // 2. 累加设备当日上机次数（分配时立即累加，确保编号唯一性）
    Integer usageCount = deviceUsageCounterService.incrementAndGet(dto.getPrintDeviceId());
    
    // 3. 生成正式产品编号（新增逻辑）
    productNumberService.generateFormalNumbers(recordId, dto.getPrintDeviceId(), usageCount);
    
    log.info("分配打印设备并生成产品编号: recordId={}, deviceId={}, usageCount={}", 
        recordId, dto.getPrintDeviceId(), usageCount);
}
```

### 5.4 ProductionProcessServiceImpl（无需修改）

**说明**：由于上机次数的累加已在分配设备时完成，`startProcess()` 方法无需修改，保持原有逻辑即可。

---

## 6. 实施细节

### 6.1 ProductNumberServiceImpl 实现

**核心逻辑**：

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductNumberServiceImpl implements IProductNumberService {
    
    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;
    private final DesignProductMapper designProductMapper;
    private final DeviceMapper deviceMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateFormalNumbers(Long recordId, Long deviceId, Integer usageCount) {
        // 1. 查询流转卡信息
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        
        // 2. 查询设备信息
        DeviceEntity device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_FOUND);
        }
        
        // 3. 生成批号（YYMMDD格式）
        String batchNo = record.getProductionBatchNo(); // 流转卡创建时已生成
        
        // 4. 获取设备编号（补齐3位）
        // 注意：device.getDeviceId()返回的是device_id字段（VARCHAR类型的业务编号），非主键id
        String deviceNo = String.format("%03d", Integer.parseInt(device.getDeviceId()));
        
        // 5. 获取上机次数（补齐3位）
        String usageCountStr = String.format("%03d", usageCount);
        
        // 6. 查询流转卡下的所有产品（按排序规则）
        List<ProductionProductEntity> products = getProductsInOrder(recordId, record.getDesignPackageId());
        
        // 7. 批量生成产品编号
        int sequenceNo = 1;
        for (ProductionProductEntity product : products) {
            String productNo = generateSingleNumber(
                batchNo, 
                product.getProductName(), 
                deviceNo, 
                usageCount, 
                sequenceNo
            );
            
            // 8. 校验唯一性
            if (!checkUniqueness(productNo)) {
                throw new BusinessException(ErrorCodeEnum.PRODUCT_NUMBER_DUPLICATE, productNo);
            }
            
            // 9. 更新产品编号
            product.setProductNo(productNo);
            productMapper.updateById(product);
            
            sequenceNo++;
        }
        
        log.info("生成正式产品编号: recordId={}, deviceId={}, usageCount={}, productCount={}", 
            recordId, deviceId, usageCount, products.size());
    }
    
    /**
     * 按排序规则查询产品列表
     * 主排序：设计产品的sort_order
     * 次排序：产品创建时间
     */
    private List<ProductionProductEntity> getProductsInOrder(Long recordId, Long designPackageId) {
        // 1. 查询生产产品列表
        List<ProductionProductEntity> products = productMapper.selectList(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .orderByAsc(ProductionProductEntity::getCreateTime)
        );
        
        // 2. 查询对应的设计产品（获取sort_order）
        List<Long> printFileIds = products.stream()
            .map(ProductionProductEntity::getPrintFileId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (printFileIds.isEmpty()) {
            return products;
        }
        
        // 3. 根据print_file_id关联查询设计产品的sort_order
        // 注意：这里需要通过package_file_id关联到design_product_file，再关联到design_product
        Map<Long, Integer> sortOrderMap = getSortOrderMap(designPackageId, printFileIds);
        
        // 4. 按sort_order排序（相同则按创建时间）
        products.sort((p1, p2) -> {
            Integer order1 = sortOrderMap.getOrDefault(p1.getPrintFileId(), Integer.MAX_VALUE);
            Integer order2 = sortOrderMap.getOrDefault(p2.getPrintFileId(), Integer.MAX_VALUE);
            int cmp = order1.compareTo(order2);
            if (cmp != 0) return cmp;
            return p1.getCreateTime().compareTo(p2.getCreateTime());
        });
        
        return products;
    }
    
    /**
     * 获取文件ID到sort_order的映射
     */
    private Map<Long, Integer> getSortOrderMap(Long designPackageId, List<Long> printFileIds) {
        // 实现逻辑：查询design_product表，通过design_product_file关联
        // 返回 Map<printFileId, sortOrder>
        // 具体实现根据实际表结构调整
        return new HashMap<>();
    }
    
    @Override
    public String generateSingleNumber(String batchNo, String productName, 
                                       String deviceNo, Integer usageCount, 
                                       Integer sequenceNo) {
        // 1. 获取产品代码
        String productCode = getProductTypeCode(productName);
        
        // 2. 格式化各部分
        String usageCountStr = String.format("%03d", usageCount);
        String sequenceNoStr = String.format("%02d", sequenceNo);
        
        // 3. 拼接编号
        return batchNo + productCode + deviceNo + usageCountStr + sequenceNoStr;
    }
    
    @Override
    public String getProductTypeCode(String productName) {
        if (productName == null) {
            return "X";
        }
        
        // 精准匹配产品名称
        return switch (productName) {
            case "医用个性化手术导板" -> "A";
            case "定制式3D打印骨模型" -> "B";
            case "定制式神经外科手术导板" -> "C";
            case "定制式放射粒子手术导板" -> "D";
            default -> "X";
        };
    }
    
    @Override
    public boolean checkUniqueness(String productNo) {
        Long count = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductNo, productNo)
        );
        return count == 0;
    }
}
```

### 6.2 DeviceUsageCounterServiceImpl 实现

**核心逻辑**：

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceUsageCounterServiceImpl extends ServiceImpl<DeviceUsageCounterMapper, DeviceUsageCounterEntity>
        implements IDeviceUsageCounterService {
    
    private final DeviceUsageCounterMapper counterMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer incrementAndGet(Long deviceId) {
        LocalDate today = LocalDate.now();
        
        // 1. 尝试累加（乐观锁）
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 查询当天记录
                DeviceUsageCounterEntity counter = counterMapper.selectOne(
                    new LambdaQueryWrapper<DeviceUsageCounterEntity>()
                        .eq(DeviceUsageCounterEntity::getDeviceId, deviceId)
                        .eq(DeviceUsageCounterEntity::getUsageDate, today)
                );
                
                if (counter == null) {
                    // 首次使用，插入记录
                    counter = new DeviceUsageCounterEntity();
                    counter.setDeviceId(deviceId);
                    counter.setUsageDate(today);
                    counter.setUsageCount(1);
                    counter.setVersion(0);
                    counterMapper.insert(counter);
                    
                    log.info("初始化设备上机次数: deviceId={}, date={}, count=1", deviceId, today);
                    return 1;
                } else {
                    // 累加（乐观锁更新）
                    int updated = counterMapper.update(null,
                        new LambdaUpdateWrapper<DeviceUsageCounterEntity>()
                            .eq(DeviceUsageCounterEntity::getId, counter.getId())
                            .eq(DeviceUsageCounterEntity::getVersion, counter.getVersion())
                            .set(DeviceUsageCounterEntity::getUsageCount, counter.getUsageCount() + 1)
                            .set(DeviceUsageCounterEntity::getVersion, counter.getVersion() + 1)
                    );
                    
                    if (updated > 0) {
                        int newCount = counter.getUsageCount() + 1;
                        log.info("累加设备上机次数: deviceId={}, date={}, count={}", deviceId, today, newCount);
                        return newCount;
                    } else {
                        // 乐观锁冲突，重试
                        log.warn("设备上机次数更新冲突，重试: deviceId={}, retry={}", deviceId, i + 1);
                    }
                }
            } catch (DuplicateKeyException e) {
                // 并发插入冲突，重试
                log.warn("设备上机次数插入冲突，重试: deviceId={}, retry={}", deviceId, i + 1);
            }
        }
        
        // 重试失败
        throw new BusinessException(ErrorCodeEnum.DEVICE_USAGE_COUNTER_UPDATE_FAILED);
    }
    
    @Override
    public Integer getTodayCount(Long deviceId) {
        LocalDate today = LocalDate.now();
        
        DeviceUsageCounterEntity counter = counterMapper.selectOne(
            new LambdaQueryWrapper<DeviceUsageCounterEntity>()
                .eq(DeviceUsageCounterEntity::getDeviceId, deviceId)
                .eq(DeviceUsageCounterEntity::getUsageDate, today)
        );
        
        return counter != null ? counter.getUsageCount() : 0;
    }
}
```

### 6.3 设计产品排序逻辑优化

**问题**：`production_product`表不直接存储`sort_order`，需要通过关联查询获取。

**解决方案**：

1. **建立关联关系**：
   - `production_product.print_file_id` → `design_package_file.id`
   - `design_package_file` 关联 `design_product_file`
   - `design_product_file.design_product_id` → `design_product.id`
   - `design_product.sort_order` 是最终排序依据

2. **SQL 查询示例**：

```sql
SELECT pp.*, dp.sort_order
FROM production_product pp
LEFT JOIN design_package_file dpf ON pp.print_file_id = dpf.id
LEFT JOIN design_product_file dprf ON dpf.file_name = dprf.package_file_name 
    AND dprf.package_id = :designPackageId
LEFT JOIN design_product dp ON dprf.design_product_id = dp.id
WHERE pp.production_record_id = :recordId
ORDER BY dp.sort_order ASC, pp.create_time ASC;
```

3. **代码实现**：可以在 Mapper 中定义自定义查询方法，或在 Service 中分步查询后排序。

---

## 7. 异常处理与边界情况

### 7.1 并发冲突处理

**场景1：多个流转卡同时分配同一设备**

- **问题**：多个线程同时调用 `incrementAndGet()`，可能出现乐观锁冲突
- **解决方案**：
  - 使用乐观锁版本号（`version` 字段）确保累加的原子性
  - 更新失败时自动重试（最多3次）
  - 每次累加后立即生成产品编号，确保编号中的上机次数准确
- **结果**：通过乐观锁+重试机制，确保并发场景下上机次数正确累加，产品编号唯一且按分配顺序递增

**场景2：产品编号重复**

- **问题**：理论上不应出现，但需要防御性校验
- **解决方案**：
  - 生成编号后立即校验唯一性
  - 检测到重复立即抛出 `PRODUCT_NUMBER_DUPLICATE` 异常
  - 整个事务回滚，不保存任何数据
- **日志记录**：记录重复的编号和相关上下文，便于排查

**场景3：产品编号重复**

- **问题**：理论上不应出现，但需要防御性校验
- **解决方案**：
  - 生成编号后立即校验唯一性
  - 检测到重复立即抛出 `PRODUCT_NUMBER_DUPLICATE` 异常
  - 整个事务回滚，不保存任何数据
- **日志记录**：记录重复的编号和相关上下文，便于排查

### 7.2 数据校验

**校验项1：流转卡状态校验**

```java
// 只有待打印或打印中状态的流转卡才能分配设备
if (!FlowStatusEnum.PENDING_PRINT.getValue().equals(record.getStatus()) &&
    !FlowStatusEnum.PRINTING.getValue().equals(record.getStatus())) {
    throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_ASSIGN_DEVICE);
}
```

**校验项2：设备类型校验**

```java
// 分配的设备必须是打印设备类型
if (!DeviceTypeEnum.PRINTER_SLA.getCode().equals(device.getDeviceType())) {
    throw new BusinessException(ErrorCodeEnum.DEVICE_TYPE_MISMATCH);
}
```

**校验项3：产品数量校验**

```java
// 流转卡下必须有产品才能生成编号
List<ProductionProductEntity> products = getProductsInOrder(recordId, designPackageId);
if (products.isEmpty()) {
    throw new BusinessException(ErrorCodeEnum.RECORD_NO_PRODUCT_FOR_NUMBER_GENERATION);
}
```

**校验项4：设备编号格式校验**

```java
// 设备编号必须是纯数字且在001-999范围内
String deviceId = device.getDeviceId();
if (!deviceId.matches("\\d+")) {
    throw new BusinessException(ErrorCodeEnum.DEVICE_ID_INVALID_FORMAT);
}
int deviceNum = Integer.parseInt(deviceId);
if (deviceNum < 1 || deviceNum > 999) {
    throw new BusinessException(ErrorCodeEnum.DEVICE_ID_OUT_OF_RANGE);
}
```

### 7.3 事务回滚策略

**原则**：分配设备和生成编号必须在同一事务中，要么全部成功，要么全部失败。

**实现**：

```java
@Transactional(rollbackFor = Exception.class)
public void assignDevice(Long recordId, AssignDeviceDTO dto) {
    try {
        // 1. 更新流转卡设备信息
        updateRecordDevice(recordId, dto);
        
        // 2. 生成产品编号
        productNumberService.generateFormalNumbers(recordId, dto.getPrintDeviceId(), usageCount);
        
        // 3. 记录日志
        log.info("分配设备并生成产品编号成功: recordId={}, deviceId={}", recordId, dto.getPrintDeviceId());
    } catch (BusinessException e) {
        log.error("分配设备失败: recordId={}, deviceId={}, reason={}", 
            recordId, dto.getPrintDeviceId(), e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("分配设备异常: recordId={}, deviceId={}", recordId, dto.getPrintDeviceId(), e);
        throw new BusinessException(ErrorCodeEnum.ASSIGN_DEVICE_FAILED);
    }
}
```

**回滚场景**：
- 流转卡更新失败 → 回滚
- 编号生成失败 → 回滚流转卡更新
- 编号唯一性校验失败 → 回滚所有操作

### 7.4 跨天边界处理

**场景**：23:59:59 分配设备，00:00:01 开始打印

**实际情况**：
- 分配设备时（23:59:59）：累加前一天的上机次数，生成包含前一天日期和次数的产品编号
- 开始打印时（00:00:01）：无上机次数相关操作

**说明**：
- **无一致性问题**：产品编号在分配设备时一次性生成完成，批号和上机次数都基于分配时刻
- **业务含义**：批号代表"流转卡创建日期"，上机次数代表"设备分配时该设备当天的第几次使用"
- **追溯性**：通过流转卡记录的 `assign_device_time`（需要新增此字段）可以追溯实际分配时间
- **设计权衡**：这是为了简化实现而接受的设计权衡，不影响产品的唯一性和可追溯性

### 7.5 设备编号格式异常

**场景**：设备表中的 `device_id` 不是纯数字或超出3位范围

**示例**：
- 非数字：`A01`, `3D-Printer`
- 超范围：`1000`, `0`

**处理策略**：
- **数据迁移阶段**：清理异常数据，确保所有打印设备的 `device_id` 为1-999的整数
- **运行时校验**：`assignDevice()` 方法中校验格式，异常则抛出 `DEVICE_ID_INVALID_FORMAT`
- **前端限制**：设备管理界面限制输入格式

### 7.6 历史数据兼容

**问题**：已存在的产品编号是 PD-000001 格式

**策略**：
- **不迁移历史数据**：历史产品编号保持不变
- **区分新旧编号**：通过编号格式判断（PD- 开头为旧编号，15位数字为新编号）
- **查询兼容**：按产品编号查询时兼容两种格式
- **报表统计**：按编号格式分组统计

### 7.7 错误码清单

需要在 `ErrorCodeEnum` 中新增以下错误码：

| 错误码 | 错误码值 | 错误消息 |
|--------|---------|----------|
| `DEVICE_USAGE_COUNTER_UPDATE_FAILED` | 701 | 设备上机次数更新失败，请重试 |
| `PRODUCT_NUMBER_DUPLICATE` | 702 | 产品编号重复：{} |
| `RECORD_NO_PRODUCT_FOR_NUMBER_GENERATION` | 703 | 流转卡无产品，无法生成编号 |
| `DEVICE_ID_INVALID_FORMAT` | 704 | 设备编号格式无效，必须为1-999的整数 |
| `DEVICE_ID_OUT_OF_RANGE` | 705 | 设备编号超出范围（1-999） |
| `ASSIGN_DEVICE_FAILED` | 706 | 分配设备失败 |
| `RECORD_STATUS_NOT_ALLOW_ASSIGN_DEVICE` | 707 | 流转卡状态不允许分配设备 |

**枚举定义示例**：

```java
public enum ErrorCodeEnum {
    // ... 现有错误码
    
    DEVICE_USAGE_COUNTER_UPDATE_FAILED(701, "设备上机次数更新失败，请重试"),
    PRODUCT_NUMBER_DUPLICATE(702, "产品编号重复：{}"),
    RECORD_NO_PRODUCT_FOR_NUMBER_GENERATION(703, "流转卡无产品，无法生成编号"),
    DEVICE_ID_INVALID_FORMAT(704, "设备编号格式无效，必须为1-999的整数"),
    DEVICE_ID_OUT_OF_RANGE(705, "设备编号超出范围（1-999）"),
    ASSIGN_DEVICE_FAILED(706, "分配设备失败"),
    RECORD_STATUS_NOT_ALLOW_ASSIGN_DEVICE(707, "流转卡状态不允许分配设备");
    
    private final int code;
    private final String message;
    
    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

---

## 8. 测试策略

### 8.1 单元测试

#### 8.1.1 ProductNumberServiceImpl 测试

**测试类**：`ProductNumberServiceImplTest`

**核心测试用例**：

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductNumberServiceImplTest {
    
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private ProductionProductMapper productMapper;
    @Mock private DesignProductMapper designProductMapper;
    @Mock private DeviceMapper deviceMapper;
    @InjectMocks private ProductNumberServiceImpl productNumberService;
    
    @Test
    void testGetProductTypeCode_精准匹配() {
        assertEquals("A", productNumberService.getProductTypeCode("医用个性化手术导板"));
        assertEquals("B", productNumberService.getProductTypeCode("定制式3D打印骨模型"));
        assertEquals("C", productNumberService.getProductTypeCode("定制式神经外科手术导板"));
        assertEquals("D", productNumberService.getProductTypeCode("定制式放射粒子手术导板"));
        assertEquals("X", productNumberService.getProductTypeCode("其他产品"));
        assertEquals("X", productNumberService.getProductTypeCode(null));
    }
    
    @Test
    void testGenerateSingleNumber_格式正确() {
        String result = productNumberService.generateSingleNumber(
            "260630", "定制式3D打印骨模型", "037", 2, 1);
        assertEquals("260630B03700201", result);
        assertEquals(15, result.length());
    }
    
    @Test
    void testGenerateSingleNumber_边界值() {
        // 设备编号边界（1, 999）
        assertEquals("260630B00100101", 
            productNumberService.generateSingleNumber("260630", "定制式3D打印骨模型", "001", 1, 1));
        assertEquals("260630B99900101", 
            productNumberService.generateSingleNumber("260630", "定制式3D打印骨模型", "999", 1, 1));
        
        // 上机次数边界（0, 999）
        assertEquals("260630B03700001", 
            productNumberService.generateSingleNumber("260630", "定制式3D打印骨模型", "037", 0, 1));
        assertEquals("260630B03799901", 
            productNumberService.generateSingleNumber("260630", "定制式3D打印骨模型", "037", 999, 1));
        
        // 产品流水号边界（1, 99）
        assertEquals("260630B03700201", 
            productNumberService.generateSingleNumber("260630", "定制式3D打印骨模型", "037", 2, 1));
        assertEquals("260630B03700299", 
            productNumberService.generateSingleNumber("260630", "定制式3D打印骨模型", "037", 2, 99));
    }
    
    @Test
    void testCheckUniqueness_编号唯一() {
        when(productMapper.selectCount(any())).thenReturn(0L);
        assertTrue(productNumberService.checkUniqueness("260630B03700201"));
    }
    
    @Test
    void testCheckUniqueness_编号重复() {
        when(productMapper.selectCount(any())).thenReturn(1L);
        assertFalse(productNumberService.checkUniqueness("260630B03700201"));
    }
    
    @Test
    void testGenerateFormalNumbers_成功生成() {
        // Mock数据准备...
        productNumberService.generateFormalNumbers(1L, 37L, 2);
        
        // 验证：产品编号已更新
        verify(productMapper, times(3)).updateById(any());
    }
    
    @Test
    void testGenerateFormalNumbers_流转卡不存在() {
        when(recordMapper.selectById(1L)).thenReturn(null);
        
        assertThrows(BusinessException.class, 
            () -> productNumberService.generateFormalNumbers(1L, 37L, 2));
    }
}
```

#### 8.1.2 DeviceUsageCounterServiceImpl 测试

**测试类**：`DeviceUsageCounterServiceImplTest`

**核心测试用例**：

```java
@ExtendWith(MockitoExtension.class)
class DeviceUsageCounterServiceImplTest {
    
    @Mock private DeviceUsageCounterMapper counterMapper;
    @InjectMocks private DeviceUsageCounterServiceImpl counterService;
    
    @Test
    void testIncrementAndGet_首次使用() {
        when(counterMapper.selectOne(any())).thenReturn(null);
        
        Integer count = counterService.incrementAndGet(1L);
        
        assertEquals(1, count);
        verify(counterMapper, times(1)).insert(any());
    }
    
    @Test
    void testIncrementAndGet_累加成功() {
        DeviceUsageCounterEntity existing = new DeviceUsageCounterEntity();
        existing.setId(1L);
        existing.setDeviceId(1L);
        existing.setUsageCount(5);
        existing.setVersion(2);
        
        when(counterMapper.selectOne(any())).thenReturn(existing);
        when(counterMapper.update(any(), any())).thenReturn(1);
        
        Integer count = counterService.incrementAndGet(1L);
        
        assertEquals(6, count);
        verify(counterMapper, times(1)).update(any(), any());
    }
    
    @Test
    void testIncrementAndGet_乐观锁冲突重试() {
        DeviceUsageCounterEntity existing = new DeviceUsageCounterEntity();
        existing.setId(1L);
        existing.setDeviceId(1L);
        existing.setUsageCount(5);
        existing.setVersion(2);
        
        when(counterMapper.selectOne(any())).thenReturn(existing);
        when(counterMapper.update(any(), any()))
            .thenReturn(0)  // 第1次失败
            .thenReturn(0)  // 第2次失败
            .thenReturn(1); // 第3次成功
        
        Integer count = counterService.incrementAndGet(1L);
        
        assertEquals(6, count);
        verify(counterMapper, times(3)).update(any(), any());
    }
    
    @Test
    void testIncrementAndGet_超过重试次数() {
        DeviceUsageCounterEntity existing = new DeviceUsageCounterEntity();
        existing.setId(1L);
        existing.setVersion(2);
        
        when(counterMapper.selectOne(any())).thenReturn(existing);
        when(counterMapper.update(any(), any())).thenReturn(0); // 一直失败
        
        assertThrows(BusinessException.class, 
            () -> counterService.incrementAndGet(1L));
    }
    
    @Test
    void testGetTodayCount_存在记录() {
        DeviceUsageCounterEntity counter = new DeviceUsageCounterEntity();
        counter.setUsageCount(10);
        
        when(counterMapper.selectOne(any())).thenReturn(counter);
        
        assertEquals(10, counterService.getTodayCount(1L));
    }
    
    @Test
    void testGetTodayCount_不存在记录() {
        when(counterMapper.selectOne(any())).thenReturn(null);
        
        assertEquals(0, counterService.getTodayCount(1L));
    }
}
```

### 8.2 集成测试

#### 8.2.1 完整流程测试

**测试类**：`ProductNumberIntegrationTest`

**测试场景**：

```java
@SpringBootTest
@Transactional
class ProductNumberIntegrationTest {
    
    @Autowired private IProductionRecordService recordService;
    @Autowired private IProductNumberService productNumberService;
    @Autowired private IDeviceUsageCounterService counterService;
    @Autowired private ProductionProductMapper productMapper;
    
    @Test
    void test完整流程_创建流转卡到生成编号() {
        // 1. 创建流转卡（触发临时编号生成）
        Long recordId = createTestRecord();
        
        // 2. 验证临时编号格式
        List<ProductionProductEntity> products = productMapper.selectList(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId));
        assertTrue(products.get(0).getProductNo().startsWith("PD-"));
        
        // 3. 分配设备（触发上机次数累加和正式编号生成）
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setPrintDeviceId(37L);
        recordService.assignDevice(recordId, dto);
        
        // 4. 验证上机次数已累加
        Integer count = counterService.getTodayCount(37L);
        assertEquals(1, count);
        
        // 5. 验证正式编号格式
        products = productMapper.selectList(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId));
        
        String firstProductNo = products.get(0).getProductNo();
        assertEquals(15, firstProductNo.length());
        assertTrue(firstProductNo.matches("\\d{15}"));
        assertTrue(firstProductNo.startsWith("260713")); // 今天日期
        assertTrue(firstProductNo.contains("037")); // 设备编号
        assertTrue(firstProductNo.contains("001")); // 上机次数001
        
        // 6. 开始打印（无上机次数相关操作）
        StartProcessDTO startDto = new StartProcessDTO();
        startDto.setProcessType("print");
        startDto.setPrimaryDeviceId(37L);
        // ... 调用startProcess
        
        // 7. 验证上机次数未再次累加
        Integer countAfterStart = counterService.getTodayCount(37L);
        assertEquals(1, countAfterStart); // 仍然是1，未变化
    }
    
    @Test
    void test并发场景_多流转卡同时分配同一设备() throws InterruptedException {
        Long deviceId = 37L;
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // 并发分配设备
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    Long recordId = createTestRecord();
                    AssignDeviceDTO dto = new AssignDeviceDTO();
                    dto.setPrintDeviceId(deviceId);
                    recordService.assignDevice(recordId, dto);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        
        // 验证：所有流转卡的产品编号都不重复
        List<ProductionProductEntity> allProducts = productMapper.selectList(null);
        Set<String> productNumbers = allProducts.stream()
            .map(ProductionProductEntity::getProductNo)
            .collect(Collectors.toSet());
        
        assertEquals(allProducts.size(), productNumbers.size());
    }
}
```

### 8.3 边界测试

| 测试场景 | 预期结果 |
|---------|---------|
| 设备编号为001 | 正式编号包含"001" |
| 设备编号为999 | 正式编号包含"999" |
| 上机次数为0 | 正式编号包含"000" |
| 上机次数为999 | 正式编号包含"999" |
| 流转卡有99个产品 | 产品流水号01-99，全部生成成功 |
| 流转卡有1个产品 | 产品流水号01，生成成功 |
| 产品名称为null | 产品代码为"X" |
| 产品名称为空字符串 | 产品代码为"X" |
| 跨天场景（23:59分配，00:01打印） | 批号为前一天，上机次数为当天的第1次 |

### 8.4 性能测试

**测试目标**：验证系统在高并发场景下的性能表现

**测试场景1：设备上机次数并发累加**

- **并发数**：50
- **操作**：同时调用 `incrementAndGet()`
- **预期**：
  - 最终计数值 = 50
  - 所有请求在3秒内完成
  - 乐观锁重试率 < 10%

**测试场景2：批量生成产品编号**

- **数据量**：100个流转卡，每个10个产品
- **操作**：批量调用 `generateFormalNumbers()`
- **预期**：
  - 全部编号唯一
  - 总耗时 < 5秒
  - 数据库连接池无溢出

### 8.5 测试检查清单

**代码层面**：
- [ ] ProductNumberService 所有公共方法有单元测试
- [ ] DeviceUsageCounterService 所有公共方法有单元测试
- [ ] 产品代码映射规则全覆盖（A/B/C/D/X）
- [ ] 编号格式校验覆盖所有边界值
- [ ] 乐观锁冲突重试逻辑已测试
- [ ] 事务回滚场景已测试

**业务层面**：
- [ ] 临时编号 → 正式编号的完整流程可运行
- [ ] 分配设备后产品编号格式正确
- [ ] 开始打印后上机次数正确累加
- [ ] 产品排序规则（sort_order + 创建时间）正确
- [ ] 跨天场景编号生成正常
- [ ] 并发场景无编号重复

**异常场景**：
- [ ] 流转卡不存在时抛出正确异常
- [ ] 设备不存在时抛出正确异常
- [ ] 设备编号格式错误时抛出正确异常
- [ ] 产品编号重复时抛出正确异常并回滚
- [ ] 流转卡无产品时抛出正确异常
- [ ] 乐观锁冲突超过重试次数时抛出正确异常

---

## 9. 实施计划

### 9.1 实施步骤

**阶段1：数据库准备（预计0.5天）**

1. 创建 `device_daily_usage_counter` 表
2. 验证表结构和索引
3. 清理设备表数据（确保 `device_id` 为1-999的整数）

**阶段2：核心服务开发（预计2天）**

1. 实现 `DeviceUsageCounterEntity` 和 `DeviceUsageCounterMapper`
2. 实现 `DeviceUsageCounterServiceImpl`
3. 实现 `ProductNumberServiceImpl`
4. 编写单元测试

**阶段3：集成修改（预计1天）**

1. 修改 `ProductionRecordServiceImpl.assignDevice()`
2. 添加错误码到 `ErrorCodeEnum`
3. 编写集成测试

**阶段4：测试验证（预计1天）**

1. 单元测试全覆盖
2. 集成测试通过
3. 边界场景测试
4. 并发场景测试

**阶段5：上线部署（预计0.5天）**

1. 执行数据库DDL
2. 部署新版本代码
3. 验证生产环境功能
4. 监控错误日志

### 9.2 回滚方案

**触发条件**：
- 生产编号重复率 > 0.1%
- 设备上机次数累加失败率 > 1%
- 系统响应时间 > 5秒

**回滚步骤**：
1. 回滚代码到上一版本
2. 临时编号继续使用旧格式（PD-000001）
3. 保留 `device_daily_usage_counter` 表（不影响旧逻辑）
4. 分析问题原因，修复后重新上线

### 9.3 监控指标

**关键指标**：
- 产品编号生成成功率（目标 > 99.9%）
- 设备上机次数累加成功率（目标 > 99%）
- 编号生成平均耗时（目标 < 100ms）
- 乐观锁冲突重试率（目标 < 5%）

**告警规则**：
- 编号重复 → 立即告警
- 上机次数累加失败率 > 1% → 告警
- 编号生成耗时 > 500ms → 告警

---

## 10. 附录

### 10.1 编号示例

| 场景 | 编号示例 | 说明 |
|------|---------|------|
| 基本场景 | 260630B03700201 | 2026-06-30，骨模型，037设备，第2次上机，第1个产品 |
| 不同产品类型 | 260630A03700201 | 产品代码A（手术导板） |
| 不同设备 | 260630B00100201 | 设备001 |
| 高上机次数 | 260630B03712301 | 第123次上机 |
| 多产品流转卡 | 260630B03700210 | 第10个产品 |

### 10.2 数据库查询示例

**查询设备当日上机次数**：

```sql
SELECT usage_count
FROM device_daily_usage_counter
WHERE device_id = 37 AND usage_date = CURDATE();
```

**查询某批次所有产品编号**：

```sql
SELECT product_no, product_name
FROM production_product
WHERE product_no LIKE '260630%'
ORDER BY product_no;
```

**统计产品编号分布**：

```sql
SELECT 
    SUBSTRING(product_no, 7, 1) AS product_code,
    COUNT(*) AS count
FROM production_product
WHERE LENGTH(product_no) = 15
GROUP BY product_code;
```

### 10.3 相关文档

- 《医工宝生产模块需求分析v1》
- 《Java编码规范》
- 《日志规范》
- 《数据库设计规范》

---

**文档状态**：✅ 已完成  
**审核人**：待指定  
**审核日期**：待定

