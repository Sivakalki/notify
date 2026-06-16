# Notify — High Level Design (HLD)

## 1. System Overview

Notify is a distributed notification delivery platform that allows a registered frontend client to:
- Create notification campaigns
- Upload bulk user cohorts (CSV / Excel / JSON)
- Deliver notifications across Email, SMS, and In-App channels
- Track delivery status, retry failures, and handle dead-letter events
- Monitor real-time analytics and Kafka consumer lag from a dashboard

---

## 2. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (React + Vite)                            │
│  Dashboard │ Campaign Mgmt │ Upload Cohort │ Analytics │ DLQ Console            │
└──────────────────────────────────┬──────────────────────────────────────────────┘
                                   │  REST (Axios, UUID header)
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      NOTIFY SPRING BOOT BACKEND (Java 21)                       │
│                                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │  Controllers │  │   Services   │  │ Kafka Producers│  │   Kafka Consumers│  │
│  │  /api/v1/*   │→ │  Business    │→ │ notification-  │  │ email / sms /    │  │
│  │              │  │  Logic       │  │ requested      │  │ in-app / retry / │  │
│  └──────────────┘  └──────────────┘  └────────────────┘  │ dlq / status     │  │
│         │                │                                └──────────────────┘  │
│  ┌──────┴──────┐  ┌──────┴──────┐                                               │
│  │  UUID       │  │ Cuckoo      │                                               │
│  │  Auth Filter│  │ Filter      │                                               │
│  │  (Redis)    │  │ (Redis)     │                                               │
│  └─────────────┘  └─────────────┘                                               │
└──────────────────────────┬──────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │ PostgreSQL  │ │    Redis    │ │    Kafka    │
    │             │ │             │ │             │
    │ - clients   │ │ - UUID keys │ │ 7 topics    │
    │ - campaigns │ │ - idempotcy │ │ see §5      │
    │ - events    │ │ - cuckoo    │ │             │
    │ - delivery  │ │   filter    │ │             │
    │ - dlq       │ │ - rate limit│ │             │
    └─────────────┘ └─────────────┘ └─────────────┘
```

---

## 3. Component Responsibilities

| Component | Responsibility |
|---|---|
| **Controllers** | Accept HTTP requests, validate headers, delegate to services |
| **UUID Auth Filter** | Validate `X-Client-UUID` header on every request using Redis |
| **Campaign Service** | Create/update campaigns, track status |
| **Upload Service** | Parse CSV/Excel/JSON, deduplicate, batch-publish to Kafka |
| **Deduplication Service** | Cuckoo Filter in Redis — insert, check, delete per cohort |
| **Notification Producer** | Routes directly to `email/sms/in-app-notification` topic based on channel |
| **Channel Consumers** | Email / SMS / In-App — attempt delivery, publish SENT or failure to retry topic |
| **Retry Consumer** | Reads `retry-notification`, applies exponential backoff, re-attempts delivery |
| **Status Consumer** | Reads `notification-status` — updates DB for SENT and DLQ outcomes |
| **Dashboard Service** | Aggregates DB + Kafka AdminClient metrics for the frontend |
| **Metrics** | Micrometer counters/gauges exposed to Prometheus |

---

## 4. Request Lifecycle — Single Notification

```
1. Frontend sends POST /api/v1/notifications/send  (X-Client-UUID: <uuid>)
2. UUIDAuthFilter validates UUID in Redis
3. NotificationController → NotificationService
4. Service checks idempotency key in Redis (TTL 24h)  → skip if duplicate
5. Service persists NotificationEvent (PENDING) to DB
6. NotificationProducer routes directly: EMAIL → email-notification,
                                          SMS   → sms-notification,
                                          IN_APP→ in-app-notification
7. ChannelConsumer attempts delivery
   ├─ Success → publishes SENT  to notification-status
   └─ Failure → publishes to retry-notification (retryCount=1)
8. StatusConsumer receives SENT → updates notification_delivery + campaign.sentCount
9. RetryConsumer: waits backoff (5^retryCount seconds), re-attempts delivery
   ├─ Success → publishes SENT to notification-status
   └─ Failure → re-publishes to retry-notification (retryCount++)
10. After 3 retries → RetryConsumer publishes DLQ status to notification-status
11. StatusConsumer receives DLQ → updates notification_delivery, creates dlq_events record
```

---

## 5. Kafka Topic Design

| Topic | Producer | Consumer | Partitions | Key |
|---|---|---|---|---|
| `email-notification` | NotificationProducer | EmailConsumer | 6 | userId |
| `sms-notification` | NotificationProducer | SmsConsumer | 6 | userId |
| `in-app-notification` | NotificationProducer | InAppConsumer | 6 | userId |
| `notification-status` | ChannelConsumers + RetryConsumer | StatusConsumer | 6 | eventId |
| `retry-notification` | ChannelConsumers + RetryConsumer | RetryConsumer | 3 | eventId |

---

## 6. Deduplication Strategy

### User-level (within a campaign)
- On CSV upload: for each userId, check Cuckoo Filter in Redis
- If present → skip, increment `duplicateCount`
- If absent → insert to filter, include in Kafka batch

### Request-level (API idempotency)
- Client sends `Idempotency-Key` header
- Backend checks Redis key `idempotency:<key>` (TTL 24h)
- If exists → return cached response
- If not → process, store result, return

### Why Cuckoo Filter over Bloom Filter
- Bloom does not support deletion — cohort user removal requires deletion
- Cuckoo has ~2x lower false positive rate at same memory
- Cuckoo supports lookup, insert, and delete

---

## 7. Retry & DLQ Pipeline

```
FAILED delivery
      │
      ▼
retry-notification (retryCount=1, backoff=5s)
      │
      ▼  (on failure)
retry-notification (retryCount=2, backoff=25s)
      │
      ▼  (on failure)
retry-notification (retryCount=3, backoff=125s)
      │
      ▼  (max retries exceeded)
notification-status (status=DLQ, full payload included)
      │
      ▼
StatusConsumer → dlq_events table  ←  DLQ Replay Console (frontend)
                                             POST /api/v1/dlq/reprocess
                                                   │
                                                   └──► re-routes to correct channel topic
```

Backoff formula: `5^retryCount` seconds.
StatusConsumer is the single writer for both SENT and DLQ outcomes — no separate DLQ consumer needed.

---

## 8. UUID Security Model

- Frontend registers once via `POST /api/v1/clients/register`
- Backend generates UUID, stores hash(UUID + host-ip) in Redis and DB
- All subsequent requests must carry `X-Client-UUID` header
- Filter verifies: hash matches stored value, request timestamp within ±5 min (replay prevention)
- Rate limiting via Redis sliding window counter per UUID

---

## 9. Database Design (Summary)

| Table | Purpose |
|---|---|
| `frontend_clients` | Registered frontend instances |
| `notification_campaigns` | Campaign metadata and counters |
| `uploaded_files` | File upload records per campaign |
| `users` | Deduped user registry |
| `notification_events` | One row per user per campaign (the unit of delivery) |
| `notification_delivery` | Final delivery outcome per event |
| `delivery_attempts` | Each retry attempt with timestamp and error |
| `retry_events` | Retry scheduling metadata |
| `dlq_events` | Dead-lettered events with raw payload |
| `notification_templates` | Reusable message templates per client |
| `cohorts` | Named user groups per client |

---

## 10. Infra (Docker Compose)

| Service | Port |
|---|---|
| postgres | 5432 |
| redis | 6379 |
| kafka (KRaft) | 9092 |
| kafka-ui | 8090 |
| pgadmin | 5050 |
| backend | 8080 |
| frontend | 5173 |

---

## 11. Observability

- **Micrometer + Prometheus**: counters for sent, failed, retried, DLQ'd
- **Kafka AdminClient**: consumer group lag per topic/partition
- **Spring Actuator**: health, metrics endpoints
- **Grafana** (optional future): dashboards on top of Prometheus
