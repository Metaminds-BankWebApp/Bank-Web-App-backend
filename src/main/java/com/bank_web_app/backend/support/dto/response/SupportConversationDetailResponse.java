package com.bank_web_app.backend.support.dto.response;

import com.bank_web_app.backend.support.entity.SupportConversationStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SupportConversationDetailResponse(
	Long conversationId,
	String category,
	String subject,
	SupportConversationStatus status,
	SupportUserResponse createdBy,
	String lastMessagePreview,
	LocalDateTime lastMessageAt,
	LocalDateTime createdAt,
	LocalDateTime closedAt,
	List<SupportMessageResponse> messages
) {}
