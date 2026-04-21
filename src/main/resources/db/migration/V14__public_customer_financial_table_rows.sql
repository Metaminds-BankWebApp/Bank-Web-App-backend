-- Seed 10 public customers for CreditLens calculation testing
-- Password for all seeded users: Demo@1234

-- 1) users
WITH public_role AS (
    SELECT role_id
    FROM roles
    WHERE role_name = 'PUBLIC_CUSTOMER'
),
seed_users(username, email, first_name, last_name, phone, nic, dob, province, address, customer_code) AS (
    VALUES
        ('pc.calc01', 'pc.calc01@primecore.local', 'Ayesha', 'Perera', '0779100001', '900101000001', DATE '1990-01-15', 'Western', 'Colombo 05', 'PC-CALC-00001'),
        ('pc.calc02', 'pc.calc02@primecore.local', 'Nimal', 'Fernando', '0779100002', '900101000002', DATE '1989-03-10', 'Southern', 'Galle', 'PC-CALC-00002'),
        ('pc.calc03', 'pc.calc03@primecore.local', 'Kavindu', 'Silva', '0779100003', '900101000003', DATE '1994-07-21', 'Central', 'Kandy', 'PC-CALC-00003'),
        ('pc.calc04', 'pc.calc04@primecore.local', 'Tharushi', 'Jayasena', '0779100004', '900101000004', DATE '1993-11-09', 'North Western', 'Kurunegala', 'PC-CALC-00004'),
        ('pc.calc05', 'pc.calc05@primecore.local', 'Sanduni', 'Wickramasinghe', '0779100005', '900101000005', DATE '1992-05-28', 'Western', 'Negombo', 'PC-CALC-00005'),
        ('pc.calc06', 'pc.calc06@primecore.local', 'Imesha', 'Ranatunga', '0779100006', '900101000006', DATE '1991-08-18', 'Sabaragamuwa', 'Ratnapura', 'PC-CALC-00006'),
        ('pc.calc07', 'pc.calc07@primecore.local', 'Pasan', 'Senanayake', '0779100007', '900101000007', DATE '1988-12-14', 'Western', 'Maharagama', 'PC-CALC-00007'),
        ('pc.calc08', 'pc.calc08@primecore.local', 'Dilini', 'Gunasekara', '0779100008', '900101000008', DATE '1995-02-06', 'Uva', 'Badulla', 'PC-CALC-00008'),
        ('pc.calc09', 'pc.calc09@primecore.local', 'Shehan', 'Abeywickrama', '0779100009', '900101000009', DATE '1990-10-30', 'Western', 'Panadura', 'PC-CALC-00009'),
        ('pc.calc10', 'pc.calc10@primecore.local', 'Janani', 'Weerasinghe', '0779100010', '900101000010', DATE '1996-04-12', 'Eastern', 'Batticaloa', 'PC-CALC-00010')
)
INSERT INTO users (
    role_id,
    username,
    email,
    password_hash,
    first_name,
    last_name,
    phone,
    nic,
    status,
    dob,
    province,
    address,
    created_at,
    updated_at
)
SELECT
    pr.role_id,
    su.username,
    su.email,
    'Demo@1234',
    su.first_name,
    su.last_name,
    su.phone,
    su.nic,
    'ACTIVE',
    su.dob,
    su.province,
    su.address,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_users su
CROSS JOIN public_role pr
ON CONFLICT (username) DO NOTHING;

-- 2) public_customers
WITH seed_profiles(username, customer_code) AS (
    VALUES
        ('pc.calc01', 'PC-CALC-00001'),
        ('pc.calc02', 'PC-CALC-00002'),
        ('pc.calc03', 'PC-CALC-00003'),
        ('pc.calc04', 'PC-CALC-00004'),
        ('pc.calc05', 'PC-CALC-00005'),
        ('pc.calc06', 'PC-CALC-00006'),
        ('pc.calc07', 'PC-CALC-00007'),
        ('pc.calc08', 'PC-CALC-00008'),
        ('pc.calc09', 'PC-CALC-00009'),
        ('pc.calc10', 'PC-CALC-00010')
)
INSERT INTO public_customers (
    user_id,
    customer_code,
    created_at,
    updated_at
)
SELECT
    u.user_id,
    sp.customer_code,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_profiles sp
JOIN users u ON u.username = sp.username
WHERE NOT EXISTS (
    SELECT 1
    FROM public_customers pc
    WHERE pc.user_id = u.user_id
);

-- 3) current financial records
WITH seed_usernames(username) AS (
    VALUES
        ('pc.calc01'),
        ('pc.calc02'),
        ('pc.calc03'),
        ('pc.calc04'),
        ('pc.calc05'),
        ('pc.calc06'),
        ('pc.calc07'),
        ('pc.calc08'),
        ('pc.calc09'),
        ('pc.calc10')
)
INSERT INTO public_customer_financial_records (
    public_customer_id,
    record_status,
    created_at,
    updated_at
)
SELECT
    pc.public_customer_id,
    'CURRENT',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_usernames s
