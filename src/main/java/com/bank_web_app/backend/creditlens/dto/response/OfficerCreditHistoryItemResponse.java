package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;

/** A CreditLens evaluation activity row visible to the officer who owns the customer. */
public record OfficerCreditHistoryItemResponse(
	Long bankEvaluationId,
	Long bankCustomerId,
	String customerCode,
	String customerName,
	String evaluationSource,
	Integer totalRiskPoints,
	String riskLevel,
	String riskLabel,
	LocalDateTime createdAt
) {
}
