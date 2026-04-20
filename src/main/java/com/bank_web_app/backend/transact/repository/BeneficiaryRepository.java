package com.bank_web_app.backend.transact.repository;

import com.bank_web_app.backend.transact.entity.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

	List<Beneficiary> findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	Optional<Beneficiary> findByBeneficiaryIdAndBankCustomer_BankCustomerId(Long beneficiaryId, Long bankCustomerId);
}
