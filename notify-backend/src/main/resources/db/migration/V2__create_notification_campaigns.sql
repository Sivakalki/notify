CREATE TABLE notification_campaigns (
    id               BIGSERIAL    PRIMARY KEY,
    client_id        UUID         NOT NULL REFERENCES frontend_clients(id),
    campaign_name    VARCHAR(255) NOT NULL,
    message          TEXT         NOT NULL,
    channel          VARCHAR(20)  NOT NULL DEFAULT 'IN_APP'
                                  CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP')),
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    total_users      INT          NOT NULL DEFAULT 0,
    sent_count       INT          NOT NULL DEFAULT 0,
    failed_count     INT          NOT NULL DEFAULT 0,
    duplicate_count  INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaigns_client_id  ON notification_campaigns(client_id);
CREATE INDEX idx_campaigns_status     ON notification_campaigns(status);
CREATE INDEX idx_campaigns_channel    ON notification_campaigns(channel);
CREATE INDEX idx_campaigns_created_at ON notification_campaigns(created_at DESC);
