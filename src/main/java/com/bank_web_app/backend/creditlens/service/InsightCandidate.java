package com.bank_web_app.backend.creditlens.service;

import com.bank_web_app.backend.creditlens.dto.response.CreditInsightItemResponse;

record InsightCandidate(
	int priority,
	CreditInsightItemResponse item
) {
}
