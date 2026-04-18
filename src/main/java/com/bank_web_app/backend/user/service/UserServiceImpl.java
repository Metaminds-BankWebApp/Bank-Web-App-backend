package com.bank_web_app.backend.user.service;

import com.bank_web_app.backend.common.exception.DuplicateFieldsException;
import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.entity.Role;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.RoleRepository;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String ROLE_PUBLIC_CUSTOMER = "PUBLIC_CUSTOMER";
	private static final String ROLE_BANK_OFFICER = "BANK_OFFICER";
	private static final String STATUS_DRAFT = "DRAFT";
	private static final String STATUS_PENDING_STEP_2 = "PENDING_STEP_2";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public UserRegistrationStepResponse saveBankCustomerStepOneDraft(BankCustomerStepOneRequest request) {
		return saveRoleStepOne(request, ROLE_BANK_CUSTOMER, STATUS_DRAFT, "Bank customer step one draft saved successfully.");
	}

	@Override
	@Transactional
	public UserRegistrationStepResponse continueBankCustomerStepOne(BankCustomerStepOneRequest request) {
		return saveRoleStepOne(request, ROLE_BANK_CUSTOMER, STATUS_PENDING_STEP_2, "Bank customer step one saved. Continue to step two.");
	}

	@Override
	@Transactional
	public UserRegistrationStepResponse savePublicCustomerStepOneDraft(BankCustomerStepOneRequest request) {
		return saveRoleStepOne(request, ROLE_PUBLIC_CUSTOMER, STATUS_DRAFT, "Public customer step one draft saved successfully.");
	}

	@Override
	@Transactional
	public UserRegistrationStepResponse continuePublicCustomerStepOne(BankCustomerStepOneRequest request) {
		return saveRoleStepOne(request, ROLE_PUBLIC_CUSTOMER, STATUS_PENDING_STEP_2, "Public customer step one saved. Continue to step two.");
	}

	@Override
	@Transactional
	public UserRegistrationStepResponse saveBankOfficerStepOneDraft(BankCustomerStepOneRequest request) {
		return saveRoleStepOne(request, ROLE_BANK_OFFICER, STATUS_DRAFT, "Bank officer step one draft saved successfully.");
	}

	@Override
	@Transactional
	public UserRegistrationStepResponse continueBankOfficerStepOne(BankCustomerStepOneRequest request) {
		return saveRoleStepOne(request, ROLE_BANK_OFFICER, STATUS_PENDING_STEP_2, "Bank officer step one saved. Continue to step two.");
	}

	@Override
	@Transactional(readOnly = true)
	public List<BankCustomerSummaryResponse> getBankCustomersForOfficer() {
		return userRepository
			.findAllByRole_RoleNameOrderByUpdatedAtDesc(ROLE_BANK_CUSTOMER)
			.stream()
			.map(user ->
				new BankCustomerSummaryResponse(
					user.getUserId(),
					formatCustomerId(user.getUserId()),
					(safe(user.getFirstName()) + " " + safe(user.getLastName())).trim(),
					safe(user.getNic()),
					safe(user.getEmail()),
					safe(user.getPhone()),
					safe(user.getStatus()),
					user.getUpdatedAt() == null ? null : user.getUpdatedAt().toString()
				)
			)
			.toList();
	}

	private UserRegistrationStepResponse saveRoleStepOne(
		BankCustomerStepOneRequest request,
		String roleName,
		String status,
		String successMessage
	) {
		validateRequest(request);

		Role registrationRole = roleRepository
			.findByRoleName(roleName)
			.orElseThrow(() -> new IllegalStateException("Role " + roleName + " not found in roles table."));

		String username = request.username().trim();
		String email = request.email().trim();
		String nic = request.nic().trim();

		LinkedHashMap<String, String> duplicateFieldErrors = new LinkedHashMap<>();
		if (userRepository.existsByUsername(username)) {
			duplicateFieldErrors.put("username", "Username is already in use.");
		}

		if (userRepository.existsByEmail(email)) {
			duplicateFieldErrors.put("email", "Email is already in use.");
		}

		if (userRepository.existsByNic(nic)) {
			duplicateFieldErrors.put("nic", "NIC is already in use.");
		}

		if (!duplicateFieldErrors.isEmpty()) {
			throw new DuplicateFieldsException(duplicateFieldErrors);
		}

		User user = new User();

		user.setRole(registrationRole);
		user.setUsername(username);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setPhone(request.mobile().trim());
		user.setNic(nic);
		user.setDob(parseDob(request.dob()));
		user.setProvince(request.province().trim());
		user.setAddress(request.address().trim());
		user.setStatus(status);

		User saved = userRepository.save(user);

		return new UserRegistrationStepResponse(
			saved.getUserId(),
			roleName,
			status,
			successMessage
		);
	}

	private void validateRequest(BankCustomerStepOneRequest request) {
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
		requireText(request.password(), "Password is required.");
		requireText(request.confirmPassword(), "Confirm password is required.");
		requireText(request.bankAccount(), "Bank account is required.");

		if (!request.password().equals(request.confirmPassword())) {
			throw new IllegalArgumentException("Password and confirm password must match.");
		}
	}

	private void requireText(String value, String message) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	private LocalDate parseDob(String dob) {
		try {
			return LocalDate.parse(dob.trim());
		} catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("DOB must be in yyyy-MM-dd format.");
		}
	}

	private String formatCustomerId(Long userId) {
		if (userId == null) {
			return "#C-00000";
		}
		return String.format("#C-%05d", userId);
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
