package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "AdminBankOfficerUpdateRequest", description = "Payload for updating bank officer details from admin module.")
public record AdminBankOfficerUpdateRequest(
	@Schema(example = "Lila")
	@NotBlank(message = "First name is required.")
	@Size(max = 100, message = "First name must not exceed 100 characters.")
	String firstName,

	@Schema(example = "Doe")
	@NotBlank(message = "Last name is required.")
	@Size(max = 100, message = "Last name must not exceed 100 characters.")
	String lastName,

	@Schema(example = "lila.doe@primecore.com")
	@NotBlank(message = "Email is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 100, message = "Email must not exceed 100 characters.")
	String email,

	@Schema(example = "0771234567")
	@NotBlank(message = "Contact number is required.")
	@Pattern(
		regexp = "^(?:070|071|072|074|075|076|077|078)\\d{7}$",
		message = "Contact number must be 10 digits and start with a valid Sri Lankan mobile prefix."
	)
	String contactNumber,

	@Schema(example = "2")
	@Positive(message = "Branch id must be a positive number.")
	Long branchId
) {
}
