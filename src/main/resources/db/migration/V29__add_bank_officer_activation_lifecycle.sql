ALTER TABLE password_reset_tokens
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(30) NOT NULL DEFAULT 'PASSWORD_RESET';

UPDATE password_reset_tokens token
SET purpose = 'OFFICER_ACTIVATION'
FROM users account
JOIN roles role ON role.role_id = account.role_id
WHERE token.user_id = account.user_id
  AND account.status = 'PENDING_ACTIVATION'
  AND role.role_name = 'BANK_OFFICER';

ALTER TABLE password_reset_tokens DROP CONSTRAINT IF EXISTS chk_password_reset_token_purpose;
ALTER TABLE password_reset_tokens
    ADD CONSTRAINT chk_password_reset_token_purpose
    CHECK (purpose IN ('PASSWORD_RESET', 'OFFICER_ACTIVATION'));

ALTER TABLE bank_officers
    ADD COLUMN IF NOT EXISTS activation_resend_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS activation_password_set_at TIMESTAMP;

ALTER TABLE bank_officers DROP CONSTRAINT IF EXISTS chk_bank_officer_activation_resends;
ALTER TABLE bank_officers
    ADD CONSTRAINT chk_bank_officer_activation_resends
    CHECK (activation_resend_count BETWEEN 0 AND 3);
