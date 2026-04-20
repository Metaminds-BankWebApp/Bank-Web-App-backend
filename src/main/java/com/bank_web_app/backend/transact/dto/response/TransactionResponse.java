package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "TransactionResponse", description = "Bank customer transaction details.")
public record TransactionResponse(
	@Schema(description = "Transaction id", example = "101")
	Long transactionId,
	@Schema(description = "Bank customer id", example = "7")
	Long bankCustomerId,
	@Schema(description = "Sender account number", example = "1002003004")
	String senderAccountNo,
	@Schema(description = "Receiver account number", example = "2003004005")
	String receiverAccountNo,
	@Schema(description = "Receiver name", example = "Kasun Perera")
	String receiverName,
	@Schema(description = "Amount", example = "12500.00")
	BigDecimal amount,
	@Schema(description = "Remark", example = "Invoice #INV-1002")
	String remark,
	@Schema(description = "Reference number", example = "TXN-20260420-9K4N2A")
	String referenceNo,
	@Schema(description = "Transaction status", example = "SUCCESS")
	String status,
	@Schema(description = "Whether OTP was verified", example = "true")
	Boolean otpVerified,
	@Schema(description = "Expense tracking enabled flag", example = "false")
	Boolean expenseTrackingEnabled,
	@Schema(description = "Failure reason when status is FAILED", example = "Insufficient balance.")
	String failureReason,
	@Schema(description = "Transaction date/time", example = "2026-04-20T11:00:00")
	LocalDateTime transactionDate
) {
}
