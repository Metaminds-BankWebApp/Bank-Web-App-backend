package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

// Response returned immediately after initiating a transfer and creating OTP.
@Schema(name = "TransactionInitiateResponse", description = "Response after initiating transaction and generating OTP.")
public record TransactionInitiateResponse(
	// Internal transaction id for the initiated transfer.
	@Schema(description = "Transaction id", example = "101")
	Long transactionId,
	// Unique transaction reference shared with client.
	@Schema(description = "Transaction reference number", example = "TXN-20260420-9K4N2A")
	String referenceNo,
	// Current transfer status after initiation (typically PENDING_OTP).
	@Schema(description = "Current transaction status", example = "PENDING_OTP", allowableValues = {
		"PENDING_OTP",
		"SUCCESS",
		"FAILED",
		"CANCELLED"
	})
	String status,
	// Email address where OTP notification was sent.
	@Schema(description = "OTP delivery target email", example = "customer.demo@primecore.local")
	String sentToEmail,
	// Expiry timestamp for the issued OTP.
	@Schema(description = "OTP expiry timestamp", example = "2026-04-20T11:10:00")
	LocalDateTime otpExpiresAt,
	// Human-readable status message for client UI.
	@Schema(description = "Operation message", example = "Transaction created. OTP has been issued for verification.")
	String message
) {
}
