package com.bank_web_app.backend.bankofficer.repository;

import com.bank_web_app.backend.bankofficer.entity.OfficerWorkQueueCase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficerWorkQueueCaseRepository extends JpaRepository<OfficerWorkQueueCase, Long> {
	List<OfficerWorkQueueCase> findAllByOrderByUpdatedAtDesc();
	Optional<OfficerWorkQueueCase> findByBankCustomer_BankCustomerIdAndCaseType(Long bankCustomerId, String caseType);
}
