package com.bank_web_app.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "UserProfileUpdateRequest", description = "Combined profile update payload for the authenticated user.")
public record UserProfileUpdateRequest(
	@Schema(description = "Full display name shown on the profile page.", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Full name is required.")
	@Size(max = 200, message = "Full name must not exceed 200 characters.")
	@Pattern(regexp = "^[A-Za-z]+(?:\\s+[A-Za-z]+)*$", message = "Full name can contain letters and spaces only.")
	String fullName,

	@Schema(description = "Email address.", example = "john.doe@primecore.bank", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Email address is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 100, message = "Email address must not exceed 100 characters.")
	String email,

	@Schema(description = "Contact phone number.", example = "+94771234567", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Phone number is required.")
	@Size(max = 20, message = "Phone number must not exceed 20 characters.")
	@Pattern(regexp = "^\\+?[0-9()\\s-]+$", message = "Use only digits, spaces, +, -, or parentheses.")
	String phone,

	@Schema(description = "Editable address field, mainly used by public customers.", example = "Colombo Central")
	@Size(max = 255, message = "Address must not exceed 255 characters.")
	String address,

	@Schema(description = "Optional new username.", example = "johnDoePC1")
	@Size(max = 50, message = "New username must not exceed 50 characters.")
	String newUsername,

	@Schema(description = "Current password, required only when changing password.", example = "CurrentPass123")
	@Size(max = 255, message = "Current password must not exceed 255 characters.")
	String currentPassword,

	@Schema(description = "Optional new password. Must be at least 10 characters with uppercase, lowercase, and a number.", example = "StrongerPass123")
	@Size(max = 255, message = "New password must not exceed 255 characters.")
	String newPassword,

	@Schema(description = "Repeat the new password.", example = "StrongerPass123")
	@Size(max = 255, message = "Confirm password must not exceed 255 characters.")
	String confirmPassword
) {
}
