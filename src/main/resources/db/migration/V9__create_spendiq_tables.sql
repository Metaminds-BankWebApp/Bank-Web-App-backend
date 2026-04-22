CREATE TABLE IF NOT EXISTS expense_categories (
    category_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    category_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expense_categories_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_expense_categories_type CHECK (UPPER(category_type) IN ('FIXED', 'VARIABLE'))
);

CREATE TABLE IF NOT EXISTS expense_records (
    expense_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    expense_date DATE NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expense_records_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_expense_records_category FOREIGN KEY (category_id) REFERENCES expense_categories (category_id),
    CONSTRAINT chk_expense_records_amount_non_negative CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS income_records (
    income_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source_name VARCHAR(100) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    income_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_income_records_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_income_records_amount_non_negative CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS budget_limits (
    budget_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    budget_amount NUMERIC(15,2) NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_limits_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_budget_limits_category FOREIGN KEY (category_id) REFERENCES expense_categories (category_id),
    CONSTRAINT uq_budget_limits_user_category_period UNIQUE (user_id, category_id, month, year),
    CONSTRAINT chk_budget_limits_amount_non_negative CHECK (budget_amount >= 0),
    CONSTRAINT chk_budget_limits_month CHECK (month BETWEEN 1 AND 12)
);

CREATE INDEX IF NOT EXISTS idx_expense_categories_user_id ON expense_categories(user_id);
CREATE INDEX IF NOT EXISTS idx_expense_records_user_id ON expense_records(user_id);
CREATE INDEX IF NOT EXISTS idx_expense_records_category_id ON expense_records(category_id);
CREATE INDEX IF NOT EXISTS idx_expense_records_expense_date ON expense_records(expense_date);
CREATE INDEX IF NOT EXISTS idx_income_records_user_id ON income_records(user_id);
CREATE INDEX IF NOT EXISTS idx_income_records_income_date ON income_records(income_date);
CREATE INDEX IF NOT EXISTS idx_budget_limits_user_period ON budget_limits(user_id, month, year);