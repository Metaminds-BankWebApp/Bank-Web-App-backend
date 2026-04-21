package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CreditDashboardResponse(
	Long evaluationId,
	Integer score,
	String riskLevel,
	String riskLabel,
	LocalDateTime createdAt,
	List<CreditDashboardFactorResponse> factors,
	CreditTrendResponse recentTrend,
	String insightTitle,
	String insightDescription,
	String insightActionLabel
) {
}
