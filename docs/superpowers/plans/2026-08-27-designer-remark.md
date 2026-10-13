# 设计师订单备注实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在设计师“我的工单”操作栏增加“备注”能力，将备注持久化到订单主表，并支持管理员在普通订单导出和自定义订单导出中按订单导出该备注。

**Architecture:** 设计师备注属于订单级的当前备注，不改变订单流程状态，写操作放在设计工单专用 Service 中并校验当前用户是否为订单分配的设计师。备注存储在 `order_main.designer_remark`，通过设计工单 VO 和订单列表 VO 返回；导出链路沿用 `OrderListVO`，新增 `designerRemark` 字段映射。第一期采用覆盖保存，不建立备注历史表。

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8, Apache POI SXSSF, JUnit 5/Mockito, Vue 3/TypeScript。

---

## 一、已确认的需求和边界

### 1. 功能目标

- 设计师在“我的工单”的操作栏看到“备注”按钮。
- 设计师可以新增、修改和清空当前订单备注。
- 备注与订单绑定，不与某一次设计文件、数据包或状态流转绑定。
- 管理员批量导出订单时，可以导出每个订单的设计师备注。
- 自定义导出字段列表中提供“设计师备注”字段。

### 2. 备注语义

本方案将备注定义为“订单当前设计处理备注”，保存新内容时覆盖旧内容，空白内容表示清空备注。

不复用以下字段：

- `audit_remark`：订单审核/驳回原因；
- `design_review_remark`：设计审核备注；
- `classic_case_remark`：经典案例备注；
- `order_designer_assignment_log.remark`：设计师分配日志说明。

第一期不记录修改历史。如果后续需要审计“谁在什么时候写了什么内容”，再独立增加备注历史表，不改变当前字段语义。

### 3. 权限边界

- 设计师只能修改当前分配给自己的订单。
- 管理员可以查看和导出备注；是否可以修改备注不在本需求范围内，第一期不开放管理员写接口。
- 不能只依赖前端隐藏按钮，后端必须重新校验订单归属和数据权限。
- 订单被重新分配后，原备注保留；新设计师可以继续修改同一订单当前备注。

### 4. 状态边界

备注不是流程动作，不触发 `FlowFacade`，不改变 `phase`、`status` 或设计时间。

保存备注的状态白名单固定沿用现有 `DesignQueryHelper.ALLOWED_DESIGN_STATUSES`：`DATA_AUDIT_PASSED(1030)`、`PENDING_DESIGN(2010)`、`DESIGN_IN_PROGRESS(2020)`、`DESIGN_COMPLETED(2030)`。订单取消、生产及后续状态拒绝保存。这样设计完成后的备注仍可补充，且与现有设计工单允许操作状态保持一致。

---

## 二、现状分析和改动边界

设计工单查询使用 `order_main` 作为订单来源，设计工单 Service 将 `OrderMainEntity` 转换为 `DesignWorkorderListVO` 和 `DesignWorkorderDetailVO`。订单导出先查询 `OrderMainEntity`，再通过 `OrderQueryHelper.toOrderListVO` 转换，最后由 `OrderExportServiceImpl` 按字段写 Excel。

因此，`OrderController` 中的三个导出方法只是 HTTP 入口，不需要增加请求参数；核心工作在实体字段、转换逻辑和导出字段映射。

涉及的既有文件：

- `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/entity/OrderMainEntity.java`
- `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignWorkorderController.java`
- `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignWorkorderService.java`
- `yigongbao-parent/yigongbao-module/design/service/impl/DesignWorkorderServiceImpl.java`
- `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/helper/OrderQueryHelper.java`
- `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderExportServiceImpl.java`

注意：当前工作区已有其他未提交改动。实施本方案时只修改本需求涉及文件，不能重置或覆盖已有工作。

---

## 三、数据模型设计

### 1. 新增字段

字段名：`designer_remark`

建议定义：

```sql
designer_remark TEXT NULL COMMENT '设计师备注'
```

使用 `TEXT` 是为了保留多行备注和后续扩展空间；接口层仍应设置明确的最大长度，建议最多 2,000 个字符，避免无边界文本进入导出和页面。

### 2. 数据库脚本

修改或新增以下文件：

