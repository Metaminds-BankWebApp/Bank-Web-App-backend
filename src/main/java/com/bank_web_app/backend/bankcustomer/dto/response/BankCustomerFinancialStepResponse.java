package com.bank_web_app.backend.bankcustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BankCustomerFinancialStepResponse", description = "Response returned after saving one financial step.")
public record BankCustomerFinancialStepResponse(
	@Schema(description = "Bank financial record id", example = "12")
	Long bankRecordId,
	@Schema(description = "Bank customer id", example = "7")
	Long bankCustomerId,
	@Schema(description = "Saved step identifier", example = "INCOME")
	String step,
	@Schema(description = "Operation result message", example = "Income step saved successfully.")
	String message
) {
}
