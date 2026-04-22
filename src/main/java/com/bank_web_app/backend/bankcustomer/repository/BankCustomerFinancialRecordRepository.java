package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerFinancialRecordRepository extends JpaRepository<BankCustomerFinancialRecord, Long> {

	Optional<BankCustomerFinancialRecord> findByBankRecordIdAndBankCustomer_BankCustomerId(Long bankRecordId, Long bankCustomerId);

	Optional<BankCustomerFinancialRecord> findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	List<BankCustomerFinancialRecord> findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);
}
