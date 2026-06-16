-- SpendIQ demo data for the public customer test account.
-- Account: public.customer.demo@primecore.local

WITH demo_user AS (
    SELECT user_id
    FROM users
    WHERE email = 'public.customer.demo@primecore.local'
)
INSERT INTO expense_categories (user_id, category_name, category_type, created_at)
SELECT demo_user.user_id, seed.category_name, seed.category_type, CURRENT_TIMESTAMP
FROM demo_user
CROSS JOIN (
    VALUES
        ('Food', 'VARIABLE'),
        ('Transport', 'VARIABLE'),
        ('Bills', 'FIXED'),
        ('Shopping', 'VARIABLE'),
        ('Health', 'VARIABLE'),
        ('Education', 'FIXED'),
        ('Entertainment', 'VARIABLE'),
        ('Savings', 'FIXED'),
        ('Rent', 'FIXED'),
        ('Utilities', 'FIXED')
) AS seed(category_name, category_type)
WHERE NOT EXISTS (
    SELECT 1
    FROM expense_categories existing
    WHERE existing.user_id = demo_user.user_id
      AND LOWER(existing.category_name) = LOWER(seed.category_name)
);

WITH demo_user AS (
    SELECT user_id
    FROM users
    WHERE email = 'public.customer.demo@primecore.local'
),
seed_income AS (
    SELECT *
    FROM (
        VALUES
            ('Salary', 45000.00, DATE '2026-02-25'),
            ('Freelance Design', 8000.00, DATE '2026-02-28'),
            ('Salary', 45000.00, DATE '2026-03-25'),
            ('Freelance Design', 10000.00, DATE '2026-03-29'),
            ('Salary', 45000.00, DATE '2026-04-25'),
            ('Freelance Design', 12000.00, DATE '2026-04-29'),
            ('Salary', 45000.00, DATE '2026-05-02')
    ) AS rows(source_name, amount, income_date)
)
INSERT INTO income_records (user_id, source_name, amount, income_date, created_at)
SELECT demo_user.user_id, seed_income.source_name, seed_income.amount, seed_income.income_date, CURRENT_TIMESTAMP
FROM demo_user
CROSS JOIN seed_income
WHERE NOT EXISTS (
    SELECT 1
    FROM income_records existing
    WHERE existing.user_id = demo_user.user_id
      AND existing.source_name = seed_income.source_name
      AND existing.amount = seed_income.amount
      AND existing.income_date = seed_income.income_date
);

