package com.bank_web_app.backend.bankofficer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BankOfficerCustomerIdentityResponse", description = "Identity mapping for a bank customer owned by the logged-in bank officer.")
public record BankOfficerCustomerIdentityResponse(
	@Schema(description = "Bank customer id", example = "14")
	Long bankCustomerId,
	@Schema(description = "User id in users table", example = "52")
	Long userId,
	@Schema(description = "Bank customer code", example = "BC-00014")
	String customerCode
) {
}