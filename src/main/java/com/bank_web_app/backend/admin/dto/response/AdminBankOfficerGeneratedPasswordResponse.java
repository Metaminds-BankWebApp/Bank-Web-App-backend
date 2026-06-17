package com.bank_web_app.backend.admin.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminBankOfficerGeneratedPasswordResponse", description = "Generated password payload for admin bank officer onboarding.")
public record AdminBankOfficerGeneratedPasswordResponse(
	@Schema(example = "mX7@jQ9#Lp")
	String password
) {
}