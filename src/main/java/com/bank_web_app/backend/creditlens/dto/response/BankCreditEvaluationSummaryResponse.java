package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;

public record BankCreditEvaluationSummaryResponse(
	Long bankEvaluationId,
	Long bankCustomerId,
	Long bankRecordId,
	Long evaluatedByOfficerId,
	String evaluationSource,
	Integer totalRiskPoints,
	String riskLevel,
	String riskLabel,
	LocalDateTime createdAt
) {
}
