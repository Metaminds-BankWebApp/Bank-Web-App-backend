-- Run this script once against the separate CRIB database before enforcing
-- the V-only old NIC format. It converts only 9-digit old NICs ending in X/x.
BEGIN;

UPDATE crib_customers
SET
    nic = LEFT(nic, 9) || 'V',
    nic_format = 'OLD'
WHERE nic ~ '^[0-9]{9}[Xx]$';

ALTER TABLE crib_customers
    DROP CONSTRAINT IF EXISTS chk_crib_customers_nic_format;

ALTER TABLE crib_customers
    ADD CONSTRAINT chk_crib_customers_nic_format
    CHECK (nic ~ '^(?:[0-9]{9}[Vv]|[0-9]{12})$');

COMMIT;
