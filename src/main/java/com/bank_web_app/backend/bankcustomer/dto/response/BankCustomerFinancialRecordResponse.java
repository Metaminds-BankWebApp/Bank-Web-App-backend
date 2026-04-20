package com.bank_web_app.backend.bankcustomer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BankCustomerFinancialRecordResponse(
	Long bankRecordId,
	Long bankCustomerId,
	Long verifiedByOfficerId,
	String dataSource,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<IncomeItem> incomes,
	List<LoanItem> loans,
	List<CardItem> cards,
	List<LiabilityItem> liabilities,
	int missedPayments
) {
	public record IncomeItem(
		Long incomeId,
		String incomeCategory,
		BigDecimal amount,
		String salaryType,
		String employmentType,
		Integer contractDurationMonths,
		String incomeStability,
		LocalDateTime createdAt
	) {
	}

	public record LoanItem(
		Long loanId,
		String loanType,
		BigDecimal monthlyEmi,
		BigDecimal remainingBalance,
		LocalDateTime createdAt
	) {
	}

	public record CardItem(
		Long cardId,
		String provider,
		BigDecimal creditLimit,
		BigDecimal outstandingBalance,
		LocalDateTime createdAt
	) {
	}

	public record LiabilityItem(
		Long liabilityId,
		String description,
		BigDecimal monthlyAmount,
		LocalDateTime createdAt
	) {
	}
}
