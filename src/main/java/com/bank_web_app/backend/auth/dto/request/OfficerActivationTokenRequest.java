package com.bank_web_app.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OfficerActivationTokenRequest(
	@NotBlank(message = "Activation token is required.")
	@Size(max = 512, message = "Activation token is invalid.")
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	String activationToken
) {}
