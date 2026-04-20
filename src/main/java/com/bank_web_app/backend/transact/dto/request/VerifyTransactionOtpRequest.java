package com.bank_web_app.backend.transact.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "VerifyTransactionOtpRequest", description = "Payload to verify OTP for a pending transaction.")
public record VerifyTransactionOtpRequest(
	@Schema(description = "Transaction reference number.", example = "TXN-20260420-9K4N2A", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Reference number is required.")
	String referenceNo,
	@Schema(description = "One-time password sent to registered email.", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "OTP code is required.")
	String otpCode
) {
}
