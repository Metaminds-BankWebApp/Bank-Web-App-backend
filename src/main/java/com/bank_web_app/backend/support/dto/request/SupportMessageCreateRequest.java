package com.bank_web_app.backend.support.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportMessageCreateRequest(
	@NotBlank(message = "Message is required.") @Size(max = 4000) String message
) {}
