# 医工宝生产环境部署与运维手册

本文档只适用于医工宝生产环境。当前服务通过 Docker Compose 运行，应用使用 `prod` 配置，对外端口为 `8082`。

## 1. 环境与目录

| 项目 | 配置 |
|---|---|
| 部署目录 | `/home/app/yigongbao-java-prod-8082` |
| Compose 文件 | `docker-compose.prod.yml` |
| 应用容器 | `yigongbao-app` |
| MySQL 容器 | `yigongbao-mysql` |
| Redis 容器 | `yigongbao-redis` |
| 应用端口 | `8082` |
| MySQL 容器端口 | `3306` |
| Redis 端口 | `6379` |

Compose 使用以下命名卷：

- `mysql-data`：MySQL 数据。
- `redis-data`：Redis 持久化数据。
- `app-files`：应用文件。
- `app-logs`：Logback 文件日志。

服务器目录至少包含：

```text
/home/app/yigongbao-java-prod-8082/
├── docker-compose.prod.yml
├── .env
└── sql/
```

不要执行会删除数据卷的命令，尤其是 `docker compose down -v` 和 `docker volume prune`。

## 2. 部署前准备

```bash
docker --version
docker compose version

sudo mkdir -p /home/app/yigongbao-java-prod-8082/sql
sudo chown -R "$USER":"$USER" /home/app/yigongbao-java-prod-8082
cd /home/app/yigongbao-java-prod-8082
```

如果服务器使用旧版 Compose，可将本文档中的 `docker compose` 替换为 `docker-compose`。

## 3. 配置生产环境变量

在部署目录创建 `.env`。密码、密钥和云服务凭据只保存在服务器，不要提交到 Git：

```dotenv
DB_NAME=yigongbao
DB_USERNAME=root
DB_PASSWORD=替换为数据库密码
REDIS_PASSWORD=替换为Redis密码

COS_SECRET_ID=替换为腾讯云SecretId
COS_SECRET_KEY=替换为腾讯云SecretKey
COS_REGION=替换为腾讯云地域
COS_BUCKET=替换为腾讯云存储桶
COS_DOMAIN=替换为腾讯云访问域名

OSS_ACCESS_KEY=替换为阿里云AccessKey
OSS_SECRET_KEY=替换为阿里云SecretKey
OSS_ENDPOINT=替换为阿里云OSS地址
OSS_BUCKET=替换为阿里云存储桶
OSS_DOMAIN=替换为阿里云访问域名

MAIL_HOST=替换为邮件服务器
MAIL_PORT=465
MAIL_USERNAME=替换为邮件账号
MAIL_PASSWORD=替换为邮件密码

DEFAULT_PASSWORD=替换为初始密码
APP_SIGN_SECRET=替换为应用签名密钥
SMS_MOCK_REDIRECT_EMAIL=替换为短信模拟转发邮箱
```

限制权限并检查 Compose 配置：

```bash
chmod 600 .env
docker compose -f docker-compose.prod.yml config
```

确认输出中没有未定义变量或 `替换为...` 后，再启动服务。

## 4. 首次启动

登录阿里云镜像仓库并启动服务：

```bash
docker login --username=你的账号 crpi-ie9dcy6o6rjohsiv.cn-hangzhou.personal.cr.aliyuncs.com
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
```

首次初始化 MySQL 时，`sql/` 中的脚本由 MySQL 初始化流程执行。已经存在的数据卷不会重复执行初始化脚本，后续变更必须使用迁移脚本。

查看日志和健康状态：

```bash
docker compose -f docker-compose.prod.yml logs --tail=200 app
docker compose -f docker-compose.prod.yml logs --tail=100 mysql redis
curl -f http://127.0.0.1:8082/api/v3/api-docs
```

## 5. 发布更新应用

阿里云完成新镜像构建后，在服务器执行：

```bash
cd /home/app/yigongbao-java-prod-8082
docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d --force-recreate app
docker compose -f docker-compose.prod.yml ps app
docker compose -f docker-compose.prod.yml logs --tail=200 app
curl -f http://127.0.0.1:8082/api/v3/api-docs
```

通常只需要更新 `app`，不需要重启 MySQL 和 Redis。如果 Compose 文件或 `.env` 同时变化，应先执行 `config` 检查。

## 6. 数据库备份

执行迁移、发布或其他高风险操作前先备份数据库。备份文件应保存到受保护目录，并定期复制到独立存储。

推荐使用带时间的文件名：

```bash
cd /home/app/yigongbao-java-prod-8082
docker exec yigongbao-mysql mysqldump -uroot -p'数据库密码' yigongbao > backup-$(date +%Y%m%d-%H%M%S).sql
ls -lh backup-*.sql
```

如果线上密码确实为 `root123456`，对应命令为：

```bash
docker exec yigongbao-mysql mysqldump -uroot -proot123456 yigongbao > backup-$(date +%Y%m%d).sql
```

