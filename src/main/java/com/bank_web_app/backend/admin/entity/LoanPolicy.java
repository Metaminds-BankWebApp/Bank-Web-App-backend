package com.bank_web_app.backend.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loan_policies")
@Getter
@Setter
@NoArgsConstructor
public class LoanPolicy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "policy_id")
	private Long policyId;

	@Column(name = "loan_type", nullable = false, unique = true, length = 30)
	private String loanType;

	@Column(name = "max_dbr_ratio", nullable = false, precision = 5, scale = 4)
	private BigDecimal maxDbrRatio;

	@Column(name = "base_interest_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal baseInterestRate;

	@Column(name = "max_tenure_months", nullable = false)
	private Integer maxTenureMonths;

	@Column(name = "min_age", nullable = false)
	private Integer minAge;

	@Column(name = "max_age", nullable = false)
	private Integer maxAge;

	@Column(name = "max_finance_percentage", precision = 5, scale = 2)
	private BigDecimal maxFinancePercentage;

	@Column(name = "min_income_required", precision = 15, scale = 2)
	private BigDecimal minIncomeRequired;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		if (status == null || status.isBlank()) {
			status = "ACTIVE";
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
