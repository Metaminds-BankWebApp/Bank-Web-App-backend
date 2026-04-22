CREATE TABLE IF NOT EXISTS bank_customer_crib_requests (
    crib_request_id BIGSERIAL PRIMARY KEY,
    bank_customer_id BIGINT NOT NULL,
    requested_by_officer_id BIGINT NOT NULL,
    request_type VARCHAR(30) NOT NULL DEFAULT 'FULL_REPORT',
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    report_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUESTED',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_received_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_customer_crib_requests_customer
        FOREIGN KEY (bank_customer_id) REFERENCES bank_customers(bank_customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_bank_customer_crib_requests_officer
        FOREIGN KEY (requested_by_officer_id) REFERENCES bank_officers(officer_id),
    CONSTRAINT chk_bank_customer_crib_requests_request_type
        CHECK (request_type IN ('FULL_REPORT', 'SUMMARY_ONLY', 'REFRESH')),
    CONSTRAINT chk_bank_customer_crib_requests_request_status
        CHECK (request_status IN ('PENDING', 'SUBMITTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_bank_customer_crib_requests_report_status
        CHECK (report_status IN ('NOT_REQUESTED', 'PENDING', 'PROCESSING', 'READY', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_bank_customer_crib_requests_response_after_request
        CHECK (response_received_at IS NULL OR response_received_at >= requested_at),
    CONSTRAINT chk_bank_customer_crib_requests_expiry_after_request
        CHECK (expires_at IS NULL OR expires_at >= requested_at)
);

CREATE INDEX IF NOT EXISTS idx_bank_customer_crib_requests_customer_id
    ON bank_customer_crib_requests(bank_customer_id);

CREATE INDEX IF NOT EXISTS idx_bank_customer_crib_requests_officer_id
    ON bank_customer_crib_requests(requested_by_officer_id);

CREATE INDEX IF NOT EXISTS idx_bank_customer_crib_requests_request_status
    ON bank_customer_crib_requests(request_status);

CREATE INDEX IF NOT EXISTS idx_bank_customer_crib_requests_report_status
    ON bank_customer_crib_requests(report_status);

CREATE INDEX IF NOT EXISTS idx_bank_customer_crib_requests_requested_at
    ON bank_customer_crib_requests(requested_at DESC);