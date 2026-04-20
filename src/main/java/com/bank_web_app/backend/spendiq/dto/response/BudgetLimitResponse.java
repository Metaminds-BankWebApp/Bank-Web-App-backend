package com.bank_web_app.backend.spendiq.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BudgetLimitResponse(
	Long budgetId,
	Long userId,
	Long categoryId,
	String categoryName,
	BigDecimal budgetAmount,
	Integer month,
	Integer year,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}