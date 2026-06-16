-- Named user groups scoped per client.
-- cohort_users is the join table — CASCADE ensures removing a cohort cleans up memberships.

CREATE TABLE cohorts (
    id          BIGSERIAL    PRIMARY KEY,
    client_id   UUID         NOT NULL REFERENCES frontend_clients(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (client_id, name)
);

CREATE TABLE cohort_users (
    cohort_id  BIGINT      NOT NULL REFERENCES cohorts(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    added_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (cohort_id, user_id)
);

CREATE INDEX idx_cohorts_client_id    ON cohorts(client_id);
CREATE INDEX idx_cohort_users_user_id ON cohort_users(user_id);