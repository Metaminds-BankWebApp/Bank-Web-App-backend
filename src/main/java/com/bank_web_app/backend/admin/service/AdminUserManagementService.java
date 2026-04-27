package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.admin.dto.response.AdminUserManagementUserResponse;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserManagementService {

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String ROLE_PUBLIC_CUSTOMER = "PUBLIC_CUSTOMER";
	private static final String CUSTOMER_TYPE_ALL = "ALL";
	private static final String CUSTOMER_TYPE_BANK = "BANK";
	private static final String CUSTOMER_TYPE_PUBLIC = "PUBLIC";
	private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "LOCKED");

	private final UserRepository userRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final PublicCustomerProfileRepository publicCustomerProfileRepository;

	public AdminUserManagementService(
		UserRepository userRepository,
		BankCustomerRepository bankCustomerRepository,
		PublicCustomerProfileRepository publicCustomerProfileRepository
	) {
		this.userRepository = userRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminUserManagementUserResponse> getUsers(String customerType, String search) {
		String normalizedType = normalizeCustomerType(customerType);
		String normalizedSearch = normalizeSearch(search);

		List<String> roleNames = switch (normalizedType) {
			case CUSTOMER_TYPE_BANK -> List.of(ROLE_BANK_CUSTOMER);
			case CUSTOMER_TYPE_PUBLIC -> List.of(ROLE_PUBLIC_CUSTOMER);
			default -> List.of(ROLE_BANK_CUSTOMER, ROLE_PUBLIC_CUSTOMER);
		};

		List<User> users = userRepository.findAllByRole_RoleNameInOrderByUpdatedAtDesc(roleNames);
		if (users.isEmpty()) {
			return List.of();
		}

		List<Long> userIds = users.stream().map(User::getUserId).toList();
		Map<Long, String> bankCustomerCodesByUserId = bankCustomerRepository
			.findAllByUser_UserIdIn(userIds)
			.stream()
			.collect(
				java.util.stream.Collectors.toMap(
					entry -> entry.getUser().getUserId(),
					BankCustomer::getCustomerCode
				)
			);
		Map<Long, String> publicCustomerCodesByUserId = publicCustomerProfileRepository
			.findAllByUser_UserIdIn(userIds)
			.stream()
			.collect(
				java.util.stream.Collectors.toMap(
					entry -> entry.getUser().getUserId(),
					PublicCustomerProfile::getCustomerCode
				)
			);

		return users
			.stream()
			.map(user -> toResponse(user, bankCustomerCodesByUserId, publicCustomerCodesByUserId))
			.filter(row -> matchesSearch(row, normalizedSearch))
			.toList();
	}

	@Transactional
	public AdminUserManagementUserResponse updateUserStatus(Long userId, String status) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("User id must be a positive number.");
		}

		String normalizedStatus = normalizeStatus(status);
		User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
		String roleName = safe(user.getRole() == null ? null : user.getRole().getRoleName());
		if (!ROLE_BANK_CUSTOMER.equals(roleName) && !ROLE_PUBLIC_CUSTOMER.equals(roleName)) {
			throw new IllegalArgumentException("Only BANK and PUBLIC customers can be managed in this module.");
		}

		user.setStatus(normalizedStatus);
		User saved = userRepository.save(user);

		String bankCode = bankCustomerRepository
			.findByUser_UserId(saved.getUserId())
			.map(BankCustomer::getCustomerCode)
			.orElse(null);
		String publicCode = publicCustomerProfileRepository
			.findByUser_UserId(saved.getUserId())
			.map(PublicCustomerProfile::getCustomerCode)
			.orElse(null);

		Map<Long, String> bankCodesByUserId = bankCode == null ? Map.of() : Map.of(saved.getUserId(), bankCode);
		Map<Long, String> publicCodesByUserId = publicCode == null ? Map.of() : Map.of(saved.getUserId(), publicCode);
		return toResponse(saved, bankCodesByUserId, publicCodesByUserId);
	}

	private AdminUserManagementUserResponse toResponse(
		User user,
		Map<Long, String> bankCustomerCodesByUserId,
		Map<Long, String> publicCustomerCodesByUserId
	) {
		Long userId = user.getUserId();
		String roleName = safe(user.getRole() == null ? null : user.getRole().getRoleName());
		boolean isBankCustomer = ROLE_BANK_CUSTOMER.equals(roleName);
		String customerType = isBankCustomer ? CUSTOMER_TYPE_BANK : CUSTOMER_TYPE_PUBLIC;

		String customerId = isBankCustomer
			? safe(bankCustomerCodesByUserId.get(userId))
			: safe(publicCustomerCodesByUserId.get(userId));
		if (customerId.isBlank()) {
			customerId = fallbackCustomerCode(isBankCustomer ? "BC" : "PC", userId);
		}

		String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
		if (fullName.isBlank()) {
			fullName = safe(user.getUsername());
		}

		return new AdminUserManagementUserResponse(
			userId,
			customerId,
			fullName,
			safe(user.getEmail()),
			safe(user.getPhone()),
			user.getCreatedAt() == null ? null : user.getCreatedAt().toString(),
			customerType,
			safe(user.getStatus()),
			safe(user.getProfilePictureUrl())
		);
	}

	private boolean matchesSearch(AdminUserManagementUserResponse row, String normalizedSearch) {
		if (normalizedSearch.isBlank()) {
			return true;
		}

		return containsIgnoreCase(row.customerId(), normalizedSearch)
			|| containsIgnoreCase(row.fullName(), normalizedSearch)
			|| containsIgnoreCase(row.email(), normalizedSearch)
			|| containsIgnoreCase(row.contactNumber(), normalizedSearch)
			|| containsIgnoreCase(row.joinedDate(), normalizedSearch)
			|| containsIgnoreCase(row.customerType(), normalizedSearch)
			|| containsIgnoreCase(row.status(), normalizedSearch);
	}

	private boolean containsIgnoreCase(String source, String search) {
		if (source == null || source.isBlank()) {
			return false;
		}
		return source.toLowerCase(Locale.ROOT).contains(search);
	}

	private String normalizeCustomerType(String customerType) {
		String normalized = safe(customerType).toUpperCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return CUSTOMER_TYPE_ALL;
		}
		if (
			!CUSTOMER_TYPE_ALL.equals(normalized) &&
			!CUSTOMER_TYPE_BANK.equals(normalized) &&
			!CUSTOMER_TYPE_PUBLIC.equals(normalized)
		) {
			throw new IllegalArgumentException("customerType must be ALL, BANK, or PUBLIC.");
		}
		return normalized;
	}

	private String normalizeStatus(String status) {
		String normalized = safe(status).toUpperCase(Locale.ROOT);
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("Status is required.");
		}
		if (!ALLOWED_STATUSES.contains(normalized)) {
			throw new IllegalArgumentException("Status must be ACTIVE, INACTIVE, or LOCKED.");
		}
		return normalized;
	}

	private String normalizeSearch(String search) {
		return safe(search).toLowerCase(Locale.ROOT);
	}

	private String fallbackCustomerCode(String prefix, Long userId) {
		if (userId == null) {
			return prefix + "-00000";
		}
		return String.format("%s-%05d", prefix, userId);
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
