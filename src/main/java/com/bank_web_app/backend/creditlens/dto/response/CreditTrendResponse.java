package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "CreditTrendResponse", description = "Trend data used in charts for a customer.")
public record CreditTrendResponse(
	@Schema(description = "Period key", example = "6m")
	String periodKey,
	@Schema(description = "Period label", example = "Last 6 months")
	String periodLabel,
	@Schema(description = "Chart labels")
	List<String> labels,
	@Schema(description = "Chart values")
	List<Integer> values,
	@Schema(description = "Trend points for charting")
	List<CreditTrendPointResponse> points,
	@Schema(description = "Summary of the trend data")
	CreditTrendSummaryResponse summary
) {
}
