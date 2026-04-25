package com.bank_web_app.backend.spendiq.dto.request;

import com.bank_web_app.backend.spendiq.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(
	name = "CreateExpenseRecordRequest",
	description = "Supports both current payload (categoryId, amount, expenseDate, paymentType) and legacy payload (accountId, title, category, amount, expenseDate, notes)."
)
public record CreateExpenseRecordRequest(
	@Schema(example = "10", description = "Current model: expense category id.")
	Long categoryId,
	@NotNull
	@DecimalMin(value = "0.01")
	@Digits(integer = 13, fraction = 2)
	@Schema(example = "2500.00")
	BigDecimal amount,
	@NotNull
	@Schema(example = "2026-04-20")
	LocalDate expenseDate,
	@NotNull
	@Schema(example = "CARD", allowableValues = { "CASH", "BANK_TRANSFER", "CARD" })
	PaymentMethod paymentType,
	@Schema(example = "1", description = "Legacy model: account id (currently ignored, authenticated user is used).")
	Long accountId,
	@Schema(example = "Lunch", description = "Legacy model: title (currently not persisted).")
	String title,
	@Schema(example = "FOOD", description = "Legacy model: category name; auto-created if missing for user.")
	String category,
	@Schema(example = "Office lunch", description = "Legacy model: notes (currently not persisted).")
	String notes
) {
}
