package com.bank_web_app.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthMeResponse", description = "Authenticated user identity and ownership context")
public record AuthMeResponse(
	@Schema(example = "5")
	Long userId,
	@Schema(example = "john.doe@bank.com")
	String email,
	@Schema(example = "john.doe")
	String username,
	@Schema(example = "John Doe")
	String fullName,
	@Schema(example = "3")
	Long roleId,
	@Schema(example = "BANK_CUSTOMER")
	String roleName,
	@Schema(example = "101", nullable = true)
	Long bankCustomerId,
	@Schema(example = "202", nullable = true)
	Long publicCustomerId,
	@Schema(example = "303", nullable = true)
	Long officerId
) {
}