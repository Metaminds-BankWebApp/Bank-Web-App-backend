package com.bank_web_app.backend.bankcustomer.entity;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bank_customer_cards")
@Getter
@Setter
@NoArgsConstructor
public class BankCustomerCard {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "card_id")
	private Long cardId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_record_id", nullable = false)
	private BankCustomerFinancialRecord financialRecord;

	@Column(name = "provider", length = 100)
	private String provider;

	@Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
	private BigDecimal creditLimit;

	@Column(name = "outstanding_balance", nullable = false, precision = 15, scale = 2)
	private BigDecimal outstandingBalance;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
