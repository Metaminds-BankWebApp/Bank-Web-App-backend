package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminMonthlyUserGrowthPointResponse", description = "Single month data point for admin monthly user growth chart.")
public record AdminMonthlyUserGrowthPointResponse(
	@Schema(description = "Calendar year", example = "2026")
	Integer year,
	@Schema(description = "Calendar month number (1-12)", example = "4")
	Integer month,
	@Schema(description = "Short month label for chart axes", example = "APR")
	String label,
	@Schema(description = "Total newly created customer users in this month", example = "88")
	Long totalUsers,
	@Schema(description = "Newly created bank-customer users in this month", example = "53")
	Long bankUsers,
	@Schema(description = "Newly created public-customer users in this month", example = "35")
	Long publicUsers
) {
}
