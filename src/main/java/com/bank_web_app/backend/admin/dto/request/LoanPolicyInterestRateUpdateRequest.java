package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(name = "LoanPolicyInterestRateUpdateRequest", description = "Payload for updating a loan policy interest rate")
public record LoanPolicyInterestRateUpdateRequest(
	@Schema(example = "1")
	@NotNull(message = "Policy id is required.")
	Long policyId,

	@Schema(example = "17.00")
	@NotNull(message = "Base interest rate is required.")
	@DecimalMin(value = "0.00", message = "Base interest rate must not be negative.")
	@DecimalMax(value = "100.00", message = "Base interest rate must not exceed 100.")
	BigDecimal baseInterestRate
) {
}
