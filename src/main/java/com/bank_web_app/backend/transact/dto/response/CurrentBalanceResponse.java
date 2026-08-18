package com.bank_web_app.backend.transact.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

// Response payload for current-balance dashboard card.
@Schema(name = "CurrentBalanceResponse", description = "Current balance card payload for logged-in bank customer transact dashboard.")
public record CurrentBalanceResponse(
	// Account number of the authenticated customer.
	@Schema(description = "Bank customer's own account number.", example = "1000000001")
	String accountNumber,
	// Latest available balance for the authenticated customer account.
	@Schema(description = "Live current balance of the customer's own account.", example = "125000.75")
	BigDecimal currentBalance
) {
}
