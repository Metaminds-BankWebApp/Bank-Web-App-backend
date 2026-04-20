package com.bank_web_app.backend.spendiq.dto.response;

import java.math.BigDecimal;

public record SpendIqMonthlySummaryResponse(
	Integer month,
	Integer year,
	BigDecimal totalIncome,
	BigDecimal totalExpense,
	BigDecimal totalBudget,
	BigDecimal netSavings,
	BigDecimal remainingBudget,
	BigDecimal budgetUsagePercentage
) {
}