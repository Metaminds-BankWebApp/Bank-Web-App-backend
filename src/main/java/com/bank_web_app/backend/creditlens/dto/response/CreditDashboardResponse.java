package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "CreditDashboardResponse", description = "Dashboard payload for a customer's credit view.")
public record CreditDashboardResponse(
	@Schema(description = "Evaluation id", example = "501")
	Long evaluationId,
	@Schema(description = "Overall score", example = "680")
	Integer score,
	@Schema(description = "Risk level", example = "MEDIUM")
	String riskLevel,
	@Schema(description = "Human-readable risk label", example = "Medium")
	String riskLabel,
	@Schema(description = "Evaluation date and time")
	LocalDateTime createdAt,
	@Schema(description = "Breakdown factors for the dashboard")
	List<CreditDashboardFactorResponse> factors,
	@Schema(description = "Recent trend data")
	CreditTrendResponse recentTrend,
	@Schema(description = "Primary insight title")
	String insightTitle,
	@Schema(description = "Primary insight description")
	String insightDescription,
	@Schema(description = "Primary insight action label", example = "View tips")
	String insightActionLabel
) {
}
