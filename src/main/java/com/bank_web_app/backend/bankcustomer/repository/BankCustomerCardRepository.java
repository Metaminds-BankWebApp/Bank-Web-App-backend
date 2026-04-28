package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BankCustomerCardRepository extends JpaRepository<BankCustomerCard, Long> {
	List<BankCustomerCard> findAllByFinancialRecord_BankRecordId(Long bankRecordId);

	void deleteByFinancialRecord_BankRecordId(Long bankRecordId);

	@Query("select distinct c.provider from BankCustomerCard c where c.provider is not null and trim(c.provider) <> '' order by c.provider")
	List<String> findDistinctProviders();
}
