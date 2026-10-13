# 订单修改功能重构设计文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档标题 | 订单修改功能重构 - 全量Diff方案 |
| 创建日期 | 2026-05-22 |
| 作者 | hanjor |
| 状态 | 设计中 |
| 版本 | 1.0 |

---

## 一、背景与问题

### 1.1 当前实现问题

当前订单修改接口 `PUT /order/modify/{orderId}/direct` 存在以下问题：

**问题1：前端参数传递复杂**
- 前端需要传递字段级别的修改数据：`{field: "hospitalDeptId", value: 123}`
- 修改关联对象（如科室）时，需要同时传递多个关联字段（科室ID、科室名称）
- 前端需要追踪哪些字段被修改，实现复杂

**问题2：修改日志过于细碎**
- 修改一个重建项目会为每个字段生成一条日志记录
- 例如修改项目的需求描述、成型需求、其他需求，会生成3条日志
- 日志记录缺乏业务语义，难以快速理解变更内容

**问题3：配置粒度不合理**
- 当前按字段级别配置可修改内容
- 业务上更关注业务对象级别的权限控制（如"患者信息"、"医生信息"）
- 字段级配置维护成本高，扩展性差

### 1.2 用户需求

1. **前端传参简化**：前端传入完整订单数据（类似创建订单），后端判断变更
2. **日志粒度优化**：按业务对象记录变更，一个对象的所有变化合并为一条日志
3. **配置粒度调整**：按业务对象标识配置可修改内容（patient、doctor、items等）
4. **详细变更记录**：日志需要记录具体的变更内容，包括描述字段的变化

---

## 二、设计目标

### 2.1 核心目标

1. **简化前端实现**：前端无需追踪变更，直接提交完整表单数据
2. **优化日志记录**：按业务对象记录，包含完整变更信息
3. **灵活配置管理**：按业务对象配置权限，易于维护和扩展
4. **保持向后兼容**：新建接口，不影响现有功能

### 2.2 非目标

- 不改动现有的 `/order/modify/{orderId}/direct` 接口
- 不改动现有的修改留痕表结构
- 不涉及审批流程的变更

---

## 三、方案设计

### 3.1 总体方案

**方案名称**：全量Diff方案

**核心思路**：
1. 前端传入完整订单数据（复用 CreateOrderDTO 结构）
2. 后端查询当前订单完整数据
3. 按业务对象分组进行 diff 对比
4. 根据配置校验变更是否允许
5. 应用变更到数据库
6. 按业务对象记录修改日志（详细模式）
7. 触发后续流转

### 3.2 业务对象定义

| 对象标识 | 对象名称 | 包含字段 |
|---------|---------|---------|
| patient | 患者信息 | patientName, patientGender, patientAge |
| doctor | 医生信息 | doctorId, doctorName, doctorPhone |
| hospital | 医院科室 | hospitalId, hospitalName, hospitalDeptId, hospitalDeptName |
| delivery | 交付信息 | isMailDelivery, deliveryAddress, expectedDeliveryTime, isUrgent |
| items | 重建项目 | 重建项目列表（对比新旧列表） |
| images | 影像文件 | imageDataFileIds, imageReportFileIds |

### 3.3 执行流程

```
1. 接收前端完整订单数据
2. 查询当前订单完整数据（order + items + files）
3. 按业务对象分组 diff 对比
   ├─ patient: 患者信息对比
   ├─ doctor: 医生信息对比
   ├─ hospital: 医院科室对比
   ├─ delivery: 交付信息对比
   ├─ items: 重建项目列表对比
   └─ images: 影像文件对比
4. 过滤无变化的对象
5. 根据配置校验变更是否允许
6. 应用变更到数据库
7. 按对象记录修改日志（详细模式）
8. 递增版本号
9. 触发后续流转
```

---

## 四、接口定义

### 4.1 新增接口

**接口地址**：`PUT /order/modify/{orderId}/full`

**接口说明**：全量修改订单，前端传入完整订单数据，后端自动判断变更内容

**权限要求**：`order:Modify`

**路径参数**：

| 参数名 | 类型 | 说明 |
|--------|------|------|
| orderId | Long | 订单ID |

**请求体**：`OrderModifyFullDTO`（结构见下文）

**响应**：`Result<Void>`

### 4.2 DTO 定义

**OrderModifyFullDTO**（新建）：

```java
@Data
public class OrderModifyFullDTO {
    // 患者信息
    private String patientName;
    private Integer patientGender;
    private Integer patientAge;
    
    // 医生信息
    private Long doctorId;
    private String doctorName;
    private String doctorPhone;
    
    // 医院科室
    private Long hospitalId;
    private Long hospitalDeptId;
    
    // 交付信息
    private Integer isMailDelivery;
    private String deliveryAddress;
    private LocalDateTime expectedDeliveryTime;
    private Integer isUrgent;
    
    // 重建项目（全量替换）
    private List<OrderItemDTO> items;
    
    // 影像文件（全量替换）
    private List<String> imageDataFileIds;
    private List<String> imageReportFileIds;
}
```

