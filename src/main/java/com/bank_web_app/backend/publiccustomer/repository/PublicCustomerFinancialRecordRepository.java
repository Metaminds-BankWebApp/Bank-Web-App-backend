package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCustomerFinancialRecordRepository extends JpaRepository<PublicCustomerFinancialRecord, Long> {

	Optional<PublicCustomerFinancialRecord> findByPublicCustomer_PublicCustomerIdAndRecordStatus(Long publicCustomerId, String recordStatus);

	Optional<PublicCustomerFinancialRecord> findByRecordIdAndPublicCustomer_PublicCustomerId(Long recordId, Long publicCustomerId);

	List<PublicCustomerFinancialRecord> findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(Long publicCustomerId);
}
