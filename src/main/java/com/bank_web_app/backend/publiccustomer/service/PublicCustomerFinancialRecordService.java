package com.bank_web_app.backend.publiccustomer.service;

import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerCardStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerIncomeStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLiabilityStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLoanStepRequest;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
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

	// Persistence dependencies for financial-record root and step-specific child rows.
	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final PublicCustomerFinancialRecordRepository financialRecordRepository;
	private final PublicCustomerIncomeRepository incomeRepository;
	private final PublicCustomerLoanRepository loanRepository;
	private final PublicCustomerCardRepository cardRepository;
	private final BankCustomerCardRepository bankCustomerCardRepository;
	private final PublicCustomerLiabilityRepository liabilityRepository;
	private final PublicCustomerMissedPaymentRepository missedPaymentRepository;
	private final PublicCustomerFinancialRecordMapper financialRecordMapper;
	private final UserRepository userRepository;

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
		UserRepository userRepository
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
		this.userRepository = userRepository;
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

		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "INCOME", "Income step saved successfully.");
	}

	// Saves step-2 loan data by replacing existing rows for current record.
	@Transactional
	public PublicCustomerFinancialStepResponse saveLoanStep(Long publicCustomerId, PublicCustomerLoanStepRequest request) {
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

		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "LOANS", "Loan step saved successfully.");
	}

	// Saves step-3 card data by replacing existing rows for current record.
	@Transactional
	public PublicCustomerFinancialStepResponse saveCardStep(Long publicCustomerId, PublicCustomerCardStepRequest request) {
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

		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "CARDS", "Card step saved successfully.");
	}

	// Saves step-4 liabilities and missed-payment aggregate for current record.
	@Transactional
	public PublicCustomerFinancialStepResponse saveLiabilityStep(Long publicCustomerId, PublicCustomerLiabilityStepRequest request) {
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

		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(
			recordId,
			publicCustomerId,
			"LIABILITIES",
			"Liability and missed-payment step saved successfully."
		);
	}

	// Returns the current financial snapshot for a given public customer id.
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
		PublicCustomerProfile profile = publicCustomerProfileRepository.findById(publicCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Public customer not found."));

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

	// Resolves authenticated principal and ensures a linked public-customer profile exists.
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
