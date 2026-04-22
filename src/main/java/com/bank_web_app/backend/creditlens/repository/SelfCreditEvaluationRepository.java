package com.bank_web_app.backend.creditlens.repository;

import com.bank_web_app.backend.creditlens.entity.SelfCreditEvaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelfCreditEvaluationRepository extends JpaRepository<SelfCreditEvaluation, Long> {

	Optional<SelfCreditEvaluation> findTopByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(Long publicCustomerId);

	Optional<SelfCreditEvaluation> findTopByPublicRecord_RecordIdOrderByCreatedAtDesc(Long recordId);

	List<SelfCreditEvaluation> findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(Long publicCustomerId);

	Optional<SelfCreditEvaluation> findBySelfEvaluationIdAndPublicCustomer_PublicCustomerId(Long selfEvaluationId, Long publicCustomerId);
}
