-- Retry scheduling state per event.
-- next_retry_at drives the scheduler — partial index keeps the scan fast.

CREATE TABLE retry_events (
    id              BIGSERIAL   PRIMARY KEY,
    event_id        BIGINT      NOT NULL UNIQUE REFERENCES notification_events(id),
    retry_count     INT         NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    next_retry_at   TIMESTAMPTZ,
    backoff_seconds INT         CHECK (backoff_seconds >= 0),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Only index pending retries (replayed rows have next_retry_at = NULL)
CREATE INDEX idx_retry_events_next_retry_at
    ON retry_events(next_retry_at)
    WHERE next_retry_at IS NOT NULL;