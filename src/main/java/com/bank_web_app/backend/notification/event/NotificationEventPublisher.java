package com.bank_web_app.backend.notification.event;

import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	public NotificationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void publish(
		NotificationEventType type,
		Long recipientUserId,
		Long actorUserId,
		Long subjectId,
		Map<String, String> metadata
	) {
		eventPublisher.publishEvent(new NotificationDomainEvent(type, recipientUserId, actorUserId, subjectId, metadata));
	}
}
