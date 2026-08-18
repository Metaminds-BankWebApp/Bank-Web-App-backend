package com.bank_web_app.backend.transact.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "transaction_otp_logs")
@Getter
@Setter
@NoArgsConstructor
public class OtpRecord {

	// Primary key of OTP log entry.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "otp_log_id")
	private Long otpLogId;

	// Transaction associated with this OTP record.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transaction_id", nullable = false)
	private Transaction transaction;

	// Stored hashed OTP value (not plain OTP).
	@Column(name = "otp_code_hash", nullable = false, length = 255)
	private String otpCodeHash;

	// Email address used for OTP delivery.
	@Column(name = "sent_to_email", nullable = false, length = 150)
	private String sentToEmail;

	// Current OTP status (e.g., SENT, VERIFIED, EXPIRED).
	@Column(name = "otp_status", nullable = false, length = 20)
	private String otpStatus;

	// OTP expiration timestamp.
	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	// Timestamp when OTP was successfully verified.
	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	// Number of resend attempts for this OTP record.
	@Column(name = "resend_count", nullable = false)
	private Integer resendCount;

	// Creation timestamp managed automatically.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Last update timestamp managed automatically.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// Applies defaults and timestamps when OTP log is first created.
	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (otpStatus == null || otpStatus.isBlank()) {
			otpStatus = "SENT";
		}
		if (resendCount == null) {
			resendCount = 0;
		}
		createdAt = now;
		updatedAt = now;
	}

	// Refreshes update timestamp whenever OTP log changes.
	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
