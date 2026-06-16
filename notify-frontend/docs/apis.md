---
Auth

All APIs except /register and /health require this header:
X-Client-UUID: <your-api-key>

---
1. Client Registration

POST /api/v1/clients/register

No auth header needed.

Request
{
  "name": "My Frontend App"
}
Response 201
{
  "clientId": "uuid",
  "apiKey": "raw-key-shown-once"
}

---
2. Campaigns

POST /api/v1/campaigns

Request
{
  "campaignName": "Summer Sale",
  "message": "Get 50% off today!",
  "channel": "EMAIL"   // EMAIL | SMS | IN_APP
}
Response 201
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
  "createdAt": "2026-06-14T10:00:00Z"
}

GET /api/v1/campaigns?page=0&size=20

Response 200 — paginated
{
  "content": [ { ...campaign } ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}

GET /api/v1/campaigns/{id}

Response 200 — single campaign object (same shape as above)

DELETE /api/v1/campaigns/{id}

Response 204 — no body (sets status to CANCELLED)

---
3. Notifications

POST /api/v1/notifications/send

Request
{
  "campaignId": 1,
  "externalUserId": "user-123",
  "email": "user@example.com",
  "phone": "555-1111"
}
Response 202
{
  "eventId": 99,
  "idempotencyKey": "notif:1:user-123",
  "status": "PENDING",
  "duplicate": false
}

GET /api/v1/notifications/history?campaignId=1&page=0&size=50

Response 200 — paginated
{
  "content": [
    {
      "eventId": 99,
      "externalUserId": "user-123",
      "channel": "EMAIL",
      "status": "SENT",
      "createdAt": "2026-06-14T10:00:00Z",
      "deliveredAt": "2026-06-14T10:00:01Z",
      "errorMessage": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 50
}

POST /api/v1/notifications/bulk-upload

Request — multipart/form-data
campaignId = 1
file       = users.csv   (columns: externalUserId, email, phone)
Response 202
{
  "uploadId": 5,
  "campaignId": 1,
  "fileName": "users.csv",
  "status": "PROCESSING"
}

---
4. Cohorts

POST /api/v1/cohorts

Request
{
  "name": "VIP Users",
  "description": "Top 1000 buyers"
}
Response 201
{
  "id": 1,
  "name": "VIP Users",
  "description": "Top 1000 buyers",
  "memberCount": 0,
  "createdAt": "2026-06-14T10:00:00Z"
}

GET /api/v1/cohorts

Response 200 — array
[ { "id": 1, "name": "VIP Users", "memberCount": 250, ... } ]

POST /api/v1/cohorts/{id}/users

Request
{
  "users": [
    { "externalUserId": "u1", "email": "u1@x.com", "phone": "111" },
    { "externalUserId": "u2", "email": "u2@x.com" }
  ]
}
Response 200
{
  "total": 2,
  "added": 2,
  "duplicates": 0
}

DELETE /api/v1/cohorts/{id}/users/{userId}

Response 204 — no body

---
5. DLQ

GET /api/v1/dlq?page=0&size=20

Response 200 — paginated (only unreplayed events)
{
  "content": [
    {
      "id": 3,
      "eventId": 99,
      "reason": "Max retries (3) exceeded",
      "payload": "{...original kafka message...}",
      "createdAt": "2026-06-14T10:00:00Z",
      "replayedAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}

POST /api/v1/dlq/{id}/reprocess

Request — no body
Response 200
{
  "id": 3,
  "eventId": 99,
  "reason": "Max retries (3) exceeded",
  "payload": "{...}",
  "createdAt": "2026-06-14T10:00:00Z",
  "replayedAt": "2026-06-14T11:00:00Z"
}

---
6. Dashboard

GET /api/v1/dashboard/metrics

Response 200 — cached 30s
{
  "totalSent": 10000,
  "totalFailed": 120,
  "totalPending": 45,
  "totalDlq": 12,
  "totalDuplicates": 300,
  "totalCampaigns": 8,
  "cachedAt": "2026-06-14T10:00:00Z"
}

GET /api/v1/consumer/lag

Response 200 — array
[
  { "groupId": "notify-email-group", "topic": "email-notification", "partition": 0, "lag": 0 },
  { "groupId": "notify-retry-group", "topic": "retry-notification",  "partition": 0, "lag": 2 }
]

---
7. Health

GET /api/v1/health

No auth needed.
Response 200
Notify is running

---
Error Response (all endpoints)

{
  "timestamp": "2026-06-14T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Campaign not found: 99",
  "path": "/api/v1/campaigns/99"
}