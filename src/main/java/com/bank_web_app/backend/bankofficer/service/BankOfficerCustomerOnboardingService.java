package com.bank_web_app.backend.bankofficer.service;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.dto.request.BankCustomerStepOneUpdateRequest;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerStepOnePrefillResponse;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.common.exception.DuplicateFieldsException;
import com.bank_web_app.backend.common.email.BankCustomerCredentialsEmailService;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.GeneratedBankCustomerCredentialsResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BankOfficerCustomerOnboardingService {

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String STATE_DRAFT = "DRAFT";
	private static final String STATE_PENDING_STEP_2 = "PENDING_STEP_2";

	private final UserService userService;
	private final BankOfficerContextService bankOfficerContextService;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final PasswordEncoder passwordEncoder;
	private final BankCustomerCredentialsEmailService credentialsEmailService;

	public BankOfficerCustomerOnboardingService(
		UserService userService,
		BankOfficerContextService bankOfficerContextService,
		AccountRepository accountRepository,
		UserRepository userRepository,
		BankCustomerRepository bankCustomerRepository,
		PasswordEncoder passwordEncoder,
		BankCustomerCredentialsEmailService credentialsEmailService
	) {
		this.userService = userService;
		this.bankOfficerContextService = bankOfficerContextService;
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.passwordEncoder = passwordEncoder;
		this.credentialsEmailService = credentialsEmailService;
	}

	public UserRegistrationStepResponse saveDraft(UserRegistrationStepOneRequest request) {
		return userService.saveBankCustomerStepOneDraft(request);
	}

	public UserRegistrationStepResponse saveAndContinue(UserRegistrationStepOneRequest request) {
		return userService.continueBankCustomerStepOne(request);
	}

	@Transactional(readOnly = true)
	public GeneratedBankCustomerCredentialsResponse generateCredentials(String firstName, String lastName) {
		return userService.generateBankCustomerCredentials(firstName, lastName);
	}

	@Transactional(readOnly = true)
	public GeneratedBankCustomerCredentialsResponse generateBankCustomerCredentials(String firstName, String lastName) {
		return generateCredentials(firstName, lastName);
	}

	@Transactional(readOnly = true)
	public BankOfficerCustomerStepOnePrefillResponse getOwnedBankCustomerStepOneByNic(String nic) {
		String normalizedNic = safeTrim(nic).replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
		if (normalizedNic.isBlank()) {
			throw new IllegalArgumentException("NIC is required.");
		}

		bankOfficerContextService.resolveLoggedInBankOfficer();
		BankCustomer customer = bankCustomerRepository
			.findByNormalizedUserNic(normalizedNic)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank customer was not found."));

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

	// Financial operations have been moved to BankOfficerFinancialService

	private UserRegistrationStepResponse updateStepOne(
		Long bankCustomerId,
		BankCustomerStepOneUpdateRequest request,
		String targetState,
		String successMessage
	) {
		validateUpdateRequest(request);

		bankOfficerContextService.resolveLoggedInBankOfficer();
		BankCustomer customer = bankCustomerRepository.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));

		User user = customer.getUser();
		String username = request.username().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String nic = request.nic().trim();

		LinkedHashMap<String, String> duplicateFieldErrors = new LinkedHashMap<>();
		if (userRepository.existsByUsernameAndUserIdNot(username, user.getUserId())) {
			duplicateFieldErrors.put("username", "Username is already in use.");
		}
		String roleName = user.getRole() == null ? "" : user.getRole().getRoleName();
		if (userRepository.existsByEmailIgnoreCaseAndRole_RoleNameAndUserIdNot(email, roleName, user.getUserId())) {
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
		boolean hasPasswordUpdate = !password.isBlank();
		if (hasPasswordUpdate) {
			user.setPasswordHash(passwordEncoder.encode(password));
		}
		userRepository.save(user);

		// Keep recorded ownership unchanged. BANK_OFFICER access is role-based,
		// so updating a customer must not implicitly reassign their owner/branch.
		customer.setAccount(account);
		customer.setAccessStatus(targetState);
		bankCustomerRepository.save(customer);

		if (hasPasswordUpdate) {
			credentialsEmailService.sendCredentialsEmail(user.getEmail(), user.getFirstName(), user.getUsername(), password);
		}

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
