package com.bank_web_app.backend.admin.entity;
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
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "audit_log_id")
	private Long auditLogId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id")
	private User actorUser;

	@Column(name = "actor_name", nullable = false, length = 150)
	private String actorName;

	@Column(name = "actor_role", length = 60)
	private String actorRole;

	@Column(name = "action_type", nullable = false, length = 80)
	private String actionType;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "target_type", length = 50)
	private String targetType;

	@Column(name = "target_id", length = 100)
	private String targetId;

	@Column(name = "details", length = 500)
	private String details;

	@Column(name = "tone", nullable = false, length = 20)
	private String tone;

	@Column(name = "ip_address", length = 64)
	private String ipAddress;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
		if (tone == null || tone.isBlank()) {
			tone = "INFO";
		}
	}
}
