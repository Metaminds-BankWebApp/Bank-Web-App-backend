package com.bank_web_app.backend.transact.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Request payload used to confirm a pending transaction with OTP.
@Schema(name = "VerifyTransactionOtpRequest", description = "Payload to verify OTP for a pending transaction.")
public record VerifyTransactionOtpRequest(
	// Unique transaction reference that links OTP to the initiated transfer.
	@Schema(description = "Transaction reference number.", example = "TXN-20260420-9K4N2A", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Reference number is required.")
	String referenceNo,
	// One-time password entered by customer for transfer authorization.
	@Schema(description = "One-time password sent to registered email.", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "OTP code is required.")
	String otpCode
) {
}
