package com.bank_web_app.backend.transact.controller;

import com.bank_web_app.backend.transact.dto.response.TransactionResponse;
import com.bank_web_app.backend.transact.service.TransactionService;
import com.bank_web_app.backend.bankofficer.service.BankOfficerContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-officers/transact")
@Tag(name = "Bank Officer Transact", description = "Read-only transaction endpoints for BANK_OFFICER users.")
public class BankOfficerTransactionController {

	// Service layer dependency for retrieving transaction records.
	private final TransactionService transactionService;
	private final BankOfficerContextService bankOfficerContextService;

	// Injects transaction service for bank officer transaction endpoints.
	public BankOfficerTransactionController(TransactionService transactionService, BankOfficerContextService bankOfficerContextService) {
		this.transactionService = transactionService;
		this.bankOfficerContextService = bankOfficerContextService;
	}

	// Returns all transactions visible to BANK_OFFICER users.
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

	@PostMapping("/transactions/{referenceNo}/escalate-otp-limit")
	@Operation(summary = "Escalate a reviewed OTP-limit transaction to admins", description = "A bank officer may notify admins only after reviewing a transaction that failed after three incorrect OTP attempts.")
	public ResponseEntity<Void> escalateOtpLimitFailure(@PathVariable String referenceNo) {
		var officer = bankOfficerContextService.resolveLoggedInBankOfficer();
		transactionService.escalateOtpLimitFailureToAdmin(referenceNo, officer.getUser().getUserId());
		return ResponseEntity.noContent().build();
	}
}
