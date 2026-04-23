-- Align SpendIQ expense schema with current JPA entities (Expense, ExpenseCategory).

CREATE TABLE IF NOT EXISTS expense_categories (
    category_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    category_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS expense_records (
    expense_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    expense_date DATE NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    tracking_source VARCHAR(30),
    tracking_reference VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE expense_categories
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS category_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS category_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE expense_records
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS category_id BIGINT,
    ADD COLUMN IF NOT EXISTS amount NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS expense_date DATE,
    ADD COLUMN IF NOT EXISTS payment_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tracking_source VARCHAR(30),
    ADD COLUMN IF NOT EXISTS tracking_reference VARCHAR(50),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_expense_categories_user') THEN
        ALTER TABLE expense_categories
            ADD CONSTRAINT fk_expense_categories_user
                FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_expense_categories_type') THEN
        ALTER TABLE expense_categories
            ADD CONSTRAINT chk_expense_categories_type
                CHECK (UPPER(category_type) IN ('FIXED', 'VARIABLE'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_expense_records_user') THEN
        ALTER TABLE expense_records
            ADD CONSTRAINT fk_expense_records_user
                FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_expense_records_category') THEN
        ALTER TABLE expense_records
            ADD CONSTRAINT fk_expense_records_category
                FOREIGN KEY (category_id) REFERENCES expense_categories (category_id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_expense_records_amount_positive') THEN
        ALTER TABLE expense_records
            ADD CONSTRAINT chk_expense_records_amount_positive
                CHECK (amount > 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_expense_categories_user_id
    ON expense_categories(user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_expense_categories_user_name_ci
    ON expense_categories(user_id, LOWER(category_name));

CREATE INDEX IF NOT EXISTS idx_expense_records_user_id
    ON expense_records(user_id);

CREATE INDEX IF NOT EXISTS idx_expense_records_category_id
    ON expense_records(category_id);

CREATE INDEX IF NOT EXISTS idx_expense_records_expense_date
    ON expense_records(expense_date);

CREATE UNIQUE INDEX IF NOT EXISTS uq_expense_records_tracking_reference
    ON expense_records(tracking_source, tracking_reference)
    WHERE tracking_source IS NOT NULL AND tracking_reference IS NOT NULL;
