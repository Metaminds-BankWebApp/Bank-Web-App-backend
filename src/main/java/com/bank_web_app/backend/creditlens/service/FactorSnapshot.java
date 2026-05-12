package com.bank_web_app.backend.creditlens.service;

import com.bank_web_app.backend.creditlens.dto.response.CreditInfoTooltipResponse;

record FactorSnapshot(
	String title,
	int points,
	int maxPoints,
	String iconKey,
	String description,
	String detail,
	CreditInfoTooltipResponse infoTooltip
) {
}
