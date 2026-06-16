package com.bank_web_app.backend.bankofficer.controller;

import com.bank_web_app.backend.bankofficer.dto.request.BankOfficerCustomerFilterRequest;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerSummaryResponse;
import com.bank_web_app.backend.bankofficer.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-officers/customers")
@Tag(name = "Bank Officer Portfolio", description = "All-customers page endpoints for bank officers.")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@GetMapping
	@Operation(
		summary = "Get all bank customers",
		description = "Returns the bank officer's customer portfolio for the all-customers page, with optional search, status, risk, and sort filters.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Portfolio returned successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized: bank officer authentication is required"),
			@ApiResponse(responseCode = "403", description = "Forbidden: logged-in user is not a bank officer")
		}
	)
	public ResponseEntity<List<BankOfficerCustomerSummaryResponse>> getAll(
		@RequestParam(required = false) String search,
		@RequestParam(required = false) String status,
		@RequestParam(required = false) String riskLevel,
		@RequestParam(required = false) String sortBy
	) {

		// The controller accepts simple query parameters and forwards them to the
		// service as a typed filter request. Filtering, sorting and risk-based
		// selection are performed on the server side inside `PortfolioService`.
		return ResponseEntity.ok(
			portfolioService.getBankCustomersForOfficer(
				new BankOfficerCustomerFilterRequest(search, status, riskLevel, sortBy)
			)
		);
	}
}
