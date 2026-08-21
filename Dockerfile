# Build the Spring Boot application with the Maven wrapper included in the repo.
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests package

# Keep the deployed image small and run it as an unprivileged user.
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --create-home spring
COPY --from=build /app/target/*.jar app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
