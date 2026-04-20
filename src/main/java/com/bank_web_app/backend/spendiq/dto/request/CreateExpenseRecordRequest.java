package com.bank_web_app.backend.spendiq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "CreateExpenseRecordRequest", description = "Request payload to create an expense record")
public record CreateExpenseRecordRequest(
	@NotNull
	@Schema(example = "10")
	Long categoryId,
	@NotNull
	@DecimalMin(value = "0.01")
	@Schema(example = "2500.00")
	BigDecimal amount,
	@NotNull
	@Schema(example = "2026-04-20")
	LocalDate expenseDate,
	@NotBlank
	@Schema(example = "CARD")
	String paymentType
) {
}