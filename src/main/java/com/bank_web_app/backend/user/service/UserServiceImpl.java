package com.bank_web_app.backend.user.service;

import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.common.exception.DuplicateFieldsException;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.entity.Role;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.RoleRepository;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserServiceImpl implements UserService {

private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
private static final String ROLE_PUBLIC_CUSTOMER = "PUBLIC_CUSTOMER";
private static final String ROLE_BANK_OFFICER = "BANK_OFFICER";
private static final String STATUS_ACTIVE = "ACTIVE";
private static final String STATE_DRAFT = "DRAFT";
private static final String STATE_PENDING_STEP_2 = "PENDING_STEP_2";
private static final String STATE_SUCCESS = "SUCCESS";

private final UserRepository userRepository;
private final RoleRepository roleRepository;
private final BranchRepository branchRepository;
private final BankOfficerRepository bankOfficerRepository;
private final BankCustomerRepository bankCustomerRepository;
private final AccountRepository accountRepository;
private final PublicCustomerProfileRepository publicCustomerProfileRepository;
private final PasswordEncoder passwordEncoder;

public UserServiceImpl(
UserRepository userRepository,
RoleRepository roleRepository,
BranchRepository branchRepository,
BankOfficerRepository bankOfficerRepository,
BankCustomerRepository bankCustomerRepository,
AccountRepository accountRepository,
PublicCustomerProfileRepository publicCustomerProfileRepository,
PasswordEncoder passwordEncoder
) {
this.userRepository = userRepository;
this.roleRepository = roleRepository;
this.branchRepository = branchRepository;
this.bankOfficerRepository = bankOfficerRepository;
this.bankCustomerRepository = bankCustomerRepository;
this.accountRepository = accountRepository;
this.publicCustomerProfileRepository = publicCustomerProfileRepository;
this.passwordEncoder = passwordEncoder;
}

@Override
@Transactional
public UserRegistrationStepResponse saveBankCustomerStepOneDraft(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_BANK_CUSTOMER);
createBankCustomerProfile(request, user, STATE_DRAFT);
return new UserRegistrationStepResponse(user.getUserId(), ROLE_BANK_CUSTOMER, STATE_DRAFT, "Bank customer draft saved successfully.");
}

@Override
@Transactional
public UserRegistrationStepResponse continueBankCustomerStepOne(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_BANK_CUSTOMER);
createBankCustomerProfile(request, user, STATE_PENDING_STEP_2);
return new UserRegistrationStepResponse(
user.getUserId(),
ROLE_BANK_CUSTOMER,
STATE_PENDING_STEP_2,
"Bank customer step one saved. Continue to step two."
);
}

@Override
@Transactional
public UserRegistrationStepResponse savePublicCustomerStepOneDraft(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_PUBLIC_CUSTOMER);
createPublicCustomerProfile(request, user);
return new UserRegistrationStepResponse(user.getUserId(), ROLE_PUBLIC_CUSTOMER, STATE_DRAFT, "Public customer draft saved successfully.");
}

@Override
@Transactional
public UserRegistrationStepResponse continuePublicCustomerStepOne(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_PUBLIC_CUSTOMER);
createPublicCustomerProfile(request, user);
return new UserRegistrationStepResponse(
user.getUserId(),
ROLE_PUBLIC_CUSTOMER,
STATE_SUCCESS,
"Public customer registration completed successfully."
);
}

@Override
@Transactional
public UserRegistrationStepResponse saveBankOfficerStepOneDraft(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_BANK_OFFICER);
createBankOfficerProfile(request, user);
return new UserRegistrationStepResponse(user.getUserId(), ROLE_BANK_OFFICER, STATE_DRAFT, "Bank officer draft saved successfully.");
}

@Override
@Transactional
public UserRegistrationStepResponse continueBankOfficerStepOne(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_BANK_OFFICER);
createBankOfficerProfile(request, user);
return new UserRegistrationStepResponse(user.getUserId(), ROLE_BANK_OFFICER, STATE_SUCCESS, "Bank officer registration completed successfully.");
}

