package com.bank_web_app.backend.spendiq.controller;

import com.bank_web_app.backend.spendiq.dto.request.CreateExpenseCategoryRequest;
import com.bank_web_app.backend.spendiq.dto.request.CreateExpenseRecordRequest;
import com.bank_web_app.backend.spendiq.dto.request.CreateIncomeRecordRequest;
import com.bank_web_app.backend.spendiq.dto.request.UpsertBudgetLimitRequest;
import com.bank_web_app.backend.spendiq.dto.response.BudgetLimitResponse;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseCategoryResponse;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseRecordResponse;
import com.bank_web_app.backend.spendiq.dto.response.IncomeRecordResponse;
import com.bank_web_app.backend.spendiq.dto.response.SpendIqMonthlySummaryResponse;
import com.bank_web_app.backend.spendiq.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spendiq")
@Tag(name = "SpendIQ", description = "Expense, income, category, and budget endpoints")
public class ExpenseController {

	private final ExpenseService expenseService;

	public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}

	@PostMapping("/categories")
	@Operation(
		summary = "Create expense category",
		description = "Creates an expense category for the authenticated user.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Category created"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication required"),
			@ApiResponse(responseCode = "409", description = "Category already exists for this user")
		}
	)
	public ResponseEntity<ExpenseCategoryResponse> createCategory(@Valid @RequestBody CreateExpenseCategoryRequest request) {
		return ResponseEntity.ok(expenseService.createCategory(request));
	}

	@GetMapping("/categories")
	@Operation(
		summary = "Get expense categories",
		description = "Returns all categories owned by the authenticated user.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Categories returned"),
			@ApiResponse(responseCode = "401", description = "Authentication required")
		}
	)
	public ResponseEntity<List<ExpenseCategoryResponse>> getCategories() {
		return ResponseEntity.ok(expenseService.getCategories());
	}

	@PostMapping("/expenses")
	@Operation(
		summary = "Create expense record (JWT required)",
		description = "Creates an expense record for the authenticated user. paymentType is required and must be one of: CASH, BANK_TRANSFER, CARD.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Expense record created"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication required"),
			@ApiResponse(responseCode = "404", description = "Expense category not found for this user")
		}
	)
	public ResponseEntity<ExpenseRecordResponse> createExpense(@Valid @RequestBody CreateExpenseRecordRequest request) {
		return ResponseEntity.ok(expenseService.createExpense(request));
	}

	@GetMapping("/expenses")
	@Operation(
		summary = "Get expense records",
		description = "Returns expense records for the authenticated user, optionally filtered by date range.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Expense records returned"),
			@ApiResponse(responseCode = "400", description = "Invalid filter range"),
			@ApiResponse(responseCode = "401", description = "Authentication required")
		}
	)
	public ResponseEntity<List<ExpenseRecordResponse>> getExpenses(
		@Parameter(description = "Inclusive start date in ISO format (yyyy-MM-dd)", example = "2026-04-01")
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@Parameter(description = "Inclusive end date in ISO format (yyyy-MM-dd)", example = "2026-04-30")
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
	) {
		return ResponseEntity.ok(expenseService.getExpenses(fromDate, toDate));
	}

	@PostMapping("/incomes")
	@Operation(
		summary = "Create income record",
		description = "Creates an income record for the authenticated user.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Income record created"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication required")
		}
	)
	public ResponseEntity<IncomeRecordResponse> createIncome(@Valid @RequestBody CreateIncomeRecordRequest request) {
		return ResponseEntity.ok(expenseService.createIncome(request));
	}

	@GetMapping("/incomes")
	@Operation(
		summary = "Get income records",
		description = "Returns income records for the authenticated user, optionally filtered by date range.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Income records returned"),
			@ApiResponse(responseCode = "400", description = "Invalid filter range"),
			@ApiResponse(responseCode = "401", description = "Authentication required")
		}
	)
	public ResponseEntity<List<IncomeRecordResponse>> getIncomes(
		@Parameter(description = "Inclusive start date in ISO format (yyyy-MM-dd)", example = "2026-04-01")
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@Parameter(description = "Inclusive end date in ISO format (yyyy-MM-dd)", example = "2026-04-30")
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
	) {
		return ResponseEntity.ok(expenseService.getIncomes(fromDate, toDate));
	}

	@PostMapping("/budgets")
	@Operation(
		summary = "Create or update budget limit",
		description = "Creates a budget for category/month/year if missing, otherwise updates it.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Budget saved"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication required"),
			@ApiResponse(responseCode = "404", description = "Expense category not found for this user")
		}
	)
	public ResponseEntity<BudgetLimitResponse> upsertBudget(@Valid @RequestBody UpsertBudgetLimitRequest request) {
		return ResponseEntity.ok(expenseService.upsertBudget(request));
	}

	@GetMapping("/budgets")
	@Operation(
		summary = "Get budget limits",
		description = "Returns budget limits for the authenticated user, optionally filtered by month and year.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Budgets returned"),
			@ApiResponse(responseCode = "400", description = "Invalid month/year values"),
			@ApiResponse(responseCode = "401", description = "Authentication required")
		}
	)
	public ResponseEntity<List<BudgetLimitResponse>> getBudgets(
		@Parameter(description = "Month number (1-12)", example = "4")
		@RequestParam(required = false) Integer month,
		@Parameter(description = "Year value", example = "2026")
		@RequestParam(required = false) Integer year
	) {
		return ResponseEntity.ok(expenseService.getBudgets(month, year));
	}

	@GetMapping("/summary")
	@Operation(
		summary = "Get monthly SpendIQ summary",
		description = "Returns monthly totals for income, expenses, budgets, and budget usage.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Summary returned"),
			@ApiResponse(responseCode = "400", description = "Invalid month/year"),
			@ApiResponse(responseCode = "401", description = "Authentication required")
		}
	)
	public ResponseEntity<SpendIqMonthlySummaryResponse> getMonthlySummary(
		@Parameter(description = "Month number (1-12)", example = "4")
		@RequestParam Integer month,
		@Parameter(description = "Year value", example = "2026")
		@RequestParam Integer year
	) {
		return ResponseEntity.ok(expenseService.getMonthlySummary(month, year));
	}
}
