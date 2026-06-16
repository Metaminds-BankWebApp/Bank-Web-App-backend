-- Demo seed for CreditLens 12-month trend data.
-- PostgreSQL script. Run manually in pgAdmin/psql against your app database.
-- Login password for all demo users below: Demo@1234

BEGIN;

DROP TABLE IF EXISTS demo_creditlens_monthly_seed;
DROP TABLE IF EXISTS demo_creditlens_profiles;

INSERT INTO roles (role_name, description, created_at)
VALUES
    ('BANK_OFFICER', 'Bank officer user', CURRENT_TIMESTAMP),
    ('BANK_CUSTOMER', 'Bank customer user', CURRENT_TIMESTAMP),
    ('PUBLIC_CUSTOMER', 'Public customer user', CURRENT_TIMESTAMP)
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO branches (branch_code, branch_name, branch_email, branch_phone, address, status, created_at, updated_at)
VALUES
    ('DEMO-001', 'Demo Branch', 'demo.branch@primecore.local', '0112999001', 'Demo Branch, Colombo', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (branch_code) DO NOTHING;

WITH officer_role AS (
    SELECT role_id FROM roles WHERE role_name = 'BANK_OFFICER'
)
INSERT INTO users (
    role_id, username, email, password_hash, first_name, last_name, phone, nic,
    status, dob, province, address, created_at, updated_at
)
SELECT
    officer_role.role_id, 'BankOfficer_01', 'bankofficer01@primecore.local',
    'Demo@1234', 'Bank', 'Officer', '0772999001', '870101999001',
    'ACTIVE', DATE '1987-01-01', 'Western', 'Colombo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM officer_role
ON CONFLICT DO NOTHING;

INSERT INTO bank_officers (user_id, branch_id, employee_code, created_at, updated_at)
SELECT u.user_id, b.branch_id, 'EMP-DEMO-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
JOIN branches b ON b.branch_code = 'DEMO-001'
WHERE u.username = 'BankOfficer_01'
  AND NOT EXISTS (
      SELECT 1 FROM bank_officers bo WHERE bo.user_id = u.user_id
  );

CREATE TEMP TABLE demo_creditlens_profiles (
    scope VARCHAR(10) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    nic VARCHAR(20) NOT NULL,
    dob DATE NOT NULL,
    province VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    customer_code VARCHAR(50) NOT NULL,
    account_number VARCHAR(30),
    monthly_income NUMERIC(15,2) NOT NULL,
    loan_emi_start NUMERIC(15,2) NOT NULL,
    loan_emi_end NUMERIC(15,2) NOT NULL,
    loan_balance_start NUMERIC(15,2) NOT NULL,
    loan_balance_end NUMERIC(15,2) NOT NULL,
    card_limit NUMERIC(15,2) NOT NULL,
    card_outstanding_start NUMERIC(15,2) NOT NULL,
    card_outstanding_end NUMERIC(15,2) NOT NULL,
    liability_start NUMERIC(15,2) NOT NULL,
    liability_end NUMERIC(15,2) NOT NULL,
    missed_start INTEGER NOT NULL,
    missed_end INTEGER NOT NULL
);

INSERT INTO demo_creditlens_profiles VALUES
    ('PUBLIC', 'PublicCustomer_01', 'publiccustomer01@primecore.local', 'Public', 'Customer 01', '0773000001', '930501300001', DATE '1993-05-01', 'Western', 'Colombo', 'PC-DEMO-12M-001', NULL, 180000, 55000, 30000, 1400000, 640000, 150000, 115000, 25000, 12000, 0, 3, 0),
    ('BANK', 'BankCustomer_01', 'bankcustomer01@primecore.local', 'Bank', 'Customer 01', '0773000101', '910101300101', DATE '1991-01-01', 'Western', 'Nugegoda', 'BC-DEMO-12M-001', '200000000101', 210000, 32000, 24000, 900000, 510000, 220000, 60000, 35000, 0, 0, 0, 0),
    ('BANK', 'BankCustomer_02', 'bankcustomer02@primecore.local', 'Bank', 'Customer 02', '0773000102', '900202300102', DATE '1990-02-02', 'Southern', 'Galle', 'BC-DEMO-12M-002', '200000000102', 160000, 52000, 36000, 1200000, 780000, 180000, 95000, 65000, 8000, 0, 2, 1),
    ('BANK', 'BankCustomer_03', 'bankcustomer03@primecore.local', 'Bank', 'Customer 03', '0773000103', '950303300103', DATE '1995-03-03', 'Central', 'Kandy', 'BC-DEMO-12M-003', '200000000103', 130000, 70000, 56000, 1500000, 1080000, 160000, 140000, 118000, 18000, 12000, 5, 4),
    ('BANK', 'BankCustomer_04', 'bankcustomer04@primecore.local', 'Bank', 'Customer 04', '0773000104', '880404300104', DATE '1988-04-04', 'North Western', 'Kurunegala', 'BC-DEMO-12M-004', '200000000104', 240000, 42000, 28000, 1000000, 620000, 260000, 150000, 52000, 10000, 0, 1, 0),
    ('BANK', 'BankCustomer_05', 'bankcustomer05@primecore.local', 'Bank', 'Customer 05', '0773000105', '960505300105', DATE '1996-05-05', 'Sabaragamuwa', 'Ratnapura', 'BC-DEMO-12M-005', '200000000105', 115000, 44000, 50000, 820000, 900000, 120000, 65000, 105000, 5000, 9000, 1, 3);

CREATE TEMP TABLE demo_creditlens_monthly_seed AS
SELECT
    p.*,
    gs.month_no,
    (date_trunc('month', CURRENT_DATE)::date - ((12 - gs.month_no) * INTERVAL '1 month'))::date AS month_date,
    ROUND(p.loan_emi_start + ((p.loan_emi_end - p.loan_emi_start) * (gs.month_no - 1) / 11.0), 2) AS loan_emi,
    ROUND(p.loan_balance_start + ((p.loan_balance_end - p.loan_balance_start) * (gs.month_no - 1) / 11.0), 2) AS remaining_balance,
    ROUND(p.card_outstanding_start + ((p.card_outstanding_end - p.card_outstanding_start) * (gs.month_no - 1) / 11.0), 2) AS card_outstanding,
    ROUND(p.liability_start + ((p.liability_end - p.liability_start) * (gs.month_no - 1) / 11.0), 2) AS liability_amount,
    GREATEST(0, ROUND(p.missed_start + ((p.missed_end - p.missed_start) * (gs.month_no - 1) / 11.0)))::int AS missed_payments
FROM demo_creditlens_profiles p
CROSS JOIN generate_series(1, 12) AS gs(month_no);

INSERT INTO users (
    role_id, username, email, password_hash, first_name, last_name, phone, nic,
    status, dob, province, address, created_at, updated_at
)
SELECT
    r.role_id, p.username, p.email, 'Demo@1234', p.first_name, p.last_name, p.phone, p.nic,
    'ACTIVE', p.dob, p.province, p.address, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_creditlens_profiles p
JOIN roles r ON r.role_name = CASE WHEN p.scope = 'PUBLIC' THEN 'PUBLIC_CUSTOMER' ELSE 'BANK_CUSTOMER' END
ON CONFLICT DO NOTHING;

INSERT INTO public_customers (user_id, customer_code, created_at, updated_at)
SELECT u.user_id, p.customer_code, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_creditlens_profiles p
JOIN users u ON u.username = p.username
WHERE p.scope = 'PUBLIC'
ON CONFLICT DO NOTHING;

INSERT INTO accounts (account_number, account_type, balance, status, created_at, updated_at)
SELECT p.account_number, 'SAVINGS', 250000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_creditlens_profiles p
WHERE p.scope = 'BANK'
ON CONFLICT (account_number) DO NOTHING;

INSERT INTO bank_customers (user_id, customer_code, officer_id, branch_id, access_status, account_id, created_at, updated_at)
SELECT u.user_id, p.customer_code, bo.officer_id, b.branch_id, 'ACTIVE', a.account_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_creditlens_profiles p
JOIN users u ON u.username = p.username
JOIN bank_officers bo ON bo.employee_code = 'EMP-DEMO-001'
JOIN branches b ON b.branch_code = 'DEMO-001'
JOIN accounts a ON a.account_number = p.account_number
WHERE p.scope = 'BANK'
ON CONFLICT DO NOTHING;

INSERT INTO public_customer_financial_records (public_customer_id, record_status, created_at, updated_at)
SELECT
    pc.public_customer_id,
    CASE WHEN m.month_no = 12 THEN 'CURRENT' ELSE 'ARCHIVED' END,
    m.month_date + TIME '10:00',
    m.month_date + TIME '10:00'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN public_customers pc ON pc.user_id = u.user_id
WHERE m.scope = 'PUBLIC'
  AND NOT EXISTS (
      SELECT 1
      FROM public_customer_financial_records r
      WHERE r.public_customer_id = pc.public_customer_id
        AND DATE(r.created_at) = m.month_date
  );

INSERT INTO bank_customer_financial_records (bank_customer_id, verified_by_officer_id, data_source, created_at, updated_at)
SELECT bc.bank_customer_id, bc.officer_id, 'MANUAL', m.month_date + TIME '10:00', m.month_date + TIME '10:00'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN bank_customers bc ON bc.user_id = u.user_id
WHERE m.scope = 'BANK'
  AND NOT EXISTS (
      SELECT 1
      FROM bank_customer_financial_records r
      WHERE r.bank_customer_id = bc.bank_customer_id
        AND DATE(r.created_at) = m.month_date
  );

INSERT INTO public_customer_incomes (record_id, income_category, amount, salary_type, employment_type, duration_months, created_at)
SELECT r.record_id, 'SALARY', m.monthly_income, 'FIXED_BASIC_SALARY', 'PERMANENT', 12, m.month_date + TIME '10:05'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r ON r.public_customer_id = pc.public_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM public_customer_incomes i WHERE i.record_id = r.record_id);

