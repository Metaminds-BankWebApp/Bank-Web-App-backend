-- Restore and strengthen indexes used while loading public CreditLens reports.
-- IF NOT EXISTS keeps this migration safe for databases where the original
-- V5/V8 index definitions were already applied.

CREATE INDEX IF NOT EXISTS idx_public_customer_financial_records_customer
    ON public_customer_financial_records(public_customer_id);

CREATE INDEX IF NOT EXISTS idx_public_customer_incomes_record
    ON public_customer_incomes(record_id);

CREATE INDEX IF NOT EXISTS idx_public_customer_loans_record
    ON public_customer_loans(record_id);

CREATE INDEX IF NOT EXISTS idx_public_customer_cards_record
    ON public_customer_cards(record_id);

CREATE INDEX IF NOT EXISTS idx_public_customer_liabilities_record
    ON public_customer_liabilities(record_id);

CREATE INDEX IF NOT EXISTS idx_self_credit_evaluations_customer
    ON self_credit_evaluations(public_customer_id);

CREATE INDEX IF NOT EXISTS idx_self_credit_evaluations_record
    ON self_credit_evaluations(public_record_id);

CREATE INDEX IF NOT EXISTS idx_self_credit_evaluations_customer_created_at
    ON self_credit_evaluations(public_customer_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_self_credit_evaluations_record_created_at
    ON self_credit_evaluations(public_record_id, created_at DESC);
