FROM eclipse-temurin:21-jre
COPY build/libs/server-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=local", "-jar", "app.jar"]
