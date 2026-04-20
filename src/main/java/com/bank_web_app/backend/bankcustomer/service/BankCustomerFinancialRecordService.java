package com.bank_web_app.backend.bankcustomer.service;

import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCardStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerIncomeStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLiabilityStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLoanStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialStepResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerIdentityResponse;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCard;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLiability;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLoan;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerMissedPayment;
import com.bank_web_app.backend.bankcustomer.mapper.BankCustomerFinancialRecordMapper;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerFinancialRecordRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerIncomeRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLiabilityRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLoanRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerMissedPaymentRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
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

	private final BankCustomerRepository bankCustomerRepository;
	private final BankCustomerFinancialRecordRepository financialRecordRepository;
	private final BankCustomerIncomeRepository incomeRepository;
	private final BankCustomerLoanRepository loanRepository;
	private final BankCustomerCardRepository cardRepository;
	private final BankCustomerLiabilityRepository liabilityRepository;
	private final BankCustomerMissedPaymentRepository missedPaymentRepository;
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
		BankCustomer customer = bankCustomerRepository.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		customer.setAccessStatus("PENDING_STEP_3");
		bankCustomerRepository.save(customer);
		return response;
	}

	private BankCustomerFinancialStepResponse doSaveIncomeStep(Long bankCustomerId, BankCustomerIncomeStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = getOrCreateLatestRecord(customer);
		Long bankRecordId = currentRecord.getBankRecordId();

		for (BankCustomerIncomeStepRequest.IncomeItem incomeItem : request.incomes()) {
			BankCustomerIncome income = new BankCustomerIncome();
			income.setFinancialRecord(currentRecord);
			income.setIncomeCategory(normalizeIncomeCategory(incomeItem.incomeCategory()));
			income.setAmount(incomeItem.amount());
			income.setSalaryType(incomeItem.salaryType());
			income.setEmploymentType(incomeItem.employmentType());
			income.setContractDurationMonths(incomeItem.contractDurationMonths());
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
		BankCustomer customer = bankCustomerRepository.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		customer.setAccessStatus("PENDING_STEP_4");
		bankCustomerRepository.save(customer);
		return response;
	}

	private BankCustomerFinancialStepResponse doSaveLoanStep(Long bankCustomerId, BankCustomerLoanStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = getOrCreateLatestRecord(customer);
		Long bankRecordId = currentRecord.getBankRecordId();

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
		BankCustomer customer = bankCustomerRepository.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		customer.setAccessStatus("PENDING_STEP_5");
		bankCustomerRepository.save(customer);
		return response;
	}

	private BankCustomerFinancialStepResponse doSaveCardStep(Long bankCustomerId, BankCustomerCardStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = getOrCreateLatestRecord(customer);
		Long bankRecordId = currentRecord.getBankRecordId();

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
		BankCustomer customer = bankCustomerRepository.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		customer.setAccessStatus("COMPLETED");
		bankCustomerRepository.save(customer);
		return response;
	}

	private BankCustomerFinancialStepResponse doSaveLiabilityStep(Long bankCustomerId, BankCustomerLiabilityStepRequest request) {
		BankCustomer customer = resolveOwnedBankCustomer(bankCustomerId);
		BankCustomerFinancialRecord currentRecord = getOrCreateLatestRecord(customer);
		Long bankRecordId = currentRecord.getBankRecordId();

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
