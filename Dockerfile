FROM node:20-alpine AS frontend-build

WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.16-eclipse-temurin-17 AS backend-build

WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src/ ./src/
COPY --from=frontend-build /app/frontend/dist/ ./src/main/resources/static/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=backend-build /app/target/healthcare-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
