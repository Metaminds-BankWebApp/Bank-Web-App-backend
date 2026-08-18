package com.bank_web_app.backend.support.entity;

import com.bank_web_app.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "support_message_reads")
@Getter
@Setter
@NoArgsConstructor
public class SupportMessageRead {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "support_message_read_id")
	private Long supportMessageReadId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "message_id", nullable = false)
	private SupportMessage message;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reader_user_id", nullable = false)
	private User reader;

	@Column(name = "read_at", nullable = false)
	private LocalDateTime readAt;

	@PrePersist
	void onCreate() {
		readAt = readAt == null ? LocalDateTime.now() : readAt;
	}
}
