package com.bank_web_app.backend.creditlens.controller;

import com.bank_web_app.backend.creditlens.dto.request.CreateBankCreditEvaluationRequest;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisCustomerProfileResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisDashboardResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditEvaluationResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditEvaluationSummaryResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditDashboardResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditInsightsResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditReportResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditTrendResponse;
import com.bank_web_app.backend.creditlens.dto.response.SelfCreditEvaluationResponse;
import com.bank_web_app.backend.creditlens.dto.response.SelfCreditEvaluationSummaryResponse;
import com.bank_web_app.backend.creditlens.service.CreditEvaluationService;
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
@RequestMapping("/api/creditlens")
@Tag(name = "CreditLens", description = "Self and bank credit evaluation endpoints.")
public class CreditEvaluationController {

	private final CreditEvaluationService creditEvaluationService;

	public CreditEvaluationController(CreditEvaluationService creditEvaluationService) {
		this.creditEvaluationService = creditEvaluationService;
	}

	@PostMapping("/public/evaluations")
	@Operation(summary = "Generate a self credit evaluation for the logged-in public customer.")
	public ResponseEntity<SelfCreditEvaluationResponse> createSelfEvaluation() {
		return ResponseEntity.ok(creditEvaluationService.createSelfEvaluation());
	}

	@GetMapping("/public/current")
	@Operation(summary = "Get the latest self credit evaluation for the logged-in public customer.")
	public ResponseEntity<SelfCreditEvaluationResponse> getCurrentSelfEvaluation() {
		return ResponseEntity.ok(creditEvaluationService.getCurrentSelfEvaluation());
	}

	@GetMapping("/public/dashboard")
	@Operation(summary = "Get the CreditLens dashboard data for the logged-in public customer.")
	public ResponseEntity<CreditDashboardResponse> getPublicDashboard() {
		return ResponseEntity.ok(creditEvaluationService.getPublicDashboard());
	}

	@GetMapping("/public/trends")
	@Operation(summary = "Get the CreditLens trend data for the logged-in public customer.")
	public ResponseEntity<CreditTrendResponse> getPublicTrends(@RequestParam(defaultValue = "6m") String range) {
		return ResponseEntity.ok(creditEvaluationService.getPublicTrends(range));
	}

	@GetMapping({"/public/insight", "/public/insights"})
	@Operation(summary = "Get the CreditLens insight cards for the logged-in public customer.")
	public ResponseEntity<CreditInsightsResponse> getPublicInsights() {
		return ResponseEntity.ok(creditEvaluationService.getPublicInsights());
	}

	@GetMapping("/public/report")
	@Operation(summary = "Get the CreditLens monthly report data for the logged-in public customer.")
	public ResponseEntity<CreditReportResponse> getPublicReport() {
		return ResponseEntity.ok(creditEvaluationService.getPublicReport());
	}

	@GetMapping("/public/history")
	@Operation(summary = "Get self credit evaluation history for the logged-in public customer.")
	public ResponseEntity<List<SelfCreditEvaluationSummaryResponse>> getSelfEvaluationHistory() {
		return ResponseEntity.ok(creditEvaluationService.getSelfEvaluationHistory());
	}

	@GetMapping("/public/evaluations/{selfEvaluationId}")
	@Operation(summary = "Get a specific self credit evaluation for the logged-in public customer.")
	public ResponseEntity<SelfCreditEvaluationResponse> getSelfEvaluationById(@PathVariable Long selfEvaluationId) {
		return ResponseEntity.ok(creditEvaluationService.getSelfEvaluationById(selfEvaluationId));
	}

	@GetMapping("/bank/current")
	@Operation(summary = "Get the latest bank credit evaluation for the logged-in bank customer.")
	public ResponseEntity<BankCreditEvaluationResponse> getCurrentBankEvaluationForCustomer() {
		return ResponseEntity.ok(creditEvaluationService.getCurrentBankEvaluationForCustomer());
	}

	@GetMapping("/bank/dashboard")
	@Operation(summary = "Get the CreditLens dashboard data for the logged-in bank customer.")
	public ResponseEntity<CreditDashboardResponse> getBankDashboard() {
		return ResponseEntity.ok(creditEvaluationService.getBankDashboard());
	}

