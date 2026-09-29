# Java 17 (igual que <java.version> en pom.xml); solo JRE, no hace falta el JDK completo en runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copiamos el jar
COPY target/crypto-data-platform-0.0.1-SNAPSHOT.jar app.jar

# Ejecutamos la app. WORKDIR fija dónde caen las rutas relativas data/raw y data/processed
# (ver docker-compose.yml, que monta ./data en /app/data para que persistan entre reinicios).
ENTRYPOINT ["java", "-jar", "app.jar"]