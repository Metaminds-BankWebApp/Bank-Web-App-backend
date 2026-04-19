package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerMissedPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCustomerMissedPaymentRepository extends JpaRepository<PublicCustomerMissedPayment, Long> {
	Optional<PublicCustomerMissedPayment> findByFinancialRecord_RecordId(Long recordId);
}
