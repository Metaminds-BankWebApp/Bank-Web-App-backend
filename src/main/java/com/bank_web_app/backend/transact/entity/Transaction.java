package com.bank_web_app.backend.transact.entity;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bank_customer_transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "transaction_id")
	private Long transactionId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_customer_id", nullable = false)
	private BankCustomer bankCustomer;

	@Column(name = "sender_account_no", nullable = false, length = 20)
	private String senderAccountNo;

	@Column(name = "receiver_account_no", nullable = false, length = 20)
	private String receiverAccountNo;

	@Column(name = "receiver_name", nullable = false, length = 150)
	private String receiverName;

	@Column(name = "amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "remark", nullable = false, length = 255)
	private String remark;

	@Column(name = "reference_no", nullable = false, unique = true, length = 50)
	private String referenceNo;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "otp_verified", nullable = false)
	private Boolean otpVerified;

	@Column(name = "expense_tracking_enabled", nullable = false)
	private Boolean expenseTrackingEnabled;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	@Column(name = "transaction_date", nullable = false)
	private LocalDateTime transactionDate;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (transactionDate == null) {
			transactionDate = now;
		}
		if (status == null || status.isBlank()) {
			status = "PENDING_OTP";
		}
		if (otpVerified == null) {
			otpVerified = Boolean.FALSE;
		}
		if (expenseTrackingEnabled == null) {
			expenseTrackingEnabled = Boolean.FALSE;
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
