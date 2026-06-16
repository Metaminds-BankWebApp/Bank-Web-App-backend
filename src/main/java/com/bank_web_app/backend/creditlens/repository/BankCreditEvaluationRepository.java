package com.bank_web_app.backend.creditlens.repository;

import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCreditEvaluationRepository extends JpaRepository<BankCreditEvaluation, Long> {

	Optional<BankCreditEvaluation> findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	Optional<BankCreditEvaluation> findTopByBankRecord_BankRecordIdOrderByCreatedAtDesc(Long bankRecordId);

	List<BankCreditEvaluation> findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	/**
	 * Support fetching evaluations for all customers assigned to an officer.
	 * The results are ordered by bankCustomer.bankCustomerId ASC, then
	 * createdAt DESC so callers can iterate and pick the first entry per
	 * customer as the latest evaluation.
	 */
	List<BankCreditEvaluation> findAllByBankCustomer_Officer_OfficerIdOrderByBankCustomer_BankCustomerIdAscCreatedAtDesc(Long officerId);

	Optional<BankCreditEvaluation> findByBankEvaluationIdAndBankCustomer_BankCustomerId(Long bankEvaluationId, Long bankCustomerId);

	boolean existsByEvaluatedByOfficer_OfficerId(Long officerId);
}
