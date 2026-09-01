package com.bank_web_app.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record OfficerActivationResponse(
	@Schema(example = "EXPIRED")
	String status,
	String message,
	int resendAttemptsUsed,
	int remainingResends,
	boolean canResend
) {}
