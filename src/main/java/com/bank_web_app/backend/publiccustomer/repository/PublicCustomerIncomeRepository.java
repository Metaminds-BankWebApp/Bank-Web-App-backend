package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCustomerIncomeRepository extends JpaRepository<PublicCustomerIncome, Long> {
	List<PublicCustomerIncome> findAllByFinancialRecord_RecordId(Long recordId);
}
