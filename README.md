# Notify — Distributed Notification Delivery Platform

A full-stack, production-grade notification system built to explore real-world distributed systems patterns: Kafka-based async delivery, deduplication with Cuckoo Filters, retry pipelines, DLQ handling, and live analytics.

---

## Why This Exists

Most tutorials show you how to send an email. They skip the hard parts: what happens when delivery fails, how do you avoid sending the same notification twice to the same user, how do you handle 100k recipients without blocking your API, how do you let an operator replay failed messages without data loss?

Notify is built to answer those questions in a real, running system — not pseudocode.

---

## What It Does

- **Campaign management** — create notification campaigns targeting user cohorts across email, SMS, or in-app channels
- **Bulk upload** — ingest CSV/Excel/JSON cohort files, validate, deduplicate, and publish to Kafka
- **Async delivery** — Kafka topics per channel; consumers process independently and track delivery status
- **Deduplication** — Cuckoo Filter (Redis) catches duplicate user IDs before they hit the pipeline; supports deletion unlike Bloom Filters
- **Retry pipeline** — failed deliveries route through an exponential-backoff retry topic before landing in the DLQ
- **DLQ console** — operators can inspect and replay dead-lettered messages from the UI
- **Live dashboard** — metrics per campaign: sent, pending, failed, retry count, Kafka consumer lag, throughput/sec

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser (React)                         │
│   Dashboard │ Campaigns │ Cohorts │ Bulk Upload │ DLQ Console   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ REST (axios)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot REST API                          │
│  Controllers → Services → Repositories (JPA) → PostgreSQL      │
│                         │                                       │
│              Redis (Cuckoo Filter dedup + idempotency keys)     │
└──────────────┬──────────────────────────────────────────────────┘
               │ Kafka Produce
               ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Apache Kafka                            │
│                                                                 │
│  notification-requested                                         │
│       │                                                         │
│       ├─▶ email-notification ──▶ [Email Consumer]              │
│       ├─▶ sms-notification ───▶ [SMS Consumer]                 │
│       └─▶ in-app-notification ▶ [In-App Consumer]              │
│                                                                 │
│  Failure path:                                                  │
│  [Consumer] ──▶ retry-notification (backoff) ──▶ dead-letter   │
└─────────────────────────────────────────────────────────────────┘
               │ Status events
               ▼
         notification-status topic → API → PostgreSQL → Dashboard
```

---

## Tech Stack

**Backend**

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis_Stack-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

**Frontend**

![React](https://img.shields.io/badge/React_19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/TailwindCSS_v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)

**Infrastructure**

![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

---

## Project Structure

```
notify/
├── notify-backend/     # Spring Boot API + Kafka producers/consumers
├── notify-frontend/    # React dashboard
├── images/             # Screenshots
├── Makefile            # Root convenience commands
└── README.md           # This file
```

Each sub-project has its own README with full setup instructions:

- [Backend setup →](./notify-backend/README.md)
- [Frontend setup →](./notify-frontend/README.md)

---

## Screenshots

![System Performance Dashboard](images/dashboard2.png)
*System Performance Overview — real-time stats and Kafka Consumer Group Lag per partition*

---

![Dashboard with Filters](images/dashboard1.png)
*Dashboard with active filters — cache bypassed for live results*

---

![Campaign Detail](images/campaign.png)
*Campaign Detail — per-notification history with delivery timestamps and status*

---

## Quick Start

**Prerequisites:** Docker, Java 21, Node 20+

```bash
# 1. Start all infrastructure (Kafka, Redis, PostgreSQL, pgAdmin, Kafka UI)
make infra

# 2. In a new terminal — start the backend API
make backend

# 3. In a new terminal — start the frontend dev server
make frontend
```

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Kafka UI | http://localhost:8082 |
| pgAdmin | http://localhost:5050 |

---

## Key Design Decisions

**Why Kafka instead of a simple job queue?**
Kafka gives per-channel independent scaling, replay-ability, and natural partitioning by `userId` for ordering guarantees per recipient. A Redis queue would couple producer and consumer scaling.

**Why Cuckoo Filter instead of Bloom Filter?**
Bloom Filters don't support deletion. When a user is removed from a cohort, their deduplication entry needs to be cleared. Cuckoo Filters support deletion with comparable false-positive rates and lower memory at high load.

**Why no auth system?**
Each frontend client registers once and receives a UUID. All API calls are signed with that UUID + a request signature derived from the host. This avoids full OAuth complexity while still preventing unauthorized cross-tenant access — appropriate for a single-operator system.

**Why Flyway?**
Schema migrations need to be auditable and repeatable across environments. Flyway ties migrations to the application lifecycle so the schema is always in sync with the code.
