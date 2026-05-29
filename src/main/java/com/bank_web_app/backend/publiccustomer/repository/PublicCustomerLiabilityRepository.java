package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLiability;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for liability rows inside financial records.
public interface PublicCustomerLiabilityRepository extends JpaRepository<PublicCustomerLiability, Long> {
	// Returns all liability rows linked to a financial record.
	List<PublicCustomerLiability> findAllByFinancialRecord_RecordId(Long recordId);

	// Deletes all liability rows linked to a financial record.
	void deleteByFinancialRecord_RecordId(Long recordId);
}
