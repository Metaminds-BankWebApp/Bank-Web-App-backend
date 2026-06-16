package com.bank_web_app.backend.creditlens.service;

import static com.bank_web_app.backend.creditlens.service.CreditEvaluationText.normalizeText;
import static com.bank_web_app.backend.creditlens.service.CreditEvaluationText.safe;
import static com.bank_web_app.backend.creditlens.service.CreditEvaluationText.toTitleCase;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.creditlens.dto.request.CreateBankCreditEvaluationRequest;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisCustomerProfileResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisCustomerRowResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisDashboardResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditEvaluationResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditEvaluationSummaryResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditDashboardResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditInsightsResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditReportResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditTrendResponse;
import com.bank_web_app.backend.creditlens.dto.response.SelfCreditEvaluationResponse;
import com.bank_web_app.backend.creditlens.dto.response.SelfCreditEvaluationSummaryResponse;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.entity.SelfCreditEvaluation;
import com.bank_web_app.backend.creditlens.mapper.CreditEvaluationMapper;
import com.bank_web_app.backend.creditlens.repository.BankCreditEvaluationRepository;
import com.bank_web_app.backend.creditlens.repository.SelfCreditEvaluationRepository;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller-facing CreditLens orchestration service.
 * Detailed auth, data loading, scoring, and response building live in focused collaborators.
 */
@Service
public class CreditEvaluationService {

	private static final Set<String> BANK_EVALUATION_SOURCES = Set.of("MANUAL", "CRIB_MERGED", "CRIB_ONLY");

	private final SelfCreditEvaluationRepository selfCreditEvaluationRepository;
	private final BankCreditEvaluationRepository bankCreditEvaluationRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final CreditEvaluationMapper creditEvaluationMapper;
	private final CreditReportPdfExportService creditReportPdfExportService;
	private final CreditEvaluationAuthService creditEvaluationAuthService;
	private final CreditEvaluationRecordService creditEvaluationRecordService;
	private final CreditEvaluationScoringService creditEvaluationScoringService;
	private final CreditEvaluationViewMapper creditEvaluationViewMapper;
	private final CreditEvaluationResponseService creditEvaluationResponseService;

	// Wires the collaborators used to orchestrate CreditLens workflows.
	public CreditEvaluationService(
		SelfCreditEvaluationRepository selfCreditEvaluationRepository,
		BankCreditEvaluationRepository bankCreditEvaluationRepository,
		BankCustomerRepository bankCustomerRepository,
		CreditEvaluationMapper creditEvaluationMapper,
		CreditReportPdfExportService creditReportPdfExportService,
		CreditEvaluationAuthService creditEvaluationAuthService,
		CreditEvaluationRecordService creditEvaluationRecordService,
		CreditEvaluationScoringService creditEvaluationScoringService,
		CreditEvaluationViewMapper creditEvaluationViewMapper,
		CreditEvaluationResponseService creditEvaluationResponseService
	) {
		this.selfCreditEvaluationRepository = selfCreditEvaluationRepository;
		this.bankCreditEvaluationRepository = bankCreditEvaluationRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.creditEvaluationMapper = creditEvaluationMapper;
		this.creditReportPdfExportService = creditReportPdfExportService;
		this.creditEvaluationAuthService = creditEvaluationAuthService;
		this.creditEvaluationRecordService = creditEvaluationRecordService;
		this.creditEvaluationScoringService = creditEvaluationScoringService;
		this.creditEvaluationViewMapper = creditEvaluationViewMapper;
		this.creditEvaluationResponseService = creditEvaluationResponseService;
	}

	@Transactional
	// Creates and returns a new self evaluation for the logged-in public customer.
	public SelfCreditEvaluationResponse createSelfEvaluation() {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		PublicCustomerFinancialRecord record = creditEvaluationRecordService.resolveCurrentPublicFinancialRecord(profile.getPublicCustomerId());
		return creditEvaluationMapper.toSelfResponse(createSelfEvaluation(profile, record));
	}