- `sql/ddl.sql`：在 `order_main` 的设计/审核字段附近加入字段；
- `sql/ddl-prod.sql`：保持生产初始化结构一致；
- `yigongbao-parent/yigongbao-module-order/src/test/resources/schema.sql`：测试库结构增加字段；
- 新增 `sql/migration-20260827-add-designer-remark.sql`：线上增量迁移，使用 `ALTER TABLE`；
- 如项目有数据库结构差异检查文档，同步记录该字段。

迁移脚本应具备重复执行保护，具体写法遵循项目现有迁移脚本风格。历史订单字段默认 `NULL`，不需要数据回填。

### 3. 实体

在 `OrderMainEntity` 增加：

```java
private String designerRemark;
```

该字段不参与订单创建 DTO、通用订单编辑 DTO 和草稿转换，避免业务员在创建或普通修改订单时注入设计师内部备注。

---

## 四、后端接口设计

### 1. 保存备注接口

建议接口：

```http
PUT /design/workorder/{orderId}/designer-remark
```

请求体：

```json
{
  "remark": "等待补充左侧髋骨数据",
  "version": 3
}
```

新增 DTO：

```java
public class SaveDesignerRemarkDTO {
    @Size(max = 2000, message = "设计师备注不能超过2000字")
    private String remark;

    @NotNull(message = "版本号不能为空")
    private Integer version;
}
```

`remark` 允许为空，用于清空备注；如果 Bean Validation 对空字符串和 `null` 的处理需要区分，应在 Service 中统一 `trim`，空白字符串转换为 `null`。

### 2. Service 边界

在 `DesignWorkorderService` 增加：

```java
void saveDesignerRemark(Long orderId, SaveDesignerRemarkDTO dto);
```

不要复用已废弃或通用的 `OrderMainService.updateOrder`，因为该接口承担订单业务字段修改、时间窗口、经典案例保护等其他语义，复用会导致设计师获得不必要的订单修改能力。

### 3. 保存流程

`DesignWorkorderServiceImpl.saveDesignerRemark` 按以下顺序执行：

1. 查询订单，不存在时返回订单不存在；
2. 获取当前登录用户；
3. 校验 `order.designer_id == currentUserId`；
4. 校验订单未删除且未取消；
5. 校验订单处于允许保存设计师备注的状态；
6. 对备注执行 `trim`，空白内容转 `null`；
7. 使用 `id + designer_id + version` 条件更新 `designer_remark`，并显式执行 `version = version + 1`；
8. 更新影响行数为 0 时返回订单版本冲突；
9. 查询并返回更新后的最新版本号；
10. 记录操作日志。

更新必须使用显式条件，示例：

```java
boolean updated = orderMainService.update(new LambdaUpdateWrapper<OrderMainEntity>()
        .eq(OrderMainEntity::getId, orderId)
        .eq(OrderMainEntity::getDesignerId, currentUserId)
        .eq(OrderMainEntity::getVersion, dto.getVersion())
        .set(OrderMainEntity::getDesignerRemark, normalizedRemark)
        .setSql("version = COALESCE(version, 0) + 1"));
```

当前 `OrderMainEntity.version` 未标注 MyBatis-Plus `@Version`，因此不能假设框架会自动递增。实现必须显式递增版本号，并将接口返回契约改为 `Result<Integer>`，返回更新后的版本号。前端保存成功后使用该版本号，不能继续携带旧版本。

### 4. Controller

在 `DesignWorkorderController` 增加：

- `@PutMapping("/{orderId}/designer-remark")`；
- `@Operation(summary = "保存设计师备注")`；
- `@OperationLog(module = "设计管理", businessType = UPDATE, operation = "保存设计师备注")`；
- `@Valid @RequestBody SaveDesignerRemarkDTO`；
- 返回 `Result<Integer>`，内容为保存后的最新订单版本号。

不将该接口挂在 `OrderController`，因为写权限属于设计工单业务，而非普通订单编辑业务。

---

## 五、设计工单返回和操作栏

### 1. 列表和详情 VO

以下 VO 增加 `designerRemark`：

- `DesignWorkorderListVO`；
- `DesignWorkorderDetailVO`。

转换逻辑增加：

```java
vo.setDesignerRemark(entity.getDesignerRemark());
```

涉及 `DesignWorkorderServiceImpl.toWorkorderListVO` 和 `getWorkorderDetail`。

