package com.bank_web_app.backend.bankofficer.service;

import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerOnboardingService {

	private final UserService userService;

	public CustomerOnboardingService(UserService userService) {
		this.userService = userService;
	}

	public UserRegistrationStepResponse createOfficerDraft(BankCustomerStepOneRequest request) {
		return userService.saveBankOfficerStepOneDraft(request);
	}

	public UserRegistrationStepResponse createOfficer(BankCustomerStepOneRequest request) {
		return userService.continueBankOfficerStepOne(request);
	}

	public List<BankCustomerSummaryResponse> getBankOfficers() {
		return userService.getBankOfficers();
	}
}
