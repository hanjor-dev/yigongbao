# 多阶段构建：第一阶段 - Maven 构建
FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /build

# 复制 pom 文件（利用 Docker 缓存层）
COPY yigongbao-parent/pom.xml ./
COPY yigongbao-parent/yigongbao-common/pom.xml ./yigongbao-common/
COPY yigongbao-parent/yigongbao-framework/pom.xml ./yigongbao-framework/
COPY yigongbao-parent/yigongbao-module-system/pom.xml ./yigongbao-module-system/
COPY yigongbao-parent/yigongbao-module-basic/pom.xml ./yigongbao-module-basic/
COPY yigongbao-parent/yigongbao-module-flow/pom.xml ./yigongbao-module-flow/
COPY yigongbao-parent/yigongbao-module-order/pom.xml ./yigongbao-module-order/
COPY yigongbao-parent/yigongbao-module-design/pom.xml ./yigongbao-module-design/
COPY yigongbao-parent/yigongbao-module-imaging/pom.xml ./yigongbao-module-imaging/
COPY yigongbao-parent/yigongbao-boot/pom.xml ./yigongbao-boot/

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY yigongbao-parent/ ./

# 构建项目
RUN mvn clean package -DskipTests -B

# 多阶段构建：第二阶段 - 运行时镜像
FROM amazoncorretto:21-alpine

WORKDIR /app

# 安装 curl（用于健康检查）
RUN apk add --no-cache curl

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从构建阶段复制 JAR 文件
COPY --from=builder /build/yigongbao-boot/target/yigongbao-boot-*.jar /app/app.jar

# 创建文件存储目录
RUN mkdir -p /app/files && chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# JVM 参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
