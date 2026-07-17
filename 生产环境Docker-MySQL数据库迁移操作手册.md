# 生产环境 Docker MySQL 数据库迁移操作手册

> 适用环境：腾讯云 Linux / OpenCloudOS / CentOS，Docker Compose 部署的医工宝生产环境
> 适用脚本：sql/migration-online-schema-2026-07-16.sql、sql/migration-online-data-2026-07-16.sql
> 最后更新：2026-07-17

## 1. 目的和迁移范围

本手册用于在生产服务器的 MySQL Docker 容器中按顺序执行线上数据库迁移：

1. 执行 schema 迁移，增加表、字段和索引，并调整必要的列定义。
2. 执行 data 迁移，补充资源、字典、权限和业务数据。
3. 验证数据库结构、关键业务数据和应用健康状态。

项目生产配置见 docker-compose.prod.yml。

## 2. 重要安全规则

### 2.1 线上已有数据卷时不会自动执行 SQL

sql 目录挂载到 /docker-entrypoint-initdb.d，只会在 MySQL 数据目录首次初始化时自动执行。线上已有 mysql-data 数据卷时，新增 SQL 文件不会因为重启容器而自动执行。

线上迁移必须通过 docker compose exec 调用容器内的 mysql 客户端，通过标准输入执行 SQL 文件。

### 2.2 禁止删除生产数据卷

~~~bash
docker compose -f docker-compose.prod.yml down -v
docker-compose -f docker-compose.prod.yml down -v
docker volume rm yigongbao_mysql-data
~~~

这些命令可能删除生产数据库数据。迁移只需要停止 app 容器，不需要删除 MySQL 容器或数据卷。

### 2.3 不要暴露生产密钥

本文命令通过容器内的 MYSQL_ROOT_PASSWORD 和 MYSQL_DATABASE 读取凭据，不需要把真实密码写入命令。

不要执行 cat .env 并复制输出，也不要将生产 .env 提交到 Git。

### 2.4 严格按顺序执行

~~~text
备份 → 检查 → 停止 app → schema 迁移 → data 迁移 → 数据验证 → 启动 app → 应用验证
~~~

schema 迁移失败时，不得继续执行 data 迁移。任一步骤出现错误，都应先保留日志并确认数据库状态。

schema 脚本对多数字段、索引和表增加操作做了存在性检查，但仍包含列定义调整和索引重建；data 脚本包含多段业务数据写入。每个脚本按本次发布只执行一次，发生错误后不要未经检查直接重跑完整 data 脚本。

## 3. 迁移前准备

### 3.1 确认维护窗口

建议安排业务低峰期，并提前通知相关人员。迁移期间需要预留数据库备份、DDL、数据回填、应用重启和冒烟验证时间。

### 3.2 登录生产服务器并进入部署目录

~~~bash
ssh <服务器用户>@<服务器公网IP>
cd /home/app/yigongbao-java-prod-8082
pwd
~~~

生产环境部署目录为 /home/app/yigongbao-java-prod-8082；如果实际目录不同，请替换为真实路径。

### 3.3 确认文件存在

~~~bash
ls -lh \
  docker-compose.prod.yml \
  .env \
  sql/migration-online-schema-2026-07-16.sql \
  sql/migration-online-data-2026-07-16.sql
~~~

如果 SQL 文件尚未上传，在本地项目根目录执行：

~~~bash
scp sql/migration-online-schema-2026-07-16.sql \
  <服务器用户>@<服务器公网IP>:/home/app/yigongbao-java-prod-8082/sql/

scp sql/migration-online-data-2026-07-16.sql \
  <服务器用户>@<服务器公网IP>:/home/app/yigongbao-java-prod-8082/sql/
~~~

不要上传本地 .env，生产服务器应继续使用服务器上已有的生产配置。

### 3.4 核对 SQL 文件哈希

上传前和上传后分别执行以下命令，两端输出必须一致：

~~~bash
sha256sum \
  sql/migration-online-schema-2026-07-16.sql \
  sql/migration-online-data-2026-07-16.sql
~~~

### 3.5 检查 Docker Compose

本文命令按 Bash 编写；如果当前 SSH 会话不是 Bash，请先执行 `bash`，并在同一个会话中完成后续操作。