INSERT INTO bank_customer_incomes (bank_record_id, income_category, amount, salary_type, employment_type, duration_months, created_at)
SELECT r.bank_record_id, 'SALARY', m.monthly_income, 'FIXED_BASIC_SALARY', 'PERMANENT', 12, m.month_date + TIME '10:05'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN bank_customers bc ON bc.user_id = u.user_id
JOIN bank_customer_financial_records r ON r.bank_customer_id = bc.bank_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'BANK'
  AND NOT EXISTS (SELECT 1 FROM bank_customer_incomes i WHERE i.bank_record_id = r.bank_record_id);

INSERT INTO public_customer_loans (record_id, loan_type, monthly_emi, remaining_balance, created_at)
SELECT r.record_id, 'PERSONAL_LOAN', m.loan_emi, m.remaining_balance, m.month_date + TIME '10:10'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r ON r.public_customer_id = pc.public_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM public_customer_loans l WHERE l.record_id = r.record_id);

INSERT INTO bank_customer_loans (bank_record_id, loan_type, monthly_emi, remaining_balance, created_at)
SELECT r.bank_record_id, 'PERSONAL_LOAN', m.loan_emi, m.remaining_balance, m.month_date + TIME '10:10'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN bank_customers bc ON bc.user_id = u.user_id
JOIN bank_customer_financial_records r ON r.bank_customer_id = bc.bank_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'BANK'
  AND NOT EXISTS (SELECT 1 FROM bank_customer_loans l WHERE l.bank_record_id = r.bank_record_id);

