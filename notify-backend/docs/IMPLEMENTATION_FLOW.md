# Notify — Backend Implementation Flow

Step-by-step development order. Each phase builds on the previous.

---

## Phase 1 — Infrastructure Setup

**Step 1: Docker Compose**
- Services: postgres, redis, kafka (KRaft mode), kafka-ui, pgadmin
- Single `docker compose up` starts everything
- Files: `docker-compose.yml`, `.env`

**Step 2: application.yml**
- Datasource, JPA, Kafka, Redis, Flyway config
- Separate profiles: `dev`, `test`

**Step 3: build.gradle — add missing dependencies**
- `flyway-core`, `flyway-database-postgresql`
- `redisson` or `spring-data-redis`
- `springdoc-openapi-starter-webmvc-ui`
- `micrometer-registry-prometheus`
- `apache-commons-csv`, `poi-ooxml` (Excel)
- `jackson-databind` (JSON upload)

---

## Phase 2 — Database Migrations (Flyway)

**Step 4: Flyway migrations V1–V11**
- V1: `frontend_clients`
- V2: `notification_campaigns`
- V3: `uploaded_files`
- V4: `users`
- V5: `notification_events`
- V6: `notification_delivery`
- V7: `delivery_attempts`
- V8: `retry_events`
- V9: `dlq_events`
- V10: `notification_templates`
- V11: `cohorts` + `cohort_users`

Switch `ddl-auto` from `update` → `validate` after migrations are in place.

---

## Phase 3 — Entities & Repositories

**Step 5: Entities**
- `FrontendClient`, `NotificationCampaign` (already exists — update to add `clientId` FK)
- `UploadedFile`, `User`, `NotificationEvent`, `NotificationDelivery`
- `DeliveryAttempt`, `RetryEvent`, `DlqEvent`, `NotificationTemplate`, `Cohort`

**Step 6: Enums**
- Move `ChannelType`, `CampaignStatus` to `enums/` package
- Add `DeliveryStatus`, `FileType`

**Step 7: Repositories**
- One `JpaRepository` per entity
- Custom queries: `findByCampaignId`, `findByStatus`, `findByEventId`

---

## Phase 4 — Client Registration

**Step 8: FrontendClient flow**
- `ClientController` → `POST /api/v1/clients/register`
- `ClientService`: generate UUID, hash(UUID + hostIp), store in DB + Redis
- Return `{ clientId, apiKey }` — apiKey shown only once

**Step 9: UuidAuthFilter**
- `OncePerRequestFilter` registered as first bean
- Validate `X-Client-UUID` header against Redis
- Attach `clientId` to request attribute for downstream use
- Exclude: `/api/v1/clients/register`, `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`

---

## Phase 5 — Campaign Management

**Step 10: Campaign CRUD**
- `CampaignController` — `POST`, `GET /`, `GET /{id}`, `DELETE /{id}`
- `CampaignService` — create, list (paginated), get, delete (soft delete: set status CANCELLED)
- Link campaign to `clientId` from request attribute

---

## Phase 6 — Kafka Infrastructure

**Step 11: KafkaConfig**
- Producer config: `StringSerializer` for key, `JsonSerializer` for value
- Consumer config: `StringDeserializer`, `JsonDeserializer`, `AUTO_OFFSET_RESET=earliest`
- Error handler: `DefaultErrorHandler` with `DeadLetterPublishingRecoverer`

**Step 12: KafkaTopicConfig**
- Define all 7 topics as `NewTopic` beans
- Partitions: 6 for main topics, 3 for retry/dlq

**Step 13: NotificationProducer**
- `send(String topic, String key, Object payload)`
- Routes directly: if EMAIL → `email-notification`, if SMS → `sms-notification`, if IN_APP → `in-app-notification`
- Uses `KafkaTemplate<String, Object>`

---

## Phase 7 — Single Notification Send

**Step 14: NotificationService**
- Validate campaign exists and belongs to client
- Check idempotency key in Redis (TTL 24h)
- Resolve user (upsert to `users` table by `externalUserId`)
- Persist `NotificationEvent` (status=PENDING)
- Publish `NotificationEventMessage` to `notification-requested`
- Cache idempotency result

**Step 15: Channel Consumers (EmailConsumer, SmsConsumer, InAppConsumer)**
- Each listens to its own topic (`email-notification`, `sms-notification`, `in-app-notification`)
- Simulate delivery (stub — log + random success/fail for now)
- On success: publish `SENT` to `notification-status`
- On failure: publish to `retry-notification` with `retryCount=0`

