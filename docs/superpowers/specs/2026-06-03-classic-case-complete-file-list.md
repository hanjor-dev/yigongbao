# 经典案例功能 - 完整文件迁移清单

## 文档信息
- **创建时间**：2026-06-03
- **目的**：确保经典案例文件迁移覆盖所有相关文件，无遗漏

---

## 第一部分：订单模块文件

### 1.1 order_file（订单文件关联表）

**表结构关键字段**：
- `file_id` VARCHAR(32) - 关联file_detail.id
- `file_category` - 文件类别（影像数据、影像报告、订单附件等）

**文件来源**：
- 影像数据（dict_code: 10.1）
- 影像报告（dict_code: 10.2）
- 订单其他附件（dict_code: 10.3）
- 免费业务审批文件（dict_code: 10.20）

**迁移策略**：
- 通过`order_id`查询所有`file_id`
- 从`file_detail`表获取文件路径信息

---

## 第二部分：设计模块文件

### 2.1 design_package（数据包表）

**表结构关键字段**：
- `file_id` VARCHAR(32) - 数据包压缩文件ID

**文件来源**：
- 设计师上传的打印文件压缩包（ZIP/RAR等）

**迁移策略**：
- 通过`order_id`查询所有数据包记录
- 收集每个数据包的`file_id`

### 2.2 design_package_file（数据包内文件表）⚠️ 之前遗漏

**表结构关键字段**：
- `file_id` VARCHAR(64) - 包内文件在OSS的文件ID
- `file_url` VARCHAR(1024) - 包内文件独立OSS访问地址
- `package_id` BIGINT - 关联的数据包ID

**文件来源**：
- 从压缩包解压出的单个STL/3MF/OBJ文件
- 每个文件单独上传到OSS并记录

**迁移策略**：
- 通过`design_package.order_id`查询所有数据包
- 再通过`package_id`查询所有`design_package_file`记录
- 收集每个文件的`file_id`

**重要性**：⭐⭐⭐ 高优先级，这些是实际的打印文件

### 2.3 design_package_file_screenshot（文件截图表）⚠️ 之前遗漏

**表结构关键字段**：
- `file_id` VARCHAR(64) - 截图文件ID
- `package_file_id` BIGINT - 关联的数据包文件ID

**文件来源**：
- 数据包文件的预览截图（PNG/JPG等）

**迁移策略**：
- 通过`design_package_file`的ID查询截图记录
- 收集每个截图的`file_id`

**重要性**：⭐⭐ 中等优先级，用于文件预览

### 2.4 design_product_file（产品关联文件表）

**表结构关键字段**：
- `design_product_id` BIGINT
- `package_file_id` BIGINT - 关联design_package_file.id

**说明**：
- 这是关联表，不直接存储文件ID
- 文件已在2.2中通过design_package_file收集
- 无需单独处理

### 2.5 design_model（3D模型文件表）

**表结构关键字段**：
- `file_id` VARCHAR(32) - 3D模型文件ID

**文件来源**：
- 可视化3D模型文件（用于在线预览）

**迁移策略**：
- 通过`order_id`查询所有模型记录
- 收集每个模型的`file_id`

**重要性**：⭐⭐⭐ 高优先级

### 2.6 design_instruction（指令单表）

**表结构关键字段**：
- `template_file_id` VARCHAR(32) - 模板文件ID（系统生成）
- `template_file_url` VARCHAR(512) - 模板文件URL
- `revised_file_id` VARCHAR(32) - 修订版文件ID（设计师上传）
- `revised_file_url` VARCHAR(512) - 修订版文件URL

**文件来源**：
- 系统自动生成的指令单模板（Excel/PDF）
- 设计师手动上传的修订版指令单

**迁移策略**：
- 通过`design_package.order_id`查询所有数据包
- 通过`package_id`查询指令单记录
- 收集`template_file_id`和`revised_file_id`（都可能存在）

**重要性**：⭐⭐⭐ 高优先级

### 2.7 design_drawing（图纸表）

**表结构关键字段**：
- `template_file_id` VARCHAR(32) - 模板文件ID
- `template_file_url` VARCHAR(512) - 模板文件URL
- `revised_file_id` VARCHAR(32) - 修订版文件ID
- `revised_file_url` VARCHAR(512) - 修订版文件URL

**文件来源**：
- 系统自动生成的图纸模板（PDF）
- 设计师手动上传的修订版图纸

**迁移策略**：
- 通过`design_package.order_id`查询所有数据包
- 通过`package_id`查询图纸记录
- 收集`template_file_id`和`revised_file_id`

**重要性**：⭐⭐⭐ 高优先级

---

## 第三部分：生产模块文件

### 3.1 production_record（生产流转卡表）⚠️ 之前遗漏

**表结构关键字段**：
- `flow_card_file_url` VARCHAR(500) - 流转卡Excel文件URL
- `order_id` BIGINT - 关联订单

**文件来源**：
- 系统生成的生产流转卡Excel文件

**⚠️ 重要发现**：
- 该表只有`flow_card_file_url`字段，**没有file_id字段**
- 文件URL直接存储，可能没有在file_detail表中记录
- 需要特殊处理：解析URL获取文件路径

**迁移策略**：
- 通过`order_id`查询所有生产记录
- 收集`flow_card_file_url`
- 解析URL提取文件路径
- 迁移文件并更新URL字段

**重要性**：⭐⭐⭐ 高优先级

---

## 第四部分：完整文件收集策略

