package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLoan;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for loan rows inside financial records.
public interface PublicCustomerLoanRepository extends JpaRepository<PublicCustomerLoan, Long> {
	// Returns all loan rows linked to a financial record.
	List<PublicCustomerLoan> findAllByFinancialRecord_RecordId(Long recordId);

	// Returns loan rows for a batch of financial records.
	List<PublicCustomerLoan> findAllByFinancialRecord_RecordIdIn(Collection<Long> recordIds);

	// Deletes all loan rows linked to a financial record.
	void deleteByFinancialRecord_RecordId(Long recordId);
}