	@GetMapping("/bank/trends")
	@Operation(summary = "Get the CreditLens trend data for the logged-in bank customer.")
	public ResponseEntity<CreditTrendResponse> getBankTrends(@RequestParam(defaultValue = "6m") String range) {
		return ResponseEntity.ok(creditEvaluationService.getBankTrends(range));
	}

	@GetMapping({"/bank/insight", "/bank/insights"})
	@Operation(summary = "Get the CreditLens insight cards for the logged-in bank customer.")
	public ResponseEntity<CreditInsightsResponse> getBankInsights() {
		return ResponseEntity.ok(creditEvaluationService.getBankInsights());
	}

	@GetMapping("/bank/report")
	@Operation(summary = "Get the CreditLens monthly report data for the logged-in bank customer.")
	public ResponseEntity<CreditReportResponse> getBankReport() {
		return ResponseEntity.ok(creditEvaluationService.getBankReport());
	}

	@GetMapping("/bank/history")
	@Operation(summary = "Get bank credit evaluation history for the logged-in bank customer.")
	public ResponseEntity<List<BankCreditEvaluationSummaryResponse>> getBankEvaluationHistoryForCustomer() {
		return ResponseEntity.ok(creditEvaluationService.getBankEvaluationHistoryForCustomer());
	}

	@GetMapping("/bank/evaluations/{bankEvaluationId}")
	@Operation(summary = "Get a specific bank credit evaluation for the logged-in bank customer.")
	public ResponseEntity<BankCreditEvaluationResponse> getBankEvaluationByIdForCustomer(@PathVariable Long bankEvaluationId) {
		return ResponseEntity.ok(creditEvaluationService.getBankEvaluationByIdForCustomer(bankEvaluationId));
	}

	@GetMapping("/officer/dashboard")
	@Operation(summary = "Get the credit-analysis dashboard for the logged-in bank officer.")
	public ResponseEntity<BankCreditAnalysisDashboardResponse> getOfficerDashboard() {
		return ResponseEntity.ok(creditEvaluationService.getOfficerDashboard());
	}

	@GetMapping("/officer/customers/{bankCustomerId}/profile")
	@Operation(summary = "Get the credit-analysis customer profile for a bank customer owned by the logged-in bank officer.")
	public ResponseEntity<BankCreditAnalysisCustomerProfileResponse> getOfficerCustomerProfile(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(creditEvaluationService.getOfficerCustomerProfile(bankCustomerId));
	}

	@PostMapping("/officer/customers/{bankCustomerId}/evaluations")
	@Operation(summary = "Create a bank credit evaluation for a bank customer owned by the logged-in bank officer.")
	public ResponseEntity<BankCreditEvaluationResponse> createBankEvaluationForOfficer(
		@PathVariable Long bankCustomerId,
		@Valid @RequestBody(required = false) CreateBankCreditEvaluationRequest request
	) {
		return ResponseEntity.ok(creditEvaluationService.createBankEvaluationForOfficer(bankCustomerId, request));
	}

	@GetMapping("/officer/customers/{bankCustomerId}/current")
	@Operation(summary = "Get the latest bank credit evaluation for a bank customer owned by the logged-in bank officer.")
	public ResponseEntity<BankCreditEvaluationResponse> getCurrentBankEvaluationForOfficer(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(creditEvaluationService.getCurrentBankEvaluationForOfficer(bankCustomerId));
	}

	@GetMapping("/officer/customers/{bankCustomerId}/history")
	@Operation(summary = "Get bank credit evaluation history for a bank customer owned by the logged-in bank officer.")
	public ResponseEntity<List<BankCreditEvaluationSummaryResponse>> getBankEvaluationHistoryForOfficer(@PathVariable Long bankCustomerId) {
		return ResponseEntity.ok(creditEvaluationService.getBankEvaluationHistoryForOfficer(bankCustomerId));
	}

	@GetMapping("/officer/customers/{bankCustomerId}/evaluations/{bankEvaluationId}")
	@Operation(summary = "Get a specific bank credit evaluation for a bank customer owned by the logged-in bank officer.")
	public ResponseEntity<BankCreditEvaluationResponse> getBankEvaluationByIdForOfficer(
		@PathVariable Long bankCustomerId,
		@PathVariable Long bankEvaluationId
	) {
		return ResponseEntity.ok(creditEvaluationService.getBankEvaluationByIdForOfficer(bankCustomerId, bankEvaluationId));
	}
}
