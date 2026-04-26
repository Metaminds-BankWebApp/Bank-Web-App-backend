package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
	name = "AdminUserManagementUserResponse",
	description = "Admin user-management row for BANK_CUSTOMER and PUBLIC_CUSTOMER users."
)
public record AdminUserManagementUserResponse(
	@Schema(example = "42")
	Long userId,
	@Schema(example = "BC-000042")
	String customerId,
	@Schema(example = "Jane Doe")
	String fullName,
	@Schema(example = "jane.doe@primecore.com")
	String email,
	@Schema(example = "0771234567")
	String contactNumber,
	@Schema(example = "2026-04-20T10:15:30")
	String joinedDate,
	@Schema(example = "BANK")
	String customerType,
	@Schema(example = "ACTIVE")
	String status,
	@Schema(example = "https://cdn.example.com/profiles/jane.png")
	String avatarUrl
) {
}
