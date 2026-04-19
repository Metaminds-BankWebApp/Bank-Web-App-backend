package com.bank_web_app.backend.publiccustomer.controller;

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
import org.springframework.web.bind.annotation.PostMapping;
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
}