INSERT INTO public_customer_cards (record_id, provider, credit_limit, outstanding_balance, created_at)
SELECT r.record_id, 'HNB', m.card_limit, m.card_outstanding, m.month_date + TIME '10:15'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r ON r.public_customer_id = pc.public_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM public_customer_cards c WHERE c.record_id = r.record_id);

INSERT INTO bank_customer_cards (bank_record_id, provider, credit_limit, outstanding_balance, created_at)
SELECT r.bank_record_id, 'Commercial Bank', m.card_limit, m.card_outstanding, m.month_date + TIME '10:15'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN bank_customers bc ON bc.user_id = u.user_id
JOIN bank_customer_financial_records r ON r.bank_customer_id = bc.bank_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'BANK'
  AND NOT EXISTS (SELECT 1 FROM bank_customer_cards c WHERE c.bank_record_id = r.bank_record_id);

INSERT INTO public_customer_liabilities (record_id, description, monthly_amount, created_at)
SELECT r.record_id, 'Other monthly commitments', m.liability_amount, m.month_date + TIME '10:20'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r ON r.public_customer_id = pc.public_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'PUBLIC'
  AND m.liability_amount > 0
  AND NOT EXISTS (SELECT 1 FROM public_customer_liabilities l WHERE l.record_id = r.record_id);

