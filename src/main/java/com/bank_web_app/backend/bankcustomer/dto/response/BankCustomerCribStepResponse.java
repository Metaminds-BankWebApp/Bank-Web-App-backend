package com.bank_web_app.backend.bankcustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BankCustomerCribStepResponse", description = "Response returned after saving CRIB onboarding steps.")
public record BankCustomerCribStepResponse(
	@Schema(description = "CRIB request id", example = "21")
	Long cribRequestId,
	@Schema(description = "Bank customer id", example = "7")
	Long bankCustomerId,
	@Schema(description = "Saved step identifier", example = "CRIB_REQUEST")
	String step,
	@Schema(description = "Current request status", example = "SUBMITTED")
	String requestStatus,
	@Schema(description = "Current report status", example = "PENDING")
	String reportStatus,
	@Schema(description = "Operation result message", example = "CRIB request step saved successfully.")
	String message
) {
}
