package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "BranchRequest", description = "Payload for creating or updating a bank branch")
public record BranchRequest(
	@Schema(example = "Colombo Main")
	@NotBlank(message = "Branch name is required.")
	@Size(max = 100, message = "Branch name must not exceed 100 characters.")
	String branchName,

	@Schema(example = "colombo.main@primecore.com")
	@NotBlank(message = "Email address is required.")
	@Pattern(
		regexp = "(?i)^[a-z0-9._%+-]+@primecore\\.com$",
		message = "Email must be in the format name@primecore.com."
	)
	@Size(max = 100, message = "Branch email must not exceed 100 characters.")
	String branchEmail,

	@Schema(example = "0112000001")
	@NotBlank(message = "Contact number is required.")
	@Pattern(
		regexp = "^(?:070|071|072|074|075|076|077|078|011|021|023|024|025|026|027|031|032|033|034|035|036|037|038|041|045|047|051|052|054|055|057|063|065|066|067|081|091)\\d{7}$",
		message = "Contact number must be 10 digits and start with a valid Sri Lankan area/mobile code."
	)
	String branchPhone,

	@Schema(example = "No 1, Main Street, Colombo")
	@Size(max = 150, message = "Address must not exceed 150 characters.")
	String address,

	@Schema(example = "ACTIVE")
	@Size(max = 20, message = "Status must not exceed 20 characters.")
	String status
) {
}
