package com.bank_web_app.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserProfileSummaryItemResponse", description = "Single summary row shown on the profile page.")
public record UserProfileSummaryItemResponse(
	@Schema(example = "User ID")
	String label,

	@Schema(example = "PC-8821")
	String value
) {
}
