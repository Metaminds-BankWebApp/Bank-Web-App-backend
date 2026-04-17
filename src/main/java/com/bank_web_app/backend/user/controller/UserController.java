package com.bank_web_app.backend.user.controller;

import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import com.bank_web_app.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Registration", description = "Role-aware user registration endpoints.")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/bank-customer/step-1/draft")
	@Operation(
		summary = "Save Bank Customer step-1 draft",
		description = "Stores first-step personal information for BANK_CUSTOMER registration in users table with DRAFT state.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> saveBankCustomerStepOneDraft(
		@Valid @RequestBody BankCustomerStepOneRequest request
	) {
		return ResponseEntity.ok(userService.saveBankCustomerStepOneDraft(request));
	}

	@PostMapping("/bank-customer/step-1/continue")
	@Operation(
		summary = "Save and continue Bank Customer step-1",
		description = "Stores first-step personal information for BANK_CUSTOMER registration in users table and marks state as PENDING_STEP_2.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> continueBankCustomerStepOne(
		@Valid @RequestBody BankCustomerStepOneRequest request
	) {
		return ResponseEntity.ok(userService.continueBankCustomerStepOne(request));
	}

	@PostMapping("/public-customer/step-1/draft")
	@Operation(
		summary = "Save Public Customer step-1 draft",
		description = "Stores first-step personal information for PUBLIC_CUSTOMER registration in users table with DRAFT state.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> savePublicCustomerStepOneDraft(
		@Valid @RequestBody BankCustomerStepOneRequest request
	) {
		return ResponseEntity.ok(userService.savePublicCustomerStepOneDraft(request));
	}

	@PostMapping("/public-customer/step-1/continue")
	@Operation(
		summary = "Save and continue Public Customer step-1",
		description = "Stores first-step personal information for PUBLIC_CUSTOMER registration in users table and marks state as PENDING_STEP_2.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> continuePublicCustomerStepOne(
		@Valid @RequestBody BankCustomerStepOneRequest request
	) {
		return ResponseEntity.ok(userService.continuePublicCustomerStepOne(request));
	}

	@PostMapping("/bank-officer/step-1/draft")
	@Operation(
		summary = "Save Bank Officer step-1 draft",
		description = "Stores first-step personal information for BANK_OFFICER registration in users table with DRAFT state.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> saveBankOfficerStepOneDraft(
		@Valid @RequestBody BankCustomerStepOneRequest request
	) {
		return ResponseEntity.ok(userService.saveBankOfficerStepOneDraft(request));
	}

	@PostMapping("/bank-officer/step-1/continue")
	@Operation(
		summary = "Save and continue Bank Officer step-1",
		description = "Stores first-step personal information for BANK_OFFICER registration in users table and marks state as PENDING_STEP_2.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> continueBankOfficerStepOne(
		@Valid @RequestBody BankCustomerStepOneRequest request
	) {
		return ResponseEntity.ok(userService.continueBankOfficerStepOne(request));
	}

	@GetMapping("/bank-officer/customers")
	@Operation(
		summary = "Get all bank customers",
		description = "Returns all BANK_CUSTOMER users for the bank officer all-customers table.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Customers retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden")
		}
	)
	public ResponseEntity<List<BankCustomerSummaryResponse>> getBankCustomersForOfficer() {
		return ResponseEntity.ok(userService.getBankCustomersForOfficer());
	}
}
