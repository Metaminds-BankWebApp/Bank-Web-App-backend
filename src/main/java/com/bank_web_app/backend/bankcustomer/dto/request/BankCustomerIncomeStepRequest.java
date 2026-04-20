package com.bank_web_app.backend.bankcustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Schema(name = "BankCustomerIncomeStepRequest", description = "Step 1 payload for saving bank customer income sources.")
public record BankCustomerIncomeStepRequest(
	@Schema(description = "Income sources captured in step 1.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Incomes list is required.")
	@Valid
	List<IncomeItem> incomes
) {
	@Schema(name = "BankCustomerIncomeItem", description = "Single income row in the income step.")
	public record IncomeItem(
		@Schema(description = "Income category", example = "SALARY", allowableValues = {"SALARY", "BUSINESS"}, requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "Income category is required.")
		String incomeCategory,
		@Schema(description = "Income amount", example = "125000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Income amount is required.")
		@Positive(message = "Income amount must be greater than 0.")
		BigDecimal amount,
		@Schema(description = "Salary type when category is SALARY", example = "FIXED")
		String salaryType,
		@Schema(description = "Employment type when category is SALARY", example = "PERMANENT")
		String employmentType,
		@Schema(description = "Contract duration in months", example = "12")
		@Min(value = 0, message = "Contract duration months cannot be negative.")
		Integer contractDurationMonths,
		@Schema(description = "Income stability when category is BUSINESS", example = "STABLE")
		String incomeStability
	) {
	}
}