命令行明文密码可能进入 Shell 历史或进程信息，生产环境应尽量使用受保护脚本或其他安全凭据方式。备份后确认文件大小合理且可读。

## 7. 执行 SQL 变更脚本

执行前必须完成数据库备份，并确认脚本已经审核、目标数据库和执行顺序正确。将脚本上传到服务器的 `sql/` 目录。

```bash
docker exec -i yigongbao-mysql mysql -uroot -p'数据库密码' yigongbao < /home/app/yigongbao-java-prod-8082/sql/migration-xxxx.sql
```

如果线上密码为 `root123456`：

```bash
docker exec -i yigongbao-mysql mysql -uroot -proot123456 yigongbao < /home/app/yigongbao-java-prod-8082/sql/migration-xxxx.sql
```

命令返回码为 `0` 才表示命令成功：

```bash
echo $?
docker compose -f docker-compose.prod.yml logs --tail=200 app
```

## 8. 恢复数据库

恢复可能覆盖现有数据。确认备份文件和目标环境后，建议先停止应用：

```bash
docker compose -f docker-compose.prod.yml stop app
docker exec -i yigongbao-mysql mysql -uroot -p'数据库密码' yigongbao < /path/to/backup-YYYYMMDD-HHMMSS.sql
docker compose -f docker-compose.prod.yml start app
docker compose -f docker-compose.prod.yml ps
```

恢复后检查应用日志、健康检查和关键业务数据。

## 9. 进入 MySQL 容器

```bash
docker exec -it yigongbao-mysql mysql -uroot -p
```

登录后：

```sql
USE yigongbao;
SHOW TABLES;
EXIT;
```

## 10. 应用日志

### 10.1 Docker 标准输出日志

导出应用容器当前已有的标准输出和错误输出：

```bash
docker logs yigongbao-app > yigongbao-app.log 2>&1
```

实时查看：

```bash
docker logs --tail=200 -f yigongbao-app
```

`docker logs` 导出的是容器标准输出，不等同于 Logback 文件日志。

### 10.2 Logback 文件日志

Logback 日志位于容器内的 `/app/logs`，通过 Compose 的 `app-logs` 命名卷持久化。当前配置为：

- 单个日志文件最大约 `10MB`。
- 保留最近 `7` 天。
- 应用启动时清理过期文件。
- Docker 标准输出日志暂不做切割。

查看文件日志：

```bash
docker exec yigongbao-app sh -c 'ls -lh /app/logs'
docker exec yigongbao-app sh -c 'tail -n 200 /app/logs/info.log'
```

查看卷的实际宿主机位置：

```bash
docker volume inspect yigongbao_app-logs
```

导出命名卷中的文件日志：

```bash
docker run --rm -v yigongbao_app-logs:/logs -v "$PWD":/backup alpine sh -c 'tar czf /backup/yigongbao-app-logs-$(date +%Y%m%d-%H%M%S).tar.gz -C /logs .'
```

不要直接删除 `app-logs` 卷，否则会丢失尚未导出的文件日志。

## 11. 常用运维命令

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml restart app
docker compose -f docker-compose.prod.yml stop app
docker compose -f docker-compose.prod.yml start app
docker stats --no-stream yigongbao-app yigongbao-mysql yigongbao-redis
docker volume ls | grep yigongbao
```

生产环境禁止随意执行：

```bash
docker compose -f docker-compose.prod.yml down -v
docker volume prune
```

## 12. 常见排障

应用启动失败：

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail=300 app
docker compose -f docker-compose.prod.yml logs --tail=200 mysql redis
```

健康检查失败：

```bash
curl -v http://127.0.0.1:8082/api/v3/api-docs
docker inspect --format='{{json .State.Health}}' yigongbao-app
```

MySQL 检查：

```bash
docker exec yigongbao-mysql mysqladmin ping -uroot -p
docker compose -f docker-compose.prod.yml logs --tail=200 mysql
```

应用连接 MySQL 时使用服务名 `mysql` 和容器端口 `3306`，不是宿主机映射端口 `3308`。

日志文件检查：

```bash
docker exec yigongbao-app sh -c 'ls -lh /app/logs'
docker inspect yigongbao-app --format='{{json .Mounts}}'
```

确认应用运行且 `app-logs` 已挂载到 `/app/logs`。

## 13. 发布检查清单

- [ ] `.env` 中生产密码、密钥和域名正确，权限为 `600`。
- [ ] `docker compose ... config` 检查通过。
- [ ] 数据库变更前已备份并确认备份文件可读。
- [ ] 已拉取最新应用镜像并重建 `app`。
- [ ] MySQL、Redis、App 均处于运行状态。
- [ ] `/api/v3/api-docs` 健康检查成功。
- [ ] 已检查应用启动日志和关键业务功能。
- [ ] `app-logs` 持久化卷存在，日志按 7 天策略清理。
