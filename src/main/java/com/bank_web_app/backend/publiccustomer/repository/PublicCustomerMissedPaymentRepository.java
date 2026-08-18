package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerMissedPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for missed-payment aggregates per financial record.
public interface PublicCustomerMissedPaymentRepository extends JpaRepository<PublicCustomerMissedPayment, Long> {
	// Returns missed-payment row linked to a financial record, if present.
	Optional<PublicCustomerMissedPayment> findByFinancialRecord_RecordId(Long recordId);
}
