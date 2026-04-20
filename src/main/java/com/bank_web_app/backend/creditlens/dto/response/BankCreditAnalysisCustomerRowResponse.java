package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;

public record BankCreditAnalysisCustomerRowResponse(
	Long bankCustomerId,
	String customerCode,
	String fullName,
	String email,
	String phone,
	Long bankEvaluationId,
	Integer totalRiskPoints,
	String riskLevel,
	String riskLabel,
	LocalDateTime evaluationDate
) {
}
