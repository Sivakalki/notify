# Notify — Backend

Spring Boot REST API with Kafka producers/consumers, PostgreSQL persistence, and Redis-based deduplication.

See the [root README](../README.md) for the full architecture and project overview.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21 |
| Docker | 24+ |
| Docker Compose | v2 |

No need to install Kafka, Redis, or PostgreSQL locally — Docker Compose handles them.

---

## Infrastructure Setup

The `Makefile` in this directory wraps Docker Compose. All services are defined in `docker-compose.yml`.

```bash
# Start Kafka + Kafka-UI + Redis only (assumes local Postgres)
make dev

# Start everything including Postgres + pgAdmin
make up

# Stop containers (keep volumes)
make down

# Full reset — stop and wipe all volumes
make down-v

# Tail logs
make logs

# Check running services
make ps
```

### Services and ports

| Service | Port | URL |
|---|---|---|
| PostgreSQL | 5432 | — |
| Redis Stack | 6379 | — |
| Kafka | 9092 | — |
| Kafka UI | 8082 | http://localhost:8082 |
| pgAdmin | 5050 | http://localhost:5050 |

---

## Running the Backend

```bash
./gradlew bootRun
```

The API starts on **http://localhost:8080**.

Swagger UI: **http://localhost:8080/swagger-ui.html**

### Environment variables

All defaults work out of the box with the Docker Compose setup. Override via environment variables or `application.yml`:

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `notify` | Database name |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `admin` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka broker address |

---

## Database Migrations

Flyway runs automatically on startup. Migration scripts live in:

```
src/main/resources/db/migration/
```

Files follow the naming convention `V{version}__{description}.sql`. Flyway applies them in order and tracks applied versions in the `flyway_schema_history` table.

---

## Project Structure

```
src/main/java/com/notify/backend/
├── controller/     REST endpoints
├── service/        Business logic
├── repository/     JPA repositories
├── kafka/          Producers and consumers
├── config/         Kafka, Redis, Async, CORS config
├── dto/            Request/response DTOs per domain
├── entity/         Hibernate entities
├── mapper/         Entity ↔ DTO mapping
├── metrics/        Micrometer custom metrics
├── filter/         Request filters (signature validation)
├── exception/      Global exception handler
└── util/           Shared utilities
```

---

## Kafka Topics

| Topic | Purpose |
|---|---|
| `notification-requested` | Entry point; router distributes to channel topics |
| `email-notification` | Email channel consumer input |
| `sms-notification` | SMS channel consumer input |
| `in-app-notification` | In-app channel consumer input |
| `retry-notification` | Exponential backoff retry queue |
| `dead-letter-notification` | Final DLQ for unrecoverable failures |
| `notification-status` | Delivery status events back to the API |

---

## Testing

```bash
# Run all tests (Testcontainers spins up real Postgres + Kafka)
./gradlew test

# Run a specific test class
./gradlew test --tests "com.notify.backend.service.NotificationServiceTest"
```

Testcontainers pulls Docker images on first run — ensure Docker is running before executing tests.

---

## Verify Redis Cuckoo Filter

The deduplication layer requires `redis-stack-server` (not plain Redis) for Cuckoo Filter commands.

```bash
make verify-redis
```

This spins up a temporary container, runs `CF.ADD` / `CF.EXISTS` / `CF.DEL`, then cleans up.
