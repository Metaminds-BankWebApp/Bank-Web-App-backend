-- Seed one bank-customer financial snapshot for LoanSense/CreditLens testing.
-- Demo user: bank.customer.demo (from V3 seeds)

WITH target_customer AS (
    SELECT
        bc.bank_customer_id,
        bc.officer_id
    FROM bank_customers bc
    JOIN users u ON u.user_id = bc.user_id
    WHERE u.username = 'bank.customer.demo'
),
inserted_record AS (
    INSERT INTO bank_customer_financial_records (
        bank_customer_id,
        verified_by_officer_id,
        data_source,
        created_at,
        updated_at
    )
    SELECT
        tc.bank_customer_id,
        tc.officer_id,
        'MANUAL',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    FROM target_customer tc
    WHERE tc.officer_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM bank_customer_financial_records existing
          WHERE existing.bank_customer_id = tc.bank_customer_id
      )
    RETURNING bank_record_id
)
SELECT 1
FROM inserted_record;

WITH target_record AS (
    SELECT r.bank_record_id
    FROM bank_customer_financial_records r
    JOIN bank_customers bc ON bc.bank_customer_id = r.bank_customer_id
    JOIN users u ON u.user_id = bc.user_id
    WHERE u.username = 'bank.customer.demo'
    ORDER BY r.created_at DESC, r.bank_record_id DESC
    LIMIT 1
)
INSERT INTO bank_customer_incomes (
    bank_record_id,
    income_category,
    amount,
    salary_type,
    employment_type,
    duration_months,
    income_stability,
    created_at
)
SELECT
    tr.bank_record_id,
    'SALARY',
    185000.00,
    'FIXED_BASIC_SALARY',
    'PERMANENT',
    12,
    NULL,
    CURRENT_TIMESTAMP
FROM target_record tr
WHERE NOT EXISTS (
    SELECT 1
    FROM bank_customer_incomes existing
    WHERE existing.bank_record_id = tr.bank_record_id
      AND existing.income_category = 'SALARY'
      AND existing.amount = 185000.00
      AND COALESCE(existing.salary_type, '') = 'FIXED_BASIC_SALARY'
      AND COALESCE(existing.employment_type, '') = 'PERMANENT'
      AND COALESCE(existing.duration_months, -1) = 12
      AND existing.income_stability IS NULL
);

WITH target_record AS (
    SELECT r.bank_record_id
    FROM bank_customer_financial_records r
    JOIN bank_customers bc ON bc.bank_customer_id = r.bank_customer_id
    JOIN users u ON u.user_id = bc.user_id
    WHERE u.username = 'bank.customer.demo'
    ORDER BY r.created_at DESC, r.bank_record_id DESC
    LIMIT 1
)
INSERT INTO bank_customer_loans (
    bank_record_id,
    loan_type,
    monthly_emi,
    remaining_balance,
    created_at
)
SELECT
    tr.bank_record_id,
    'PERSONAL_LOAN',
    28000.00,
    900000.00,
    CURRENT_TIMESTAMP
FROM target_record tr
WHERE NOT EXISTS (
    SELECT 1
    FROM bank_customer_loans existing
    WHERE existing.bank_record_id = tr.bank_record_id
      AND existing.loan_type = 'PERSONAL_LOAN'
      AND existing.monthly_emi = 28000.00
      AND existing.remaining_balance = 900000.00
);

WITH target_record AS (
    SELECT r.bank_record_id
    FROM bank_customer_financial_records r
    JOIN bank_customers bc ON bc.bank_customer_id = r.bank_customer_id
    JOIN users u ON u.user_id = bc.user_id
    WHERE u.username = 'bank.customer.demo'
    ORDER BY r.created_at DESC, r.bank_record_id DESC
    LIMIT 1
)
INSERT INTO bank_customer_cards (
    bank_record_id,
    provider,
    credit_limit,
    outstanding_balance,
    created_at
)
SELECT
    tr.bank_record_id,
    'HNB',
    250000.00,
    45000.00,
    CURRENT_TIMESTAMP
FROM target_record tr
WHERE NOT EXISTS (
    SELECT 1
    FROM bank_customer_cards existing
    WHERE existing.bank_record_id = tr.bank_record_id
      AND COALESCE(existing.provider, '') = 'HNB'
      AND existing.credit_limit = 250000.00
      AND existing.outstanding_balance = 45000.00
);

WITH target_record AS (
    SELECT r.bank_record_id
    FROM bank_customer_financial_records r
    JOIN bank_customers bc ON bc.bank_customer_id = r.bank_customer_id
    JOIN users u ON u.user_id = bc.user_id
    WHERE u.username = 'bank.customer.demo'
    ORDER BY r.created_at DESC, r.bank_record_id DESC
    LIMIT 1
)
INSERT INTO bank_customer_liabilities (
    bank_record_id,
    description,
    monthly_amount,
    created_at
)
SELECT
    tr.bank_record_id,
    'Vehicle lease installment',
    15000.00,
    CURRENT_TIMESTAMP
FROM target_record tr
WHERE NOT EXISTS (
    SELECT 1
    FROM bank_customer_liabilities existing
    WHERE existing.bank_record_id = tr.bank_record_id
      AND existing.description = 'Vehicle lease installment'
      AND existing.monthly_amount = 15000.00
);

WITH target_record AS (
    SELECT r.bank_record_id
    FROM bank_customer_financial_records r
    JOIN bank_customers bc ON bc.bank_customer_id = r.bank_customer_id
    JOIN users u ON u.user_id = bc.user_id
    WHERE u.username = 'bank.customer.demo'
    ORDER BY r.created_at DESC, r.bank_record_id DESC
    LIMIT 1
)
INSERT INTO bank_customer_missed_payments (
    bank_record_id,
    missed_payments,
    created_at
)
SELECT
    tr.bank_record_id,
    1,
    CURRENT_TIMESTAMP
FROM target_record tr
ON CONFLICT (bank_record_id) DO UPDATE
SET missed_payments = EXCLUDED.missed_payments;
