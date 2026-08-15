# 产品列表增加打印设备编号设计

## 背景

`POST /production/product/list` 按产品分页返回 `ProductionProductDetailVO`。每个生产产品通过
`production_record_id` 关联流转卡，打印设备配置保存在流转卡的 `print_device_code` 字段中。

产品列表需要增加打印设备编号，以便前端展示每个产品所关联的打印设备。

## 目标与范围

- 在产品列表的每条记录中增加 `printDeviceCode`。
- 字段值来自产品所属流转卡的 `production_record.print_device_code`。
- 流转卡未分配打印设备或已经释放配置时返回 `null`。
- 不修改请求参数、分页结构、数据权限或既有返回字段。
- 不修改数据库表，不增加设备表查询，不修改前端。

## 方案

### 返回模型

在 `ProductionProductDetailVO` 的流转卡信息区域增加：

```java
@JsonInclude(JsonInclude.Include.ALWAYS)
private String printDeviceCode;
```

JSON 字段名保持 Jackson 默认驼峰格式 `printDeviceCode`。项目全局使用 `NON_NULL`，因此该字段
单独使用 `ALWAYS`，确保未分配或已释放设备时响应中仍明确包含
`"printDeviceCode": null`。

### 数据来源与组装

`ProductionProductServiceImpl.pageProductDetails` 已经根据当前页产品的
`productionRecordId` 批量查询流转卡并构建 `recordMap`。组装 VO 时直接从对应的
`ProductionRecordEntity` 读取 `printDeviceCode` 并设置到返回对象。

该方案复用现有批量查询，不增加 SQL 次数，也不会产生 N+1 查询。

### 空值和异常处理

- 找到流转卡且已分配打印设备：返回对应 `printDeviceCode`。
- 找到流转卡但未分配或已释放打印设备：返回 `null`。
- 未找到关联流转卡：保持现有容错行为，流转卡相关字段均不回填，`printDeviceCode` 返回
  `null`。

## 兼容性

本次只在成功响应记录中增加可选字段，对现有调用方为向后兼容变更。接口路径、请求体、
分页格式、权限过滤和已有字段含义均保持不变。

## 测试

- 服务测试：产品关联的流转卡具有打印设备编号时，VO 正确返回 `printDeviceCode`。
- 服务测试：流转卡打印设备编号为空时，VO 返回 `null`。
- 服务测试：多条产品跨多个流转卡时只执行一次流转卡批量查询，不产生逐产品查询或设备查询。
- 服务测试：关联流转卡缺失时保持容错行为，`printDeviceCode` 为 `null`。
- Controller JSON 测试：响应记录包含 `printDeviceCode` 字段及预期值；空值场景也明确包含
  `"printDeviceCode": null`。
- 执行生产模块完整测试，确认无回归。
