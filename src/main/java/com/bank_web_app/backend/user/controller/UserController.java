package com.bank_web_app.backend.user.controller;

import com.bank_web_app.backend.user.dto.response.BankCustomerSummaryResponse;
import com.bank_web_app.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Hidden
@Tag(name = "Users", description = "User listing endpoints.")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/bank-officer/customers")
	public ResponseEntity<List<BankCustomerSummaryResponse>> getBankCustomersForOfficer() {
		return ResponseEntity.ok(userService.getBankCustomersForOfficer());
	}

	@GetMapping("/public-customer")
	public ResponseEntity<List<BankCustomerSummaryResponse>> getPublicCustomers() {
		return ResponseEntity.ok(userService.getPublicCustomers());
	}

	@GetMapping("/bank-officer")
	public ResponseEntity<List<BankCustomerSummaryResponse>> getBankOfficers() {
		return ResponseEntity.ok(userService.getBankOfficers());
	}
}
