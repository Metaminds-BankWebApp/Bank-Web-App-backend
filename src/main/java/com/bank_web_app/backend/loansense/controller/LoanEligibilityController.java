package com.bank_web_app.backend.loansense.controller;

import com.bank_web_app.backend.loansense.dto.request.CreateLoanSenseEvaluationRequest;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseOfficerDashboardResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseEvaluationResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseHistoryItemResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanTypeDetailResponse;
import com.bank_web_app.backend.loansense.service.LoanEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/loansense")
@Tag(name = "LoanSense", description = "Bank-customer LoanSense eligibility endpoints")
public class LoanEligibilityController {

	private final LoanEligibilityService loanEligibilityService;

	public LoanEligibilityController(LoanEligibilityService loanEligibilityService) {
		this.loanEligibilityService = loanEligibilityService;
	}

	@GetMapping("/bank/current")
	@Operation(summary = "Get the latest LoanSense evaluation for the logged-in bank customer")
	public ResponseEntity<LoanSenseEvaluationResponse> getCurrentBankEvaluation() {
		return ResponseEntity.ok(loanEligibilityService.getCurrentEvaluation());
	}

	@GetMapping("/bank/history")
	@Operation(summary = "Get LoanSense history rows for the logged-in bank customer")
	public ResponseEntity<List<LoanSenseHistoryItemResponse>> getBankHistory(
		@RequestParam(required = false) String loanType,
		@RequestParam(required = false) Integer months
	) {
		return ResponseEntity.ok(loanEligibilityService.getHistory(loanType, months));
	}

	@GetMapping("/bank/evaluations/{loansenseEvaluationId}")
	@Operation(summary = "Get a LoanSense evaluation by id for the logged-in bank customer")
	public ResponseEntity<LoanSenseEvaluationResponse> getBankEvaluationById(@PathVariable Long loansenseEvaluationId) {
		return ResponseEntity.ok(loanEligibilityService.getEvaluationById(loansenseEvaluationId));
	}

	@GetMapping("/bank/loan-types/{loanType}")
	@Operation(summary = "Get LoanSense detail for a specific loan type for the logged-in bank customer")
	public ResponseEntity<LoanTypeDetailResponse> getCurrentBankLoanTypeDetail(@PathVariable String loanType) {
		return ResponseEntity.ok(loanEligibilityService.getCurrentLoanTypeDetail(loanType));
	}

	@PostMapping("/officer/customers/{bankCustomerId}/evaluations")
	@Operation(summary = "Initiate a LoanSense evaluation for a bank customer owned by the logged-in bank officer")
	public ResponseEntity<LoanSenseEvaluationResponse> createEvaluationForOfficer(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody(required = false) CreateLoanSenseEvaluationRequest request
	) {
		return ResponseEntity.ok(loanEligibilityService.createEvaluationForOfficer(bankCustomerId, request));
	}

	@GetMapping("/officer/customers/{bankCustomerId}/current")
	@Operation(summary = "Get the latest LoanSense evaluation for a bank customer owned by the logged-in bank officer")
	public ResponseEntity<LoanSenseEvaluationResponse> getCurrentEvaluationForOfficer(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(loanEligibilityService.getCurrentEvaluationForOfficer(bankCustomerId));
	}

	@GetMapping("/officer/customers/{bankCustomerId}/history")
	@Operation(summary = "Get LoanSense history rows for a bank customer owned by the logged-in bank officer")
	public ResponseEntity<List<LoanSenseHistoryItemResponse>> getHistoryForOfficer(
		@PathVariable Long bankCustomerId,
		@RequestParam(required = false) String loanType,
		@RequestParam(required = false) Integer months
	) {
		return ResponseEntity.ok(loanEligibilityService.getHistoryForOfficer(bankCustomerId, loanType, months));
	}

	@GetMapping("/officer/customers/{bankCustomerId}/evaluations/{loansenseEvaluationId}")
	@Operation(summary = "Get a LoanSense evaluation by id for a bank customer owned by the logged-in bank officer")
	public ResponseEntity<LoanSenseEvaluationResponse> getEvaluationByIdForOfficer(
		@PathVariable Long bankCustomerId,
		@PathVariable Long loansenseEvaluationId
	) {
		return ResponseEntity.ok(loanEligibilityService.getEvaluationByIdForOfficer(bankCustomerId, loansenseEvaluationId));
	}

	@GetMapping("/officer/customers/{bankCustomerId}/loan-types/{loanType}")
	@Operation(summary = "Get LoanSense detail for a loan type for a bank customer owned by the logged-in bank officer")
	public ResponseEntity<LoanTypeDetailResponse> getLoanTypeDetailForOfficer(
		@PathVariable Long bankCustomerId,
		@PathVariable String loanType
	) {
		return ResponseEntity.ok(loanEligibilityService.getLoanTypeDetailForOfficer(bankCustomerId, loanType));
	}

	@GetMapping("/officer/dashboard")
	@Operation(summary = "Get LoanSense dashboard summary for the logged-in bank officer")
	public ResponseEntity<LoanSenseOfficerDashboardResponse> getOfficerDashboard() {
		return ResponseEntity.ok(loanEligibilityService.getOfficerDashboard());
	}

	@GetMapping("/current")
	@Operation(summary = "Get the latest LoanSense evaluation for the logged-in bank customer")
	public ResponseEntity<LoanSenseEvaluationResponse> getCurrentEvaluation() {
		return ResponseEntity.ok(loanEligibilityService.getCurrentEvaluation());
	}

	@GetMapping("/history")
	@Operation(summary = "Get LoanSense history rows for the logged-in bank customer")
	public ResponseEntity<List<LoanSenseHistoryItemResponse>> getHistory(
		@RequestParam(required = false) String loanType,
		@RequestParam(required = false) Integer months
	) {
		return ResponseEntity.ok(loanEligibilityService.getHistory(loanType, months));
	}

	@GetMapping("/evaluations/{loansenseEvaluationId}")
	@Operation(summary = "Get a LoanSense evaluation by id for the logged-in bank customer")
	public ResponseEntity<LoanSenseEvaluationResponse> getEvaluationById(@PathVariable Long loansenseEvaluationId) {
		return ResponseEntity.ok(loanEligibilityService.getEvaluationById(loansenseEvaluationId));
	}

	@GetMapping("/loan-types/{loanType}")
	@Operation(summary = "Get LoanSense detail for a specific loan type for the logged-in bank customer")
	public ResponseEntity<LoanTypeDetailResponse> getCurrentLoanTypeDetail(@PathVariable String loanType) {
		return ResponseEntity.ok(loanEligibilityService.getCurrentLoanTypeDetail(loanType));
	}
}