**OrderItemDTO**（复用现有）：

```java
@Data
public class OrderItemDTO {
    private Long orderItemId;  // null=新增，非null=修改
    private Long bodyPartId;
    private String bodyPartName;
    private Long projectId;
    private String projectName;
    private String projectDesc;
    private String moldingRequirement;
    private String otherRequirement;
}
```

---


## 五、配置结构

### 5.1 配置格式

配置存储在 `sys_config` 表，key 为 `order.modify.full.config`：

```json
{
  "ORDER": {
    "allowedObjects": ["patient", "doctor", "hospital", "delivery", "items", "images"],
    "objects": {
      "patient": {
        "label": "患者信息",
        "fields": ["patientName", "patientGender", "patientAge"]
      },
      "doctor": {
        "label": "医生信息",
        "fields": ["doctorId", "doctorName", "doctorPhone"]
      },
      "hospital": {
        "label": "医院科室",
        "fields": ["hospitalId", "hospitalDeptId"]
      },
      "delivery": {
        "label": "交付信息",
        "fields": ["isMailDelivery", "deliveryAddress", "expectedDeliveryTime", "isUrgent"]
      },
      "items": {
        "label": "重建项目",
        "coreFields": ["bodyPartId", "projectId"],
        "descFields": ["projectDesc", "moldingRequirement", "otherRequirement"]
      },
      "images": {
        "label": "影像文件"
      }
    }
  },
  "DESIGN": {
    "allowedObjects": ["items"]
  }
}
```

### 5.2 配置说明

- `allowedObjects`: 该阶段允许修改的业务对象列表
- `objects`: 每个对象的详细配置
  - `label`: 用于日志记录的中文名称
  - `fields`: 该对象包含的字段列表（用于 diff 对比）
  - `coreFields`: 重建项目的核心字段（影响项目本质）
  - `descFields`: 重建项目的描述字段（补充说明）

---

## 六、Diff 算法设计

### 6.1 简单对象 Diff

**适用对象**：patient、doctor、hospital、delivery

**算法逻辑**：

1. 提取旧值和新值（按配置的 fields 列表）
2. 对比是否有变化
3. 生成变更描述

**格式化示例**：
- 患者信息：`张三(男,45岁)` → `李四(女,38岁)`
- 医生信息：`张医生(138xxx)` → `李医生(139xxx)`
- 医院科室：`北京协和医院-骨科` → `北京协和医院-神经外科`
- 交付信息：`不邮寄,不加急` → `邮寄至北京市xxx,加急`

---

### 6.2 重建项目 Diff（详细模式）

**核心思路**：
1. 按 orderItemId 匹配新旧项目
2. 分别对比核心字段和描述字段
3. 生成详细的变更描述

**核心字段对比**：
- 检查项目名称是否变化（部位或产品变化会导致项目名称变化）
- 示例：`膝关节假体` → `髋关节假体`

**描述字段对比**：
- 逐个检查：projectDesc、moldingRequirement、otherRequirement
- 示例：`需求描述：常规假体→定制假体`

**日志记录示例**：

```
重建项目变更：
修改[膝关节假体→髋关节假体，需求描述：常规假体→定制假体，特殊材料]；
修改[肩关节假体：需求描述：定制假体→定制假体，加急处理]；
新增[踝关节假体]；
删除[腕关节假体]
```

**字段值格式化**：
- 空值显示为"无"
- 超过20字符的内容截取前20字符+"..."

---

### 6.3 影像文件 Diff

**算法逻辑**：
1. 查询当前影像数据文件和影像报告文件
2. 对比新旧文件ID列表
3. 生成变更描述

**日志记录示例**：
- `影像文件：影像数据3个→5个，影像报告1个→2个`

---

## 七、日志记录格式

### 7.1 日志表结构

复用现有的 `order_modification_log` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| order_id | Long | 订单ID |
| order_code | String | 订单编号 |
| field_name | String | 对象标识（patient/doctor/items等） |
| field_label | String | 对象名称（患者信息/医生信息等） |
| old_value | String | 旧值描述 |
| new_value | String | 新值描述 |
| modifier_id | Long | 修改人ID |
| modifier_name | String | 修改人姓名 |
| create_time | DateTime | 修改时间 |

### 7.2 日志记录示例

| field_name | field_label | old_value | new_value |
|-----------|-------------|-----------|-----------|
| patient | 患者信息 | 张三(男,45岁) | 李四(女,38岁) |
| doctor | 医生信息 | 张医生(138xxx) | 李医生(139xxx) |
| delivery | 交付信息 | 不邮寄,不加急 | 邮寄至北京市xxx,加急 |
| items | 重建项目 | 2个项目 | 修改[膝关节→髋关节]，新增[踝关节] |
| images | 影像文件 | 数据3个,报告1个 | 数据5个,报告2个 |

---

## 八、实现要点

### 8.1 核心类设计

**新增类**：

1. `OrderModifyFullDTO` - 请求DTO
2. `OrderModifyFullService` - 业务逻辑Service
3. `OrderModifyFullController` - 控制器（新增方法）
4. `ObjectChange` - 对象变更记录
5. `ModifyFullConfig` - 配置DTO

