package com.bank_web_app.backend.crib.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CribDatasetSnapshotResponse(
	String nic,
	String sourceDatabase,
	LocalDateTime retrievedAt,
	Integer creditScore,
	Integer inquiryCount,
	Integer activeLoansCount,
	BigDecimal totalActiveLoanValue,
	Integer missedPaymentsLast12Months,
	String suitabilitySummary,
	List<CribLoanItem> loans,
	List<CribCardItem> creditCards,
	List<CribLiabilityItem> liabilities
) {
	public record CribLoanItem(
		String loanType,
		BigDecimal monthlyEmi,
		BigDecimal remainingBalance
	) {
	}

	public record CribCardItem(
		String provider,
		BigDecimal creditLimit,
		BigDecimal outstandingBalance
	) {
	}

	public record CribLiabilityItem(
		String description,
		BigDecimal monthlyAmount
	) {
	}
}