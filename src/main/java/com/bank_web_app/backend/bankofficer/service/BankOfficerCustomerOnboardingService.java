package com.bank_web_app.backend.bankofficer.service;

import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCardStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCribRequestStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCribRetrievalStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerIncomeStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLiabilityStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLoanStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerCribStepResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialStepResponse;
import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankcustomer.service.BankCustomerFinancialRecordService;
import com.bank_web_app.backend.bankofficer.dto.request.BankCustomerStepOneUpdateRequest;
import com.bank_web_app.backend.bankofficer.dto.response.AccountVerificationResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerIdentityResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerStepOnePrefillResponse;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.common.exception.DuplicateFieldsException;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import com.bank_web_app.backend.user.service.UserService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BankOfficerCustomerOnboardingService {

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String STATE_DRAFT = "DRAFT";
	private static final String STATE_PENDING_STEP_2 = "PENDING_STEP_2";

	private final UserService userService;
	private final AccountRepository accountRepository;
	private final BankCustomerFinancialRecordService financialRecordService;
	private final UserRepository userRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final PasswordEncoder passwordEncoder;

	public BankOfficerCustomerOnboardingService(
		UserService userService,
		AccountRepository accountRepository,
		BankCustomerFinancialRecordService financialRecordService,
		UserRepository userRepository,
		BankOfficerRepository bankOfficerRepository,
		BankCustomerRepository bankCustomerRepository,
		PasswordEncoder passwordEncoder
	) {
		this.userService = userService;
		this.accountRepository = accountRepository;
		this.financialRecordService = financialRecordService;
		this.userRepository = userRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public UserRegistrationStepResponse saveDraft(UserRegistrationStepOneRequest request) {
		return userService.saveBankCustomerStepOneDraft(request);
	}

	public UserRegistrationStepResponse saveAndContinue(UserRegistrationStepOneRequest request) {
		return userService.continueBankCustomerStepOne(request);
	}

	@Transactional(readOnly = true)
	public BankOfficerCustomerStepOnePrefillResponse getOwnedBankCustomerStepOneByNic(String nic) {
		String normalizedNic = safeTrim(nic);
		if (normalizedNic.isBlank()) {
			throw new IllegalArgumentException("NIC is required.");
		}

		BankOfficer officer = resolveLoggedInBankOfficer();
		BankCustomer customer = bankCustomerRepository
			.findByUser_NicAndOfficer_OfficerId(normalizedNic, officer.getOfficerId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NIC not found for this bank officer."));

		User user = customer.getUser();
		Account account = customer.getAccount();

		return new BankOfficerCustomerStepOnePrefillResponse(
			customer.getBankCustomerId(),
			user.getUserId(),
			customer.getCustomerCode(),
			customer.getAccessStatus(),
			safeTrim(user.getFirstName()),
			safeTrim(user.getLastName()),
			safeTrim(user.getNic()),
			user.getDob() == null ? null : user.getDob().toString(),
			safeTrim(user.getEmail()),
			safeTrim(user.getPhone()),
			safeTrim(user.getProvince()),
			safeTrim(user.getAddress()),
			safeTrim(user.getUsername()),
			account == null ? null : safeTrim(account.getAccountNumber()),
			account == null ? null : safeTrim(account.getStatus()),
			account == null ? null : safeTrim(account.getAccountType())
		);
	}

	@Transactional
	public UserRegistrationStepResponse updateStepOneDraft(Long bankCustomerId, BankCustomerStepOneUpdateRequest request) {
		return updateStepOne(bankCustomerId, request, STATE_DRAFT, "Bank customer draft updated successfully.");
	}

	@Transactional
	public UserRegistrationStepResponse updateStepOneAndContinue(Long bankCustomerId, BankCustomerStepOneUpdateRequest request) {
		return updateStepOne(
			bankCustomerId,
			request,
			STATE_PENDING_STEP_2,
			"Bank customer step one updated. Continue to step two."
		);
	}

	public List<BankCustomerSummaryResponse> getAll() {
		return userService.getBankCustomersForOfficer();
	}

	public BankOfficerCustomerIdentityResponse getOwnedBankCustomerIdentityByUserId(Long userId) {
		return financialRecordService.getOwnedBankCustomerIdentityByUserId(userId);
	}

	public BankCustomerFinancialStepResponse saveIncomeStepDraft(Long bankCustomerId, BankCustomerIncomeStepRequest request) {
		return financialRecordService.saveIncomeStepDraft(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveIncomeStepAndContinue(Long bankCustomerId, BankCustomerIncomeStepRequest request) {
		return financialRecordService.saveIncomeStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveLoanStepDraft(Long bankCustomerId, BankCustomerLoanStepRequest request) {
		return financialRecordService.saveLoanStepDraft(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveLoanStepAndContinue(Long bankCustomerId, BankCustomerLoanStepRequest request) {
		return financialRecordService.saveLoanStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveCardStepDraft(Long bankCustomerId, BankCustomerCardStepRequest request) {
		return financialRecordService.saveCardStepDraft(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveCardStepAndContinue(Long bankCustomerId, BankCustomerCardStepRequest request) {
		return financialRecordService.saveCardStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveLiabilityStepDraft(Long bankCustomerId, BankCustomerLiabilityStepRequest request) {
		return financialRecordService.saveLiabilityStepDraft(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveLiabilityStepAndContinue(Long bankCustomerId, BankCustomerLiabilityStepRequest request) {
		return financialRecordService.saveLiabilityStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerCribStepResponse saveCribLinkingStepAndContinue(Long bankCustomerId, BankCustomerCribRequestStepRequest request) {
		return financialRecordService.saveCribLinkingStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerCribStepResponse saveCribRequestStepAndContinue(Long bankCustomerId, BankCustomerCribRequestStepRequest request) {
		return financialRecordService.saveCribRequestStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerCribStepResponse saveCribRetrievalStepAndContinue(Long bankCustomerId, BankCustomerCribRetrievalStepRequest request) {
		return financialRecordService.saveCribRetrievalStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerCribStepResponse completeCribReviewAndOnboarding(Long bankCustomerId) {
		return financialRecordService.completeCribReviewAndOnboarding(bankCustomerId);
	}

	public BankCustomerFinancialRecordResponse getCurrentFinancialRecord(Long bankCustomerId) {
		return financialRecordService.getCurrentFinancialRecord(bankCustomerId);
	}

	public List<BankCustomerFinancialRecordSummaryResponse> getFinancialRecordHistory(Long bankCustomerId) {
		return financialRecordService.getFinancialRecordHistory(bankCustomerId);
	}

	public BankCustomerFinancialRecordResponse getFinancialRecordById(Long bankCustomerId, Long bankRecordId) {
		return financialRecordService.getFinancialRecordById(bankCustomerId, bankRecordId);
	}

	public AccountVerificationResponse verifyAccount(String accountNumber) {
		if (accountNumber == null || accountNumber.trim().isEmpty()) {
			return new AccountVerificationResponse(false, null, "NOT_FOUND", null, "Account number is required.");
		}

		String normalized = accountNumber.trim();
		Account account = accountRepository.findByAccountNumber(normalized).orElse(null);
		if (account == null) {
			return new AccountVerificationResponse(false, null, "NOT_FOUND", null, "Account not found.");
		}

		return new AccountVerificationResponse(
			true,
			account.getAccountId(),
			account.getStatus(),
			account.getAccountType(),
			"Account found."
		);
	}

	private UserRegistrationStepResponse updateStepOne(
		Long bankCustomerId,
		BankCustomerStepOneUpdateRequest request,
		String targetState,
		String successMessage
	) {
		validateUpdateRequest(request);

		BankOfficer loggedOfficer = resolveLoggedInBankOfficer();
		BankCustomer customer = bankCustomerRepository.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		if (!customer.getOfficer().getOfficerId().equals(loggedOfficer.getOfficerId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This bank customer is not assigned to the logged-in officer.");
		}

		User user = customer.getUser();
		String username = request.username().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String nic = request.nic().trim();

		LinkedHashMap<String, String> duplicateFieldErrors = new LinkedHashMap<>();
		if (userRepository.existsByUsernameAndUserIdNot(username, user.getUserId())) {
			duplicateFieldErrors.put("username", "Username is already in use.");
		}
		if (userRepository.existsByEmailAndUserIdNot(email, user.getUserId())) {
			duplicateFieldErrors.put("email", "Email is already in use.");
		}
		if (userRepository.existsByNicAndUserIdNot(nic, user.getUserId())) {
			duplicateFieldErrors.put("nic", "NIC is already in use.");
		}
		if (!duplicateFieldErrors.isEmpty()) {
			throw new DuplicateFieldsException(duplicateFieldErrors);
		}

		String accountNumber = resolveAccountNumber(request);
		Account account = accountRepository.findByAccountNumber(accountNumber)
			.orElseThrow(() -> new IllegalArgumentException("Account not found."));
		if (bankCustomerRepository.existsByAccount_AccountIdAndBankCustomerIdNot(account.getAccountId(), customer.getBankCustomerId())) {
			throw new IllegalArgumentException("Bank account is already linked to another customer.");
		}

		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setNic(nic);
		user.setDob(parseDob(request.dob()));
		user.setEmail(email);
		user.setPhone(request.mobile().trim());
		user.setProvince(request.province().trim());
		user.setAddress(request.address().trim());
		user.setUsername(username);

		String password = safeTrim(request.password());
		if (!password.isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(password));
		}
		userRepository.save(user);

		customer.setOfficer(loggedOfficer);
		customer.setBranch(loggedOfficer.getBranch());
		customer.setAccount(account);
		customer.setAccessStatus(targetState);
		bankCustomerRepository.save(customer);

		return new UserRegistrationStepResponse(
			user.getUserId(),
			ROLE_BANK_CUSTOMER,
			targetState,
			successMessage
		);
	}

	private void validateUpdateRequest(BankCustomerStepOneUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required.");
		}

		requireText(request.firstName(), "First name is required.");
		requireText(request.lastName(), "Last name is required.");
		requireText(request.nic(), "NIC is required.");
		requireText(request.dob(), "Date of birth is required.");
		requireText(request.email(), "Email is required.");
		requireText(request.mobile(), "Mobile is required.");
		requireText(request.province(), "Province is required.");
		requireText(request.address(), "Address is required.");
		requireText(request.username(), "Username is required.");
		parseDob(request.dob());

		String password = safeTrim(request.password());
		String confirmPassword = safeTrim(request.confirmPassword());
		if (password.isBlank() != confirmPassword.isBlank()) {
			throw new IllegalArgumentException("Password and confirm password must both be provided when changing password.");
		}
		if (!password.isBlank() && !password.equals(confirmPassword)) {
			throw new IllegalArgumentException("Password and confirm password must match.");
		}
	}

	private String resolveAccountNumber(BankCustomerStepOneUpdateRequest request) {
		String fromRequest = safeTrim(request.accountNumber());
		if (!fromRequest.isBlank()) {
			return fromRequest;
		}
		if (request.bankAccount() != null && request.bankAccount() > 0) {
			return String.valueOf(request.bankAccount());
		}
		throw new IllegalArgumentException("Account number is required for bank customer registration.");
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

	private LocalDate parseDob(String dob) {
		try {
			return LocalDate.parse(dob.trim());
		} catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("DOB must be in yyyy-MM-dd format.");
		}
	}

	private String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private void requireText(String value, String message) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}
}
