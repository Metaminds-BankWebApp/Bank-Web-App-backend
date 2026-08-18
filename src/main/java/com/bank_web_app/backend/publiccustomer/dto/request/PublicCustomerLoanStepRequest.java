package com.bank_web_app.backend.publiccustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

// Step-2 request payload containing loan obligations for a public customer.
@Schema(name = "PublicCustomerLoanStepRequest", description = "Step 2 payload for saving public customer loan details.")
public record PublicCustomerLoanStepRequest(
	// Loan rows submitted in the loan step.
	@Schema(description = "Loan entries captured in step 2.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Loans list is required.")
	@Valid
	List<LoanItem> loans
) {
	// Single loan row included in step-2 submission.
	@Schema(name = "PublicCustomerLoanItem", description = "Single loan row in the loan step.")
	public record LoanItem(
		// Loan category/product name.
		@Schema(description = "Loan type", example = "Housing Loan", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "Loan type is required.")
		String loanType,
		// Current monthly EMI for this loan.
		@Schema(description = "Monthly EMI value", example = "42500.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Monthly EMI is required.")
		@Min(value = 0, message = "Monthly EMI cannot be negative.")
		BigDecimal monthlyEmi,
		// Remaining principal/outstanding value for this loan.
		@Schema(description = "Remaining outstanding loan balance", example = "3200000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Remaining balance is required.")
		@Min(value = 0, message = "Remaining balance cannot be negative.")
		BigDecimal remainingBalance
	) {
	}
}
