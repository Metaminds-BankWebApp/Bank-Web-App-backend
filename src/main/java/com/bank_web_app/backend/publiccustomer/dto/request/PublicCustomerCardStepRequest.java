package com.bank_web_app.backend.publiccustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(name = "PublicCustomerCardStepRequest", description = "Step 3 payload for saving public customer card details.")
public record PublicCustomerCardStepRequest(
	@Schema(description = "Card entries captured in step 3.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Cards list is required.")
	@Valid
	List<CardItem> cards
) {
	@Schema(name = "PublicCustomerCardItem", description = "Single card row in the card step.")
	public record CardItem(
		@Schema(description = "Card provider name", example = "HSBK Platinum Visa")
		String provider,
		@Schema(description = "Credit card limit", example = "250000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Credit limit is required.")
		@Min(value = 0, message = "Credit limit cannot be negative.")
		BigDecimal creditLimit,
		@Schema(description = "Current outstanding card balance", example = "65000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Outstanding balance is required.")
		@Min(value = 0, message = "Outstanding balance cannot be negative.")
		BigDecimal outstandingBalance
	) {
	}
}
