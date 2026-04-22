package com.bank_web_app.backend.spendiq.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseRecordResponse(
	Long expenseId,
	Long userId,
	Long categoryId,
	String categoryName,
	BigDecimal amount,
	LocalDate expenseDate,
	String paymentType,
	LocalDateTime createdAt
) {
}