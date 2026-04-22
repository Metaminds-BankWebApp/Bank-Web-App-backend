CREATE TABLE IF NOT EXISTS spendiq_expenses (
    expense_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    expense_date DATE NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_spendiq_expenses_account FOREIGN KEY (account_id) REFERENCES accounts (account_id) ON DELETE CASCADE,
    CONSTRAINT chk_spendiq_expenses_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_spendiq_expenses_account_id ON spendiq_expenses(account_id);
CREATE INDEX IF NOT EXISTS idx_spendiq_expenses_expense_date ON spendiq_expenses(expense_date);
