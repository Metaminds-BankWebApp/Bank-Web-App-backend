package com.bank_web_app.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "Login request payload")
public record LoginRequest(
	@NotBlank(message = "Email is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 100, message = "Email must not exceed 100 characters.")
	@Schema(example = "john.doe@bank.com", requiredMode = Schema.RequiredMode.REQUIRED)
	String email,

	@NotBlank(message = "Password is required.")
	@Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters.")
	@Schema(example = "StrongPass123", requiredMode = Schema.RequiredMode.REQUIRED)
	String password
) {
}
