package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "BeneficiaryResponse", description = "Saved beneficiary details for bank customer transfers.")
public record BeneficiaryResponse(
	@Schema(description = "Beneficiary id", example = "35")
	Long beneficiaryId,
	@Schema(description = "Bank customer id", example = "7")
	Long bankCustomerId,
	@Schema(description = "Beneficiary account number", example = "2003004005")
	String beneficiaryAccountNo,
	@Schema(description = "Nickname", example = "Water bill")
	String nickName,
	@Schema(description = "Remark", example = "Monthly utility transfer")
	String remark,
	@Schema(description = "Created timestamp", example = "2026-04-20T10:00:00")
	LocalDateTime createdAt
) {
}