~~~bash
docker --version
docker compose version
~~~

推荐定义简化命令：

~~~bash
dc() {
  docker compose --env-file .env -f docker-compose.prod.yml "$@"
}
~~~

检查 Compose 配置：

~~~bash
dc config --quiet
~~~

没有输出且退出码为 0，表示配置解析成功。

生产环境还必须确认命令没有出现变量未配置的 warning。即使 Compose 返回 0，未配置的变量也可能被替换为空字符串；看到 COS、DB、Redis、邮件、签名密钥等变量缺失时，应先补齐生产 `.env`，不要继续迁移。

如果服务器只有旧版 docker-compose，定义：

~~~bash
dc() {
  docker-compose -f docker-compose.prod.yml "$@"
}
~~~

### 3.6 检查环境变量但不打印密码

~~~bash
grep -E '^(DB_NAME|DB_USERNAME|DB_PASSWORD)=' .env \
  | sed -E 's/=.*/=<已配置>/g'
~~~

至少应确认 DB_NAME、DB_USERNAME、DB_PASSWORD 已配置。

## 4. 迁移前检查

### 4.1 查看服务状态

~~~bash
dc ps
~~~

预期 mysql、redis、app 均为 Up，最好为 healthy。

### 4.2 检查 MySQL 健康状态

~~~bash
dc exec -T mysql \
  sh -c 'exec mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD" --silent'
~~~

预期输出：

~~~text
mysqld is alive
~~~

### 4.3 确认连接到正确数据库

~~~bash
dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    -e "SELECT DATABASE(), VERSION(), NOW();"'
~~~

确认 DATABASE() 是生产数据库名称，MySQL 版本为 8.0，时间和时区符合预期。

### 4.4 检查本次迁移目标数据

~~~bash
dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    --default-character-set=utf8mb4 \
    -e "SELECT id, role_name, data_scope_type FROM sys_role WHERE id = 3;"'
~~~

本次角色更新的目标是：

~~~text
id = 3
role_name = 区域管理员
data_scope_type = dept
~~~

## 5. 备份生产数据库

### 5.1 创建备份目录和批次号

后续备份文件名使用 TS 变量，重新登录服务器后需要重新执行本节命令。

~~~bash
mkdir -p backup
TS=$(date +%Y%m%d-%H%M%S)
test -n "$TS"
echo "$TS"
~~~

### 5.2 导出 MySQL 备份

必须先启用 pipefail，确保 mysqldump 失败时不会被后面的 gzip 成功状态掩盖：

~~~bash
set -o pipefail

if ! dc exec -T mysql \
  sh -c 'exec mysqldump \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction \
    --routines \
    --events \
    --triggers \
    --hex-blob \
    --set-gtid-purged=OFF \
    "$MYSQL_DATABASE"' \
  2> "backup/mysqldump-$TS.err" \
  | gzip > "backup/yigongbao-$TS.sql.gz"; then
  echo "mysqldump failed; see backup/mysqldump-$TS.err" >&2
  exit 1
fi
~~~

参数说明：

- --single-transaction：适合 InnoDB，尽量减少备份期间的锁影响。
- --routines、--events、--triggers：保留数据库对象。
- --set-gtid-purged=OFF：避免写入当前环境的 GTID 信息。
- 备份文件应复制到独立存储，不能只保留在同一台生产服务器。

### 5.3 验证备份文件

~~~bash
ls -lh "backup/yigongbao-$TS.sql.gz"
test -s "backup/yigongbao-$TS.sql.gz"
gzip -t "backup/yigongbao-$TS.sql.gz"
sha256sum "backup/yigongbao-$TS.sql.gz"
~~~

test -s 确认文件非空，gzip -t 没有输出且退出码为 0，表示压缩文件格式完整。还要检查 backup/mysqldump-$TS.err；条件允许时，应在测试环境实际恢复一次。

## 6. 停止应用写入

建议在执行 DDL 和数据回填前停止应用，只保留 MySQL 和 Redis：

~~~bash
dc stop app
dc ps
~~~

确认 app 已停止、mysql 仍为 Up/healthy：

~~~bash
dc exec -T mysql \
  sh -c 'exec mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD" --silent'
~~~

如果必须在线迁移，至少要确认没有持续写入相关表的任务，并实时观察锁等待和数据库负载。

