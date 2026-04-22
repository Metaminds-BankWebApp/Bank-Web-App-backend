package com.bank_web_app.backend.creditlens.dto.response;

import java.util.List;

public record CreditInsightsResponse(
	List<CreditInsightItemResponse> keyRiskFactors,
	List<CreditInsightItemResponse> positiveBehaviors,
	List<CreditInsightItemResponse> financialTips,
	String reportBannerTitle,
	String reportBannerDescription,
	String reportActionLabel
) {
}
