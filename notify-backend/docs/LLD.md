# Notify — Low Level Design (LLD)

---

## 1. Package Structure

```
src/main/java/com/notify/backend/
├── controller/
│   ├── ClientController.java
│   ├── CampaignController.java
│   ├── NotificationController.java
│   ├── CohortController.java
│   ├── DashboardController.java
│   ├── DlqController.java
│   └── HealthController.java
├── service/
│   ├── ClientService.java
│   ├── CampaignService.java
│   ├── NotificationService.java
│   ├── UploadService.java
│   ├── CohortService.java
│   ├── DashboardService.java
│   ├── DlqService.java
│   ├── DeduplicationService.java
│   └── impl/
│       ├── ClientServiceImpl.java
│       ├── CampaignServiceImpl.java
│       ├── NotificationServiceImpl.java
│       ├── UploadServiceImpl.java
│       ├── CohortServiceImpl.java
│       ├── DashboardServiceImpl.java
│       ├── DlqServiceImpl.java
│       └── DeduplicationServiceImpl.java
├── repository/
│   ├── FrontendClientRepository.java
│   ├── NotificationCampaignRepository.java
│   ├── UploadedFileRepository.java
│   ├── UserRepository.java
│   ├── NotificationEventRepository.java
│   ├── NotificationDeliveryRepository.java
│   ├── DeliveryAttemptRepository.java
│   ├── RetryEventRepository.java
│   ├── DlqEventRepository.java
│   ├── NotificationTemplateRepository.java
│   └── CohortRepository.java
├── kafka/
│   ├── KafkaTopics.java             ← topic name constants
│   ├── producer/
│   │   └── NotificationProducer.java
│   ├── consumer/
│   │   ├── EmailConsumer.java
│   │   ├── SmsConsumer.java
│   │   ├── InAppConsumer.java
│   │   ├── RetryConsumer.java
│   │   └── StatusConsumer.java      ← handles both SENT and DLQ outcomes
│   └── config/
│       └── KafkaTopicConfig.java
├── config/
│   ├── KafkaConfig.java
│   ├── RedisConfig.java
│   └── OpenApiConfig.java
├── dto/
│   ├── client/
│   │   ├── RegisterClientRequest.java
│   │   └── RegisterClientResponse.java
│   ├── campaign/
│   │   ├── CreateCampaignRequest.java
│   │   └── CampaignResponse.java
│   ├── notification/
│   │   ├── SendNotificationRequest.java
│   │   ├── NotificationEventMessage.java   ← Kafka payload
│   │   └── NotificationStatusMessage.java  ← Kafka payload
│   ├── cohort/
│   │   ├── CreateCohortRequest.java
│   │   ├── CohortResponse.java
│   │   └── RemoveUserRequest.java
│   ├── dashboard/
│   │   ├── DashboardMetricsResponse.java
│   │   └── ConsumerLagResponse.java
│   └── dlq/
│       ├── DlqEventResponse.java
│       └── ReprocessRequest.java
├── entity/
│   ├── FrontendClient.java
│   ├── NotificationCampaign.java
│   ├── UploadedFile.java
│   ├── User.java
│   ├── NotificationEvent.java
│   ├── NotificationDelivery.java
│   ├── DeliveryAttempt.java
│   ├── RetryEvent.java
│   ├── DlqEvent.java
│   ├── NotificationTemplate.java
│   └── Cohort.java
├── enums/
│   ├── ChannelType.java       (EMAIL, SMS, IN_APP)
│   ├── CampaignStatus.java    (PENDING, IN_PROGRESS, COMPLETED, FAILED)
│   ├── DeliveryStatus.java    (PENDING, SENT, FAILED, RETRYING, DLQ)
│   └── FileType.java          (CSV, EXCEL, JSON)
├── filter/
│   └── UuidAuthFilter.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ClientNotFoundException.java
│   ├── DuplicateRequestException.java
│   └── CampaignNotFoundException.java
├── metrics/
│   └── NotificationMetrics.java
├── scheduler/
│   └── MetricsRefreshScheduler.java
├── util/
│   ├── CsvParser.java
│   ├── ExcelParser.java
│   ├── JsonParser.java
│   └── IdempotencyUtil.java
└── NotifyBackendApplication.java
```

---

## 2. Database Schema (Flyway Migrations)

### V1 — frontend_clients
```sql
CREATE TABLE frontend_clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    api_key_hash VARCHAR(512) NOT NULL UNIQUE,  -- hash(uuid + host_ip)
    host_ip     VARCHAR(45),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
```

### V2 — notification_campaigns
```sql
CREATE TABLE notification_campaigns (
    id              BIGSERIAL PRIMARY KEY,
    client_id       UUID NOT NULL REFERENCES frontend_clients(id),
    campaign_name   VARCHAR(255) NOT NULL,
    message         TEXT NOT NULL,
    channel         VARCHAR(20) NOT NULL,   -- EMAIL | SMS | IN_APP
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_users     INT DEFAULT 0,
    sent_count      INT DEFAULT 0,
    failed_count    INT DEFAULT 0,
    duplicate_count INT DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_campaigns_client ON notification_campaigns(client_id);
CREATE INDEX idx_campaigns_status ON notification_campaigns(status);
```

