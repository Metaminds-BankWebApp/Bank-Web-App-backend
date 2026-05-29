package com.bank_web_app.backend.publiccustomer.service;

import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerCardStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerIncomeStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLiabilityStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLoanStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialStepResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerCardProviderOptionResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerMeResponse;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublicCustomerService {

	// Delegated service for registration and list onboarding operations.
	private final PublicCustomerOnboardingService onboardingService;
	// Delegated service for financial-step and profile-related operations.
	private final PublicCustomerFinancialRecordService financialRecordService;

	// Injects onboarding and financial-record services.
	public PublicCustomerService(
		PublicCustomerOnboardingService onboardingService,
		PublicCustomerFinancialRecordService financialRecordService
	) {
		this.onboardingService = onboardingService;
		this.financialRecordService = financialRecordService;
	}

	// Saves public-customer registration as draft.
	public UserRegistrationStepResponse saveDraft(UserRegistrationStepOneRequest request) {
		return onboardingService.saveDraft(request);
	}

	// Completes public-customer registration.
	public UserRegistrationStepResponse register(UserRegistrationStepOneRequest request) {
		return onboardingService.register(request);
	}

	// Returns public-customer summary list.
	public List<BankCustomerSummaryResponse> getAll() {
		return onboardingService.getAll();
	}

	// Returns authenticated public-customer profile mapping.
	public PublicCustomerMeResponse getMe() {
		return financialRecordService.getLoggedInPublicCustomerProfile();
	}

	// Returns card-provider options for application card step.
	public List<PublicCustomerCardProviderOptionResponse> getCardProviderOptions() {
		return financialRecordService.getCardProviderOptions();
	}

	// Saves income step values.
	public PublicCustomerFinancialStepResponse saveIncomeStep(Long publicCustomerId, PublicCustomerIncomeStepRequest request) {
		return financialRecordService.saveIncomeStep(publicCustomerId, request);
	}

	// Saves loan step values.
	public PublicCustomerFinancialStepResponse saveLoanStep(Long publicCustomerId, PublicCustomerLoanStepRequest request) {
		return financialRecordService.saveLoanStep(publicCustomerId, request);
	}

	// Saves card step values.
	public PublicCustomerFinancialStepResponse saveCardStep(Long publicCustomerId, PublicCustomerCardStepRequest request) {
		return financialRecordService.saveCardStep(publicCustomerId, request);
	}

	// Saves liability and missed-payment step values.
	public PublicCustomerFinancialStepResponse saveLiabilityStep(Long publicCustomerId, PublicCustomerLiabilityStepRequest request) {
		return financialRecordService.saveLiabilityStep(publicCustomerId, request);
	}

	// Returns current financial snapshot.
	public PublicCustomerFinancialRecordResponse getCurrentFinancialRecord(Long publicCustomerId) {
		return financialRecordService.getCurrentFinancialRecord(publicCustomerId);
	}

	// Returns financial snapshot history.
	public List<PublicCustomerFinancialRecordSummaryResponse> getFinancialRecordHistory(Long publicCustomerId) {
		return financialRecordService.getFinancialRecordHistory(publicCustomerId);
	}

	// Returns a specific financial snapshot by record id.
	public PublicCustomerFinancialRecordResponse getFinancialRecordById(Long publicCustomerId, Long recordId) {
		return financialRecordService.getFinancialRecordById(publicCustomerId, recordId);
	}
}
