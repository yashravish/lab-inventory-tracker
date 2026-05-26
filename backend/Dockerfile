# syntax=docker/dockerfile:1.6

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache deps first
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && cp target/*.jar /workspace/app.jar

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/app.jar /app/app.jar
USER app

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

# Railway injects PORT; application.properties binds server.port to ${PORT}.
EXPOSE 8082

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
