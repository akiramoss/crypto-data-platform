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
├── dto           → API response objects
├── mapper        → DTO → Entity conversion
├── domain        → Database entities
├── repository    → JPA repositories
├── service       → Business logic
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
4. Process data
5. Save into MySQL
6. Save processed data

---

# 🐳 Run with Docker (Recommended)

## 1. Build the project

```bash
.\mvnw clean package -DskipTests
```

## 2. Run containers

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

* Host: `localhost`
* Port: `3307`
* Database: `crypto`
* User: `root`
* Password: `root`

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
* time_stamp

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

The system runs automatically:

```text
Fetch crypto data periodically
```

You can modify frequency in:

```java
CryptoScheduler.java
```

---

# 🧪 Notes on Testing

Tests are disabled during build:

```bash
-DskipTests
```

Reason:

* The project depends on external systems (DB, API)
* Integration tests should be added later

---

# 🚀 Features

* Real API integration
* Batch data processing
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
* Add REST endpoints for querying data
* Add proper integration tests
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
