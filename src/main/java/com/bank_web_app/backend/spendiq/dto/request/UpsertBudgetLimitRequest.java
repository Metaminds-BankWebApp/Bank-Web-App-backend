package com.bank_web_app.backend.spendiq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(name = "UpsertBudgetLimitRequest", description = "Request payload to create or update a budget limit")
public record UpsertBudgetLimitRequest(
	@NotNull
	@Schema(example = "10")
	Long categoryId,
	@NotNull
	@DecimalMin(value = "0.01")
	@Schema(example = "50000.00")
	BigDecimal budgetAmount,
	@NotNull
	@Min(1)
	@Max(12)
	@Schema(example = "4")
	Integer month,
	@NotNull
	@Min(2000)
	@Max(3000)
	@Schema(example = "2026")
	Integer year
) {
}