package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "BankCreditEvaluationResponse", description = "Detailed bank customer credit evaluation payload.")
public record BankCreditEvaluationResponse(
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
	@Schema(description = "Optional officer remarks", example = "Customer is improving after recent repayment.", nullable = true)
	String remarks,
	@Schema(description = "Total risk points", example = "72")
	Integer totalRiskPoints,
	@Schema(description = "Risk level", example = "LOW")
	String riskLevel,
	@Schema(description = "Human-readable risk label", example = "Low")
	String riskLabel,
	@Schema(description = "Total monthly income")
	BigDecimal totalMonthlyIncome,
	@Schema(description = "Total monthly debt payment")
	BigDecimal totalMonthlyDebtPayment,
	@Schema(description = "Total card limit")
	BigDecimal totalCardLimit,
	@Schema(description = "Total card outstanding")
	BigDecimal totalCardOutstanding,
	@Schema(description = "Debt-to-income ratio")
	BigDecimal dtiRatio,
	@Schema(description = "Debt-to-income band", example = "Medium")
	String dtiBand,
	@Schema(description = "Credit utilization ratio")
	BigDecimal creditUtilizationRatio,
	@Schema(description = "Credit utilization band", example = "High")
	String creditUtilizationBand,
	@Schema(description = "Count of active facilities", example = "4")
	Integer activeFacilitiesCount,
	@Schema(description = "Count of missed payments", example = "2")
	Integer missedPaymentsCount,
	@Schema(description = "Payment history score points", example = "18")
	Integer paymentHistoryPoints,
	@Schema(description = "DTI score points", example = "12")
	Integer dtiPoints,
	@Schema(description = "Utilization score points", example = "20")
	Integer utilizationPoints,
	@Schema(description = "Income stability score points", example = "0")
	Integer incomeStabilityPoints,
	@Schema(description = "Exposure score points", example = "5")
	Integer exposurePoints,
	@Schema(description = "Whether the report was generated", example = "true")
	Boolean reportGenerated,
	@Schema(description = "Evaluation date and time")
	LocalDateTime createdAt,
	@Schema(description = "Risk factor breakdown for this evaluation")
	List<CreditRiskFactorResponse> factors
) {
}
