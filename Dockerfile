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
COPY yigongbao-parent/yigongbao-module-production/pom.xml ./yigongbao-module-production/
COPY yigongbao-parent/yigongbao-module-notification/pom.xml ./yigongbao-module-notification/
COPY yigongbao-parent/yigongbao-module-dashboard/pom.xml ./yigongbao-module-dashboard/
COPY yigongbao-parent/yigongbao-boot/pom.xml ./yigongbao-boot/

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY yigongbao-parent/ ./

# 构建项目
RUN mvn clean package -DskipTests -B

# 多阶段构建：第二阶段 - 运行时镜像（生产环境）
FROM eclipse-temurin:21-jre

WORKDIR /app

# 创建非 root 用户
RUN groupadd -r appuser && useradd -r -g appuser -u 1001 appuser

# 从构建阶段复制 JAR 文件
COPY --from=builder /build/yigongbao-boot/target/yigongbao-boot-*.jar /app/app.jar

# 创建文件存储目录并设置权限
RUN mkdir -p /app/files && chown -R appuser:appuser /app

# 切换到非 root 用户
USER appuser

# 暴露端口（HTTP API）
EXPOSE 8082

# JVM 参数优化（生产环境）
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/files -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
