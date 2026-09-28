# 🚀 Crypto Data Platform

A scalable **data engineering project** built with **Spring Boot**, designed to ingest, process, and store cryptocurrency market data using a real API.

---

# 🧠 Overview

This project simulates a **real-world data pipeline**:

```text
API → DTO → Service → Processing → Database + Filesystem
```

It fetches cryptocurrency data from CoinGecko, processes it, stores it in a MySQL database, and also saves raw and processed data locally.

---

# ⚙️ Tech Stack

* Java 17
* Spring Boot 3
* Spring Data JPA
* MySQL
* Docker & Docker Compose
* Maven
* REST API (CoinGecko)

---

# 📂 Project Structure

```text
crypto-data-platform
│
├── client        → API calls
├── controller    → Own REST API (query data, ranking)
├── dto           → API/response objects
├── mapper        → DTO → Entity conversion
├── domain        → Database entities
├── repository    → JPA repositories
├── service       → Business logic (ingestion, fluctuation, ranking)
├── scheduler     → Automated data ingestion
├── data
│   ├── raw       → Raw JSON (NDJSON format)
│   └── processed → Processed data
```

---

# 🔄 Data Flow

1. Fetch data from API
2. Convert DTO → Entity
3. Store RAW data (JSON)
4. Calculate price fluctuation vs. the previous stored record for that symbol
5. Save into MySQL
6. Save processed data

---

# 🐳 Run with Docker (Recommended)

## 1. Build the project

```bash
.\mvnw clean package -DskipTests
```

## 2. Configure credentials (optional)

Copy `.env.example` to `.env` and adjust the values if you don't want the defaults
below. `.env` is git-ignored and read automatically by `docker-compose`.

## 3. Run containers

```bash
docker-compose up --build
```

---

## 🔥 What happens automatically?

* MySQL starts on port **3307**
* Spring Boot app starts on port **8080**
* Scheduler runs and fetches crypto data
* Data is:

    * Saved in DB
    * Saved in `/data/raw`
    * Saved in `/data/processed`

---

# 🗄️ Database

### Connection

Defaults (override via `.env`, see above):

* Host: `localhost`
* Port: `3307`
* Database: `crypto_db`
* User: `cryptouser`
* Password: `crypto123`

---

# 📊 Example Table

```sql
crypto_price
```

Fields:

* id
* symbol
* price
* market_cap
* volume
* event_time
* timestamp
* price_fluctuation — % change vs. the previous stored record for the same symbol (`null` for a
  symbol's first-ever record)

---

# 🔌 REST API

* `GET /api/cryptos/{symbol}` — price history for that symbol (newest first), including its
  fluctuation. Symbols are matched case-insensitively. Returns an empty list (not a 404) if the
  symbol has no data yet.

  ```bash
  curl http://localhost:8080/api/cryptos/btc
  ```

* `GET /api/cryptos/ranking?minSamples=3&limit=10` — ranks all symbols by "gain density": the
  percentage of their recorded fluctuations that were positive (i.e. how consistently a coin goes
  up from one session to the next). `minSamples` (default 3) excludes symbols with too little
  history to be meaningful; `limit` (default 10) caps the result size.

  ```bash
  curl http://localhost:8080/api/cryptos/ranking
  ```

---

# 📁 Data Storage

## RAW Data (NDJSON)

```text
data/raw/crypto_YYYY-MM-DD_HH-mm-ss.json
```

Each line = one crypto record

---

## Processed Data

Stored after transformation for analytics or future pipelines.

---

# ⏱️ Scheduler

The system runs automatically, fetching crypto data every 5 minutes by default.

You can change the frequency via the `crypto.scheduler.fixed-rate-ms` property in
`application.properties` (value in milliseconds), no code changes needed.

---

# 🧪 Notes on Testing

The project has a full unit test suite (JUnit 5, Mockito, AssertJ) that mocks
external dependencies (DB, CoinGecko API), so `./mvnw test` runs without needing
a real database or network access.

`-DskipTests` is used only for the Docker build command above, to keep image
builds fast — tests are expected to be run separately via `./mvnw test`.

---

# 🚀 Features

* Real API integration
* Batch data processing
* Price fluctuation tracking per symbol, session over session
* "Gain density" ranking of the most consistently rising coins
* REST API to query a coin's history and the ranking
* Duplicate handling via DB constraints
* File-based raw data storage
* Dockerized environment
* Clean architecture (layered)

---

# 🧠 What You Learn From This Project

* Building a data ingestion pipeline
* Working with external APIs
* DTO → Entity mapping
* Handling persistence with JPA
* Using Docker for real environments
* Structuring scalable backend systems

---

# 📌 Future Improvements

* Add Kafka (streaming)
* Add Redis (caching)
* Add integration tests (e.g. Testcontainers against a real MySQL)
* Deploy to cloud (AWS / GCP)

---

# 👨‍💻 Author

Built by Iñaki Ramos Iturria as a **data engineering + backend learning project**.

---

# 🏁 Final Status

```text
PRODUCTION-READY BASE PROJECT ✅
```
---

👉 Ready to extend into a real data platform 🚀
