# 生产产品台账 22 列导出设计

## 目标

保持 `POST /production/record/product-ledger/export` 接口、权限范围和最多 10000 条限制不变，将当前 41 列产品台账调整为已确认的 22 列模板。导出粒度仍为一条 `production_product` 一行。

## 数据关联

主查询从 `production_product pp` 出发：

- `INNER JOIN production_record pr ON pp.production_record_id = pr.id`
- `INNER JOIN order_main om ON pr.order_id = om.id AND om.is_deleted = 0`
- 后处理时长通过按流转卡关联的标量聚合子查询获得：`p.production_record_id = pr.id`，并过滤 `p.is_deleted = 0`；同一流转卡下的每条产品行共享该时长。禁止直接 JOIN 工序表，避免产品行重复

现有产品状态范围、逻辑删除过滤、医院/加工中心权限过滤和 10000 条限制保持不变。

## 导出列

| 顺序 | 表头 | 数据来源与规则 |
|---:|---|---|
| 1 | 序号 | Excel 行序号，从 1 自动递增 |
| 2 | 订单流水号 | `om.order_code` |
| 3 | 时间 | `om.create_time`，格式 `yyyy年M月d日` |
| 4 | 产品编号 | `pp.product_no` |
| 5 | 数据文件名称 | `pp.file_name` 去除最后一个扩展名；`patient.v2.stl` 输出 `patient.v2` |
| 6 | 产品名称 | `pp.product_name` |
| 7 | 型号/规格 | `pp.spec_name` |
| 8 | 材质 | `pp.color_name` 与 `pp.material_name` 无分隔符拼接；任一为空时输出另一项 |
| 9 | 打印时长 | `pr.print_finish_time - pr.print_start_time`，输出 `HH:mm:ss`，小时允许超过 23 |
| 10 | 总重量（g） | `pp.weight`，Excel 数值；空值保持空白 |
| 11 | 处理时长 | 累加 `wash/cure/clean_dry` 的 `end_time-start_time`；输出中文分钟/小时，不含 `print` 和 `pack` |
| 12 | 数量 | 固定数值 1，因为每条 `production_product` 已代表一件产品 |
| 13 | 医院 | `om.hospital_name` |
| 14 | 患者 | `om.patient_name` |
| 15 | 性别 | `om.patient_gender`：`12.1=男`、`12.2=女`，未知值原样输出 |
| 16 | 年龄 | `om.patient_age`；空值保持空白 |
| 17 | 操作人员 | `pr.producer_name`，即流转卡认领生产员快照 |
| 18 | 医生 | `om.doctor_name` |
| 19 | 科室 | `om.hospital_dept_name` |
| 20 | 业务员 | `om.operator_name` |
| 21 | 出库情况 | `pp.warehouse_out_time IS NOT NULL` 为“已出库”，否则“未出库” |
| 22 | 备注 | 固定空白 |

## 时长与空值规则

打印时长仅在起止时间均存在且结束时间不早于开始时间时计算，否则为空。格式按总小时数手动计算，避免超过 24 小时回绕。

后处理只包含 `wash`、`cure`、`clean_dry`：

- 没有任何后处理工序时输出 `0分钟`；
- 存在后处理工序但任一工序缺少起止时间时保持空白；
- 任一工序 `end_time < start_time` 时视为异常数据，整列保持空白，不输出负时长；
- 所有工序时间完整时累计秒数，少于 60 分钟显示 `N分钟`，整小时显示 `N小时`，其余显示 `N小时M分钟`。

聚合仅针对当前 `pr.id` 下未逻辑删除的 `wash/cure/clean_dry` 记录。秒数转换为分钟时向下取整，舍弃不足一分钟的余秒，例如 2428 秒显示 `40分钟`。

当前系统保存的是固定排程时长，因此医疗器械正常情况下通常得到 60 分钟；本次不改变工序时间模型。

## 查询条件与排序

`ProductLedgerExportDTO.startTime/endTime` 统一解释为订单创建时间范围。列表和计数 SQL 都必须使用：

```sql
om.create_time >= #{dto.startTime}
om.create_time <= #{dto.endTime}
```

排序改为：

```sql
ORDER BY om.create_time DESC, pp.id DESC
```

保证 Excel 可见日期、筛选条件和序号顺序一致。

## 保持不变

- Controller 路径、请求 DTO 结构和文件下载方式
- 数据权限注入方式
- 无数据时的业务异常
- 超过 10000 条时只导出前 10000 条并显示顶部警告
- Sheet 名称“生产产品台账”

## 测试要求

- Excel 表头严格为上述 22 列且顺序一致
- 验证订单日期格式、文件名去最后后缀、多点文件名、材质拼接
- 验证打印时长可超过 24 小时、空值和负时长为空
- 验证后处理只统计三类工序，明确排除包装和打印
- 验证数量与重量使用数值单元格，性别和出库状态转换正确
- Mapper SQL 同时验证列表与计数均使用 `om.create_time`，且不再使用 `pp.create_time` 过滤
