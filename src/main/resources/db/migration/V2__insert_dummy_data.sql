INSERT INTO roles (role_name, description, created_at)
SELECT 'ADMIN', 'System administrators with full access.', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE role_name = 'ADMIN'
);

INSERT INTO roles (role_name, description, created_at)
SELECT 'BANK_OFFICER', 'Bank staff users who manage customer-facing operations.', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE role_name = 'BANK_OFFICER'
);

INSERT INTO roles (role_name, description, created_at)
SELECT 'BANK_CUSTOMER', 'Registered bank customers who use banking services.', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE role_name = 'BANK_CUSTOMER'
);

INSERT INTO roles (role_name, description, created_at)
SELECT 'PUBLIC_CUSTOMER', 'General public users with limited access.', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE role_name = 'PUBLIC_CUSTOMER'
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
    created_at,
    updated_at
)
SELECT
    r.role_id,
    'admin_demo',
    'admin@bank.com',
    'hashed_password_admin',
    'John',
    'Admin',
    '+1234567890',
    'NIC1001',
    'ACTIVE',
    NOW(),
    NOW()
FROM roles r
WHERE r.role_name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@bank.com'
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
    created_at,
    updated_at
)
SELECT
    r.role_id,
    'officer_demo',
    'officer@bank.com',
    'hashed_password_officer',
    'Jane',
    'Officer',
    '+1234567891',
    'NIC1002',
    'ACTIVE',
    NOW(),
    NOW()
FROM roles r
WHERE r.role_name = 'BANK_OFFICER'
AND NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'officer@bank.com'
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
    created_at,
    updated_at
)
SELECT
    r.role_id,
    'customer_demo',
    'customer@bank.com',
    'hashed_password_customer',
    'Bob',
    'Customer',
    '+1234567892',
    'NIC1003',
    'ACTIVE',
    NOW(),
    NOW()
FROM roles r
WHERE r.role_name = 'BANK_CUSTOMER'
AND NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'customer@bank.com'
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
    created_at,
    updated_at
)
SELECT
    r.role_id,
    'public_demo',
    'public@bank.com',
    'hashed_password_public',
    'Alice',
    'Public',
    '+1234567893',
    'NIC1004',
    'ACTIVE',
    NOW(),
    NOW()
FROM roles r
WHERE r.role_name = 'PUBLIC_CUSTOMER'
AND NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'public@bank.com'
);
