# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Download dependencies first: this layer is cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Tests run with ./mvnw test before building the image.
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: run the jar on a small Java runtime ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Do not run as root inside the container.
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
