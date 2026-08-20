package com.bank_web_app.backend.transact.controller;

import com.bank_web_app.backend.transact.dto.request.CreateBeneficiaryRequest;
import com.bank_web_app.backend.transact.dto.request.CreateTransactionRequest;
import com.bank_web_app.backend.transact.dto.request.ResendTransactionOtpRequest;
import com.bank_web_app.backend.transact.dto.request.UpdateBeneficiaryRequest;
import com.bank_web_app.backend.transact.dto.request.VerifyTransactionOtpRequest;
import com.bank_web_app.backend.transact.dto.response.BeneficiaryResponse;
import com.bank_web_app.backend.transact.dto.response.CurrentBalanceResponse;
import com.bank_web_app.backend.transact.dto.response.TransactDashboardSummaryResponse;
import com.bank_web_app.backend.transact.dto.response.TransactionInitiateResponse;
import com.bank_web_app.backend.transact.dto.response.TransactionResponse;
import com.bank_web_app.backend.transact.service.TransactionService;
import com.bank_web_app.backend.transact.service.TransactionReceiptPdfService;
import com.bank_web_app.backend.transact.service.TransactionStatementPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-customers/transact")
@Tag(name = "Bank Customer Transact", description = "Transfer, OTP verification, beneficiary, and history endpoints for BANK_CUSTOMER users.")
public class TransactionController {

	// Service layer for transfer, OTP, beneficiary, and history operations.
	private final TransactionService transactionService;
	// Service layer for generating statement-style transaction history PDFs.
	private final TransactionStatementPdfService transactionStatementPdfService;
	// Service layer for generating a receipt PDF for one completed transfer.
	private final TransactionReceiptPdfService transactionReceiptPdfService;

	// Injects transact services used by customer-facing transact endpoints.
	public TransactionController(
		TransactionService transactionService,
		TransactionStatementPdfService transactionStatementPdfService,
		TransactionReceiptPdfService transactionReceiptPdfService
	) {
		this.transactionService = transactionService;
		this.transactionStatementPdfService = transactionStatementPdfService;
		this.transactionReceiptPdfService = transactionReceiptPdfService;
	}

	// Initiates a transfer and triggers OTP delivery for confirmation.
	@PostMapping("/transactions/initiate")
	@Operation(
		summary = "Initiate transfer transaction",
		description = "Creates a transaction for the logged-in BANK_CUSTOMER and issues a 6-digit OTP (valid for 5 minutes) via transactional email. Sender ownership is resolved from authenticated context (/api/auth/me bankCustomerId). Validates receiver account existence, transfer amount <= Rs.100,000.00, and minimum remaining sender balance Rs.1,000.00.",
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

	// Verifies OTP and completes the pending transfer when valid.
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

	// Cancels a transfer before a valid OTP has completed it.
	@PostMapping("/transactions/{referenceNo}/cancel")
	@Operation(
		summary = "Cancel pending transfer",
		description = "Cancels a transaction that is still in PENDING_OTP status. No funds are transferred.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Pending transaction cancelled"),
			@ApiResponse(responseCode = "400", description = "Transaction cannot be cancelled"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<TransactionResponse> cancelTransaction(@PathVariable String referenceNo) {
		return ResponseEntity.ok(transactionService.cancelTransaction(referenceNo));
	}

	// Sends a new OTP for a transaction still pending OTP verification.
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

	// Returns current account balance details for the authenticated bank customer.
	@GetMapping("/dashboard/current-balance")
	@Operation(
		summary = "Get current balance card data",
		description = "Returns account number and current balance for the logged-in BANK_CUSTOMER only, using the same ownership context as /api/auth/me bankCustomerId.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Current balance returned successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer"),
			@ApiResponse(responseCode = "404", description = "Account not found for logged-in bank customer")
		}
	)
	public ResponseEntity<CurrentBalanceResponse> getCurrentBalance() {
		return ResponseEntity
			.ok()
			.cacheControl(CacheControl.noStore().mustRevalidate())
			.body(transactionService.getCurrentBalance());
	}

	// Returns dashboard summary metrics for the authenticated bank customer.
	@GetMapping("/dashboard/summary")
	@Operation(
		summary = "Get transact dashboard summary cards",
		description = "Returns current-balance and transaction summary cards for the logged-in BANK_CUSTOMER only, using the same ownership context as /api/auth/me bankCustomerId. Reads account data from accounts table and totals from bank_customer_transactions table.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Dashboard summary returned successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer"),
			@ApiResponse(responseCode = "404", description = "Account not found for logged-in bank customer")
		}
	)
	public ResponseEntity<TransactDashboardSummaryResponse> getDashboardSummary() {
		return ResponseEntity
			.ok()
			.cacheControl(CacheControl.noStore().mustRevalidate())
			.body(transactionService.getDashboardSummary());
	}

	// Returns transaction history for the authenticated bank customer.
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

	// Generates and downloads transaction history report as a PDF file.
	@GetMapping(value = "/transactions/history/report", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(
		summary = "Download transaction history report as PDF",
		description = "Returns a statement-style PDF for the logged-in BANK_CUSTOMER. Supports optional date range filters. If dates are omitted, the current month is used.",
		responses = {
			@ApiResponse(responseCode = "200", description = "PDF report generated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid date range"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank customer")
		}
	)
	public ResponseEntity<byte[]> downloadHistoryReport(
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
	) {
		TransactionStatementPdfService.StatementPdfResult report = transactionStatementPdfService.generateStatementPdf(
			fromDate,
			toDate
		);

		return ResponseEntity
			.ok()
			.cacheControl(CacheControl.noStore().mustRevalidate())
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.fileName() + "\"")
			.body(report.content());
	}

	// Generates and downloads a PDF receipt for one successful customer-owned transaction.
	@GetMapping(value = "/transactions/{referenceNo}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(
		summary = "Download transaction receipt as PDF",
		description = "Returns a receipt PDF for a successful transaction owned by the logged-in BANK_CUSTOMER.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Receipt PDF generated successfully"),
			@ApiResponse(responseCode = "400", description = "Transaction is not successful"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank customer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: the transaction is not owned by the logged-in customer"),
			@ApiResponse(responseCode = "404", description = "Transaction not found")
		}
	)
	public ResponseEntity<byte[]> downloadTransactionReceipt(@PathVariable String referenceNo) {
		TransactionReceiptPdfService.ReceiptPdfResult receipt = transactionReceiptPdfService.generateReceiptPdf(referenceNo);

		return ResponseEntity
			.ok()
			.cacheControl(CacheControl.noStore().mustRevalidate())
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + receipt.fileName() + "\"")
			.body(receipt.content());
	}

	// Returns a single transaction using its reference number.
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

	// Creates a new beneficiary for the authenticated bank customer.
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

	// Updates an existing beneficiary owned by the authenticated bank customer.
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

	// Lists all beneficiaries saved by the authenticated bank customer.
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

	// Deletes a beneficiary owned by the authenticated bank customer.
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
