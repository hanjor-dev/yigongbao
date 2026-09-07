# 设计工单更新订单评估意见设计

## 背景

设计工单需要提供一个独立的备注更新入口。该接口不使用已有的 `order_main.data_evaluation_opinion` 字段，而是将内容保存到新增的 `order_main.designer_remark` 字段，避免与原有影像数据评估意见语义混用。

## 接口设计

- 请求方式：`POST`
- 请求路径：`/design/workorder/{orderId}/designer-remark`
- 路径参数：`orderId`，订单 ID
- 请求体：

```json
{
  "designerRemark": "设计师备注内容"
}
```

- 返回值：沿用项目统一的 `Result<Void>` 成功响应。
- 请求参数使用 DTO 校验设计师备注必填，并限制在数据库字段可承载的长度范围内。

## 分层实现

1. 在设计模块新增 `SaveDesignerRemarkDTO`，承载并校验 `designerRemark`。
2. 在 `DesignWorkorderService` 增加 `saveDesignerRemark(Long orderId, String remark)`。
3. 在 `DesignWorkorderServiceImpl` 中校验订单存在，使用已有 `OrderMainService` 更新 `OrderMainEntity.designerRemark`，订单不存在时抛出既有 `ORDER_NOT_FOUND` 异常。
4. 在 `DesignWorkorderController` 新增接口，并记录“保存设计师备注”操作日志。

## 数据流

```text
HTTP 请求
  -> Controller 参数校验
  -> DesignWorkorderService
  -> OrderMainService.updateById
  -> order_main.designer_remark
```

## 错误处理

- `designerRemark` 为空或超过约定长度：由 Jakarta Validation 返回参数错误。
- `orderId` 对应订单不存在：由服务层抛出 `ErrorCodeEnum.ORDER_NOT_FOUND`。
- 更新失败：沿用现有全局异常处理和事务边界。

## 测试策略

- Controller 测试：验证请求体字段能够传递到服务层，并返回统一成功结果。
- Service 测试：验证存在订单时仅更新设计师备注字段；验证订单不存在时抛出订单不存在异常。
- 执行设计模块相关测试及必要的模块构建验证。

## 非目标

- 不改变订单状态、阶段、版本号或其他订单字段。
- 不修改 `order_main.data_evaluation_opinion`。
- 不新增独立的订单 Mapper，复用已有订单服务能力。
