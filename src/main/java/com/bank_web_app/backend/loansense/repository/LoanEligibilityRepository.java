package com.bank_web_app.backend.loansense.repository;

import com.bank_web_app.backend.loansense.entity.LoanSenseEvaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanEligibilityRepository extends JpaRepository<LoanSenseEvaluation, Long> {

	Optional<LoanSenseEvaluation> findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	List<LoanSenseEvaluation> findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(Long bankCustomerId);

	Optional<LoanSenseEvaluation> findByLoansenseEvaluationIdAndBankCustomer_BankCustomerId(
		Long loansenseEvaluationId,
		Long bankCustomerId
	);
}
