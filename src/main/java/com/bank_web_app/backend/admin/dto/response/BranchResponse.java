package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BranchResponse", description = "Branch details response")
public record BranchResponse(
	@Schema(example = "1")
	Long branchId,
	@Schema(example = "COL-001")
	String branchCode,
	@Schema(example = "Colombo Main")
	String branchName,
	@Schema(example = "colombo.main@primecore.local")
	String branchEmail,
	@Schema(example = "0112000001")
	String branchPhone,
	@Schema(example = "No 1, Main Street, Colombo")
	String address,
	@Schema(example = "ACTIVE")
	String status,
	@Schema(example = "2026-04-19T12:00:00")
	String updatedAt
) {
}
