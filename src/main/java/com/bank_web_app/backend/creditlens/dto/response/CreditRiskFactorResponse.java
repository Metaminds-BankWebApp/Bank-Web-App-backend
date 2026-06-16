package com.bank_web_app.backend.creditlens.dto.response;

/**
 * Score contribution summary for one CreditLens factor category.
 */
public record CreditRiskFactorResponse(
	String name,
	Integer value,
	Integer max
) {
}
