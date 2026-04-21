ALTER TABLE bank_customers
    DROP CONSTRAINT IF EXISTS chk_bank_customers_access_status;

ALTER TABLE bank_customers
    ADD CONSTRAINT chk_bank_customers_access_status
        CHECK (
            access_status IN (
                'ACTIVE',
                'INACTIVE',
                'DRAFT',
                'PENDING_STEP_2',
                'PENDING_STEP_3',
                'PENDING_STEP_4',
                'PENDING_STEP_5',
                'PENDING_STEP_6',
                'PENDING_STEP_7',
                'PENDING_STEP_8',
                'COMPLETED'
            )
        );
