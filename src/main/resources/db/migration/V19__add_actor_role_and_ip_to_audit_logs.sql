ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS actor_role VARCHAR(60);

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_role
    ON audit_logs (actor_role);
