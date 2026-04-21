package com.bank_web_app.backend.bankofficer.controller;

import com.bank_web_app.backend.bankofficer.dto.response.AccountVerificationResponse;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerIdentityResponse;
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
import com.bank_web_app.backend.bankofficer.service.BankOfficerCustomerOnboardingService;
import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
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
@Tag(name = "Bank Officer Customers", description = "Bank officer-owned bank customer onboarding endpoints")
public class BankOfficerCustomerController {

	private final BankOfficerCustomerOnboardingService onboardingService;

	public BankOfficerCustomerController(BankOfficerCustomerOnboardingService onboardingService) {
		this.onboardingService = onboardingService;
	}

	@PostMapping("/step-1/draft")
	@Operation(
		summary = "Save BANK_CUSTOMER draft",
		description = "Authenticated bank officer creates BANK_CUSTOMER draft. Officer and branch are resolved from the logged-in user; account must already exist for the provided account number.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: authenticated user is not a bank officer"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> saveDraft(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(onboardingService.saveDraft(request));
	}

	@PostMapping("/step-1/continue")
	@Operation(
		summary = "Save and continue BANK_CUSTOMER step-1",
		description = "Authenticated bank officer creates BANK_CUSTOMER and marks as PENDING_STEP_2. Officer and branch are resolved from the logged-in user; account must already exist for the provided account number.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: authenticated user is not a bank officer"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> saveAndContinue(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(onboardingService.saveAndContinue(request));
	}

	@GetMapping
	@Operation(summary = "Get all bank customers", description = "Returns bank customer summaries for officer view.")
	public ResponseEntity<List<BankCustomerSummaryResponse>> getAll() {
		return ResponseEntity.ok(onboardingService.getAll());
	}

	@GetMapping("/user/{userId}")
	@Operation(
		summary = "Resolve owned bank customer id by user id",
		description = "Returns the bank_customer_id for a user owned by the logged-in bank officer. This endpoint bridges step-1 (user creation) to steps 2-5 (financial data). Used after user registration to obtain the bankCustomerId needed for financial step APIs. Only returns data for customers assigned to the logged-in officer.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Bank customer identity resolved successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: user is not a bank officer or customer not assigned to this officer"),
			@ApiResponse(responseCode = "404", description = "Bank customer not found for this officer")
		}
	)
	public ResponseEntity<BankOfficerCustomerIdentityResponse> getOwnedBankCustomerIdentityByUserId(
		@PathVariable Long userId
	) {
		return ResponseEntity.ok(onboardingService.getOwnedBankCustomerIdentityByUserId(userId));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/income/draft")
	@Operation(
		summary = "Save income step as draft (Step 2)",
		description = "Saves income data as a draft without advancing to the next step. Draft data can be modified, and the customer stays on step-2. Only the assigned bank officer can add income for their customers.",
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
		return ResponseEntity.ok(onboardingService.saveIncomeStepDraft(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/income/continue")
	@Operation(
		summary = "Save income step and continue to next (Step 2)",
		description = "Saves income data and marks step-2 as complete, transitioning the customer to step-3. Only the assigned bank officer can add income for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Income step saved successfully and customer advanced to next step"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer or step-1 not completed")
		}
	)
	public ResponseEntity<BankCustomerFinancialStepResponse> saveIncomeStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerIncomeStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveIncomeStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/loans/draft")
	@Operation(
		summary = "Save loans step as draft (Step 3)",
		description = "Saves loan data as a draft without advancing to the next step. Draft data can be modified, and the customer stays on step-3. Only the assigned bank officer can add loans for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Loans draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer or step-1 not completed")
		}
	)
	public ResponseEntity<BankCustomerFinancialStepResponse> saveLoanStepDraft(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerLoanStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveLoanStepDraft(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/loans/continue")
	@Operation(
		summary = "Save loans step and continue to next (Step 3)",
		description = "Saves loan data and marks step-3 as complete, transitioning the customer to step-4. Only the assigned bank officer can add loans for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Loans step saved successfully and customer advanced to next step"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer or step-1 not completed")
		}
	)
	public ResponseEntity<BankCustomerFinancialStepResponse> saveLoanStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerLoanStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveLoanStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/cards/draft")
	@Operation(
		summary = "Save cards step as draft (Step 4)",
		description = "Saves credit card data as a draft without advancing to the next step. Draft data can be modified, and the customer stays on step-4. Only the assigned bank officer can add cards for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Cards draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer or step-1 not completed")
		}
	)
	public ResponseEntity<BankCustomerFinancialStepResponse> saveCardStepDraft(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCardStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveCardStepDraft(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/cards/continue")
	@Operation(
		summary = "Save cards step and continue to next (Step 4)",
		description = "Saves credit card data and marks step-4 as complete, transitioning the customer to step-5. Only the assigned bank officer can add cards for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Cards step saved successfully and customer advanced to next step"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer or step-1 not completed")
		}
	)
	public ResponseEntity<BankCustomerFinancialStepResponse> saveCardStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCardStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveCardStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/liabilities/draft")
	@Operation(
		summary = "Save liabilities step as draft (Step 5)",
		description = "Saves liabilities and missed payments data as a draft without completing step-5. Draft data can be modified, and the customer stays on step-5. Only the assigned bank officer can add liabilities for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Liabilities draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer or step-1 not completed")
		}
	)
	public ResponseEntity<BankCustomerFinancialStepResponse> saveLiabilityStepDraft(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerLiabilityStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveLiabilityStepDraft(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/liabilities/continue")
	@Operation(
		summary = "Save liabilities step and complete onboarding (Step 5)",
		description = "Saves liabilities and missed payments data and marks step-5 (final step) as complete, finishing the bank customer onboarding. Only the assigned bank officer can add liabilities for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Liabilities step saved successfully and customer onboarding completed"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer or step-1 not completed")
		}
	)
	public ResponseEntity<BankCustomerFinancialStepResponse> saveLiabilityStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerLiabilityStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveLiabilityStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/crib-request/continue")
	@Operation(
		summary = "Save CRIB request step and continue (Step 6)",
		description = "Saves CRIB request details and transitions the customer to step-7. Only the assigned bank officer can perform this step for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "CRIB request step saved successfully and customer advanced to next step"),
			@ApiResponse(responseCode = "400", description = "Validation failed or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer")
		}
	)
	public ResponseEntity<BankCustomerCribStepResponse> saveCribRequestStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCribRequestStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveCribRequestStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/crib-retrieval/continue")
	@Operation(
		summary = "Save CRIB retrieval step and continue (Step 7)",
		description = "Saves CRIB retrieval/report status and transitions the customer to step-8 review. Only the assigned bank officer can perform this step for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "CRIB retrieval step saved successfully and customer advanced to next step"),
			@ApiResponse(responseCode = "400", description = "Validation failed, CRIB request not found, or bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer")
		}
	)
	public ResponseEntity<BankCustomerCribStepResponse> saveCribRetrievalStepAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerCribRetrievalStepRequest request
	) {
		return ResponseEntity.ok(onboardingService.saveCribRetrievalStepAndContinue(bankCustomerId, request));
	}

	@PostMapping("/{bankCustomerId}/financial-records/steps/review/complete")
	@Operation(
		summary = "Complete review and onboarding (Step 8)",
		description = "Marks bank customer onboarding as COMPLETED after review step. Only the assigned bank officer can complete onboarding for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Onboarding completed successfully"),
			@ApiResponse(responseCode = "400", description = "Bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer")
		}
	)
	public ResponseEntity<BankCustomerCribStepResponse> completeCribReviewAndOnboarding(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(onboardingService.completeCribReviewAndOnboarding(bankCustomerId));
	}

	@GetMapping("/{bankCustomerId}/financial-records/current")
	@Operation(
		summary = "Get current financial record",
		description = "Returns the latest financial snapshot including income, loans, cards, and liabilities for the given bank customer. Only the assigned bank officer can view financial records for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Current financial record retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Bank customer or financial record not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer")
		}
	)
	public ResponseEntity<BankCustomerFinancialRecordResponse> getCurrentFinancialRecord(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(onboardingService.getCurrentFinancialRecord(bankCustomerId));
	}

	@GetMapping("/{bankCustomerId}/financial-records/history")
	@Operation(
		summary = "Get financial record history",
		description = "Returns all financial snapshots (revisions) for the given bank customer, showing the evolution of financial data over time. Only the assigned bank officer can view financial records for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Financial record history retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Bank customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer")
		}
	)
	public ResponseEntity<List<BankCustomerFinancialRecordSummaryResponse>> getFinancialRecordHistory(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(onboardingService.getFinancialRecordHistory(bankCustomerId));
	}

	@GetMapping("/{bankCustomerId}/financial-records/{bankRecordId}")
	@Operation(
		summary = "Get financial record by id",
		description = "Returns a specific financial snapshot (revision) with complete incomes, loans, cards, liabilities, and missed payments. Only the assigned bank officer can view financial records for their customers.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Financial record retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Financial record not found for the bank customer"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer")
		}
	)
	public ResponseEntity<BankCustomerFinancialRecordResponse> getFinancialRecordById(
		@PathVariable Long bankCustomerId,
		@PathVariable Long bankRecordId
	) {
		return ResponseEntity.ok(onboardingService.getFinancialRecordById(bankCustomerId, bankRecordId));
	}

	@GetMapping("/accounts/verify")
	@Operation(
		summary = "Verify account number",
		description = "Checks whether an account exists in accounts table and returns its status for onboarding verification.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Verification result returned"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	public ResponseEntity<AccountVerificationResponse> verifyAccount(@RequestParam String accountNumber) {
		return ResponseEntity.ok(onboardingService.verifyAccount(accountNumber));
	}
}
