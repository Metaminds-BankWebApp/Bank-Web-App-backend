package com.bank_web_app.backend.transact.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(name = "CreateTransactionRequest", description = "Payload to initiate a bank customer transfer transaction.")
public record CreateTransactionRequest(
	@Schema(description = "Beneficiary account number (10 digits).", example = "2003004005", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Receiver account number is required.")
	@Pattern(regexp = "^[0-9]{10}$", message = "Receiver account number must contain exactly 10 digits.")
	String receiverAccountNo,
	@Schema(description = "Beneficiary full name.", example = "Kasun Perera", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Receiver name is required.")
	@Size(max = 150, message = "Receiver name must not exceed 150 characters.")
	String receiverName,
	@Schema(description = "Transfer amount.", example = "12500.00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Amount is required.")
	@DecimalMin(value = "0.01", message = "Amount must be greater than 0.")
	@DecimalMax(value = "100000.00", message = "Transaction amount must not exceed Rs. 100,000.00.")
	@Digits(integer = 13, fraction = 2, message = "Amount supports up to 13 digits and 2 decimal places.")
	BigDecimal amount,
	@Schema(description = "Transfer remark or invoice reference.", example = "Invoice #INV-1002", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Remark is required.")
	@Size(max = 255, message = "Remark must not exceed 255 characters.")
	String remark,
	@Schema(description = "Whether to auto-track this successful transfer in SpendIQ expenses.", example = "false")
	Boolean expenseTrackingEnabled
) {
}
