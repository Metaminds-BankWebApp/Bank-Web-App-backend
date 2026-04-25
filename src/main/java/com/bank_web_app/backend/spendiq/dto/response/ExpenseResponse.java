package com.bank_web_app.backend.spendiq.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
	Long expenseId,
	Long accountId,
	String accountNumber,
	String title,
	String category,
	BigDecimal amount,
	LocalDate expenseDate,
	String notes,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {}
