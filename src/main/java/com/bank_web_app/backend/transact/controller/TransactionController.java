package com.bank_web_app.backend.transact.controller;

import com.bank_web_app.backend.transact.dto.request.CreateBeneficiaryRequest;
import com.bank_web_app.backend.transact.dto.request.CreateTransactionRequest;
import com.bank_web_app.backend.transact.dto.request.ResendTransactionOtpRequest;
import com.bank_web_app.backend.transact.dto.request.UpdateBeneficiaryRequest;
import com.bank_web_app.backend.transact.dto.request.VerifyTransactionOtpRequest;
import com.bank_web_app.backend.transact.dto.response.BeneficiaryResponse;
import com.bank_web_app.backend.transact.dto.response.TransactionInitiateResponse;
import com.bank_web_app.backend.transact.dto.response.TransactionResponse;
import com.bank_web_app.backend.transact.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-customers/transact")
@Tag(name = "Bank Customer Transact", description = "Transfer, OTP verification, beneficiary, and history endpoints for BANK_CUSTOMER users.")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PostMapping("/transactions/initiate")
	@Operation(
		summary = "Initiate transfer transaction",
		description = "Creates a transaction for the logged-in BANK_CUSTOMER and issues a 6-digit OTP (valid for 5 minutes) via Brevo SMTP email. Sender ownership is resolved from authenticated context (/api/auth/me bankCustomerId). Validates receiver account existence, transfer amount <= Rs.100,000.00, and minimum remaining sender balance Rs.1,000.00.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Transaction initiated and OTP issued"),
			@ApiResponse(responseCode = "400", description = "Validation failed or account/amount is invalid"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer"),
			@ApiResponse(responseCode = "503", description = "OTP email delivery failed")
		}
	)
	public ResponseEntity<TransactionInitiateResponse> initiateTransaction(@Valid @RequestBody CreateTransactionRequest request) {
		return ResponseEntity.ok(transactionService.initiateTransaction(request));
	}

	@PostMapping("/transactions/verify-otp")
	@Operation(
		summary = "Verify transfer OTP",
		description = "Verifies OTP for pending transaction and completes money transfer when OTP succeeds. Transaction status changes from PENDING_OTP to SUCCESS only after valid OTP. If expenseTrackingEnabled=true, SpendIQ integration runs after success.",
		responses = {
			@ApiResponse(responseCode = "200", description = "OTP verified and transaction completed"),
			@ApiResponse(responseCode = "400", description = "Invalid OTP, expired OTP, or business validation failure"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<TransactionResponse> verifyOtp(@Valid @RequestBody VerifyTransactionOtpRequest request) {
		return ResponseEntity.ok(transactionService.verifyOtp(request));
	}

	@PostMapping("/transactions/resend-otp")
	@Operation(
		summary = "Resend transfer OTP",
		description = "Resends a new 6-digit OTP for a transaction that is still in PENDING_OTP status and records resend count in OTP logs.",
		responses = {
			@ApiResponse(responseCode = "200", description = "OTP resent successfully"),
			@ApiResponse(responseCode = "400", description = "Transaction is not eligible for OTP resend"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer"),
			@ApiResponse(responseCode = "503", description = "OTP email delivery failed")
		}
	)
	public ResponseEntity<TransactionInitiateResponse> resendOtp(@Valid @RequestBody ResendTransactionOtpRequest request) {
		return ResponseEntity.ok(transactionService.resendOtp(request));
	}

	@GetMapping("/transactions/history")
	@Operation(
		summary = "Get transaction history",
		description = "Returns transaction history for the logged-in BANK_CUSTOMER directly from the bank_customer_transactions table (reverse chronological by transaction_date).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Transaction history returned successfully from transaction database"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<List<TransactionResponse>> getHistory() {
		return ResponseEntity.ok(transactionService.getHistory());
	}

	@GetMapping("/transactions/{referenceNo}")
	@Operation(
		summary = "Get transaction by reference",
		description = "Returns one transaction for the logged-in BANK_CUSTOMER by transaction reference number.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Transaction returned successfully"),
			@ApiResponse(responseCode = "400", description = "Transaction not found for this customer"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String referenceNo) {
		return ResponseEntity.ok(transactionService.getByReferenceNo(referenceNo));
	}

	@PostMapping("/beneficiaries")
	@Operation(
		summary = "Create beneficiary",
		description = "Creates a saved beneficiary for the logged-in BANK_CUSTOMER. Ownership is resolved from the authenticated context (same source as /api/auth/me bankCustomerId).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Beneficiary created successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or beneficiary account invalid"),
			@ApiResponse(responseCode = "409", description = "Duplicate beneficiary account for this bank customer"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<BeneficiaryResponse> createBeneficiary(@Valid @RequestBody CreateBeneficiaryRequest request) {
		return ResponseEntity.ok(transactionService.createBeneficiary(request));
	}

	@PutMapping("/beneficiaries/{beneficiaryId}")
	@Operation(
		summary = "Update beneficiary",
		description = "Updates one beneficiary owned by the logged-in BANK_CUSTOMER. Ownership is resolved from the authenticated context (same source as /api/auth/me bankCustomerId).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Beneficiary updated successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or beneficiary account invalid"),
			@ApiResponse(responseCode = "409", description = "Duplicate beneficiary account for this bank customer"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<BeneficiaryResponse> updateBeneficiary(
		@PathVariable Long beneficiaryId,
		@Valid @RequestBody UpdateBeneficiaryRequest request
	) {
		return ResponseEntity.ok(transactionService.updateBeneficiary(beneficiaryId, request));
	}

	@GetMapping("/beneficiaries")
	@Operation(
		summary = "Get beneficiaries",
		description = "Returns all beneficiaries saved by the logged-in BANK_CUSTOMER.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Beneficiaries returned successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<List<BeneficiaryResponse>> getBeneficiaries() {
		return ResponseEntity.ok(transactionService.getBeneficiaries());
	}

	@DeleteMapping("/beneficiaries/{beneficiaryId}")
	@Operation(
		summary = "Delete beneficiary",
		description = "Deletes one beneficiary owned by the logged-in BANK_CUSTOMER.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Beneficiary deleted successfully"),
			@ApiResponse(responseCode = "400", description = "Beneficiary not found for this customer"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<Void> deleteBeneficiary(@PathVariable Long beneficiaryId) {
		transactionService.deleteBeneficiary(beneficiaryId);
		return ResponseEntity.noContent().build();
	}
}
