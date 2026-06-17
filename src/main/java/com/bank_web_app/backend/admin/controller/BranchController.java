package com.bank_web_app.backend.admin.controller;
import java.util.List;

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

import com.bank_web_app.backend.admin.dto.request.BranchRequest;
import com.bank_web_app.backend.admin.dto.response.BranchResponse;
import com.bank_web_app.backend.admin.service.BranchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for branch creation, listing, updates, status changes, and deletion.
 */

@RestController
@RequestMapping("/api/admin/branches")
@Tag(name = "Admin Branches", description = "Admin branch management endpoints")
public class BranchController {

	private final BranchService branchService;

	public BranchController(BranchService branchService) {
		this.branchService = branchService;
	}

	@PostMapping
	@Operation(
		summary = "Create branch",
		description = "Creates a new bank branch. Branch code is auto-generated in the backend. Database branchId is internal only.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Branch created successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	// Creates a new entity from validated request data.
	public ResponseEntity<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
		return ResponseEntity.ok(branchService.create(request));
	}

	@GetMapping
	@Operation(summary = "Get all branches", description = "Returns all branches.")
	// Returns all records needed by the admin table view.
	public ResponseEntity<List<BranchResponse>> getAll() {
		return ResponseEntity.ok(branchService.getAll());
	}

	@GetMapping("/{branchId}")
	@Operation(summary = "Get branch by id", description = "Returns a branch by internal database branch id.")
	// Returns one record by its identifier.
	public ResponseEntity<BranchResponse> getById(@PathVariable Long branchId) {
		return ResponseEntity.ok(branchService.getById(branchId));
	}

	@PutMapping("/{branchId}")
	@Operation(summary = "Update branch", description = "Updates full branch details using the internal database branch id.")
	// Updates an existing record from validated request fields.
	public ResponseEntity<BranchResponse> update(@PathVariable Long branchId, @Valid @RequestBody BranchRequest request) {
		return ResponseEntity.ok(branchService.update(branchId, request));
	}

	@PatchMapping("/{branchId}/status")
	@Operation(summary = "Update branch status", description = "Updates branch status to ACTIVE, INACTIVE, or MAINTENANCE.")
	// Updates only the status field for the selected record.
	public ResponseEntity<BranchResponse> updateStatus(@PathVariable Long branchId, @RequestParam String status) {
		return ResponseEntity.ok(branchService.updateStatus(branchId, status));
	}

	@DeleteMapping("/{branchId}")
	@Operation(summary = "Delete branch permanently", description = "Deletes a branch permanently when no linked officers or customers exist.")
	// Deletes the selected record after validation and permission checks.
	public ResponseEntity<BranchResponse> delete(@PathVariable Long branchId) {
		return ResponseEntity.ok(branchService.delete(branchId));
	}
}

