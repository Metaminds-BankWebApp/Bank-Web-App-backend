package com.bank_web_app.backend.publiccustomer.dto.response;

import java.time.LocalDateTime;

public record PublicCustomerFinancialRecordSummaryResponse(
	Long recordId,
	Long publicCustomerId,
	String recordStatus,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
