package com.bank_web_app.backend.spendiq.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncomeRecordResponse(
	Long incomeId,
	Long userId,
	String sourceName,
	BigDecimal amount,
	LocalDate incomeDate,
	LocalDateTime createdAt
) {
}