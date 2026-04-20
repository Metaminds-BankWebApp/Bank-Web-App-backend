package com.bank_web_app.backend.transact.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(name = "CreateTransactionRequest", description = "Payload to initiate a bank customer transfer transaction.")
public record CreateTransactionRequest(
	@Schema(description = "Sender account number. Must match logged-in bank customer account.", example = "1002003004", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Sender account number is required.")
	String senderAccountNo,
	@Schema(description = "Beneficiary account number.", example = "2003004005", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Receiver account number is required.")
	String receiverAccountNo,
	@Schema(description = "Beneficiary full name.", example = "Kasun Perera", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Receiver name is required.")
	String receiverName,
	@Schema(description = "Transfer amount.", example = "12500.00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Amount is required.")
	@DecimalMin(value = "0.01", message = "Amount must be greater than 0.")
	BigDecimal amount,
	@Schema(description = "Transfer remark or invoice reference.", example = "Invoice #INV-1002", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Remark is required.")
	String remark,
	@Schema(description = "Whether to save this transfer for expense tracking.", example = "false")
	Boolean expenseTrackingEnabled
) {
}
