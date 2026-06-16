package com.bank_web_app.backend.bankofficer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BankOfficerCustomerSummaryResponse", description = "Customer summary used by the bank officer all-customers list with backend-owned risk data.")
public record BankOfficerCustomerSummaryResponse(
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
	@Schema(example = "LOW")
	String riskLevel,
	@Schema(example = "680")
	Integer creditScore,
	@Schema(example = "2026-04-17T18:30:00")
	String lastUpdated
) {
}