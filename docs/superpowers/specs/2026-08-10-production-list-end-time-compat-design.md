# 生产列表结束时间临时兼容设计

## 目标

生产流转卡分页列表中的 `postProcessingEndTime` 暂时返回打印完成时间 `printFinishTime`，以满足当前前端“生产结束时间”列的取值要求。

## 范围

- 仅修改 `ProductionRecordServiceImpl.pageRecords` 的列表 VO 转换结果。
- 保留数据库中的 `print_finish_time`、`post_processing_end_time` 及其写入逻辑不变。
- 保留详情接口的 `postProcessingEndTime` 真实语义不变。
- 添加注释，明确该赋值是前端字段切换完成前的临时兼容逻辑。

## 数据流

分页查询从 `production_record` 读取两个真实时间字段，经 `BeanUtil.copyProperties` 复制到 `ProductionRecordVO`；随后仅在列表转换中执行 `vo.setPostProcessingEndTime(e.getPrintFinishTime())`。因此列表响应中的两个字段暂时都返回打印完成时间，实体和其他接口不受影响。

## 测试

构造打印完成时间与后处理结束时间不同的流转卡，调用 `pageRecords`，断言列表结果中的 `postProcessingEndTime` 等于 `printFinishTime`，同时确认 `printFinishTime` 自身不变。
