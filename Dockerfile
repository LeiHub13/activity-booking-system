# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制 pom.xml 以利用依赖缓存
COPY pom.xml .
RUN mvn -q dependency:go-offline -B || true

# 复制源码并打包
COPY src ./src
RUN mvn -q clean package -DskipTests -B

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/target/activity-booking-system-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENV SERVER_ADDRESS=0.0.0.0 \
    TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "/app/app.jar"]