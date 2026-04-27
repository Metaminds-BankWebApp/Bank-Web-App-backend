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
import com.bank_web_app.backend.creditlens.dto.response.CreditDashboardFactorResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditDashboardResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditInfoTooltipResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditInsightItemResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditInsightsResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditReportResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditReportSnapshotResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditRiskFactorResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditTrendPointResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditTrendResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditTrendSummaryResponse;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
	private static final BigDecimal ESTIMATED_CARD_MIN_PAYMENT_RATIO = new BigDecimal("0.05");
	private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH);
	private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);
	private static final DateTimeFormatter SHORT_MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

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
			.map(this::synchronizeSelfEvaluation)
			.map(creditEvaluationMapper::toSelfSummary)
			.toList();
	}

	@Transactional
	public SelfCreditEvaluationResponse getSelfEvaluationById(Long selfEvaluationId) {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		SelfCreditEvaluation evaluation = selfCreditEvaluationRepository
			.findBySelfEvaluationIdAndPublicCustomer_PublicCustomerId(selfEvaluationId, profile.getPublicCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Self credit evaluation not found for this public customer."));
		return creditEvaluationMapper.toSelfResponse(synchronizeSelfEvaluation(evaluation));
	}

	@Transactional
	public BankCreditEvaluationResponse getCurrentBankEvaluationForCustomer() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		BankCreditEvaluation evaluation = getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return creditEvaluationMapper.toBankResponse(evaluation);
	}

	@Transactional
	public List<BankCreditEvaluationSummaryResponse> getBankEvaluationHistoryForCustomer() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return bankCreditEvaluationRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.stream()
			.map(this::synchronizeBankEvaluation)
			.map(creditEvaluationMapper::toBankSummary)
			.toList();
	}

	@Transactional
	public BankCreditEvaluationResponse getBankEvaluationByIdForCustomer(Long bankEvaluationId) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		BankCreditEvaluation evaluation = bankCreditEvaluationRepository
			.findByBankEvaluationIdAndBankCustomer_BankCustomerId(bankEvaluationId, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Bank credit evaluation not found for this bank customer."));
		return creditEvaluationMapper.toBankResponse(synchronizeBankEvaluation(evaluation));
	}

	@Transactional
	public CreditDashboardResponse getPublicDashboard() {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		SelfCreditEvaluation currentEvaluation = getOrCreateLatestSelfEvaluation(profile);
		List<EvaluationView> views = getPublicEvaluationViews(profile);
		return buildDashboardResponse(toView(currentEvaluation), views);
	}

	@Transactional
	public CreditDashboardResponse getBankDashboard() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		BankCreditEvaluation currentEvaluation = getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		List<EvaluationView> views = getBankEvaluationViews(bankCustomer);
		return buildDashboardResponse(toView(currentEvaluation), views);
	}

	@Transactional
	public CreditTrendResponse getPublicTrends(String range) {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		getOrCreateLatestSelfEvaluation(profile);
		return buildTrendResponse(getPublicEvaluationViews(profile), normalizeTrendRange(range));
	}

	@Transactional
	public CreditTrendResponse getBankTrends(String range) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return buildTrendResponse(getBankEvaluationViews(bankCustomer), normalizeTrendRange(range));
	}

	@Transactional
	public CreditInsightsResponse getPublicInsights() {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		SelfCreditEvaluation currentEvaluation = getOrCreateLatestSelfEvaluation(profile);
		List<EvaluationView> views = getPublicEvaluationViews(profile);
		return buildInsightsResponse(
			toView(currentEvaluation),
			views,
			loadRecordBreakdown(toView(currentEvaluation))
		);
	}

	@Transactional
	public CreditInsightsResponse getBankInsights() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		BankCreditEvaluation currentEvaluation = getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		List<EvaluationView> views = getBankEvaluationViews(bankCustomer);
		return buildInsightsResponse(
			toView(currentEvaluation),
			views,
			loadRecordBreakdown(toView(currentEvaluation))
		);
	}

	@Transactional
	public CreditReportResponse getPublicReport() {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		getOrCreateLatestSelfEvaluation(profile);
		return buildReportResponse("PUBLIC_CUSTOMER", "Self Assessment", getPublicEvaluationViews(profile));
	}

	@Transactional
	public CreditReportResponse getBankReport() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return buildReportResponse("BANK_CUSTOMER", "Bank Assessment", getBankEvaluationViews(bankCustomer));
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

	@Transactional
	public BankCreditAnalysisCustomerProfileResponse getOfficerCustomerProfile(Long bankCustomerId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCreditEvaluation latestEvaluation = bankCreditEvaluationRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.map(this::synchronizeBankEvaluation)
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
			.map(this::synchronizeBankEvaluation)
			.map(creditEvaluationMapper::toBankSummary)
			.toList();
	}

	@Transactional
	public BankCreditEvaluationResponse getBankEvaluationByIdForOfficer(Long bankCustomerId, Long bankEvaluationId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCreditEvaluation evaluation = bankCreditEvaluationRepository
			.findByBankEvaluationIdAndBankCustomer_BankCustomerId(bankEvaluationId, bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank credit evaluation not found for this bank customer."));
		return creditEvaluationMapper.toBankResponse(synchronizeBankEvaluation(evaluation));
	}

	private SelfCreditEvaluation getOrCreateLatestSelfEvaluation(PublicCustomerProfile profile) {
		PublicCustomerFinancialRecord currentRecord = resolveCurrentPublicFinancialRecord(profile.getPublicCustomerId());
		SelfCreditEvaluation latestEvaluation = selfCreditEvaluationRepository
			.findTopByPublicRecord_RecordIdOrderByCreatedAtDesc(currentRecord.getRecordId())
			.orElse(null);

		if (latestEvaluation != null && !isRecordUpdatedAfterEvaluation(currentRecord.getUpdatedAt(), latestEvaluation.getCreatedAt())) {
			return synchronizeSelfEvaluation(latestEvaluation);
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
			return synchronizeBankEvaluation(latestEvaluation);
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
		validatePublicFinancialInputs(incomes, loans, cards, liabilities, missedPaymentsCount);

		BigDecimal totalMonthlyIncome = incomes.stream()
			.map(PublicCustomerIncome::getAmount)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardLimit = cards.stream()
			.map(PublicCustomerCard::getCreditLimit)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardOutstanding = cards.stream()
			.map(PublicCustomerCard::getOutstandingBalance)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal estimatedCardMinPayment = estimateCardMinimumPayment(totalCardOutstanding);
		BigDecimal totalMonthlyDebtPayment = loans.stream()
			.map(PublicCustomerLoan::getMonthlyEmi)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.add(liabilities.stream()
				.map(PublicCustomerLiability::getMonthlyAmount)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add))
			.add(estimatedCardMinPayment);
		int activeFacilitiesCount = loans.size() + cards.size();
		int incomeStabilityPoints = calculateIncomeStabilityPointsForPublic(incomes);

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
		validateBankFinancialInputs(incomes, loans, cards, liabilities, missedPaymentsCount);

		BigDecimal totalMonthlyIncome = incomes.stream()
			.map(BankCustomerIncome::getAmount)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardLimit = cards.stream()
			.map(BankCustomerCard::getCreditLimit)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardOutstanding = cards.stream()
			.map(BankCustomerCard::getOutstandingBalance)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal estimatedCardMinPayment = estimateCardMinimumPayment(totalCardOutstanding);
		BigDecimal totalMonthlyDebtPayment = loans.stream()
			.map(BankCustomerLoan::getMonthlyEmi)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.add(liabilities.stream()
				.map(BankCustomerLiability::getMonthlyAmount)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add))
			.add(estimatedCardMinPayment);
		int activeFacilitiesCount = loans.size() + cards.size();
		int incomeStabilityPoints = calculateIncomeStabilityPointsForBank(incomes);

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
			return 8;
		}
		if (missedPaymentsCount <= 3) {
			return 18;
		}
		return 30;
	}

	private int calculateDtiPoints(BigDecimal dtiRatio) {
		if (dtiRatio.compareTo(new BigDecimal("0.30")) <= 0) {
			return 0;
		}
		if (dtiRatio.compareTo(new BigDecimal("0.50")) <= 0) {
			return 12;
		}
		return 25;
	}

	private int calculateUtilizationPoints(BigDecimal creditUtilizationRatio) {
		if (creditUtilizationRatio.compareTo(new BigDecimal("0.40")) <= 0) {
			return 0;
		}
		if (creditUtilizationRatio.compareTo(new BigDecimal("0.70")) <= 0) {
			return 10;
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
		return 10;
	}

	private int calculateIncomeStabilityPointsForPublic(List<PublicCustomerIncome> incomes) {
		if (incomes.isEmpty()) {
			return 15;
		}
		BigDecimal share = BigDecimal.valueOf(15)
			.divide(BigDecimal.valueOf(incomes.size()), 4, RoundingMode.HALF_UP);
		BigDecimal totalPoints = incomes.stream()
			.map(income -> share.multiply(resolveIncomeRiskMultiplier(
				income.getIncomeCategory(),
				income.getEmploymentType(),
				income.getDurationMonths(),
				income.getIncomeStability()
			)))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return totalPoints.setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private int calculateIncomeStabilityPointsForBank(List<BankCustomerIncome> incomes) {
		if (incomes.isEmpty()) {
			return 15;
		}
		BigDecimal share = BigDecimal.valueOf(15)
			.divide(BigDecimal.valueOf(incomes.size()), 4, RoundingMode.HALF_UP);
		BigDecimal totalPoints = incomes.stream()
			.map(income -> share.multiply(resolveIncomeRiskMultiplier(
				income.getIncomeCategory(),
				income.getEmploymentType(),
				income.getDurationMonths(),
				income.getIncomeStability()
			)))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return totalPoints.setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private BigDecimal resolveIncomeRiskMultiplier(
		String incomeCategory,
		String employmentType,
		Integer durationMonths,
		String incomeStability
	) {
		String normalizedCategory = normalizeText(incomeCategory);
		String normalizedEmploymentType = normalizeText(employmentType);
		String normalizedIncomeStability = normalizeText(incomeStability);
		int normalizedDurationMonths = durationMonths == null ? 0 : durationMonths;

		if ("SALARY".equals(normalizedCategory)) {
			if (normalizedEmploymentType.contains("PERMANENT")) {
				if (normalizedDurationMonths <= 0 || normalizedDurationMonths > 12) {
					return BigDecimal.ZERO;
				}
				if (normalizedDurationMonths >= 6) {
					return new BigDecimal("0.5");
				}
				return BigDecimal.ONE;
			}
			if (normalizedEmploymentType.contains("CONTRACT")) {
				return normalizedDurationMonths > 0 && normalizedDurationMonths < 6 ? BigDecimal.ONE : new BigDecimal("0.5");
			}
			return BigDecimal.ONE;
		}

		if ("BUSINESS".equals(normalizedCategory)) {
			if ("STABLE".equals(normalizedIncomeStability)) {
				return BigDecimal.ZERO;
			}
			if (
				normalizedIncomeStability.contains("MEDIUM") ||
				normalizedIncomeStability.contains("MODERATE")
			) {
				return new BigDecimal("0.5");
			}
			return BigDecimal.ONE;
		}

		return BigDecimal.ONE;
	}

	private String resolveRiskLevel(int totalRiskPoints) {
		if (totalRiskPoints < 40) {
			return "LOW";
		}
		if (totalRiskPoints < 70) {
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

	private List<EvaluationView> getPublicEvaluationViews(PublicCustomerProfile profile) {
		return selfCreditEvaluationRepository
			.findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(profile.getPublicCustomerId())
			.stream()
			.map(this::synchronizeSelfEvaluation)
			.map(this::toView)
			.toList();
	}

	private List<EvaluationView> getBankEvaluationViews(BankCustomer bankCustomer) {
		return bankCreditEvaluationRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.stream()
			.map(this::synchronizeBankEvaluation)
			.map(this::toView)
			.toList();
	}

	private CreditDashboardResponse buildDashboardResponse(EvaluationView current, List<EvaluationView> history) {
		return new CreditDashboardResponse(
			current.evaluationId(),
			current.totalRiskPoints(),
			current.riskLevel(),
			toRiskDisplayLabel(current.riskLevel()),
			current.createdAt(),
			buildDashboardFactors(current),
			buildTrendResponse(history, "6m"),
			"Decrease your Credit Risk Score",
			"Understand the key factors increasing your credit risk and follow practical steps to improve them.",
			"Learn More"
		);
	}

	private CreditTrendResponse buildTrendResponse(List<EvaluationView> history, String rangeKey) {
		String normalizedRange = normalizeTrendRange(rangeKey);
		int monthLimit = "12m".equals(normalizedRange) ? 12 : 6;
		List<EvaluationView> monthlyViews = getLatestEvaluationsPerMonth(history);
		if (monthlyViews.size() > monthLimit) {
			monthlyViews = monthlyViews.subList(monthlyViews.size() - monthLimit, monthlyViews.size());
		}

		DateTimeFormatter labelFormatter = monthLimit == 6 ? DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH) : SHORT_MONTH_LABEL_FORMATTER;
		List<CreditTrendPointResponse> points = monthlyViews.stream()
			.map(view -> new CreditTrendPointResponse(
				YearMonth.from(view.createdAt()).toString(),
				view.createdAt().format(labelFormatter),
				view.totalRiskPoints(),
				view.createdAt()
			))
			.toList();

		return new CreditTrendResponse(
			normalizedRange,
			monthLimit == 6 ? "6 Month View" : "12 Month View",
			points.stream().map(CreditTrendPointResponse::monthLabel).toList(),
			points.stream().map(CreditTrendPointResponse::score).toList(),
			points,
			buildTrendSummary(monthlyViews, normalizedRange)
		);
	}

	private CreditTrendSummaryResponse buildTrendSummary(List<EvaluationView> monthlyViews, String rangeKey) {
		if (monthlyViews.isEmpty()) {
			return new CreditTrendSummaryResponse(
				"No Evaluation Yet",
				0,
				"Trend will appear after the first evaluation is generated.",
				"Generate the first evaluation to identify key drivers.",
				"Need at least one monthly evaluation",
				"Generate your first evaluation",
				"STABLE"
			);
		}

		EvaluationView latest = monthlyViews.get(monthlyViews.size() - 1);
		if (monthlyViews.size() < 2) {
			return new CreditTrendSummaryResponse(
				toRiskSummaryLabel(latest.riskLevel()),
				0,
				"Trend will appear after another monthly evaluation.",
				buildCurrentPrimaryDriver(latest),
				"Need at least two monthly evaluations",
				resolveNextTarget(latest.totalRiskPoints()),
				"STABLE"
			);
		}

		EvaluationView earliest = monthlyViews.get(0);
		int delta = latest.totalRiskPoints() - earliest.totalRiskPoints();
		String direction = delta < 0 ? "IMPROVING" : (delta > 0 ? "WORSENING" : "STABLE");
		String trendText;
		if (delta < 0) {
			trendText = monthlyViews.size() >= ("12m".equals(rangeKey) ? 12 : 6)
				? ("Improved over last " + ("12m".equals(rangeKey) ? "12" : "6") + " months")
				: ("Improved since " + earliest.createdAt().format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)));
		} else if (delta > 0) {
			trendText = monthlyViews.size() >= ("12m".equals(rangeKey) ? 12 : 6)
				? ("Risk increased over last " + ("12m".equals(rangeKey) ? "12" : "6") + " months")
				: ("Risk increased since " + earliest.createdAt().format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)));
		} else {
			trendText = "Score remained stable across recent evaluations";
		}

		BigDecimal averageMonthlyChange = BigDecimal.valueOf(Math.abs(delta))
			.divide(BigDecimal.valueOf(Math.max(1, monthlyViews.size() - 1)), 1, RoundingMode.HALF_UP);
		String momentumText = delta == 0
			? "Average monthly movement is currently flat"
			: (
				(delta < 0 ? "Avg decrease: " : "Avg increase: ") +
				averageMonthlyChange.stripTrailingZeros().toPlainString() +
				" risk pts/month"
			);

		return new CreditTrendSummaryResponse(
			toRiskSummaryLabel(latest.riskLevel()),
			delta,
			trendText,
			resolveBiggestDriver(earliest, latest, direction),
			momentumText,
			resolveNextTarget(latest.totalRiskPoints()),
			direction
		);
	}

	private CreditInsightsResponse buildInsightsResponse(
		EvaluationView current,
		List<EvaluationView> history,
		RecordBreakdown breakdown
	) {
		List<EvaluationView> monthlyViews = getLatestEvaluationsPerMonth(history);
		List<CreditInsightItemResponse> keyRiskFactors = buildKeyRiskFactors(current);
		List<CreditInsightItemResponse> positiveBehaviors = buildPositiveBehaviors(current, monthlyViews);
		List<CreditInsightItemResponse> financialTips = buildFinancialTips(current, breakdown);

		return new CreditInsightsResponse(
			keyRiskFactors,
			positiveBehaviors,
			financialTips,
			"Get a full credit report",
			"Get a complete summary of your credit profile, risk factors, and recommended next actions.",
			"View Report"
		);
	}

	private CreditReportResponse buildReportResponse(
		String customerType,
		String evaluationType,
		List<EvaluationView> history
	) {
		List<EvaluationView> monthlyViews = getLatestEvaluationsPerMonth(history);
		List<CreditReportSnapshotResponse> snapshots = new ArrayList<>();
		for (int index = 0; index < monthlyViews.size(); index++) {
			EvaluationView view = monthlyViews.get(index);
			RecordBreakdown breakdown = loadRecordBreakdown(view);
			snapshots.add(new CreditReportSnapshotResponse(
				view.evaluationId(),
				view.createdAt().format(MONTH_LABEL_FORMATTER),
				breakdown.income(),
				breakdown.loanEmi(),
				breakdown.creditCardBalance(),
				breakdown.creditCardLimit(),
				breakdown.otherLiabilities(),
				view.totalRiskPoints(),
				toRiskDisplayLabel(view.riskLevel()),
				view.evaluationType(),
				view.createdAt(),
				view.createdAt().format(REPORT_DATE_FORMATTER).toUpperCase(Locale.ROOT),
				view.missedPaymentsCount(),
				view.activeFacilitiesCount(),
				toPercentage(view.dtiRatio()),
				toPercentage(view.creditUtilizationRatio()),
				resolveDtiBand(view.dtiRatio()),
				buildRiskFactors(view)
			));
		}

		return new CreditReportResponse(
			customerType,
			evaluationType,
			LocalDateTime.now(),
			snapshots
		);
	}

	private List<CreditDashboardFactorResponse> buildDashboardFactors(EvaluationView current) {
		return List.of(
			new CreditDashboardFactorResponse("Payment history", current.paymentHistoryPoints(), 30, resolveFactorColor(current.paymentHistoryPoints(), 30), null),
			new CreditDashboardFactorResponse(
				"DTI",
				current.dtiPoints(),
				25,
				resolveFactorColor(current.dtiPoints(), 25),
				new CreditInfoTooltipResponse(
					"Debt-to-Income (DTI)",
					"Shows how much of your monthly income goes toward debt payments.",
					"DTI = (Total monthly debt payments / Gross monthly income) x 100"
				)
			),
			new CreditDashboardFactorResponse(
				"Credit utilization",
				current.utilizationPoints(),
				20,
				resolveFactorColor(current.utilizationPoints(), 20),
				new CreditInfoTooltipResponse(
					"Credit Utilization",
					"Shows how much of your available revolving credit you are currently using.",
					"Utilization = (Total card balances / Total credit limits) x 100"
				)
			),
			new CreditDashboardFactorResponse("Income stability", current.incomeStabilityPoints(), 15, resolveFactorColor(current.incomeStabilityPoints(), 15), null),
			new CreditDashboardFactorResponse("Active Facilities", current.exposurePoints(), 10, resolveFactorColor(current.exposurePoints(), 10), null)
		);
	}

	private List<CreditInsightItemResponse> buildKeyRiskFactors(EvaluationView current) {
		List<FactorSnapshot> factors = List.of(
			new FactorSnapshot(
				"Payment History",
				current.paymentHistoryPoints(),
				30,
				"circle-alert",
				current.missedPaymentsCount() + " missed payment(s) in the last 12 months",
				current.paymentHistoryPoints() + "/30 points",
				null
			),
			new FactorSnapshot(
				"Debt-to-Income",
				current.dtiPoints(),
				25,
				"trending-down",
				"Current: " + formatPercentageLabel(current.dtiRatio()) + " (" + current.dtiPoints() + "/25 points)",
				"Keeping DTI at or below 30% removes DTI risk points.",
				new CreditInfoTooltipResponse(
					"Debt-to-Income (DTI)",
					"Shows how much of your monthly income goes toward debt payments.",
					"DTI = (Total monthly debt payments / Gross monthly income) x 100"
				)
			),
			new FactorSnapshot(
				"Credit Utilization",
				current.utilizationPoints(),
				20,
				"credit-card",
				"Current: " + formatPercentageLabel(current.creditUtilizationRatio()) + " (" + current.utilizationPoints() + "/20 points)",
				"Keeping utilization at or below 40% removes utilization risk points.",
				new CreditInfoTooltipResponse(
					"Credit Utilization",
					"Shows how much of your available revolving credit you are currently using.",
					"Utilization = (Total card balances / Total credit limits) x 100"
				)
			),
			new FactorSnapshot(
				"Income Stability",
				current.incomeStabilityPoints(),
				15,
				"briefcase",
				"Income stability currently contributes " + current.incomeStabilityPoints() + "/15 points.",
				"Stable permanent income or lower business fluctuation helps reduce this factor.",
				null
			),
			new FactorSnapshot(
				"Active Facilities",
				current.exposurePoints(),
				10,
				"building-2",
				"Current: " + current.activeFacilitiesCount() + " active facilities (" + current.exposurePoints() + "/10 points)",
				"Keeping facilities at two or fewer removes exposure points.",
				null
			)
		);

		return factors.stream()
			.sorted(Comparator.comparingInt(FactorSnapshot::points).reversed().thenComparing(FactorSnapshot::title))
			.limit(3)
			.map(factor -> new CreditInsightItemResponse(
				factor.title(),
				factor.description(),
				factor.detail(),
				resolveBadgeText(factor.points(), factor.maxPoints()),
				resolveBadgeTone(factor.points(), factor.maxPoints()),
				factor.iconKey(),
				factor.infoTooltip()
			))
			.toList();
	}

	private List<CreditInsightItemResponse> buildPositiveBehaviors(EvaluationView current, List<EvaluationView> monthlyViews) {
		EvaluationView previous = monthlyViews.size() >= 2 ? monthlyViews.get(monthlyViews.size() - 2) : null;
		List<InsightCandidate> candidates = new ArrayList<>();

		int paymentStrength = current.missedPaymentsCount() == 0 ? 95 : (current.missedPaymentsCount() == 1 ? 75 : (current.missedPaymentsCount() <= 3 ? 50 : 0));
		if (paymentStrength > 0) {
			candidates.add(new InsightCandidate(
				paymentStrength,
				new CreditInsightItemResponse(
					current.missedPaymentsCount() == 0 ? "No recent missed payments" : "Payment history is below the maximum-risk band",
					current.missedPaymentsCount() == 0
						? "No missed payments are currently affecting the last 12-month history."
						: current.missedPaymentsCount() + " missed payment(s) keep this factor below the 4+ highest-risk bracket.",
					"Keeping the next 12 months clean can reduce payment-history risk points further.",
					current.missedPaymentsCount() == 0 ? "CLEAN" : "RECOVERABLE",
					current.missedPaymentsCount() == 0 ? "green" : "amber",
					"check-circle",
					null
				)
			));
		}

		int dtiStrength = current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0 ? 90 : (current.dtiRatio().compareTo(new BigDecimal("0.50")) <= 0 ? 55 : 0);
		if (dtiStrength > 0) {
			candidates.add(new InsightCandidate(
				dtiStrength,
				new CreditInsightItemResponse(
					current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0 ? "Debt-to-income is within the low-risk band" : "DTI is below the critical threshold",
					"Current DTI is " + formatPercentageLabel(current.dtiRatio()) + ".",
					current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0
						? "This keeps DTI at 0/25 points."
						: "Keeping DTI at or below 50% avoids the highest DTI penalty.",
					current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0 ? "STRONG" : "CONTROLLED",
					current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0 ? "green" : "amber",
					"percent",
					null
				)
			));
		}

		int utilizationStrength = current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0
			? 85
			: (current.creditUtilizationRatio().compareTo(new BigDecimal("0.70")) <= 0 ? 55 : 0);
		if (utilizationStrength > 0) {
			candidates.add(new InsightCandidate(
				utilizationStrength,
				new CreditInsightItemResponse(
					current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0
						? "Credit utilization is within the healthy band"
						: "Utilization is below the maximum-risk zone",
					"Current utilization is " + formatPercentageLabel(current.creditUtilizationRatio()) + ".",
					current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0
						? "This keeps utilization at 0/20 points."
						: "Staying at or below 70% avoids the maximum utilization penalty.",
					current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0 ? "HEALTHY" : "CONTROLLED",
					current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0 ? "green" : "amber",
					"credit-card",
					null
				)
			));
		}

		int exposureStrength = current.activeFacilitiesCount() <= 2 ? 80 : (current.activeFacilitiesCount() <= 4 ? 55 : 0);
		if (exposureStrength > 0) {
			candidates.add(new InsightCandidate(
				exposureStrength,
				new CreditInsightItemResponse(
					current.activeFacilitiesCount() <= 2 ? "Credit exposure is well controlled" : "Active facilities remain manageable",
					"Current exposure is " + current.activeFacilitiesCount() + " active facilities.",
					current.activeFacilitiesCount() <= 2
						? "This keeps exposure at 0/10 points."
						: "Keeping facilities at four or fewer avoids the highest exposure penalty.",
					current.activeFacilitiesCount() <= 2 ? "BALANCED" : "MANAGEABLE",
					current.activeFacilitiesCount() <= 2 ? "green" : "amber",
					"building-2",
					null
				)
			));
		}

		int incomeStrength = current.incomeStabilityPoints() == 0 ? 82 : (current.incomeStabilityPoints() <= 7 ? 50 : 0);
		if (incomeStrength > 0) {
			candidates.add(new InsightCandidate(
				incomeStrength,
				new CreditInsightItemResponse(
					current.incomeStabilityPoints() == 0 ? "Income profile is stable" : "Income stability is partially supportive",
					"Income stability currently contributes " + current.incomeStabilityPoints() + "/15 points.",
					current.incomeStabilityPoints() == 0
						? "Stable employment or business patterns are helping the score."
						: "Income stability is not in the highest-risk tier.",
					current.incomeStabilityPoints() == 0 ? "VERIFIED" : "SUPPORTIVE",
					current.incomeStabilityPoints() == 0 ? "green" : "blue",
					"badge-check",
					null
				)
			));
		}

		if (previous != null) {
			if (current.totalRiskPoints() < previous.totalRiskPoints()) {
				candidates.add(new InsightCandidate(
					78,
					new CreditInsightItemResponse(
						"Risk score improved since the last evaluation",
						"Score moved from " + previous.totalRiskPoints() + " to " + current.totalRiskPoints() + ".",
						"Recent behavior is moving the profile in the right direction.",
						"IMPROVING",
						"green",
						"trending-up",
						null
					)
				));
			} else if (current.totalRiskPoints().equals(previous.totalRiskPoints())) {
				candidates.add(new InsightCandidate(
					35,
					new CreditInsightItemResponse(
						"Risk score remained stable",
						"Score stayed at " + current.totalRiskPoints() + " since the last evaluation.",
						"Maintaining stability helps prevent a move into a worse risk band.",
						"STABLE",
						"blue",
						"activity",
						null
					)
				));
			}
		}

		return candidates.stream()
			.sorted(Comparator.comparingInt(InsightCandidate::priority).reversed())
			.limit(3)
			.map(InsightCandidate::item)
			.toList();
	}

	private List<CreditInsightItemResponse> buildFinancialTips(EvaluationView current, RecordBreakdown breakdown) {
		List<InsightCandidate> candidates = new ArrayList<>();

		if (current.creditUtilizationRatio().compareTo(new BigDecimal("0.70")) > 0) {
			candidates.add(new InsightCandidate(
				95,
				new CreditInsightItemResponse(
					"Bring utilization below 70% first",
					"Current utilization is " + formatPercentageLabel(current.creditUtilizationRatio()) + ", which is in the highest-risk band.",
					"Below 70% can drop utilization risk by 10 points. Below 40% can remove up to " + current.utilizationPoints() + " points.",
					"-10 TO -" + current.utilizationPoints() + " PTS",
					"amber",
					"credit-card",
					null
				)
			));
		} else if (current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) > 0) {
			candidates.add(new InsightCandidate(
				85,
				new CreditInsightItemResponse(
					"Bring utilization below 40%",
					"Current utilization is " + formatPercentageLabel(current.creditUtilizationRatio()) + ".",
					"Reducing balances before the statement date can remove the current " + current.utilizationPoints() + " utilization points.",
					"-" + current.utilizationPoints() + " PTS",
					"amber",
					"credit-card",
					null
				)
			));
		}

		if (current.dtiRatio().compareTo(new BigDecimal("0.50")) > 0) {
			candidates.add(new InsightCandidate(
				92,
				new CreditInsightItemResponse(
					"Reduce DTI below 50% first",
					"Current DTI is " + formatPercentageLabel(current.dtiRatio()) + ".",
					"Below 50% reduces DTI points from 25 to 12. Below 30% removes the factor entirely.",
					"-13 TO -" + current.dtiPoints() + " PTS",
					"amber",
					"percent",
					null
				)
			));
		} else if (current.dtiRatio().compareTo(new BigDecimal("0.30")) > 0) {
			candidates.add(new InsightCandidate(
				80,
				new CreditInsightItemResponse(
					"Reduce DTI below 30%",
					"Current DTI is " + formatPercentageLabel(current.dtiRatio()) + ".",
					"Paying down monthly debt or increasing stable income can remove the current " + current.dtiPoints() + " DTI points.",
					"-" + current.dtiPoints() + " PTS",
					"amber",
					"percent",
					null
				)
			));
		}

		if (current.paymentHistoryPoints() > 0) {
			candidates.add(new InsightCandidate(
				88,
				new CreditInsightItemResponse(
					"Avoid any new missed payments",
					"Payment history currently contributes " + current.paymentHistoryPoints() + "/30 points.",
					"Autopay, due-date reminders, and clearing overdue amounts help future evaluations remove these points over time.",
					"UP TO -" + current.paymentHistoryPoints() + " PTS",
					"amber",
					"circle-alert",
					null
				)
			));
		}

		if (current.exposurePoints() > 0) {
			int immediateGain = current.activeFacilitiesCount() >= 5 ? 5 : current.exposurePoints();
			candidates.add(new InsightCandidate(
				65,
				new CreditInsightItemResponse(
					"Limit new facilities and reduce active exposure",
					"Current exposure is " + current.activeFacilitiesCount() + " active facilities.",
					"Closing unused cards or avoiding new borrowing can reduce exposure points by " + immediateGain + " or more.",
					"UP TO -" + current.exposurePoints() + " PTS",
					"blue",
					"building-2",
					null
				)
			));
		}

		if (current.incomeStabilityPoints() > 0) {
			candidates.add(new InsightCandidate(
				55,
				new CreditInsightItemResponse(
					"Strengthen proof of stable income",
					"Income stability currently contributes " + current.incomeStabilityPoints() + "/15 points.",
					"Longer employment continuity, stable contracts, or lower business fluctuation can reduce this factor.",
					"UP TO -" + current.incomeStabilityPoints() + " PTS",
					"blue",
					"briefcase",
					null
				)
			));
		}

		return candidates.stream()
			.sorted(Comparator.comparingInt(InsightCandidate::priority).reversed())
			.limit(3)
			.map(InsightCandidate::item)
			.toList();
	}

	private List<EvaluationView> getLatestEvaluationsPerMonth(List<EvaluationView> history) {
		Map<YearMonth, EvaluationView> latestByMonth = new LinkedHashMap<>();
		history.stream()
			.sorted(Comparator.comparing(EvaluationView::createdAt))
			.forEach(view -> latestByMonth.put(YearMonth.from(view.createdAt()), view));
		return new ArrayList<>(latestByMonth.values());
	}

	private String resolveBiggestDriver(EvaluationView earliest, EvaluationView latest, String direction) {
		Map<String, Integer> deltas = Map.of(
			"PAYMENT", latest.paymentHistoryPoints() - earliest.paymentHistoryPoints(),
			"DTI", latest.dtiPoints() - earliest.dtiPoints(),
			"UTILIZATION", latest.utilizationPoints() - earliest.utilizationPoints(),
			"INCOME", latest.incomeStabilityPoints() - earliest.incomeStabilityPoints(),
			"EXPOSURE", latest.exposurePoints() - earliest.exposurePoints()
		);

		if ("IMPROVING".equals(direction)) {
			Map.Entry<String, Integer> best = deltas.entrySet()
				.stream()
				.min(Map.Entry.comparingByValue())
				.orElse(null);
			if (best != null && best.getValue() < 0) {
				return switch (best.getKey()) {
					case "PAYMENT" -> "Fewer missed-payment points";
					case "DTI" -> "Reduced DTI pressure";
					case "UTILIZATION" -> "Lower utilization over time";
					case "INCOME" -> "More stable income profile";
					case "EXPOSURE" -> "Lower active credit exposure";
					default -> "Improved factor mix";
				};
			}
		}

		if ("WORSENING".equals(direction)) {
			Map.Entry<String, Integer> worst = deltas.entrySet()
				.stream()
				.max(Map.Entry.comparingByValue())
				.orElse(null);
			if (worst != null && worst.getValue() > 0) {
				return switch (worst.getKey()) {
					case "PAYMENT" -> "Payment history deterioration";
					case "DTI" -> "Higher DTI pressure";
					case "UTILIZATION" -> "Higher credit utilization";
					case "INCOME" -> "Less stable income profile";
					case "EXPOSURE" -> "Higher active credit exposure";
					default -> "Worsening factor mix";
				};
			}
		}

		return buildCurrentPrimaryDriver(latest);
	}

	private String buildCurrentPrimaryDriver(EvaluationView current) {
		Map<String, Integer> points = Map.of(
			"Payment history", current.paymentHistoryPoints(),
			"Debt-to-income pressure", current.dtiPoints(),
			"Credit utilization", current.utilizationPoints(),
			"Income stability", current.incomeStabilityPoints(),
			"Active facilities", current.exposurePoints()
		);
		return points.entrySet()
			.stream()
			.max(Map.Entry.comparingByValue())
			.map(entry -> entry.getValue() <= 0 ? "Risk profile is currently well balanced" : (entry.getKey() + " remains the biggest pressure"))
			.orElse("Risk profile is currently well balanced");
	}

	private String resolveNextTarget(int score) {
		if (score >= 70) {
			return "Reduce " + (score - 69) + " risk pts to reach Medium Risk";
		}
		if (score >= 40) {
			return "Reduce " + (score - 39) + " risk pts to reach Low Risk";
		}
		return "Stay below 40 to remain in Low Risk";
	}

	private List<CreditRiskFactorResponse> buildRiskFactors(EvaluationView view) {
		return List.of(
			new CreditRiskFactorResponse("Payment History", view.paymentHistoryPoints(), 30),
			new CreditRiskFactorResponse("Debt-to-Income", view.dtiPoints(), 25),
			new CreditRiskFactorResponse("Utilization", view.utilizationPoints(), 20),
			new CreditRiskFactorResponse("Income Stability", view.incomeStabilityPoints(), 15),
			new CreditRiskFactorResponse("Active Facilities", view.exposurePoints(), 10)
		);
	}

	private EvaluationView toView(SelfCreditEvaluation evaluation) {
		return new EvaluationView(
			evaluation.getSelfEvaluationId(),
			evaluation.getPublicRecord().getRecordId(),
			"PUBLIC",
			"Self Assessment",
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			evaluation.getTotalMonthlyIncome(),
			evaluation.getTotalMonthlyDebtPayment(),
			evaluation.getTotalCardLimit(),
			evaluation.getTotalCardOutstanding(),
			evaluation.getDtiRatio(),
			evaluation.getCreditUtilizationRatio(),
			evaluation.getActiveFacilitiesCount(),
			evaluation.getMissedPaymentsCount(),
			evaluation.getPaymentHistoryPoints(),
			evaluation.getDtiPoints(),
			evaluation.getUtilizationPoints(),
			evaluation.getIncomeStabilityPoints(),
			evaluation.getExposurePoints(),
			evaluation.getCreatedAt()
		);
	}

	private EvaluationView toView(BankCreditEvaluation evaluation) {
		return new EvaluationView(
			evaluation.getBankEvaluationId(),
			evaluation.getBankRecord().getBankRecordId(),
			"BANK",
			"Bank Assessment",
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			evaluation.getTotalMonthlyIncome(),
			evaluation.getTotalMonthlyDebtPayment(),
			evaluation.getTotalCardLimit(),
			evaluation.getTotalCardOutstanding(),
			evaluation.getDtiRatio(),
			evaluation.getCreditUtilizationRatio(),
			evaluation.getActiveFacilitiesCount(),
			evaluation.getMissedPaymentsCount(),
			evaluation.getPaymentHistoryPoints(),
			evaluation.getDtiPoints(),
			evaluation.getUtilizationPoints(),
			evaluation.getIncomeStabilityPoints(),
			evaluation.getExposurePoints(),
			evaluation.getCreatedAt()
		);
	}

	private RecordBreakdown loadRecordBreakdown(EvaluationView view) {
		if ("PUBLIC".equals(view.scope())) {
			List<PublicCustomerIncome> incomes = publicCustomerIncomeRepository.findAllByFinancialRecord_RecordId(view.recordId());
			List<PublicCustomerLoan> loans = publicCustomerLoanRepository.findAllByFinancialRecord_RecordId(view.recordId());
			List<PublicCustomerCard> cards = publicCustomerCardRepository.findAllByFinancialRecord_RecordId(view.recordId());
			List<PublicCustomerLiability> liabilities = publicCustomerLiabilityRepository.findAllByFinancialRecord_RecordId(view.recordId());
			BigDecimal income = incomes.stream().map(PublicCustomerIncome::getAmount).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal loanEmi = loans.stream().map(PublicCustomerLoan::getMonthlyEmi).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal cardBalance = cards.stream().map(PublicCustomerCard::getOutstandingBalance).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal cardLimit = cards.stream().map(PublicCustomerCard::getCreditLimit).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal liabilitiesTotal = liabilities.stream().map(PublicCustomerLiability::getMonthlyAmount).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			return new RecordBreakdown(
				income.setScale(2, RoundingMode.HALF_UP),
				loanEmi.setScale(2, RoundingMode.HALF_UP),
				cardBalance.setScale(2, RoundingMode.HALF_UP),
				cardLimit.setScale(2, RoundingMode.HALF_UP),
				liabilitiesTotal.setScale(2, RoundingMode.HALF_UP),
				estimateCardMinimumPayment(cardBalance)
			);
		}

		List<BankCustomerIncome> incomes = bankCustomerIncomeRepository.findAllByFinancialRecord_BankRecordId(view.recordId());
		List<BankCustomerLoan> loans = bankCustomerLoanRepository.findAllByFinancialRecord_BankRecordId(view.recordId());
		List<BankCustomerCard> cards = bankCustomerCardRepository.findAllByFinancialRecord_BankRecordId(view.recordId());
		List<BankCustomerLiability> liabilities = bankCustomerLiabilityRepository.findAllByFinancialRecord_BankRecordId(view.recordId());
		BigDecimal income = incomes.stream().map(BankCustomerIncome::getAmount).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal loanEmi = loans.stream().map(BankCustomerLoan::getMonthlyEmi).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal cardBalance = cards.stream().map(BankCustomerCard::getOutstandingBalance).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal cardLimit = cards.stream().map(BankCustomerCard::getCreditLimit).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal liabilitiesTotal = liabilities.stream().map(BankCustomerLiability::getMonthlyAmount).map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new RecordBreakdown(
			income.setScale(2, RoundingMode.HALF_UP),
			loanEmi.setScale(2, RoundingMode.HALF_UP),
			cardBalance.setScale(2, RoundingMode.HALF_UP),
			cardLimit.setScale(2, RoundingMode.HALF_UP),
			liabilitiesTotal.setScale(2, RoundingMode.HALF_UP),
			estimateCardMinimumPayment(cardBalance)
		);
	}

	private SelfCreditEvaluation synchronizeSelfEvaluation(SelfCreditEvaluation evaluation) {
		EvaluationMetrics metrics = buildPublicEvaluationMetrics(evaluation.getPublicRecord());
		if (!matchesSelfEvaluationMetrics(evaluation, metrics)) {
			Boolean existingReportGenerated = evaluation.getReportGenerated();
			applyCommonMetricsToSelfEvaluation(evaluation, metrics);
			evaluation.setReportGenerated(existingReportGenerated == null ? Boolean.FALSE : existingReportGenerated);
			return selfCreditEvaluationRepository.save(evaluation);
		}
		return evaluation;
	}

	private BankCreditEvaluation synchronizeBankEvaluation(BankCreditEvaluation evaluation) {
		EvaluationMetrics metrics = buildBankEvaluationMetrics(evaluation.getBankRecord());
		if (!matchesBankEvaluationMetrics(evaluation, metrics)) {
			Boolean existingReportGenerated = evaluation.getReportGenerated();
			applyCommonMetricsToBankEvaluation(evaluation, metrics);
			evaluation.setReportGenerated(existingReportGenerated == null ? Boolean.FALSE : existingReportGenerated);
			return bankCreditEvaluationRepository.save(evaluation);
		}
		return evaluation;
	}

	private boolean matchesSelfEvaluationMetrics(SelfCreditEvaluation evaluation, EvaluationMetrics metrics) {
		return
			Objects.equals(evaluation.getTotalRiskPoints(), metrics.totalRiskPoints()) &&
			Objects.equals(evaluation.getRiskLevel(), metrics.riskLevel()) &&
			isSameAmount(evaluation.getTotalMonthlyIncome(), metrics.totalMonthlyIncome()) &&
			isSameAmount(evaluation.getTotalMonthlyDebtPayment(), metrics.totalMonthlyDebtPayment()) &&
			isSameAmount(evaluation.getTotalCardLimit(), metrics.totalCardLimit()) &&
			isSameAmount(evaluation.getTotalCardOutstanding(), metrics.totalCardOutstanding()) &&
			isSameAmount(evaluation.getDtiRatio(), metrics.dtiRatio()) &&
			isSameAmount(evaluation.getCreditUtilizationRatio(), metrics.creditUtilizationRatio()) &&
			Objects.equals(evaluation.getActiveFacilitiesCount(), metrics.activeFacilitiesCount()) &&
			Objects.equals(evaluation.getMissedPaymentsCount(), metrics.missedPaymentsCount()) &&
			Objects.equals(evaluation.getPaymentHistoryPoints(), metrics.paymentHistoryPoints()) &&
			Objects.equals(evaluation.getDtiPoints(), metrics.dtiPoints()) &&
			Objects.equals(evaluation.getUtilizationPoints(), metrics.utilizationPoints()) &&
			Objects.equals(evaluation.getIncomeStabilityPoints(), metrics.incomeStabilityPoints()) &&
			Objects.equals(evaluation.getExposurePoints(), metrics.exposurePoints());
	}

	private boolean matchesBankEvaluationMetrics(BankCreditEvaluation evaluation, EvaluationMetrics metrics) {
		return
			Objects.equals(evaluation.getTotalRiskPoints(), metrics.totalRiskPoints()) &&
			Objects.equals(evaluation.getRiskLevel(), metrics.riskLevel()) &&
			isSameAmount(evaluation.getTotalMonthlyIncome(), metrics.totalMonthlyIncome()) &&
			isSameAmount(evaluation.getTotalMonthlyDebtPayment(), metrics.totalMonthlyDebtPayment()) &&
			isSameAmount(evaluation.getTotalCardLimit(), metrics.totalCardLimit()) &&
			isSameAmount(evaluation.getTotalCardOutstanding(), metrics.totalCardOutstanding()) &&
			isSameAmount(evaluation.getDtiRatio(), metrics.dtiRatio()) &&
			isSameAmount(evaluation.getCreditUtilizationRatio(), metrics.creditUtilizationRatio()) &&
			Objects.equals(evaluation.getActiveFacilitiesCount(), metrics.activeFacilitiesCount()) &&
			Objects.equals(evaluation.getMissedPaymentsCount(), metrics.missedPaymentsCount()) &&
			Objects.equals(evaluation.getPaymentHistoryPoints(), metrics.paymentHistoryPoints()) &&
			Objects.equals(evaluation.getDtiPoints(), metrics.dtiPoints()) &&
			Objects.equals(evaluation.getUtilizationPoints(), metrics.utilizationPoints()) &&
			Objects.equals(evaluation.getIncomeStabilityPoints(), metrics.incomeStabilityPoints()) &&
			Objects.equals(evaluation.getExposurePoints(), metrics.exposurePoints());
	}

	private boolean isSameAmount(BigDecimal left, BigDecimal right) {
		return safeAmount(left).compareTo(safeAmount(right)) == 0;
	}

	private void validatePublicFinancialInputs(
		List<PublicCustomerIncome> incomes,
		List<PublicCustomerLoan> loans,
		List<PublicCustomerCard> cards,
		List<PublicCustomerLiability> liabilities,
		int missedPaymentsCount
	) {
		incomes.forEach(income -> {
			if (safeAmount(income.getAmount()).compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Each public-customer income must be greater than 0 before calculating CreditLens.");
			}
		});
		loans.forEach(loan -> {
			if (safeAmount(loan.getMonthlyEmi()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Public-customer loan EMI values must not be negative before calculating CreditLens.");
			}
		});
		cards.forEach(card -> {
			if (safeAmount(card.getCreditLimit()).compareTo(BigDecimal.ZERO) < 0 || safeAmount(card.getOutstandingBalance()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Public-customer credit-card limits and balances must not be negative before calculating CreditLens.");
			}
		});
		liabilities.forEach(liability -> {
			if (safeAmount(liability.getMonthlyAmount()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Public-customer liability values must not be negative before calculating CreditLens.");
			}
		});
		if (missedPaymentsCount < 0) {
			throw new IllegalArgumentException("Public-customer missed payments must not be negative before calculating CreditLens.");
		}
	}

	private void validateBankFinancialInputs(
		List<BankCustomerIncome> incomes,
		List<BankCustomerLoan> loans,
		List<BankCustomerCard> cards,
		List<BankCustomerLiability> liabilities,
		int missedPaymentsCount
	) {
		incomes.forEach(income -> {
			if (safeAmount(income.getAmount()).compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Each bank-customer income must be greater than 0 before calculating CreditLens.");
			}
		});
		loans.forEach(loan -> {
			if (safeAmount(loan.getMonthlyEmi()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Bank-customer loan EMI values must not be negative before calculating CreditLens.");
			}
		});
		cards.forEach(card -> {
			if (safeAmount(card.getCreditLimit()).compareTo(BigDecimal.ZERO) < 0 || safeAmount(card.getOutstandingBalance()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Bank-customer credit-card limits and balances must not be negative before calculating CreditLens.");
			}
		});
		liabilities.forEach(liability -> {
			if (safeAmount(liability.getMonthlyAmount()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Bank-customer liability values must not be negative before calculating CreditLens.");
			}
		});
		if (missedPaymentsCount < 0) {
			throw new IllegalArgumentException("Bank-customer missed payments must not be negative before calculating CreditLens.");
		}
	}

	private BigDecimal estimateCardMinimumPayment(BigDecimal totalCardOutstanding) {
		return safeAmount(totalCardOutstanding)
			.multiply(ESTIMATED_CARD_MIN_PAYMENT_RATIO)
			.setScale(2, RoundingMode.HALF_UP);
	}

	private String normalizeTrendRange(String range) {
		if (range == null || range.isBlank()) {
			return "6m";
		}
		String normalized = range.trim().toLowerCase(Locale.ROOT);
		if (!"6m".equals(normalized) && !"12m".equals(normalized)) {
			throw new IllegalArgumentException("Trend range must be 6m or 12m.");
		}
		return normalized;
	}

	private String resolveBadgeText(int value, int max) {
		if (value <= 0) {
			return "LOW";
		}
		BigDecimal ratio = BigDecimal.valueOf(value)
			.divide(BigDecimal.valueOf(Math.max(1, max)), 4, RoundingMode.HALF_UP);
		if (ratio.compareTo(BigDecimal.ONE) >= 0) {
			return "MAX RISK";
		}
		if (ratio.compareTo(new BigDecimal("0.66")) >= 0) {
			return "HIGH";
		}
		if (ratio.compareTo(new BigDecimal("0.33")) >= 0) {
			return "MEDIUM";
		}
		return "LOW";
	}

	private String resolveBadgeTone(int value, int max) {
		String badgeText = resolveBadgeText(value, max);
		return switch (badgeText) {
			case "MAX RISK" -> "red";
			case "HIGH" -> "orange";
			case "MEDIUM" -> "amber";
			default -> "green";
		};
	}

	private String resolveFactorColor(int value, int max) {
		BigDecimal ratio = BigDecimal.valueOf(value)
			.divide(BigDecimal.valueOf(Math.max(1, max)), 4, RoundingMode.HALF_UP);
		if (ratio.compareTo(new BigDecimal("0.33")) <= 0) {
			return "#34d399";
		}
		if (ratio.compareTo(new BigDecimal("0.66")) <= 0) {
			return "#fbbf24";
		}
		return "#ef4444";
	}

	private BigDecimal toPercentage(BigDecimal ratio) {
		return safeAmount(ratio)
			.multiply(new BigDecimal("100"))
			.setScale(1, RoundingMode.HALF_UP);
	}

	private String formatPercentageLabel(BigDecimal ratio) {
		return toPercentage(ratio).stripTrailingZeros().toPlainString() + "%";
	}

	private String toRiskDisplayLabel(String riskLevel) {
		return toTitleCase(riskLevel);
	}

	private String toRiskSummaryLabel(String riskLevel) {
		return toTitleCase(riskLevel) + " Risk";
	}

	private String resolveDtiBand(BigDecimal dtiRatio) {
		if (safeAmount(dtiRatio).compareTo(new BigDecimal("0.30")) <= 0) {
			return "Low";
		}
		if (safeAmount(dtiRatio).compareTo(new BigDecimal("0.50")) <= 0) {
			return "Medium";
		}
		return "High";
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

	private record EvaluationView(
		Long evaluationId,
		Long recordId,
		String scope,
		String evaluationType,
		Integer totalRiskPoints,
		String riskLevel,
		BigDecimal totalMonthlyIncome,
		BigDecimal totalMonthlyDebtPayment,
		BigDecimal totalCardLimit,
		BigDecimal totalCardOutstanding,
		BigDecimal dtiRatio,
		BigDecimal creditUtilizationRatio,
		Integer activeFacilitiesCount,
		Integer missedPaymentsCount,
		Integer paymentHistoryPoints,
		Integer dtiPoints,
		Integer utilizationPoints,
		Integer incomeStabilityPoints,
		Integer exposurePoints,
		LocalDateTime createdAt
	) {
	}

	private record RecordBreakdown(
		BigDecimal income,
		BigDecimal loanEmi,
		BigDecimal creditCardBalance,
		BigDecimal creditCardLimit,
		BigDecimal otherLiabilities,
		BigDecimal estimatedCardMinimumPayment
	) {
	}

	private record InsightCandidate(
		int priority,
		CreditInsightItemResponse item
	) {
	}

	private record FactorSnapshot(
		String title,
		int points,
		int maxPoints,
		String iconKey,
		String description,
		String detail,
		CreditInfoTooltipResponse infoTooltip
	) {
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
