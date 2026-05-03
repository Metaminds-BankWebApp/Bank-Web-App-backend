package com.bank_web_app.backend.creditlens.dto.response;

/**
 * Tooltip content that explains how a CreditLens metric or factor is derived.
 */
public record CreditInfoTooltipResponse(
	String title,
	String description,
	String formula
) {
}
