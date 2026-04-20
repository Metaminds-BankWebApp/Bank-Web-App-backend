package com.bank_web_app.backend.spendiq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateExpenseCategoryRequest", description = "Request payload to create an expense category")
public record CreateExpenseCategoryRequest(
	@NotBlank
	@Schema(example = "Food")
	String categoryName,
	@NotBlank
	@Schema(example = "FIXED", description = "Allowed values: FIXED, VARIABLE")
	String categoryType
) {
}