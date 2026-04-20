package com.bank_web_app.backend.publiccustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PublicCustomerMeResponse", description = "Resolved profile details for the logged-in PUBLIC_CUSTOMER user.")
public record PublicCustomerMeResponse(
	@Schema(description = "Public customer profile id", example = "17")
	Long publicCustomerId,
	@Schema(description = "User id in users table", example = "42")
	Long userId,
	@Schema(description = "Public customer code", example = "PC-00017")
	String customerCode
) {
}