### V3 — uploaded_files
```sql
CREATE TABLE uploaded_files (
    id          BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES notification_campaigns(id),
    file_name   VARCHAR(255) NOT NULL,
    file_type   VARCHAR(10) NOT NULL,   -- CSV | EXCEL | JSON
    row_count   INT DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
```

### V4 — users
```sql
CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    external_user_id VARCHAR(255) NOT NULL UNIQUE,
    email            VARCHAR(255),
    phone            VARCHAR(20),
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_external ON users(external_user_id);
```

### V5 — notification_events
```sql
CREATE TABLE notification_events (
    id               BIGSERIAL PRIMARY KEY,
    campaign_id      BIGINT NOT NULL REFERENCES notification_campaigns(id),
    user_id          BIGINT NOT NULL REFERENCES users(id),
    channel          VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key  VARCHAR(512) NOT NULL UNIQUE,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_events_campaign ON notification_events(campaign_id);
CREATE INDEX idx_events_status   ON notification_events(status);
```

### V6 — notification_delivery
```sql
CREATE TABLE notification_delivery (
    id           BIGSERIAL PRIMARY KEY,
    event_id     BIGINT NOT NULL REFERENCES notification_events(id),
    channel      VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    delivered_at TIMESTAMP,
    error_message TEXT
);
```

### V7 — delivery_attempts
```sql
CREATE TABLE delivery_attempts (
    id             BIGSERIAL PRIMARY KEY,
    event_id       BIGINT NOT NULL REFERENCES notification_events(id),
    attempt_number INT NOT NULL,
    status         VARCHAR(20) NOT NULL,
    attempted_at   TIMESTAMP NOT NULL DEFAULT now(),
    error          TEXT
);
```

### V8 — retry_events
```sql
CREATE TABLE retry_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        BIGINT NOT NULL REFERENCES notification_events(id),
    retry_count     INT NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMP,
    backoff_seconds INT
);
```

### V9 — dlq_events
```sql
CREATE TABLE dlq_events (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT NOT NULL REFERENCES notification_events(id),
    reason      TEXT,
    payload     JSONB NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    replayed_at TIMESTAMP
);
```

### V10 — notification_templates
```sql
CREATE TABLE notification_templates (
    id          BIGSERIAL PRIMARY KEY,
    client_id   UUID NOT NULL REFERENCES frontend_clients(id),
    name        VARCHAR(255) NOT NULL,
    subject     VARCHAR(512),
    body        TEXT NOT NULL,
    channel     VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
```

### V11 — cohorts
```sql
CREATE TABLE cohorts (
    id          BIGSERIAL PRIMARY KEY,
    client_id   UUID NOT NULL REFERENCES frontend_clients(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE cohort_users (
    cohort_id BIGINT NOT NULL REFERENCES cohorts(id),
    user_id   BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (cohort_id, user_id)
);
```

---

## 3. Entity Classes

### FrontendClient
```java
@Entity @Table(name = "frontend_clients")
public class FrontendClient {
    @Id UUID id;
    String name;
    String apiKeyHash;
    String hostIp;
    boolean isActive;
    LocalDateTime createdAt;
}
```

### NotificationEvent
```java
@Entity @Table(name = "notification_events")
public class NotificationEvent {
    @Id @GeneratedValue Long id;
    @ManyToOne NotificationCampaign campaign;
    @ManyToOne User user;
    @Enumerated ChannelType channel;
    @Enumerated DeliveryStatus status;
    String idempotencyKey;
    LocalDateTime createdAt;
}
```

### DlqEvent
```java
@Entity @Table(name = "dlq_events")
public class DlqEvent {
    @Id @GeneratedValue Long id;
    @ManyToOne NotificationEvent event;
    String reason;
    @JdbcTypeCode(SqlTypes.JSON) String payload;
    LocalDateTime createdAt;
    LocalDateTime replayedAt;
}
```

---

## 4. Kafka Message Payloads

### NotificationEventMessage (API → channel topics directly)
```json
{
  "eventId": 1001,
  "campaignId": 5,
  "userId": 42,
  "externalUserId": "user-xyz",
  "email": "user@example.com",
  "phone": "+911234567890",
  "channel": "EMAIL",
  "message": "Hello!",
  "retryCount": 0,
  "idempotencyKey": "camp5-user42-EMAIL"
}
```

### NotificationStatusMessage (channel → notification-status)
```json
{
  "eventId": 1001,
  "status": "SENT",
  "deliveredAt": "2026-06-14T10:00:00",
  "errorMessage": null
}
```

---

## 5. REST API Contracts