JOIN users u ON u.username = s.username
JOIN public_customers pc ON pc.user_id = u.user_id
WHERE NOT EXISTS (
    SELECT 1
    FROM public_customer_financial_records r
    WHERE r.public_customer_id = pc.public_customer_id
      AND r.record_status = 'CURRENT'
);

-- 4) incomes
WITH income_seed(username, income_category, amount, salary_type, employment_type, duration_months, income_stability) AS (
    VALUES
        ('pc.calc01', 'SALARY',   160000.00, 'FIXED_BASIC_SALARY', 'PERMANENT', NULL, NULL),
        ('pc.calc02', 'SALARY',   100000.00, 'FIXED_BASIC_SALARY', 'PERMANENT', NULL, NULL),
        ('pc.calc02', 'BUSINESS',  50000.00, NULL,                 NULL,        NULL, 'STABLE'),
        ('pc.calc03', 'SALARY',   100000.00, 'FIXED_BASIC_SALARY', 'CONTRACT',  10,   NULL),
        ('pc.calc04', 'BUSINESS', 140000.00, NULL,                 NULL,        NULL, 'MEDIUM_FLUCTUATION'),
        ('pc.calc05', 'SALARY',   120000.00, 'FIXED_BASIC_SALARY', 'PERMANENT', NULL, NULL),
        ('pc.calc06', 'SALARY',    90000.00, 'FIXED_BASIC_SALARY', 'CONTRACT',   4,   NULL),
        ('pc.calc07', 'BUSINESS', 110000.00, NULL,                 NULL,        NULL, 'HIGH_FLUCTUATION'),
        ('pc.calc08', 'SALARY',    80000.00, 'FIXED_BASIC_SALARY', 'FREELANCE', NULL, NULL),
        ('pc.calc09', 'SALARY',   130000.00, 'FIXED_BASIC_SALARY', 'PERMANENT', NULL, NULL),
        ('pc.calc10', 'BUSINESS',  95000.00, NULL,                 NULL,        NULL, 'MEDIUM_FLUCTUATION')
)
INSERT INTO public_customer_incomes (
    record_id,
    income_category,
    amount,
    salary_type,
    employment_type,
    duration_months,
    income_stability,
    created_at
)
SELECT
    r.record_id,
    i.income_category,
    i.amount,
    i.salary_type,
    i.employment_type,
    i.duration_months,
    i.income_stability,
    CURRENT_TIMESTAMP
FROM income_seed i
JOIN users u ON u.username = i.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r
    ON r.public_customer_id = pc.public_customer_id
   AND r.record_status = 'CURRENT'
WHERE NOT EXISTS (
    SELECT 1
    FROM public_customer_incomes existing
    WHERE existing.record_id = r.record_id
      AND existing.income_category = i.income_category
      AND existing.amount = i.amount
      AND COALESCE(existing.salary_type, '') = COALESCE(i.salary_type, '')
      AND COALESCE(existing.employment_type, '') = COALESCE(i.employment_type, '')
      AND COALESCE(existing.duration_months, -1) = COALESCE(i.duration_months, -1)
      AND COALESCE(existing.income_stability, '') = COALESCE(i.income_stability, '')
);

-- 5) loans
WITH loan_seed(username, loan_type, monthly_emi, remaining_balance) AS (
    VALUES
        ('pc.calc01', 'PERSONAL_LOAN', 18000.00, 400000.00),
        ('pc.calc02', 'VEHICLE_LOAN',  20000.00, 350000.00),
        ('pc.calc03', 'PERSONAL_LOAN', 25000.00, 600000.00),
        ('pc.calc04', 'HOME_LOAN',     28000.00, 550000.00),
        ('pc.calc04', 'PERSONAL_LOAN',  9000.00, 140000.00),
        ('pc.calc05', 'VEHICLE_LOAN',  30000.00, 500000.00),
        ('pc.calc06', 'HOME_LOAN',     30000.00, 700000.00),
        ('pc.calc06', 'PERSONAL_LOAN', 15000.00, 250000.00),
        ('pc.calc07', 'HOME_LOAN',     20000.00, 450000.00),
        ('pc.calc07', 'PERSONAL_LOAN', 18000.00, 320000.00),
        ('pc.calc08', 'VEHICLE_LOAN',  18000.00, 260000.00),
        ('pc.calc08', 'PERSONAL_LOAN', 12000.00, 180000.00),
        ('pc.calc10', 'HOME_LOAN',     25000.00, 390000.00),
        ('pc.calc10', 'PERSONAL_LOAN',  7000.00, 120000.00)
)
INSERT INTO public_customer_loans (
    record_id,
    loan_type,
    monthly_emi,
    remaining_balance,
    created_at
)
SELECT
    r.record_id,
    l.loan_type,
    l.monthly_emi,
    l.remaining_balance,
    CURRENT_TIMESTAMP
