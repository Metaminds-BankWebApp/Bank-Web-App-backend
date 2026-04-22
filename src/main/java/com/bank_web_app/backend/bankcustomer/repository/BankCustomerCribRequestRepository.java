package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCribRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerCribRequestRepository extends JpaRepository<BankCustomerCribRequest, Long> {

	Optional<BankCustomerCribRequest> findTopByBankCustomer_BankCustomerIdOrderByRequestedAtDesc(Long bankCustomerId);

	List<BankCustomerCribRequest> findAllByBankCustomer_BankCustomerIdOrderByRequestedAtDesc(Long bankCustomerId);
}
