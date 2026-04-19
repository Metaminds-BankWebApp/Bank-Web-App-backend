package com.bank_web_app.backend.bankofficer.controller;

import com.bank_web_app.backend.bankofficer.service.CustomerOnboardingService;
import com.bank_web_app.backend.user.dto.request.BankCustomerStepOneRequest;
import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-officers")
public class CustomerOnboardingController {

	private final CustomerOnboardingService customerOnboardingService;

	public CustomerOnboardingController(CustomerOnboardingService customerOnboardingService) {
		this.customerOnboardingService = customerOnboardingService;
	}

	@PostMapping("/draft")
	public ResponseEntity<UserRegistrationStepResponse> createDraft(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(customerOnboardingService.createOfficerDraft(request));
	}

	@PostMapping
	public ResponseEntity<UserRegistrationStepResponse> create(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(customerOnboardingService.createOfficer(request));
	}

	@GetMapping
	public ResponseEntity<List<BankCustomerSummaryResponse>> getAll() {
		return ResponseEntity.ok(customerOnboardingService.getBankOfficers());
	}
}
