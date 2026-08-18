CREATE TABLE IF NOT EXISTS password_reset_tokens (
    password_reset_token_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    otp_expires_at TIMESTAMP NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    verified_at TIMESTAMP,
    reset_token_hash VARCHAR(128) UNIQUE,
    reset_token_expires_at TIMESTAMP,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_active
    ON password_reset_tokens (user_id, created_at DESC)
    WHERE consumed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_reset_token_hash
    ON password_reset_tokens (reset_token_hash);
