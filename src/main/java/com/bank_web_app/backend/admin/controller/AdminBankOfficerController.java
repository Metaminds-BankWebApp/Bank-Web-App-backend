package com.bank_web_app.backend.admin.controller;

import com.bank_web_app.backend.admin.service.AdminBankOfficerService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bank-officers")
@Tag(name = "Admin Bank Officers", description = "Admin-owned bank officer onboarding endpoints")
public class AdminBankOfficerController {

	private final AdminBankOfficerService adminBankOfficerService;

	public AdminBankOfficerController(AdminBankOfficerService adminBankOfficerService) {
		this.adminBankOfficerService = adminBankOfficerService;
	}

	@PostMapping("/draft")
	@Operation(
		summary = "Save BANK_OFFICER draft",
		description = "Admin creates BANK_OFFICER draft.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> createDraft(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(adminBankOfficerService.createDraft(request));
	}

	@PostMapping
	@Operation(
		summary = "Create BANK_OFFICER",
		description = "Admin creates BANK_OFFICER with SUCCESS state.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Bank officer created successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> create(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(adminBankOfficerService.create(request));
	}

	@GetMapping
	@Operation(summary = "Get all bank officers", description = "Returns all bank officers.")
	public ResponseEntity<List<BankCustomerSummaryResponse>> getAll() {
		return ResponseEntity.ok(adminBankOfficerService.getAll());
	}
}
