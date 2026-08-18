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
@Table(name = "public_customer_incomes")
@Getter
@Setter
@NoArgsConstructor
public class PublicCustomerIncome {

	// Primary key of income entry.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "income_id")
	private Long incomeId;

	// Parent financial record this income belongs to.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "record_id", nullable = false)
	private PublicCustomerFinancialRecord financialRecord;

	// Income category/type (salary, business, etc.).
	@Column(name = "income_category", nullable = false, length = 20)
	private String incomeCategory;

	// Amount declared for this income source.
	@Column(name = "amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	// Salary type when category is salary-based.
	@Column(name = "salary_type", length = 30)
	private String salaryType;

	// Employment type metadata for salary records.
	@Column(name = "employment_type", length = 30)
	private String employmentType;

	// Duration in months for relevant salary entries.
	@Column(name = "duration_months")
	private Integer durationMonths;

	// Stability value when category is business income.
	@Column(name = "income_stability", length = 30)
	private String incomeStability;

	// Row creation timestamp.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Initializes created timestamp when row is first persisted.
	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
