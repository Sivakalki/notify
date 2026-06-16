# PingFlow — Distributed Notification Delivery Platform

## Planned Document Structure

### 1. Project Vision

* Problem statement
* Why this system exists
* Real-world business use case
* Goals & non-goals

### 2. High-Level Architecture

* End-to-end architecture diagram (Mermaid)
* Kafka producer/consumer flow
* Frontend → Backend → Kafka → Channel consumers
* Retry & DLQ pipeline
* Deduplication architecture
* Service lag monitoring

### 3. Complete Tech Stack

#### Frontend

* React + Vite
* TailwindCSS
* shadcn/ui
* TanStack Router
* TanStack Query
* Recharts (analytics)
* Axios
* Zustand (optional)

#### Backend

* Java 21
* Spring Boot 3
* Spring Kafka
* Spring Data JPA (Hibernate)
* PostgreSQL
* Redis
* Bloom Filter vs Cuckoo Filter decision
* Micrometer + Prometheus metrics
* OpenAPI/Swagger
* Flyway migrations

#### Infra

* Docker Compose
* Kafka
* Zookeeper/KRaft
* Redis
* PostgreSQL
* pgAdmin
* Kafka UI

#### Testing

* Testcontainers
* JUnit 5
* Mockito
* Integration testing strategy

### 4. Project Requirements

#### Functional Requirements

* Single frontend UUID-based access
* Notification creation
* Bulk upload (CSV/Excel/JSON)
* User deduplication
* Multi-channel delivery
* Retry mechanism
* DLQ handling
* Dashboard analytics
* Consumer lag monitoring
* Notification history
* Cohort management
* User removal from cohort

#### Non-Functional Requirements

* Scalability
* Fault tolerance
* Reliability
* Idempotency
* High throughput
* Eventual consistency

### 5. UUID Security Design

* No auth system
* Frontend registration UUID
* Hashed with frontend host/IP
* Request signature validation
* Replay attack prevention
* API key rotation strategy
* Rate limiting

### 6. Detailed System Flow

#### Flow 1 — Single Notification

User → API → Kafka → Consumers → Delivery Tracking

#### Flow 2 — Bulk CSV Upload

CSV Upload → Parsing → Validation → Deduplication → Kafka Publish

#### Flow 3 — Retry Pipeline

Failure → Retry Topic → Exponential Backoff → DLQ

#### Flow 4 — User Removal

Cuckoo Filter deletion flow

### 7. Kafka Design

#### Topics

* notification-requested
* email-notification
* sms-notification
* in-app-notification
* retry-notification
* dead-letter-notification
* notification-status

#### Partition strategy

* userId partitioning
* ordering guarantees
* throughput decisions

#### Consumer groups

* Email Consumer
* SMS Consumer
* In-App Consumer

### 8. Database Design

Complete PostgreSQL schema.

Tables:

* frontend_clients
* notification_campaigns
* uploaded_files
* users
* notification_events
* notification_delivery
* delivery_attempts
* retry_events
* dlq_events
* notification_templates
* cohorts

With:

* full schema
* entity relationships
* indexes
* optimization strategy

### 9. Hibernate Entity Structure

* Entity classes
* relationships
* JPA mappings
* lazy vs eager loading
* performance optimizations

### 10. Redis Usage

* request deduplication
* idempotency keys
* caching metrics
* rate limiting

### 11. Cuckoo Filter Design

Why Cuckoo > Bloom Filter here:

* supports deletion
* lower memory
* better cohort management

Implementation strategy:

* insertion
* membership check
* deletion

### 12. Frontend Architecture

Folder structure:
src/
├── components/
├── routes/
├── services/
├── hooks/
├── store/
├── pages/
├── layouts/
├── charts/
└── lib/

Dashboard Pages:

* Overview Dashboard
* Campaign Management
* Upload Cohort
* Notification Analytics
* Consumer Lag Monitoring
* Failed Notifications
* DLQ Replay Console

### 13. Backend Architecture

Folder structure:
src/main/java/com/pingflow/
├── controller
├── service
├── repository
├── kafka
├── config
├── dto
├── entity
├── mapper
├── scheduler
├── metrics
├── filter
├── exception
└── util

### 14. REST API Design

Complete API list.

Examples:
POST /api/v1/notifications/send

POST /api/v1/notifications/bulk-upload

GET /api/v1/dashboard/metrics

GET /api/v1/consumer/lag

GET /api/v1/campaigns

POST /api/v1/cohort/remove-user

POST /api/v1/dlq/reprocess

Including:

* request/response bodies
* status codes
* validation

### 15. Dashboard Metrics

* notifications raised
* sent
* pending
* failed
* retry count
* duplicate users removed
* Kafka lag
* throughput/sec
* channel-wise success rate

### 16. Docker Setup

Single command:
docker compose up

Complete setup for:

* backend
* frontend
* postgres
* redis
* kafka
* kafka-ui

### 17. Testing Strategy

* Unit testing
* Integration testing
* Kafka testing
* DB testing
* Testcontainers examples

### 18. Load Testing

Using k6.

Metrics to measure:

* throughput
* latency
* retry rate
* deduplication performance

### 19. Resume Framing

How to present this project on resume.

Strong bullet points.

### 20. Interview Storytelling

How to explain:

* Kafka decisions
* deduplication
* retry handling
* DLQ
* idempotency
* scaling

### 21. Future Enhancements

* WebSocket live updates
* template builder
* multi-tenant support
* scheduling notifications
* analytics engine
* Kubernetes deployment
