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
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.service.CreditEvaluationService;
import com.bank_web_app.backend.loansense.dto.request.CreateLoanSenseEvaluationRequest;
import com.bank_web_app.backend.loansense.dto.request.LoanSenseLoanInputRequest;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseOfficerCustomerRowResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseOfficerDashboardResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseEvaluationResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseHistoryItemResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanTypeDetailResponse;
import com.bank_web_app.backend.loansense.entity.LoanEligibilityResult;
import com.bank_web_app.backend.loansense.entity.LoanSenseEvaluation;
import com.bank_web_app.backend.loansense.mapper.LoanEligibilityMapper;
import com.bank_web_app.backend.loansense.repository.LoanEligibilityRepository;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.notification.event.NotificationEventType;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

/**
 * Core LoanSense domain service.
 *
 * Responsibilities:
 * - Build and persist loan eligibility evaluations from latest customer financial data.
 * - Reuse the latest evaluation when upstream dependencies have not changed.
 * - Provide customer/officer views, loan-type details, and history projections.
 */
@Service
public class LoanEligibilityService {

	private static final List<String> SUPPORTED_LOAN_TYPES = List.of("PERSONAL", "VEHICLE", "EDUCATION", "HOUSING");
	private static final Set<String> SUPPORTED_LOAN_TYPE_SET = Set.copyOf(SUPPORTED_LOAN_TYPES);
	private static final BigDecimal CARD_MIN_PAYMENT_RATIO = new BigDecimal("0.05");
	private static final BigDecimal DEFAULT_MAX_DBR_RATIO = new BigDecimal("0.40");
	private static final BigDecimal MINOR_INTEREST_RATE_INCREASE_THRESHOLD = new BigDecimal("0.25");
	private static final BigDecimal MAJOR_INTEREST_RATE_INCREASE_THRESHOLD = new BigDecimal("2.00");
	private static final BigDecimal HUNDRED = new BigDecimal("100");

	private final LoanEligibilityRepository loanEligibilityRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository;
	private final BankCustomerIncomeRepository bankCustomerIncomeRepository;
	private final BankCustomerLoanRepository bankCustomerLoanRepository;
	private final BankCustomerCardRepository bankCustomerCardRepository;
	private final BankCustomerLiabilityRepository bankCustomerLiabilityRepository;
	private final BankCustomerMissedPaymentRepository bankCustomerMissedPaymentRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final LoanPolicyRepository loanPolicyRepository;
	private final RiskAdjustmentRepository riskAdjustmentRepository;
	private final UserRepository userRepository;
	private final CreditEvaluationService creditEvaluationService;
	private final LoanEligibilityMapper loanEligibilityMapper;
	private final NotificationEventPublisher notificationEventPublisher;

	public LoanEligibilityService(
		LoanEligibilityRepository loanEligibilityRepository,
		BankCustomerRepository bankCustomerRepository,
		BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository,
		BankCustomerIncomeRepository bankCustomerIncomeRepository,
		BankCustomerLoanRepository bankCustomerLoanRepository,
		BankCustomerCardRepository bankCustomerCardRepository,
		BankCustomerLiabilityRepository bankCustomerLiabilityRepository,
		BankCustomerMissedPaymentRepository bankCustomerMissedPaymentRepository,
		BankOfficerRepository bankOfficerRepository,
		LoanPolicyRepository loanPolicyRepository,
		RiskAdjustmentRepository riskAdjustmentRepository,
		UserRepository userRepository,
		CreditEvaluationService creditEvaluationService,
		LoanEligibilityMapper loanEligibilityMapper,
		NotificationEventPublisher notificationEventPublisher
	) {
		this.loanEligibilityRepository = loanEligibilityRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankCustomerFinancialRecordRepository = bankCustomerFinancialRecordRepository;
		this.bankCustomerIncomeRepository = bankCustomerIncomeRepository;
		this.bankCustomerLoanRepository = bankCustomerLoanRepository;
		this.bankCustomerCardRepository = bankCustomerCardRepository;
		this.bankCustomerLiabilityRepository = bankCustomerLiabilityRepository;
		this.bankCustomerMissedPaymentRepository = bankCustomerMissedPaymentRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.loanPolicyRepository = loanPolicyRepository;
		this.riskAdjustmentRepository = riskAdjustmentRepository;
		this.userRepository = userRepository;
		this.creditEvaluationService = creditEvaluationService;
		this.loanEligibilityMapper = loanEligibilityMapper;
		this.notificationEventPublisher = notificationEventPublisher;
	}

