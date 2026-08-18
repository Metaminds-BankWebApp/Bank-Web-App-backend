package com.bank_web_app.backend.support.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportConversationCreateRequest(
	@NotBlank(message = "Category is required.") @Size(max = 60) String category,
	@NotBlank(message = "Subject is required.") @Size(max = 160) String subject,
	@NotBlank(message = "Message is required.") @Size(max = 4000) String message
) {}
