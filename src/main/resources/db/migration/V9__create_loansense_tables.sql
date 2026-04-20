CREATE TABLE IF NOT EXISTS loan_policies (
    policy_id BIGSERIAL PRIMARY KEY,
    loan_type VARCHAR(30) NOT NULL UNIQUE,
    max_dbr_ratio NUMERIC(5,4) NOT NULL,
    base_interest_rate NUMERIC(5,2) NOT NULL,
    max_tenure_months INTEGER NOT NULL,
    min_age INTEGER NOT NULL,
    max_age INTEGER NOT NULL,
    max_finance_percentage NUMERIC(5,2),
    min_income_required NUMERIC(15,2),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_loan_policies_loan_type CHECK (loan_type IN ('PERSONAL', 'VEHICLE', 'EDUCATION', 'HOUSING')),
    CONSTRAINT chk_loan_policies_max_dbr_ratio CHECK (max_dbr_ratio > 0 AND max_dbr_ratio <= 1),
    CONSTRAINT chk_loan_policies_base_interest_rate CHECK (base_interest_rate >= 0),
    CONSTRAINT chk_loan_policies_max_tenure CHECK (max_tenure_months > 0),
    CONSTRAINT chk_loan_policies_age_range CHECK (min_age >= 18 AND max_age >= min_age),
    CONSTRAINT chk_loan_policies_max_finance_percentage CHECK (
        max_finance_percentage IS NULL OR (max_finance_percentage >= 0 AND max_finance_percentage <= 100)
    ),
    CONSTRAINT chk_loan_policies_min_income_required CHECK (min_income_required IS NULL OR min_income_required >= 0),
    CONSTRAINT chk_loan_policies_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_loan_policies_status
    ON loan_policies(status);

CREATE TABLE IF NOT EXISTS risk_adjustments (
    adjustment_id BIGSERIAL PRIMARY KEY,
    risk_level VARCHAR(20) NOT NULL UNIQUE,
    multiplier NUMERIC(4,2) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_risk_adjustments_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_risk_adjustments_multiplier CHECK (multiplier > 0)
);

CREATE TABLE IF NOT EXISTS loansense_evaluations (
    loansense_evaluation_id BIGSERIAL PRIMARY KEY,
    bank_customer_id BIGINT NOT NULL,
    bank_record_id BIGINT NOT NULL,
    bank_evaluation_id BIGINT NOT NULL,
    monthly_income NUMERIC(15,2) NOT NULL,
    total_existing_loan_emi NUMERIC(15,2) NOT NULL,
    leasing_hire_purchase_payment NUMERIC(15,2) NOT NULL,
    credit_card_outstanding NUMERIC(15,2) NOT NULL,
    credit_card_limit NUMERIC(15,2) NOT NULL,
    credit_card_min_payment NUMERIC(15,2),
    missed_payments_count INTEGER NOT NULL,
    tmdo NUMERIC(15,2) NOT NULL,
    dbr NUMERIC(6,4) NOT NULL,
    max_allowed_emi NUMERIC(15,2) NOT NULL,
    available_emi_capacity NUMERIC(15,2) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_multiplier NUMERIC(4,2) NOT NULL,
    overall_status VARCHAR(30) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loansense_evaluations_customer FOREIGN KEY (bank_customer_id)
        REFERENCES bank_customers(bank_customer_id),
    CONSTRAINT fk_loansense_evaluations_record FOREIGN KEY (bank_record_id)
        REFERENCES bank_customer_financial_records(bank_record_id),
    CONSTRAINT fk_loansense_evaluations_bank_evaluation FOREIGN KEY (bank_evaluation_id)
        REFERENCES bank_credit_evaluations(bank_evaluation_id),
    CONSTRAINT chk_loansense_evaluations_income_non_negative CHECK (monthly_income >= 0),
    CONSTRAINT chk_loansense_evaluations_loan_emi_non_negative CHECK (total_existing_loan_emi >= 0),
    CONSTRAINT chk_loansense_evaluations_leasing_non_negative CHECK (leasing_hire_purchase_payment >= 0),
    CONSTRAINT chk_loansense_evaluations_card_outstanding_non_negative CHECK (credit_card_outstanding >= 0),
    CONSTRAINT chk_loansense_evaluations_card_limit_non_negative CHECK (credit_card_limit >= 0),
    CONSTRAINT chk_loansense_evaluations_card_min_payment_non_negative CHECK (
        credit_card_min_payment IS NULL OR credit_card_min_payment >= 0
    ),
    CONSTRAINT chk_loansense_evaluations_missed_payments_non_negative CHECK (missed_payments_count >= 0),
    CONSTRAINT chk_loansense_evaluations_tmdo_non_negative CHECK (tmdo >= 0),
    CONSTRAINT chk_loansense_evaluations_dbr_non_negative CHECK (dbr >= 0),
    CONSTRAINT chk_loansense_evaluations_max_allowed_emi_non_negative CHECK (max_allowed_emi >= 0),
    CONSTRAINT chk_loansense_evaluations_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_loansense_evaluations_overall_status CHECK (overall_status IN ('ELIGIBLE', 'PARTIALLY_ELIGIBLE', 'NOT_ELIGIBLE'))
);

CREATE INDEX IF NOT EXISTS idx_loansense_evaluations_customer
    ON loansense_evaluations(bank_customer_id);

CREATE INDEX IF NOT EXISTS idx_loansense_evaluations_record
    ON loansense_evaluations(bank_record_id);

CREATE INDEX IF NOT EXISTS idx_loansense_evaluations_bank_evaluation
    ON loansense_evaluations(bank_evaluation_id);

CREATE INDEX IF NOT EXISTS idx_loansense_evaluations_created_at
    ON loansense_evaluations(created_at DESC);

CREATE TABLE IF NOT EXISTS loan_eligibility_results (
    loan_result_id BIGSERIAL PRIMARY KEY,
    loansense_evaluation_id BIGINT NOT NULL,
    loan_type VARCHAR(30) NOT NULL,
    customer_age INTEGER NOT NULL,
    asset_value NUMERIC(15,2),
    estimated_emi NUMERIC(15,2) NOT NULL,
    recommended_max_amount NUMERIC(15,2),
    interest_rate NUMERIC(5,2),
    tenure_months INTEGER,
    eligibility_status VARCHAR(30) NOT NULL,
    decision_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_eligibility_results_evaluation FOREIGN KEY (loansense_evaluation_id)
        REFERENCES loansense_evaluations(loansense_evaluation_id),
    CONSTRAINT chk_loan_eligibility_results_loan_type CHECK (loan_type IN ('PERSONAL', 'VEHICLE', 'EDUCATION', 'HOUSING')),
    CONSTRAINT chk_loan_eligibility_results_customer_age CHECK (customer_age >= 18),
    CONSTRAINT chk_loan_eligibility_results_asset_value_non_negative CHECK (asset_value IS NULL OR asset_value >= 0),
    CONSTRAINT chk_loan_eligibility_results_estimated_emi_non_negative CHECK (estimated_emi >= 0),
    CONSTRAINT chk_loan_eligibility_results_recommended_max_amount_non_negative CHECK (
        recommended_max_amount IS NULL OR recommended_max_amount >= 0
    ),
    CONSTRAINT chk_loan_eligibility_results_interest_rate_non_negative CHECK (interest_rate IS NULL OR interest_rate >= 0),
    CONSTRAINT chk_loan_eligibility_results_tenure_months_positive CHECK (tenure_months IS NULL OR tenure_months > 0),
    CONSTRAINT chk_loan_eligibility_results_status CHECK (
        eligibility_status IN ('ELIGIBLE', 'PARTIALLY_ELIGIBLE', 'NOT_ELIGIBLE')
    )
);

CREATE INDEX IF NOT EXISTS idx_loan_eligibility_results_evaluation
    ON loan_eligibility_results(loansense_evaluation_id);

CREATE INDEX IF NOT EXISTS idx_loan_eligibility_results_loan_type
    ON loan_eligibility_results(loan_type);

INSERT INTO loan_policies (
    loan_type,
    max_dbr_ratio,
    base_interest_rate,
    max_tenure_months,
    min_age,
    max_age,
    max_finance_percentage,
    min_income_required,
    status
) VALUES
    ('PERSONAL', 0.4000, 17.00, 60, 21, 60, NULL, 50000.00, 'ACTIVE'),
    ('VEHICLE', 0.4000, 15.00, 84, 21, 65, 80.00, 75000.00, 'ACTIVE'),
    ('EDUCATION', 0.4000, 12.00, 120, 18, 55, NULL, 200000.00, 'ACTIVE'),
    ('HOUSING', 0.4000, 10.00, 240, 21, 60, 90.00, 250000.00, 'ACTIVE')
ON CONFLICT (loan_type) DO NOTHING;

INSERT INTO risk_adjustments (
    risk_level,
    multiplier,
    description
) VALUES
    ('LOW', 1.00, 'Low-risk profiles receive the full recommended loan amount.'),
    ('MEDIUM', 0.85, 'Medium-risk profiles receive a moderate reduction to protect repayment capacity.'),
    ('HIGH', 0.65, 'High-risk profiles receive a conservative reduction because of elevated repayment risk.')
ON CONFLICT (risk_level) DO NOTHING;
