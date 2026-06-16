-- One row per (campaign, user) pair — the atomic unit of delivery.
-- idempotency_key prevents duplicate sends even under retry or duplicate uploads.

CREATE TABLE notification_events (
    id               BIGSERIAL    PRIMARY KEY,
    campaign_id      BIGINT       NOT NULL REFERENCES notification_campaigns(id),
    user_id          BIGINT       NOT NULL REFERENCES users(id),
    channel          VARCHAR(20)  NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP')),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING', 'DLQ')),
    idempotency_key  VARCHAR(512) NOT NULL UNIQUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_events_campaign_id ON notification_events(campaign_id);
CREATE INDEX idx_notification_events_user_id     ON notification_events(user_id);
CREATE INDEX idx_notification_events_status      ON notification_events(status);
CREATE INDEX idx_notification_events_created_at  ON notification_events(created_at DESC);