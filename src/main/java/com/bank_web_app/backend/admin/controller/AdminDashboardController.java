package com.bank_web_app.backend.admin.controller;

import com.bank_web_app.backend.admin.dto.response.AdminDashboardSummaryResponse;
import com.bank_web_app.backend.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "Admin dashboard summary endpoints")
public class AdminDashboardController {

	private final AdminDashboardService adminDashboardService;

	public AdminDashboardController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping("/summary")
	@Operation(
		summary = "Get admin dashboard summary",
		description = "Returns total counts for users, branches, officers and transactions.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Summary loaded successfully")
		}
	)
	public ResponseEntity<AdminDashboardSummaryResponse> getSummary() {
		return ResponseEntity.ok(adminDashboardService.getSummary());
	}
}