FROM loan_seed l
JOIN users u ON u.username = l.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r
    ON r.public_customer_id = pc.public_customer_id
   AND r.record_status = 'CURRENT'
WHERE NOT EXISTS (
    SELECT 1
    FROM public_customer_loans existing
    WHERE existing.record_id = r.record_id
      AND existing.loan_type = l.loan_type
      AND existing.monthly_emi = l.monthly_emi
      AND existing.remaining_balance = l.remaining_balance
);

-- 6) credit cards
WITH card_seed(username, provider, credit_limit, outstanding_balance) AS (
    VALUES
        ('pc.calc01', 'HNB',       120000.00, 15000.00),
        ('pc.calc02', 'Commercial',100000.00, 30000.00),
        ('pc.calc03', 'Sampath',   100000.00, 45000.00),
        ('pc.calc03', 'Amex',       30000.00, 20000.00),
        ('pc.calc04', 'NDB',        80000.00, 20000.00),
        ('pc.calc05', 'HSBC',       90000.00, 65000.00),
        ('pc.calc06', 'Sampath',    80000.00, 70000.00),
        ('pc.calc06', 'Amex',       25000.00, 20000.00),
        ('pc.calc07', 'Commercial', 70000.00, 55000.00),
        ('pc.calc07', 'DFCC',       20000.00, 18000.00),
        ('pc.calc08', 'BOC',        60000.00, 40000.00),
        ('pc.calc09', 'HNB',       150000.00, 50000.00),
        ('pc.calc10', 'Seylan',     50000.00, 35000.00)
)
INSERT INTO public_customer_cards (
    record_id,
    provider,
    credit_limit,
    outstanding_balance,
    created_at
)
SELECT
    r.record_id,
    c.provider,
    c.credit_limit,
    c.outstanding_balance,
    CURRENT_TIMESTAMP
FROM card_seed c
JOIN users u ON u.username = c.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r
    ON r.public_customer_id = pc.public_customer_id
   AND r.record_status = 'CURRENT'
WHERE NOT EXISTS (
    SELECT 1
    FROM public_customer_cards existing
    WHERE existing.record_id = r.record_id
      AND COALESCE(existing.provider, '') = COALESCE(c.provider, '')
      AND existing.credit_limit = c.credit_limit
      AND existing.outstanding_balance = c.outstanding_balance
);

-- 7) liabilities
WITH liability_seed(username, description, monthly_amount) AS (
    VALUES
        ('pc.calc03', 'Lease Rental',        8000.00),
        ('pc.calc04', 'Family Support',      6000.00),
        ('pc.calc05', 'Other Liabilities',  10000.00),
        ('pc.calc06', 'Lease Rental',       12000.00),
        ('pc.calc07', 'Business Commitments',9000.00),
        ('pc.calc08', 'Hire Purchase',      12000.00),
        ('pc.calc10', 'Other Liabilities',   6000.00)
)
INSERT INTO public_customer_liabilities (
    record_id,
    description,
    monthly_amount,
    created_at
)
SELECT
    r.record_id,
    l.description,
    l.monthly_amount,
    CURRENT_TIMESTAMP
FROM liability_seed l
JOIN users u ON u.username = l.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r
    ON r.public_customer_id = pc.public_customer_id
   AND r.record_status = 'CURRENT'
WHERE NOT EXISTS (
    SELECT 1
    FROM public_customer_liabilities existing
    WHERE existing.record_id = r.record_id
      AND existing.description = l.description
      AND existing.monthly_amount = l.monthly_amount
);

-- 8) missed payments
WITH missed_seed(username, missed_payments) AS (
    VALUES
        ('pc.calc01', 0),
        ('pc.calc02', 0),
        ('pc.calc03', 1),
        ('pc.calc04', 2),
        ('pc.calc05', 1),
        ('pc.calc06', 4),
        ('pc.calc07', 3),
        ('pc.calc08', 4),
        ('pc.calc09', 0),
        ('pc.calc10', 1)
)
INSERT INTO public_customer_missed_payments (
    record_id,
    missed_payments,
    created_at
)
SELECT
    r.record_id,
    m.missed_payments,
    CURRENT_TIMESTAMP
FROM missed_seed m
JOIN users u ON u.username = m.username
JOIN public_customers pc ON pc.user_id = u.user_id
JOIN public_customer_financial_records r
    ON r.public_customer_id = pc.public_customer_id
   AND r.record_status = 'CURRENT'
ON CONFLICT (record_id) DO UPDATE
SET missed_payments = EXCLUDED.missed_payments;
