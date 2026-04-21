package com.bank_web_app.backend.creditlens.dto.response;

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
