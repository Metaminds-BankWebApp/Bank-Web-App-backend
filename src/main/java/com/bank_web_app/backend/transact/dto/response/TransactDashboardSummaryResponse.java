package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Consolidated transact dashboard payload returned to authenticated customers.
@Schema(name = "TransactDashboardSummaryResponse", description = "Full dashboard payload for logged-in bank customer transact dashboard.")
public record TransactDashboardSummaryResponse(
	// Customer account number used as dashboard identity.
	@Schema(description = "Bank customer's own account number.", example = "1000000001")
	String accountNumber,
	// Current available balance shown on dashboard.
	@Schema(description = "Live current balance of the customer's own account.", example = "125000.75")
	BigDecimal currentBalance,
	// Total number of linked transactions for this account.
	@Schema(description = "Total number of transactions linked to this account (sent or received).", example = "42")
	long totalTransactions,
	// Sum of successful outgoing transfer amounts.
	@Schema(description = "Total successful sent amount from this account.", example = "356000.00")
	BigDecimal totalSent,
	// Sum of successful incoming transfer amounts.
	@Schema(description = "Total successful received amount into this account.", example = "214500.00")
	BigDecimal totalReceived,
	// Monthly timeline data used by chart widgets.
	@Schema(description = "Timeline data for the last 12 months.")
	TransactionTimeline timeline,
	// Breakdown of transaction statuses.
	@Schema(description = "Transaction status counts based on bank_customer_transactions table.")
	TransactionStatusSummary transactionStatus,
	// Breakdown of OTP processing statuses.
	@Schema(description = "OTP status counts based on transaction_otp_logs table.")
	OtpStatusSummary otpStatus,
	// Total beneficiary records saved by this customer.
	@Schema(description = "Number of saved beneficiaries for this customer.", example = "6")
	long savedBeneficiaries,
	// Compact list of latest transaction rows for dashboard preview.
	@ArraySchema(schema = @Schema(implementation = RecentTransactionItem.class), arraySchema = @Schema(description = "Most recent transactions (sent/received)."))
	List<RecentTransactionItem> recentTransactions
) {
	// Timeline structure used to render monthly transaction trend chart.
	@Schema(name = "TransactDashboardTimeline", description = "Transaction timeline for chart rendering.")
	public record TransactionTimeline(
		// X-axis labels (typically month short names).
		@ArraySchema(schema = @Schema(example = "MAY"), arraySchema = @Schema(description = "Month labels in chronological order."))
		List<String> labels,
		// Y-axis values mapped to each label.
		@ArraySchema(schema = @Schema(example = "45000.00"), arraySchema = @Schema(description = "Transaction amount totals for each label."))
		List<BigDecimal> values
	) {
	}

	// Count summary of transaction status categories.
	@Schema(name = "TransactDashboardTransactionStatusSummary", description = "Transaction status breakdown from bank_customer_transactions table.")
	public record TransactionStatusSummary(
		// Number of transactions completed successfully.
		@Schema(example = "18")
		long successCount,
		// Number of transactions ended as failed.
		@Schema(example = "2")
		long failedCount,
		// Number of transactions waiting for OTP verification.
		@Schema(example = "1")
		long pendingOtpCount,
		// Number of transactions cancelled before completion.
		@Schema(example = "0")
		long cancelledCount
	) {
	}

	// Count summary of OTP processing lifecycle statuses.
	@Schema(name = "TransactDashboardOtpStatusSummary", description = "OTP status breakdown from transaction_otp_logs table.")
	public record OtpStatusSummary(
		// Total OTP codes sent.
		@Schema(example = "9")
		long sentCount,
		// Total OTP codes verified successfully.
		@Schema(example = "7")
		long verifiedCount,
		// Total OTP codes expired before use.
		@Schema(example = "1")
		long expiredCount,
		// Total OTP verification attempts that failed.
		@Schema(example = "1")
		long failedCount
	) {
	}

	// Lightweight transaction row used for recent-activity section.
	@Schema(name = "TransactDashboardRecentTransactionItem", description = "Recent transaction row for dashboard quick view.")
	public record RecentTransactionItem(
		// Internal transaction id.
		@Schema(example = "101")
		Long transactionId,
		// Customer-facing unique reference number.
		@Schema(example = "TXN-20260429101530-AB12CD")
		String referenceNo,
		// Date-time when transaction was created/recorded.
		@Schema(example = "2026-04-29T10:15:30")
		LocalDateTime transactionDate,
		// Direction relative to logged-in account (e.g., SENT/RECEIVED).
		@Schema(example = "SENT")
		String direction,
		// Other party account number in this transaction.
		@Schema(example = "1000000007")
		String counterpartyAccountNo,
		// Other party display name in this transaction.
		@Schema(example = "Yash")
		String counterpartyName,
		// Transaction amount.
		@Schema(example = "25000.00")
		BigDecimal amount,
		// Current transaction status label.
		@Schema(example = "SUCCESS")
		String status,
		// Optional remark associated with the transaction.
		@Schema(example = "Utility bill")
		String remark
	) {
	}
}