## 7. 执行 schema 迁移

### 7.1 执行命令

schema 脚本包含 MySQL 客户端的 DELIMITER 和存储过程定义，必须使用容器内的 mysql 客户端执行：

~~~bash
set -o pipefail

dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    --default-character-set=utf8mb4 \
    --show-warnings' \
  < sql/migration-online-schema-2026-07-16.sql \
  2>&1 | tee "backup/schema-migration-$TS.log"
~~~

set -o pipefail 确保 mysql 执行失败时，即使 tee 成功，整个命令仍返回失败状态。

### 7.2 schema 成功标准

同时满足以下条件：

1. 命令退出码为 0。
2. 日志中没有 ERROR、Syntax error、Duplicate、Lock wait timeout 等错误。
3. 日志中出现：

~~~text
schema migration complete
~~~

检查日志：

~~~bash
if grep -nEi 'error|syntax|duplicate|lock wait|foreign key|access denied' \
  "backup/schema-migration-$TS.log"; then
  echo "schema migration log contains an error" >&2
  exit 1
fi
grep -nF 'schema migration complete' "backup/schema-migration-$TS.log" \
  || { echo "schema completion marker not found" >&2; exit 1; }
~~~

### 7.3 schema 结构验证

~~~bash
dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    --default-character-set=utf8mb4 \
    -e "
      SHOW TABLES LIKE '\''device_daily_usage_counter'\'';
      SHOW TABLES LIKE '\''order_cancel_apply'\'';
      SHOW COLUMNS FROM order_main LIKE '\''has_pending_cancel_apply'\'';
      SHOW COLUMNS FROM production_record LIKE '\''product_id'\'';
      SHOW COLUMNS FROM production_record LIKE '\''product_name'\'';
      SHOW COLUMNS FROM production_record LIKE '\''product_category'\'';
      SHOW COLUMNS FROM production_record LIKE '\''product_category_name'\'';
      SHOW COLUMNS FROM production_record LIKE '\''pack_material'\'';
      SHOW COLUMNS FROM design_drawing LIKE '\''qr_file_id'\'';
      SHOW INDEX FROM production_record WHERE Key_name IN ('\''uk_package_product'\'', '\''idx_production_record_category'\'');
    "'
~~~

至少应确认以下对象存在：

- device_daily_usage_counter
- order_cancel_apply
- order_main.has_pending_cancel_apply
- production_record.product_id
- production_record.product_name
- production_record.product_category
- production_record.product_category_name
- production_record.pack_material
- production_record.uk_package_product
- production_record.idx_production_record_category
- design_drawing.qr_file_id

## 8. 执行 data 迁移

只有 schema 迁移通过后，才能执行：

~~~bash
set -o pipefail

dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    --default-character-set=utf8mb4 \
    --show-warnings' \
  < sql/migration-online-data-2026-07-16.sql \
  2>&1 | tee "backup/data-migration-$TS.log"
~~~

成功标准：

1. 命令退出码为 0。
2. 日志中没有 SQL、重复键、外键、锁等待或连接错误。
3. 日志中出现：

~~~text
data migration complete
~~~

检查日志：

~~~bash
if grep -nEi 'error|syntax|duplicate|lock wait|foreign key|access denied' \
  "backup/data-migration-$TS.log"; then
  echo "data migration log contains an error" >&2
  exit 1
fi
grep -nF 'data migration complete' "backup/data-migration-$TS.log" \
  || { echo "data completion marker not found" >&2; exit 1; }
~~~

## 9. data 迁移结果验证

### 9.1 验证区域管理员数据范围

~~~bash
dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    -e "
      SELECT id, role_name, data_scope_type
      FROM sys_role
      WHERE id = 3;

      SELECT COUNT(*) AS wrong_scope_count
      FROM sys_role
      WHERE id = 3
        AND role_name = '\''区域管理员'\''
        AND NOT (data_scope_type <=> '\''dept'\'');
    "'
~~~

预期第一条查询返回 data_scope_type=dept，第二条查询的 wrong_scope_count 为 0。

### 9.2 验证取消申请资源和权限

