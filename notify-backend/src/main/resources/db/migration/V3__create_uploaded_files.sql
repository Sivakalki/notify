CREATE TABLE uploaded_files (
    id           BIGSERIAL    PRIMARY KEY,
    campaign_id  BIGINT       NOT NULL REFERENCES notification_campaigns(id),
    file_name    VARCHAR(255) NOT NULL,
    file_type    VARCHAR(10)  NOT NULL CHECK (file_type IN ('CSV', 'EXCEL', 'JSON')),
    row_count    INT          NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING'
                              CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_uploaded_files_campaign_id ON uploaded_files(campaign_id);
CREATE INDEX idx_uploaded_files_status      ON uploaded_files(status);