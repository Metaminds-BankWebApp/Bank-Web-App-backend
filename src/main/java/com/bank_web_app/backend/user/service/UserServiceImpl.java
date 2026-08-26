package com.bank_web_app.backend.user.service;

import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.entity.BranchStatus;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.bankofficer.service.BankOfficerContextService;
import com.bank_web_app.backend.bankofficer.service.PortfolioService;
import com.bank_web_app.backend.common.email.BankCustomerCredentialsEmailService;
import com.bank_web_app.backend.common.email.BankOfficerCredentialsEmailService;
import com.bank_web_app.backend.common.exception.DuplicateFieldsException;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.notification.event.NotificationEventType;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.GeneratedBankCustomerCredentialsResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.entity.Role;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.RoleRepository;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
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
private static final Pattern NIC_REGEX = Pattern.compile("^(?:\\d{9}[Vv]|\\d{12})$");
private static final Pattern BANK_OFFICER_MOBILE_REGEX = Pattern.compile("^(?:077|076|078|070|072|074|075|071)\\d{7}$");
private static final Pattern BANK_OFFICER_EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);
private static final Pattern NAME_STARTS_WITH_LETTER_REGEX = Pattern.compile("^\\p{L}.*$");
private static final Set<String> SRI_LANKA_PROVINCES = Set.of(
"western",
"central",
"southern",
"northern",
"eastern",
"north western",
"north central",
"uva",
"sabaragamuwa"
);

private final UserRepository userRepository;
private final RoleRepository roleRepository;
private final BranchRepository branchRepository;
private final BankOfficerRepository bankOfficerRepository;
private final BankCustomerRepository bankCustomerRepository;
private final AccountRepository accountRepository;
private final PublicCustomerProfileRepository publicCustomerProfileRepository;
private final BankOfficerContextService bankOfficerContextService;
private final PortfolioService portfolioService;
private final PasswordEncoder passwordEncoder;
private final BankCustomerCredentialsEmailService bankCustomerCredentialsEmailService;
private final BankOfficerCredentialsEmailService bankOfficerCredentialsEmailService;
private final NotificationEventPublisher notificationEventPublisher;
private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

public UserServiceImpl(
UserRepository userRepository,
RoleRepository roleRepository,
BranchRepository branchRepository,
BankOfficerRepository bankOfficerRepository,
BankCustomerRepository bankCustomerRepository,
AccountRepository accountRepository,
PublicCustomerProfileRepository publicCustomerProfileRepository,
BankOfficerContextService bankOfficerContextService,
PortfolioService portfolioService,
PasswordEncoder passwordEncoder,
BankCustomerCredentialsEmailService bankCustomerCredentialsEmailService,
BankOfficerCredentialsEmailService bankOfficerCredentialsEmailService,
NotificationEventPublisher notificationEventPublisher
) {
this.userRepository = userRepository;
this.roleRepository = roleRepository;
this.branchRepository = branchRepository;
this.bankOfficerRepository = bankOfficerRepository;
this.bankCustomerRepository = bankCustomerRepository;
this.accountRepository = accountRepository;
this.publicCustomerProfileRepository = publicCustomerProfileRepository;
this.bankOfficerContextService = bankOfficerContextService;
this.portfolioService = portfolioService;
this.passwordEncoder = passwordEncoder;
this.bankCustomerCredentialsEmailService = bankCustomerCredentialsEmailService;
this.bankOfficerCredentialsEmailService = bankOfficerCredentialsEmailService;
this.notificationEventPublisher = notificationEventPublisher;
}

@Override
@Transactional
public UserRegistrationStepResponse saveBankCustomerStepOneDraft(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_BANK_CUSTOMER);
BankCustomer customer = createBankCustomerProfile(request, user, STATE_DRAFT);
publishBankCustomerCreated(user, customer);
return new UserRegistrationStepResponse(user.getUserId(), ROLE_BANK_CUSTOMER, STATE_DRAFT, "Bank customer draft saved successfully.");
}

@Override
@Transactional
public UserRegistrationStepResponse continueBankCustomerStepOne(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_BANK_CUSTOMER);
BankCustomer customer = createBankCustomerProfile(request, user, STATE_PENDING_STEP_2);
publishBankCustomerCreated(user, customer);

return new UserRegistrationStepResponse(
user.getUserId(),
ROLE_BANK_CUSTOMER,
STATE_PENDING_STEP_2,
"Bank customer step one saved and credentials email was sent. Continue to step two."
);
}

@Override
@Transactional(readOnly = true)
public GeneratedBankCustomerCredentialsResponse generateBankCustomerCredentials(String firstName, String lastName) {
	String username = generateUniqueBankCustomerUsername(firstName, lastName);
	String password = generateTemporaryPassword(12);
	return new GeneratedBankCustomerCredentialsResponse(username, password);
}

