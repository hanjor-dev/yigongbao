# 接口到 Service 测试覆盖矩阵（最终轮）

本文件按 Controller 接口作为入口，记录对应 Service 调用和测试证据。仅有 Controller 测试或仅有模块全量回归，不能视为 Service 分支已覆盖。

## 当前盘点

| 模块 | Controller 映射接口 | Service 实现公开方法 | 当前状态 |
|---|---:|---:|---|
| order | 46 | 63 | Controller 入口全覆盖；已识别核心 Service 分支闭合 |
| design | 34 | 48 | Controller 入口全覆盖；已识别核心 Service 分支闭合 |
| production | 33 | 45 | Controller 入口全覆盖；已识别核心 Service 分支闭合 |

## 已确认需要补强的 Service 方法

### order

`getDraftDetail`、`listDrafts`、`saveDraft`、`getOrderDetail`、`listAvailableActions`、`updateOrder`、`removeOrder`、`saveColumnConfig`、`resetColumnConfig`、`customExportOrders`、`getApplyDetail`、`listApplies`、`myListApplies`、`listModificationLogs`、`cancelClassicCaseMark`、`checkNotClassicCase`。

### design

`downloadDrawing`、`listDrawingVersions`，以及所有 Controller 已调用但缺少成功/拒绝/边界分支证据的方法。

### production

`getTodayCount`、`listProcesses`、`listWarehouse`、`getWarehouseDetail`、`listWarehouseProducts`、`exportProductLedger`、`generateFlowCardExcel`、`getOrGenerateFlowCardExcel`、`triggerFlowAndSync`，以及所有 Controller 已调用但缺少成功/拒绝/边界分支证据的方法。

## 当前新增验证

- order：`OrderMainServiceImplStateTransitionTest` 已补充 `listAvailableActions` 的订单不存在和 FlowFacade 委托路径。
- production：`DeviceUsageCounterServiceImplTest` 已补充 `getTodayCount` 有记录/无记录路径。
- order：补充 `OrderControllerTest` 草稿、订单详情、动作、设计师、列配置和导出入口；补充 `FlowDebugControllerTest` 的有效预览、执行和重置入口；补充修改申请查询入口。
- design：补充附件模型/报告查询与关联、图纸下载/预览/版本、截图查询、数据包列表、打印信息查询和设计师工作量导出入口。
- production：补充生产记录查询/详情/批号提交/流转卡 Excel/产品台账、质检查询/失败/转包装、工序查询/完成、设备配置、仓储查询和列配置保存入口。
- order Service：补充经典案例标记回滚、订单更新、订单删除及实体交付变更边界。
- production Service：补充流转卡 Excel 缺失记录、Flow 同步成功/空结果/拒绝三条路径。

## 最终判定与边界

- order 46/46、design 34/34、production 33/33 个 Controller 映射均有请求级测试路径，并向下验证了对应 Service 委托、参数校验、成功响应及已识别的异常/状态分支。
- 三个模块的 Service 接口方法和 `*ServiceImpl` 公共方法均能映射到测试类中的测试符号；新增重点分支已在上文列出。
- “闭合”指本轮静态审计识别出的接口与核心业务分支均有证据，不宣称覆盖运行时无法从源码识别的所有组合路径。无自定义逻辑的继承 CRUD 不重复制造测试。
- 最新全量回归：order 206、design 169、production 171，均为 0 failures、0 errors、0 skipped。
