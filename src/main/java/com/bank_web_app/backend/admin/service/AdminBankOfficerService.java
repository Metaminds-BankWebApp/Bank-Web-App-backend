package com.bank_web_app.backend.admin.service;
import com.bank_web_app.backend.admin.dto.request.AdminBankOfficerUpdateRequest;
import com.bank_web_app.backend.admin.dto.response.AdminBankOfficerSummaryResponse;
import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerFinancialRecordRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.creditlens.repository.BankCreditEvaluationRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.repository.UserRepository;
import com.bank_web_app.backend.user.service.UserService;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates Admin business logic, validation, and persistence workflows.
 */

@Service
public class AdminBankOfficerService {

	private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "LOCKED");
	private static final String GENERATED_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%&*!";
	private static final int GENERATED_PASSWORD_LENGTH = 10;
	private static final int USERNAME_MAX_LENGTH = 50;
	private static final int USERNAME_SUFFIX_LENGTH = 3;
	private static final int USERNAME_ATTEMPT_LIMIT = 300;
	private static final Pattern BANK_OFFICER_EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserService userService;
	private final BankOfficerRepository bankOfficerRepository;
	private final BranchRepository branchRepository;
	private final UserRepository userRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository;
	private final BankCreditEvaluationRepository bankCreditEvaluationRepository;
	private final AuditLogService auditLogService;

	public AdminBankOfficerService(
		UserService userService,
		BankOfficerRepository bankOfficerRepository,
		BranchRepository branchRepository,
		UserRepository userRepository,
		BankCustomerRepository bankCustomerRepository,
		BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository,
		BankCreditEvaluationRepository bankCreditEvaluationRepository,
		AuditLogService auditLogService
	) {
		this.userService = userService;
		this.bankOfficerRepository = bankOfficerRepository;
		this.branchRepository = branchRepository;
		this.userRepository = userRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankCustomerFinancialRecordRepository = bankCustomerFinancialRecordRepository;
		this.bankCreditEvaluationRepository = bankCreditEvaluationRepository;
		this.auditLogService = auditLogService;
	}

	// Creates a draft officer account preview before final submission.
	public UserRegistrationStepResponse createDraft(UserRegistrationStepOneRequest request) {
		UserRegistrationStepResponse response = userService.saveBankOfficerStepOneDraft(request);
		auditLogService.logAction(
			"BANK_OFFICER_DRAFT_CREATED",
			"Created Officer Draft: \"" + safe(request.firstName()) + " " + safe(request.lastName()) + "\"",
			"BANK_OFFICER",
			response.userId() == null ? null : String.valueOf(response.userId()),
			"Saved bank officer onboarding draft.",
			"INFO"
		);
		return response;
	}

	// Creates a new entity from validated request data.
	public UserRegistrationStepResponse create(UserRegistrationStepOneRequest request) {
		UserRegistrationStepResponse response = userService.continueBankOfficerStepOne(request);
		auditLogService.logAction(
			"BANK_OFFICER_CREATED",
			"Created Bank Officer: \"" + safe(request.firstName()) + " " + safe(request.lastName()) + "\"",
			"BANK_OFFICER",
			response.userId() == null ? null : String.valueOf(response.userId()),
			"Bank officer account was created successfully.",
			"SUCCESS"
		);
		return response;
	}

	@Transactional(readOnly = true)
	// Generates a suggested username for the Add Officer form.
	public String generateSuggestedUsername(String firstName, String lastName) {
		String normalizedFirst = sanitizeUsernameSegment(firstName);
		String normalizedLast = sanitizeUsernameSegment(lastName);

		String base = normalizedFirst + normalizedLast;
		if (base.isBlank()) {
			base = !normalizedFirst.isBlank() ? normalizedFirst : normalizedLast;
		}
		if (base.isBlank()) {
			base = "officer";
		}

		int maxBaseLength = Math.max(1, USERNAME_MAX_LENGTH - USERNAME_SUFFIX_LENGTH);
		if (base.length() > maxBaseLength) {
			base = base.substring(0, maxBaseLength);
		}

		for (int attempt = 0; attempt < USERNAME_ATTEMPT_LIMIT; attempt++) {
			String suffix = String.valueOf(100 + SECURE_RANDOM.nextInt(900));
			String candidate = base + suffix;
			if (!userRepository.existsByUsername(candidate)) {
				return candidate;
			}
		}

		long rollingSuffix = System.currentTimeMillis() % 1_000_000L;
		String candidate = buildUsernameWithSuffix(base, rollingSuffix);
		while (userRepository.existsByUsername(candidate)) {
			rollingSuffix++;
			candidate = buildUsernameWithSuffix(base, rollingSuffix);
		}
		return candidate;
	}

	// Generates a suggested password for the Add Officer form.
	public String generateSuggestedPassword() {
		char[] password = new char[GENERATED_PASSWORD_LENGTH];
		for (int index = 0; index < GENERATED_PASSWORD_LENGTH; index++) {
			int randomIndex = SECURE_RANDOM.nextInt(GENERATED_PASSWORD_CHARS.length());
			password[index] = GENERATED_PASSWORD_CHARS.charAt(randomIndex);
		}
		return new String(password);
	}

	@Transactional(readOnly = true)
	// Returns all records needed by the admin table view.
	public List<AdminBankOfficerSummaryResponse> getAll() {
		return bankOfficerRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
	}

	@Transactional
	// Updates only the status field for the selected record.
	public AdminBankOfficerSummaryResponse updateStatus(Long userId, String status) {
		BankOfficer officer = findByUserId(userId);
		User user = officer.getUser();
		String normalizedStatus = normalizeStatus(status);
		user.setStatus(normalizedStatus);
		userRepository.save(user);
		AdminBankOfficerSummaryResponse response = toResponse(officer);
		auditLogService.logAction(
			"BANK_OFFICER_STATUS_CHANGED",
			"Changed Officer Status: \"" + safe(response.fullName()) + "\" -> " + normalizedStatus,
			"BANK_OFFICER",
			safe(response.employeeCode()),
			"Updated officer user status.",
			"ACTIVE".equals(normalizedStatus) ? "SUCCESS" : "WARNING"
		);
		return response;
	}

	@Transactional
	// Updates an existing record from validated request fields.
	public AdminBankOfficerSummaryResponse update(Long userId, AdminBankOfficerUpdateRequest request) {
		BankOfficer officer = findByUserId(userId);
		User user = officer.getUser();

		String normalizedEmail = safe(request.email()).toLowerCase(Locale.ROOT);
		if (normalizedEmail.isBlank()) {
			throw new IllegalArgumentException("Email is required.");
		}
		if (!BANK_OFFICER_EMAIL_REGEX.matcher(normalizedEmail).matches()) {
			throw new IllegalArgumentException("Email must be in the format name@gmail.com.");
		}
		if (userRepository.existsByEmailAndUserIdNot(normalizedEmail, user.getUserId())) {
			throw new IllegalArgumentException("Email is already in use.");
		}

		Branch branch = branchRepository
			.findById(request.branchId())
			.orElseThrow(() -> new IllegalArgumentException("Branch not found."));

		user.setFirstName(safe(request.firstName()));
		user.setLastName(safe(request.lastName()));
		user.setEmail(normalizedEmail);
		user.setPhone(safe(request.contactNumber()));
		officer.setBranch(branch);

		userRepository.save(user);
		bankOfficerRepository.save(officer);
		AdminBankOfficerSummaryResponse response = toResponse(officer);
		auditLogService.logAction(
			"BANK_OFFICER_UPDATED",
			"Updated Officer Details: \"" + safe(response.fullName()) + "\"",
			"BANK_OFFICER",
			safe(response.employeeCode()),
			"Updated officer profile details and branch assignment.",
			"INFO"
		);
		return response;
	}

	@Transactional
	// Permanently removes the selected officer record.
	public AdminBankOfficerSummaryResponse deletePermanently(Long userId) {
		BankOfficer officer = findByUserId(userId);
		Long officerId = officer.getOfficerId();
		if (bankCustomerRepository.existsByOfficer_OfficerId(officerId)) {
			throw new IllegalArgumentException(
				"This officer cannot be deleted because bank customers are assigned to the officer."
			);
		}
		if (
			bankCustomerFinancialRecordRepository.existsByVerifiedByOfficer_OfficerId(officerId) ||
			bankCreditEvaluationRepository.existsByEvaluatedByOfficer_OfficerId(officerId)
		) {
			throw new IllegalArgumentException(
				"This officer cannot be deleted because financial or evaluation records are linked to the officer."
			);
		}

		AdminBankOfficerSummaryResponse response = toResponse(officer);
		User user = officer.getUser();
		bankOfficerRepository.delete(officer);
		userRepository.delete(user);
		auditLogService.logAction(
			"BANK_OFFICER_DELETED",
			"Deleted Officer Permanently: \"" + safe(response.fullName()) + "\"",
			"BANK_OFFICER",
			safe(response.employeeCode()),
			"Officer and linked user account were deleted permanently.",
			"WARNING"
		);
		return response;
	}

	private BankOfficer findByUserId(Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("User id must be a positive number.");
		}

		return bankOfficerRepository.findByUser_UserId(userId).orElseThrow(() -> new IllegalArgumentException("Bank officer not found."));
	}

	private String normalizeStatus(String status) {
		String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("Status is required.");
		}
		if (!ALLOWED_STATUSES.contains(normalized)) {
			throw new IllegalArgumentException("Status must be ACTIVE, INACTIVE, or LOCKED.");
		}
		return normalized;
	}

	private AdminBankOfficerSummaryResponse toResponse(BankOfficer officer) {
		User user = officer.getUser();
		String fullName = ((safe(user.getFirstName()) + " " + safe(user.getLastName())).trim());
		if (fullName.isBlank()) {
			fullName = safe(user.getUsername());
		}

		return new AdminBankOfficerSummaryResponse(
			user.getUserId(),
			safe(officer.getEmployeeCode()),
			fullName,
			safe(user.getEmail()),
			safe(user.getPhone()),
			safe(user.getStatus()),
			officer.getCreatedAt() == null ? null : officer.getCreatedAt().toString(),
			user.getUpdatedAt() == null ? null : user.getUpdatedAt().toString(),
			officer.getBranch() == null ? null : officer.getBranch().getBranchId(),
			officer.getBranch() == null ? "" : safe(officer.getBranch().getBranchName())
		);
	}

	private String sanitizeUsernameSegment(String value) {
		String normalized = safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
		if (normalized.isBlank()) {
			return "";
		}
		return normalized;
	}

	private String buildUsernameWithSuffix(String base, long suffix) {
		String candidate = base + suffix;
		if (candidate.length() > USERNAME_MAX_LENGTH) {
			return candidate.substring(0, USERNAME_MAX_LENGTH);
		}
		return candidate;
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
