package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminBankOfficerService {

	private final UserService userService;

	public AdminBankOfficerService(UserService userService) {
		this.userService = userService;
	}

	public UserRegistrationStepResponse createDraft(BankCustomerStepOneRequest request) {
		return userService.saveBankOfficerStepOneDraft(request);
	}

	public UserRegistrationStepResponse create(BankCustomerStepOneRequest request) {
		return userService.continueBankOfficerStepOne(request);
	}

	public List<BankCustomerSummaryResponse> getAll() {
		return userService.getBankOfficers();
	}
}
