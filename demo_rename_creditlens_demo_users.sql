-- Rename the previously seeded mentor demo users to presentation-friendly names.
-- This does not change table structure or evaluation data.

BEGIN;

UPDATE users
SET
    username = 'BankOfficer_01',
    email = 'bankofficer01@primecore.local',
    first_name = 'Bank',
    last_name = 'Officer',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'mentor.officer'
   OR email = 'mentor.officer@primecore.local';

UPDATE users
SET
    username = 'PublicCustomer_01',
    email = 'publiccustomer01@primecore.local',
    first_name = 'Public',
    last_name = 'Customer 01',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'mentor.public.credit12'
   OR email = 'mentor.public.credit12@primecore.local';

UPDATE users
SET
    username = 'BankCustomer_01',
    email = 'bankcustomer01@primecore.local',
    first_name = 'Bank',
    last_name = 'Customer 01',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'mentor.bank.01'
   OR email = 'mentor.bank.01@primecore.local';

UPDATE users
SET
    username = 'BankCustomer_02',
    email = 'bankcustomer02@primecore.local',
    first_name = 'Bank',
    last_name = 'Customer 02',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'mentor.bank.02'
   OR email = 'mentor.bank.02@primecore.local';

UPDATE users
SET
    username = 'BankCustomer_03',
    email = 'bankcustomer03@primecore.local',
    first_name = 'Bank',
    last_name = 'Customer 03',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'mentor.bank.03'
   OR email = 'mentor.bank.03@primecore.local';

UPDATE users
SET
    username = 'BankCustomer_04',
    email = 'bankcustomer04@primecore.local',
    first_name = 'Bank',
    last_name = 'Customer 04',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'mentor.bank.04'
   OR email = 'mentor.bank.04@primecore.local';

UPDATE users
SET
    username = 'BankCustomer_05',
    email = 'bankcustomer05@primecore.local',
    first_name = 'Bank',
    last_name = 'Customer 05',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'mentor.bank.05'
   OR email = 'mentor.bank.05@primecore.local';

COMMIT;

SELECT username, email, first_name, last_name
FROM users
WHERE username IN (
    'BankOfficer_01',
    'PublicCustomer_01',
    'BankCustomer_01',
    'BankCustomer_02',
    'BankCustomer_03',
    'BankCustomer_04',
    'BankCustomer_05'
)
ORDER BY username;
