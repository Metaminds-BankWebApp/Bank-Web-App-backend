package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCustomerCardRepository extends JpaRepository<PublicCustomerCard, Long> {
	List<PublicCustomerCard> findAllByFinancialRecord_RecordId(Long recordId);
}
