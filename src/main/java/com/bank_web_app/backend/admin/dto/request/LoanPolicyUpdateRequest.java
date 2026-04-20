package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(name = "LoanPolicyUpdateRequest", description = "Payload for updating a LoanSense loan policy")
public record LoanPolicyUpdateRequest(
	@Schema(example = "PERSONAL")
	@NotBlank(message = "Loan type is required.")
	@Size(max = 30, message = "Loan type must not exceed 30 characters.")
	String loanType,

	@Schema(example = "0.4000")
	@NotNull(message = "Max DBR ratio is required.")
	@DecimalMin(value = "0.0001", message = "Max DBR ratio must be greater than 0.")
	@DecimalMax(value = "1.0000", message = "Max DBR ratio must not exceed 1.")
	BigDecimal maxDbrRatio,

	@Schema(example = "17.00")
	@NotNull(message = "Base interest rate is required.")
	@DecimalMin(value = "0.00", message = "Base interest rate must not be negative.")
	@DecimalMax(value = "100.00", message = "Base interest rate must not exceed 100.")
	BigDecimal baseInterestRate,

	@Schema(example = "60")
	@NotNull(message = "Max tenure months is required.")
	@Min(value = 1, message = "Max tenure months must be at least 1.")
	Integer maxTenureMonths,

	@Schema(example = "21")
	@NotNull(message = "Minimum age is required.")
	@Min(value = 18, message = "Minimum age must be at least 18.")
	Integer minAge,

	@Schema(example = "60")
	@NotNull(message = "Maximum age is required.")
	@Min(value = 18, message = "Maximum age must be at least 18.")
	Integer maxAge,

	@Schema(example = "80.00")
	@DecimalMin(value = "0.00", message = "Max finance percentage must not be negative.")
	@DecimalMax(value = "100.00", message = "Max finance percentage must not exceed 100.")
	BigDecimal maxFinancePercentage,

	@Schema(example = "50000.00")
	@DecimalMin(value = "0.00", message = "Minimum income required must not be negative.")
	BigDecimal minIncomeRequired,

	@Schema(example = "ACTIVE")
	@Size(max = 20, message = "Status must not exceed 20 characters.")
	String status
) {
}
