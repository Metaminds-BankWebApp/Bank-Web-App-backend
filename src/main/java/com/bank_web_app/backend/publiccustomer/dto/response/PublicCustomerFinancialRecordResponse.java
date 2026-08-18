package com.bank_web_app.backend.publiccustomer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Detailed financial snapshot returned for a public customer application record.
public record PublicCustomerFinancialRecordResponse(
	// Unique financial record id.
	Long recordId,
	// Owning public-customer id.
	Long publicCustomerId,
	// Record state (for example CURRENT or ARCHIVED).
	String recordStatus,
	// Record creation timestamp.
	LocalDateTime createdAt,
	// Most recent update timestamp.
	LocalDateTime updatedAt,
	// Income entries captured for this record.
	List<IncomeItem> incomes,
	// Loan entries captured for this record.
	List<LoanItem> loans,
	// Card entries captured for this record.
	List<CardItem> cards,
	// Liability entries captured for this record.
	List<LiabilityItem> liabilities,
	// Aggregate missed-payments count associated with this snapshot.
	int missedPayments
) {
	// One income row inside the financial record.
	public record IncomeItem(
		// Unique income row id.
		Long incomeId,
		// Income category/type.
		String incomeCategory,
		// Declared income amount.
		BigDecimal amount,
		// Salary type when category is salary-based.
		String salaryType,
		// Employment type when applicable.
		String employmentType,
		// Duration in months when provided.
		Integer durationMonths,
		// Business income stability marker when applicable.
		String incomeStability,
		// Income row creation timestamp.
		LocalDateTime createdAt
	) {
	}

	// One loan row inside the financial record.
	public record LoanItem(
		// Unique loan row id.
		Long loanId,
		// Loan type/product label.
		String loanType,
		// Monthly EMI amount.
		BigDecimal monthlyEmi,
		// Remaining loan balance.
		BigDecimal remainingBalance,
		// Loan row creation timestamp.
		LocalDateTime createdAt
	) {
	}

	// One card row inside the financial record.
	public record CardItem(
		// Unique card row id.
		Long cardId,
		// Card provider/bank name.
		String provider,
		// Approved card credit limit.
		BigDecimal creditLimit,
		// Current outstanding card amount.
		BigDecimal outstandingBalance,
		// Card row creation timestamp.
		LocalDateTime createdAt
	) {
	}

	// One liability row inside the financial record.
	public record LiabilityItem(
		// Unique liability row id.
		Long liabilityId,
		// Liability description text.
		String description,
		// Monthly liability payment amount.
		BigDecimal monthlyAmount,
		// Liability row creation timestamp.
		LocalDateTime createdAt
	) {
	}
}
