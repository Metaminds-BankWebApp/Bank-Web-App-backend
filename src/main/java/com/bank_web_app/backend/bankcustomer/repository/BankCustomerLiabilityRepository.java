package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLiability;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerLiabilityRepository extends JpaRepository<BankCustomerLiability, Long> {
	List<BankCustomerLiability> findAllByFinancialRecord_BankRecordId(Long bankRecordId);

	void deleteByFinancialRecord_BankRecordId(Long bankRecordId);
}
