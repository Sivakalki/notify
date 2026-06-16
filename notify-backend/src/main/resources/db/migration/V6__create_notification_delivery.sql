-- Final delivery outcome for each notification event.
-- One-to-one with notification_events (UNIQUE on event_id).

CREATE TABLE notification_delivery (
    id            BIGSERIAL   PRIMARY KEY,
    event_id      BIGINT      NOT NULL UNIQUE REFERENCES notification_events(id),
    channel       VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP')),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING', 'DLQ')),
    delivered_at  TIMESTAMPTZ,
    error_message TEXT
);

CREATE INDEX idx_notification_delivery_status ON notification_delivery(status);