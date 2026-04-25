package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BranchResponse", description = "Branch details response")
public record BranchResponse(
	@Schema(example = "1", description = "Internal database primary key. Frontend should not display this as Branch ID.")
	Long branchId,
	@Schema(example = "BR-001", description = "Visible branch code shown in the frontend as Branch ID")
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
