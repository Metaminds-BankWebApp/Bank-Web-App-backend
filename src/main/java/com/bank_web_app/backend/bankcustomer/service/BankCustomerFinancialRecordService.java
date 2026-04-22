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
import com.bank_web_app.backend.crib.dto.response.CribDatasetSnapshotResponse;
import com.bank_web_app.backend.crib.service.CribDatasetService;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
	private final UserRepository userRepository;
	private final BankOfficerRepository bankOfficerRepository;

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
		UserRepository userRepository,
		BankOfficerRepository bankOfficerRepository
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
		this.userRepository = userRepository;
		this.bankOfficerRepository = bankOfficerRepository;
	}

	@Transactional(readOnly = true)
	public BankOfficerCustomerIdentityResponse getOwnedBankCustomerIdentityByUserId(Long userId) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer customer = bankCustomerRepository
			.findByUser_UserIdAndOfficer_OfficerId(userId, officer.getOfficerId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank customer not found for this officer."));

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
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId, STATUS_PENDING_STEP_3);
		customer.setAccessStatus(STATUS_PENDING_STEP_4);
		bankCustomerRepository.save(customer);
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
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId, STATUS_PENDING_STEP_4);
		customer.setAccessStatus(STATUS_PENDING_STEP_5);
		bankCustomerRepository.save(customer);
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
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId, STATUS_PENDING_STEP_5);
		customer.setAccessStatus(STATUS_PENDING_STEP_6);
		bankCustomerRepository.save(customer);
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
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId, STATUS_PENDING_STEP_6);
		customer.setAccessStatus(STATUS_PENDING_STEP_7);
		bankCustomerRepository.save(customer);
		return response;
	}

	@Transactional
	public BankCustomerCribStepResponse saveCribLinkingStepAndContinue(Long bankCustomerId, BankCustomerCribRequestStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId, STATUS_PENDING_STEP_2);
		String customerNic = customer.getUser() == null ? null : customer.getUser().getNic();
		String requestedNic = request.nic() == null ? "" : request.nic().trim();
		if (!requestedNic.isBlank() && customerNic != null && !customerNic.trim().isBlank() && !requestedNic.equalsIgnoreCase(customerNic.trim())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided NIC does not match the selected bank customer.");
		}
		String nic = requestedNic.isBlank() ? customerNic : requestedNic;
		if (nic == null || nic.trim().isBlank()) {
			throw new IllegalArgumentException("NIC/ID is required to link CRIB data.");
		}
		CribDatasetSnapshotResponse cribSnapshot = cribDatasetService.lookupSnapshotByNic(nic);

		BankCustomerCribRequest cribRequest = new BankCustomerCribRequest();
		cribRequest.setBankCustomer(customer);
		cribRequest.setRequestedByOfficer(customer.getOfficer());
		cribRequest.setRequestType(normalizeRequestType(request.requestType()));
		cribRequest.setRequestStatus("COMPLETED");
		cribRequest.setReportStatus("READY");
		LocalDateTime now = LocalDateTime.now();
		cribRequest.setRequestedAt(now);
		cribRequest.setResponseReceivedAt(now);

		BankCustomerCribRequest saved = cribRequestRepository.save(cribRequest);

		customer.setAccessStatus(STATUS_PENDING_STEP_3);
		bankCustomerRepository.save(customer);

		return new BankCustomerCribStepResponse(
			saved.getCribRequestId(),
			bankCustomerId,
			"CRIB_LINKING",
			saved.getRequestStatus(),
			saved.getReportStatus(),
			"CRIB linking step saved successfully.",
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

		customer.setAccessStatus("PENDING_STEP_8");
		bankCustomerRepository.save(customer);

		return new BankCustomerCribStepResponse(
			saved.getCribRequestId(),
			bankCustomerId,
			"CRIB_RETRIEVAL",
			saved.getRequestStatus(),
			saved.getReportStatus(),
			"CRIB retrieval step saved successfully.",
			null
		);
	}

	@Transactional
	public BankCustomerCribStepResponse completeCribReviewAndOnboarding(Long bankCustomerId) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId, STATUS_PENDING_STEP_7);
		customer.setAccessStatus(STATUS_COMPLETED);
		bankCustomerRepository.save(customer);
		BankCustomerCribRequest latest = cribRequestRepository
			.findTopByBankCustomer_BankCustomerIdOrderByRequestedAtDesc(bankCustomerId)
			.orElse(null);

		return new BankCustomerCribStepResponse(
			latest != null ? latest.getCribRequestId() : null,
			bankCustomerId,
			"CRIB_REVIEW",
			latest != null ? latest.getRequestStatus() : null,
			latest != null ? latest.getReportStatus() : null,
			"Bank customer onboarding completed successfully.",
			null
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
		return financialRecordRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.orElseGet(() -> {
				BankCustomerFinancialRecord record = new BankCustomerFinancialRecord();
				record.setBankCustomer(customer);
				record.setVerifiedByOfficer(customer.getOfficer());
				record.setDataSource("MANUAL");
				return financialRecordRepository.save(record);
			});
	}

	private void touchRecord(BankCustomerFinancialRecord record) {
		record.setUpdatedAt(LocalDateTime.now());
		financialRecordRepository.save(record);
	}

	private BankCustomer resolveOwnedBankCustomer(Long bankCustomerId) {
		return resolveOwnedBankCustomer(bankCustomerId, null);
	}

	private BankCustomer resolveOwnedBankCustomer(Long bankCustomerId, String expectedAccessStatus) {
		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer customer = bankCustomerRepository.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));

		if (!customer.getOfficer().getOfficerId().equals(officer.getOfficerId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This bank customer is not assigned to the logged-in officer.");
		}

		String accessStatus = customer.getAccessStatus() == null ? "" : customer.getAccessStatus().trim().toUpperCase(Locale.ROOT);
		if ("DRAFT".equals(accessStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank customer step-1 must be completed before adding financial data.");
		}
		if (expectedAccessStatus != null && !expectedAccessStatus.equals(accessStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank customer is not at the expected onboarding step.");
		}

		return customer;
	}

	private BankOfficer resolveLoggedInBankOfficer() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bank officer authentication is required.");
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		User officerUser = userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal))
			.or(() -> userRepository.findByUsername(normalizedPrincipal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));

		return bankOfficerRepository.findByUser_UserId(officerUser.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a bank officer."));
	}
}
