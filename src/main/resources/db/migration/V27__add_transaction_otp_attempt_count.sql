ALTER TABLE bank_customer_transactions
    ADD COLUMN IF NOT EXISTS otp_attempt_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE bank_customer_transactions
    DROP CONSTRAINT IF EXISTS chk_bank_customer_transactions_otp_attempt_count;

ALTER TABLE bank_customer_transactions
    ADD CONSTRAINT chk_bank_customer_transactions_otp_attempt_count
    CHECK (otp_attempt_count >= 0 AND otp_attempt_count <= 3);
