FROM openjdk:17-jdk-alpine
COPY target/*.jar kanban-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "kanban-0.0.1-SNAPSHOT.jar"]

