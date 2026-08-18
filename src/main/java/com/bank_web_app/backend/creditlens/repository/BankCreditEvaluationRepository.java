package com.bank_web_app.backend.creditlens.repository;

import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for bank-customer CreditLens evaluations, including officer-generated views.
 */
public interface BankCreditEvaluationRepository extends JpaRepository<BankCreditEvaluation, Long> {

	// Finds the newest bank evaluation for a bank customer.
	Optional<BankCreditEvaluation> findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	// Finds the newest bank evaluation created for one bank financial record.
	Optional<BankCreditEvaluation> findTopByBankRecord_BankRecordIdOrderByCreatedAtDesc(Long bankRecordId);

	// Lists all bank evaluations for the bank customer history view.
	List<BankCreditEvaluation> findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	// Finds one bank evaluation only when it belongs to the bank customer.
	Optional<BankCreditEvaluation> findByBankEvaluationIdAndBankCustomer_BankCustomerId(Long bankEvaluationId, Long bankCustomerId);

	// Checks whether an officer has produced any bank credit evaluations.
	boolean existsByEvaluatedByOfficer_OfficerId(Long officerId);

	BankCreditEvaluation[] findAllByBankCustomer_Officer_OfficerIdOrderByBankCustomer_BankCustomerIdAscCreatedAtDesc(
			Long officerId);

	// Lists the complete evaluation activity feed for customers assigned to one officer.
	List<BankCreditEvaluation> findAllByBankCustomer_Officer_OfficerIdOrderByCreatedAtDesc(Long officerId);

	List<BankCreditEvaluation> findAllByOrderByBankCustomer_BankCustomerIdAscCreatedAtDesc();

	List<BankCreditEvaluation> findAllByOrderByCreatedAtDesc();
}
