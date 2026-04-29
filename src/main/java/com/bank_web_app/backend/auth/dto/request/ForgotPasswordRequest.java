package com.bank_web_app.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ForgotPasswordRequest", description = "Request a password reset OTP for an account email.")
public record ForgotPasswordRequest(
	@NotBlank(message = "Email is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 100, message = "Email must not exceed 100 characters.")
	String email
) {}
