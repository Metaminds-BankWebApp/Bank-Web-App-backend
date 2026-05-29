package com.bank_web_app.backend.transact.repository;

import com.bank_web_app.backend.transact.entity.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for beneficiary persistence and lookup operations.
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

	// Returns all beneficiaries for a customer, newest first.
	List<Beneficiary> findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	// Finds a beneficiary by id, scoped to a specific customer owner.
	Optional<Beneficiary> findByBeneficiaryIdAndBankCustomer_BankCustomerId(Long beneficiaryId, Long bankCustomerId);

	// Checks whether a beneficiary account already exists for this customer.
	boolean existsByBankCustomer_BankCustomerIdAndBeneficiaryAccountNo(Long bankCustomerId, String beneficiaryAccountNo);

	// Checks duplicate beneficiary account while excluding one beneficiary id (update case).
	boolean existsByBankCustomer_BankCustomerIdAndBeneficiaryAccountNoAndBeneficiaryIdNot(
		Long bankCustomerId,
		String beneficiaryAccountNo,
		Long beneficiaryId
	);

	// Counts total saved beneficiaries for a customer.
	long countByBankCustomer_BankCustomerId(Long bankCustomerId);
}
