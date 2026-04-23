package com.bank_web_app.backend.spendiq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "CreateIncomeRecordRequest", description = "Request payload to create an income record")
public record CreateIncomeRecordRequest(
	@NotBlank
	@Schema(example = "Salary")
	String sourceName,
	@NotNull
	@DecimalMin(value = "0.01")
	@Digits(integer = 13, fraction = 2)
	@Schema(example = "145000.00")
	BigDecimal amount,
	@NotNull
	@Schema(example = "2026-04-01")
	LocalDate incomeDate
) {
}