**Step 17: StatusConsumer**
- `@KafkaListener(topics = "notification-status")`
- Upsert `NotificationDelivery` row
- Update `NotificationEvent.status`
- Increment `campaign.sentCount` or `campaign.failedCount`

---

## Phase 8 — Bulk Upload

**Step 18: UploadController + UploadService**
- `POST /api/v1/notifications/bulk-upload` (multipart)
- Save `UploadedFile` record
- Dispatch async parsing task

**Step 19: File Parsers**
- `CsvParser`: Apache Commons CSV
- `ExcelParser`: Apache POI
- `JsonParser`: Jackson ObjectMapper
- All return `List<UserRow>` with `externalUserId`, `email`, `phone`

**Step 20: DeduplicationService (Cuckoo Filter)**
- Redis Cuckoo Filter via `CF.ADD` / `CF.EXISTS` / `CF.DEL` commands
- Use Lettuce custom command or Redisson `RBloomFilter` as fallback
- Key pattern: `cuckoo:campaign:<campaignId>`

**Step 21: Batch Kafka Publish**
- For each `UserRow`: check dedup → if new, upsert user, create event, publish
- Increment `campaign.duplicateCount` for duplicates
- Update `UploadedFile.status` to `COMPLETED`

---

## Phase 9 — Retry & DLQ Pipeline

**Step 22: RetryConsumer**
- `@KafkaListener(topics = "retry-notification")`
- Compute `backoff = 5^(retryCount+1)` seconds, sleep, re-attempt delivery
- Save `DeliveryAttempt` row on each attempt
- On success → publish `SENT` to `notification-status`
- On failure → increment `retryCount`, re-publish to `retry-notification`
- If `retryCount >= 3` → publish `DLQ` status to `notification-status` (with full payload for replay)

**Step 23: StatusConsumer handles DLQ**
- When `status = DLQ`: update `notification_delivery`, create `dlq_events` record, update `NotificationEvent.status = DLQ`
- No separate DLQ consumer needed

**Step 24: DlqController**
- `GET /api/v1/dlq` — paginated list
- `POST /api/v1/dlq/reprocess` — mark as replayed, republish directly to correct channel topic

---

## Phase 10 — Cohort Management

**Step 25: CohortController + CohortService**
- `POST /api/v1/cohorts` — create cohort
- `GET /api/v1/cohorts` — list for client
- `POST /api/v1/cohorts/{id}/users` — add users (bulk, with dedup)
- `DELETE /api/v1/cohorts/{id}/users/{userId}` — remove user
  - Deletes from `cohort_users`
  - Calls `DeduplicationService.delete(cohortId, userId)` on Cuckoo Filter

---

## Phase 11 — Dashboard & Metrics

**Step 26: DashboardService**
- Aggregate counts from DB (sent, failed, pending, dlq, duplicates)
- Kafka consumer lag via `AdminClient.listConsumerGroupOffsets()`
- Cache result in Redis for 30s (avoid DB hammering)

**Step 27: DashboardController**
- `GET /api/v1/dashboard/metrics`
- `GET /api/v1/consumer/lag`

**Step 28: NotificationMetrics (Micrometer)**
- Register counters on application start
- Increment from StatusConsumer and RetryConsumer
- Expose via `/actuator/prometheus`

---

## Phase 12 — Exception Handling & Validation

**Step 29: GlobalExceptionHandler**
- `@RestControllerAdvice`
- Handle: `MethodArgumentNotValidException` → 400
- Handle: `ClientNotFoundException`, `CampaignNotFoundException` → 404
- Handle: `DuplicateRequestException` → 409
- Handle: generic `Exception` → 500
- Consistent error body: `{ timestamp, status, error, path }`

---

## Phase 13 — Testing

**Step 30: Unit Tests**
- `NotificationServiceImpl` — mock repo + kafka, test idempotency
- `DeduplicationService` — mock Redis, test insert/exists/delete
- `UploadService` — test CSV/Excel parsing

**Step 31: Integration Tests (Testcontainers)**
- `PostgreSQL` container for repository layer tests
- `Kafka` container for producer/consumer round-trip tests
- `Redis` container for filter and auth filter tests

---

## Development Order Summary

```
Phase 1  → Docker + Config
Phase 2  → Flyway Migrations
Phase 3  → Entities + Repos
Phase 4  → Client Registration + Auth Filter
Phase 5  → Campaign CRUD
Phase 6  → Kafka Config + Topics + Producer
Phase 7  → Single Notification Flow (end-to-end Kafka)
Phase 8  → Bulk Upload + Deduplication
Phase 9  → Retry + DLQ + Replay
Phase 10 → Cohort Management
Phase 11 → Dashboard + Metrics
Phase 12 → Exception Handling
Phase 13 → Testing
```