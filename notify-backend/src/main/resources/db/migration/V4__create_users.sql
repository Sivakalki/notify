-- Deduplicated user registry.
-- external_user_id is the caller's own user identifier (from their system).
-- email and phone are optional — at least one must be present for delivery.

CREATE TABLE users (
    id               BIGSERIAL    PRIMARY KEY,
    external_user_id VARCHAR(255) NOT NULL UNIQUE,
    email            VARCHAR(255),
    phone            VARCHAR(20),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_contact CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE INDEX idx_users_external_user_id ON users(external_user_id);
CREATE INDEX idx_users_email            ON users(email) WHERE email IS NOT NULL;
CREATE INDEX idx_users_phone            ON users(phone) WHERE phone IS NOT NULL;