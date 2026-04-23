package com.bank_web_app.backend.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
		return ResponseEntity.ok(branchService.create(request));
	}

	@GetMapping
	@Operation(summary = "Get all branches", description = "Returns all branches.")
	public ResponseEntity<List<BranchResponse>> getAll() {
		return ResponseEntity.ok(branchService.getAll());
	}

	@GetMapping("/{branchId}")
	@Operation(summary = "Get branch by id", description = "Returns a branch by internal database branch id.")
	public ResponseEntity<BranchResponse> getById(@PathVariable Long branchId) {
		return ResponseEntity.ok(branchService.getById(branchId));
	}

	@PutMapping("/{branchId}")
	@Operation(summary = "Update branch", description = "Updates full branch details using the internal database branch id.")
	public ResponseEntity<BranchResponse> update(@PathVariable Long branchId, @Valid @RequestBody BranchRequest request) {
		return ResponseEntity.ok(branchService.update(branchId, request));
	}

	@PatchMapping("/{branchId}/status")
	@Operation(summary = "Update branch status", description = "Updates branch status to ACTIVE, INACTIVE, or MAINTENANCE.")
	public ResponseEntity<BranchResponse> updateStatus(@PathVariable Long branchId, @RequestParam String status) {
		return ResponseEntity.ok(branchService.updateStatus(branchId, status));
	}
}
