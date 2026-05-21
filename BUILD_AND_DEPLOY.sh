#!/bin/bash
# 本地构建并推送到阿里云镜像仓库

set -e

echo "=========================================="
echo "开始本地构建 Docker 镜像"
echo "=========================================="

# 1. 登录阿里云镜像仓库
echo "步骤 1/4: 登录阿里云镜像仓库..."
docker login --username=18251917668@163.com crpi-ie9dcy6o6rjohsiv.cn-hangzhou.personal.cr.aliyuncs.com

# 2. 构建镜像
echo "步骤 2/4: 构建 Docker 镜像（预计 5-10 分钟）..."
docker build -t yigongbao-app:local .

# 3. 打标签
echo "步骤 3/4: 打标签..."
docker tag yigongbao-app:local crpi-ie9dcy6o6rjohsiv.cn-hangzhou.personal.cr.aliyuncs.com/yigongbao/yigongbao-app:latest

# 4. 推送到阿里云
echo "步骤 4/4: 推送到阿里云（预计 3-5 分钟）..."
docker push crpi-ie9dcy6o6rjohsiv.cn-hangzhou.personal.cr.aliyuncs.com/yigongbao/yigongbao-app:latest

echo "=========================================="
echo "构建并推送完成！"
echo "=========================================="
echo ""
echo "下一步：在服务器执行以下命令更新部署"
echo "cd /home/app/yigongbao-java-8081"
echo "docker-compose -f docker-compose.test.yml pull app"
echo "docker-compose -f docker-compose.test.yml up -d app"
