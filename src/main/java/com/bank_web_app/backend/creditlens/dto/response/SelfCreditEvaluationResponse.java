package com.bank_web_app.backend.creditlens.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SelfCreditEvaluationResponse(
	Long selfEvaluationId,
	Long publicCustomerId,
	Long publicRecordId,
	Integer totalRiskPoints,
	String riskLevel,
	String riskLabel,
	BigDecimal totalMonthlyIncome,
	BigDecimal totalMonthlyDebtPayment,
	BigDecimal totalCardLimit,
	BigDecimal totalCardOutstanding,
	BigDecimal dtiRatio,
	String dtiBand,
	BigDecimal creditUtilizationRatio,
	String creditUtilizationBand,
	Integer activeFacilitiesCount,
	Integer missedPaymentsCount,
	Integer paymentHistoryPoints,
	Integer dtiPoints,
	Integer utilizationPoints,
	Integer incomeStabilityPoints,
	Integer exposurePoints,
	Boolean reportGenerated,
	LocalDateTime createdAt,
	List<CreditRiskFactorResponse> factors
) {
}
