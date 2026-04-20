package com.bank_web_app.backend.loansense.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanSenseLoanOptionResponse(
	Long loanResultId,
	String loanType,
	String loanTypeKey,
	String loanTypeLabel,
	String eligibilityStatus,
	String eligibilityLabel,
	BigDecimal recommendedMaxAmount,
	BigDecimal estimatedEmi,
	BigDecimal interestRate,
	Integer tenureMonths,
	String tenureLabel,
	Integer customerAge,
	String decisionReason,
	LocalDateTime createdAt
) {
}
