package com.bank_web_app.backend.bankofficer.controller;

import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCardStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCribRequestStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerCribRetrievalStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerIncomeStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLiabilityStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.request.BankCustomerLoanStepRequest;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerCribStepResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialStepResponse;
import com.bank_web_app.backend.bankofficer.dto.response.AccountVerificationResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerIdentityResponse;
import com.bank_web_app.backend.bankofficer.service.BankOfficerFinancialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-officers/customers")
@Tag(name = "Bank Officer Financial Data", description = "Financial onboarding and record endpoints for bank officers.")
public class FinancialDataController {

	private final BankOfficerFinancialService financialService;

	public FinancialDataController(BankOfficerFinancialService financialService) {
		this.financialService = financialService;
	}

	@GetMapping("/user/{userId}")
	@Operation(
		summary = "Resolve owned bank customer id by user id",
		description = "Returns the bank_customer_id for a user owned by the logged-in bank officer. This bridges step-1 user creation to the later financial-record APIs.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Bank customer identity resolved successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: user is not a bank officer or customer not assigned to this officer"),
			@ApiResponse(responseCode = "404", description = "Bank customer not found for this officer")
		}
	)
	public ResponseEntity<BankOfficerCustomerIdentityResponse> getOwnedBankCustomerIdentityByUserId(@PathVariable Long userId) {
		return ResponseEntity.ok(financialService.getOwnedBankCustomerIdentityByUserId(userId));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/income/draft")
	@Operation(
		summary = "Save income step as draft (Step 2)",
		description = "Saves income data as a draft without advancing to the next step.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Income draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer or step-1 not completed")
		}
	)
	public ResponseEntity<BankCustomerFinancialStepResponse> saveIncomeStepDraft(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerIncomeStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveIncomeStepDraft(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/income/continue")
	@Operation(summary = "Save income step and continue to next (Step 2)", description = "Saves income data and advances the customer to step-3.")
	public ResponseEntity<BankCustomerFinancialStepResponse> saveIncomeStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerIncomeStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveIncomeStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/loans/draft")
	@Operation(summary = "Save loans step as draft (Step 3)", description = "Saves loan data as a draft without advancing to the next step.")
	public ResponseEntity<BankCustomerFinancialStepResponse> saveLoanStepDraft(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerLoanStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveLoanStepDraft(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/loans/continue")
	@Operation(summary = "Save loans step and continue to next (Step 3)", description = "Saves loan data and advances the customer to step-4.")
	public ResponseEntity<BankCustomerFinancialStepResponse> saveLoanStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerLoanStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveLoanStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/cards/draft")
	@Operation(summary = "Save cards step as draft (Step 4)", description = "Saves card data as a draft without advancing to the next step.")
	public ResponseEntity<BankCustomerFinancialStepResponse> saveCardStepDraft(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCardStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveCardStepDraft(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/cards/continue")
	@Operation(summary = "Save cards step and continue to next (Step 4)", description = "Saves card data and advances the customer to step-5.")
	public ResponseEntity<BankCustomerFinancialStepResponse> saveCardStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCardStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveCardStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/liabilities/draft")
	@Operation(summary = "Save liabilities step as draft (Step 5)", description = "Saves liabilities and missed payments data as a draft without completing step-5.")
	public ResponseEntity<BankCustomerFinancialStepResponse> saveLiabilityStepDraft(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerLiabilityStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveLiabilityStepDraft(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/liabilities/continue")
	@Operation(summary = "Save liabilities step and complete onboarding (Step 5)", description = "Saves liabilities and missed payments data and completes onboarding.")
	public ResponseEntity<BankCustomerFinancialStepResponse> saveLiabilityStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerLiabilityStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveLiabilityStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/crib-linking/continue")
	@Operation(summary = "Save CRIB linking step and continue (Step 2)", description = "Saves CRIB request and retrieval data in one combined step.")
	public ResponseEntity<BankCustomerCribStepResponse> saveCribLinkingStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCribRequestStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveCribLinkingStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/crib-request/continue")
	@Operation(summary = "Save CRIB request step and continue (Step 6)", description = "Saves CRIB request details and transitions the customer to step-7.")
	public ResponseEntity<BankCustomerCribStepResponse> saveCribRequestStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCribRequestStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveCribRequestStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/crib-retrieval/continue")
	@Operation(summary = "Save CRIB retrieval step and continue (Step 7)", description = "Saves CRIB retrieval/report status and transitions the customer to step-8 review.")
	public ResponseEntity<BankCustomerCribStepResponse> saveCribRetrievalStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCribRetrievalStepRequest request
	) {
		return ResponseEntity.ok(financialService.saveCribRetrievalStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/review/complete")
	@Operation(summary = "Complete review and onboarding (Step 8)", description = "Marks bank customer onboarding as COMPLETED after review step.")
	public ResponseEntity<BankCustomerCribStepResponse> completeCribReviewAndOnboarding(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(financialService.completeCribReviewAndOnboarding(bankCustomerId));
	}

	@PostMapping("/{bankCustomerId}/financial-records/maintenance/complete")
	@Operation(summary = "Finalise completed-customer financial maintenance", description = "Finalises a versioned financial maintenance snapshot without reopening onboarding.")
	public ResponseEntity<BankCustomerFinancialStepResponse> completeFinancialMaintenance(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(financialService.completeFinancialMaintenance(bankCustomerId));
	}

	@GetMapping("/{bankCustomerId}/financial-records/current")
	@Operation(summary = "Get current financial record", description = "Returns the latest financial snapshot for the given bank customer.")
	public ResponseEntity<BankCustomerFinancialRecordResponse> getCurrentFinancialRecord(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(financialService.getCurrentFinancialRecord(bankCustomerId));
	}

	@GetMapping("/{bankCustomerId}/financial-records/history")
	@Operation(summary = "Get financial record history", description = "Returns all financial snapshots for the given bank customer.")
	public ResponseEntity<List<BankCustomerFinancialRecordSummaryResponse>> getFinancialRecordHistory(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(financialService.getFinancialRecordHistory(bankCustomerId));
	}

	@GetMapping("/{bankCustomerId}/financial-records/{bankRecordId}")
	@Operation(summary = "Get financial record by id", description = "Returns a specific financial snapshot with all related details.")
	public ResponseEntity<BankCustomerFinancialRecordResponse> getFinancialRecordById(
		@PathVariable Long bankCustomerId,
		@PathVariable Long bankRecordId
	) {
		return ResponseEntity.ok(financialService.getFinancialRecordById(bankCustomerId, bankRecordId));
	}

	@GetMapping("/accounts/verify")
	@Operation(summary = "Verify account number", description = "Checks whether an account exists in the accounts table.")
	public ResponseEntity<AccountVerificationResponse> verifyAccount(@RequestParam String accountNumber) {
		return ResponseEntity.ok(financialService.verifyAccount(accountNumber));
	}
}
