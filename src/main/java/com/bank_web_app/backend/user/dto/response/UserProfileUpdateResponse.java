package com.bank_web_app.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserProfileUpdateResponse", description = "Response returned after updating the authenticated user's profile page data.")
public record UserProfileUpdateResponse(
	@Schema(example = "Profile updated successfully.")
	String message,

	UserProfileResponse profile
) {
}
