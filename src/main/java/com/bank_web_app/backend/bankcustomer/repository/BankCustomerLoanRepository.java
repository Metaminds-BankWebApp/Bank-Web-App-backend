package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLoan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerLoanRepository extends JpaRepository<BankCustomerLoan, Long> {
	List<BankCustomerLoan> findAllByFinancialRecord_BankRecordId(Long bankRecordId);
}
