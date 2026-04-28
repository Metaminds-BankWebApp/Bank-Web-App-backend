package com.bank_web_app.backend.publiccustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PublicCustomerCardProviderOptionResponse", description = "Single card provider option for public customer card form dropdown.")
public record PublicCustomerCardProviderOptionResponse(
	@Schema(description = "Card provider / bank name.", example = "Commercial Bank")
	String provider
) {
}
