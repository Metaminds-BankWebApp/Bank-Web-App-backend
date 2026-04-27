DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'bank_customer_transaction_otp_logs'
    ) THEN
        ALTER TABLE bank_customer_transaction_otp_logs RENAME TO transaction_otp_logs;
    END IF;
END $$;
