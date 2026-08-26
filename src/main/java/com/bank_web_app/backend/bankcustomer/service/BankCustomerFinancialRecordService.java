package com.bank_web_app.backend.bankcustomer.service;

import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCardStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCribRequestStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCribRetrievalStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerIncomeStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLiabilityStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLoanStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerCribStepResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialStepResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerIdentityResponse;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCard;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCribRequest;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLiability;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLoan;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerMissedPayment;
import com.bank_web_app.backend.bankcustomer.mapper.BankCustomerFinancialRecordMapper;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCribRequestRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerFinancialRecordRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerIncomeRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLiabilityRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLoanRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerMissedPaymentRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.auth.entity.PasswordResetToken;
import com.bank_web_app.backend.auth.repository.PasswordResetTokenRepository;
import com.bank_web_app.backend.common.email.BankCustomerActivationEmailService;
import com.bank_web_app.backend.crib.dto.response.CribDatasetSnapshotResponse;
import com.bank_web_app.backend.crib.service.CribDatasetService;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BankCustomerFinancialRecordService {
	private static final String STATUS_PENDING_STEP_2 = "PENDING_STEP_2";
	private static final String STATUS_PENDING_STEP_3 = "PENDING_STEP_3";
	private static final String STATUS_PENDING_STEP_4 = "PENDING_STEP_4";
	private static final String STATUS_PENDING_STEP_5 = "PENDING_STEP_5";
	private static final String STATUS_PENDING_STEP_6 = "PENDING_STEP_6";
	private static final String STATUS_PENDING_STEP_7 = "PENDING_STEP_7";
	private static final String STATUS_COMPLETED = "COMPLETED";
	private static final String DATA_SOURCE_MAINTENANCE_IN_PROGRESS = "OFFICER_MAINTENANCE";
	private static final String DATA_SOURCE_MAINTENANCE_FINALIZED = "OFFICER_MAINTENANCE_FINAL";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final BankCustomerRepository bankCustomerRepository;
	private final BankCustomerFinancialRecordRepository financialRecordRepository;
	private final BankCustomerIncomeRepository incomeRepository;
	private final BankCustomerLoanRepository loanRepository;
	private final BankCustomerCardRepository cardRepository;
	private final BankCustomerLiabilityRepository liabilityRepository;
	private final BankCustomerMissedPaymentRepository missedPaymentRepository;
	private final BankCustomerCribRequestRepository cribRequestRepository;
	private final CribDatasetService cribDatasetService;
	private final BankCustomerFinancialRecordMapper financialRecordMapper;
	private final com.bank_web_app.backend.bankofficer.service.BankOfficerContextService bankOfficerContextService;
	private final UserRepository userRepository;
	private final BankOfficerRepository bankOfficerRepository;
    private final com.bank_web_app.backend.creditlens.service.CreditEvaluationService creditEvaluationService;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final BankCustomerActivationEmailService customerActivationEmailService;

	public BankCustomerFinancialRecordService(
		BankCustomerRepository bankCustomerRepository,
		BankCustomerFinancialRecordRepository financialRecordRepository,
		BankCustomerIncomeRepository incomeRepository,
		BankCustomerLoanRepository loanRepository,
		BankCustomerCardRepository cardRepository,
		BankCustomerLiabilityRepository liabilityRepository,
		BankCustomerMissedPaymentRepository missedPaymentRepository,
		BankCustomerCribRequestRepository cribRequestRepository,
		CribDatasetService cribDatasetService,
		BankCustomerFinancialRecordMapper financialRecordMapper,
		com.bank_web_app.backend.bankofficer.service.BankOfficerContextService bankOfficerContextService,
		UserRepository userRepository,
		BankOfficerRepository bankOfficerRepository,
		com.bank_web_app.backend.creditlens.service.CreditEvaluationService creditEvaluationService,
		PasswordResetTokenRepository passwordResetTokenRepository,
		PasswordEncoder passwordEncoder,
		BankCustomerActivationEmailService customerActivationEmailService
	) {
		this.bankCustomerRepository = bankCustomerRepository;
		this.financialRecordRepository = financialRecordRepository;
		this.incomeRepository = incomeRepository;
		this.loanRepository = loanRepository;
		this.cardRepository = cardRepository;
		this.liabilityRepository = liabilityRepository;
		this.missedPaymentRepository = missedPaymentRepository;
		this.cribRequestRepository = cribRequestRepository;
		this.cribDatasetService = cribDatasetService;
		this.financialRecordMapper = financialRecordMapper;
		this.bankOfficerContextService = bankOfficerContextService;
		this.userRepository = userRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.creditEvaluationService = creditEvaluationService;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.customerActivationEmailService = customerActivationEmailService;
	}

	@Transactional(readOnly = true)
	public BankOfficerCustomerIdentityResponse getOwnedBankCustomerIdentityByUserId(Long userId) {
		resolveLoggedInBankOfficer();
		BankCustomer customer = bankCustomerRepository
			.findByUser_UserId(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank customer not found."));

		return new BankOfficerCustomerIdentityResponse(
			customer.getBankCustomerId(),
			customer.getUser().getUserId(),
			customer.getCustomerCode()
		);
	}

	@Transactional
	public BankCustomerFinancialStepResponse saveIncomeStepDraft(Long bankCustomerId, BankCustomerIncomeStepRequest request) {
		return doSaveIncomeStep(bankCustomerId, request);
	}

	@Transactional
	public BankCustomerFinancialStepResponse saveIncomeStepAndContinue(Long bankCustomerId, BankCustomerIncomeStepRequest request) {
		BankCustomerFinancialStepResponse response = doSaveIncomeStep(bankCustomerId, request);
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		ensureCustomerReachedStep(customer, STATUS_PENDING_STEP_3);
		advanceCustomerStepIfNeeded(customer, STATUS_PENDING_STEP_4);
		return response;
	}

	private BankCustomerFinancialStepResponse doSaveIncomeStep(Long bankCustomerId, BankCustomerIncomeStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = getOrCreateLatestRecord(customer);
		Long bankRecordId = currentRecord.getBankRecordId();

		incomeRepository.deleteByFinancialRecord_BankRecordId(bankRecordId);

		for (BankCustomerIncomeStepRequest.IncomeItem incomeItem : request.incomes()) {
			BankCustomerIncome income = new BankCustomerIncome();
			income.setFinancialRecord(currentRecord);
			income.setIncomeCategory(normalizeIncomeCategory(incomeItem.incomeCategory()));
			income.setAmount(incomeItem.amount());
			income.setSalaryType(incomeItem.salaryType());
			income.setEmploymentType(incomeItem.employmentType());
			income.setDurationMonths(incomeItem.durationMonths());
			income.setIncomeStability(incomeItem.incomeStability());
			incomeRepository.save(income);
		}

		touchRecord(currentRecord);
		return new BankCustomerFinancialStepResponse(bankRecordId, bankCustomerId, "INCOME", "Income step saved successfully.");
	}

	@Transactional
	public BankCustomerFinancialStepResponse saveLoanStepDraft(Long bankCustomerId, BankCustomerLoanStepRequest request) {
		return doSaveLoanStep(bankCustomerId, request);
	}

	@Transactional
	public BankCustomerFinancialStepResponse saveLoanStepAndContinue(Long bankCustomerId, BankCustomerLoanStepRequest request) {
		BankCustomerFinancialStepResponse response = doSaveLoanStep(bankCustomerId, request);
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		ensureCustomerReachedStep(customer, STATUS_PENDING_STEP_4);
		advanceCustomerStepIfNeeded(customer, STATUS_PENDING_STEP_5);
		return response;
	}

	private BankCustomerFinancialStepResponse doSaveLoanStep(Long bankCustomerId, BankCustomerLoanStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = getOrCreateLatestRecord(customer);
		Long bankRecordId = currentRecord.getBankRecordId();

		loanRepository.deleteByFinancialRecord_BankRecordId(bankRecordId);

		for (BankCustomerLoanStepRequest.LoanItem loanItem : request.loans()) {
			BankCustomerLoan loan = new BankCustomerLoan();
			loan.setFinancialRecord(currentRecord);
			loan.setLoanType(loanItem.loanType());
			loan.setMonthlyEmi(loanItem.monthlyEmi());
			loan.setRemainingBalance(loanItem.remainingBalance());
			loanRepository.save(loan);
		}

		touchRecord(currentRecord);
		return new BankCustomerFinancialStepResponse(bankRecordId, bankCustomerId, "LOANS", "Loan step saved successfully.");
	}

	@Transactional
	public BankCustomerFinancialStepResponse saveCardStepDraft(Long bankCustomerId, BankCustomerCardStepRequest request) {
		return doSaveCardStep(bankCustomerId, request);
	}

	@Transactional
	public BankCustomerFinancialStepResponse saveCardStepAndContinue(Long bankCustomerId, BankCustomerCardStepRequest request) {
		BankCustomerFinancialStepResponse response = doSaveCardStep(bankCustomerId, request);
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		ensureCustomerReachedStep(customer, STATUS_PENDING_STEP_5);
		advanceCustomerStepIfNeeded(customer, STATUS_PENDING_STEP_6);
		return response;
	}

	private BankCustomerFinancialStepResponse doSaveCardStep(Long bankCustomerId, BankCustomerCardStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = getOrCreateLatestRecord(customer);
		Long bankRecordId = currentRecord.getBankRecordId();

		cardRepository.deleteByFinancialRecord_BankRecordId(bankRecordId);

		for (BankCustomerCardStepRequest.CardItem cardItem : request.cards()) {
			BankCustomerCard card = new BankCustomerCard();
			card.setFinancialRecord(currentRecord);
			card.setProvider(cardItem.provider());
			card.setCreditLimit(cardItem.creditLimit());
			card.setOutstandingBalance(cardItem.outstandingBalance());
			cardRepository.save(card);
		}

		touchRecord(currentRecord);
		return new BankCustomerFinancialStepResponse(bankRecordId, bankCustomerId, "CARDS", "Card step saved successfully.");
	}

	@Transactional
	public BankCustomerFinancialStepResponse saveLiabilityStepDraft(Long bankCustomerId, BankCustomerLiabilityStepRequest request) {
		return doSaveLiabilityStep(bankCustomerId, request);
	}

	@Transactional
	public BankCustomerFinancialStepResponse saveLiabilityStepAndContinue(Long bankCustomerId, BankCustomerLiabilityStepRequest request) {
		BankCustomerFinancialStepResponse response = doSaveLiabilityStep(bankCustomerId, request);
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		ensureCustomerReachedStep(customer, STATUS_PENDING_STEP_6);
		advanceCustomerStepIfNeeded(customer, STATUS_PENDING_STEP_7);
		return response;
	}

	@Transactional
	public BankCustomerCribStepResponse saveCribLinkingStepAndContinue(Long bankCustomerId, BankCustomerCribRequestStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		ensureCustomerReachedStep(customer, STATUS_PENDING_STEP_2);
		String customerNic = customer.getUser() == null ? null : customer.getUser().getNic();
		String requestedNic = request.nic() == null ? "" : request.nic().trim();
		if (!requestedNic.isBlank() && customerNic != null && !customerNic.trim().isBlank() && !requestedNic.equalsIgnoreCase(customerNic.trim())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided NIC does not match the selected bank customer.");
		}
		String nic = requestedNic.isBlank() ? customerNic : requestedNic;
		if (nic == null || nic.trim().isBlank()) {
			throw new IllegalArgumentException("NIC/ID is required to link CRIB data.");
		}
		CribDatasetSnapshotResponse cribSnapshot = null;
		String requestStatus = "COMPLETED";
		String reportStatus = "READY";
		String responseMessage = "CRIB linking step saved successfully.";
		try {
			cribSnapshot = cribDatasetService.lookupSnapshotByNic(nic);
		} catch (ResponseStatusException ex) {
			if (ex.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
				throw ex;
			}
			requestStatus = "FAILED";
			reportStatus = "FAILED";
			responseMessage = "ID not found in CRIB. Continued with manual financial capture.";
		}

		BankCustomerCribRequest cribRequest = new BankCustomerCribRequest();
		cribRequest.setBankCustomer(customer);
		cribRequest.setRequestedByOfficer(customer.getOfficer());
		cribRequest.setRequestType(normalizeRequestType(request.requestType()));
		cribRequest.setRequestStatus(requestStatus);
		cribRequest.setReportStatus(reportStatus);
		LocalDateTime now = LocalDateTime.now();
		cribRequest.setRequestedAt(now);
		cribRequest.setResponseReceivedAt(now);

		BankCustomerCribRequest saved = cribRequestRepository.save(cribRequest);

		advanceCustomerStepIfNeeded(customer, STATUS_PENDING_STEP_3);

		return new BankCustomerCribStepResponse(
			saved.getCribRequestId(),
			bankCustomerId,
			"CRIB_LINKING",
			saved.getRequestStatus(),
			saved.getReportStatus(),
			responseMessage,
			null,
			null,
			cribSnapshot
		);
	}

	@Transactional
	public BankCustomerCribStepResponse saveCribRequestStepAndContinue(Long bankCustomerId, BankCustomerCribRequestStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);

		BankCustomerCribRequest cribRequest = new BankCustomerCribRequest();
		cribRequest.setBankCustomer(customer);
		cribRequest.setRequestedByOfficer(customer.getOfficer());
		cribRequest.setRequestType(normalizeRequestType(request.requestType()));
		cribRequest.setRequestStatus("SUBMITTED");
		cribRequest.setReportStatus("PENDING");
		cribRequest.setRequestedAt(LocalDateTime.now());

		BankCustomerCribRequest saved = cribRequestRepository.save(cribRequest);
		customer.setAccessStatus("PENDING_STEP_7");
		bankCustomerRepository.save(customer);

		return new BankCustomerCribStepResponse(
			saved.getCribRequestId(),
			bankCustomerId,
			"CRIB_REQUEST",
			saved.getRequestStatus(),
			saved.getReportStatus(),
			"CRIB request step saved successfully.",
			null,
			null,
			null
		);
	}

	@Transactional
	public BankCustomerCribStepResponse saveCribRetrievalStepAndContinue(Long bankCustomerId, BankCustomerCribRetrievalStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);

		BankCustomerCribRequest cribRequest = cribRequestRepository
			.findTopByBankCustomer_BankCustomerIdOrderByRequestedAtDesc(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("No CRIB request found for this bank customer."));

		String requestStatus = normalizeRequestStatus(request.requestStatus());
		String reportStatus = normalizeReportStatus(request.reportStatus());
		cribRequest.setRequestStatus(requestStatus);
		cribRequest.setReportStatus(reportStatus);
		if ("READY".equals(reportStatus) || "FAILED".equals(reportStatus)) {
			cribRequest.setResponseReceivedAt(LocalDateTime.now());
		}

		BankCustomerCribRequest saved = cribRequestRepository.save(cribRequest);

		customer.setAccessStatus(STATUS_PENDING_STEP_7);
		bankCustomerRepository.save(customer);

		return new BankCustomerCribStepResponse(
			saved.getCribRequestId(),
			bankCustomerId,
			"CRIB_RETRIEVAL",
			saved.getRequestStatus(),
			saved.getReportStatus(),
			"CRIB retrieval step saved successfully.",
			null,
			null,
			null
		);
	}

	@Transactional
	public BankCustomerCribStepResponse completeCribReviewAndOnboarding(Long bankCustomerId) {
		BankCustomer customer = resolveOwnedBankCustomerForFinalReview(bankCustomerId);
		customer.setAccessStatus(STATUS_COMPLETED);
		bankCustomerRepository.save(customer);
		String activationToken = createActivationToken(customer.getUser());
		customerActivationEmailService.sendActivationEmail(
			customer.getUser().getEmail(), customer.getUser().getFirstName(), customer.getUser().getUsername(), activationToken
		);
		BankCustomerCribRequest latest = cribRequestRepository
			.findTopByBankCustomer_BankCustomerIdOrderByRequestedAtDesc(bankCustomerId)
			.orElse(null);

		Long createdEvalId = null;
		Integer createdEvalPoints = null;
		// After onboarding completes, generate a Bank credit evaluation for this customer and capture its id/points
		try {
			var evalResp = creditEvaluationService.createBankEvaluationForOfficer(bankCustomerId, null);
			if (evalResp != null) {
				createdEvalId = evalResp.bankEvaluationId();
				createdEvalPoints = evalResp.totalRiskPoints();
			}
		} catch (Exception ex) {
			Logger logger = LoggerFactory.getLogger(BankCustomerFinancialRecordService.class);
			logger.warn("Failed to create bank credit evaluation during onboarding for customer {}: {}", bankCustomerId, ex.getMessage());
		}

		return new BankCustomerCribStepResponse(
			latest != null ? latest.getCribRequestId() : null,
			bankCustomerId,
			"CRIB_REVIEW",
			latest != null ? latest.getRequestStatus() : null,
			latest != null ? latest.getReportStatus() : null,
			"Bank customer onboarding completed. A password-setup invitation was sent to the customer.",
			createdEvalId,
			createdEvalPoints,
			null
		);
	}

	/**
	 * Closes a post-onboarding financial maintenance session without changing the
	 * customer's onboarding state. A new credit evaluation is produced from the
	 * maintained snapshot so the review queue uses the current information.
	 */
	@Transactional
	public BankCustomerFinancialStepResponse completeFinancialMaintenance(Long bankCustomerId) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		if (!STATUS_COMPLETED.equals(normalizeAccessStatus(customer.getAccessStatus()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Financial maintenance is available only after onboarding is completed.");
		}
		BankCustomerFinancialRecord currentRecord = financialRecordRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("No financial record found for this bank customer."));
		if (!DATA_SOURCE_MAINTENANCE_IN_PROGRESS.equals(currentRecord.getDataSource())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no financial maintenance session to finalise.");
		}
		currentRecord.setDataSource(DATA_SOURCE_MAINTENANCE_FINALIZED);
		touchRecord(currentRecord);
		try {
			creditEvaluationService.createBankEvaluationForOfficer(bankCustomerId, null);
		} catch (Exception ex) {
			LoggerFactory.getLogger(BankCustomerFinancialRecordService.class)
				.warn("Failed to refresh bank credit evaluation after maintenance for customer {}: {}", bankCustomerId, ex.getMessage());
		}
		return new BankCustomerFinancialStepResponse(
			currentRecord.getBankRecordId(), bankCustomerId, "FINANCIAL_MAINTENANCE",
			"Financial maintenance finalised. Onboarding remains completed and a new review evaluation was requested."
		);
	}

	private BankCustomerFinancialStepResponse doSaveLiabilityStep(Long bankCustomerId, BankCustomerLiabilityStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = getOrCreateLatestRecord(customer);
		Long bankRecordId = currentRecord.getBankRecordId();

		liabilityRepository.deleteByFinancialRecord_BankRecordId(bankRecordId);

		for (BankCustomerLiabilityStepRequest.LiabilityItem liabilityItem : request.liabilities()) {
			BankCustomerLiability liability = new BankCustomerLiability();
			liability.setFinancialRecord(currentRecord);
			liability.setDescription(liabilityItem.description());
			liability.setMonthlyAmount(liabilityItem.monthlyAmount());
			liabilityRepository.save(liability);
		}

		BankCustomerMissedPayment missedPayment = missedPaymentRepository
			.findByFinancialRecord_BankRecordId(bankRecordId)
			.orElseGet(() -> {
				BankCustomerMissedPayment entity = new BankCustomerMissedPayment();
				entity.setFinancialRecord(currentRecord);
				return entity;
			});
		missedPayment.setMissedPayments(request.missedPayments());
		missedPaymentRepository.save(missedPayment);

		touchRecord(currentRecord);
		return new BankCustomerFinancialStepResponse(
			bankRecordId,
			bankCustomerId,
			"LIABILITIES",
			"Liability and missed-payment step saved successfully."
		);
	}

	@Transactional(readOnly = true)
	public BankCustomerFinancialRecordResponse getCurrentFinancialRecord(Long bankCustomerId) {
		resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = financialRecordRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("No financial record found for this bank customer."));

		return mapRecordToResponse(currentRecord);
	}

	@Transactional(readOnly = true)
	public List<BankCustomerFinancialRecordSummaryResponse> getFinancialRecordHistory(Long bankCustomerId) {
		resolveOwnedBankCustomer(bankCustomerId);

		return financialRecordRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.stream()
			.map(financialRecordMapper::toSummary)
			.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public BankCustomerFinancialRecordResponse getFinancialRecordById(Long bankCustomerId, Long bankRecordId) {
		resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord record = financialRecordRepository
			.findByBankRecordIdAndBankCustomer_BankCustomerId(bankRecordId, bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Financial record not found for this bank customer."));

		return mapRecordToResponse(record);
	}

	private BankCustomerFinancialRecordResponse mapRecordToResponse(BankCustomerFinancialRecord record) {
		Long bankRecordId = record.getBankRecordId();
		int missedPayments = missedPaymentRepository
			.findByFinancialRecord_BankRecordId(bankRecordId)
			.map(BankCustomerMissedPayment::getMissedPayments)
			.orElse(0);

		return financialRecordMapper.toResponse(
			record,
			incomeRepository.findAllByFinancialRecord_BankRecordId(bankRecordId),
			loanRepository.findAllByFinancialRecord_BankRecordId(bankRecordId),
			cardRepository.findAllByFinancialRecord_BankRecordId(bankRecordId),
			liabilityRepository.findAllByFinancialRecord_BankRecordId(bankRecordId),
			missedPayments
		);
	}

	private String normalizeIncomeCategory(String incomeCategory) {
		String normalized = incomeCategory == null ? "" : incomeCategory.trim().toUpperCase(Locale.ROOT);
		if ("SALARY WORKER".equals(normalized)) {
			return "SALARY";
		}
		if ("BUSINESS PERSON".equals(normalized)) {
			return "BUSINESS";
		}
		if (!"SALARY".equals(normalized) && !"BUSINESS".equals(normalized)) {
			throw new IllegalArgumentException("Income category must be SALARY or BUSINESS.");
		}
		return normalized;
	}

	private String normalizeRequestType(String requestType) {
		String normalized = requestType == null ? "" : requestType.trim().toUpperCase(Locale.ROOT);
		if (!"FULL_REPORT".equals(normalized) && !"SUMMARY_ONLY".equals(normalized) && !"REFRESH".equals(normalized)) {
			throw new IllegalArgumentException("Request type must be FULL_REPORT, SUMMARY_ONLY, or REFRESH.");
		}
		return normalized;
	}

	private String normalizeRequestStatus(String requestStatus) {
		String normalized = requestStatus == null ? "" : requestStatus.trim().toUpperCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return "COMPLETED";
		}
		if (
			!"PENDING".equals(normalized) &&
			!"SUBMITTED".equals(normalized) &&
			!"IN_PROGRESS".equals(normalized) &&
			!"COMPLETED".equals(normalized) &&
			!"FAILED".equals(normalized) &&
			!"CANCELLED".equals(normalized)
		) {
			throw new IllegalArgumentException("Request status must be PENDING, SUBMITTED, IN_PROGRESS, COMPLETED, FAILED, or CANCELLED.");
		}
		return normalized;
	}

	private String normalizeReportStatus(String reportStatus) {
		String normalized = reportStatus == null ? "" : reportStatus.trim().toUpperCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return "READY";
		}
		if (
			!"NOT_REQUESTED".equals(normalized) &&
			!"PENDING".equals(normalized) &&
			!"PROCESSING".equals(normalized) &&
			!"READY".equals(normalized) &&
			!"FAILED".equals(normalized) &&
			!"EXPIRED".equals(normalized)
		) {
			throw new IllegalArgumentException("Report status must be NOT_REQUESTED, PENDING, PROCESSING, READY, FAILED, or EXPIRED.");
		}
		return normalized;
	}

	private BankCustomerFinancialRecord getOrCreateLatestRecord(BankCustomer customer) {
		Long bankCustomerId = customer.getBankCustomerId();
		BankCustomerFinancialRecord latestRecord = financialRecordRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.orElse(null);
		if (latestRecord == null) {
				BankCustomerFinancialRecord record = new BankCustomerFinancialRecord();
				record.setBankCustomer(customer);
				record.setVerifiedByOfficer(customer.getOfficer());
				record.setDataSource("MANUAL");
				return financialRecordRepository.save(record);
		}
		if (
			STATUS_COMPLETED.equals(normalizeAccessStatus(customer.getAccessStatus())) &&
			!DATA_SOURCE_MAINTENANCE_IN_PROGRESS.equals(latestRecord.getDataSource())
		) {
			return createMaintenanceSnapshot(customer, latestRecord);
		}
		return latestRecord;
	}

	private BankCustomerFinancialRecord createMaintenanceSnapshot(BankCustomer customer, BankCustomerFinancialRecord source) {
		BankCustomerFinancialRecord snapshot = new BankCustomerFinancialRecord();
		snapshot.setBankCustomer(customer);
		snapshot.setVerifiedByOfficer(resolveLoggedInBankOfficer());
		snapshot.setDataSource(DATA_SOURCE_MAINTENANCE_IN_PROGRESS);
		BankCustomerFinancialRecord savedSnapshot = financialRecordRepository.save(snapshot);

		incomeRepository.findAllByFinancialRecord_BankRecordId(source.getBankRecordId()).forEach(item -> {
			BankCustomerIncome copy = new BankCustomerIncome();
			copy.setFinancialRecord(savedSnapshot); copy.setIncomeCategory(item.getIncomeCategory()); copy.setAmount(item.getAmount());
			copy.setSalaryType(item.getSalaryType()); copy.setEmploymentType(item.getEmploymentType());
			copy.setDurationMonths(item.getDurationMonths()); copy.setIncomeStability(item.getIncomeStability()); incomeRepository.save(copy);
		});
		loanRepository.findAllByFinancialRecord_BankRecordId(source.getBankRecordId()).forEach(item -> {
			BankCustomerLoan copy = new BankCustomerLoan();
			copy.setFinancialRecord(savedSnapshot); copy.setLoanType(item.getLoanType()); copy.setMonthlyEmi(item.getMonthlyEmi()); copy.setRemainingBalance(item.getRemainingBalance()); loanRepository.save(copy);
		});
		cardRepository.findAllByFinancialRecord_BankRecordId(source.getBankRecordId()).forEach(item -> {
			BankCustomerCard copy = new BankCustomerCard();
			copy.setFinancialRecord(savedSnapshot); copy.setProvider(item.getProvider()); copy.setCreditLimit(item.getCreditLimit()); copy.setOutstandingBalance(item.getOutstandingBalance()); cardRepository.save(copy);
		});
		liabilityRepository.findAllByFinancialRecord_BankRecordId(source.getBankRecordId()).forEach(item -> {
			BankCustomerLiability copy = new BankCustomerLiability();
			copy.setFinancialRecord(savedSnapshot); copy.setDescription(item.getDescription()); copy.setMonthlyAmount(item.getMonthlyAmount()); liabilityRepository.save(copy);
		});
		missedPaymentRepository.findByFinancialRecord_BankRecordId(source.getBankRecordId()).ifPresent(item -> {
			BankCustomerMissedPayment copy = new BankCustomerMissedPayment();
			copy.setFinancialRecord(savedSnapshot); copy.setMissedPayments(item.getMissedPayments()); missedPaymentRepository.save(copy);
		});
		return savedSnapshot;
	}

	private void touchRecord(BankCustomerFinancialRecord record) {
		record.setUpdatedAt(LocalDateTime.now());
		financialRecordRepository.save(record);
	}

	private String createActivationToken(User user) {
		LocalDateTime now = LocalDateTime.now();
		passwordResetTokenRepository.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getUserId())
			.forEach(token -> token.setConsumedAt(now));
		String rawToken = randomSecret();
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setOtpHash(passwordEncoder.encode(randomSecret()));
		token.setOtpExpiresAt(now.plusHours(24));
		token.setFailedAttempts(0);
		token.setVerifiedAt(now);
		token.setResetTokenHash(sha256Hex(rawToken));
		token.setResetTokenExpiresAt(now.plusHours(24));
		passwordResetTokenRepository.save(token);
		return rawToken;
	}

	private String randomSecret() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String sha256Hex(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder(digest.length * 2);
			for (byte item : digest) result.append(String.format("%02x", item));
			return result.toString();
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to create activation token.", exception);
		}
	}

	private void ensureCustomerReachedStep(BankCustomer customer, String minimumRequiredStatus) {
		String currentStatus = normalizeAccessStatus(customer.getAccessStatus());
		String requiredStatus = normalizeAccessStatus(minimumRequiredStatus);
		if (onboardingStepRank(currentStatus) < onboardingStepRank(requiredStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank customer is not at the expected onboarding step.");
		}
	}

	private void advanceCustomerStepIfNeeded(BankCustomer customer, String minimumNextStatus) {
		String currentStatus = normalizeAccessStatus(customer.getAccessStatus());
		String nextStatus = normalizeAccessStatus(minimumNextStatus);
		if (onboardingStepRank(currentStatus) < onboardingStepRank(nextStatus)) {
			customer.setAccessStatus(nextStatus);
			bankCustomerRepository.save(customer);
		}
	}

	private int onboardingStepRank(String accessStatus) {
		return switch (accessStatus) {
			case STATUS_PENDING_STEP_2 -> 2;
			case STATUS_PENDING_STEP_3 -> 3;
			case STATUS_PENDING_STEP_4 -> 4;
			case STATUS_PENDING_STEP_5 -> 5;
			case STATUS_PENDING_STEP_6 -> 6;
			case STATUS_PENDING_STEP_7 -> 7;
			case STATUS_COMPLETED -> 8;
			default -> 0;
		};
	}

	private BankCustomer resolveOwnedBankCustomerForFinalReview(Long bankCustomerId) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		String accessStatus = normalizeAccessStatus(customer.getAccessStatus());
		if (!STATUS_PENDING_STEP_7.equals(accessStatus) && !"PENDING_STEP_8".equals(accessStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank customer is not at the expected onboarding step.");
		}
		return customer;
	}

	private String normalizeAccessStatus(String accessStatus) {
		return accessStatus == null ? "" : accessStatus.trim().toUpperCase(Locale.ROOT);
	}

	private BankCustomer resolveOwnedBankCustomer(Long bankCustomerId) {
		return resolveOwnedBankCustomer(bankCustomerId, null);
	}

	private BankCustomer resolveOwnedBankCustomer(Long bankCustomerId, String expectedAccessStatus) {
		resolveLoggedInBankOfficer();
		BankCustomer customer = bankCustomerRepository.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));

		String accessStatus = normalizeAccessStatus(customer.getAccessStatus());
		if ("DRAFT".equals(accessStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank customer step-1 must be completed before adding financial data.");
		}
		if (expectedAccessStatus != null && !expectedAccessStatus.equals(accessStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank customer is not at the expected onboarding step.");
		}

		return customer;
	}

	private BankOfficer resolveLoggedInBankOfficer() {
		return bankOfficerContextService.resolveLoggedInBankOfficer();
	}
}
