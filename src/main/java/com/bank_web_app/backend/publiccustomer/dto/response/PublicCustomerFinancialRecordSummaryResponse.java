package com.bank_web_app.backend.publiccustomer.dto.response;

import java.time.LocalDateTime;

// Lightweight summary payload for listing financial snapshots.
public record PublicCustomerFinancialRecordSummaryResponse(
	// Unique financial record id.
	Long recordId,
	// Owning public-customer id.
	Long publicCustomerId,
	// Record state (for example CURRENT or ARCHIVED).
	String recordStatus,
	// Record creation timestamp.
	LocalDateTime createdAt,
	// Record last update timestamp.
	LocalDateTime updatedAt
) {
}
