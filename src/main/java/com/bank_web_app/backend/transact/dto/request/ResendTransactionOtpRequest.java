package com.bank_web_app.backend.transact.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ResendTransactionOtpRequest", description = "Payload to resend OTP for a pending transaction.")
public record ResendTransactionOtpRequest(
	@Schema(description = "Transaction reference number.", example = "TXN-20260420-9K4N2A", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Reference number is required.")
	String referenceNo
) {
}
