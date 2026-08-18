package com.bank_web_app.backend.publiccustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

// Step-3 request payload containing card obligations for a public customer.
@Schema(name = "PublicCustomerCardStepRequest", description = "Step 3 payload for saving public customer card details.")
public record PublicCustomerCardStepRequest(
	// Card rows submitted in the card step.
	@Schema(description = "Card entries captured in step 3.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Cards list is required.")
	@Valid
	List<CardItem> cards
) {
	// Single card item row included in step-3 submission.
	@Schema(name = "PublicCustomerCardItem", description = "Single card row in the card step.")
	public record CardItem(
		// Name of issuing provider/bank and card product.
		@Schema(description = "Card provider name", example = "HSBK Platinum Visa")
		@NotBlank(message = "Card provider is required.")
		@Size(max = 100, message = "Card provider must not exceed 100 characters.")
		String provider,
		// Card limit assigned by issuer.
		@Schema(description = "Credit card limit", example = "250000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Credit limit is required.")
		@Min(value = 0, message = "Credit limit cannot be negative.")
		BigDecimal creditLimit,
		// Current unpaid amount for this card.
		@Schema(description = "Current outstanding card balance", example = "65000.00", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "Outstanding balance is required.")
		@Min(value = 0, message = "Outstanding balance cannot be negative.")
		BigDecimal outstandingBalance
	) {
	}
}
