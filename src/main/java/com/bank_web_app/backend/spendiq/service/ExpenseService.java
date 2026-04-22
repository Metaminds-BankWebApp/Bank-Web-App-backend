package com.bank_web_app.backend.spendiq.service;

import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.spendiq.dto.request.CreateExpenseRequest;
import com.bank_web_app.backend.spendiq.dto.response.CategoryBreakdownResponse;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseResponse;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseSummaryResponse;
import com.bank_web_app.backend.spendiq.entity.Expense;
import com.bank_web_app.backend.spendiq.repository.ExpenseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class ExpenseService {

	private final ExpenseRepository expenseRepository;
	private final AccountRepository accountRepository;

	public ExpenseService(ExpenseRepository expenseRepository, AccountRepository accountRepository) {
		this.expenseRepository = expenseRepository;
		this.accountRepository = accountRepository;
	}

	public ExpenseResponse createExpense(CreateExpenseRequest request) {
		Account account = getAccountOrThrow(request.accountId());

		Expense expense = new Expense();
		expense.setAccount(account);
		expense.setTitle(request.title().trim());
		expense.setCategory(request.category().trim().toUpperCase());
		expense.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
		expense.setExpenseDate(request.expenseDate());
		expense.setNotes(normalizeNotes(request.notes()));

		return toResponse(expenseRepository.save(expense));
	}

	@Transactional(readOnly = true)
	public List<ExpenseResponse> getExpenses(Long accountId, LocalDate from, LocalDate to) {
		Account account = getAccountOrThrow(accountId);
		validateDateRange(from, to);

		List<Expense> expenses = hasDateRange(from, to)
			? expenseRepository.findAllByAccount_AccountIdAndExpenseDateBetweenOrderByExpenseDateDescExpenseIdDesc(
				account.getAccountId(),
				from,
				to
			)
			: expenseRepository.findAllByAccount_AccountIdOrderByExpenseDateDescExpenseIdDesc(account.getAccountId());

		return expenses.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public ExpenseSummaryResponse getSummary(Long accountId, LocalDate from, LocalDate to) {
		Account account = getAccountOrThrow(accountId);
		LocalDate resolvedTo = to == null ? LocalDate.now() : to;
		LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
		validateDateRange(resolvedFrom, resolvedTo);

		List<Expense> expenses = expenseRepository.findAllByAccount_AccountIdAndExpenseDateBetweenOrderByExpenseDateDescExpenseIdDesc(
			account.getAccountId(),
			resolvedFrom,
			resolvedTo
		);

		BigDecimal totalSpent = expenses.stream()
			.map(Expense::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.setScale(2, RoundingMode.HALF_UP);

		long totalTransactions = expenses.size();
		BigDecimal averageExpense = totalTransactions == 0
			? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
			: totalSpent.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);

		Map<String, CategoryAccumulator> categoryTotals = new LinkedHashMap<>();
		for (Expense expense : expenses) {
			categoryTotals.computeIfAbsent(expense.getCategory(), ignored -> new CategoryAccumulator())
				.add(expense.getAmount());
		}

		List<CategoryBreakdownResponse> categoryBreakdown = categoryTotals.entrySet()
			.stream()
			.map(entry -> new CategoryBreakdownResponse(
				entry.getKey(),
				entry.getValue().total().setScale(2, RoundingMode.HALF_UP),
				entry.getValue().count()
			))
			.sorted(Comparator.comparing(CategoryBreakdownResponse::totalAmount).reversed())
			.toList();

		String topCategory = categoryBreakdown.isEmpty() ? null : categoryBreakdown.getFirst().category();
		List<ExpenseResponse> recentExpenses = expenses.stream().limit(5).map(this::toResponse).toList();

		return new ExpenseSummaryResponse(
			account.getAccountId(),
			account.getAccountNumber(),
			resolvedFrom,
			resolvedTo,
			totalSpent,
			totalTransactions,
			averageExpense,
			topCategory,
			categoryBreakdown,
			recentExpenses
		);
	}

	private Account getAccountOrThrow(Long accountId) {
		return accountRepository.findById(accountId)
			.orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Account not found."));
	}

	private boolean hasDateRange(LocalDate from, LocalDate to) {
		return from != null && to != null;
	}

	private void validateDateRange(LocalDate from, LocalDate to) {
		if ((from == null) != (to == null)) {
			throw new IllegalArgumentException("Provide both from and to dates together.");
		}
		if (from != null && from.isAfter(to)) {
			throw new IllegalArgumentException("From date must be on or before to date.");
		}
	}

	private String normalizeNotes(String notes) {
		if (notes == null) {
			return null;
		}
		String normalized = notes.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private ExpenseResponse toResponse(Expense expense) {
		return new ExpenseResponse(
			expense.getExpenseId(),
			expense.getAccount().getAccountId(),
			expense.getAccount().getAccountNumber(),
			expense.getTitle(),
			expense.getCategory(),
			expense.getAmount(),
			expense.getExpenseDate(),
			expense.getNotes(),
			expense.getCreatedAt(),
			expense.getUpdatedAt()
		);
	}

	private static final class CategoryAccumulator {

		private BigDecimal total = BigDecimal.ZERO;
		private long count = 0;

		private void add(BigDecimal amount) {
			total = total.add(amount);
			count++;
		}

		private BigDecimal total() {
			return total;
		}

		private long count() {
			return count;
		}
	}
}
