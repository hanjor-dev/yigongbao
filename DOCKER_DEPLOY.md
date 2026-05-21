# Docker 部署快速参考

## 清空数据卷并重启

**警告：此操作会删除所有数据库数据和应用文件，不可恢复！**

### 测试环境

```bash
cd /home/app/yigongbao-java-8081

# 1. 停止并删除容器和数据卷
docker-compose -f docker-compose.test.yml down -v

# 2. 确认数据卷已删除
docker volume ls | grep yigongbao

# 3. 重新启动服务（会自动创建新的数据卷并初始化数据库）
docker-compose -f docker-compose.test.yml up -d

# 4. 查看启动日志
docker-compose -f docker-compose.test.yml logs -f
```

### 生产环境

```bash
cd /home/app/yigongbao-java-8081

# 1. 停止并删除容器和数据卷
docker-compose -f docker-compose.prod.yml down -v

# 2. 重新启动服务
docker-compose -f docker-compose.prod.yml up -d

# 3. 查看启动日志
docker-compose -f docker-compose.prod.yml logs -f
```

## 仅清空数据库（保留应用文件）

如果只想重置数据库，不删除上传的文件：

```bash
# 1. 停止服务
docker-compose -f docker-compose.test.yml stop

# 2. 删除 MySQL 和 Redis 数据卷
docker volume rm yigongbao_mysql-data
docker volume rm yigongbao_redis-data

# 3. 重新启动（保留 app-files 卷）
docker-compose -f docker-compose.test.yml up -d
```

## 数据备份（清空前建议备份）

```bash
# 备份 MySQL 数据库
docker exec yigongbao-mysql mysqldump -uroot -p${DB_PASSWORD} yigongbao > backup-$(date +%Y%m%d-%H%M%S).sql

# 备份应用文件
docker run --rm -v yigongbao_app-files:/data -v $(pwd):/backup alpine tar czf /backup/app-files-$(date +%Y%m%d-%H%M%S).tar.gz -C /data .
```

## 常见问题

### Q: down -v 会删除哪些内容？
A: 删除容器、网络和所有命名数据卷（mysql-data, redis-data, app-files）

### Q: 如何只重启应用不清空数据？
A: 使用 `docker-compose restart app` 或 `docker-compose up -d app`

### Q: 数据库初始化脚本在哪里？
A: `/home/app/yigongbao-java-8081/sql/` 目录，容器首次启动时自动执行
