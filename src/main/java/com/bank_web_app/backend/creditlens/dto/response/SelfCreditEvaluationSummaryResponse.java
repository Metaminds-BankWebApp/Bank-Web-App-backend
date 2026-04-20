package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;

public record SelfCreditEvaluationSummaryResponse(
	Long selfEvaluationId,
	Long publicCustomerId,
	Long publicRecordId,
	Integer totalRiskPoints,
	String riskLevel,
	String riskLabel,
	LocalDateTime createdAt
) {
}
