-- PrimeCore notification storage setup.
-- Run this file manually against the same PostgreSQL database used by the backend.
-- This is intentionally NOT a Flyway migration.

BEGIN;

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    notification_type VARCHAR(60) NOT NULL,
    source VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    action_key VARCHAR(80),
    action_metadata TEXT,
    deduplication_key VARCHAR(180),
    affected_count INTEGER NOT NULL DEFAULT 1 CHECK (affected_count > 0),
    read_at TIMESTAMP,
    dismissed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notifications_recipient_deduplication
        UNIQUE (recipient_user_id, deduplication_key)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread
    ON notifications (recipient_user_id, read_at)
    WHERE dismissed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_source
    ON notifications (source);

-- Keep the script compatible with a table that Hibernate may have created first.
ALTER TABLE notifications
    ALTER COLUMN affected_count SET DEFAULT 1;
ALTER TABLE notifications
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE notifications
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

COMMIT;

-- Optional demo seed data. Run this section only if sample inbox items are useful.
-- It creates one safe example for every existing user and remains idempotent.

INSERT INTO notifications (
    recipient_user_id,
    notification_type,
    source,
    severity,
    title,
    message,
    action_key,
    action_metadata,
    deduplication_key,
    affected_count,
    created_at,
    updated_at
)
SELECT
    u.user_id,
    CASE r.role_name
        WHEN 'ADMIN' THEN 'SYSTEM_NOTICE'
        WHEN 'BANK_OFFICER' THEN 'CUSTOMER_ASSIGNED'
        WHEN 'BANK_CUSTOMER' THEN 'LOANSENSE_RESULT_AVAILABLE'
        ELSE 'MONTHLY_FINANCIAL_REVIEW_DUE'
    END,
    CASE r.role_name
        WHEN 'ADMIN' THEN 'SYSTEM'
        WHEN 'BANK_OFFICER' THEN 'ONBOARDING'
        WHEN 'BANK_CUSTOMER' THEN 'LOANSENSE'
        ELSE 'SYSTEM'
    END,
    'INFO',
    'Welcome to the notification inbox',
    'PrimeCore notifications are now connected to your account.',
    'DASHBOARD',
    '{}',
    'manual-welcome-v1',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
JOIN roles r ON r.role_id = u.role_id
ON CONFLICT (recipient_user_id, deduplication_key) DO NOTHING;