**复用类**：

1. `OrderDataValidator` - 数据校验
2. `OrderModificationLogEntity` - 修改日志实体
3. `FlowFacade` - 流程流转

### 8.2 关键方法

**Service 主方法**：

```java
@Transactional(rollbackFor = Exception.class)
public void modifyOrderFull(Long orderId, OrderModifyFullDTO dto) {
    // 1. 查询当前订单
    // 2. 按对象 diff
    // 3. 过滤无变化
    // 4. 校验变更
    // 5. 应用变更
    // 6. 记录日志
    // 7. 递增版本号
    // 8. 触发流转
}
```

### 8.3 数据校验

**校验点**：

1. 订单存在性校验
2. 订单阶段校验（ORDER/DESIGN）
3. 变更对象权限校验（根据配置）
4. 关联数据存在性校验（医院、医生、部位、项目等）
5. 重建项目不能为空校验

### 8.4 事务管理

- 整个修改过程在一个事务中完成
- 使用 `@Transactional(rollbackFor = Exception.class)`
- 任何异常都会回滚所有变更

### 8.5 并发控制

- 使用订单版本号（version字段）实现乐观锁
- 修改完成后递增版本号
- 使持有旧版本的审核操作失效

---

## 九、测试计划

### 9.1 单元测试

**测试类**：`OrderModifyFullServiceTest`

**测试场景**：

1. **简单对象修改**
   - 修改患者信息
   - 修改医生信息
   - 修改交付信息

2. **重建项目修改**
   - 修改项目核心字段（部位、产品）
   - 修改项目描述字段（需求描述、成型需求）
   - 新增项目
   - 删除项目
   - 混合操作（新增+修改+删除）

3. **影像文件修改**
   - 替换影像数据文件
   - 替换影像报告文件

4. **混合修改**
   - 同时修改多个对象

5. **异常场景**
   - 订单不存在
   - 订单阶段不允许修改
   - 变更对象不在允许列表
   - 重建项目为空

### 9.2 集成测试

**测试场景**：

1. 完整流程测试（订单阶段修改 → 审核通过）
2. 完整流程测试（设计阶段修改 → 重新审核）
3. 并发修改测试（版本号冲突）

---

## 十、实施计划

### 10.1 开发任务

| 任务 | 说明 | 预计工时 |
|------|------|---------|
| 1. 创建DTO类 | OrderModifyFullDTO、ModifyFullConfig、ObjectChange | 0.5h |
| 2. 实现配置加载 | 从sys_config加载配置，解析JSON | 0.5h |
| 3. 实现简单对象Diff | patient、doctor、hospital、delivery | 1h |
| 4. 实现重建项目Diff | 核心字段+描述字段对比 | 2h |
| 5. 实现影像文件Diff | 文件列表对比 | 0.5h |
| 6. 实现变更应用逻辑 | 将变更写入数据库 | 1.5h |
| 7. 实现日志记录 | 按对象记录修改日志 | 0.5h |
| 8. 实现Controller | 新增接口方法 | 0.5h |
| 9. 编写单元测试 | 覆盖主要场景 | 2h |
| 10. 编写集成测试 | 完整流程测试 | 1h |
| 11. 更新接口文档 | 补充新接口说明 | 0.5h |

**总计**：约 10.5 小时

### 10.2 实施步骤

**阶段1：基础准备**（1h）
1. 创建DTO类
2. 实现配置加载
3. 在sys_config表中添加配置数据

**阶段2：核心功能**（5h）
1. 实现各类对象的Diff算法
2. 实现变更应用逻辑
3. 实现日志记录

**阶段3：接口层**（0.5h）
1. 在OrderModifyApplyController中新增方法
2. 配置路由和权限

**阶段4：测试**（3h）
1. 编写单元测试
2. 编写集成测试
3. 手动测试验证

**阶段5：文档**（1h）
1. 更新接口文档
2. 更新CLAUDE.md（如需要）

### 10.3 风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| 配置格式复杂 | 开发难度增加 | 提供配置示例，编写配置校验逻辑 |
| Diff算法性能 | 大订单修改慢 | 优化查询，使用索引，限制项目数量 |
| 日志记录过多 | 数据库压力 | 合理控制日志详细度，定期归档 |
| 前端适配成本 | 推广困难 | 保留旧接口，逐步迁移 |

---

## 十一、总结

### 11.1 方案优势

1. **前端实现简单**：无需追踪变更，直接提交表单数据
2. **日志记录清晰**：按业务对象记录，包含完整变更信息
3. **配置灵活易维护**：按业务对象配置，易于扩展
4. **向后兼容**：新建接口，不影响现有功能

### 11.2 后续优化

1. 支持部分传参（只传修改的对象）
2. 支持批量修改多个订单
3. 支持修改历史对比查看
4. 支持修改审批流程（如需要）

---

**文档状态**：设计完成，待审查

**下一步**：用户审查设计文档 → 开始实施

