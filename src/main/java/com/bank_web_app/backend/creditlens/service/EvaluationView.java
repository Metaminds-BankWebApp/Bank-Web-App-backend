package com.bank_web_app.backend.creditlens.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record EvaluationView(
	Long evaluationId,
	Long recordId,
	String scope,
	String evaluationType,
	Integer totalRiskPoints,
	String riskLevel,
	BigDecimal totalMonthlyIncome,
	BigDecimal totalMonthlyDebtPayment,
	BigDecimal totalCardLimit,
	BigDecimal totalCardOutstanding,
	BigDecimal dtiRatio,
	BigDecimal creditUtilizationRatio,
	Integer activeFacilitiesCount,
	Integer missedPaymentsCount,
	Integer paymentHistoryPoints,
	Integer dtiPoints,
	Integer utilizationPoints,
	Integer incomeStabilityPoints,
	Integer exposurePoints,
	LocalDateTime createdAt
) {
}
