package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(name = "BulkLoanPolicyInterestRateUpdateRequest", description = "Payload for updating multiple loan policy interest rates")
public record BulkLoanPolicyInterestRateUpdateRequest(
	@NotEmpty(message = "At least one policy update is required.")
	List<@Valid LoanPolicyInterestRateUpdateRequest> policies
) {
}
