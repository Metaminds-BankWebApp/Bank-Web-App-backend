package com.bank_web_app.backend.bankofficer.service;

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
import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankcustomer.service.BankCustomerFinancialRecordService;
import com.bank_web_app.backend.bankofficer.dto.response.AccountVerificationResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerIdentityResponse;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BankOfficerFinancialService {

    private final BankCustomerFinancialRecordService financialRecordService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final BankOfficerRepository bankOfficerRepository;

    public BankOfficerFinancialService(
        BankCustomerFinancialRecordService financialRecordService,
        AccountRepository accountRepository,
        UserRepository userRepository,
        BankOfficerRepository bankOfficerRepository
    ) {
        this.financialRecordService = financialRecordService;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.bankOfficerRepository = bankOfficerRepository;
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

    public BankCustomerFinancialStepResponse completeFinancialMaintenance(Long bankCustomerId) {
        return financialRecordService.completeFinancialMaintenance(bankCustomerId);
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
