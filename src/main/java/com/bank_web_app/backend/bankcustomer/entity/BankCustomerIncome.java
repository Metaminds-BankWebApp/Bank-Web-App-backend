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
@Table(name = "bank_customer_incomes")
@Getter
@Setter
@NoArgsConstructor
public class BankCustomerIncome {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "income_id")
	private Long incomeId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_record_id", nullable = false)
	private BankCustomerFinancialRecord financialRecord;

	@Column(name = "income_category", nullable = false, length = 20)
	private String incomeCategory;

	@Column(name = "amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "salary_type", length = 30)
	private String salaryType;

	@Column(name = "employment_type", length = 30)
	private String employmentType;

	@Column(name = "duration_months")
	private Integer durationMonths;

	@Column(name = "income_stability", length = 30)
	private String incomeStability;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
