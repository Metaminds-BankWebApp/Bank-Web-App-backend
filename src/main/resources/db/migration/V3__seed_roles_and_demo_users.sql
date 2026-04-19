INSERT INTO roles (role_name, description, created_at)
VALUES
    ('ADMIN', 'System administrator', CURRENT_TIMESTAMP),
    ('BANK_OFFICER', 'Bank officer user', CURRENT_TIMESTAMP),
    ('BANK_CUSTOMER', 'Bank customer user', CURRENT_TIMESTAMP),
    ('PUBLIC_CUSTOMER', 'Public customer user', CURRENT_TIMESTAMP)
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO branches (branch_code, branch_name, branch_email, branch_phone, address, status, created_at, updated_at)
VALUES
    ('COL-001', 'Colombo Main', 'colombo.main@primecore.local', '0112000001', 'No 1, Main Street, Colombo', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (branch_code) DO NOTHING;

WITH admin_role AS (
    SELECT role_id
    FROM roles
    WHERE role_name = 'ADMIN'
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
    admin_role.role_id,
    'admin.demo',
    'admin.demo@primecore.local',
    'Demo@1234',
    'Admin',
    'Demo',
    '0771000001',
    '199012345678',
    'ACTIVE',
    DATE '1990-01-10',
    'Western',
    'Colombo',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM admin_role
ON CONFLICT DO NOTHING;

WITH officer_role AS (
    SELECT role_id
    FROM roles
    WHERE role_name = 'BANK_OFFICER'
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
    officer_role.role_id,
    'officer.demo',
    'officer.demo@primecore.local',
    'Demo@1234',
    'Officer',
    'Demo',
    '0771000002',
    '199112345678',
    'ACTIVE',
    DATE '1991-02-11',
    'Western',
    'Kandy',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM officer_role
ON CONFLICT DO NOTHING;

INSERT INTO bank_officers (user_id, branch_id, employee_code, created_by_admin_user_id, created_at, updated_at)
SELECT
    officer_user.user_id,
    branch.branch_id,
    'EMP-BO-00001',
    admin_user.user_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users officer_user
JOIN roles officer_role ON officer_role.role_id = officer_user.role_id
JOIN users admin_user ON admin_user.username = 'admin.demo'
JOIN branches branch ON branch.branch_code = 'COL-001'
WHERE officer_role.role_name = 'BANK_OFFICER'
  AND officer_user.username = 'officer.demo'
  AND NOT EXISTS (
      SELECT 1
      FROM bank_officers existing_officer
      WHERE existing_officer.user_id = officer_user.user_id
  );

WITH public_customer_role AS (
    SELECT role_id
    FROM roles
    WHERE role_name = 'PUBLIC_CUSTOMER'
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
    public_customer_role.role_id,
    'public.customer.demo',
    'public.customer.demo@primecore.local',
    'Demo@1234',
    'Public',
    'Customer',
    '0771000004',
    '199312345678',
    'ACTIVE',
    DATE '1993-04-13',
    'Northern',
    'Jaffna',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM public_customer_role
ON CONFLICT DO NOTHING;

INSERT INTO public_customers (user_id, customer_code, created_at, updated_at)
SELECT
    public_customer.user_id,
    'PC-00001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users public_customer
WHERE public_customer.username = 'public.customer.demo'
  AND NOT EXISTS (
      SELECT 1
      FROM public_customers existing_public_customer
      WHERE existing_public_customer.user_id = public_customer.user_id
  );

WITH bank_customer_role AS (
    SELECT role_id
    FROM roles
    WHERE role_name = 'BANK_CUSTOMER'
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
    bank_customer_role.role_id,
    'bank.customer.demo',
    'bank.customer.demo@primecore.local',
    'Demo@1234',
    'Bank',
    'Customer',
    '0771000003',
    '199212345678',
    'ACTIVE',
    DATE '1992-03-12',
    'Southern',
    'Galle',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM bank_customer_role
ON CONFLICT DO NOTHING;

INSERT INTO accounts (account_number, account_type, balance, status, created_at, updated_at)
SELECT
    '100000000001',
    'SAVINGS',
    250000.00,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM accounts existing_account
    WHERE existing_account.account_number = '100000000001'
);

INSERT INTO bank_customers (user_id, customer_code, officer_id, branch_id, access_status, account_id, created_at, updated_at)
SELECT
    customer_user.user_id,
    'BC-00001',
    officer.officer_id,
    branch.branch_id,
    'ACTIVE',
    account.account_id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users customer_user
JOIN roles customer_role ON customer_role.role_id = customer_user.role_id
JOIN bank_officers officer ON officer.employee_code = 'EMP-BO-00001'
JOIN branches branch ON branch.branch_code = 'COL-001'
JOIN accounts account ON account.account_number = '100000000001'
WHERE customer_role.role_name = 'BANK_CUSTOMER'
  AND customer_user.username = 'bank.customer.demo'
  AND NOT EXISTS (
      SELECT 1
      FROM bank_customers existing_bank_customer
      WHERE existing_bank_customer.user_id = customer_user.user_id
  );
