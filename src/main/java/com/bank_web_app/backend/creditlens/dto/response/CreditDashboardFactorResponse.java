package com.bank_web_app.backend.creditlens.dto.response;

/**
 * Single factor card entry displayed in the CreditLens dashboard factor breakdown.
 */
public record CreditDashboardFactorResponse(
	String name,
	Integer value,
	Integer max,
	String colorHex,
	CreditInfoTooltipResponse infoTooltip
) {
}
