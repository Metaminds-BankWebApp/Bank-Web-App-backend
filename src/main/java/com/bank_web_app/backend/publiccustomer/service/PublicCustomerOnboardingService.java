package com.bank_web_app.backend.publiccustomer.service;

import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublicCustomerOnboardingService {

	private final UserService userService;

	public PublicCustomerOnboardingService(UserService userService) {
		this.userService = userService;
	}

	public UserRegistrationStepResponse saveDraft(BankCustomerStepOneRequest request) {
		return userService.savePublicCustomerStepOneDraft(request);
	}

	public UserRegistrationStepResponse register(BankCustomerStepOneRequest request) {
		return userService.continuePublicCustomerStepOne(request);
	}

	public List<BankCustomerSummaryResponse> getAll() {
		return userService.getPublicCustomers();
	}
}
