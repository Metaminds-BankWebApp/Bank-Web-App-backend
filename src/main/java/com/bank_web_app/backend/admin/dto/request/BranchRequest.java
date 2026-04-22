package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "BranchRequest", description = "Payload for creating or updating a bank branch")
public record BranchRequest(
	@Schema(example = "Colombo Main")
	@NotBlank(message = "Branch name is required.")
	@Size(max = 100, message = "Branch name must not exceed 100 characters.")
	String branchName,

	@Schema(example = "colombo.main@primecore.local")
	@Email(message = "Enter a valid branch email.")
	@Size(max = 100, message = "Branch email must not exceed 100 characters.")
	String branchEmail,

	@Schema(example = "0112000001")
	@Size(max = 20, message = "Branch phone must not exceed 20 characters.")
	String branchPhone,

	@Schema(example = "No 1, Main Street, Colombo")
	@Size(max = 150, message = "Address must not exceed 150 characters.")
	String address,

	@Schema(example = "ACTIVE")
	@Size(max = 20, message = "Status must not exceed 20 characters.")
	String status
) {
}
