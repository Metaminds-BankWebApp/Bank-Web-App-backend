package com.bank_web_app.backend.publiccustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

// Step-4 request payload containing liabilities and missed-payment count.
@Schema(name = "PublicCustomerLiabilityStepRequest", description = "Step 4 payload for saving liabilities and missed payments.")
public record PublicCustomerLiabilityStepRequest(
	// Liability rows submitted in the liabilities step.
	@Schema(description = "Liability entries captured in step 4.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Liabilities list is required.")
	@Valid
	List<LiabilityItem> liabilities,
	// Total missed payments over the last twelve months.
	@Schema(description = "Aggregate missed payment count in the last 12 months", example = "2")
	@Min(value = 0, message = "Missed payments cannot be negative.")
	int missedPayments
) {
	// Single liability row included in step-4 submission.
	@Schema(name = "PublicCustomerLiabilityItem", description = "Single liability row in the liability step.")
	public record LiabilityItem(
		// Free-text description for this liability.
		@Schema(description = "Liability description", example = "Personal lease commitment", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "Liability description is required.")
		String description,
		// Monthly payment obligation amount for this liability.
		@Schema(description = "Monthly liability amount", example = "15000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Monthly amount is required.")
		@Min(value = 0, message = "Monthly amount cannot be negative.")
		BigDecimal monthlyAmount
	) {
	}
}
