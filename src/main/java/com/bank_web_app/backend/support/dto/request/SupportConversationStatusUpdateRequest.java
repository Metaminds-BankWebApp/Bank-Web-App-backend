package com.bank_web_app.backend.support.dto.request;

import com.bank_web_app.backend.support.entity.SupportConversationStatus;
import jakarta.validation.constraints.NotNull;

public record SupportConversationStatusUpdateRequest(
	@NotNull(message = "Status is required.") SupportConversationStatus status
) {}
