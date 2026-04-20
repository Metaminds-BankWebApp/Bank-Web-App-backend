package com.bank_web_app.backend.bankcustomer.dto.response;

import java.time.LocalDateTime;

public record BankCustomerFinancialRecordSummaryResponse(
	Long bankRecordId,
	Long bankCustomerId,
	Long verifiedByOfficerId,
	String dataSource,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
