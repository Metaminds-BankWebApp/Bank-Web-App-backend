CREATE TABLE IF NOT EXISTS password_reset_otps (
    password_reset_otp_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    otp_code_hash VARCHAR(255) NOT NULL,
    reset_token_hash VARCHAR(255),
    sent_to_email VARCHAR(150) NOT NULL,
    otp_status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_otps_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_password_reset_otps_status
        CHECK (otp_status IN ('SENT', 'VERIFIED', 'USED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_password_reset_otps_user_created_at
    ON password_reset_otps(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_password_reset_otps_status_expires_at
    ON password_reset_otps(otp_status, expires_at);

CREATE INDEX IF NOT EXISTS idx_password_reset_otps_reset_token_hash
    ON password_reset_otps(reset_token_hash)
    WHERE reset_token_hash IS NOT NULL;
