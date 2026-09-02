package com.bank_web_app.backend.admin.service;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.bank_web_app.backend.admin.dto.request.AdminBankOfficerUpdateRequest;
import com.bank_web_app.backend.admin.dto.request.AdminBankOfficerCreateRequest;
import com.bank_web_app.backend.admin.dto.response.AdminBankOfficerSummaryResponse;
import com.bank_web_app.backend.common.exception.DuplicateFieldsException;
import com.bank_web_app.backend.common.identity.SriLankanNicDateOfBirth;
import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.auth.repository.PasswordResetTokenRepository;
import com.bank_web_app.backend.auth.repository.RefreshTokenRepository;
import com.bank_web_app.backend.auth.service.OfficerActivationService;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerFinancialRecordRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.creditlens.repository.BankCreditEvaluationRepository;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.notification.event.NotificationEventType;
import com.bank_web_app.backend.notification.repository.NotificationRepository;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import com.bank_web_app.backend.user.entity.Role;
import com.bank_web_app.backend.user.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Orchestrates Admin business logic, validation, and persistence workflows.
 */

@Service
public class AdminBankOfficerService {

	private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "SUSPEND");
	private static final String ROLE_BANK_OFFICER = "BANK_OFFICER";
	private static final String STATUS_PENDING_ACTIVATION = "PENDING_ACTIVATION";
	private static final int USERNAME_MAX_LENGTH = 50;
	private static final int USERNAME_SUFFIX_LENGTH = 3;
	private static final int USERNAME_ATTEMPT_LIMIT = 300;
	private static final Pattern BANK_OFFICER_EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);
	private static final ZoneId SRI_LANKA_TIME_ZONE = ZoneId.of("Asia/Colombo");
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final BankOfficerRepository bankOfficerRepository;
	private final BranchRepository branchRepository;
	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final NotificationRepository notificationRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository;
	private final BankCreditEvaluationRepository bankCreditEvaluationRepository;
	private final AuditLogService auditLogService;
	private final NotificationEventPublisher notificationEventPublisher;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final OfficerActivationService officerActivationService;

	public AdminBankOfficerService(
		BankOfficerRepository bankOfficerRepository,
		BranchRepository branchRepository,
		UserRepository userRepository,
		PasswordResetTokenRepository passwordResetTokenRepository,
		RefreshTokenRepository refreshTokenRepository,
		NotificationRepository notificationRepository,
		BankCustomerRepository bankCustomerRepository,
		BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository,
		BankCreditEvaluationRepository bankCreditEvaluationRepository,
		AuditLogService auditLogService,
		NotificationEventPublisher notificationEventPublisher,
		RoleRepository roleRepository,
		PasswordEncoder passwordEncoder,
		OfficerActivationService officerActivationService
	) {
		this.bankOfficerRepository = bankOfficerRepository;
		this.branchRepository = branchRepository;
		this.userRepository = userRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.notificationRepository = notificationRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankCustomerFinancialRecordRepository = bankCustomerFinancialRecordRepository;
		this.bankCreditEvaluationRepository = bankCreditEvaluationRepository;
		this.auditLogService = auditLogService;
		this.notificationEventPublisher = notificationEventPublisher;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.officerActivationService = officerActivationService;
	}

	// Creates a new entity from validated request data.
	@Transactional
	public UserRegistrationStepResponse create(AdminBankOfficerCreateRequest request) {
		LocalDate dateOfBirth = validateCreateRequest(request);
		Role role = roleRepository.findByRoleName(ROLE_BANK_OFFICER)
			.orElseThrow(() -> new IllegalStateException("Role BANK_OFFICER not found."));
		Branch branch = branchRepository.findById(request.branchId())
			.orElseThrow(() -> new IllegalArgumentException("Branch not found."));
		if (branch.getStatus() != com.bank_web_app.backend.admin.entity.BranchStatus.ACTIVE && branch.getStatus() != com.bank_web_app.backend.admin.entity.BranchStatus.MAINTENANCE) {
			throw new IllegalArgumentException("Only active or maintenance branches can be assigned to a bank officer.");
		}

		User user = new User();
		user.setRole(role);
		user.setFirstName(safe(request.firstName()));
		user.setLastName(safe(request.lastName()));
		user.setNic(safe(request.nic()));
		user.setDob(dateOfBirth);
		user.setEmail(safe(request.email()).toLowerCase(Locale.ROOT));
		user.setPhone(safe(request.mobile()));
		user.setProvince(safe(request.province()));
		user.setAddress(safe(request.address()));
		user.setUsername(safe(request.username()));
		// A random unusable bootstrap value satisfies the non-null database field; it is never disclosed.
		user.setPasswordHash(passwordEncoder.encode(generateSecret()));
		user.setStatus(STATUS_PENDING_ACTIVATION);
		userRepository.save(user);

		BankOfficer officer = new BankOfficer();
		officer.setUser(user);
		officer.setBranch(branch);
		officer.setEmployeeCode(generateEmployeeCode());
		if (request.createdByAdminUserId() != null) userRepository.findById(request.createdByAdminUserId()).ifPresent(officer::setCreatedByAdminUser);
		bankOfficerRepository.save(officer);

		officerActivationService.sendInitialInvitation(officer);
		UserRegistrationStepResponse response = new UserRegistrationStepResponse(user.getUserId(), ROLE_BANK_OFFICER, STATUS_PENDING_ACTIVATION, "Bank officer created and activation invitation sent.");
		auditLogService.logAction(
			"BANK_OFFICER_CREATED",
			"Created Bank Officer: \"" + safe(request.firstName()) + " " + safe(request.lastName()) + "\"",
			"BANK_OFFICER",
			response.userId() == null ? null : String.valueOf(response.userId()),
			"Bank officer account is pending activation and an invitation email was sent.",
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


	@Transactional
	public void resendActivation(Long userId) {
		BankOfficer officer = findByUserId(userId);
		User user = officer.getUser();
		officerActivationService.resendByUserId(userId);
		auditLogService.logAction("BANK_OFFICER_ACTIVATION_RESENT", "Resent Officer Activation: \"" + safe(user.getUsername()) + "\"", "BANK_OFFICER", safe(officer.getEmployeeCode()), "Sent a replacement one-time activation invitation.", "INFO");
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
		if (STATUS_PENDING_ACTIVATION.equalsIgnoreCase(safe(user.getStatus()))) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"A pending officer's status cannot be changed until their first successful sign-in."
			);
		}
		String normalizedStatus = normalizeStatus(status);
		user.setStatus(normalizedStatus);
		userRepository.save(user);
		if (!"ACTIVE".equals(normalizedStatus)) {
			refreshTokenRepository.deleteByUser_UserId(user.getUserId());
		}
		AdminBankOfficerSummaryResponse response = toResponse(officer);
		auditLogService.logAction(
			"BANK_OFFICER_STATUS_CHANGED",
			"Changed Officer Status: \"" + safe(response.fullName()) + "\" -> " + normalizedStatus,
			"BANK_OFFICER",
			safe(response.employeeCode()),
			"Updated officer user status.",
			"ACTIVE".equals(normalizedStatus) ? "SUCCESS" : "WARNING"
		);
		notificationEventPublisher.publish(
			NotificationEventType.OFFICER_STATUS_CHANGED,
			user.getUserId(),
			null,
			user.getUserId(),
			Map.of("status", normalizedStatus)
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
		Map<String, String> duplicateFieldErrors = new java.util.LinkedHashMap<>();
		if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(normalizedEmail, user.getUserId())) {
			duplicateFieldErrors.put("email", "Email is already in use.");
		}
		String normalizedPhone = safe(request.contactNumber());
		if (branchRepository.existsByBranchPhone(normalizedPhone) ||
			userRepository.existsByPhoneAndRole_RoleNameInAndUserIdNot(normalizedPhone, List.of("BANK_OFFICER"), user.getUserId())) {
			duplicateFieldErrors.put("contactNumber", "Contact number is already in use.");
		}
		if (!duplicateFieldErrors.isEmpty()) {
			throw new DuplicateFieldsException(duplicateFieldErrors);
		}

		Branch branch = branchRepository
			.findById(request.branchId())
			.orElseThrow(() -> new IllegalArgumentException("Branch not found."));

		user.setFirstName(safe(request.firstName()));
		user.setLastName(safe(request.lastName()));
		user.setEmail(normalizedEmail);
		user.setPhone(normalizedPhone);
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
		passwordResetTokenRepository.deleteByUser_UserId(userId);
		bankOfficerRepository.delete(officer);
		auditLogService.detachActorForDeletedUser(userId);
		notificationRepository.deleteByRecipient_UserId(userId);
		refreshTokenRepository.deleteByUser_UserId(userId);
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
			throw new IllegalArgumentException("Status must be ACTIVE or SUSPEND.");
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
			normalizeDisplayStatus(user.getStatus()),
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

	private String normalizeDisplayStatus(String status) {
		if (STATUS_PENDING_ACTIVATION.equalsIgnoreCase(safe(status))) {
			return STATUS_PENDING_ACTIVATION;
		}
		return "ACTIVE".equalsIgnoreCase(safe(status)) ? "ACTIVE" : "SUSPEND";
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

	private LocalDate validateCreateRequest(AdminBankOfficerCreateRequest request) {
		if (request == null) throw new IllegalArgumentException("Request body is required.");
		String username = safe(request.username());
		String email = safe(request.email()).toLowerCase(Locale.ROOT);
		String nic = safe(request.nic());
		String phone = safe(request.mobile());
		if (!BANK_OFFICER_EMAIL_REGEX.matcher(email).matches()) throw new IllegalArgumentException("Email must be in the format name@gmail.com.");
		LocalDate dateOfBirth = SriLankanNicDateOfBirth.parse(nic)
			.orElseThrow(() -> new IllegalArgumentException("NIC contains an invalid date of birth."));
		if (!dateOfBirth.toString().equals(safe(request.dob()))) {
			throw new IllegalArgumentException("Date of birth must match the value derived from NIC.");
		}
		if (dateOfBirth.plusYears(18).isAfter(LocalDate.now(SRI_LANKA_TIME_ZONE))) {
			throw new IllegalArgumentException("Bank officer must be at least 18 years old.");
		}
		if (userRepository.existsByUsername(username)) throw new DuplicateFieldsException(Map.of("username", "Username is already in use."));
		if (userRepository.existsByNic(nic)) throw new DuplicateFieldsException(Map.of("nic", "NIC is already in use."));
		if (userRepository.existsByEmailIgnoreCaseAndRole_RoleName(email, ROLE_BANK_OFFICER)) throw new DuplicateFieldsException(Map.of("email", "Email is already in use."));
		if (userRepository.existsByPhoneAndRole_RoleNameIn(phone, List.of(ROLE_BANK_OFFICER))) throw new DuplicateFieldsException(Map.of("mobile", "Contact number is already in use."));
		return dateOfBirth;
	}

	private String generateSecret() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String generateEmployeeCode() {
		long next = bankOfficerRepository.count() + 1;
		String code = String.format("EMP-BO-%05d", next);
		while (bankOfficerRepository.existsByEmployeeCode(code)) code = String.format("EMP-BO-%05d", ++next);
		return code;
	}

}
