INSERT INTO roles (role_name, description, created_at)
VALUES
    ('ADMIN', 'System administrator', NOW()),
    ('BANK_OFFICER', 'Bank officer user', NOW()),
    ('BANK_CUSTOMER', 'Bank customer user', NOW()),
    ('PUBLIC_CUSTOMER', 'Public customer user', NOW())
ON CONFLICT (role_name) DO NOTHING;

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
    'W',
    'C',
    NOW(),
    NOW()
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
    address,
    created_at,
    updated_at
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
    'W',
    'K',
    NOW(),
    NOW()
FROM roles r
WHERE r.role_name = 'BANK_OFFICER'
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
    address,
    created_at,
    updated_at
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
    'S',
    'G',
    NOW(),
    NOW()
FROM roles r
WHERE r.role_name = 'BANK_CUSTOMER'
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
    address,
    created_at,
    updated_at
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
    'N',
    'J',
    NOW(),
    NOW()
FROM roles r
WHERE r.role_name = 'PUBLIC_CUSTOMER'
ON CONFLICT (email) DO NOTHING;
