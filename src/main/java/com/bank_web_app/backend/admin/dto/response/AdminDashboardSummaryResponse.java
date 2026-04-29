package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminDashboardSummaryResponse", description = "Top-level summary metrics for the admin dashboard.")
public record AdminDashboardSummaryResponse(
	@Schema(example = "134")
	Long totalUsers,
	@Schema(example = "12")
	Long totalBranches,
	@Schema(example = "9")
	Long totalOfficers,
	@Schema(example = "456")
	Long totalTransactions
) {
}
