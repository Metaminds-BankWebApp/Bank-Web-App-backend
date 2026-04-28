package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "AdminUserManagementUpdateRequest", description = "Payload for updating BANK/PUBLIC customer user details from admin user management module.")
public record AdminUserManagementUpdateRequest(
	@Schema(example = "Jane")
	@NotBlank(message = "First name is required.")
	@Size(max = 100, message = "First name must not exceed 100 characters.")
	String firstName,

	@Schema(example = "Doe")
	@NotBlank(message = "Last name is required.")
	@Size(max = 100, message = "Last name must not exceed 100 characters.")
	String lastName,

	@Schema(example = "jane.doe@primecore.com")
	@NotBlank(message = "Email is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 100, message = "Email must not exceed 100 characters.")
	String email,

	@Schema(example = "0771234567")
	@NotBlank(message = "Contact number is required.")
	@Pattern(
		regexp = "^\\+?[0-9()\\s-]{7,20}$",
		message = "Contact number format is invalid."
	)
	String contactNumber
) {
}
