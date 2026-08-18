package com.bank_web_app.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthActionResponse(
	@Schema(example = "If an account matches those details, a verification code has been sent.")
	String message,
	@Schema(description = "Short-lived one-time reset token, returned only after valid OTP verification.")
	String resetToken
) {}
