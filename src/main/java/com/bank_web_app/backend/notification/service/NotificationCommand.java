package com.bank_web_app.backend.notification.service;

import com.bank_web_app.backend.notification.entity.NotificationSeverity;
import com.bank_web_app.backend.notification.entity.NotificationSource;
import com.bank_web_app.backend.notification.entity.NotificationType;
import java.util.Map;

public record NotificationCommand(
	Long recipientUserId,
	NotificationType type,
	NotificationSource source,
	NotificationSeverity severity,
	String title,
	String message,
	String actionKey,
	Map<String, String> actionMetadata,
	String deduplicationKey,
	boolean aggregate
) {}
