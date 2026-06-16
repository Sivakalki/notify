-- Registered frontend instances that are allowed to call this API.
-- api_key_hash stores SHA-256(raw_uuid + host_ip) — the raw UUID is shown once and never persisted.

CREATE TABLE frontend_clients (
    id            UUID        PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    api_key_hash  VARCHAR(512) NOT NULL UNIQUE,
    host_ip       VARCHAR(45),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_frontend_clients_api_key_hash ON frontend_clients(api_key_hash);
