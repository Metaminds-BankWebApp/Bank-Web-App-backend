package com.bank_web_app.backend.admin.controller;
import com.bank_web_app.backend.admin.dto.request.BulkLoanPolicyInterestRateUpdateRequest;
import com.bank_web_app.backend.admin.dto.request.LoanPolicyUpdateRequest;
import com.bank_web_app.backend.admin.dto.response.LoanPolicyResponse;
import com.bank_web_app.backend.admin.service.LoanPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for viewing and updating loan-policy records and interest-rate changes.
 */

@RestController
@RequestMapping("/api/admin/loan-policies")
@Tag(name = "Admin Loan Policies", description = "LoanSense loan policy management endpoints")
public class LoanPolicyController {

	private final LoanPolicyService loanPolicyService;

	public LoanPolicyController(LoanPolicyService loanPolicyService) {
		this.loanPolicyService = loanPolicyService;
	}

	@GetMapping
	@Operation(summary = "Get all LoanSense loan policies")
	// Returns all records needed by the admin table view.
	public ResponseEntity<List<LoanPolicyResponse>> getAll() {
		return ResponseEntity.ok(loanPolicyService.getAll());
	}

	@GetMapping("/{policyId}")
	@Operation(summary = "Get a LoanSense loan policy by id")
	// Returns one record by its identifier.
	public ResponseEntity<LoanPolicyResponse> getById(@PathVariable Long policyId) {
		return ResponseEntity.ok(loanPolicyService.getById(policyId));
	}

	@PutMapping("/{policyId}")
	@Operation(summary = "Update a LoanSense loan policy")
	// Updates an existing record from validated request fields.
	public ResponseEntity<LoanPolicyResponse> update(@PathVariable Long policyId, @Valid @RequestBody LoanPolicyUpdateRequest request) {
		return ResponseEntity.ok(loanPolicyService.update(policyId, request));
	}

	@PutMapping("/interest-rates")
	@Operation(summary = "Update LoanSense loan policy interest rates in bulk")
	// Bulk-updates base interest rates for loan policy records.
	public ResponseEntity<List<LoanPolicyResponse>> updateInterestRates(
		@Valid @RequestBody BulkLoanPolicyInterestRateUpdateRequest request
	) {
		return ResponseEntity.ok(loanPolicyService.updateInterestRates(request));
	}
}

