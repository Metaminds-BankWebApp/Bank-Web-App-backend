package com.bank_web_app.backend.notification.entity;

import com.bank_web_app.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
	name = "notifications",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_notifications_recipient_deduplication",
		columnNames = { "recipient_user_id", "deduplication_key" }
	),
	indexes = {
		@Index(name = "idx_notifications_recipient_created", columnList = "recipient_user_id, created_at"),
		@Index(name = "idx_notifications_recipient_unread", columnList = "recipient_user_id, read_at"),
		@Index(name = "idx_notifications_source", columnList = "source")
	}
)
@Getter
@Setter
@NoArgsConstructor
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id")
	private Long notificationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_user_id", nullable = false)
	private User recipient;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 60)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 30)
	private NotificationSource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity", nullable = false, length = 20)
	private NotificationSeverity severity;

	@Column(name = "title", nullable = false, length = 180)
	private String title;

	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "action_key", length = 80)
	private String actionKey;

	@Column(name = "action_metadata", columnDefinition = "TEXT")
	private String actionMetadata;

	@Column(name = "deduplication_key", length = 180)
	private String deduplicationKey;

	@Column(name = "affected_count", nullable = false)
	private Integer affectedCount = 1;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Column(name = "dismissed_at")
	private LocalDateTime dismissedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = createdAt == null ? now : createdAt;
		updatedAt = now;
		affectedCount = affectedCount == null || affectedCount < 1 ? 1 : affectedCount;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