	@Transactional
	// Returns the latest self evaluation, creating one when financial data changed.
	public SelfCreditEvaluationResponse getCurrentSelfEvaluation() {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		return creditEvaluationMapper.toSelfResponse(getOrCreateLatestSelfEvaluation(profile));
	}

	@Transactional
	// Returns the public customer's self evaluation history.
	public List<SelfCreditEvaluationSummaryResponse> getSelfEvaluationHistory() {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		getOrCreateLatestSelfEvaluation(profile);
		return selfCreditEvaluationRepository
			.findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(profile.getPublicCustomerId())
			.stream()
			.map(this::synchronizeSelfEvaluation)
			.map(creditEvaluationMapper::toSelfSummary)
			.toList();
	}

	@Transactional
	// Returns one self evaluation after confirming it belongs to the public customer.
	public SelfCreditEvaluationResponse getSelfEvaluationById(Long selfEvaluationId) {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		SelfCreditEvaluation evaluation = selfCreditEvaluationRepository
			.findBySelfEvaluationIdAndPublicCustomer_PublicCustomerId(selfEvaluationId, profile.getPublicCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Self credit evaluation not found for this public customer."));
		return creditEvaluationMapper.toSelfResponse(synchronizeSelfEvaluation(evaluation));
	}

	@Transactional
	// Returns the latest bank evaluation for the logged-in bank customer.
	public BankCreditEvaluationResponse getCurrentBankEvaluationForCustomer() {
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveLoggedInBankCustomer();
		BankCreditEvaluation evaluation = getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return creditEvaluationMapper.toBankResponse(evaluation);
	}

	@Transactional
	// Returns bank evaluation history for the logged-in bank customer.
	public List<BankCreditEvaluationSummaryResponse> getBankEvaluationHistoryForCustomer() {
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveLoggedInBankCustomer();
		getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return bankCreditEvaluationRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.stream()
			.map(this::synchronizeBankEvaluation)
			.map(creditEvaluationMapper::toBankSummary)
			.toList();
	}

	@Transactional
	// Returns one bank evaluation after confirming it belongs to the customer.
	public BankCreditEvaluationResponse getBankEvaluationByIdForCustomer(Long bankEvaluationId) {
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveLoggedInBankCustomer();
		BankCreditEvaluation evaluation = bankCreditEvaluationRepository
			.findByBankEvaluationIdAndBankCustomer_BankCustomerId(bankEvaluationId, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Bank credit evaluation not found for this bank customer."));
		return creditEvaluationMapper.toBankResponse(synchronizeBankEvaluation(evaluation));
	}

	@Transactional
	// Builds the public customer's CreditLens dashboard response.
	public CreditDashboardResponse getPublicDashboard() {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		SelfCreditEvaluation currentEvaluation = getOrCreateLatestSelfEvaluation(profile);
		List<EvaluationView> views = getPublicEvaluationViews(profile);
		return creditEvaluationResponseService.buildDashboardResponse(creditEvaluationViewMapper.toView(currentEvaluation), views);
	}

	@Transactional
	// Builds the bank customer's CreditLens dashboard response.
	public CreditDashboardResponse getBankDashboard() {
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveLoggedInBankCustomer();
		BankCreditEvaluation currentEvaluation = getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		List<EvaluationView> views = getBankEvaluationViews(bankCustomer);
		return creditEvaluationResponseService.buildDashboardResponse(creditEvaluationViewMapper.toView(currentEvaluation), views);
	}

	@Transactional
	// Builds public customer trend data for the requested range.
	public CreditTrendResponse getPublicTrends(String range) {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		getOrCreateLatestSelfEvaluation(profile);
		return creditEvaluationResponseService.buildTrendResponse(
			getPublicEvaluationViews(profile),
			creditEvaluationResponseService.normalizeTrendRange(range)
		);
	}

	@Transactional
	// Builds bank customer trend data for the requested range.
	public CreditTrendResponse getBankTrends(String range) {
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveLoggedInBankCustomer();
		getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return creditEvaluationResponseService.buildTrendResponse(
			getBankEvaluationViews(bankCustomer),
			creditEvaluationResponseService.normalizeTrendRange(range)
		);
	}

	@Transactional
	// Builds insight cards for the logged-in public customer.
	public CreditInsightsResponse getPublicInsights() {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		SelfCreditEvaluation currentEvaluation = getOrCreateLatestSelfEvaluation(profile);
		List<EvaluationView> views = getPublicEvaluationViews(profile);
		EvaluationView currentView = creditEvaluationViewMapper.toView(currentEvaluation);
		return creditEvaluationResponseService.buildInsightsResponse(
			currentView,
			views,
			creditEvaluationRecordService.loadRecordBreakdown(currentView)
		);
	}

	@Transactional
	// Builds insight cards for the logged-in bank customer.
	public CreditInsightsResponse getBankInsights() {
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveLoggedInBankCustomer();
		BankCreditEvaluation currentEvaluation = getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		List<EvaluationView> views = getBankEvaluationViews(bankCustomer);
		EvaluationView currentView = creditEvaluationViewMapper.toView(currentEvaluation);
		return creditEvaluationResponseService.buildInsightsResponse(
			currentView,
			views,
			creditEvaluationRecordService.loadRecordBreakdown(currentView)
		);
	}

	@Transactional
	// Builds monthly report data for the logged-in public customer.
	public CreditReportResponse getPublicReport() {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		getOrCreateLatestSelfEvaluation(profile);
		return creditEvaluationResponseService.buildReportResponse("PUBLIC_CUSTOMER", "Self Assessment", getPublicEvaluationViews(profile));
	}

	@Transactional
	// Exports a public customer's selected self evaluation as a PDF.
	public byte[] getPublicReportPdf(Long selfEvaluationId) {
		PublicCustomerProfile profile = creditEvaluationAuthService.resolveLoggedInPublicCustomerProfile();
		SelfCreditEvaluation evaluation = selfCreditEvaluationRepository
			.findBySelfEvaluationIdAndPublicCustomer_PublicCustomerId(selfEvaluationId, profile.getPublicCustomerId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit report was not found for this evaluation."));
		SelfCreditEvaluation synchronizedEvaluation = synchronizeSelfEvaluation(evaluation);
		EvaluationView view = creditEvaluationViewMapper.toView(synchronizedEvaluation);
		RecordBreakdown breakdown = creditEvaluationRecordService.loadRecordBreakdown(view);
		byte[] file = creditReportPdfExportService.exportReport(
			new CreditReportPdfExportService.CreditReportPdfModel(
				resolvePublicCustomerDisplayName(profile),
				resolvePublicCustomerCode(profile),
				creditEvaluationResponseService.formatMonthLabel(view),
				view.evaluationType(),
				view.totalRiskPoints(),
				creditEvaluationResponseService.toRiskDisplayLabel(view.riskLevel()),
				creditEvaluationResponseService.formatExportTimestamp(LocalDateTime.now()),
				creditEvaluationResponseService.formatExportTimestamp(view.createdAt()),
				breakdown.income(),
				breakdown.loanEmi(),
				creditEvaluationRecordService.loadPublicLoanRemainingBalance(view.recordId()),
				breakdown.creditCardBalance(),
				breakdown.creditCardLimit(),
				breakdown.otherLiabilities(),
				view.missedPaymentsCount(),
				view.activeFacilitiesCount(),
				creditEvaluationResponseService.toPercentageValue(view.dtiRatio()),
				creditEvaluationResponseService.toPercentageValue(view.creditUtilizationRatio()),
				creditEvaluationResponseService.resolveDtiBand(view.dtiRatio()),
				creditEvaluationResponseService.buildRiskFactors(view)
			)
		);
		if (!Boolean.TRUE.equals(synchronizedEvaluation.getReportGenerated())) {
			synchronizedEvaluation.setReportGenerated(Boolean.TRUE);
			selfCreditEvaluationRepository.save(synchronizedEvaluation);
		}
		return file;
	}

	@Transactional
	// Builds monthly report data for the logged-in bank customer.
	public CreditReportResponse getBankReport() {
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveLoggedInBankCustomer();
		getOrCreateLatestBankEvaluationForCustomer(bankCustomer);
		return creditEvaluationResponseService.buildReportResponse("BANK_CUSTOMER", "Bank Assessment", getBankEvaluationViews(bankCustomer));
	}

	@Transactional
	// Exports a bank customer's selected evaluation as a PDF.
	public byte[] getBankReportPdf(Long bankEvaluationId) {
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveLoggedInBankCustomer();
		BankCreditEvaluation evaluation = bankCreditEvaluationRepository
			.findByBankEvaluationIdAndBankCustomer_BankCustomerId(bankEvaluationId, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit report was not found for this evaluation."));
		return exportBankCreditReportPdf(bankCustomer, synchronizeBankEvaluation(evaluation));
	}

	@Transactional
	// Builds the bank officer dashboard from assigned customer evaluations.
	public BankCreditAnalysisDashboardResponse getOfficerDashboard() {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		List<BankCreditAnalysisCustomerRowResponse> rows = bankCustomerRepository
			.findAllByOfficer_OfficerIdOrderByUpdatedAtDesc(officer.getOfficerId())
			.stream()
			.map(customer -> bankCreditEvaluationRepository
				.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(customer.getBankCustomerId())
				.map(this::synchronizeBankEvaluation)
				.orElse(null))
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
	// Builds a profile summary for a bank customer assigned to the officer.
	public BankCreditAnalysisCustomerProfileResponse getOfficerCustomerProfile(Long bankCustomerId) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
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
	// Creates a bank evaluation for an officer-owned bank customer.
	public BankCreditEvaluationResponse createBankEvaluationForOfficer(
		Long bankCustomerId,
		CreateBankCreditEvaluationRequest request
	) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCustomerFinancialRecord record = creditEvaluationRecordService.resolveLatestBankFinancialRecord(bankCustomer.getBankCustomerId());
		String evaluationSource = normalizeBankEvaluationSource(request == null ? null : request.evaluationSource());
		String remarks = normalizeOptionalText(request == null ? null : request.remarks());
		return creditEvaluationMapper.toBankResponse(createBankEvaluation(bankCustomer, record, officer, evaluationSource, remarks));
	}

	@Transactional
	// Returns the latest evaluation for an officer-owned bank customer.
	public BankCreditEvaluationResponse getCurrentBankEvaluationForOfficer(Long bankCustomerId) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
		return creditEvaluationMapper.toBankResponse(getOrCreateLatestBankEvaluationForCustomer(bankCustomer));
	}

	@Transactional
	// Builds trend data for an officer-owned bank customer.
	public CreditTrendResponse getOfficerCustomerTrends(Long bankCustomerId, String range) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
		getOrCreateLatestBankEvaluation(bankCustomer, officer);
		return creditEvaluationResponseService.buildTrendResponse(
			getBankEvaluationViews(bankCustomer),
			creditEvaluationResponseService.normalizeTrendRange(range)
		);
	}

	@Transactional
	// Builds insight cards for an officer-owned bank customer.
	public CreditInsightsResponse getOfficerCustomerInsights(Long bankCustomerId) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCreditEvaluation currentEvaluation = getOrCreateLatestBankEvaluation(bankCustomer, officer);
		List<EvaluationView> views = getBankEvaluationViews(bankCustomer);
		EvaluationView currentView = creditEvaluationViewMapper.toView(currentEvaluation);
		return creditEvaluationResponseService.buildInsightsResponse(
			currentView,
			views,
			creditEvaluationRecordService.loadRecordBreakdown(currentView)
		);
	}

	@Transactional
	// Builds monthly report data for an officer-owned bank customer.
	public CreditReportResponse getOfficerCustomerReport(Long bankCustomerId) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
		getOrCreateLatestBankEvaluation(bankCustomer, officer);
		return creditEvaluationResponseService.buildReportResponse("BANK_CUSTOMER", "Bank Assessment", getBankEvaluationViews(bankCustomer));
	}

