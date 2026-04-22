package com.bank_web_app.backend.bankofficer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "BankCustomerStepOneUpdateRequest", description = "Step-1 payload for updating an existing BANK_CUSTOMER owned by the logged-in bank officer.")
public record BankCustomerStepOneUpdateRequest(
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
	@Schema(description = "Province", example = "Western", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Province is required.")
	String province,
	@Schema(description = "Address", example = "123, Main Street, Colombo", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Address is required.")
	String address,
	@Schema(description = "Username for account login", example = "john.doe.2000", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Username is required.")
	@Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters.")
	String username,
	@Schema(description = "Optional new password. Leave empty to keep current password.", example = "StrongPass123")
	@Size(max = 255, message = "Password must not exceed 255 characters.")
	String password,
	@Schema(description = "Optional new password confirmation. Required only when password is provided.", example = "StrongPass123")
	@Size(max = 255, message = "Confirm password must not exceed 255 characters.")
	String confirmPassword,
	@Schema(description = "Legacy bank account field retained for backward compatibility.", example = "123456789")
	@Positive(message = "Bank account must be a positive integer.")
	Integer bankAccount,
	@Schema(description = "Account number mapped to the bank customer.", example = "20010010012345")
	@Size(max = 30, message = "Account number must not exceed 30 characters.")
	String accountNumber
) {
}
