package com.bank_web_app.backend.admin.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "AdminMonthlyUserGrowthResponse", description = "Monthly user growth dataset for admin dashboard charts.")
public record AdminMonthlyUserGrowthResponse(
	@Schema(description = "Requested number of months included in chart", example = "6")
	Integer months,
	@Schema(description = "Ordered chart points from oldest month to newest month")
	List<AdminMonthlyUserGrowthPointResponse> points
) {
}
