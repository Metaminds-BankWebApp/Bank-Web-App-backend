package com.bank_web_app.backend.support.entity;

import com.bank_web_app.backend.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "support_messages")
@Getter
@Setter
@NoArgsConstructor
public class SupportMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "message_id")
	private Long messageId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversation_id", nullable = false)
	private SupportConversation conversation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_user_id")
	private User sender;

	@Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
	private String messageText;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SupportMessageRead> reads = new ArrayList<>();

	@PrePersist
	void onCreate() {
		createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
	}
}
