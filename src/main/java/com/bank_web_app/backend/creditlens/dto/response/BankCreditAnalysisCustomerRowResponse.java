package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Summary row shown in the officer portfolio table for CreditLens analysis.
 */
@Schema(name = "BankCreditAnalysisCustomerRowResponse", description = "Single row in the bank officer credit analysis dashboard table.")
public record BankCreditAnalysisCustomerRowResponse(
	@Schema(description = "Bank customer id", example = "14")
	Long bankCustomerId,
	@Schema(description = "Bank customer code", example = "BC-00014")
	String customerCode,
	@Schema(description = "Customer full name", example = "Amila Silva")
	String fullName,
	@Schema(description = "Customer email", example = "amila.silva@example.com")
	String email,
	@Schema(description = "Customer phone", example = "+94771234567")
	String phone,
	@Schema(description = "Latest bank evaluation id", example = "501")
	Long bankEvaluationId,
	@Schema(description = "Latest total risk points", example = "72")
	Integer totalRiskPoints,
	@Schema(description = "Risk level", example = "LOW")
	String riskLevel,
	@Schema(description = "Human-readable risk label", example = "Low")
	String riskLabel,
	@Schema(description = "Date and time of the latest evaluation")
	LocalDateTime evaluationDate
) {
}
