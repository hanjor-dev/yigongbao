# 服务器环境配置指南

## 一、服务器环境准备

### 1. 安装 Docker（OpenCloudOS / CentOS）

```bash
# 安装依赖
sudo yum install -y yum-utils device-mapper-persistent-data lvm2

# 添加阿里云 Docker 仓库
sudo yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo

# 安装 Docker
sudo yum install -y docker-ce docker-ce-cli containerd.io

# 启动 Docker 服务
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
docker --version
```

### 2. 安装 Docker Compose

```bash
# 使用 GitHub 官方源下载
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

### 2. 配置 Docker 镜像加速（国内服务器推荐）

```bash
# 创建或编辑 Docker 配置文件
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live",
    "https://hub.rat.dev"
  ]
}
EOF

# 重启 Docker 服务
sudo systemctl daemon-reload
sudo systemctl restart docker
```

---

## 二、创建部署目录

```bash
# 创建项目目录
sudo mkdir -p /home/app/yigongbao-java-8081/{sql,logs}

# 设置目录权限
sudo chown -R $USER:$USER /home/app/yigongbao-java-8081

# 进入部署目录
cd /home/app/yigongbao-java-8081
```

**目录结构：**
```
/home/app/yigongbao-java-8081/
├── docker-compose.test.yml    # 测试环境编排文件
├── docker-compose.prod.yml    # 生产环境编排文件
├── env.test.text              # 测试环境变量（已废弃）
├── env.prod.text              # 生产环境变量（已废弃）
├── .env                       # 环境变量文件（docker-compose 默认读取）
├── sql/                       # 数据库初始化脚本
│   ├── ddl.sql
│   ├── init.sql
│   └── sys_area.sql
└── logs/                      # 应用日志目录（自动创建）
```

---

## 三、上传配置文件

将以下文件从本地上传到服务器 `/home/app/yigongbao-java-8081/` 目录：

### 1. Docker Compose 配置文件

- `docker-compose.test.yml` - 测试环境
- `docker-compose.prod.yml` - 生产环境

### 2. 环境变量文件

创建 `.env` 文件（测试/生产环境共用，根据实际环境修改）：

```bash
cat > .env <<'EOF'
# 数据库配置
DB_NAME=yigongbao
DB_USERNAME=root
DB_PASSWORD=root123456

# Redis 配置
REDIS_PASSWORD=redis123456

# 阿里云 OSS
OSS_ACCESS_KEY=你的AccessKey
OSS_SECRET_KEY=你的SecretKey
OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
OSS_BUCKET=yigongbao
OSS_DOMAIN=https://你的OSS域名/

# 邮件配置
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=your-password

# 系统配置
DEFAULT_PASSWORD=123456
APP_SIGN_SECRET=your-sign-secret-at-least-32-chars
SMS_MOCK_REDIRECT_EMAIL=test@example.com
EOF
```

**说明：**
- 测试环境和生产环境使用相同的文件名 `.env`
- 在不同服务器上根据实际环境修改文件内容
- docker-compose 会自动读取 `.env` 文件

### 3. 数据库初始化脚本

```bash
# 上传 SQL 文件到 sql/ 目录
# 方式1：使用 scp
scp sql/ddl.sql user@server:/home/app/yigongbao-java-8081/sql/
scp sql/init.sql user@server:/home/app/yigongbao-java-8081/sql/

# 方式2：使用 rsync
rsync -avz sql/ user@server:/home/app/yigongbao-java-8081/sql/
```

---

## 四、登录阿里云镜像仓库

```bash
# 使用你的阿里云账号和密码登录
docker login --username=你的阿里云账号 crpi-ie9dcy6o6rjohsiv.cn-hangzhou.personal.cr.aliyuncs.com

# 输入密码后，登录成功会显示：
# Login Succeeded
```

---

## 五、启动服务

### 测试环境部署

```bash
cd /home/app/yigongbao-java-8081

# 启动所有服务（docker-compose 自动读取 .env 文件）
docker-compose -f docker-compose.test.yml up -d

# 查看服务状态
docker-compose -f docker-compose.test.yml ps

# 查看应用日志
docker-compose -f docker-compose.test.yml logs -f app
```

### 生产环境部署

```bash
cd /home/app/yigongbao-java-8081

# 启动所有服务（docker-compose 自动读取 .env 文件）
docker-compose -f docker-compose.prod.yml up -d

# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看应用日志
docker-compose -f docker-compose.prod.yml logs -f app
```

---

## 六、日志管理

### 6.1 日志位置

**容器日志（实时输出）：**
```bash
# 查看应用日志
docker-compose -f docker-compose.test.yml logs -f app

# 查看 MySQL 日志
docker-compose -f docker-compose.test.yml logs -f mysql

# 查看 Redis 日志
docker-compose -f docker-compose.test.yml logs -f redis
```

**文件日志（持久化存储）：**

应用日志存储在 Docker 数据卷中，映射到宿主机：

```bash
# 查看日志数据卷位置
docker volume inspect yigongbao_app-files

# 日志文件位置（容器内）：
# /app/logs/yigongbao-info.log  - INFO 级别日志
# /app/logs/yigongbao-error.log - ERROR 级别日志

# 进入容器查看日志
docker exec -it yigongbao-app sh
cd /app/logs
tail -f yigongbao-info.log
```

### 6.2 日志配置

日志配置在 `logback-spring.xml` 中：

- **日志路径**：`/app/logs/`（容器内）
- **日志文件**：
  - `yigongbao-info.log` - INFO 及以上级别
  - `yigongbao-error.log` - ERROR 级别
- **滚动策略**：
  - 单文件最大 10MB
  - 保留 30 天
  - 自动压缩归档

### 6.3 日志查看命令

```bash
# 实时查看最新日志
docker-compose -f docker-compose.test.yml logs -f app

