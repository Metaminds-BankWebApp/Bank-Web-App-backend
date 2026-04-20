package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "TransactionInitiateResponse", description = "Response after initiating transaction and generating OTP.")
public record TransactionInitiateResponse(
	@Schema(description = "Transaction id", example = "101")
	Long transactionId,
	@Schema(description = "Transaction reference number", example = "TXN-20260420-9K4N2A")
	String referenceNo,
	@Schema(description = "Current transaction status", example = "PENDING_OTP")
	String status,
	@Schema(description = "OTP delivery target email", example = "customer.demo@primecore.local")
	String sentToEmail,
	@Schema(description = "OTP expiry timestamp", example = "2026-04-20T11:10:00")
	LocalDateTime otpExpiresAt,
	@Schema(description = "Operation message", example = "Transaction created. OTP has been issued for verification.")
	String message
) {
}
