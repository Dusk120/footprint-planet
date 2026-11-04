# ===== 构建阶段：Maven 打 jar =====
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 先只拷贝 pom.xml，利用层缓存预下载依赖（改源码不触发重新下载）
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

# 再拷贝源码并打包
COPY src ./src
RUN mvn -B -q -DskipTests package

# ===== 运行阶段：JRE 跑 jar =====
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# 复制打包出的可执行 jar（artifactId 与版本可变，用通配符）
COPY --from=builder /app/target/*.jar app.jar

# JVM 堆由 JAVA_OPTS 控制，docker-compose 里可压低以适配小内存
ENV JAVA_OPTS=""

# 后端监听 8080（README 约定）
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