### 4.1 通过file_detail表收集的文件

```sql
-- 1. 订单文件
SELECT file_id FROM order_file WHERE order_id = ?

-- 2. 数据包压缩文件
SELECT file_id FROM design_package WHERE order_id = ?

-- 3. 数据包内文件 ⚠️
SELECT dpf.file_id 
FROM design_package dp
JOIN design_package_file dpf ON dp.id = dpf.package_id
WHERE dp.order_id = ?

-- 4. 文件截图 ⚠️
SELECT dpfs.file_id
FROM design_package dp
JOIN design_package_file dpf ON dp.id = dpf.package_id
JOIN design_package_file_screenshot dpfs ON dpf.id = dpfs.package_file_id
WHERE dp.order_id = ?

-- 5. 3D模型
SELECT file_id FROM design_model WHERE order_id = ?

-- 6. 指令单（模板+修订版）
SELECT template_file_id, revised_file_id
FROM design_instruction di
JOIN design_package dp ON di.package_id = dp.id
WHERE dp.order_id = ?

-- 7. 图纸（模板+修订版）
SELECT template_file_id, revised_file_id
FROM design_drawing dd
JOIN design_package dp ON dd.package_id = dp.id
WHERE dp.order_id = ?
```

### 4.2 通过URL字段收集的文件 ⚠️ 特殊处理

```sql
-- 8. 生产流转卡文件
SELECT flow_card_file_url 
FROM production_record 
WHERE order_id = ?
```

**特殊说明**：
- 该表没有file_id字段，只有URL
- 需要解析URL提取文件路径
- 迁移后更新production_record表的flow_card_file_url字段

---

## 第五部分：需要更新的表和字段

### 5.1 通过file_detail表更新的（批量更新）

**更新file_detail表**：
- 更新`path`字段：改为`classic-cases/{orderCode}/xxx`
- 更新`base_path`字段（如果使用）
- 更新`url`字段：新的访问地址

**影响的业务表**（只读file_id，不需要更新）：
- order_file
- design_package
- design_package_file ⚠️
- design_package_file_screenshot ⚠️
- design_model
- design_instruction
- design_drawing

### 5.2 需要单独更新URL字段的表 ⚠️

**production_record表**：
- 字段：`flow_card_file_url`
- 更新策略：解析旧URL，构建新URL，更新字段

**design_package_file表**：
- 字段：`file_url`
- 更新策略：虽然有file_id，但也冗余了URL，需要同步更新

---

## 第六部分：实现建议

### 6.1 文件迁移的两种实现方式

**方式一：通过file_detail统一迁移（推荐）**
```java
// 1. 收集所有file_id
List<String> fileIds = collectAllFileIds(orderId);

// 2. 批量查询file_detail
List<FileDetail> files = fileDetailMapper.selectBatchIds(fileIds);

// 3. 批量迁移文件并更新file_detail
for (FileDetail file : files) {
    String newPath = "classic-cases/" + orderCode + "/" + getBusinessType(file);
    // 调用x-file-storage迁移文件
    // 更新file_detail表
}
```

**方式二：特殊处理URL字段**
```java
// production_record的flow_card_file_url
String oldUrl = record.getFlowCardFileUrl();
String newUrl = migrateFileByUrl(oldUrl, orderCode);
// 更新production_record表
```

### 6.2 关键代码片段

```java
private List<String> collectAllFileIds(Long orderId) {
    List<String> fileIds = new ArrayList<>();
    
    // 1. 订单文件
    fileIds.addAll(orderFileMapper.selectFileIdsByOrderId(orderId));
    
    // 2. 数据包文件
    fileIds.addAll(designPackageMapper.selectFileIdsByOrderId(orderId));
    
    // 3. 数据包内文件 ⚠️ 新增
    fileIds.addAll(designPackageFileMapper.selectFileIdsByOrderId(orderId));
    
    // 4. 文件截图 ⚠️ 新增
    fileIds.addAll(designPackageFileScreenshotMapper.selectFileIdsByOrderId(orderId));
    
    // 5. 3D模型
    fileIds.addAll(designModelMapper.selectFileIdsByOrderId(orderId));
    
    // 6. 指令单
    fileIds.addAll(designInstructionMapper.selectFileIdsByOrderId(orderId));
    
    // 7. 图纸
    fileIds.addAll(designDrawingMapper.selectFileIdsByOrderId(orderId));
    
    // 去重
    return fileIds.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
}
```

---

## 第七部分：总结

### 7.1 遗漏的文件来源（共3处）

1. ⚠️ **design_package_file表** - 数据包内的STL等打印文件
2. ⚠️ **design_package_file_screenshot表** - 文件预览截图
3. ⚠️ **production_record表** - 生产流转卡Excel文件

### 7.2 文件总数估算

一个典型订单的文件数量：
- 订单文件：5-10个（影像数据、报告等）
- 数据包文件：1-3个（压缩包）
- 数据包内文件：10-30个（STL文件）⚠️
- 文件截图：10-30个 ⚠️
- 3D模型：1-3个
- 指令单：2-4个（模板+修订版）
- 图纸：2-4个（模板+修订版）
- 流转卡：1-3个 ⚠️

**总计：约30-90个文件/订单**

### 7.3 实施优先级

**P0（必须）**：
- design_package_file - 核心打印文件
- production_record - 生产流转卡

**P1（重要）**：
- design_package_file_screenshot - 文件预览

---

**文档完成**
