package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "CurrentBalanceResponse", description = "Current balance card payload for logged-in bank customer transact dashboard.")
public record CurrentBalanceResponse(
	@Schema(description = "Bank customer's own account number.", example = "1000000001")
	String accountNumber,
	@Schema(description = "Live current balance of the customer's own account.", example = "125000.75")
	BigDecimal currentBalance
) {
}
