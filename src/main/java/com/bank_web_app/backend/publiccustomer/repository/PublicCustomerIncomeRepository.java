package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for income rows inside financial records.
public interface PublicCustomerIncomeRepository extends JpaRepository<PublicCustomerIncome, Long> {
	// Returns all income rows linked to a financial record.
	List<PublicCustomerIncome> findAllByFinancialRecord_RecordId(Long recordId);

	// Returns income rows for a batch of financial records.
	List<PublicCustomerIncome> findAllByFinancialRecord_RecordIdIn(Collection<Long> recordIds);

	// Deletes all income rows linked to a financial record.
	void deleteByFinancialRecord_RecordId(Long recordId);
}
