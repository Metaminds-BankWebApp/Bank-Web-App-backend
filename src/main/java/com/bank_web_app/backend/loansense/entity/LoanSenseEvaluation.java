package com.bank_web_app.backend.loansense.entity;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loansense_evaluations")
@Getter
@Setter
@NoArgsConstructor
public class LoanSenseEvaluation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "loansense_evaluation_id")
	private Long loansenseEvaluationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_customer_id", nullable = false)
	private BankCustomer bankCustomer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_record_id", nullable = false)
	private BankCustomerFinancialRecord bankRecord;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_evaluation_id", nullable = false)
	private BankCreditEvaluation bankEvaluation;

	@Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
	private BigDecimal monthlyIncome;

	@Column(name = "total_existing_loan_emi", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalExistingLoanEmi;

	@Column(name = "leasing_hire_purchase_payment", nullable = false, precision = 15, scale = 2)
	private BigDecimal leasingHirePurchasePayment;

	@Column(name = "credit_card_outstanding", nullable = false, precision = 15, scale = 2)
	private BigDecimal creditCardOutstanding;

	@Column(name = "credit_card_limit", nullable = false, precision = 15, scale = 2)
	private BigDecimal creditCardLimit;

	@Column(name = "credit_card_min_payment", precision = 15, scale = 2)
	private BigDecimal creditCardMinPayment;

	@Column(name = "missed_payments_count", nullable = false)
	private Integer missedPaymentsCount;

	@Column(name = "tmdo", nullable = false, precision = 15, scale = 2)
	private BigDecimal tmdo;

	@Column(name = "dbr", nullable = false, precision = 6, scale = 4)
	private BigDecimal dbr;

	@Column(name = "max_allowed_emi", nullable = false, precision = 15, scale = 2)
	private BigDecimal maxAllowedEmi;

	@Column(name = "available_emi_capacity", nullable = false, precision = 15, scale = 2)
	private BigDecimal availableEmiCapacity;

	@Column(name = "risk_level", nullable = false, length = 20)
	private String riskLevel;

	@Column(name = "risk_multiplier", nullable = false, precision = 4, scale = 2)
	private BigDecimal riskMultiplier;

	@Column(name = "overall_status", nullable = false, length = 30)
	private String overallStatus;

	@Column(name = "remarks", columnDefinition = "TEXT")
	private String remarks;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "loanSenseEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt ASC")
	private List<LoanEligibilityResult> results = new ArrayList<>();

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
