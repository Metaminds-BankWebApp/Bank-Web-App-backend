package com.bank_web_app.backend.admin.controller;

import com.bank_web_app.backend.admin.dto.response.AdminBankOfficerSummaryResponse;
import com.bank_web_app.backend.admin.service.AdminBankOfficerService;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	public ResponseEntity<UserRegistrationStepResponse> createDraft(@Valid @RequestBody UserRegistrationStepOneRequest request) {
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
	public ResponseEntity<UserRegistrationStepResponse> create(@Valid @RequestBody UserRegistrationStepOneRequest request) {
		return ResponseEntity.ok(adminBankOfficerService.create(request));
	}

	@GetMapping
	@Operation(summary = "Get all bank officers", description = "Returns all bank officers.")
	public ResponseEntity<List<AdminBankOfficerSummaryResponse>> getAll() {
		return ResponseEntity.ok(adminBankOfficerService.getAll());
	}

	@PatchMapping("/{userId}/status")
	@Operation(
		summary = "Update bank officer status",
		description = "Updates user status of a bank officer to ACTIVE, INACTIVE, or LOCKED."
	)
	public ResponseEntity<AdminBankOfficerSummaryResponse> updateStatus(
		@PathVariable Long userId,
		@RequestParam String status
	) {
		return ResponseEntity.ok(adminBankOfficerService.updateStatus(userId, status));
	}

	@DeleteMapping("/{userId}")
	@Operation(
		summary = "Deactivate bank officer",
		description = "Soft-deactivates a bank officer by setting user status to INACTIVE."
	)
	public ResponseEntity<AdminBankOfficerSummaryResponse> deactivate(@PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.OK).body(adminBankOfficerService.deactivate(userId));
	}
}
