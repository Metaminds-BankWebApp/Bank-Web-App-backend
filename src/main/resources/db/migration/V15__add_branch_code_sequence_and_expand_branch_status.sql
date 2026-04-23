CREATE SEQUENCE IF NOT EXISTS branch_code_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 1;

DO $$
DECLARE max_code bigint;
BEGIN
    SELECT MAX(CAST(SUBSTRING(branch_code FROM 4) AS INTEGER))
    INTO max_code
    FROM branches
    WHERE branch_code ~ '^BR-[0-9]+$';

    IF max_code IS NULL THEN
        PERFORM setval('branch_code_seq', 1, false);
    ELSE
        PERFORM setval('branch_code_seq', max_code, true);
    END IF;
END $$;

ALTER TABLE branches
    DROP CONSTRAINT IF EXISTS chk_branches_status;

ALTER TABLE branches
    ADD CONSTRAINT chk_branches_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'));