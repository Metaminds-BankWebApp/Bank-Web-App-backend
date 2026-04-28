package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminBankOfficerSummaryResponse", description = "Summary row for admin bank officer management table.")
public record AdminBankOfficerSummaryResponse(
	@Schema(example = "5")
	Long userId,
	@Schema(example = "EMP-BO-00005")
	String employeeCode,
	@Schema(example = "Lila Doe")
	String fullName,
	@Schema(example = "lila.doe@bank.com")
	String email,
	@Schema(example = "0771234567")
	String phone,
	@Schema(example = "ACTIVE")
	String status,
	@Schema(example = "2026-04-24T08:30:00")
	String createdAt,
	@Schema(example = "2026-04-24T10:15:30")
	String lastUpdated,
	@Schema(example = "2")
	Long branchId,
	@Schema(example = "Colombo Main")
	String branchName
) {
}
