package com.bank_web_app.backend.loansense.service;

import com.bank_web_app.backend.admin.entity.LoanPolicy;
import com.bank_web_app.backend.admin.entity.RiskAdjustment;
import com.bank_web_app.backend.admin.repository.LoanPolicyRepository;
import com.bank_web_app.backend.admin.repository.RiskAdjustmentRepository;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCard;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLiability;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLoan;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerMissedPayment;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerFinancialRecordRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerIncomeRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLiabilityRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLoanRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerMissedPaymentRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.service.CreditEvaluationService;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseEvaluationResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseHistoryItemResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanTypeDetailResponse;
import com.bank_web_app.backend.loansense.entity.LoanEligibilityResult;
import com.bank_web_app.backend.loansense.entity.LoanSenseEvaluation;
import com.bank_web_app.backend.loansense.mapper.LoanEligibilityMapper;
import com.bank_web_app.backend.loansense.repository.LoanEligibilityRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoanEligibilityService {

	private static final List<String> SUPPORTED_LOAN_TYPES = List.of("PERSONAL", "VEHICLE", "EDUCATION", "HOUSING");
	private static final Set<String> SUPPORTED_LOAN_TYPE_SET = Set.copyOf(SUPPORTED_LOAN_TYPES);
	private static final BigDecimal CARD_MIN_PAYMENT_RATIO = new BigDecimal("0.05");
	private static final BigDecimal DEFAULT_MAX_DBR_RATIO = new BigDecimal("0.40");

	private final LoanEligibilityRepository loanEligibilityRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository;
	private final BankCustomerIncomeRepository bankCustomerIncomeRepository;
	private final BankCustomerLoanRepository bankCustomerLoanRepository;
	private final BankCustomerCardRepository bankCustomerCardRepository;
	private final BankCustomerLiabilityRepository bankCustomerLiabilityRepository;
	private final BankCustomerMissedPaymentRepository bankCustomerMissedPaymentRepository;
	private final LoanPolicyRepository loanPolicyRepository;
	private final RiskAdjustmentRepository riskAdjustmentRepository;
	private final UserRepository userRepository;
	private final CreditEvaluationService creditEvaluationService;
	private final LoanEligibilityMapper loanEligibilityMapper;

	public LoanEligibilityService(
		LoanEligibilityRepository loanEligibilityRepository,
		BankCustomerRepository bankCustomerRepository,
		BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository,
		BankCustomerIncomeRepository bankCustomerIncomeRepository,
		BankCustomerLoanRepository bankCustomerLoanRepository,
		BankCustomerCardRepository bankCustomerCardRepository,
		BankCustomerLiabilityRepository bankCustomerLiabilityRepository,
		BankCustomerMissedPaymentRepository bankCustomerMissedPaymentRepository,
		LoanPolicyRepository loanPolicyRepository,
		RiskAdjustmentRepository riskAdjustmentRepository,
		UserRepository userRepository,
		CreditEvaluationService creditEvaluationService,
		LoanEligibilityMapper loanEligibilityMapper
	) {
		this.loanEligibilityRepository = loanEligibilityRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankCustomerFinancialRecordRepository = bankCustomerFinancialRecordRepository;
		this.bankCustomerIncomeRepository = bankCustomerIncomeRepository;
		this.bankCustomerLoanRepository = bankCustomerLoanRepository;
		this.bankCustomerCardRepository = bankCustomerCardRepository;
		this.bankCustomerLiabilityRepository = bankCustomerLiabilityRepository;
		this.bankCustomerMissedPaymentRepository = bankCustomerMissedPaymentRepository;
		this.loanPolicyRepository = loanPolicyRepository;
		this.riskAdjustmentRepository = riskAdjustmentRepository;
		this.userRepository = userRepository;
		this.creditEvaluationService = creditEvaluationService;
		this.loanEligibilityMapper = loanEligibilityMapper;
	}

	@Transactional
	public LoanSenseEvaluationResponse getCurrentEvaluation() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		return loanEligibilityMapper.toEvaluationResponse(getOrCreateLatestEvaluation(bankCustomer));
	}

	@Transactional
	public LoanTypeDetailResponse getCurrentLoanTypeDetail(String loanType) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		LoanSenseEvaluation evaluation = getOrCreateLatestEvaluation(bankCustomer);
		return buildLoanTypeDetail(evaluation, normalizeLoanType(loanType));
	}

	@Transactional
	public List<LoanSenseHistoryItemResponse> getHistory(String loanType, Integer months) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		getOrCreateLatestEvaluation(bankCustomer);

		String normalizedLoanType = loanType == null || loanType.isBlank() ? null : normalizeLoanType(loanType);
		LocalDateTime threshold = months == null ? null : LocalDateTime.now().minusMonths(normalizePositiveMonths(months));

		return loanEligibilityRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.stream()
			.filter(evaluation -> threshold == null || !evaluation.getCreatedAt().isBefore(threshold))
			.flatMap(evaluation ->
				evaluation.getResults()
					.stream()
					.filter(result -> normalizedLoanType == null || normalizedLoanType.equals(result.getLoanType()))
					.map(result -> loanEligibilityMapper.toHistoryItemResponse(evaluation, result))
			)
			.sorted(
				Comparator
					.comparing(LoanSenseHistoryItemResponse::evaluationDate, Comparator.nullsLast(Comparator.reverseOrder()))
					.thenComparing(item -> loanTypeOrder(item.loanType()))
			)
			.toList();
	}

	@Transactional(readOnly = true)
	public LoanSenseEvaluationResponse getEvaluationById(Long loansenseEvaluationId) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		LoanSenseEvaluation evaluation = loanEligibilityRepository
			.findByLoansenseEvaluationIdAndBankCustomer_BankCustomerId(loansenseEvaluationId, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("LoanSense evaluation not found for this bank customer."));
		return loanEligibilityMapper.toEvaluationResponse(evaluation);
	}

	private LoanTypeDetailResponse buildLoanTypeDetail(LoanSenseEvaluation evaluation, String loanType) {
		LoanEligibilityResult result = evaluation.getResults()
			.stream()
			.filter(item -> loanType.equals(item.getLoanType()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Loan result not found for the requested loan type."));

		LoanPolicy policy = loanPolicyRepository.findByLoanType(loanType).orElse(null);
		RiskAdjustment adjustment = riskAdjustmentRepository.findByRiskLevel(normalizeText(evaluation.getRiskLevel())).orElse(null);
		return loanEligibilityMapper.toDetailResponse(evaluation, result, policy, adjustment);
	}

	private LoanSenseEvaluation getOrCreateLatestEvaluation(BankCustomer bankCustomer) {
		BankCustomerFinancialRecord latestRecord = resolveLatestBankFinancialRecord(bankCustomer.getBankCustomerId());
		BankCreditEvaluation bankCreditEvaluation = resolveCurrentBankCreditEvaluation(bankCustomer);
		LoanSenseEvaluation latestEvaluation = loanEligibilityRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.orElse(null);

		if (
			latestEvaluation != null &&
			!isDependencyUpdatedAfter(
				latestEvaluation.getCreatedAt(),
				latestRecord.getUpdatedAt(),
				bankCreditEvaluation.getCreatedAt(),
				resolveLatestPolicyUpdatedAt(),
				resolveLatestRiskAdjustmentUpdatedAt()
			)
		) {
			return latestEvaluation;
		}

		return createEvaluation(bankCustomer, latestRecord, bankCreditEvaluation);
	}

	private LoanSenseEvaluation createEvaluation(
		BankCustomer bankCustomer,
		BankCustomerFinancialRecord record,
		BankCreditEvaluation bankCreditEvaluation
	) {
		Long bankRecordId = record.getBankRecordId();
		List<BankCustomerIncome> incomes = bankCustomerIncomeRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerLoan> loans = bankCustomerLoanRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerCard> cards = bankCustomerCardRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerLiability> liabilities = bankCustomerLiabilityRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		int missedPaymentsCount = bankCustomerMissedPaymentRepository
			.findByFinancialRecord_BankRecordId(bankRecordId)
			.map(BankCustomerMissedPayment::getMissedPayments)
			.orElse(0);

		BigDecimal monthlyIncome = sum(incomes.stream().map(BankCustomerIncome::getAmount).toList());
		if (monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("A positive monthly income is required before generating a LoanSense evaluation.");
		}

		BigDecimal totalExistingLoanEmi = sum(loans.stream().map(BankCustomerLoan::getMonthlyEmi).toList());
		BigDecimal leasingHirePurchasePayment = sum(liabilities.stream().map(BankCustomerLiability::getMonthlyAmount).toList());
		BigDecimal creditCardOutstanding = sum(cards.stream().map(BankCustomerCard::getOutstandingBalance).toList());
		BigDecimal creditCardLimit = sum(cards.stream().map(BankCustomerCard::getCreditLimit).toList());
		BigDecimal creditCardMinPayment = creditCardOutstanding
			.multiply(CARD_MIN_PAYMENT_RATIO)
			.setScale(2, RoundingMode.HALF_UP);

		Map<String, LoanPolicy> activePolicyMap = loanPolicyRepository
			.findAllByStatusOrderByLoanTypeAsc("ACTIVE")
			.stream()
			.collect(Collectors.toMap(policy -> normalizeText(policy.getLoanType()), Function.identity()));
		Map<String, RiskAdjustment> riskAdjustmentMap = riskAdjustmentRepository
			.findAllByOrderByRiskLevelAsc()
			.stream()
			.collect(Collectors.toMap(adjustment -> normalizeText(adjustment.getRiskLevel()), Function.identity()));

		BigDecimal maxDbrRatio = activePolicyMap
			.values()
			.stream()
			.map(LoanPolicy::getMaxDbrRatio)
			.min(Comparator.naturalOrder())
			.orElse(DEFAULT_MAX_DBR_RATIO);
		BigDecimal tmdo = totalExistingLoanEmi.add(leasingHirePurchasePayment).add(creditCardMinPayment).setScale(2, RoundingMode.HALF_UP);
		BigDecimal dbr = tmdo.divide(monthlyIncome, 4, RoundingMode.HALF_UP);
		BigDecimal maxAllowedEmi = monthlyIncome.multiply(maxDbrRatio).setScale(2, RoundingMode.HALF_UP);
		BigDecimal availableEmiCapacity = maxAllowedEmi.subtract(tmdo).setScale(2, RoundingMode.HALF_UP);

		String riskLevel = normalizeText(bankCreditEvaluation.getRiskLevel());
		RiskAdjustment riskAdjustment = riskAdjustmentMap.get(riskLevel);
		BigDecimal riskMultiplier = riskAdjustment == null ? BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP) : riskAdjustment.getMultiplier();
		int customerAge = resolveCustomerAge(bankCustomer.getUser());

		LoanSenseEvaluation evaluation = new LoanSenseEvaluation();
		evaluation.setBankCustomer(bankCustomer);
		evaluation.setBankRecord(record);
		evaluation.setBankEvaluation(bankCreditEvaluation);
		evaluation.setMonthlyIncome(monthlyIncome.setScale(2, RoundingMode.HALF_UP));
		evaluation.setTotalExistingLoanEmi(totalExistingLoanEmi.setScale(2, RoundingMode.HALF_UP));
		evaluation.setLeasingHirePurchasePayment(leasingHirePurchasePayment.setScale(2, RoundingMode.HALF_UP));
		evaluation.setCreditCardOutstanding(creditCardOutstanding.setScale(2, RoundingMode.HALF_UP));
		evaluation.setCreditCardLimit(creditCardLimit.setScale(2, RoundingMode.HALF_UP));
		evaluation.setCreditCardMinPayment(creditCardMinPayment);
		evaluation.setMissedPaymentsCount(missedPaymentsCount);
		evaluation.setTmdo(tmdo);
		evaluation.setDbr(dbr);
		evaluation.setMaxAllowedEmi(maxAllowedEmi);
		evaluation.setAvailableEmiCapacity(availableEmiCapacity);
		evaluation.setRiskLevel(riskLevel);
		evaluation.setRiskMultiplier(riskMultiplier);

		List<LoanEligibilityResult> results = new ArrayList<>();
		for (String loanType : SUPPORTED_LOAN_TYPES) {
			LoanPolicy policy = activePolicyMap.get(loanType);
			LoanEligibilityResult result = buildLoanResult(
				evaluation,
				loanType,
				policy,
				riskAdjustment,
				customerAge,
				monthlyIncome,
				dbr,
				availableEmiCapacity,
				missedPaymentsCount
			);
			results.add(result);
		}

		evaluation.setResults(results);
		evaluation.setOverallStatus(resolveOverallStatus(results));
		evaluation.setRemarks(buildRemarks(evaluation.getOverallStatus(), availableEmiCapacity, riskAdjustment));
		return loanEligibilityRepository.save(evaluation);
	}

	private LoanEligibilityResult buildLoanResult(
		LoanSenseEvaluation evaluation,
		String loanType,
		LoanPolicy policy,
		RiskAdjustment riskAdjustment,
		int customerAge,
		BigDecimal monthlyIncome,
		BigDecimal dbr,
		BigDecimal availableEmiCapacity,
		int missedPaymentsCount
	) {
		LoanEligibilityResult result = new LoanEligibilityResult();
		result.setLoanSenseEvaluation(evaluation);
		result.setLoanType(loanType);
		result.setCustomerAge(customerAge);
		result.setAssetValue(null);

		BigDecimal usableEmiCapacity = availableEmiCapacity.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
		result.setEstimatedEmi(usableEmiCapacity);
		result.setInterestRate(policy == null ? null : policy.getBaseInterestRate());
		result.setTenureMonths(policy == null ? null : policy.getMaxTenureMonths());

		List<String> blockers = new ArrayList<>();
		List<String> cautions = new ArrayList<>();

		if (policy == null) {
			blockers.add("This loan product is not currently configured as active.");
		} else {
			if (customerAge < policy.getMinAge() || customerAge > policy.getMaxAge()) {
				blockers.add("Customer age is outside the policy age range for this product.");
			}
			if (usableEmiCapacity.compareTo(BigDecimal.ZERO) <= 0) {
				blockers.add("Current debt obligations already consume the allowed EMI capacity.");
			}
			if (dbr.compareTo(policy.getMaxDbrRatio()) > 0) {
				blockers.add("Current debt burden ratio is above the allowed policy limit.");
			}
			if (policy.getMinIncomeRequired() != null && monthlyIncome.compareTo(policy.getMinIncomeRequired()) < 0) {
				cautions.add("Monthly income is below the preferred threshold for this product.");
			}
			if ("HIGH".equals(normalizeText(evaluation.getRiskLevel()))) {
				cautions.add("High credit risk reduces the recommended amount for this product.");
			}
			if (missedPaymentsCount >= 3) {
				cautions.add("Recent missed payments make the recommendation more conservative.");
			}
		}

		BigDecimal recommendedMaxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		String eligibilityStatus;
		if (!blockers.isEmpty()) {
			eligibilityStatus = "NOT_ELIGIBLE";
		} else {
			BigDecimal multiplier = riskAdjustment == null ? BigDecimal.ONE : riskAdjustment.getMultiplier();
			recommendedMaxAmount = usableEmiCapacity
				.multiply(BigDecimal.valueOf(policy.getMaxTenureMonths()))
				.multiply(multiplier)
				.setScale(2, RoundingMode.HALF_UP);
			eligibilityStatus = cautions.isEmpty() ? "ELIGIBLE" : "PARTIALLY_ELIGIBLE";
		}

		result.setEligibilityStatus(eligibilityStatus);
		result.setRecommendedMaxAmount(recommendedMaxAmount);
		result.setDecisionReason(buildDecisionReason(blockers, cautions, eligibilityStatus));
		return result;
	}

	private String buildDecisionReason(List<String> blockers, List<String> cautions, String eligibilityStatus) {
		if (!blockers.isEmpty()) {
			return String.join(" ", blockers);
		}
		if (!cautions.isEmpty()) {
			return String.join(" ", cautions);
		}
		if ("ELIGIBLE".equals(eligibilityStatus)) {
			return "Current affordability, age, and policy checks all pass for this product.";
		}
		return "Recommendation generated from the latest verified financial and credit evaluation data.";
	}

	private String resolveOverallStatus(List<LoanEligibilityResult> results) {
		boolean hasEligible = results.stream().anyMatch(result -> "ELIGIBLE".equals(result.getEligibilityStatus()));
		if (hasEligible) {
			return "ELIGIBLE";
		}
		boolean hasPartial = results.stream().anyMatch(result -> "PARTIALLY_ELIGIBLE".equals(result.getEligibilityStatus()));
		return hasPartial ? "PARTIALLY_ELIGIBLE" : "NOT_ELIGIBLE";
	}

	private String buildRemarks(String overallStatus, BigDecimal availableEmiCapacity, RiskAdjustment riskAdjustment) {
		if ("NOT_ELIGIBLE".equals(overallStatus)) {
			return availableEmiCapacity.compareTo(BigDecimal.ZERO) <= 0
				? "Current monthly obligations are already at or above the permitted EMI threshold."
				: "Current policy checks prevent an approval recommendation at this time.";
		}
		if ("PARTIALLY_ELIGIBLE".equals(overallStatus)) {
			return "Some products need more conservative limits because of income, repayment history, or credit risk conditions.";
		}
		if (riskAdjustment != null && riskAdjustment.getDescription() != null && !riskAdjustment.getDescription().isBlank()) {
			return riskAdjustment.getDescription().trim();
		}
		return "LoanSense evaluation generated from the latest verified financial record and bank credit evaluation.";
	}

	private BankCreditEvaluation resolveCurrentBankCreditEvaluation(BankCustomer bankCustomer) {
		return creditEvaluationService.getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
	}

	private BankCustomerFinancialRecord resolveLatestBankFinancialRecord(Long bankCustomerId) {
		return bankCustomerFinancialRecordRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("No financial record found for this bank customer."));
	}

	private LocalDateTime resolveLatestPolicyUpdatedAt() {
		return loanPolicyRepository.findAllByOrderByLoanTypeAsc().stream().map(LoanPolicy::getUpdatedAt).max(LocalDateTime::compareTo).orElse(null);
	}

	private LocalDateTime resolveLatestRiskAdjustmentUpdatedAt() {
		return riskAdjustmentRepository
			.findAllByOrderByRiskLevelAsc()
			.stream()
			.map(RiskAdjustment::getUpdatedAt)
			.max(LocalDateTime::compareTo)
			.orElse(null);
	}

	private boolean isDependencyUpdatedAfter(LocalDateTime evaluationCreatedAt, LocalDateTime... dependencyTimes) {
		if (evaluationCreatedAt == null) {
			return true;
		}
		for (LocalDateTime dependencyTime : dependencyTimes) {
			if (dependencyTime != null && dependencyTime.isAfter(evaluationCreatedAt)) {
				return true;
			}
		}
		return false;
	}

	private int resolveCustomerAge(User user) {
		if (user == null || user.getDob() == null) {
			throw new IllegalArgumentException("Customer date of birth is required before generating a LoanSense evaluation.");
		}
		return Period.between(user.getDob(), LocalDate.now()).getYears();
	}

	private BankCustomer resolveLoggedInBankCustomer() {
		User user = resolveAuthenticatedUser("Bank customer authentication is required.");
		String roleName = user.getRole() == null || user.getRole().getRoleName() == null
			? ""
			: user.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);
		if (!"BANK_CUSTOMER".equals(roleName)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a bank customer.");
		}
		return bankCustomerRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bank customer profile was not found for logged-in user."));
	}

	private User resolveAuthenticatedUser(String unauthenticatedMessage) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthenticatedMessage);
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		return userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal))
			.or(() -> userRepository.findByUsername(normalizedPrincipal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));
	}

	private String normalizeLoanType(String loanType) {
		String normalized = normalizeText(loanType);
		if (!SUPPORTED_LOAN_TYPE_SET.contains(normalized)) {
			throw new IllegalArgumentException("Loan type must be PERSONAL, VEHICLE, EDUCATION, or HOUSING.");
		}
		return normalized;
	}

	private int normalizePositiveMonths(Integer value) {
		if (value == null || value <= 0) {
			throw new IllegalArgumentException("Months filter must be a positive number.");
		}
		return value;
	}

	private int loanTypeOrder(String loanType) {
		return switch (normalizeText(loanType)) {
			case "PERSONAL" -> 0;
			case "VEHICLE" -> 1;
			case "EDUCATION" -> 2;
			case "HOUSING" -> 3;
			default -> 99;
		};
	}

	private BigDecimal sum(List<BigDecimal> values) {
		return values.stream().map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal safeAmount(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private String normalizeText(String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}
}
