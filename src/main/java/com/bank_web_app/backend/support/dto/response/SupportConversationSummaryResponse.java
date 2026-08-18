package com.bank_web_app.backend.support.dto.response;

import com.bank_web_app.backend.support.entity.SupportConversationStatus;
import java.time.LocalDateTime;

public record SupportConversationSummaryResponse(
	Long conversationId,
	String category,
	String subject,
	SupportConversationStatus status,
	SupportUserResponse createdBy,
	String lastMessagePreview,
	LocalDateTime lastMessageAt,
	long unreadMessageCount,
	LocalDateTime createdAt,
	LocalDateTime closedAt
) {}
