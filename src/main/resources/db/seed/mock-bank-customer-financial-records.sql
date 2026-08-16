-- DEVELOPMENT/STAGING ONLY. Run after creating the five mock bank customers.
-- Creates one related financial record, income, loan, card, liability, and
-- missed-payment record for each mock customer.

WITH seed (username) AS (
    VALUES
        ('bank.customer.01'),
        ('bank.customer.02'),
        ('bank.customer.03'),
        ('bank.customer.04'),
        ('bank.customer.05')
)
INSERT INTO bank_customer_financial_records (
    bank_customer_id, verified_by_officer_id, data_source, created_at, updated_at
)
SELECT customer.bank_customer_id, customer.officer_id, 'MANUAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seed
JOIN users user_record ON user_record.username = seed.username
JOIN bank_customers customer ON customer.user_id = user_record.user_id
WHERE NOT EXISTS (
    SELECT 1
    FROM bank_customer_financial_records existing
    WHERE existing.bank_customer_id = customer.bank_customer_id
);

WITH seed (username, income_category, amount, salary_type, employment_type, duration_months) AS (
    VALUES
        ('bank.customer.01', 'SALARY', 185000.00, 'FIXED_BASIC_SALARY', 'PERMANENT', 24),
        ('bank.customer.02', 'SALARY', 145000.00, 'FIXED_BASIC_SALARY', 'PERMANENT', 18),
        ('bank.customer.03', 'BUSINESS', 210000.00, NULL, NULL, 36),
        ('bank.customer.04', 'SALARY', 120000.00, 'FIXED_BASIC_SALARY', 'CONTRACT', 12),
        ('bank.customer.05', 'SALARY', 165000.00, 'FIXED_BASIC_SALARY', 'PERMANENT', 30)
)
INSERT INTO bank_customer_incomes (
    bank_record_id, income_category, amount, salary_type, employment_type,
    duration_months, income_stability, created_at
)
SELECT record.bank_record_id, seed.income_category, seed.amount, seed.salary_type,
       seed.employment_type, seed.duration_months, 'STABLE', CURRENT_TIMESTAMP
FROM seed
JOIN users user_record ON user_record.username = seed.username
JOIN bank_customers customer ON customer.user_id = user_record.user_id
JOIN bank_customer_financial_records record ON record.bank_customer_id = customer.bank_customer_id
WHERE NOT EXISTS (
    SELECT 1 FROM bank_customer_incomes existing
    WHERE existing.bank_record_id = record.bank_record_id
      AND existing.income_category = seed.income_category
      AND existing.amount = seed.amount
);

WITH seed (username, loan_type, monthly_emi, remaining_balance) AS (
    VALUES
        ('bank.customer.01', 'PERSONAL_LOAN', 18000.00, 420000.00),
        ('bank.customer.02', 'VEHICLE_LOAN', 22000.00, 650000.00),
        ('bank.customer.03', 'HOME_LOAN', 35000.00, 2800000.00),
        ('bank.customer.04', 'PERSONAL_LOAN', 12000.00, 210000.00),
        ('bank.customer.05', 'VEHICLE_LOAN', 26000.00, 780000.00)
)
INSERT INTO bank_customer_loans (bank_record_id, loan_type, monthly_emi, remaining_balance, created_at)
SELECT record.bank_record_id, seed.loan_type, seed.monthly_emi, seed.remaining_balance, CURRENT_TIMESTAMP
FROM seed
JOIN users user_record ON user_record.username = seed.username
JOIN bank_customers customer ON customer.user_id = user_record.user_id
JOIN bank_customer_financial_records record ON record.bank_customer_id = customer.bank_customer_id
WHERE NOT EXISTS (
    SELECT 1 FROM bank_customer_loans existing
    WHERE existing.bank_record_id = record.bank_record_id
      AND existing.loan_type = seed.loan_type
      AND existing.monthly_emi = seed.monthly_emi
);

WITH seed (username, provider, credit_limit, outstanding_balance) AS (
    VALUES
        ('bank.customer.01', 'HNB', 250000.00, 45000.00),
        ('bank.customer.02', 'Commercial', 180000.00, 52000.00),
        ('bank.customer.03', 'Sampath', 300000.00, 120000.00),
        ('bank.customer.04', 'BOC', 100000.00, 18000.00),
        ('bank.customer.05', 'DFCC', 220000.00, 76000.00)
)
INSERT INTO bank_customer_cards (bank_record_id, provider, credit_limit, outstanding_balance, created_at)
SELECT record.bank_record_id, seed.provider, seed.credit_limit, seed.outstanding_balance, CURRENT_TIMESTAMP
FROM seed
JOIN users user_record ON user_record.username = seed.username
JOIN bank_customers customer ON customer.user_id = user_record.user_id
JOIN bank_customer_financial_records record ON record.bank_customer_id = customer.bank_customer_id
WHERE NOT EXISTS (
    SELECT 1 FROM bank_customer_cards existing
    WHERE existing.bank_record_id = record.bank_record_id
      AND existing.provider = seed.provider
      AND existing.credit_limit = seed.credit_limit
);

WITH seed (username, description, monthly_amount) AS (
    VALUES
        ('bank.customer.01', 'Vehicle lease installment', 15000.00),
        ('bank.customer.02', 'Family support', 10000.00),
        ('bank.customer.03', 'Business commitment', 25000.00),
        ('bank.customer.04', 'Insurance premium', 8000.00),
        ('bank.customer.05', 'Lease installment', 14000.00)
)
INSERT INTO bank_customer_liabilities (bank_record_id, description, monthly_amount, created_at)
SELECT record.bank_record_id, seed.description, seed.monthly_amount, CURRENT_TIMESTAMP
FROM seed
JOIN users user_record ON user_record.username = seed.username
JOIN bank_customers customer ON customer.user_id = user_record.user_id
JOIN bank_customer_financial_records record ON record.bank_customer_id = customer.bank_customer_id
WHERE NOT EXISTS (
    SELECT 1 FROM bank_customer_liabilities existing
    WHERE existing.bank_record_id = record.bank_record_id
      AND existing.description = seed.description
);

WITH seed (username, missed_payments) AS (
    VALUES
        ('bank.customer.01', 0),
        ('bank.customer.02', 1),
        ('bank.customer.03', 2),
        ('bank.customer.04', 0),
        ('bank.customer.05', 1)
)
INSERT INTO bank_customer_missed_payments (bank_record_id, missed_payments, created_at)
SELECT record.bank_record_id, seed.missed_payments, CURRENT_TIMESTAMP
FROM seed
JOIN users user_record ON user_record.username = seed.username
JOIN bank_customers customer ON customer.user_id = user_record.user_id
JOIN bank_customer_financial_records record ON record.bank_customer_id = customer.bank_customer_id
ON CONFLICT (bank_record_id) DO UPDATE
SET missed_payments = EXCLUDED.missed_payments;
