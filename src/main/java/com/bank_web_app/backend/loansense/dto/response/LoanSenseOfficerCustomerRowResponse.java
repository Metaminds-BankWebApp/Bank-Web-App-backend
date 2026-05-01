package com.bank_web_app.backend.loansense.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanSenseOfficerCustomerRowResponse(
	Long bankCustomerId,
	String customerCode,
	String customerName,
	Long loansenseEvaluationId,
	String overallStatus,
	String overallStatusLabel,
	String riskLevel,
	String riskLabel,
	BigDecimal maxRecommendedAmount,
	BigDecimal availableEmiCapacity,
	LocalDateTime lastEvaluatedAt
) {
}
