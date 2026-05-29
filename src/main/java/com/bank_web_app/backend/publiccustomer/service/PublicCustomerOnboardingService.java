package com.bank_web_app.backend.publiccustomer.service;

import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublicCustomerOnboardingService {

	// Reuses user-service onboarding operations for public customers.
	private final UserService userService;

	// Injects user service dependency for onboarding flows.
	public PublicCustomerOnboardingService(UserService userService) {
		this.userService = userService;
	}

	// Persists step-one registration as DRAFT.
	public UserRegistrationStepResponse saveDraft(UserRegistrationStepOneRequest request) {
		return userService.savePublicCustomerStepOneDraft(request);
	}

	// Completes public-customer registration step-one flow.
	public UserRegistrationStepResponse register(UserRegistrationStepOneRequest request) {
		return userService.continuePublicCustomerStepOne(request);
	}

	// Returns public-customer summary list for management screens.
	public List<BankCustomerSummaryResponse> getAll() {
		return userService.getPublicCustomers();
	}
}
