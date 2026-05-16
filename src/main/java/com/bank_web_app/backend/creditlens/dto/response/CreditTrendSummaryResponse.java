package com.bank_web_app.backend.creditlens.dto.response;

/**
 * Narrative summary shown alongside the CreditLens monthly trend chart.
 */
public record CreditTrendSummaryResponse(
	String riskLabel,
	Integer riskDelta,
	String trendText,
	String biggestDriver,
	String momentumText,
	String nextTarget,
	String direction
) {
}
