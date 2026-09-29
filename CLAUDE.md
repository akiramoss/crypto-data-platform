# Crypto Data Platform

Pipeline de ingesta de datos de criptomonedas con Spring Boot. Cada 5 min (configurable) el
scheduler llama a la API de CoinGecko, guarda los datos RAW en ficheros NDJSON, los transforma a
entidades, calcula la variación de precio respecto a la sesión anterior, las inserta en MySQL y
guarda una copia PROCESSED en ficheros. Una API REST permite consultar el histórico por symbol y
un ranking de las criptos más consistentemente alcistas. Un dashboard (dashboard/) visualiza esos
datos: precios más recientes, gráfico de histórico por symbol y el ranking.

## Stack
- Backend: Java 17, Spring Boot 3.3, Spring Data JPA, MySQL 8, Maven (wrapper), Lombok
- Spring Boot Actuator (health check)
- Frontend (dashboard/): React + Vite + TypeScript, Recharts, react-router-dom
- Docker + Docker Compose (MySQL + backend + dashboard/nginx)
- Tests backend: JUnit 5, Mockito y AssertJ (incluidos en spring-boot-starter-test)
- Tests frontend: Vitest + React Testing Library

## Estructura backend (paquete com.crypto_data_platform)
- client/      -> CryptoApiClient: llamada HTTP a CoinGecko (RestTemplate)
- config/      -> CryptoApiConfig (propiedades crypto.api.*), JacksonConfig, ClockConfig,
                   DashboardCorsProperties + CorsConfig (CORS solo para /api/**, origen del
                   dashboard vía dashboard.cors.allowed-origin)
- controller/  -> CryptoController: API REST propia (ver "API REST" abajo);
                   GlobalExceptionHandler: cuerpo de error consistente (ApiErrorResponse) para
                   toda la API vía @RestControllerAdvice
- dto/         -> CryptoApiResponse (respuesta de CoinGecko), CryptoPriceResponse,
                   CryptoRankingEntry y ApiErrorResponse (respuestas de nuestra propia API REST)
- mapper/      -> CryptoMapper: DTO -> entidad
- domain/      -> CryptoPrice: entidad JPA (único por symbol + event_time)
- repository/  -> CryptoRepository
- service/     -> CryptoService (orquesta el pipeline), RawDataService, ProcessedDataService,
                   PriceFluctuationService (variación % vs. sesión anterior por symbol),
                   CryptoRankingService (ranking de "densidad de ganancias"),
                   CryptoPriceQueryService (latest por symbol + histórico acotado por fechas,
                   pensado para el dashboard)
- scheduler/   -> CryptoScheduler (@Scheduled, intervalo en crypto.scheduler.fixed-rate-ms)

## Estructura frontend (dashboard/, ver dashboard/README.md)
- src/api/        -> client.ts: fetch wrapper, base URL desde VITE_API_BASE_URL
- src/types/       -> tipos que reflejan los DTOs del backend (mantener sincronizados a mano)
- src/pages/       -> OverviewPage, HistoryPage, RankingPage
- src/components/  -> tablas, gráfico (Recharts), estados de loading/error/empty compartidos
- src/hooks/        -> useAsync (loading/error/data + reload)
- src/utils/format.ts -> formateo de moneda, notación compacta (1.69T), porcentajes, fechas

## API REST
- GET /api/cryptos/{symbol}          -> histórico completo de ese symbol, más reciente primero,
                                          con su priceFluctuation (% vs. registro anterior), sin
                                          acotar. Lista vacía (200) si el symbol no tiene datos aún.
- GET /api/cryptos/latest            -> última cotización conocida de cada symbol (para el resumen
                                          del dashboard).
- GET /api/cryptos/{symbol}/history?from=&to= -> histórico acotado por rango de fechas (ISO-8601)
                                          para el gráfico del dashboard. from/to opcionales
                                          (default: últimos 30 días); rango máximo 180 días (ver
                                          CryptoPriceQueryService.MAX_RANGE_DAYS), 400 si se supera
                                          o si from > to.
- GET /api/cryptos/ranking?minSamples=3&limit=10 -> ranking por "densidad de ganancias": % de
                                          variaciones registradas que fueron positivas, por symbol.
                                          minSamples filtra symbols con pocos datos (default 3).
                                          minSamples/limit negativos -> 400.
- GET /actuator/health                -> health check (Spring Boot Actuator), incluye el estado
                                          de la conexión a la base de datos (indicador "db").

Todos los errores de validación devuelven un cuerpo JSON consistente (ApiErrorResponse) vía
GlobalExceptionHandler: {timestamp, status, error, message, path}.

## Comandos backend
- Compilar:      ./mvnw clean package        (Windows: .\mvnw clean package)
- Tests:         ./mvnw test
- Un test:       ./mvnw test -Dtest=NombreDelTest
- Docker (todo): ./mvnw clean package -DskipTests && docker-compose up --build
                  (arranca MySQL, backend y dashboard; dashboard en http://localhost:5173)

## Comandos frontend (dentro de dashboard/)
- Instalar:      npm install
- Dev server:    npm run dev   (necesita dashboard/.env con VITE_API_BASE_URL, ver .env.example)
- Build:         npm run build (type-check + build)
- Tests:         npm run test
- Lint:          npm run lint

## Entorno
- MySQL en Docker: puerto 3307 del host, base crypto_db, usuario cryptouser.
- application.properties apunta a localhost:3307 (coincide con el puerto expuesto por Docker).
- Los ficheros se escriben en data/raw y data/processed (ruta relativa).
- Logging SQL de Hibernate en DEBUG/TRACE solo con el perfil "dev" activo
  (application-dev.properties), no en el perfil por defecto.
- Dashboard en Docker: puerto 5173 del host (nginx sirve el build y hace proxy de /api/* al
  backend, mismo origen -> sin CORS). En dev local (npm run dev) el dashboard corre en un origen
  distinto al backend, por lo que necesita CORS: dashboard.cors.allowed-origin en
  application.properties (default http://localhost:5173, override vía DASHBOARD_CORS_ALLOWED_ORIGIN).

## Convenciones backend
- Inyección por constructor, nunca @Autowired en campos.
- Usar SLF4J (Logger) para logs, nunca System.out.
- Código y nombres en inglés; comentarios en español si aportan contexto.
- Los tests NO deben depender de MySQL real ni de la API real: usar mocks y un perfil
  de test (H2 en memoria o slices como @DataJpaTest).
- Cambios pequeños y verificables: ejecutar ./mvnw test tras cada cambio.

## Convenciones frontend (dashboard/)
- src/types/api.ts refleja los DTOs del backend a mano (no hay schema compartido); si cambia un
  DTO en dto/, actualizar ese fichero también.
- La URL base de la API viene siempre de VITE_API_BASE_URL (nunca hardcodeada); vacía = mismo
  origen (caso Docker/nginx).
- Cada vista (pages/) maneja explícitamente loading/empty/error, normalmente vía el hook useAsync.
- Cambios pequeños y verificables: ejecutar npm run test (y npm run build para type-check) tras
  cada cambio.