列表返回备注前必须复用设计工单数据权限校验；详情接口也必须在读取订单前调用 `DesignQueryHelper.checkOrderReadable(orderId)`，避免仅因 VO 增加字段而暴露其他设计师的内部备注。列表查询已通过设计工单数据范围过滤，详情查询需要补齐同等校验。

列表返回备注便于前端点击“备注”时直接回显；详情同时返回，保持两种入口的语义一致。

### 2. 操作栏

“备注”不是状态机动作，不加入 `FlowActionEnum`，也不建议塞入 `/order/{id}/actions` 的流程动作列表。

推荐前端将其作为设计工单固定能力：

```text
查看 | 开始设计 | 完成设计 | 备注
```

如果当前前端所有按钮都依赖动作接口，则应增加独立的非流程能力字段，或由设计工单页面根据当前用户和订单归属显示。不能让前端把 `remark` 当作流程动作调用。

备注弹窗建议支持：

- 多行文本输入；
- 最大长度提示；
- 已有备注回显；
- 保存、取消；
- 清空备注；
- 版本冲突时提示刷新后重试。

---

## 六、订单列表和导出链路

### 1. `OrderListVO` 和转换

`OrderListVO` 增加：

```java
private String designerRemark;
```

在 `OrderQueryHelper.toOrderListVO` 增加：

```java
vo.setDesignerRemark(entity.getDesignerRemark());
```

这样普通导出和自定义导出都能从同一个 VO 读取备注，避免两套导出逻辑分别查询字段。

### 2. 普通导出 `exportOrders`

`OrderController.exportOrders` 不需要修改方法签名。`OrderExportServiceImpl.exportOrders` 使用订单列配置决定导出列，因此需要：

1. 在 `setCellValue(Cell, OrderListVO, String)` 增加 `designerRemark` 分支；
2. 将 `designerRemark` 加入订单默认列配置；
3. 更新 `DefaultConfigProperties` 的默认 JSON；
4. 更新 `sql/init.sql` 中 `order.column.config` 的默认 JSON；
5. 对已有用户个人列配置不改写 JSON，但普通管理员导出时如果个人配置中没有 `designerRemark`，服务端在可见列末尾强制追加该列；
6. 如果 `designerRemark` 已配置，则沿用用户的排序、标签和宽度；
7. 如前端列配置有字段白名单，补充 `designerRemark`。

普通导出必须保证管理员导出的每个订单都带有“设计师备注”列，因此该列不能仅依赖新安装环境的默认配置。默认列标签为“设计师备注”，宽度 240 左右，放在设计师或设计时间附近。

### 3. 自定义导出 `customExportOrders`

在 `OrderExportServiceImpl` 增加：

```java
labels.put("designerRemark", "设计师备注");
fields.add(new OrderExportFieldVO("designerRemark", "设计师备注"));
```

同时在统一 `setCellValue` 中处理：

```java
case "designerRemark":
    cell.setCellValue(StrUtil.nullToEmpty(order.getDesignerRemark()));
    break;
```

`OrderController.customExportOrders` 和 `getAvailableExportFields` 的接口签名不变。前端调用 `getAvailableExportFields` 后即可看到新字段并提交 `designerRemark`。

### 4. 重建项目拆行

当前自定义导出会按重建项目拆分 Excel 行。备注是订单级字段，因此一个订单拆成多行时，每一行都重复写入同一备注，保证每行都能独立识别订单情况。

空备注统一导出为空字符串，不输出 `null`。

---

## 七、前端接口和数据契约

前端需要同步：

- 增加保存设计师备注 API 方法；
- 增加 `SaveDesignerRemarkDTO` 类型；
- 在订单行/设计工单行类型中增加 `designerRemark`；
- 在设计工单操作栏增加“备注”；
- 打开弹窗时使用当前行备注回显，保存成功后更新当前行数据或刷新列表；
- 使用返回的 `version` 或重新获取详情，避免下一次操作携带旧版本；
- 自定义导出字段选择器使用后端返回的 `designerRemark`，不要前端硬编码另一套字段名。

如果后端保存接口不返回最新对象，前端保存成功后至少递增/刷新版本号，推荐直接刷新当前列表页或重新请求详情。

---

## 八、测试方案

