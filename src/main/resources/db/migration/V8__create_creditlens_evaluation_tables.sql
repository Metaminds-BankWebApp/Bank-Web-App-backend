CREATE TABLE IF NOT EXISTS self_credit_evaluations (
    self_evaluation_id BIGSERIAL PRIMARY KEY,
    public_customer_id BIGINT NOT NULL,
    public_record_id BIGINT NOT NULL,
    total_risk_points INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    total_monthly_income NUMERIC(15,2) NOT NULL,
    total_monthly_debt_payment NUMERIC(15,2) NOT NULL,
    total_card_limit NUMERIC(15,2) NOT NULL,
    total_card_outstanding NUMERIC(15,2) NOT NULL,
    dti_ratio NUMERIC(6,4) NOT NULL,
    credit_utilization_ratio NUMERIC(6,4) NOT NULL,
    active_facilities_count INTEGER NOT NULL,
    missed_payments_count INTEGER NOT NULL,
    payment_history_points INTEGER NOT NULL,
    dti_points INTEGER NOT NULL,
    utilization_points INTEGER NOT NULL,
    income_stability_points INTEGER NOT NULL,
    exposure_points INTEGER NOT NULL,
    report_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_self_credit_evaluations_customer FOREIGN KEY (public_customer_id)
        REFERENCES public_customers (public_customer_id),
    CONSTRAINT fk_self_credit_evaluations_record FOREIGN KEY (public_record_id)
        REFERENCES public_customer_financial_records (record_id),
    CONSTRAINT chk_self_credit_evaluations_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_self_credit_evaluations_total_risk_points CHECK (total_risk_points >= 0 AND total_risk_points <= 100),
    CONSTRAINT chk_self_credit_evaluations_payment_history_points CHECK (payment_history_points >= 0 AND payment_history_points <= 30),
    CONSTRAINT chk_self_credit_evaluations_dti_points CHECK (dti_points >= 0 AND dti_points <= 25),
    CONSTRAINT chk_self_credit_evaluations_utilization_points CHECK (utilization_points >= 0 AND utilization_points <= 20),
    CONSTRAINT chk_self_credit_evaluations_income_stability_points CHECK (income_stability_points >= 0 AND income_stability_points <= 15),
    CONSTRAINT chk_self_credit_evaluations_exposure_points CHECK (exposure_points >= 0 AND exposure_points <= 10),
    CONSTRAINT chk_self_credit_evaluations_income_non_negative CHECK (total_monthly_income >= 0),
    CONSTRAINT chk_self_credit_evaluations_debt_non_negative CHECK (total_monthly_debt_payment >= 0),
    CONSTRAINT chk_self_credit_evaluations_card_limit_non_negative CHECK (total_card_limit >= 0),
    CONSTRAINT chk_self_credit_evaluations_card_outstanding_non_negative CHECK (total_card_outstanding >= 0),
    CONSTRAINT chk_self_credit_evaluations_dti_ratio_non_negative CHECK (dti_ratio >= 0),
    CONSTRAINT chk_self_credit_evaluations_utilization_ratio_non_negative CHECK (credit_utilization_ratio >= 0),
    CONSTRAINT chk_self_credit_evaluations_active_facilities_non_negative CHECK (active_facilities_count >= 0),
    CONSTRAINT chk_self_credit_evaluations_missed_payments_non_negative CHECK (missed_payments_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_self_credit_evaluations_customer
    ON self_credit_evaluations(public_customer_id);

CREATE INDEX IF NOT EXISTS idx_self_credit_evaluations_record
    ON self_credit_evaluations(public_record_id);

CREATE INDEX IF NOT EXISTS idx_self_credit_evaluations_created_at
    ON self_credit_evaluations(created_at DESC);

CREATE TABLE IF NOT EXISTS bank_credit_evaluations (
    bank_evaluation_id BIGSERIAL PRIMARY KEY,
    bank_customer_id BIGINT NOT NULL,
    bank_record_id BIGINT NOT NULL,
    evaluated_by_officer_id BIGINT NOT NULL,
    evaluation_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    remarks TEXT,
    total_risk_points INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    total_monthly_income NUMERIC(15,2) NOT NULL,
    total_monthly_debt_payment NUMERIC(15,2) NOT NULL,
    total_card_limit NUMERIC(15,2) NOT NULL,
    total_card_outstanding NUMERIC(15,2) NOT NULL,
    dti_ratio NUMERIC(6,4) NOT NULL,
    credit_utilization_ratio NUMERIC(6,4) NOT NULL,
    active_facilities_count INTEGER NOT NULL,
    missed_payments_count INTEGER NOT NULL,
    payment_history_points INTEGER NOT NULL,
    dti_points INTEGER NOT NULL,
    utilization_points INTEGER NOT NULL,
    income_stability_points INTEGER NOT NULL,
    exposure_points INTEGER NOT NULL,
    report_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_credit_evaluations_customer FOREIGN KEY (bank_customer_id)
        REFERENCES bank_customers (bank_customer_id),
    CONSTRAINT fk_bank_credit_evaluations_record FOREIGN KEY (bank_record_id)
        REFERENCES bank_customer_financial_records (bank_record_id),
    CONSTRAINT fk_bank_credit_evaluations_officer FOREIGN KEY (evaluated_by_officer_id)
        REFERENCES bank_officers (officer_id),
    CONSTRAINT chk_bank_credit_evaluations_source CHECK (evaluation_source IN ('MANUAL', 'CRIB_MERGED', 'CRIB_ONLY')),
    CONSTRAINT chk_bank_credit_evaluations_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_bank_credit_evaluations_total_risk_points CHECK (total_risk_points >= 0 AND total_risk_points <= 100),
    CONSTRAINT chk_bank_credit_evaluations_payment_history_points CHECK (payment_history_points >= 0 AND payment_history_points <= 30),
    CONSTRAINT chk_bank_credit_evaluations_dti_points CHECK (dti_points >= 0 AND dti_points <= 25),
    CONSTRAINT chk_bank_credit_evaluations_utilization_points CHECK (utilization_points >= 0 AND utilization_points <= 20),
    CONSTRAINT chk_bank_credit_evaluations_income_stability_points CHECK (income_stability_points >= 0 AND income_stability_points <= 15),
    CONSTRAINT chk_bank_credit_evaluations_exposure_points CHECK (exposure_points >= 0 AND exposure_points <= 10),
    CONSTRAINT chk_bank_credit_evaluations_income_non_negative CHECK (total_monthly_income >= 0),
    CONSTRAINT chk_bank_credit_evaluations_debt_non_negative CHECK (total_monthly_debt_payment >= 0),
    CONSTRAINT chk_bank_credit_evaluations_card_limit_non_negative CHECK (total_card_limit >= 0),
    CONSTRAINT chk_bank_credit_evaluations_card_outstanding_non_negative CHECK (total_card_outstanding >= 0),
    CONSTRAINT chk_bank_credit_evaluations_dti_ratio_non_negative CHECK (dti_ratio >= 0),
    CONSTRAINT chk_bank_credit_evaluations_utilization_ratio_non_negative CHECK (credit_utilization_ratio >= 0),
    CONSTRAINT chk_bank_credit_evaluations_active_facilities_non_negative CHECK (active_facilities_count >= 0),
    CONSTRAINT chk_bank_credit_evaluations_missed_payments_non_negative CHECK (missed_payments_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bank_credit_evaluations_customer
    ON bank_credit_evaluations(bank_customer_id);

CREATE INDEX IF NOT EXISTS idx_bank_credit_evaluations_record
    ON bank_credit_evaluations(bank_record_id);

CREATE INDEX IF NOT EXISTS idx_bank_credit_evaluations_officer
    ON bank_credit_evaluations(evaluated_by_officer_id);

CREATE INDEX IF NOT EXISTS idx_bank_credit_evaluations_created_at
    ON bank_credit_evaluations(created_at DESC);