WITH demo_user AS (
    SELECT user_id
    FROM users
    WHERE email = 'public.customer.demo@primecore.local'
),
seed_expenses AS (
    SELECT *
    FROM (
        VALUES
            ('Rent', 18000.00, DATE '2026-02-01', 'BANK_TRANSFER', 'PUBLIC_DEMO_RENT_2026_02'),
            ('Bills', 4200.00, DATE '2026-02-05', 'BANK_TRANSFER', 'PUBLIC_DEMO_BILLS_2026_02'),
            ('Food', 7500.00, DATE '2026-02-08', 'CASH', 'PUBLIC_DEMO_FOOD_2026_02_A'),
            ('Transport', 3600.00, DATE '2026-02-12', 'CASH', 'PUBLIC_DEMO_TRANSPORT_2026_02'),
            ('Entertainment', 2800.00, DATE '2026-02-18', 'CARD', 'PUBLIC_DEMO_ENTERTAINMENT_2026_02'),
            ('Savings', 6000.00, DATE '2026-02-26', 'BANK_TRANSFER', 'PUBLIC_DEMO_SAVINGS_2026_02'),
            ('Rent', 18000.00, DATE '2026-03-01', 'BANK_TRANSFER', 'PUBLIC_DEMO_RENT_2026_03'),
            ('Utilities', 5100.00, DATE '2026-03-04', 'BANK_TRANSFER', 'PUBLIC_DEMO_UTILITIES_2026_03'),
            ('Food', 9200.00, DATE '2026-03-08', 'CASH', 'PUBLIC_DEMO_FOOD_2026_03_A'),
            ('Transport', 4100.00, DATE '2026-03-13', 'CASH', 'PUBLIC_DEMO_TRANSPORT_2026_03'),
            ('Shopping', 6500.00, DATE '2026-03-18', 'CARD', 'PUBLIC_DEMO_SHOPPING_2026_03'),
            ('Health', 3200.00, DATE '2026-03-21', 'CARD', 'PUBLIC_DEMO_HEALTH_2026_03'),
            ('Savings', 7000.00, DATE '2026-03-27', 'BANK_TRANSFER', 'PUBLIC_DEMO_SAVINGS_2026_03'),
            ('Rent', 18000.00, DATE '2026-04-01', 'BANK_TRANSFER', 'PUBLIC_DEMO_RENT_2026_04'),
            ('Utilities', 5400.00, DATE '2026-04-04', 'BANK_TRANSFER', 'PUBLIC_DEMO_UTILITIES_2026_04'),
            ('Food', 9800.00, DATE '2026-04-08', 'CASH', 'PUBLIC_DEMO_FOOD_2026_04_A'),
            ('Transport', 4300.00, DATE '2026-04-12', 'CASH', 'PUBLIC_DEMO_TRANSPORT_2026_04'),
            ('Entertainment', 4200.00, DATE '2026-04-16', 'CARD', 'PUBLIC_DEMO_ENTERTAINMENT_2026_04'),
            ('Education', 8500.00, DATE '2026-04-20', 'BANK_TRANSFER', 'PUBLIC_DEMO_EDUCATION_2026_04'),
            ('Shopping', 7200.00, DATE '2026-04-23', 'CARD', 'PUBLIC_DEMO_SHOPPING_2026_04'),
            ('Savings', 5000.00, DATE '2026-04-26', 'BANK_TRANSFER', 'PUBLIC_DEMO_SAVINGS_2026_04'),
            ('Rent', 18000.00, DATE '2026-05-01', 'BANK_TRANSFER', 'PUBLIC_DEMO_RENT_2026_05'),
            ('Health', 3000.00, DATE '2026-05-02', 'CASH', 'PUBLIC_DEMO_HEALTH_2026_05'),
            ('Entertainment', 200.00, DATE '2026-05-02', 'BANK_TRANSFER', 'PUBLIC_DEMO_ENTERTAINMENT_2026_05'),
            ('Food', 3600.00, DATE '2026-05-03', 'CASH', 'PUBLIC_DEMO_FOOD_2026_05_A'),
            ('Transport', 1200.00, DATE '2026-05-03', 'CASH', 'PUBLIC_DEMO_TRANSPORT_2026_05'),
            ('Education', 4500.00, DATE '2026-05-03', 'BANK_TRANSFER', 'PUBLIC_DEMO_EDUCATION_2026_05')
    ) AS rows(category_name, amount, expense_date, payment_type, tracking_reference)
)
INSERT INTO expense_records (user_id, category_id, amount, expense_date, payment_type, tracking_source, tracking_reference, created_at)
SELECT demo_user.user_id, category.category_id, seed_expenses.amount, seed_expenses.expense_date, seed_expenses.payment_type, 'DEMO_SEED', seed_expenses.tracking_reference, CURRENT_TIMESTAMP
FROM demo_user
JOIN seed_expenses ON TRUE
JOIN expense_categories category
  ON category.user_id = demo_user.user_id
 AND LOWER(category.category_name) = LOWER(seed_expenses.category_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM expense_records existing
    WHERE existing.tracking_source = 'DEMO_SEED'
      AND existing.tracking_reference = seed_expenses.tracking_reference
);