@Override
@Transactional(readOnly = true)
public List<BankCustomerSummaryResponse> getBankCustomersForOfficer() {
	BankOfficer officer = resolveLoggedInBankOfficer();
	return bankCustomerRepository
		.findAllByOfficer_OfficerIdOrderByUpdatedAtDesc(officer.getOfficerId())
		.stream()
		.map(customer -> toSummary(customer.getUser(), customer.getCustomerCode()))
		.toList();
}

@Override
@Transactional(readOnly = true)
public List<BankCustomerSummaryResponse> getPublicCustomers() {
return userRepository
.findAllByRole_RoleNameOrderByUpdatedAtDesc(ROLE_PUBLIC_CUSTOMER)
.stream()
.map(user -> {
String customerCode = publicCustomerProfileRepository
.findByUser_UserId(user.getUserId())
.map(PublicCustomerProfile::getCustomerCode)
.orElse(formatCode("PC", user.getUserId()));
return toSummary(user, customerCode);
})
.toList();
}

@Override
@Transactional(readOnly = true)
public List<BankCustomerSummaryResponse> getBankOfficers() {
return userRepository
.findAllByRole_RoleNameOrderByUpdatedAtDesc(ROLE_BANK_OFFICER)
.stream()
.map(user -> {
String employeeCode = bankOfficerRepository
.findByUser_UserId(user.getUserId())
.map(BankOfficer::getEmployeeCode)
.orElse(formatCode("BO", user.getUserId()));
return toSummary(user, employeeCode);
})
.toList();
}

private User createUserForRole(UserRegistrationStepOneRequest request, String roleName) {
validateBaseRequest(request);
Role role = roleRepository
.findByRoleName(roleName)
.orElseThrow(() -> new IllegalStateException("Role " + roleName + " not found."));
String username = request.username().trim();
String email = request.email().trim().toLowerCase(Locale.ROOT);
String nic = request.nic().trim();
validateUniqueness(username, email, nic);

User user = new User();
user.setRole(role);
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
user.setStatus(STATUS_ACTIVE);
return userRepository.save(user);
}

private void createPublicCustomerProfile(UserRegistrationStepOneRequest request, User user) {
String customerCode = resolveCustomerCode(
request.customerCode(),
"PC",
user.getUserId(),
publicCustomerProfileRepository::existsByCustomerCode
);
PublicCustomerProfile profile = new PublicCustomerProfile();
profile.setUser(user);
profile.setCustomerCode(customerCode);
publicCustomerProfileRepository.save(profile);
}

private void createBankOfficerProfile(UserRegistrationStepOneRequest request, User user) {
Long branchId = request.branchId();
if (branchId == null) {
throw new IllegalArgumentException("Branch id is required for bank officer registration.");
}
String employeeCode = generateBankOfficerEmployeeCode();

Branch branch = branchRepository
.findById(branchId)
.orElseThrow(() -> new IllegalArgumentException("Branch not found."));
BankOfficer officer = new BankOfficer();
officer.setUser(user);
officer.setBranch(branch);
officer.setEmployeeCode(employeeCode);
officer.setCreatedByAdminUser(resolveOptionalAdmin(request.createdByAdminUserId()));
bankOfficerRepository.save(officer);
}

private String generateBankOfficerEmployeeCode() {
	long nextValue = bankOfficerRepository.count() + 1L;
	String candidate = String.format("EMP-BO-%05d", nextValue);

	while (bankOfficerRepository.existsByEmployeeCode(candidate)) {
		nextValue++;
		candidate = String.format("EMP-BO-%05d", nextValue);
	}

	return candidate;
}

