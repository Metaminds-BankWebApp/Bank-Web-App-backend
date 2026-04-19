package com.bank_web_app.backend.bankofficer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccountVerificationResponse", description = "Result of bank account verification")
public record AccountVerificationResponse(
	@Schema(example = "true")
	boolean exists,
	@Schema(example = "12", nullable = true)
	Long accountId,
	@Schema(example = "ACTIVE")
	String status,
	@Schema(example = "SAVINGS", nullable = true)
	String accountType,
	@Schema(example = "Account found.")
	String message
) {
}
