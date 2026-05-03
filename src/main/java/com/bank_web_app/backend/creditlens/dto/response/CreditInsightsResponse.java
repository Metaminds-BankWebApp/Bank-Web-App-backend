package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Groups the insight sections shown on the dedicated CreditLens insight page.
 */
@Schema(name = "CreditInsightsResponse", description = "Insight cards and tips for a customer's credit profile.")
public record CreditInsightsResponse(
	@Schema(description = "Key risk factor items")
	List<CreditInsightItemResponse> keyRiskFactors,
	@Schema(description = "Positive behavior items")
	List<CreditInsightItemResponse> positiveBehaviors,
	@Schema(description = "Financial tips items")
	List<CreditInsightItemResponse> financialTips,
	@Schema(description = "Report banner title")
	String reportBannerTitle,
	@Schema(description = "Report banner description")
	String reportBannerDescription,
	@Schema(description = "Report action label", example = "Download report")
	String reportActionLabel
) {
}
