package com.bank_web_app.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ResetPasswordRequest", description = "Set a new password after successful OTP verification.")
public record ResetPasswordRequest(
	@NotBlank(message = "Reset session is required.")
	@Size(max = 512, message = "Reset session is invalid.")
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	String resetToken,

	@NotBlank(message = "New password is required.")
	@Size(max = 255, message = "New password must not exceed 255 characters.")
	@Schema(example = "StrongerPass123", requiredMode = Schema.RequiredMode.REQUIRED)
	String password,

	@NotBlank(message = "Confirm password is required.")
	@Size(max = 255, message = "Confirm password must not exceed 255 characters.")
	@Schema(example = "StrongerPass123", requiredMode = Schema.RequiredMode.REQUIRED)
	String confirmPassword
) {}
