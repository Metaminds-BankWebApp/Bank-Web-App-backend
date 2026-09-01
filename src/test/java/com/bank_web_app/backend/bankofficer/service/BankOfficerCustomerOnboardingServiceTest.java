package com.bank_web_app.backend.bankofficer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.dto.request.BankCustomerContactUpdateRequest;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerStepOnePrefillResponse;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.common.email.BankCustomerCredentialsEmailService;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import com.bank_web_app.backend.user.service.UserService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BankOfficerCustomerOnboardingServiceTest {

	@Mock private UserService userService;
	@Mock private BankOfficerContextService officerContextService;
	@Mock private AccountRepository accountRepository;
	@Mock private UserRepository userRepository;
	@Mock private BankCustomerRepository bankCustomerRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private BankCustomerCredentialsEmailService credentialsEmailService;
	@Mock private UserRegistrationStepOneRequest registrationRequest;

	private BankOfficerCustomerOnboardingService onboardingService;

	@BeforeEach
	void setUp() {
		onboardingService = new BankOfficerCustomerOnboardingService(
			userService,
			officerContextService,
			accountRepository,
			userRepository,
			bankCustomerRepository,
			passwordEncoder,
			credentialsEmailService
		);
	}

	@Test
	void delegatesSaveAndContinueToUserRegistrationService() {
		UserRegistrationStepResponse expected = new UserRegistrationStepResponse(
			12L,
			"BANK_CUSTOMER",
			"PENDING_STEP_2",
			"Continue to step two."
		);
		when(userService.continueBankCustomerStepOne(registrationRequest)).thenReturn(expected);

		UserRegistrationStepResponse actual = onboardingService.saveAndContinue(registrationRequest);

		assertThat(actual).isSameAs(expected);
		verify(userService).continueBankCustomerStepOne(registrationRequest);
	}

	@Test
	void normalizesNicAndReturnsExistingCustomerPrefill() {
		BankOfficer officer = officer(5L);
		BankCustomer customer = customer(30L, officer);
		when(officerContextService.resolveLoggedInBankOfficer()).thenReturn(officer);
		when(bankCustomerRepository.findByNormalizedUserNic("200012345678V")).thenReturn(Optional.of(customer));

		BankOfficerCustomerStepOnePrefillResponse response = onboardingService.getOwnedBankCustomerStepOneByNic(
			" 2000 12345678v "
		);

		assertThat(response.bankCustomerId()).isEqualTo(30L);
		assertThat(response.firstName()).isEqualTo("Alice");
		assertThat(response.nic()).isEqualTo("200012345678V");
		assertThat(response.accountNumber()).isEqualTo("1000000001");
		verify(bankCustomerRepository).findByNormalizedUserNic("200012345678V");
	}

	@Test
	void preventsOfficerFromUpdatingAnotherOfficersCompletedCustomer() {
		BankOfficer loggedInOfficer = officer(5L);
		BankCustomer customer = customer(30L, officer(6L));
		customer.setAccessStatus("COMPLETED");
		when(officerContextService.resolveLoggedInBankOfficer()).thenReturn(loggedInOfficer);
		when(bankCustomerRepository.findById(30L)).thenReturn(Optional.of(customer));

		assertThatThrownBy(() -> onboardingService.updateCompletedCustomerContactDetails(30L, contactRequest()))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception ->
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
			);
	}

	@Test
	void updatesOnlyContactDetailsForOwnedCompletedCustomer() {
		BankOfficer officer = officer(5L);
		BankCustomer customer = customer(30L, officer);
		customer.setAccessStatus("COMPLETED");
		when(officerContextService.resolveLoggedInBankOfficer()).thenReturn(officer);
		when(bankCustomerRepository.findById(30L)).thenReturn(Optional.of(customer));
		when(userRepository.existsByEmailIgnoreCaseAndUserIdNot("new@example.com", 12L)).thenReturn(false);

		UserRegistrationStepResponse response = onboardingService.updateCompletedCustomerContactDetails(
			30L,
			contactRequest()
		);

		assertThat(response.state()).isEqualTo("COMPLETED");
		assertThat(customer.getUser().getEmail()).isEqualTo("new@example.com");
		assertThat(customer.getUser().getPhone()).isEqualTo("+94771234567");
		assertThat(customer.getAccount().getAccountNumber()).isEqualTo("1000000001");
		verify(userRepository).save(customer.getUser());
	}

	private BankCustomerContactUpdateRequest contactRequest() {
		return new BankCustomerContactUpdateRequest(
			" New@Example.com ",
			"+94771234567",
			"Western",
			"123 Main Street, Colombo"
		);
	}

	private BankOfficer officer(Long id) {
		BankOfficer officer = new BankOfficer();
		officer.setOfficerId(id);
		return officer;
	}

	private BankCustomer customer(Long id, BankOfficer officer) {
		User user = new User();
		user.setUserId(12L);
		user.setFirstName("Alice");
		user.setLastName("Customer");
		user.setNic("200012345678V");
		user.setDob(LocalDate.of(2000, 3, 15));
		user.setEmail("alice@example.com");
		user.setPhone("+94770000000");
		user.setProvince("Western");
		user.setAddress("Old address");
		user.setUsername("alice.customer");

		Account account = new Account();
		account.setAccountId(9L);
		account.setAccountNumber("1000000001");
		account.setAccountType("SAVINGS");
		account.setStatus("ACTIVE");

		BankCustomer customer = new BankCustomer();
		customer.setBankCustomerId(id);
		customer.setCustomerCode("BC-00030");
		customer.setAccessStatus("PENDING_STEP_2");
		customer.setOfficer(officer);
		customer.setUser(user);
		customer.setAccount(account);
		return customer;
	}
}
