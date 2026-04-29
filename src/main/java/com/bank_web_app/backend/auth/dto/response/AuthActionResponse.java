package com.bank_web_app.backend.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AuthActionResponse", description = "Generic auth action result.")
public record AuthActionResponse(
	String message,
	String resetToken
) {
	public static AuthActionResponse message(String message) {
		return new AuthActionResponse(message, null);
	}

	public static AuthActionResponse withResetToken(String message, String resetToken) {
		return new AuthActionResponse(message, resetToken);
	}
}
