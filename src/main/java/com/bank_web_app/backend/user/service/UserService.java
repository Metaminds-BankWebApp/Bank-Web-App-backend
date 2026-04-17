package com.bank_web_app.backend.user.service;

import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import java.util.List;

public interface UserService {

	UserRegistrationStepResponse saveBankCustomerStepOneDraft(BankCustomerStepOneRequest request);

	UserRegistrationStepResponse continueBankCustomerStepOne(BankCustomerStepOneRequest request);

	UserRegistrationStepResponse savePublicCustomerStepOneDraft(BankCustomerStepOneRequest request);

	UserRegistrationStepResponse continuePublicCustomerStepOne(BankCustomerStepOneRequest request);

	UserRegistrationStepResponse saveBankOfficerStepOneDraft(BankCustomerStepOneRequest request);

	UserRegistrationStepResponse continueBankOfficerStepOne(BankCustomerStepOneRequest request);

	List<BankCustomerSummaryResponse> getBankCustomersForOfficer();
}
