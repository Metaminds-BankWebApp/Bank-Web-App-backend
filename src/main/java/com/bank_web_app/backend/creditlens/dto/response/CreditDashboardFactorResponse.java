package com.bank_web_app.backend.creditlens.dto.response;

public record CreditDashboardFactorResponse(
	String name,
	Integer value,
	Integer max,
	String colorHex,
	CreditInfoTooltipResponse infoTooltip
) {
}
