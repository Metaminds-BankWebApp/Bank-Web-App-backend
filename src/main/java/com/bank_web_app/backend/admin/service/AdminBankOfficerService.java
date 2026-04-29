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
	private final BranchRepository branchRepository;
	private final UserRepository userRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository;
	private final BankCreditEvaluationRepository bankCreditEvaluationRepository;

	public AdminBankOfficerService(
		UserService userService,
		BankOfficerRepository bankOfficerRepository,
		BranchRepository branchRepository,
		UserRepository userRepository,
		BankCustomerRepository bankCustomerRepository,
		BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository,
		BankCreditEvaluationRepository bankCreditEvaluationRepository
	) {
		this.userService = userService;
		this.bankOfficerRepository = bankOfficerRepository;
		this.branchRepository = branchRepository;
		this.userRepository = userRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankCustomerFinancialRecordRepository = bankCustomerFinancialRecordRepository;
		this.bankCreditEvaluationRepository = bankCreditEvaluationRepository;
	}

	public UserRegistrationStepResponse createDraft(UserRegistrationStepOneRequest request) {
		return userService.saveBankOfficerStepOneDraft(request);
	}

	public UserRegistrationStepResponse create(UserRegistrationStepOneRequest request) {
		return userService.continueBankOfficerStepOne(request);
	}

	@Transactional(readOnly = true)
	public List<AdminBankOfficerSummaryResponse> getAll() {
		return bankOfficerRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
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
	public AdminBankOfficerSummaryResponse update(Long userId, AdminBankOfficerUpdateRequest request) {
		BankOfficer officer = findByUserId(userId);
		User user = officer.getUser();

		String normalizedEmail = safe(request.email()).toLowerCase(Locale.ROOT);
		if (normalizedEmail.isBlank()) {
			throw new IllegalArgumentException("Email is required.");
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
		return toResponse(officer);
	}

	@Transactional
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

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
