ALTER TABLE notification_campaigns
    DROP COLUMN sent_count,
    DROP COLUMN failed_count,
    DROP COLUMN duplicate_count;