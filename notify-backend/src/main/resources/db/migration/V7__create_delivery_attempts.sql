-- Every individual send attempt per event (initial + retries).
-- (event_id, attempt_number) is unique — no duplicate attempt records.

CREATE TABLE delivery_attempts (
    id             BIGSERIAL   PRIMARY KEY,
    event_id       BIGINT      NOT NULL REFERENCES notification_events(id),
    attempt_number INT         NOT NULL CHECK (attempt_number >= 1),
    status         VARCHAR(20) NOT NULL CHECK (status IN ('SENT', 'FAILED')),
    attempted_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    error          TEXT,
    UNIQUE (event_id, attempt_number)
);

CREATE INDEX idx_delivery_attempts_event_id ON delivery_attempts(event_id);