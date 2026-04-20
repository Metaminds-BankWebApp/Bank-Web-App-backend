package com.bank_web_app.backend.loansense.repository;

import com.bank_web_app.backend.loansense.entity.LoanEligibilityResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanEligibilityResultRepository extends JpaRepository<LoanEligibilityResult, Long> {

	List<LoanEligibilityResult> findAllByLoanSenseEvaluation_LoansenseEvaluationIdOrderByCreatedAtAsc(Long loansenseEvaluationId);

	Optional<LoanEligibilityResult> findByLoanSenseEvaluation_LoansenseEvaluationIdAndLoanType(
		Long loansenseEvaluationId,
		String loanType
	);
}
