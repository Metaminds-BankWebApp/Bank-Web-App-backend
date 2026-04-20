package com.bank_web_app.backend.loansense.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanTypeDetailResponse(
	Long loansenseEvaluationId,
	Long loanResultId,
	String loanType,
	String loanTypeKey,
	String loanTypeLabel,
	String eligibilityStatus,
	String eligibilityLabel,
	BigDecimal recommendedMaxAmount,
	BigDecimal estimatedEmi,
	BigDecimal interestRate,
	Integer policyMinTenureMonths,
	Integer policyMaxTenureMonths,
	String tenureLabel,
	Integer customerAge,
	BigDecimal monthlyIncome,
	BigDecimal totalExistingLoanEmi,
	BigDecimal creditCardMinPayment,
	BigDecimal leasingHirePurchasePayment,
	BigDecimal tmdo,
	BigDecimal dbr,
	BigDecimal policyMaxDbrRatio,
	BigDecimal maxAllowedEmi,
	BigDecimal availableEmiCapacity,
	String riskLevel,
	String riskLabel,
	BigDecimal riskMultiplier,
	String riskAdjustmentDescription,
	BigDecimal policyMinIncomeRequired,
	Integer policyMinAge,
	Integer policyMaxAge,
	String decisionReason,
	LocalDateTime createdAt
) {
}
