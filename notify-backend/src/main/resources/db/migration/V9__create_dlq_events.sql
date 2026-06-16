-- Dead-lettered events with their full Kafka payload stored as JSONB.
-- replayed_at is NULL for pending replays — partial index keeps DLQ console queries fast.

CREATE TABLE dlq_events (
    id          BIGSERIAL   PRIMARY KEY,
    event_id    BIGINT      NOT NULL UNIQUE REFERENCES notification_events(id),
    reason      TEXT,
    payload     JSONB       NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    replayed_at TIMESTAMPTZ
);

CREATE INDEX idx_dlq_events_pending ON dlq_events(created_at DESC) WHERE replayed_at IS NULL;