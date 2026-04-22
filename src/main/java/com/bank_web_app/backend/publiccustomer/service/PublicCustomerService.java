package com.bank_web_app.backend.publiccustomer.service;

import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerCardStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerIncomeStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLiabilityStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLoanStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialStepResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerMeResponse;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublicCustomerService {

	private final PublicCustomerOnboardingService onboardingService;
	private final PublicCustomerFinancialRecordService financialRecordService;

	public PublicCustomerService(
		PublicCustomerOnboardingService onboardingService,
		PublicCustomerFinancialRecordService financialRecordService
	) {
		this.onboardingService = onboardingService;
		this.financialRecordService = financialRecordService;
	}

	public UserRegistrationStepResponse saveDraft(UserRegistrationStepOneRequest request) {
		return onboardingService.saveDraft(request);
	}

	public UserRegistrationStepResponse register(UserRegistrationStepOneRequest request) {
		return onboardingService.register(request);
	}

	public List<BankCustomerSummaryResponse> getAll() {
		return onboardingService.getAll();
	}

	public PublicCustomerMeResponse getMe() {
		return financialRecordService.getLoggedInPublicCustomerProfile();
	}

	public PublicCustomerFinancialStepResponse saveIncomeStep(Long publicCustomerId, PublicCustomerIncomeStepRequest request) {
		return financialRecordService.saveIncomeStep(publicCustomerId, request);
	}

	public PublicCustomerFinancialStepResponse saveLoanStep(Long publicCustomerId, PublicCustomerLoanStepRequest request) {
		return financialRecordService.saveLoanStep(publicCustomerId, request);
	}

	public PublicCustomerFinancialStepResponse saveCardStep(Long publicCustomerId, PublicCustomerCardStepRequest request) {
		return financialRecordService.saveCardStep(publicCustomerId, request);
	}

	public PublicCustomerFinancialStepResponse saveLiabilityStep(Long publicCustomerId, PublicCustomerLiabilityStepRequest request) {
		return financialRecordService.saveLiabilityStep(publicCustomerId, request);
	}

	public PublicCustomerFinancialRecordResponse getCurrentFinancialRecord(Long publicCustomerId) {
		return financialRecordService.getCurrentFinancialRecord(publicCustomerId);
	}

	public List<PublicCustomerFinancialRecordSummaryResponse> getFinancialRecordHistory(Long publicCustomerId) {
		return financialRecordService.getFinancialRecordHistory(publicCustomerId);
	}

	public PublicCustomerFinancialRecordResponse getFinancialRecordById(Long publicCustomerId, Long recordId) {
		return financialRecordService.getFinancialRecordById(publicCustomerId, recordId);
	}
}
