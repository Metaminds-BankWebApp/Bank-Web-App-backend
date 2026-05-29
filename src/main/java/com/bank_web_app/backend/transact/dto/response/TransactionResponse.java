package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Detailed transaction payload returned for history and lookup endpoints.
@Schema(name = "TransactionResponse", description = "Bank customer transaction details.")
public record TransactionResponse(
	// Internal transaction id.
	@Schema(description = "Transaction id", example = "101")
	Long transactionId,
	// Owner bank-customer id linked to this transaction row.
	@Schema(description = "Bank customer id", example = "7")
	Long bankCustomerId,
	// Sender account number.
	@Schema(description = "Sender account number", example = "1002003004")
	String senderAccountNo,
	// Receiver account number.
	@Schema(description = "Receiver account number", example = "2003004005")
	String receiverAccountNo,
	// Receiver display name captured with transaction details.
	@Schema(description = "Receiver name", example = "Kasun Perera")
	String receiverName,
	// Monetary transaction amount.
	@Schema(description = "Amount", example = "12500.00")
	BigDecimal amount,
	// Optional transaction remark or note.
	@Schema(description = "Remark", example = "Invoice #INV-1002")
	String remark,
	// Customer-facing transaction reference number.
	@Schema(description = "Reference number", example = "TXN-20260420-9K4N2A")
	String referenceNo,
	// Transaction processing status.
	@Schema(description = "Transaction status", example = "SUCCESS", allowableValues = {
		"PENDING_OTP",
		"SUCCESS",
		"FAILED",
		"CANCELLED"
	})
	String status,
	// Indicates whether OTP verification was completed.
	@Schema(description = "Whether OTP was verified", example = "true")
	Boolean otpVerified,
	// Indicates whether expense tracking integration was requested.
	@Schema(description = "Expense tracking enabled flag", example = "false")
	Boolean expenseTrackingEnabled,
	// Optional failure reason when status is FAILED.
	@Schema(description = "Failure reason when status is FAILED", example = "Insufficient balance.")
	String failureReason,
	// Timestamp when transaction was recorded.
	@Schema(description = "Transaction date/time", example = "2026-04-20T11:00:00")
	LocalDateTime transactionDate
) {
}