INSERT INTO bank_customer_liabilities (bank_record_id, description, monthly_amount, created_at)
SELECT r.bank_record_id, 'Other monthly commitments', m.liability_amount, m.month_date + TIME '10:20'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN bank_customers bc ON bc.user_id = u.user_id
JOIN bank_customer_financial_records r ON r.bank_customer_id = bc.bank_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'BANK'
  AND m.liability_amount > 0
  AND NOT EXISTS (SELECT 1 FROM bank_customer_liabilities l WHERE l.bank_record_id = r.bank_record_id);

INSERT INTO public_customer_missed_payments (record_id, missed_payments, created_at)
SELECT r.record_id, m.missed_payments, m.month_date + TIME '10:25'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r ON r.public_customer_id = pc.public_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'PUBLIC'
ON CONFLICT (record_id) DO UPDATE SET missed_payments = EXCLUDED.missed_payments;

INSERT INTO bank_customer_missed_payments (bank_record_id, missed_payments, created_at)
SELECT r.bank_record_id, m.missed_payments, m.month_date + TIME '10:25'
FROM demo_creditlens_monthly_seed m
JOIN users u ON u.username = m.username
JOIN bank_customers bc ON bc.user_id = u.user_id
JOIN bank_customer_financial_records r ON r.bank_customer_id = bc.bank_customer_id AND DATE(r.created_at) = m.month_date
WHERE m.scope = 'BANK'
ON CONFLICT (bank_record_id) DO UPDATE SET missed_payments = EXCLUDED.missed_payments;

