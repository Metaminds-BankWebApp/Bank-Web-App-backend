package com.bank_web_app.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ResetPasswordRequest", description = "Set a new password after OTP verification.")
public record ResetPasswordRequest(
	@NotBlank(message = "Email is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 100, message = "Email must not exceed 100 characters.")
	String email,

	@NotBlank(message = "Reset token is required.")
	@Size(max = 255, message = "Reset token must not exceed 255 characters.")
	String resetToken,

	@NotBlank(message = "Password is required.")
	@Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters.")
	String password,

	@NotBlank(message = "Confirm password is required.")
	@Size(min = 8, max = 255, message = "Confirm password must be between 8 and 255 characters.")
	String confirmPassword
) {}
