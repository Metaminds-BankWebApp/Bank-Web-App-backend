-- Email addresses may be reused by different account roles; usernames remain unique login identifiers.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

-- Convert the retired states before enforcing the Active/Suspend status model.
UPDATE users
SET status = 'SUSPEND'
WHERE status IN ('INACTIVE', 'LOCKED');

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;
ALTER TABLE users
    ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPEND'));

CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
