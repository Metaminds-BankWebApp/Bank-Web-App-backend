package com.bank_web_app.backend.bankofficer.controller;

import com.bank_web_app.backend.bankofficer.dto.response.AccountVerificationResponse;
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
