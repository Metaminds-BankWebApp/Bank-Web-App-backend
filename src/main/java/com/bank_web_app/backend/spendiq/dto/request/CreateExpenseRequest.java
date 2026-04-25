package com.bank_web_app.backend.spendiq.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
	@NotNull(message = "Account id is required.")
	Long accountId,
	@NotBlank(message = "Title is required.")
	@Size(max = 120, message = "Title must not exceed 120 characters.")
	String title,
	@NotBlank(message = "Category is required.")
	@Size(max = 50, message = "Category must not exceed 50 characters.")
	String category,
	@NotNull(message = "Amount is required.")
	@DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
	BigDecimal amount,
	@NotNull(message = "Expense date is required.")
	LocalDate expenseDate,
	@Size(max = 500, message = "Notes must not exceed 500 characters.")
	String notes
) {}
