-- DEVELOPMENT/STAGING ONLY. Run manually in Neon SQL Editor after JPA has
-- created the schema. This script is idempotent and is not a Flyway migration.
--
-- All four accounts use the password: Demo@1234
-- Change or remove these accounts before exposing the environment publicly.

INSERT INTO roles (role_name, description, created_at)
VALUES
    ('ADMIN', 'System administrator', CURRENT_TIMESTAMP),
    ('BANK_OFFICER', 'Bank officer', CURRENT_TIMESTAMP),
    ('BANK_CUSTOMER', 'Bank customer', CURRENT_TIMESTAMP),
    ('PUBLIC_CUSTOMER', 'Public customer', CURRENT_TIMESTAMP)
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO branches (branch_code, branch_name, branch_email, branch_phone, address, status, created_at, updated_at)
VALUES ('TEST-001', 'Test Branch', 'test.branch@example.test', '0112000001', 'Test Address', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (branch_code) DO NOTHING;

-- BCrypt hash for Demo@1234 (cost factor 12).
WITH seed_users (role_name, username, email, first_name, last_name, phone, nic, dob, province, address) AS (
    VALUES
        ('ADMIN', 'admin.mock', 'admin.mock@example.test', 'Admin', 'Mock', '0771000101', '199012345601', DATE '1990-01-10', 'Western', 'Colombo'),
        ('BANK_OFFICER', 'officer.mock', 'officer.mock@example.test', 'Officer', 'Mock', '0771000102', '199112345602', DATE '1991-02-11', 'Central', 'Kandy'),
        ('BANK_CUSTOMER', 'bank.customer.mock', 'bank.customer.mock@example.test', 'Bank', 'Customer', '0771000103', '199212345603', DATE '1992-03-12', 'Southern', 'Galle'),
        ('PUBLIC_CUSTOMER', 'public.customer.mock', 'public.customer.mock@example.test', 'Public', 'Customer', '0771000104', '199312345604', DATE '1993-04-13', 'Northern', 'Jaffna')
)
INSERT INTO users (role_id, username, email, password_hash, first_name, last_name, phone, nic, dob, province, address, status, created_at, updated_at)
SELECT
    roles.role_id,
    seed_users.username,
    seed_users.email,
    '$2a$12$00VuZDzTjb0OGHmri06/q.MOY5DEWE28QE7MAS0jUPDv2pnFiiegW',
    seed_users.first_name,
    seed_users.last_name,
    seed_users.phone,
    seed_users.nic,
    seed_users.dob,
    seed_users.province,
    seed_users.address,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_users
JOIN roles ON roles.role_name = seed_users.role_name
ON CONFLICT (username) DO NOTHING;

INSERT INTO bank_officers (user_id, branch_id, employee_code, created_by_admin_user_id, created_at, updated_at)
SELECT officer.user_id, branch.branch_id, 'EMP-TEST-001', admin.user_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users officer
JOIN users admin ON admin.username = 'admin.mock'
JOIN branches branch ON branch.branch_code = 'TEST-001'
WHERE officer.username = 'officer.mock'
  AND NOT EXISTS (SELECT 1 FROM bank_officers existing WHERE existing.user_id = officer.user_id);

INSERT INTO accounts (account_number, account_type, balance, status, created_at, updated_at)
VALUES ('999000000001', 'SAVINGS', 100000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (account_number) DO NOTHING;

INSERT INTO bank_customers (user_id, customer_code, officer_id, branch_id, account_id, access_status, created_at, updated_at)
SELECT customer.user_id, 'BC-TEST-001', officer.officer_id, branch.branch_id, account.account_id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users customer
JOIN bank_officers officer ON officer.employee_code = 'EMP-TEST-001'
JOIN branches branch ON branch.branch_code = 'TEST-001'
JOIN accounts account ON account.account_number = '999000000001'
WHERE customer.username = 'bank.customer.mock'
  AND NOT EXISTS (SELECT 1 FROM bank_customers existing WHERE existing.user_id = customer.user_id);

INSERT INTO public_customers (user_id, customer_code, created_at, updated_at)
SELECT customer.user_id, 'PC-TEST-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users customer
WHERE customer.username = 'public.customer.mock'
  AND NOT EXISTS (SELECT 1 FROM public_customers existing WHERE existing.user_id = customer.user_id);