~~~bash
dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    -e "
      SELECT resource_code, resource_name
      FROM sys_resource
      WHERE resource_code IN (
        '\''order:TabMyCancel'\'',
        '\''order:CancelApply'\'',
        '\''order:CancelApprove'\'',
        '\''order:CancelReject'\'',
        '\''order:CancelHistory'\''
      )
      ORDER BY resource_code;
    "'
~~~

应返回上述资源编码对应的记录。

### 9.3 验证二维码字典

~~~bash
dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    -e "
      SELECT id, parent_id, dict_code, dict_name, status
      FROM sys_dict
      WHERE dict_code = '\''10.21'\''
        AND is_deleted = 0;
    "'
~~~

预期至少有一条 dict_code=10.21、dict_name=图纸二维码图片 的记录。

### 9.4 验证业务统计

~~~bash
dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    -e "
      SELECT
        COUNT(*) AS total_active_records,
        SUM(product_id IS NULL) AS records_without_product_id,
        SUM(product_category IS NULL) AS records_without_category,
        SUM(product_category_name IS NULL) AS records_without_category_name
      FROM production_record
      WHERE is_deleted = 0;

      SELECT COUNT(*) AS pending_cancel_apply_count
      FROM order_main
      WHERE is_deleted = 0
        AND has_pending_cancel_apply = 1;
    "'
~~~

## 10. 启动应用并验证服务

### 10.1 启动应用

~~~bash
dc up -d app
dc ps
~~~

本次数据库迁移不会自动拉取应用镜像。只有在发布流程已经审核并明确指定应用镜像版本或 digest 时，才按发布流程更新 app。不要直接对生产配置中的 latest 执行 dc pull app，避免拉取未经本次迁移验证的代码。

### 10.2 检查应用日志

~~~bash
dc logs --tail=200 app
~~~

重点确认没有：

- 数据库连接失败。
- Unknown column。
- Table doesn't exist。
- MyBatis 映射或启动 Bean 创建失败。
- Redis 连接失败。

### 10.3 检查容器健康状态

~~~bash
dc ps
~~~

预期 app、mysql、redis 均为 Up，健康检查通过。

### 10.4 检查应用 API

生产配置中的应用端口为 8082，context path 为 /api：

~~~bash
curl -fsS http://127.0.0.1:8082/api/v3/api-docs \
  > /tmp/yigongbao-api-docs.json

wc -c /tmp/yigongbao-api-docs.json
~~~

命令退出码为 0 且文件大小大于 0，表示 API 文档端点可访问。之后还应使用测试账号执行：

- 登录。
- 查询角色和权限。
- 查询订单列表和详情。
- 查询生产记录。
- 验证取消申请、图纸二维码和数据范围功能。

## 11. 失败处理和故障排查

### 11.1 通用原则

