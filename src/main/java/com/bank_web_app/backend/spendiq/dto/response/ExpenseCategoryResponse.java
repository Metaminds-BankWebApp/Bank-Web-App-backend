package com.bank_web_app.backend.spendiq.dto.response;

import java.time.LocalDateTime;

public record ExpenseCategoryResponse(
	Long categoryId,
	Long userId,
	String categoryName,
	String categoryType,
	LocalDateTime createdAt
) {
}