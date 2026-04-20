ALTER TABLE expense_records
    ADD COLUMN IF NOT EXISTS tracking_source VARCHAR(30),
    ADD COLUMN IF NOT EXISTS tracking_reference VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS uq_expense_records_tracking_reference
    ON expense_records(tracking_source, tracking_reference)
    WHERE tracking_source IS NOT NULL AND tracking_reference IS NOT NULL;