@Override
@Transactional
public UserRegistrationStepResponse savePublicCustomerStepOneDraft(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_PUBLIC_CUSTOMER);
createPublicCustomerProfile(request, user);
publishCustomerRegistration(user, ROLE_PUBLIC_CUSTOMER, null);
return new UserRegistrationStepResponse(user.getUserId(), ROLE_PUBLIC_CUSTOMER, STATE_DRAFT, "Public customer draft saved successfully.");
}

@Override
@Transactional
public UserRegistrationStepResponse continuePublicCustomerStepOne(UserRegistrationStepOneRequest request) {
User user = createUserForRole(request, ROLE_PUBLIC_CUSTOMER);
createPublicCustomerProfile(request, user);
publishCustomerRegistration(user, ROLE_PUBLIC_CUSTOMER, null);
publishPublicFinancialDetailsRequired(user);
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
	List<BankOfficerCustomerSummaryResponse> rows = portfolioService.getBankCustomersForOfficer();
	return rows.stream()
		.map(r -> new BankCustomerSummaryResponse(r.userId(), r.customerId(), r.fullName(), r.nic(), r.email(), r.phone(), r.status(), r.lastUpdated()))
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
validateBaseRequest(request, roleName);
if (ROLE_BANK_OFFICER.equals(roleName)) {
validateBankOfficerConstraints(request);
}
Role role = roleRepository
.findByRoleName(roleName)
.orElseThrow(() -> new IllegalStateException("Role " + roleName + " not found."));
String requestedUsername = safeTrim(request.username());
String username = ROLE_BANK_CUSTOMER.equals(roleName) && requestedUsername.isBlank()
	? generateUniqueBankCustomerUsername(request.firstName(), request.lastName())
	: requestedUsername;
String email = request.email().trim().toLowerCase(Locale.ROOT);
String nic = request.nic().trim();
String phone = request.mobile().trim();
validateUniqueness(username, email, nic, phone, roleName);

User user = new User();
user.setRole(role);
user.setUsername(username);
user.setEmail(email);
String bootstrapPassword = ROLE_BANK_CUSTOMER.equals(roleName) ? generateTemporaryPassword(32) : request.password();
user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
user.setFirstName(request.firstName().trim());
user.setLastName(request.lastName().trim());
user.setPhone(phone);
user.setNic(nic);
user.setDob(parseDob(request.dob()));
user.setProvince(request.province().trim());
user.setAddress(safeTrim(request.address()));
user.setStatus(ROLE_BANK_CUSTOMER.equals(roleName) ? "PENDING_ACTIVATION" : STATUS_ACTIVE);
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
if (branch.getStatus() != BranchStatus.ACTIVE && branch.getStatus() != BranchStatus.MAINTENANCE) {
throw new IllegalArgumentException("Only active or maintenance branches can be assigned to a bank officer.");
}
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

private String generateUniqueBankCustomerUsername(String firstName, String lastName) {
	String normalized = normalizeForUsername(firstName) + normalizeForUsername(lastName);
	if (normalized.isBlank()) {
		normalized = "bankcustomer";
	}

	for (int attempt = 0; attempt < 30; attempt++) {
		String candidate = normalized + generateThreeDigitSuffix();
		if (candidate.length() > 20) {
			candidate = candidate.substring(0, 20);
		}
		if (!userRepository.existsByUsername(candidate)) {
			return candidate;
		}
	}

	String fallback = normalized;
	if (fallback.length() > 17) {
		fallback = fallback.substring(0, 17);
	}
	int suffix = 100;
	String candidate = fallback + suffix;
	while (userRepository.existsByUsername(candidate)) {
		suffix++;
		candidate = fallback + suffix;
		if (candidate.length() > 20) {
			candidate = candidate.substring(0, 20);
		}
	}
	return candidate;
}

private String normalizeForUsername(String value) {
	if (value == null) {
		return "";
	}
	String cleaned = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	return cleaned;
}

private String generateThreeDigitSuffix() {
	int value = 100 + SECURE_RANDOM.nextInt(900);
	return String.valueOf(value);
}

private String generateTemporaryPassword(int length) {
	String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
	String lower = "abcdefghijkmnopqrstuvwxyz";
	String digits = "23456789";
	String symbols = "!@#$%&*?";
	String charset = upper + lower + digits + symbols;
	StringBuilder builder = new StringBuilder();
	builder.append(randomChar(upper));
	builder.append(randomChar(lower));
	builder.append(randomChar(digits));
	builder.append(randomChar(symbols));
	while (builder.length() < length) {
		builder.append(randomChar(charset));
	}
	char[] chars = builder.toString().toCharArray();
	for (int i = chars.length - 1; i > 0; i--) {
		int swapIndex = SECURE_RANDOM.nextInt(i + 1);
		char temp = chars[i];
		chars[i] = chars[swapIndex];
		chars[swapIndex] = temp;
	}
	return new String(chars);
}

private char randomChar(String source) {
	return source.charAt(SECURE_RANDOM.nextInt(source.length()));
}

private BankCustomer createBankCustomerProfile(UserRegistrationStepOneRequest request, User user, String accessStatus) {
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
return bankCustomerRepository.save(customer);
}

private void publishBankCustomerCreated(User user, BankCustomer customer) {
	Long officerUserId = customer.getOfficer().getUser().getUserId();
	publishCustomerRegistration(user, ROLE_BANK_CUSTOMER, officerUserId);
	notificationEventPublisher.publish(
		NotificationEventType.BANK_CUSTOMER_ASSIGNED,
		officerUserId,
		officerUserId,
		customer.getBankCustomerId(),
		Map.of(
			"customerId", String.valueOf(customer.getBankCustomerId()),
			"customerName", buildFullName(user)
		)
	);
}

private void publishCustomerRegistration(User user, String roleName, Long actorUserId) {
	notificationEventPublisher.publish(
		NotificationEventType.ADMIN_NEW_CUSTOMER,
		null,
		actorUserId,
		user.getUserId(),
		Map.of("role", roleName)
	);
}

private void publishPublicFinancialDetailsRequired(User user) {
	notificationEventPublisher.publish(
		NotificationEventType.PUBLIC_FINANCIAL_DETAILS_REQUIRED,
		user.getUserId(),
		user.getUserId(),
		user.getUserId(),
		Map.of()
	);
}

private String buildFullName(User user) {
	String fullName = (safeTrim(user.getFirstName()) + " " + safeTrim(user.getLastName())).trim();
	return fullName.isBlank() ? user.getUsername() : fullName;
}

private BankOfficer resolveLoggedInBankOfficer() {
	return bankOfficerContextService.resolveLoggedInBankOfficer();
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

private void validateBaseRequest(UserRegistrationStepOneRequest request, String roleName) {
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
if (!ROLE_BANK_OFFICER.equals(roleName)) {
requireText(request.address(), "Address is required.");
}
if (!ROLE_BANK_CUSTOMER.equals(roleName)) {
	requireText(request.username(), "Username is required.");
	requireText(request.password(), "Password is required.");
	requireText(request.confirmPassword(), "Confirm password is required.");
	if (!request.password().equals(request.confirmPassword())) {
		throw new IllegalArgumentException("Password and confirm password must match.");
	}
}
}

private void validateBankOfficerConstraints(UserRegistrationStepOneRequest request) {
String firstName = safeTrim(request.firstName());
if (!NAME_STARTS_WITH_LETTER_REGEX.matcher(firstName).matches()) {
throw new IllegalArgumentException("First name must start with a letter.");
}

String lastName = safeTrim(request.lastName());
if (!NAME_STARTS_WITH_LETTER_REGEX.matcher(lastName).matches()) {
throw new IllegalArgumentException("Last name must start with a letter.");
}

String nic = safeTrim(request.nic());
if (!NIC_REGEX.matcher(nic).matches()) {
throw new IllegalArgumentException("Enter a valid NIC number.");
}

String mobile = safeTrim(request.mobile());
if (!BANK_OFFICER_MOBILE_REGEX.matcher(mobile).matches()) {
throw new IllegalArgumentException("Contact number must be 10 digits and start with 070, 071, 072, 074, 075, 076, 077, or 078.");
}

String email = safeTrim(request.email());
if (!BANK_OFFICER_EMAIL_REGEX.matcher(email).matches()) {
throw new IllegalArgumentException("Email must be in the format name@gmail.com.");
}

String province = safeTrim(request.province()).toLowerCase(Locale.ROOT);
if (!SRI_LANKA_PROVINCES.contains(province)) {
throw new IllegalArgumentException("Please select a valid Sri Lankan province.");
}

LocalDate dob = parseDob(request.dob());
if (dob.isAfter(LocalDate.now().minusYears(18))) {
throw new IllegalArgumentException("Bank officer must be at least 18 years old.");
}
}

private void validateUniqueness(String username, String email, String nic, String phone, String roleName) {
LinkedHashMap<String, String> duplicateFieldErrors = new LinkedHashMap<>();
if (userRepository.existsByUsername(username)) {
duplicateFieldErrors.put("username", "Username is already in use.");
}
if (userRepository.existsByEmailIgnoreCase(email)) {
duplicateFieldErrors.put("email", "Email is already in use.");
}
if (branchRepository.existsByBranchPhone(phone)) {
	duplicateFieldErrors.put("mobile", "Contact number is already in use.");
}
if (ROLE_BANK_OFFICER.equals(roleName) && userRepository.existsByPhoneAndRole_RoleNameIn(phone, List.of(ROLE_BANK_OFFICER))) {
	duplicateFieldErrors.put("mobile", "Contact number is already in use.");
}
if ((ROLE_BANK_CUSTOMER.equals(roleName) || ROLE_PUBLIC_CUSTOMER.equals(roleName)) &&
	userRepository.existsByPhoneAndRole_RoleNameIn(phone, List.of(ROLE_BANK_CUSTOMER, ROLE_PUBLIC_CUSTOMER))) {
	duplicateFieldErrors.put("mobile", "Contact number is already in use.");
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
