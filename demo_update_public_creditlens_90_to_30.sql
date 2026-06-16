-- Update only PublicCustomer_01 so the 12-month CreditLens trend moves
-- from about 90 risk points down to 30 risk points.
-- This does not change table structure.

BEGIN;

DROP TABLE IF EXISTS demo_public_credit_trend_target;

CREATE TEMP TABLE demo_public_credit_trend_target (
    month_no INTEGER PRIMARY KEY,
    target_points INTEGER NOT NULL,
    loan_emi NUMERIC(15,2) NOT NULL,
    remaining_balance NUMERIC(15,2) NOT NULL,
    card_limit NUMERIC(15,2) NOT NULL,
    card_outstanding NUMERIC(15,2) NOT NULL,
    liability_count INTEGER NOT NULL,
    liability_amount NUMERIC(15,2) NOT NULL,
    missed_payments INTEGER NOT NULL,
    employment_type VARCHAR(30) NOT NULL,
    duration_months INTEGER,
    income_stability_points INTEGER NOT NULL,
    payment_history_points INTEGER NOT NULL,
    dti_points INTEGER NOT NULL,
    utilization_points INTEGER NOT NULL,
    exposure_points INTEGER NOT NULL
);

INSERT INTO demo_public_credit_trend_target VALUES
    (1,  90, 85000.00, 1700000.00, 150000.00,  97500.00, 3, 10000.00, 5, 'FREELANCE', NULL, 15, 30, 25, 10, 10),
    (2,  88, 80000.00, 1580000.00, 150000.00, 120000.00, 1, 12000.00, 5, 'PERMANENT', 12, 8, 30, 25, 20, 5),
    (3,  78, 84000.00, 1460000.00, 150000.00,  90000.00, 1,  8000.00, 5, 'PERMANENT', 12, 8, 30, 25, 10, 5),
    (4,  73, 88000.00, 1340000.00, 150000.00,  90000.00, 0,     0.00, 5, 'PERMANENT', 12, 8, 30, 25, 10, 0),
    (5,  66, 80000.00, 1220000.00, 150000.00,  90000.00, 1,  8000.00, 2, 'PERMANENT', 12, 8, 18, 25, 10, 5),
    (6,  61, 90000.00, 1100000.00, 150000.00,  90000.00, 0,     0.00, 2, 'PERMANENT', 12, 8, 18, 25, 10, 0),
    (7,  53, 60000.00,  980000.00, 150000.00,  75000.00, 1, 10000.00, 2, 'PERMANENT', 12, 8, 18, 12, 10, 5),
    (8,  43, 62000.00,  880000.00, 150000.00,  45000.00, 1,  9000.00, 2, 'PERMANENT', 12, 8, 18, 12,  0, 5),
    (9,  38, 60000.00,  790000.00, 150000.00,  75000.00, 0,     0.00, 1, 'PERMANENT', 12, 8,  8, 12, 10, 0),
    (10, 35, 52000.00,  720000.00, 150000.00,  75000.00, 1,  8000.00, 0, 'PERMANENT', 12, 8,  0, 12, 10, 5),
    (11, 33, 60000.00,  670000.00, 150000.00,  45000.00, 1,  7000.00, 1, 'PERMANENT', 12, 8,  8, 12,  0, 5),
    (12, 30, 60000.00,  620000.00, 150000.00,  75000.00, 0,     0.00, 0, 'PERMANENT', 12, 8,  0, 12, 10, 0);

DELETE FROM self_credit_evaluations e
USING public_customer_financial_records r
JOIN public_customers pc ON pc.public_customer_id = r.public_customer_id
JOIN users u ON u.user_id = pc.user_id
WHERE e.public_record_id = r.record_id
  AND u.username = 'PublicCustomer_01';

DELETE FROM public_customer_incomes i
USING public_customer_financial_records r
JOIN public_customers pc ON pc.public_customer_id = r.public_customer_id
JOIN users u ON u.user_id = pc.user_id
WHERE i.record_id = r.record_id
  AND u.username = 'PublicCustomer_01';

DELETE FROM public_customer_loans l
USING public_customer_financial_records r
JOIN public_customers pc ON pc.public_customer_id = r.public_customer_id
JOIN users u ON u.user_id = pc.user_id
WHERE l.record_id = r.record_id
  AND u.username = 'PublicCustomer_01';

DELETE FROM public_customer_cards c
USING public_customer_financial_records r
JOIN public_customers pc ON pc.public_customer_id = r.public_customer_id
JOIN users u ON u.user_id = pc.user_id
WHERE c.record_id = r.record_id
  AND u.username = 'PublicCustomer_01';

DELETE FROM public_customer_liabilities li
USING public_customer_financial_records r
JOIN public_customers pc ON pc.public_customer_id = r.public_customer_id
JOIN users u ON u.user_id = pc.user_id
WHERE li.record_id = r.record_id
  AND u.username = 'PublicCustomer_01';

DELETE FROM public_customer_missed_payments mp
USING public_customer_financial_records r
JOIN public_customers pc ON pc.public_customer_id = r.public_customer_id
JOIN users u ON u.user_id = pc.user_id
WHERE mp.record_id = r.record_id
  AND u.username = 'PublicCustomer_01';

