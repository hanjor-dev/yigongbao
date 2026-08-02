# 生产产品台账导出逻辑修正设计

## 背景

生产产品台账导出当前仅查询 `in_process`、`fail`、`pass`、`completed`，导致产品进入待入库、已入库或已出库状态后无法导出。客户要求导出生产订单开始打印及后续状态的产品数据。

## 目标

修正生产产品台账导出接口，使查询范围覆盖产品从开始打印到正常生产完成的完整状态链路，并保证总数查询与明细查询使用完全一致的过滤条件。

## 状态范围

纳入导出的产品状态：

- `in_process`：生产中，打印开始时由 `pending` 转入
- `fail`：质检不合格
- `pass`：质检合格
- `pending_warehouse_in`：待入库
- `warehoused`：已入库
- `warehouse_out`：已出库
- `completed`：已完成，作为完成态兼容值

排除：

- `pending`：尚未开始打印
- `cancelled`：已废弃，暂不作为正常生产链路导出

## 权限逻辑

- `CENTER`：仅查询当前用户 `center_id` 对应的生产记录。
- `ALL`：查询全部加工中心和医院的数据。
- `HOSPITALS`：保留现有医院范围过滤逻辑。
- 其他数据范围类型：保留现有拒绝导出行为。

角色范围来源于角色表的 `data_scope_type`，不在生产导出接口中硬编码角色名称。

## 实现方案

在 `ProductionProductMapper` 中统一更新 `listProductLedgerData` 和 `countProductLedgerData` 的产品状态条件，确保两条 SQL 的状态集合一致。Service 层、Excel 字段和权限拼装逻辑不做无关调整。

## 测试方案

- 增加 Mapper SQL 文本级回归测试，验证两个查询均包含完整的 7 个状态且不再使用旧的 4 状态集合。
- 保留并运行现有生产模块测试，验证控制器委托、服务流程和 Excel 构建逻辑未受影响。
- 对生产管理员和超级管理员分别验证权限条件：加工中心角色追加 `processing_center_id`，全部权限不追加权限限制。
