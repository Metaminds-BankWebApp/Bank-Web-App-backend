package com.bank_web_app.backend.publiccustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

// Step-1 request payload containing income sources for a public customer.
@Schema(name = "PublicCustomerIncomeStepRequest", description = "Step 1 payload for saving public customer income sources.")
public record PublicCustomerIncomeStepRequest(
	// Income rows submitted in the income step.
	@Schema(description = "Income sources captured in step 1.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Incomes list is required.")
	@Valid
	List<IncomeItem> incomes
) {
	// Single income source row included in step-1 submission.
	@Schema(name = "PublicCustomerIncomeItem", description = "Single income row in the income step.")
	public record IncomeItem(
		// Declared type/category of income source.
		@Schema(description = "Income category", example = "SALARY", allowableValues = {"SALARY", "BUSINESS"}, requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "Income category is required.")
		String incomeCategory,
		// Amount contributed by this income source.
		@Schema(description = "Income amount", example = "125000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Income amount is required.")
		@Positive(message = "Income amount must be greater than 0.")
		BigDecimal amount,
		// Salary structure when income category is SALARY.
		@Schema(
			description = "Salary type when category is SALARY",
			example = "FIXED",
			allowableValues = {"FIXED", "AVERAGE_VARIABLE"}
		)
		String salaryType,
		// Employment classification when category is SALARY.
		@Schema(
			description = "Employment type when category is SALARY",
			example = "PERMANENT",
			allowableValues = {"PERMANENT", "CONTRACT"}
		)
		String employmentType,
		// Duration in months applicable to contract salary entries.
		@Schema(description = "Contract duration in months; required for contract and unused for permanent employment", example = "12")
		@Min(value = 1, message = "Contract duration must be at least 1 month.")
		Integer durationMonths,
		// Stability indicator when income category is BUSINESS.
		@Schema(description = "Income stability when category is BUSINESS", example = "STABLE")
		String incomeStability
	) {
	}
}
