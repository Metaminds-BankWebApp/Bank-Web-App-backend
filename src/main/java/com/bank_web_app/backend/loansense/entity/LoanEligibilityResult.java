package com.bank_web_app.backend.loansense.entity;

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
@Table(name = "loan_eligibility_results")
@Getter
@Setter
@NoArgsConstructor
public class LoanEligibilityResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "loan_result_id")
	private Long loanResultId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "loansense_evaluation_id", nullable = false)
	private LoanSenseEvaluation loanSenseEvaluation;

	@Column(name = "loan_type", nullable = false, length = 30)
	private String loanType;

	@Column(name = "customer_age", nullable = false)
	private Integer customerAge;

	@Column(name = "asset_value", precision = 15, scale = 2)
	private BigDecimal assetValue;

	@Column(name = "estimated_emi", nullable = false, precision = 15, scale = 2)
	private BigDecimal estimatedEmi;

	@Column(name = "recommended_max_amount", precision = 15, scale = 2)
	private BigDecimal recommendedMaxAmount;

	@Column(name = "interest_rate", precision = 5, scale = 2)
	private BigDecimal interestRate;

	@Column(name = "tenure_months")
	private Integer tenureMonths;

	@Column(name = "eligibility_status", nullable = false, length = 30)
	private String eligibilityStatus;

	@Column(name = "decision_reason", columnDefinition = "TEXT")
	private String decisionReason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
