# Notify — API Reference

Base URL: `http://localhost:8080`

## Authentication

All endpoints except `/api/v1/clients/register` and `/api/v1/health` require the following header:

```
X-Client-UUID: <your-api-key>
```

The API key is issued once during client registration and cannot be retrieved again.

---

## Error Response

All error responses follow this consistent format:

```json
{
  "timestamp": "2026-06-15T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Campaign not found: 99",
  "path": "/api/v1/campaigns/99"
}
```

| Status | Meaning |
|--------|---------|
| 400 | Validation failed / bad request body |
| 401 | Missing or invalid API key |
| 404 | Resource not found |
| 405 | HTTP method not supported |
| 409 | Duplicate request conflict |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error |

---

## 1. Health

### `GET /api/v1/health`
Liveness check. No auth required.

**Response `200`**
```
Notify is running
```

---

## 2. Client Registration

### `POST /api/v1/clients/register`
Register a new frontend client and receive an API key. No auth required.

**Request Body**
```json
{
  "name": "My Frontend App"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | Yes | Display name for the client |

**Response `201`**
```json
{
  "clientId": "3f6b1c2a-4e5d-7890-abcd-ef1234567890",
  "apiKey": "raw-api-key-shown-only-once"
}
```

> **Important:** Store the `apiKey` immediately. It is shown only once and cannot be retrieved again.

---

## 3. Campaigns

### `POST /api/v1/campaigns`
Create a new notification campaign.

**Request Body**
```json
{
  "campaignName": "Summer Sale",
  "message": "Get 50% off today!",
  "channel": "EMAIL"
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| campaignName | string | Yes | 2–255 characters |
| message | string | Yes | Up to 5000 characters |
| channel | string | Yes | `EMAIL` `SMS` `IN_APP` |

**Response `201`**
```json
{
  "id": 1,
  "campaignName": "Summer Sale",
  "message": "Get 50% off today!",
  "channel": "EMAIL",
  "status": "PENDING",
  "totalUsers": 0,
  "sentCount": 0,
  "failedCount": 0,
  "duplicateCount": 0,
  "createdAt": "2026-06-15T10:00:00Z",
  "updatedAt": "2026-06-15T10:00:00Z"
}
```

---

### `GET /api/v1/campaigns`
List all campaigns for the authenticated client. Results are paginated.

**Query Parameters**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | int | 0 | Page number (zero-based) |
| size | int | 20 | Page size |

**Response `200`**
```json
{
  "content": [
    {
      "id": 1,
      "campaignName": "Summer Sale",
      "message": "Get 50% off today!",
      "channel": "EMAIL",
      "status": "PENDING",
      "totalUsers": 500,
      "sentCount": 480,
      "failedCount": 20,
      "duplicateCount": 10,
      "createdAt": "2026-06-15T10:00:00Z",
      "updatedAt": "2026-06-15T10:05:00Z"
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

> `uploads` is not included in the list response. Use `GET /api/v1/campaigns/{id}` for per-upload breakdown.

---

### `GET /api/v1/campaigns/{id}`
Get a single campaign by ID. Includes the list of bulk uploads with per-upload duplicate counts.

**Path Parameters**

| Param | Type | Description |
|-------|------|-------------|
| id | long | Campaign ID |

**Response `200`**
```json
{
  "id": 1,
  "campaignName": "Summer Sale",
  "message": "Get 50% off today!",
  "channel": "EMAIL",
  "status": "PENDING",
  "totalUsers": 8000,
  "sentCount": 7800,
  "failedCount": 0,
  "duplicateCount": 500,
  "createdAt": "2026-06-15T10:00:00Z",
  "updatedAt": "2026-06-15T10:05:00Z",
  "uploads": [
    {
      "uploadId": 1,
      "campaignId": 1,
      "fileName": "users_june.csv",
      "fileType": "CSV",
      "rowCount": 8500,
      "duplicateCount": 500,
      "status": "COMPLETED",
      "createdAt": "2026-06-15T10:00:00Z"
    }
  ]
}
```

| Upload field | Description |
|---|---|
| `rowCount` | Total rows in the uploaded file |
| `duplicateCount` | Rows blocked by the deduplication filter for this upload |
| `status` | `PROCESSING` → `COMPLETED` or `FAILED` |

**Response `404`** — campaign not found or belongs to a different client

---

### `PUT /api/v1/campaigns/{id}`
Update an existing campaign. All fields are required.

**Path Parameters**

| Param | Type | Description |
|-------|------|-------------|
| id | long | Campaign ID |

**Request Body**
```json
{
  "campaignName": "Summer Sale Updated",
  "message": "Get 60% off today!",
  "channel": "EMAIL",
  "status": "PENDING"
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| campaignName | string | Yes | 2–255 characters |
| message | string | Yes | Up to 5000 characters |
| channel | string | Yes | `EMAIL` `SMS` `IN_APP` |
| status | string | Yes | `PENDING` `IN_PROGRESS` `COMPLETED` `FAILED` `CANCELLED` |

**Response `200`** — updated campaign object

---

### `DELETE /api/v1/campaigns/{id}`
Cancel a campaign. This is a soft delete — sets status to `CANCELLED`.

**Path Parameters**

| Param | Type | Description |
|-------|------|-------------|
| id | long | Campaign ID |

**Response `204`** — no body

---

## 4. Notifications

### `POST /api/v1/notifications/send`
Send a notification to a single user for a campaign.

**Request Body**
```json
{
  "campaignId": 1,
  "externalUserId": "user-123",
  "email": "user@example.com",
  "phone": "555-1111"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| campaignId | long | Yes | ID of the campaign |
| externalUserId | string | Yes | Your system's user identifier |
| email | string | No | Required if channel is `EMAIL` |
| phone | string | No | Required if channel is `SMS` |

**Response `202`**
```json
{
  "eventId": 99,
  "idempotencyKey": "notif:1:user-123",
  "status": "PENDING",
  "duplicate": false
}
```

> If the same `campaignId` + `externalUserId` combination is sent again within 24 hours, `duplicate` will be `true` and no new event is created.

---

### `GET /api/v1/notifications/history`
Get notification delivery history for a campaign.

**Query Parameters**

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| campaignId | long | Yes | — | Campaign to fetch history for |
| page | int | No | 0 | Page number |
| size | int | No | 50 | Page size |

**Response `200`**
```json
{
  "content": [
    {
      "eventId": 99,
      "externalUserId": "user-123",
      "channel": "EMAIL",
      "status": "SENT",
      "createdAt": "2026-06-15T10:00:00Z",
      "deliveredAt": "2026-06-15T10:00:01Z",
      "errorMessage": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 50
}
```

| Status value | Meaning |
|-------------|---------|
| `PENDING` | Event created, queued for delivery (also during retries) |
| `SENT` | Successfully delivered |
| `DLQ` | All 3 retries exhausted — moved to Dead Letter Queue |

---

### `POST /api/v1/notifications/bulk-upload`
Send notifications to many users by uploading a file. Processing is asynchronous — the endpoint returns immediately.

**Request** — `multipart/form-data`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| campaignId | long | Yes | Target campaign ID |
| file | file | Yes | `.csv`, `.xlsx`, `.xls`, or `.json` |

**File format** — required column: `externalUserId`. Optional: `email`, `phone`

CSV example:
```
externalUserId,email,phone
user-1,user1@example.com,555-1111
user-2,user2@example.com,555-2222
```

JSON example:
```json
[
  { "externalUserId": "user-1", "email": "user1@example.com", "phone": "555-1111" },
  { "externalUserId": "user-2", "email": "user2@example.com" }
]
```

**Response `202`**
```json
{
  "uploadId": 5,
  "campaignId": 1,
  "fileName": "users.csv",
  "fileType": "CSV",
  "rowCount": 0,
  "duplicateCount": 0,
  "status": "PROCESSING",
  "createdAt": "2026-06-15T10:00:00Z"
}
```

> `rowCount` and `duplicateCount` are `0` at creation time and populated once processing completes. Poll `GET /api/v1/campaigns/{id}` to see the final per-upload counts under `uploads`.

---

## 5. Cohorts

### `POST /api/v1/cohorts`
Create a new user cohort.

**Request Body**
```json
{
  "name": "VIP Users",
  "description": "Top 1000 buyers"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | Yes | Unique name within the client |
| description | string | No | Optional description |

**Response `201`**
```json
{
  "id": 1,
  "name": "VIP Users",
  "description": "Top 1000 buyers",
  "memberCount": 0,
  "createdAt": "2026-06-15T10:00:00Z"
}
```

---

### `GET /api/v1/cohorts`
List all cohorts for the authenticated client.

**Response `200`**
```json
[
  {
    "id": 1,
    "name": "VIP Users",
    "description": "Top 1000 buyers",
    "memberCount": 250,
    "createdAt": "2026-06-15T10:00:00Z"
  }
]
```

---

### `POST /api/v1/cohorts/{id}/users`
Add users to a cohort in bulk. Duplicate users are automatically skipped.

**Path Parameters**

| Param | Type | Description |
|-------|------|-------------|
| id | long | Cohort ID |

**Request Body**
```json
{
  "users": [
    { "externalUserId": "u1", "email": "u1@example.com", "phone": "111" },
    { "externalUserId": "u2", "email": "u2@example.com" }
  ]
}
```

**Response `200`**
```json
{
  "total": 2,
  "added": 2,
  "duplicates": 0
}
```

---

### `DELETE /api/v1/cohorts/{id}/users/{userId}`
Remove a user from a cohort.

**Path Parameters**

| Param | Type | Description |
|-------|------|-------------|
| id | long | Cohort ID |
| userId | long | Internal user ID |

**Response `204`** — no body

---

## 6. DLQ (Dead Letter Queue)

### `GET /api/v1/dlq`
List all pending DLQ events (not yet replayed), newest first.

**Query Parameters**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |

**Response `200`**
```json
{
  "content": [
    {
      "id": 3,
      "eventId": 99,
      "reason": "Max retries (3) exceeded",
      "payload": "{...original kafka message...}",
      "createdAt": "2026-06-15T10:00:00Z",
      "replayedAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### `POST /api/v1/dlq/{id}/reprocess`
Replay a DLQ event — republishes directly to the correct channel topic with retry count reset to 0.

**Path Parameters**

| Param | Type | Description |
|-------|------|-------------|
| id | long | DLQ event ID |

**Request Body** — none

**Response `200`**
```json
{
  "id": 3,
  "eventId": 99,
  "reason": "Max retries (3) exceeded",
  "payload": "{...}",
  "createdAt": "2026-06-15T10:00:00Z",
  "replayedAt": "2026-06-15T11:00:00Z"
}
```

**Response `400`** — if the event has already been replayed

---

## 7. Dashboard

### `GET /api/v1/dashboard/metrics`
Aggregated notification counts. All counts are derived from `notification_events` and `uploaded_files` — never from stale denormalized counters.

When called with **no query parameters**, the result is cached for 30 seconds. When any filter is provided, caching is skipped and the query runs live.

**Query Parameters**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| startDate | string (ISO 8601) | No | Include only events created on or after this timestamp |
| endDate | string (ISO 8601) | No | Include only events created on or before this timestamp |
| campaignIds | long (repeatable) | No | Filter by one or more campaign IDs |
| uploadFileName | string | No | Filter campaigns that have a bulk-upload file whose name contains this value (case-insensitive substring match) |

> When both `campaignIds` and `uploadFileName` are provided, only campaigns matching **both** filters are included (intersection).

**Example — date range**
```
GET /api/v1/dashboard/metrics?startDate=2026-06-01T00:00:00Z&endDate=2026-06-15T23:59:59Z
```

**Example — campaign IDs**
```
GET /api/v1/dashboard/metrics?campaignIds=1&campaignIds=2&campaignIds=5
```

**Example — upload file name**
```
GET /api/v1/dashboard/metrics?uploadFileName=users_june
```

**Example — combined**
```
GET /api/v1/dashboard/metrics?startDate=2026-06-01T00:00:00Z&campaignIds=1&campaignIds=2&uploadFileName=vip
```

**Response `200`**
```json
{
  "totalSent": 10000,
  "totalPending": 45,
  "totalFailed": 12,
  "totalDuplicates": 300,
  "totalCampaigns": 8,
  "cachedAt": "2026-06-15T10:00:00Z"
}
```

| Field | Description |
|-------|-------------|
| `totalSent` | Events confirmed delivered (within filter scope) |
| `totalPending` | Events not yet delivered, includes in-flight retries (within filter scope) |
| `totalFailed` | Events that exhausted all 3 retries — now in the Dead Letter Queue (within filter scope) |
| `totalDuplicates` | Users blocked by the deduplication filter during bulk upload (summed from matching uploads) |
| `totalCampaigns` | Number of campaigns matched by the filter; total campaigns when no filter is applied |
| `cachedAt` | Timestamp when the response was computed |

> **Invariant (unfiltered):** `totalSent + totalPending + totalFailed` always equals the total number of notification events ever created.

---

### `GET /api/v1/consumer/lag`
Per-partition Kafka consumer lag for all notification consumer groups.

**Response `200`**
```json
[
  { "groupId": "notify-email-group",  "topic": "email-notification",  "partition": 0, "lag": 0 },
  { "groupId": "notify-sms-group",    "topic": "sms-notification",    "partition": 0, "lag": 0 },
  { "groupId": "notify-inapp-group",  "topic": "in-app-notification", "partition": 0, "lag": 0 },
  { "groupId": "notify-status-group", "topic": "notification-status", "partition": 0, "lag": 0 },
  { "groupId": "notify-retry-group",  "topic": "retry-notification",  "partition": 0, "lag": 2 }
]
```

> A lag value above 0 means messages are waiting to be processed. High lag on `retry-notification` indicates delivery failures are piling up.
