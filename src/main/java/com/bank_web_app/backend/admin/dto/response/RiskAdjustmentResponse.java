package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "RiskAdjustmentResponse", description = "LoanSense risk adjustment response")
public record RiskAdjustmentResponse(
	@Schema(example = "1")
	Long adjustmentId,
	@Schema(example = "LOW")
	String riskLevel,
	@Schema(example = "Low Risk")
	String riskLabel,
	@Schema(example = "1.00")
	BigDecimal multiplier,
	@Schema(example = "Low-risk profiles receive the full recommended loan amount.")
	String description,
	@Schema(example = "2026-04-20T12:00:00")
	String updatedAt
) {
}
