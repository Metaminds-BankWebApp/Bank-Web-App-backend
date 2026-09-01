ALTER TABLE bank_customers
    ADD COLUMN IF NOT EXISTS activation_resend_count INTEGER NOT NULL DEFAULT 0;
