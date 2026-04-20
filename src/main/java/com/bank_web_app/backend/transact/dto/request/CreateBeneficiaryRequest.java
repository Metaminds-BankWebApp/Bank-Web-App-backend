package com.bank_web_app.backend.transact.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateBeneficiaryRequest", description = "Payload to save a beneficiary for the logged-in bank customer.")
public record CreateBeneficiaryRequest(
	@Schema(description = "Beneficiary account number.", example = "2003004005", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Beneficiary account number is required.")
	String beneficiaryAccountNo,
	@Schema(description = "Beneficiary nickname.", example = "Water bill", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Nick name is required.")
	String nickName,
	@Schema(description = "Beneficiary note.", example = "Monthly utility transfer", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Remark is required.")
	String remark
) {
}
