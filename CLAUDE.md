# Crypto Data Platform

Pipeline de ingesta de datos de criptomonedas con Spring Boot. Cada 60 s el scheduler
llama a la API de CoinGecko, guarda los datos RAW en ficheros NDJSON, los transforma a
entidades, los inserta en MySQL y guarda una copia PROCESSED en ficheros.

## Stack
- Java 17, Spring Boot 3.3, Spring Data JPA, MySQL 8, Maven (wrapper), Lombok
- Docker + Docker Compose
- Tests: JUnit 5, Mockito y AssertJ (incluidos en spring-boot-starter-test)

## Estructura (paquete com.crypto_data_platform)
- client/      -> CryptoApiClient: llamada HTTP a CoinGecko (RestTemplate)
- config/      -> CryptoApiConfig (propiedades crypto.api.*), JacksonConfig
- dto/         -> CryptoApiResponse: respuesta de la API
- mapper/      -> CryptoMapper: DTO -> entidad
- domain/      -> CryptoPrice: entidad JPA (único por symbol + event_time)
- repository/  -> CryptoRepository
- service/     -> CryptoService (orquesta el pipeline), RawDataService, ProcessedDataService
- scheduler/   -> CryptoScheduler (@Scheduled cada 60 s)

## Comandos
- Compilar:      ./mvnw clean package        (Windows: .\mvnw clean package)
- Tests:         ./mvnw test
- Un test:       ./mvnw test -Dtest=NombreDelTest
- Docker:        ./mvnw clean package -DskipTests && docker-compose up --build

## Entorno
- MySQL en Docker: puerto 3307 del host, base crypto_db, usuario cryptouser.
- application.properties apunta a localhost:3306 (no coincide con Docker).
- Los ficheros se escriben en data/raw y data/processed (ruta relativa).

## Convenciones
- Inyección por constructor, nunca @Autowired en campos.
- Usar SLF4J (Logger) para logs, nunca System.out.
- Código y nombres en inglés; comentarios en español si aportan contexto.
- Los tests NO deben depender de MySQL real ni de la API real: usar mocks y un perfil
  de test (H2 en memoria o slices como @DataJpaTest).
- Cambios pequeños y verificables: ejecutar ./mvnw test tras cada cambio.
