package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Lightweight bank-customer CreditLens history item used in lists and timelines.
 */
@Schema(name = "BankCreditEvaluationSummaryResponse", description = "Summary row for a bank credit evaluation for list views.")
public record BankCreditEvaluationSummaryResponse(
	@Schema(description = "Bank evaluation id", example = "501")
	Long bankEvaluationId,
	@Schema(description = "Bank customer id", example = "14")
	Long bankCustomerId,
	@Schema(description = "Bank record id", example = "77")
	Long bankRecordId,
	@Schema(description = "Officer id who evaluated the customer", example = "7")
	Long evaluatedByOfficerId,
	@Schema(description = "Evaluation source", example = "MANUAL")
	String evaluationSource,
	@Schema(description = "Total risk points", example = "72")
	Integer totalRiskPoints,
	@Schema(description = "Risk level", example = "LOW")
	String riskLevel,
	@Schema(description = "Human-readable risk label", example = "Low")
	String riskLabel,
	@Schema(description = "Evaluation date and time")
	LocalDateTime createdAt
) {
}
