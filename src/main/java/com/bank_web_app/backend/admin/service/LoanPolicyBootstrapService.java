package com.bank_web_app.backend.admin.service;
import com.bank_web_app.backend.admin.entity.LoanPolicy;
import com.bank_web_app.backend.admin.repository.LoanPolicyRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates Admin business logic, validation, and persistence workflows.
 */

@Service
public class LoanPolicyBootstrapService {

	private static final List<LoanPolicySeed> DEFAULT_POLICIES = List.of(
		new LoanPolicySeed("PERSONAL", new BigDecimal("0.4000"), new BigDecimal("17.00"), 60, 21, 60, null, new BigDecimal("50000.00")),
		new LoanPolicySeed("VEHICLE", new BigDecimal("0.4000"), new BigDecimal("15.00"), 84, 21, 65, new BigDecimal("80.00"), new BigDecimal("75000.00")),
		new LoanPolicySeed("EDUCATION", new BigDecimal("0.4000"), new BigDecimal("12.00"), 120, 18, 55, null, new BigDecimal("200000.00")),
		new LoanPolicySeed("HOUSING", new BigDecimal("0.4000"), new BigDecimal("10.00"), 240, 21, 60, new BigDecimal("90.00"), new BigDecimal("250000.00"))
	);

	private final LoanPolicyRepository loanPolicyRepository;

	public LoanPolicyBootstrapService(LoanPolicyRepository loanPolicyRepository) {
		this.loanPolicyRepository = loanPolicyRepository;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	// Ensures default loan-policy records exist when the module initializes.
	public void ensureDefaultLoanPolicies() {
		for (LoanPolicySeed seed : DEFAULT_POLICIES) {
			if (loanPolicyRepository.findByLoanType(seed.loanType()).isPresent()) {
				continue;
			}

			LoanPolicy policy = new LoanPolicy();
			policy.setLoanType(seed.loanType());
			policy.setMaxDbrRatio(seed.maxDbrRatio());
			policy.setBaseInterestRate(seed.baseInterestRate());
			policy.setMaxTenureMonths(seed.maxTenureMonths());
			policy.setMinAge(seed.minAge());
			policy.setMaxAge(seed.maxAge());
			policy.setMaxFinancePercentage(seed.maxFinancePercentage());
			policy.setMinIncomeRequired(seed.minIncomeRequired());
			policy.setStatus("ACTIVE");

			loanPolicyRepository.save(policy);
		}
	}

	private record LoanPolicySeed(
		String loanType,
		BigDecimal maxDbrRatio,
		BigDecimal baseInterestRate,
		Integer maxTenureMonths,
		Integer minAge,
		Integer maxAge,
		BigDecimal maxFinancePercentage,
		BigDecimal minIncomeRequired
	) {}
}
