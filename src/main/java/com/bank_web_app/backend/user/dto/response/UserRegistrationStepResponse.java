package com.bank_web_app.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserRegistrationStepResponse", description = "Response returned after saving Bank Customer step-one registration state.")
public record UserRegistrationStepResponse(
	@Schema(description = "User id in users table", example = "12")
	Long userId,
	@Schema(description = "Registration role", example = "BANK_CUSTOMER")
	String role,
	@Schema(description = "Registration state", example = "DRAFT")
	String state,
	@Schema(description = "Response message", example = "Bank customer step one draft saved successfully.")
	String message
) {
}
