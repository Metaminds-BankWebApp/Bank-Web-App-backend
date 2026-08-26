package com.bank_web_app.backend.bankofficer.controller;

import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerStepOnePrefillResponse;
import com.bank_web_app.backend.bankofficer.dto.request.BankCustomerStepOneUpdateRequest;
import com.bank_web_app.backend.bankofficer.dto.request.BankCustomerContactUpdateRequest;
import com.bank_web_app.backend.bankofficer.service.BankOfficerCustomerOnboardingService;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.GeneratedBankCustomerCredentialsResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController         //this is a controller class that handles HTTP requests related to the bank officer's customer creation.
@RequestMapping("/api/bank-officers/customers")     //all methods in this class handle requests that start with /api/bank-officers/customers
@Tag(name = "Bank Officer Customers", description = "Bank officer-owned bank customer onboarding endpoints")
public class BankOfficerCustomerController {

	private final BankOfficerCustomerOnboardingService onboardingService;

	public BankOfficerCustomerController(BankOfficerCustomerOnboardingService onboardingService) {
		this.onboardingService = onboardingService;
	}

	@PostMapping("/step-1/draft")
	@Operation(
		summary = "Save BANK_CUSTOMER step-1 draft (NIC check performed)",
		description = "Authenticated bank officer creates BANK_CUSTOMER draft. The NIC provided is checked for duplicates — if an existing user with the same NIC/email/username exists a 409 Conflict is returned. Officer and branch are resolved from the logged-in user; account must already exist for the provided account number.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: authenticated user is not a bank officer"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> saveDraft(@Valid @RequestBody UserRegistrationStepOneRequest request) {
		return ResponseEntity.ok(onboardingService.saveDraft(request));
	}

	@PostMapping("/step-1/continue")
	@Operation(
		summary = "Save and continue BANK_CUSTOMER step-1 (NIC check performed)",
		description = "Authenticated bank officer creates BANK_CUSTOMER and marks as PENDING_STEP_2. The NIC is validated and checked for existing users; conflict responses (409) indicate duplicate NIC/email/username. Officer and branch are resolved from the logged-in user; account must already exist for the provided account number.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: authenticated user is not a bank officer"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> saveAndContinue(@Valid @RequestBody UserRegistrationStepOneRequest request) {
		return ResponseEntity.ok(onboardingService.saveAndContinue(request));
	}

	@GetMapping("/credentials/generate")
	@Operation(
		summary = "Generate BANK_CUSTOMER username and temporary password",
		description = "Creates a unique username using first name and last name, then returns a temporary password suggestion for officer onboarding.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Generated credentials returned successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: authenticated user is not a bank officer")
		}
	)
	public ResponseEntity<GeneratedBankCustomerCredentialsResponse> generateCredentials(
		@RequestParam String firstName,
		@RequestParam String lastName
	) {
		return ResponseEntity.ok(onboardingService.generateBankCustomerCredentials(firstName, lastName));
	}

	@GetMapping("/step-1/by-nic")
	@Operation(
		summary = "Get existing BANK_CUSTOMER step-1 data by NIC (NIC ownership check)",
		description = "Returns step-1 details for an existing bank customer owned by the logged-in bank officer. Use this to prefill forms before updating instead of creating duplicate rows. This endpoint verifies NIC ownership — it only returns data for customers assigned to the logged-in officer and responds 404 when NIC is not found or owned by another officer.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Existing bank customer step-1 data retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: authenticated user is not a bank officer"),
			@ApiResponse(responseCode = "404", description = "NIC not found for this bank officer")
		}
	)
	public ResponseEntity<BankOfficerCustomerStepOnePrefillResponse> getOwnedStepOneByNic(@RequestParam String nic) {
		return ResponseEntity.ok(onboardingService.getOwnedBankCustomerStepOneByNic(nic));
	}

	@PutMapping("/{bankCustomerId}/step-1/draft")
	@Operation(
		summary = "Update BANK_CUSTOMER step-1 draft (NIC uniqueness enforced)",
		description = "Updates an existing bank customer step-1 record in DRAFT state. NIC, email and username uniqueness is enforced to avoid duplicates; a 409 Conflict is returned when conflicts are detected. This operation is used when editing an existing draft entry.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Step-1 draft updated successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer"),
			@ApiResponse(responseCode = "404", description = "Bank customer not found"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, username, or account already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> updateDraft(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerStepOneUpdateRequest request
	) {
		return ResponseEntity.ok(onboardingService.updateStepOneDraft(bankCustomerId, request));
	}

	@PutMapping("/{bankCustomerId}/step-1/continue")
	@Operation(
		summary = "Update BANK_CUSTOMER step-1 and continue (NIC uniqueness enforced)",
		description = "Updates an existing bank customer step-1 record and sets onboarding state to PENDING_STEP_2. NIC/email/username uniqueness is validated and will return 409 on conflict. Use this when editing an existing step-1 and advancing the onboarding state.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Step-1 updated successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: bank customer is not assigned to this officer"),
			@ApiResponse(responseCode = "404", description = "Bank customer not found"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, username, or account already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> updateAndContinue(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerStepOneUpdateRequest request
	) {
		return ResponseEntity.ok(onboardingService.updateStepOneAndContinue(bankCustomerId, request));
	}

	@PutMapping("/{bankCustomerId}/contact-details")
	@Operation(summary = "Update completed-customer contact details", description = "Updates email, mobile, province, and address for an assigned completed customer. Legal name, NIC, DOB, username, and account ownership are excluded and require their respective controlled processes.")
	public ResponseEntity<UserRegistrationStepResponse> updateCompletedCustomerContactDetails(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody BankCustomerContactUpdateRequest request
	) {
		return ResponseEntity.ok(onboardingService.updateCompletedCustomerContactDetails(bankCustomerId, request));
	}
}
