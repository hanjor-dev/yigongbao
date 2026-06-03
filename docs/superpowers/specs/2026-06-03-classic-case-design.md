# 订单经典案例功能详细设计

## 文档信息

- **创建时间**：2026-06-03
- **文档版本**：v1.0
- **设计者**：Kiro
- **评审状态**：待评审

## 变更历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| v1.0 | 2026-06-03 | Kiro | 初始版本 |

## 目录

1. [需求概述](#1-需求概述)
2. [数据库设计](#2-数据库设计)
3. [实体类设计](#3-实体类设计)
4. [DTO/VO设计](#4-dtovo设计)
5. [Service层设计](#5-service层设计)
6. [Controller层设计](#6-controller层设计)
7. [文件迁移服务设计](#7-文件迁移服务设计)
8. [删除和修改保护设计](#8-删除和修改保护设计)
9. [错误码定义](#9-错误码定义)
10. [测试用例](#10-测试用例)

---

## 1. 需求概述

### 1.1 功能需求

订单模块需要增加"经典案例"功能，核心需求包括：

1. **标记功能**：支持将已完成订单标记为经典案例
2. **查询功能**：提供经典案例的快速查询入口
3. **保护机制**：经典案例的所有数据**不可删除、不可修改**
4. **文件迁移**：所有关联文件转移到经典案例专用目录

### 1.2 保护范围

经典案例的保护范围包括：

**订单模块**：
- order_main（订单主表）
- order_item（订单明细）
- order_file（订单文件）

**设计模块**：
- design_package（数据包）
- design_product（设计产品）
- design_model（3D模型）
- design_review（设计审核）
- design_instruction（指令单）
- design_drawing（图纸）

**生产模块**：
- production_process（生产流程）

**文件模块**：
- file_detail（文件详情）

### 1.3 文件迁移规则

**原路径格式**：
```
/{platform}/{basePath}/{path}/{filename}
```

**新路径格式**：
```
/{platform}/classic-cases/{orderCode}/{业务类型}/{filename}
```

**示例**：
```
原：/aliyun-oss/orders/2024/03/order_123_file.pdf
新：/aliyun-oss/classic-cases/ORD202403001/order-files/order_123_file.pdf
```

---

## 2. 数据库设计

### 2.1 order_main表字段扩展

在`order_main`表中增加以下字段：

```sql
ALTER TABLE order_main 
ADD COLUMN is_classic_case TINYINT DEFAULT 0 COMMENT '是否经典案例：0-否，1-是';

ALTER TABLE order_main 
ADD COLUMN classic_case_time DATETIME COMMENT '标记为经典案例的时间';

ALTER TABLE order_main 
ADD COLUMN classic_case_by BIGINT COMMENT '标记为经典案例的操作人ID';

ALTER TABLE order_main 
ADD COLUMN classic_case_remark VARCHAR(500) COMMENT '标记为经典案例的备注';
```

### 2.2 索引创建

为经典案例查询创建复合索引：

```sql
CREATE INDEX idx_order_classic_case 
ON order_main(is_classic_case, create_time DESC);
```

**索引说明**：
- `is_classic_case`：快速过滤经典案例
- `create_time DESC`：按创建时间倒序排列

### 2.3 字段约束

| 字段名 | 类型 | 允许NULL | 默认值 | 说明 |
|--------|------|----------|--------|------|
| is_classic_case | TINYINT | NO | 0 | 0=普通订单，1=经典案例 |
| classic_case_time | DATETIME | YES | NULL | 标记时间（标记时自动填充） |
| classic_case_by | BIGINT | YES | NULL | 操作人ID（从SaToken获取） |
| classic_case_remark | VARCHAR(500) | YES | NULL | 标记原因/备注 |

---

## 3. 实体类设计

### 3.1 OrderMainEntity扩展

在现有`OrderMainEntity`中增加以下字段：

```java
/**
 * 是否经典案例：0-否，1-是
 */
private Integer isClassicCase;

/**
 * 标记为经典案例的时间
 */
private LocalDateTime classicCaseTime;

/**
 * 标记为经典案例的操作人ID
 */
private Long classicCaseBy;

/**
 * 标记为经典案例的备注
 */
private String classicCaseRemark;
```

**说明**：
- 继承`BaseEntity`的公共字段（id, createTime等）保持不变
- 新增字段遵循项目命名规范

---

## 4. DTO/VO设计

### 4.1 MarkClassicCaseDTO（标记请求DTO）

```java
package com.yigongbao.module.order.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 标记订单为经典案例请求DTO
 */
@Data
public class MarkClassicCaseDTO {
    
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    
    @NotBlank(message = "备注不能为空")
    private String remark;
}
```

### 4.2 ClassicCaseQueryDTO（查询请求DTO）

```java
package com.yigongbao.module.order.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 经典案例查询请求DTO
 */
@Data
public class ClassicCaseQueryDTO {
    
    private String orderCode;        // 订单编号（模糊查询）
    private String patientName;      // 患者姓名
    private Long hospitalId;         // 医院ID
    private Long bodyPartId;         // 部位ID
    private LocalDateTime startTime; // 标记开始时间
    private LocalDateTime endTime;   // 标记结束时间
    
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
```

### 4.3 ClassicCaseVO（返回VO）

```java
package com.yigongbao.module.order.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 经典案例视图对象VO
 */
@Data
public class ClassicCaseVO {
    
    private Long id;
    private String orderCode;
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String hospitalName;
    private String bodyPartName;
    private String projectName;
    private LocalDateTime classicCaseTime;
    private String classicCaseRemark;
    private LocalDateTime createTime;
}
```

---

## 5. Service层设计

### 5.1 IOrderClassicCaseService接口

```java
package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.vo.ClassicCaseVO;

/**
 * 订单经典案例服务接口
 */
public interface IOrderClassicCaseService {
    
    /**
     * 标记订单为经典案例
     */
    void markAsClassicCase(MarkClassicCaseDTO dto);
    
    /**
     * 查询经典案例列表（分页）
     */
    IPage<ClassicCaseVO> listClassicCases(ClassicCaseQueryDTO dto);
    
    /**
     * 获取经典案例详情
     */
    ClassicCaseVO getClassicCaseDetail(Long orderId);
    
    /**
     * 检查订单是否为经典案例
     */
    boolean isClassicCase(Long orderId);
}
```

### 5.2 核心方法实现逻辑

**markAsClassicCase() 实现流程**：
1. 验证订单状态（必须是已完成状态：phase=80）
2. 检查是否已标记为经典案例（幂等性）
3. 更新order_main表字段
4. 调用文件迁移服务
5. 记录操作日志

**listClassicCases() 实现逻辑**：
1. 构建查询条件（is_classic_case=1）
2. 执行分页查询
3. 转换Entity为VO
4. 返回分页结果

---

## 6. Controller层设计

### 6.1 OrderClassicCaseController

```java
package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.service.IOrderClassicCaseService;
import com.yigongbao.module.order.vo.ClassicCaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单经典案例Controller
 */
@RestController
@RequestMapping("/order/classic-case")
@RequiredArgsConstructor
public class OrderClassicCaseController {
    
    private final IOrderClassicCaseService classicCaseService;
    
    @PostMapping("/mark")
    public Result<Void> markAsClassicCase(@RequestBody @Validated MarkClassicCaseDTO dto) {
        classicCaseService.markAsClassicCase(dto);
        return Result.success();
    }
    
    @PostMapping("/list")
    public Result<IPage<ClassicCaseVO>> list(@RequestBody ClassicCaseQueryDTO dto) {
        return Result.success(classicCaseService.listClassicCases(dto));
    }
    
    @GetMapping("/{orderId}")
    public Result<ClassicCaseVO> detail(@PathVariable Long orderId) {
        return Result.success(classicCaseService.getClassicCaseDetail(orderId));
    }
}
```

### 6.2 接口清单

| 接口路径 | 请求方式 | 说明 |
|---------|---------|------|
| /order/classic-case/mark | POST | 标记订单为经典案例 |
| /order/classic-case/list | POST | 查询经典案例列表 |
| /order/classic-case/{orderId} | GET | 获取经典案例详情 |

---

## 7. 文件迁移服务设计

### 7.1 ClassicCaseFileService接口

```java
package com.yigongbao.module.order.service;

import java.util.List;

/**
 * 经典案例文件迁移服务
 */
public interface IClassicCaseFileService {
    
    /**
     * 收集订单所有关联文件ID
     */
    List<String> collectOrderFileIds(Long orderId);
    
    /**
     * 批量迁移文件到经典案例目录
     */
    void migrateFilesToClassicCase(Long orderId, String orderCode);
}
```

### 7.2 文件收集逻辑

需要从以下表收集fileId：
1. `order_file` - 订单文件（影像数据、报告、附件等）
2. `design_package` - 数据包压缩文件（ZIP/RAR等）
3. `design_package_file` - ⚠️ 数据包内文件（STL/3MF/OBJ等打印文件）**【之前遗漏】**
4. `design_package_file_screenshot` - ⚠️ 文件预览截图（PNG/JPG等）**【之前遗漏】**
5. `design_model` - 3D模型文件（用于在线预览）
6. `design_instruction` - 指令单文件（template_file_id + revised_file_id）
7. `design_drawing` - 图纸文件（template_file_id + revised_file_id）

**特殊处理**：
8. `production_record.flow_card_file_url` - ⚠️ 生产流转卡Excel文件 **【之前遗漏】**
   - 该表只有URL字段，**没有file_id字段**
   - 需要解析URL提取文件路径，迁移后更新URL字段

### 7.3 迁移实现策略

#### 方式一：通过file_detail表迁移（适用于1-7项）

**步骤1**：批量查询file_detail表获取文件路径信息
**步骤2**：调用x-file-storage复制文件到新路径（保留原文件）
**步骤3**：批量更新file_detail表的path、basePath、url字段
**步骤4**：事务提交后异步删除原文件
**步骤5**：失败时回滚数据库，不删除原文件

**特别注意**：`design_package_file`表既有`file_id`又冗余存储了`file_url`字段，迁移时需要同步更新该表的`file_url`字段

#### 方式二：通过URL字段迁移（适用于production_record）

**步骤1**：查询production_record获取flow_card_file_url
**步骤2**：解析URL提取文件路径（如：/orders/2024/03/flow_card.xlsx）
**步骤3**：调用x-file-storage复制文件到classic-cases目录
**步骤4**：构建新URL并更新production_record.flow_card_file_url字段
**步骤5**：事务提交后异步删除原文件
**步骤6**：失败时回滚数据库，不删除原文件

**事务边界**：两种方式的迁移操作都在同一个@Transactional方法中执行

---

## 8. 删除和修改保护设计

### 8.1 保护方法

在`OrderServiceImpl`中增加统一检查方法：

```java
/**
 * 检查订单是否为经典案例，是则抛出异常
 */
protected void checkNotClassicCase(Long orderId, String operation) {
    OrderMainEntity order = getById(orderId);
    if (order != null && StatusConstants.YES.equals(order.getIsClassicCase())) {
        log.warn("尝试{}经典案例订单: orderId={}", operation, orderId);
        throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_PROTECTED);
    }
}
```

### 8.2 需要增加保护的方法

**订单模块**（OrderServiceImpl）：
- `removeOrder(Long orderId)` - 删除订单前检查
- `updateOrder(UpdateOrderDTO dto)` - 更新订单前检查
- `removeOrderItem(Long itemId)` - 删除订单项前检查

**设计模块**（DesignPackageServiceImpl等）：
- 所有删除方法前增加检查
- 所有更新方法前增加检查

**生产模块**（ProductionProcessServiceImpl等）：
- 所有删除方法前增加检查
- 所有更新方法前增加检查

### 8.3 保护示例

```java
@Override
public void removeOrder(Long orderId) {
    // 检查是否为经典案例
    checkNotClassicCase(orderId, "删除");
    
    // 执行删除逻辑
    removeById(orderId);
}
```

---

## 9. 错误码定义

### 9.1 ErrorCodeEnum扩展

在`ErrorCodeEnum`中增加以下错误码：

```java
// 经典案例相关错误码（620-629）
CLASSIC_CASE_PROTECTED(620, "经典案例订单受保护，不可删除或修改"),
CLASSIC_CASE_ALREADY_MARKED(621, "订单已标记为经典案例"),
CLASSIC_CASE_ORDER_NOT_COMPLETED(622, "只有已完成订单才能标记为经典案例"),
CLASSIC_CASE_FILE_MIGRATE_FAILED(623, "经典案例文件迁移失败"),
```

### 9.2 错误码说明

| 错误码 | 说明 | 使用场景 |
|--------|------|----------|
| 620 | 经典案例受保护 | 尝试删除或修改经典案例时 |
| 621 | 已标记为经典案例 | 重复标记时 |
| 622 | 订单未完成 | 标记未完成订单时 |
| 623 | 文件迁移失败 | 文件迁移过程出错时 |

---

## 10. 测试用例

### 10.1 标记功能测试

| 测试场景 | 输入 | 预期结果 |
|---------|------|----------|
| 正常标记 | orderId=1（已完成订单），remark="典型案例" | 标记成功，is_classic_case=1 |
| 重复标记 | orderId=1（已标记） | 抛出异常621 |
| 未完成订单 | orderId=2（phase≠80） | 抛出异常622 |
| 订单不存在 | orderId=999 | 抛出异常404 |

### 10.2 查询功能测试

| 测试场景 | 输入 | 预期结果 |
|---------|------|----------|
| 查询全部 | is_classic_case=1 | 返回所有经典案例 |
| 按订单号 | orderCode="ORD001" | 返回匹配订单 |
| 按医院 | hospitalId=1 | 返回该医院的经典案例 |
| 分页查询 | pageNum=1, pageSize=10 | 返回第一页10条 |

### 10.3 保护功能测试

| 测试场景 | 输入 | 预期结果 |
|---------|------|----------|
| 删除经典案例订单 | removeOrder(orderId=1) | 抛出异常620 |
| 修改经典案例订单 | updateOrder(orderId=1) | 抛出异常620 |
| 删除经典案例订单项 | removeOrderItem(itemId=1) | 抛出异常620 |
| 删除普通订单 | removeOrder(orderId=2) | 删除成功 |

### 10.4 文件迁移测试

| 测试场景 | 预期结果 |
|---------|----------|
| 迁移订单文件 | 文件复制到新目录，file_detail更新 |
| 迁移设计文件 | 所有设计文件迁移成功 |
| 迁移失败回滚 | 数据库回滚，原文件保留 |

---

## 11. 实施清单

- [ ] 数据库DDL脚本执行
- [ ] OrderMainEntity字段扩展
- [ ] DTO/VO类创建
- [ ] IOrderClassicCaseService接口及实现
- [ ] OrderClassicCaseController创建
- [ ] IClassicCaseFileService接口及实现
- [ ] 删除和修改保护方法增加
- [ ] ErrorCodeEnum错误码扩展
- [ ] 单元测试编写
- [ ] 接口测试
- [ ] 文档更新

---

**文档结束**
