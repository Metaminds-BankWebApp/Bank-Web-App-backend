CREATE TABLE IF NOT EXISTS bank_customer_transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    bank_customer_id BIGINT NOT NULL,
    sender_account_no VARCHAR(20) NOT NULL,
    receiver_account_no VARCHAR(20) NOT NULL,
    receiver_name VARCHAR(150) NOT NULL,
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    remark VARCHAR(255) NOT NULL,
    reference_no VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_OTP',
    otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    expense_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason VARCHAR(255),
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_transactions_customer
        FOREIGN KEY (bank_customer_id) REFERENCES bank_customers(bank_customer_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_transactions_customer_id
    ON bank_customer_transactions(bank_customer_id);

CREATE INDEX IF NOT EXISTS idx_bank_customer_transactions_reference_no
    ON bank_customer_transactions(reference_no);

CREATE INDEX IF NOT EXISTS idx_bank_customer_transactions_date
    ON bank_customer_transactions(transaction_date DESC);

CREATE TABLE IF NOT EXISTS bank_customer_beneficiaries (
    beneficiary_id BIGSERIAL PRIMARY KEY,
    bank_customer_id BIGINT NOT NULL,
    beneficiary_account_no VARCHAR(20) NOT NULL,
    nick_name VARCHAR(100) NOT NULL,
    remark VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_beneficiaries_customer
        FOREIGN KEY (bank_customer_id) REFERENCES bank_customers(bank_customer_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_beneficiaries_customer_id
    ON bank_customer_beneficiaries(bank_customer_id);

CREATE INDEX IF NOT EXISTS idx_bank_customer_beneficiaries_account_no
    ON bank_customer_beneficiaries(beneficiary_account_no);

CREATE TABLE IF NOT EXISTS bank_customer_transaction_otp_logs (
    otp_log_id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    otp_code_hash VARCHAR(255) NOT NULL,
    sent_to_email VARCHAR(150) NOT NULL,
    otp_status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    resend_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_transaction_otp_logs_transaction
        FOREIGN KEY (transaction_id) REFERENCES bank_customer_transactions(transaction_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_transaction_otp_logs_transaction_id
    ON bank_customer_transaction_otp_logs(transaction_id);

CREATE INDEX IF NOT EXISTS idx_bank_customer_transaction_otp_logs_expires_at
    ON bank_customer_transaction_otp_logs(expires_at);
