ALTER TABLE email_outbox ADD COLUMN retry_count INT DEFAULT 0 NOT NULL;
ALTER TABLE email_outbox ADD COLUMN next_retry_at TIMESTAMP;
