package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerMissedPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerMissedPaymentRepository extends JpaRepository<BankCustomerMissedPayment, Long> {
	Optional<BankCustomerMissedPayment> findByFinancialRecord_BankRecordId(Long bankRecordId);
}
