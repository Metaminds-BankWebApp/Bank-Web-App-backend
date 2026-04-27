WITH ranked AS (
    SELECT beneficiary_id,
           ROW_NUMBER() OVER (
               PARTITION BY bank_customer_id, beneficiary_account_no
               ORDER BY created_at ASC, beneficiary_id ASC
           ) AS row_num
    FROM bank_customer_beneficiaries
)
DELETE FROM bank_customer_beneficiaries target
USING ranked source
WHERE target.beneficiary_id = source.beneficiary_id
  AND source.row_num > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_bank_customer_beneficiaries_customer_account
    ON bank_customer_beneficiaries (bank_customer_id, beneficiary_account_no);
