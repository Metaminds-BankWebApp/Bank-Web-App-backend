INSERT INTO roles (role_name, description)
VALUES
    ('ADMIN', 'System administrator'),
    ('BANK_OFFICER', 'Bank officer user'),
    ('BANK_CUSTOMER', 'Bank customer user'),
    ('PUBLIC_CUSTOMER', 'Public customer user')
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO branches (branch_code, branch_name, branch_email, branch_phone, address, status)
VALUES
    ('COL-001', 'Colombo Main', 'colombo.main@primecore.local', '0112000001', 'No 1, Main Street, Colombo', 'ACTIVE')
ON CONFLICT (branch_code) DO NOTHING;

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
    address
)
SELECT
    r.role_id,
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
    'Colombo'
FROM roles r
WHERE r.role_name = 'ADMIN'
ON CONFLICT (email) DO NOTHING;

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
    address
)
SELECT
    r.role_id,
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
    'Kandy'
FROM roles r
WHERE r.role_name = 'BANK_OFFICER'
ON CONFLICT (email) DO NOTHING;

INSERT INTO bank_officers (user_id, branch_id, employee_code, created_by_admin_user_id)
SELECT
    officer_user.user_id,
    b.branch_id,
    'EMP-BO-00001',
    admin_user.user_id
FROM users officer_user
JOIN roles officer_role ON officer_role.role_id = officer_user.role_id
JOIN users admin_user ON admin_user.username = 'admin.demo'
JOIN branches b ON b.branch_code = 'COL-001'
WHERE officer_role.role_name = 'BANK_OFFICER'
  AND officer_user.username = 'officer.demo'
  AND NOT EXISTS (
      SELECT 1 FROM bank_officers bo WHERE bo.user_id = officer_user.user_id
  );

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
    address
)
SELECT
    r.role_id,
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
    'Jaffna'
FROM roles r
WHERE r.role_name = 'PUBLIC_CUSTOMER'
ON CONFLICT (email) DO NOTHING;

INSERT INTO public_customers (user_id, customer_code)
SELECT
    u.user_id,
    'PC-00001'
FROM users u
WHERE u.username = 'public.customer.demo'
  AND NOT EXISTS (
      SELECT 1 FROM public_customers pc WHERE pc.user_id = u.user_id
  );

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
    address
)
SELECT
    r.role_id,
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
    'Galle'
FROM roles r
WHERE r.role_name = 'BANK_CUSTOMER'
ON CONFLICT (email) DO NOTHING;

INSERT INTO accounts (account_number, account_type, balance, status)
SELECT
    '100000000001',
    'SAVINGS',
    250000.00,
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM accounts WHERE account_number = '100000000001'
);

INSERT INTO bank_customers (user_id, customer_code, officer_id, branch_id, access_status, account_id)
SELECT
    customer_user.user_id,
    'BC-00001',
    officer.officer_id,
    b.branch_id,
    'ACTIVE',
    a.account_id
FROM users customer_user
JOIN roles customer_role ON customer_role.role_id = customer_user.role_id
JOIN bank_officers officer ON officer.employee_code = 'EMP-BO-00001'
JOIN branches b ON b.branch_code = 'COL-001'
JOIN accounts a ON a.account_number = '100000000001'
WHERE customer_role.role_name = 'BANK_CUSTOMER'
  AND customer_user.username = 'bank.customer.demo'
  AND NOT EXISTS (
      SELECT 1 FROM bank_customers bc WHERE bc.user_id = customer_user.user_id
  );
