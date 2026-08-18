package com.bank_web_app.backend.publiccustomer.entity;

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
@Table(name = "public_customer_cards")
@Getter
@Setter
@NoArgsConstructor
public class PublicCustomerCard {

	// Primary key of the card entry.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "card_id")
	private Long cardId;

	// Parent financial record this card belongs to.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "record_id", nullable = false)
	private PublicCustomerFinancialRecord financialRecord;

	// Card issuer/provider name.
	@Column(name = "provider", length = 100)
	private String provider;

	// Approved credit limit for the card.
	@Column(name = "credit_limit", nullable = false, precision = 15, scale = 2)
	private BigDecimal creditLimit;

	// Current outstanding card balance.
	@Column(name = "outstanding_balance", nullable = false, precision = 15, scale = 2)
	private BigDecimal outstandingBalance;

	// Row creation timestamp.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Initializes created timestamp when row is first persisted.
	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
