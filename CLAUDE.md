# Crypto Data Platform

Pipeline de ingesta de datos de criptomonedas con Spring Boot. Cada 5 min (configurable) el
scheduler llama a la API de CoinGecko, guarda los datos RAW en ficheros NDJSON, los transforma a
entidades, calcula la variación de precio respecto a la sesión anterior, las inserta en MySQL y
guarda una copia PROCESSED en ficheros. Una API REST permite consultar el histórico por symbol y
un ranking de las criptos más consistentemente alcistas.

## Stack
- Java 17, Spring Boot 3.3, Spring Data JPA, MySQL 8, Maven (wrapper), Lombok
- Spring Boot Actuator (health check)
- Docker + Docker Compose
- Tests: JUnit 5, Mockito y AssertJ (incluidos en spring-boot-starter-test)

## Estructura (paquete com.crypto_data_platform)
- client/      -> CryptoApiClient: llamada HTTP a CoinGecko (RestTemplate)
- config/      -> CryptoApiConfig (propiedades crypto.api.*), JacksonConfig
- controller/  -> CryptoController: API REST propia (GET /api/cryptos/{symbol}, /ranking)
- dto/         -> CryptoApiResponse (respuesta de CoinGecko), CryptoPriceResponse y
                   CryptoRankingEntry (respuestas de nuestra propia API REST)
- mapper/      -> CryptoMapper: DTO -> entidad
- domain/      -> CryptoPrice: entidad JPA (único por symbol + event_time)
- repository/  -> CryptoRepository
- service/     -> CryptoService (orquesta el pipeline), RawDataService, ProcessedDataService,
                   PriceFluctuationService (variación % vs. sesión anterior por symbol),
                   CryptoRankingService (ranking de "densidad de ganancias")
- scheduler/   -> CryptoScheduler (@Scheduled, intervalo en crypto.scheduler.fixed-rate-ms)

## API REST
- GET /api/cryptos/{symbol}          -> histórico de precios de ese symbol, más reciente primero,
                                          con su priceFluctuation (% vs. registro anterior). Lista
                                          vacía (200) si el symbol no tiene datos aún.
- GET /api/cryptos/ranking?minSamples=3&limit=10 -> ranking por "densidad de ganancias": % de
                                          variaciones registradas que fueron positivas, por symbol.
                                          minSamples filtra symbols con pocos datos (default 3).
- GET /actuator/health                -> health check (Spring Boot Actuator), incluye el estado
                                          de la conexión a la base de datos (indicador "db").

## Comandos
- Compilar:      ./mvnw clean package        (Windows: .\mvnw clean package)
- Tests:         ./mvnw test
- Un test:       ./mvnw test -Dtest=NombreDelTest
- Docker:        ./mvnw clean package -DskipTests && docker-compose up --build

## Entorno
- MySQL en Docker: puerto 3307 del host, base crypto_db, usuario cryptouser.
- application.properties apunta a localhost:3307 (coincide con el puerto expuesto por Docker).
- Los ficheros se escriben en data/raw y data/processed (ruta relativa).
- Logging SQL de Hibernate en DEBUG/TRACE solo con el perfil "dev" activo
  (application-dev.properties), no en el perfil por defecto.

## Convenciones
- Inyección por constructor, nunca @Autowired en campos.
- Usar SLF4J (Logger) para logs, nunca System.out.
- Código y nombres en inglés; comentarios en español si aportan contexto.
- Los tests NO deben depender de MySQL real ni de la API real: usar mocks y un perfil
  de test (H2 en memoria o slices como @DataJpaTest).
- Cambios pequeños y verificables: ejecutar ./mvnw test tras cada cambio.
