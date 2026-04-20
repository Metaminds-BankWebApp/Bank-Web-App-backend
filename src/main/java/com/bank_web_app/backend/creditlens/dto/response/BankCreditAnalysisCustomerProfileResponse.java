package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;

public record BankCreditAnalysisCustomerProfileResponse(
	Long bankCustomerId,
	Long userId,
	String customerCode,
	String fullName,
	String nic,
	String email,
	String phone,
	String status,
	String accountNumber,
	String accountType,
	String accountStatus,
	Long officerId,
	Long branchId,
	Long latestBankEvaluationId,
	Integer latestRiskPoints,
	String latestRiskLevel,
	String latestRiskLabel,
	LocalDateTime latestEvaluationDate
) {
}
