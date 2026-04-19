package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLoan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCustomerLoanRepository extends JpaRepository<PublicCustomerLoan, Long> {
	List<PublicCustomerLoan> findAllByFinancialRecord_RecordId(Long recordId);

	void deleteByFinancialRecord_RecordId(Long recordId);
}
