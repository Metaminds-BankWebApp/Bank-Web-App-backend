package com.bank_web_app.backend.bankcustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "BankCustomerCribRequestStepRequest", description = "Step 6 payload to submit CRIB request details.")
public record BankCustomerCribRequestStepRequest(
	@Schema(description = "Type of CRIB request", example = "FULL_REPORT", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Request type is required.")
	String requestType
) {
}
