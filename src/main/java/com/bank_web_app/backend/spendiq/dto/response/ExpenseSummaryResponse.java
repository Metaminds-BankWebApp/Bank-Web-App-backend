package com.bank_web_app.backend.spendiq.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseSummaryResponse(
	Long accountId,
	String accountNumber,
	LocalDate from,
	LocalDate to,
	BigDecimal totalSpent,
	long totalTransactions,
	BigDecimal averageExpense,
	String topCategory,
	List<CategoryBreakdownResponse> categoryBreakdown,
	List<ExpenseResponse> recentExpenses
) {}
