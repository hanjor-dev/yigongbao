# 数据库结构差异对比报告（ddl-prod.sql -> ddl.sql）

生成时间：2026-07-16

## 输入文件

| 文件 | 用途 | 结论 |
|---|---|---|
| `sql/ddl-prod.sql` | 线上生产旧结构 | 作为迁移起点 |
| `sql/ddl.sql` | 本地最新结构 | 作为主目标结构 |
| `sql/migration-ddl-prod-to-ddl-2026-07-14.sql` | 旧差异脚本 | 仅参考，覆盖不完整且 `uk_package_product` 实现与当前 `ddl.sql` 不一致 |
| `sql/alter-device-usage-counter-2026-07-15.sql` | 设备计数字段补充 | 已体现在当前 `ddl.sql` 的建表语句中 |
| `sql/alter-production-record-add-category.sql` | 流转卡产品类别字段 | 字段已体现在当前 `ddl.sql`；索引 `idx_production_record_category` 仅在 alter 脚本中出现 |
| `sql/migration-production-record-category.sql` | 流转卡历史类别数据迁移 | 需要纳入数据迁移脚本 |
| `sql/migration_20260627_remove_phone_unique_constraint.sql` | 手机号唯一索引调整 | 当前 `ddl.sql` 已体现，需要纳入结构脚本 |
| `sql/migration_20260627_service_provider.sql` | 服务商字典/角色数据 | 数据迁移类变更，需要纳入数据脚本 |

## 结构差异表

| 序号 | 表 | 差异类型 | 线上旧结构 | 本地/脚本目标 | 处理脚本 | 备注 |
|---:|---|---|---|---|---|---|
| 1 | `sys_user` | 索引变更 | `uk_phone` 唯一表达式索引，逻辑删除下手机号唯一 | 删除 `uk_phone`，新增普通索引 `idx_user_phone(phone)` | `migration-online-schema-2026-07-16.sql` | 来自 `migration_20260627_remove_phone_unique_constraint.sql`；支持同手机号多账号 |
| 2 | `order_main` | 新增字段 | 无 `has_pending_cancel_apply` | `has_pending_cancel_apply TINYINT DEFAULT 0 COMMENT '是否有待审核的取消申请（0=否，1=是）'` | `migration-online-schema-2026-07-16.sql` | 当前 `ddl.sql` 已有；旧迁移脚本也包含 |
| 3 | `order_main` | 新增索引 | 无 | `idx_order_main_has_pending_cancel_apply(has_pending_cancel_apply)` | `migration-online-schema-2026-07-16.sql` | 加速取消申请待审筛选 |
| 4 | `device_daily_usage_counter` | 新增表 | 无 | 新建设备每日上机次数统计表，含 `device_id/usage_date/usage_count/version/BaseEntity` 字段 | `migration-online-schema-2026-07-16.sql` | 2026-07-15 alter 中补充的 `create_by/update_by/is_deleted` 已纳入完整建表 |
| 5 | `device_daily_usage_counter` | 新增唯一索引 | 无 | `uk_device_date(device_id, usage_date)` | `migration-online-schema-2026-07-16.sql` | 保证同设备同日期只有一条计数 |
| 6 | `device_daily_usage_counter` | 新增普通索引 | 无 | `idx_usage_date(usage_date)` | `migration-online-schema-2026-07-16.sql` | 查询日期维度统计 |
| 7 | `order_cancel_apply` | 新增表 | 无 | 新建订单取消申请表 | `migration-online-schema-2026-07-16.sql` | 支持取消申请审核流程 |
| 8 | `order_cancel_apply` | 新增索引 | 无 | `idx_order_cancel_apply_order_id(order_id)` | `migration-online-schema-2026-07-16.sql` | 按订单查询取消申请 |
| 9 | `order_cancel_apply` | 新增索引 | 无 | `idx_order_cancel_apply_audit_status(audit_status)` | `migration-online-schema-2026-07-16.sql` | 待审列表筛选 |
| 10 | `order_cancel_apply` | 新增索引 | 无 | `idx_order_cancel_apply_apply_by(apply_by)` | `migration-online-schema-2026-07-16.sql` | 申请人维度查询 |
| 11 | `production_record` | 新增字段 | 无 `product_id` | `product_id BIGINT COMMENT '产品ID'` | `migration-online-schema-2026-07-16.sql` | 支持一个设计包按产品拆分多张流转卡 |
| 12 | `production_record` | 新增字段 | 无 `product_name` | `product_name VARCHAR(100) COMMENT '产品名称（冗余）'` | `migration-online-schema-2026-07-16.sql` | 流转卡冗余展示 |
| 13 | `production_record` | 新增字段 | 无 `product_category` | `product_category VARCHAR(50) COMMENT '产品大类代码（如17.1，冗余自product.category）'` | `migration-online-schema-2026-07-16.sql` | 当前 `ddl.sql` 已有 |
| 14 | `production_record` | 新增字段 | 无 `product_category_name` | `product_category_name VARCHAR(100) COMMENT '产品大类名称（如"模型"、"导板"，冗余自product.category_name）'` | `migration-online-schema-2026-07-16.sql` | 当前 `ddl.sql` 已有 |
| 15 | `production_record` | 新增字段 | 无 `pack_material` | `pack_material VARCHAR(100) COMMENT '包装材质（如：纸封袋、PE符合食品包装袋）'` | `migration-online-schema-2026-07-16.sql` | 用于包装/流转卡展示 |
| 16 | `production_record` | 新增唯一索引 | 无 | `uk_package_product(design_package_id, product_id)` | `migration-online-schema-2026-07-16.sql` | 当前 `ddl.sql` 是普通组合唯一索引；旧迁移脚本写成函数索引，已按最新 DDL 修正 |
| 17 | `production_record` | 新增普通索引 | `ddl.sql` 未体现 | `idx_production_record_category(product_category)` | `migration-online-schema-2026-07-16.sql` | 来自 `alter-production-record-add-category.sql`，用于类别筛选；属于 alter-only 补充 |
| 18 | `production_product` | 字段变更 | `product_no VARCHAR(50) NOT NULL COMMENT '产品编号'` | `product_no VARCHAR(50) NULL COMMENT '产品编号（分配设备时生成）'` | `migration-online-schema-2026-07-16.sql` | 支持创建产品记录时暂不生成正式编号 |

