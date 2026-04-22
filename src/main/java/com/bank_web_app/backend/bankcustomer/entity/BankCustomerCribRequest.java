package com.bank_web_app.backend.bankcustomer.entity;

import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
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
@Table(name = "bank_customer_crib_requests")
@Getter
@Setter
@NoArgsConstructor
public class BankCustomerCribRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "crib_request_id")
	private Long cribRequestId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_customer_id", nullable = false)
	private BankCustomer bankCustomer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requested_by_officer_id", nullable = false)
	private BankOfficer requestedByOfficer;

	@Column(name = "request_type", nullable = false, length = 30)
	private String requestType;

	@Column(name = "request_status", nullable = false, length = 20)
	private String requestStatus;

	@Column(name = "report_status", nullable = false, length = 20)
	private String reportStatus;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "response_received_at")
	private LocalDateTime responseReceivedAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		if (requestedAt == null) {
			requestedAt = now;
		}
		if (requestType == null || requestType.isBlank()) {
			requestType = "FULL_REPORT";
		}
		if (requestStatus == null || requestStatus.isBlank()) {
			requestStatus = "PENDING";
		}
		if (reportStatus == null || reportStatus.isBlank()) {
			reportStatus = "NOT_REQUESTED";
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
