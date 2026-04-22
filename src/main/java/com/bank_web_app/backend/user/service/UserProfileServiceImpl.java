package com.bank_web_app.backend.user.service;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.common.exception.DuplicateFieldsException;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.user.dto.request.UserProfileUpdateRequest;
import com.bank_web_app.backend.user.dto.response.UserProfileResponse;
import com.bank_web_app.backend.user.dto.response.UserProfileSummaryItemResponse;
import com.bank_web_app.backend.user.dto.response.UserProfileUpdateResponse;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileServiceImpl implements UserProfileService {

	private static final String ROLE_PUBLIC_CUSTOMER = "PUBLIC_CUSTOMER";
	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String ROLE_BANK_OFFICER = "BANK_OFFICER";
	private static final String ROLE_ADMIN = "ADMIN";
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,}$");

	private final UserRepository userRepository;
	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final PasswordEncoder passwordEncoder;

	public UserProfileServiceImpl(
		UserRepository userRepository,
		PublicCustomerProfileRepository publicCustomerProfileRepository,
		BankCustomerRepository bankCustomerRepository,
		BankOfficerRepository bankOfficerRepository,
		PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional(readOnly = true)
	public UserProfileResponse getMyProfile() {
		return buildProfile(resolveLoggedInUser());
	}

	@Override
	@Transactional
	public UserProfileUpdateResponse updateMyProfile(UserProfileUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Request body is required.");
		}

		User user = resolveLoggedInUser();
		updatePersonalDetails(user, request);
		updateUsernameIfRequested(user, request.newUsername());
		updatePasswordIfRequested(user, request.currentPassword(), request.newPassword(), request.confirmPassword());

		User savedUser = userRepository.save(user);
		return new UserProfileUpdateResponse("Profile updated successfully.", buildProfile(savedUser));
	}

	private void updatePersonalDetails(User user, UserProfileUpdateRequest request) {
		NameParts nameParts = splitFullName(request.fullName());
		String normalizedEmail = normalizeEmail(request.email());
		String normalizedPhone = normalizeText(request.phone());
		String normalizedAddress = normalizeNullableText(request.address());

		Map<String, String> duplicateFieldErrors = new LinkedHashMap<>();
		userRepository
			.findByEmailIgnoreCase(normalizedEmail)
			.filter(existing -> !existing.getUserId().equals(user.getUserId()))
			.ifPresent(existing -> duplicateFieldErrors.put("email", "Email is already in use."));

		if (!duplicateFieldErrors.isEmpty()) {
			throw new DuplicateFieldsException(duplicateFieldErrors);
		}

		if (ROLE_PUBLIC_CUSTOMER.equals(resolveRoleName(user)) && normalizedAddress.isBlank()) {
			throw new IllegalArgumentException("Address is required for public customer profiles.");
		}

		user.setFirstName(nameParts.firstName());
		user.setLastName(nameParts.lastName());
		user.setEmail(normalizedEmail);
		user.setPhone(normalizedPhone);

		if (request.address() != null || ROLE_PUBLIC_CUSTOMER.equals(resolveRoleName(user))) {
			user.setAddress(normalizedAddress.isBlank() ? null : normalizedAddress);
		}
	}

	private void updateUsernameIfRequested(User user, String requestedUsername) {
		String normalizedUsername = normalizeNullableText(requestedUsername);
		if (normalizedUsername.isBlank()) {
			return;
		}
		if (normalizedUsername.length() < 4) {
			throw new IllegalArgumentException("New username must be at least 4 characters long.");
		}

		if (normalizedUsername.equalsIgnoreCase(safe(user.getUsername()))) {
			throw new IllegalArgumentException("New username must be different from current username.");
		}

		Map<String, String> duplicateFieldErrors = new LinkedHashMap<>();
		userRepository
			.findByUsernameIgnoreCase(normalizedUsername)
			.filter(existing -> !existing.getUserId().equals(user.getUserId()))
			.ifPresent(existing -> duplicateFieldErrors.put("newUsername", "Username is already in use."));
		if (!duplicateFieldErrors.isEmpty()) {
			throw new DuplicateFieldsException(duplicateFieldErrors);
		}

		user.setUsername(normalizedUsername);
	}

	private void updatePasswordIfRequested(
		User user,
		String currentPassword,
		String newPassword,
		String confirmPassword
	) {
		String current = safe(currentPassword);
		String next = safe(newPassword);
		String confirm = safe(confirmPassword);
		boolean hasPasswordIntent = !(current.isBlank() && next.isBlank() && confirm.isBlank());
		if (!hasPasswordIntent) {
			return;
		}

		if (current.isBlank()) {
			throw new IllegalArgumentException("Current password is required to change password.");
		}
		if (next.isBlank()) {
			throw new IllegalArgumentException("New password is required.");
		}
		if (confirm.isBlank()) {
			throw new IllegalArgumentException("Please confirm your new password.");
		}
		if (!next.equals(confirm)) {
			throw new IllegalArgumentException("Confirm password does not match.");
		}
		if (!PASSWORD_PATTERN.matcher(next).matches()) {
			throw new IllegalArgumentException("Password must be at least 10 characters and include uppercase, lowercase, and numbers.");
		}
		if (!matchesPasswordAndUpgradeIfNeeded(user, current)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
		}
		if (passwordEncoder.matches(next, user.getPasswordHash())) {
			throw new IllegalArgumentException("New password must be different from the current password.");
		}

		user.setPasswordHash(passwordEncoder.encode(next));
	}

	private UserProfileResponse buildProfile(User user) {
		String roleName = resolveRoleName(user);
		String fullName = buildFullName(user);
		String initials = buildInitials(fullName.isBlank() ? safe(user.getUsername()) : fullName);
		String badgeText = roleName.replace('_', ' ');
		String roleDisplayName = toTitleCase(roleName.replace('_', ' '));
		String customerCode = null;
		String employeeCode = null;
		String accountNumber = null;
		String branchName = null;
		String branchLocation = null;
		List<UserProfileSummaryItemResponse> summaryItems = new ArrayList<>();

		if (ROLE_PUBLIC_CUSTOMER.equals(roleName)) {
			PublicCustomerProfile profile = publicCustomerProfileRepository
				.findByUser_UserId(user.getUserId())
				.orElse(null);
			customerCode = profile == null ? formatCode("PC", user.getUserId()) : safe(profile.getCustomerCode());
			summaryItems.add(new UserProfileSummaryItemResponse("User ID", customerCode));
			summaryItems.add(new UserProfileSummaryItemResponse("Address", fallback(user.getAddress(), "-")));
			summaryItems.add(new UserProfileSummaryItemResponse("Joined Date", formatDate(user.getCreatedAt())));
		} else if (ROLE_BANK_CUSTOMER.equals(roleName)) {
			BankCustomer bankCustomer = bankCustomerRepository.findByUser_UserId(user.getUserId()).orElse(null);
			customerCode = bankCustomer == null ? formatCode("BC", user.getUserId()) : safe(bankCustomer.getCustomerCode());
			accountNumber = bankCustomer != null && bankCustomer.getAccount() != null
				? safe(bankCustomer.getAccount().getAccountNumber())
				: null;
			branchName = bankCustomer != null && bankCustomer.getBranch() != null
				? safe(bankCustomer.getBranch().getBranchName())
				: null;
			branchLocation = bankCustomer != null && bankCustomer.getBranch() != null
				? fallback(bankCustomer.getBranch().getAddress(), branchName)
				: null;
			summaryItems.add(new UserProfileSummaryItemResponse("User ID", customerCode));
			summaryItems.add(new UserProfileSummaryItemResponse("Branch Location", fallback(branchLocation, "-")));
			summaryItems.add(new UserProfileSummaryItemResponse("Joined Date", formatDate(user.getCreatedAt())));
		} else if (ROLE_BANK_OFFICER.equals(roleName)) {
			BankOfficer officer = bankOfficerRepository.findByUser_UserId(user.getUserId()).orElse(null);
			employeeCode = officer == null ? formatCode("EMP", user.getUserId()) : safe(officer.getEmployeeCode());
			branchName = officer != null && officer.getBranch() != null
				? safe(officer.getBranch().getBranchName())
				: null;
			branchLocation = officer != null && officer.getBranch() != null
				? fallback(officer.getBranch().getAddress(), branchName)
				: null;
			summaryItems.add(new UserProfileSummaryItemResponse("Employee ID", employeeCode));
			summaryItems.add(new UserProfileSummaryItemResponse("Branch Location", fallback(branchLocation, "-")));
			summaryItems.add(new UserProfileSummaryItemResponse("Joined Date", formatDate(user.getCreatedAt())));
		} else if (ROLE_ADMIN.equals(roleName)) {
			employeeCode = formatCode("EMP", user.getUserId());
			summaryItems.add(new UserProfileSummaryItemResponse("Employee ID", employeeCode));
			summaryItems.add(new UserProfileSummaryItemResponse("Joined Date", formatDate(user.getCreatedAt())));
		} else {
			summaryItems.add(new UserProfileSummaryItemResponse("User ID", formatCode("USR", user.getUserId())));
			summaryItems.add(new UserProfileSummaryItemResponse("Joined Date", formatDate(user.getCreatedAt())));
		}

		return new UserProfileResponse(
			user.getUserId(),
			roleName,
			roleDisplayName,
			badgeText,
			fullName,
			initials,
			safe(user.getEmail()),
			safe(user.getPhone()),
			safe(user.getUsername()),
			safe(user.getNic()),
			user.getDob() == null ? null : user.getDob().toString(),
			safe(user.getAddress()),
			safe(user.getProfilePictureUrl()),
			safe(user.getStatus()),
			formatDate(user.getCreatedAt()),
			customerCode,
			employeeCode,
			accountNumber,
			branchName,
			branchLocation,
			List.copyOf(summaryItems)
		);
	}

	private User resolveLoggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		return userRepository
			.findByEmailIgnoreCase(normalizedPrincipal)
			.or(() -> userRepository.findByUsernameIgnoreCase(principal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));
	}

	private boolean matchesPasswordAndUpgradeIfNeeded(User user, String rawPassword) {
		String stored = user.getPasswordHash();
		if (stored == null || stored.isBlank()) {
			return false;
		}
		if (isBcryptHash(stored)) {
			return passwordEncoder.matches(rawPassword, stored);
		}
		if (!stored.equals(rawPassword)) {
			return false;
		}
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		return true;
	}

	private boolean isBcryptHash(String value) {
		return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
	}

	private String resolveRoleName(User user) {
		return user.getRole() == null || user.getRole().getRoleName() == null
			? ""
			: user.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);
	}

	private String buildFullName(User user) {
		String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
		return fullName.isBlank() ? safe(user.getEmail()) : fullName;
	}

	private NameParts splitFullName(String fullName) {
		String normalized = normalizeText(fullName);
		int firstSpaceIndex = normalized.indexOf(' ');
		if (firstSpaceIndex < 0) {
			return new NameParts(normalized, "");
		}
		String firstName = normalized.substring(0, firstSpaceIndex).trim();
		String lastName = normalized.substring(firstSpaceIndex + 1).trim();
		return new NameParts(firstName, lastName);
	}

	private String buildInitials(String value) {
		String normalized = normalizeNullableText(value);
		if (normalized.isBlank()) {
			return "NA";
		}

		String[] parts = normalized.split("\\s+");
		StringBuilder initials = new StringBuilder();
		for (String part : parts) {
			if (!part.isBlank()) {
				initials.append(Character.toUpperCase(part.charAt(0)));
			}
			if (initials.length() == 2) {
				break;
			}
		}
		if (initials.length() == 0) {
			return normalized.substring(0, Math.min(2, normalized.length())).toUpperCase(Locale.ROOT);
		}
		return initials.toString();
	}

	private String formatDate(LocalDateTime dateTime) {
		LocalDate date = dateTime == null ? null : dateTime.toLocalDate();
		return date == null ? "-" : date.toString();
	}

	private String formatCode(String prefix, Long value) {
		if (value == null) {
			return prefix + "-00000";
		}
		return String.format("%s-%05d", prefix, value);
	}

	private String normalizeEmail(String value) {
		return normalizeText(value).toLowerCase(Locale.ROOT);
	}

	private String normalizeText(String value) {
		String normalized = normalizeNullableText(value);
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("Required text value is missing.");
		}
		return normalized;
	}

	private String normalizeNullableText(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().replaceAll("\\s+", " ");
	}

	private String fallback(String primary, String fallback) {
		String normalizedPrimary = safe(primary);
		return normalizedPrimary.isBlank() ? safe(fallback) : normalizedPrimary;
	}

	private String toTitleCase(String value) {
		String normalized = safe(value).toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return normalized;
		}
		String[] parts = normalized.split("\\s+");
		StringBuilder result = new StringBuilder();
		for (String part : parts) {
			if (part.isBlank()) {
				continue;
			}
			if (result.length() > 0) {
				result.append(' ');
			}
			result.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				result.append(part.substring(1));
			}
		}
		return result.toString();
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private record NameParts(String firstName, String lastName) {}
}
