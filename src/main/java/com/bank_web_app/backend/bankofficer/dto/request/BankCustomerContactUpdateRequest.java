package com.bank_web_app.backend.bankofficer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Officer-maintained contact data; legal identity and account ownership are deliberately excluded. */
public record BankCustomerContactUpdateRequest(
	@NotBlank @Email @Size(max = 100) @Schema(example = "customer@example.com") String email,
	@NotBlank @Size(max = 20) @Schema(example = "+94771234567") String mobile,
	@NotBlank @Size(max = 100) @Schema(example = "Western") String province,
	@NotBlank @Size(max = 255) @Schema(example = "123 Main Street, Colombo") String address
) {}
