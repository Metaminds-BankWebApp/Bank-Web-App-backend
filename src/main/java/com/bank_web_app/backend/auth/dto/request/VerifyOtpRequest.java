package com.bank_web_app.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "VerifyOtpRequest", description = "Verify a password reset one-time password.")
public record VerifyOtpRequest(
	@NotBlank(message = "Email is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 100, message = "Email must not exceed 100 characters.")
	String email,

	@NotBlank(message = "OTP is required.")
	@Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit code.")
	String otp
) {}
