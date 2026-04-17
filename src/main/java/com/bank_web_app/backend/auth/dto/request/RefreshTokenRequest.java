package com.bank_web_app.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RefreshTokenRequest", description = "Refresh token request payload")
public record RefreshTokenRequest(
	@NotBlank(message = "Refresh token is required.")
	@Size(max = 512, message = "Refresh token must not exceed 512 characters.")
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	String refreshToken
) {
}
