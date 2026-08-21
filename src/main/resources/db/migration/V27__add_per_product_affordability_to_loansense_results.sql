ALTER TABLE loan_eligibility_results
    ADD COLUMN IF NOT EXISTS policy_max_dbr_ratio NUMERIC(5,4),
    ADD COLUMN IF NOT EXISTS max_allowed_emi NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS available_emi_capacity NUMERIC(15,2);

ALTER TABLE loan_eligibility_results
    ADD CONSTRAINT chk_loan_eligibility_results_policy_dbr_non_negative
        CHECK (policy_max_dbr_ratio IS NULL OR policy_max_dbr_ratio >= 0),
    ADD CONSTRAINT chk_loan_eligibility_results_max_allowed_emi_non_negative
        CHECK (max_allowed_emi IS NULL OR max_allowed_emi >= 0);
