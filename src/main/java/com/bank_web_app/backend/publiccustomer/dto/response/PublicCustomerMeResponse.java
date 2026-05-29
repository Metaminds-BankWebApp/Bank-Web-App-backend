package com.bank_web_app.backend.publiccustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// Response payload describing logged-in public customer identity mapping.
@Schema(name = "PublicCustomerMeResponse", description = "Resolved profile details for the logged-in PUBLIC_CUSTOMER user.")
public record PublicCustomerMeResponse(
	// Public-customer profile id linked to the authenticated user.
	@Schema(description = "Public customer profile id", example = "17")
	Long publicCustomerId,
	// User table id of the authenticated principal.
	@Schema(description = "User id in users table", example = "42")
	Long userId,
	// Human-readable public customer code.
	@Schema(description = "Public customer code", example = "PC-00017")
	String customerCode
) {
}
