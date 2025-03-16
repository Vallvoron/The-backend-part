FROM maven:3.8.6-jdk-17 AS builder
WORKDIR /app
COPY . /app
RUN mvn clean install

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=$PORT", "-jar", "app.jar"]