package com.bank_web_app.backend.creditlens.service;

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
import com.bank_web_app.backend.creditlens.dto.request.CreateBankCreditEvaluationRequest;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisCustomerProfileResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisCustomerRowResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisDashboardResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditEvaluationResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditEvaluationSummaryResponse;
import com.bank_web_app.backend.creditlens.dto.response.SelfCreditEvaluationResponse;
import com.bank_web_app.backend.creditlens.dto.response.SelfCreditEvaluationSummaryResponse;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.entity.SelfCreditEvaluation;
import com.bank_web_app.backend.creditlens.mapper.CreditEvaluationMapper;
import com.bank_web_app.backend.creditlens.repository.BankCreditEvaluationRepository;
import com.bank_web_app.backend.creditlens.repository.SelfCreditEvaluationRepository;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerCard;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLiability;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLoan;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerMissedPayment;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerCardRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerFinancialRecordRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerIncomeRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLiabilityRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLoanRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerMissedPaymentRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreditEvaluationService {

	private static final Set<String> BANK_EVALUATION_SOURCES = Set.of("MANUAL", "CRIB_MERGED", "CRIB_ONLY");

	private final SelfCreditEvaluationRepository selfCreditEvaluationRepository;
	private final BankCreditEvaluationRepository bankCreditEvaluationRepository;
	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final PublicCustomerFinancialRecordRepository publicCustomerFinancialRecordRepository;
	private final PublicCustomerIncomeRepository publicCustomerIncomeRepository;
	private final PublicCustomerLoanRepository publicCustomerLoanRepository;
	private final PublicCustomerCardRepository publicCustomerCardRepository;
	private final PublicCustomerLiabilityRepository publicCustomerLiabilityRepository;
	private final PublicCustomerMissedPaymentRepository publicCustomerMissedPaymentRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository;
	private final BankCustomerIncomeRepository bankCustomerIncomeRepository;
	private final BankCustomerLoanRepository bankCustomerLoanRepository;
	private final BankCustomerCardRepository bankCustomerCardRepository;
	private final BankCustomerLiabilityRepository bankCustomerLiabilityRepository;
	private final BankCustomerMissedPaymentRepository bankCustomerMissedPaymentRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final UserRepository userRepository;
	private final CreditEvaluationMapper creditEvaluationMapper;

	public CreditEvaluationService(
		SelfCreditEvaluationRepository selfCreditEvaluationRepository,
		BankCreditEvaluationRepository bankCreditEvaluationRepository,
		PublicCustomerProfileRepository publicCustomerProfileRepository,
		PublicCustomerFinancialRecordRepository publicCustomerFinancialRecordRepository,
		PublicCustomerIncomeRepository publicCustomerIncomeRepository,
		PublicCustomerLoanRepository publicCustomerLoanRepository,
		PublicCustomerCardRepository publicCustomerCardRepository,
		PublicCustomerLiabilityRepository publicCustomerLiabilityRepository,
		PublicCustomerMissedPaymentRepository publicCustomerMissedPaymentRepository,
		BankCustomerRepository bankCustomerRepository,
		BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository,
		BankCustomerIncomeRepository bankCustomerIncomeRepository,
		BankCustomerLoanRepository bankCustomerLoanRepository,
		BankCustomerCardRepository bankCustomerCardRepository,
		BankCustomerLiabilityRepository bankCustomerLiabilityRepository,
		BankCustomerMissedPaymentRepository bankCustomerMissedPaymentRepository,
		BankOfficerRepository bankOfficerRepository,
		UserRepository userRepository,
		CreditEvaluationMapper creditEvaluationMapper
	) {
		this.selfCreditEvaluationRepository = selfCreditEvaluationRepository;
		this.bankCreditEvaluationRepository = bankCreditEvaluationRepository;
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
		this.publicCustomerFinancialRecordRepository = publicCustomerFinancialRecordRepository;
		this.publicCustomerIncomeRepository = publicCustomerIncomeRepository;
		this.publicCustomerLoanRepository = publicCustomerLoanRepository;
		this.publicCustomerCardRepository = publicCustomerCardRepository;
		this.publicCustomerLiabilityRepository = publicCustomerLiabilityRepository;
		this.publicCustomerMissedPaymentRepository = publicCustomerMissedPaymentRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankCustomerFinancialRecordRepository = bankCustomerFinancialRecordRepository;
		this.bankCustomerIncomeRepository = bankCustomerIncomeRepository;
		this.bankCustomerLoanRepository = bankCustomerLoanRepository;
		this.bankCustomerCardRepository = bankCustomerCardRepository;
		this.bankCustomerLiabilityRepository = bankCustomerLiabilityRepository;
		this.bankCustomerMissedPaymentRepository = bankCustomerMissedPaymentRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.userRepository = userRepository;
		this.creditEvaluationMapper = creditEvaluationMapper;
	}

	@Transactional
	public SelfCreditEvaluationResponse createSelfEvaluation() {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		PublicCustomerFinancialRecord record = resolveCurrentPublicFinancialRecord(profile.getPublicCustomerId());
		return creditEvaluationMapper.toSelfResponse(createSelfEvaluation(profile, record));
	}

	@Transactional
	public SelfCreditEvaluationResponse getCurrentSelfEvaluation() {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		return creditEvaluationMapper.toSelfResponse(getOrCreateLatestSelfEvaluation(profile));
	}

	@Transactional
	public List<SelfCreditEvaluationSummaryResponse> getSelfEvaluationHistory() {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		getOrCreateLatestSelfEvaluation(profile);
		return selfCreditEvaluationRepository
			.findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(profile.getPublicCustomerId())
			.stream()
			.map(creditEvaluationMapper::toSelfSummary)
			.toList();
	}

	@Transactional(readOnly = true)
	public SelfCreditEvaluationResponse getSelfEvaluationById(Long selfEvaluationId) {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		SelfCreditEvaluation evaluation = selfCreditEvaluationRepository
			.findBySelfEvaluationIdAndPublicCustomer_PublicCustomerId(selfEvaluationId, profile.getPublicCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Self credit evaluation not found for this public customer."));
		return creditEvaluationMapper.toSelfResponse(evaluation);
	}

	@Transactional
	public BankCreditEvaluationResponse getCurrentBankEvaluationForCustomer() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		BankCreditEvaluation evaluation = getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return creditEvaluationMapper.toBankResponse(evaluation);
	}

	@Transactional(readOnly = true)
	public List<BankCreditEvaluationSummaryResponse> getBankEvaluationHistoryForCustomer() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		return bankCreditEvaluationRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.stream()
			.map(creditEvaluationMapper::toBankSummary)
			.toList();
	}

	@Transactional(readOnly = true)
	public BankCreditEvaluationResponse getBankEvaluationByIdForCustomer(Long bankEvaluationId) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		BankCreditEvaluation evaluation = bankCreditEvaluationRepository
			.findByBankEvaluationIdAndBankCustomer_BankCustomerId(bankEvaluationId, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Bank credit evaluation not found for this bank customer."));
		return creditEvaluationMapper.toBankResponse(evaluation);
	}

	@Transactional
	public BankCreditAnalysisDashboardResponse getOfficerDashboard() {
		BankOfficer officer = resolveLoggedInBankOfficer();
		List<BankCreditAnalysisCustomerRowResponse> rows = bankCustomerRepository
			.findAllByOfficer_OfficerIdOrderByUpdatedAtDesc(officer.getOfficerId())
			.stream()
			.map(customer -> {
				try {
					return getOrCreateLatestBankEvaluation(customer, officer);
				} catch (IllegalArgumentException ex) {
					return null;
				}
			})
			.filter(Objects::nonNull)
			.map(creditEvaluationMapper::toDashboardRow)
			.toList();

		int lowRiskCount = (int) rows.stream().filter(row -> "LOW".equalsIgnoreCase(row.riskLevel())).count();
		int mediumRiskCount = (int) rows.stream().filter(row -> "MEDIUM".equalsIgnoreCase(row.riskLevel())).count();
		int highRiskCount = (int) rows.stream().filter(row -> "HIGH".equalsIgnoreCase(row.riskLevel())).count();

		return new BankCreditAnalysisDashboardResponse(
			rows.size(),
			lowRiskCount,
			mediumRiskCount,
			highRiskCount,
			rows
		);
	}

	@Transactional(readOnly = true)
	public BankCreditAnalysisCustomerProfileResponse getOfficerCustomerProfile(Long bankCustomerId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCreditEvaluation latestEvaluation = bankCreditEvaluationRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.orElse(null);

		User user = bankCustomer.getUser();
		return new BankCreditAnalysisCustomerProfileResponse(
			bankCustomer.getBankCustomerId(),
			user.getUserId(),
			bankCustomer.getCustomerCode(),
			buildFullName(user),
			safe(user.getNic()),
			safe(user.getEmail()),
			safe(user.getPhone()),
			safe(user.getStatus()),
			bankCustomer.getAccount().getAccountNumber(),
			bankCustomer.getAccount().getAccountType(),
			bankCustomer.getAccount().getStatus(),
			bankCustomer.getOfficer().getOfficerId(),
			bankCustomer.getBranch().getBranchId(),
			latestEvaluation == null ? null : latestEvaluation.getBankEvaluationId(),
			latestEvaluation == null ? null : latestEvaluation.getTotalRiskPoints(),
			latestEvaluation == null ? null : latestEvaluation.getRiskLevel(),
			latestEvaluation == null ? null : toTitleCase(latestEvaluation.getRiskLevel()),
			latestEvaluation == null ? null : latestEvaluation.getCreatedAt()
		);
	}

	@Transactional
	public BankCreditEvaluationResponse createBankEvaluationForOfficer(
		Long bankCustomerId,
		CreateBankCreditEvaluationRequest request
	) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCustomerFinancialRecord record = resolveLatestBankFinancialRecord(bankCustomer.getBankCustomerId());
		String evaluationSource = normalizeBankEvaluationSource(request == null ? null : request.evaluationSource());
		String remarks = normalizeOptionalText(request == null ? null : request.remarks());
		return creditEvaluationMapper.toBankResponse(createBankEvaluation(bankCustomer, record, officer, evaluationSource, remarks));
	}

	@Transactional
	public BankCreditEvaluationResponse getCurrentBankEvaluationForOfficer(Long bankCustomerId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		return creditEvaluationMapper.toBankResponse(getOrCreateLatestBankEvaluationForCustomer(bankCustomer));
	}

	@Transactional
	public BankCreditEvaluation getOrCreateLatestBankEvaluationForCustomer(BankCustomer bankCustomer) {
		if (bankCustomer == null || bankCustomer.getBankCustomerId() == null) {
			throw new IllegalArgumentException("Bank customer is required to generate a bank credit evaluation.");
		}
		if (bankCustomer.getOfficer() == null || bankCustomer.getOfficer().getOfficerId() == null) {
			throw new IllegalArgumentException("Bank customer must be assigned to a bank officer before generating a bank credit evaluation.");
		}
		return getOrCreateLatestBankEvaluation(bankCustomer, bankCustomer.getOfficer());
	}

	@Transactional
	public List<BankCreditEvaluationSummaryResponse> getBankEvaluationHistoryForOfficer(Long bankCustomerId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		getOrCreateLatestBankEvaluation(bankCustomer, officer);
		return bankCreditEvaluationRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.stream()
			.map(creditEvaluationMapper::toBankSummary)
			.toList();
	}

	@Transactional(readOnly = true)
	public BankCreditEvaluationResponse getBankEvaluationByIdForOfficer(Long bankCustomerId, Long bankEvaluationId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCreditEvaluation evaluation = bankCreditEvaluationRepository
			.findByBankEvaluationIdAndBankCustomer_BankCustomerId(bankEvaluationId, bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank credit evaluation not found for this bank customer."));
		return creditEvaluationMapper.toBankResponse(evaluation);
	}

	private SelfCreditEvaluation getOrCreateLatestSelfEvaluation(PublicCustomerProfile profile) {
		PublicCustomerFinancialRecord currentRecord = resolveCurrentPublicFinancialRecord(profile.getPublicCustomerId());
		SelfCreditEvaluation latestEvaluation = selfCreditEvaluationRepository
			.findTopByPublicRecord_RecordIdOrderByCreatedAtDesc(currentRecord.getRecordId())
			.orElse(null);

		if (latestEvaluation != null && !isRecordUpdatedAfterEvaluation(currentRecord.getUpdatedAt(), latestEvaluation.getCreatedAt())) {
			return latestEvaluation;
		}

		return createSelfEvaluation(profile, currentRecord);
	}

	private SelfCreditEvaluation createSelfEvaluation(
		PublicCustomerProfile profile,
		PublicCustomerFinancialRecord record
	) {
		EvaluationMetrics metrics = buildPublicEvaluationMetrics(record);

		SelfCreditEvaluation evaluation = new SelfCreditEvaluation();
		evaluation.setPublicCustomer(profile);
		evaluation.setPublicRecord(record);
		applyCommonMetricsToSelfEvaluation(evaluation, metrics);
		return selfCreditEvaluationRepository.save(evaluation);
	}

	private BankCreditEvaluation getOrCreateLatestBankEvaluation(BankCustomer bankCustomer, BankOfficer officer) {
		BankCustomerFinancialRecord latestRecord = resolveLatestBankFinancialRecord(bankCustomer.getBankCustomerId());
		BankCreditEvaluation latestEvaluation = bankCreditEvaluationRepository
			.findTopByBankRecord_BankRecordIdOrderByCreatedAtDesc(latestRecord.getBankRecordId())
			.orElse(null);

		if (latestEvaluation != null && !isRecordUpdatedAfterEvaluation(latestRecord.getUpdatedAt(), latestEvaluation.getCreatedAt())) {
			return latestEvaluation;
		}

		return createBankEvaluation(bankCustomer, latestRecord, officer, "MANUAL", null);
	}

	private BankCreditEvaluation createBankEvaluation(
		BankCustomer bankCustomer,
		BankCustomerFinancialRecord record,
		BankOfficer officer,
		String evaluationSource,
		String remarks
	) {
		EvaluationMetrics metrics = buildBankEvaluationMetrics(record);

		BankCreditEvaluation evaluation = new BankCreditEvaluation();
		evaluation.setBankCustomer(bankCustomer);
		evaluation.setBankRecord(record);
		evaluation.setEvaluatedByOfficer(officer);
		evaluation.setEvaluationSource(evaluationSource);
		evaluation.setRemarks(remarks);
		applyCommonMetricsToBankEvaluation(evaluation, metrics);
		return bankCreditEvaluationRepository.save(evaluation);
	}

	private void applyCommonMetricsToSelfEvaluation(SelfCreditEvaluation evaluation, EvaluationMetrics metrics) {
		evaluation.setTotalRiskPoints(metrics.totalRiskPoints());
		evaluation.setRiskLevel(metrics.riskLevel());
		evaluation.setTotalMonthlyIncome(metrics.totalMonthlyIncome());
		evaluation.setTotalMonthlyDebtPayment(metrics.totalMonthlyDebtPayment());
		evaluation.setTotalCardLimit(metrics.totalCardLimit());
		evaluation.setTotalCardOutstanding(metrics.totalCardOutstanding());
		evaluation.setDtiRatio(metrics.dtiRatio());
		evaluation.setCreditUtilizationRatio(metrics.creditUtilizationRatio());
		evaluation.setActiveFacilitiesCount(metrics.activeFacilitiesCount());
		evaluation.setMissedPaymentsCount(metrics.missedPaymentsCount());
		evaluation.setPaymentHistoryPoints(metrics.paymentHistoryPoints());
		evaluation.setDtiPoints(metrics.dtiPoints());
		evaluation.setUtilizationPoints(metrics.utilizationPoints());
		evaluation.setIncomeStabilityPoints(metrics.incomeStabilityPoints());
		evaluation.setExposurePoints(metrics.exposurePoints());
		evaluation.setReportGenerated(Boolean.FALSE);
	}

	private void applyCommonMetricsToBankEvaluation(BankCreditEvaluation evaluation, EvaluationMetrics metrics) {
		evaluation.setTotalRiskPoints(metrics.totalRiskPoints());
		evaluation.setRiskLevel(metrics.riskLevel());
		evaluation.setTotalMonthlyIncome(metrics.totalMonthlyIncome());
		evaluation.setTotalMonthlyDebtPayment(metrics.totalMonthlyDebtPayment());
		evaluation.setTotalCardLimit(metrics.totalCardLimit());
		evaluation.setTotalCardOutstanding(metrics.totalCardOutstanding());
		evaluation.setDtiRatio(metrics.dtiRatio());
		evaluation.setCreditUtilizationRatio(metrics.creditUtilizationRatio());
		evaluation.setActiveFacilitiesCount(metrics.activeFacilitiesCount());
		evaluation.setMissedPaymentsCount(metrics.missedPaymentsCount());
		evaluation.setPaymentHistoryPoints(metrics.paymentHistoryPoints());
		evaluation.setDtiPoints(metrics.dtiPoints());
		evaluation.setUtilizationPoints(metrics.utilizationPoints());
		evaluation.setIncomeStabilityPoints(metrics.incomeStabilityPoints());
		evaluation.setExposurePoints(metrics.exposurePoints());
		evaluation.setReportGenerated(Boolean.FALSE);
	}

	private EvaluationMetrics buildPublicEvaluationMetrics(PublicCustomerFinancialRecord record) {
		Long recordId = record.getRecordId();
		List<PublicCustomerIncome> incomes = publicCustomerIncomeRepository.findAllByFinancialRecord_RecordId(recordId);
		List<PublicCustomerLoan> loans = publicCustomerLoanRepository.findAllByFinancialRecord_RecordId(recordId);
		List<PublicCustomerCard> cards = publicCustomerCardRepository.findAllByFinancialRecord_RecordId(recordId);
		List<PublicCustomerLiability> liabilities = publicCustomerLiabilityRepository.findAllByFinancialRecord_RecordId(recordId);
		int missedPaymentsCount = publicCustomerMissedPaymentRepository
			.findByFinancialRecord_RecordId(recordId)
			.map(PublicCustomerMissedPayment::getMissedPayments)
			.orElse(0);

		BigDecimal totalMonthlyIncome = incomes.stream()
			.map(PublicCustomerIncome::getAmount)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalMonthlyDebtPayment = loans.stream()
			.map(PublicCustomerLoan::getMonthlyEmi)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.add(liabilities.stream()
				.map(PublicCustomerLiability::getMonthlyAmount)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
		BigDecimal totalCardLimit = cards.stream()
			.map(PublicCustomerCard::getCreditLimit)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardOutstanding = cards.stream()
			.map(PublicCustomerCard::getOutstandingBalance)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		int activeFacilitiesCount = loans.size() + cards.size();
		int incomeStabilityPoints = calculateIncomeStabilityPointsForPublic(incomes, totalMonthlyIncome);

		return calculateEvaluationMetrics(
			totalMonthlyIncome,
			totalMonthlyDebtPayment,
			totalCardLimit,
			totalCardOutstanding,
			activeFacilitiesCount,
			missedPaymentsCount,
			incomeStabilityPoints
		);
	}

	private EvaluationMetrics buildBankEvaluationMetrics(BankCustomerFinancialRecord record) {
		Long bankRecordId = record.getBankRecordId();
		List<BankCustomerIncome> incomes = bankCustomerIncomeRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerLoan> loans = bankCustomerLoanRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerCard> cards = bankCustomerCardRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerLiability> liabilities = bankCustomerLiabilityRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		int missedPaymentsCount = bankCustomerMissedPaymentRepository
			.findByFinancialRecord_BankRecordId(bankRecordId)
			.map(BankCustomerMissedPayment::getMissedPayments)
			.orElse(0);

		BigDecimal totalMonthlyIncome = incomes.stream()
			.map(BankCustomerIncome::getAmount)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalMonthlyDebtPayment = loans.stream()
			.map(BankCustomerLoan::getMonthlyEmi)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.add(liabilities.stream()
				.map(BankCustomerLiability::getMonthlyAmount)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
		BigDecimal totalCardLimit = cards.stream()
			.map(BankCustomerCard::getCreditLimit)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardOutstanding = cards.stream()
			.map(BankCustomerCard::getOutstandingBalance)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		int activeFacilitiesCount = loans.size() + cards.size();
		int incomeStabilityPoints = calculateIncomeStabilityPointsForBank(incomes, totalMonthlyIncome);

		return calculateEvaluationMetrics(
			totalMonthlyIncome,
			totalMonthlyDebtPayment,
			totalCardLimit,
			totalCardOutstanding,
			activeFacilitiesCount,
			missedPaymentsCount,
			incomeStabilityPoints
		);
	}

	private EvaluationMetrics calculateEvaluationMetrics(
		BigDecimal totalMonthlyIncome,
		BigDecimal totalMonthlyDebtPayment,
		BigDecimal totalCardLimit,
		BigDecimal totalCardOutstanding,
		int activeFacilitiesCount,
		int missedPaymentsCount,
		int incomeStabilityPoints
	) {
		if (totalMonthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("A positive monthly income is required before generating a credit evaluation.");
		}

		BigDecimal dtiRatio = totalMonthlyDebtPayment
			.divide(totalMonthlyIncome, 4, RoundingMode.HALF_UP);
		BigDecimal creditUtilizationRatio = totalCardLimit.compareTo(BigDecimal.ZERO) <= 0
			? (totalCardOutstanding.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO)
			: totalCardOutstanding.divide(totalCardLimit, 4, RoundingMode.HALF_UP);

		int paymentHistoryPoints = calculatePaymentHistoryPoints(missedPaymentsCount);
		int dtiPoints = calculateDtiPoints(dtiRatio);
		int utilizationPoints = calculateUtilizationPoints(creditUtilizationRatio);
		int exposurePoints = calculateExposurePoints(activeFacilitiesCount);
		int totalRiskPoints = paymentHistoryPoints + dtiPoints + utilizationPoints + incomeStabilityPoints + exposurePoints;

		return new EvaluationMetrics(
			totalRiskPoints,
			resolveRiskLevel(totalRiskPoints),
			totalMonthlyIncome.setScale(2, RoundingMode.HALF_UP),
			totalMonthlyDebtPayment.setScale(2, RoundingMode.HALF_UP),
			totalCardLimit.setScale(2, RoundingMode.HALF_UP),
			totalCardOutstanding.setScale(2, RoundingMode.HALF_UP),
			dtiRatio,
			creditUtilizationRatio,
			activeFacilitiesCount,
			missedPaymentsCount,
			paymentHistoryPoints,
			dtiPoints,
			utilizationPoints,
			incomeStabilityPoints,
			exposurePoints
		);
	}

	private int calculatePaymentHistoryPoints(int missedPaymentsCount) {
		if (missedPaymentsCount <= 0) {
			return 0;
		}
		if (missedPaymentsCount == 1) {
			return 6;
		}
		if (missedPaymentsCount == 2) {
			return 12;
		}
		if (missedPaymentsCount == 3) {
			return 18;
		}
		if (missedPaymentsCount == 4) {
			return 24;
		}
		return 30;
	}

	private int calculateDtiPoints(BigDecimal dtiRatio) {
		if (dtiRatio.compareTo(new BigDecimal("0.20")) <= 0) {
			return 0;
		}
		if (dtiRatio.compareTo(new BigDecimal("0.30")) <= 0) {
			return 4;
		}
		if (dtiRatio.compareTo(new BigDecimal("0.40")) <= 0) {
			return 7;
		}
		if (dtiRatio.compareTo(new BigDecimal("0.45")) <= 0) {
			return 12;
		}
		if (dtiRatio.compareTo(new BigDecimal("0.50")) <= 0) {
			return 15;
		}
		if (dtiRatio.compareTo(new BigDecimal("0.60")) <= 0) {
			return 20;
		}
		return 25;
	}

	private int calculateUtilizationPoints(BigDecimal creditUtilizationRatio) {
		if (creditUtilizationRatio.compareTo(new BigDecimal("0.10")) <= 0) {
			return 0;
		}
		if (creditUtilizationRatio.compareTo(new BigDecimal("0.30")) <= 0) {
			return 6;
		}
		if (creditUtilizationRatio.compareTo(new BigDecimal("0.50")) <= 0) {
			return 10;
		}
		if (creditUtilizationRatio.compareTo(new BigDecimal("0.70")) <= 0) {
			return 16;
		}
		return 20;
	}

	private int calculateExposurePoints(int activeFacilitiesCount) {
		if (activeFacilitiesCount <= 2) {
			return 0;
		}
		if (activeFacilitiesCount <= 4) {
			return 5;
		}
		if (activeFacilitiesCount <= 6) {
			return 8;
		}
		return 10;
	}

	private int calculateIncomeStabilityPointsForPublic(
		List<PublicCustomerIncome> incomes,
		BigDecimal totalMonthlyIncome
	) {
		if (incomes.isEmpty() || totalMonthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
			return 15;
		}

		BigDecimal weightedRisk = incomes.stream()
			.map(income -> safeAmount(income.getAmount()).multiply(BigDecimal.valueOf(resolveIncomeRisk(
				income.getIncomeCategory(),
				income.getSalaryType(),
				income.getEmploymentType(),
				income.getContractDurationMonths(),
				income.getIncomeStability()
			))))
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		return weightedRisk
			.divide(totalMonthlyIncome, 0, RoundingMode.HALF_UP)
			.intValue();
	}

	private int calculateIncomeStabilityPointsForBank(
		List<BankCustomerIncome> incomes,
		BigDecimal totalMonthlyIncome
	) {
		if (incomes.isEmpty() || totalMonthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
			return 15;
		}

		BigDecimal weightedRisk = incomes.stream()
			.map(income -> safeAmount(income.getAmount()).multiply(BigDecimal.valueOf(resolveIncomeRisk(
				income.getIncomeCategory(),
				income.getSalaryType(),
				income.getEmploymentType(),
				income.getContractDurationMonths(),
				income.getIncomeStability()
			))))
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		return weightedRisk
			.divide(totalMonthlyIncome, 0, RoundingMode.HALF_UP)
			.intValue();
	}

	private int resolveIncomeRisk(
		String incomeCategory,
		String salaryType,
		String employmentType,
		Integer contractDurationMonths,
		String incomeStability
	) {
		String normalizedCategory = normalizeText(incomeCategory);
		String normalizedSalaryType = normalizeText(salaryType);
		String normalizedEmploymentType = normalizeText(employmentType);
		String normalizedIncomeStability = normalizeText(incomeStability);

		if ("SALARY".equals(normalizedCategory)) {
			if (normalizedEmploymentType.contains("PERMANENT")) {
				return 0;
			}
			if (normalizedSalaryType.contains("FIXED") && normalizedEmploymentType.contains("FULL")) {
				return 2;
			}
			if (contractDurationMonths != null && contractDurationMonths >= 12) {
				return 5;
			}
			if (contractDurationMonths != null && contractDurationMonths >= 6) {
				return 8;
			}
			return 12;
		}

		if ("BUSINESS".equals(normalizedCategory)) {
			if (normalizedIncomeStability.contains("STABLE")) {
				return 4;
			}
			if (normalizedIncomeStability.contains("MODERATE") || normalizedIncomeStability.contains("SEASONAL")) {
				return 8;
			}
			if (normalizedIncomeStability.contains("LOW") || normalizedIncomeStability.contains("UNSTABLE")) {
				return 15;
			}
			return 10;
		}

		return 10;
	}

	private String resolveRiskLevel(int totalRiskPoints) {
		if (totalRiskPoints < 40) {
			return "LOW";
		}
		if (totalRiskPoints < 65) {
			return "MEDIUM";
		}
		return "HIGH";
	}

	private String normalizeBankEvaluationSource(String value) {
		String normalized = normalizeText(value);
		if (normalized.isBlank()) {
			return "MANUAL";
		}
		if (!BANK_EVALUATION_SOURCES.contains(normalized)) {
			throw new IllegalArgumentException("Evaluation source must be MANUAL, CRIB_MERGED, or CRIB_ONLY.");
		}
		return normalized;
	}

	private String normalizeOptionalText(String value) {
		String normalized = value == null ? null : value.trim();
		return normalized == null || normalized.isBlank() ? null : normalized;
	}

	private PublicCustomerFinancialRecord resolveCurrentPublicFinancialRecord(Long publicCustomerId) {
		return publicCustomerFinancialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(publicCustomerId, "CURRENT")
			.orElseThrow(() -> new IllegalArgumentException("No current financial record found for this public customer."));
	}

	private BankCustomerFinancialRecord resolveLatestBankFinancialRecord(Long bankCustomerId) {
		return bankCustomerFinancialRecordRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("No financial record found for this bank customer."));
	}

	private boolean isRecordUpdatedAfterEvaluation(LocalDateTime recordUpdatedAt, LocalDateTime evaluationCreatedAt) {
		return recordUpdatedAt != null && evaluationCreatedAt != null && recordUpdatedAt.isAfter(evaluationCreatedAt);
	}

	private PublicCustomerProfile resolveLoggedInPublicCustomerProfile() {
		User user = resolveAuthenticatedUser("Public customer authentication is required.");
		return publicCustomerProfileRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a public customer."));
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

	private BankCustomer resolveOwnedBankCustomer(Long bankCustomerId, BankOfficer officer) {
		BankCustomer bankCustomer = bankCustomerRepository
			.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		if (!bankCustomer.getOfficer().getOfficerId().equals(officer.getOfficerId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This bank customer is not assigned to the logged-in officer.");
		}
		return bankCustomer;
	}

	private String buildFullName(User user) {
		return (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
	}

	private String toTitleCase(String value) {
		String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return "";
		}
		return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
	}

	private String normalizeText(String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}

	private BigDecimal safeAmount(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private record EvaluationMetrics(
		int totalRiskPoints,
		String riskLevel,
		BigDecimal totalMonthlyIncome,
		BigDecimal totalMonthlyDebtPayment,
		BigDecimal totalCardLimit,
		BigDecimal totalCardOutstanding,
		BigDecimal dtiRatio,
		BigDecimal creditUtilizationRatio,
		int activeFacilitiesCount,
		int missedPaymentsCount,
		int paymentHistoryPoints,
		int dtiPoints,
		int utilizationPoints,
		int incomeStabilityPoints,
		int exposurePoints
	) {
	}
}