1. 立即停止后续迁移步骤。
2. 保存终端输出和 backup/*-migration-*.log 文件。
3. 不要使用 --force 忽略 SQL 错误。
4. 不要在确认数据库状态前重复执行完整 data 脚本。
5. 记录失败的 section、SQL 和是否已产生部分变更。

schema 中的 DDL 可能自动提交，data 迁移也没有把全部操作放在一个事务中。错误发生后，不能简单执行 ROLLBACK 期待全部恢复。

### 11.2 常见错误

| 错误现象 | 处理方向 |
| --- | --- |
| Access denied for user | 检查 Compose 使用的 .env，确认容器内密码与现有数据库初始化密码一致。 |
| Unknown database | 确认 MYSQL_DATABASE 和应用实际连接的数据库。 |
| Table doesn't exist | 核对初始化版本和部署目录，不要直接补建表。 |
| Duplicate entry | 检查对应表数据和迁移日志，不要用 --force 跳过。 |
| production_record 重复 | schema 会在创建唯一索引前检测重复，需要业务确认清理方案。 |
| Lock wait timeout exceeded | 确认 app 已停止，检查长事务和锁等待。 |
| DELIMITER 或存储过程语法错误 | 确认使用容器内的 mysql 客户端执行。 |
| 应用报 Unknown column | 检查 schema 是否完整执行，以及应用镜像是否匹配。 |

查看数据库线程：

~~~bash
dc exec -T mysql \
  sh -c 'exec mysql \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --database="$MYSQL_DATABASE" \
    -e "SHOW FULL PROCESSLIST;"'
~~~

## 12. 数据恢复方案

### 12.1 优先使用定向修复

如果只有个别数据结果不符合预期：

1. 停止应用写入。
2. 根据迁移日志定位影响范围。
3. 从备份或审计数据确认原值。
4. 编写并审核定向修复 SQL。
5. 修复后重新执行只读验证。

不要为了恢复单个字段直接删除整个数据库。

### 12.2 完整恢复

以下操作具有破坏性，只有在确认迁移造成不可接受的数据问题、备份可用，并得到负责人明确批准后才能执行。

先停止应用并验证备份：

~~~bash
dc stop app

ls -lh backup/yigongbao-<备份时间>.sql.gz
gzip -t backup/yigongbao-<备份时间>.sql.gz

dc exec -T mysql \
  sh -c 'printf "%s\n" "$MYSQL_DATABASE"'
~~~

再次核对数据库名称和备份文件后，才允许执行：

~~~bash
set -euo pipefail

dc exec -T mysql \
  sh -c '
    case "$MYSQL_DATABASE" in
      ""|*[!A-Za-z0-9_]* )
        echo "unsafe or empty MYSQL_DATABASE; aborting" >&2
        exit 1
        ;;
    esac
    exec mysql \
      -uroot \
      -p"$MYSQL_ROOT_PASSWORD" \
      -e "DROP DATABASE $MYSQL_DATABASE; CREATE DATABASE $MYSQL_DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  '

zcat "backup/yigongbao-<备份时间>.sql.gz" \
  | dc exec -T mysql \
      sh -c 'exec mysql \
        -uroot \
        -p"$MYSQL_ROOT_PASSWORD" \
        --database="$MYSQL_DATABASE" \
        --default-character-set=utf8mb4'
~~~

如果应用使用的 DB_USERNAME 不是 root，恢复后还必须确认该账号对新建数据库具有正确权限，再启动 app。

恢复完成后，重新执行结构和数据验证，再启动应用：

~~~bash
dc up -d app
dc ps
dc logs --tail=200 app
~~~

如果备份恢复命令失败，不要继续启动应用，应保留现场并联系数据库负责人。

## 13. 迁移完成检查清单

### 迁移前

- [ ] 已确认维护窗口和回滚负责人。
- [ ] 已确认当前目录是生产部署目录。
- [ ] 已确认 Compose、.env 和两份迁移 SQL 存在。
- [ ] 已核对 SQL 文件 SHA-256。
- [ ] dc config --quiet 执行成功。
- [ ] MySQL 容器状态正常。
- [ ] 已确认连接到正确数据库。
- [ ] 已完成数据库备份并通过 gzip -t。
- [ ] 备份已复制到独立存储。
- [ ] 已停止 app 容器。

### schema 迁移后

- [ ] 命令退出码为 0。
- [ ] 日志出现 schema migration complete。
- [ ] 没有 SQL、重复键或锁等待错误。
- [ ] 新增表、字段和索引检查通过。

### data 迁移后

- [ ] 命令退出码为 0。
- [ ] 日志出现 data migration complete。
- [ ] sys_role.id=3 的 data_scope_type 为 dept。
- [ ] 取消申请资源和权限检查通过。
- [ ] 字典 10.21 检查通过。
- [ ] 生产记录统计和人工检查列表已保存并交由业务确认。

### 应用恢复后

- [ ] app、mysql、redis 状态正常。
- [ ] 应用日志没有数据库结构错误。
- [ ] /api/v3/api-docs 可以访问。
- [ ] 登录和核心业务冒烟验证通过。
- [ ] schema/data 日志和数据库备份已归档。

## 14. 相关文件

- [docker-compose.prod.yml](docker-compose.prod.yml)：生产 Docker Compose 配置。
- [sql/migration-online-schema-2026-07-16.sql](sql/migration-online-schema-2026-07-16.sql)：schema 迁移脚本。
- [sql/migration-online-data-2026-07-16.sql](sql/migration-online-data-2026-07-16.sql)：data 迁移脚本。
- [DOCKER_DEPLOY.md](DOCKER_DEPLOY.md)：Docker 部署和日常运维参考。
- [SERVER_SETUP.md](SERVER_SETUP.md)：服务器环境和部署目录说明。
