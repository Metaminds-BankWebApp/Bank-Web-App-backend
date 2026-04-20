package com.bank_web_app.backend.loansense.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanSenseHistoryItemResponse(
	Long loansenseEvaluationId,
	Long loanResultId,
	String evaluationMonthLabel,
	LocalDateTime evaluationDate,
	String loanType,
	String loanTypeKey,
	String loanTypeLabel,
	String eligibilityStatus,
	String eligibilityLabel,
	BigDecimal recommendedMaxAmount,
	Integer tenureMonths,
	String tenureLabel,
	String riskLevel,
	String riskLabel
) {
}
