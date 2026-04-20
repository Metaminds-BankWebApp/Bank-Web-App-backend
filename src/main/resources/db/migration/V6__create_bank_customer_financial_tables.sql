CREATE TABLE IF NOT EXISTS bank_customer_financial_records (
    bank_record_id BIGSERIAL PRIMARY KEY,
    bank_customer_id BIGINT NOT NULL,
    verified_by_officer_id BIGINT NOT NULL,
    data_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_financial_records_customer FOREIGN KEY (bank_customer_id)
        REFERENCES bank_customers (bank_customer_id),
    CONSTRAINT fk_bank_customer_financial_records_officer FOREIGN KEY (verified_by_officer_id)
        REFERENCES bank_officers (officer_id)
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_financial_records_customer
    ON bank_customer_financial_records(bank_customer_id);

CREATE TABLE IF NOT EXISTS bank_customer_incomes (
    income_id BIGSERIAL PRIMARY KEY,
    bank_record_id BIGINT NOT NULL,
    income_category VARCHAR(20) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    salary_type VARCHAR(30),
    employment_type VARCHAR(30),
    contract_duration_months INTEGER,
    income_stability VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_incomes_record FOREIGN KEY (bank_record_id)
        REFERENCES bank_customer_financial_records (bank_record_id) ON DELETE CASCADE,
    CONSTRAINT chk_bank_customer_incomes_category CHECK (income_category IN ('SALARY', 'BUSINESS')),
    CONSTRAINT chk_bank_customer_incomes_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_bank_customer_incomes_contract_duration_non_negative CHECK (
        contract_duration_months IS NULL OR contract_duration_months >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_incomes_record
    ON bank_customer_incomes(bank_record_id);

CREATE TABLE IF NOT EXISTS bank_customer_loans (
    loan_id BIGSERIAL PRIMARY KEY,
    bank_record_id BIGINT NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    monthly_emi NUMERIC(15,2) NOT NULL,
    remaining_balance NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_loans_record FOREIGN KEY (bank_record_id)
        REFERENCES bank_customer_financial_records (bank_record_id) ON DELETE CASCADE,
    CONSTRAINT chk_bank_customer_loans_monthly_emi_non_negative CHECK (monthly_emi >= 0),
    CONSTRAINT chk_bank_customer_loans_remaining_balance_non_negative CHECK (remaining_balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_loans_record
    ON bank_customer_loans(bank_record_id);

CREATE TABLE IF NOT EXISTS bank_customer_cards (
    card_id BIGSERIAL PRIMARY KEY,
    bank_record_id BIGINT NOT NULL,
    provider VARCHAR(100),
    credit_limit NUMERIC(15,2) NOT NULL,
    outstanding_balance NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_cards_record FOREIGN KEY (bank_record_id)
        REFERENCES bank_customer_financial_records (bank_record_id) ON DELETE CASCADE,
    CONSTRAINT chk_bank_customer_cards_credit_limit_non_negative CHECK (credit_limit >= 0),
    CONSTRAINT chk_bank_customer_cards_outstanding_balance_non_negative CHECK (outstanding_balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_cards_record
    ON bank_customer_cards(bank_record_id);

CREATE TABLE IF NOT EXISTS bank_customer_liabilities (
    liability_id BIGSERIAL PRIMARY KEY,
    bank_record_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    monthly_amount NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_liabilities_record FOREIGN KEY (bank_record_id)
        REFERENCES bank_customer_financial_records (bank_record_id) ON DELETE CASCADE,
    CONSTRAINT chk_bank_customer_liabilities_monthly_amount_non_negative CHECK (monthly_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_liabilities_record
    ON bank_customer_liabilities(bank_record_id);

CREATE TABLE IF NOT EXISTS bank_customer_missed_payments (
    missed_payment_id BIGSERIAL PRIMARY KEY,
    bank_record_id BIGINT NOT NULL UNIQUE,
    missed_payments INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_missed_payments_record FOREIGN KEY (bank_record_id)
        REFERENCES bank_customer_financial_records (bank_record_id) ON DELETE CASCADE,
    CONSTRAINT chk_bank_customer_missed_payments_non_negative CHECK (missed_payments >= 0)
);
