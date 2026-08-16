package com.bank_web_app.backend.notification.event;

import java.util.Map;

public record NotificationDomainEvent(
	NotificationEventType type,
	Long recipientUserId,
	Long actorUserId,
	Long subjectId,
	Map<String, String> metadata
) {
	public NotificationDomainEvent {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
