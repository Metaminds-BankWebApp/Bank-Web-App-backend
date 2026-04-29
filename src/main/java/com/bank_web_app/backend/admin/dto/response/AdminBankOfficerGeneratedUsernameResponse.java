package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminBankOfficerGeneratedUsernameResponse", description = "Generated username payload for admin bank officer onboarding.")
public record AdminBankOfficerGeneratedUsernameResponse(
	@Schema(example = "kamaledirisinghe482")
	String username
) {
}