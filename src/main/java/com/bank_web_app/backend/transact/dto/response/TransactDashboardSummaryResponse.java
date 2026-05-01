package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "TransactDashboardSummaryResponse", description = "Full dashboard payload for logged-in bank customer transact dashboard.")
public record TransactDashboardSummaryResponse(
	@Schema(description = "Bank customer's own account number.", example = "1000000001")
	String accountNumber,
	@Schema(description = "Live current balance of the customer's own account.", example = "125000.75")
	BigDecimal currentBalance,
	@Schema(description = "Total number of transactions linked to this account (sent or received).", example = "42")
	long totalTransactions,
	@Schema(description = "Total successful sent amount from this account.", example = "356000.00")
	BigDecimal totalSent,
	@Schema(description = "Total successful received amount into this account.", example = "214500.00")
	BigDecimal totalReceived,
	@Schema(description = "Timeline data for the last 12 months.")
	TransactionTimeline timeline,
	@Schema(description = "Transaction status counts based on bank_customer_transactions table.")
	TransactionStatusSummary transactionStatus,
	@Schema(description = "OTP status counts based on transaction_otp_logs table.")
	OtpStatusSummary otpStatus,
	@Schema(description = "Number of saved beneficiaries for this customer.", example = "6")
	long savedBeneficiaries,
	@ArraySchema(schema = @Schema(implementation = RecentTransactionItem.class), arraySchema = @Schema(description = "Most recent transactions (sent/received)."))
	List<RecentTransactionItem> recentTransactions
) {
	@Schema(name = "TransactDashboardTimeline", description = "Transaction timeline for chart rendering.")
	public record TransactionTimeline(
		@ArraySchema(schema = @Schema(example = "MAY"), arraySchema = @Schema(description = "Month labels in chronological order."))
		List<String> labels,
		@ArraySchema(schema = @Schema(example = "45000.00"), arraySchema = @Schema(description = "Transaction amount totals for each label."))
		List<BigDecimal> values
	) {
	}

	@Schema(name = "TransactDashboardTransactionStatusSummary", description = "Transaction status breakdown from bank_customer_transactions table.")
	public record TransactionStatusSummary(
		@Schema(example = "18")
		long successCount,
		@Schema(example = "2")
		long failedCount,
		@Schema(example = "1")
		long pendingOtpCount,
		@Schema(example = "0")
		long cancelledCount
	) {
	}

	@Schema(name = "TransactDashboardOtpStatusSummary", description = "OTP status breakdown from transaction_otp_logs table.")
	public record OtpStatusSummary(
		@Schema(example = "9")
		long sentCount,
		@Schema(example = "7")
		long verifiedCount,
		@Schema(example = "1")
		long expiredCount,
		@Schema(example = "1")
		long failedCount
	) {
	}

	@Schema(name = "TransactDashboardRecentTransactionItem", description = "Recent transaction row for dashboard quick view.")
	public record RecentTransactionItem(
		@Schema(example = "101")
		Long transactionId,
		@Schema(example = "TXN-20260429101530-AB12CD")
		String referenceNo,
		@Schema(example = "2026-04-29T10:15:30")
		LocalDateTime transactionDate,
		@Schema(example = "SENT")
		String direction,
		@Schema(example = "1000000007")
		String counterpartyAccountNo,
		@Schema(example = "Yash")
		String counterpartyName,
		@Schema(example = "25000.00")
		BigDecimal amount,
		@Schema(example = "SUCCESS")
		String status,
		@Schema(example = "Utility bill")
		String remark
	) {
	}
}
