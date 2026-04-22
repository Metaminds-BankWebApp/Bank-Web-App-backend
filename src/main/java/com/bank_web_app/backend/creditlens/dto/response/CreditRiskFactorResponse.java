package com.bank_web_app.backend.creditlens.dto.response;

public record CreditRiskFactorResponse(
	String name,
	Integer value,
	Integer max
) {
}
