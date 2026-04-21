-- =========================================
-- PRIMECORE CRIB-ONLY DATASET SCHEMA
-- =========================================

-- Drop tables first if you want a clean re-run
DROP TABLE IF EXISTS crib_credit_summary CASCADE;
DROP TABLE IF EXISTS crib_payment_history CASCADE;
DROP TABLE IF EXISTS crib_other_liability_records CASCADE;
DROP TABLE IF EXISTS crib_credit_card_records CASCADE;
DROP TABLE IF EXISTS crib_loan_records CASCADE;
DROP TABLE IF EXISTS crib_customers CASCADE;

-- =========================================
-- 1. MASTER CUSTOMER TABLE
-- =========================================
CREATE TABLE crib_customers (
    id BIGSERIAL PRIMARY KEY,
    customer_code VARCHAR(30) UNIQUE NOT NULL,
    nic VARCHAR(20) UNIQUE NOT NULL,
    nic_format VARCHAR(10),
    full_name VARCHAR(150) NOT NULL,
    gender VARCHAR(10),
    date_of_birth DATE,
    city VARCHAR(100),
    report_generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================
-- 2. LOAN RECORDS
-- One row = one loan facility
-- =========================================
CREATE TABLE crib_loan_records (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    lender_name VARCHAR(120),
    loan_type VARCHAR(50) NOT NULL,
    facility_type VARCHAR(50),
    amount DECIMAL(15,2) NOT NULL,
    remaining_balance DECIMAL(15,2) DEFAULT 0,
    monthly_emi DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    opened_date DATE,
    closed_date DATE,
    rescheduled_flag BOOLEAN DEFAULT FALSE,
    default_flag BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_crib_loan_customer
        FOREIGN KEY (customer_id)
        REFERENCES crib_customers(id)
        ON DELETE CASCADE
);

-- =========================================
-- 3. CREDIT CARD RECORDS
-- One row = one credit card
-- =========================================
CREATE TABLE crib_credit_card_records (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    issuer_name VARCHAR(120),
    card_type VARCHAR(50),
    credit_limit DECIMAL(15,2) NOT NULL,
    outstanding_balance DECIMAL(15,2) DEFAULT 0,
    payment_status VARCHAR(30),
    opened_date DATE,
    closed_date DATE,
    default_flag BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_crib_card_customer
        FOREIGN KEY (customer_id)
        REFERENCES crib_customers(id)
        ON DELETE CASCADE
);

-- =========================================
-- 4. OTHER LIABILITY RECORDS
-- leasing / hire purchase / overdraft / guarantor obligation
-- =========================================
CREATE TABLE crib_other_liability_records (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    liability_type VARCHAR(50) NOT NULL,
    provider_name VARCHAR(120),
    monthly_amount DECIMAL(15,2) DEFAULT 0,
    remaining_balance DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(30),
    opened_date DATE,
    closed_date DATE,
    default_flag BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_crib_other_liability_customer
        FOREIGN KEY (customer_id)
        REFERENCES crib_customers(id)
        ON DELETE CASCADE
);

-- =========================================
-- 5. PAYMENT HISTORY
-- One row per customer summary
-- =========================================
CREATE TABLE crib_payment_history (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    missed_payments_last_12m INT DEFAULT 0,
    late_payments_last_12m INT DEFAULT 0,
    on_time_payments_last_12m INT DEFAULT 0,
    max_days_past_due INT DEFAULT 0,
    loan_default_history BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_crib_payment_customer
        FOREIGN KEY (customer_id)
        REFERENCES crib_customers(id)
        ON DELETE CASCADE
);

-- =========================================
-- 6. CREDIT SUMMARY
-- One row per customer aggregated bureau-style summary
-- =========================================
CREATE TABLE crib_credit_summary (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    active_loan_count INT DEFAULT 0,
    secured_loan_count INT DEFAULT 0,
    unsecured_loan_count INT DEFAULT 0,
    active_facilities_count INT DEFAULT 0,
    closed_loan_count INT DEFAULT 0,
    defaulted_facilities_count INT DEFAULT 0,
    credit_card_count INT DEFAULT 0,
    total_loan_amount DECIMAL(15,2) DEFAULT 0,
    total_remaining_balance DECIMAL(15,2) DEFAULT 0,
    total_monthly_emi DECIMAL(15,2) DEFAULT 0,
    total_credit_limit DECIMAL(15,2) DEFAULT 0,
    total_card_outstanding DECIMAL(15,2) DEFAULT 0,
    credit_utilization_ratio DECIMAL(8,4) DEFAULT 0,
    rescheduled_loan_count INT DEFAULT 0,
    credit_score INT,
    risk_level VARCHAR(20),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_crib_summary_customer
        FOREIGN KEY (customer_id)
        REFERENCES crib_customers(id)
        ON DELETE CASCADE
);

-- =========================================
-- OPTIONAL CHECK CONSTRAINTS
-- =========================================

ALTER TABLE crib_loan_records
ADD CONSTRAINT chk_crib_loan_status
CHECK (status IN ('ACTIVE', 'CLOSED', 'DEFAULT', 'WRITTEN_OFF'));

ALTER TABLE crib_credit_card_records
ADD CONSTRAINT chk_crib_card_payment_status
CHECK (payment_status IN ('GOOD', 'LATE', 'DEFAULT'));

ALTER TABLE crib_other_liability_records
ADD CONSTRAINT chk_crib_other_liability_status
CHECK (status IN ('ACTIVE', 'CLOSED', 'DEFAULT'));

ALTER TABLE crib_credit_summary
ADD CONSTRAINT chk_crib_risk_level
CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH'));

-- =========================================
-- INDEXES FOR FASTER SEARCH
-- =========================================
CREATE INDEX idx_crib_customers_nic ON crib_customers(nic);
CREATE INDEX idx_crib_customers_name ON crib_customers(full_name);
CREATE INDEX idx_crib_loans_customer_id ON crib_loan_records(customer_id);
CREATE INDEX idx_crib_cards_customer_id ON crib_credit_card_records(customer_id);
CREATE INDEX idx_crib_liabilities_customer_id ON crib_other_liability_records(customer_id);
CREATE INDEX idx_crib_payment_customer_id ON crib_payment_history(customer_id);
CREATE INDEX idx_crib_summary_customer_id ON crib_credit_summary(customer_id);
CREATE INDEX idx_crib_summary_risk_level ON crib_credit_summary(risk_level);