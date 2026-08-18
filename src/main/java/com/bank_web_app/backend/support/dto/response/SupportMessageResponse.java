package com.bank_web_app.backend.support.dto.response;

import java.time.LocalDateTime;

public record SupportMessageResponse(
	Long messageId,
	SupportUserResponse sender,
	String message,
	LocalDateTime createdAt,
	boolean readByCurrentUser,
	boolean readByOtherParty
) {}
