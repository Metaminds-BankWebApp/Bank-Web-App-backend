package com.bank_web_app.backend.bankofficer.service;

import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCardStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCribRequestStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCribRetrievalStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerIncomeStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLiabilityStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLoanStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerCribStepResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialStepResponse;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.service.BankCustomerFinancialRecordService;
import com.bank_web_app.backend.bankofficer.dto.response.AccountVerificationResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerIdentityResponse;
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
	private final BankCustomerFinancialRecordService financialRecordService;

	public BankOfficerCustomerOnboardingService(
		UserService userService,
		AccountRepository accountRepository,
		BankCustomerFinancialRecordService financialRecordService
	) {
		this.userService = userService;
		this.accountRepository = accountRepository;
		this.financialRecordService = financialRecordService;
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

	public BankOfficerCustomerIdentityResponse getOwnedBankCustomerIdentityByUserId(Long userId) {
		return financialRecordService.getOwnedBankCustomerIdentityByUserId(userId);
	}

	public BankCustomerFinancialStepResponse saveIncomeStepDraft(Long bankCustomerId, BankCustomerIncomeStepRequest request) {
		return financialRecordService.saveIncomeStepDraft(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveIncomeStepAndContinue(Long bankCustomerId, BankCustomerIncomeStepRequest request) {
		return financialRecordService.saveIncomeStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveLoanStepDraft(Long bankCustomerId, BankCustomerLoanStepRequest request) {
		return financialRecordService.saveLoanStepDraft(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveLoanStepAndContinue(Long bankCustomerId, BankCustomerLoanStepRequest request) {
		return financialRecordService.saveLoanStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveCardStepDraft(Long bankCustomerId, BankCustomerCardStepRequest request) {
		return financialRecordService.saveCardStepDraft(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveCardStepAndContinue(Long bankCustomerId, BankCustomerCardStepRequest request) {
		return financialRecordService.saveCardStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveLiabilityStepDraft(Long bankCustomerId, BankCustomerLiabilityStepRequest request) {
		return financialRecordService.saveLiabilityStepDraft(bankCustomerId, request);
	}

	public BankCustomerFinancialStepResponse saveLiabilityStepAndContinue(Long bankCustomerId, BankCustomerLiabilityStepRequest request) {
		return financialRecordService.saveLiabilityStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerCribStepResponse saveCribLinkingStepAndContinue(Long bankCustomerId, BankCustomerCribRequestStepRequest request) {
		return financialRecordService.saveCribLinkingStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerCribStepResponse saveCribRequestStepAndContinue(Long bankCustomerId, BankCustomerCribRequestStepRequest request) {
		return financialRecordService.saveCribRequestStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerCribStepResponse saveCribRetrievalStepAndContinue(Long bankCustomerId, BankCustomerCribRetrievalStepRequest request) {
		return financialRecordService.saveCribRetrievalStepAndContinue(bankCustomerId, request);
	}

	public BankCustomerCribStepResponse completeCribReviewAndOnboarding(Long bankCustomerId) {
		return financialRecordService.completeCribReviewAndOnboarding(bankCustomerId);
	}

	public BankCustomerFinancialRecordResponse getCurrentFinancialRecord(Long bankCustomerId) {
		return financialRecordService.getCurrentFinancialRecord(bankCustomerId);
	}

	public List<BankCustomerFinancialRecordSummaryResponse> getFinancialRecordHistory(Long bankCustomerId) {
		return financialRecordService.getFinancialRecordHistory(bankCustomerId);
	}

	public BankCustomerFinancialRecordResponse getFinancialRecordById(Long bankCustomerId, Long bankRecordId) {
		return financialRecordService.getFinancialRecordById(bankCustomerId, bankRecordId);
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
