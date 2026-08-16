package com.bank_web_app.backend.notification.dto.response;

import java.util.List;

public record NotificationPageResponse(
	List<NotificationResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	long unreadCount,
	long actionNeededCount
) {}
