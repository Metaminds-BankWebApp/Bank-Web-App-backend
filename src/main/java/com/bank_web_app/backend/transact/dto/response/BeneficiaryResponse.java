package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

// Response payload representing one saved beneficiary entry.
@Schema(name = "BeneficiaryResponse", description = "Saved beneficiary details for bank customer transfers.")
public record BeneficiaryResponse(
	// Unique beneficiary record id.
	@Schema(description = "Beneficiary id", example = "35")
	Long beneficiaryId,
	// Owner bank-customer id for this beneficiary.
	@Schema(description = "Bank customer id", example = "7")
	Long bankCustomerId,
	// Beneficiary account number stored for future transfers.
	@Schema(description = "Beneficiary account number", example = "2003004005")
	String beneficiaryAccountNo,
	// User-friendly beneficiary nickname.
	@Schema(description = "Nickname", example = "Water bill")
	String nickName,
	// Additional note saved with beneficiary details.
	@Schema(description = "Remark", example = "Monthly utility transfer")
	String remark,
	// Timestamp when this beneficiary record was created.
	@Schema(description = "Created timestamp", example = "2026-04-20T10:00:00")
	LocalDateTime createdAt
) {
}