	@Transactional
	// Returns the latest eligibility evaluation for the logged-in customer.
	public LoanSenseEvaluationResponse getCurrentEvaluation() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		return loanEligibilityMapper.toEvaluationResponse(getOrCreateLatestEvaluation(bankCustomer));
	}

	@Transactional
	// Returns detailed metrics for one loan type in the latest evaluation.
	public LoanTypeDetailResponse getCurrentLoanTypeDetail(String loanType) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		LoanSenseEvaluation evaluation = getOrCreateLatestEvaluation(bankCustomer);
		return buildLoanTypeDetail(evaluation, normalizeLoanType(loanType));
	}

	@Transactional
	// Returns eligibility history for the logged-in customer.
	public List<LoanSenseHistoryItemResponse> getHistory(String loanType, Integer months) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		getOrCreateLatestEvaluation(bankCustomer);
		return buildHistoryResponses(bankCustomer.getBankCustomerId(), loanType, months);
	}

	@Transactional(readOnly = true)
	// Returns one evaluation record by id for the logged-in customer.
	public LoanSenseEvaluationResponse getEvaluationById(Long loansenseEvaluationId) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		LoanSenseEvaluation evaluation = loanEligibilityRepository
			.findByLoansenseEvaluationIdAndBankCustomer_BankCustomerId(loansenseEvaluationId, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("LoanSense evaluation not found for this bank customer."));
		return loanEligibilityMapper.toEvaluationResponse(evaluation);
	}

	@Transactional
	// Creates a LoanSense evaluation for a customer managed by the logged-in officer.
	public LoanSenseEvaluationResponse createEvaluationForOfficer(Long bankCustomerId, CreateLoanSenseEvaluationRequest request) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCustomerFinancialRecord latestRecord = resolveLatestBankFinancialRecord(bankCustomer.getBankCustomerId());
		BankCreditEvaluation bankCreditEvaluation = resolveCurrentBankCreditEvaluation(bankCustomer);
		String previousStatus = loanEligibilityRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.map(LoanSenseEvaluation::getOverallStatus)
			.orElse(null);
		LoanSenseEvaluation evaluation = createEvaluation(
			bankCustomer,
			latestRecord,
			bankCreditEvaluation,
			parseRequestedLoanInputs(request)
		);
		publishLoanSenseEvaluationNotification(bankCustomer, officer, evaluation, previousStatus);
		return loanEligibilityMapper.toEvaluationResponse(evaluation);
	}

	@Transactional
	// Returns the latest evaluation for an officer-managed customer.
	public LoanSenseEvaluationResponse getCurrentEvaluationForOfficer(Long bankCustomerId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		return loanEligibilityMapper.toEvaluationResponse(getOrCreateLatestEvaluation(bankCustomer));
	}

	@Transactional
	// Returns eligibility history for an officer-managed customer.
	public List<LoanSenseHistoryItemResponse> getHistoryForOfficer(Long bankCustomerId, String loanType, Integer months) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		getOrCreateLatestEvaluation(bankCustomer);
		return buildHistoryResponses(bankCustomer.getBankCustomerId(), loanType, months);
	}

	@Transactional(readOnly = true)
	// Returns one evaluation record by id for an officer-managed customer.
	public LoanSenseEvaluationResponse getEvaluationByIdForOfficer(Long bankCustomerId, Long loansenseEvaluationId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		LoanSenseEvaluation evaluation = loanEligibilityRepository
			.findByLoansenseEvaluationIdAndBankCustomer_BankCustomerId(loansenseEvaluationId, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("LoanSense evaluation not found for this bank customer."));
		return loanEligibilityMapper.toEvaluationResponse(evaluation);
	}

	@Transactional
	// Returns loan-type detail for an officer-managed customer.
	public LoanTypeDetailResponse getLoanTypeDetailForOfficer(Long bankCustomerId, String loanType) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		LoanSenseEvaluation evaluation = getOrCreateLatestEvaluation(bankCustomer);
		return buildLoanTypeDetail(evaluation, normalizeLoanType(loanType));
	}

	@Transactional
	// Returns LoanSense dashboard rows for the logged-in officer.
	public LoanSenseOfficerDashboardResponse getOfficerDashboard() {
		BankOfficer officer = resolveLoggedInBankOfficer();
		List<LoanSenseOfficerCustomerRowResponse> rows = bankCustomerRepository
			.findAllByOfficer_OfficerIdOrderByUpdatedAtDesc(officer.getOfficerId())
			.stream()
			.map(this::toOfficerCustomerRow)
			.toList();

		int evaluatedCustomers = (int) rows.stream().filter(row -> !"NOT_EVALUATED".equals(row.overallStatus())).count();
		int eligibleCustomers = (int) rows.stream().filter(row -> "ELIGIBLE".equals(row.overallStatus())).count();
		int partiallyEligibleCustomers = (int) rows.stream().filter(row -> "PARTIALLY_ELIGIBLE".equals(row.overallStatus())).count();
		int notEligibleCustomers = (int) rows.stream().filter(row -> "NOT_ELIGIBLE".equals(row.overallStatus())).count();

		return new LoanSenseOfficerDashboardResponse(
			rows.size(),
			evaluatedCustomers,
			eligibleCustomers,
			partiallyEligibleCustomers,
			notEligibleCustomers,
			rows
		);
	}

	private List<LoanSenseHistoryItemResponse> buildHistoryResponses(Long bankCustomerId, String loanType, Integer months) {
		String normalizedLoanType = loanType == null || loanType.isBlank() ? null : normalizeLoanType(loanType);
		LocalDateTime threshold = months == null ? null : LocalDateTime.now().minusMonths(normalizePositiveMonths(months));

		return loanEligibilityRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
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

	private LoanSenseOfficerCustomerRowResponse toOfficerCustomerRow(BankCustomer customer) {
		LoanSenseEvaluation evaluation = resolveLatestEvaluationForDashboard(customer);
		if (evaluation == null) {
			return new LoanSenseOfficerCustomerRowResponse(
				customer.getBankCustomerId(),
				customer.getCustomerCode(),
				buildFullName(customer),
				null,
				"NOT_EVALUATED",
				"Not Evaluated",
				null,
				null,
				null,
				null,
				null
			);
		}

		BigDecimal maxRecommendedAmount = evaluation.getResults()
			.stream()
			.map(LoanEligibilityResult::getRecommendedMaxAmount)
			.filter(Objects::nonNull)
			.max(Comparator.naturalOrder())
			.orElse(null);

		return new LoanSenseOfficerCustomerRowResponse(
			customer.getBankCustomerId(),
			customer.getCustomerCode(),
			buildFullName(customer),
			evaluation.getLoansenseEvaluationId(),
			evaluation.getOverallStatus(),
			toEligibilityLabel(evaluation.getOverallStatus()),
			evaluation.getRiskLevel(),
			toRiskLabel(evaluation.getRiskLevel()),
			maxRecommendedAmount,
			evaluation.getAvailableEmiCapacity(),
			evaluation.getCreatedAt()
		);
	}

	private LoanSenseEvaluation resolveLatestEvaluationForDashboard(BankCustomer customer) {
		LoanSenseEvaluation latestEvaluation = loanEligibilityRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(customer.getBankCustomerId())
			.orElse(null);
		if (latestEvaluation == null) {
			return null;
		}
		return reconcileOverallStatus(latestEvaluation);
	}

	private String buildFullName(BankCustomer customer) {
		User user = customer.getUser();
		if (user == null) {
			return customer.getCustomerCode();
		}
		String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
		String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
		String fullName = (firstName + " " + lastName).trim();
		return fullName.isEmpty() ? customer.getCustomerCode() : fullName;
	}

	private String toEligibilityLabel(String status) {
		return switch (normalizeText(status)) {
			case "ELIGIBLE" -> "Eligible";
			case "PARTIALLY_ELIGIBLE" -> "Partially Eligible";
			case "NOT_ELIGIBLE" -> "Not Eligible";
			default -> "Unknown";
		};
	}

	private String toRiskLabel(String riskLevel) {
		return switch (normalizeText(riskLevel)) {
			case "LOW" -> "Low Risk";
			case "MEDIUM" -> "Medium Risk";
			case "HIGH" -> "High Risk";
			default -> "";
		};
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
			return reconcileOverallStatus(latestEvaluation);
		}

		return createEvaluation(
			bankCustomer,
			latestRecord,
			bankCreditEvaluation,
			LoanRequestInput.forAllLoanTypes()
		);
	}

	private LoanSenseEvaluation reconcileOverallStatus(LoanSenseEvaluation evaluation) {
		String resolvedOverallStatus = resolveOverallStatus(evaluation.getResults());
		String currentOverallStatus = normalizeText(evaluation.getOverallStatus());
		if (resolvedOverallStatus.equals(currentOverallStatus)) {
			return evaluation;
		}

		evaluation.setOverallStatus(resolvedOverallStatus);
		RiskAdjustment riskAdjustment = riskAdjustmentRepository.findByRiskLevel(normalizeText(evaluation.getRiskLevel())).orElse(null);
		evaluation.setRemarks(buildRemarks(resolvedOverallStatus, safeAmount(evaluation.getAvailableEmiCapacity()), riskAdjustment));
		return loanEligibilityRepository.save(evaluation);
	}

	private LoanSenseEvaluation createEvaluation(
		BankCustomer bankCustomer,
		BankCustomerFinancialRecord record,
		BankCreditEvaluation bankCreditEvaluation,
		LoanRequestInput requestInput
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
		validateFinancialInputs(totalExistingLoanEmi, leasingHirePurchasePayment, creditCardOutstanding, creditCardLimit, missedPaymentsCount);
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
		Map<String, BigDecimal> previousInterestRatesByLoanType = loanEligibilityRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.map(this::extractInterestRatesByLoanType)
			.orElse(Map.of());

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

		Set<String> loanTypesToEvaluate = requestInput.requestedLoanTypes().isEmpty()
			? new LinkedHashSet<>(SUPPORTED_LOAN_TYPES)
			: requestInput.requestedLoanTypes();

		List<LoanEligibilityResult> results = new ArrayList<>();
		for (String loanType : SUPPORTED_LOAN_TYPES) {
			if (!loanTypesToEvaluate.contains(loanType)) {
				continue;
			}
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
				missedPaymentsCount,
				requestInput.assetValuesByLoanType().get(loanType),
				previousInterestRatesByLoanType.get(loanType)
			);
			results.add(result);
		}

		evaluation.setResults(results);
		evaluation.setOverallStatus(resolveOverallStatus(results));
		evaluation.setRemarks(buildRemarks(evaluation.getOverallStatus(), availableEmiCapacity, riskAdjustment));
		return loanEligibilityRepository.save(evaluation);
	}

	private void publishLoanSenseEvaluationNotification(
		BankCustomer bankCustomer,
		BankOfficer officer,
		LoanSenseEvaluation evaluation,
		String previousStatus
	) {
		Map<String, String> metadata = new java.util.LinkedHashMap<>();
		metadata.put("evaluationId", String.valueOf(evaluation.getLoansenseEvaluationId()));
		metadata.put("customerId", String.valueOf(bankCustomer.getBankCustomerId()));
		metadata.put("officerUserId", String.valueOf(officer.getUser().getUserId()));
		metadata.put("overallStatus", normalizeText(evaluation.getOverallStatus()));
		if (previousStatus != null && !previousStatus.isBlank()) {
			metadata.put("previousStatus", previousStatus);
		}
		notificationEventPublisher.publish(
			NotificationEventType.LOANSENSE_EVALUATED,
			bankCustomer.getUser().getUserId(),
			officer.getUser().getUserId(),
			evaluation.getLoansenseEvaluationId(),
			metadata
		);
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
		int missedPaymentsCount,
		BigDecimal assetValue,
		BigDecimal previousInterestRate
	) {
		LoanEligibilityResult result = new LoanEligibilityResult();
		result.setLoanSenseEvaluation(evaluation);
		result.setLoanType(loanType);
		result.setCustomerAge(customerAge);
		BigDecimal normalizedAssetValue = assetValue == null ? null : assetValue.setScale(2, RoundingMode.HALF_UP);
		result.setAssetValue(normalizedAssetValue);

		BigDecimal usableEmiCapacity = availableEmiCapacity.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
		result.setEstimatedEmi(usableEmiCapacity);
		result.setInterestRate(policy == null ? null : policy.getBaseInterestRate());

		List<String> blockers = new ArrayList<>();
		List<String> cautions = new ArrayList<>();
		Integer adjustedTenureMonths = null;

		if (policy == null) {
			blockers.add("This loan product is not currently configured as active.");
		} else {
			if (customerAge < policy.getMinAge() || customerAge > policy.getMaxAge()) {
				blockers.add("Customer age is outside the policy age range for this product.");
			}

			adjustedTenureMonths = resolveAdjustedTenureMonths(policy, customerAge);
			if (adjustedTenureMonths <= 0) {
				blockers.add("Loan tenure exceeds the policy age-at-maturity limit for this customer.");
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
			if ("VEHICLE".equals(loanType) && normalizedAssetValue == null) {
				cautions.add("Vehicle value was not provided; EMI-based recommendation is used.");
			}

			if (previousInterestRate != null && policy.getBaseInterestRate() != null) {
				BigDecimal rateIncrease = policy.getBaseInterestRate().subtract(previousInterestRate);
				if (rateIncrease.compareTo(MAJOR_INTEREST_RATE_INCREASE_THRESHOLD) >= 0) {
					blockers.add("Base interest rate increased significantly since the previous evaluation.");
				} else if (rateIncrease.compareTo(MINOR_INTEREST_RATE_INCREASE_THRESHOLD) >= 0) {
					cautions.add("Base interest rate increased since the previous evaluation.");
				}
			}
		}

		BigDecimal recommendedMaxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		String eligibilityStatus;
		if (!blockers.isEmpty()) {
			eligibilityStatus = "NOT_ELIGIBLE";
		} else {
			BigDecimal multiplier = riskAdjustment == null ? BigDecimal.ONE : safeAmount(riskAdjustment.getMultiplier());
			if (multiplier.compareTo(BigDecimal.ZERO) <= 0) {
				multiplier = BigDecimal.ONE;
			}

			BigDecimal grossAmount = usableEmiCapacity
				.multiply(BigDecimal.valueOf(adjustedTenureMonths))
				.multiply(multiplier)
				.setScale(2, RoundingMode.HALF_UP);
			BigDecimal principalAmount = convertGrossToPrincipal(grossAmount, policy.getBaseInterestRate());
			recommendedMaxAmount = applyAssetCap(principalAmount, normalizedAssetValue, policy);
			if (recommendedMaxAmount.compareTo(BigDecimal.ZERO) <= 0) {
				blockers.add("Calculated affordable amount is too low under current policy and risk settings.");
				eligibilityStatus = "NOT_ELIGIBLE";
				recommendedMaxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
				result.setTenureMonths(adjustedTenureMonths);
				result.setEligibilityStatus(eligibilityStatus);
				result.setRecommendedMaxAmount(recommendedMaxAmount);
				result.setDecisionReason(buildDecisionReason(blockers, cautions, eligibilityStatus));
				return result;
			}
			eligibilityStatus = cautions.isEmpty() ? "ELIGIBLE" : "PARTIALLY_ELIGIBLE";
		}

		result.setTenureMonths(adjustedTenureMonths);
		result.setEligibilityStatus(eligibilityStatus);
		result.setRecommendedMaxAmount(recommendedMaxAmount);
		result.setDecisionReason(buildDecisionReason(blockers, cautions, eligibilityStatus));
		return result;
	}

	private LoanRequestInput parseRequestedLoanInputs(CreateLoanSenseEvaluationRequest request) {
		if (request == null || request.loans() == null || request.loans().isEmpty()) {
			return LoanRequestInput.forAllLoanTypes();
		}

		Set<String> requestedLoanTypes = new LinkedHashSet<>();
		Map<String, BigDecimal> assetValuesByLoanType = new java.util.HashMap<>();
		for (LoanSenseLoanInputRequest item : request.loans()) {
			if (item == null) {
				throw new IllegalArgumentException("Loan request entries must not be null.");
			}

			String loanType = normalizeLoanType(item.loanType());
			if (!requestedLoanTypes.add(loanType)) {
				throw new IllegalArgumentException("Duplicate loan type found in request payload.");
			}

			BigDecimal assetValue = item.assetValue();
			if (assetValue != null && assetValue.compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Asset value must not be negative.");
			}
			assetValuesByLoanType.put(loanType, assetValue == null ? null : assetValue.setScale(2, RoundingMode.HALF_UP));
		}

		if (requestedLoanTypes.isEmpty()) {
			throw new IllegalArgumentException("At least one loan type must be requested.");
		}

		return new LoanRequestInput(requestedLoanTypes, assetValuesByLoanType);
	}

	private int resolveAdjustedTenureMonths(LoanPolicy policy, int customerAge) {
		if (policy == null || policy.getMaxTenureMonths() == null || policy.getMaxTenureMonths() <= 0) {
			return 0;
		}
		int remainingYears = policy.getMaxAge() - customerAge;
		if (remainingYears <= 0) {
			return 0;
		}
		int ageBoundTenureMonths = remainingYears * 12;
		return Math.max(0, Math.min(policy.getMaxTenureMonths(), ageBoundTenureMonths));
	}

	private BigDecimal convertGrossToPrincipal(BigDecimal grossAmount, BigDecimal interestRate) {
		BigDecimal normalizedGross = safeAmount(grossAmount).setScale(2, RoundingMode.HALF_UP);
		if (normalizedGross.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) <= 0) {
			return normalizedGross;
		}
		BigDecimal denominator = HUNDRED.add(interestRate);
		if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
			return normalizedGross;
		}
		return normalizedGross.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
	}

	private BigDecimal applyAssetCap(BigDecimal principalAmount, BigDecimal assetValue, LoanPolicy policy) {
		BigDecimal amount = safeAmount(principalAmount).setScale(2, RoundingMode.HALF_UP);
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (policy == null || policy.getMaxFinancePercentage() == null || assetValue == null) {
			return amount;
		}

		BigDecimal financeRatio = policy.getMaxFinancePercentage().divide(HUNDRED, 4, RoundingMode.HALF_UP);
		BigDecimal assetCap = safeAmount(assetValue).multiply(financeRatio).setScale(2, RoundingMode.HALF_UP);
		if (assetCap.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return amount.min(assetCap).setScale(2, RoundingMode.HALF_UP);
	}

	private void validateFinancialInputs(
		BigDecimal totalExistingLoanEmi,
		BigDecimal leasingHirePurchasePayment,
		BigDecimal creditCardOutstanding,
		BigDecimal creditCardLimit,
		int missedPaymentsCount
	) {
		if (safeAmount(totalExistingLoanEmi).compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Total existing loan EMIs must not be negative.");
		}
		if (safeAmount(leasingHirePurchasePayment).compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Leasing or hire-purchase payment must not be negative.");
		}
		if (safeAmount(creditCardOutstanding).compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Credit-card outstanding balance must not be negative.");
		}
		if (safeAmount(creditCardLimit).compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Credit-card limit must not be negative.");
		}
		if (missedPaymentsCount < 0) {
			throw new IllegalArgumentException("Missed payments count must not be negative.");
		}
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
		if (results == null || results.isEmpty()) {
			return "NOT_ELIGIBLE";
		}

		int eligibleCount = 0;
		int partialCount = 0;
		int notEligibleCount = 0;

		for (LoanEligibilityResult result : results) {
			String normalizedStatus = normalizeText(result == null ? null : result.getEligibilityStatus());
			switch (normalizedStatus) {
				case "ELIGIBLE" -> eligibleCount += 1;
				case "PARTIALLY_ELIGIBLE" -> partialCount += 1;
				case "NOT_ELIGIBLE" -> notEligibleCount += 1;
				default -> partialCount += 1;
			}
		}

		int total = results.size();

		if (eligibleCount == total) {
			return "ELIGIBLE";
		}
		if (partialCount == total) {
			return "PARTIALLY_ELIGIBLE";
		}
		if (notEligibleCount == total) {
			return "NOT_ELIGIBLE";
		}

		// If one or more NOT_ELIGIBLE exists together with other statuses, treat overall as partial.
		if (notEligibleCount > 0) {
			return "PARTIALLY_ELIGIBLE";
		}

		// Mixed ELIGIBLE + PARTIALLY_ELIGIBLE (no NOT_ELIGIBLE):
		// ELIGIBLE wins on tie and majority, otherwise PARTIALLY_ELIGIBLE.
		return eligibleCount >= partialCount ? "ELIGIBLE" : "PARTIALLY_ELIGIBLE";
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

	private BankOfficer resolveLoggedInBankOfficer() {
		User user = resolveAuthenticatedUser("Bank officer authentication is required.");
		return bankOfficerRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a bank officer."));
	}

	private BankCustomer resolveOwnedBankCustomer(Long bankCustomerId, BankOfficer officer) {
		BankCustomer bankCustomer = bankCustomerRepository
			.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		if (bankCustomer.getOfficer() == null || !bankCustomer.getOfficer().getOfficerId().equals(officer.getOfficerId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This bank customer is not assigned to the logged-in bank officer.");
		}
		return bankCustomer;
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

	private Map<String, BigDecimal> extractInterestRatesByLoanType(LoanSenseEvaluation evaluation) {
		return evaluation
			.getResults()
			.stream()
			.filter(result -> result.getLoanType() != null)
			.filter(result -> result.getInterestRate() != null)
			.collect(
				Collectors.toMap(
					result -> normalizeText(result.getLoanType()),
					LoanEligibilityResult::getInterestRate,
					(existing, replacement) -> replacement
				)
			);
	}

	private record LoanRequestInput(
		Set<String> requestedLoanTypes,
		Map<String, BigDecimal> assetValuesByLoanType
	) {
		static LoanRequestInput forAllLoanTypes() {
			return new LoanRequestInput(Set.of(), Map.of());
		}
	}
}

