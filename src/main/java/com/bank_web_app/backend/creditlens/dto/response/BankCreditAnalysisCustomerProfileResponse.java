package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "BankCreditAnalysisCustomerProfileResponse", description = "Customer profile payload for the bank officer credit analysis detail page.")
public record BankCreditAnalysisCustomerProfileResponse(
	@Schema(description = "Bank customer id", example = "14")
	Long bankCustomerId,
	@Schema(description = "User id", example = "52")
	Long userId,
	@Schema(description = "Customer code", example = "BC-00014")
	String customerCode,
	@Schema(description = "Customer full name", example = "Amila Silva")
	String fullName,
	@Schema(description = "National identity card number", example = "200012345678")
	String nic,
	@Schema(description = "Email address", example = "amila.silva@example.com")
	String email,
	@Schema(description = "Phone number", example = "+94771234567")
	String phone,
	@Schema(description = "Customer status", example = "ACTIVE")
	String status,
	@Schema(description = "Account number", example = "20010010012345")
	String accountNumber,
	@Schema(description = "Account type", example = "SAVINGS")
	String accountType,
	@Schema(description = "Account status", example = "ACTIVE")
	String accountStatus,
	@Schema(description = "Assigned bank officer id", example = "7")
	Long officerId,
	@Schema(description = "Branch id", example = "3")
	Long branchId,
	@Schema(description = "Latest bank evaluation id", example = "501", nullable = true)
	Long latestBankEvaluationId,
	@Schema(description = "Latest total risk points", example = "72", nullable = true)
	Integer latestRiskPoints,
	@Schema(description = "Latest risk level", example = "LOW", nullable = true)
	String latestRiskLevel,
	@Schema(description = "Latest risk label", example = "Low", nullable = true)
	String latestRiskLabel,
	@Schema(description = "Latest evaluation date", nullable = true)
	LocalDateTime latestEvaluationDate
) {
}
