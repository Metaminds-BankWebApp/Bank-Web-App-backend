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
@Table(name = "public_customer_loans")
@Getter
@Setter
@NoArgsConstructor
public class PublicCustomerLoan {

	// Primary key of loan entry.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "loan_id")
	private Long loanId;

	// Parent financial record this loan belongs to.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "record_id", nullable = false)
	private PublicCustomerFinancialRecord financialRecord;

	// Loan type/category label.
	@Column(name = "loan_type", nullable = false, length = 50)
	private String loanType;

	// Current monthly EMI for this loan.
	@Column(name = "monthly_emi", nullable = false, precision = 15, scale = 2)
	private BigDecimal monthlyEmi;

	// Remaining outstanding loan balance.
	@Column(name = "remaining_balance", nullable = false, precision = 15, scale = 2)
	private BigDecimal remainingBalance;

	// Row creation timestamp.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Initializes created timestamp when row is first persisted.
	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
