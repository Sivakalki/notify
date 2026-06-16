-- Reusable message templates scoped per client.
-- (client_id, name) must be unique — no two templates with the same name per client.

CREATE TABLE notification_templates (
    id         BIGSERIAL    PRIMARY KEY,
    client_id  UUID         NOT NULL REFERENCES frontend_clients(id),
    name       VARCHAR(255) NOT NULL,
    subject    VARCHAR(512),
    body       TEXT         NOT NULL,
    channel    VARCHAR(20)  NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (client_id, name)
);

CREATE INDEX idx_notification_templates_client_id ON notification_templates(client_id);