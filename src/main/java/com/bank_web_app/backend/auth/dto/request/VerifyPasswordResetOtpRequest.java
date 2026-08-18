package com.bank_web_app.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "VerifyPasswordResetOtpRequest", description = "Verify a password-reset OTP.")
public record VerifyPasswordResetOtpRequest(
	@NotBlank(message = "Email address or username is required.")
	@Size(max = 100, message = "Email address or username must not exceed 100 characters.")
	@Schema(example = "john.doe@bank.com or john.doe", requiredMode = Schema.RequiredMode.REQUIRED)
	String identifier,

	@NotBlank(message = "OTP code is required.")
	@Pattern(regexp = "\\d{6}", message = "OTP code must be 6 digits.")
	@Schema(example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
	String otp
) {}
