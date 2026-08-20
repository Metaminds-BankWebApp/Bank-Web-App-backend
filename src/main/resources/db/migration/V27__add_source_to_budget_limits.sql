ALTER TABLE budget_limits
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE budget_limits
    ADD CONSTRAINT chk_budget_limits_source CHECK (source IN ('MANUAL', 'ROLLOVER'));