WITH target_records AS (
    SELECT
        pc.public_customer_id,
        r.record_id,
        t.*,
        (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date AS month_date
    FROM demo_public_credit_trend_target t
    JOIN users u ON u.username = 'PublicCustomer_01'
    JOIN public_customers pc ON pc.user_id = u.user_id
    JOIN public_customer_financial_records r
        ON r.public_customer_id = pc.public_customer_id
       AND DATE(r.created_at) = (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date
)
INSERT INTO public_customer_incomes (
    record_id, income_category, amount, salary_type, employment_type,
    duration_months, income_stability, created_at
)
SELECT
    record_id,
    'SALARY',
    180000.00,
    'FIXED_BASIC_SALARY',
    employment_type,
    duration_months,
    NULL,
    month_date + TIME '10:05'
FROM target_records;

WITH target_records AS (
    SELECT
        r.record_id,
        t.*,
        (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date AS month_date
    FROM demo_public_credit_trend_target t
    JOIN users u ON u.username = 'PublicCustomer_01'
    JOIN public_customers pc ON pc.user_id = u.user_id
    JOIN public_customer_financial_records r
        ON r.public_customer_id = pc.public_customer_id
       AND DATE(r.created_at) = (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date
)
INSERT INTO public_customer_loans (record_id, loan_type, monthly_emi, remaining_balance, created_at)
SELECT record_id, 'PERSONAL_LOAN', loan_emi, remaining_balance, month_date + TIME '10:10'
FROM target_records;

WITH target_records AS (
    SELECT
        r.record_id,
        t.*,
        (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date AS month_date
    FROM demo_public_credit_trend_target t
    JOIN users u ON u.username = 'PublicCustomer_01'
    JOIN public_customers pc ON pc.user_id = u.user_id
    JOIN public_customer_financial_records r
        ON r.public_customer_id = pc.public_customer_id
       AND DATE(r.created_at) = (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date
)
INSERT INTO public_customer_cards (record_id, provider, credit_limit, outstanding_balance, created_at)
SELECT record_id, 'HNB', card_limit, card_outstanding, month_date + TIME '10:15'
FROM target_records;

WITH target_records AS (
    SELECT
        r.record_id,
        t.*,
        (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date AS month_date
    FROM demo_public_credit_trend_target t
    JOIN users u ON u.username = 'PublicCustomer_01'
    JOIN public_customers pc ON pc.user_id = u.user_id
    JOIN public_customer_financial_records r
        ON r.public_customer_id = pc.public_customer_id
       AND DATE(r.created_at) = (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date
)
INSERT INTO public_customer_liabilities (record_id, description, monthly_amount, created_at)
SELECT
    tr.record_id,
    'Trend demo liability ' || gs.liability_no,
    tr.liability_amount,
    tr.month_date + TIME '10:20'
FROM target_records tr
CROSS JOIN LATERAL generate_series(1, tr.liability_count) AS gs(liability_no);

WITH target_records AS (
    SELECT
        r.record_id,
        t.*,
        (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date AS month_date
    FROM demo_public_credit_trend_target t
    JOIN users u ON u.username = 'PublicCustomer_01'
    JOIN public_customers pc ON pc.user_id = u.user_id
    JOIN public_customer_financial_records r
        ON r.public_customer_id = pc.public_customer_id
       AND DATE(r.created_at) = (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date
)
INSERT INTO public_customer_missed_payments (record_id, missed_payments, created_at)
SELECT record_id, missed_payments, month_date + TIME '10:25'
FROM target_records;

WITH target_records AS (
    SELECT
        pc.public_customer_id,
        r.record_id,
        t.*,
        (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date AS month_date,
        ROUND(t.loan_emi + (t.card_outstanding * 0.05) + (t.liability_count * t.liability_amount), 2) AS debt_payment,
        2 + t.liability_count AS active_facilities_count
    FROM demo_public_credit_trend_target t
    JOIN users u ON u.username = 'PublicCustomer_01'
    JOIN public_customers pc ON pc.user_id = u.user_id
    JOIN public_customer_financial_records r
        ON r.public_customer_id = pc.public_customer_id
       AND DATE(r.created_at) = (date_trunc('month', CURRENT_DATE)::date - ((12 - t.month_no) * INTERVAL '1 month'))::date
)
INSERT INTO self_credit_evaluations (
    public_customer_id, public_record_id, total_risk_points, risk_level,
    total_monthly_income, total_monthly_debt_payment, total_card_limit, total_card_outstanding,
    dti_ratio, credit_utilization_ratio, active_facilities_count, missed_payments_count,
    payment_history_points, dti_points, utilization_points, income_stability_points,
    exposure_points, report_generated, created_at
)
SELECT
    public_customer_id,
    record_id,
    target_points,
    CASE WHEN target_points <= 33 THEN 'LOW' WHEN target_points <= 66 THEN 'MEDIUM' ELSE 'HIGH' END,
    180000.00,
    debt_payment,
    card_limit,
    card_outstanding,
    ROUND(debt_payment / 180000.00, 4),
    ROUND(card_outstanding / card_limit, 4),
    active_facilities_count,
    missed_payments,
    payment_history_points,
    dti_points,
    utilization_points,
    income_stability_points,
    exposure_points,
    FALSE,
    month_date + TIME '11:00'
FROM target_records;

DROP TABLE IF EXISTS demo_public_credit_trend_target;

COMMIT;

SELECT
    TO_CHAR(e.created_at, 'Mon YYYY') AS month,
    e.total_risk_points,
    e.risk_level
FROM self_credit_evaluations e
JOIN public_customers pc ON pc.public_customer_id = e.public_customer_id
JOIN users u ON u.user_id = pc.user_id
WHERE u.username = 'PublicCustomer_01'
ORDER BY e.created_at;

