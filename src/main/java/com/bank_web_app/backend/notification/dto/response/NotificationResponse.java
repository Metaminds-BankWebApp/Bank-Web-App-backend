package com.bank_web_app.backend.notification.dto.response;

import com.bank_web_app.backend.notification.entity.NotificationSeverity;
import com.bank_web_app.backend.notification.entity.NotificationSource;
import com.bank_web_app.backend.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(
	Long id,
	NotificationType type,
	NotificationSource source,
	NotificationSeverity severity,
	String title,
	String message,
	String actionKey,
	Map<String, String> actionMetadata,
	Integer affectedCount,
	boolean unread,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {}
