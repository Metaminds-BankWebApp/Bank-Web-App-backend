package com.bank_web_app.backend.publiccustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(
	name = "PublicCustomerApplicationProgressResponse",
	description = "Backend-owned completion state for the public-customer financial application."
)
public record PublicCustomerApplicationProgressResponse(
	Long publicCustomerId,
	Long recordId,
	int completionPercentage,
	int completedSteps,
	int totalSteps,
	String overallStatus,
	LocalDateTime submittedAt,
	List<ApplicationStep> steps
) {
	public record ApplicationStep(
		String code,
		String label,
		String status,
		boolean completed
	) {
	}
}
