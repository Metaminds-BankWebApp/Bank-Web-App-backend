package com.bank_web_app.backend.creditlens.entity;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
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
@Table(name = "self_credit_evaluations")
@Getter
@Setter
@NoArgsConstructor
public class SelfCreditEvaluation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "self_evaluation_id")
	private Long selfEvaluationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "public_customer_id", nullable = false)
	private PublicCustomerProfile publicCustomer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "public_record_id", nullable = false)
	private PublicCustomerFinancialRecord publicRecord;

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
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
