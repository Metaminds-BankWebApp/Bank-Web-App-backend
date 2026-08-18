package com.bank_web_app.backend.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ForgotPasswordRequest", description = "Request a password reset using an email address or username.")
public record ForgotPasswordRequest(
	@NotBlank(message = "Email address or username is required.")
	@Size(max = 100, message = "Email address or username must not exceed 100 characters.")
	@JsonAlias("email")
	@Schema(example = "john.doe@bank.com or john.doe", requiredMode = Schema.RequiredMode.REQUIRED)
	String identifier
) {}
