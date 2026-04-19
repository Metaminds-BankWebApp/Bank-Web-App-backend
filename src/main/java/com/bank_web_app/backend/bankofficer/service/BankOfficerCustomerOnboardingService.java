package com.bank_web_app.backend.bankofficer.service;

import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankofficer.dto.response.AccountVerificationResponse;
import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BankOfficerCustomerOnboardingService {

	private final UserService userService;
	private final AccountRepository accountRepository;

	public BankOfficerCustomerOnboardingService(UserService userService, AccountRepository accountRepository) {
		this.userService = userService;
		this.accountRepository = accountRepository;
	}

	public UserRegistrationStepResponse saveDraft(BankCustomerStepOneRequest request) {
		return userService.saveBankCustomerStepOneDraft(request);
	}

	public UserRegistrationStepResponse saveAndContinue(BankCustomerStepOneRequest request) {
		return userService.continueBankCustomerStepOne(request);
	}

	public List<BankCustomerSummaryResponse> getAll() {
		return userService.getBankCustomersForOfficer();
	}

	public AccountVerificationResponse verifyAccount(String accountNumber) {
		if (accountNumber == null || accountNumber.trim().isEmpty()) {
			return new AccountVerificationResponse(false, null, "NOT_FOUND", null, "Account number is required.");
		}

		String normalized = accountNumber.trim();
		Account account = accountRepository.findByAccountNumber(normalized).orElse(null);
		if (account == null) {
			return new AccountVerificationResponse(false, null, "NOT_FOUND", null, "Account not found.");
		}

		return new AccountVerificationResponse(
			true,
			account.getAccountId(),
			account.getStatus(),
			account.getAccountType(),
			"Account found."
		);
	}
}
