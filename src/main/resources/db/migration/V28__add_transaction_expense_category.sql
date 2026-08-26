ALTER TABLE bank_customer_transactions
    ADD COLUMN IF NOT EXISTS expense_category_name VARCHAR(100);
