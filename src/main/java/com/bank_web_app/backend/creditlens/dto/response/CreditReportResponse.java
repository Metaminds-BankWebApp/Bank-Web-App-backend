package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated report payload used by the CreditLens report screen and PDF export flow.
 */
@Schema(name = "CreditReportResponse", description = "Monthly report payload for a customer's credit evaluations.")
public record CreditReportResponse(
	@Schema(description = "Customer type", example = "BANK")
	String customerType,
	@Schema(description = "Evaluation type", example = "MONTHLY")
	String evaluationType,
	@Schema(description = "Report generation timestamp")
	LocalDateTime generatedAt,
	@Schema(description = "List of report snapshots")
	List<CreditReportSnapshotResponse> snapshots
) {
}
