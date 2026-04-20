package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerIncomeRepository extends JpaRepository<BankCustomerIncome, Long> {
	List<BankCustomerIncome> findAllByFinancialRecord_BankRecordId(Long bankRecordId);

	void deleteByFinancialRecord_BankRecordId(Long bankRecordId);
}
