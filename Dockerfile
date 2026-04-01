# Usamos Java 21 (igual que tu proyecto)
FROM openjdk:21-jdk-slim

# Copiamos el jar
COPY target/crypto-data-platform-0.0.1-SNAPSHOT.jar app.jar

# Ejecutamos la app
ENTRYPOINT ["java", "-jar", "/app.jar"]