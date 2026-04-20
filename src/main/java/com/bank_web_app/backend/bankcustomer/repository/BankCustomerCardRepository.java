package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerCardRepository extends JpaRepository<BankCustomerCard, Long> {
	List<BankCustomerCard> findAllByFinancialRecord_BankRecordId(Long bankRecordId);
}
