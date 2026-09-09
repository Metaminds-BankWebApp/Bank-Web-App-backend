package com.bank_web_app.backend.bankcustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(name = "BankCustomerLoanStepRequest", description = "Step 2 payload for saving bank customer loan details.")
public record BankCustomerLoanStepRequest(
	@Schema(description = "Loan entries captured in step 2.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Loans list is required.")
	@Valid
	List<LoanItem> loans
) {
	@Schema(name = "BankCustomerLoanItem", description = "Single loan row in the loan step.")
	public record LoanItem(
		@Schema(description = "Loan type", example = "Housing Loan", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "Loan type is required.")
		String loanType,
		@Schema(description = "Monthly EMI value", example = "42500.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Monthly EMI is required.")
		@DecimalMin(value = "1.00", message = "Monthly EMI must be at least 1.")
		BigDecimal monthlyEmi,
		@Schema(description = "Remaining outstanding loan balance", example = "3200000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Remaining balance is required.")
		@DecimalMin(value = "1.00", message = "Remaining balance must be at least 1.")
		BigDecimal remainingBalance
	) {
	}
}
