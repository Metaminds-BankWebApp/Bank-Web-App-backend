package com.bank_web_app.backend.publiccustomer.controller;

import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerCardStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerIncomeStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLiabilityStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLoanStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialStepResponse;
import com.bank_web_app.backend.publiccustomer.service.PublicCustomerService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public-customers")
@Tag(name = "Public Customers", description = "PUBLIC_CUSTOMER onboarding and listing endpoints.")
public class PublicCustomerController {

	private final PublicCustomerService publicCustomerService;

	public PublicCustomerController(PublicCustomerService publicCustomerService) {
		this.publicCustomerService = publicCustomerService;
	}

	@PostMapping("/draft")
	@Operation(
		summary = "Save PUBLIC_CUSTOMER draft",
		description = "Creates PUBLIC_CUSTOMER user and profile with DRAFT state.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Draft saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> saveDraft(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(publicCustomerService.saveDraft(request));
	}

	@PostMapping
	@Operation(
		summary = "Register PUBLIC_CUSTOMER",
		description = "Creates PUBLIC_CUSTOMER user and profile with SUCCESS state.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Public customer created successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> register(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(publicCustomerService.register(request));
	}

	@GetMapping
	@Operation(
		summary = "Get all public customers",
		description = "Returns PUBLIC_CUSTOMER summaries for customer management views.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Public customers retrieved successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden")
		}
	)
	public ResponseEntity<List<BankCustomerSummaryResponse>> getAll() {
		return ResponseEntity.ok(publicCustomerService.getAll());
	}

	@PutMapping("/{publicCustomerId}/financial-records/steps/income")
	@Operation(
		summary = "Save income step",
		description = "Stores step 1 (income) for the current financial record.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Income step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or public customer not found")
		}
	)
	public ResponseEntity<PublicCustomerFinancialStepResponse> saveIncomeStep(
		@PathVariable Long publicCustomerId,
		@Valid @RequestBody PublicCustomerIncomeStepRequest request
	) {
		return ResponseEntity.ok(publicCustomerService.saveIncomeStep(publicCustomerId, request));
	}

	@PutMapping("/{publicCustomerId}/financial-records/steps/loans")
	@Operation(
		summary = "Save loans step",
		description = "Stores step 2 (loans) for the current financial record.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Loans step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or public customer not found")
		}
	)
	public ResponseEntity<PublicCustomerFinancialStepResponse> saveLoanStep(
		@PathVariable Long publicCustomerId,
		@Valid @RequestBody PublicCustomerLoanStepRequest request
	) {
		return ResponseEntity.ok(publicCustomerService.saveLoanStep(publicCustomerId, request));
	}

	@PutMapping("/{publicCustomerId}/financial-records/steps/cards")
	@Operation(
		summary = "Save cards step",
		description = "Stores step 3 (cards) for the current financial record.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Cards step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or public customer not found")
		}
	)
	public ResponseEntity<PublicCustomerFinancialStepResponse> saveCardStep(
		@PathVariable Long publicCustomerId,
		@Valid @RequestBody PublicCustomerCardStepRequest request
	) {
		return ResponseEntity.ok(publicCustomerService.saveCardStep(publicCustomerId, request));
	}

	@PutMapping("/{publicCustomerId}/financial-records/steps/liabilities")
	@Operation(
		summary = "Save liabilities step",
		description = "Stores step 4 (liabilities + missed payments) for the current financial record.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Liabilities step saved successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed or public customer not found")
		}
	)
	public ResponseEntity<PublicCustomerFinancialStepResponse> saveLiabilityStep(
		@PathVariable Long publicCustomerId,
		@Valid @RequestBody PublicCustomerLiabilityStepRequest request
	) {
		return ResponseEntity.ok(publicCustomerService.saveLiabilityStep(publicCustomerId, request));
	}

	@GetMapping("/{publicCustomerId}/financial-records/current")
	@Operation(
		summary = "Get current financial record",
		description = "Returns the CURRENT financial snapshot for the given public customer.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Current financial record retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Public customer or current record not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden")
		}
	)
	public ResponseEntity<PublicCustomerFinancialRecordResponse> getCurrentFinancialRecord(
		@PathVariable Long publicCustomerId
	) {
		return ResponseEntity.ok(publicCustomerService.getCurrentFinancialRecord(publicCustomerId));
	}

	@GetMapping("/{publicCustomerId}/financial-records/history")
	@Operation(
		summary = "Get financial record history",
		description = "Returns all financial snapshots (CURRENT and ARCHIVED) for the given public customer.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Financial record history retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Public customer not found"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden")
		}
	)
	public ResponseEntity<List<PublicCustomerFinancialRecordSummaryResponse>> getFinancialRecordHistory(
		@PathVariable Long publicCustomerId
	) {
		return ResponseEntity.ok(publicCustomerService.getFinancialRecordHistory(publicCustomerId));
	}

	@GetMapping("/{publicCustomerId}/financial-records/{recordId}")
	@Operation(
		summary = "Get financial record by id",
		description = "Returns a specific financial snapshot with incomes, loans, cards, liabilities, and missed payments.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Financial record retrieved successfully"),
			@ApiResponse(responseCode = "400", description = "Financial record not found for the public customer"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "403", description = "Forbidden")
		}
	)
	public ResponseEntity<PublicCustomerFinancialRecordResponse> getFinancialRecordById(
		@PathVariable Long publicCustomerId,
		@PathVariable Long recordId
	) {
		return ResponseEntity.ok(publicCustomerService.getFinancialRecordById(publicCustomerId, recordId));
	}
}
