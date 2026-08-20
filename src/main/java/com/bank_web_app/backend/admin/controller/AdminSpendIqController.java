package com.bank_web_app.backend.admin.controller;

import com.bank_web_app.backend.spendiq.service.BudgetLimitRolloverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/spendiq")
@Tag(name = "Admin SpendIQ", description = "Admin-triggered SpendIQ maintenance endpoints")
public class AdminSpendIqController {

	private final BudgetLimitRolloverService budgetLimitRolloverService;

	public AdminSpendIqController(BudgetLimitRolloverService budgetLimitRolloverService) {
		this.budgetLimitRolloverService = budgetLimitRolloverService;
	}

	@PostMapping("/budget-rollover")
	@Operation(
		summary = "Manually trigger budget rollover",
		description = "Copies every user's current-month budgets into next month for categories that don't already have a" +
			" next-month budget. This runs automatically on the last day of each month; use this endpoint to run it on" +
			" demand, e.g. for testing or backfilling a missed scheduled run."
	)
	public ResponseEntity<Map<String, Integer>> triggerBudgetRollover() {
		int created = budgetLimitRolloverService.rolloverBudgetsToNextMonth();
		return ResponseEntity.ok(Map.of("recordsCreated", created));
	}
}
