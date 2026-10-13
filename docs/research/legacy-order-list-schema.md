# 历史订单列表归档表

`legacy_order_list` 是历史列表的独立只读快照表，不与新版 `order_main` 建立流程关系。`status_raw`、`business_type_raw`、`print_raw` 和各 `*_raw` 字段保存旧系统原文；对应的标准时间/金额列仅在可解析时写入。

来源唯一键为 `(source_system, source_order_id)`。`source_row_hash` 用于增量迁移幂等判断，`mapping_version` 用于后续字段规则升级。`legacy_migration_task` 保存每次迁移的请求人、快照时间、读写统计和错误摘要。

`legacy_migration_error` 保存单条失败记录的来源订单、错误类型、错误消息和原始快照。迁移程序按 `od_id` 游标每批读取旧订单，并按批次查询目标归档记录，避免一次性加载全部旧数据。

本阶段项目摘要写入 `rebuild_project_summary`；详情、文件、项目明细和旧路径仍待后续阶段增加扩展表及查询接口。
