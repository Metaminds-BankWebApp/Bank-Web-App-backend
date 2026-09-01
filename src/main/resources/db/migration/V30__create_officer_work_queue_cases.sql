CREATE TABLE officer_work_queue_cases (
  work_queue_case_id BIGSERIAL PRIMARY KEY,
  bank_customer_id BIGINT NOT NULL REFERENCES bank_customers(bank_customer_id),
  case_type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  updated_by_officer_id BIGINT NOT NULL REFERENCES bank_officers(officer_id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_work_queue_customer_case UNIQUE (bank_customer_id, case_type),
  CONSTRAINT chk_work_queue_case_type CHECK (case_type IN ('RISK_REVIEW', 'PROFILE_COMPLETION')),
  CONSTRAINT chk_work_queue_case_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'ESCALATED'))
);
