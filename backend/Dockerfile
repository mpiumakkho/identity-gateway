FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src

RUN chmod +x mvnw && ./mvnw -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/backend/target/identity-gateway-backend-*.jar /app/identity-gateway-backend.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/identity-gateway-backend.jar"]
