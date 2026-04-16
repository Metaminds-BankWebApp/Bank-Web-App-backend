-- Roles
INSERT INTO roles (name)
SELECT 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

INSERT INTO roles (name)
SELECT 'BANK_OFFICER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'BANK_OFFICER');

INSERT INTO roles (name)
SELECT 'BANK_CUSTOMER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'BANK_CUSTOMER');

INSERT INTO roles (name)
SELECT 'PUBLIC_CUSTOMER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'PUBLIC_CUSTOMER');

-- Users
INSERT INTO users (full_name, email, password, phone_number, status, created_at, updated_at)
SELECT 'John Admin', 'admin@bank.com', 'hashed_password_admin', '+1234567890', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@bank.com');

INSERT INTO users (full_name, email, password, phone_number, status, created_at, updated_at)
SELECT 'Jane Officer', 'officer@bank.com', 'hashed_password_officer', '+1234567891', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'officer@bank.com');

INSERT INTO users (full_name, email, password, phone_number, status, created_at, updated_at)
SELECT 'Bob Customer', 'customer@bank.com', 'hashed_password_customer', '+1234567892', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'customer@bank.com');

INSERT INTO users (full_name, email, password, phone_number, status, created_at, updated_at)
SELECT 'Alice Public', 'public@bank.com', 'hashed_password_public', '+1234567893', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'public@bank.com');

-- User-role mappings
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@bank.com'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'BANK_OFFICER'
WHERE u.email = 'officer@bank.com'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'BANK_CUSTOMER'
WHERE u.email = 'customer@bank.com'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'PUBLIC_CUSTOMER'
WHERE u.email = 'public@bank.com'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);
