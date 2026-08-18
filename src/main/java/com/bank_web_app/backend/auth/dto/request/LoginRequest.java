package com.bank_web_app.backend.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "Login request payload")
public record LoginRequest(
	@NotBlank(message = "Email address or username is required.")
	@Size(max = 100, message = "Email address or username must not exceed 100 characters.")
	@Schema(example = "john.doe@bank.com or john.doe", requiredMode = Schema.RequiredMode.REQUIRED)
	@JsonAlias("email")
	String identifier,

	@NotBlank(message = "Password is required.")
	@Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters.")
	@Schema(example = "StrongPass123", requiredMode = Schema.RequiredMode.REQUIRED)
	String password
) {
}
