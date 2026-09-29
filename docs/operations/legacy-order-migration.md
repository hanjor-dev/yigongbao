# 历史订单迁移运行说明

## 配置

在 `yigongbao-boot/src/main/resources/application-*.yml` 的 `yigongbao.legacy-datasource` 配置旧库只读连接。生产环境通过环境变量注入 URL、用户名和密码，默认关闭旧库连接；测试环境固定关闭。

生产环境旧库账号只授予查询权限。JDBC URL 建议包含 `connectTimeout=10000&socketTimeout=60000`，例如：

```text
jdbc:mysql://old-db:3306/ry?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=true&connectTimeout=10000&socketTimeout=60000
```

账号授权示例（请由数据库管理员按实际账号、主机和库名执行）：

```sql
GRANT SELECT ON ry.* TO 'yigongbao_legacy_ro'@'应用服务器IP';
```

应用侧连接池同时配置为只读，不能用具备写权限的旧库账号替代。

## 执行

1. 执行 `yigongbao-parent/sql/migration/2026-09-28-add-legacy-order-list.sql`，创建归档表、任务表和菜单权限。
2. 确认旧库账号仅具备 `SELECT` 权限。
3. 确认旧库复制已完成，并检查 `jy_order` 表及 `od_id`、`od_num` 字段存在；迁移接口会在创建任务前执行主表结构预检查。
4. 登录具有权限的账号，在“业务运营-历史订单”点击“增量迁移”。
5. 页面轮询任务状态；迁移完成后刷新归档列表。

任务通过 `(source_system, source_order_id)` 幂等写入，并使用源行哈希跳过未变化记录。当前实现会重扫旧 `jy_order`，因此可发现历史状态变化；读取和写入均不修改旧库或新版订单表。

## 排查

- “旧医工宝数据库未配置或未启用”：检查 profile 配置与环境变量，并确认 `enabled: true`。
- 任务部分完成：查看任务的 `errorCount`；失败行不会阻断其他订单，修复数据后可再次点击迁移。
- 任务提示邮寄信息或重建项目未完成映射：检查 `jy_instruct`、`jy_module` 是否已复制完成；订单主体仍会继续迁移。
- 归档列表为空：先确认 SQL 已执行、迁移任务为 `SUCCESS/PARTIAL_SUCCESS`，再检查 `legacy_order_list`。
