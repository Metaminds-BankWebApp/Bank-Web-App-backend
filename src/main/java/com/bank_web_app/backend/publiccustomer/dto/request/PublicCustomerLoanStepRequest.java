package com.bank_web_app.backend.publiccustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(name = "PublicCustomerLoanStepRequest", description = "Step 2 payload for saving public customer loan details.")
public record PublicCustomerLoanStepRequest(
	@Schema(description = "Loan entries captured in step 2.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Loans list is required.")
	@Valid
	List<LoanItem> loans
) {
	@Schema(name = "PublicCustomerLoanItem", description = "Single loan row in the loan step.")
	public record LoanItem(
		@Schema(description = "Loan type", example = "Housing Loan", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "Loan type is required.")
		String loanType,
		@Schema(description = "Monthly EMI value", example = "42500.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Monthly EMI is required.")
		@Min(value = 0, message = "Monthly EMI cannot be negative.")
		BigDecimal monthlyEmi,
		@Schema(description = "Remaining outstanding loan balance", example = "3200000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Remaining balance is required.")
		@Min(value = 0, message = "Remaining balance cannot be negative.")
		BigDecimal remainingBalance
	) {
	}
}
