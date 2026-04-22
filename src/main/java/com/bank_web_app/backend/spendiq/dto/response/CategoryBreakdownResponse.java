package com.bank_web_app.backend.spendiq.dto.response;

import java.math.BigDecimal;

public record CategoryBreakdownResponse(
	String category,
	BigDecimal totalAmount,
	long expenseCount
) {}
