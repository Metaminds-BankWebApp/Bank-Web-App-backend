ALTER TABLE expense_records
    DROP CONSTRAINT IF EXISTS chk_expense_records_payment_type_allowed;

ALTER TABLE expense_records
    ADD CONSTRAINT chk_expense_records_payment_type_allowed
        CHECK (UPPER(payment_type) IN ('CASH', 'BANK_TRANSFER', 'CARD'));
