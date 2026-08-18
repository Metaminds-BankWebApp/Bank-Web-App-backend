package com.bank_web_app.backend.publiccustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// Response payload returned after saving one step of financial information.
@Schema(name = "PublicCustomerFinancialStepResponse", description = "Response returned after saving one financial step.")
public record PublicCustomerFinancialStepResponse(
	// Id of the financial record being updated.
	@Schema(description = "Financial record id", example = "12")
	Long recordId,
	// Public customer id owning the financial record.
	@Schema(description = "Public customer id", example = "7")
	Long publicCustomerId,
	// Identifier of the step that was saved.
	@Schema(description = "Saved step identifier", example = "INCOME")
	String step,
	// User-facing operation result message.
	@Schema(description = "Operation result message", example = "Income step saved successfully.")
	String message
) {
}
