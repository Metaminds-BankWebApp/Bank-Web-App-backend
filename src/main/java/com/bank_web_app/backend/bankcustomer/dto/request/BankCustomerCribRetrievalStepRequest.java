package com.bank_web_app.backend.bankcustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BankCustomerCribRetrievalStepRequest", description = "Step 7 payload to update CRIB retrieval/report status.")
public record BankCustomerCribRetrievalStepRequest(
	@Schema(description = "CRIB request status", example = "COMPLETED")
	String requestStatus,
	@Schema(description = "CRIB report status", example = "READY")
	String reportStatus
) {
}