### 1. 数据库和实体

- [ ] 测试 schema 包含 `designer_remark`；
- [ ] MyBatis-Plus 能将 `designer_remark` 映射为 `designerRemark`；
- [ ] 历史订单 `NULL` 能正常查询和导出。

### 2. Service 权限

- [ ] 当前分配设计师可以保存备注；
- [ ] 非当前分配设计师保存失败；
- [ ] 管理员不能通过设计师写接口修改备注；
- [ ] 订单不存在返回订单不存在；
- [ ] 已取消订单保存失败；
- [ ] 备注超过 2,000 字校验失败；
- [ ] 空白备注会清空原备注；
- [ ] 版本号冲突返回版本冲突；
- [ ] 保存不会调用流程引擎，不改变订单状态和阶段。

### 3. 查询返回

- [ ] 设计工单列表返回 `designerRemark`；
- [ ] 设计工单详情返回 `designerRemark`；
- [ ] 订单列表转换返回 `designerRemark`；
- [ ] 不同用户只能在后端允许的数据范围内查看订单备注。

### 4. 导出

- [ ] `getAvailableExportFields` 返回 `designerRemark/设计师备注`；
- [ ] `customExportOrders` 选择 `designerRemark` 后导出正确内容；
- [ ] `exportOrders` 使用默认列配置时包含备注列；
- [ ] 已有个人列配置的管理员导出仍强制包含备注列，但不改写其持久化配置；
- [ ] 空备注导出为空字符串；
- [ ] 一个订单多个重建项目时，每个拆分行都包含相同备注；
- [ ] 普通导出和自定义导出均继续遵守原有数据权限和 10,000 条订单限制。

### 5. Controller

- [ ] 未登录请求被拦截；
- [ ] 非设计师请求不能保存；
- [ ] 合法请求返回成功；
- [ ] `@OperationLog` 能记录保存操作；
- [ ] 参数校验错误返回统一错误格式。

---

## 九、文件清单

### 数据库

- Create: `sql/migration-20260827-add-designer-remark.sql`
- Modify: `sql/ddl.sql`
- Modify: `sql/ddl-prod.sql`
- Modify: `sql/init.sql`
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/resources/schema.sql`

### 公共实体和订单查询

- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/entity/OrderMainEntity.java`
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/config/DefaultConfigProperties.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/OrderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/helper/OrderQueryHelper.java`

### 设计工单

- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/SaveDesignerRemarkDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignWorkorderController.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignWorkorderService.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderDetailVO.java`

### 导出

- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderExportServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderExportServiceImplTest.java`
- No signature change expected: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/OrderController.java`

### 测试

- Modify/Create: 设计工单 Controller/Service 测试
- Modify/Create: `OrderExportServiceImplTest`
- Modify/Create: `DesignWorkorderServiceImplTest`

---

## 十、实施顺序

### Task 1：数据库字段和实体映射

**Files:**

- Create: `sql/migration-20260827-add-designer-remark.sql`
- Modify: `sql/ddl.sql`
- Modify: `sql/ddl-prod.sql`
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/resources/schema.sql`
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/entity/OrderMainEntity.java`

- [ ] **Step 1: 先增加字段映射测试或更新实体结构测试**
- [ ] **Step 2: 运行订单模块相关测试，确认测试库字段缺失时失败**
- [ ] **Step 3: 增加 SQL 字段和 Java 实体字段**
- [ ] **Step 4: 运行实体/订单模块测试，确认映射通过**

### Task 2：设计师备注保存接口

**Files:**

- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/SaveDesignerRemarkDTO.java`
- Modify: `DesignWorkorderController.java`
- Modify: `DesignWorkorderService.java`
- Modify: `DesignWorkorderServiceImpl.java`

- [ ] **Step 1: 编写当前设计师、越权用户、版本冲突和清空备注测试**
- [ ] **Step 2: 运行测试，确认接口/Service 尚未实现而失败**
- [ ] **Step 3: 实现 DTO、Service 方法和显式权限校验**
- [ ] **Step 4: 实现带版本号的定向更新**
- [ ] **Step 5: 增加 Controller 和操作日志**
- [ ] **Step 6: 运行设计模块测试，确认全部通过**

### Task 3：列表和详情返回备注

**Files:**

- Modify: `DesignWorkorderListVO.java`
- Modify: `DesignWorkorderDetailVO.java`
- Modify: `DesignWorkorderServiceImpl.java`

- [ ] **Step 1: 编写列表/详情返回备注测试**
- [ ] **Step 2: 在详情查询入口补充 `DesignQueryHelper.checkOrderReadable(orderId)`，列表继续沿用设计工单数据范围过滤**
- [ ] **Step 3: 增加 VO 字段和转换赋值**
- [ ] **Step 4: 运行设计工单 Service 测试，确认越权详情不会返回备注**

### Task 4：订单导出字段

**Files:**

- Modify: `OrderListVO.java`
- Modify: `OrderQueryHelper.java`
- Modify: `OrderExportServiceImpl.java`
- Modify: `DefaultConfigProperties.java`
- Modify: `sql/init.sql`

- [ ] **Step 1: 为普通导出、自定义导出和字段列表增加失败测试**
- [ ] **Step 2: 增加 `designerRemark` VO 转换**
- [ ] **Step 3: 增加普通导出单元格写入分支**
- [ ] **Step 4: 增加自定义导出标签和可选字段**
- [ ] **Step 5: 更新默认列配置；管理员普通导出在用户个人配置缺少备注字段时，仅在本次导出内追加备注列，不改写个人配置**
- [ ] **Step 6: 验证多重建项目拆行时备注重复正确**
- [ ] **Step 7: 运行 `OrderExportServiceImplTest` 和订单模块测试**

### Task 5：前端操作栏和契约

**Files:**

- Modify: 前端订单/设计工单 API 类型文件
- Modify: 前端设计工单列表和备注弹窗组件

- [ ] **Step 1: 增加备注接口和字段类型**
- [ ] **Step 2: 在设计工单操作栏增加“备注”**
- [ ] **Step 3: 实现回显、保存、清空和版本冲突提示**
- [ ] **Step 4: 验证管理员导出字段选择包含“设计师备注”**
- [ ] **Step 5: 在前端仓库运行类型检查和构建；当前后端工作区不包含前端源码时，记录为外部联调验收项**

### Task 6：集成验证

- [ ] **Step 1: 执行订单模块测试**
- [ ] **Step 2: 执行设计模块测试**
- [ ] **Step 3: 在前端仓库执行类型检查/构建；当前后端工作区不包含前端源码时，记录为外部联调验收项**
- [ ] **Step 4: 使用测试数据验证设计师保存、列表回显、管理员导出完整链路**
- [ ] **Step 5: 检查 Git diff，确保只包含本需求相关文件**

---

## 十一、验收标准

1. 设计师在自己工单操作栏可看到“备注”，并能保存、修改、清空备注。
2. 非当前订单设计师无法通过接口修改备注。
3. 备注保存不会改变订单状态、阶段、设计时间或流程历史。
4. 设计工单列表/详情能正确回显备注。
5. `GET /order/export/fields` 返回“设计师备注”。
6. 普通订单导出在默认列配置下包含设计师备注。
7. 自定义导出选择设计师备注后，Excel 内容正确。
8. 订单因重建项目拆成多行时，每行均能看到对应订单备注。
9. 老订单无备注时导出为空，不出现 `null`，不影响其他字段。
10. 原有订单数据权限、导出数量限制、状态流转和订单编辑逻辑不回归。

---

## 十二、未决项和默认选择

### 未决项 1：备注是否保留历史

默认选择：不保留历史，`order_main.designer_remark` 覆盖保存。

### 未决项 2：管理员是否可修改备注

默认选择：管理员只读和导出，暂不开放修改；如确认需要管理员代设计师补录，再增加独立权限分支。

### 未决项 3：设计完成后是否允许修改

默认选择：允许。保存状态白名单固定为 `DATA_AUDIT_PASSED(1030)`、`PENDING_DESIGN(2010)`、`DESIGN_IN_PROGRESS(2020)`、`DESIGN_COMPLETED(2030)`，接口和前端必须使用同一套状态白名单。

### 未决项 4：备注是否作为设计工单默认展示列

默认选择：列表返回字段，操作栏弹窗查看和编辑；不默认增加列表正文列，避免占用过多表格宽度。订单普通导出默认增加导出列，自定义导出始终提供可选字段。
