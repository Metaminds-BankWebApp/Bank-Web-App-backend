package com.bank_web_app.backend.notification.repository;

import com.bank_web_app.backend.notification.entity.Notification;
import com.bank_web_app.backend.notification.entity.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

	Optional<Notification> findByRecipient_UserIdAndDeduplicationKey(Long recipientUserId, String deduplicationKey);

	Optional<Notification> findByNotificationIdAndRecipient_UserIdAndDismissedAtIsNull(
		Long notificationId,
		Long recipientUserId
	);

	List<Notification> findAllByRecipient_UserIdAndTypeAndDismissedAtIsNull(
		Long recipientUserId,
		NotificationType type
	);

	long deleteByRecipient_UserId(Long recipientUserId);
}