WITH public_metrics AS (
    SELECT
        pc.public_customer_id,
        r.record_id,
        m.month_date,
        m.monthly_income AS total_monthly_income,
        ROUND(m.loan_emi + m.liability_amount + (m.card_outstanding * 0.05), 2) AS total_monthly_debt_payment,
        m.card_limit AS total_card_limit,
        m.card_outstanding AS total_card_outstanding,
        ROUND((m.loan_emi + m.liability_amount + (m.card_outstanding * 0.05)) / m.monthly_income, 4) AS dti_ratio,
        ROUND(m.card_outstanding / m.card_limit, 4) AS credit_utilization_ratio,
        (2 + CASE WHEN m.liability_amount > 0 THEN 1 ELSE 0 END) AS active_facilities_count,
        m.missed_payments AS missed_payments_count,
        0 AS income_stability_points
    FROM demo_creditlens_monthly_seed m
    JOIN users u ON u.username = m.username
    JOIN public_customers pc ON pc.user_id = u.user_id
    JOIN public_customer_financial_records r ON r.public_customer_id = pc.public_customer_id AND DATE(r.created_at) = m.month_date
    WHERE m.scope = 'PUBLIC'
),
public_scored AS (
    SELECT
        pm.*,
        CASE WHEN missed_payments_count <= 0 THEN 0 WHEN missed_payments_count = 1 THEN 8 WHEN missed_payments_count <= 3 THEN 18 ELSE 30 END AS payment_history_points,
        CASE WHEN dti_ratio <= 0.30 THEN 0 WHEN dti_ratio <= 0.50 THEN 12 ELSE 25 END AS dti_points,
        CASE WHEN credit_utilization_ratio <= 0.40 THEN 0 WHEN credit_utilization_ratio <= 0.70 THEN 10 ELSE 20 END AS utilization_points,
        CASE WHEN active_facilities_count <= 2 THEN 0 WHEN active_facilities_count <= 4 THEN 5 ELSE 10 END AS exposure_points
    FROM public_metrics pm
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
    payment_history_points + dti_points + utilization_points + income_stability_points + exposure_points,
    CASE
        WHEN payment_history_points + dti_points + utilization_points + income_stability_points + exposure_points <= 33 THEN 'LOW'
        WHEN payment_history_points + dti_points + utilization_points + income_stability_points + exposure_points <= 66 THEN 'MEDIUM'
        ELSE 'HIGH'
    END,
    total_monthly_income,
    total_monthly_debt_payment,
    total_card_limit,
    total_card_outstanding,
    dti_ratio,
    credit_utilization_ratio,
    active_facilities_count,
    missed_payments_count,
    payment_history_points,
    dti_points,
    utilization_points,
    income_stability_points,
    exposure_points,
    FALSE,
    month_date + TIME '11:00'
FROM public_scored ps
WHERE NOT EXISTS (
    SELECT 1 FROM self_credit_evaluations e
    WHERE e.public_customer_id = ps.public_customer_id
      AND e.public_record_id = ps.record_id
      AND DATE(e.created_at) = ps.month_date
);

WITH bank_metrics AS (
    SELECT
        bc.bank_customer_id,
        bc.officer_id,
        r.bank_record_id,
        m.month_date,
        m.monthly_income AS total_monthly_income,
        ROUND(m.loan_emi + m.liability_amount + (m.card_outstanding * 0.05), 2) AS total_monthly_debt_payment,
        m.card_limit AS total_card_limit,
        m.card_outstanding AS total_card_outstanding,
        ROUND((m.loan_emi + m.liability_amount + (m.card_outstanding * 0.05)) / m.monthly_income, 4) AS dti_ratio,
        ROUND(m.card_outstanding / m.card_limit, 4) AS credit_utilization_ratio,
        (2 + CASE WHEN m.liability_amount > 0 THEN 1 ELSE 0 END) AS active_facilities_count,
        m.missed_payments AS missed_payments_count,
        0 AS income_stability_points
    FROM demo_creditlens_monthly_seed m
    JOIN users u ON u.username = m.username
    JOIN bank_customers bc ON bc.user_id = u.user_id
    JOIN bank_customer_financial_records r ON r.bank_customer_id = bc.bank_customer_id AND DATE(r.created_at) = m.month_date
    WHERE m.scope = 'BANK'
),
bank_scored AS (
    SELECT
        bm.*,
        CASE WHEN missed_payments_count <= 0 THEN 0 WHEN missed_payments_count = 1 THEN 8 WHEN missed_payments_count <= 3 THEN 18 ELSE 30 END AS payment_history_points,
        CASE WHEN dti_ratio <= 0.30 THEN 0 WHEN dti_ratio <= 0.50 THEN 12 ELSE 25 END AS dti_points,
        CASE WHEN credit_utilization_ratio <= 0.40 THEN 0 WHEN credit_utilization_ratio <= 0.70 THEN 10 ELSE 20 END AS utilization_points,
        CASE WHEN active_facilities_count <= 2 THEN 0 WHEN active_facilities_count <= 4 THEN 5 ELSE 10 END AS exposure_points
    FROM bank_metrics bm
)
INSERT INTO bank_credit_evaluations (
    bank_customer_id, bank_record_id, evaluated_by_officer_id, evaluation_source, remarks,
    total_risk_points, risk_level, total_monthly_income, total_monthly_debt_payment,
    total_card_limit, total_card_outstanding, dti_ratio, credit_utilization_ratio,
    active_facilities_count, missed_payments_count, payment_history_points, dti_points,
    utilization_points, income_stability_points, exposure_points, report_generated, created_at
)
SELECT
    bank_customer_id,
    bank_record_id,
    officer_id,
    'MANUAL',
    '12-month demo evaluation',
    payment_history_points + dti_points + utilization_points + income_stability_points + exposure_points,
    CASE
        WHEN payment_history_points + dti_points + utilization_points + income_stability_points + exposure_points <= 33 THEN 'LOW'
        WHEN payment_history_points + dti_points + utilization_points + income_stability_points + exposure_points <= 66 THEN 'MEDIUM'
        ELSE 'HIGH'
    END,
    total_monthly_income,
    total_monthly_debt_payment,
    total_card_limit,
    total_card_outstanding,
    dti_ratio,
    credit_utilization_ratio,
    active_facilities_count,
    missed_payments_count,
    payment_history_points,
    dti_points,
    utilization_points,
    income_stability_points,
    exposure_points,
    FALSE,
    month_date + TIME '11:00'
FROM bank_scored bs
WHERE NOT EXISTS (
    SELECT 1 FROM bank_credit_evaluations e
    WHERE e.bank_customer_id = bs.bank_customer_id
      AND e.bank_record_id = bs.bank_record_id
      AND DATE(e.created_at) = bs.month_date
);

DROP TABLE IF EXISTS demo_creditlens_monthly_seed;
DROP TABLE IF EXISTS demo_creditlens_profiles;

COMMIT;

-- Demo logins:
-- public customer: PublicCustomer_01 / Demo@1234
-- bank customers: BankCustomer_01, BankCustomer_02, BankCustomer_03, BankCustomer_04, BankCustomer_05 / Demo@1234
-- officer: BankOfficer_01 / Demo@1234

