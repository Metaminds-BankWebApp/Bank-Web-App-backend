package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Admin-entered officer identity details. Passwords are intentionally not accepted. */
@Schema(name = "AdminBankOfficerCreateRequest", description = "Creates an officer and sends a one-time activation invitation.")
public record AdminBankOfficerCreateRequest(
	@NotBlank @Size(max = 100) String firstName,
	@NotBlank @Size(max = 100) String lastName,
	@NotBlank @Size(max = 20) String nic,
	@NotBlank String dob,
	@NotBlank @Size(max = 100) String email,
	@NotBlank @Size(max = 20) String mobile,
	@NotBlank @Size(max = 100) String province,
	@Size(max = 255) String address,
	@NotBlank @Size(min = 4, max = 50) String username,
	@NotNull @Positive Long branchId,
	@Positive Long createdByAdminUserId
) {}