## 数据迁移差异表

| 序号 | 目标 | 迁移内容 | 自动化策略 | 输出/风险 |
|---:|---|---|---|---|
| 1 | `sys_dict` | 新增服务商机构类型 `1.4` | `INSERT ... SELECT ... WHERE NOT EXISTS` | 幂等 |
| 2 | `sys_role` | 新增 `salesman-self` 角色 | `WHERE NOT EXISTS` | 幂等 |
| 3 | `sys_role_resource` | 复制 `salesman` 资源给 `salesman-self` | 避免重复插入 | 依赖线上存在 `salesman` 角色 |
| 4 | `sys_dept_org` | 企业部门补充生产企业关联 | 若部门未有关联则插入 | `manufacturer.org.id` 不存在时 fallback 为 `1` |
| 5 | `order_main` | 回填 `has_pending_cancel_apply` | 根据 `order_cancel_apply.audit_status=1` 设置，其他为 0 | 新建表通常为空，因此大多为 0 |
| 6 | `device_daily_usage_counter` | 根据历史 `production_product.product_no` 初始化设备每日上机次数 | 解析编号 `YYMMDD + 产品代码 + 设备号 + 上机次数 + 序号`，按设备/日期取最大上机次数 | 仅能解析符合新格式的编号 |
| 7 | `production_record` | 回填 `product_id/product_name/category/category_name` | 仅当“一个设计包只有一张活跃流转卡，且设计包只有一个产品”时自动填 | 多产品老数据不自动拆卡，输出人工确认清单 |
| 8 | `production_record` | 二次尝试按 `production_product.product_name` 匹配产品主数据 | 仅当一张流转卡只有一个产品名，且产品主数据唯一匹配时填充 | 保守策略，避免误填 |
| 9 | `production_record` | 已有 `product_id` 的记录补类别 | 关联 `product` 表补 `category/category_name` | 幂等 |
| 10 | `sys_resource` | 修正历史资源编码大小写 | `id=1115 AND resource_code='order:review'` 时改为 `order:Review` | 定点幂等修正，避免误改其他资源 |

## 审查修正记录

| 项 | 原问题 | 修正 |
|---|---|---|
| `production_record.uk_package_product` | 初版脚本检测到同名索引已存在就跳过；如果线上曾执行旧脚本，可能保留函数唯一索引，和当前 `ddl.sql` 不一致 | 结构脚本已改为先检查重复数据，再重建为 `UNIQUE KEY uk_package_product (design_package_id, product_id)` |
| `sys_role.data_scope_type` | 旧服务商脚本使用 `HOSPITALS` 大写，但当前 DDL、初始化数据和代码枚举使用小写 `hospitals` | 数据脚本已改为插入小写 `hospitals`，并兼容修正已存在的大写历史值 |
| `production_record` 数据回填 | 线上执行数据脚本时，`production_record/design_product/product/production_product` 部分字符列排序规则混用 `utf8mb4_unicode_ci` 与 `utf8mb4_0900_ai_ci`，触发 MySQL 1267 `Illegal mix of collations` | 数据脚本已在跨表字符串匹配、`COALESCE` 赋值和人工确认查询中显式统一为 `COLLATE utf8mb4_unicode_ci` |

## 生成脚本

| 脚本 | 用途 | 建议执行顺序 |
|---|---|---:|
| `sql/migration-online-schema-2026-07-16.sql` | 线上结构变更 | 1 |
| `sql/migration-online-data-2026-07-16.sql` | 历史数据迁移与校验查询 | 2 |

## 执行前重点风险

| 风险 | 原因 | 建议 |
|---|---|---|
| `uk_package_product` 创建/后续回填失败 | 若线上已有相同 `design_package_id + product_id` 重复数据，组合唯一索引会失败 | 先执行备份；如生产已有部分回填数据，先查重再执行结构脚本 |
| 多产品历史流转卡无法无损自动拆分 | 旧结构一张流转卡可能覆盖一个设计包多个产品，新结构按产品拆分 | 数据脚本只补唯一可判断记录，并输出 `manual_check_required` 清单 |
| `device_daily_usage_counter` 初始化不完整 | 只能解析符合 15 位新产品编号格式的数据 | 执行后查看脚本末尾计数查询，必要时人工补计数 |
| `idx_production_record_category` 与 `ddl.sql` 不一致 | 该索引只存在于 alter 脚本，主 DDL 未更新 | 本次按用户要求综合 alter/migration 纳入线上脚本 |
