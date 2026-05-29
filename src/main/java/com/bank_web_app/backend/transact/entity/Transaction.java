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

	// Primary key of the transaction record.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "transaction_id")
	private Long transactionId;

	// Owning bank customer for this transaction row.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_customer_id", nullable = false)
	private BankCustomer bankCustomer;

	// Source account number from which amount is sent.
	@Column(name = "sender_account_no", nullable = false, length = 20)
	private String senderAccountNo;

	// Destination account number receiving the transfer.
	@Column(name = "receiver_account_no", nullable = false, length = 20)
	private String receiverAccountNo;

	// Receiver display name stored at transaction time.
	@Column(name = "receiver_name", nullable = false, length = 150)
	private String receiverName;

	// Monetary amount of the transaction.
	@Column(name = "amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	// Customer-provided remark/reference note.
	@Column(name = "remark", nullable = false, length = 255)
	private String remark;

	// Unique customer-facing transaction reference number.
	@Column(name = "reference_no", nullable = false, unique = true, length = 50)
	private String referenceNo;

	// Transaction status (e.g., PENDING_OTP, SUCCESS, FAILED).
	@Column(name = "status", nullable = false, length = 20)
	private String status;

	// Indicates whether OTP verification was completed.
	@Column(name = "otp_verified", nullable = false)
	private Boolean otpVerified;

	// Indicates whether expense tracking integration is enabled.
	@Column(name = "expense_tracking_enabled", nullable = false)
	private Boolean expenseTrackingEnabled;

	// Failure reason captured when transaction ends in FAILED status.
	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	// Business transaction timestamp.
	@Column(name = "transaction_date", nullable = false)
	private LocalDateTime transactionDate;

	// Creation timestamp managed automatically.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Last update timestamp managed automatically.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// Applies defaults and timestamps when transaction is first persisted.
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

	// Refreshes update timestamp whenever transaction changes.
	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
