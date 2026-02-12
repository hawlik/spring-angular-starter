# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY apps/api/pom.xml .
COPY apps/api/.mvn .mvn
COPY apps/api/mvnw .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY apps/api/src src
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
