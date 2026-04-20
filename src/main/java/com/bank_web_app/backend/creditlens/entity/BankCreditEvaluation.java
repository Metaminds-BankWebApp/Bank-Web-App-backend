package com.bank_web_app.backend.creditlens.entity;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bank_credit_evaluations")
@Getter
@Setter
@NoArgsConstructor
public class BankCreditEvaluation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "bank_evaluation_id")
	private Long bankEvaluationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_customer_id", nullable = false)
	private BankCustomer bankCustomer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_record_id", nullable = false)
	private BankCustomerFinancialRecord bankRecord;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evaluated_by_officer_id", nullable = false)
	private BankOfficer evaluatedByOfficer;

	@Column(name = "evaluation_source", nullable = false, length = 30)
	private String evaluationSource = "MANUAL";

	@Column(name = "remarks", columnDefinition = "TEXT")
	private String remarks;

	@Column(name = "total_risk_points", nullable = false)
	private Integer totalRiskPoints;

	@Column(name = "risk_level", nullable = false, length = 20)
	private String riskLevel;

	@Column(name = "total_monthly_income", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalMonthlyIncome;

	@Column(name = "total_monthly_debt_payment", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalMonthlyDebtPayment;

	@Column(name = "total_card_limit", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalCardLimit;

	@Column(name = "total_card_outstanding", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalCardOutstanding;

	@Column(name = "dti_ratio", nullable = false, precision = 6, scale = 4)
	private BigDecimal dtiRatio;

	@Column(name = "credit_utilization_ratio", nullable = false, precision = 6, scale = 4)
	private BigDecimal creditUtilizationRatio;

	@Column(name = "active_facilities_count", nullable = false)
	private Integer activeFacilitiesCount;

	@Column(name = "missed_payments_count", nullable = false)
	private Integer missedPaymentsCount;

	@Column(name = "payment_history_points", nullable = false)
	private Integer paymentHistoryPoints;

	@Column(name = "dti_points", nullable = false)
	private Integer dtiPoints;

	@Column(name = "utilization_points", nullable = false)
	private Integer utilizationPoints;

	@Column(name = "income_stability_points", nullable = false)
	private Integer incomeStabilityPoints;

	@Column(name = "exposure_points", nullable = false)
	private Integer exposurePoints;

	@Column(name = "report_generated", nullable = false)
	private Boolean reportGenerated = Boolean.FALSE;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (reportGenerated == null) {
			reportGenerated = Boolean.FALSE;
		}
		if (evaluationSource == null || evaluationSource.isBlank()) {
			evaluationSource = "MANUAL";
		}
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
