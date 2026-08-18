package com.bank_web_app.backend.transact.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Request payload used to create a new saved beneficiary for the authenticated customer.
@Schema(name = "CreateBeneficiaryRequest", description = "Payload to save a beneficiary for the logged-in bank customer.")
public record CreateBeneficiaryRequest(
	// Target beneficiary account number entered by the customer.
	@Schema(description = "Beneficiary account number (6-20 digits).", example = "2003004005", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Beneficiary account number is required.")
	@Pattern(regexp = "^[0-9]{6,20}$", message = "Beneficiary account number must contain 6 to 20 digits.")
	String beneficiaryAccountNo,
	// Customer-defined short name for easy identification in transfers.
	@Schema(description = "Beneficiary nickname.", example = "Water bill", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Nick name is required.")
	@Size(max = 100, message = "Nick name must not exceed 100 characters.")
	String nickName,
	// Optional-style note field used by the app as transfer context text.
	@Schema(description = "Beneficiary note.", example = "Monthly utility transfer", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Remark is required.")
	@Size(max = 255, message = "Remark must not exceed 255 characters.")
	String remark
) {
}
