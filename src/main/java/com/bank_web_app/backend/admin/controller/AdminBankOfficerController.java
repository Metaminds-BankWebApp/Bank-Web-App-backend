package com.bank_web_app.backend.admin.controller;
import com.bank_web_app.backend.admin.dto.request.AdminBankOfficerUpdateRequest;
import com.bank_web_app.backend.admin.dto.request.AdminBankOfficerUsernameGenerationRequest;
import com.bank_web_app.backend.admin.dto.response.AdminBankOfficerGeneratedPasswordResponse;
import com.bank_web_app.backend.admin.dto.response.AdminBankOfficerGeneratedUsernameResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for bank-officer onboarding, listing, updates, and lifecycle actions.
 */

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
	// Creates a draft officer account preview before final submission.
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
	// Creates a new entity from validated request data.
	public ResponseEntity<UserRegistrationStepResponse> create(@Valid @RequestBody UserRegistrationStepOneRequest request) {
		return ResponseEntity.ok(adminBankOfficerService.create(request));
	}

	@PostMapping("/credentials/username")
	@Operation(
		summary = "Generate bank officer username",
		description = "Generates a backend-owned suggested username for BANK_OFFICER onboarding.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Username generated successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	// Generates a unique username for officer onboarding.
	public ResponseEntity<AdminBankOfficerGeneratedUsernameResponse> generateUsername(
		@Valid @RequestBody AdminBankOfficerUsernameGenerationRequest request
	) {
		String username = adminBankOfficerService.generateSuggestedUsername(request.firstName(), request.lastName());
		return ResponseEntity.ok(new AdminBankOfficerGeneratedUsernameResponse(username));
	}

	@GetMapping("/credentials/password")
	@Operation(
		summary = "Generate bank officer password",
		description = "Generates a backend-owned suggested password for BANK_OFFICER onboarding.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Password generated successfully")
		}
	)
	// Generates a strong temporary password for officer onboarding.
	public ResponseEntity<AdminBankOfficerGeneratedPasswordResponse> generatePassword() {
		String password = adminBankOfficerService.generateSuggestedPassword();
		return ResponseEntity.ok(new AdminBankOfficerGeneratedPasswordResponse(password));
	}

	@GetMapping
	@Operation(summary = "Get all bank officers", description = "Returns all bank officers.")
	// Returns all records needed by the admin table view.
	public ResponseEntity<List<AdminBankOfficerSummaryResponse>> getAll() {
		return ResponseEntity.ok(adminBankOfficerService.getAll());
	}

	@PatchMapping("/{userId}/status")
	@Operation(
		summary = "Update bank officer status",
		description = "Updates user status of a bank officer to ACTIVE, INACTIVE, or LOCKED."
	)
	// Updates only the status field for the selected record.
	public ResponseEntity<AdminBankOfficerSummaryResponse> updateStatus(
		@PathVariable Long userId,
		@RequestParam String status
	) {
		return ResponseEntity.ok(adminBankOfficerService.updateStatus(userId, status));
	}

	@PutMapping("/{userId}")
	@Operation(summary = "Update bank officer details", description = "Updates editable profile details of a bank officer.")
	// Updates an existing record from validated request fields.
	public ResponseEntity<AdminBankOfficerSummaryResponse> update(
		@PathVariable Long userId,
		@Valid @RequestBody AdminBankOfficerUpdateRequest request
	) {
		return ResponseEntity.ok(adminBankOfficerService.update(userId, request));
	}

	@DeleteMapping("/{userId}")
	@Operation(
		summary = "Delete bank officer permanently",
		description = "Permanently deletes a bank officer and linked user account when no dependent records exist."
	)
	// Deletes the selected record after validation and permission checks.
	public ResponseEntity<AdminBankOfficerSummaryResponse> delete(@PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.OK).body(adminBankOfficerService.deletePermanently(userId));
	}
}

