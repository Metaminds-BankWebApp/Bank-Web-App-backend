package com.bank_web_app.backend.support.entity;

import com.bank_web_app.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "support_conversations")
@Getter
@Setter
@NoArgsConstructor
public class SupportConversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "conversation_id")
	private Long conversationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private User createdBy;

	@Column(nullable = false, length = 60)
	private String category;

	@Column(nullable = false, length = 160)
	private String subject;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SupportConversationStatus status;

	@Column(name = "last_message_preview", length = 500)
	private String lastMessagePreview;

	@Column(name = "last_message_at", nullable = false)
	private LocalDateTime lastMessageAt;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = createdAt == null ? now : createdAt;
		updatedAt = now;
		lastMessageAt = lastMessageAt == null ? now : lastMessageAt;
		status = status == null ? SupportConversationStatus.OPEN : status;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
