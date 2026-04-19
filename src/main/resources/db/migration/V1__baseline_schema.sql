CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    nic VARCHAR(20) UNIQUE,
    dob DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    address VARCHAR(255),
    province VARCHAR(100),
    profile_picture_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE TABLE IF NOT EXISTS branches (
    branch_id BIGSERIAL PRIMARY KEY,
    branch_code VARCHAR(20) NOT NULL UNIQUE,
    branch_name VARCHAR(100) NOT NULL,
    branch_email VARCHAR(100) UNIQUE,
    branch_phone VARCHAR(20),
    address VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_branches_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE IF NOT EXISTS bank_officers (
    officer_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    branch_id BIGINT NOT NULL,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    created_by_admin_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_officers_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_bank_officers_branch FOREIGN KEY (branch_id) REFERENCES branches (branch_id),
    CONSTRAINT fk_bank_officers_created_by_admin FOREIGN KEY (created_by_admin_user_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS public_customers (
    public_customer_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_public_customers_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    account_type VARCHAR(30) NOT NULL,
    balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'FROZEN')),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS bank_customers (
    bank_customer_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    officer_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    access_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    account_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customers_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_bank_customers_officer FOREIGN KEY (officer_id) REFERENCES bank_officers (officer_id),
    CONSTRAINT fk_bank_customers_branch FOREIGN KEY (branch_id) REFERENCES branches (branch_id),
    CONSTRAINT fk_bank_customers_account FOREIGN KEY (account_id) REFERENCES accounts (account_id),
    CONSTRAINT chk_bank_customers_access_status CHECK (access_status IN ('ACTIVE', 'INACTIVE', 'DRAFT', 'PENDING_STEP_2'))
);

CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_bank_officers_branch_id ON bank_officers(branch_id);
CREATE INDEX IF NOT EXISTS idx_bank_customers_officer_id ON bank_customers(officer_id);
CREATE INDEX IF NOT EXISTS idx_bank_customers_branch_id ON bank_customers(branch_id);
