-- Support conversations notify administrators and requesters. The notifications
-- table was initially created with database checks generated from the older enums.
ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_notification_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_notification_type_check
    CHECK (notification_type IN (
        'NEW_CUSTOMER_SUMMARY',
        'CUSTOMER_ASSIGNED',
        'BRANCH_STATUS_CHANGED',
        'OFFICER_ACCOUNT_STATUS_CHANGED',
        'SYSTEM_NOTICE',
        'SPENDIQ_TRANSFER_IMPORTED',
        'SPENDIQ_BUDGET_THRESHOLD',
        'SPENDIQ_MONTHLY_SUMMARY',
        'CREDITLENS_RESULT_AVAILABLE',
        'CREDITLENS_PORTFOLIO_ATTENTION',
        'LOANSENSE_RESULT_AVAILABLE',
        'LOANSENSE_PORTFOLIO_ATTENTION',
        'LOAN_POLICY_CHANGED',
        'FINANCIAL_DETAILS_MISSING',
        'MONTHLY_FINANCIAL_REVIEW_DUE',
        'SUPPORT_MESSAGE'
    ));

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_source_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_source_check
    CHECK (source IN (
        'ADMIN',
        'ONBOARDING',
        'TRANSACT',
        'SPENDIQ',
        'CREDITLENS',
        'LOANSENSE',
        'SUPPORT',
        'SYSTEM'
    ));
