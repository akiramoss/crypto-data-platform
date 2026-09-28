# Java 17 (igual que <java.version> en pom.xml); solo JRE, no hace falta el JDK completo en runtime
FROM eclipse-temurin:17-jre-jammy

# Copiamos el jar
COPY target/crypto-data-platform-0.0.1-SNAPSHOT.jar app.jar

# Ejecutamos la app
ENTRYPOINT ["java", "-jar", "/app.jar"]