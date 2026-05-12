package com.bank_web_app.backend.creditlens.service;

import java.math.BigDecimal;

record EvaluationMetrics(
	int totalRiskPoints,
	String riskLevel,
	BigDecimal totalMonthlyIncome,
	BigDecimal totalMonthlyDebtPayment,
	BigDecimal totalCardLimit,
	BigDecimal totalCardOutstanding,
	BigDecimal dtiRatio,
	BigDecimal creditUtilizationRatio,
	int activeFacilitiesCount,
	int missedPaymentsCount,
	int paymentHistoryPoints,
	int dtiPoints,
	int utilizationPoints,
	int incomeStabilityPoints,
	int exposurePoints
) {
}