	@Transactional
	// Exports a selected officer-owned bank customer evaluation as a PDF.
	public byte[] getOfficerCustomerReportPdf(Long bankCustomerId, Long bankEvaluationId) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCreditEvaluation evaluation = bankCreditEvaluationRepository
			.findByBankEvaluationIdAndBankCustomer_BankCustomerId(bankEvaluationId, bankCustomerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit report was not found for this evaluation."));
		return exportBankCreditReportPdf(bankCustomer, synchronizeBankEvaluation(evaluation));
	}

	@Transactional
	// Returns or creates the latest bank evaluation for a bank customer.
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
	// Returns bank evaluation history for an officer-owned bank customer.
	public List<BankCreditEvaluationSummaryResponse> getBankEvaluationHistoryForOfficer(Long bankCustomerId) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		BankCustomer bankCustomer = creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
		getOrCreateLatestBankEvaluation(bankCustomer, officer);
		return bankCreditEvaluationRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.stream()
			.map(this::synchronizeBankEvaluation)
			.map(creditEvaluationMapper::toBankSummary)
			.toList();
	}

	@Transactional
	// Returns one evaluation for an officer-owned bank customer.
	public BankCreditEvaluationResponse getBankEvaluationByIdForOfficer(Long bankCustomerId, Long bankEvaluationId) {
		BankOfficer officer = creditEvaluationAuthService.resolveLoggedInBankOfficer();
		creditEvaluationAuthService.resolveOwnedBankCustomer(bankCustomerId, officer);
		BankCreditEvaluation evaluation = bankCreditEvaluationRepository
			.findByBankEvaluationIdAndBankCustomer_BankCustomerId(bankEvaluationId, bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank credit evaluation not found for this bank customer."));
		return creditEvaluationMapper.toBankResponse(synchronizeBankEvaluation(evaluation));
	}

	// Prepares and exports the shared bank-customer PDF report.
	private byte[] exportBankCreditReportPdf(BankCustomer bankCustomer, BankCreditEvaluation evaluation) {
		EvaluationView view = creditEvaluationViewMapper.toView(evaluation);
		RecordBreakdown breakdown = creditEvaluationRecordService.loadRecordBreakdown(view);
		byte[] file = creditReportPdfExportService.exportReport(
			new CreditReportPdfExportService.CreditReportPdfModel(
				resolveBankCustomerDisplayName(bankCustomer),
				resolveBankCustomerCode(bankCustomer),
				creditEvaluationResponseService.formatMonthLabel(view),
				view.evaluationType(),
				view.totalRiskPoints(),
				creditEvaluationResponseService.toRiskDisplayLabel(view.riskLevel()),
				creditEvaluationResponseService.formatExportTimestamp(LocalDateTime.now()),
				creditEvaluationResponseService.formatExportTimestamp(view.createdAt()),
				breakdown.income(),
				breakdown.loanEmi(),
				creditEvaluationRecordService.loadBankLoanRemainingBalance(view.recordId()),
				breakdown.creditCardBalance(),
				breakdown.creditCardLimit(),
				breakdown.otherLiabilities(),
				view.missedPaymentsCount(),
				view.activeFacilitiesCount(),
				creditEvaluationResponseService.toPercentageValue(view.dtiRatio()),
				creditEvaluationResponseService.toPercentageValue(view.creditUtilizationRatio()),
				creditEvaluationResponseService.resolveDtiBand(view.dtiRatio()),
				creditEvaluationResponseService.buildRiskFactors(view)
			)
		);

		if (!Boolean.TRUE.equals(evaluation.getReportGenerated())) {
			evaluation.setReportGenerated(Boolean.TRUE);
			bankCreditEvaluationRepository.save(evaluation);
		}

		return file;
	}

	// Gets the latest self evaluation or creates one when the record changed.
	private SelfCreditEvaluation getOrCreateLatestSelfEvaluation(PublicCustomerProfile profile) {
		PublicCustomerFinancialRecord currentRecord = creditEvaluationRecordService.resolveCurrentPublicFinancialRecord(profile.getPublicCustomerId());
		SelfCreditEvaluation latestEvaluation = selfCreditEvaluationRepository
			.findTopByPublicRecord_RecordIdOrderByCreatedAtDesc(currentRecord.getRecordId())
			.orElse(null);

		if (
			latestEvaluation != null &&
			!creditEvaluationRecordService.isRecordUpdatedAfterEvaluation(currentRecord.getUpdatedAt(), latestEvaluation.getCreatedAt())
		) {
			return synchronizeSelfEvaluation(latestEvaluation);
		}

		return createSelfEvaluation(profile, currentRecord);
	}

	// Creates and saves a self evaluation from a public financial record.
	private SelfCreditEvaluation createSelfEvaluation(
		PublicCustomerProfile profile,
		PublicCustomerFinancialRecord record
	) {
		EvaluationMetrics metrics = creditEvaluationScoringService.buildPublicEvaluationMetrics(record);

		SelfCreditEvaluation evaluation = new SelfCreditEvaluation();
		evaluation.setPublicCustomer(profile);
		evaluation.setPublicRecord(record);
		creditEvaluationScoringService.applyCommonMetricsToSelfEvaluation(evaluation, metrics);
		return selfCreditEvaluationRepository.save(evaluation);
	}

	// Gets the latest bank evaluation or creates one when the record changed.
	private BankCreditEvaluation getOrCreateLatestBankEvaluation(BankCustomer bankCustomer, BankOfficer officer) {
		BankCustomerFinancialRecord latestRecord = creditEvaluationRecordService.resolveLatestBankFinancialRecord(bankCustomer.getBankCustomerId());
		BankCreditEvaluation latestEvaluation = bankCreditEvaluationRepository
			.findTopByBankRecord_BankRecordIdOrderByCreatedAtDesc(latestRecord.getBankRecordId())
			.orElse(null);

		if (
			latestEvaluation != null &&
			!creditEvaluationRecordService.isRecordUpdatedAfterEvaluation(latestRecord.getUpdatedAt(), latestEvaluation.getCreatedAt())
		) {
			return synchronizeBankEvaluation(latestEvaluation);
		}

		return createBankEvaluation(bankCustomer, latestRecord, officer, "MANUAL", null);
	}

	// Creates and saves a bank evaluation from a bank financial record.
	private BankCreditEvaluation createBankEvaluation(
		BankCustomer bankCustomer,
		BankCustomerFinancialRecord record,
		BankOfficer officer,
		String evaluationSource,
		String remarks
	) {
		EvaluationMetrics metrics = creditEvaluationScoringService.buildBankEvaluationMetrics(record);

		BankCreditEvaluation evaluation = new BankCreditEvaluation();
		evaluation.setBankCustomer(bankCustomer);
		evaluation.setBankRecord(record);
		evaluation.setEvaluatedByOfficer(officer);
		evaluation.setEvaluationSource(evaluationSource);
		evaluation.setRemarks(remarks);
		creditEvaluationScoringService.applyCommonMetricsToBankEvaluation(evaluation, metrics);
		return bankCreditEvaluationRepository.save(evaluation);
	}

	// Normalizes and validates the source used for a bank evaluation.
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

	// Trims optional remarks and stores null for blank text.
	private String normalizeOptionalText(String value) {
		String normalized = value == null ? null : value.trim();
		return normalized == null || normalized.isBlank() ? null : normalized;
	}

	// Loads all self evaluations as shared view objects for public UI sections.
	private List<EvaluationView> getPublicEvaluationViews(PublicCustomerProfile profile) {
		return selfCreditEvaluationRepository
			.findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(profile.getPublicCustomerId())
			.stream()
			.map(this::synchronizeSelfEvaluation)
			.map(creditEvaluationViewMapper::toView)
			.toList();
	}

	// Loads all bank evaluations as shared view objects for bank UI sections.
	private List<EvaluationView> getBankEvaluationViews(BankCustomer bankCustomer) {
		return bankCreditEvaluationRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.stream()
			.map(this::synchronizeBankEvaluation)
			.map(creditEvaluationViewMapper::toView)
			.toList();
	}

	// Refreshes a self evaluation when financial inputs no longer match.
	private SelfCreditEvaluation synchronizeSelfEvaluation(SelfCreditEvaluation evaluation) {
		EvaluationMetrics metrics = creditEvaluationScoringService.buildPublicEvaluationMetrics(evaluation.getPublicRecord());
		if (!creditEvaluationScoringService.matchesSelfEvaluationMetrics(evaluation, metrics)) {
			Boolean existingReportGenerated = evaluation.getReportGenerated();
			creditEvaluationScoringService.applyCommonMetricsToSelfEvaluation(evaluation, metrics);
			evaluation.setReportGenerated(existingReportGenerated == null ? Boolean.FALSE : existingReportGenerated);
			return selfCreditEvaluationRepository.save(evaluation);
		}
		return evaluation;
	}

	// Refreshes a bank evaluation when financial inputs no longer match.
	private BankCreditEvaluation synchronizeBankEvaluation(BankCreditEvaluation evaluation) {
		EvaluationMetrics metrics = creditEvaluationScoringService.buildBankEvaluationMetrics(evaluation.getBankRecord());
		if (!creditEvaluationScoringService.matchesBankEvaluationMetrics(evaluation, metrics)) {
			Boolean existingReportGenerated = evaluation.getReportGenerated();
			creditEvaluationScoringService.applyCommonMetricsToBankEvaluation(evaluation, metrics);
			evaluation.setReportGenerated(existingReportGenerated == null ? Boolean.FALSE : existingReportGenerated);
			return bankCreditEvaluationRepository.save(evaluation);
		}
		return evaluation;
	}

	// Builds a full name from user first and last name fields.
	private String buildFullName(User user) {
		return (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
	}

	// Resolves the display name shown on public customer reports.
	private String resolvePublicCustomerDisplayName(PublicCustomerProfile profile) {
		User user = profile.getUser();
		String fullName = buildFullName(user);
		if (!fullName.isBlank()) {
			return fullName;
		}
		String username = safe(user.getUsername());
		return username.isBlank() ? "Public Customer" : username;
	}

	// Resolves the customer code shown on public customer reports.
	private String resolvePublicCustomerCode(PublicCustomerProfile profile) {
		String customerCode = safe(profile.getCustomerCode());
		return customerCode.isBlank() ? "N/A" : customerCode;
	}

	// Resolves the display name shown on bank customer reports.
	private String resolveBankCustomerDisplayName(BankCustomer bankCustomer) {
		User user = bankCustomer.getUser();
		String fullName = buildFullName(user);
		if (!fullName.isBlank()) {
			return fullName;
		}
		String username = safe(user.getUsername());
		return username.isBlank() ? "Bank Customer" : username;
	}

	// Resolves the customer code shown on bank customer reports.
	private String resolveBankCustomerCode(BankCustomer bankCustomer) {
		String customerCode = safe(bankCustomer.getCustomerCode());
		return customerCode.isBlank() ? "N/A" : customerCode;
	}
}
