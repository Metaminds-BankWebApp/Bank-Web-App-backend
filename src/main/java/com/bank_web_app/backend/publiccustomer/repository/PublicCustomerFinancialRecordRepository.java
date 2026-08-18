package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for financial-record snapshots of public-customer applications.
public interface PublicCustomerFinancialRecordRepository extends JpaRepository<PublicCustomerFinancialRecord, Long> {

	// Finds one record by customer id and status (e.g., CURRENT).
	Optional<PublicCustomerFinancialRecord> findByPublicCustomer_PublicCustomerIdAndRecordStatus(Long publicCustomerId, String recordStatus);

	// Finds one record by record id scoped to a customer owner.
	Optional<PublicCustomerFinancialRecord> findByRecordIdAndPublicCustomer_PublicCustomerId(Long recordId, Long publicCustomerId);

	// Returns all records for a customer, newest first.
	List<PublicCustomerFinancialRecord> findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(Long publicCustomerId);
}
