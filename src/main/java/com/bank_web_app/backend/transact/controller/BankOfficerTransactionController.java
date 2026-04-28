package com.bank_web_app.backend.transact.controller;

import com.bank_web_app.backend.transact.dto.response.TransactionResponse;
import com.bank_web_app.backend.transact.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-officers/transact")
@Tag(name = "Bank Officer Transact", description = "Read-only transaction endpoints for BANK_OFFICER users.")
public class BankOfficerTransactionController {

	private final TransactionService transactionService;

	public BankOfficerTransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@GetMapping("/transactions")
	@Operation(
		summary = "Get all transactions",
		description = "Returns all customer transactions for BANK_OFFICER users, sorted by transaction date descending.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Transaction list returned successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank officer")
		}
	)
	public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
		return ResponseEntity.ok(transactionService.getAllTransactions());
	}
}