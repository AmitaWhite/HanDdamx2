ALTER TABLE notification
    ADD COLUMN dedup_key VARCHAR(255);

CREATE UNIQUE INDEX uk_notification_dedup_key
    ON notification (dedup_key);
