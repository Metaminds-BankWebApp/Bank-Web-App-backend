package com.bank_web_app.backend.bankofficer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BankOfficerCustomerStepOnePrefillResponse", description = "Step-1 prefill payload for an existing bank customer owned by the logged-in bank officer.")
public record BankOfficerCustomerStepOnePrefillResponse(
	@Schema(description = "Bank customer id", example = "14")
	Long bankCustomerId,
	@Schema(description = "User id in users table", example = "52")
	Long userId,
	@Schema(description = "Bank customer code", example = "BC-00014")
	String customerCode,
	@Schema(description = "Current onboarding/access status", example = "PENDING_STEP_4")
	String accessStatus,
	@Schema(description = "Customer first name", example = "John")
	String firstName,
	@Schema(description = "Customer last name", example = "Doe")
	String lastName,
	@Schema(description = "Customer NIC", example = "200012345678")
	String nic,
	@Schema(description = "Date of birth in ISO format (yyyy-MM-dd)", example = "2000-03-15")
	String dob,
	@Schema(description = "Email address", example = "john.doe@bank.com")
	String email,
	@Schema(description = "Mobile number", example = "+94771234567")
	String mobile,
	@Schema(description = "Province", example = "Western")
	String province,
	@Schema(description = "Address", example = "123, Main Street, Colombo")
	String address,
	@Schema(description = "Username for account login", example = "john.doe.2000")
	String username,
	@Schema(description = "Linked account number", example = "20010010012345")
	String accountNumber,
	@Schema(description = "Linked account status", example = "ACTIVE")
	String accountStatus,
	@Schema(description = "Linked account type", example = "SAVINGS")
	String accountType
) {
}
