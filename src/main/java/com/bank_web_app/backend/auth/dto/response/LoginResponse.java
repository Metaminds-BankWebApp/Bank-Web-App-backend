package com.bank_web_app.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Successful login response")
public record LoginResponse(
	@Schema(example = "8f1a77cc-6be0-4d72-a6d3-86bc4ef3f3a3")
	String accessToken,
	@Schema(example = "Bearer")
	String tokenType,
	@Schema(example = "p5M6Gq2b7S4f8w...", description = "Opaque refresh token used to rotate access tokens")
	String refreshToken,
	@Schema(example = "3600")
	Long expiresIn,
	UserInfo user
) {
	public record UserInfo(
		@Schema(example = "5")
		String id,
		@Schema(example = "john.doe@bank.com")
		String email,
		@Schema(example = "John Doe")
		String fullName,
		@Schema(example = "BANK_CUSTOMER")
		String role
	) {
	}
}