WITH demo_user AS (
    SELECT user_id
    FROM users
    WHERE email = 'public.customer.demo@primecore.local'
),
seed_budgets AS (
    SELECT *
    FROM (
        VALUES
            ('Rent', 18000.00, 2, 2026),
            ('Bills', 5500.00, 2, 2026),
            ('Food', 10000.00, 2, 2026),
            ('Transport', 4500.00, 2, 2026),
            ('Entertainment', 3500.00, 2, 2026),
            ('Savings', 6000.00, 2, 2026),
            ('Rent', 18000.00, 3, 2026),
            ('Utilities', 5500.00, 3, 2026),
            ('Food', 10000.00, 3, 2026),
            ('Transport', 4500.00, 3, 2026),
            ('Shopping', 6000.00, 3, 2026),
            ('Health', 3500.00, 3, 2026),
            ('Savings', 7000.00, 3, 2026),
            ('Rent', 18000.00, 4, 2026),
            ('Utilities', 5500.00, 4, 2026),
            ('Food', 10000.00, 4, 2026),
            ('Transport', 4500.00, 4, 2026),
            ('Entertainment', 4000.00, 4, 2026),
            ('Education', 9000.00, 4, 2026),
            ('Shopping', 6500.00, 4, 2026),
            ('Savings', 6000.00, 4, 2026),
            ('Rent', 18000.00, 5, 2026),
            ('Health', 4000.00, 5, 2026),
            ('Entertainment', 2500.00, 5, 2026),
            ('Food', 9000.00, 5, 2026),
            ('Transport', 4500.00, 5, 2026),
            ('Education', 5000.00, 5, 2026)
    ) AS rows(category_name, budget_amount, month, year)
)
UPDATE budget_limits existing
SET
    budget_amount = seed_budgets.budget_amount,
    updated_at = CURRENT_TIMESTAMP
FROM demo_user
JOIN seed_budgets ON TRUE
JOIN expense_categories category
  ON category.user_id = demo_user.user_id
 AND LOWER(category.category_name) = LOWER(seed_budgets.category_name)
WHERE existing.user_id = demo_user.user_id
  AND existing.category_id = category.category_id
  AND existing.month = seed_budgets.month
  AND existing.year = seed_budgets.year;

WITH demo_user AS (
    SELECT user_id
    FROM users
    WHERE email = 'public.customer.demo@primecore.local'
),
seed_budgets AS (
    SELECT *
    FROM (
        VALUES
            ('Rent', 18000.00, 2, 2026),
            ('Bills', 5500.00, 2, 2026),
            ('Food', 10000.00, 2, 2026),
            ('Transport', 4500.00, 2, 2026),
            ('Entertainment', 3500.00, 2, 2026),
            ('Savings', 6000.00, 2, 2026),
            ('Rent', 18000.00, 3, 2026),
            ('Utilities', 5500.00, 3, 2026),
            ('Food', 10000.00, 3, 2026),
            ('Transport', 4500.00, 3, 2026),
            ('Shopping', 6000.00, 3, 2026),
            ('Health', 3500.00, 3, 2026),
            ('Savings', 7000.00, 3, 2026),
            ('Rent', 18000.00, 4, 2026),
            ('Utilities', 5500.00, 4, 2026),
            ('Food', 10000.00, 4, 2026),
            ('Transport', 4500.00, 4, 2026),
            ('Entertainment', 4000.00, 4, 2026),
            ('Education', 9000.00, 4, 2026),
            ('Shopping', 6500.00, 4, 2026),
            ('Savings', 6000.00, 4, 2026),
            ('Rent', 18000.00, 5, 2026),
            ('Health', 4000.00, 5, 2026),
            ('Entertainment', 2500.00, 5, 2026),
            ('Food', 9000.00, 5, 2026),
            ('Transport', 4500.00, 5, 2026),
            ('Education', 5000.00, 5, 2026)
    ) AS rows(category_name, budget_amount, month, year)
)
INSERT INTO budget_limits (user_id, category_id, budget_amount, month, year, created_at, updated_at)
SELECT demo_user.user_id, category.category_id, seed_budgets.budget_amount, seed_budgets.month, seed_budgets.year, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_user
JOIN seed_budgets ON TRUE
JOIN expense_categories category
  ON category.user_id = demo_user.user_id
 AND LOWER(category.category_name) = LOWER(seed_budgets.category_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM budget_limits existing
    WHERE existing.user_id = demo_user.user_id
      AND existing.category_id = category.category_id
      AND existing.month = seed_budgets.month
      AND existing.year = seed_budgets.year
);
