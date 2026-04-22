package com.bank_web_app.backend.publiccustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PublicCustomerFinancialStepResponse", description = "Response returned after saving one financial step.")
public record PublicCustomerFinancialStepResponse(
	@Schema(description = "Financial record id", example = "12")
	Long recordId,
	@Schema(description = "Public customer id", example = "7")
	Long publicCustomerId,
	@Schema(description = "Saved step identifier", example = "INCOME")
	String step,
	@Schema(description = "Operation result message", example = "Income step saved successfully.")
	String message
) {
}
