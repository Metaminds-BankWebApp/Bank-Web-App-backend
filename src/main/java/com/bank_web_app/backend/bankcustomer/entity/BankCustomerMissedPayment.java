package com.bank_web_app.backend.bankcustomer.entity;

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
@Table(name = "bank_customer_missed_payments")
@Getter
@Setter
@NoArgsConstructor
public class BankCustomerMissedPayment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "missed_payment_id")
	private Long missedPaymentId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_record_id", nullable = false, unique = true)
	private BankCustomerFinancialRecord financialRecord;

	@Column(name = "missed_payments", nullable = false)
	private Integer missedPayments = 0;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