# 查看最近 100 行日志
docker-compose -f docker-compose.test.yml logs --tail=100 app

# 查看指定时间后的日志
docker-compose -f docker-compose.test.yml logs --since 2024-01-01T00:00:00 app

# 进入容器查看文件日志
docker exec -it yigongbao-app tail -f /app/logs/yigongbao-info.log
```

---

## 七、更新部署

### 自动构建流程

1. 本地推送代码到 GitHub
2. 阿里云自动构建镜像（10-20 分钟）
3. 服务器拉取最新镜像并重启

### 更新命令

```bash
cd /home/app/yigongbao-java-8081

# 拉取最新镜像
docker-compose -f docker-compose.test.yml pull app

# 重启应用
docker-compose -f docker-compose.test.yml up -d app

# 查看日志确认启动成功
docker-compose -f docker-compose.test.yml logs -f app
```

---

## 八、常用运维命令

```bash
# 查看服务状态
docker-compose -f docker-compose.test.yml ps

# 重启服务
docker-compose -f docker-compose.test.yml restart app

# 停止服务
docker-compose -f docker-compose.test.yml stop

# 停止并删除容器（保留数据卷）
docker-compose -f docker-compose.test.yml down

# 停止并删除容器和数据卷（清空数据库）
docker-compose -f docker-compose.test.yml down -v

# 进入应用容器
docker exec -it yigongbao-app sh

# 进入 MySQL 容器
docker exec -it yigongbao-mysql mysql -uroot -p

# 备份数据库
docker exec yigongbao-mysql mysqldump -uroot -p${DB_PASSWORD} yigongbao > backup-$(date +%Y%m%d).sql

# 查看容器资源占用
docker stats yigongbao-app yigongbao-mysql yigongbao-redis
```

---

## 九、健康检查

### 应用健康检查

```bash
# 检查 API 文档是否可访问
curl http://localhost:8081/api/v3/api-docs

# 检查 Swagger UI（测试环境）
curl http://localhost:8081/api/swagger-ui.html
```

### 数据库健康检查

```bash
# 进入 MySQL 容器
docker exec -it yigongbao-mysql mysql -uroot -p${DB_PASSWORD}

# 查看数据库
SHOW DATABASES;
USE yigongbao;
SHOW TABLES;
```

### Redis 健康检查

```bash
# 进入 Redis 容器
docker exec -it yigongbao-redis redis-cli -a ${REDIS_PASSWORD}

# 测试连接
PING
# 应返回：PONG
```

---

## 十、故障排查

### 1. 应用启动失败

```bash
# 查看详细日志
docker-compose -f docker-compose.test.yml logs app

# 检查环境变量是否正确
docker-compose -f docker-compose.test.yml config

# 检查容器状态
docker-compose -f docker-compose.test.yml ps
```

### 2. 数据库连接失败

```bash
# 检查 MySQL 是否启动
docker-compose -f docker-compose.test.yml ps mysql

# 查看 MySQL 日志
docker-compose -f docker-compose.test.yml logs mysql

# 手动测试连接
docker exec -it yigongbao-mysql mysql -h127.0.0.1 -P3306 -uroot -p
```

### 3. 镜像拉取失败

```bash
# 检查是否已登录阿里云镜像仓库
docker login crpi-ie9dcy6o6rjohsiv.cn-hangzhou.personal.cr.aliyuncs.com

# 手动拉取镜像
docker pull crpi-ie9dcy6o6rjohsiv.cn-hangzhou.personal.cr.aliyuncs.com/yigongbao/yigongbao-app:latest
```

### 4. 端口被占用

```bash
# 查看端口占用情况
sudo netstat -tulpn | grep 8081
sudo netstat -tulpn | grep 3308

# 停止占用端口的服务
sudo systemctl stop <service-name>
```

---

## 十一、安全建议

1. **修改默认密码**：部署后立即修改 `.env` 文件中的所有默认密码
2. **限制端口访问**：使用防火墙限制数据库端口（3308）仅本地访问
3. **定期备份**：设置定时任务备份数据库和重要文件
4. **日志监控**：定期检查错误日志，及时发现问题
5. **更新镜像**：定期更新 Docker 镜像，修复安全漏洞

```bash
# 配置防火墙（示例）
sudo ufw allow 8080/tcp   # 生产环境应用端口
sudo ufw allow 8081/tcp   # 测试环境应用端口
sudo ufw deny 3308/tcp    # 禁止外部访问数据库
sudo ufw deny 6379/tcp    # 禁止外部访问 Redis
```

---

## 十二、部署检查清单

部署前确认：

- [ ] Docker 已安装并启动
- [ ] 部署目录已创建（/home/app/yigongbao-java-8081）
- [ ] docker-compose.test.yml 已上传
- [ ] .env.test 已创建并配置正确
- [ ] sql/ 目录已上传初始化脚本
- [ ] 已登录阿里云镜像仓库
- [ ] 防火墙规则已配置
- [ ] 端口无冲突（8081, 3308, 6379）

部署后确认：

- [ ] 所有容器状态为 healthy
- [ ] API 文档可访问
- [ ] 数据库初始化成功
- [ ] Redis 连接正常
- [ ] 日志输出正常
- [ ] 文件上传功能正常（OSS）

---

**文档版本**：1.0  
**最后更新**：2026-05-20
