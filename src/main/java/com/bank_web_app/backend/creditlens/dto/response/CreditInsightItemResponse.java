package com.bank_web_app.backend.creditlens.dto.response;

/**
 * Insight card item used for risk factors, positive behaviors, and recommended actions.
 */
public record CreditInsightItemResponse(
	String title,
	String description,
	String detail,
	String badgeText,
	String badgeTone,
	String iconKey,
	CreditInfoTooltipResponse infoTooltip
) {
}
