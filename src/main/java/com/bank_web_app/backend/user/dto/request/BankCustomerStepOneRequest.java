package com.bank_web_app.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "BankCustomerStepOneRequest", description = "Step 1 payload for Bank Customer registration draft/continue.")
public record BankCustomerStepOneRequest(
	@Schema(description = "Customer first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "First name is required.")
	@Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters.")
	String firstName,
	@Schema(description = "Customer last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Last name is required.")
	@Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters.")
	String lastName,
	@Schema(description = "National identity card number", example = "200012345678", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "NIC is required.")
	@Size(max = 20, message = "NIC must not exceed 20 characters.")
	String nic,
	@Schema(description = "Date of birth in ISO format (yyyy-MM-dd)", example = "2000-03-15", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Date of birth is required.")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "DOB must be in yyyy-MM-dd format.")
	String dob,
	@Schema(description = "Email address", example = "john.doe@bank.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Email is required.")
	@Email(message = "Enter a valid email address.")
	@Size(max = 100, message = "Email must not exceed 100 characters.")
	String email,
	@Schema(description = "Mobile number", example = "+94771234567", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Mobile is required.")
	@Size(max = 20, message = "Mobile must not exceed 20 characters.")
	String mobile,
	@Schema(description = "Province text from frontend. Persisted in current schema as a single-character code until schema evolves.", example = "Western", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Province is required.")
	String province,
	@Schema(description = "Address text from frontend. Persisted in current schema as a single-character placeholder until schema evolves.", example = "123, Main Street, Colombo", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Address is required.")
	String address,
	@Schema(description = "Username for account login", example = "john.doe.2000", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Username is required.")
	@Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters.")
	String username,
	@Schema(description = "Raw password from frontend. Will be stored as-is for now and later replaced with hashing in auth flow.", example = "StrongPass123", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Password is required.")
	@Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters.")
	String password,
	@Schema(description = "Client-side password confirmation", example = "StrongPass123", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Confirm password is required.")
	String confirmPassword,
	@Schema(description = "Bank account number", example = "100023456789", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Bank account is required.")
	String bankAccount
) {
}
