package com.bank_web_app.backend.loansense.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LoanSenseEvaluationResponse(
	Long loansenseEvaluationId,
	Long bankCustomerId,
	Long bankRecordId,
	Long bankEvaluationId,
	BigDecimal monthlyIncome,
	BigDecimal totalExistingLoanEmi,
	BigDecimal leasingHirePurchasePayment,
	BigDecimal creditCardOutstanding,
	BigDecimal creditCardLimit,
	BigDecimal creditCardMinPayment,
	Integer missedPaymentsCount,
	BigDecimal tmdo,
	BigDecimal dbr,
	BigDecimal maxAllowedEmi,
	BigDecimal availableEmiCapacity,
	String riskLevel,
	String riskLabel,
	BigDecimal riskMultiplier,
	String overallStatus,
	String overallStatusLabel,
	String remarks,
	LocalDateTime createdAt,
	List<LoanSenseLoanOptionResponse> loanOptions
) {
}
