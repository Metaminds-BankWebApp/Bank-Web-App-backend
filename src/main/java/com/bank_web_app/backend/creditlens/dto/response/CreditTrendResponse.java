package com.bank_web_app.backend.creditlens.dto.response;

import java.util.List;

public record CreditTrendResponse(
	String periodKey,
	String periodLabel,
	List<String> labels,
	List<Integer> values,
	List<CreditTrendPointResponse> points,
	CreditTrendSummaryResponse summary
) {
}
