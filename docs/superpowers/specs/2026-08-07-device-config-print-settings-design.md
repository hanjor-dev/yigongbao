# getDeviceConfig 打印配置回显扩展设计

## 目标

扩展 `GET /production/record/{id}/device-config`，在现有设备信息基础上返回已保存的打印材质和打印参数，方便前端重新进入设备配置页面时一次性回显。

## 接口兼容

请求路径、HTTP 方法和参数保持不变。`DeviceConfigVO` 仅新增两个可空字段：

- `material: String`：来自 `production_record.material`。
- `printParams: String`：来自该流转卡 `production_process` 中 `process_type = 'print'` 的 `process_params`。

新增响应字段不会影响现有调用方。历史记录没有对应值时，VO 字段值为 `null`；由于项目全局采用 Jackson `NON_NULL` 序列化策略，最终 JSON 响应中会省略对应字段，前端按未配置处理。

## 后端实现

`getDeviceConfig(recordId)` 保留现有流转卡存在性校验和 `BeanUtil.copyProperties`。`material` 在 VO 增加同名字段后由 BeanUtil 自动复制；服务随后按 `productionRecordId + processType=print` 查询打印工序，将 `processParams` 写入 VO。

若未找到打印工序，VO 中保持 `printParams = null`（JSON 中省略），不将其视为异常。查询限定 `LIMIT 1`，与当前每张流转卡一条打印工序的领域约束保持一致。

## 范围

本次不修改 `assignDevice` 请求、不改变保存位置、不新增数据库字段，也不调整流转卡详情或工序列表接口。产品重量继续通过流转卡详情中的 `products[].weight` 获取。

## 测试

- 已保存材质及打印参数时，`getDeviceConfig` 同时返回两者。
- 未找到打印工序时，设备信息和材质正常返回，VO 的 `printParams` 为 `null`；Controller 序列化结果允许省略该字段。
- 流转卡不存在时继续抛出原业务异常。
- 运行 `ProductionRecordServiceImplTest`、Controller 相关测试及生产模块全量测试。
