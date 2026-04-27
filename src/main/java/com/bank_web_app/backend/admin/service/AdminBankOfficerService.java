package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.admin.dto.response.AdminBankOfficerSummaryResponse;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.service.UserService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBankOfficerService {

	private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "LOCKED");

	private final UserService userService;
	private final BankOfficerRepository bankOfficerRepository;
	private final UserRepository userRepository;

	public AdminBankOfficerService(
		UserService userService,
		BankOfficerRepository bankOfficerRepository,
		UserRepository userRepository
	) {
		this.userService = userService;
		this.bankOfficerRepository = bankOfficerRepository;
		this.userRepository = userRepository;
	}

	public UserRegistrationStepResponse createDraft(UserRegistrationStepOneRequest request) {
		return userService.saveBankOfficerStepOneDraft(request);
	}

	public UserRegistrationStepResponse create(UserRegistrationStepOneRequest request) {
		return userService.continueBankOfficerStepOne(request);
	}

	@Transactional(readOnly = true)
	public List<AdminBankOfficerSummaryResponse> getAll() {
		return bankOfficerRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
	}

	@Transactional
	public AdminBankOfficerSummaryResponse updateStatus(Long userId, String status) {
		BankOfficer officer = findByUserId(userId);
		User user = officer.getUser();
		user.setStatus(normalizeStatus(status));
		userRepository.save(user);
		return toResponse(officer);
	}

	@Transactional
	public AdminBankOfficerSummaryResponse deactivate(Long userId) {
		return updateStatus(userId, "INACTIVE");
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

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
