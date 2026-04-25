package com.bank_web_app.backend.user.service;

import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import java.util.List;

public interface UserService {

	UserRegistrationStepResponse saveBankCustomerStepOneDraft(UserRegistrationStepOneRequest request);

	UserRegistrationStepResponse continueBankCustomerStepOne(UserRegistrationStepOneRequest request);

	UserRegistrationStepResponse savePublicCustomerStepOneDraft(UserRegistrationStepOneRequest request);

	UserRegistrationStepResponse continuePublicCustomerStepOne(UserRegistrationStepOneRequest request);

	UserRegistrationStepResponse saveBankOfficerStepOneDraft(UserRegistrationStepOneRequest request);

	UserRegistrationStepResponse continueBankOfficerStepOne(UserRegistrationStepOneRequest request);

	List<BankCustomerSummaryResponse> getBankCustomersForOfficer();

	List<BankCustomerSummaryResponse> getPublicCustomers();

	List<BankCustomerSummaryResponse> getBankOfficers();
}
