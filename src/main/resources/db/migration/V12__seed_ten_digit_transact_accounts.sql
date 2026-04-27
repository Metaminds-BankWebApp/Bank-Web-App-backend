INSERT INTO accounts (account_number, account_type, balance, status, created_at, updated_at)
VALUES
    ('2003004005', 'SAVINGS', 15000.00, 'ACTIVE', NOW(), NOW()),
    ('3004005006', 'CURRENT', 22000.00, 'ACTIVE', NOW(), NOW()),
    ('4005006007', 'SAVINGS', 18000.00, 'ACTIVE', NOW(), NOW())
ON CONFLICT (account_number) DO NOTHING;
