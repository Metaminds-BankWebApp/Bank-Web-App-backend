ALTER TABLE public_customer_financial_records
    ADD COLUMN IF NOT EXISTS income_step_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS loan_step_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS card_step_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS liability_step_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS review_step_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS application_submitted_at TIMESTAMP;

UPDATE public_customer_financial_records record
SET income_step_status = CASE
        WHEN EXISTS (
            SELECT 1 FROM public_customer_incomes income
            WHERE income.record_id = record.record_id
        ) THEN 'COMPLETED'
        ELSE income_step_status
    END,
    loan_step_status = CASE
        WHEN EXISTS (
            SELECT 1 FROM public_customer_loans loan
            WHERE loan.record_id = record.record_id
        ) THEN 'COMPLETED'
        ELSE loan_step_status
    END,
    card_step_status = CASE
        WHEN EXISTS (
            SELECT 1 FROM public_customer_cards card
            WHERE card.record_id = record.record_id
        ) THEN 'COMPLETED'
        ELSE card_step_status
    END,
    liability_step_status = CASE
        WHEN EXISTS (
            SELECT 1 FROM public_customer_liabilities liability
            WHERE liability.record_id = record.record_id
        ) OR EXISTS (
            SELECT 1 FROM public_customer_missed_payments payment
            WHERE payment.record_id = record.record_id
        ) THEN 'COMPLETED'
        ELSE liability_step_status
    END,
    review_step_status = CASE
        WHEN EXISTS (
            SELECT 1 FROM self_credit_evaluations evaluation
            WHERE evaluation.public_record_id = record.record_id
        ) THEN 'COMPLETED'
        ELSE review_step_status
    END,
    application_submitted_at = COALESCE(
        application_submitted_at,
        (
            SELECT MAX(evaluation.created_at)
            FROM self_credit_evaluations evaluation
            WHERE evaluation.public_record_id = record.record_id
        )
    );

ALTER TABLE public_customer_financial_records
    DROP CONSTRAINT IF EXISTS chk_public_customer_income_step_status,
    DROP CONSTRAINT IF EXISTS chk_public_customer_loan_step_status,
    DROP CONSTRAINT IF EXISTS chk_public_customer_card_step_status,
    DROP CONSTRAINT IF EXISTS chk_public_customer_liability_step_status,
    DROP CONSTRAINT IF EXISTS chk_public_customer_review_step_status;

ALTER TABLE public_customer_financial_records
    ADD CONSTRAINT chk_public_customer_income_step_status
        CHECK (income_step_status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
    ADD CONSTRAINT chk_public_customer_loan_step_status
        CHECK (loan_step_status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
    ADD CONSTRAINT chk_public_customer_card_step_status
        CHECK (card_step_status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
    ADD CONSTRAINT chk_public_customer_liability_step_status
        CHECK (liability_step_status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
    ADD CONSTRAINT chk_public_customer_review_step_status
        CHECK (review_step_status IN ('PENDING', 'COMPLETED'));
