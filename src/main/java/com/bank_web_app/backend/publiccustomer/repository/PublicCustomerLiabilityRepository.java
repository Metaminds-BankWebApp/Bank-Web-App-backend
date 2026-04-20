package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLiability;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCustomerLiabilityRepository extends JpaRepository<PublicCustomerLiability, Long> {
	List<PublicCustomerLiability> findAllByFinancialRecord_RecordId(Long recordId);

	void deleteByFinancialRecord_RecordId(Long recordId);
}
