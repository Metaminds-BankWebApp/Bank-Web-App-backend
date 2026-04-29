package com.bank_web_app.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GeneratedBankCustomerCredentialsResponse", description = "Generated bank customer username and password suggestion.")
public record GeneratedBankCustomerCredentialsResponse(
	@Schema(description = "Generated unique username", example = "seekadissanayake482")
	String username,
	@Schema(description = "Generated temporary password", example = "A7!kQ29xP#z4")
	String password
) {
}