private void createBankCustomerProfile(UserRegistrationStepOneRequest request, User user, String accessStatus) {
	BankOfficer loggedOfficer = resolveLoggedInBankOfficer();
	if (request.officerId() != null && !loggedOfficer.getOfficerId().equals(request.officerId())) {
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Step-1 officer id does not match the logged-in bank officer.");
	}
	if (request.branchId() != null && !loggedOfficer.getBranch().getBranchId().equals(request.branchId())) {
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Step-1 branch id does not match the logged-in bank officer branch.");
	}

String accountNumber = resolveAccountNumber(request);
	Account savedAccount = accountRepository
		.findByAccountNumber(accountNumber)
		.orElseThrow(() -> new IllegalArgumentException("Account not found."));
	if (bankCustomerRepository.existsByAccount_AccountId(savedAccount.getAccountId())) {
		throw new IllegalArgumentException("Bank account is already linked to another customer.");
	}

String customerCode = resolveCustomerCode(
request.customerCode(),
"BC",
user.getUserId(),
bankCustomerRepository::existsByCustomerCode
);
BankCustomer customer = new BankCustomer();
customer.setUser(user);
customer.setCustomerCode(customerCode);
	customer.setOfficer(loggedOfficer);
	customer.setBranch(loggedOfficer.getBranch());
customer.setAccount(savedAccount);
customer.setAccessStatus(accessStatus);
bankCustomerRepository.save(customer);
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

private String resolveAccountNumber(UserRegistrationStepOneRequest request) {
String fromRequest = safeTrim(request.accountNumber());
if (!fromRequest.isBlank()) {
return fromRequest;
}
if (request.bankAccount() != null && request.bankAccount() > 0) {
return String.valueOf(request.bankAccount());
}
throw new IllegalArgumentException("Account number is required for bank customer registration.");
}

private String resolveAccountType(String accountType) {
String normalized = safeTrim(accountType).toUpperCase(Locale.ROOT);
return normalized.isBlank() ? "SAVINGS" : normalized;
}

private User resolveOptionalAdmin(Long adminUserId) {
if (adminUserId == null) {
return null;
}
return userRepository
.findById(adminUserId)
.orElseThrow(() -> new IllegalArgumentException("Created-by admin user was not found."));
}

private String resolveCustomerCode(String override, String prefix, Long userId, Predicate<String> existsPredicate) {
String explicit = safeTrim(override);
if (!explicit.isBlank()) {
if (existsPredicate.test(explicit)) {
throw new IllegalArgumentException("Customer code is already in use.");
}
return explicit;
}

String generated = formatCode(prefix, userId);
if (!existsPredicate.test(generated)) {
return generated;
}
int suffix = 1;
String candidate = generated + "-" + suffix;
while (existsPredicate.test(candidate)) {
suffix++;
candidate = generated + "-" + suffix;
}
return candidate;
}

private void validateBaseRequest(UserRegistrationStepOneRequest request) {
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
if (!request.password().equals(request.confirmPassword())) {
throw new IllegalArgumentException("Password and confirm password must match.");
}
}

private void validateUniqueness(String username, String email, String nic) {
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
}

private LocalDate parseDob(String dob) {
try {
return LocalDate.parse(dob.trim());
} catch (DateTimeParseException ex) {
throw new IllegalArgumentException("DOB must be in yyyy-MM-dd format.");
}
}

private BankCustomerSummaryResponse toSummary(User user, String customerId) {
return new BankCustomerSummaryResponse(
user.getUserId(),
customerId,
(safe(user.getFirstName()) + " " + safe(user.getLastName())).trim(),
safe(user.getNic()),
safe(user.getEmail()),
safe(user.getPhone()),
safe(user.getStatus()),
user.getUpdatedAt() == null ? null : user.getUpdatedAt().toString()
);
}

private String formatCode(String prefix, Long value) {
if (value == null) {
return prefix + "-00000";
}
return String.format("%s-%05d", prefix, value);
}

private String safe(String value) {
return value == null ? "" : value.trim();
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
