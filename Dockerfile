#FROM openjdk:17-jdk-alpine AS build-state
#WORKDIR app
#COPY . .
#RUN ./mvnw clean package -DskipTests

#FROM openjdk:17-jdk-alpine AS run
#COPY --from=build-state target/kanban.jar kanban.jar
#ENTRYPOINT ["java","-jar","kanban.jar"]

#single stage
#FROM openjdk:17-jdk-alpine AS build-state
#WORKDIR app
#COPY . .
#RUN ./mvnw clean package
#CMD ["java","-jar","target/kanban.jar"]

# Stage 1: Build the application
FROM openjdk:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

