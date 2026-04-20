package com.bank_web_app.backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(name = "RiskAdjustmentUpdateRequest", description = "Payload for updating a LoanSense risk adjustment")
public record RiskAdjustmentUpdateRequest(
	@Schema(example = "LOW")
	@NotBlank(message = "Risk level is required.")
	@Size(max = 20, message = "Risk level must not exceed 20 characters.")
	String riskLevel,

	@Schema(example = "1.00")
	@NotNull(message = "Multiplier is required.")
	@DecimalMin(value = "0.01", message = "Multiplier must be greater than 0.")
	@DecimalMax(value = "10.00", message = "Multiplier must not exceed 10.")
	BigDecimal multiplier,

	@Schema(example = "Low-risk profiles receive the full recommended loan amount.")
	@Size(max = 255, message = "Description must not exceed 255 characters.")
	String description
) {
}
