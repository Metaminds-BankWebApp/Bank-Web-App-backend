package com.bank_web_app.backend.spendiq.controller;

import com.bank_web_app.backend.spendiq.dto.request.CreateExpenseRequest;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseResponse;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseSummaryResponse;
import com.bank_web_app.backend.spendiq.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spendiq/expenses")
@Tag(name = "SpendIQ", description = "Expense tracking and spending insight APIs")
public class ExpenseController {

	private final ExpenseService expenseService;

	public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
		summary = "Create expense",
		description = "Create a manual expense record for an existing bank account.",
		responses = {
			@ApiResponse(responseCode = "201", description = "Expense created"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "404", description = "Account not found")
		}
	)
	public ExpenseResponse createExpense(@Valid @RequestBody CreateExpenseRequest request) {
		return expenseService.createExpense(request);
	}

	@GetMapping
	@Operation(
		summary = "List expenses",
		description = "List expenses for an account, optionally filtered by a date range.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Expenses returned"),
			@ApiResponse(responseCode = "400", description = "Invalid query parameters"),
			@ApiResponse(responseCode = "404", description = "Account not found")
		}
	)
	public ResponseEntity<List<ExpenseResponse>> getExpenses(
		@RequestParam Long accountId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return ResponseEntity.ok(expenseService.getExpenses(accountId, from, to));
	}

	@GetMapping("/summary")
	@Operation(
		summary = "Get expense summary",
		description = "Get SpendIQ summary metrics and category breakdown for an account.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Summary returned"),
			@ApiResponse(responseCode = "400", description = "Invalid query parameters"),
			@ApiResponse(responseCode = "404", description = "Account not found")
		}
	)
	public ResponseEntity<ExpenseSummaryResponse> getSummary(
		@RequestParam Long accountId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return ResponseEntity.ok(expenseService.getSummary(accountId, from, to));
	}
}
