package com.bank_web_app.backend.loansense.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record LoanSenseLoanInputRequest(
	@NotBlank(message = "Loan type is required.")
	String loanType,

	@DecimalMin(value = "0.00", message = "Asset value must not be negative.")
	BigDecimal assetValue
) {
}
