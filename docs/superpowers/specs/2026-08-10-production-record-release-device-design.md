# 生产流转卡强制释放打印设备配置设计

## 目标

为待打印生产流转卡提供强制释放接口，撤销该流转卡最近一次 `assignDevice` 提交产生的全部打印配置，使原设备能够被其他流转卡重新分配，并允许当前流转卡重新完整填写打印配置。

## 接口

- `POST /production/record/{id}/release-device`
- 无请求体，成功返回 `Result<Void>`。
- 操作日志类型为 `CANCEL`，操作名称为“强制释放打印设备配置”。
- 接口幂等：待打印记录重复释放仍返回成功，并清理可能存在的残留配置。

## 状态约束

- 仅 `FlowStatusEnum.PENDING_PRINT` 允许释放。
- 记录不存在时返回 `PRODUCTION_RECORD_NOT_FOUND`。
- 非待打印状态返回新的 `RECORD_STATUS_NOT_ALLOW_RELEASE_DEVICE`。
- `assignDevice` 对已存在 `printDeviceId` 的记录拒绝重复提交，要求先释放再重新填写。

## 操作权限

- 仅超级管理员、公司管理员、生产员和生产管理员允许分配或释放打印设备；其他角色及不存在的用户一律拒绝。
- 生产员和生产管理员受加工中心数据域约束：流转卡所属订单与目标打印设备都必须属于当前用户绑定的加工中心。
- 超级管理员和公司管理员可跨加工中心处理异常数据。

## 事务内清理范围

### production_record

清空 `print_device_id`、`print_device_code`、`print_device_name`、`material`，并更新 `content_update_time` 使已生成的流转卡 Excel 缓存失效。状态继续保持待打印。

### production_process 的 PRINT 工序

清空 `device_id`、`device_no`、`device_name`、`process_params`、`operator_id`、`operator_name`。释放不修改非打印工序。

### production_product

清空当前流转卡全部产品的 `product_no` 和 `weight`。产品编号包含旧设备编号和旧上机次数，释放后继续保留会造成数据矛盾；产品名称、设计材质、打印文件和产品状态保持不变。

### 不回退的数据

不递减 `device_daily_usage_counter`。它表示设备当日全局分配顺序，递减会在并发场景中造成编号重复。释放后重新分配会使用新的顺序号，允许出现序号空档。

## 并发与一致性

- `assignDevice` 按“设备行→流转卡行”顺序执行 `SELECT ... FOR UPDATE`；`releaseDevice` 锁定流转卡行。固定锁顺序避免与设备状态同步事务形成死锁环。
- 设备状态先持久化再发布同步事件；打印开始时锁定当前流转卡的全部有效产品，并要求所有产品都从待生产更新为生产中，否则整个监听事务回滚。
- 设备状态监听器将“待打印→打印中”改为带 `id + status + printDeviceId` 条件的原子更新；影响行数为 0 时停止后续工序、产品和 Flow 更新。
- 为 `production_record(print_device_id, status, is_deleted)` 增加联合索引，支撑设备占用查询和锁定。
- 释放只撤销数据库中的逻辑分配，不修改设备的实时在线或运行状态。其他流转卡重新分配时仍需满足设备在线、空闲且未被其他记录绑定。

## 错误码

- `RECORD_STATUS_NOT_ALLOW_RELEASE_DEVICE`：只有待打印状态允许释放。
- `RECORD_DEVICE_ALREADY_ASSIGNED`：当前流转卡已分配设备，必须先释放。

## 测试

- Controller 路由、日志入口和 Service 委托。
- 待打印记录释放后完整清空三张表的相关字段。
- 非待打印状态拒绝且无任何写操作。
- 重复释放幂等。
- 已分配记录不能直接重复调用 `assignDevice`。
- 释放不调用设备计数器递减或产品编号生成。
- 监听器条件更新失败时不执行后续副作用。
- 迁移脚本和目标 DDL 包含联合索引。
- 生产模块针对性测试、全量测试、编译和差异检查。
