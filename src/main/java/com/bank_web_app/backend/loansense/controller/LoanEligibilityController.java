package com.bank_web_app.backend.loansense.controller;

import com.bank_web_app.backend.loansense.dto.response.LoanSenseEvaluationResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseHistoryItemResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanTypeDetailResponse;
import com.bank_web_app.backend.loansense.service.LoanEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