### Client Registration
```
POST /api/v1/clients/register
Body:  { "name": "MyApp", "hostIp": "192.168.1.1" }
Response 201: { "clientId": "<uuid>", "apiKey": "<uuid>" }
```

### Campaign APIs
```
POST   /api/v1/campaigns
GET    /api/v1/campaigns                    ?page=0&size=20
GET    /api/v1/campaigns/{id}
DELETE /api/v1/campaigns/{id}
```

### Notification APIs
```
POST /api/v1/notifications/send
Headers: X-Client-UUID, Idempotency-Key
Body: { "campaignId": 5, "userId": "user-xyz", "channel": "EMAIL" }

POST /api/v1/notifications/bulk-upload
Headers: X-Client-UUID
Body: multipart/form-data  (file + campaignId)

GET /api/v1/notifications/history?campaignId=5&page=0&size=50
```

### Cohort APIs
```
POST   /api/v1/cohorts
GET    /api/v1/cohorts
GET    /api/v1/cohorts/{id}
POST   /api/v1/cohorts/{id}/users              (add users)
DELETE /api/v1/cohorts/{id}/users/{userId}     (remove user, deletes from Cuckoo Filter)
```

### Dashboard APIs
```
GET /api/v1/dashboard/metrics       → DashboardMetricsResponse
GET /api/v1/consumer/lag            → List<ConsumerLagResponse>
```

### DLQ APIs
```
GET  /api/v1/dlq?page=0&size=20     → List<DlqEventResponse>
POST /api/v1/dlq/reprocess          Body: { "dlqEventIds": [1, 2, 3] }
```

---

## 6. Filter — UuidAuthFilter

```
Order: First in filter chain
Excluded: /api/v1/clients/register, /actuator/**, /swagger-ui/**

Logic:
  1. Extract X-Client-UUID header
  2. Compute hash(uuid + request.remoteAddr)
  3. Lookup in Redis: "client:<uuid>" → stored hash + active flag
  4. If missing or inactive → 401
  5. If hash mismatch → 403
  6. Rate limit check: Redis INCR "rate:<uuid>:<minute>" → if > 1000 → 429
  7. Set clientId in request attribute, continue chain
```

---

## 7. Deduplication Service

```
CuckooFilterService (Redis backed using Redisson or raw Redis commands)

  insert(campaignId, userId):
    key = "cuckoo:campaign:<campaignId>"
    CF.ADD key userId

  exists(campaignId, userId) → boolean:
    CF.EXISTS key userId

  delete(campaignId, userId):
    CF.DEL key userId

Used in:
  - UploadService: check before publishing each user to Kafka
  - CohortService.removeUser: delete from filter + cohort_users table
```

---

## 8. Retry Consumer Logic

```java
@KafkaListener(topics = "retry-notification")
void consume(NotificationEventMessage msg) {
    if (msg.retryCount >= MAX_RETRIES) {
        // Max retries exceeded — publish DLQ status to notification-status
        // StatusConsumer will update DB and create dlq_events record
        NotificationStatusMessage dlq = NotificationStatusMessage.builder()
            .eventId(msg.getEventId())
            .campaignId(msg.getCampaignId())
            .status(DeliveryStatus.DLQ)
            .payload(toJson(msg))          // full payload stored for replay
            .build();
        producer.send(KafkaTopics.NOTIFICATION_STATUS, String.valueOf(msg.getEventId()), dlq);
        return;
    }
    long backoffSeconds = (long) Math.pow(5, msg.retryCount + 1);
    Thread.sleep(backoffSeconds * 1000);
    // attempt delivery
    // on success → publish SENT to notification-status
    // on failure → msg.retryCount++ → publish back to retry-notification
}
```

Max retries: 3. Backoffs: 5s, 25s, 125s.
No separate DLQ consumer — StatusConsumer handles DLQ entries alongside SENT entries.

---

## 9. Dashboard Metrics Response

```json
{
  "totalCampaigns": 12,
  "notificationsRaised": 50000,
  "sent": 47200,
  "pending": 800,
  "failed": 1000,
  "retryCount": 980,
  "dlqCount": 20,
  "duplicatesRemoved": 3500,
  "channelBreakdown": {
    "EMAIL":  { "sent": 20000, "failed": 400 },
    "SMS":    { "sent": 15000, "failed": 300 },
    "IN_APP": { "sent": 12200, "failed": 300 }
  },
  "consumerLag": [
    { "topic": "email-notification",  "lag": 0 },
    { "topic": "sms-notification",    "lag": 12 },
    { "topic": "in-app-notification", "lag": 5 }
  ]
}
```

---

## 10. Micrometer Metrics

| Metric Name | Type | Labels |
|---|---|---|
| `notify.notification.sent` | Counter | channel |
| `notify.notification.failed` | Counter | channel |
| `notify.notification.retried` | Counter | attempt_number |
| `notify.notification.dlq` | Counter | — |
| `notify.deduplication.hits` | Counter | — |
| `notify.consumer.lag` | Gauge | topic |
| `notify.campaign.created` | Counter | — |