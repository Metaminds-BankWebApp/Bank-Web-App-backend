package com.bank_web_app.backend.admin.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AdminBankOfficerUsernameGenerationRequest", description = "Payload used to generate a suggested BANK_OFFICER username.")
public record AdminBankOfficerUsernameGenerationRequest(
	@Schema(example = "Kamal", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "First name is required for username generation.")
	@Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters.")
	String firstName,
	@Schema(example = "Edirisinghe", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Last name is required for username generation.")
	@Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters.")
	String lastName
) {
}