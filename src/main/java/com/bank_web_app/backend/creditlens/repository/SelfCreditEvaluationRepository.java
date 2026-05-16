package com.bank_web_app.backend.creditlens.repository;

import com.bank_web_app.backend.creditlens.entity.SelfCreditEvaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for public-customer CreditLens evaluations and their historical snapshots.
 */
public interface SelfCreditEvaluationRepository extends JpaRepository<SelfCreditEvaluation, Long> {

	// Finds the newest self evaluation for a public customer.
	Optional<SelfCreditEvaluation> findTopByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(Long publicCustomerId);

	// Finds the newest self evaluation created for one financial record.
	Optional<SelfCreditEvaluation> findTopByPublicRecord_RecordIdOrderByCreatedAtDesc(Long recordId);

	// Lists all self evaluations for the public customer history view.
	List<SelfCreditEvaluation> findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(Long publicCustomerId);

	// Finds one self evaluation only when it belongs to the public customer.
	Optional<SelfCreditEvaluation> findBySelfEvaluationIdAndPublicCustomer_PublicCustomerId(Long selfEvaluationId, Long publicCustomerId);
}
