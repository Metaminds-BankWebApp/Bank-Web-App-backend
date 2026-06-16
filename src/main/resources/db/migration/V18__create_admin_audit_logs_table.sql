CREATE TABLE IF NOT EXISTS audit_logs (
    audit_log_id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    actor_name VARCHAR(150) NOT NULL,
    action_type VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    details VARCHAR(500),
    tone VARCHAR(20) NOT NULL DEFAULT 'INFO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_audit_logs_tone
        CHECK (tone IN ('SUCCESS', 'WARNING', 'INFO', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at
    ON audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action_type
    ON audit_logs (action_type);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_user
    ON audit_logs (actor_user_id);
