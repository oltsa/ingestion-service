FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .

RUN mvn clean verify

FROM gcr.io/distroless/java21-debian12

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]