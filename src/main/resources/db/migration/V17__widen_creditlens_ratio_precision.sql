ALTER TABLE self_credit_evaluations
    ALTER COLUMN dti_ratio TYPE NUMERIC(10,4),
    ALTER COLUMN credit_utilization_ratio TYPE NUMERIC(10,4);

ALTER TABLE bank_credit_evaluations
    ALTER COLUMN dti_ratio TYPE NUMERIC(10,4),
    ALTER COLUMN credit_utilization_ratio TYPE NUMERIC(10,4);