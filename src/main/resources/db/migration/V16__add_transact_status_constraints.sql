ALTER TABLE bank_customer_transactions
    DROP CONSTRAINT IF EXISTS chk_bank_customer_transactions_status;

ALTER TABLE bank_customer_transactions
    ADD CONSTRAINT chk_bank_customer_transactions_status
    CHECK (status IN ('PENDING_OTP', 'SUCCESS', 'FAILED', 'CANCELLED'));

ALTER TABLE transaction_otp_logs
    DROP CONSTRAINT IF EXISTS chk_transaction_otp_logs_status;

ALTER TABLE transaction_otp_logs
    ADD CONSTRAINT chk_transaction_otp_logs_status
    CHECK (otp_status IN ('SENT', 'VERIFIED', 'EXPIRED', 'FAILED'));
