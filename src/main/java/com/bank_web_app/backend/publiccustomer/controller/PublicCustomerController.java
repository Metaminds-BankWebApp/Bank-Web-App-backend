package com.bank_web_app.backend.publiccustomer.controller;

import com.bank_web_app.backend.publiccustomer.service.PublicCustomerService;
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
@RequestMapping("/api/public-customers")
public class PublicCustomerController {

	private final PublicCustomerService publicCustomerService;

	public PublicCustomerController(PublicCustomerService publicCustomerService) {
		this.publicCustomerService = publicCustomerService;
	}

	@PostMapping("/draft")
	public ResponseEntity<UserRegistrationStepResponse> saveDraft(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(publicCustomerService.saveDraft(request));
	}

	@PostMapping
	public ResponseEntity<UserRegistrationStepResponse> register(@Valid @RequestBody BankCustomerStepOneRequest request) {
		return ResponseEntity.ok(publicCustomerService.register(request));
	}

	@GetMapping
	public ResponseEntity<List<BankCustomerSummaryResponse>> getAll() {
		return ResponseEntity.ok(publicCustomerService.getAll());
	}
}
