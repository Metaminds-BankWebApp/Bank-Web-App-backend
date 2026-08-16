package com.bank_web_app.backend.publiccustomer.service;

import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerCardStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerIncomeStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLiabilityStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLoanStepRequest;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
import com.bank_web_app.backend.creditlens.repository.SelfCreditEvaluationRepository;
import com.bank_web_app.backend.notification.service.NotificationService;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerApplicationProgressResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerCardProviderOptionResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerMeResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialStepResponse;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerCard;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLiability;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLoan;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerMissedPayment;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.mapper.PublicCustomerFinancialRecordMapper;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerCardRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerFinancialRecordRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerIncomeRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLiabilityRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLoanRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerMissedPaymentRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicCustomerFinancialRecordService {

	private static final String PENDING = "PENDING";
	private static final String COMPLETED = "COMPLETED";
	private static final String SKIPPED = "SKIPPED";
	private static final int TOTAL_APPLICATION_STEPS = 5;

	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final PublicCustomerFinancialRecordRepository financialRecordRepository;
	private final PublicCustomerIncomeRepository incomeRepository;
	private final PublicCustomerLoanRepository loanRepository;
	private final PublicCustomerCardRepository cardRepository;
	private final BankCustomerCardRepository bankCustomerCardRepository;
	private final PublicCustomerLiabilityRepository liabilityRepository;
	private final PublicCustomerMissedPaymentRepository missedPaymentRepository;
	private final PublicCustomerFinancialRecordMapper financialRecordMapper;
	private final SelfCreditEvaluationRepository selfCreditEvaluationRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;

	// Injects repositories and mapper required for public-customer financial workflows.
	public PublicCustomerFinancialRecordService(
		PublicCustomerProfileRepository publicCustomerProfileRepository,
		PublicCustomerFinancialRecordRepository financialRecordRepository,
		PublicCustomerIncomeRepository incomeRepository,
		PublicCustomerLoanRepository loanRepository,
		PublicCustomerCardRepository cardRepository,
		BankCustomerCardRepository bankCustomerCardRepository,
		PublicCustomerLiabilityRepository liabilityRepository,
		PublicCustomerMissedPaymentRepository missedPaymentRepository,
		PublicCustomerFinancialRecordMapper financialRecordMapper,
		SelfCreditEvaluationRepository selfCreditEvaluationRepository,
		UserRepository userRepository,
		NotificationService notificationService
	) {
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
		this.financialRecordRepository = financialRecordRepository;
		this.incomeRepository = incomeRepository;
		this.loanRepository = loanRepository;
		this.cardRepository = cardRepository;
		this.bankCustomerCardRepository = bankCustomerCardRepository;
		this.liabilityRepository = liabilityRepository;
		this.missedPaymentRepository = missedPaymentRepository;
		this.financialRecordMapper = financialRecordMapper;
		this.selfCreditEvaluationRepository = selfCreditEvaluationRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
	}

	// Default card-provider list used as baseline dropdown options.
	private static final List<String> DEFAULT_CARD_PROVIDER_BANK_NAMES = List.of(
		"Bank of Ceylon",
		"People's Bank",
		"Commercial Bank",
		"Sampath Bank",
		"Hatton National Bank",
		"Nations Trust Bank",
		"DFCC Bank",
		"NDB Bank",
		"Seylan Bank",
		"Union Bank",
		"Pan Asia Bank",
		"Cargills Bank",
		"Standard Chartered Bank",
		"HSBC"
	);

	// Resolves identity details for the logged-in public customer.
	@Transactional(readOnly = true)
	public PublicCustomerMeResponse getLoggedInPublicCustomerProfile() {
		PublicCustomerProfile profile = resolveLoggedInPublicCustomerProfile();
		return new PublicCustomerMeResponse(
			profile.getPublicCustomerId(),
			profile.getUser().getUserId(),
			profile.getCustomerCode()
		);
	}

	// Builds card-provider dropdown options from defaults plus stored providers.
	@Transactional(readOnly = true)
	public List<PublicCustomerCardProviderOptionResponse> getCardProviderOptions() {
		// Ensure only the logged-in PUBLIC_CUSTOMER can request step options for their flow.
		resolveLoggedInPublicCustomerProfile();

		Set<String> providerNames = new LinkedHashSet<>();
		addNormalizedProviderNames(providerNames, DEFAULT_CARD_PROVIDER_BANK_NAMES);
		addNormalizedProviderNames(providerNames, cardRepository.findDistinctProviders());
		addNormalizedProviderNames(providerNames, bankCustomerCardRepository.findDistinctProviders());

		return providerNames.stream().map(PublicCustomerCardProviderOptionResponse::new).toList();
	}

	// Saves step-1 income data by replacing existing rows for current record.
	@Transactional
	public PublicCustomerFinancialStepResponse saveIncomeStep(Long publicCustomerId, PublicCustomerIncomeStepRequest request) {
		if (request.incomes().isEmpty()) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Add at least one income source, or use Skip if you do not want to provide this section."
			);
		}

		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();

		incomeRepository.deleteByFinancialRecord_RecordId(recordId);

		for (PublicCustomerIncomeStepRequest.IncomeItem incomeItem : request.incomes()) {
			PublicCustomerIncome income = new PublicCustomerIncome();
			income.setFinancialRecord(currentRecord);
			income.setIncomeCategory(normalizeIncomeCategory(incomeItem.incomeCategory()));
			income.setAmount(incomeItem.amount());
			income.setSalaryType(incomeItem.salaryType());
			income.setEmploymentType(incomeItem.employmentType());
			income.setDurationMonths(incomeItem.durationMonths());
			income.setIncomeStability(incomeItem.incomeStability());
			incomeRepository.save(income);
		}

		currentRecord.setIncomeStepStatus(COMPLETED);
		resetReviewStep(currentRecord);
		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "INCOME", "Income step saved successfully.");
	}

	// Saves step-2 loan data by replacing existing rows for current record.
	@Transactional
	public PublicCustomerFinancialStepResponse saveLoanStep(Long publicCustomerId, PublicCustomerLoanStepRequest request) {
		if (request.loans().isEmpty()) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Add at least one loan, or use Skip if you have no loan details to provide."
			);
		}

		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();

		loanRepository.deleteByFinancialRecord_RecordId(recordId);

		for (PublicCustomerLoanStepRequest.LoanItem loanItem : request.loans()) {
			PublicCustomerLoan loan = new PublicCustomerLoan();
			loan.setFinancialRecord(currentRecord);
			loan.setLoanType(loanItem.loanType());
			loan.setMonthlyEmi(loanItem.monthlyEmi());
			loan.setRemainingBalance(loanItem.remainingBalance());
			loanRepository.save(loan);
		}

		currentRecord.setLoanStepStatus(COMPLETED);
		resetReviewStep(currentRecord);
		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "LOANS", "Loan step saved successfully.");
	}

	// Saves step-3 card data by replacing existing rows for current record.
	@Transactional
	public PublicCustomerFinancialStepResponse saveCardStep(Long publicCustomerId, PublicCustomerCardStepRequest request) {
		if (request.cards().isEmpty()) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Add at least one credit card, or use Skip if you have no card details to provide."
			);
		}

		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();

		cardRepository.deleteByFinancialRecord_RecordId(recordId);

		for (PublicCustomerCardStepRequest.CardItem cardItem : request.cards()) {
			PublicCustomerCard card = new PublicCustomerCard();
			card.setFinancialRecord(currentRecord);
			card.setProvider(cardItem.provider());
			card.setCreditLimit(cardItem.creditLimit());
			card.setOutstandingBalance(cardItem.outstandingBalance());
			cardRepository.save(card);
		}

		currentRecord.setCardStepStatus(COMPLETED);
		resetReviewStep(currentRecord);
		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "CARDS", "Card step saved successfully.");
	}

	// Saves step-4 liabilities and missed-payment aggregate for current record.
	@Transactional
	public PublicCustomerFinancialStepResponse saveLiabilityStep(Long publicCustomerId, PublicCustomerLiabilityStepRequest request) {
		if (request.liabilities().isEmpty() && request.missedPayments() == 0) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Add a liability or missed payment, or use Skip if you have no liability details to provide."
			);
		}

		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();

		liabilityRepository.deleteByFinancialRecord_RecordId(recordId);

		for (PublicCustomerLiabilityStepRequest.LiabilityItem liabilityItem : request.liabilities()) {
			PublicCustomerLiability liability = new PublicCustomerLiability();
			liability.setFinancialRecord(currentRecord);
			liability.setDescription(liabilityItem.description());
			liability.setMonthlyAmount(liabilityItem.monthlyAmount());
			liabilityRepository.save(liability);
		}

		PublicCustomerMissedPayment missedPayment = missedPaymentRepository
			.findByFinancialRecord_RecordId(recordId)
			.orElseGet(() -> {
				PublicCustomerMissedPayment entity = new PublicCustomerMissedPayment();
				entity.setFinancialRecord(currentRecord);
				return entity;
			});
		missedPayment.setMissedPayments(request.missedPayments());
		missedPaymentRepository.save(missedPayment);

		currentRecord.setLiabilityStepStatus(COMPLETED);
		resetReviewStep(currentRecord);
		touchRecord(currentRecord);
		notificationService.resolveFinancialDetailsMissing(
			currentRecord.getPublicCustomer().getUser().getUserId()
		);
		return new PublicCustomerFinancialStepResponse(
			recordId,
			publicCustomerId,
			"LIABILITIES",
			"Liability and missed-payment step saved successfully."
		);
	}

	@Transactional
	public PublicCustomerApplicationProgressResponse getApplicationProgress(Long publicCustomerId) {
		resolveOwnedPublicCustomerProfile(publicCustomerId);

		return financialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(publicCustomerId, "CURRENT")
			.map(record -> toApplicationProgress(reconcileLegacyProgress(record)))
			.orElseGet(() -> emptyApplicationProgress(publicCustomerId));
	}

	@Transactional
	public PublicCustomerApplicationProgressResponse skipApplicationStep(Long publicCustomerId, String stepCode) {
		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();
		String normalizedStep = normalizeApplicationStep(stepCode);

		switch (normalizedStep) {
			case "INCOME" -> {
				incomeRepository.deleteByFinancialRecord_RecordId(recordId);
				currentRecord.setIncomeStepStatus(SKIPPED);
			}
			case "LOANS" -> {
				loanRepository.deleteByFinancialRecord_RecordId(recordId);
				currentRecord.setLoanStepStatus(SKIPPED);
			}
			case "CARDS" -> {
				cardRepository.deleteByFinancialRecord_RecordId(recordId);
				currentRecord.setCardStepStatus(SKIPPED);
			}
			case "LIABILITIES" -> {
				liabilityRepository.deleteByFinancialRecord_RecordId(recordId);
				missedPaymentRepository.findByFinancialRecord_RecordId(recordId).ifPresent(missedPaymentRepository::delete);
				currentRecord.setLiabilityStepStatus(SKIPPED);
			}
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported application step.");
		}

		resetReviewStep(currentRecord);
		touchRecord(currentRecord);
		return toApplicationProgress(currentRecord);
	}

	@Transactional
	public PublicCustomerApplicationProgressResponse submitApplication(Long publicCustomerId) {
		resolveOwnedPublicCustomerProfile(publicCustomerId);
		PublicCustomerFinancialRecord currentRecord = financialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(publicCustomerId, "CURRENT")
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Complete or skip the financial sections before submitting the application."
			));
		currentRecord = reconcileLegacyProgress(currentRecord);

		if (
			PENDING.equals(currentRecord.getIncomeStepStatus()) ||
			PENDING.equals(currentRecord.getLoanStepStatus()) ||
			PENDING.equals(currentRecord.getCardStepStatus()) ||
			PENDING.equals(currentRecord.getLiabilityStepStatus())
		) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Complete or skip every financial section before submitting the application."
			);
		}

		currentRecord.setReviewStepStatus(COMPLETED);
		currentRecord.setApplicationSubmittedAt(LocalDateTime.now());
		touchRecord(currentRecord);
		return toApplicationProgress(currentRecord);
	}

	@Transactional(readOnly = true)
	public PublicCustomerFinancialRecordResponse getCurrentFinancialRecord(Long publicCustomerId) {
		PublicCustomerFinancialRecord currentRecord = financialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(publicCustomerId, "CURRENT")
			.orElseThrow(() -> new IllegalArgumentException("No current financial record found for this public customer."));

		return mapRecordToResponse(currentRecord);
	}

	// Returns summary history of all financial snapshots for a public customer.
	@Transactional(readOnly = true)
	public List<PublicCustomerFinancialRecordSummaryResponse> getFinancialRecordHistory(Long publicCustomerId) {
		if (!publicCustomerProfileRepository.existsById(publicCustomerId)) {
			throw new IllegalArgumentException("Public customer not found.");
		}

		return financialRecordRepository
			.findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(publicCustomerId)
			.stream()
			.map(financialRecordMapper::toSummary)
			.collect(Collectors.toList());
	}

	// Returns one financial snapshot by record id for the public customer.
	@Transactional(readOnly = true)
	public PublicCustomerFinancialRecordResponse getFinancialRecordById(Long publicCustomerId, Long recordId) {
		PublicCustomerFinancialRecord record = financialRecordRepository
			.findByRecordIdAndPublicCustomer_PublicCustomerId(recordId, publicCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Financial record not found for this public customer."));

		return mapRecordToResponse(record);
	}

	// Loads all child step rows and maps them into full response DTO.
	private PublicCustomerFinancialRecordResponse mapRecordToResponse(PublicCustomerFinancialRecord record) {
		Long recordId = record.getRecordId();
		int missedPayments = missedPaymentRepository.findByFinancialRecord_RecordId(recordId)
			.map(PublicCustomerMissedPayment::getMissedPayments)
			.orElse(0);

		return financialRecordMapper.toResponse(
			record,
			incomeRepository.findAllByFinancialRecord_RecordId(recordId),
			loanRepository.findAllByFinancialRecord_RecordId(recordId),
			cardRepository.findAllByFinancialRecord_RecordId(recordId),
			liabilityRepository.findAllByFinancialRecord_RecordId(recordId),
			missedPayments
		);
	}

	// Normalizes accepted income-category aliases into canonical values.
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

	// Adds non-empty provider names into target set after trimming.
	private void addNormalizedProviderNames(Set<String> sink, List<String> values) {
		for (String value : values) {
			String normalized = value == null ? "" : value.trim();
			if (!normalized.isBlank()) {
				sink.add(normalized);
			}
		}
	}

	// Gets existing CURRENT record or creates one when absent.
	private PublicCustomerFinancialRecord getOrCreateCurrentRecord(Long publicCustomerId) {
		PublicCustomerProfile profile = resolveOwnedPublicCustomerProfile(publicCustomerId);

		return financialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(publicCustomerId, "CURRENT")
			.orElseGet(() -> {
				PublicCustomerFinancialRecord record = new PublicCustomerFinancialRecord();
				record.setPublicCustomer(profile);
				record.setRecordStatus("CURRENT");
				return financialRecordRepository.save(record);
			});
	}

	// Updates parent record timestamp after step save.
	private void touchRecord(PublicCustomerFinancialRecord record) {
		record.setUpdatedAt(LocalDateTime.now());
		financialRecordRepository.save(record);
	}

	private PublicCustomerFinancialRecord reconcileLegacyProgress(PublicCustomerFinancialRecord record) {
		Long recordId = record.getRecordId();
		boolean changed = false;

		if (PENDING.equals(record.getIncomeStepStatus()) && !incomeRepository.findAllByFinancialRecord_RecordId(recordId).isEmpty()) {
			record.setIncomeStepStatus(COMPLETED);
			changed = true;
		}
		if (PENDING.equals(record.getLoanStepStatus()) && !loanRepository.findAllByFinancialRecord_RecordId(recordId).isEmpty()) {
			record.setLoanStepStatus(COMPLETED);
			changed = true;
		}
		if (PENDING.equals(record.getCardStepStatus()) && !cardRepository.findAllByFinancialRecord_RecordId(recordId).isEmpty()) {
			record.setCardStepStatus(COMPLETED);
			changed = true;
		}
		if (
			PENDING.equals(record.getLiabilityStepStatus()) &&
			(
				!liabilityRepository.findAllByFinancialRecord_RecordId(recordId).isEmpty() ||
				missedPaymentRepository.findByFinancialRecord_RecordId(recordId).isPresent()
			)
		) {
			record.setLiabilityStepStatus(COMPLETED);
			changed = true;
		}
		if (PENDING.equals(record.getReviewStepStatus())) {
			var latestEvaluation = selfCreditEvaluationRepository.findTopByPublicRecord_RecordIdOrderByCreatedAtDesc(recordId);
			if (
				latestEvaluation.isPresent() &&
				(
					record.getUpdatedAt() == null ||
					!latestEvaluation.get().getCreatedAt().isBefore(record.getUpdatedAt())
				)
			) {
				record.setReviewStepStatus(COMPLETED);
				record.setApplicationSubmittedAt(latestEvaluation.get().getCreatedAt());
				changed = true;
			}
		}

		return changed ? financialRecordRepository.save(record) : record;
	}

	private void resetReviewStep(PublicCustomerFinancialRecord record) {
		record.setReviewStepStatus(PENDING);
		record.setApplicationSubmittedAt(null);
	}

	private String normalizeApplicationStep(String stepCode) {
		String normalized = stepCode == null ? "" : stepCode.trim().toUpperCase(Locale.ROOT);
		if (!Set.of("INCOME", "LOANS", "CARDS", "LIABILITIES").contains(normalized)) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Application step must be INCOME, LOANS, CARDS, or LIABILITIES."
			);
		}
		return normalized;
	}

	private PublicCustomerApplicationProgressResponse emptyApplicationProgress(Long publicCustomerId) {
		List<PublicCustomerApplicationProgressResponse.ApplicationStep> steps = List.of(
			applicationStep("INCOME", "Income Details", PENDING),
			applicationStep("LOANS", "Loan Details", PENDING),
			applicationStep("CARDS", "Credit Card Details", PENDING),
			applicationStep("LIABILITIES", "Liability Details", PENDING),
			applicationStep("REVIEW", "Review Details", PENDING)
		);
		return new PublicCustomerApplicationProgressResponse(
			publicCustomerId,
			null,
			0,
			0,
			TOTAL_APPLICATION_STEPS,
			"NOT_STARTED",
			null,
			steps
		);
	}

	private PublicCustomerApplicationProgressResponse toApplicationProgress(PublicCustomerFinancialRecord record) {
		List<PublicCustomerApplicationProgressResponse.ApplicationStep> steps = List.of(
			applicationStep("INCOME", "Income Details", record.getIncomeStepStatus()),
			applicationStep("LOANS", "Loan Details", record.getLoanStepStatus()),
			applicationStep("CARDS", "Credit Card Details", record.getCardStepStatus()),
			applicationStep("LIABILITIES", "Liability Details", record.getLiabilityStepStatus()),
			applicationStep("REVIEW", "Review Details", record.getReviewStepStatus())
		);
		int completedSteps = (int) steps.stream()
			.filter(PublicCustomerApplicationProgressResponse.ApplicationStep::completed)
			.count();
		int completionPercentage = completedSteps * 100 / TOTAL_APPLICATION_STEPS;
		String overallStatus = completedSteps == TOTAL_APPLICATION_STEPS ? "COMPLETED" : "IN_PROGRESS";

		return new PublicCustomerApplicationProgressResponse(
			record.getPublicCustomer().getPublicCustomerId(),
			record.getRecordId(),
			completionPercentage,
			completedSteps,
			TOTAL_APPLICATION_STEPS,
			overallStatus,
			record.getApplicationSubmittedAt(),
			steps
		);
	}

	private PublicCustomerApplicationProgressResponse.ApplicationStep applicationStep(
		String code,
		String label,
		String status
	) {
		String normalizedStatus = status == null || status.isBlank() ? PENDING : status;
		return new PublicCustomerApplicationProgressResponse.ApplicationStep(
			code,
			label,
			normalizedStatus,
			COMPLETED.equals(normalizedStatus)
		);
	}

	private PublicCustomerProfile resolveOwnedPublicCustomerProfile(Long publicCustomerId) {
		PublicCustomerProfile loggedInProfile = resolveLoggedInPublicCustomerProfile();
		if (!loggedInProfile.getPublicCustomerId().equals(publicCustomerId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own application data.");
		}
		return loggedInProfile;
	}

	private PublicCustomerProfile resolveLoggedInPublicCustomerProfile() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Public customer authentication is required.");
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		User user = userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal))
			.or(() -> userRepository.findByUsername(normalizedPrincipal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));

		return publicCustomerProfileRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a public customer."));
	}
}
