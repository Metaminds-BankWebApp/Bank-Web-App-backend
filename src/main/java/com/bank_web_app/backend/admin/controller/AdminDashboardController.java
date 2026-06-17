package com.bank_web_app.backend.admin.controller;
import com.bank_web_app.backend.admin.dto.response.AdminDashboardSummaryResponse;
import com.bank_web_app.backend.admin.dto.response.AdminMonthlyUserGrowthResponse;
import com.bank_web_app.backend.admin.dto.response.AdminRecentActionResponse;
import com.bank_web_app.backend.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for admin dashboard summary, monthly growth, and recent-action endpoints.
 */

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
	// Returns dashboard summary counts for admin overview cards.
	public ResponseEntity<AdminDashboardSummaryResponse> getSummary() {
		return ResponseEntity.ok(adminDashboardService.getSummary());
	}

	@GetMapping("/recent-actions")
	@Operation(
		summary = "Get recent admin actions",
		description = "Returns recent admin actions within the requested time window. Default window is last 12 hours.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Recent actions loaded successfully")
		}
	)
	// Returns recent admin actions within the requested time window.
	public ResponseEntity<List<AdminRecentActionResponse>> getRecentActions(
		@RequestParam(defaultValue = "12") Integer hours,
		@RequestParam(defaultValue = "20") Integer limit
	) {
		return ResponseEntity.ok(adminDashboardService.getRecentActions(hours, limit));
	}

	@GetMapping("/monthly-user-growth")
	@Operation(
		summary = "Get monthly user growth chart data",
		description = "Returns month-by-month new customer user counts (all, bank, public) for the requested window.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Monthly user growth loaded successfully")
		}
	)
	// Returns monthly user-growth metrics for the dashboard chart.
	public ResponseEntity<AdminMonthlyUserGrowthResponse> getMonthlyUserGrowth(
		@RequestParam(defaultValue = "6") Integer months
	) {
		return ResponseEntity.ok(adminDashboardService.getMonthlyUserGrowth(months));
	}
}

