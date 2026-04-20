package com.bank_web_app.backend.bankcustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(name = "BankCustomerLiabilityStepRequest", description = "Step 4 payload for saving liabilities and missed payments.")
public record BankCustomerLiabilityStepRequest(
	@Schema(description = "Liability entries captured in step 4.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Liabilities list is required.")
	@Valid
	List<LiabilityItem> liabilities,
	@Schema(description = "Aggregate missed payment count in the last 12 months", example = "2")
	@Min(value = 0, message = "Missed payments cannot be negative.")
	int missedPayments
) {
	@Schema(name = "BankCustomerLiabilityItem", description = "Single liability row in the liability step.")
	public record LiabilityItem(
		@Schema(description = "Liability description", example = "Personal lease commitment", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "Liability description is required.")
		String description,
		@Schema(description = "Monthly liability amount", example = "15000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Monthly amount is required.")
		@Min(value = 0, message = "Monthly amount cannot be negative.")
		BigDecimal monthlyAmount
	) {
	}
}
