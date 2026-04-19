package com.bank_web_app.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BankCustomerSummaryResponse", description = "Unified customer summary for officer/customer/admin listings")
public record BankCustomerSummaryResponse(
	@Schema(example = "42")
	Long userId,
	@Schema(example = "#C-00042")
	String customerId,
	@Schema(example = "Jane Doe")
	String fullName,
	@Schema(example = "199012345678")
	String nic,
	@Schema(example = "jane.doe@bank.com")
	String email,
	@Schema(example = "0771234567")
	String phone,
	@Schema(example = "ACTIVE")
	String status,
	@Schema(example = "2026-04-17T18:30:00")
	String lastUpdated
) {
}
