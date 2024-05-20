# Stage 1: Build
FROM openjdk:17-jdk-alpine AS build
WORKDIR /workspace/app

# Copy Maven wrapper and related files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code and build the application
COPY src src
RUN ./mvnw install -DskipTests

# Extract the jar file using Spring Boot Layertools
RUN mkdir -p target/extracted && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Stage 2: Run
FROM openjdk:17-jdk-alpine AS run
WORKDIR /workspace/app

# Set the extracted directory
ARG EXTRACTED=/workspace/app/target/extracted

# Copy the extracted layers
COPY --from=build ${EXTRACTED}/dependencies/ ./
COPY --from=build ${EXTRACTED}/spring-boot-loader/ ./
COPY --from=build ${EXTRACTED}/snapshot-dependencies/ ./
COPY --from=build ${EXTRACTED}/application/ ./

# Expose port and set the entrypoint
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]