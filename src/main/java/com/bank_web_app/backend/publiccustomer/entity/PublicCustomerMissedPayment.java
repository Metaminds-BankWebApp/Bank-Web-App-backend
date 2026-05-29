package com.bank_web_app.backend.publiccustomer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "public_customer_missed_payments")
@Getter
@Setter
@NoArgsConstructor
public class PublicCustomerMissedPayment {

	// Primary key of missed-payment aggregate row.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "missed_payment_id")
	private Long missedPaymentId;

	// One-to-one financial record owning this missed-payment count.
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "record_id", nullable = false, unique = true)
	private PublicCustomerFinancialRecord financialRecord;

	// Aggregate missed-payments count in the tracked window.
	@Column(name = "missed_payments", nullable = false)
	private Integer missedPayments = 0;

	// Row creation timestamp.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Initializes created timestamp when row is first persisted.
	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
